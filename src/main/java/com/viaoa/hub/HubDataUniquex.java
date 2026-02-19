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

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;

import com.viaoa.object.OAObject;


/**
 * Extended unique-state container referenced by {@link HubDataUnique}.
 * 
 * <p>Holds granular linkage, listener, and auto-create options for this Hub:
 * <ul>
 *   <li>Master–detail linkage (linkToHub, linkFromPropertyName, etc.)</li>
 *   <li>Listener tree and detail-hub vector</li>
 *   <li>Weak shared-hub registry</li>
 *   <li>Auto-creation flags for link targets</li>
 * </ul>
 *
 * <p>Used internally by Hub wiring and event propagation logic; never accessed
 * directly from public APIs.</p>
 */
class HubDataUniquex<T extends OAObject> implements java.io.Serializable {
    static final long serialVersionUID = 1L;  // used for object serialization
	

	/** these options are not enforced on OAObjects, they are used to flag options */
    //	boolean allowNew = true, allowDelete = true, allowEdit = true;
	
    /**
     * Default active-object position for newly created lists; -1 indicates
     * no default and results in a null active object unless explicitly set.
     */
	protected transient int defaultPos = -1;
	
	/**
	 * Flag indicating whether the active object should be set to null when
	 * it is removed from the Hub.
	 */
	protected transient boolean bNullOnRemove;
	
	/**
	    Hub Listeners that receives all Hub and OAObject events.
	    @see HubListener
	    @see HubEvent
	*/
	// protected transient Vector vecListener;
	
	// 20101218 replaces vetListener
	/**
	 * Listener tree that manages Hub and object-level event propagation
	 * for this Hub instance.
	 */
	protected transient volatile HubListenerTree listenerTree;
	
    
	
	/**
	 * Collection of HubDetail instances representing detail Hubs owned
	 * by this Hub.
	 */
	protected transient volatile Vector<HubDetail> vecHubDetail;
	
	/**
	    List of listeners for calculated properties.
	    The hub will automatically listen for changes to any property that a calculated property
	    is dependent on.
	    @see Hub#addListener(HubListener,String)
	*/
// 20101218 replaced by HubListenerTree	
//	transient Vector<HubCalcEventListener> calcEventListeners;
	
	
	/**
	 * The Hub to which this Hub is linked, defining the master side of
	 * an active-object linkage relationship.
	 */
	protected transient Hub<?> linkToHub;
	
	/**
	 * Indicates whether the link relationship uses positional linking
	 * instead of object-based linking.
	 */
	protected transient boolean linkPos;
	
	/**
	 * Property name on the link target object used for link-to operations.
	 */
	protected transient String linkToPropertyName;  // ex: hubDept linked to Emp on property  "dept"
	
	/**
	 * Getter method used to retrieve the link-to property value from the
	 * associated object.
	 */
	protected transient Method linkToGetMethod;     //     getDept()
	
	/**
	 * Setter method used to assign the link-to property value on the
	 * associated object.
	 */
	protected transient Method linkToSetMethod;     //     setDept()
	
	/**
	 * Property name on the link Hub used to update a corresponding
	 * property on the linked-to (master) Hub.
	 */
	protected transient String linkFromPropertyName;
	
	/**
	 * Getter method used to retrieve the link-from property value from
	 * the associated object.
	 */
	protected transient Method linkFromGetMethod;
	
	/**
	 * Listener responsible for synchronizing active-object changes between
	 * this Hub and its linked master Hub.
	 */
    protected transient HubLinkEventListener hubLinkEventListener;
	
    /**
     * Hub with which this Hub shares its data list; active object may be
     * shared depending on configuration.
     */
	protected transient Hub<T> sharedHub;
	
	
	/**
	    List of Hubs that are sharing the same objects as this Hub.  Each of these Hubs will
	    have the same HubData object.  If the active object is also being shared, then
	    the HubDataActive object will also be the same.
	*/
//	transient Vector vecSharedHub;

	// 20120715 replaces vecSharedHubs
	/**
	 * Weak-reference array tracking Hubs that share the same underlying
	 * data list as this Hub.
	 */
	protected transient volatile WeakReference<Hub<T>>[] weakSharedHubs;	
	
	
	/**
	 * Hub that receives the active object whenever this Hub's active object
	 * is changed, typically used for pick-list style behavior.
	 */
	protected transient Hub<T> addHub;

	/**
	 * Indicates whether a new object should be automatically created in
	 * the linked-to Hub when this Hub’s active object changes.
	 */
    protected transient boolean bAutoCreate;
    
    /**
     * Determines whether duplicates are allowed when auto-creating objects
     * in the link-to Hub.
     */
    protected transient boolean bAutoCreateAllowDups;

	public static final class FriendAccess {
		private FriendAccess() {
		}
	}

	private final static FriendAccess friendAccess = new FriendAccess();
	static FriendAccess getFriendAccess() {
		return friendAccess;
	}
}
