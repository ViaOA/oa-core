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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.datasource.OASelect;
import com.viaoa.graph.OAGraph;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectHubDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

/**
 * Implements data-source selection, lazy loading, and incremental fetch for a
 * {@link Hub}.
 *
 * <p><b>Responsibilities</b>
 * <ul>
 *   <li>Manage {@link OASelect} lifecycle and thread-safe fetchMore sequences.</li>
 *   <li>Load objects incrementally while honoring Hub capacity and vector limits.</li>
 *   <li>Handle cancellation, errors, and automatic synchronization with data sources.</li>
 *   <li>Support full dataset retrieval when required for serialization.</li>
 * </ul>
 *
 * <p>Forms the core of OA’s on-demand loading behavior for distributed and
 * large-scale collections.
 */
public class HubSelectDelegate {
	private static Logger LOG = Logger.getLogger(HubSelectDelegate.class.getName());

	/*
	OAGraph g = getGraph(hub, null);
	if (g == null) return;
	g.hubs().getHubSelectService().?(?);

	OAGraph g = OARuntime.get().graph(c);
	if (g == null) return;
	g.hubs().getHubSelectService().?(?);
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
	 * Retrieves additional objects for the Hub from its most recent select()
	 * operation. Uses the select tied to the Hub.
	 *
	 * @param thisHub the Hub whose select results are being extended
	 * @return number of objects loaded during this fetch
	 */
	public static int fetchMore(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return 0;
		return g.hubs().getHubSelectService().fetchMore(thisHub);
	}

	/**
	 * Retrieves more objects from the given {@link OASelect}, using its
	 * configured fetch amount to determine how many items to load.
	 *
	 * @param thisHub the Hub to populate
	 * @param sel     the OASelect instance providing objects
	 * @return number of objects fetched
	 */
	protected static int fetchMore(Hub thisHub, OASelect sel) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return 0;
		return g.hubs().getHubSelectService().fetchMore(thisHub, sel);
	}

	/**
	 * Retrieves more objects using an explicit fetch amount instead of the
	 * OASelect’s configured value.
	 *
	 * @param thisHub the Hub to populate
	 * @param famt    number of objects to attempt retrieval
	 * @return number of objects fetched
	 */
	protected static int fetchMore(Hub thisHub, int famt) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return 0;
		return g.hubs().getHubSelectService().fetchMore(thisHub, famt);
	}

	/**
	 * Core thread-safe implementation of fetchMore. Ensures only one thread
	 * loads data at a time for the given Hub by managing fetch locks in
	 * {@code hmHubFetch}.
	 *
	 * @param thisHub the Hub being populated
	 * @param sel     the OASelect providing data
	 * @param famt    fetch amount to use
	 * @return number of objects fetched
	 */
	protected static int fetchMore(Hub thisHub, OASelect sel, int famt) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return 0;
		return g.hubs().getHubSelectService().fetchMore(thisHub, sel, famt);
	}

	/**
	 * Internal worker that pulls objects from the OASelect and inserts them into
	 * the Hub’s backing vector.
	 *
	 * <p>Behavior:</p>
	 * <ul>
	 *   <li>Stops when no more data is available.</li>
	 *   <li>Dynamically grows Hub capacity when needed.</li>
	 *   <li>Adds objects using {@link HubAddRemoveDelegate} with loading guarded
	 *       by {@link OAThreadLocalDelegate}.</li>
	 *   <li>Restores Hub “changed” state after loading completes.</li>
	 * </ul>
	 *
	 * @return number of objects successfully added
	 */
	protected static int _fetchMore(Hub thisHub, OASelect sel, int famt) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return 0;
		return g.hubs().getHubSelectService()._fetchMore(thisHub, sel, famt);
	}

	/**
	 * Determines whether additional data is available for the Hub’s current
	 * select() operation. Starts the select if needed.
	 *
	 * @param thisHub the Hub being queried
	 * @return true if more data is available; false otherwise
	 */
	public static boolean isMoreData(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubSelectService().isMoreData(thisHub);
	}

	/**
	 * Determines whether the given OASelect has more data to fetch.
	 *
	 * @param sel the OASelect instance
	 * @return true if more data is available; false otherwise
	 */
	public static boolean isMoreData(OASelect sel) {
		OAGraph g = OARuntime.get().graph(sel.getSelectClass());
		if (g == null) return false;
		return g.hubs().getHubSelectService().isMoreData(sel);
	}

	/**
	 * Loads all remaining data for the Hub’s current select(), using the Hub’s
	 * default select instance.
	 *
	 * @param thisHub the Hub whose select results should be fully loaded
	 */
	public static void loadAllData(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubSelectService().loadAllData(thisHub);
	}

	/**
	 * Fully loads all remaining objects from the given OASelect into the Hub.
	 *
	 * <p>Ensures only one thread performs the full-load operation at a time.
	 * Handles cancellation, error recovery, and select state transitions.</p>
	 *
	 * @param thisHub the Hub being populated
	 * @param select  the OASelect instance to load from
	 */
	public static void loadAllData(Hub thisHub, OASelect select) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubSelectService().loadAllData(thisHub, select);
	}

	/**
	 * Returns the OASelect associated with the Hub, or null if none exists.
	 *
	 * @param thisHub the Hub being queried
	 * @return the Hub’s current OASelect, or null
	 */
	public static OASelect getSelect(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubSelectService().getSelect(thisHub);
	}

	/**
	 * Retrieves the Hub’s OASelect instance, optionally creating a new one if none
	 * exists.
	 *
	 * @param thisHub       the Hub being queried
	 * @param bCreateIfNull true to create a new OASelect when missing
	 * @return the existing or newly created OASelect
	 */
	protected static OASelect getSelect(Hub thisHub, boolean bCreateIfNull) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubSelectService().getSelect(thisHub, bCreateIfNull);
	}

	/**
	 * Main select() method for Hubs. Prepares and executes the OASelect query,
	 * initializes Hub metadata, handles append/overwrite modes, and loads the
	 * first batch of data.
	 *
	 * @param thisHub the Hub to populate
	 * @param select  the select definition to run
	 */
	public static void select(final Hub thisHub, OASelect select) { // This is the main select method for Hub that all of the other select methods call.
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubSelectService().select(thisHub, select);
	}

	/**
	 * Cancels the Hub’s current OASelect and optionally removes it from Hub data.
	 * Also resets selectAllHub flags and resizes the Hub to fit its contents.
	 *
	 * @param thisHub       the Hub whose select is being canceled
	 * @param bRemoveSelect true to clear the Hub’s select reference
	 */
	public static void cancelSelect(Hub thisHub, boolean bRemoveSelect) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubSelectService().cancelSelect(thisHub, bRemoveSelect);
	}

	/**
	 * Returns the total number of matching records for the Hub’s current select().
	 *
	 * @param thisHub the Hub being queried
	 * @return the count value, or -1 if no select exists
	 */
	public static int getCount(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return 0;
		return g.hubs().getHubSelectService().getCount(thisHub);
	}

	/**
	 * Indicates whether the current select() has been counted.
	 *
	 * @param thisHub the Hub being checked
	 * @return true if counted; false otherwise
	 */
	public static boolean isCounted(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubSelectService().isCounted(thisHub);
	}

	/**
	 * Updates the Hub’s select() WHERE clause, creating a new OASelect if needed.
	 *
	 * @param thisHub the Hub whose select WHERE clause is modified
	 * @param s       the WHERE clause string
	 */
	public static void setSelectWhere(Hub thisHub, String s) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubSelectService().setSelectWhere(thisHub, s);
	}

	/**
	 * Returns the WHERE clause associated with the Hub’s current select().
	 *
	 * @param thisHub the Hub being queried
	 * @return the WHERE clause, or null if none exists
	 */
	public static String getSelectWhere(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubSelectService().getSelectWhere(thisHub);
	}

	/**
	 * Sets the ORDER BY clause for the Hub’s select operation. Creates a new
	 * OASelect instance if none exists and the sort property is non-empty.
	 *
	 * @param thisHub the Hub whose sort order is being modified
	 * @param s       the ORDER BY clause string
	 */
	public static void setSelectOrder(Hub thisHub, String s) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubSelectService().setSelectOrder(thisHub, s);
	}

	/**
	 * Returns the ORDER BY clause associated with the Hub’s select().
	 *
	 * @param thisHub the Hub being queried
	 * @return the ORDER BY clause or null if none exists
	 */
	public static String getSelectOrder(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubSelectService().getSelectOrder(thisHub);
	}

	/**
	 * Executes a select() operation on the Hub with an optionally append-mode
	 * OASelect created automatically.
	 *
	 * @param thisHub     the Hub to populate
	 * @param bAppendFlag true to append results; false to overwrite
	 */
	public static void select(Hub thisHub, boolean bAppendFlag) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubSelectService().select(thisHub, bAppendFlag);
	}

	/**
	 * Runs a select() using a WHERE object, WHERE clause, parameter list,
	 * ORDER BY clause, and append mode. Creates a new OASelect accordingly.
	 *
	 * @param thisHub      the Hub to populate
	 * @param whereObject  the object used for property-based filtering
	 * @param whereClause  the textual WHERE clause
	 * @param whereParams  parameter values for the WHERE clause
	 * @param orderByClause the sort expression
	 * @param bAppendFlag  true to append results; false to overwrite
	 */
	protected static void select(Hub thisHub, OAObject whereObject, String whereClause,
			Object[] whereParams, String orderByClause, boolean bAppendFlag) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubSelectService().select(thisHub, whereObject, whereClause, whereParams, orderByClause, bAppendFlag);
	}

	/**
	 * Same as the other select() overload, but applies an {@link OAFilter}
	 * to further restrict which objects qualify after retrieval.
	 *
	 * @param thisHub     the Hub to populate
	 * @param whereObject the WHERE-object used for reverse property resolution
	 * @param whereClause the WHERE clause
	 * @param whereParams list of parameters for the WHERE clause
	 * @param orderByClause ORDER BY clause
	 * @param bAppendFlag true to append results
	 * @param filter      filter applied to objects after select()
	 */
	protected static void select(Hub thisHub, OAObject whereObject, String whereClause,
			Object[] whereParams, String orderByClause, boolean bAppendFlag, OAFilter filter) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubSelectService().select(thisHub, whereObject, whereClause, whereParams, orderByClause, bAppendFlag, filter);
	}

	/**
	 * Performs a passthru select(), sending raw WHERE and ORDER clauses directly
	 * to the underlying data source without additional Hub-based constraints.
	 *
	 * @param thisHub     the Hub to populate
	 * @param whereClause raw WHERE clause
	 * @param orderClause raw ORDER BY clause
	 */
	public static void selectPassthru(Hub thisHub, String whereClause, String orderClause) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubSelectService().selectPassthru(thisHub, whereClause, orderClause);
	}

	/**
	 * Passthru select() variant that also supports append mode.
	 *
	 * @param thisHub     the Hub to populate
	 * @param whereClause raw WHERE clause
	 * @param orderClause raw ORDER BY clause
	 * @param bAppend     whether to append instead of clearing the Hub first
	 */
	public static void selectPassthru(Hub thisHub, String whereClause, String orderClause, boolean bAppend) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubSelectService().selectPassthru(thisHub, whereClause, orderClause, bAppend);
	}

	/**
	 * Returns the Hub currently used as the "whereHub" for select(), or null if none.
	 *
	 * @param thisHub the Hub being queried
	 * @return the whereHub controlling select filtering, or null
	 */
	public static Hub getSelectWhereHub(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubSelectService().getSelectWhereHub(thisHub);
	}

	/**
	 * Sets the Hub to be used as the "whereHub" for select(), which constrains
	 * queries based on a linked property path.
	 *
	 * @param thisHub the Hub whose whereHub is being set
	 * @param hub     the Hub to use for filtering
	 */
	public static void setSelectWhereHub(Hub thisHub, Hub hub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubSelectService().setSelectWhereHub(thisHub, hub);
	}

	/**
	 * Returns the property path associated with the Hub’s whereHub, or null.
	 *
	 * @param thisHub the Hub being queried
	 * @return the whereHub property path
	 */
	public static String getSelectWhereHubPropertyPath(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubSelectService().getSelectWhereHubPropertyPath(thisHub);
	}

	/**
	 * Sets the property path used for converting a whereHub into an equivalent
	 * WHERE clause during select().
	 *
	 * @param thisHub the Hub being configured
	 * @param pp      the property path to use for filtering
	 */
	public static void setSelectWhereHubPropertyPath(Hub thisHub, String pp) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubSelectService().setSelectWhereHubPropertyPath(thisHub, pp);
	}

	/*
	 * Check to see if thisHub can use the whereHub and "converted" PP as another Hub.
	 * <p>
	 * Example setup:<br>
	 * hubWhere&lt;Company&gt;<br>
	 * PP: [Company.]customers.orders.orderItems<br>
	 * reversePP: orderItem.order.customer.company<br>
	 * <p>
	 * hubFrom = Hub&lt;OrderItem&gt;, hubWhere+PP=hub&lt;Company&gt;+"customers.orders.orderItems"<br>
	 * a select for hubFrom&lt;OrderItem&gt; would add a whereClause "AND order.customer.company=?", hubWhere&lt;Company&gt;.AO<br>
	 * Which would limit it to only selecting orderitems that are for a company.
	 * <p>
	 * Now ... if we have a Hub&lt;Order&gt; that is used to select an Order for an OrderItem, then it would need to only select under the
	 * same Company.<br>
	 * thisHub&lt;Order&gt;<br>
	 * calling adoptWhereHub(thisHub, "order", hubFrom&lt;OrderItem&gt;, where "order" is the property from hubFrom&lt;OrderItem&gt; to
	 * thisHub&lt;Order&gt;<br>
	 * would get hubWhere+PP from hubFrom&lt;Orderitem&gt;, which would be hub&lt;Company&gt;+"customers.orders.orderItems", and would
	 * reverse it to "order.customer.company", where the first link is "order", which matches propName "order", and thisHub&lt;Order&gt;
	 * would end up using hub&lt;Company&gt;+"customers.orders"
	 * <p>
	 *
	 * @param thisHub  Hub that could be in the same propertyPath of the hubFromHub.whereHubPropertyPath
	 * @param propName the link name of thisHub from hubFrom.
	 * @param hubFrom  hub that might have a selectWhereHub & PP that can be used by thisHub.
	 */
	
	/**
	 * Attempts to adopt the whereHub + propertyPath from another Hub if thisHub
	 * participates in the same property path chain.
	 *
	 * <p>Used to propagate filtering constraints across related Hubs.</p>
	 *
	 * @param thisHub  the Hub attempting to adopt whereHub filtering
	 * @param propName the property linking hubFrom → thisHub
	 * @param hubFrom  the Hub that may supply whereHub filtering rules
	 * @return true if the whereHub was successfully adopted
	 */
	public static boolean adoptWhereHub(final Hub thisHub, final String propName, final Hub hubFrom) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubSelectService().adoptWhereHub(thisHub, propName, hubFrom);
	}

	/**
	 * Refreshes the Hub’s contents. Fires pre-refresh events, then delegates
	 * to {@link #_refresh(Hub)} while honoring thread-local refresh flags.
	 *
	 * @param thisHub the Hub to refresh
	 * @return true if refresh occurred; false otherwise
	 */
	public static boolean refresh(final Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubSelectService().refresh(thisHub);
	}

	/**
	 * Re-runs the select() operation or reloads the master-object detail list
	 * to bring the Hub’s contents into consistency with the data source.
	 *
	 * <p>Behavior:</p>
	 * <ul>
	 *   <li>If no select exists: refreshes based on the master object link.</li>
	 *   <li>If select exists: resets, re-selects, and reconciles adds/removes.</li>
	 *   <li>Maintains element ordering to match the freshly retrieved dataset.</li>
	 * </ul>
	 *
	 * @param thisHub the Hub being refreshed
	 * @return true if refresh succeeded, false otherwise
	 */
	protected static boolean _refresh(final Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubSelectService()._refresh(thisHub);
	}

	/**
	 * Re-executes the last select() operation associated with the Hub.
	 *
	 * <p>Behavior:</p>
	 * <ul>
	 *   <li>Rebuilds the OASelect if needed (e.g., detail Hub with no select).</li>
	 *   <li>Runs select(), then merges results into the Hub.</li>
	 *   <li>Restores Active Object after reload.</li>
	 * </ul>
	 *
	 * @param thisHub the Hub to refresh
	 * @return true if successful
	 */
	public static boolean refreshSelect(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubSelectService().refreshSelect(thisHub);
	}

}
