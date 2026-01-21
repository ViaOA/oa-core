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
package com.viaoa.sync.remote;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.clientserver.OADataSourceClient;
import com.viaoa.graph.OAGraph;
import com.viaoa.graph.OAGraphImpl;
import com.viaoa.graph.object.OAObjectCacheService;
import com.viaoa.graph.object.OAObjectKeyService;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectKeyDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAFilter;

/**
 * Server-side datasource proxy used by {@link RemoteClientImpl} to execute
 * {@link OADataSource} operations initiated by a remote client.
 * <p>
 * {@code RemoteDataSource} translates high-level DS operations into direct
 * calls against the appropriate server-side datasource. It supports:
 * <ul>
 *   <li>assigning IDs,</li>
 *   <li>insert/update/save/delete,</li>
 *   <li>many-to-many link updates,</li>
 *   <li>query execution and iteration management,</li>
 *   <li>count and passthrough count operations,</li>
 *   <li>SELECT iterator creation and paging,</li>
 *   <li>blob/property retrieval,</li>
 *   <li>storage capability checks.</li>
 * </ul>
 *
 * <h2>Iterator Management</h2>
 * SELECT operations return an opaque string ID. The server stores a live
 * iterator in {@code hashIterator}, and subsequent calls to:
 * <ul>
 *   <li>{@code IT_HASNEXT},</li>
 *   <li>{@code IT_NEXT},</li>
 *   <li>{@code IT_REMOVE}</li>
 * </ul>
 * operate against that server-side iterator.
 *
 * <h2>Object Resolution</h2>
 * When queries or updates reference keys instead of objects, the datasource
 * will:
 * <ul>
 *   <li>look up objects in the cache,</li>
 *   <li>load from datasource if needed,</li>
 *   <li>reassign GUIDs for newly loaded objects if required.</li>
 * </ul>
 *
 * <h2>Cache Retention</h2>
 * The abstract {@link #setCached(OAObject)} method allows implementations to
 * mark that a client now holds a reference to the object, preventing the
 * server from garbage-collecting it prematurely.
 *
 * <p>
 * This class is the remote execution engine for OA's datasource layer during
 * client–server synchronization.
 */
public abstract class RemoteDataSource {
	private static Logger LOG = Logger.getLogger(RemoteDataSource.class.getName());

	/**
	 * Map storing active iterators for SELECT operations.
	 * <p>
	 * The key is a generated iterator identifier returned to the client,
	 * and the value is the server-side iterator instance.
	 * </p>
	 */
	private ConcurrentHashMap<String, Iterator> hashIterator = new ConcurrentHashMap<String, Iterator>(); // used to store DB

	/**
	 * Executes a datasource command on behalf of a remote client.
	 * <p>
	 * Dispatches the command to the appropriate {@link OADataSource} operation,
	 * manages iterator lifecycle, resolves objects from keys when required,
	 * and returns any result value.
	 * </p>
	 *
	 * @param command the datasource command identifier
	 * @param objects arguments for the datasource command
	 * @return the result of the command, or {@code null} if none
	 */
	public Object datasource(int command, Object[] objects) {
		//LOG.finer("command="+command);
		Object obj = null;
		Class clazz, masterClass;
		OADataSource ds;
		Object objKey;
		boolean b;
		int x;
		Object whereObject;
		String propFromWhereObject;

		switch (command) {
		case OADataSourceClient.ASSIGN_ID:
			clazz = (Class) objects[0].getClass();
			ds = getDataSource(clazz);
			if (ds != null) {
				//not needed
				//OARemoteThreadDelegate.sendMessages(true);
				ds.assignId((OAObject) objects[0]);
				//OARemoteThreadDelegate.sendMessages(false);
			}
			break;

		case OADataSourceClient.IT_NEXT:
			obj = datasourceNext((String) objects[0]);
			break;
		case OADataSourceClient.IT_HASNEXT:
			obj = Boolean.valueOf(datasourceHasNext((String) objects[0]));
			break;
		case OADataSourceClient.IS_AVAILABLE:
			ds = getDataSource();
			if (ds != null) {
				b = ds.isAvailable();
				obj = Boolean.valueOf(b);
			}
			break;
		case OADataSourceClient.GET_ASSIGN_ID_ON_CREATE:
			ds = getDataSource();
			if (ds != null) {
				b = ds.getAssignIdOnCreate();
				obj = Boolean.valueOf(b);
			} else {
				obj = Boolean.FALSE;
			}
			break;
		case OADataSourceClient.MAX_LENGTH:
			clazz = (Class) objects[0];
			ds = getDataSource(clazz);
			if (ds != null) {
				x = ds.getMaxLength(clazz, (String) objects[1]);
				// System.out.println("note: RemoteDataSource call to MAX_LENGTH when it should be on the client.");
				obj = Integer.valueOf(x);
			}
			break;
		case OADataSourceClient.IS_CLASS_SUPPORTED:
			clazz = (Class) objects[0];
			ds = getDataSource(clazz);
			obj = Boolean.valueOf((ds != null));
			break;

		case OADataSourceClient.UPDATE_MANY2MANY_LINKS:
			clazz = (Class) objects[0];
			ds = getDataSource(clazz);
			if (ds != null) {
				whereObject = getObject(clazz, objects[1]);
				ds.updateMany2ManyLinks((OAObject) whereObject, (OAObject[]) objects[2], (OAObject[]) objects[3], (String) objects[4]);
			}
			break;

		case OADataSourceClient.INSERT:
			obj = objects[0];
			if (obj != null) {
				ds = getDataSource(obj.getClass());
				if (ds != null) {
					ds.insert((OAObject) obj);
				}
				obj = null;
			}
			break;

		case OADataSourceClient.UPDATE:
			obj = objects[0];
			if (obj != null) {
				ds = getDataSource(obj.getClass());
				if (ds != null) {
					ds.update((OAObject) obj, (String[]) objects[1], (String[]) objects[2]);
				}
				obj = null;
			}
			break;

		case OADataSourceClient.SAVE:
			obj = objects[0];
			if (obj != null) {
				ds = getDataSource(obj.getClass());
				if (ds != null) {
					ds.save((OAObject) obj);
				}
				obj = null;
			}
			break;

		case OADataSourceClient.DELETE:
			obj = objects[0];
			if (obj != null) {
				ds = getDataSource(obj.getClass());
				if (ds != null) {
					ds.delete((OAObject) obj);
				}
				obj = null;
			}
			break;

		case OADataSourceClient.DELETE_ALL:
			Class c = (Class) objects[0];
			if (c != null) {
				ds = getDataSource(c);
				if (ds != null) {
					ds.deleteAll(c);
				}
				obj = null;
			}
			break;
		case OADataSourceClient.COUNT:
			clazz = (Class) objects[0];
			ds = getDataSource(clazz);
			if (ds != null) {
				String queryWhere = (String) objects[1];
				Object[] params = (Object[]) objects[2];
				Class whereClass = (Class) objects[3];
				OAObjectKey whereKey = (OAObjectKey) objects[4];
				propFromWhereObject = (String) objects[5];
				String extraWhere = (String) objects[6];
				int max = (Integer) objects[7];

				whereObject = null;
				if (whereClass != null && whereKey != null) {
					whereObject = getObject(whereClass, whereKey);
				}

				x = ds.count(clazz, queryWhere, params, (OAObject) whereObject, propFromWhereObject, extraWhere, max);
				obj = Integer.valueOf(x);
			} else {
				obj = Integer.valueOf(-1);
			}
			break;

		case OADataSourceClient.COUNTPASSTHRU:
			clazz = (Class) objects[0];
			ds = getDataSource(clazz);
			if (ds != null) {
				x = ds.countPassthru(clazz, (String) objects[1], (Integer) objects[2]);
				obj = Integer.valueOf(x);
			} else {
				obj = Integer.valueOf(-1);
			}
			break;

		case OADataSourceClient.SUPPORTSSTORAGE:
			ds = getDataSource();
			if (ds != null) {
				b = ds.supportsStorage();
				obj = Boolean.valueOf(b);
			} else {
				obj = null;
			}
			break;
		case OADataSourceClient.EXECUTE:
			ds = getDataSource();
			if (ds != null) {
				return ds.execute((String) objects[0]);
			} else {
				obj = null;
			}
			break;
		case OADataSourceClient.IT_REMOVE:
			Iterator iterator = (Iterator) hashIterator.get(objects[0]);
			if (iterator != null) {
				iterator.remove();
				hashIterator.remove(objects[0]);
				LOG.finer("remove iterator, size=" + hashIterator.size());
			}
			break;

		case OADataSourceClient.SELECT:
			clazz = (Class) objects[0];
			ds = getDataSource(clazz);
			String selectId;
			if (ds != null) {
				String queryWhere = (String) objects[1];
				Object[] params = (Object[]) objects[2];
				String queryOrder = (String) objects[3];
				Class whereClass = (Class) objects[4];
				OAObjectKey whereKey = (OAObjectKey) objects[5];
				propFromWhereObject = (String) objects[6];
				String extraWhere = (String) objects[7];
				int max = (Integer) objects[8];
				boolean bDirty = (Boolean) objects[9];
				boolean bHasFilter = (Boolean) objects[10];

				whereObject = null;
				if (whereClass != null && whereKey != null) {
					whereObject = getObject(whereClass, whereKey);
				}

				OAFilter filter = null;
				/* 20170201 not needed, needs to filter on whereClause
				if (bHasFilter) {
				    // if client has a filter, then create a dummy one here
				    filter = new OAFilter() {
				        public boolean isUsed(Object obj) {
				            return true;
				        };
				    };
				}
				 */
				iterator = ds.select(	clazz,
										queryWhere, params, queryOrder,
										(OAObject) whereObject, propFromWhereObject, extraWhere,
										max, filter, bDirty);

				selectId = "select" + aiSelectCount.incrementAndGet();
				if (iterator != null) {
					hashIterator.put(selectId, iterator);
					LOG.finer("add iterator, size=" + hashIterator.size());
				}
			} else {
				selectId = null;
			}
			return selectId;

		case OADataSourceClient.SELECTPASSTHRU:
			clazz = (Class) objects[0];
			ds = getDataSource(clazz);
			if (ds != null) {
				iterator = ds.selectPassthru(	clazz, (String) objects[1], (String) objects[2], (Integer) objects[3], null,
												(Boolean) objects[4]);
				obj = "select" + aiSelectCount.incrementAndGet();
				hashIterator.put((String) obj, iterator);
				LOG.finer("add iterator, size=" + hashIterator.size());
			} else {
				obj = null;
			}
			break;

		case OADataSourceClient.INSERT_WO_REFERENCES:
			whereObject = objects[0];
			if (whereObject == null) {
				break;
			}
			clazz = whereObject.getClass();
			ds = getDataSource(clazz);
			if (ds != null) {
				OAObject oa = (OAObject) whereObject;
				ds.insertWithoutReferences((OAObject) oa);
				OAObjectDelegate.setNew(oa, false);
			}
			break;
		case OADataSourceClient.GET_PROPERTY:
			clazz = (Class) objects[0];
			ds = getDataSource(clazz);
			if (ds != null) {
				objKey = (OAObjectKey) objects[1];
				whereObject = getObject(clazz, objKey);
				String prop = (String) objects[2];
				obj = ds.getPropertyBlobValue((OAObject) whereObject, prop);
			}
			break;
		}
		return obj;
	}

	/**
	 * Resolves an object instance from a key or object reference.
	 * <p>
	 * Attempts to locate the object in cache first, then loads it
	 * from the datasource if not found.
	 * </p>
	 *
	 * @param objectClass the class of the object
	 * @param obj the object key or object instance
	 * @return the resolved {@link OAObject}, or {@code null} if not found
	 */
	private OAObject getObject(Class objectClass, Object obj) {
		if (objectClass == null || obj == null) {
			return null;
		}
		if (obj instanceof OAObject) {
			return (OAObject) obj;
		}

		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(objectClass);
    	final OAObjectCacheService srvcObjectCache = og.getOAObjectService().getOAObjectCacheService();
    	final OAObjectKeyService srvcObjectKey = og.getOAObjectService().getOAObjectKeyService();
		
		OAObjectKey key = srvcObjectKey.createObjectKey(objectClass, obj);

		OAObject objNew = (OAObject) srvcObjectCache.get(objectClass, key);
		if (objNew == null) {
			objNew = (OAObject) OADataSource.getObject(objectClass, key);
		}
		return objNew;
	}

	/**
	 * Resolves the datasource associated with a specific class.
	 *
	 * @param c the class used to determine the datasource
	 * @return the resolved datasource, or a default datasource if not found
	 */
	protected OADataSource getDataSource(Class c) {
		if (c != null) {
			OADataSource ds = OADataSource.getDataSource(c);
			if (ds != null) {
				return ds;
			}
		}
		if (defaultDataSource == null) {
			OADataSource[] dss = OADataSource.getDataSources();
			if (dss != null && dss.length > 0) {
				defaultDataSource = dss[0];
			}
		}
		return defaultDataSource;
	}

	/**
	 * Counter used to generate unique identifiers for SELECT iterators.
	 */
	private AtomicInteger aiSelectCount = new AtomicInteger();
    
	/**
	 * Default datasource used when no class-specific datasource is available.
	 */
	private OADataSource defaultDataSource;

	/**
	 * Returns the default datasource.
	 *
	 * @return the default datasource
	 */
	protected OADataSource getDataSource() {
		return getDataSource(null);
	}

	/**
	 * Marks an object as cached on behalf of the remote client.
	 * <p>
	 * Implementations should record that the client now holds a reference
	 * to this object to prevent premature server-side eviction.
	 * </p>
	 *
	 * @param obj the object to mark as cached
	 */
	public abstract void setCached(OAObject obj);

	/**
	 * Retrieves the next batch of objects from a server-side SELECT iterator.
	 * <p>
	 * Returns up to a fixed number of objects and updates the iterator
	 * lifecycle, removing it when exhausted.
	 * </p>
	 *
	 * @param id the iterator identifier
	 * @return an array of result objects, or {@code null} if none remain
	 */
	protected Object[] datasourceNext(String id) {
		Iterator iterator = (Iterator) hashIterator.get(id);
		if (iterator == null) {
			return null;
		}

		ArrayList<Object> al = new ArrayList();
		for (int i = 0; i < 500; i++) {
			if (!iterator.hasNext()) {
				break;
			}
			Object obj = iterator.next();
			al.add(obj);
			if (obj instanceof OAObject) {
				OAObject oa = (OAObject) obj;
				/* was:  need to always add, in case it's not inHub w/master on client
				 *     client will sent a removeFromServerCache if not needed
				if (!OAObjectHubDelegate.isInHubWithMaster(oa)) {
				    // CACHE_NOTE: need to have OAObject.bCachedOnServer=true set by Client.
				    // see: OAObjectCSDelegate.addedToCache((OAObject) msg.newValue); // flag obj to know that it is cached on server for this client.
				    this.setCached(oa);
				}
				*/
				this.setCached(oa);
			}
		}
		int x = al.size();
		if (x == 0) {
			iterator.remove();
			hashIterator.remove(id);
			LOG.finer("remove iterator, size=" + hashIterator.size());
		}
		Object[] objs = new Object[x];
		if (x > 0) {
			al.toArray(objs);
		}
		return objs;
	}

	/**
	 * Determines whether a SELECT iterator has additional results.
	 *
	 * @param id the iterator identifier
	 * @return {@code true} if more results are available, otherwise {@code false}
	 */
	protected boolean datasourceHasNext(String id) {
		Iterator iterator = (Iterator) hashIterator.get(id);
		return (iterator != null && iterator.hasNext());
	}
}
