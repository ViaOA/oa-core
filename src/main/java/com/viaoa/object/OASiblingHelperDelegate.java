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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;

/**
 * Identifies sibling objects that are likely to require the same reference
 * property to be loaded, based on the calling thread's active Hub navigation
 * and recently accessed property-paths (tracked by {@link OASiblingHelper}).
 *
 * <p>This enables efficient clustered lazy-loading: when a reference is
 * requested on one object in a Hub, nearby objects that would require the
 * same data can be prefetched in a single request to the server or datasource,
 * improving responsiveness and reducing concurrency load.</p>
 *
 * <p>Search is bounded by several mechanisms to avoid over-fetching and to
 * maintain UI performance: recursion guards, time budgets, sliding window
 * hub scans, and path count limits. Only already-loaded data is examined, and
 * returned results are {@link OAObjectKey} instances so that identity and
 * lazy-loading semantics are preserved.</p>
 *
 * @see OASiblingHelper
 * @see OAThreadLocalDelegate
 * @see OALinkInfo
 * @see Hub
 */
public class OASiblingHelperDelegate {

	/*
	OAGraph g = getGraph(null, oaObj);
	if (g == null) return;
	g.objects().getOAObjectSiblingService().??(oaObj);
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
	 * Notifies all thread-local OASiblingHelper instances that a reference
	 * property was accessed on the given object so they can record the link
	 * step for sibling detection.
	 *
	 * @param obj               the object whose reference was accessed
	 * @param linkPropertyName  the accessed link-property name
	 */
	public static void onGetObjectReference(final OAObject obj, final String linkPropertyName) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return;
		g.objects().getOAObjectSiblingService().onGetObjectReference(obj, linkPropertyName);
	}

	/**
	 * Convenience wrapper that delegates to the overloaded getSiblings method
	 * without an ignore map.
	 *
	 * @param mainObject the object requesting siblings
	 * @param property   the property name being accessed
	 * @param maxAmount  maximum number of siblings to return
	 * @return an array of sibling object keys
	 */
	public static OAObjectKey[] getSiblings(final OAObject mainObject, final String property, final int maxAmount) {
		OAGraph g = getGraph(null, mainObject);
		if (g == null) return null;
		return g.objects().getOAObjectSiblingService().getSiblings(mainObject, property, maxAmount);
	}

	/**
	 * Returns sibling objects that are likely to require the same property
	 * to be loaded. Enforces per-thread call limits, measures runtime, and
	 * delegates to the internal _getSiblings method.
	 *
	 * @param mainObject the object requesting sibling evaluation
	 * @param property   the property name being accessed
	 * @param maxAmount  maximum number of sibling keys to return
	 * @param hmIgnore   keys already being processed by concurrent requests
	 * @return an array of sibling object keys
	 */
	public static OAObjectKey[] getSiblings(final OAObject mainObject, final String property, final int maxAmount,
			ConcurrentHashMap<Long, Boolean> hmIgnore) {
		OAGraph g = getGraph(null, mainObject);
		if (g == null) return null;
		return g.objects().getOAObjectSiblingService().getSiblings(mainObject, property, maxAmount, hmIgnore);
	}


	/**
	 * Scans the given hub for objects that require the same property to be
	 * loaded. Uses an OAFinder with loaded-data constraints and adds each
	 * qualifying object's key to the results.
	 *
	 * @param alFoundObjectKey list collecting found sibling keys
	 * @param hubRoot          the hub to scan
	 * @param startPosHubRoot  starting hub index for scanning
	 * @param finderPropertyPath the property path used for scanning
	 * @param origProperty     the original property being accessed
	 * @param linkInfo         metadata describing the property link
	 * @param mainObject       the object requesting siblings
	 * @param hmTypeOneObjKey  per-thread one-to-one key tracking
	 * @param hmIgnore         map of objects to skip
	 * @param maxAmount        maximum number of siblings to find
	 * @param msStarted        start time for enforcing time limits
	 * @param runCount         recursion/iteration counter
	 */
	protected static void findSiblings(
			final ArrayList<OAObjectKey> alFoundObjectKey,
			final Hub hubRoot, final int startPosHubRoot, final String finderPropertyPath, final String origProperty,
			final OALinkInfo linkInfo,
			final OAObject mainObject,
			final HashMap<OAObjectKey, OAObject> hmTypeOneObjKey, // for calling thread, refobjs already looked at
			final ConcurrentHashMap<Long, Boolean> hmIgnore, // for all threads
			final int maxAmount,
			final long msStarted,
			final int runCount) {

		OAGraph g = getGraph(null, mainObject);
		if (g == null) return;
		g.objects().getOAObjectSiblingService().findSiblings(alFoundObjectKey, hubRoot, startPosHubRoot, finderPropertyPath, origProperty, linkInfo, mainObject, hmTypeOneObjKey, hmIgnore, maxAmount, msStarted, runCount);
	}

	/**
	 * Returns the hub that provides the best candidate set of sibling objects
	 * for the given master object, using link alignment and hub hierarchy
	 * scoring.
	 *
	 * @param masterObject the object whose hubs are being evaluated
	 * @param liToMaster   optional link-restriction for selecting the hub
	 * @return the hub best suited for sibling evaluation, or null if none match
	 */
	public static Hub findBestSiblingHub(OAObject masterObject, OALinkInfo liToMaster) {
		OAGraph g = getGraph(null, masterObject);
		if (g == null) return null;
		return g.objects().getOAObjectSiblingService().findBestSiblingHub(masterObject, liToMaster);
	}
}
