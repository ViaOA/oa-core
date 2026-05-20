/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.concurrent;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.logging.Logger;

/*qqqqqqqqqqqqqqqqq
CODEX

1. OAPool.get / OAPool.loadMinimum — failed create() leaks pool capacity
     Severity: High
     Bug/risk: get() reserves a Pool slot under lock, marks it used, increments currentUsed, then calls create()
     outside the synchronized block. If create() throws or returns null, the reserved slot remains in alResource as
     used=true with resource=null, and currentUsed is never decremented. loadMinimum() has a related path where null-
     resource slots remain in the pool if create() fails.
     Production impact: a transient resource creation failure can permanently reduce or exhaust pool capacity. With
     max > 0, future callers can block forever even though no real resource exists. This is especially risky for
     OARemoteMultiplexerClient virtual socket pooling.
     Minimal hardening: wrap create() in try/catch; on failure, reacquire the lock, remove the reserved slot,
     decrement currentUsed if needed, notify waiters, then rethrow. Reject null resources or treat them as creation
     failure.
  2. OAPool.get — interrupted wait is swallowed and interrupt status is lost
     Severity: Medium
     Bug/risk: while waiting for a resource, alResource.wait() catches Exception and ignores it. InterruptedException
     is swallowed and the interrupt flag is cleared. The loop then continues waiting or returns a resource.
     Production impact: shutdown/cancel paths using thread interruption can fail to stop blocked pool callers. Under
     runtime shutdown or remote disconnect, this can leave blocked worker threads alive longer than intended.
     Minimal hardening: catch InterruptedException separately, restore interrupt status, and either throw a runtime
     exception or return according to an explicit pool cancellation contract.
  3. OAPool.remove / OAPool.release — removed(resource) exceptions can escape after state mutation
     Severity: Medium
     Bug/risk: pool state is already mutated before removed(resource) runs outside the lock. If removed throws, the
     resource has been removed from the pool and waiters may already have been notified or not notified depending on
     path. In OARemoteMultiplexerClient, removed(VirtualSocket) wraps close failure in RuntimeException.
     Production impact: cleanup failure can surface to callers after the pool has already dropped ownership, leaving
     the caller uncertain whether the resource is still usable or closed. For sockets, this can produce stale/half-
     closed resources outside the pool’s tracking.
     Minimal hardening: define removal cleanup semantics explicitly. Prefer logging/aggregating cleanup failure after
     state mutation, or return a status while guaranteeing the pool no longer owns the resource.


1. OAPool.release — shrinking release does not notify waiters
     Severity: High
     Bug/risk: when release(resource) decides bRelease=true, it removes the pool entry and exits without notifyAll().
     Waiters in get() may be blocked because the pool was at max; after removal, capacity is available, but no wakeup
     is sent.
     Production impact: a thread can remain blocked even though the pool now has room to create a replacement
     resource. This is a real stall risk for bounded pools such as remote virtual socket pools.
     Minimal hardening: notify waiters after any release that changes availability or capacity, including the shrink/
     remove path.
  2. OAPool.setMaximum / setMinimum / setHighMarkTimeLimit — dynamic pool configuration is unsynchronized
     Severity: Medium
     Bug/risk: min, max, and msHighMarkValidTimeLimit are plain fields. They are written without synchronization/
     volatile and read inside synchronized pool logic. setMaximum() also does not notify blocked waiters if the max is
     increased.
     Production impact: runtime code can change the maximum socket/resource pool size, but waiting threads might not
     see the new value promptly, and even if the value is visible they may remain parked until another release/add
     notification.
     Minimal hardening: guard configuration writes with the same alResource lock or make fields volatile; notify
     waiters when increasing max.
  3. OAPool.add — externally added resources can exceed maximum capacity
     Severity: Low/Medium
     Bug/risk: add(TYPE obj) unconditionally appends a resource and notifies waiters. It does not check max,
     duplicates, or whether the resource is already managed.
     Production impact: if used by runtime code to seed or replace resources, the pool can exceed its intended maximum
     and can contain duplicate entries for the same resource. Duplicate entries can later cause double-release/double-
     remove ambiguity.
     Minimal hardening: enforce max unless explicitly documented as an override path, and reject/ignore resources
     already present.

 */

/**
 * Generic thread-safe object pool that maintains a configurable minimum and
 * maximum number of pooled instances. The pool grows on demand, blocks callers
 * when all objects are in use and the maximum size has been reached, and
 * gradually shrinks when usage decreases. Instances are created and released
 * using the {@link #create()} and {@link #removed(Object)} callback methods. <p>
 *
 * The pool tracks active usage counts, high-water marks, and decay thresholds
 * to determine when idle resources may be released. Allocation and release
 * operations synchronize on the internal resource list, while object creation
 * is performed outside the synchronized block to avoid blocking other threads.
 * The class is thread-safe and suitable for concurrent use. <p>
 *
 * Subclasses should implement {@link #create()} to construct pooled objects
 * and {@link #removed(Object)} to dispose of objects that are no longer needed.
 * Each instance of the pool manages its own state and should not be shared
 * across class loaders.
 */
public abstract class OAPool<TYPE> {
    private static Logger LOG = Logger.getLogger(OAPool.class.getName());

    /**
     * Runtime class type of the pooled resource.
     */
    private Class<TYPE> classType;
    
    /**
     * Minimum number of resources the pool should maintain.
     */
    private int min;
    
    /**
     * Maximum number of resources allowed in the pool, or zero for unlimited.
     */
    private int max;
    
    /**
     * Count of threads currently waiting for a resource to become available.
     */
    private int waitCnt;
    
    /**
     * Internal list of pooled resources and their usage state.
     */
    private final ArrayList<Pool> alResource = new ArrayList<Pool>();
    
    /**
     * Current number of resources that are checked out for use.
     */
    private volatile int currentUsed;
    
    /**
     * High-water mark representing recent peak concurrent usage.
     */
    private volatile int highMark;
    
    /**
     * Time, in milliseconds, until which the high-water mark remains valid.
     */
    private volatile long msHighMarkValid;  // msTime that highMark is valid

    /**
     * Duration in milliseconds that the high-water mark remains valid.
     */
    private int msHighMarkValidTimeLimit = 5000;
    
    /**
     * Internal container representing a pooled resource and its usage state.
     */
    class Pool {
    	/**
    	 * The pooled resource instance.
    	 */
        TYPE resource;

        /**
         * Flag indicating whether the resource is currently in use.
         */
        boolean used;
    }
    
    /**
     * Creates a new pool using an explicit resource class type.
     *
     * @param clazz the class of the pooled resource
     * @param min the minimum number of resources to maintain
     * @param max the maximum number of resources allowed
     */
    public OAPool(Class clazz, int min, int max) {
        this.classType = clazz;
        this.min = min;
        this.max = max;
    }
    
    /**
     * Creates a new pool and infers the resource class type from the generic
     * superclass declaration.
     *
     * @param min the minimum number of resources to maintain
     * @param max the maximum number of resources allowed
     */
    public OAPool(int min, int max) {
        this.min = min;
        this.max = max;
        Class c = getClass();
        for (; c != null;) {
            Type type = c.getGenericSuperclass();
            if (type instanceof ParameterizedType) {
                classType = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
                break;
            }
            c = c.getSuperclass();
        }
        LOG.fine("classType=" + classType);
        if (classType == null) {
            throw new RuntimeException("class must define <TYPE>, or use constructure that accepts 'Class clazz'");
        }
    }
    
    /**
     * Sets the amount of time that the high-water mark remains valid before
     * allowing the pool to shrink.
     *
     * @param ms time limit in milliseconds
     */
    public void setHighMarkTimeLimit(int ms) {
        msHighMarkValidTimeLimit = ms;
    }
    
    /**
     * Sets the minimum number of resources the pool should maintain.
     *
     * @param x the minimum pool size
     */
    public void setMinimum(int x) {
        min = x;
    }

    /**
     * Returns the minimum number of resources the pool should maintain.
     *
     * @return the minimum pool size
     */
    public int getMinimum() {
        return min;
    }
    
    /**
     * Sets the maximum number of resources allowed in the pool.
     *
     * @param x the maximum pool size
     */
    public void setMaximum(int x) {
        max = x;
    }
    
    /**
     * Returns the maximum number of resources allowed in the pool.
     *
     * @return the maximum pool size
     */
    public int getMaximum() {
        return max;
    }
    
    /**
     * Returns the current number of pooled entries (in-use and available).
     *
     * @return the current size of the internal pool list
     */
    public int getCurrentSize() {
        int x;
        synchronized (alResource) {
            x = alResource.size();
        }
        return x;
    }
    
    /**
     * Returns the current number of pooled resources that are checked out for use.
     *
     * @return the number of resources currently in use
     */
    public int getCurrentUsed() {
        synchronized (alResource) {
            return currentUsed;
        }
    }

    /*
     * This will make sure that the pool has at least minimum amount of objects.
     * By default, the pool will start with size zero objects.  
     */
    /**
     * Ensures that the pool contains at least the configured minimum number of
     * resource entries, creating new resources as needed.
     */
    public void loadMinimum() {
        ArrayList<Pool> al = new ArrayList<Pool>(min);
        synchronized (alResource) {
            int x = alResource.size();
            for (int i=x; i<min; i++) {
                Pool p = new Pool();
                p.used = true;
                alResource.add(p);
                al.add(p);
            }
        }
        
        for (Pool p : al) {
            if (p.resource != null) continue;
            TYPE res = create();
            synchronized (alResource) {
                p.resource = res;
                p.used = false;
            }
        }
        if (al.size() > 0) {
            synchronized (alResource) {
                if (waitCnt > 0) alResource.notifyAll();
            }            
        }
    }
    
    /**
     * Obtains a resource from the pool, blocking if necessary until one becomes
     * available or a new resource can be created.
     *
     * @return an available pooled resource
     */
    public TYPE get() {
        Pool pool = null;
        synchronized (alResource) {
            for ( ;; ) {
                for (Pool p: alResource) {
                    if (!p.used && (p.resource != null)) {
                        pool = p;
                        break;
                    }
                }
                if (pool != null) break;
                
                int x = alResource.size();
                if (x < max || max == 0) {
                    pool = new Pool();
                    alResource.add(pool);
                    break;
                }

                // need to wait
                try {
                    waitCnt++;
                    alResource.wait();
                }
                catch (Exception e) {
                }
                finally {
                    waitCnt--;
                }
            }
            pool.used = true;
            
            currentUsed++;
            long msNow = System.currentTimeMillis();
            if (currentUsed >= highMark) {
                highMark = currentUsed;
                // System.out.println((new OATime()).toString("hh:mm:ss.S")+" get ... highMark="+highMark);                            
                msHighMarkValid = msNow + msHighMarkValidTimeLimit;
            }
            else if (msNow > msHighMarkValid) { // let it creep down
                highMark = Math.max(currentUsed, highMark-1);
                msHighMarkValid = msNow + (msHighMarkValidTimeLimit/3);
                // System.out.println((new OATime()).toString("hh:mm:ss.S")+" get/creepdown ... highMark="+highMark);                            
            }
        }
        // needs to be create outside of sync block
        if (pool.resource == null) {
            TYPE res = create();
            pool.resource = res;
        }
        return pool.resource;
    }
    
    /**
     * Removes the specified resource from the pool and invokes the removal callback.
     *
     * @param resource the resource to remove from the pool
     */
    public void remove(TYPE resource) {
        boolean bFound = false;
        synchronized (alResource) {
            for (Pool p: alResource) {
                if (p.resource != resource) continue;
                if (p.used) currentUsed--;
                p.used = false;
                alResource.remove(p);
                if (waitCnt > 0) alResource.notifyAll();
                bFound = true;
                break;
            }
        }        
        if (bFound) removed(resource);
    }
    
    /**
     * Returns a previously obtained resource to the pool, or removes it if pool
     * shrink conditions are met.
     *
     * @param resource the resource to release back to the pool
     */
    public void release(TYPE resource) {
        if (resource == null) return;
        boolean bRelease = false;
        synchronized (alResource) {
            for (Pool p: alResource) {
                if (p.resource != resource) continue;
                if (p.used) currentUsed--;
                p.used = false;

                int x = alResource.size();
                if (x > min) {
                    long msNow = System.currentTimeMillis();
                    int mark = (msNow > msHighMarkValid) ? min : highMark;
                    if (x > mark) {
                        bRelease = true;
                        if (msNow > msHighMarkValid) {
                            highMark = Math.max(currentUsed, highMark-1);
                            //System.out.println((new OATime()).toString("hh:mm:ss.S")+" releasing/creepdown ... highMark="+highMark);                            
                            msHighMarkValid = msNow + (msHighMarkValidTimeLimit/3);
                        }
                    }
                }
                if (bRelease) {
                    alResource.remove(p);
                }
                else {
                    if (waitCnt > 0) alResource.notifyAll();
                }
                break;
            }
        }
        if (bRelease) {
            removed(resource);
        }
    }

    /**
     * Returns an array containing all resources currently managed by the pool.
     *
     * @return an array of pooled resource instances
     */
    public Object[] getAllItems() {
        synchronized (alResource) {
            int x = alResource.size();
            Object[] objs = new Object[x];
            int i = 0;
            for (Pool pool : alResource) {
                objs[i++] = pool.resource;
            }
            return objs;
        }
    }

    /**
     * Adds an externally created resource to the pool and marks it as available.
     *
     * @param obj the resource instance to add
     */
    public void add(TYPE obj) {
        if (obj == null) return;
        synchronized (alResource) {
            Pool p = new Pool();
            p.used = false;
            p.resource = obj;
            alResource.add(p);
            if (waitCnt > 0) alResource.notifyAll();
        }
    }
    
    /**
     * Callback used to create a new resource instance for the pool.
     *
     * @return a newly created resource
     */
    protected abstract TYPE create();
    
    /**
     * Callback invoked when a resource is permanently removed from the pool.
     *
     * @param resource the resource that was removed
     */
    protected abstract void removed(TYPE resource);
    
}
