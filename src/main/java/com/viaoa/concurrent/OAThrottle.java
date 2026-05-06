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

import java.util.concurrent.atomic.*;

/*qqqqqqqqqqqqqq
CODEX

 - Method: check
  - Issue: Check-then-set race allows multiple concurrent callers through for the same interval.
  - Why it is a problem: Two threads can both read an expired aiMsLast, both pass the delay test, both set
    aiMsLast, and both return true. This violates the class contract that check() returns true only once per delay
    interval.
  - Classification: CODEX/FIXNOW

 - Method: check
  - Issue: aiMsLast.get() + msDelay can overflow.
  - Why it is a problem: With a very large delay, adding msDelay to a positive timestamp can wrap negative and
    allow calls through immediately.
  - Classification: CODEX/FIXNOW

  - Method: setDelay, getDelay, check
  - Issue: msDelay is mutable but not volatile/atomic.
  - Why it is a problem: A thread calling setDelay() does not have a guaranteed visibility relationship with
    threads concurrently calling check(), so callers can continue using a stale delay.
  - Classification: CODEX/FIXNOW

 - Method: setDelay, check
  - Issue: Negative delays are accepted and make check() effectively always pass.
  - Why it is a problem: A bad configuration such as new OAThrottle(-1) disables throttling silently, which is
    risky for logging/event suppression.
  - Classification: CODEX/CONTRACT

 - Method: check
  - Issue: Uses System.currentTimeMillis() for elapsed-time control.
  - Why it is a problem: Wall-clock changes affect throttle behavior. If the system clock moves backward,
    throttling can suppress operations much longer than msDelay; if it jumps forward, it can allow early.
  - Classification: CODEX/DEFER

 - Method: reset, check
  - Issue: reset() is not atomic with respect to check().
  - Why it is a problem: A concurrent check() can increment aiCnt and/or set aiMsLast while reset() is clearing
    them, producing ambiguous state: lost check counts or an immediate post-reset pass being overwritten by reset.
  - Classification: CODEX/CONTRACT

 - Method: getLastThrottle
  - Issue: Method returns the last successful timestamp, but the JavaDoc says it returns total check count.
  - Why it is a problem: The method name and implementation are useful, but the documented contract is wrong and
    can mislead callers/tests.
  - Classification: CODEX/CONTRACT

*/

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
