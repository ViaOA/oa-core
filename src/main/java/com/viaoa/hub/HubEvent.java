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

import java.util.logging.Logger;

import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.log.OALogger;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.runtime.OARuntime;

/*qqqqqqqqqqqqqq
CODEX

OA listeners ??
   short curcuit policy
      only for "before" events
      others need to catch exception and call all listeners


 */



/**
 * qqqqqqqqqqqqqqqqqqqqqqqqqqqq dropped "extends PropertyChangeEvent"
 * 
 * Single event type used by {@link Hub} and {@link com.viaoa.object.OAObject} to
 * represent structural and property-change activity throughout the OA graph.
 * <p>
 * Extends {@link java.beans.PropertyChangeEvent} and adds Hub-specific context:
 * current object, positional info (pos/from/to), cancel/response flags, and
 * multiple constructors for add/insert/remove/move/replace/property-change cases.
 * <p>
 * For link properties persisted as {@link com.viaoa.object.OAObjectKey}, {@link #getOldValue()}
 * resolves the prior reference to its real {@code OAObject} using link metadata, so
 * listeners always see object instances (not keys) for reverse-link scenarios.
 * <p>
 * Typical producers: Hub add/remove/move/AO changes, OAObject property updates.
 * Typical consumers: {@link HubListener} implementations across UI, caching, and sync.
 */
public class HubEvent<TYPE extends OAObject> {
	private static final Logger LOG = OALogger.getLogger(HubEvent.class);
	
	Hub<TYPE> hub;
	
	/**
	 * The object associated with this event, such as the added,
	 * removed, moved, or property-changed object.
	 */
	TYPE object;

	TYPE object2;
	
	private Object oldValue;
	
	/**
	 * Cached resolved old-value object used when translating an
	 * {@link OAObjectKey} into its corresponding {@link OAObject}
	 * instance for reverse-link property changes.
	 */
	private Object oldValue2;

	private Object newValue;
	
	private String propertyName;

	/**
	 * Positional indices used for add/insert/remove and move events.
	 * {@code pos} is the original or associated position;
	 * {@code toPos} is the destination position for move events.
	 */
	int pos, toPos;
	
	/**
	 * Flag indicating whether this event has been marked as canceled,
	 * preventing further processing.
	 */
	boolean bCancel;
	
	/**
	 * Optional response text that listeners can set to return
	 * information back to the event producer.
	 */
	String response;

	
	
	/**
	 * Internal counter used for debug tracing in the optional
	 * p(String) diagnostic helper method.
	 */
	static int cnt = 0;

	
	/**
	 * Creates a HubEvent with the given Hub as its source and no associated
	 * object or position information.
	 *
	 * @param source the Hub generating the event
	 */
	public HubEvent(Hub<TYPE> hub) {
		this.hub = hub;
	}
	
	
	/**
	 * Creates a HubEvent for an add event where an object is added to a Hub.
	 *
	 * @param hub the Hub generating the event
	 * @param obj the object added
	 */
	public HubEvent(Hub<TYPE> hub, TYPE obj) {
		this.hub = hub;
		this.object = obj;
	}

	/**
	 * Creates a HubEvent associated directly with an object that is not
	 * tied to a positional Hub operation.
	 *
	 * @param obj the object associated with the event
	 */
	public HubEvent(TYPE obj) {
		this.object = obj;
	}
	
	
	/**
	 * Creates a HubEvent for a property-change originating from a Hub.
	 *
	 * @param source        the Hub generating the event
	 * @param obj           the object whose property changed
	 * @param propertyName  the name of the changed property
	 * @param oldValue      the previous property value
	 * @param newValue      the new property value
	 */
	public HubEvent(Hub<TYPE> hub, TYPE obj, String propertyName, Object oldValue, Object newValue) {
		this.hub = hub;
		this.object = obj;
		this.propertyName = propertyName;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}
	
	
	/**
	 * Creates a HubEvent for a property-change originating directly from
	 * an object rather than a Hub.
	 *
	 * @param obj           the object whose property changed
	 * @param propertyName  the name of the changed property
	 * @param oldValue      the previous property value
	 * @param newValue      the new property value
	 */
	public HubEvent(TYPE obj, String propertyName, Object oldValue, Object newValue) {
		this(null, obj, propertyName, oldValue, newValue);
	}

	/**
	 * Creates a HubEvent for a Hub replace operation where one object is
	 * substituted with another.
	 *
	 * @param source   the Hub generating the event
	 * @param oldValue the object being replaced
	 * @param newValue the replacement object
	 */
	public HubEvent(Hub<TYPE> hub, TYPE object, TYPE object2) {
		this.hub = hub;
		this.object = object;
		this.object2 = object2;
	}

	public HubEvent(Hub<TYPE> hub, TYPE obj, String propertyName) {
		this.hub = hub;
		this.object = obj;
		this.propertyName = propertyName;
	}
	
	
	/**
	 * Creates a HubEvent representing a move operation where an object is
	 * moved from one position to another within the Hub.
	 *
	 * @param source  the Hub generating the event
	 * @param posFrom the original position
	 * @param posTo   the new position
	 */
	public HubEvent(Hub<TYPE> hub, int posFrom, int posTo) {
		this.hub = hub;
		this.pos = posFrom;
		this.toPos = posTo;
	}

	/**
	 * Creates a HubEvent for an insert operation or a positional add/remove
	 * event involving the specified object and position.
	 *
	 * @param source the Hub generating the event
	 * @param obj    the object inserted or affected
	 * @param pos    the position associated with the event
	 */
	public HubEvent(Hub<TYPE> hub, TYPE obj, int pos) {
		this.hub = hub;
		this.object = obj;
		this.pos = pos;
	}


	
	
	public Hub<TYPE> getHub() {
		return hub;
	}
	

	/**
	 * Returns the object associated with this event.
	 *
	 * @return the event object
	 */
	public TYPE getObject() {
		return object;
	}

	public TYPE getObject2() {
		return object2;
	}
	
	public String getPropertyName() {
		return propertyName;
	}
	
	public Object getNewValue() {
		return newValue;
	}
	
	
	/**
	 * Returns the old value associated with this event. If the old value is
	 * an {@link OAObjectKey} and the event object is an {@link OAObject},
	 * attempts to resolve the key to its corresponding object instance using
	 * link metadata.
	 *
	 * @return the resolved old value
	 */
	@SuppressWarnings("unchecked")
	public Object getOldValue() {
		if (oldValue2 != null) {
			return oldValue2;
		}
		Object oldObj = oldValue;
		boolean bError = false;
		if (oldObj instanceof OAObjectKey && object instanceof OAObject) {
			OAGraphInternal og = (OAGraphInternal) OARuntime.graph(object);
			OAObjectInfo oi = og.objectsInternal().callObjectInfoGetOAObjectInfo(object);
			if (oi != null) {
				OALinkInfo li = og.objectsInternal().callObjectInfoGetLinkInfo(oi, getPropertyName());
				if (li != null) {
					og = (OAGraphInternal) OARuntime.graph(li.getToClass());
					oldObj = og.objectsInternal().callObjectReflectGetObject((Class<TYPE>) li.getToClass(), (OAObjectKey) oldObj);
					oldValue2 = oldObj;
				} else {
					bError = true;
				}
			} else {
				bError = true;
			}
		}
		if (bError) {
			LOG.warning("HubEvent.getOldValue() not finding Object for OAObjectKey: " + oldObj + ", object=" + object + ", prop="+ getPropertyName());
		}
		return oldObj;
	}
	
	
	
	/**
	 * Returns the position associated with this event. Used for add,
	 * insert, remove, and active-object events.
	 *
	 * @return the position value
	 */
	public int getPos() {
		return pos;
	}

	/**
	 * Returns whether this event has been flagged for cancellation.
	 *
	 * @return true if the event is canceled, otherwise false
	 */
	public boolean getCancel() {
		return bCancel;
	}

	/**
	 * Sets the cancel flag for this event. Used internally by Hub
	 * processing to stop further event propagation.
	 *
	 * @param b true to cancel the event
	 */
	void setCancel(boolean b) {
		bCancel = b;
	}

	/**
	 * Returns the original position of an object for move events.
	 *
	 * @return the source position
	 */
	public int getFromPos() {
		return pos;
	}

	/**
	 * Returns the destination position for move events.
	 *
	 * @return the target position
	 */
	public int getToPos() {
		return toPos;
	}



	/**
	 * Sets the response string for this event. Can be used by listeners to
	 * communicate information back to the event producer.
	 *
	 * @param response the response text
	 */
	public void setResponse(String response) {
		this.response = response;
	}

	/**
	 * Returns the response string that was set for this event.
	 *
	 * @return the response text, or null if none was set
	 */
	public String getResponse() {
		return this.response;
	}

	/**
	 * Returns whether the given property name matches the property name
	 * associated with this event.
	 *
	 * @param name the property name to test
	 * @return true if the names match, otherwise false
	 */
	public boolean isProperty(String name) {
		if (name == null) {
			return false;
		}
		return name.equalsIgnoreCase(getPropertyName());
	}
	
	/**
	 * Internal debug helper that prints an event trace message every tenth
	 * invocation. Used for monitoring event activity during development.
	 *
	 * @param s the message to print
	 */
	void p(String s) {
		if ((cnt % 10) == 0) {
			System.out.println("Event =========> " + (++cnt) + " " + s);
		}
	}
	
}
