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
package com.viaoa.hub;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.lang.reflect.*;

import com.viaoa.graph.OAGraphImpl;
import com.viaoa.graph.service.object.OAObjectInfoService;
import com.viaoa.object.*;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadImpl;
import com.viaoa.runtime.thread.OARemoteThreadService;

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

    /**
     * Global counter tracking the number of active HubAutoSequence listeners.
     * Useful for debugging and monitoring system-wide sequencing activity.
     */
    public static int autoSequenceHubListenerCount;
    
    /**
     * The Hub whose objects will receive automatically maintained sequence
     * values. A HubListener is attached to this Hub when sequencing is enabled.
     */
    protected Hub hub;

    /**
     * Name of the numeric OAObject property that stores the sequence value.
     * The corresponding setter method is resolved through reflection.
     */
    protected String propertyName;

    /**
     * Cached setter method for the sequence property. Initialized during setup
     * and used to update sequence numbers on objects efficiently through
     * reflection.
     */
    protected transient Method propertySetMethod;

    /**
     * Starting sequence number applied to the first object in the Hub.
     * Subsequent objects receive incremented values when resequenced.
     */
    protected int startNumber;

    /**
     * Indicates whether sequence numbers should remain contiguous after object
     * removal. When true, deletions trigger resequencing to close gaps.
     */
    protected boolean bKeepSeq;

    /**
     * When true, sequence updates are performed exclusively on the server and
     * pushed to clients using Hub messaging, reducing client-side overhead.
     */
    protected boolean bServerSideOnly;
    
    /**
     * Default constructor. The hub and property name must be set before this
     * instance becomes active for sequencing.
     */
    public HubAutoSequence() {
    }

    /**
     * Ensures cleanup by calling {@link #close()} before finalization.
     *
     * @throws Throwable if superclass finalization throws an exception
     */
    protected void finalize() throws Throwable {
    	close();
        super.finalize();
    }

    /**
     * Closes this HubAutoSequence by detaching it from the current hub.  
     * Removes its HubListener and clears internal references.
     */
    public void close() {
        if (hub != null) setHub(null);
    }
    
    /**
     * Constructs a HubAutoSequence using the given Hub, property name, and start
     * number. Sequence numbers are recomputed when objects are added, inserted, or
     * removed.
     *
     * @param hub          the hub whose objects will be sequenced
     * @param propertyName the numeric property to update
     * @param startNumber  the starting sequence number applied to the first object
     */
    public HubAutoSequence(Hub hub, String propertyName, int startNumber) {
        this(hub,propertyName,startNumber, false, false);
    }
    
    /**
     * Constructs a HubAutoSequence with control over whether sequence values are
     * kept contiguous after removals.
     *
     * @param hub          the hub whose objects will be sequenced
     * @param propertyName the numeric property to update
     * @param startNumber  the starting sequence number
     * @param bKeepSeq     whether sequence numbers should remain contiguous after removals
     */
    public HubAutoSequence(Hub hub, String propertyName, int startNumber, boolean bKeepSeq) {
        this(hub,propertyName,startNumber, bKeepSeq, false);
    }

    /**
     * Constructs a HubAutoSequence with full configuration options, including
     * server-side-only sequencing. When server-side-only is enabled, sequence
     * updates are performed on the server and pushed to clients.
     *
     * @param hub             the hub whose objects will be sequenced
     * @param propertyName    the numeric property to update
     * @param startNumber      the starting sequence number
     * @param bKeepSeq        whether sequence numbers remain contiguous after removals
     * @param bServerSideOnly whether sequence updates are controlled exclusively by the server
     */
    public HubAutoSequence(Hub hub, String propertyName, int startNumber, boolean bKeepSeq, boolean bServerSideOnly) {
		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(this.hub);
        if (bServerSideOnly && !og.getHubService().getHubCSService().isServer(hub)) {
            LOG.warning("bServerSideOnly should be false, since this is not the server");
        }
        this.startNumber = startNumber;
        this.bKeepSeq = bKeepSeq;
        this.bServerSideOnly = bServerSideOnly;
        setHub(hub);
        setPropertyName(propertyName);
    }
    

    /**
     * Constructs a HubAutoSequence using default start number (0) and default
     * sequence behavior.
     *
     * @param hub          the hub whose objects will be sequenced
     * @param propertyName the numeric property to update
     */
    public HubAutoSequence(Hub hub, String propertyName) {
        setHub(hub);
        setPropertyName(propertyName);
    }
    
    /**
     * Returns the starting sequence number assigned to the first object in the hub.
     *
     * @return the starting sequence number
     */
    public int getStartNumber() {
        return startNumber;
    }

    /**
     * Sets the starting sequence number and recalculates the sequence values.
     *
     * @param i the new starting sequence number
     */
    public void setStartNumber(int i) {
        startNumber = i;
        setup();
    }
    
    /**
     * Sets the hub to be sequenced. Removes any existing listener and attaches
     * this instance as a HubListener to the new hub. Triggers setup of the
     * sequence property.
     *
     * @param hub the hub to attach to
     */
    public Hub getHub() {
        return hub;
    }
    
    /**
     * Sets the hub to be sequenced. Removes any existing listener and attaches
     * this instance as a HubListener to the new hub. Triggers setup of the
     * sequence property.
     *
     * @param hub the hub to attach to
     */
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
     * Returns the name of the property used to store sequence numbers.
     *
     * @return the sequence property name
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Sets the property used to store sequence numbers and triggers setup to
     * locate the corresponding setter method.
     *
     * @param propertyName the name of the numeric property to update
     */
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
        this.propertySetMethod = null;
        setup();
    }
    
    /**
     * Initializes the setter method for the sequence property and triggers
     * resequencing. Validates that the property exists and accepts a numeric
     * parameter.
     */
    protected void setup() {    
        if (propertyName == null || hub == null) return;

        Class c = hub.getObjectClass();
        if (c == null) return;
        
		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(c);
		final OAObjectInfoService srvcObjectInfo = og.getOAObjectService().getOAObjectInfoService();
        Method met = srvcObjectInfo.getMethod(c, "set" + propertyName);
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
        
    /**
     * Atomic counter used to detect interleaving or overlapping resequence
     * operations. Ensures resequencing is performed consistently even under
     * concurrent Hub events.
     */
    private final AtomicInteger aiResequenceCnt = new AtomicInteger();  // used instead of synchronization
    
    /**
     * Recomputes sequence values for all objects in the hub, beginning at the
     * default starting position.
     */
    public void resequence() {
        resequence(0);
    }

    private final static ConcurrentHashMap<Object, Object> hmUpdateSeq = new ConcurrentHashMap<>();
    
    /**
     * Recomputes sequence values beginning at the specified starting position.
     * Uses a shared lock to avoid concurrent resequence operations and optionally
     * aggregates updates when running server-side-only.
     *
     * @param startPos the position from which to begin resequencing
     */
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

    	final OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();  
        try {
            if (bServerSideOnly) {
                srvcOARemoteThread.sendMessages(true); 
            }
            _resequence(startPos);
        }
        finally {
            if (bServerSideOnly) {
            	srvcOARemoteThread.sendMessages(false); 
            }
            synchronized (hmUpdateSeq) {
                hmUpdateSeq.remove(this);
                hmUpdateSeq.notifyAll();
            }
        }
    }
    
    /**
     * Internal implementation of the resequence operation. Assigns sequence
     * numbers to each loaded object using the configured start number. Uses an
     * atomic counter to avoid interleaving operations.
     *
     * @param startPos the position from which to begin resequencing
     */
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
    
    /**
     * HubListener callback invoked after an insert event. Resequences objects
     * beginning at the inserted position.
     *
     * @param e the HubEvent describing the insert
     */
    public @Override void afterInsert(HubEvent e) {
        int pos = e.getPos();
        resequence(pos);
    }
    
    /**
     * HubListener callback invoked after an add event. Resequences objects
     * beginning at the added position.
     *
     * @param e the HubEvent describing the add
     */
    public @Override void afterAdd(HubEvent e) {
        int pos = e.getPos();
        resequence(pos);
    }

    /**
     * HubListener callback invoked after a remove event. If keep-sequence mode
     * is enabled, resequences objects beginning at the removed position.
     *
     * @param e the HubEvent describing the removal
     */
    public @Override void afterRemove(HubEvent e) {
        if (bKeepSeq) {
            int pos = e.getPos();
            resequence(pos);
        }
    }

    /**
     * HubListener callback invoked after a move event. Resequences the entire hub.
     *
     * @param e the HubEvent describing the move
     */
    public @Override void afterMove(HubEvent e) {
        resequence(0);
    }

    /**
     * HubListener callback invoked when the hub receives a new list event.
     * Resequences all objects from the beginning.
     *
     * @param e the HubEvent associated with the new list
     */
    public @Override void onNewList(HubEvent e) {
        resequence(0);
    }

    /**
     * HubListener callback invoked after the hub is sorted.  
     * Resequences all objects to ensure sequence numbers match the new order.
     *
     * @param e the HubEvent describing the sort
     */
    public @Override void afterSort(HubEvent e) {
        resequence(0);
    }
}

