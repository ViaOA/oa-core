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

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Vector;

import com.viaoa.datasource.OASelect;
import com.viaoa.hub.HubData.FriendAccess;
import com.viaoa.object.OAObjectInfo;

/**
 * Optional extended state for a {@link HubData} instance.
 * 
 * <p>Contains auxiliary members that are only allocated when needed, to keep
 * base HubData lightweight.</p>
 *
 * <p>Includes:</p>
 * <ul>
 *   <li>Sorting (property, direction, listener)</li>
 *   <li>{@link OASelect} query linkage and refresh flags</li>
 *   <li>Unique-property and duplication control</li>
 *   <li>Change tracking vectors (add/remove)</li>
 *   <li>Metadata cache ({@link OAObjectInfo}) and custom properties</li>
 *   <li>Auto-sequence and auto-match delegates</li>
 *   <li>Where-Hub linkage for select filtering</li>
 * </ul>
 *
 * <p>Provides {@code isNeeded()} and {@code shouldSerialize()} predicates to
 * prune serialization footprint.</p>
 */
public class HubDatax implements java.io.Serializable {
	static final long serialVersionUID = 1L; // used for object serialization

	/**
	 * Determines whether extended HubData state is required.  
	 * Returns true if any optional fields—such as sorting configuration, unique-property
	 * settings, change-tracking vectors, listeners, selection state, refresh flag,
	 * cached properties, select order, auto-sequence/match delegates, or a where-hub
	 * reference—have been set or contain data.
	 *
	 * @return true if extended state is in use; otherwise false
	 */
	public boolean isNeeded() {
		if (sortProperty != null) {
			return true;
		}
		if (!sortAsc) {
			return true;
		}
		if (uniqueProperty != null) {
			return true;
		}

		if (vecAdd != null && vecAdd.size() > 0) {
			return true;
		}
		if (vecRemove != null && vecRemove.size() > 0) {
			return true;
		}
		if (sortListener != null) {
			return true;
		}
		if (select != null && select.hasMore()) {
			return true;
		}
		if (refresh) {
			return true;
		}
		if (hashProperty != null && hashProperty.size() > 0) {
			return true;
		}
		if (selectOrder != null) {
			return true;
		}
		if (autoSequence != null) {
			return true;
		}
		if (autoMatch != null) {
			return true;
		}
		if (selectWhereHub != null) {
			return true;
		}
		return false;
	}

	/**
	 * Indicates whether this extended HubData state should be serialized.  
	 * Serialization is required when sorting configuration, sorting direction,
	 * unique-property settings, or change-tracking are enabled.
	 *
	 * @return true if this state should be serialized; otherwise false
	 */
	public boolean shouldSerialize() {
		if (sortProperty != null) {
			return true;
		}
		if (!sortAsc) {
			return true;
		}
		if (uniqueProperty != null) {
			return true;
		}
		if (bTrackChanges) {
			return true;
		}
		return false;
	}

	/**
	 * Counter that is incremented when a new list of objects is loaded. Incremented by select, setSharedHub, and when detail hubs list is
	 * changed to match the master hub's activeObject.<br>
	 * This can be used to know if a hub has been changed without requiring the set up of a HubListener.
	 * <p>
	 * This is used by JSP components to know if a frame should be updated. <br>
	 * See com.viaoa.html.OATable and com.viaoa.html.OANav
	 */
	// protected transient int newListCount;

	/**
	 * Tracks objects added to the Hub when change tracking is enabled.
	 */
	protected transient Vector vecAdd;

	/**
	 * Tracks objects removed from the Hub when change tracking is enabled.
	 */
	protected transient Vector vecRemove;

	/**
	 * Listener responsible for maintaining sorted order when sorting
	 * is enabled for this Hub.
	 */
	protected transient HubSortListener sortListener;
	
	//  info to keep Hub objects sorted when sent to other computers, see HubSerializerDelegate._readResolve - it will set up sorting when received
	
	/**
	 * Name of the property used to sort Hub objects; defaults to the
	 * sort property defined by link information.
	 */
	protected String sortProperty; // defaults to linkInfo.sortProperty

	/**
	 * Indicates whether sorting is in ascending order; defaults to true.
	 */
	protected boolean sortAsc = true;

	/**
	 * Select query used to populate or filter objects loaded from a data source.
	 */
	protected transient OASelect select;

	/**
	 * Flag indicating whether active objects should always be refreshed
	 * from the data source; currently not implemented.
	 */
	protected boolean refresh = false;

	/**
	 * Name of the property that must hold a unique value among all
	 * objects in the Hub.
	 */
	protected String uniqueProperty;

	/**
	 * Getter method for retrieving the value of the unique property.
	 */
	protected transient Method uniquePropertyGetMethod;

	/**
	 * Indicates whether this HubDatax state is disabled for processing.
	 */
	protected transient boolean disabled;

	/**
	 * Identifies whether this Hub stores OAObject instances.
	 */
	protected boolean oaObjectFlag;

	/**
	 * Determines whether objects can be added or removed; becomes false
	 * when detail Hubs originate from arrays or non-Hub sources.
	 */
	protected boolean dupAllowAddRemove = true;

	/**
	 * Cached metadata describing the OAObject type stored in this Hub.
	 */
	protected transient OAObjectInfo objectInfo;

	/**
	 * Case-insensitive map of arbitrary name/value properties associated
	 * with this HubDatax instance.
	 */
	protected Hashtable hashProperty;

	/**
	 * Property path(s) specifying alternate ordering for select operations.
	 */
	protected String selectOrder;

	/**
	 * Delegate responsible for maintaining a property that matches
	 * an object's position within the Hub.
	 */
	protected transient HubAutoSequence autoSequence;

	/**
	 * Delegate ensuring the Hub contains an associated object for each
	 * object found in another Hub.
	 */
	protected transient HubAutoMatch autoMatch;

	/**
	 * Flag indicating whether add, insert, and remove operations should
	 * be tracked for this Hub.
	 */
	protected boolean bTrackChanges;

	/**
	 * Hub used for OASelect.whereHub filtering when selecting objects
	 * from a data source.
	 */
	protected transient Hub selectWhereHub;

	/**
	 * Property path from the selectWhereHub to this Hub, defining how
	 * related objects are navigated for select filtering.
	 */
	protected transient String selectWhereHubPropertyPath;

	public static final class FriendAccess {
		private FriendAccess() {
		}
	}

	private final static FriendAccess friendAccess = new FriendAccess();
	static FriendAccess getFriendAccess() {
		return friendAccess;
	}
}
