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
}
