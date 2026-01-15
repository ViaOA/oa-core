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

import com.viaoa.graph.object.OAObjectInfoService;
import com.viaoa.graph.object.OAObjectReflectService;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OALogger;

/**
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
public class HubEvent<T> extends java.beans.PropertyChangeEvent {
	private static final Logger LOG = OALogger.getLogger(HubEvent.class);
	
	/**
	 * The object associated with this event, such as the added,
	 * removed, moved, or property-changed object.
	 */
	T object;
	
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
	 * Cached resolved old-value object used when translating an
	 * {@link OAObjectKey} into its corresponding {@link OAObject}
	 * instance for reverse-link property changes.
	 */
	private Object oldValue2;
	
	
	/**
	 * Internal counter used for debug tracing in the optional
	 * p(String) diagnostic helper method.
	 */
	static int cnt = 0;

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

	/**
	 * Creates a HubEvent for a property-change originating from a Hub.
	 *
	 * @param source        the Hub generating the event
	 * @param obj           the object whose property changed
	 * @param propertyName  the name of the changed property
	 * @param oldValue      the previous property value
	 * @param newValue      the new property value
	 */
	public HubEvent(Hub source, T obj, String propertyName, Object oldValue, Object newValue) {
		super(source, propertyName, oldValue, newValue);
		//p("1: propChange "+obj+" "+propertyName );
		this.object = obj;
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
	public HubEvent(T obj, String propertyName, Object oldValue, Object newValue) {
		super(obj, propertyName, oldValue, newValue);
		//p("2: propChange "+obj+" "+propertyName );
		this.object = obj;
	}

	/**
	 * Returns the Hub that generated this event, or null if the source is
	 * not a Hub.
	 *
	 * @return the Hub source, or null
	 */
	public Hub<T> getHub() {
		Object obj = getSource();
		if (obj instanceof Hub) {
			return (Hub<T>) obj;
		}
		return null;
	}

	/**
	 * Creates a HubEvent for a Hub replace operation where one object is
	 * substituted with another.
	 *
	 * @param source   the Hub generating the event
	 * @param oldValue the object being replaced
	 * @param newValue the replacement object
	 */
	public HubEvent(Hub<T> source, T oldValue, T newValue) {
		super(source, null, oldValue, newValue);
		//p("3: replace "+source.getObjectClass() );
		object = newValue;
	}

	/**
	 * Creates a HubEvent representing a move operation where an object is
	 * moved from one position to another within the Hub.
	 *
	 * @param source  the Hub generating the event
	 * @param posFrom the original position
	 * @param posTo   the new position
	 */
	public HubEvent(Hub<T> source, int posFrom, int posTo) {
		super(source, null, null, null);
		//p("4: move "+source.getObjectClass() );//qqqqqqqqqqq
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
	public HubEvent(Hub<T> source, T obj, int pos) {
		super(source, null, null, null);
		//p("5: add/insert/remove "+source.getObjectClass()+" "+obj );//qqqqqqqqqqq
		this.object = obj;
		this.pos = pos;
	}

	/**
	 * Creates a HubEvent for an add event where an object is added to a Hub.
	 *
	 * @param source the Hub generating the event
	 * @param obj    the object added
	 */
	public HubEvent(Hub<T> source, T obj) {
		this(source, obj, -1);
	}

	/**
	 * Creates a HubEvent associated directly with an object that is not
	 * tied to a positional Hub operation.
	 *
	 * @param obj the object associated with the event
	 */
	public HubEvent(T obj) {
		super(obj, null, null, null);
		this.object = obj;
	}

	/**
	 * Creates a HubEvent with the given Hub as its source and no associated
	 * object or position information.
	 *
	 * @param source the Hub generating the event
	 */
	public HubEvent(Hub<T> source) {
		this(source, null, -1);
	}

	/**
	 * Returns the object associated with this event.
	 *
	 * @return the event object
	 */
	public T getObject() {
		return (T) object;
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
	 * Returns the old value associated with this event. If the old value is
	 * an {@link OAObjectKey} and the event object is an {@link OAObject},
	 * attempts to resolve the key to its corresponding object instance using
	 * link metadata.
	 *
	 * @return the resolved old value
	 */
	@Override
	public Object getOldValue() {
		if (oldValue2 != null) {
			return oldValue2;
		}
		Object oldObj = super.getOldValue();
		boolean bError = false;
		if (oldObj instanceof OAObjectKey && object instanceof OAObject) {
			final OAObjectInfoService srvcObjectInfo = OARuntime.get().graph((OAObject) object).objects().getOAObjectInfoService();
			OAObjectInfo oi = srvcObjectInfo.getOAObjectInfo((OAObject) object);
			if (oi != null) {
				OALinkInfo li = srvcObjectInfo.getLinkInfo(oi, getPropertyName());
				if (li != null) {
					final OAObjectReflectService srvcOAObjectReflect = OARuntime.get().graph(li.getToClass()).objects().getOAObjectReflectService();
					oldObj = srvcOAObjectReflect.getObject(li.getToClass(), (OAObjectKey) oldObj);
					oldValue2 = oldObj;
				} else {
					bError = true;// else error qqqqqqq
				}
			} else {
				bError = true;// else error qqqqqqq
			}
		}
		//qqqqqqqqqqq
		if (bError) {
			LOG.warning("HubEvent.getOldValue() not finding Object for OAObjectKey: " + oldObj + ", object=" + object + ", prop="
					+ getPropertyName());
		}

		return oldObj;
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
}
