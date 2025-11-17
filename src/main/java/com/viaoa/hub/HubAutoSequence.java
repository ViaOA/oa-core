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
package com.viaoa.hub;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.lang.reflect.*;

import com.viaoa.object.*;
import com.viaoa.remote.OARemoteThreadDelegate;

/**
 * Automatically maintains a numeric sequence property on every object in a {@link Hub},
 * keeping the property value equal to the object’s position in that Hub.
 *
 * <p>Used for ordered collections where the visual or logical order matters but
 * is not defined by a data sort (e.g., form line numbers, menu order, display rank).</p>
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li>Listens for structural Hub events (add, insert, remove, move, sort, newList).</li>
 *   <li>Recomputes and updates the sequence property using reflection on the specified setter method.</li>
 *   <li>Supports a configurable start number (default 0) and optional “keep sequence” mode
 *       to decide whether to close gaps after removals.</li>
 *   <li>In distributed configurations, can suppress per-object change messages and send
 *       a single consolidated update from the server.</li>
 * </ul>
 *
 * <h3>Constructor Parameters</h3>
 * <ul>
 *   <li><b>hub</b> – the Hub whose objects are to be sequenced.</li>
 *   <li><b>propertyName</b> – the numeric OAObject property to update.</li>
 *   <li><b>startNumber</b> – starting index (typically 0 or 1).</li>
 *   <li><b>bKeepSeq</b> – if true, numbers stay contiguous after removals.</li>
 *   <li><b>bServerSideOnly</b> – if true, sequence updates are performed on the server
 *       and pushed to clients through Hub messaging.</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * Hub<InvoiceLine> hubLines = new Hub<>(InvoiceLine.class);
 * new HubAutoSequence(hubLines, "lineNumber", 1, false, true);
 * }</pre>
 *
 * This ensures each {@code InvoiceLine} has its {@code lineNumber} property set to
 * match its position in the Hub, starting at 1.
 */
public class HubAutoSequence extends HubListenerAdapter implements java.io.Serializable {
    static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(HubAutoSequence.class.getName());

    public static int autoSequenceHubListenerCount;
    
    protected Hub hub;
    protected String propertyName;
    protected transient Method propertySetMethod;
    protected int startNumber;
    protected boolean bKeepSeq;
    protected boolean bServerSideOnly;
    
    public HubAutoSequence() {
    }

    protected void finalize() throws Throwable {
    	close();
        super.finalize();
    }

    public void close() {
        if (hub != null) setHub(null);
    }
    
    /**
        Create a new HubAutoSequence.
        @param propertyName is int property for storing number.
    */
    public HubAutoSequence(Hub hub, String propertyName, int startNumber) {
        this(hub,propertyName,startNumber, false, false);
    }
    
    /**
        Create a new HubAutoSequence.
        @param propertyName is int property for storing number.
        @param bKeepSeq, if false then seq numbers are not updated when an object is removed        
    */
    public HubAutoSequence(Hub hub, String propertyName, int startNumber, boolean bKeepSeq) {
        this(hub,propertyName,startNumber, bKeepSeq, false);
    }

    /**
     * 
     * @param hub
     * @param propertyName
     * @param startNumber
     * @param bKeepSeq
     * @param bServerSideOnly this is used by Hub.setAutoSequence(...) so that the server will control the seq property and
     * send CS messages to clients.  If true, then the property changes (for seq prop) will need to be sent to clients.
     */
    public HubAutoSequence(Hub hub, String propertyName, int startNumber, boolean bKeepSeq, boolean bServerSideOnly) {
        if (bServerSideOnly && !HubCSDelegate.isServer(hub)) {
            LOG.warning("bServerSideOnly should be false, since this is not the server");
        }
        this.startNumber = startNumber;
        this.bKeepSeq = bKeepSeq;
        this.bServerSideOnly = bServerSideOnly;
        setHub(hub);
        setPropertyName(propertyName);
    }
    

    /**
        Create a new HubAutoSequence.
        @param propertyName is int property for storing number.
    */
    public HubAutoSequence(Hub hub, String propertyName) {
        setHub(hub);
        setPropertyName(propertyName);
    }

    
    
    /** Set the starting number to be used for first object. default is "0". */
    public int getStartNumber() {
        return startNumber;
    }
    /** Set the starting number to be used for first object. default is "0". */
    public void setStartNumber(int i) {
        startNumber = i;
        setup();
    }
    
    public Hub getHub() {
        return hub;
    }
    
    
    public void setHub(Hub hub) {
        if (this.hub != null) {
            this.hub.removeHubListener(this);
            autoSequenceHubListenerCount--;
        }
        this.hub = hub;
        if (hub != null) {
            hub.addHubListener(this);
            autoSequenceHubListenerCount++;
        }
        this.propertySetMethod = null;
        setup();
    }

    /** 
        Number property in object that is used to keep track of the order of the object within the hub.
        The hub will set the value based on the objects position within the Hub.  
        <p>
        Note: the object is not automatically saved. 
    */
    public String getPropertyName() {
        return propertyName;
    }
    /** @see getPropertyName */
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
        this.propertySetMethod = null;
        setup();
    }
    
    protected void setup() {    
        if (propertyName == null || hub == null) return;

        Class c = hub.getObjectClass();
        if (c == null) return;
        
        Method met = OAObjectInfoDelegate.getMethod(c, "set" + propertyName);
        //was: Method met = OAReflect.getMethod(c, "set"+propertyName);
        if (met == null) {
            throw new RuntimeException("setter method not found for property "+propertyName+", class="+c);
        }

        Class[] classes = met.getParameterTypes();
        if (classes == null || classes.length != 1) {
            throw new RuntimeException("Property "+propertyName+" must accept a numeric parameter");
        }
        c = classes[0];
        if (!c.equals(int.class) && !c.equals(long.class) && !c.equals(char.class)) {
            throw new RuntimeException("Property "+propertyName+" must accept a numeric parameter");
        }
        propertySetMethod = met;
        resequence(0);
    }
        
    private final AtomicInteger aiResequenceCnt = new AtomicInteger();  // used instead of synchronization
    
    public void resequence() {
        resequence(0);
    }

    private final static ConcurrentHashMap<Object, Object> hmUpdateSeq = new ConcurrentHashMap<>();
    
    protected void resequence(int startPos) {
        if (hub.isDeletingAll()) return;
        synchronized (hmUpdateSeq) {
            for (int i=0; ;i++) {
                if (hmUpdateSeq.get(this) == null || i>3) {
                    hmUpdateSeq.put(this, this);
                    break;
                }
                try {
                    hmUpdateSeq.wait(50);
                }
                catch (Exception e) {}
            }
        }

        boolean b = false;
        try {
            if (bServerSideOnly) {
                b = OARemoteThreadDelegate.sendMessages(true); 
            }
            _resequence(startPos);
        }
        finally {
            if (bServerSideOnly) {
                OARemoteThreadDelegate.sendMessages(b); 
            }
            synchronized (hmUpdateSeq) {
                hmUpdateSeq.remove(this);
                hmUpdateSeq.notifyAll();
            }
        }
    }
    
    private void _resequence(int startPos) {
        startPos = 0; // since deletes dont reseq and can leave gaps
        int cnt = aiResequenceCnt.incrementAndGet();
        int x = hub.getSize();  // only seq loaded objects
        for (int i=startPos; i<x; i++) {
            Object obj = hub.elementAt(i);
            if (obj == null) break;
            if (cnt != aiResequenceCnt.get()) break;
            
            // if this is ClientThread then need to send to other clients
            try {
                propertySetMethod.invoke(obj, new Object[] { Integer.valueOf(i+startNumber) });
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    /** HubListener interface method, used to listen to changes to Hub and update sequence numbers. */
    public @Override void afterInsert(HubEvent e) {
        int pos = e.getPos();
        resequence(pos);
    }
    /** HubListener interface method, used to listen to changes to Hub and update sequence numbers. */
    public @Override void afterAdd(HubEvent e) {
        int pos = e.getPos();
        resequence(pos);
    }
    /** HubListener interface method, used to listen to changes to Hub and update sequence numbers. */
    public @Override void afterRemove(HubEvent e) {
        if (bKeepSeq) {
            int pos = e.getPos();
            resequence(pos);
        }
    }
    /** HubListener interface method, used to listen to changes to Hub and update sequence numbers. */
    public @Override void afterMove(HubEvent e) {
        resequence(0);
    }
    /** HubListener interface method, used to listen to changes to Hub and update sequence numbers. */
    public @Override void onNewList(HubEvent e) {
        resequence(0);
    }
    /** HubListener interface method, used to listen to changes to Hub and update sequence numbers. */
    public @Override void afterSort(HubEvent e) {
        resequence(0);
    }
}

