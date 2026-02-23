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

import java.util.Iterator;

import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectKey;
import com.viaoa.util.OAFilter;

/**
 * Defines the standard contract that all OA persistence providers must implement.
 * <p>
 * {@code OADataSourceInterface} formalizes the CRUD, query, and configuration
 * operations used by {@link com.viaoa.datasource.OADataSource} and its subclasses.
 * Implementations may represent relational databases, in-memory stores, REST
 * services, distributed caches, or any other persistence mechanism.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li><b>CRUD Operations:</b> Insert, update, delete, and select
 *       {@link com.viaoa.object.OAObject} instances from the underlying store.</li>
 *   <li><b>ID Management:</b> Assign unique identifiers and control
 *       auto-increment or GUID strategies.</li>
 *   <li><b>Query Translation:</b> Convert object‐based filters and property
 *       paths into the native query language (SQL, REST parameters, etc.).</li>
 *   <li><b>Transaction Support:</b> Optionally participate in
 *       {@link com.viaoa.transaction.OATransaction} contexts and batching.</li>
 *   <li><b>Configuration:</b> Provide connection details, read‐only flags,
 *       and query capabilities to higher-level components.</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li>Provide a single, uniform interface for all persistence types.</li>
 *   <li>Keep {@link com.viaoa.object.OAObject} independent of storage details.</li>
 *   <li>Enable new DataSource types (e.g., JDBC, REST, cache, distributed)
 *       to plug in without changing framework or model code.</li>
 * </ul>
 *
 * <h2>Typical Implementations</h2>
 * <ul>
 *   <li>{@link com.viaoa.datasource.jdbc.OADataSourceJDBC} — SQL/relational backend</li>
 *   <li>{@link com.viaoa.datasource.clientserver.OADataSourceClient} — distributed sync client</li>
 *   <li>{@link com.viaoa.datasource.rest.OADataSourceRest} — REST service connector</li>
 *   <li>{@link com.viaoa.datasource.memory.OADataSourceMemory} — in-memory transient store</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * public class MyCustomDataSource implements OADataSourceInterface {
 *     @Override
 *     public void insert(OAObject obj) { ... }
 *     @Override
 *     public Iterator<OAObject> select(Class<?> type, OAFilter filter) { ... }
 * }
 * }</pre>
 *
 * @see com.viaoa.datasource.OADataSource
 * @see com.viaoa.datasource.OADataSourceIterator
 * @see com.viaoa.datasource.OASelect
 */
public interface OADataSourceInterface {

	/**
	 * Determines whether the DataSource supports persistence operations for
	 * the specified class.
	 *
	 * @param clazz the class to evaluate
	 * @return true if the class is supported
	 */
	boolean isClassSupported(Class<?> clazz);

	/**
	 * Determines whether the DataSource supports the specified class and
	 * satisfies the additional filter criteria.
	 *
	 * @param clazz  the class to evaluate
	 * @param filter optional filter for fine-grained support evaluation
	 * @return true if supported under the given filter
	 */
	<T> boolean isClassSupported(Class<?> clazz, OAFilter<T> filter);

	/**
	 * Indicates whether the DataSource supports persistent storage operations
	 * such as insert, update, delete, and select.
	 *
	 * @return true if storage operations are supported
	 */
	boolean supportsStorage();

	/**
	 * Indicates whether the DataSource is currently available for use.
	 *
	 * @return true if available
	 */
	boolean isAvailable();

	/**
	 * Returns whether this DataSource is enabled for use.
	 *
	 * @return true if enabled
	 */
	boolean getEnabled();

	/**
	 * Enables or disables this DataSource.
	 *
	 * @param b true to enable; false to disable
	 */
	void setEnabled(boolean b);

	/**
	 * Indicates whether object identifiers may be modified after assignment.
	 *
	 * @return true if ID changes are permitted
	 */
	boolean getAllowIdChange();

	/**
	 * Sets whether new objects should be assigned identifiers at creation
	 * time rather than at persistence.
	 *
	 * @param b true to assign IDs on object creation
	 */
	void setAssignIdOnCreate(boolean b);

	/**
	 * Returns whether identifiers are assigned when objects are created.
	 *
	 * @return true if IDs are assigned at creation time
	 */
	boolean getAssignIdOnCreate();

	/**
	 * Assigns a unique identifier to the given object using the DataSource's
	 * ID generation strategy.
	 *
	 * @param object the object receiving an ID
	 */
	void assignId(OAObject object);

	/**
	 * Indicates whether the DataSource can provide record counts efficiently
	 * before performing a full select operation.
	 *
	 * @return true if pre-count operations are supported
	 */
	boolean getSupportsPreCount();

	/**
	 * Closes the DataSource and releases any underlying resources such as
	 * connections, memory structures, or network handles.
	 */
	void close();

	/**
	 * Reopens the DataSource after a previous close operation.
	 *
	 * @param pos optional position or configuration index for reinitialization
	 */
	void reopen(int pos);

	/**
	 * Indicates whether the DataSource will assign a value for the specified
	 * property before the object is persisted.
	 *
	 * @param object       the object being checked
	 * @param propertyName the property to evaluate
	 * @return true if the DataSource will create the property's value
	 */
	boolean willCreatePropertyValue(OAObject object, String propertyName);

	/**
	 * Saves the given object, performing insert or update operations as needed.
	 *
	 * @param obj the object to save
	 */
	void save(OAObject obj);

	/**
	 * Updates the specified object, including and excluding the provided
	 * sets of properties.
	 *
	 * @param object            the object to update
	 * @param includeProperties properties that must be updated
	 * @param excludeProperties properties that must not be updated
	 */
	void update(OAObject object, String[] includeProperties, String[] excludeProperties);

	/**
	 * Updates all modified properties of the specified object.
	 *
	 * @param obj the object to update
	 */
	void update(OAObject obj);

	/**
	 * Inserts the given object into the underlying store.
	 *
	 * @param object the object to insert
	 */
	void insert(OAObject object);

	/**
	 * Inserts the object without processing or inserting any referenced
	 * objects or relationships.
	 *
	 * @param obj the object to insert
	 */
	void insertWithoutReferences(OAObject obj);

	/**
	 * Deletes the specified object from the underlying store.
	 *
	 * @param object the object to delete
	 */
	void delete(OAObject object);

	/**
	 * Deletes all objects of the specified class from the underlying store.
	 *
	 * @param c the class whose objects should be removed
	 */
	void deleteAll(Class c);

	/**
	 * Updates many-to-many link relationships for the master object by applying
	 * additions and removals through the specified link property.
	 *
	 * @param masterObject     the owning/master object
	 * @param adds             objects to add to the relationship
	 * @param removes          objects to remove from the relationship
	 * @param propFromMaster   property name defining the relationship
	 */
	void updateMany2ManyLinks(OAObject masterObject, OAObject[] adds, OAObject[] removes, String propFromMaster);

	/**
	 * Selects objects using a property-path query expression and optional
	 * filtering, ordering, limits, and where-object linkage.
	 *
	 * @param selectClass               class of objects to return
	 * @param queryWhere               property-path query expression
	 * @param params                   parameter values for query placeholders
	 * @param queryOrder               ordering expression
	 * @param whereObject              reference object for query construction
	 * @param propertyFromWhereObject  property linking the whereObject
	 * @param extraWhere               additional where-clause fragment
	 * @param max                      maximum number of results, or zero for unlimited
	 * @param filter                   optional filter applied after selection
	 * @param bDirty                   true to populate all properties even if already loaded
	 * @return iterator over matching objects
	 */
	<T> Iterator<T> select(Class<T> selectClass,
			String queryWhere, Object[] params, String queryOrder,
			OAObject whereObject, String propertyFromWhereObject, String extraWhere,
			int max, OAFilter<T> filter, boolean bDirty);

	/**
	 * Performs a select using the DataSource's native query language rather
	 * than OA's property-path system.
	 *
	 * @param selectClass class of returned objects
	 * @param queryWhere  native query expression
	 * @param queryOrder  native ordering expression
	 * @param max         maximum number of results
	 * @param filter      optional post-selection filter
	 * @param bDirty      true to fully populate properties
	 * @return iterator over matching objects
	 */
	<T> Iterator<T> selectPassthru(Class<T> selectClass,
			String queryWhere, String queryOrder,
			int max, OAFilter<T> filter, boolean bDirty);

	/**
	 * Executes a native command on the underlying DataSource.
	 *
	 * @param command the native command to execute
	 * @return result returned by the command, if any
	 */
	Object execute(String command);

	/**
	 * Counts objects matching the specified query, parameters, where-object
	 * linkage, and optional extra where clause.
	 *
	 * @param selectClass               class whose objects are counted
	 * @param queryWhere               property-path query expression
	 * @param params                   parameters for the expression
	 * @param whereObject              reference object for query construction
	 * @param propertyFromWhereObject  property linking the whereObject
	 * @param extraWhere               additional where-clause fragment
	 * @param max                      maximum count limit, or zero for unlimited
	 * @return number of matching objects
	 */
	int count(Class<?> selectClass,
			String queryWhere, Object[] params,
			OAObject whereObject, String propertyFromWhereObject, String extraWhere, int max);

	/**
	 * Performs a count operation using the DataSource’s native query language.
	 *
	 * @param selectClass class being counted
	 * @param queryWhere  native where-clause
	 * @param max         maximum count, or zero for unlimited
	 * @return number of matching objects
	 */
	int countPassthru(Class<?> selectClass,
			String queryWhere, int max);

	/**
	 * Retrieves an object using metadata, class information, and its object key.
	 *
	 * @param oi     metadata describing the object type
	 * @param clazz  class of the object to retrieve
	 * @param key    object key identifying the instance
	 * @param bDirty true to fully populate the object
	 * @return the matching object, or null if not found
	 */
	<T> T getObject(OAObjectInfo oi, Class<T> clazz, OAObjectKey key, boolean bDirty);

	/**
	 * Returns the raw BLOB value for the specified property of the given object.
	 *
	 * @param obj           the object containing the property
	 * @param propertyName  the BLOB property to retrieve
	 * @return byte array representing the BLOB value, or null if unavailable
	 */
	byte[] getPropertyBlobValue(OAObject obj, String propertyName);

	/**
	 * Returns the maximum allowed length for the specified property, as
	 * defined by the underlying DataSource or schema.
	 *
	 * @param c             the class containing the property
	 * @param propertyName  the property to evaluate
	 * @return maximum length supported for that property
	 */
	int getMaxLength(Class<?> c, String propertyName);

	/**
	 * Hook for implementations to perform internal corruption checks.
	 * Default implementation performs no action.
	 *
	 * @throws Exception if corruption detection fails
	 */
	default void checkForCorruption() throws Exception {
	}

	/**
	 * Performs a backup of the underlying DataSource using the specified
	 * location. Default implementation performs no action.
	 *
	 * @param location location to store the backup
	 * @throws Exception if backup fails
	 */
	default void backup(String location) throws Exception {
	}

	/**
	 * Restores the DataSource from the given location. Default implementation
	 * performs no action.
	 *
	 * @param location backup source location
	 * @throws Exception if restore fails
	 */
	default void restore(String location) throws Exception {
	}

	/**
	 * Performs optional DataSource compression or optimization.
	 * Default implementation performs no action.
	 *
	 * @throws Exception if compression fails
	 */
	default void compress() throws Exception {
	}

	/**
	 * Indicates whether this DataSource represents a client that must
	 * connect to a remote server. Default is false.
	 *
	 * @return false unless overridden by an implementation
	 */
	default boolean isClient() {
		return false;
	}

}
