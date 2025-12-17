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
package com.viaoa.util;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.object.OAPerformance;


/**
 * High-performance, thread-safe Circular Queue supporting multi-consumer
 * messaging with per-session delivery tracking. Designed for distributed
 * synchronization (OASync) and durable message fan-out.
 *
 * <p>Each producer append increments a monotonically increasing head
 * position. Each consumer tracks its own tail position. Overrun detection
 * ensures that no consumer reads stale data; a queue overrun will trigger
 * an exception so the session can recover.</p>
 *
 * <p>Queue entries are cleared once all registered sessions have advanced
 * past them, enabling efficient memory use without GC pressure.</p>
 *
 * <p>The queue purposely uses the same array instance for its lifetime,
 * reducing allocation and supporting extremely high-throughput write paths.</p>
 *
 * <b>Key Features</b>
 * <ul>
 *   <li>Multiple consumers with independent progress</li>
 *   <li>Guaranteed ordering, no message loss without overrun signal</li>
 *   <li>Efficient cleanup of delivered messages</li>
 *   <li>Optional throttling if consumers lag behind</li>
 *   <li>Thread-safe single-writer model</li>
 *   <li>Wait/notify support for blocking reads</li>
 * </ul>
 *
 * <p>Intended for use by OASync and other internal messaging layers where
 * throughput, ordering, and fault detection are critical.</p>
 * 
 * Note: this is made abstract to be able to get the Generic class that is used.
 */
public abstract class OACircularQueue<TYPE> {
    private static final Logger LOG = Logger.getLogger(OACircularQueue.class.getName());
    
    /**
     * The size of the underlying array backing the circular queue.
     */
    private volatile int queueSize;
    
    /**
     * Lock object used to synchronize access to queue state and operations.
     */
    private final Object LOCKQueue = new Object();
    
    /**
     * The array that stores queued messages in circular fashion.
     */
    private volatile TYPE[] msgQueue;
    
    /**
     * Optional name assigned to this circular queue instance.
     */
    private String name;

    /**
     * Monotonically increasing position indicating where the next message
     * will be inserted into the queue.
     */
    private volatile long queueHeadPosition;  

    /**
     * Tracks the lowest queue position currently in use by active sessions.
     */
    private volatile long queueLowPosition;  

    /**
     * The last queue position that was fully consumed by all registered sessions.
     */
    private volatile long lastUsedPos;

    /**
     * Flag indicating that one or more consumer threads are waiting for messages.
     */
    private volatile boolean bWaitingToGet;

    /**
     * The runtime {@link Class} representing the generic TYPE stored in the queue.
     */
    private Class<TYPE> classType;

    /**
     * Map of session identifiers to their corresponding session state.
     */
    private final ConcurrentHashMap<Integer, Session> hmSession = new ConcurrentHashMap<Integer, Session>();;
    
    /**
     * Millisecond delay used for initial throttling when consumers lag behind.
     */
    private final int MS_Throttle1 = 2;

    /**
     * Millisecond delay used for extended throttling when throttling persists.
     */
    private final int MS_Throttle2 = 6;
    
    /**
     * Millisecond delay used when waiting to avoid queue overrun.
     */
    private final int MS_Wait = 20;
    
    
    /**
     * Internal structure used to track per-session queue consumption state.
     */
    private static class Session {
        int id;
        volatile long queuePos;
        volatile long msLastRead;
        volatile boolean bInactive; 
        volatile boolean bOverrun; 
    }
    
    
    /**
     * Creates a new circular queue with the specified backing array size.
     *
     * This constructor initializes the instance and sets the queue size.
     *
     * @param queueSize the size of the array backing the queue
     */
    public OACircularQueue(int queueSize) {
        this();
        setSize(queueSize);
    }

    /**
     * Creates a new circular queue with an explicit message class and array size.
     *
     * @param clazz the {@link Class} representing the message type
     * @param queueSize the size of the array backing the queue
     */
    public OACircularQueue(Class clazz, int queueSize) {
        this.classType = clazz;
        setSize(queueSize);
    }    
    
    /**
     * Protected constructor that determines the generic message type using
     * reflection on the class hierarchy.
     *
     * This constructor resolves the TYPE parameter from the generic superclass
     * definition and validates that it is available.
     */
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
     * Sets the size of the array backing the circular queue.
     *
     * This method reallocates the internal message array using the configured
     * message type.
     *
     * @param queueSize the new size of the queue array
     */
    public void setSize(int queueSize) {
        synchronized(LOCKQueue) {
            this.queueSize = queueSize;
            msgQueue = (TYPE[]) Array.newInstance(classType, queueSize);
        }
    }
    
    /**
     * Returns the size of the array backing the circular queue.
     *
     * @return the queue size
     */
    public int getSize() {
        return queueSize;
    }
    
    /**
     * Registers a new consumer session with the queue.
     *
     * This initializes session state and sets the session's starting position
     * to the current head position.
     *
     * @param sessionId identifier for the session
     * @return the current queue head position
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
    
    /**
     * Unregisters a consumer session from the queue.
     *
     * This removes the session so it is no longer tracked for consumption
     * or cleanup purposes.
     *
     * @param sessionId identifier for the session to remove
     */
    public void unregisterSession(int sessionId) {
        this.queueLowPosition = 0; // reset
        hmSession.remove(sessionId);
    }
    
    /**
     * Cleans up queue entries that are no longer needed by any registered session.
     *
     * This method nulls out message slots that all active sessions have
     * advanced past.
     */
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
     * Returns the current head position where the next message will be added.
     *
     * @return the current queue head position
     */
    public long getHeadPostion() {
        synchronized(LOCKQueue) {
            return queueHeadPosition;
        }
    }
    
    /**
     * Adds a message to the queue without throttling.
     *
     * @param msg the message to add
     * @return the position where the message was added
     */
    public int addMessageToQueue(TYPE msg) {
        return addMessageToQueue(msg, 0);
    }

    /**
     * Adds a message to the queue without throttling.
     *
     * @param msg the message to add
     * @return the position where the message was added
     */
    public int addMessage(TYPE msg) {
        return addMessageToQueue(msg, 0);
    }

    /**
     * Adds a message to the queue with a specified throttle amount.
     *
     * @param msg the message to add
     * @param throttleAmount throttle limit to apply
     * @return the position where the message was added
     */
    public int addMessageToQueue(final TYPE msg, final int throttleAmount) {
        return addMessageToQueue(msg, throttleAmount, -1);
    }
    
    /**
     * Adds a message to the queue with throttling and an optional session to ignore.
     *
     * This method may wait or retry internally to avoid queue overruns.
     *
     * @param msg the message to add
     * @param throttleAmount throttle limit to apply
     * @param throttleSessionToIgnore session identifier to ignore for throttling
     * @return the position where the message was added
     */
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
    
    /**
     * Counter tracking how many times message insertion waited to avoid overrun.
     */
    private volatile int cntQueueWait; // number of times a addMessage has called wait.    

    /**
     * Counter tracking how many times message insertion was throttled.
     */
    private volatile int cntQueueThrottle;    
    
    /**
     * Timestamp of the last log entry related to avoiding a queue overrun.
     */
    private volatile long tsLastAvoidOverrunLog;
    
    /**
     * Timestamp of the last log entry related to throttling.
     */
    private volatile long tsLastThrottleLog;
    
    /**
     * Timestamp of the last periodic add-message status log.
     */
    private volatile long tsLastAddLog;
    
    /**
     * Timestamp of the last log entry related to a session lagging over one second.
     */
    private volatile long tsLastOneSecondLog;
    
    
    /**
     * Internal method that performs the actual insertion of a message into the queue.
     *
     * This method applies overrun detection, throttling logic, and wait behavior
     * based on session consumption state and retry count.
     *
     * @param msg the message to add
     * @param throttleAmount throttle limit to apply
     * @param throttleSessionToIgnore session identifier to ignore for throttling
     * @param retryCnt number of retries attempted so far
     * @return the position where the message was added, or a negative value
     *         indicating a wait duration
     */
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
    
    /**
     * Constant indicating that a consumer should wait until notified
     * rather than using a fixed timeout.
     */
    public final int msWaitUntilNotified = -1;
    
    /**
     * Called when a session is approaching a queue overrun condition.
     *
     * @param sessionId identifier for the session
     * @param msSinceLastRead milliseconds since the session last read a message
     * @return {@code false} by default, indicating the queue should not wait
     */
    protected boolean shouldWaitOnSlowSession(int sessionId, int msSinceLastRead) {
        return false;
    }
    
    /**
     * Returns the next available message, blocking until one is available.
     *
     * @param posTail current position to pull the message from
     * @return the next message
     * @throws Exception if a queue overrun occurs
     */
    public TYPE getMessage(long posTail) throws Exception {
        TYPE[] vals = getMessages(posTail, 1, msWaitUntilNotified);
        return vals[0];
    }
    
    /**
     * Returns the next available message, waiting up to the specified time.
     *
     * @param posTail current position to pull the message from
     * @param msMaxWait maximum number of milliseconds to wait
     * @return the next message, or {@code null} if none are available
     * @throws Exception if a queue overrun occurs
     */
    public TYPE getMessage(long posTail, int msMaxWait) throws Exception {
        TYPE[] vals = getMessages(posTail, 1, msMaxWait);
        if (vals == null || vals.length == 0) return null;
        return vals[0];
    }

    /**
     * Returns the number of messages currently available starting from the
     * specified tail position.
     *
     * @param posTail current position to evaluate availability from
     * @return the number of available messages
     * @throws Exception if the queue has been overrun
     */
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
     * Returns available messages starting from the specified tail position.
     *
     * This method blocks until at least one message is available.
     *
     * @param posTail current position to pull messages from
     * @return an array of messages
     * @throws Exception if a queue overrun occurs
     */
    public TYPE[] getMessages(long posTail) throws Exception {
        return getMessages(posTail, 0, msWaitUntilNotified);
    }
    
    /**
     * Returns available messages starting from the specified tail position.
     *
     * This method blocks until at least one message is available or the
     * maximum return amount is reached.
     *
     * @param posTail current position to pull messages from
     * @param maxReturnAmount maximum number of messages to return
     * @return an array of messages
     * @throws Exception if a queue overrun occurs
     */
    public TYPE[] getMessages(long posTail, int maxReturnAmount) throws Exception {
        return getMessages(posTail, maxReturnAmount, msWaitUntilNotified);
    }

    /**
     * Returns available messages starting from the specified tail position.
     *
     * @param posTail current position to pull messages from
     * @param maxReturnAmount maximum number of messages to return
     * @param msMaxWait maximum number of milliseconds to wait for messages
     * @return an array of messages
     * @throws Exception if a queue overrun occurs
     */
    public TYPE[] getMessages(long posTail, int maxReturnAmount, int msMaxWait) throws Exception {
        TYPE[] msgs =  _getMessages(-1, null, posTail, maxReturnAmount, msMaxWait);
        return msgs;
    }
    
    /**
     * Returns available messages for a specific session.
     *
     * This method updates session state before and after retrieving messages.
     *
     * @param sessionId identifier for the session
     * @param posTail current position to pull messages from
     * @param maxReturnAmount maximum number of messages to return
     * @param msMaxWait maximum number of milliseconds to wait for messages
     * @return an array of messages
     * @throws Exception if a queue overrun occurs
     */
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

    /**
     * Timestamp used to control periodic cleanup operations.
     */
    private volatile long msLastTime;
    
    /**
     * Internal method that retrieves messages from the queue.
     *
     * This method handles blocking, overrun detection, cleanup, and message copying.
     *
     * @param sessionId identifier for the session
     * @param session session state, or {@code null} if not applicable
     * @param posTail current position to pull messages from
     * @param maxReturnAmount maximum number of messages to return
     * @param maxWait maximum number of milliseconds to wait for messages
     * @return an array of messages, or {@code null} if none are available
     * @throws Exception if a queue overrun occurs
     */
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

    /**
     * Updates the last-read timestamp for the specified session.
     *
     * @param sessionId identifier for the session to keep active
     */
    public void keepAlive(final int sessionId) {
        Session session = hmSession.get(sessionId);
        if (session != null) {
            session.msLastRead = System.currentTimeMillis();
            session.bInactive = false;
        }
    }
    
    /**
     * Returns the message at the specified array position.
     *
     * @param pos actual array position within the queue
     * @return the message at the position, or {@code null} if invalid
     */
    public TYPE getMessagesAtPos(int pos) {
        if (pos < 0 || pos >= msgQueue.length) return null;
        TYPE x = msgQueue[pos];
        return x;
    }
    
    /**
     * Sets the name for this circular queue instance.
     *
     * @param s the name to assign
     */
    public void setName(String s) {
        this.name = s;
    }

    /**
     * Returns the name of this circular queue instance.
     *
     * @return the queue name
     */
    public String getName() {
        return name;
    }
    
}
