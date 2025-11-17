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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for executing a group of {@link Runnable} tasks concurrently with a
 * synchronized starting point. <p>
 *
 * OAConcurrent creates one thread per runnable, blocks each new thread on a
 * {@link java.util.concurrent.CyclicBarrier}, and then releases them so all
 * run simultaneously. A {@link java.util.concurrent.CountDownLatch} is used to
 * wait for all tasks to complete before returning from {@link #run()}. <p>
 *
 * This class does not interact with OAObject, Hub, or OA thread-context
 * mechanisms. It simply provides a deterministic way to launch a batch of
 * runnables with coordinated start timing. All threads are non-daemon and exit
 * only after their runnable finishes.
 */
public class OAConcurrent {
    private static Logger LOG = Logger.getLogger(OAConcurrent.class.getName());
    
    private CountDownLatch countDownLatch;
    private CyclicBarrier barrier;
    private Runnable[] runnables;
    
    public OAConcurrent(Runnable[] runnables) {
        this.runnables = runnables;
    }
    
    public void run() throws Exception {
        int max = (runnables == null) ? 0 : runnables.length;
        if (max == 0) return;
        
        countDownLatch = new CountDownLatch(max);
        barrier = new CyclicBarrier(max);
        
        for (int i=0; i<max; i++) {
            final int pos= i;
            Thread t = new Thread() {
                public void run() {
                    try {
                        barrier.await();
                        runnables[pos].run();
                    }
                    catch (Exception e) {
                        LOG.log(Level.WARNING, "exception in OAThreadManager", e);
                    }
                    finally {
                        countDownLatch.countDown();
                    }
                }
            };
            t.setName("OAConcurrent."+pos);
            t.start();
        }

        countDownLatch.await();
    }
}
