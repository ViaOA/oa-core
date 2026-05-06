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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.viaoa.lang.OAString;

/**
 * Wrapper around {@link java.util.concurrent.ThreadPoolExecutor} that creates a
 * named, daemon-thread executor for background processing within OA-based
 * applications. <p>
 *
 * OAExecutorService supports two modes:
 * <ul>
 *   <li><b>size == 0</b>: creates an unbounded cached thread pool using a
 *       {@link java.util.concurrent.SynchronousQueue}.</li>
 *   <li><b>size > 0</b>: creates a fixed number of worker threads backed by a
 *       very large {@link java.util.concurrent.LinkedBlockingQueue}.</li>
 * </ul>
 *
 * Each submitted task increments an internal counter, and tasks may be
 * submitted as {@link Runnable} or {@link Callable}, optionally using timed
 * waits. All threads created are daemon threads and therefore do not block JVM
 * exit. <p>
 *
 * This class does not automatically propagate OA thread context from the
 * submitting thread; callers must handle OAThreadLocalDelegate propagation if
 * required. The executor is lazily created when first needed.
 */
public class OAExecutorService {
    private static Logger LOG = Logger.getLogger(OAExecutorService.class.getName());
    
    /**
     * Internal thread pool executor. Lazily created by {@link #getExecutorService()}.
     */
    private ThreadPoolExecutor executorService;
    
    /**
     * Counter tracking the total number of tasks submitted to this executor.
     */
    private final AtomicInteger aiTotalSubmitted = new AtomicInteger();
    
    /**
     * Determines executor behavior:
     * <ul>
     *   <li>0 = cached/unbounded thread pool</li>
     *   <li>> 0 = fixed-size thread pool</li>
     * </ul>
     */
    private final int size;
    
    /**
     * Optional name used as part of thread naming when creating worker threads.
     */
    private final String name;
    
    /**
     * Queue backing the fixed-size thread pool. Unused when {@link #size} == 0.
     */
    private LinkedBlockingQueue<Runnable> que;
    
    /**
     * Constructs an executor service in cached-thread-pool mode
     * (equivalent to size = 0).
     */
    public OAExecutorService() {
        this(0, null);
    }

    /**
     * Constructs an executor service in cached-thread-pool mode using the
     * specified naming prefix for worker threads.
     *
     * @param name optional naming prefix for threads
     */
    public OAExecutorService(String name) {
        this.size = 0;
        this.name = name;
        getExecutorService();
    }
    
    /**
     * Constructs an executor service with the specified pool size and thread
     * naming prefix. When size is 0, a cached thread pool is created; otherwise a
     * fixed-size pool is created.
     *
     * @param size number of worker threads, or 0 for cached mode
     * @param name optional naming prefix for threads
     */
    public OAExecutorService(int size, String name) {
        this.size = size;
        this.name = name;
        getExecutorService();
    }
    
    /**
     * Submits a runnable task for asynchronous execution.
     * Increments the submitted-task counter and forwards the task to
     * {@link #getExecutorService()}.
     *
     * @param r runnable to submit
     * @return future representing the task
     * @throws RuntimeException if the executor has been shut down
     */
    public Future submit(Runnable r) {
        if (executorService == null) throw new RuntimeException("executorService has been shutdown");
        aiTotalSubmitted.incrementAndGet();
        Future f = getExecutorService().submit(r);
        return f;
    }

    /**
     * Submits a runnable task and waits up to the specified timeout for it to
     * complete. The return value is the same {@link Future} produced by the
     * executor.
     *
     * @param r runnable to run
     * @param maxWait maximum wait duration
     * @param tu time unit for maxWait
     * @return future representing the task
     * @throws Exception if the timeout expires or the task throws an exception
     * @throws RuntimeException if the executor has been shut down
     */
    public Future submitAndWait(Runnable r, int maxWait, TimeUnit tu) throws Exception {
        if (executorService == null) throw new RuntimeException("executorService has been shutdown");
        aiTotalSubmitted.incrementAndGet();
        Future f = getExecutorService().submit(r);
        Object objx = f.get(maxWait, tu);
        objx = null; // no-op
        return f;
    }
    
    /**
     * Submits a callable task for asynchronous execution.
     * Increments the submitted-task counter and returns the resulting Future.
     *
     * @param c callable to submit
     * @return future representing the task
     * @throws RuntimeException if the executor has been shut down
     */
    public Future submit(Callable c) {
        if (executorService == null) throw new RuntimeException("executorService has been shutdown");
        aiTotalSubmitted.incrementAndGet();
        Future f = getExecutorService().submit(c);
        return f;
    }

    /**
     * Submits a callable task and waits for it to complete up to the specified
     * timeout. The result of {@link Future#get(long, TimeUnit)} is ignored, and
     * only the Future itself is returned.
     *
     * @param c callable to run
     * @param maxWait maximum wait duration
     * @param tu time unit for maxWait
     * @return future representing the task
     * @throws Exception if the timeout expires or the callable throws an exception
     * @throws RuntimeException if the executor has been shut down
     */
    public Future submitAndWait(Callable c, int maxWait, TimeUnit tu) throws Exception {
        if (executorService == null) throw new RuntimeException("executorService has been shutdown");
        aiTotalSubmitted.incrementAndGet();
        Future f = getExecutorService().submit(c);
        Object objx = f.get(maxWait, tu);
        objx = null; // no-op
        return f;
    }

    /**
     * Shuts down the executor service gracefully. After shutdown, no new tasks
     * may be submitted.
     */
    public void close() {
        if (executorService == null) return;
        executorService.shutdown();
    }
    
    /**
     * Lazily creates and returns the internal {@link ThreadPoolExecutor}.  
     *
     * <p>Behavior:</p>
     * <ul>
     *   <li>Creates daemon-thread workers using a custom {@link ThreadFactory}.</li>
     *   <li>Uses {@link SynchronousQueue} when size == 0.</li>
     *   <li>Uses a very large {@link LinkedBlockingQueue} when size > 0.</li>
     *   <li>In fixed-size mode, allows core threads to time out.</li>
     * </ul>
     *
     * @return executor service instance
     */
    public ExecutorService getExecutorService() {
        if (executorService != null) return executorService;
        
        ThreadFactory tf = new ThreadFactory() {
            AtomicInteger ai = new AtomicInteger();
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                String s = "";
                if (OAString.isNotEmpty(name)) s = name+".";
                t.setName("OAExecutorService."+ s + ai.getAndIncrement());
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        };
        
        if (size == 0) {
            // executorService = Executors.newCachedThreadPool(tf);
            executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), tf); 
        }
        else {
            // min/max must be equal, since new threads are only created when queue is full
            que = new LinkedBlockingQueue<Runnable>(Integer.MAX_VALUE);
            executorService = new ThreadPoolExecutor(size, size, 60L, TimeUnit.SECONDS, que, tf); 
            executorService.allowCoreThreadTimeOut(true);
        }
        return executorService;
    }
    
    /**
     * Returns the number of tasks currently waiting in the queue for the
     * fixed-size pool. Returns 0 for cached-mode executors.
     *
     * @return number of queued tasks
     */
    public int getQueueSize() {
        if (que == null) return 0;
        return que.size();
    }

    /**
     * Returns the number of threads in the pool, or 0 if the executor has not been
     * created yet.
     *
     * @return current pool size
     */
    public int getThreadPoolSize() {
        if (executorService == null) return 0;
        return executorService.getPoolSize();
    }

    /**
     * Returns the number of actively executing threads, or 0 if the executor has
     * not been created yet.
     *
     * @return active thread count
     */
    public int getActiveThreads() {
        if (executorService == null) return 0;
        return executorService.getActiveCount();
    }

    /**
     * Test harness demonstrating ThreadPoolExecutor behavior using various pool
     * configurations. Not used by OA runtime code.
     *
     * @param args ignored
     * @throws Exception if test execution fails
     */
    public static void main(String[] args) throws Exception {
        Executors.newCachedThreadPool();
        Executors.newFixedThreadPool(12);
        
        ThreadPoolExecutor te = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>());

        for (int i=0; i<10; i++) {
            final int id = i;
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    System.out.println("Run START for "+id);
                    try {
                        Thread.sleep(10000);
                    }
                    catch (Exception e) {}
                    System.out.println("Run DONE for "+id);
                }
            };
            System.out.println("Created "+id);
            te.submit(r);
        }
    }
}
