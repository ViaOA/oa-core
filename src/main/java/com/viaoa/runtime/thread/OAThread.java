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
package com.viaoa.runtime.thread;

import com.viaoa.datetime.OADateTime;
import com.viaoa.runtime.OARuntime;
import com.viaoa.session.OASessionUser;

/**
 * Thread implementation that automatically propagates OA thread context from
 * the creating thread into the newly created thread. <p>
 *
 * The constructor snapshots the current context from
 * {@link com.viaoa.object.OAThreadLocalDelegate#getContext()}, restores it at
 * the beginning of {@link #run()}, and clears it when the task completes. This
 * ensures that background work has access to the same OAObjectGraph,
 * user/session information, or other context bound to the originating thread. <p>
 *
 * Also provides utility sleep methods (delay, yield, sleepUntil) that wrap
 * {@link Thread#sleep(long)} and integrate with OA temporal classes.
 */
public class OAThread extends Thread {

	/**
	 * Snapshot of the OA thread-local context captured from the creating thread.
	 * Restored at the start of {@link #run()} and cleared afterward.
	 */
	private final OAThreadLocal threadLocal;

	/**
	 * The runnable task executed by this thread. Invoked inside {@link #run()}
	 * after restoring OA context.
	 */
	private Runnable runnable;

	/**
	 * Creates a new OAThread that captures the current OA thread-local context and
	 * stores the runnable to execute.
	 *
	 * @param runnable the task to execute in this thread
	 */
	public OAThread(Runnable runnable) {
		threadLocal = OARuntime.thread().getThreadLocalService().getOAThreadLocal();
		this.runnable = runnable;
	}

	/**
	 * Restores the captured OA thread-local context, executes the runnable, and
	 * then clears the context when finished.
	 */
	@Override
	public void run() {
		if (threadLocal != null) {
			OARuntime.thread().getThreadLocalService().initialize(threadLocal);
		}
		try {
			runnable.run();
		}
		finally {
			OARuntime.thread().getThreadLocalService().initialize(null);
		}
	}

	/**
	 * Causes the currently executing thread to yield by delegating to
	 * {@link #sleep(long)} with a duration of zero.
	 */
    public static void yield() {
        sleep(0);
    }

    /**
     * Sleeps for the specified number of milliseconds. Delegates to
     * {@link #sleep(long)}.
     *
     * @param ms number of milliseconds to sleep
     */
    public static void delay(long ms) {
        sleep(ms);
    }

    /**
     * Sleeps for the specified number of seconds by converting the value to
     * milliseconds and delegating to {@link #sleep(long)}.
     *
     * @param sec number of seconds to sleep
     */
    public static void sleepSeconds(long sec) {
        sleep(sec * 1000);
    }
    
    /**
     * Sleeps the current thread for the specified duration. If {@code ms} is zero,
     * the thread yields instead of sleeping. Negative durations are ignored.
     * Exceptions during sleep are suppressed.
     *
     * @param ms milliseconds to sleep
     */
	public static void sleep(long ms) {
	    if (ms < 0) return;
	    try {
            if (ms > 0) {
                Thread.sleep(ms);
            } else {
                Thread.yield();
            }
	    }
	    catch (Exception e) {
	    }
	}

	/**
	 * Sleeps until the specified date/time occurs. Uses an unlimited maximum wait
	 * period. Delegates to {@link #sleepUntil(OADateTime, long)}.
	 *
	 * @param dt target date/time to sleep until
	 */
    public static void sleepUntil(OADateTime dt) {
        sleepUntil(dt, 0);
    }
	
    /**
     * Sleeps until the specified date/time occurs or until the maximum allowed
     * number of seconds has passed, whichever comes first.
     *
     * <p>If {@code dt} is in the future, the method computes the number of seconds
     * between now and the target time, sleeps for that duration (capped by
     * {@code maxSeconds} when greater than zero), and returns immediately if
     * {@code dt} is null or already in the past.</p>
     *
     * @param dt target date/time to sleep until
     * @param maxSeconds maximum number of seconds to sleep; if less than 1,
     *                   no maximum limit is applied
     */
    public static void sleepUntil(OADateTime dt, long maxSeconds) {
        if (dt == null) return;
        
        OADateTime dtNow = new OADateTime();
        if (dtNow.before(dt)) {
            long secs = dtNow.betweenSeconds(dt);
            sleepSeconds( Math.min(secs, maxSeconds < 1 ? secs : maxSeconds) );
        }
    }
}
