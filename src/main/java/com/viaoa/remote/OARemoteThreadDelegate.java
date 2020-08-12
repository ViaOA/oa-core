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

import java.util.ArrayList;

import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheListener;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.sync.OASyncDelegate;

public class OARemoteThreadDelegate {

    public static boolean isRemoteThread() {
        Thread t = Thread.currentThread();
        return (t instanceof OARemoteThread);
    }

    /**
     * used to check to make sure that a RemoteThread is not holding up the msg queue
     */
    public static boolean isSafeToCallRemoteMethod() {
        Thread t = Thread.currentThread();
        if (!(t instanceof OARemoteThread)) return true;
        OARemoteThread rt = (OARemoteThread) t;
        if (rt.startedNextThread) return true;
        return false;
    }
    
    public static boolean shouldSendMessages() {
        Thread t = Thread.currentThread();
        if (!(t instanceof OARemoteThread)) return true;
        return ((OARemoteThread) t).getSendMessages();
    }

    /**
     * this will start another thread to process the next msg in the queue.
     * returns true if current thread is remoteThread.
     */
    public static void startNextThread() {
        Thread t = Thread.currentThread();
        if (t instanceof OARemoteThread) {
            OARemoteThread rt = (OARemoteThread) t;
            if (rt.startedNextThread) return;
            rt.startNextThread();
        }
        // 20160121
        OAThreadLocalDelegate.notifyWaitingThread();
    }
    public static boolean startedNextThread() {
        Thread t = Thread.currentThread();
        if (t instanceof OARemoteThread) {
            OARemoteThread rt = (OARemoteThread) t;
            return rt.startedNextThread();
        }
        return true;
    }

    
    public static RequestInfo getRequestInfo() {
        Thread t = Thread.currentThread();
        if (t instanceof OARemoteThread) {
            OARemoteThread rt = (OARemoteThread) t;
            return rt.requestInfo;
        }
        return null;
    }
    
    
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
    public static boolean queueEvent(Runnable r) {
        Thread t = Thread.currentThread();
        if (!(t instanceof OARemoteThread)) return false;
        OARemoteThread rt = (OARemoteThread) t;
        if (!rt.getAllowRunnable()) return false;

        rt.addRunnable(r);
        return true;
    }
}
