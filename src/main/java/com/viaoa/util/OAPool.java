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
package com.viaoa.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.logging.Logger;

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
