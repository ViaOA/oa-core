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
import java.util.List;

import com.viaoa.graph.OAGraph;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OACompare;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAReflect;
import com.viaoa.util.OAString;

/**
 * Provides link-based synchronization between two {@link Hub} instances.
 *
 * <p><b>Purpose</b> – to wire one Hub's objects to another via reference
 * properties, maintaining automatic synchronization between Active Objects.
 *
 * <p><b>Responsibilities</b>
 * <ul>
 *   <li>Resolve link metadata via {@link OALinkInfo} and reflection.</li>
 *   <li>Set up bidirectional or positional link behavior.</li>
 *   <li>Attach and detach listeners to propagate reference changes.</li>
 *   <li>Validate link integrity (class compatibility, existing links, recursion).</li>
 * </ul>
 *
 * <p>Used by {@code Hub.setLinkHub()} to create the reactive connection between
 * Hubs for reference synchronization.
 */
public class HubLinkDelegate {
	
	/*
	OAGraph g = getGraph(hub, null);
	if (g == null) return;
	g.hubs().getHubLinkService().?(?);

	OAGraph g = OARuntime.get().graph(c);
	if (g == null) return;
	g.hubs().getHubLinkService().?(?);
    */
	static OAGraph getGraph(Hub hub, OAObject obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
	}
	
	
	/**
	 * Configures this Hub to link to another Hub using the specified reference properties.
	 * <p>
	 * Sets link metadata, resolves getter/setter methods, validates class compatibility,
	 * installs event listeners, and performs initial synchronization of Active Objects.
	 *
	 * @param thisHub               the Hub establishing the link
	 * @param propertyFrom          reference property name from thisHub's objects
	 * @param linkToHub             the Hub to link to
	 * @param propertyTo            reference property name in linkToHub's objects
	 * @param linkPosFlag           true if linking based on positional index
	 * @param bAutoCreate           true to auto-create linked objects
	 * @param bAutoCreateAllowDups  true to allow duplicates when auto-creating objects
	 */
	protected static void setLinkHub(Hub thisHub, String propertyFrom, Hub linkToHub, String propertyTo, boolean linkPosFlag,
			boolean bAutoCreate, boolean bAutoCreateAllowDups) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubLinkService().setLinkHub(thisHub, propertyFrom, linkToHub, propertyTo, linkPosFlag, bAutoCreate, bAutoCreateAllowDups);
	}

	/**
	 * Determines whether auto-create mode is enabled for this Hub's link.
	 *
	 * @param thisHub the Hub being checked
	 * @return true if auto-create is enabled; otherwise false
	 */
	public static boolean isLinkAutoCreated(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubLinkService().isLinkAutoCreated(thisHub);
	}

	/**
	 * Determines whether auto-create mode is enabled for the Hub or any shared Hub.
	 *
	 * @param thisHub            the Hub being checked
	 * @param bIncludeCopiedHubs true to also check shared/copied Hubs
	 * @return true if auto-create is enabled; otherwise false
	 */
	public static boolean isLinkAutoCreated(final Hub thisHub, boolean bIncludeCopiedHubs) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubLinkService().isLinkAutoCreated(thisHub, bIncludeCopiedHubs);
	}

	/**
	 * Determines whether this Hub is linked using positional index.
	 *
	 * @param thisHub the Hub to examine
	 * @return true if linked by position; otherwise false
	 */
	public static boolean getLinkedOnPos(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubLinkService().getLinkedOnPos(thisHub);
	}

	/**
	 * Determines whether this Hub or any shared Hub uses positional linking.
	 *
	 * @param thisHub            the Hub to examine
	 * @param bIncludeCopiedHubs true to evaluate copied/shared Hubs
	 * @return true if positional linking is active; otherwise false
	 */
	public static boolean getLinkedOnPos(final Hub thisHub, boolean bIncludeCopiedHubs) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubLinkService().getLinkedOnPos(thisHub, bIncludeCopiedHubs);
	}

	/**
	 * Updates the linked-to property for the active object based on changes
	 * from the linked-from Hub.
	 *
	 * @param thisHub    the Hub owning the linked property
	 * @param fromObject the source object whose value is being applied
	 * @param pos        location index when linking by position
	 */
	public static void updateLinkProperty(Hub thisHub, Object fromObject, int pos) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubLinkService().updateLinkProperty(thisHub, fromObject, pos);
	}


	/**
	 * Retrieves the value of the linked-to property for the given object.
	 *
	 * @param thisHub    the Hub whose linking configuration defines the lookup
	 * @param linkObject the object whose linked property value is requested
	 * @return the linked property value, or null if none
	 */
	public static Object getPropertyValueInLinkedToHub(Hub thisHub, Object linkObject) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubLinkService().getPropertyValueInLinkedToHub(thisHub, linkObject);
	}

	/**
	 * Retrieves the property name used as the link-to target.
	 *
	 * @param thisHub the Hub whose link property is requested
	 * @return the link-to property name, or null if none
	 */
	public static String getLinkToProperty(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubLinkService().getLinkToProperty(thisHub);
	}

	/**
	 * Retrieves the link-to property name from this Hub or shared Hubs.
	 *
	 * @param thisHub            the Hub to inspect
	 * @param bIncludeCopiedHubs true to evaluate copied/shared Hubs
	 * @return the link-to property name, or null if none
	 */
	public static String getLinkToProperty(final Hub thisHub, boolean bIncludeCopiedHubs) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubLinkService().getLinkToProperty(thisHub, bIncludeCopiedHubs);
	}

	/**
	 * Retrieves the property name used as the link-from reference.
	 *
	 * @param thisHub the Hub whose link-from property is requested
	 * @return the link-from property name, or null if none
	 */
	public static String getLinkFromProperty(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubLinkService().getLinkFromProperty(thisHub);
	}

	/**
	 * Retrieves the link-from property name from this Hub or shared Hubs.
	 *
	 * @param thisHub            the Hub to inspect
	 * @param bIncludeCopiedHubs true to inspect copied/shared Hubs
	 * @return the link-from property name, or null if none
	 */
	public static String getLinkFromProperty(final Hub thisHub, boolean bIncludeCopiedHubs) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubLinkService().getLinkFromProperty(thisHub, bIncludeCopiedHubs);
	}

	/**
	 * Retrieves the Hub that this Hub is linked to, optionally searching shared Hubs.
	 *
	 * @param thisHub            the Hub whose link target is requested
	 * @param bIncludeCopiedHubs true to include shared/copied Hubs
	 * @return the linked-to Hub, or null if none
	 */
	public static Hub getLinkToHub(final Hub thisHub, boolean bIncludeCopiedHubs) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubLinkService().getLinkToHub(thisHub, bIncludeCopiedHubs);
	}

	/**
	 * Retrieves the Hub that this Hub links to.
	 *
	 * @param thisHub            the Hub to check
	 * @param bIncludeCopiedHubs true to check shared/copied Hubs
	 * @return the linked-to Hub, or null
	 */
	public static Hub getHubWithLink(final Hub thisHub, boolean bIncludeCopiedHubs) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubLinkService().getHubWithLink(thisHub, bIncludeCopiedHubs);
	}

	/**
	 * Determines whether the Hub is linked using position-based linking.
	 *
	 * @param thisHub the Hub to examine
	 * @return true if linking by position; otherwise false
	 */
	public static boolean getLinkHubOnPos(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubLinkService().getLinkHubOnPos(thisHub);
	}

	/**
	 * Determines whether this Hub or any shared Hub uses position-based linking.
	 *
	 * @param thisHub            the Hub to inspect
	 * @param bIncludeCopiedHubs true to include copied/shared Hubs
	 * @return true if any Hub uses positional linking; otherwise false
	 */
	public static boolean getLinkHubOnPos(final Hub thisHub, boolean bIncludeCopiedHubs) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubLinkService().getLinkHubOnPos(thisHub, bIncludeCopiedHubs);
	}

	/**
	 * Retrieves the setter method used to apply linked property values.
	 *
	 * @param thisHub the Hub whose setter method is requested
	 * @return the link-to setter method, or null if none configured
	 */
	public static Method getLinkSetMethod(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubLinkService().getLinkSetMethod(thisHub);
	}

	/**
	 * Retrieves the link-to setter method from this Hub or, optionally, any
	 * shared/copied Hub.
	 *
	 * @param thisHub            the Hub whose setter method is examined
	 * @param bIncludeCopiedHubs true to include shared/copied Hubs
	 * @return the link-to setter method, or null if none found
	 */
	public static Method getLinkSetMethod(final Hub thisHub, boolean bIncludeCopiedHubs) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubLinkService().getLinkSetMethod(thisHub, bIncludeCopiedHubs);
	}

	/**
	 * Retrieves the getter method used to obtain values for link updates.
	 *
	 * @param thisHub the Hub whose getter method is requested
	 * @return the link-to getter method, or null if none configured
	 */
	public static Method getLinkGetMethod(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubLinkService().getLinkGetMethod(thisHub);
	}

	/**
	 * Retrieves the getter method used to resolve link values, optionally checking
	 * shared/copied Hubs.
	 *
	 * @param thisHub            the Hub being examined
	 * @param bIncludeCopiedHubs true to include shared/copied Hubs
	 * @return the getter method, or null if not found
	 */
	public static Method getLinkGetMethod(final Hub thisHub, boolean bIncludeCopiedHubs) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubLinkService().getLinkGetMethod(thisHub, bIncludeCopiedHubs);
	}

	/**
	 * Retrieves the property path for the link-to property.
	 *
	 * @param thisHub the Hub whose link path is requested
	 * @return the link-to property path, or null if none set
	 */
	public static String getLinkHubPath(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubLinkService().getLinkHubPath(thisHub);
	}

	/**
	 * Retrieves the link-to property path from this Hub or, optionally, any
	 * shared/copied Hubs.
	 *
	 * @param thisHub            the Hub to evaluate
	 * @param bIncludeCopiedHubs true to include shared/copied Hubs
	 * @return the link-to property path, or null if none exists
	 */
	public static String getLinkHubPath(final Hub thisHub, boolean bIncludeCopiedHubs) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubLinkService().getLinkHubPath(thisHub, bIncludeCopiedHubs);
	}

	/**
	 * Updates the from-Hub based on changes from the link-to Hub using its current
	 * link configuration.
	 *
	 * @param fromHub   the Hub receiving the update
	 * @param linkToHub the Hub providing the linked value
	 * @param obj       the new value used for updating
	 */
	protected static void updateLinkedToHub(Hub fromHub, Hub linkToHub, Object obj) {
		OAGraph g = getGraph(fromHub, null);
		if (g == null) return;
		g.hubs().getHubLinkService().updateLinkedToHub(fromHub, linkToHub, obj);
	}

	/**
	 * Performs a comprehensive update of the from-Hub when the linked-to Hub
	 * changes, handling recursive relationships, positional links, and cascaded
	 * master/detail adjustments.
	 *
	 * @param fromHub         the Hub being updated
	 * @param linkToHub       the Hub that initiated the update
	 * @param obj             the new value to apply
	 * @param changedPropName the property that triggered the update, or null
	 */
	protected static void updateLinkedToHub(final Hub fromHub, Hub linkToHub, Object obj, String changedPropName) {
		OAGraph g = getGraph(fromHub, null);
		if (g == null) return;
		g.hubs().getHubLinkService().updateLinkedToHub(fromHub, linkToHub, obj, changedPropName);
	}

}
