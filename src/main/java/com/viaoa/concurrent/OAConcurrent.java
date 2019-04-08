/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Allows multiple runnables to all start and run at the same time, and waits for them to all complete 
 * before returning.
 * @author vvia
 *
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
