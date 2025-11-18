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
package com.viaoa.util;

import java.util.concurrent.atomic.*;

/**
 * Thread-safe time-based throttle used to limit how frequently an operation
 * may succeed. A call to {@link #check()} returns {@code true} only if at
 * least the configured delay interval has elapsed since the last successful
 * check. All calls increment an internal counter regardless of whether the
 * throttle condition is satisfied. <p>
 *
 * The class maintains the timestamp of the last permitted check using
 * {@link AtomicLong} and is safe for concurrent use. A call to {@link #reset()}
 * clears both the counter and the last-allowed timestamp, causing the next
 * invocation of {@code check()} to return {@code true}. <p>
 *
 * This utility is typically used to suppress excessive event firing, logging,
 * polling, or other operations that should not run more frequently than a
 * specified interval.
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
