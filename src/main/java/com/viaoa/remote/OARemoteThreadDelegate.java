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
package com.viaoa.remote;

import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.remote.info.RequestInfo;

/**
 * Static helper methods used to interact with the execution context of
 * {@link OARemoteThread}. This class hides the internal details of remote
 * thread management and provides a simple, safe API for:
 *
 * <ul>
 *   <li>detecting whether the current thread is a remote-processing thread,</li>
 *   <li>determining whether it is safe to invoke additional remote methods,</li>
 *   <li>controlling whether events generated inside a remote thread should be
 *       broadcast to other clients,</li>
 *   <li>signaling that the current thread has passed its primary execution
 *       point and another remote thread may begin processing the next
 *       message,</li>
 *   <li>optionally queueing background runnables for execution by remote
 *       worker threads.</li>
 * </ul>
 *
 * <p>
 * These methods are used throughout OA's remote messaging layer to coordinate
 * correct sequencing and to avoid ripple effects when multiple clients process
 * the same message concurrently.
 * </p>
 *
 * <p>
 * Methods that enable or disable message broadcasting return the previous
 * state so callers may temporarily override the setting in a
 * try/finally pattern.
 * </p>
 */
public class OARemoteThreadDelegate {

	/**
	 * Determines whether the current thread is an {@link OARemoteThread}. The
	 * method checks the runtime type of the current thread and returns true if it
	 * is an instance of {@code OARemoteThread}.
	 *
	 * @return true if the current thread is an OARemoteThread, otherwise false
	 */
    public static boolean isRemoteThread() {
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
    public static boolean isSafeToCallRemoteMethod() {
        Thread t = Thread.currentThread();
        if (!(t instanceof OARemoteThread)) return true;
        OARemoteThread rt = (OARemoteThread) t;
        if (rt.startedNextThread) return true;
        return false;
    }
    
    /**
     * Indicates whether remote messages should be broadcast from the current
     * thread. If the current thread is not an {@link OARemoteThread}, messages
     * should be sent. If it is a remote thread, its internal send-messages flag
     * determines the result.
     *
     * @return true if messages should be sent, otherwise false
     */
    public static boolean shouldSendMessages() {
        Thread t = Thread.currentThread();
        if (!(t instanceof OARemoteThread)) return true;
        return ((OARemoteThread) t).getSendMessages();
    }

    /**
     * Marks that the current thread has reached its primary processing point and
     * may allow another {@link OARemoteThread} to begin handling the next
     * message. If the current thread is a remote thread and has not already
     * started the next thread, its {@code startNextThread()} method is invoked.
     * Afterward, any thread waiting in {@code OAThreadLocalDelegate} is notified.
     */
    public static void startNextThread() {
        Thread t = Thread.currentThread();
        if (t instanceof OARemoteThread) {
            OARemoteThread rt = (OARemoteThread) t;
            if (rt.startedNextThread) return;
            rt.startNextThread();
        }
        OAThreadLocalDelegate.notifyWaitingThread();
    }
    
    /**
     * Returns whether the current thread has already signaled that another
     * {@link OARemoteThread} may begin processing a new message. If the current
     * thread is not a remote thread, the method always returns true.
     *
     * @return true if the next thread has been started, otherwise false
     */
    public static boolean startedNextThread() {
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
    public static RequestInfo getRequestInfo() {
        Thread t = Thread.currentThread();
        if (t instanceof OARemoteThread) {
            OARemoteThread rt = (OARemoteThread) t;
            return rt.requestInfo;
        }
        return null;
    }
    
    /**
     * Enables message sending for the current thread by delegating to
     * {@link #sendMessages(boolean)} with a value of true.
     *
     * @return the previous send-messages state for the current thread
     */
    public static boolean sendMessages() {
        return sendMessages(true);
    }
    
    /**
     * Updates whether messages should be sent from the current thread. If the
     * thread is not an {@link OARemoteThread}, messages are always considered
     * sendable and the method returns true. For remote threads, the previous
     * send-messages state is returned after setting the new state.
     *
     * @param b true to enable message sending, false to disable it
     * @return the previous send-messages state
     */
    public static boolean sendMessages(boolean b) {
        Thread t = Thread.currentThread();
        if (!(t instanceof OARemoteThread)) return true;
        boolean bx = ((OARemoteThread) t).getSendMessages();
        ((OARemoteThread) t).setSendMessages(b);
        return bx;
    }

    /**
     * Checks whether the current thread, if it is an {@link OARemoteThread}, is
     * currently configured to send remote messages.
     *
     * @return true if the remote thread is sending messages, otherwise false
     */
    public static boolean isRemoteThreadSendingMessages() {
        Thread t = Thread.currentThread();
        if (!(t instanceof OARemoteThread)) return false;
        boolean bx = ((OARemoteThread) t).getSendMessages();
        return bx;
    }
    
    /**
     * Determines whether runnable events should be queued for background
     * processing. This is true only if the current thread is an
     * {@link OARemoteThread} and the thread allows runnables to be queued.
     *
     * @return true if events should be queued, otherwise false
     */
    public static boolean shouldEventsBeQueued() {
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
    public static boolean queueEvent(Runnable r) {
        Thread t = Thread.currentThread();
        if (!(t instanceof OARemoteThread)) return false;
        OARemoteThread rt = (OARemoteThread) t;
        if (!rt.getAllowRunnable()) return false;

        rt.addRunnable(r);
        return true;
    }
}
