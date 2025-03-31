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
package com.viaoa.remote;

import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.remote.info.RequestInfo;

public class OARemoteThreadDelegate {

    public static boolean isRemoteThread() {
        Thread t = Thread.currentThread();
        return (t instanceof OARemoteThread);
    }

    /**
     * Used to check to check if a RemoteThread has reach it's "mark"/purpose
     * for the method that it's currently processing. 
     */
    public static boolean isSafeToCallRemoteMethod() {
        Thread t = Thread.currentThread();
        if (!(t instanceof OARemoteThread)) return true;
        OARemoteThread rt = (OARemoteThread) t;
        if (rt.startedNextThread) return true;
        return false;
    }
    
    /**
     * By default OARemoteThreads do not sent messages. 
     * This is to check if OARemoteThread.sendMessages is true.
     */
    public static boolean shouldSendMessages() {
        Thread t = Thread.currentThread();
        if (!(t instanceof OARemoteThread)) return true;
        return ((OARemoteThread) t).getSendMessages();
    }

    /**
     * This is called once the msg that is being processed has met it's "mark".
     * This will notify another OARemoteThread to process the next msg in the queue.
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
     * Check to see if nextThread has been started.
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
     * Get the current RequestInfo message that is being processed by this thread.
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
     * Flat to have oasync msgs sent to other computers.
     */
    public static boolean sendMessages() {
        return sendMessages(true);
    }
    /**
     * This allows messages from an OARemoteThread to be sent out to clients.
     * By default, any messages generated from an OARemoteThread are not sent.
     */
    public static boolean sendMessages(boolean b) {
        Thread t = Thread.currentThread();
        if (!(t instanceof OARemoteThread)) return true;
        boolean bx = ((OARemoteThread) t).getSendMessages();
        ((OARemoteThread) t).setSendMessages(b);
        return bx;
    }

    public static boolean isRemoteThreadSendingMessages() {
        Thread t = Thread.currentThread();
        if (!(t instanceof OARemoteThread)) return false;
        boolean bx = ((OARemoteThread) t).getSendMessages();
        return bx;
    }
    
    
    public static boolean shouldEventsBeQueued() {
        Thread t = Thread.currentThread();
        if (!(t instanceof OARemoteThread)) return false;
        OARemoteThread rt = (OARemoteThread) t;
        return rt.getAllowRunnable();
    }
    
    /**
     * If this is an OARemoteThread, then add this to background thread processing.
     * @param r runnable to run.
     * @return true if runnable was added to que for background OARemoteThreads to process.
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
