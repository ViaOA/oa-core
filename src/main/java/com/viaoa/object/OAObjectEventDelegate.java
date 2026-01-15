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
package com.viaoa.object;


import java.util.logging.Logger;

import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;

/**
 * Centralized event router for property changes on {@link OAObject}.
 * <p>
 * All mutation flows that affect object state, relationships, dirty flags,
 * hub membership, undo history, or distributed synchronization go through this
 * delegate to preserve global consistency and strict ordering rules.
 *
 * <h3>Primary Responsibilities</h3>
 * <ul>
 *   <li><b>Before / After Change Sequencing</b> — guarantees listener contract:
 *       before-change → mutation → after-change</li>
 *   <li><b>Loading-aware Mutations</b> — suppresses unnecessary events during
 *       lazy-load or server-side initialization without losing correctness</li>
 *   <li><b>Reference & Reverse Link Updates</b> — adjusts owning Hubs and
 *       reverse 1-1 or 1-many relationships without re-entrant storms</li>
 *   <li><b>Identity Impact Tracking</b> — routes ID changes to cache/index
 *       ensuring no drift or duplicate instances</li>
 *   <li><b>Distributed Sync Integration</b> — only propagates to server/clients
 *       when the object is authoritative and eligible for transmission</li>
 *   <li><b>Undo Support</b> — builds Undoable property edits when enabled</li>
 *   <li><b>Trigger Execution</b> — invokes model-level onChange callbacks safely</li>
 * </ul>
 *
 * <h3>Correctness Guarantees</h3>
 * <ul>
 *   <li>Events only fire for real state changes (except primitive wrappers where
 *       UI correctness requires notifications)</li>
 *   <li>No property change is allowed if violated by callbacks or metadata 
 *       constraints (unique, id, recursive-parent rules)</li>
 *   <li>Hub + reverse link changes occur <em>after</em> mutation but <em>before</em>
 *       triggers and distributed sync to ensure graph stability</li>
 *   <li>Thread-local context prevents recursive storms and misplaced sync</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * Application code does not call this class directly. Event emission is driven
 * automatically by {@link OAObject} and metadata-aware automation.
 *
 * @since OA 1.0
 * @see OAObject
 * @see OAObjectDelegate
 * @see OAObjectHubDelegate
 * @see OAObjectCacheDelegate
 */
public class OAObjectEventDelegate {

	private static Logger LOG = Logger.getLogger(OAObjectEventDelegate.class.getName());
	
	/*
	OAGraph g = getGraph(null, oaObj);
	if (g == null) return;
	g.objects().getOAObjectEventService().??(oaObj);
    */
	
	static OAGraph getGraph(Hub hub, OAObject obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		// if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
	}

	
	/**
	 * Entry point for emitting a before-change notification for a property.
	 * Performs initial null checks and equality checks, then delegates to the
	 * internal method that performs full validation and event routing.
	 *
	 * @param oaObj        object whose property is changing
	 * @param propertyName name of the property
	 * @param oldObj       previous value
	 * @param newObj       new value
	 * @param bLocalOnly   if true, suppresses cross-computer sync
	 * @param bSetChanged  if true, allows downstream logic to mark the object as changed
	 */
	private static void fireBeforePropertyChange(final OAObject oaObj, final String propertyName,
			Object oldObj, final Object newObj, final boolean bLocalOnly, final boolean bSetChanged) {
		//qqqqqqqq method was protected
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectEventService().fireBeforePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged);
	}



	/**
	 * Public entry point for emitting an after-change property event.
	 * Delegates to the full implementation with unknown-values disabled
	 * and reference-checking disabled.
	 *
	 * @param oaObj        object whose property changed
	 * @param propertyName name of the modified property
	 * @param oldObj       previous value
	 * @param newObj       new value
	 * @param bLocalOnly   if true, suppresses cross-computer sync
	 * @param bSetChanged  if true, allows flagging the object as changed
	 */
	private static void firePropertyChange(final OAObject oaObj, final String propertyName, Object oldObj, Object newObj,
			boolean bLocalOnly, boolean bSetChanged) {
		//qqqqqqqq method was protected
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectEventService().firePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged);
	}

	/**
	 * Convenience wrapper for emitting a property-change event with an optional
	 * unknown-values flag, delegating to the full implementation.
	 *
	 * @param oaObj          object whose property changed
	 * @param propertyName   name of the property
	 * @param oldObj         previous value
	 * @param newObj         new value
	 * @param bLocalOnly     if true, suppresses cross-computer sync
	 * @param bSetChanged    if true, allows flagging the object as changed
	 * @param bUnknownValues if true, skips some equality and load-state checks
	 */
	private static void firePropertyChange(final OAObject oaObj, final String propertyName, Object oldObj, Object newObj,
			boolean bLocalOnly, boolean bSetChanged, boolean bUnknownValues) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectEventService().firePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged, bUnknownValues);
	}

	/**
	 * Full implementation of property-change propagation. Applies metadata and
	 * reference rules, updates primitive-null markers, performs ID and unique
	 * validation, updates inverse references, records undo edits, sends hub
	 * before/after events, updates link membership, applies triggers, manages
	 * distributed-sync routing, and sets the object's changed flag when needed.
	 *
	 * @param oaObj           object whose property changed
	 * @param propertyName    name of the property
	 * @param oldObj          previous value
	 * @param newObj          new value
	 * @param bLocalOnly      if true, suppresses cross-computer sync
	 * @param bSetChanged     if true, allows setting the changed flag
	 * @param bUnknownValues  if true, skips some old-value validation
	 * @param bIsCheckingRef  internal flag used during recursive reference updates
	 */
	private static void firePropertyChange(final OAObject oaObj, final String propertyName, Object oldObj, Object newObj,
			final boolean bLocalOnly, final boolean bSetChanged, final boolean bUnknownValues, final boolean bIsCheckingRef) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectEventService().firePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged, bUnknownValues, bIsCheckingRef);
	}

	/**
	 * Notifies all hubs referencing the object that a property is about to change,
	 * allowing listeners to process before-change semantics.
	 *
	 * @param oaObj        object whose property will change
	 * @param propertyName property name
	 * @param oldObj       previous value
	 * @param newObj       new value
	 */
	private static void sendHubBeforePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectEventService().sendHubBeforePropertyChange(oaObj, propertyName, oldObj, newObj);
	}

	/**
	 * Handles reference-property updates by adjusting membership in reverse-link
	 * hubs, managing ownership relationships, and maintaining recursive link
	 * consistency. Ensures that the object is removed from old hubs and added to
	 * new hubs when required, and updates master/active objects where appropriate.
	 *
	 * @param oaObj     object whose link reference changed
	 * @param oi        metadata for the object's class
	 * @param linkInfo  metadata describing the modified link
	 * @param oldObj    prior reference value (may be OAObjectKey)
	 * @param newObj    new reference value
	 */
	private static void sendHubPropertyChange(final OAObject oaObj, final String propertyName, final Object oldObj, final Object newObj,
			final OALinkInfo linkInfo) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectEventService().sendHubPropertyChange(oaObj, propertyName, oldObj, newObj, linkInfo);
	}


	/**
	 * Sends an after-load event to all hubs referencing the object, allowing
	 * listeners to perform initialization once the object has been fully loaded.
	 *
	 * @param oaObj object that has just completed loading
	 */
	private static void fireAfterLoadEvent(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectEventService().fireAfterLoadEvent(oaObj);
	}

}
