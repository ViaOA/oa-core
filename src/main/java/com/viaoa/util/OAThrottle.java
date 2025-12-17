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

	/**
	 * Stores the timestamp, in milliseconds, of the last successful throttle check.
	 * This value is updated when {@link #check()} returns {@code true}.
	 */
    private final AtomicLong aiMsLast = new AtomicLong();
    
    /**
     * Counts the total number of times {@link #check()} has been invoked.
     * This counter is incremented on every call, regardless of throttle outcome.
     */
    private final AtomicLong aiCnt = new AtomicLong();
    
    /**
     * The minimum delay interval, in milliseconds, that must elapse between
     * successful throttle checks.
     */
    private long msDelay;

  
    /**
     * Creates a new throttle instance with the specified delay interval.
     *
     * @param msDelay the minimum number of milliseconds required between
     *                successful calls to {@link #check()}.
     */
    public OAThrottle(long msDelay) {
        setDelay(msDelay);
    }

    /**
     * Sets the minimum delay interval between successful throttle checks.
     *
     * @param msDelay the delay interval in milliseconds.
     */
    public void setDelay(long msDelay) {
        this.msDelay = msDelay;
    }

    /**
     * Returns the configured delay interval for this throttle.
     *
     * @return the delay interval in milliseconds.
     */
    public long getDelay() {
        return msDelay;
    }
    
    /**
     * Checks whether the required delay interval has elapsed since the last
     * successful throttle check.
     * <p>
     * This method increments the internal check counter on every invocation.
     * If the delay interval has not yet elapsed, it returns {@code false}.
     * Otherwise, it updates the last successful check time and returns {@code true}.
     *
     * @return {@code true} if the delay interval has elapsed since the last
     *         successful check; {@code false} otherwise.
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
    
    /**
     * Returns the current system time in milliseconds.
     *
     * @return the current value of {@link System#currentTimeMillis()}.
     */
    public long now() {
        long ms = System.currentTimeMillis();
        return ms;
    }

    /**
     * Resets the throttle state by clearing the check counter and the last
     * successful throttle timestamp.
     * <p>
     * After calling this method, the next call to {@link #check()} will return
     * {@code true}.
     */
    public void reset() {
        aiMsLast.set(0);
        aiCnt.set(0);
    }
    
    /**
     * Returns the total number of times {@link #check()} has been called.
     *
     * @return the number of check invocations.
     */
    public long getCheckCount() {
        return aiCnt.get();
    }

    /**
     * Returns the total number of times {@link #check()} has been called.
     *
     * @return the number of check invocations.
     */
    public long getCount() {
        return aiCnt.get();
    }

    /**
     * Returns the total number of times {@link #check()} has been called.
     *
     * @return the number of check invocations.
     */
    public long getLastThrottle() {
        return aiMsLast.get();
    }
}
