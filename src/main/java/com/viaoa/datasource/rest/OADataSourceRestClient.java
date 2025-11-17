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
package com.viaoa.datasource.rest;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.OADataSourceIterator;
import com.viaoa.datasource.objectcache.ObjectCacheIterator;
import com.viaoa.hub.Hub;
import com.viaoa.json.OAJson;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OASiblingHelper;
import com.viaoa.sync.OASync;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAFilter;

/**
 * REST-based {@link com.viaoa.datasource.OADataSource} implementation for OA clients.
 * <p>
 * {@code OADataSourceRestClient} enables any OA application to access a remote
 * OADataSource through JSON/HTTP using an {@link OADataSourceRestInterface}.
 * It fully supports CRUD operations, queries, and iterative result streaming.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Transparent remote CRUD and query operations over REST.</li>
 *   <li>JSON serialization via {@link com.viaoa.json.OAJson}.</li>
 *   <li>Fallback to local {@link com.viaoa.datasource.objectcache.ObjectCacheIterator}
 *       when applicable.</li>
 *   <li>Supports {@link com.viaoa.filter.OAFilter} for client-side filtering.</li>
 * </ul>
 *
 */
public class OADataSourceRestClient extends OADataSource {
	private Hashtable hashClass = new Hashtable();
	private OADataSourceRestInterface restAPI;

	/**
	 * Create new OADataSourceClient that uses OARestClient to communicate with OADataSource on OAServer.
	 */
	public OADataSourceRestClient(Package packagex, OADataSourceRestInterface restAPI) {
		if (packagex == null) {
			packagex = OASync.ObjectPackage;
		}
		this.restAPI = restAPI;
	}

	public OADataSourceRestInterface getRestAPI() {
		return restAPI;
	}

	private boolean bCalledGetAssignIdOnCreate;
	private boolean bGetAssignIdOnCreate;

	public void setAssignIdOnCreate(boolean b) {
		bCalledGetAssignIdOnCreate = true;
		bGetAssignIdOnCreate = b;
	}

	@Override
	public boolean getAssignIdOnCreate() {
		if (bCalledGetAssignIdOnCreate) {
			return bGetAssignIdOnCreate;
		}
		verifyConnection();
		bGetAssignIdOnCreate = getRestAPI().getAssignIdOnCreate();
		bCalledGetAssignIdOnCreate = true;
		return bGetAssignIdOnCreate;
	}

	@Override
	public boolean isAvailable() {
		verifyConnection();
		return getRestAPI().isAvailable();
	}

	private final Map<String, Integer> hmMax = new HashMap<String, Integer>();

	@Override
	public int getMaxLength(Class c, String propertyName) {
		String key = (c.getName() + "-" + propertyName).toUpperCase();
		Integer ix = hmMax.get(key);
		if (ix != null) {
			return ix.intValue();
		}

		int iResult;
		verifyConnection();
		int max = getRestAPI().getMaxLength(c, propertyName);
		hmMax.put(key, max);
		return max;
	}

	public void setMaxLength(Class c, String propertyName, int length) {
		if (c == null || propertyName == null) {
			return;
		}
		String key = (c.getName() + "-" + propertyName).toUpperCase();
		hmMax.put(key, Integer.valueOf(length));
	}

	protected void verifyConnection() {
		if (getRestAPI() == null) {
			throw new RuntimeException("OADataSourceClient connection is not set");
		}
	}

	//NOTE: this needs to see if any of "clazz" superclasses are supported
	@Override
	public boolean isClassSupported(Class clazz, OAFilter filter) {
		if (clazz == null) {
			return false;
		}

		Boolean B = (Boolean) hashClass.get(clazz);
		if (B != null) {
			return B.booleanValue();
		}

		if (filter != null) {
			if (OAObjectCacheDelegate.getSelectAllHub(clazz) != null) {
				return true;
			}
		}

		verifyConnection();
		boolean b = getRestAPI().isClassSupported(clazz);

		hashClass.put(clazz, b);
		return b;
	}

	@Override
	public void insertWithoutReferences(OAObject obj) {
		if (obj == null) {
			return;
		}
		getRestAPI().insertWithoutReferences(obj);
		obj.setNew(false);
		obj.setDeleted(false);
		obj.setChanged(false);
	}

	@Override
	public void insert(OAObject obj) {
		if (obj == null) {
			return;
		}
		getRestAPI().insert(obj);
		obj.setNew(false);
		obj.setDeleted(false);
		obj.setChanged(false);
	}

	@Override
	public void update(OAObject obj, String[] includeProperties, String[] excludeProperties) {
		if (obj == null) {
			return;
		}
		getRestAPI().update(obj, includeProperties, excludeProperties);
		obj.setNew(false);
		obj.setDeleted(false);
		obj.setChanged(false);
	}

	@Override
	public void save(OAObject obj) {
		if (obj == null) {
			return;
		}
		getRestAPI().save(obj);
		obj.setNew(false);
		obj.setDeleted(false);
		obj.setChanged(false);
	}

	@Override
	public void delete(OAObject obj) {
		if (obj == null) {
			return;
		}
		getRestAPI().delete(obj);
		obj.setNew(false);
		obj.setDeleted(true);
		obj.setChanged(false);
	}

	@Override
	public void deleteAll(Class c) {
		if (c == null) {
			return;
		}
		getRestAPI().deleteAll(c);
	}

	@Override
	public int count(Class selectClass,
			String queryWhere, Object[] params,
			OAObject whereObject, String propertyFromWhereObject, String extraWhere, int max) {
		Class whereClass = null;
		String whereId = null;
		if (whereObject != null) {
			whereId = OAJson.convertObjectKeyToJsonSinglePartId(whereObject.getObjectKey());
			whereClass = whereObject.getClass();
		}

		int cnt = getRestAPI().count(selectClass, queryWhere, params, whereClass, whereId, propertyFromWhereObject, extraWhere, max);
		return cnt;
	}

	@Override
	public int countPassthru(Class selectClass, String queryWhere, int max) {
		int cnt = getRestAPI().countPassthru(selectClass, queryWhere, max);
		return cnt;
	}

	private boolean bCalledSupportsStorage;
	private boolean bSupportsStorage;

	/** does this dataSource support selecting/storing/deleting */
	@Override
	public boolean supportsStorage() {
		if (bCalledSupportsStorage) {
			return bSupportsStorage;
		}

		bSupportsStorage = getRestAPI().supportsStorage();
		bCalledSupportsStorage = true;
		return bSupportsStorage;
	}

	@Override
	public OADataSourceIterator select(Class selectClass,
			String queryWhere, Object[] params, String queryOrder,
			OAObject whereObject, String propertyFromWhereObject, String extraWhere,
			int max, OAFilter filter, boolean bDirty) {

		if (filter != null) {
			if (OAObjectCacheDelegate.getSelectAllHub(selectClass) != null) {
				ObjectCacheIterator it = new ObjectCacheIterator(selectClass, filter);
				it.setMax(max);
				return it;
			}
		}

		Class whereClass = null;
		String whereId = null;
		if (whereObject != null) {
			whereId = OAJson.convertObjectKeyToJsonSinglePartId(whereObject.getObjectKey());
			whereClass = whereObject.getClass();
		}

		int selectId = getRestAPI().select(	selectClass,
											queryWhere, params, queryOrder,
											whereClass, whereId,
											propertyFromWhereObject, extraWhere, max, bDirty);

		return new MyIterator(selectClass, selectId, filter);
	}

	@Override
	public OADataSourceIterator selectPassthru(Class selectClass,
			String queryWhere, String queryOrder,
			int max, OAFilter filter, boolean bDirty) {
		if (filter != null) {
			if (OAObjectCacheDelegate.getSelectAllHub(selectClass) != null) {
				ObjectCacheIterator it = new ObjectCacheIterator(selectClass, filter);
				it.setMax(max);
				return it;
			}
		}
		int selectId = getRestAPI().selectPassThru(selectClass, queryWhere, queryOrder, max, bDirty);
		return new MyIterator(selectClass, selectId, filter);
	}

	@Override
	public Object execute(String command) {
		return getRestAPI().execute(command);
	}

	@Override
	public void assignId(OAObject obj) {
		if (obj == null) {
			return;
		}
		OAObject objx = getRestAPI().assignId(obj, obj.getClass());

		if (objx == null) {
			return;
		}

		OAObjectKey okx = objx.getObjectKey();

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(obj.getClass());

		Object[] ids = okx.getObjectIds();

		int cnt = -1;
		for (String id : oi.getKeyProperties()) {
			cnt++;
			if (cnt == ids.length) {
				break;
			}
			obj.setProperty(id, ids[cnt]);
		}
	}

	@Override
	public boolean willCreatePropertyValue(OAObject object, String propertyName) {
		Object obj = getRestAPI().willCreatePropertyValue(object, propertyName);
		boolean b = OAConv.toBoolean(obj);
		return b;
	}

	/**
	 * Iterator Class that is used by select methods, works directly with OADataSource on OAServer.
	 */
	class MyIterator implements OADataSourceIterator {
		final int selectId;
		final Class clazz;
		OAObjectKey key; // object to return
		boolean bKey;
		Object[] cache;
		int cachePos = 0;
		OAFilter filter;
		private OASiblingHelper siblingHelper;
		private Hub hubReadAhead;

		public MyIterator(Class c, int selectId, OAFilter filter) {
			this.clazz = c;
			this.selectId = selectId;
			this.filter = filter;
			getMoreFromServer();
		}

		@Override
		public OASiblingHelper getSiblingHelper() {
			return siblingHelper;
		}

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

		protected synchronized void getMoreFromServer() {
			cachePos = 0;
			cache = (Object[]) getRestAPI().next(selectId, clazz);
			if (cache == null || cache.length == 0) {
				cache = null;
				close();
				return;
			}
			if (siblingHelper == null) {
				this.hubReadAhead = new Hub();
				siblingHelper = new OASiblingHelper(this.hubReadAhead);
			}

			for (Object obj : cache) {
				if (obj == null) {
					break;
				}
				// the server will add the object to the session cache (server side) if it is not in a hub w/master
				/* qqq ??
				if (OAObjectHubDelegate.isInHubWithMaster((OAObject) obj)) {
					OAObjectCSDelegate.removeFromServerSideCache((OAObject) obj);
				}
				*/
				hubReadAhead.add(obj);
			}
		}

		public synchronized Object next() {
			if (!hasNext()) {
				return null;
			}
			Object obj = null;
			if (key != null) {
				obj = OAObjectCacheDelegate.get(clazz, key);
				if (obj == null) {
					// not on this system, need to get from server
					//qqqqqqq todo:
					//qqqqqqq  OASyncDelegate.getRemoteServer(packagex).getObject(clazz, key);
				}
				bKey = false;
				return obj;
			}
			obj = cache[cachePos++];

			return obj;
		}

		public void remove() {
			getRestAPI().removeSelect(selectId);
			close();
		}

		@Override
		public String getQuery() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getQuery2() {
			// TODO Auto-generated method stub
			return null;
		}

		public void close() {
			if (hubReadAhead != null) {
				hubReadAhead.clear();
				hubReadAhead = null;
			}
			if (siblingHelper != null) {
				siblingHelper = null;
			}
		}

		public void finalize() throws Throwable {
			super.finalize();
			close();
		}

	}

	@Override
	public void updateMany2ManyLinks(OAObject masterObject, OAObject[] adds, OAObject[] removes, String propertyNameFromMaster) {
		Class masterClass = null;
		String masterId = null;
		if (masterObject != null) {
			masterId = OAJson.convertObjectKeyToJsonSinglePartId(masterObject.getObjectKey());
			masterClass = masterObject.getClass();
		}

		Class addClass = null;
		if (adds != null && adds.length > 0) {
			addClass = adds[0].getClass();
		}

		Class removeClass = null;
		if (removes != null && removes.length > 0) {
			removeClass = removes[0].getClass();
		}

		getRestAPI().updateMany2ManyLinks(masterClass, masterId, adds, addClass, removes, removeClass, propertyNameFromMaster);
	}

	@Override
	public byte[] getPropertyBlobValue(OAObject obj, String propertyName) {
		throw new RuntimeException("not yet implemented");
	}

	@Override
	public boolean isClient() {
		return true;
	}
}
