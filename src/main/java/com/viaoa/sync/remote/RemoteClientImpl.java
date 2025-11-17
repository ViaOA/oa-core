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
package com.viaoa.sync.remote;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectPropertyDelegate;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.sync.OASyncDelegate;
/**
 * Base server-side implementation of {@link RemoteClientInterface}. Each
 * connected client has its own concrete instance, created by the server's
 * {@code RemoteServerImpl}.
 * <p>
 * A {@code RemoteClientImpl} services requests originating from a client:
 * <ul>
 *   <li>detail loading via {@link ClientGetDetail},</li>
 *   <li>datasource operations such as {@code insert}, {@code update},
 *       {@code select}, and {@code count},</li>
 *   <li>object caching and cache retention on the server,</li>
 *   <li>copy operations and refresh operations.</li>
 * </ul>
 *
 * <h2>Detail Loading</h2>
 * Detail loading is delegated to a {@link ClientGetDetail} instance, which
 * manages sibling logic, depth rules, and object-graph serialization.
 *
 * <h2>Datasource Access</h2>
 * This class exposes a virtual {@link RemoteDataSource} to the client:
 * <ul>
 *   <li>All DS commands execute on the server's {@link OADataSource}.</li>
 *   <li>Iterator state for selects is tracked per client.</li>
 *   <li>Objects loaded from the datasource are optionally flagged in the
 *       client's GUID registry so the sync layer knows they exist on the
 *       client.</li>
 * </ul>
 *
 * <h2>Cache Retention</h2>
 * The abstract {@link #updateObjectCache(OAObject)} method allows the server
 * to mark objects as “in use” by that client, preventing premature GC or
 * eviction.
 *
 * <p>
 * This class contains substantial shared logic for all remote client sessions.
 */
public abstract class RemoteClientImpl implements RemoteClientInterface {
	private static Logger LOG = Logger.getLogger(RemoteClientImpl.class.getName());
	// protected ConcurrentHashMap<Object, Object> hashCache = new ConcurrentHashMap<Object, Object>();
	// protected ConcurrentHashMap<Object, Object> hashLock = new ConcurrentHashMap<Object, Object>();
	private ClientGetDetail clientGetDetail;
	private volatile RemoteDataSource remoteDataSource;
	private int sessionId;
	private final Map<Long, Boolean> hmGuid;

	public RemoteClientImpl(int sessionId, Map<Long, Boolean> hmGuid) {
		this.sessionId = sessionId;
		this.hmGuid = hmGuid;
		clientGetDetail = new ClientGetDetail(sessionId, hmGuid) {
			@Override
			protected void loadDataInBackground(OAObject obj, String property) {
				RemoteClientImpl.this.loadDataInBackground(obj, property);
			}
		};
	}

	/**
	 * called when a other props or sibling data cant be loaded for current request, because of timeout. This can be overwritten to have it
	 * done in a background thread.
	 */
	protected void loadDataInBackground(OAObject obj, String property) {
	}

	// 20160101
	public void close() {
		clientGetDetail.close();
		clientGetDetail = null;
		remoteDataSource = null;
	}

	@Override
	public Object getDetail(int id, Class masterClass, OAObjectKey masterObjectKey, String property, String[] masterProps,
			OAObjectKey[] siblingKeys, boolean bForHubMerger) {
		LOG.fine(id + ") masterClass=" + masterClass + ", prop=" + property);
		Object obj = clientGetDetail.getDetail(id, masterClass, masterObjectKey, property, masterProps, siblingKeys, bForHubMerger);
		return obj;
	}

	// 20151129 does not put in the msg queue, but will write the return value using the same vsocket that the msg queue thread uses.
	@Override
	public Object getDetailNow(int id, Class masterClass, OAObjectKey masterObjectKey, String property, String[] masterProps,
			OAObjectKey[] siblingKeys, boolean bForHubMerger) {
		LOG.fine(id + ") masterClass=" + masterClass + ", prop=" + property);
		Object obj = clientGetDetail.getDetail(id, masterClass, masterObjectKey, property, masterProps, siblingKeys, bForHubMerger);
		return obj;
	}

	@Override
	public Object getDetail(int id, Class masterClass, OAObjectKey masterObjectKey, String property, boolean bForHubMerger) {
		Object obj = clientGetDetail.getDetail(id, masterClass, masterObjectKey, property, null, null, bForHubMerger);
		return obj;
	}

	public RemoteDataSource getRemoteDataSource() {
		if (remoteDataSource == null) {
			synchronized (this) {
				if (remoteDataSource == null) {
					remoteDataSource = new RemoteDataSource() {
						// used when an object from ds is not already in a hub with master.
						@Override
						public void setCached(OAObject obj) {
                            if (hmGuid != null) {
                                long guid = obj.getGuid();
                                hmGuid.putIfAbsent(guid, false);
                            }
							RemoteClientImpl.this.updateObjectCache(obj);
						}
					};
				}
			}
		}
		return remoteDataSource;
	}

	@Override
	public Object datasource(int command, Object[] objects) {
		Object result = null;
		try {
			result = getRemoteDataSource().datasource(command, objects);
		} catch (Exception e) {
			RuntimeException ex = new RuntimeException(
					"Exception in remoteClient.datasource, command=" + command + ", original exception msg=" + e.toString(), e);
			throw (ex);
		}
		return result;
	}

	
	@Override
	public Object datasourceReturnOnQueue(int command, Object[] objects) {
		Object result = null;
		try {
			result = getRemoteDataSource().datasource(command, objects);
		} catch (Exception e) {
			RuntimeException ex = new RuntimeException(
					"Exception in remoteClient.datasource, command=" + command + ", original exception msg=" + e.toString(), e);
			throw (ex);
		}
		return result;
	}
	
	
	@Override
	public void datasourceNoReturn(int command, Object[] objects) {
		try {
			getRemoteDataSource().datasource(command, objects);
		} catch (Exception e) {
			RuntimeException ex = new RuntimeException(
					"Exception in remoteClient.datasourceNoReturn, command=" + command + ", original exception msg=" + e.toString());
			throw (ex);
		}
	}

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
				return dss[0];
			}
		}
		return defaultDataSource;
	}

	protected OADataSource defaultDataSource;

	protected OADataSource getDataSource() {
		return getDataSource(null);
	}

	@Override
	public OAObject createCopy(Class objectClass, OAObjectKey objectKey, String[] excludeProperties) {
		OAObject obj = (OAObject) OAObjectCacheDelegate.getObject(objectClass, objectKey);
		if (obj == null) {
			return null;
		}
		OAObject objx = OAObjectReflectDelegate.createCopy(obj, excludeProperties);
		return objx;
	}

	/**
	 * Called to add objects to a client's server side cache, so that server will not GC the object.
	 */
	public abstract void updateObjectCache(OAObject obj);

	@Override
	public boolean deleteAll(Class objectClass, OAObjectKey objectKey, String hubPropertyName) {
		OAObject obj = getObject(objectClass, objectKey);
		if (obj == null) {
			return false;
		}

		Hub h = getHub(obj, hubPropertyName);
		if (h == null) {
			// store null so that it can be an empty hub if needed (and wont have to get from server)
			if (!OASyncDelegate.isServer(objectClass)) {
				OAObjectPropertyDelegate.setPropertyCAS(obj, hubPropertyName, null, null, true, false);
			}
			return false;
		}
		h.deleteAll();
		return true;
	}

	// on the server, if the object is not found in the cache, then it will be loaded by the datasource
	private OAObject getObject(Class objectClass, OAObjectKey origKey) {
		OAObject obj = (OAObject) OAObjectCacheDelegate.get(objectClass, origKey);
		if (obj == null && OASyncDelegate.isServer(objectClass)) {
			obj = (OAObject) OADataSource.getObject(objectClass, origKey);
			if (obj != null) {
				// object must have been GCd, use the original guid
				OAObjectDelegate.reassignGuid(obj, origKey);
			}
		}
		return obj;
	}

	// on the server, if the Hub is not found in the cache, then it will be loaded by the datasource
	private Hub getHub(OAObject obj, String hubPropertyName) {
		if (obj == null) {
			return null;
		}
		boolean bWasLoaded = OAObjectReflectDelegate.isReferenceHubLoaded(obj, hubPropertyName);
		if (!bWasLoaded && !OASyncDelegate.isServer(obj.getClass())) {
			return null;
		}
		Object objx = OAObjectReflectDelegate.getProperty(obj, hubPropertyName);
		if (!(objx instanceof Hub)) {
			return null;
		}

		// loadCachedOwners will have been done by the call to getObject(masterObj)
		return (Hub) objx;
	}

	/* moved to  remoteSync  serverDelete, clientDelete
	@Override
	public boolean delete(Class objectClass, OAObjectKey objectKey) {
		OAObject obj = getObject(objectClass, objectKey);
		if (obj == null) {
			return false;
		}
		obj.delete();
		return true;
	}
	*/

	@Override
	public void refresh(Class objectClass, OAObjectKey objectKey) {
		OAObject obj = (OAObject) OAObjectCacheDelegate.get(objectClass, objectKey);
		if (obj != null) {
			obj.refresh();
		}
	}

	@Override
	public void refresh(Class objectClass, OAObjectKey objectKey, String propertyName) {
		OAObject obj = (OAObject) OAObjectCacheDelegate.get(objectClass, objectKey);
		if (obj != null) {
			obj.refresh(propertyName);
		}
	}
}
