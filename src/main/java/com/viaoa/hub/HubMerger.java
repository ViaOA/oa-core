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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.viaoa.graph.OAGraph;
import com.viaoa.graph.OAGraphImpl;
import com.viaoa.graph.service.hub.HubAddRemoveService;
import com.viaoa.graph.service.object.OAObjectInfoService;
import com.viaoa.object.OACascade;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAPerformance;
import com.viaoa.object.OASiblingHelper;
import com.viaoa.object.OAThreadLocal;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadImpl;
import com.viaoa.runtime.thread.OARemoteThreadService;
import com.viaoa.runtime.thread.OAThreadLocalService;
import com.viaoa.util.OAPropertyPath;

/**
 * Dynamically merges one or more {@link Hub}s into a combined, live-synchronized
 * projection based on a {@link com.viaoa.util.OAPropertyPath}.
 * <p>
 * Each root object in the {@code hubRoot} is traversed along the property path to
 * produce a flattened set of destination objects in {@code hubCombined}. Changes in
 * any source Hub (add/remove/AO/refresh) are reflected in the combined Hub.
 *
 * <p><b>Use Cases</b>:
 * <ul>
 *   <li>Aggregate all detail objects across a collection of master objects.</li>
 *   <li>Build derived Hubs for recursive or nested one-to-many structures.</li>
 *   <li>Share active-object state across related Hubs when {@code bShareActiveObject} is true.</li>
 * </ul>
 *
 * <p><b>Implementation Details</b>:
 * <ul>
 *   <li>Parses and validates {@link OAPropertyPath} to build a linked chain of {@code Node} objects.</li>
 *   <li>Maintains membership through {@code Data} trees that react to Hub events.</li>
 *   <li>Uses {@link java.util.concurrent.locks.ReentrantReadWriteLock} for atomic membership updates.</li>
 *   <li>Supports background initialization and sibling tracking via {@link com.viaoa.object.OASiblingHelper}.</li>
 * </ul>
 *
 * <p>Subclasses may override {@code beforeRemoveRealHub}, {@code afterAddRealHub}, etc.,
 * to intercept events prior to or following updates to the combined Hub.
 */
public class HubMerger<F extends OAObject, T extends OAObject> {
    private static Logger LOG = Logger.getLogger(HubMerger.class.getName());
    
    /**
     * Debug flag that can be enabled to assist with tracing internal behavior.
     */
    public boolean DEBUG;

    /**
     * Global flag controlling whether extensive verification logic is executed.
     */
    public static final boolean bVERIFY = false;

    /* Programming notes: Node: defines the straight path of nodes. Each node has a child node. Data:
     * used to create a tree of nodes for objects in the hubs. If the property is a type=One then the
     * actual Node will have a temp Hub that is used to store the unique values. Each data has an array
     * of children Data. A new Data is created for each object in the parent Data, until the child.node
     * is null. 20120522 add support for recursive objects in the path */

    /**
     * Root node of the property-path traversal model. Represents the first link
     * in the chain used to expand objects from the root Hub.
     */
    private Node nodeRoot; // first node from propertyPath

    /**
     * Root Data object representing the starting point of merged membership
     * used to populate the combined Hub.
     */
    private Data dataRoot; // node used for the root Hub

    /**
     * The property path used to traverse from objects in the root Hub to
     * destination objects in the combined Hub.
     */
    String propertyPath; // property path

    /**
     * Destination Hub that receives merged objects as defined by the property path.
     */
    Hub hubCombined; // Hub that stores the results

    /**
     * The root Hub whose objects serve as the starting points for property-path expansion.
     */
    Hub hubRoot; // main hub used as the first hub.

    /**
     * Indicates whether the final-level Hub in the property path should be shared
     * with the combinedHub instead of copying members into it.
     */
    boolean bShareEndHub; // if true, then hubCombined can be shared with the currently found "single"
                            // Hub
    /**
     * When sharing is enabled, indicates whether the Active Object should also be shared
     * from the terminal Hub.
     */
    boolean bShareActiveObject; // if bShareEndHub, then this will set the sharedHub as sharing the AO

    /**
     * Controls whether all objects in hubRoot are used (true) or only its active object (false).
     */
    boolean bUseAll; // if false, then only use AO in the rootHub, otherwise all objects will be used.

    /**
     * Flag indicating whether a refresh should occur whenever the rootHub's AO changes.
     */
    boolean bRefreshOnActiveObjectChange;

    /**
     * Internal override used during events to suppress isUsed() logic temporarily.
     */
    boolean bIgnoreIsUsedFlag; // flag to have isUsed() return false;

    /**
     * Indicates whether the HubMerger is active. When disabled, no updates are processed.
     */
    private boolean bEnabled = true;

    /**
     * Flag to track whether the property path includes a recursive link.
     */
    private boolean bIsRecusive;

    /**
     * When true, objects from the rootHub itself are included in the merged results.
     */
    private boolean bIncludeRootHub;

    /**
     * Controls whether HubMerger uses a background thread for onNewList processing.
     */
    private boolean bUseBackgroundThread;

    /**
     * Read/write lock used to synchronize access to mutable Data tree structures.
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Running count of HubListener instances created internally for this merger.
     * Intended for diagnostics and testing only.
     */
    public int TotalHubListeners; // for testing only

    /**
     * Global counter tracking the total number of HubListeners created across
     * all HubMerger instances.
     */
    public static final AtomicInteger aiHubListenerCount = new AtomicInteger(); // number of HubListeners used by all HubMerger

    /**
     * Indicates whether the HubMerger is running exclusively on the server side.
     * <p>
     * When set to {@code true}, HubMerger operations that modify Hubs will publish
     * events to connected clients even if the change originates from an OAClientThread.
     * This ensures that client applications receive updates despite the merger being
     * created and managed only on the server.
     */
    private boolean bServerSideOnly;

    // used to run onNewList in another thread that can be cancelled
    /**
     * Counter used to track the number of onNewList operations initiated by this
     * HubMerger, allowing background threads to detect when a newer list load
     * request supersedes a previous one.
     */
    private final AtomicInteger aiNewList = new AtomicInteger();

    /**
     * Counter indicating when the combined Hub is in a loading state. Used to
     * temporarily suppress or adjust behavior during list-loading operations.
     */
    private final AtomicInteger aiLoadingCombinedHub = new AtomicInteger();

    /**
     * Lazily-initialized helper used to track sibling relationships while navigating
     * recursive property paths. Ensures correct traversal when dealing with repeating
     * or cyclic structures.
     */
    private OASiblingHelper siblingHelper;

    /**
     * Unique identifier for this HubMerger instance, assigned at construction using a
     * global counter. Used for diagnostics and logging.
     */
    private final int id;

    /**
     * Global incremental counter used to assign unique IDs to all HubMerger
     * instances upon construction.
     */
    private static final AtomicInteger aiId = new AtomicInteger();

    /**
     * Creates a HubMerger for a root Hub and property path using default settings:
     * no sharing of the active object and using only the active object of the root.
     *
     * @param hubRoot the root Hub used to traverse objects
     * @param hubCombinedObjects the destination Hub that receives merged objects
     * @param propertyPath the property path for traversal
     */
    public HubMerger(Hub<F> hubRoot, Hub<T> hubCombinedObjects, String propertyPath) {
        this(hubRoot, hubCombinedObjects, propertyPath, false, null, true, false, false);
    }

    /**
     * Creates a HubMerger specifying whether all objects in the root Hub should be
     * used instead of only the active object.
     *
     * @param hubRoot the root Hub
     * @param hubCombinedObjects the destination Hub
     * @param propertyPath the property path for traversal
     * @param bUseAll true to use all objects in the root Hub; false to use only its AO
     */
    public HubMerger(Hub<F> hubRoot, Hub<T> hubCombinedObjects, String propertyPath, boolean bUseAll) {
        this(hubRoot, hubCombinedObjects, propertyPath, false, null, bUseAll, false, false);
    }

    /**
     * Creates a HubMerger specifying whether to share the active object from the
     * discovered terminal Hub and whether to use all objects from the root Hub.
     *
     * @param hubRoot the root Hub
     * @param hubCombinedObjects the destination Hub
     * @param propertyPath the property path for traversal
     * @param bShareActiveObject true to share the AO of the last Hub
     * @param bUseAll true to use all objects in the root Hub
     */
    public HubMerger(Hub<F> hubRoot, Hub<T> hubCombinedObjects, String propertyPath, boolean bShareActiveObject, boolean bUseAll) {
        this(hubRoot, hubCombinedObjects, propertyPath, bShareActiveObject, null, bUseAll, false, false);
    }

    /**
     * Creates a HubMerger specifying sharing behavior, select-order filtering, and
     * whether to process all objects in the root Hub.
     *
     * @param hubRoot the root Hub
     * @param hubCombinedObjects the destination Hub
     * @param propertyPath the path used to traverse from root to target objects
     * @param bShareActiveObject true if the AO from the final Hub is shared
     * @param selectOrder optional ordering for filtering operations
     * @param bUseAll true to include all root Hub objects
     */
    public HubMerger(Hub<F> hubRoot, Hub<T> hubCombinedObjects, String propertyPath, boolean bShareActiveObject, String selectOrder,
            boolean bUseAll) {
        this(hubRoot, hubCombinedObjects, propertyPath, bShareActiveObject, selectOrder, bUseAll, false, false);
    }

    /**
     * Enables or disables the background-thread mode for handling onNewList events.
     *
     * @param b true to process list loading in a background thread; false otherwise
     */
    public void setUseBackgroundThread(boolean b) {
        bUseBackgroundThread = b;
    }

    /**
     * Returns whether this HubMerger processes onNewList events using a background
     * thread.
     *
     * @return true if background processing is enabled; false otherwise
     */
    public boolean getUseBackgroundThread() {
        return bUseBackgroundThread;
    }

    /**
     * Full constructor allowing configuration of sharing, ordering, inclusion of
     * root objects, and background thread usage.
     *
     * @param hubRoot the root Hub
     * @param hubCombinedObjects the destination Hub
     * @param propertyPath the traversal path
     * @param bShareActiveObject true if the AO is shared from the terminal Hub
     * @param selectOrder optional ordering expression for filtering
     * @param bUseAll true to use all objects in the root Hub
     * @param bIncludeRootHub true to include root Hub objects in the result
     * @param bUseBackgroundThread true to enable background updates
     */
    public HubMerger(Hub<F> hubRoot, Hub<T> hubCombinedObjects,
            String propertyPath, boolean bShareActiveObject, String selectOrder,
            boolean bUseAll, boolean bIncludeRootHub, boolean bUseBackgroundThread) {
        id = aiId.getAndIncrement();
        if (hubRoot == null) {
            throw new IllegalArgumentException("Root hub can not be null");
        }
        LOG.fine("hubRoot=" + hubRoot.getObjectClass().getSimpleName() + ", propertyPath=" + propertyPath);

        if (hubCombinedObjects == null) {
            // 20150720 allow combinedHub to be null
            //throw new IllegalArgumentException("Combined hub can not be null");
        }
        setUseBackgroundThread(bUseBackgroundThread);
        init(hubRoot, hubCombinedObjects, propertyPath, bShareActiveObject, selectOrder, bUseAll, bIncludeRootHub);
    }

    /**
     * Constructor providing configuration for active-object sharing, using all root
     * Hub objects, and including the root Hub in the merged results.
     *
     * @param hubRoot the root Hub
     * @param hubCombinedObjects the destination Hub
     * @param propertyPath the traversal path
     * @param bShareActiveObject true to share the AO of the terminal Hub
     * @param bUseAll true to use all root Hub objects
     * @param bIncludeRootHub true to include root Hub objects
     */
    public HubMerger(Hub<F> hubRoot, Hub<T> hubCombinedObjects, String propertyPath, boolean bShareActiveObject, boolean bUseAll,
            boolean bIncludeRootHub) {
        this(hubRoot, hubCombinedObjects, propertyPath, bShareActiveObject, null, bUseAll, bIncludeRootHub, false);
    }

    /**
     * Flag indicating whether this HubMerger was created using the constructor that
     * accepts a single object instead of a Hub. Used to modify initialization logic.
     */
    private boolean bCreatedFromOneObject;

    /**
     * Creates a HubMerger initialized from a single source object rather than a
     * root Hub. A temporary Hub is created to wrap the object.
     *
     * @param obj the starting object for traversal
     * @param hubCombinedObjects the destination Hub
     * @param propertyPath the traversal path
     */
    public HubMerger(F obj, Hub<T> hubCombinedObjects, String propertyPath) {
        id = aiId.getAndIncrement();
        bCreatedFromOneObject = true;
        Hub h = new Hub(obj.getClass());
        h.add(obj);
        h.setPos(0);
        init(h, hubCombinedObjects, propertyPath, false, null, true, false);
    }

    /**
     * Returns the number of objects included in the merged result according to the
     * Data tree.
     *
     * @return total object count, or 0 if uninitialized
     */
    public int getObjectCount() {
        if (dataRoot == null) {
            return 0;
        }
        return dataRoot.getObjectCount();
    }

    /**
     * Sets whether this merger is server-side only. When true, events generated by
     * this merger will be published to clients even when triggered by an OAClientThread.
     *
     * @param b true to operate in server-side-only mode
     */
    public void setServerSideOnly(boolean b) {
        bServerSideOnly = b;
    }

    /**
     * A no-operation HubListener attached to the combined Hub solely to mark that a
     * HubMerger is associated with it. Used internally for bookkeeping.
     */
    private HubListener hlCombinedNoOp;

    /**
     * Internal initializer invoked by constructors. Creates listeners, configures
     * flags, prepares the sibling helper, performs initial loading, and builds the
     * node/data model used for the merger.
     *
     * @param hubRoot the root Hub
     * @param hubCombinedObjects the destination Hub
     * @param propertyPath the traversal path
     * @param bShareActiveObject whether the terminal Hub AO is shared
     * @param selectOrder optional ordering expression
     * @param bUseAll whether to include all root Hub objects
     * @param bIncludeRootHub whether to include root Hub members in results
     */
    private void init(Hub hubRoot, Hub hubCombinedObjects, String propertyPath, boolean bShareActiveObject, String selectOrder,
            boolean bUseAll, boolean bIncludeRootHub) {

        this.hubRoot = hubRoot;
        this.hubCombined = hubCombinedObjects;
        this.propertyPath = propertyPath;
        this.bShareActiveObject = bShareActiveObject;
        this.bUseAll = bUseAll;
        this.bIncludeRootHub = bIncludeRootHub;

        if (hubCombined != null) {
            hlCombinedNoOp = new HubListenerAdapter(this, "HubMerger", "hubMerger, hubRoot=" + hubRoot + ", pp=" + propertyPath) {
                // no-op, just want to know that hubCombined uses a HubMerger
            };
            hubCombined.addHubListener(hlCombinedNoOp);
        }

        long ts = System.currentTimeMillis();

		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
        
        final OASiblingHelper sh = getSiblingHelper();
        final boolean bx = srvcOAThreadLocal.addSiblingHelper(sh);
        final boolean bz = bServerSideOnly;
        try {
            // 20120624 hubCombined could be a detail hub.
            srvcOAThreadLocal.setSuppressCSMessages(true);
            if (!bz) {
                aiLoadingCombinedHub.incrementAndGet();
            }
            _init();
        } finally {
            srvcOAThreadLocal.setSuppressCSMessages(false);
            if (!bz) {
                aiLoadingCombinedHub.decrementAndGet();
            }
            if (bx) {
                srvcOAThreadLocal.removeSiblingHelper(sh);
            }
        }
        ts = System.currentTimeMillis() - ts;

        String s = ("HM." + id + ") new HubMerger hub=" + hubRoot + ", propertyPath=" + propertyPath + ", useAll=" + bUseAll
                + ", useBackgroundThread=" + getUseBackgroundThread());
        s += ", combinedHub=" + hubCombined;
        s += ", time=" + ts + "ms";

        if (!getUseBackgroundThread()) {
            if (bUseAll) {
                int x = hubRoot.size();
                if (x > 100) {
                    if (x > 350 || propertyPath.indexOf(".") > 0) {
                        s += ", ALERT (large root hub)";
                    }
                }
            }
            if (hubCombined != null && hubCombined.getSize() > 250) {
                s += ", ALERT (large result hub)";
            }
            if (ts > 1000) {
                s += ", ALERT (took over 1second)";
            }
        }

        if ((hubCombined != null && hubCombined.size() > 2000) || ts > 2500) {
            // Exception e = new Exception("HubMerger performance concern");
            OAPerformance.LOG.log(Level.FINE, s);
        }
        OAPerformance.LOG.finer(s);
        LOG.fine(s);
    }

    /**
     * Performs low-level initialization by creating the Node chain for the property
     * path and establishing the root Data node used to drive the merge process.
     */
    private void _init() {
        createNodes(); // this will create nodeRoot
        this.dataRoot = new Data(null, nodeRoot, null, hubRoot);
        nodeRoot.data = dataRoot;
    }

    /**
     * Lazily creates and returns the OASiblingHelper used for tracking sibling
     * relationships during recursive traversal of property paths.
     *
     * @return the associated OASiblingHelper instance
     */
    public OASiblingHelper getSiblingHelper() {
        if (siblingHelper == null) {
            siblingHelper = new OASiblingHelper<>(this.hubRoot);
            siblingHelper.add(this.propertyPath);
        }
        return siblingHelper;
    }

    /**
     * Returns the root Hub whose objects serve as the starting point for traversal.
     *
     * @return the root Hub
     */
    public Hub getRootHub() {
        return this.hubRoot;
    }

    /**
     * Returns the Hub that receives merged/combined objects produced by this
     * HubMerger.
     *
     * @return the combined Hub
     */
    public Hub getCombinedHub() {
        return this.hubCombined;
    }

    /**
     * Enables or disables the HubMerger. When re-enabled, the combined Hub is rebuilt
     * and appropriate events are fired to synchronize its state.
     *
     * @param b true to enable; false to disable
     */
    public void setEnabled(boolean b) {
        if (this.bEnabled == b) {
            return;
        }
        this.bEnabled = b;
        if (bEnabled) {
			final OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();  
            try {
                if (bServerSideOnly) {
                    srvcOARemoteThread.sendMessages(true);
                }
                if (!bShareEndHub && hubCombined != null) {
                    hubCombined.clear();
                }
                dataRoot.onNewList(null);
                dataRoot.afterChangeActiveObject(null);
            } finally {
                if (bServerSideOnly) {
                    srvcOARemoteThread.sendMessages(false);
                }
            }
        }
    }

    /**
     * Returns whether the HubMerger is currently active.
     *
     * @return true if enabled; false otherwise
     */
    public boolean getEnabled() {
        return this.bEnabled;
    }

    /**
     * Returns the property path used by this HubMerger.
     *
     * @return the property path
     */
    public String getPath() {
        return this.propertyPath;
    }

    /**
     * Optional textual description of the HubMerger, supplied by callers for logging
     * or informational purposes.
     */
    private String description;

    /**
     * Assigns a descriptive label for this HubMerger.
     *
     * @param desc the description text
     */
    public void setDescription(String desc) {
        description = desc;
    }

    /**
     * Returns the description assigned to this HubMerger.
     *
     * @return the description, or null if not set
     */
    public String getDescription() {
        return description;
    }

    /*
     * Note: if multiple threads are making changes that affect the node data, then errors could show up.
     */
    /**
     * Performs internal consistency checks on the Node/Data model when the global
     * verification flag is enabled. Logs warnings for structural mismatches or
     * unexpected relationships.
     */
    public void verify() {
        // qqqqq todo: needs to verify recursive data
        if (!bVERIFY) {
            return;
        }
        if (!bEnabled) {
            return;
        }
        // XOG.finest("verifing nodes");
        // Nodes
        for (Node node = nodeRoot; node != null; node = node.child) {
            if (node.clazz == null) {
                LOG.warning("node.clazz == null");
            }
            if (node.liFromParentToChild == null) {
                if (node != nodeRoot) {
                    LOG.warning("liFromParentToChild == null for Node:" + node.property);
                }
            } else if (node.liFromParentToChild.getType() == OALinkInfo.ONE) {
                if (node.data == null) {
                    if (bUseAll) {
                        // this might not be a problem, since the properties could be null
                        // LOG.warning("Node: "+node.property+" is used for type=One but data is null");
                    }
                } else if (node.data.parentObject != null) {
                    LOG.warning("Node: " + node.property + " is used for type=One and parentObject != null");
                }
            } else { // Many
                if (node.data != null) {
                    LOG.warning("Node: " + node.property + " is type=Many data != null");
                }
            }
        }

        // verify hubCombinued objects are used
        if (!bShareEndHub && hubCombined != null) {
            for (int i = 0;; i++) {
                Object obj = hubCombined.getAt(i);
                if (obj == null) {
                    break;
                }
                if (!isUsed(obj)) {
                    LOG.warning("Object in hubCombined is not used");
                }
            }
        }

        // XOG.finest("verifying data");
        dataRoot.verify();
        for (Node node = nodeRoot; node != null; node = node.child) {
            if (node.data != null) {
                node.data.verify();
            }
        }
        // XOG.finest("verify complete");
    }

    /**
     * Determines whether the given object is part of the merged result. Delegates to
     * the internal isUsed method using the root Data node.
     *
     * @param objFind the object to check
     * @return true if the object is included; false otherwise
     */
    private boolean isUsed(Object objFind) {
        if (bIgnoreIsUsedFlag) {
            return false;
        }
        if (!bEnabled) {
            return false;
        }
        boolean b = isUsed(objFind, null);
        return b;
    }

    /**
     * Internal method that checks whether the given object is included in the merged
     * structure relative to a specific Node in the traversal chain.
     *
     * @param objFind the object to locate
     * @param nodeFind optional Node restricting the search
     * @return true if the object is used; false otherwise
     */
    private boolean isUsed(Object objFind, Node nodeFind) {
        if (bIgnoreIsUsedFlag) {
            return false;
        }
        if (!bEnabled) {
            return false;
        }
        // go back to dataRoot, or closest type=One
        Data dataFnd = dataRoot;
        for (Node n = nodeRoot; n != nodeFind; n = n.child) {
            if (n.liFromParentToChild != null && n.liFromParentToChild.getType() == OALinkInfo.ONE && n.data != null) {
                dataFnd = n.data;
            }
        }
        if (dataFnd == null) {
            return false;
        }
        boolean b = dataFnd._isUsed(objFind, nodeFind);
        return b;
    }

    /**
     * Hook method invoked before an object is removed from the real (source) Hub
     * rather than from the combined Hub. Subclasses may override to intercept events.
     *
     * @param e the HubEvent describing the removal
     */
    protected void beforeRemoveRealHub(HubEvent<T> e) {
    }

    /**
     * Hook method invoked after an object is removed from the real (source) Hub.
     * Subclasses may override to implement custom behavior.
     *
     * @param e the HubEvent for the removal
     */
    protected void afterRemoveRealHub(HubEvent<T> e) {
    }

    /**
     * Hook invoked before all objects are removed from the real (source) Hub.
     * Subclasses may override to intercept the event.
     *
     * @param e the HubEvent for the remove-all operation
     */
    protected void beforeRemoveAllRealHub(HubEvent<T> e) {
    }

    /**
     * Hook invoked after all objects have been removed from the real (source) Hub.
     *
     * @param e the HubEvent for the remove-all operation
     */
    protected void afterRemoveAllRealHub(HubEvent<T> e) {
    }

    /**
     * Hook invoked after an object is moved within the real Hub. Subclasses may
     * override to process reordering events.
     *
     * @param e the HubEvent describing the move
     */
    protected void afterMoveRealHub(HubEvent<T> e) {
    }

    /**
     * Hook invoked before an object is inserted into the real (source) Hub.
     *
     * @param e the HubEvent describing the insertion
     */
    protected void beforeInsertRealHub(HubEvent<T> e) {
    }

    /**
     * Hook invoked after an object is inserted into the real (source) Hub.
     *
     * @param e the HubEvent describing the insertion
     */
    protected void afterInsertRealHub(HubEvent<T> e) {
    }

    /**
     * Hook invoked before an object is added to the real (source) Hub.
     *
     * @param e the HubEvent describing the add
     */
    protected void beforeAddRealHub(HubEvent<T> e) {
    }

    /**
     * Hook invoked after an object is added to the real (source) Hub.
     *
     * @param e the HubEvent describing the add
     */
    protected void afterAddRealHub(HubEvent<T> e) {
    }

    /**
     * Hook invoked when the real (source) Hub generates an onNewList event. This
     * occurs when its underlying list is replaced. Subclasses may override.
     *
     * @param e the HubEvent describing the list replacement
     */
    protected void onNewListRealHub(HubEvent<T> e) {
    }

    /* //qqqqqq not sure if this is used // check to see if this, and Data.getChildrenCount() can be
     * removed public int getChildrenCount() { if (!bEnabled) return 0; int cnt = 0;
     *
     * Node node = nodeRoot; for ( ; node != null; node = node.child) { if (node.data != null) cnt +=
     * node.data.getChildrenCount(); } // this needs to consider what to do if recursive is included cnt
     * += dataRoot.getChildrenCount(); return cnt; } */
    
    
    /**
     * Builds the linked chain of Node objects defining the traversal model for the
     * property path. Validates link types, detects recursive structures, assigns
     * filter constructors, and ensures class compatibility with the combined Hub.
     */
    protected void createNodes() {
        bShareEndHub = !bUseAll;
        Class clazz = hubRoot.getObjectClass();
		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(clazz);

        // 20120809 using new OAPropertyPath
        OAPropertyPath oaPropPath = new OAPropertyPath(propertyPath);
        try {
            oaPropPath.setup(clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cant find property for PropertyPath=\"" + propertyPath + "\" starting with Class "
                    + hubRoot.getObjectClass().getName(), e);
        }
        if (oaPropPath.hasPrivateLink()) {
            throw new RuntimeException("property path has private link, pp=" + oaPropPath.getPropertyPath());
        }
        String[] pps = oaPropPath.getProperties();
        Method[] methods = oaPropPath.getMethods();
        Class[] classes = oaPropPath.getClasses();
        Constructor[] filterConstructors = oaPropPath.getFilterConstructors();

        Object[][] filterParamValues = oaPropPath.getFilterParamValues();
        int pos = 0;
        if (filterParamValues != null) {
            for (Object[] objs : filterParamValues) {
                if (objs != null) {
                    int i = 0;
                    for (Object obj : objs) {
                        if ("?".equals(obj)) {
                            //qqqqq if any param is "?" then need to have as HubMerger input value(s)
                            // if (filterInputValues == null || pos > filterInputValues.length - 1) throw new RuntimeExcepiton(...
                            // objs[i] = filterInputValues[pos++]; // this will need to be replacement value

                            throw new RuntimeException("propertyPath has filter with input param '?', which is not yet supported");
                        }
                    }
                }
            }
        }

        nodeRoot = new Node(null);
        nodeRoot.clazz = clazz;
        Node node = nodeRoot;
        boolean bLastWasMany = false;
        OALinkInfo lastLinkInfo = null; // 20131009

        for (int i = 0;; i++) {
			final OAObjectInfoService srvcObjectInfo = og.getOAObjectService().getOAObjectInfoService();
            OAObjectInfo oi = srvcObjectInfo.getOAObjectInfo(clazz);
            OALinkInfo recursiveLinkInfo = srvcObjectInfo.getRecursiveLinkInfo(oi, OALinkInfo.MANY);
            Node recursiveNode = null;

            // 20131009 check to see if link is recursive
            if (bLastWasMany && recursiveLinkInfo != null && lastLinkInfo != null && lastLinkInfo.getRecursive()) {
                // was: if (bLastWasMany && recursiveLinkInfo != null) {
                bIsRecusive = true;
                recursiveNode = new Node(null);
                recursiveNode.property = recursiveLinkInfo.getName();
                recursiveNode.liFromParentToChild = recursiveLinkInfo;
                recursiveNode.clazz = recursiveLinkInfo.getToClass();
                recursiveNode.recursiveChild = recursiveNode;
                node.recursiveChild = recursiveNode;
                bShareEndHub = false;
            }

            if (i == pps.length) {
                break;
            }
            String prop = pps[i];

            OALinkInfo linkInfo = srvcObjectInfo.getLinkInfo(oi, prop);
            if (linkInfo == null) {
                throw new IllegalArgumentException("Cant find " + prop + " for PropertyPath \"" + propertyPath + "\" starting with Class "
                        + hubRoot.getObjectClass().getName());
            }
            bLastWasMany = linkInfo.getType() == linkInfo.MANY;
            lastLinkInfo = linkInfo;

            if (bShareEndHub) {
                if (linkInfo.getType() == OALinkInfo.MANY) {
                    if (i < (pps.length - 1)) {
                        bShareEndHub = false; // only the last one can be many
                    }
                } else {
                    if (i == (pps.length - 1)) {
                        bShareEndHub = false; // the last one can must be a many
                    }
                }
            }

            Node node2 = new Node(node);
            node2.property = prop;
            node2.liFromParentToChild = linkInfo;

            clazz = classes[i];

            node2.clazz = clazz;
            node2.filterConstructor = filterConstructors[i];
            if (filterParamValues != null) {
                node2.filterParams = filterParamValues[i];
            }
            node.child = node2;
            node = node2;

            if (recursiveNode != null) {
                recursiveNode.child = node2;
            }
        }
        // verify that last property is same class as hubCombined
        if (hubCombined != null && hubCombined.getObjectClass() == null) {
            og.getHubService().setObjectClass(hubCombined, clazz);
        }
        if (hubCombined != null && !hubCombined.getObjectClass().equals(clazz)) {
            if (!clazz.equals(Hub.class)) {
                // if (!OAObject.class.equals(clazz)) { // 20120809 could be using generic type reference
                // (ex: OALeftJoin.A)
                throw new IllegalArgumentException("Classes do not match.  Property path \"" + propertyPath + "\" is for objects of Class "
                        + clazz.getName() + " and hubCombined is for objects of Class " + hubCombined.getObjectClass());
                // }
            }
        }
        if (bIncludeRootHub && hubCombined != null) {
            if (!hubRoot.getObjectClass().equals(clazz)) {
                throw new IllegalArgumentException("IncludeRootHub=true, and HubRoot class does not match.  Property path \"" + propertyPath
                        + "\" is for objects of Class " + clazz.getName() + " and hubCombined is for objects of Class "
                        + hubCombined.getObjectClass());
            }
        }
    }

    /**
     * Releases all Data and Node structures, removes listeners, and clears shared-
     * hub references. After calling this, the HubMerger becomes inactive.
     */
    public void close() {
        // LOG.finer("closing");
        if (nodeRoot == null) {
            return;
        }
        bIgnoreIsUsedFlag = true;
        dataRoot.close();
        Node node = nodeRoot;
        while (node != null) {
            if (dataRoot != node.data) {
                node.close();
            }
            node = node.child;
        }
        bIgnoreIsUsedFlag = false;
        nodeRoot = null;
        dataRoot = null;

        if (hlCombinedNoOp != null && hubCombined != null) {
            hubCombined.removeHubListener(hlCombinedNoOp);
            hlCombinedNoOp = null;
        }
    }

    /**
     * Ensures cleanup during garbage collection by delegating to close(). Exists for
     * legacy compatibility; explicit close() is preferred.
     */
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

    /**
     * Represents a single step in the property-path traversal chain. Each Node
     * corresponds to one link (ONE or MANY) and may have a child Node or a recursive
     * child when encountering self-referential structures.
     */
    class Node {
        Class clazz;
        String property;
        OALinkInfo liFromParentToChild;
        Constructor filterConstructor;
        Object[] filterParams;
        Node child;
        Node recursiveChild;
        Data data; // first node for root and used for Hub for link.type = One
        OACascade cascade;
        Node parent;

        /**
         * Creates a Node linked to the specified parent Node.
         *
         * @param parent the parent Node in the traversal chain
         */
        public Node(Node parent) {
            this.parent = parent;
        }

        /**
         * Closes this Node by closing its associated Data object (if any) and clearing
         * references for cleanup.
         */
        void close() {
            if (data != null) {
                data.close();
            }
            data = null;
        }

        /**
         * Returns a human-readable description of this Node, including its class,
         * property name, and link type (ONE or MANY).
         *
         * @return descriptive string representation
         */
        public @Override String toString() {
            String s = liFromParentToChild == null ? "root" : liFromParentToChild.getType() == OALinkInfo.MANY ? "Many" : "One";
            s = "class: " + clazz + ", property: " + property + ", type:" + s;
            return s;
        }
    }

    /**
     * Represents a node-instance within the Data tree. Each Data object corresponds
     * to a Node and an actual Hub instance, and manages event handling, child Data
     * creation, recursive traversal, and Hub synchronization logic.
     */
    final class Data extends HubListenerAdapter {
        final Node node;
        final Data parent;
        final OAObject parentObject; // parent object of hub
        Hub hub;
        Hub hubFilterMaster; // if using filter, then this is the master/orig that is then filtered into "hub"
        HubFilter hubFilter;
        volatile ArrayList<Data> alChildren;
        volatile boolean bHubListener;

        /**
         * Creates a Data node tied to a Node definition, a parent Data node, an optional
         * parent object, and the Hub instance supplying its members.
         *
         * @param parent the parent Data node
         * @param node the Node defining traversal rules
         * @param parentObject the owning object (null for type=One Data roots)
         * @param hubNew the Hub supplying objects for this Data node
         */
        Data(Data parent, Node node, OAObject parentObject, Hub hubNew) {
            if (hubNew == null) {
                throw new RuntimeException("hub can not be null");
            }
            if (!node.clazz.equals(hubNew.getObjectClass())) {
                if (!node.clazz.equals(Hub.class)) {
                    // 20130709
                    if (!OAObject.class.isAssignableFrom(node.clazz)) {
                        throw new RuntimeException("Hub class does not equal Node class");
                    }
                    /* was if (!OAObject.class.equals(node.clazz)) { // 20120809 could be using generic type
                     * reference (ex: OALeftJoin.A) throw new
                     * RuntimeException("Hub class does not equal Node class"); } */
                }
            }
            this.parent = parent;
            this.node = node;
            this.parentObject = parentObject;
            this.hub = hubNew;

            if (node.filterConstructor != null) {
                this.hubFilterMaster = hubNew;
                this.hub = new Hub(hubNew.getObjectClass());
                try {
                    int x = this.node.filterParams == null ? 0 : this.node.filterParams.length;
                    Object[] objs = new Object[2 + x];
                    objs[0] = this.hubFilterMaster;
                    objs[1] = this.hub;
                    if (x > 0) {
                        System.arraycopy(this.node.filterParams, 0, objs, 2, x);
                    }
                    hubFilter = ((CustomHubFilter) node.filterConstructor.newInstance(objs)).getHubFilter();
                } catch (Exception e) {
                    throw new RuntimeException("exception while creating Filter", e);
                }
            }

            // 20160806
            if (node == null || node.child == null || node.child.liFromParentToChild == null
                    || node.child.liFromParentToChild.getCalcDependentProperties() == null
                    || node.child.liFromParentToChild.getCalcDependentProperties().length == 0) {
                this.hub.addHubListener(this);
            } else {
                this.hub.addHubListener(this,
                                        node.child.liFromParentToChild.getName(),
                                        node.child.liFromParentToChild.getCalcDependentProperties(),
                                        true);
            }

            bHubListener = true;
            aiHubListenerCount.incrementAndGet();
            TotalHubListeners++;
            createChildren();
        }

        /**
         * Returns the total count of objects represented by this Data node, including its
         * own Hub's contents and all descendant Data nodes.
         *
         * @return total object count
         */
        public int getObjectCount() {
            if (hub == null) {
                return 0;
            }
            int cnt = hub.getSize();
            if (alChildren == null) {
                return cnt;
            }
            try {
                lock.readLock().lock();
                cnt = alChildren.size();
                for (Data child : alChildren) {
                    cnt += child.getObjectCount();
                }
            } finally {
                lock.readLock().unlock();
            }
            return cnt;
        }

        /**
         * Returns the number of child Data nodes beneath this Data node, accounting for
         * recursive structures when present.
         *
         * @return count of child Data nodes
         */
        public int getChildrenCount() {
            if (!bEnabled) {
                return 0;
            }
            int cnt;
            try {
                lock.readLock().lock();
                if (alChildren == null) {
                    return 0;
                }
                cnt = alChildren.size();
                for (Data child : alChildren) {
                    if (child.parentObject != null) {
                        cnt += child.getChildrenCount();
                    }
                }
            } finally {
                lock.readLock().unlock();
            }
            return cnt;
        }

        /**
         * Performs internal consistency validation for this Data node and its children
         * when verification is enabled. Logs structural mismatches and unexpected states.
         */
        void verify() {
    		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(hub);
            // todo: test when data is recursive
            if (!bVERIFY) {
                return;
            }
            if (!bEnabled) {
                return;
            }
            // All
            if (hub == null) {
                LOG.warning("hub == null, all data should have a hub.");
                return;
            }
            if (!hub.getObjectClass().equals(node.clazz)) {
                LOG.warning("hub.objectClass != node.clazz");
            }

            // node.data
            if (node.data != null) {
                if (node.liFromParentToChild != null && node.liFromParentToChild.getType() != OALinkInfo.ONE) {
                    LOG.warning("node.data != null for type!=one");
                }
            }

            // node.clazz
            if (hub != null && !node.clazz.equals(hub.getObjectClass())) {
                LOG.warning("node.clazz != hub.objectClass");
            }

            // first node
            if (node == nodeRoot) {
                if (parentObject != null) {
                    LOG.warning("should not have parentObject for nodeRoot");
                }
                if (hub != hubRoot) {
                    LOG.warning("dataRoot.hub != hubRoot");
                }
                try {
                    lock.readLock().lock();
                    if (alChildren == null) {
                        LOG.warning("dataRoot.alChildren == null");
                    }

                    if (bUseAll) {
                        int x1 = alChildren.size();
                        int x2 = hub.getSize();
                        if (x1 != x2) {
                            if (Math.abs(x1 - x2) > 1) {
                                LOG.warning("alChildren.size=" + x1 + " != hub.getSize=" + x2);
                            }
                        }
                    } else {
                        int x = (hubRoot.getAO() == null) ? 0 : 1;

                        if (x == 0 && node.child.liFromParentToChild.getType() == OALinkInfo.ONE) {
                            if (og.getHubService().getHubDetailService().getLinkInfoFromDetailToMaster(hubRoot) == node.child.liFromParentToChild) {
                                if (hubRoot.getMasterObject() != null) {
                                    x = 1;
                                }
                            }
                        }
                        if (node.recursiveChild != null) {
                            x *= 2;
                        }
                        if (Math.abs(alChildren.size() - x) > 1) {
                            LOG.warning("bUseAll=false, alChildren.size != " + x);
                        }
                    }
                } finally {
                    lock.readLock().unlock();
                }
            }

            // last node
            if (node.child == null && node.recursiveChild == null) {
                if (alChildren != null) {
                    LOG.warning("node.child=null, alChildren != null");
                }
            }

            // not first or last
            if (alChildren == null) {
                if (node.child != null || node.recursiveChild != null) {
                    LOG.warning("alChildren == null");
                }
            }

            if (node.data == this) {
                if (parentObject != null) {
                    LOG.warning("parentObject != null");
                }
            }

            // ONE
            if (node.liFromParentToChild != null && node.liFromParentToChild.getType() == OALinkInfo.ONE) {
                if (parentObject != null) {
                    LOG.warning("parentObject != null");
                }
                if (node.data != this) {
                    LOG.warning("node.data != this");
                }
                for (int i = 0; hub != null; i++) {
                    Object obj = hub.getAt(i);
                    if (obj == null) {
                        break;
                    }
                    if (!isUsed(obj, node)) {
                        LOG.warning("Object in type.One is not used");
                    }
                }
            }

            // MANY
            if (node.liFromParentToChild == null || node.liFromParentToChild.getType() == OALinkInfo.MANY) {
                if (node.liFromParentToChild == null) {
                    if (node.data == null) {
                        LOG.warning("node.data == null for nodeRoot");
                    }
                } else {
                    if (node.data != null) {
                        LOG.warning("node.data != null for type=Many");
                    }
                }
                if (node.child == null && bShareEndHub && hubCombined != null && this.hub != hubCombined.getSharedHub()) {
                    LOG.warning("node.hub != hubCombined.sharedHub");
                }
                if (node.child != null) {
                    try {
                        lock.readLock().lock();

                        if (this == dataRoot && !bUseAll) {
                            if (alChildren.size() > 1) {
                                LOG.warning("alChildren.size > 1");
                            }
                        }
                        if (bUseAll) {
                            if (alChildren == null) {
                                LOG.warning("alChildren = null");
                            } else if (hub == null) {
                                LOG.warning("Hub = null");
                            } else {
                                int x1 = alChildren.size();
                                int x2 = hub.getSize();
                                if (node.recursiveChild != null) {
                                    x2 *= 2;
                                }
                                if (x1 != x2) {
                                    if (Math.abs(x1 - x2) > 1) {
                                        LOG.warning("alChildren.size=" + x1 + " != hub.getSize=" + x2);
                                    }
                                }
                            }
                        } else {
                            for (Data child : alChildren) {
                                if (child.parentObject != null && !hub.contains(child.parentObject)) {
                                    LOG.warning("alChildren object not in hub");
                                }
                            }
                        }
                    } finally {
                        lock.readLock().unlock();
                    }

                    for (int i = 0; hub != null; i++) {
                        Object obj = hub.getAt(i);
                        if (obj == null) {
                            break;
                        }
                        if (node.child == null && hubCombined != null && !hubCombined.contains(obj)) {
                            LOG.warning("object not in hubCombined");
                        }
                    }
                } else {
                    if (node.recursiveChild != null) {
                        if (alChildren == null) {
                            LOG.warning("alChildren = null");
                        } else if (hub == null) {
                            LOG.warning("Hub = null");
                        } else {
                            int x1 = alChildren.size();
                            int x2 = hub.getSize();
                            if (x1 != x2) {
                                if (Math.abs(x1 - x2) > 1) {
                                    LOG.warning("recursive alChildren.size=" + x1 + " != hub.getSize=" + x2);
                                }
                            }
                        }
                    }

                    if (!bShareEndHub) {
                        for (int i = 0; hub != null; i++) {
                            Object obj = hub.getAt(i);
                            if (obj == null) {
                                break;
                            }
                            if (hubCombined != null && !hubCombined.contains(obj)) {
                                LOG.warning("object not in hubCombined");
                            }
                        }
                    }
                }
            }

            try {
                lock.readLock().lock();
                if (alChildren != null) {
                    for (Data child : alChildren) {
                        if (child.node.data == null) {
                            child.verify();
                        }
                    }
                }
            } finally {
                lock.readLock().unlock();
            }
        }

        /**
         * Determines whether the current background thread should stop processing due to
         * a newer onNewList operation superseding its workload.
         *
         * @return true if the thread should stop processing; false otherwise
         */
        private boolean shouldQuit() {
            Thread t = Thread.currentThread();
            if (t instanceof MyThread) {
                int x = ((MyThread) t).cntNewList;
                if (x != aiNewList.get()) {
                    return true;
                }
            }
            return false;

        }

        /**
         * Builds or rebuilds the child Data nodes based on this Data node’s Hub contents
         * and the traversal rules of its associated Node.
         */
        void createChildren() {
            if (!bEnabled) {
                return;
            }

            if (shouldQuit()) {
                return;
            }

            if (node.child != null || node.recursiveChild != null) {
                try {
                    int x = Math.max(hub.getSize(), 3);
                    if (!bUseAll && this == dataRoot) {
                        x = 1;
                    }
                    lock.writeLock().lock();
                    alChildren = new ArrayList<Data>(x);
                } finally {
                    lock.writeLock().unlock();
                }
            }

    		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(HubMerger.this.getRootHub());
            if (node.child == null) {
                if (bShareEndHub) {
                    if (hubCombined != null) {
                        hubCombined.setSharedHub(hub, bShareActiveObject);
                    }
                } else {
                    if (og.getSyncService().isClient()) {
                        // preload, so that any getDetail will be more efficient
                        for (int i = 0;; i++) {
                            OAObject obj = (OAObject) hub.elementAt(i);
                            if (obj == null) {
                                break;
                            }
                            if (shouldQuit()) {
                                return;
                            }
                            if (node.child != null && node.child.liFromParentToChild != null) {
                                node.child.liFromParentToChild.getValue(obj);
                            } else if (node.liFromParentToChild != null) {
                                // 20201221 bug??
                                // node.liFromParentToChild.getValue(obj);
                            }
                        }
                    }
                    for (int i = 0;; i++) {
                        OAObject obj = (OAObject) hub.elementAt(i);
                        if (obj == null) {
                            break;
                        }
                        if (shouldQuit()) {
                            return;
                        }
                        createChild(obj);
                    }
                }
            } else {
                if (bUseAll || this.node != nodeRoot && nodeRoot != null) {
                    OAThreadLocal tl;
                    Hub hubx = null;
                    //if (!bCreatedFromOneObject) hubx = srvcOAThreadLocal.setGetDetailHub(hub);
                    try {
                        if (og.getSyncService().isClient()) {
                            // preload, so that any getDetail will be more efficient
                            for (int i = 0;; i++) {
                                OAObject obj = (OAObject) hub.elementAt(i);
                                if (obj == null) {
                                    break;
                                }
                                if (shouldQuit()) {
                                    return;
                                }
                                if (node.child != null && node.child.liFromParentToChild != null) {
                                    node.child.liFromParentToChild.getValue(obj);
                                } else if (node.liFromParentToChild != null) {
                                    node.liFromParentToChild.getValue(obj);
                                }
                            }
                        }
                        for (int i = 0;; i++) {
                            OAObject obj = (OAObject) hub.elementAt(i);
                            if (obj == null) {
                                break;
                            }
                            if (shouldQuit()) {
                                return;
                            }
                            createChild(obj);
                        }
                    } finally {
                        //    if (!bCreatedFromOneObject) srvcOAThreadLocal.resetGetDetailHub(hubx);
                    }
                } else {
                    OAObject obj = (OAObject) hub.getAO();
                    if (obj != null) {
                        createChild(obj);
                    } else {
                        createChildUsingMaster();
                    }
                }
            }
        }

        // 20110809 see if the the masterHub/Object can be used. This is for cases where hub.size=0, but
        // you want
        // to have the merger get objects based on master. ex: OrderContacts propPath
        // "order.customer.contacts" for a
        // hub to link and autocreate the orderContact objects

        /**
         * Attempts to create child Data nodes using the master object of the Hub when
         * the Hub is empty—used for detail-Hub traversal cases.
         */
        void createChildUsingMaster() {
			final OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();  
            try {
                if (bServerSideOnly) {
                    srvcOARemoteThread.sendMessages(true);
                }
                _createChildUsingMaster();
            } finally {
                if (bServerSideOnly) {
                    srvcOARemoteThread.sendMessages(false);
                }
            }
        }

        /**
         * Internal implementation of createChildUsingMaster(). Performs link resolution
         * and constructs necessary Data nodes when navigating through master references.
         */
        void _createChildUsingMaster() {
            if (!bEnabled) {
                return;
            }
            // XOG.finer("createChild");
            if (node.child == null) {
                return;
            }

    		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(HubMerger.this.getRootHub());
            String s = og.getHubService().getHubDetailService().getPropertyFromDetailToMaster(hub);
            if (s == null || !s.equalsIgnoreCase(node.child.property)) {
                return;
            }

            // 20210311
            if (node.child.child == null) {
                return;
            }

            if (node.child.liFromParentToChild.getType() == OALinkInfo.ONE) { // store in Node.data.hub
                if (node.child.data == null) {
                    Hub h;
                    if (node.child.child == null) {
                        h = hubCombined;
                    } else {
                        h = new Hub(node.child.clazz);
                    }

                    node.child.data = new Data(this, node.child, null, h);
                }
                OAObject ref = (OAObject) hub.getMasterObject();
                if (ref == null) {
                    return;
                }

                if (!node.child.data.hub.contains(ref)) {
                    node.child.data.hub.add(ref); // this will send afterAdd(), which will create children
                }

                if (alChildren != null && alChildren.size() == 0) { // could have been closed in another thread
                    try {
                        lock.writeLock().lock();
                        this.alChildren.add(node.child.data);
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
            } else {
                Hub h = (Hub) hub.getMasterHub();
                if (h == null) {
                    return;
                }
                Data d = new Data(this, node.child, null, h);
                try {
                    lock.writeLock().lock();
                    if (alChildren != null && d != null) { // could have been closed in another thread
                        alChildren.add(d);
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }

        /**
         * Creates child Data nodes for the given parent object, including recursive
         * children if defined for the traversal Node.
         *
         * @param parent the parent OAObject
         */
        void createChild(OAObject parent) {
            _createChild(parent);
            _createRecursiveChild(parent);
        }

        /**
         * Internal wrapper for child creation that applies server-side messaging rules
         * before invoking the main implementation.
         *
         * @param parent the parent object to process
         */
        void _createChild(OAObject parent) {
            if (shouldQuit()) {
                return;
            }
			final OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();  
            try {
                if (bServerSideOnly) {
                    srvcOARemoteThread.sendMessages(true);
                }
                _createChild2(parent);
            } finally {
                if (bServerSideOnly) {
                    srvcOARemoteThread.sendMessages(false);
                }
            }
        }

        /**
         * Core child-creation routine that resolves ONE/MANY links, updates the combined
         * Hub when needed, and populates Data nodes for subsequent traversal.
         *
         * @param parent the parent object used for link navigation
         */
        void _createChild2(OAObject parent) {
            if (!bEnabled) {
                return;
                // XOG.finer("createChild");
            }

            // 20131209
            if (node == nodeRoot && bIncludeRootHub) {
                if (hubCombined != null && !hubCombined.contains(parent)) {
                    final boolean bx = aiLoadingCombinedHub.get() > 0;
        			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
                    try {
                        if (bx) {
                            srvcOAThreadLocal.setLoading(true);
                        }
                        hubCombined.add(parent);
                    } finally {
                        if (bx) {
                            srvcOAThreadLocal.setLoading(false);
                        }
                    }
                }
            }

            if (node.child == null) {
                if (!bShareEndHub && hubCombined != null && !hubCombined.contains(parent)) {
                    final boolean bx = aiLoadingCombinedHub.get() > 0;
        			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
                    try {
                        if (bx) {
                            srvcOAThreadLocal.setLoading(true);
                        }
                        hubCombined.add(parent);
                    } finally {
                        if (bx) {
                            srvcOAThreadLocal.setLoading(false);
                        }
                    }
                }
                HubMerger.this.onAddToCombined(this, parent);
            } else if (node.child.liFromParentToChild.getType() == OALinkInfo.ONE) { // store in Node.data.hub
                if (node.child.data == null) {
                    Hub h;
                    if (node.child.child == null && hubCombined != null) {
                        h = hubCombined;
                    } else {
                        h = new Hub(node.child.clazz);
                    }
                    Data data = new Data(this, node.child, null, h);
                    node.child.data = data;
                }
                OAObject ref = (OAObject) node.child.liFromParentToChild.getValue(parent);

                if (ref != null) {
                    if (!node.child.data.hub.contains(ref)) {
                        node.child.data.hub.add(ref); // this will send afterAdd(), which will create children
                    }
                }
                try {
                    lock.writeLock().lock();
                    if (alChildren != null && alChildren.size() == 0 && node.child.data != null) { // could have been closed in another thread
                        this.alChildren.add(node.child.data); // even if obj==null, so that verify will
                                                                // work - it looks for alChildren.size=1
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            } else {
                Hub h = (Hub) node.child.liFromParentToChild.getValue(parent);
                Data d = new Data(this, node.child, parent, h);
                try {
                    lock.writeLock().lock();
                    if (alChildren != null && d != null) { // could have been closed in another thread
                        alChildren.add(d);
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }

        /**
         * Creates a Data child for recursive property links. Uses an OACascade tracker
         * to prevent infinite loops when navigating self-referential relationships.
         *
         * @param parent the object whose recursive children are being created
         */
        void _createRecursiveChild(OAObject parent) {
            if (!bEnabled) {
                return;
            }
            if (node.recursiveChild == null) {
                return;
            }
            if (shouldQuit()) {
                return;
            }

            boolean bHadCascade;
            if (node.recursiveChild.cascade == null) {
                node.recursiveChild.cascade = new OACascade();
                bHadCascade = false;
            } else {
                bHadCascade = true;
            }

            if (node.recursiveChild.cascade.wasCascaded(parent, true)) {
                return;
            }

            Hub h = (Hub) node.recursiveChild.liFromParentToChild.getValue(parent);
            Data d = new Data(this, node.recursiveChild, parent, h);
            try {
                lock.writeLock().lock();
                if (alChildren != null && d != null) { // could have been closed in another thread
                    alChildren.add(d);
                }
            } finally {
                lock.writeLock().unlock();
                if (!bHadCascade) {
                    node.recursiveChild.cascade = null;
                }
            }
        }

        /**
         * Internal recursive check to determine whether the given object is part of this
         * Data tree. Traverses child Data nodes when appropriate.
         *
         * @param objFind the object to check
         * @param nodeFind optional Node restricting the search scope
         * @return true if the object is included; false otherwise
         */
        private boolean _isUsed(Object objFind, Node nodeFind) {
            if (bIgnoreIsUsedFlag) {
                return false;
            }
            if (!bEnabled) {
                return false;
            }
            if (node.child == null) {
                boolean b = (nodeFind == null && hub != null && hub.contains(objFind));
                if (b || node.recursiveChild == null) {
                    return b;
                }
            }

            if (this.node.child != null && this.node.child == nodeFind) {
                if (this.node == nodeRoot && !bUseAll) {
                    OAObject obj = (OAObject) this.hub.getAO();
                    OAObject ref = null;
                    if (obj == null) {
                        ref = this.hub.getMasterObject();
                    } else {
                        ref = (OAObject) node.child.liFromParentToChild.getValue(obj);
                    }

                    if (ref == objFind) {
                        return true;
                    }
                } else {
                    for (int i = 0;; i++) {
                        OAObject obj = (OAObject) this.hub.elementAt(i);
                        if (obj == null) {
                            break;
                        }
                        OAObject ref = (OAObject) node.child.liFromParentToChild.getValue(obj);
                        if (ref == objFind) {
                            return true;
                        }
                    }

                    if (this.hub.size() == 0 && this.node == nodeRoot) {
                        OAObject ref = this.hub.getMasterObject();
                        if (ref == objFind) {
                            return true;
                        }
                    }
                }
            } else {
                if (alChildren != null) {
                    for (int i = 0;; i++) {
                        Data data;
                        try {
                            lock.readLock().lock();
                            if (i >= alChildren.size()) {
                                break;
                            }
                            data = alChildren.get(i);
                        } finally {
                            lock.readLock().unlock();
                        }
                        if (data._isUsed(objFind, nodeFind)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        /**
         * Returns a descriptive representation of this Data node, including its property
         * name, parent object, and Hub details.
         *
         * @return a descriptive String
         */
        public @Override String toString() {
            String s = "";
            if (hub != null) {
                s = ", hub:" + hub.getObjectClass().getName() + ", cnt:" + hub.getSize();
            }
            return node.property + ", parent:" + parentObject + s;
        }

        /**
         * Removes the given object from this Data node and its descendants, applying
         * server-side messaging rules before delegating to the core removal logic.
         *
         * @param obj object being removed
         */
        void remove(Object obj) {
			final OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();  
            try {
                if (bServerSideOnly) {
                    srvcOARemoteThread.sendMessages(true);
                }
                _remove(obj);
            } finally {
                if (bServerSideOnly) {
                    srvcOARemoteThread.sendMessages(false);
                }
            }
        }

        /**
         * Core removal implementation. Updates the combined Hub, handles recursive-node
         * semantics, and detaches descendant Data nodes as needed.
         *
         * @param obj the object to remove
         */
        void _remove(final Object obj) {
            if (!bEnabled) {
                return;
            }

            if (alChildren == null || node.child == null) {
                if (isUsed(obj)) {
                    // needs to remove from alChildren, ex: when using recursive properties
                    // was: return;
                } else if (!bShareEndHub) {
                    if (this.hub == hubCombined) {
                        if (hubCombined == null || !hubCombined.contains(obj)) {
                            return; // might have already been removed
                        }
                    }
        			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
                    if (srvcOAThreadLocal.isHubMergerChanging()) { // 20120102
                        // 20120612 dont send event, unless there is a recursive prop, which needs to
                        // have recursives nodes updated
                		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(hubCombined);
        				final HubAddRemoveService srvcHubAddRemove = og.getHubService().getHubAddRemoveService();
                        srvcHubAddRemove.remove(hubCombined, obj, false, bIsRecusive, false, false, false, false);
                    } else {
                        if (hubCombined != null) {
                            hubCombined.remove(obj);
                        }
                    }
                }
                if (obj instanceof OAObject) {
                    onRemoveFromCombined(this, (OAObject) obj);
                }
                if (alChildren == null) {
                    return;
                }
            }

            // 20131209
            if (node == nodeRoot && bIncludeRootHub) {
                if (!isUsed(obj)) {
        			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
                    if (srvcOAThreadLocal.isHubMergerChanging()) {
                		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(hubCombined);
        				final HubAddRemoveService srvcHubAddRemove = og.getHubService().getHubAddRemoveService();
                        srvcHubAddRemove.remove(hubCombined, obj, false, bIsRecusive, false, false, false, false);
                    } else {
                        if (hubCombined != null) {
                            hubCombined.remove(obj);
                        }
                    }
                }
            }

            for (int alPos = 0;; alPos++) {
                Data child;
                try {
                    lock.readLock().lock();
                    if (alChildren == null || alPos >= alChildren.size()) {
                        break;
                    }
                    child = alChildren.get(alPos);
                } finally {
                    lock.readLock().unlock();
                }
                if (obj == child.parentObject) { // will always be a type=Many
                    try {
                        lock.writeLock().lock();
                        if (alChildren == null || alPos >= alChildren.size()) {
                            break;
                        }
                        this.alChildren.remove(alPos);
                        alPos--;
                    } finally {
                        lock.writeLock().unlock();
                    }
                    child.close();
                    if (this.node.recursiveChild == null) {
                        break;
                    }
                }
                if (child.parentObject == null) { // will always be a type=One
                    Object ref = node.child.liFromParentToChild.getValue(obj);
                    try {
                        lock.writeLock().lock();
                        if (alChildren == null || alPos >= alChildren.size()) {
                            break;
                        }
                        /* 20210102 this is a OneLink, that stores objs in node.data.hub, so keep the 1 alChildren data object
                        this.alChildren.remove(alPos);
                        alPos--;
                        */
                    } finally {
                        lock.writeLock().unlock();
                    }
                    if (ref != null) {
                        if (!isUsed(ref, child.node)) {
                			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
                            if (srvcOAThreadLocal.isHubMergerChanging()) { // 20120102
                        		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(child.hub);
                				final HubAddRemoveService srvcHubAddRemove = og.getHubService().getHubAddRemoveService();
                				srvcHubAddRemove.remove(child.hub, ref, false, false, false, false, false, false);
                            } else {
                                child.hub.remove(ref);
                            }
                        }
                    }
                    if (this.node.recursiveChild == null) {
                        break;
                    }
                }
            }
        }

        /**
         * Closes this Data node, removes listeners, detaches children, and optionally
         * clears the combined Hub when operating at terminal nodes.
         */
        void close() {
            // XOG.finer("close");
            if (hub != null && bHubListener) {
                hub.removeHubListener(this);
                aiHubListenerCount.decrementAndGet();
                bHubListener = false;
                TotalHubListeners--;
            }
            if (hubFilter != null) {
                hubFilter.close();
                hubFilter = null;
            }
            boolean bLockSet = true;
            try {
                lock.readLock().lock();
                if (alChildren == null || node.child == null) {
                    if (bShareEndHub) {
                        if (hubCombined != null) {
                            hubCombined.setSharedHub(null);
                        }
                        return;
                    }
                    if (hub != null) {
                        Object[] objs = hub.toArray();
                        lock.readLock().unlock();
                        bLockSet = false;
                        for (int i = 0; i < objs.length; i++) {
                            remove(objs[i]);
                        }
                    }
                    if (alChildren == null) {
                        return;
                    }
                }
            } finally {
                if (bLockSet) {
                    lock.readLock().unlock();
                }
            }

            if (node.child != null && node.child.liFromParentToChild.getType() == OALinkInfo.ONE) {
                // dont call close on Node.data. This will instead use remove()
                Object[] objs = hub.toArray();
                for (int i = 0; i < objs.length; i++) {
                    remove(objs[i]);
                }
            } else {
                for (;;) {
                    Data child;
                    try {
                        lock.writeLock().lock();
                        if (alChildren == null || alChildren.size() == 0) {
                            break;
                        }
                        child = alChildren.get(0);
                        alChildren.remove(0);
                    } finally {
                        lock.writeLock().unlock();
                    }
                    child.close();
                }
            }
            try {
                lock.writeLock().lock();
                alChildren = null;
            } finally {
                lock.writeLock().unlock();
            }
        }

        // ============ HubListener for Hub used for child
        /**
         * Invoked before the associated Hub removes all its objects. Delegates HubMerger
         * notifications and clears Data-node structures as needed.
         *
         * @param e the HubEvent for the remove-all operation
         */
        public @Override void beforeRemoveAll(HubEvent e) {
            final boolean b = (hub == hubRoot);
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
            try {
                if (b) {
                    srvcOAThreadLocal.setHubMergerChanging(true);
                }
                _beforeRemoveAll(e);
            } finally {
                if (b) {
                    srvcOAThreadLocal.setHubMergerChanging(false);
                }
            }
        }

        /**
         * Internal remove-all logic. Removes all child objects from this Data node while
         * respecting recursive cases and is-used semantics.
         *
         * @param e the HubEvent being processed
         */
        private void _beforeRemoveAll(HubEvent e) {
            //20150622
            if ((node == nodeRoot && bIncludeRootHub) || (node.child == null)) {
                HubMerger.this.beforeRemoveAllRealHub(e);
            }
            /*was
            Hub h = e.getHub();
            if (h.getObjectClass().equals(hubCombined.getObjectClass())) {
                HubMerger.this.beforeRemoveAllRealHub(e);
            }
            */

            if (!bEnabled) {
                return;
            }

            // 20140611 hub.clear/removeAll no longer removes each obj
            /*
            if (this != dataRoot) return;
            if (!bUseAll) return;
            if (hub.isLoading()) return;
            */

            boolean hold = bIgnoreIsUsedFlag;
            bIgnoreIsUsedFlag = true;
            for (int i = 0;; i++) {
                Object obj = hub.getAt(i);
                if (obj == null) {
                    break;
                }
                remove(obj);
            }
            if (!hold) {
                bIgnoreIsUsedFlag = false;
            }
        }

        /**
         * Notifies HubMerger after a remove-all operation from the real Hub.
         *
         * @param e the HubEvent describing the removal
         */
        @Override
        public void afterRemoveAll(HubEvent e) {
            //20150622
            if ((node == nodeRoot && bIncludeRootHub) || (node.child == null)) {
                HubMerger.this.afterRemoveAllRealHub(e);
            }
            /*
            Hub h = e.getHub();
            if (h.getObjectClass().equals(hubCombined.getObjectClass())) {
                HubMerger.this.afterRemoveAllRealHub(e);
            }
            */
        }

        final Object LockNewList = new Object();
        final HashSet<Thread> hsNewList = new HashSet<>();

        /**
         * Handles onNewList events issued by the Hub. Supports background-thread
         * processing and ensures that list rebuilds are serialized when multiple updates
         * overlap.
         *
         * @param hubEvent the list-reset event
         */
        @Override
        public void onNewList(final HubEvent hubEvent) {
            // only needed if this is root hub, and using all objects in hub (not just AO).  Otherwise the hub.setAO(null, force) event will load the nodes
            if (this.node != nodeRoot || !bUseAll) {
                return;
            }

    		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(HubMerger.this.getRootHub());
            if (og.getSyncService().isServer()) {
                _onNewList();
                return;
            }

            final int cnt = aiNewList.incrementAndGet();
            aiLoadingCombinedHub.incrementAndGet();

            final Thread threadEvent = Thread.currentThread();

            getExecutorService().submit(new Runnable() {
                @Override
                public void run() {
                    ((MyThread) Thread.currentThread()).cntNewList = cnt;

                    if (shouldQuit()) {
                        return;
                    }

                    synchronized (LockNewList) {
                        hsNewList.add(threadEvent);
                        if (hsNewList.size() > 1) {
                            for (;;) {
                                try {
                                    LockNewList.wait();
                                    break;
                                } catch (Exception e) {
                                }
                            }
                        }
                    }
                    try {
                        _onNewList();
                    }
                    finally {
                        aiLoadingCombinedHub.decrementAndGet();
                    }

                    if (bIncludeRootHub || (node.child == null)) {
                        onNewListRealHub(hubEvent);
                    }

                    synchronized (LockNewList) {
                        hsNewList.remove(threadEvent);
                        for (;;) {
                            try {
                                LockNewList.notifyAll();
                                break;
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            });
        }

        /**
         * Performs the main onNewList processing inside the correct thread context,
         * measuring execution time and logging performance characteristics.
         */
        private void _onNewList() {
            long ts = System.currentTimeMillis();
            final boolean b = bServerSideOnly;
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
            try {
                srvcOAThreadLocal.setHubMergerChanging(true);
                if (!b) {
                    aiLoadingCombinedHub.incrementAndGet();
                }
                _onNewList2();
            } finally {
                srvcOAThreadLocal.setHubMergerChanging(false);
                if (!b) {
                    aiLoadingCombinedHub.decrementAndGet();
                    if (!shouldQuit()) {
                		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(HubMerger.this.getRootHub());
                        og.getHubService().getHubEventService().fireOnNewListEvent(hubCombined, false);
                    }
                }
            }
            if (shouldQuit()) {
                return;
            }

            ts = System.currentTimeMillis() - ts;
            if (ts > 25) {
                String s = ("HM." + id + ") onNewList hub=" + hubRoot + ", propertyPath=" + propertyPath + ", useAll=" + bUseAll
                        + ", useBackgroundThread=" + getUseBackgroundThread());
                s += ", combinedHub=" + hubCombined;
                s += ", time=" + ts + "ms";

                if (!getUseBackgroundThread()) {
                    if (bUseAll) {
                        int x = hubRoot.size();
                        if (x > 50) {
                            if (x > 150 || propertyPath.indexOf(".") > 0) {
                                s += ", ALERT";
                            }
                        }
                    }
                    if (hubCombined.getSize() > 250) {
                        s += ", ALERT";
                    }
                    if (ts > 1000) {
                        s += ", ALERT";
                    }
                }
                OAPerformance.LOG.finer(s);
                LOG.fine(s);
            }
        }

        /**
         * Clears and reloads the combined Hub as part of a list-replacement sequence,
         * then fires the corresponding onNewList event.
         */
        public void _onNewList2() {
            if (!bShareEndHub) {
                if (hubCombined != null) {
                    hubCombined.clear();
                }
            }
            try {
                _onNewList3();
            } finally {
            }
            if (hubCombined != null) {
                if (!bShareEndHub) {
            		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(HubMerger.this.getRootHub());
                    og.getHubService().getHubEventService().fireOnNewListEvent(hubCombined, true);
                }
            }
        }

        /**
         * Wraps list processing in sibling-helper and messaging-suppression management,
         * ensuring that child traversal occurs under correct OAThreadLocal conditions.
         */
        private void _onNewList3() {
            final OASiblingHelper sh = getSiblingHelper();
			final OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();  
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
            boolean bx = srvcOAThreadLocal.addSiblingHelper(sh);
            try {
                if (bServerSideOnly) {
                    srvcOARemoteThread.sendMessages(true);
                } else {
                    srvcOAThreadLocal.setSuppressCSMessages(true);
                }
                _onNewList4();
            } finally {
                if (bServerSideOnly) {
                    srvcOARemoteThread.sendMessages(false);
                } else {
                    srvcOAThreadLocal.setSuppressCSMessages(false);
                }
                if (bx) {
                    srvcOAThreadLocal.removeSiblingHelper(sh);
                }
            }
        }

        /**
         * Drives the top-level list processing, enabling and disabling the ignore-used
         * flag, then triggering either full or partial child creation depending on
         * bUseAll and active-object state.
         */
        private void _onNewList4() {
            if (!bEnabled) {
                return;
            }
            try {
                bIgnoreIsUsedFlag = true;
                _onNewList5();
            } finally {
                bIgnoreIsUsedFlag = false;
            }

            // load children
            if (bUseAll) {
                createChildren();
            } else {
                Object obj = hub.getAO();
                if (obj != null) {
                    createChild((OAObject) obj);
                } else {
                    createChildUsingMaster();
                }
            }
        }

        /**
         * Clears child Data nodes in preparation for rebuilding them when the Hub's list
         * is replaced. Handles both ONE and MANY link types appropriately.
         */
        private void _onNewList5() {
            if (node.child != null && node.child.liFromParentToChild.getType() == OALinkInfo.ONE) {

                // dont call close on Node.data. This will instead call remove()
                // not used: final boolean bIsMergerChanging = srvcOAThreadLocal.isHubMergerChanging();
                for (; node.child.data != null && node.child.data.hub != null;) {
                    Object obj = node.child.data.hub.getAt(0);
                    if (obj == null) {
                        break;
                    }
                    node.child.data.hub.remove(obj);
                }
                try {
                    lock.writeLock().lock();
                    if (alChildren != null) {
                        alChildren.clear();
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            } else {
                for (;;) {
                    Data child;
                    try {
                        lock.writeLock().lock();
                        if (alChildren == null || alChildren.size() == 0) {
                            break;
                        }
                        child = alChildren.get(0);
                        alChildren.remove(0);
                    } finally {
                        lock.writeLock().unlock();
                    }
                    child.close();
                }
            }
        }

        /*
         * If rootHub, then wait for background thread to finish loading
         */
        /**
         * Runs after onNewList event propagation. For UI-thread execution paths, this
         * method may pause until background list processing finishes.
         *
         * @param hubEvent the list event
         */
        @Override
        public void afterNewList(HubEvent hubEvent) {
    		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(HubMerger.this.getRootHub());
            if ((hub != hubRoot) || og.getSyncService().isServer()) {
                return;
            }

            if (getUseBackgroundThread() || SwingUtilities.isEventDispatchThread()) {
                return; // let run in the background
            }

            Thread t = Thread.currentThread();
            synchronized (LockNewList) {
                for (int i = 0; i < 40; i++) {
                    if (!hsNewList.contains(t)) {
                        break;
                    }
                    if (i > 20) {
                        LOG.log(Level.WARNING, "HubMerger lockNewList timeout waiting for HubMerger thread to finish",
                                new Exception("HubMerger lockNewList timeout after 500ms, will continue"));
                        break;
                    }
                    try {
                        LockNewList.wait(25);
                    } catch (Exception e) {
                    }
                }
            }

        }

        /**
         * Intercepts remove events before the underlying Hub processes them. This method
         * notifies HubMerger when appropriate and prepares removal state.
         *
         * @param e the HubEvent for the removal
         */
        @Override
        public void beforeRemove(HubEvent e) {
            Object obj = e.getObject();
            final boolean b = (hub == hubRoot);
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
            try {
                if (b) {
                    srvcOAThreadLocal.setHubMergerChanging(true);
                }
                if (obj != null) {
                    //20150622
                    if ((node == nodeRoot && bIncludeRootHub) || (node.child == null)) {
                        HubMerger.this.beforeRemoveRealHub(e);
                    }
                    /*was
                    Class c = obj.getClass();
                    if (c.equals(hubCombined.getObjectClass()) || OAObject.class.equals(hubCombined.getObjectClass())) {
                        HubMerger.this.beforeRemoveRealHub(e);
                    }
                    */
                }
            } finally {
                if (b) {
                    srvcOAThreadLocal.setHubMergerChanging(false);
                }
            }
        }

        /**
         * Handles post-remove logic, including notifying HubMerger hooks and performing
         * Data-node removal when enabled.
         *
         * @param e the HubEvent after removal
         */
        @Override
        public void afterRemove(HubEvent e) {
            Object obj = e.getObject();

            if (obj != null) {
                //20150622
                if ((node == nodeRoot && bIncludeRootHub) || (node.child == null)) {
                    HubMerger.this.afterRemoveRealHub(e);
                }
                /*was
                Class c = obj.getClass();
                if (c.equals(hubCombined.getObjectClass())) {
                    HubMerger.this.afterRemoveRealHub(e);
                }
                */
            }
            if (!bEnabled) {
                return;
            }
            if (this == dataRoot && !bUseAll) {
                if (!bIncludeRootHub) { // 20131209
                    return;
                }
            }
            try {
                // 20120903 removed/commented this, and need to have hub event sent out for remove
                // if (hub == hubRoot) srvcOAThreadLocal.setHubMergerIsChanging(true);
                remove(obj);
            } finally {
                // if (hub == hubRoot) srvcOAThreadLocal.setHubMergerIsChanging(false);
            }
        }

        /**
         * Intercepts add events before the Hub processes them. Notifies HubMerger when
         * applicable and sets HubMerger state flags.
         *
         * @param e the HubEvent for the add
         */
        @Override
        public void beforeAdd(HubEvent e) {
            final boolean b = (hub == hubRoot);
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
            try {
                if (b) {
                    srvcOAThreadLocal.setHubMergerChanging(true);
                }
                Object obj = e.getObject();
                if (obj != null) {
                    //20150622
                    if ((node == nodeRoot && bIncludeRootHub) || (node.child == null)) {
                        HubMerger.this.beforeAddRealHub(e);
                    }
                    /*was
                    Class c = obj.getClass();
                    if (c.equals(hubCombined.getObjectClass()) || OAObject.class.equals(hubCombined.getObjectClass())) {
                        HubMerger.this.beforeAddRealHub(e);
                    }
                    */
                }
            } finally {
                if (b) {
                    srvcOAThreadLocal.setHubMergerChanging(false);
                }
            }
        }

        /**
         * Handles post-add logic for Hub events. Notifies HubMerger hooks, updates state,
         * and delegates to afterAdd2 for child creation when appropriate.
         *
         * @param e the HubEvent describing the add
         */
        public @Override void afterAdd(HubEvent e) {
            final boolean b = (hub == hubRoot);
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
            try {
                if (b) {
                    srvcOAThreadLocal.setHubMergerChanging(true);
                }
                Object obj = e.getObject();
                if (obj != null) {
                    //20150622
                    if ((node == nodeRoot && bIncludeRootHub) || (node.child == null)) {
                        HubMerger.this.afterAddRealHub(e);
                    }
                    /*was
                    Class c = obj.getClass();
                    if (c.equals(hubCombined.getObjectClass()) || OAObject.class.equals(hubCombined.getObjectClass())) {
                        HubMerger.this.afterAddRealHub(e);
                    }
                    */
                }

                afterAdd2(e);
            }
            catch (ArrayIndexOutOfBoundsException ex) {
                // ignore
            } finally {
                if (b) {
                    srvcOAThreadLocal.setHubMergerChanging(false);
                }
            }
        }

        /**
         * Internal helper invoked after an add or insert event. If the added object is
         * still present, creates child Data nodes to extend the traversal structure.
         *
         * @param e the HubEvent received
         */
        private void afterAdd2(HubEvent e) {
            if (!bEnabled) {
                return;
            }
            if (this == dataRoot && !bUseAll) {
                return;
                // 20150713 took this out, since hubFilter initialize sets isLoading=true
                //            if (hub.isLoading()) return;
            }

            // 20140312 verify that object is still in Hub
            if (e.getHub().contains(e.getObject())) {
                createChild((OAObject) e.getObject());
            }
        }

        /**
         * Intercepts insert events before they occur in the real Hub. Notifies relevant
         * HubMerger hooks and applies state flags for controlled processing.
         *
         * @param e the HubEvent for the insertion
         */
        @Override
        public void beforeInsert(HubEvent e) {
            final boolean b = (hub == hubRoot);
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
            try {
                if (b) {
                    srvcOAThreadLocal.setHubMergerChanging(true);
                }
                Object obj = e.getObject();
                if (obj != null) {
                    //20150622
                    if ((node == nodeRoot && bIncludeRootHub) || (node.child == null)) {
                        HubMerger.this.beforeInsertRealHub(e);
                    }
                    /*was
                    Class c = obj.getClass();
                    if (c.equals(hubCombined.getObjectClass()) || OAObject.class.equals(hubCombined.getObjectClass())) {
                        HubMerger.this.beforeInsertRealHub(e);
                    }
                    */
                }
            } finally {
                if (b) {
                    srvcOAThreadLocal.setHubMergerChanging(false);
                }
            }
        }

        /**
         * Handles post-insert logic, including notifying HubMerger hooks and delegating
         * to afterAdd2 to create new child Data nodes.
         *
         * @param e the HubEvent describing the insertion
         */
        @Override 
        public void afterInsert(HubEvent e) {
            final boolean b = (hub == hubRoot);
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
            try {
                if (b) {
                    srvcOAThreadLocal.setHubMergerChanging(true);
                }
                Object obj = e.getObject();
                if (obj != null) {
                    //20150622
                    if ((node == nodeRoot && bIncludeRootHub) || (node.child == null)) {
                        HubMerger.this.afterInsertRealHub(e);
                    }
                    /*was
                    Class c = obj.getClass();
                    if (c.equals(hubCombined.getObjectClass()) || OAObject.class.equals(hubCombined.getObjectClass())) {
                        HubMerger.this.afterInsertRealHub(e);
                    }
                    */
                }
                afterAdd2(e);
            } finally {
                if (b) {
                    srvcOAThreadLocal.setHubMergerChanging(false);
                }
            }
        }

        /**
         * Handles move events originating from the underlying Hub. Notifies HubMerger
         * hooks when the movement involves objects that appear in the combined Hub.
         *
         * @param e the HubEvent describing the move
         */
        @Override
        public void afterMove(HubEvent e) {
            //20150622
            if ((node == nodeRoot && bIncludeRootHub) || (node.child == null)) {
                HubMerger.this.afterMoveRealHub(e);
            }
            /*
            Hub h = e.getHub();
            if (h != null && h.getObjectClass().equals(hubCombined.getObjectClass())) {
                HubMerger.this.afterMoveRealHub(e);
            }
            */
        }

        /**
         * Intercepts property-change events, applying server-side messaging rules before
         * delegating to the internal handler for traversal updates.
         *
         * @param e the HubEvent representing the property change
         */
        public @Override void afterPropertyChange(HubEvent e) {
			final OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();  
            try {
                if (bServerSideOnly) {
                    srvcOARemoteThread.sendMessages(true);
                }
                _afterPropertyChange(e);
            } finally {
                if (bServerSideOnly) {
                    srvcOARemoteThread.sendMessages(false);
                }
            }
        }

        /**
         * Core property-change handler. Evaluates whether the change affects the current
         * Data node, updates child links accordingly, and propagates structure updates.
         *
         * @param e the HubEvent describing the property update
         */
       void _afterPropertyChange(HubEvent e) {
            if (!bEnabled) {
                return;
            }
            if (node.child == null) {
                return; // last nodes
            }
            String prop = e.getPropertyName();
            if (prop == null) {
                return;
            }

            // 20160806
            //was: if (node.child.liFromParentToChild.getType() != OALinkInfo.ONE) return;
            if (!node.child.liFromParentToChild.getName().equalsIgnoreCase(prop)) {
                return;
            }

            // 20110324 data might not have been created,
            if (node.child.data == null) {
                return;
            }

            // 20160806 could be a calculated many link
            if (node.child.liFromParentToChild.getType() == OALinkInfo.MANY) {
                if (!node.child.liFromParentToChild.getCalculated()) {
                    return;
                }
                Object objx = e.getObject();
                if (!(objx instanceof OAObject)) {
                    return;
                }
                // calling the method will cause the hub to be updated
                node.child.liFromParentToChild.getValue((OAObject) objx);
                return;
            }

            if (this == dataRoot && !bUseAll) {
                if (e.getObject() != hubRoot.getAO()) {
                    return;
                }
            }

            Object ref = e.getOldValue();
            if (ref != null) {
                if (!isUsed(ref, node.child)) {
                    node.child.data.hub.remove(ref);
                }
            }

            ref = e.getNewValue();
            if (ref != null) {
                if (!node.child.data.hub.contains(ref)) {
                    // 20200407 added siblingHelper
                    final OASiblingHelper sh = getSiblingHelper();
        			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
                    boolean bx = srvcOAThreadLocal.addSiblingHelper(sh);
                    try {
                        node.child.data.hub.add(ref);
                    } finally {
                        if (bx) {
                            srvcOAThreadLocal.removeSiblingHelper(sh);
                        }
                    }
                }
            }
        }

       /**
        * Handles changes to the active object (AO) of the root Hub when the HubMerger
        * is configured to refresh based on AO changes. Only executes when this Data
        * node represents the root of the traversal chain.
        *
        * <p>If bUseAll is false, the method ensures that updates only occur when the
        * AO itself changes. When bUseAll is true, refreshes occur only if the flag
        * bRefreshOnActiveObjectChange is enabled.</p>
        *
        * <p>If the new AO is the same as the currently loaded child object's parent,
        * the refresh step is skipped unless the AO is null (which forces a rebuild).</p>
        *
        * <p>When a refresh is required, the method triggers a list rebuild by invoking
        * the internal _onNewList() routine.</p>
        *
        * @param evt the HubEvent describing the active-object change
        */
        public @Override void afterChangeActiveObject(HubEvent evt) {
            // only need for hubRoot when bUseAll=false, so that it can load the AO nodes
            if (!bEnabled) {
                return;
            }
            if (this != dataRoot || this.node != nodeRoot) {
                return;
            }
            if (bUseAll) {
                if (!bRefreshOnActiveObjectChange) {
                    return;
                }
            }

            // if the AO is the same, then this can be skipped
            if (evt != null && alChildren != null && alChildren.size() > 0) {
                Data d = alChildren.get(0);
                if (d == null || d.parentObject == evt.getObject()) {
                    if (evt.getObject() != null) { // 20201222 could be repopulating based on parent/masterObject and PP using parent
                        return;
                    }
                }
            }

            _onNewList();
        }

        /**
         * Handles load events from the underlying Hub. When enabled, this method updates
         * the Data tree to reflect newly loaded objects.
         *
         * <p>If this Data node represents the root of the traversal and bUseAll is false,
         * the load event is ignored unless the loaded object matches the root Hub’s
         * active object.</p>
         *
         * <p>The method temporarily marks the thread as performing HubMerger-related
         * changes, removes the previously loaded object from the Data tree, and then
         * recreates child Data nodes for the newly loaded object.</p>
         *
         * @param e the HubEvent representing the load of a new object
         */
        @Override
        public void afterLoad(HubEvent e) {
            if (!bEnabled) {
                return;
            }

            if (this == dataRoot && !bUseAll) {
                if (e.getObject() != hubRoot.getAO()) {
                    return;
                }
            }
            final boolean b = (hub == hubRoot);
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
            try {
                if (b) {
                    srvcOAThreadLocal.setHubMergerChanging(true);
                }
                OAObject obj = (OAObject) e.getObject();
                remove(obj);
                createChild(obj);
            } finally {
                if (b) {
                    srvcOAThreadLocal.setHubMergerChanging(false);
                }
            }
        }
    }

    /**
     * Callback invoked when an object is added to the combined Hub as a result of
     * traversal expansion. The default implementation performs no action but exists
     * for subclasses to override when custom behavior is needed.
     *
     * @param data the Data node responsible for adding the object
     * @param obj  the object added to the combined Hub
     */
    protected void onAddToCombined(Data data, OAObject obj) {
    }

    /**
     * Callback invoked when an object is removed from the combined Hub during
     * traversal contraction or restructuring. The default implementation performs
     * no action but can be overridden by subclasses to provide custom behavior.
     *
     * @param data the Data node responsible for the removal
     * @param obj  the object removed from the combined Hub
     */
    protected void onRemoveFromCombined(Data data, OAObject obj) {
    }

    /**
     * Returns whether the HubMerger processes all objects in the root Hub instead
     * of only its active object.
     *
     * @return true if all root Hub objects are used; false otherwise
     */
    public boolean getUseAll() {
        return bUseAll;
    }

    /**
     * Indicates whether the HubMerger should refresh its structure when the active
     * object of the root Hub changes.
     *
     * @return true if a refresh should occur on AO change; false otherwise
     */
    public boolean getRefreshOnActiveObjectChange() {
        return bRefreshOnActiveObjectChange;
    }

    /**
     * Enables or disables refresh behavior when the root Hub’s active object
     * changes.
     *
     * @param b true to refresh when the active object changes; false to disable
     */
    public void setRefreshOnActiveObjectChange(boolean b) {
        this.bRefreshOnActiveObjectChange = b;
    }

    /**
     * Shared executor service used by HubMerger to execute background tasks such as
     * asynchronous onNewList processing. Lazily initialized by getExecutorService().
     */
    private static volatile ExecutorService executorService;

    /**
     * Counter used to generate unique names for background worker threads created by
     * the HubMerger's executor service.
     */
    private static final AtomicInteger aiThreadCnt = new AtomicInteger();

    /**
     * Lazily initializes and returns the shared ExecutorService used for background
     * operations such as onNewList processing. Creates a cached thread pool using a
     * custom ThreadFactory that produces MyThread instances.
     *
     * @return the ExecutorService used by HubMerger for asynchronous processing
     */
    protected static ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = Executors.newCachedThreadPool(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new MyThread(r, "HubMerger." + aiThreadCnt.getAndIncrement());
                    t.setDaemon(true);
                    t.setPriority(Thread.MIN_PRIORITY);
                    return t;
                }
            });
        }
        return executorService;
    }

    /**
     * Indicates whether the combined Hub is currently being populated or refreshed.
     * Uses an internal counter to determine whether a load operation is active.
     *
     * @return true if the combined Hub is in a loading state; false otherwise
     */
    public boolean isLoadingCombinedHub() {
        return aiLoadingCombinedHub.get() > 0;
    }

    /**
     * Custom thread implementation used for HubMerger background tasks. Tracks the
     * most recent onNewList counter to detect whether the thread's work has become
     * outdated.
     */
    private static class MyThread extends Thread {
    	/**
    	 * Stores the onNewList counter value assigned to this thread when it begins
    	 * processing. Used to determine whether newer list operations supersede this
    	 * thread’s work.
    	 */
        int cntNewList;

        /**
         * Constructs a new MyThread instance configured for HubMerger background
         * processing. Assigns the runnable task and thread name, and delegates to
         * the superclass Thread constructor.
         *
         * @param r    the runnable task to execute
         * @param name the name assigned to this worker thread
         */
        public MyThread(Runnable r, String name) {
            super(r, name);
        }
    }

}
