/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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

	boolean isClassSupported(Class clazz);

	boolean isClassSupported(Class clazz, OAFilter filter);

	boolean supportsStorage();

	boolean isAvailable();

	boolean getEnabled();

	void setEnabled(boolean b);

	boolean getAllowIdChange();

	void setAssignIdOnCreate(boolean b);

	boolean getAssignIdOnCreate();

	void assignId(OAObject object);

	boolean getSupportsPreCount();

	void close();

	void reopen(int pos);

	boolean willCreatePropertyValue(OAObject object, String propertyName);

	void save(OAObject obj);

	void update(OAObject object, String[] includeProperties, String[] excludeProperties);

	void update(OAObject obj);

	void insert(OAObject object);

	void insertWithoutReferences(OAObject obj);

	void delete(OAObject object);

	void deleteAll(Class c);

	void updateMany2ManyLinks(OAObject masterObject, OAObject[] adds, OAObject[] removes, String propFromMaster);

	/**
	 * @param selectClass
	 * @param queryWhere              where clause for selecting objects.
	 * @param params                  param values for "?" in the queryWhere.
	 * @param queryOrder              sort order
	 * @param whereObject             master object to select from.
	 * @param propertyFromWhereObject
	 * @param extraWhere              added to the query.
	 * @param max
	 * @param filter                  this can be used if the datasource does not support a way to query for the results.
	 * @param bDirty                  true if objects should be fully populated, even if they are already loaded (in cache, etc).
	 * @return
	 */
	Iterator select(Class selectClass,
			String queryWhere, Object[] params, String queryOrder,
			OAObject whereObject, String propertyFromWhereObject, String extraWhere,
			int max, OAFilter filter, boolean bDirty);

	Iterator selectPassthru(Class selectClass,
			String queryWhere, String queryOrder,
			int max, OAFilter filter, boolean bDirty);

	Object execute(String command);

	int count(Class selectClass,
			String queryWhere, Object[] params,
			OAObject whereObject, String propertyFromWhereObject, String extraWhere, int max);

	int countPassthru(Class selectClass,
			String queryWhere, int max);

	Object getObject(OAObjectInfo oi, Class clazz, OAObjectKey key, boolean bDirty);

	byte[] getPropertyBlobValue(OAObject obj, String propertyName);

	int getMaxLength(Class c, String propertyName);

	default void checkForCorruption() throws Exception {
	}

	default void backup(String location) throws Exception {
	}

	default void restore(String location) throws Exception {
	}

	default void compress() throws Exception {
	}

	/**
	 * Is this a client computer that needs to remotely connect to datasource on a server.
	 */
	default boolean isClient() {
		return false;
	}

}
