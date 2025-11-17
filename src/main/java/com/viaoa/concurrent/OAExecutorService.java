/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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

import com.viaoa.util.OAString;

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
    private ThreadPoolExecutor executorService;
    private final AtomicInteger aiTotalSubmitted = new AtomicInteger();
    private final int size;
    private final String name;
    private LinkedBlockingQueue<Runnable> que;
    
    public OAExecutorService() {
        this(0, null);
    }

    public OAExecutorService(String name) {
        this.size = 0;
        this.name = name;
        getExecutorService();
    }
    
    public OAExecutorService(int size, String name) {
        this.size = size;
        this.name = name;
        getExecutorService();
    }
    
    public Future submit(Runnable r) {
        if (executorService == null) throw new RuntimeException("executorService has been shutdown");
        aiTotalSubmitted.incrementAndGet();
        Future f = getExecutorService().submit(r);
        return f;
    }
    public Future submitAndWait(Runnable r, int maxWait, TimeUnit tu) throws Exception {
        if (executorService == null) throw new RuntimeException("executorService has been shutdown");
        aiTotalSubmitted.incrementAndGet();
        Future f = getExecutorService().submit(r);
        Object objx = f.get(maxWait, tu);
        objx = null; // no-op
        return f;
    }
    
    public Future submit(Callable c) {
        if (executorService == null) throw new RuntimeException("executorService has been shutdown");
        aiTotalSubmitted.incrementAndGet();
        Future f = getExecutorService().submit(c);
        return f;
    }
    public Future submitAndWait(Callable c, int maxWait, TimeUnit tu) throws Exception {
        if (executorService == null) throw new RuntimeException("executorService has been shutdown");
        aiTotalSubmitted.incrementAndGet();
        Future f = getExecutorService().submit(c);
        Object objx = f.get(maxWait, tu);
        objx = null; // no-op
        return f;
    }

    public void close() {
        if (executorService == null) return;
        executorService.shutdown();
    }
    
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
     * number of elements in the queue waiting on thread to pick it up.
     */
    public int getQueueSize() {
        if (que == null) return 0;
        return que.size();
    }
    public int getThreadPoolSize() {
        if (executorService == null) return 0;
        return executorService.getPoolSize();
    }
    public int getActiveThreads() {
        if (executorService == null) return 0;
        return executorService.getActiveCount();
    }

    // test
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
