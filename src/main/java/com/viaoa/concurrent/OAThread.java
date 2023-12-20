package com.viaoa.concurrent;

import com.viaoa.object.OAThreadLocalDelegate;

/**
 * Allows a thread to have the same context as the thread that created it.
 *
 * @author vvia
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
    
	public static void sleep(long ms) {
	    if (ms <= 0) return;
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
	
}
