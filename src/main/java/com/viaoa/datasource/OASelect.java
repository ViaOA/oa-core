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
package com.viaoa.datasource;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.viaoa.filter.OAQueryFilter;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAPerformance;
import com.viaoa.object.OASiblingHelper;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadImpl;
import com.viaoa.runtime.thread.OAThreadLocalService;
import com.viaoa.util.OAArray;
import com.viaoa.util.OAComparator;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAString;
/**
 * Executes object-based queries across any {@link OADataSource}.
 * <p>
 * {@code OASelect} represents OA's unified query mechanism that supports
 * relational, in-memory, distributed, and REST data sources. It translates
 * property-path queries into the native language of the active DataSource
 * while preserving full object identity and graph relationships.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Supports property-path and parameterized queries (e.g., "customer.name == ?").</li>
 *   <li>Integrates with {@link OADataSource} to perform full CRUD selects.</li>
 *   <li>Returns results lazily via {@link OADataSourceIterator}.</li>
 *   <li>Automatically reuses cached {@link OAObject} instances (no duplicates).</li>
 *   <li>Supports filters ({@link OAFilter}) for in-memory post-processing.</li>
 *   <li>Optional pre-count and cancellation support via {@link OASelectManager}.</li>
 *   <li>Fully thread-safe and transaction-aware.</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * OASelect<Customer> sel =
 *     new OASelect<>(Customer.class, "name like ?", new Object[]{"J%"}, "name", 100);
 * while (sel.hasNext()) {
 *     Customer c = sel.next();
 *     System.out.println(c.getName());
 * }
 * sel.close();
 * }</pre>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *   <li>Graph-based query abstraction decoupled from physical storage.</li>
 *   <li>Supports distributed and REST-backed data sources without code changes.</li>
 *   <li>Integrates seamlessly with {@link com.viaoa.hub.Hub} for live data binding.</li>
 * </ul>
 *
 * @param <T> the {@link OAObject} type returned by the query
 * @see OADataSource
 * @see OADataSourceIterator
 * @see OASelectManager
 * @see OAFilter
 */
public class OASelect<TYPE extends OAObject> implements Iterable<TYPE>, AutoCloseable, Closeable {
	static final long serialVersionUID = 1L;

	private static Logger LOG = Logger.getLogger(OASelect.class.getName());

	/**
	 * Counter used to assign unique IDs to each OASelect instance.
	 */
	private static final AtomicInteger aiId = new AtomicInteger();

	/**
	 * Unique identifier assigned to this OASelect instance when created.
	 */
	private final int id;

	/**
	 * Class of objects being selected by this query.
	 */
	protected Class clazz;

	/**
	 * The property-path or passthru where-clause associated with this query.
	 */
	protected String where;
	
	/**
	 * Ordering clause used to sort results, if supported by the DataSource.
	 */
	protected String order;
	
	/**
	 * Flag indicating whether the query uses DataSource.selectPassthru()
	 * instead of OA property-path evaluation.
	 */
	protected boolean bPassthru;
	
	/**
	 * Whether results should be appended to an existing Hub or collection
	 * instead of overwriting it.
	 */
	protected boolean bAppend;
	
	/**
	 * Indicates whether the associated Hub should rewind to its first object
	 * when new results are loaded.
	 */
	protected boolean bRewind = true; // set back to first object

	/*
	 * Select based on a where object/hub.ao and the property path to this.hub<TYPE>.
	 * <p>
	 * This will then add to the whereClause of the query, by taking the reverse of the PP that is equal the whereObject.
	 * <p>
	 * examples: if whereObject+pp is Dept+"emps" and this select is for Emp.class, then query will have added: "AND dept.id = ?", dept
	 * <p>
	 * if whereObject+pp is Dept+"emps.orders" and this select is for Order.class, then query will have added: "AND "emp.dept = ?", dept
	 */

	/**
	 * Object used to generate a reverse-path where clause that selects all
	 * objects referencing this whereObject.
	 */
	protected OAObject whereObject;

	/**
	 * Hub used as an alternative where source; its active object may act as
	 * the whereObject for query construction.
	 */
	protected Hub whereHub;

	/**
	 * Property path used to relate the whereObject (or Hub.AO) to the objects
	 * selected by this OASelect.
	 */
	protected String whereObjectPropertyPath;

	/**
	 * Maximum number of objects to load. Zero means unlimited.
	 */
	protected int max; // max amount of objects to load
	
	/**
	 * When true, the DataSource count() method is invoked before selecting
	 * to determine total result size.
	 */
	protected boolean bCountFirst; // count before selecting
	
	/**
	 * Number of objects read so far. Starts at -1 until reading begins.
	 */
	protected int amountRead = -1;
	
	/**
	 * Cached count of total matching objects. -1 indicates that no count
	 * has been performed or completed.
	 */
	protected volatile int amountCount = -1;
	
	/**
	 * Parameter values substituted for '?' placeholders in the where clause.
	 */
	protected Object[] params;
	
	/**
	 * Iterator returned by the underlying DataSource during execution.
	 * Provides streaming access to query results.
	 */
	protected volatile transient OADataSourceIterator query;

	/**
	 * Default number of records to fetch per batch when performing incremental
	 * read operations. This value is used by {@link Hub} and other components
	 * that support lazy or paged loading of DataSource results. A higher value
	 * increases throughput at the cost of memory; a lower value improves
	 * responsiveness for UIs that load progressively.
	 */
	public static final int defaultFetchAmount = 45;
	
	/**
	 * Number of records to fetch at a time when reading from the DataSource.
	 * Defaults to {@link #defaultFetchAmount}. This value is used primarily by
	 * {@link Hub} to support progressive loading, enabling large result sets to
	 * be retrieved in smaller chunks instead of all at once.
	 */
	protected int fetchAmount = defaultFetchAmount; // used by Hub to know how many to read at a time
	
	/**
	 * Indicates whether the select operation has been cancelled. When true,
	 * iteration stops early and the underlying {@link OADataSourceIterator}
	 * is closed to release resources.
	 */
	protected volatile boolean bCancelled;
	
	/**
	 * Tracks whether this OASelect has begun execution. Used to prevent
	 * double-starting and to determine cancellation semantics.
	 */
	protected volatile boolean bHasBeenStarted;

	/**
	 * Flag indicating whether selection should be performed using an
	 * {@link OAFinder} instead of the {@link OADataSource}. This is true when
	 * searching from a Hub, or when the DataSource does not support direct
	 * queries for the given whereClause.
	 */
	protected boolean bUseFinder;

	/**
	 * Timestamp of the last object retrieval (via {@link #next()}).
	 * Used for timeout detection and long-running query diagnostics.
	 */
	protected volatile long lastReadTime; // used with for determining timeout

	/**
	 * Optional in-memory filter applied after the DataSource iterator returns
	 * objects. Used to exclude objects that do not meet criteria not handled
	 * by the underlying DataSource.
	 */
	protected OAFilter<TYPE> oaFilter; // this will be used by OASelect to filter iterator returned values
	
	/**
	 * Optional filter passed to the DataSource. If the DataSource does not
	 * support query evaluation, it may use this filter to perform in-memory
	 * evaluation of candidate objects.
	 */
	protected OAFilter<TYPE> dsFilter; // this will be sent to DataSource, which will use it if it does not support queries

	/**
	 * Finder used as an alternative selection mechanism. When present and
	 * enabled via {@link #bUseFinder}, all results are derived from scanning
	 * objects reachable from the finder root rather than from the DataSource.
	 */
	protected OAFinder<?, TYPE> finder; // will be used instead of calling datasource

	/**
	 * Hub whose contents serve as the search domain when using an
	 * {@link OAFinder}. This allows selection to operate directly against an
	 * existing in-memory collection instead of querying the DataSource.
	 */
	protected Hub<TYPE> hubSearch; // hub used to search from, instead of using DataSource

	/**
	 * Signals whether the DataSource should bypass caching and return fresh
	 * data. When true, select operations force a read-through rather than
	 * reusing cached objects.
	 */
	private boolean bDirty; // data should always be loaded from datasource

	/**
	 * Tracks whether {@link #bDirty} was explicitly set by the caller.
	 * Prevents automatic refresh behavior from overriding user intent.
	 */
	private boolean bDirtyWasSet;
	
	/**
	 * Indicates that a select operation is actively executing. Used to manage
	 * cancellation behavior and ensure thread-safe select sequencing.
	 */
	private volatile boolean bIsSelectingNow;
	
	/**
	 * Marks that iteration has completed (no additional objects available).
	 * Prevents repeated queries or redundant `hasNext()` checks.
	 */
	private volatile boolean bHasNextCompleted;

	/**
	 * Creates a new uninitialized OASelect instance and assigns it a unique
	 * identifier. The selectClass, where clause, and other query parameters
	 * must be configured before use.
	 */
	public OASelect() {
		this.id = aiId.incrementAndGet();
	}

	/**
	 * Creates a new OASelect configured to select objects of the specified class.
	 *
	 * @param c the class of objects to be selected
	 */
	public OASelect(Class<TYPE> c) {
		this();
		setSelectClass(c);
	}

	/**
	 * Creates a new OASelect for passthru-based queries.
	 *
	 * @param c the class of objects to select
	 * @param passthru true to use DataSource.selectPassthru
	 * @param where the passthru where clause
	 * @param order an optional ordering clause
	 */
	public OASelect(Class<TYPE> c, boolean passthru, String where, String order) {
		this();
		setSelectClass(c);
		setPassthru(passthru);
		setWhere(where);
		setOrder(order);
	}

	/**
	 * Creates a new OASelect configured with a where clause and optional order.
	 *
	 * @param c the class of objects to select
	 * @param where the OA property-path where clause
	 * @param order the order-by clause
	 */
	public OASelect(Class<TYPE> c, String where, String order) {
		this();
		setSelectClass(c);
		setWhere(where);
		setOrder(order);
	}

	/**
	 * Creates a new OASelect that uses parameterized query values.
	 *
	 * @param c the class of objects to select
	 * @param where the OA property-path where clause
	 * @param params parameter values substituted for '?' placeholders
	 * @param order the order-by clause
	 */
	public OASelect(Class<TYPE> c, String where, Object[] params, String order) {
		this();
		setSelectClass(c);
		setWhere(where);
		setParams(params);
		setOrder(order);
	}

	/**
	 * Creates a new OASelect using a whereObject-based reverse-path query.
	 *
	 * @param c the class of objects to select
	 * @param whereObject the object used to build a reverse relationship constraint
	 * @param order the order-by clause
	 */
	public OASelect(Class<TYPE> c, OAObject whereObject, String order) {
		this();
		setWhereObject(whereObject);
		setOrder(order);
	}

	/**
	 * Returns the unique identifier assigned to this OASelect instance.
	 *
	 * @return the select ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets the parameter values used to replace '?' markers in the where clause.
	 *
	 * @param params the parameter array
	 */
	public void setParams(Object[] params) {
		this.params = params;
	}

	/**
	 * Returns the parameter values currently associated with this select.
	 *
	 * @return the query parameters, or null if none were assigned
	 */
	public Object[] getParams() {
		return this.params;
	}

	/**
	 * Adds an additional where-clause segment and its parameters to the existing
	 * where clause. The new clause is appended using "AND".
	 *
	 * @param whereClause the additional where fragment
	 * @param params parameter values for the new fragment
	 */
	public void add(String whereClause, Object[] params) {
		this.where = OAString.concat(this.where, whereClause, " AND ");
		this.params = OAArray.add(Object.class, this.params, params);
	}

	/**
	 * Sets the Hub whose contents will be used as the local search domain instead
	 * of querying the DataSource.
	 *
	 * @param hub the Hub used for in-memory searching
	 */
	public void setSearchHub(Hub<TYPE> hub) {
		this.hubSearch = hub;
	}

	/**
	 * Returns the Hub used for local search operations, or null if none is set.
	 *
	 * @return the Hub serving as the search domain
	 */
	public Hub<TYPE> getSearchHub() {
		return this.hubSearch;
	}

	/**
	 * Resets the select and clears iteration state, allowing it to be executed
	 * again using the existing configuration.
	 */
	public void reset() {
		reset(false);
	}

	/**
	 * Resets internal select state and optionally clears the where clause,
	 * order clause, and whereObject.
	 *
	 * @param bClearOutValues true to remove where/order/whereObject settings
	 */
	public void reset(boolean bClearOutValues) {
		closeQuery();
		if (bClearOutValues) {
			where = null;
			order = null;
			whereObject = null;
		}
		bHasNextCompleted = false;
		amountCount = -1;
		amountRead = -1;
		bCancelled = false;
		bHasBeenStarted = false;
		lastReadTime = 0;
	}

	/**
	 * Sets the whereObject and the property path used to derive a reverse-path
	 * relationship for the query.
	 *
	 * @param whereObject the base object used to restrict results
	 * @param pp the property path from whereObject to the select class
	 */
	public void setWhereObject(OAObject whereObject, String pp) {
		this.whereObject = whereObject;
		this.whereObjectPropertyPath = pp;
	}

	/**
	 * Sets the whereObject used to generate a reverse-path constraint. The
	 * property path must be supplied separately if multiple paths exist.
	 *
	 * @param whereObject the object used to restrict the query
	 */
	public void setWhereObject(OAObject whereObject) {
		this.whereObject = whereObject;
	}

	/**
	 * Returns the object used to generate reverse-path where constraints, or null
	 * if no whereObject has been assigned.
	 *
	 * @return the whereObject
	 */
	public Object getWhereObject() {
		return whereObject;
	}

	/**
	 * Returns the object used to generate reverse-path where constraints, or null
	 * if no whereObject has been assigned.
	 *
	 * @return the whereObject
	 */
	public void setPropertyFromWhereObject(String propName) {
		whereObjectPropertyPath = propName;
	}

	/**
	 * Sets the property path used to derive the reverse-path query constraint.
	 *
	 * @param propName the property path from whereObject to the target class
	 */
	public void setWhereObjectPropertyPath(String propName) {
		whereObjectPropertyPath = propName;
	}

	/**
	 * Returns the relationship property name used when selecting from a
	 * whereObject that has multiple paths to the target class.
	 *
	 * @return the property name or null if unspecified
	 */
	public String getPropertyFromWhereObject() {
		return whereObjectPropertyPath;
	}

	/**
	 * Returns the property path used to relate the whereObject (or Hub.AO)
	 * to the target select class. This path is used to construct a reverse-path
	 * query constraint.
	 *
	 * @return the property path, or null if none has been assigned
	 */
	public String getWhereObjectPropertyPath() {
		return whereObjectPropertyPath;
	}

	/**
	 * Returns the {@link OADataSource} associated with the selectClass. If no
	 * class has been assigned, null is returned.
	 *
	 * @return the DataSource used for select operations, or null
	 */
	public OADataSource getDataSource() {
		if (clazz == null) {
			return null;
		}
		OADataSource ds = OADataSource.getDataSource(clazz, getDataSourceFilter());
		return ds;
	}

	/**
	 * Sets the class of objects that this OASelect will return. This must be
	 * assigned before execution of the select operation.
	 *
	 * @param c the class to be selected
	 */
	public void setSelectClass(Class c) {
		this.clazz = c;
	}

	/**
	 * Returns the class of objects selected by this OASelect instance.
	 *
	 * @return the target class
	 */
	public Class getSelectClass() {
		return clazz;
	}

	/**
	 * Sets the where clause associated with this query. This may be a full
	 * property-path expression or a passthru clause depending on query mode.
	 *
	 * @param s the where clause text
	 */
	public void setWhere(String s) {
		where = s;
	}

	/**
	 * Sets the where clause and its corresponding parameter values in a single
	 * operation.
	 *
	 * @param s the where clause
	 * @param params parameter values substituted for '?' markers
	 */
	public void setWhere(String s, Object[] params) {
		where = s;
		setParams(params);
	}

	/**
	 * Convenience method for setting a where clause with a single parameter.
	 *
	 * @param s the where clause
	 * @param param single parameter value for '?'
	 */
	public void setWhere(String s, Object param) {
		where = s;
		setParams(new Object[] { params });
	}

	/**
	 * Returns the current where clause, which may be a property-path expression
	 * or a passthru clause depending on query mode.
	 *
	 * @return the where clause text, or null
	 */
	public String getWhere() {
		return where;
	}

	/**
	 * Sets the internal flag indicating whether the select operation has begun.
	 * This is typically controlled by OASelect but may be overridden externally.
	 *
	 * @param b true to mark as started
	 */
	public void setHasBeenSelected(boolean b) {
		this.bHasBeenStarted = b;
	}

	/**
	 * Returns whether this select has already been executed or initiated via
	 * lazy execution.
	 *
	 * @return true if select has begun
	 */
	public boolean getHasBeenSelected() {
		return this.bHasBeenStarted;
	}

	/**
	 * Assigns the in-memory filter applied after the DataSource returns objects.
	 * This affects which objects are returned by {@link #next()}.
	 *
	 * @param hfi the Hub-based filter to apply
	 */
	public void setHubFilter(OAFilter<TYPE> hfi) {
		this.oaFilter = hfi;
	}

	/**
	 * Returns the Hub-level filter used to screen objects after they are returned
	 * from the DataSource or finder.
	 *
	 * @return the filter, or null if none
	 */
	public OAFilter<TYPE> getHubFilter() {
		return this.oaFilter;
	}

	/**
	 * Sets the in-memory OAFilter applied to results. Equivalent to calling
	 * {@link #setHubFilter(OAFilter)}.
	 *
	 * @param hfi the filter used to evaluate returned objects
	 */
	public void setFilter(OAFilter<TYPE> hfi) {
		this.oaFilter = hfi;
	}

	/**
	 * Returns the in-memory OAFilter applied after fetching objects.
	 *
	 * @return the filter, or null
	 */
	public OAFilter<TYPE> getFilter() {
		return this.oaFilter;
	}

	/**
	 * Sets an optional filter that the DataSource can use to evaluate objects
	 * when it does not support where-clause queries natively.
	 *
	 * @param hfi the DataSource-level filter
	 */
	public void setDataSourceFilter(OAFilter<TYPE> hfi) {
		this.dsFilter = hfi;
	}

	/**
	 * Returns the filter used by the DataSource when query expressions are not
	 * supported or when post-processing is needed.
	 *
	 * @return the DataSource filter, or null
	 */
	public OAFilter<TYPE> getDataSourceFilter() {
		return this.dsFilter;
	}

	/**
	 * Assigns an {@link OAFinder} instance used to produce results through
	 * in-memory traversal rather than querying the DataSource.
	 *
	 * @param finder the finder used for object discovery
	 */
	public void setFinder(OAFinder<?, TYPE> finder) {
		this.finder = finder;
	}

	/**
	 * Returns the currently assigned {@link OAFinder}, or null if selection
	 * will not use finder-based traversal.
	 *
	 * @return the finder instance
	 */
	public OAFinder<?, TYPE> getFinder() {
		return this.finder;
	}

	/**
	 * Sets the order-by clause used to sort query results.
	 *
	 * @param s the order-by clause
	 */
	public void setOrder(String s) {
		order = s;
	}

	/**
	 * Returns the order-by clause applied to the select operation.
	 *
	 * @return the ordering clause, or null
	 */
	public String getOrder() {
		return order;
	}

	/**
	 * Sets the order-by clause for this query. This is an alias for
	 * {@link #setOrder(String)}.
	 *
	 * @param s the order-by clause
	 */
	public void setOrderBy(String s) {
		order = s;
	}

	/**
	 * Returns the order-by clause. This is an alias for {@link #getOrder()}.
	 *
	 * @return the order-by text or null
	 */
	public String getOrderBy() {
		return order;
	}

	/**
	 * Convenience alias for {@link #setOrder(String)}. Sets the property path
	 * used to sort results.
	 *
	 * @param s the sort property path
	 */
	public void setSortBy(String s) {
		setOrder(s);
	}

	/**
	 * Returns the sort property path used to order results, equivalent to
	 * {@link #getOrder()}.
	 *
	 * @return the sort path or null
	 */
	public String getSortBy() {
		return getOrder();
	}

	/**
	 * Alias for {@link #setPassthru(boolean)}. Enables passthru query execution
	 * against the DataSource.
	 *
	 * @param b true to use selectPassthru()
	 */
	public void setPassThru(boolean b) {
		setPassthru(b);
	}

	/**
	 * Sets whether this select should use passthru mode. When enabled, the query
	 * is passed directly to the DataSource without OA property-path interpretation.
	 *
	 * @param b true to use passthru select mode
	 */
	public void setPassthru(boolean b) {
		bPassthru = b;
	}

	/**
	 * Returns whether passthru mode is enabled for this query.
	 *
	 * @return true if passthru mode is active
	 */
	public boolean getPassthru() {
		return bPassthru;
	}

	/**
	 * Alias for {@link #getPassthru()}. Returns true if passthru select mode
	 * is enabled.
	 *
	 * @return true when using passthru selection
	 */
	public boolean getPassThru() {
		return bPassthru;
	}

	/**
	 * Sets whether results should be appended to an existing Hub or collection
	 * rather than replacing its contents.
	 *
	 * @param b true to append results instead of overwriting
	 */
	public void setAppend(boolean b) {
		bAppend = b;
	}

	/**
	 * Returns whether results will be appended to the destination Hub or
	 * collection during binding.
	 *
	 * @return true if append mode is active
	 */
	public boolean getAppend() {
		return bAppend;
	}

	/**
	 * Sets whether the Hub should rewind to its first object when new results
	 * are loaded.
	 *
	 * @param b true to rewind to the first object
	 */
	public void setRewind(boolean b) {
		bRewind = b;
	}

	/**
	 * Returns whether Hub rewind behavior is enabled.
	 *
	 * @return true if Hub rewind is active
	 */
	public boolean getRewind() {
		return bRewind;
	}

	/**
	 * Enables a pre-count operation before selecting. When true, the DataSource
	 * will compute the total number of result rows before iteration begins.
	 *
	 * @param b true to run count() before select()
	 */
	public void setCountFirst(boolean b) {
		this.bCountFirst = b;
	}

	/**
	 * Returns whether a pre-count operation is configured to run before the
	 * main select operation.
	 *
	 * @return true if pre-count is enabled
	 */
	public boolean getCountFirst() {
		return bCountFirst;
	}

	/**
	 * Sets the maximum number of objects to return. A value of zero means
	 * unlimited results.
	 *
	 * @param x the maximum number of rows to fetch
	 */
	public void setMax(int x) {
		max = x;
	}

	/**
	 * Returns the maximum number of objects to retrieve for this query.
	 *
	 * @return the max row limit, or zero if unlimited
	 */
	public int getMax() {
		return max;
	}

	/**
	 * Returns the batch size used when progressively fetching results from the
	 * DataSource or Hub. Defaults to {@link #defaultFetchAmount}.
	 *
	 * @return the fetch amount
	 */
	public int getFetchAmount() {
		return fetchAmount;
	}

	/**
	 * Sets the batch size for progressive loading. A value less than zero is
	 * treated as zero.
	 *
	 * @param fa the number of records to fetch at once
	 */
	public void setFetchAmount(int fa) {
		fetchAmount = Math.max(0, fa);
	}

	/**
	 * Returns the total number of matching objects. Uses the DataSource count()
	 * method when supported, or derives the count from Finder results or the
	 * number of items already read.
	 *
	 * @return the total row count
	 */
	public synchronized int getCount() {
		if (amountCount < 0) {
			OADataSource ds = getDataSource();

			if (!hasMore() && amountRead >= 0) {
				return amountRead;
			} else {
				if (alFinderResults != null) {
					return alFinderResults.size();
				}

				if (ds == null || !ds.getSupportsPreCount()) {
					// load all
					return amountRead + 1; /// only know that there is at least one more
				} else if (bPassthru) {
					amountCount = ds.countPassthru(where, max);
				} else {
					if (whereObject != null) {
						amountCount = ds.count(clazz, whereObject, whereObjectPropertyPath, max);
					} else {
						amountCount = ds.count(clazz, where, params, max);
					}
				}
			}
		}
		return amountCount;
	}

	/**
	 * Returns true if the count has already been computed, either via pre-count
	 * or because iteration has completed and all results are known.
	 *
	 * @return true if row count is available
	 */
	public boolean isCounted() {
		if (amountCount != -1) {
			return true;
		}
		if (!bHasBeenStarted) {
			return false;
		}
		return (!hasMore()); // if hasMore is false, then all are loaded
	}

	/**
	 * Returns the number of objects that have been read so far from this select.
	 * This value increments each time {@link #next()} successfully returns an object.
	 *
	 * @return the number of objects read
	 */
	public int getAmountRead() {
		return (Math.max(0, amountRead));
	}

	/**
	 * Sends a passthru command directly to the underlying {@link OADataSource}.
	 * This is used for DataSource-specific operations that are not part of the
	 * standard OASelect query workflow.
	 *
	 * @param command the command string to execute
	 * @throws RuntimeException if the selectClass is not set or no DataSource exists
	 */
	public void execute(String command) {
		if (clazz == null) {
			throw new RuntimeException("OASelect.execute() needs selectClass set");
		}
		OADataSource ds = getDataSource();
		if (ds == null) {
			throw new RuntimeException("OASelect.execute() cant find datasource for class " + clazz);
		}
		ds.execute(command);
	}

	/**
	 * Assigns a where clause and order-by clause, then performs the select.
	 *
	 * @param where the where clause
	 * @param order the order-by clause
	 */
	public void select(String where, String order) {
		setWhere(where);
		setOrder(order);
		select();
	}

	/**
	 * Assigns a where clause, parameters, and ordering, then executes the select.
	 *
	 * @param where the where clause
	 * @param params query parameter values
	 * @param order the order-by clause
	 */
	public void select(String where, Object[] params, String order) {
		setWhere(where);
		setOrder(order);
		setParams(params);
		select();
	}

	/**
	 * Sets the where clause and immediately performs the select.
	 *
	 * @param where the where clause to use
	 */
	public void select(String where) {
		setWhere(where);
		select();
	}

	/**
	 * Sets the where clause and its parameters, then performs the select.
	 *
	 * @param where the where clause
	 * @param params parameter values for the clause
	 */
	public void select(String where, Object[] params) {
		setWhere(where);
		setParams(params);
		select();
	}

	/**
	 * Cached list of results produced by the {@link OAFinder} when finder-based
	 * selection is used. Iteration proceeds from this list instead of from
	 * a DataSource iterator.
	 */
	private volatile List<TYPE> alFinderResults;

	/**
	 * Cursor used when iterating through {@link #alFinderResults}. Tracks the
	 * next index to return from the finder result list.
	 */
	private int posFinderResults;

	/**
	 * Executes the select operation. Initializes timing, delegates to
	 * {@link #_select()}, and records performance metrics. This method is thread-safe.
	 */
	public synchronized void select() {
		lastReadTime = System.currentTimeMillis();
		_select();
		long x = System.currentTimeMillis() - lastReadTime;
		if (x > 2500) {
			OAPerformance.LOG
					.fine("query took " + x + "ms, class=" + getSelectClass() + ", where=" + getWhere() + ", whereObj=" + getWhereObject());
		}
	}

	/**
	 * Core selection engine for OASelect. Determines whether to use a DataSource
	 * select, passthru query, Finder-based traversal, or Hub-based search.
	 *
	 * Responsibilities include:
	 *  • preparing filters  
	 *  • determining finder vs. DataSource execution  
	 *  • initializing DataSource iterator  
	 *  • computing pre-counts  
	 *  • registering with OASelectManager  
	 */
	protected void _select() {
		if (bHasBeenStarted && !bCancelled) {
			closeQuery(); // cancel previous select
		}
		bHasBeenStarted = true;
		bCancelled = false;
		alFinderResults = null;
		posFinderResults = 0;
		amountRead = 0;
		amountCount = -1;
		bUseFinder = false;

		//qqqqqqq
		// 20221209
		if (!bDirty && !bDirtyWasSet) {
			bDirty = OARuntime.thread().isRefreshing();
		}

		if (hubSearch != null && finder == null) {
			finder = new OAFinder(hubSearch, null);
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(clazz);
			OALinkInfo li = og.hubsInternal().callHubDetailGetLinkInfoFromMasterObjectToDetail(hubSearch);
			if (li != null && !li.getRecursive()) {
				finder.setAllowRecursiveRoot(false);
			}
			bUseFinder = true;
		}

		if (!bUseFinder && finder != null) {
			if ((whereHub != null || whereObject != null) && OAString.isNotEmpty(whereObjectPropertyPath)) {
				OADataSource ds = getDataSource();
				bUseFinder = ds == null || !ds.supportsStorage();
			} else {
				bUseFinder = true;
			}
		}

		// 20140129
		if (bUseFinder) {
			final OAQueryFilter filterQuery = OAString.isEmpty(where) ? null : new OAQueryFilter(clazz, where, params);
			// todo: whereObject, propertyFromWhereObject need to be added to where ??

			OAFilter filter = new OAFilter<TYPE>() {
				@Override
				public boolean isUsed(TYPE obj) {
					if (filterQuery != null && !filterQuery.isUsed(obj)) {
						return false;
					}
					if (dsFilter != null && !dsFilter.isUsed(obj)) {
						return false;
					}
					if (oaFilter != null && !oaFilter.isUsed(obj)) {
						return false;
					}
					if (hubSearch != null) {
						if (!hubSearch.contains(obj)) {
							return false;
						}
					}
					return true;
				}
			};

			OAFilter filterx = finder.getFilter(); // hold
			try {
				finder.addFilter(filter);

				alFinderResults = finder.find();

				// sort the array
				if (alFinderResults.size() > 0) {
					String ord = getSortBy();
					if (OAString.isNotEmpty(ord)) {
						OAComparator comparator = new OAComparator(getSelectClass(), ord, true);
						Collections.sort(alFinderResults, comparator);
					}
				}
			} finally {
				finder.setFilter(filterx);
			}
			return;
		}

		if (clazz == null) {
			throw new RuntimeException("OASelect.select() needs selectClass set");
		}
		OADataSource ds = getDataSource();
		if (ds == null) {
			//throw new RuntimeException("OASelect.select() cant find datasource for class "+clazz);
			cancel();
			return;
		}

		try {
			bIsSelectingNow = true;
			OAObject whereObjx = whereObject;
			if (whereObjx == null && whereHub != null) {
				whereObjx = (OAObject) whereHub.getAO();
			}
			if (whereObjx != null) {
				if (bCountFirst && amountCount < 0) {
					amountCount = ds.count(clazz, where, params, whereObjx, whereObjectPropertyPath, null, max);
				}
				query = ds.select(	clazz, where, params, order, whereObjx, whereObjectPropertyPath, null, max, getDataSourceFilter(),
									getDirty());
			} else {
				if (bPassthru) {
					if (bCountFirst && amountCount < 0) {
						amountCount = ds.countPassthru(where, max);
					}
					query = ds.selectPassthru(clazz, where, order, max, getDataSourceFilter(), getDirty());
				} else {
					if (bCountFirst && amountCount < 0) {
						amountCount = ds.count(clazz, where, params, max);
					}
					query = ds.select(clazz, where, params, order, max, getDataSourceFilter(), getDirty());
				}
			}
			OADataSourceIterator q = query;
			if (q != null) {
				q.hasNext();
			}
		} finally {
			bIsSelectingNow = false;
		}
		OASelectManager.add(this);
	}

	/**
	 * Returns whether a select operation is currently executing. Used to prevent
	 * reentrancy and to coordinate cancellation behavior.
	 *
	 * @return true if select() or _select() is actively running
	 */
	public boolean isSelectingNow() {
		return bIsSelectingNow;
	}

	/**
	 * Ensures that query resources are released when the OASelect instance is
	 * garbage collected. Calls {@link #closeQuery()}.
	 */
	protected void finalize() throws Throwable {
		super.finalize();
		closeQuery();
	}

	/**
	 * Returns the primary SQL/native query string produced by the underlying
	 * {@link OADataSourceIterator}, or null if no query is active.
	 *
	 * @return the DataSource query text
	 */
	public String getDataSourceQuery() {
		if (query == null) {
			return null;
		}
		return query.getQuery();
	}

	/**
	 * Returns an optional secondary query representation, depending on the
	 * DataSource implementation. May include additional debug or internal
	 * statement information.
	 *
	 * @return the secondary query text or null
	 */
	public String getDataSourceQuery2() {
		if (query == null) {
			return null;
		}
		return query.getQuery2();
	}

	/**
	 * Returns the next matching object, applying OAFilter and finder-based
	 * filtering as needed. Automatically skips objects that do not pass
	 * in-memory filters and closes the query when results are exhausted.
	 *
	 * @return the next object, or null when no more results exist
	 */
	public TYPE next() {
		// 20120617 added hubFilter
		TYPE obj;
		for (;;) {
			obj = _next();
			if (obj == null) {
				break;
			}
			if (oaFilter == null || finder != null) {
				break;
			}

			// 20190130
			OASiblingHelper siblingHelper = query == null ? null : query.getSiblingHelper();
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
			boolean bx = ((siblingHelper != null) && srvcOAThreadLocal.addSiblingHelper(siblingHelper));
			try {
				if (oaFilter.isUsed(obj)) {
					break;
				}
			} finally {
				if (bx) {
					srvcOAThreadLocal.removeSiblingHelper(siblingHelper);
				}
			}
		}
		return obj;
	}

	/**
	 * Internal version of {@link #next()}. Retrieves the next object from either
	 * the finder results or the DataSource iterator. Updates internal counters,
	 * enforces max limits, and closes the query when done.
	 *
	 * @return the next object or null if finished
	 */
	public synchronized TYPE _next() {
        if (hasNextCompleted()) return null;
		if (!bHasBeenStarted) {
			select();
		}

		TYPE obj = null;
		if (bUseFinder && finder != null) {
			if (alFinderResults == null) {
				return null;
			}
			int x = alFinderResults.size();
			if (posFinderResults >= x) {
				alFinderResults = null;
				return null;
			}
			obj = alFinderResults.get(posFinderResults++);
		} else {
			if (query == null) {
				return null;
			}
			obj = (TYPE) query.next();
			/*was
			try {
			    obj = (TYPE) query.next();
			}
			catch (Exception e) {
			    obj = null;
			    if (query != null) {
			        LOG.log(Level.WARNING, "", e);
			    }
			}
			*/
		}
		if (obj == null) {
			closeQuery();
		} else {
			amountRead++;
			if (max > 0 && amountRead >= max) {
				closeQuery();
			}
			lastReadTime = System.currentTimeMillis();
		}

		return obj;
	}

	/**
	 * Returns whether this select has been cancelled. Once cancelled, iteration
	 * stops and the underlying query iterator is closed.
	 *
	 * @return true if cancelled
	 */
	public boolean isCancelled() {
		return bCancelled;
	}

	/**
	 * Cancels the select, stops iteration, clears finder results, and closes the
	 * DataSource iterator if active.
	 */
	public void cancel() { // 20200516 removed sync
		//was: public synchronized void cancel() {
		if (!bHasBeenStarted) {
			bCancelled = true;
		} else {
			bCancelled = (bIsSelectingNow || (bHasBeenStarted && hasMore()));
		}
		alFinderResults = null;
		closeQuery();
	}

	/**
	 * Thread-safe version of {@link #cancel()}. Closes the select and releases
	 * associated resources.
	 */
	public synchronized void close() {
		cancel();
	}

	/**
	 * Releases the DataSource iterator, unregisters this select from
	 * {@link OASelectManager}, clears cached finder results, and marks the
	 * iteration as completed.
	 */
	private void closeQuery() {
		if (query != null) {
			query.remove();
			query = null;
		}
		OASelectManager.remove(this);
		alFinderResults = null;
		bHasNextCompleted = true;
	}
	
	/**
	 * Returns true when iteration has completed and no more results will ever
	 * be available. Used to short-circuit redundant hasNext() calls.
	 *
	 * @return true if iteration is finished
	 */
	public boolean hasNextCompleted() {
	    return bHasNextCompleted;
	}

	/**
	 * Alias for {@link #hasMore()}. Returns true if at least one more object
	 * may be available.
	 *
	 * @return true if more results may exist
	 */
	public boolean hasNext() {
		return hasMore();
	}

	/**
	 * Returns true if additional results are available. If the select has not yet
	 * started, it will be automatically executed. Uses finder-based iteration or
	 * DataSource iteration depending on configuration. Closes the query when no
	 * more results exist.
	 *
	 * @return true if another object can be retrieved
	 */
	public synchronized boolean hasMore() {
	    if (hasNextCompleted()) return false;
		if (!bHasBeenStarted) {
			select();
		}

		if (bUseFinder && finder != null) {
			if (alFinderResults == null) {
				return false;
			}
			int x = alFinderResults.size();
			return (posFinderResults < x);
		}

		boolean b = query != null && query.hasNext();
		if (!b) {
			closeQuery();
		}
		return b;
	}

	/**
	 * Returns true if this OASelect represents a full-table scan — meaning no where
	 * clause, no filters, no max limit, no finder, and no whereObject/whereHub.
	 *
	 * @return true if the query selects all objects of the class
	 */
	public boolean isSelectAll() {
		boolean result = false;
		if (!bCancelled && OAString.isEmpty(getWhere()) && getFilter() == null && getFinder() == null && getMax() == 0
				&& getWhereObject() == null && getWhereHub() == null && getSearchHub() == null) {
			result = true;
		}
		return result;
	}

	/**
	 * Returns whether the select has already been initiated, either explicitly
	 * through select() or implicitly through hasMore()/next().
	 *
	 * @return true if selection has begun
	 */
	public synchronized boolean hasBeenStarted() {
		return bHasBeenStarted;
	}

	/**
	 * Returns the timestamp (milliseconds) of when the most recent object was
	 * retrieved. Useful for monitoring long-running queries and idle timeouts.
	 *
	 * @return timestamp of last read
	 */
	public long getLastReadTime() {
		return lastReadTime;
	}

	/**
	 * Returns an Iterator backed by this OASelect. The iterator delegates to
	 * {@link #hasMore()} and {@link #next()}, enabling foreach-style traversal
	 * while preserving all OASelect behaviors.
	 *
	 * @return an Iterator over the selected objects
	 */
	public Iterator<TYPE> iterator() {
		Iterator<TYPE> iter = new Iterator<TYPE>() {
			int pos;
			Object objNext;

			@Override
			public boolean hasNext() {
				boolean b = OASelect.this.hasMore();
				return b;
			}

			@Override
			public void remove() {
			}

			@Override
			public TYPE next() {
				return OASelect.this.next();
			}
		};
		return iter;
	}

	/**
	 * Sets the dirty flag, which instructs the DataSource to bypass caching and
	 * retrieve fresh data. Also marks that the dirty value was explicitly set so
	 * automatic refresh logic does not override it.
	 *
	 * @param b true to enforce uncached DataSource reads
	 */
	public void setDirty(boolean b) {
		bDirtyWasSet = true;
		this.bDirty = b;
	}

	/**
	 * Returns whether the select is marked as dirty, meaning the DataSource should
	 * bypass cached read optimization and fetch fresh results.
	 *
	 * @return true if dirty mode is active
	 */
	public boolean getDirty() {
		return this.bDirty;
	}

	/**
	 * Sets the whereHub and its relationship property path. The Hub's active
	 * object is used as a reverse-path selector for the query.
	 *
	 * @param hubWhere the Hub defining the source object
	 * @param ppFromWhereHub the property path from hubWhere.AO to the select class
	 */
	public void setWhereHub(Hub hubWhere, String ppFromWhereHub) {
		setWhereHub(hubWhere);
		setWhereHubPropertyPath(ppFromWhereHub);
	}

	/**
	 * Sets the Hub whose active object will be used as the whereObject for
	 * reverse-path query construction.
	 *
	 * @param hub the Hub supplying the active object for filtering
	 */
	public void setWhereHub(Hub hub) {
		this.whereHub = hub;
	}

	/**
	 * Returns the Hub whose active object serves as the implicit whereObject
	 * when constructing reverse-path filtering logic.
	 *
	 * @return the whereHub, or null if not configured
	 */
	public Hub getWhereHub() {
		return whereHub;
	}

	/**
	 * Returns the property path used when applying reverse-path selection based on
	 * whereHub.AO. This mirrors the logic used for whereObject-based filtering.
	 *
	 * @return the property path, or null if unspecified
	 */
	public String getWhereHubPropertyPath() {
		return this.whereObjectPropertyPath;
	}

	/**
	 * Sets the property path used to relate whereHub.AO to the select class.
	 * This path defines the reverse-relationship constraint for the query.
	 *
	 * @param pp the property path from Hub.AO to the target type
	 */
	public void setWhereHubPropertyPath(String pp) {
		this.whereObjectPropertyPath = pp;
	}
}
