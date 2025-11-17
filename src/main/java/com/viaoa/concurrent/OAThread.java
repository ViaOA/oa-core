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

import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.util.OADateTime;

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

	private final Object context;
	private Runnable runnable;

	public OAThread(Runnable runnable) {
		context = OAThreadLocalDelegate.getContext();
		this.runnable = runnable;
	}

	@Override
	public void run() {
		if (context != null) {
			OAThreadLocalDelegate.setContext(context);
		}
		runnable.run();
		if (context != null) {
			OAThreadLocalDelegate.setContext(null);
		}
	}

    public static void yield() {
        sleep(0);
    }

    public static void delay(long ms) {
        sleep(ms);
    }

    public static void sleepSeconds(long sec) {
        sleep(sec * 1000);
    }
    
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

    public static void sleepUntil(OADateTime dt) {
        sleepUntil(dt, 0);
    }
	
    public static void sleepUntil(OADateTime dt, long maxSeconds) {
        if (dt == null) return;
        
        OADateTime dtNow = new OADateTime();
        if (dtNow.before(dt)) {
            long secs = dtNow.betweenSeconds(dt);
            sleepSeconds( Math.min(secs, maxSeconds < 1 ? secs : maxSeconds) );
        }
    }
}
