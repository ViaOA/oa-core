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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEventDelegate;
import com.viaoa.runtime.OARuntime;

/**
 * Coordinates the full save lifecycle for {@link OAObject} instances.
 * <p>
 * This delegate manages persistence, cascading, ordering, and event sequencing
 * across the entire Object Graph. It ensures that all related objects are saved
 * in a consistent order and that new parent objects are persisted before any
 * dependent references or child collections.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li><b>Cascade Save:</b> Traverses all links according to cascade rules
 *       ({@code CASCADE_LINK_RULES}, {@code CASCADE_OWNED_LINKS},
 *       {@code CASCADE_ALL_LINKS}, {@code CASCADE_NONE}).</li>
 *   <li><b>Parent-Before-Child Order:</b> Ensures that new referenced objects
 *       in {@code ONE} links are saved before their dependents to maintain
 *       referential integrity.</li>
 *   <li><b>Hub Integration:</b> Fires {@code beforeSave} and {@code afterSave}
 *       events for each Hub reference; integrates with {@link HubEventDelegate}.</li>
 *   <li><b>DataSource Coordination:</b> Delegates to
 *       {@link com.viaoa.datasource.OAObjectDSDelegate#save(OAObject)} for
 *       physical persistence, regardless of DataSource type.</li>
 *   <li><b>Thread-Safety:</b> Prevents re-entrant saves via
 *       {@link OACascade} tracking and synchronized GUID-based locks.</li>
 *   <li><b>Error Recovery:</b> Retries failed saves up to four times and logs
 *       detailed warnings for transient DataSource errors.</li>
 * </ul>
 *
 * <h2>Design Guarantees</h2>
 * <ul>
 *   <li>Single-threaded per-object save enforcement using {@code hmSaveNewLock}.</li>
 *   <li>Deterministic save ordering with recursion depth protection.</li>
 *   <li>Event ordering compatible with OAObject's before/after semantics.</li>
 *   <li>DataSource-agnostic persistence model.</li>
 * </ul>
 *
 * @see OAObject
 * @see OAObjectDeleteDelegate
 * @see com.viaoa.datasource.OAObjectDSDelegate
 * @see com.viaoa.hub.HubEventDelegate
 */
public class OAObjectSaveDelegate {
	private static Logger LOG = Logger.getLogger(OAObjectSaveDelegate.class.getName());

	/*
	OAGraph g = getGraph(null, oaObj);
	if (g == null) return;
	g.objects().getOAObjectSaveService().??(oaObj);

	OAGraph g = OARuntime.get().graph(c);
	if (g == null) return;
	g.objects().getOAObjectSaveService().??(oaObj);
    */
	static OAGraph getGraph(Hub hub, OAObject obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
	}
	
	protected static void save(OAObject oaObj, int iCascadeRule) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectSaveService().save(oaObj, iCascadeRule);
	}

	public static void save(OAObject oaObj, int iCascadeRule, OACascade cascade) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectSaveService().save(oaObj, iCascadeRule, cascade);
	}


	/**
	 * Called by HubSaveDelegate.saveAll() to save all New Many2Many added objects.
	 */
	public static void _saveObjectOnly(OAObject oaObj, OACascade cascade) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectSaveService()._saveObjectOnly(oaObj, cascade);
	}

	/**

	*/
	protected static boolean onSave(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectSaveService().onSave(oaObj);
	}
}
