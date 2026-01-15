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

import java.util.Vector;

import com.viaoa.graph.object.OAObjectInfoService;
import com.viaoa.graph.object.OAObjectKeyService;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectKeyDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.transaction.OATransaction;
import com.viaoa.util.OAFilter;

/**
 * Abstract base class for all OA persistence providers.
 * <p>
 * {@code OADataSource} defines the complete CRUD contract for OA's
 * object–relational and object–remote mapping layer. Subclasses implement
 * the physical persistence logic (e.g., JDBC, REST, memory, distributed).
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Automatic DataSource registration and lookup by model class.</li>
 *   <li>Full CRUD abstraction: insert, update, delete, select, count, execute.</li>
 *   <li>Supports property-path queries translated to native query language.</li>
 *   <li>Transaction and batch-awareness via {@link com.viaoa.transaction.OATransaction}.</li>
 *   <li>Supports chaining multiple DataSources (cache + remote, etc.).</li>
 *   <li>Read-only and ignore-write safety controls.</li>
 *   <li>Fully thread-safe registration and iteration.</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li>OAObjects remain persistence-agnostic.</li>
 *   <li>Supports relational, document, REST, or custom storage providers.</li>
 *   <li>Object queries automatically converted to native DS queries.</li>
 * </ul>
 *
 * @see OASelect
 * @see OADataSourceIterator
 * @see OADataSourceDelegate
 * @see com.viaoa.datasource.jdbc.OADataSourceJDBC
 */
public abstract class OADataSource implements OADataSourceInterface {

	
	/** Optional name assigned to this DataSource. */
	protected String name;
	
	/**
	 * Marks this DataSource as a “last resort” when searching for a supporting
	 * DataSource. Used for catch-all sources.
	 */
	protected boolean bLast;
	
	/**
	 * If true, autonumber properties are assigned when an object is created;
	 * otherwise, assignment occurs when the object is saved.
	 */
	protected boolean bAssignNumberOnCreate = false; 
	
	/**
	 * Optional GUID prefix used when generating sequence-based object keys.
	 */
	protected String guid;

	/**
	 * Flag indicating whether this DataSource is active for lookup and use.
	 */
	protected boolean bEnabled = true;

	/** If true, write operations throw an exception. */
	private boolean bReadOnly;

	/** If true, write operations are silently ignored. */
	private boolean bIgnoreWrites;

	/**
	 * Returns all registered DataSources. Results are cached in {@link #dsAll}
	 * until the registration changes.
	 *
	 * @return array of DataSource instances
	 */
	public static OADataSource[] getDataSources() {
		return OARuntime.get().dataSources().getDataSources();
	}

	/**
	 * Returns the first enabled DataSource that supports the given class.
	 *
	 * @param clazz class to evaluate
	 * @return supporting DataSource or null
	 */
	public static OADataSource getDataSource(Class clazz) {
		return OARuntime.get().dataSources().getDataSource(clazz);
	}

	/**
	 * Returns a DataSource that supports the class and passes the filter.
	 * A DataSource marked as {@code bLast=true} is considered only after others.
	 *
	 * @param clazz class to evaluate
	 * @param filter optional filter used by the DataSource
	 * @return matching DataSource or null
	 */
	public static OADataSource getDataSource(Class clazz, OAFilter filter) {
		return OARuntime.get().dataSources().getDataSource(clazz, filter);
	}

	/**
	 * Sets a GUID prefix used when creating sequence-assigned object keys.
	 *
	 * @param gid GUID prefix
	 */
	public void setGuid(String gid) {
		guid = gid;
	}

	/**
	 * Returns the GUID prefix used for autonumber key generation.
	 *
	 * @return GUID prefix or null
	 */
	public String getGuid() {
		return guid;
	}

	/** Returns whether this DataSource is enabled for lookup. */
	@Override
	public boolean getEnabled() {
		return this.bEnabled;
	}

	/**
	 * Enables or disables this DataSource for lookup.
	 *
	 * @param b true to enable; false to disable
	 */
	@Override
	public void setEnabled(boolean b) {
		this.bEnabled = b;
	}

	/**
	 * Retrieves an object using a String ID value.
	 *
	 * @param clazz object class
	 * @param id String key value
	 * @return matching object or null
	 */
	public static Object getObject(Class clazz, String id) {
    	final OAObjectKeyService srvcObjectKey = OARuntime.get().graph(clazz).objects().getOAObjectKeyService();
		OAObjectKey key = srvcObjectKey.createObjectKey(clazz, (Object) id);
		return getObject(clazz, key);
	}

	/** Retrieves an object using an int ID value. */
	public static Object getObject(Class clazz, int id) {
    	final OAObjectKeyService srvcObjectKey = OARuntime.get().graph(clazz).objects().getOAObjectKeyService();
		OAObjectKey key = srvcObjectKey.createObjectKey(clazz, (Object) id);
		return getObject(clazz, key);
	}

	/** Retrieves an object using a long ID value. */
	public static Object getObject(Class clazz, long id) {
    	final OAObjectKeyService srvcObjectKey = OARuntime.get().graph(clazz).objects().getOAObjectKeyService();
		OAObjectKey key = srvcObjectKey.createObjectKey(clazz, (Object) id);
		return getObject(clazz, key);
	}

	/** Retrieves an object using an arbitrary ID value. */
	public static Object getObject(Class clazz, Object id) {
    	final OAObjectKeyService srvcObjectKey = OARuntime.get().graph(clazz).objects().getOAObjectKeyService();
		OAObjectKey key = srvcObjectKey.createObjectKey(clazz, id);
		return getObject(clazz, key);
	}

	/**
	 * Retrieves an object using a composite key composed of the given ID array.
	 */
	public static Object getObject(Class clazz, Object[] ids) {
		OAObjectKey key = new OAObjectKey(ids);
		return getObject(clazz, key);
	}
	
	/**
	 * Retrieves a single object using the provided OAObjectKey. Uses the
	 * DataSource returned by {@link #getDataSource(Class)}.
	 *
	 * @param clazz object class
	 * @param key object key
	 * @return matching object or null
	 */
	public static Object getObject(Class clazz, OAObjectKey key) {
		if (clazz == null || key == null) {
			return null;
		}
    	final OAObjectInfoService srvcObjectInfo = OARuntime.get().graph(clazz).objects().getOAObjectInfoService();
		OAObjectInfo oi = srvcObjectInfo.getOAObjectInfo(clazz);
		OADataSource ds = getDataSource(clazz);
		if (ds == null) {
			return null;
		}
		return ds.getObject(oi, clazz, key, false);
	}

	/**
	 * Resolves an object using metadata from {@link OAObjectInfo} and a generated
	 * property-path query. Uses {@link #select} to perform the retrieval.
	 *
	 * @param oi object metadata
	 * @param clazz class of object
	 * @param key object key
	 * @param bDirty include dirty objects
	 * @return matching object or null
	 */
	@Override
	public Object getObject(OAObjectInfo oi, Class clazz, OAObjectKey key, boolean bDirty) {
		if (clazz == null || key == null || oi == null) {
			return null;
		}
		OADataSource ds = getDataSource(clazz);
		if (ds == null) {
			return null;
		}

		String[] props = oi.getIdProperties();

		String query = "";
		for (int i = 0; props != null && i < props.length; i++) {
			if (i > 0) {
				query += " && ";
			}
			query += props[i] + " == ?";
		}

		Object obj = null;
		OADataSourceIterator it = ds.select(clazz, query, key.getObjectIds(), "", bDirty);
		if (it != null && it.hasNext()) {
			obj = it.next();
			it.remove();
		}
		return obj;
	}

	/** Sets whether autonumber properties are assigned on create or save. */
	@Override
	public void setAssignIdOnCreate(boolean b) {
		bAssignNumberOnCreate = b;
	}

	/** Returns whether autonumber values are assigned on create. */
	@Override
	public boolean getAssignIdOnCreate() {
		return bAssignNumberOnCreate;
	}

	/** Returns true; subclasses may override to report availability. */
	@Override
	public boolean isAvailable() {
		return true;
	}

	/**
	 * Builds a Vector containing formatted information for all registered
	 * DataSources.
	 *
	 * @return vector containing DataSource info
	 */
	public static Vector getInfo() {
		Vector vec = new Vector(20, 20);
		vec.addElement("OADataSource Info --- ");
		OADataSource[] dss = getDataSources();
		for (int i = 0; i < dss.length; i++) {
			vec.addElement("OADataSource #" + i);
			dss[i].getInfo(vec);
		}
		return vec;
	}

	/**
	 * Adds this DataSource's information to the provided Vector.
	 * Default implementation does nothing.
	 *
	 * @param vec destination vector
	 */
	public void getInfo(Vector vec) {
	}

	//-------------------------------------------------------
	//-------------------------------------------------------
	//-------------------------------------------------------

	/**
	 * Returns the maximum allowed length for the property, or -1 for unlimited.
	 */
	@Override
	public int getMaxLength(Class c, String propertyName) {
		return -1;
	}

	/**
	 * Constructs and registers this DataSource.
	 *
	 * @see #OADataSource(boolean)
	 */
	public OADataSource() {
		this(true);
	}

	/**
	 * Constructs a DataSource with optional registration.
	 *
	 * @param bRegister if true, register this DataSource in the global list
	 */
	public OADataSource(boolean bRegister) {
		if (bRegister) {
			OARuntime.get().dataSources().register(this);
		}
	}


	/**
	 * Returns the global change counter, incremented when DataSource registration
	 * changes.
	 */
	public static int getChangeCounter() {
		return OARuntime.get().dataSources().getChangeCounter();
	}

	/**
	 * Finalizer that closes this DataSource before garbage collection.
	 */
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	/** Closes all registered data sources and clears the global list. */
	public static void closeAll() {
		OARuntime.get().dataSources().closeAll();
	}

	/** Closes this DataSource and removes it from the global list. */
	@Override
	public void close() {
		removeFromList();
	}

	/** Removes this DataSource from the global registry. */
	public void removeFromList() {
		OARuntime.get().dataSources().removeFromList(this);
	}

	/**
	 * Re-adds this DataSource at the given position after it has been closed.
	 *
	 * @param pos insertion index
	 */
	@Override
	public void reopen(int pos) {
		OARuntime.get().dataSources().reopen(pos, this);
	}

	/**
	 * Marks this DataSource to be used last in lookup operations.
	 *
	 * @param b true to use as fallback DataSource
	 */
	public void setLast(boolean b) {
		bLast = b;
	}

	public boolean getLast() {
		return bLast;
	}
	
	/**
	 * Moves this DataSource to the specified position in the global list.
	 *
	 * @param pos target index
	 */
	public void setPosition(int pos) {
		OARuntime.get().dataSources().setPosition(pos, this);
	}

	/**
	 * Returns this DataSource's index in the global list.
	 *
	 * @return position or -1 if not registered
	 */
	public int getPosition() {
		return OARuntime.get().dataSources().getPosition(this);
	}

	/** Sets the name of this DataSource. */
	public void setName(String name) {
		this.name = name;
	}

	/** Returns the name of this DataSource. */
	public String getName() {
		return name;
	}

	/**
	 * Returns the name if defined, otherwise the default object representation.
	 */
	public String toString() {
		if (name == null) {
			return super.toString();
		} else {
			return name;
		}
	}

	/**
	 * Returns whether this DataSource supports persistence for the given class.
	 */
	@Override
	public abstract boolean isClassSupported(Class clazz, OAFilter filter);

	@Override
	public boolean isClassSupported(Class clazz) {
		return isClassSupported(clazz, null);
	}

	/**
	 * Updates link-table relationships for a Many-to-Many mapping.
	 * Uses the hub.masterObject, Hub.getRemovedObjects(), Hub.getAddedObjects() to find out which objects were added or removed. <br>
	 */
	@Override
	public abstract void updateMany2ManyLinks(OAObject masterObject, OAObject[] adds, OAObject[] removes, String propFromMaster);

	/** Inserts a new object into the DataSource. */
	@Override
	public abstract void insert(OAObject obj);

	/** Inserts an object without its reference properties. */
	@Override
	public abstract void insertWithoutReferences(OAObject obj);

	/** Updates an existing object in the DataSource. */
	@Override
	public abstract void update(OAObject obj, String[] includeProperties, String[] excludeProperties);

	/** Update an existing object in the DataSource. */
	@Override
	public void update(OAObject obj) {
		update(obj, null, null);
	}

	/** Deletes an object from the DataSource. */
	@Override
	public abstract void delete(OAObject obj);

	@Override
	public void deleteAll(Class c) {
		// no-op by default
	}

	/**
	 * Used to save an object to DataSource.
	 * <p>
	 * If object is an OAObject, then update() or insert() will be called, else nothing is done.
	 */
	@Override
	public void save(OAObject obj) {
		// if it can be decided to use either insert() or update()
		if (obj == null) {
			return;
		}
		if (obj.getNew()) {
			insert(obj);
		} else {
			update(obj);
		}
	}

	/**
	 * Perform a count on the DataSource using a query.
	 *
	 * @param selectClass Class to perform query on
	 * @param queryWhere  query using property paths based on Object structure.
	 * @see OASelect
	 */
	@Override
	public abstract int count(Class selectClass,
			String queryWhere, Object[] params,
			OAObject whereObject, String propertyFromWhereObject, String extraWhere, int max);

	public int count(Class selectClass, String queryWhere, int max) {
		int x = count(	selectClass,
						queryWhere, null,
						null, null, null, max);
		if (max < 1) {
			return x;
		}
		return Math.min(x, max);
	}

	/**
	 * Performs a COUNT operation using the OA property-path query language.
	 * Delegates to the primary {@link #count(Class, String, Object[], OAObject, String, String, int)}
	 * implementation with no parameters, no where-object, and a max of zero.
	 *
	 * @param selectClass the class to perform the count against
	 * @param queryWhere  the property-path based query expression
	 * @return number of matching objects
	 */
	public int count(Class selectClass, String queryWhere) {
		return count(selectClass, queryWhere, 0);
	}

	/**
	 * Performs a COUNT operation using the given query and parameters.
	 * Delegates to the primary count method with the provided params and max,
	 * without a where-object or extra where-clause.
	 *
	 * @param selectClass the class to count
	 * @param queryWhere  property-path query
	 * @param params      parameters for '?' placeholders
	 * @param max         maximum count limit, or zero for unlimited
	 * @return number of matching objects, capped at max if max > 0
	 */
	public int count(Class selectClass, String queryWhere, Object[] params, int max) {
		int x = count(	selectClass,
						queryWhere, params,
						null, null, null, max);
		if (max < 1) {
			return x;
		}
		return Math.min(x, max);
	}

	/**
	 * Performs a COUNT using a single query parameter. Wraps the parameter
	 * into an array if non-null, then delegates to the primary count method.
	 *
	 * @param selectClass the class to count
	 * @param queryWhere  property-path query
	 * @param param       single parameter value
	 * @param max         maximum count limit, or zero for unlimited
	 * @return number of matching objects, capped at max if max > 0
	 */
	public int count(Class selectClass, String queryWhere, Object param, int max) {
		int x = count(	selectClass,
						queryWhere, param == null ? null : (new Object[] { param }),
						null, null, null, max);
		if (max < 1) {
			return x;
		}
		return Math.min(x, max);
	}

	/**
	 * Performs a COUNT using a query and parameter array with no maximum limit.
	 * Delegates to the primary count method with max set to zero.
	 *
	 * @param selectClass the class to count
	 * @param queryWhere  property-path query
	 * @param params      parameter array
	 * @return number of matching objects
	 */
	public int count(Class selectClass, String queryWhere, Object[] params) {
		return count(	selectClass,
						queryWhere, params,
						null, null, null, 0);
	}

	/**
	 * Performs a COUNT using a single parameter and no maximum limit.
	 * Delegates to the primary count method with max set to zero.
	 *
	 * @param selectClass the class to count
	 * @param queryWhere  property-path query
	 * @param param       single parameter value
	 * @return number of matching objects
	 */
	public int count(Class selectClass, String queryWhere, Object param) {
		return count(	selectClass,
						queryWhere, param == null ? null : (new Object[] { param }),
						null, null, null, 0);
	}

	/**
	 * Counts objects of the given class using a where-object reference and
	 * property name to generate the query. Delegates to the primary count
	 * method without parameters or extra where-clause.
	 *
	 * @param selectClass the class to count
	 * @param whereObject reference object for query construction
	 * @param propertyNameFromWhereObject property used in the where-clause
	 * @param max maximum count limit, or zero for unlimited
	 * @return number of matching objects, capped at max if max > 0
	 */
	public int count(Class selectClass, OAObject whereObject, String propertyNameFromWhereObject, int max) {
		int x = count(	selectClass,
						null, null,
						whereObject, propertyNameFromWhereObject, null, max);
		if (max < 1) {
			return x;
		}
		return Math.min(x, max);
	}

	/**
	 * Counts objects using a where-object and property reference without a
	 * maximum limit. Delegates to the variant with max set to zero.
	 *
	 * @param selectClass the class to count
	 * @param whereObject reference object for query construction
	 * @param propertyNameFromWhereObject property used in the where-clause
	 * @return number of matching objects
	 */
	public int count(Class selectClass, OAObject whereObject, String propertyNameFromWhereObject) {
		return count(selectClass, whereObject, propertyNameFromWhereObject, 0);
	}

	/**
	 * Performs a COUNT using a where-object, optional extra where-clause,
	 * and parameters. Delegates to the primary count method with the supplied
	 * values.
	 *
	 * @param selectClass the class to count
	 * @param whereObject reference object for query construction
	 * @param extraWhere  additional where-clause expression
	 * @param params      parameters for the expression
	 * @param propertyNameFromWhereObject property used in the where-clause
	 * @param max maximum count limit, or zero for unlimited
	 * @return number of matching objects, capped at max if max > 0
	 */
	public int count(Class selectClass, OAObject whereObject, String extraWhere, Object[] params, String propertyNameFromWhereObject,
			int max) {
		int x = count(	selectClass,
						null, params,
						whereObject, propertyNameFromWhereObject, extraWhere, max);
		if (max < 1) {
			return x;
		}
		return Math.min(x, max);
	}

	/**
	 * Performs a COUNT using a where-object and optional extra where-clause
	 * with no maximum limit. Delegates to the variant with max set to zero.
	 *
	 * @param selectClass the class to count
	 * @param whereObject reference object for query construction
	 * @param extraWhere  additional where-clause
	 * @param params      parameters for the expression
	 * @param propertyNameFromWhereObject property for where-condition
	 * @return number of matching objects
	 */
	public int count(Class selectClass, OAObject whereObject, String extraWhere, Object[] params, String propertyNameFromWhereObject) {
		return count(selectClass, whereObject, extraWhere, params, propertyNameFromWhereObject, 0);
	}

	/**
	 * Performs a COUNT using the DataSource's native query language rather
	 * than OA's property-path query system.
	 *
	 * @param selectClass the class to count
	 * @param queryWhere  native query expression
	 * @param max maximum count limit, or zero for unlimited
	 * @return number of matching rows
	 */
	@Override
	public abstract int countPassthru(Class selectClass,
			String queryWhere, int max);

	/**
	 * Performs a native COUNT without specifying a class. Delegates to the
	 * abstract passthru method with selectClass set to null.
	 *
	 * @param queryWhere native query expression
	 * @param max maximum count limit, or zero for unlimited
	 * @return number of matching rows
	 */
	public int countPassthru(String queryWhere, int max) {
		int x = countPassthru(null, queryWhere, max);
		if (max < 1) {
			return x;
		}
		return Math.min(x, max);
	}

	/**
	 * Performs a native COUNT with no maximum limit. Delegates to the
	 * variant that accepts a max value of zero.
	 *
	 * @param queryWhere native query expression
	 * @return number of matching rows
	 */
	public int countPassthru(String queryWhere) {
		return countPassthru(queryWhere, 0);
	}

	/**
	 * Indicates whether the DataSource supports storing, selecting, and
	 * deleting persistent objects.
	 *
	 * @return true if the DataSource supports storage operations
	 */
	@Override
	public abstract boolean supportsStorage();

	/**
	 * Performs a query using the OA property-path query language and returns
	 * an iterator over the matching objects.
	 *
	 * @param selectClass the class of objects to return
	 * @param queryWhere  property-path query expression
	 * @param params      parameter values for query substitution
	 * @param queryOrder  ordering expression
	 * @param whereObject optional reference object used in query construction
	 * @param propertyFromWhereObject property used for where-object filtering
	 * @param extraWhere  additional where-clause
	 * @param max maximum number of objects to return, or zero for unlimited
	 * @param filter optional OAFilter for post-filtering
	 * @param bDirty whether dirty (unsaved) objects should be included
	 * @return iterator yielding matching objects
	 */
	@Override
	public abstract OADataSourceIterator select(Class selectClass,
			String queryWhere, Object[] params, String queryOrder,
			OAObject whereObject, String propertyFromWhereObject, String extraWhere,
			int max, OAFilter filter, boolean bDirty);

	/**
	 * Performs a select for all objects of the given class. Delegates to the
	 * primary select method with no query, parameters, or limits.
	 *
	 * @param selectClass the class of objects to select
	 * @return iterator yielding matching objects
	 */
	public OADataSourceIterator select(Class selectClass) {
		return select(	selectClass,
						(String) null, (Object[]) null, (String) null,
						(OAObject) null, (String) null, (String) null,
						0, (OAFilter) null, false);
	}

	/**
	 * Performs a select using the given query expression. Delegates to the
	 * primary select method without parameters, ordering, or limits.
	 *
	 * @param selectClass the class of objects to select
	 * @param queryWhere property-path query expression
	 * @return iterator yielding matching objects
	 */
	public OADataSourceIterator select(Class selectClass, String queryWhere) {
		return select(	selectClass,
						queryWhere, (Object[]) null, (String) null,
						(OAObject) null, (String) null, (String) null,
						0, (OAFilter) null, false);
	}

	/**
	 * Performs a select using the given query expression and orderBy clause.
	 * Delegates to the primary select method without parameters or limits.
	 *
	 * @param selectClass the class of results
	 * @param queryWhere query expression
	 * @param orderBy ordering expression
	 * @return iterator yielding matching objects
	 */
	public OADataSourceIterator select(Class selectClass, String queryWhere, String orderBy) {
		return select(	selectClass,
						queryWhere, (Object[]) null, orderBy,
						(OAObject) null, (String) null, (String) null,
						0, (OAFilter) null, false);
	}

	/**
	 * Performs a select with query, ordering, max limit, filter, and dirty
	 * flag. Delegates to the primary select method.
	 *
	 * @param selectClass result class
	 * @param queryWhere query expression
	 * @param queryOrder ordering expression
	 * @param max maximum results, or zero for unlimited
	 * @param filter optional OAFilter
	 * @param bDirty whether to include dirty objects
	 * @return iterator yielding matching objects
	 */
	public OADataSourceIterator select(Class selectClass, String queryWhere, String queryOrder, int max, OAFilter filter, boolean bDirty) {
		return select(	selectClass,
						queryWhere, null, queryOrder,
						null, null, null,
						max, filter, bDirty);
	}

	/**
	 * Performs a select using the given query, ordering, max limit, and
	 * dirty flag. Delegates to the primary select method with no filter.
	 *
	 * @param selectClass result class
	 * @param queryWhere query expression
	 * @param queryOrder ordering expression
	 * @param max max results
	 * @param bDirty include dirty objects
	 * @return iterator yielding matching objects
	 */
	public OADataSourceIterator select(Class selectClass, String queryWhere, String queryOrder, int max, boolean bDirty) {
		return select(	selectClass,
						queryWhere, null, queryOrder,
						null, null, null,
						max, null, bDirty);
	}

	/**
	 * Performs a select using the given query, ordering, and dirty flag
	 * with no max limit. Delegates to the primary select method.
	 *
	 * @param selectClass result class
	 * @param queryWhere query expression
	 * @param queryOrder ordering expression
	 * @param bDirty include dirty objects
	 * @return iterator yielding matching objects
	 */
	public OADataSourceIterator select(Class selectClass, String queryWhere, String queryOrder, boolean bDirty) {
		return select(	selectClass,
						queryWhere, null, queryOrder,
						null, null, null,
						0, null, bDirty);
	}

	/**
	 * Performs a select using the given query, parameters, ordering,
	 * maximum limit, and dirty flag. Delegates to the primary select method
	 * without a filter.
	 *
	 * @param selectClass result class
	 * @param queryWhere  property-path query expression
	 * @param params      parameter values
	 * @param queryOrder  ordering expression
	 * @param max         maximum results, or zero for unlimited
	 * @param bDirty      whether to include dirty objects
	 * @return iterator yielding matching objects
	 */
	public OADataSourceIterator select(Class selectClass, String queryWhere, Object[] params, String queryOrder, int max, boolean bDirty) {
		return select(	selectClass,
						queryWhere, params, queryOrder,
						null, null, null,
						max, null, bDirty);
	}

	/**
	 * Performs a select using the given query, parameters, ordering,
	 * and dirty flag with no maximum limit. Delegates to the primary
	 * select method with max set to zero.
	 *
	 * @param selectClass result class
	 * @param queryWhere  query expression
	 * @param params      parameter values
	 * @param queryOrder  ordering expression
	 * @param bDirty      include dirty objects
	 * @return iterator yielding matching objects
	 */
	public OADataSourceIterator select(Class selectClass, String queryWhere, Object[] params, String queryOrder, boolean bDirty) {
		return select(	selectClass,
						queryWhere, params, queryOrder,
						null, null, null,
						0, null, bDirty);
	}

	/**
	 * Performs a select using the given query, parameters, ordering,
	 * maximum limit, filter, and dirty flag. Delegates to the primary
	 * select method.
	 *
	 * @param selectClass result class
	 * @param queryWhere  query expression
	 * @param params      parameter values
	 * @param queryOrder  ordering clause
	 * @param max         maximum results
	 * @param filter      optional OAFilter
	 * @param bDirty      include dirty objects
	 * @return iterator over matching results
	 */
	public OADataSourceIterator select(Class selectClass, String queryWhere, Object[] params, String queryOrder, int max, OAFilter filter,
			boolean bDirty) {
		return select(	selectClass,
						queryWhere, params, queryOrder,
						null, null, null,
						max, filter, bDirty);
	}

	/**
	 * Performs a select using a single parameter value wrapped into an array.
	 * Delegates to the primary select method.
	 *
	 * @param selectClass result class
	 * @param queryWhere  query expression
	 * @param param       single parameter value
	 * @param queryOrder  ordering clause
	 * @param max         maximum results
	 * @param filter      optional OAFilter
	 * @param bDirty      include dirty objects
	 * @return iterator over matching results
	 */
	public OADataSourceIterator select(Class selectClass, String queryWhere, Object param, String queryOrder, int max, OAFilter filter,
			boolean bDirty) {
		return select(	selectClass,
						queryWhere, param == null ? null : (new Object[] { param }), queryOrder,
						null, null, null,
						max, filter, bDirty);
	}

	/**
	 * Performs a select using a single parameter, ordering, maximum limit,
	 * and dirty flag, without a filter. Delegates to the variant that
	 * supports filtering.
	 *
	 * @param selectClass result class
	 * @param queryWhere  query expression
	 * @param param       single parameter value
	 * @param queryOrder  ordering clause
	 * @param max         maximum results
	 * @param bDirty      include dirty objects
	 * @return iterator over matching results
	 */
	public OADataSourceIterator select(Class selectClass, String queryWhere, Object param, String queryOrder, int max, boolean bDirty) {
		return select(selectClass, queryWhere, param, queryOrder, max, null, bDirty);
	}

	/**
	 * Performs a select using a single parameter, ordering clause, and dirty
	 * flag with no maximum limit. Delegates to the variant that accepts max.
	 *
	 * @param selectClass result class
	 * @param queryWhere  query expression
	 * @param param       single parameter
	 * @param queryOrder  ordering clause
	 * @param bDirty      include dirty objects
	 * @return iterator over matching results
	 */
	public OADataSourceIterator select(Class selectClass, String queryWhere, Object param, String queryOrder, boolean bDirty) {
		return select(selectClass, queryWhere, param, queryOrder, 0, null, bDirty);
	}

	/**
	 * Performs a select using a where-object reference, additional
	 * where-clause, parameters, ordering, maximum limit, and optional filter.
	 * Delegates to the primary select method.
	 *
	 * @param selectClass result class
	 * @param whereObject reference object for query construction
	 * @param propertyNameFromWhereObject property name used for linking
	 * @param addToWhere  additional where-clause
	 * @param args        parameter values
	 * @param queryOrder  ordering clause
	 * @param max         maximum results
	 * @param filter      optional OAFilter
	 * @param bDirty      include dirty objects
	 * @return iterator over matching results
	 */
	public OADataSourceIterator select(Class selectClass,
			OAObject whereObject, String propertyNameFromWhereObject, String addToWhere, Object[] args,
			String queryOrder, int max, OAFilter filter, boolean bDirty) {
		return select(	selectClass,
						addToWhere, args, queryOrder,
						whereObject, propertyNameFromWhereObject, null,
						max, filter, bDirty);
	}

	/**
	 * Performs a select using a where-object, extra where text, parameters,
	 * ordering, maximum limit, and dirty flag without a filter. Delegates
	 * to the variant that accepts a filter.
	 *
	 * @param selectClass result class
	 * @param whereObject reference object
	 * @param propertyNameFromWhereObject linking property
	 * @param addToWhere additional where-clause text
	 * @param args       parameter values
	 * @param queryOrder ordering clause
	 * @param max        maximum results
	 * @param bDirty     include dirty objects
	 * @return iterator over matching results
	 */
	public OADataSourceIterator select(Class selectClass, OAObject whereObject,
			String propertyNameFromWhereObject, String addToWhere, Object[] args, String queryOrder, int max, boolean bDirty) {
		return select(selectClass, whereObject, propertyNameFromWhereObject, addToWhere, args, queryOrder, max, null, bDirty);
	}

	/**
	 * Performs a select using a where-object, extra where-clause text,
	 * parameters, ordering, and dirty flag with no maximum limit.
	 *
	 * @param selectClass result class
	 * @param whereObject reference object
	 * @param propertyNameFromWhereObject linking property
	 * @param addToWhere additional where text
	 * @param args       parameters
	 * @param queryOrder ordering clause
	 * @param bDirty     include dirty objects
	 * @return iterator over matching results
	 */
	public OADataSourceIterator select(Class selectClass, OAObject whereObject, String propertyNameFromWhereObject,
			String addToWhere, Object[] args,
			String queryOrder, boolean bDirty) {
		return select(selectClass, whereObject, propertyNameFromWhereObject, addToWhere, args, queryOrder, 0, null, bDirty);
	}

	/**
	 * Performs a select using a where-object, linking property, ordering,
	 * maximum limit, filter, and dirty flag. Delegates to the primary select
	 * method.
	 *
	 * @param selectClass result class
	 * @param whereObject reference object
	 * @param propertyNameFromWhereObject linking property name
	 * @param queryOrder ordering clause
	 * @param max        maximum results
	 * @param filter     optional OAFilter
	 * @param bDirty     include dirty objects
	 * @return iterator over matching results
	 */
	public OADataSourceIterator select(Class selectClass, OAObject whereObject, String propertyNameFromWhereObject, String queryOrder,
			int max, OAFilter filter, boolean bDirty) {
		return select(	selectClass,
						null, null, queryOrder,
						whereObject, propertyNameFromWhereObject, null,
						max, filter, bDirty);
	}

	/**
	 * Performs a select using a where-object, linking property, ordering,
	 * maximum limit, and dirty flag without a filter. Delegates to the
	 * variant supporting filters.
	 *
	 * @param selectClass result class
	 * @param whereObject reference object
	 * @param propertyNameFromWhereObject linking property
	 * @param queryOrder ordering clause
	 * @param max        maximum results
	 * @param bDirty     include dirty objects
	 * @return iterator over matching results
	 */
	public OADataSourceIterator select(Class selectClass, OAObject whereObject, String propertyNameFromWhereObject, String queryOrder,
			int max, boolean bDirty) {
		return select(selectClass, whereObject, propertyNameFromWhereObject, queryOrder, max, null, bDirty);
	}

	/**
	 * Performs a select using a where-object, linking property, ordering,
	 * and dirty flag with no maximum limit.
	 *
	 * @param selectClass result class
	 * @param whereObject reference object
	 * @param propertyNameFromWhereObject linking property
	 * @param queryOrder ordering clause
	 * @param bDirty     include dirty objects
	 * @return iterator over matching results
	 */
	public OADataSourceIterator select(Class selectClass, OAObject whereObject, String propertyNameFromWhereObject, String queryOrder,
			boolean bDirty) {
		return select(selectClass, whereObject, propertyNameFromWhereObject, queryOrder, 0, null, bDirty);
	}

	// hasNext(), next(), remove() (used to close)

	/**
	 * Performs a select using the DataSource's native query language rather
	 * than OA's property-path query system.
	 *
	 * @param selectClass class of returned objects
	 * @param queryWhere  native query expression
	 * @param queryOrder  native ordering expression
	 * @param max         maximum results, or zero for unlimited
	 * @param filter      optional filter
	 * @param bDirty      include dirty objects
	 * @return iterator over matching results
	 */
	public abstract OADataSourceIterator selectPassthru(Class selectClass,
			String queryWhere, String queryOrder,
			int max, OAFilter filter, boolean bDirty);

	/**
	 * Performs a native select using the given query, maximum limit, filter,
	 * and dirty flag. Delegates to the primary passthru select method.
	 *
	 * @param selectClass result class
	 * @param query       native query expression
	 * @param max         maximum results
	 * @param filter      optional filter
	 * @param bDirty      include dirty objects
	 * @return iterator over matching results
	 */
	public OADataSourceIterator selectPassthru(Class selectClass, String query, int max, OAFilter filter, boolean bDirty) {
		return selectPassthru(	selectClass,
								query, null,
								max, filter, bDirty);
	}

	/**
	 * Performs a native select with a maximum limit and dirty flag without a
	 * filter. Delegates to the variant that accepts a filter.
	 *
	 * @param selectClass result class
	 * @param query       native query
	 * @param max         maximum results
	 * @param bDirty      include dirty objects
	 * @return iterator over results
	 */
	public OADataSourceIterator selectPassthru(Class selectClass, String query, int max, boolean bDirty) {
		return selectPassthru(	selectClass,
								query, null,
								max, null, bDirty);
	}

	/**
	 * Performs a native select with no maximum limit. Delegates to the
	 * variant that accepts a max value.
	 *
	 * @param selectClass result class
	 * @param query       native query
	 * @param bDirty      include dirty objects
	 * @return iterator over results
	 */
	public OADataSourceIterator selectPassthru(Class selectClass, String query, boolean bDirty) {
		return selectPassthru(	selectClass,
								query, null,
								0, null, bDirty);
	}

	/**
	 * Performs a native select using the given query, ordering, maximum
	 * limit, and dirty flag. Delegates to the variant that accepts a filter.
	 *
	 * @param selectClass result class
	 * @param query       native query expression
	 * @param queryOrder  ordering expression
	 * @param max         maximum results
	 * @param bDirty      include dirty objects
	 * @return iterator over results
	 */
	public OADataSourceIterator selectPassthru(Class selectClass, String query, String queryOrder, int max, boolean bDirty) {
		return selectPassthru(	selectClass,
								query, queryOrder,
								max, null, bDirty);
	}

	/**
	 * Performs a native select using a query and ordering expression with
	 * no maximum limit. Delegates to the variant that accepts a max value.
	 *
	 * @param selectClass result class
	 * @param query       native query
	 * @param queryOrder  ordering expression
	 * @param bDirty      include dirty objects
	 * @return iterator over results
	 */
	public OADataSourceIterator selectPassthru(Class selectClass, String query, String queryOrder, boolean bDirty) {
		return selectPassthru(	selectClass,
								query, queryOrder,
								0, null, bDirty);
	}

	/**
	 * Executes a native command on the underlying DataSource.
	 *
	 * @param command native command to execute
	 * @return result of the command, if any
	 */
	@Override
	public abstract Object execute(String command);

	/**
	 * Assigns an identifier value to the given object. Called during object
	 * initialization for DataSources that manage ID assignment.
	 *
	 * @param obj the object receiving an assigned id
	 */
	@Override
	public abstract void assignId(OAObject obj);

	/**
	 * Indicates whether the DataSource will assign a value for the specified
	 * property before the object is saved. Defaults to false.
	 *
	 * @param object        the OAObject being evaluated
	 * @param propertyName  the property to check
	 * @return true if the DataSource will assign the value, otherwise false
	 */
	@Override
	public boolean willCreatePropertyValue(OAObject object, String propertyName) {
		return false;
	}

	/**
	 * Indicates whether object identifier properties may be changed after
	 * the object has been saved. Defaults to true.
	 *
	 * @return true if ID values are allowed to change
	 */
	@Override
	public boolean getAllowIdChange() {
		return true;
	}

	/**
	 * Retrieves the byte[] (BLOB) value of the specified property from the
	 * underlying DataSource.
	 *
	 * @param obj           the OAObject containing the property
	 * @param propertyName  name of the property to retrieve
	 * @return BLOB value as byte array, or null if not found
	 */
	@Override
	public abstract byte[] getPropertyBlobValue(OAObject obj, String propertyName);

	/**
	 * Indicates whether this DataSource supports pre-count operations,
	 * allowing the number of rows to be determined before a select.
	 * Defaults to true.
	 *
	 * @return true if pre-count operations are supported
	 */
	@Override
	public boolean getSupportsPreCount() {
		return true;
	}

	/**
	 * Determines whether batch update operations are permitted based on the
	 * current active transaction. Returns true if the active transaction has
	 * batch mode enabled.
	 *
	 * @return true if batch updates are allowed
	 */
	public boolean isAllowingBatch() {
		final OATransaction tran = OARuntime.get().threadLocals().getTransaction();
		final boolean bIsForBatch = tran != null && tran.getUseBatch();
		return bIsForBatch;
	}

	/**
	 * Indicates whether there is an active OATransaction for the current
	 * thread.
	 *
	 * @return true if in an active transaction
	 */
	public boolean isInTransaction() {
		final OATransaction tran = OARuntime.get().threadLocals().getTransaction();
		return (tran != null);
	}

	/**
	 * Sets the read-only flag. When false, write operations such as insert,
	 * update, and delete will throw an exception.
	 *
	 * @param readOnly true to enable read-only mode
	 */
	public void setReadOnly(boolean readOnly) {
		this.bReadOnly = readOnly;
	}

	/**
	 * Returns whether this DataSource is in read-only mode.
	 *
	 * @return true if read-only
	 */
	public boolean getReadOnly() {
		return bReadOnly;
	}

	/**
	 * Sets whether write operations should be ignored. When true, insert,
	 * update, and delete operations are silently skipped.
	 *
	 * @param ignoreWrites true to ignore write operations
	 */
	public void setIgnoreWrites(boolean ignoreWrites) {
		this.bIgnoreWrites = ignoreWrites;
	}

	/**
	 * Returns whether write operations are ignored. If ignore-writes is
	 * enabled but the current transaction allows writes for read-only
	 * DataSources, then false is returned.
	 *
	 * @return true if write operations are ignored
	 */
	public boolean getIgnoreWrites() {
		if (bIgnoreWrites) {
			final OATransaction tran = OARuntime.get().threadLocals().getTransaction();
			if (tran != null && tran.getAllowWritesIfDsIsReadonly()) {
				return false;
			}
		}
		return bIgnoreWrites;
	}

}
