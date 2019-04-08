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
package com.viaoa.util;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.object.OAPerformance;

/**
 * Thread safe Circular Queue.
 * There is a single headPostion, and each consumer can have  a tailPosition.
 * Client access (getMessageXxx methods) will check overruns, and will throw an 
 * exception if a queue overrun occurs.
 * 
 * A session id can be used so that entries can be removed from the queue once all sessions have received it.
 * 
 * @author vvia
 * Note: this is made abstract to be able to get the Generic class that is used.
 */
public abstract class OACircularQueue<TYPE> {
    private static final Logger LOG = Logger.getLogger(OACircularQueue.class.getName());
    
    private volatile int queueSize;
    private final Object LOCKQueue = new Object();
    
    private volatile TYPE[] msgQueue;
    private String name;

    /** running value that keeps next position to insert a message.
     *  Uses module queueSize to determine the array position.  
    */
    private volatile long queueHeadPosition;  
    private volatile long queueLowPosition;  

    // last position that a registered session has used. All previous positions can be set to null
    private volatile long lastUsedPos;

    private volatile boolean bWaitingToGet;

    private Class<TYPE> classType;

    private final ConcurrentHashMap<Integer, Session> hmSession = new ConcurrentHashMap<Integer, Session>();;
    
    private final int MS_Throttle1 = 2;
    private final int MS_Throttle2 = 6;
    private final int MS_Wait = 20;
    
    
    private static class Session {
        int id;
        volatile long queuePos;
        volatile long msLastRead;
        volatile boolean bInactive; 
        volatile boolean bOverrun; 
    }
    
    
    /**
     * Create a new circular queue. 
     * @param queueSize actual size of the array that backs the queue.
     */
    public OACircularQueue(int queueSize) {
        this();
        setSize(queueSize);
    }
    public OACircularQueue(Class clazz, int queueSize) {
        this.classType = clazz;
        setSize(queueSize);
    }    
    protected OACircularQueue() {
        Class c = getClass();
        for (; c != null;) {
            Type type = c.getGenericSuperclass();
            if (type instanceof ParameterizedType) {
                classType = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
                break;
            }
            c = c.getSuperclass();
        }
        OACircularQueue.LOG.fine("classType=" + classType);
        if (classType == null) {
            throw new RuntimeException("class must define <TYPE>, or use construture that accepts 'Class clazz'");
        }
    }
    /**
     * set the size of the array behind the circular queue 
     */
    public void setSize(int queueSize) {
        synchronized(LOCKQueue) {
            this.queueSize = queueSize;
            msgQueue = (TYPE[]) Array.newInstance(classType, queueSize);
        }
    }
    public int getSize() {
        return queueSize;
    }
    
    
    /**
     * This is used to let the queue know who the consumers are, so that 
     * queue slots can be set to null once they are not needed.  
     * It is also used so that queue writers dont go too fast and overrun the readers.
     * @param sessionId identifier for the session
     * param maxFallBehind max amount that it can fall behind the head, else an addMessage will wait for up to 1 second.
     * @return current queueHeadPosition
     */
    public long registerSession(int sessionId) {
        this.queueLowPosition = 0; // reset
        Session session = new Session();
        session.id = sessionId;
        session.msLastRead = System.currentTimeMillis();
        
        long x;
        synchronized (LOCKQueue) {
            x = queueHeadPosition;
            session.queuePos = x;
            hmSession.put(session.id, session);
        }
        return x;
    }
    public void unregisterSession(int sessionId) {
        this.queueLowPosition = 0; // reset
        hmSession.remove(sessionId);
    }
    
    // 20141208 null out queue slots, so that they can be GC'd
    //   this is called from a sync block
    protected void cleanupQueue() {
        if (hmSession.size() == 0) return; // no session registered
        if (queueHeadPosition < 1) return;
        long pos = queueHeadPosition-1;
        boolean bFoundOne = false;
        for (Map.Entry<Integer, Session> entry : hmSession.entrySet()) {
            Session session = entry.getValue();
            if (session.queuePos < (queueHeadPosition - queueSize)) {
                continue; // overflow
            }
            bFoundOne = true;
            pos = Math.min(pos, session.queuePos); 
        }
        if (bFoundOne && lastUsedPos < pos) {
            lastUsedPos = Math.max(queueHeadPosition-queueSize, lastUsedPos);
            for (long i=lastUsedPos; i<pos; i++) {
                msgQueue[(int)(i % queueSize)] = null;
            }
            lastUsedPos = pos;
        }
    }
    
    /**
     * current position where the next message will be added. 
     */
    public long getHeadPostion() {
        synchronized(LOCKQueue) {
            return queueHeadPosition;
        }
    }
    
    /**
     * Add a new message to the queue.
     * @return position of message in the queue
     */
    public int addMessageToQueue(TYPE msg) {
        return addMessageToQueue(msg, 0);
    }
    public int addMessage(TYPE msg) {
        return addMessageToQueue(msg, 0);
    }
    /**
     * param bThrottle if true, then make sure that headPos is not too far ahead of readers
     */
    public int addMessageToQueue(final TYPE msg, final int throttleAmount) {
        return addMessageToQueue(msg, throttleAmount, -1);
    }
    
    public int addMessageToQueue(final TYPE msg, final int throttleAmount, final int throttleSessionToIgnore) {
        int x;
        for (int i=0 ; ;i++) {
            synchronized(LOCKQueue) {
                x = _addMessage(msg, throttleAmount, throttleSessionToIgnore, i);
            }
            if (x >= 0) break;
            try {
                Thread.sleep(-x);
            }
            catch (Exception e) {
                System.out.println("circque error:"+e);
            }
        }
        return x;
    }
    
    private volatile int cntQueueWait; // number of times a addMessage has called wait.    
    private volatile int cntQueueThrottle;    
    private volatile long tsLastAvoidOverrunLog;
    private volatile long tsLastThrottleLog;
    private volatile long tsLastAddLog;
    private volatile long tsLastOneSecondLog;
    
    
    private int _addMessage(final TYPE msg, int throttleAmount, final int throttleSessionToIgnore, final int retryCnt) {
        final long tsNow = System.currentTimeMillis();
        if (throttleAmount < 1 && ((queueLowPosition + queueSize) > (queueHeadPosition + Math.min(100,(queueSize/10)))) ) {
        }
        else {
            queueLowPosition = queueHeadPosition;
            boolean bNeedsThrottle = false;
            Session slowSessionFound = null;
            
            for (Map.Entry<Integer, Session> entry : hmSession.entrySet()) {
                Session session = entry.getValue();
                if (session.bInactive || session.bOverrun) {
                    continue;
                }
                
                if ((session.queuePos + queueSize) < queueHeadPosition) {
                    session.bOverrun = true;
                    continue; // overflowed already
                }
                queueLowPosition = Math.min(session.queuePos, queueLowPosition);

                boolean bIsSafe = (session.queuePos + queueSize) > (queueHeadPosition + Math.min(100,(queueSize/10)));
                
                // check to see if it is getting close to a queue overrun
                if (bIsSafe) {
                    if (throttleAmount < 1 || bNeedsThrottle || retryCnt > 10) {
                    }
                    else {
                        // see if it needs to be throttled
                        if ((session.queuePos + throttleAmount) > queueHeadPosition) continue;
                        if (session.id != throttleSessionToIgnore) {
                            bNeedsThrottle = true;
                        }
                    }
                    continue;
                }

                slowSessionFound = session;
                
                if (session.msLastRead + 1000 < tsNow) {
                    if (tsLastOneSecondLog + 1000 < tsNow) {
                        String s = ("session over 1+ seconds getting last msg, queSize="+queueSize+
                                ", currentHeadPos="+queueHeadPosition+", session="+session.id+
                                ", sessionPos="+session.queuePos+", lastRead="+(tsNow-session.msLastRead)+"ms ago");
                        tsLastOneSecondLog = tsNow;
                        OACircularQueue.LOG.fine(s);
                        if (OAPerformance.IncludeCircularQueue) OAPerformance.LOG.fine(s);
                    }
                    if (!shouldWaitOnSlowSession(session.id, (int)(tsNow-session.msLastRead))) {
                        session.bInactive = true;
                        continue;  // too slow, dont wait for this one
                    }
                }
            }

            
            if (slowSessionFound != null) {
                ++cntQueueWait;
                if (tsNow > tsLastAvoidOverrunLog + 1000) {
                    String s = ("cqName="+name+", avoiding queue overrun, queSize="+queueSize+", queHeadPos="+queueHeadPosition+
                        ", totalSessions="+hmSession.size() +
                        ", slowSession="+slowSessionFound.id +
                        ", qpos="+slowSessionFound.queuePos +
                        ", totalWaits="+cntQueueWait +
                        ", totalThrottles="+cntQueueThrottle
                        );
                    OACircularQueue.LOG.fine(s);
                    if (OAPerformance.IncludeCircularQueue) OAPerformance.LOG.fine(s);
                    tsLastAvoidOverrunLog = tsNow;
                    tsLastAddLog = tsNow;
                }
                if (retryCnt < 200) return -MS_Wait;
                bNeedsThrottle = false;
            }

        
            if (bNeedsThrottle) {
                ++cntQueueThrottle;
                if (tsNow > tsLastThrottleLog + 1000) {
                    String s = ("cqName="+name+", queue throttle, queSize="+queueSize+", queHeadPos="+queueHeadPosition+
                        ", totalSessions="+hmSession.size() +
                        ", throttleAmount="+throttleAmount +
                        ", totalWaits="+cntQueueWait +
                        ", totalThrottles="+cntQueueThrottle
                        );
                    OACircularQueue.LOG.fine(s);
                    if (OAPerformance.IncludeCircularQueue) OAPerformance.LOG.fine(s);
                    tsLastThrottleLog = tsNow;
                    tsLastAddLog = tsNow;
                }
                if (retryCnt < 5) return -MS_Throttle1;
                return -MS_Throttle2;
            }            
        }

        if (tsNow > tsLastAddLog + 5000) {
            String s = ("cqName="+name+", queSize="+queueSize+", queHeadPos="+queueHeadPosition+
                ", totalSessions="+hmSession.size() +
                ", throttleAmount="+throttleAmount +
                ", totalWaits="+cntQueueWait +
                ", totalThrottles="+cntQueueThrottle
                );
            OACircularQueue.LOG.fine(s);
            if (OAPerformance.IncludeCircularQueue) OAPerformance.LOG.fine(s);
            tsLastAddLog = tsNow;
        }
        
        int posHead = (int) (queueHeadPosition++ % queueSize);
        
        if (queueHeadPosition < 0) {
            queueHeadPosition = posHead + 1;
        }
        msgQueue[posHead] = msg;
        if (bWaitingToGet) {
            bWaitingToGet = false;
            LOCKQueue.notifyAll();
        }
        
        return posHead;
    }
    
    public final int msWaitUntilNotified = -1;
    
    /**
     * This is call whenever a session is getting close to a queue overrun and it has not
     * called get for 1+ seconds.
     * @param sessionId
     * @return default is false
     */
    protected boolean shouldWaitOnSlowSession(int sessionId, int msSinceLastRead) {
        return false;
    }
    
    /**
     * will block until a message is available.
     */
    public TYPE getMessage(long posTail) throws Exception {
        TYPE[] vals = getMessages(posTail, 1, msWaitUntilNotified);
        return vals[0];
    }
    /**
     * Get next message, with a timeout
     * @param posTail current position to pull messages from
     * @param msMaxWait max number of milliseconds to wait
     */
    public TYPE getMessage(long posTail, int msMaxWait) throws Exception {
        TYPE[] vals = getMessages(posTail, 1, msMaxWait);
        if (vals == null || vals.length == 0) return null;
        return vals[0];
    }
    public int getAmountAvailable(long posTail) throws Exception {
        int amt;
        synchronized(LOCKQueue) {
            if ((posTail + queueSize) <= queueHeadPosition) {
                throw new Exception("message queue overrun");
            }
            amt = (int) (queueHeadPosition - posTail);
        }
        return amt;
    }
    
    /**
     * will block until at least one message is available.
     */
    public TYPE[] getMessages(long posTail) throws Exception {
        return getMessages(posTail, 0, msWaitUntilNotified);
    }
    
    /**
     * will block until at least one message is available.
     */
    public TYPE[] getMessages(long posTail, int maxReturnAmount) throws Exception {
        return getMessages(posTail, maxReturnAmount, msWaitUntilNotified);
    }

    /**
     * @param posTail current position to use to get next message
     * @param maxReturnAmount 
     * @param msMaxWait if no messages are available, wait this amount of miliseconds for an available message.
     */
    public TYPE[] getMessages(long posTail, int maxReturnAmount, int msMaxWait) throws Exception {
        TYPE[] msgs =  _getMessages(-1, null, posTail, maxReturnAmount, msMaxWait);
        return msgs;
    }
    public TYPE[] getMessages(final int sessionId, final long posTail, final int maxReturnAmount, int msMaxWait) throws Exception {
        TYPE[] msgs = null;

        Session session;
        if (sessionId >= 0 && hmSession.size() != 0) {
            session = hmSession.get(sessionId);
        }
        else session = null;
        
        if (session != null) {
            session.msLastRead = System.currentTimeMillis();
            session.bInactive = false;
        }
        msgs = _getMessages(sessionId, session, posTail, maxReturnAmount, msMaxWait);
        if (session != null) session.msLastRead = System.currentTimeMillis();

        if (msgs != null && msgs.length > 0) {
            if (session != null) session.queuePos = (posTail + msgs.length);
        }        
        return msgs;
    }

    private volatile long msLastTime;
    
    private TYPE[] _getMessages(final int sessionId, final Session session, long posTail, final int maxReturnAmount, final int maxWait) throws Exception {
        int amt;
        
        if ((posTail + queueSize) < queueHeadPosition) {
            if (session != null) session.bOverrun = true;
            throw new Exception("message queue overrun, sessionId="+sessionId+", pos="+posTail+", headPos="+queueHeadPosition);
        }
        else {
            if (posTail > queueHeadPosition) {
                posTail = queueHeadPosition;
                //throw new IllegalArgumentException("posTail should not be larger then headPos");
            }
        }
        
        // first check without locking
        amt = (int) ((queueHeadPosition-1) - posTail);  // note: use -1 since this code is not sync, and the addMsg could be in the process
        if (maxReturnAmount > 0 && amt > maxReturnAmount) {
            amt = maxReturnAmount;
        }
        
        if (amt <= 0 && maxWait != 0) {
            // 20190320 this was not sync'd
            /*was
            private AtomicInteger  aiCleanupQueue = new AtomicInteger(); 
            if (aiCleanupQueue.incrementAndGet() % 50 == 0) {
                cleanupQueue();
            }
            */

            synchronized(LOCKQueue) {
                long ms = System.currentTimeMillis();
                if (msLastTime < (ms - 5000)) {
                    msLastTime = ms;
                    cleanupQueue();
                }
                
                for (int i=0; ;i++) {
                    amt = (int) (queueHeadPosition - posTail);
                    if (amt > 0) {
                        if (maxReturnAmount > 0 && amt > maxReturnAmount) {
                            amt = maxReturnAmount;
                        }
                        break;
                    }
                    if (i > 0 && maxWait > 0) break;
                    
                    bWaitingToGet = true;
                    if (maxWait > 0) {
                        LOCKQueue.wait(maxWait);
                    }
                    else {
                        LOCKQueue.wait();
                    }
                }
            }
        }

        TYPE[] msgs;
        if (amt > 0) {
            msgs = (TYPE[]) Array.newInstance(classType, amt);
            for (int i=0; i<amt; i++) {
                msgs[i] = msgQueue[ (int) (posTail++ % queueSize) ]; 
            }
        }
        else {
            msgs = null;
        }
        return msgs;
    }

    public void keepAlive(final int sessionId) {
        Session session = hmSession.get(sessionId);
        if (session != null) {
            session.msLastRead = System.currentTimeMillis();
            session.bInactive = false;
        }
    }
    
    /**
     * Get message at actual position in queue
     * @param pos is actual array position, must be less then queSize, else null is returned. 
     * @return
     */
    public TYPE getMessagesAtPos(int pos) {
        if (pos < 0 || pos >= msgQueue.length) return null;
        TYPE x = msgQueue[pos];
        return x;
    }
    
    public void setName(String s) {
        this.name = s;
    }
    public String getName() {
        return name;
    }
    
}
