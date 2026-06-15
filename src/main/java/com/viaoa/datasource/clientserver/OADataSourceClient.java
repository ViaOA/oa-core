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
package com.viaoa.datasource.clientserver;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.OADataSourceIterator;
import com.viaoa.datasource.objectcache.ObjectCacheIterator;
import com.viaoa.filter.OAFilter;
import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.graph.sibling.OASiblingHelper;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.remote.RemoteClientInterface;

/*qqqqqqqqqqqqqqqq
CODEX

 3. OADataSourceClient registration path
      - Exact path: OASyncService.createClient() creates new OADataSourceClient(og.getPackageName()); the
        constructor only assigns packageName; no registration occurs; later OASelect.getDataSource() calls
        OARuntime.datasource().get(clazz, filter) and can return null.
      - Why bug: sync client datasource can be created but never discoverable through the runtime datasource
        registry, causing silent select cancellation/no datasource behavior.
      - Minimal fix: either register OADataSourceClient when sync creates it, or restore constructor/creation-time
        registration if that is the intended datasource contract.
      - Suggested test: start client sync path, then assert OARuntime.datasource().get(SomeServerBackedClass.class)
        returns the client datasource.


1. RemoteDataSource.getDataSource() / getDataSource(Class)
      - Exact path: RemoteDataSource.datasource(...) handles classless commands like IS_AVAILABLE,
        GET_ASSIGN_ID_ON_CREATE, SUPPORTSSTORAGE, and EXECUTE by calling getDataSource(). That delegates to
        getDataSource(null). The fallback-to-first-datasource block is now commented out, so getDataSource(null)
        always returns null.
      - Why bug: the fix correctly prevents unsupported class-specific commands from falling through to an unrelated
        datasource, but it also disables valid classless datasource commands that historically require the default/
        first datasource.
      - Semantic/invariant violated: class-specific routing must require class support; classless datasource
        commands may still use the default datasource.
      - Minimal fix: split the behavior:

        protected OADataSource getDataSource(Class c) {
            if (c == null) return getDataSource();
            return OARuntime.datasource().get(c);
        }

        protected OADataSource getDataSource() {
            for (OADataSource ds : OARuntime.datasource().getAll()) {
                return ds;
            }
            return null;
        }
        Or equivalent, as long as class-specific calls do not fallback and classless calls still can.

      - Suggested test: register one datasource, call remote SUPPORTSSTORAGE/IS_AVAILABLE/EXECUTE and verify it
        reaches that datasource; separately verify unsupported class-specific IS_CLASS_SUPPORTED returns false.


4. src/main/java/com/viaoa/datasource/clientserver/OADataSourceClient.java, getMaxLength(...)
      - Exact execution path: multiple threads call getMaxLength concurrently; hmMax is a plain HashMap mutated
        without synchronization.
      - Why concrete bug: datasource clients are runtime shared infrastructure; concurrent metadata lookups can race
        and corrupt/cache stale values unpredictably.
      - Minimal fix: use ConcurrentHashMap<String, Integer> or synchronize the cache access.
      - Suggested test: concurrent repeated getMaxLength calls for different class/property keys using a fake remote
        client; verify stable results and no map corruption.


*/

/**
 * Client-side {@link com.viaoa.datasource.OADataSource} implementation that forwards
 * all data access requests to a remote OA Server through {@link com.viaoa.sync.remote.RemoteClientInterface}.
 * <p>
 * {@code OADataSourceClient} enables distributed OA applications where client objects
 * transparently interact with a remote server-side {@code OADataSource}. All CRUD
 * and query operations are marshaled via the OA synchronization layer.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Transparent remote delegation for all {@code OADataSource} operations.</li>
 *   <li>Supports insert, update, delete, count, and select with server iteration.</li>
 *   <li>Automatic connection acquisition via {@link com.viaoa.sync.OASyncDelegate}.</li>
 *   <li>Caches per-class metadata (max length, class support flags).</li>
 *   <li>Fallback to local {@link com.viaoa.datasource.objectcache.ObjectCacheIterator}
 *       when the class is locally cached.</li>
 * </ul>
 *
 * <h2>Remote Execution</h2>
 * Each method uses an operation code constant (e.g., {@code INSERT}, {@code DELETE})
 * to call {@link RemoteClientInterface#datasource(int, Object[])} or
 * {@link RemoteClientInterface#datasourceReturnOnQueue(int, Object[])}.
 *
 * <h2>Iterator Behavior</h2>
 * Remote queries return lightweight iterator proxies that fetch results from
 * the server in batches, automatically updating local caches through
 * {@link com.viaoa.object.OAObjectCSDelegate}.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * OADataSourceClient dsClient = new OADataSourceClient();
 * dsClient.insert(myOrder);
 * OADataSourceIterator it = dsClient.select(Order.class, "status='Open'", null, null, null, null, null, 100, null, false);
 * }</pre>
 *
 * @see com.viaoa.datasource.OADataSource
 * @see com.viaoa.sync.remote.RemoteClientInterface
 * @see com.viaoa.sync.OASyncDelegate
 */
public class OADataSourceClient extends OADataSource {
	/**
	 * Cache mapping classes to Boolean values indicating whether each class
	 * is supported by the remote datasource. Entries are populated after
	 * querying the remote server.
	 */
	private Hashtable hashClass = new Hashtable();
	
	/**
	 * Cached reference to the remote client used for executing datasource
	 * operations against the OA Server. Lazily initialized on first access.
	 */
	private RemoteClientInterface remoteClientSync;

	/**
	 * Operation code used to query the remote datasource to determine whether
	 * it is currently available.
	 */
	public static final int IS_AVAILABLE = 0;

	/**
	 * Operation code used to request the maximum length of a property from
	 * the remote datasource.
	 */
	public static final int MAX_LENGTH = 1;

	/**
	 * Operation code used to check whether a class is supported by the
	 * remote datasource.
	 */
	public static final int IS_CLASS_SUPPORTED = 2;

	/**
	 * Operation code used to insert an object on the remote datasource
	 * without processing its references.
	 */
	public static final int INSERT_WO_REFERENCES = 3;

	/**
	 * Operation code used to update many-to-many link relationships
	 * on the remote datasource.
	 */
	public static final int UPDATE_MANY2MANY_LINKS = 4;

	/**
	 * Operation code used to insert an object on the remote datasource.
	 */
	public static final int INSERT = 5;

	/**
	 * Operation code used to update an object on the remote datasource.
	 */
	public static final int UPDATE = 6;

	/**
	 * Operation code used to delete an object on the remote datasource.
	 */
	public static final int DELETE = 7;

	/**
	 * Operation code used to perform a save operation on the remote datasource.
	 */
	public static final int SAVE = 8;

	/**
	 * Operation code used to count records matching a query on the remote datasource.
	 */
	public static final int COUNT = 10;

	/**
	 * Operation code used for passthrough count operations on the remote datasource.
	 */
	public static final int COUNTPASSTHRU = 11;

	/**
	 * Operation code used to determine whether the remote datasource supports
	 * storage operations (insert, update, delete).
	 */
	public static final int SUPPORTSSTORAGE = 13;

	//public static final int CONVERTTOSTRING = 14;
	//public static final int CONVERTTOSTRING2 = 15;

	/**
	 * Operation code used to execute an arbitrary command on the remote datasource.
	 */
	public static final int EXECUTE = 16;

	/**
	 * Operation code used to determine whether the remote datasource will
	 * create a value for a specific property.
	 */
	public static final int WILLCREATEPROPERTYVALUE = 17;

	/**
	 * Operation code used by iterator proxies to determine whether more results
	 * are available from the remote datasource.
	 */
	public static final int IT_HASNEXT = 18;

	/**
	 * Operation code used by iterator proxies to request the next batch of items
	 * from the remote datasource.
	 */
	public static final int IT_NEXT = 19;

	/**
	 * Operation code used by iterator proxies to remove an item from the remote
	 * datasource.
	 */
	public static final int IT_REMOVE = 20;

	/**
	 * Operation code used to perform a select (query) operation on the remote
	 * datasource, returning an iterator token for fetching results.
	 */
	public static final int SELECT = 21;

	/**
	 * Operation code used to perform a passthrough select query on the remote
	 * datasource, returning an iterator token for result retrieval.
	 */
	public static final int SELECTPASSTHRU = 22;

	/**
	 * Operation code used to retrieve from the remote datasource whether IDs
	 * should be automatically assigned when objects are created.
	 */
	public static final int GET_ASSIGN_ID_ON_CREATE = 24;

	/**
	 * Operation code used to instruct the remote datasource to assign an ID
	 * to a specified object.
	 */
	public static final int ASSIGN_ID = 25;

	/**
	 * Operation code used to retrieve a property value—specifically blob data—
	 * from the remote datasource for a given object.
	 */
	public static final int GET_PROPERTY = 26;

	/**
	 * Operation code used to delete all objects of a given class from the
	 * remote datasource.
	 */
	public static final int DELETE_ALL = 27;

	/**
	 * The package identifier used to retrieve the corresponding remote client
	 * from {@link com.viaoa.sync.OASyncDelegate}. Defines the synchronization
	 * namespace under which operations are executed.
	 */
	private final String packageName;

	/**
	 * Constructs a new client-side datasource using the specified package to
	 * resolve remote synchronization clients. If {@code packagex} is null,
	 * the default {@code OASync.ObjectPackage} is used.
	 *
	 * @param packagex the package namespace for synchronization
	 */
	public OADataSourceClient(String packageName) {
		this.packageName = packageName;
	}

	/**
	 * Constructs a client-side datasource using the default synchronization
	 * package. Delegates to {@link #OADataSourceClient(Package)}.
	 */
	public OADataSourceClient() {
		this(null);
	}

	/**
	 * Returns the remote client used to communicate with the OA Server. The
	 * client reference is lazily initialized on first access using
	 * {@link com.viaoa.sync.OASyncDelegate#getRemoteClient(Package)}.
	 *
	 * @return the remote client interface
	 */
	public RemoteClientInterface getRemoteClient() {
		if (remoteClientSync == null) {
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(packageName);
			remoteClientSync = og.syncInternal().getRemoteClient();
		}
		return remoteClientSync;
	}

	/**
	 * Tracks whether {@link #getAssignIdOnCreate()} has already been invoked
	 * or overridden locally, preventing repeated remote calls.
	 */
	private boolean bCalledGetAssignIdOnCreate;

	/**
	 * Cached flag indicating whether the remote datasource is configured to
	 * automatically assign IDs upon object creation.
	 */
	private boolean bGetAssignIdOnCreate;

	/**
	 * Overrides the remote datasource setting for automatic ID assignment.
	 * Marks this instance as having an explicitly configured value.
	 *
	 * @param b true to enable automatic ID assignment
	 */
	public void setAssignIdOnCreate(boolean b) {
		bCalledGetAssignIdOnCreate = true;
		bGetAssignIdOnCreate = b;
	}

	/**
	 * Returns whether the remote datasource is configured to automatically
	 * assign IDs when objects are created.
	 * <p>
	 * If the value was previously overridden via {@link #setAssignIdOnCreate},
	 * the cached value is returned. Otherwise, the setting is retrieved from
	 * the remote datasource and cached for subsequent access.
	 *
	 * @return true if automatic ID assignment on create is enabled
	 */
	public boolean getAssignIdOnCreate() {
		if (bCalledGetAssignIdOnCreate) {
			return bGetAssignIdOnCreate;
		}
		verifyConnection();
		Object obj = getRemoteClient().datasource(GET_ASSIGN_ID_ON_CREATE, new Object[] {});
		bCalledGetAssignIdOnCreate = true;
		if (obj instanceof Boolean) {
			bGetAssignIdOnCreate = ((Boolean) obj).booleanValue();
		}
		return bGetAssignIdOnCreate;
	}

	/**
	 * Queries the remote datasource to determine whether it is currently
	 * available. Returns false if the remote response is not a Boolean.
	 *
	 * @return true if the datasource is available
	 */
	public boolean isAvailable() {
		verifyConnection();
		Object obj = getRemoteClient().datasource(IS_AVAILABLE, null);
		if (obj instanceof Boolean) {
			return ((Boolean) obj).booleanValue();
		}
		return false;
	}

	/**
	 * Cache storing maximum property lengths returned from the remote datasource
	 * using keys of the form "CLASSNAME-PROPERTYNAME".
	 */
	private Map<String, Integer> hmMax = new HashMap<String, Integer>();

	/**
	 * Retrieves the maximum allowed length for the specified property of the
	 * given class. Uses a cache to avoid repeated remote calls.
	 * <p>
	 * If no cached value exists, the remote datasource is queried and the
	 * result (or -1 if invalid) is stored in the cache.
	 *
	 * @param c the class owning the property
	 * @param propertyName the property name
	 * @return the maximum length, or -1 if unavailable
	 */
	public int getMaxLength(Class c, String propertyName) {
		String key = (c.getName() + "-" + propertyName).toUpperCase();
		Object objx = hmMax.get(key);
		if (objx != null) {
			return ((Integer) objx).intValue();
		}

		int iResult;
		verifyConnection();
		Object obj = getRemoteClient().datasource(MAX_LENGTH, new Object[] { c, propertyName });
		if (obj instanceof Integer) {
			iResult = ((Integer) obj).intValue();
		} else {
			iResult = -1;
		}
		hmMax.put(key, iResult);
		return iResult;
	}

	/**
	 * Manually sets the cached maximum length for the specified property.
	 * Does not perform any remote communication.
	 *
	 * @param c the class owning the property
	 * @param propertyName the property name
	 * @param length the maximum length to cache
	 */
	public void setMaxLength(Class c, String propertyName, int length) {
		if (c == null || propertyName == null) {
			return;
		}
		String key = (c.getName() + "-" + propertyName).toUpperCase();
		hmMax.put(key, Integer.valueOf(length));
	}

	/**
	 * Ensures that a remote client connection is available. Throws a
	 * {@link RuntimeException} if the remote client has not been initialized.
	 */
	protected void verifyConnection() {
		if (getRemoteClient() == null) {
			throw new RuntimeException("OADataSourceClient connection is not set");
		}
	}

	//NOTE: this needs to see if any of "clazz" superclasses are supported
	/**
	 * Determines whether the specified class is supported by the remote datasource.
	 * <p>
	 * Checks the local cache first. If a filter is present and the class has a
	 * locally cached "select all" Hub, the class is automatically treated as
	 * supported. Otherwise, a remote query is performed, and the result cached.
	 *
	 * @param clazz  the class to test
	 * @param filter optional filter used for local cached evaluation
	 * @return true if supported by the remote datasource
	 */
	public boolean isClassSupported(Class clazz, OAFilter filter) {
		if (clazz == null) {
			return false;
		}

		Boolean B = (Boolean) hashClass.get(clazz);
		if (B != null) {
			return B.booleanValue();
		}

		if (filter != null) {
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(clazz);
        	if (og.objectsInternal().callObjectCacheGetSelectAllHub(clazz) != null) {
				return true;
			}
		}
		if (getRemoteClient() == null) return false;		

		Object obj = getRemoteClient().datasource(IS_CLASS_SUPPORTED, new Object[] { clazz });
		boolean b = false;
		if (obj instanceof Boolean) {
			b = ((Boolean) obj).booleanValue();
		}

		hashClass.put(clazz, Boolean.valueOf(b));
		return b;
	}

	/**
	 * Sends an insert-without-references request for the specified object to
	 * the remote datasource. Does nothing if the object is null.
	 *
	 * @param obj the object to insert
	 */
	public void insertWithoutReferences(OAObject obj) {
		if (obj == null) {
			return;
		}
		getRemoteClient().datasource(INSERT_WO_REFERENCES, new Object[] { obj });
	}

	/**
	 * Sends an insert request for the specified object to the remote datasource.
	 * Does nothing if the object is null.
	 *
	 * @param obj the object to insert
	 */
	public void insert(OAObject obj) {
		if (obj == null) {
			return;
		}
		getRemoteClient().datasource(INSERT, new Object[] { obj });
	}

	/**
	 * Sends an update request for the specified object to the remote datasource.
	 * Includes optional arrays for controlling which properties should be included
	 * or excluded during the update. Does nothing if the object is null.
	 *
	 * @param obj               the object to update
	 * @param includeProperties properties to include in the update
	 * @param excludeProperties properties to exclude from the update
	 */
	public @Override void update(OAObject obj, String[] includeProperties, String[] excludeProperties) {
		if (obj == null) {
			return;
		}
		getRemoteClient().datasource(UPDATE, new Object[] { obj, includeProperties, excludeProperties });
	}

	/**
	 * Sends a save request for the specified object to the remote datasource.
	 * Uses a return-on-queue call to avoid blocking. Does nothing if the object
	 * is null.
	 *
	 * @param obj the object to save
	 */
	public @Override void save(OAObject obj) {
		if (obj == null) {
			return;
		}
		getRemoteClient().datasourceReturnOnQueue(SAVE, new Object[] { obj });
	}

	/**
	 * Sends a delete request for the specified object to the remote datasource
	 * using a return-on-queue call. Does nothing if the object is null.
	 *
	 * @param obj the object to delete
	 */
	public @Override void delete(OAObject obj) {
		if (obj == null) {
			return;
		}
		getRemoteClient().datasourceReturnOnQueue(DELETE, new Object[] { obj });
	}

	/**
	 * Sends a delete-all request to the remote datasource for the specified class.
	 * Does nothing if the class parameter is null.
	 *
	 * @param c the class of objects to delete
	 */
	public @Override void deleteAll(Class c) {
		if (c == null) {
			return;
		}
		getRemoteClient().datasourceReturnOnQueue(DELETE_ALL, new Object[] { c });
	}

	/**
	 * Counts the number of matching records on the remote datasource using the
	 * specified query parameters. Converts the where-object into a class and key
	 * pair for remote transmission.
	 *
	 * @param selectClass             the class to count
	 * @param queryWhere              the where clause
	 * @param params                  query parameters
	 * @param whereObject             optional object used as the source of the query
	 * @param propertyFromWhereObject property used when evaluating against whereObject
	 * @param extraWhere              additional where clause fragment
	 * @param max                     maximum result threshold
	 * @return the record count, or -1 if unavailable
	 */
	@Override
	public int count(Class selectClass,
			String queryWhere, Object[] params,
			OAObject whereObject, String propertyFromWhereObject, String extraWhere, int max) {
		Class whereClass = null;
		OAObjectKey whereKey = null;
		if (whereObject != null) {
			whereClass = whereObject.getClass();
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(whereObject);
			whereKey = og.objectsInternal().callObjectKeyGetKey(whereObject);
		}

		Object[] objs = new Object[] { selectClass, queryWhere, params, whereClass, whereKey, propertyFromWhereObject, extraWhere, max };

		Object obj = getRemoteClient().datasource(COUNT, objs);
		if (obj instanceof Integer) {
			return ((Integer) obj).intValue();
		}
		return -1;
	}

	/**
	 * Performs a passthrough count operation on the remote datasource for the
	 * specified class and where clause.
	 *
	 * @param selectClass the class to count
	 * @param queryWhere  the where clause
	 * @param max         the maximum number of rows to consider
	 * @return the count value, or -1 if unavailable
	 */
	@Override
	public int countPassthru(Class selectClass, String queryWhere, int max) {
		Object obj = getRemoteClient().datasource(COUNTPASSTHRU, new Object[] { selectClass, queryWhere, max });
		if (obj instanceof Integer) {
			return ((Integer) obj).intValue();
		}
		return -1;
	}

	/**
	 * Tracks whether the supports-storage flag has been retrieved from the
	 * remote datasource, preventing redundant remote calls.
	 */
	private boolean bCalledSupportsStorage;

	/**
	 * Cached result indicating whether the remote datasource supports storage
	 * operations such as insert, update, and delete.
	 */
	private boolean bSupportsStorage;

	/**
	 * Returns whether the remote datasource supports storage operations.
	 * The result is cached on first call; subsequent calls use the cached value.
	 *
	 * @return true if storage operations are supported
	 */
	public @Override boolean supportsStorage() {
		if (bCalledSupportsStorage) {
			return bSupportsStorage;
		}
		RemoteClientInterface rc = getRemoteClient();
		if (rc == null) {
			return false;
		}

		Object obj = rc.datasource(SUPPORTSSTORAGE, null);
		bCalledSupportsStorage = true;
		if (obj instanceof Boolean) {
			bSupportsStorage = ((Boolean) obj).booleanValue();
		}
		return bSupportsStorage;
	}

	/**
	 * Performs a select query against the remote datasource and returns a
	 * proxy iterator for retrieving results in batches.
	 * <p>
	 * If a filter is provided and the class has a locally cached "select all"
	 * hub, a local {@link ObjectCacheIterator} is returned instead of using
	 * the remote datasource.
	 *
	 * @param selectClass             the class to query
	 * @param queryWhere              where clause
	 * @param params                  query parameters
	 * @param queryOrder              order clause
	 * @param whereObject             optional source object for contextual filtering
	 * @param propertyFromWhereObject optional property for whereObject filtering
	 * @param extraWhere              extra where clause fragment
	 * @param max                     maximum number of returned results
	 * @param filter                  optional filter to apply locally
	 * @param bDirty                  whether to include dirty objects
	 * @return a datasource iterator, or null if remote server returns null
	 */
	@Override
	public OADataSourceIterator select(Class selectClass,
			String queryWhere, Object[] params, String queryOrder,
			OAObject whereObject, String propertyFromWhereObject, String extraWhere,
			int max, OAFilter filter, boolean bDirty) {
		if (filter != null) {
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(selectClass);
			
			if (og.objectsInternal().callObjectCacheGetSelectAllHub(selectClass) != null) {
				ObjectCacheIterator it = new ObjectCacheIterator(selectClass, filter);
				it.setMax(max);
				return it;
			}
		}

		Class whereClass = null;
		OAObjectKey whereKey = null;
		if (whereObject != null) {
			whereClass = whereObject.getClass();
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(whereClass);
			whereKey = og.objectsInternal().callObjectKeyGetKey(whereObject);
		}

		Object[] objs = new Object[] {
				selectClass,
				queryWhere, params, queryOrder,
				whereClass, whereKey,
				propertyFromWhereObject, extraWhere,
				max, bDirty, (filter != null)
		};

		Object obj = getRemoteClient().datasource(SELECT, objs);
		if (obj == null) {
			return null;
		}
		// dont send the filter to the server, it could serialize extra data, etc.
		return new MyIterator(selectClass, obj, filter);
	}

	/**
	 * Performs a passthrough select query against the remote datasource.
	 * If a local "select all" hub exists for the class, returns an
	 * {@link ObjectCacheIterator} instead.
	 *
	 * @param selectClass the class to query
	 * @param queryWhere  where clause
	 * @param queryOrder  order clause
	 * @param max         maximum number of results
	 * @param filter      local filter
	 * @param bDirty      whether dirty objects are included
	 * @return an iterator for remote result retrieval
	 */
	@Override
	public OADataSourceIterator selectPassthru(Class selectClass,
			String queryWhere, String queryOrder,
			int max, OAFilter filter, boolean bDirty) {
		if (filter != null) {
			
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(selectClass);
			
			if (og.objectsInternal().callObjectCacheGetSelectAllHub(selectClass) != null) {
				ObjectCacheIterator it = new ObjectCacheIterator(selectClass, filter);
				it.setMax(max);
				return it;
			}
		}
		Object obj = getRemoteClient().datasource(SELECTPASSTHRU, new Object[] { selectClass, queryWhere, queryOrder, max, bDirty });
		if (obj == null) {
			return null;
		}
		return new MyIterator(selectClass, obj, filter);
	}

	/**
	 * Executes an arbitrary command on the remote datasource using the
	 * {@link #EXECUTE} operation code.
	 *
	 * @param command the command to execute
	 * @return the result returned by the remote datasource
	 */
	public @Override Object execute(String command) {
		return getRemoteClient().datasource(EXECUTE, new Object[] { command });
	}

	/**
	 * Requests that the remote datasource assign an ID to the specified object.
	 * Uses a queued call to avoid blocking.
	 *
	 * @param obj the object requiring ID assignment
	 */
	public @Override void assignId(OAObject obj) {
		getRemoteClient().datasourceReturnOnQueue(ASSIGN_ID, new Object[] { obj });
	}

	/**
	 * Determines whether the remote datasource will create a value for the
	 * specified property on the given object.
	 *
	 * @param object       the object being considered
	 * @param propertyName the property being evaluated
	 * @return true if the remote datasource indicates a value will be created
	 */
	public @Override boolean willCreatePropertyValue(OAObject object, String propertyName) {
		Object obj = getRemoteClient().datasource(WILLCREATEPROPERTYVALUE, new Object[] { object, propertyName });
		if (obj instanceof Boolean) {
			return ((Boolean) obj).booleanValue();
		}
		return false;
	}

	/**
	 * Iterator implementation used for remote query results. Communicates with
	 * the remote datasource to fetch result batches and update local caches.
	 */
	class MyIterator implements OADataSourceIterator {
		/**
		 * Identifier returned by the remote datasource representing the remote
		 * iterator state. Used when requesting additional result batches.
		 */
		Object id;

		/**
		 * The class type of objects returned by this iterator.
		 */
		Class clazz;
		
		/**
		 * Optional object key used when the iterator represents a single object
		 * reference rather than a batched remote iterator.
		 */
		OAObjectKey key; // object to return
		
		/**
		 * Indicates whether this iterator is in single-key mode, returning exactly
		 * one object referenced by {@link #key}.
		 */
		boolean bKey;
		
		/**
		 * Local buffer containing the most recently fetched batch of results
		 * from the remote datasource. Used by {@link #hasNext()} and {@link #next()}.
		 */
		Object[] cache;
		
		/**
		 * Current read position within the {@link #cache} array.
		 */
		int cachePos = 0;
		
		/**
		 * Optional filter applied locally when iterating over remote batches.
		 * Objects failing the filter are skipped.
		 */
		OAFilter filter;
		
		/**
		 * Helper used to maintain sibling relationships among objects returned
		 * by this iterator. Created upon first server fetch.
		 */
		private OASiblingHelper siblingHelper;
		
		/**
		 * Hub used for read-ahead support when receiving batches of objects from
		 * the remote datasource.
		 */
		private Hub<OAObject> hubReadAhead;

		/**
		 * Constructs a new iterator operating in remote-batch mode.
		 *
		 * @param c      the class of returned objects
		 * @param id     the remote iterator token
		 * @param filter optional local filter applied to returned objects
		 */
		public MyIterator(Class c, Object id, OAFilter filter) {
			this.clazz = c;
			this.id = id;
			this.filter = filter;
			getMoreFromServer();
		}

		/**
		 * Constructs an iterator operating in single-key mode, returning at most
		 * one object identified by the supplied {@link OAObjectKey}.
		 *
		 * @param key the key of the object to return
		 */
		public MyIterator(OAObjectKey key) {
			this.key = key;
			this.bKey = true;
		}

		/**
		 * Returns the sibling helper created for this iterator, if any.
		 *
		 * @return the sibling helper, or null if none exists
		 */
		@Override
		public OASiblingHelper getSiblingHelper() {
			return siblingHelper;
		}

		/**
		 * Determines whether more objects are available from this iterator.
		 * <p>
		 * Behavior:
		 * <ul>
		 *   <li>If in key mode, returns true until the single referenced object
		 *       has been consumed.</li>
		 *   <li>Iterates through the current cache and applies the optional filter.</li>
		 *   <li>If cache is exhausted, fetches the next batch from the server.</li>
		 * </ul>
		 *
		 * @return true if another object is available
		 */
		public synchronized boolean hasNext() {
			if (key != null) {
				return (bKey);
			}

			for (;;) {
				if (cache == null) {
					break;
				}
				for (; cachePos < cache.length; cachePos++) {
					if (cache[cachePos] == null) {
						return false;
					}
					if (filter == null || filter.isUsed(cache[cachePos])) {
						return true;
					}
				}
				getMoreFromServer();
			}

			return false;
		}

		/**
		 * Retrieves the next batch of objects from the remote datasource using
		 * the {@link #IT_NEXT} operation code. Updates local caches and sibling
		 * structures. If no more results are available, closes the iterator.
		 */
		protected synchronized void getMoreFromServer() {
			cachePos = 0;
			cache = (Object[]) getRemoteClient().datasource(IT_NEXT, new Object[] { id });
			if (cache == null || cache.length == 0) {
				cache = null;
				//20190130
				close();
				return;
			}
			//20190130
			if (siblingHelper == null) {
				this.hubReadAhead = new Hub();
				siblingHelper = new OASiblingHelper(this.hubReadAhead);
			}

			for (Object objx : cache) {
				if (objx == null) continue;
				OAObject obj = (OAObject) objx;
				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(obj.getClass());
				og.objectsInternal().callObjectCSUpdateObjectsWithoutHubs(obj);
                hubReadAhead.add(obj);
			}
		}

		/**
		 * Returns the next object from this iterator.
		 * <p>
		 * Behavior:
		 * <ul>
		 *   <li>In key mode: returns the object identified by {@link #key} and ends iteration.</li>
		 *   <li>In batch mode: returns the next cached object, fetching additional
		 *       batches as needed via {@link #hasNext()}.</li>
		 * </ul>
		 *
		 * @return the next object, or null if none are available
		 */
		public synchronized Object next() {
			if (!hasNext()) {
				return null;
			}
			Object obj = null;
			if (key != null) {
				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(clazz);
				obj = og.objectsInternal().callObjectCacheGet(clazz, key);
				if (obj == null) {
					// not on this system, need to get from server
					og.syncInternal().getRemoteServer().getObject(clazz, key);
				}
				bKey = false;
				return obj;
			}
			obj = cache[cachePos++];

			return obj;
		}

		/**
		 * Requests removal of the current iterator element on the remote datasource
		 * using the {@link #IT_REMOVE} operation code, then closes the iterator.
		 */
		public void remove() {
			getRemoteClient().datasourceNoReturn(IT_REMOVE, new Object[] { id });
			close();
		}

		/**
		 * Placeholder method returning null. Query string retrieval is not implemented.
		 *
		 * @return null
		 */
		@Override
		public String getQuery() {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * Placeholder method returning null. Secondary query string retrieval is
		 * not implemented.
		 *
		 * @return null
		 */
		@Override
		public String getQuery2() {
			return null;
		}

		/**
		 * Releases iterator resources, clearing the read-ahead hub and resetting
		 * the sibling helper.
		 */
		public void close() {
			if (hubReadAhead != null) {
				hubReadAhead.clear();
				hubReadAhead = null;
			}
			if (siblingHelper != null) {
				siblingHelper = null;
			}
		}

		/**
		 * Finalizer that ensures iterator resources are released by calling
		 * {@link #close()} before garbage collection.
		 *
		 * @throws Throwable if the superclass finalizer throws an exception
		 */
		public void finalize() throws Throwable {
			super.finalize();
			close();
		}
	}

	/**
	 * Sends an update-many-to-many-links request to the remote datasource.
	 * Includes the master object's class, key, added objects, removed objects,
	 * and the property name defining the relationship.
	 *
	 * @param masterObject            the master object
	 * @param adds                    objects to add to the relationship
	 * @param removes                 objects to remove
	 * @param propertyNameFromMaster  the property representing the relationship
	 */
	public @Override void updateMany2ManyLinks(OAObject masterObject, OAObject[] adds, OAObject[] removes, String propertyNameFromMaster) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(masterObject);
		getRemoteClient().datasource(UPDATE_MANY2MANY_LINKS, new Object[] { masterObject.getClass(),
				og.objectsInternal().callObjectKeyGetKey(masterObject), adds, removes, propertyNameFromMaster });
	}

	/**
	 * Retrieves a blob property value from the remote datasource for the given
	 * object and property name. Returns null if the response is not a byte array.
	 *
	 * @param obj          the object whose blob value is being requested
	 * @param propertyName the name of the property
	 * @return the blob value, or null if unavailable
	 */
	@Override
	public byte[] getPropertyBlobValue(OAObject obj, String propertyName) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(obj);
		Object objx = getRemoteClient().datasource(	GET_PROPERTY,
													new Object[] { obj.getClass(), og.objectsInternal().callObjectKeyGetKey(obj), propertyName });
		if (objx instanceof byte[]) {
			return (byte[]) objx;
		}
		return null;
	}

	/**
	 * Identifies this datasource as a client-side implementation.
	 *
	 * @return true
	 */
	@Override
	public boolean isClient() {
		return true;
	}
}
