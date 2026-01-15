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

import java.util.List;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubAddRemoveDelegate;
import com.viaoa.hub.HubCSDelegate;
import com.viaoa.hub.HubDSDelegate;
import com.viaoa.hub.HubDataDelegate;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubEventDelegate;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.OASync;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.util.*;

/**
 * Handles the full delete lifecycle for {@link OAObject} instances.
 * <p>
 * This delegate coordinates all aspects of deletion across the Object Graph:
 * recursive cascade removal, reference nulling, Hub membership cleanup,
 * event dispatch, DataSource notification, and distributed synchronization.
 * It guarantees referential integrity and prevents orphaned objects, while
 * maintaining the single-instance invariant throughout the runtime graph.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li><b>Cascade Delete:</b> Recursively deletes all dependent child objects before
 *       removing the parent. Honors {@code cascadeDelete=true} metadata in link definitions
 *       and ensures proper ordering to avoid constraint violations.</li>
 *
 *   <li><b>Reference Cleanup:</b> Clears or nulls all foreign-key references pointing
 *       to the deleted object (1→1, 1→M, M→1, and M→M). Removes the object from all Hubs,
 *       including private and calculated collections.</li>
 *
 *   <li><b>Event Lifecycle:</b> Fires {@code beforeDelete} and {@code afterDelete} events
 *       in proper sequence. Updates the internal "deleted" flag and suppresses unnecessary
 *       change propagation after removal. All event sequencing respects the OAObject contract
 *       for before/after ordering.</li>
 *
 *   <li><b>Thread and Reentrancy Safety:</b> Uses {@link com.viaoa.object.OAThreadLocalDelegate}
 *       to mark delete operations in progress and avoid re-entrant or duplicate cascades.
 *       Thread-local tracking also prevents concurrent cross-graph deletions from interfering
 *       with one another.</li>
 *
 *   <li><b>DataSource Integration:</b> On the server, delegates to
 *       {@link com.viaoa.datasource.OAObjectDSDelegate#delete(OAObject)} to perform
 *       the physical removal in the underlying DataSource. On the client, deletes only
 *       from the in-memory graph and relies on server synchronization for persistence.</li>
 *
 *   <li><b>Distributed Synchronization:</b> Coordinates with
 *       {@link com.viaoa.comm.OAObjectCSDelegate} to broadcast deletes between
 *       client and server. Ensures GUID-based object identity is honored across
 *       distributed sessions.</li>
 *
 *   <li><b>Many-to-Many Handling:</b> Removes link table entries through
 *       {@link com.viaoa.hub.HubDSDelegate#removeMany2ManyLinks(Hub, OAObject)}
 *       and cleans up inverse relationships using {@code updateMany2ManyLinks()}.</li>
 *
 *   <li><b>Undo and Audit Hooks:</b> Integrates with {@code OAUndoDelegate} and
 *       {@code OAObjectLogDelegate} to capture delete operations for undo and audit
 *       trails, when enabled.</li>
 * </ul>
 *
 * <h2>Delete Sequence</h2>
 * <ol>
 *   <li>Fire {@code beforeDelete} event.</li>
 *   <li>Mark object as deleting (ThreadLocal guard).</li>
 *   <li>Recursively delete children (cascade).</li>
 *   <li>Clear all reverse references and Hub memberships.</li>
 *   <li>Perform DataSource delete (server only).</li>
 *   <li>Remove from cache and Hub indexes.</li>
 *   <li>Fire {@code afterDelete} event and user callbacks.</li>
 * </ol>
 *
 * <h2>Concurrency and Safety</h2>
 * Deletions are transactional at the object-graph level: all related references
 * and events are processed atomically within the same thread. Re-entrant
 * or nested delete calls on the same object are ignored via {@link OACascade}.
 *
 * <h2>Design Notes</h2>
 * <ul>
 *   <li>Works for any {@link com.viaoa.datasource.OADataSource} implementation,
 *       including SQL, REST, or in-memory stores.</li>
 *   <li>Uses GUIDs and object identity to guarantee consistent resolution
 *       across threads, caches, and distributed sessions.</li>
 *   <li>All Hub and reverse-link cleanups are event-driven, ensuring that
 *       downstream listeners (UI, sync clients, loggers) are notified in order.</li>
 * </ul>
 *
 * @see OAObject
 * @see OAObjectDelegate
 * @see com.viaoa.datasource.OAObjectDSDelegate
 * @see com.viaoa.comm.OAObjectCSDelegate
 * @see com.viaoa.hub.Hub
 */
public class OAObjectDeleteDelegate {
	public static Logger LOG = Logger.getLogger(OAObjectDeleteDelegate.class.getName());

	static OAGraph getGraph(Hub hub, OAObject obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		// if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
	}
	
	/**
	 * Deletes the specified object using full delete lifecycle processing.
	 * <p>
	 * If client/server routing allows the delete to run locally, an
	 * {@link OACascade} instance is created and the internal delete
	 * method is invoked.
	 *
	 * @param oaObj the object to delete; ignored if {@code null}
	 */
	public static void delete(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectDeleteService().delete(oaObj);
	}

	/**
	 * Performs a server-side delete for the specified object. A new
	 * {@link OACascade} instance is created and passed to the internal
	 * delete method.
	 *
	 * @param oaObj the object to delete
	 */
    public static void syncServerDelete(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectDeleteService().syncServerDelete(oaObj);
    }
	
    /**
     * Performs a client-side delete for objects that exist only within
     * the client's cache. A new {@link OACascade} instance is created
     * and passed to the internal delete method.
     *
     * @param oaObj the object to delete
     */
	public static void syncClientDelete(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectDeleteService().syncClientDelete(oaObj);
	}
	
	/**
	 * Updates the deleted flag on the specified object and fires the
	 * appropriate before/after property-change events. If the object
	 * is being restored (deleted flag set to {@code false}), its key
	 * integrity is reverified and it is re-added to the cache.
	 *
	 * @param oaObj the object whose deleted flag is updated
	 * @param tf the new deleted flag value
	 * @throws RuntimeException if key verification fails when
	 *                          clearing the deleted flag
	 */
	public static void setDeleted(OAObject oaObj, boolean tf) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectDeleteService().setDeleted(oaObj, tf);
	}

	/**
	 * Performs the full internal delete lifecycle, including event
	 * dispatch, cascade delete processing, reference cleanup, DataSource
	 * delete, hub removal, and distributed client notification.
	 *
	 * @param oaObj the object to delete
	 * @param cascade the cascade-tracking object used to prevent
	 *                re-entrant deletions
	 */
	public static void delete(final OAObject oaObj, OACascade cascade) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectDeleteService().delete(oaObj, cascade);
	}

	/**
	 * Determines whether the specified object can be deleted by checking
	 * all link definitions that require the related collection or reference
	 * to be empty prior to deletion.
	 *
	 * @param oaObj the object being evaluated
	 * @return {@code true} if all required links are empty; otherwise {@code false}
	 */
	public static boolean canDelete(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectDeleteService().canDelete(oaObj);
	}

	/**
	 * Returns an array of link definitions that must be empty before the
	 * specified object can be deleted. Only links marked as requiring empty
	 * state and containing non-empty values are included.
	 *
	 * @param oaObj the object being evaluated
	 * @return an array of required-empty link definitions, or {@code null}
	 *         if none exist
	 */
	public static OALinkInfo[] getMustBeEmptyBeforeDelete(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectDeleteService().getMustBeEmptyBeforeDelete(oaObj);
	}


}
