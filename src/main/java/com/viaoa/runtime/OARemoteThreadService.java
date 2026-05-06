package com.viaoa.runtime;

import java.util.logging.Logger;

import com.viaoa.remote.info.RequestInfo;
import com.viaoa.runtime.thread.OARemoteThread;

public abstract class OARemoteThreadService {
	private Logger LOG = Logger.getLogger(OARemoteThreadService.class.getName());

	public OARemoteThreadService() {
	}

	/**
	 * Determines whether the current thread is an {@link OARemoteThread}. The
	 * method checks the runtime type of the current thread and returns true if it
	 * is an instance of {@code OARemoteThread}.
	 *
	 * @return true if the current thread is an OARemoteThread, otherwise false
	 */
    public boolean isRemoteThread() {
        Thread t = Thread.currentThread();
        return (t instanceof OARemoteThread);
    }

    /**
     * Determines whether it is safe to perform additional remote method calls
     * within the current thread. If the thread is not an
     * {@link OARemoteThread}, it is always considered safe; otherwise the method
     * returns true only if the thread has already started the next remote thread.
     *
     * @return true if it is safe to call a remote method, otherwise false
     */
    public boolean isSafeToCallRemoteMethod() {
        Thread t = Thread.currentThread();
        if (!(t instanceof OARemoteThread)) return true;
        OARemoteThread rt = (OARemoteThread) t;
        if (rt.startedNextThread) return true;
        return false;
    }
    
    /**
     * Marks that the current thread has reached its primary processing point and
     * may allow another {@link OARemoteThread} to begin handling the next
     * message. If the current thread is a remote thread and has not already
     * started the next thread, its {@code startNextThread()} method is invoked.
     * Afterward, any thread waiting in {@code OAThreadLocalDelegate} is notified.
     */
    public void startNextThread() {
        Thread t = Thread.currentThread();
        if (t instanceof OARemoteThread) {
            OARemoteThread rt = (OARemoteThread) t;
            if (rt.startedNextThread) return;
            rt.startNextThread();
        }
        callThreadLocalNotifyWaitingThread();
    }
    
    
    
    /**
     * Returns whether the current thread has already signaled that another
     * {@link OARemoteThread} may begin processing a new message. If the current
     * thread is not a remote thread, the method always returns true.
     *
     * @return true if the next thread has been started, otherwise false
     */
    public boolean startedNextThread() {
        Thread t = Thread.currentThread();
        if (t instanceof OARemoteThread) {
            OARemoteThread rt = (OARemoteThread) t;
            return rt.startedNextThread();
        }
        return true;
    }

    /**
     * Retrieves the {@link RequestInfo} associated with the current thread if it
     * is an {@link OARemoteThread}. Otherwise, returns null.
     *
     * @return the RequestInfo for the current remote thread, or null if not a remote thread
     */
    public RequestInfo getRequestInfo() {
        Thread t = Thread.currentThread();
        if (t instanceof OARemoteThread) {
            OARemoteThread rt = (OARemoteThread) t;
            return rt.requestInfo;
        }
        return null;
    }
    
    
    /**
     * Determines whether runnable events should be queued for background
     * processing. This is true only if the current thread is an
     * {@link OARemoteThread} and the thread allows runnables to be queued.
     *
     * @return true if events should be queued, otherwise false
     */
    public boolean shouldEventsBeQueued() {
        Thread t = Thread.currentThread();
        if (!(t instanceof OARemoteThread)) return false;
        OARemoteThread rt = (OARemoteThread) t;
        return rt.getAllowRunnable();
    }
    
    /**
     * Queues the given runnable for execution within the current
     * {@link OARemoteThread}, provided that the thread allows runnables to be
     * processed. If the current thread is not a remote thread or event queuing is
     * not allowed, the method returns false. Otherwise, the runnable is executed
     * via the thread's {@code addRunnable} method and true is returned.
     *
     * @param r the runnable to process
     * @return true if the runnable was queued and executed, otherwise false
     */
    public boolean queueEvent(Runnable r) {
        Thread t = Thread.currentThread();
        if (!(t instanceof OARemoteThread)) return false;
        OARemoteThread rt = (OARemoteThread) t;
        if (!rt.getAllowRunnable()) return false;

        rt.addRunnable(r);
        return true;
    }
	
    protected abstract void callThreadLocalNotifyWaitingThread();
}
