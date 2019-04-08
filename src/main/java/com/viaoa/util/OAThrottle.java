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
package com.viaoa.util;

import java.util.concurrent.atomic.*;

/**
 * Used to throttle based on a minimum time (ms) frame.
 * Use the check() method to know if it's time again.
 * @author vvia
 */
public class OAThrottle {

    private final AtomicLong aiMsLast = new AtomicLong();
    private final AtomicLong aiCnt = new AtomicLong();
    private long msDelay;

  
    public OAThrottle(long msDelay) {
        setDelay(msDelay);
    }

    public void setDelay(long msDelay) {
        this.msDelay = msDelay;
    }
    public long getDelay() {
        return msDelay;
    }
    
    /**
     * This will check to see if the the required delay/time has passed since the last call to check.
     * @return
     */
    public boolean check() {
        aiCnt.incrementAndGet();
        long msNow = System.currentTimeMillis();
        if (aiMsLast.get() + msDelay > msNow) {
            return false;
        }
        aiMsLast.set(msNow);
        return true;
    }
    
    public long now() {
        long ms = System.currentTimeMillis();
        return ms;
    }

    /**
     * sets throttle counter and last valid check time to 0L.
     */
    public void reset() {
        aiMsLast.set(0);
        aiCnt.set(0);
    }
    
    public long getCheckCount() {
        return aiCnt.get();
    }
    public long getCount() {
        return aiCnt.get();
    }
    /**
     * Returns the last time that a call to check() returned true.
     */
    public long getLastThrottle() {
        return aiMsLast.get();
    }
}
