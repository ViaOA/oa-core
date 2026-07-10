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

import java.util.*;
import java.util.logging.Logger;

import com.viaoa.callback.OAObjectSerializerCallback;
import com.viaoa.compare.match.OAMatchNotExist;
import com.viaoa.datasource.OADataSource;
import com.viaoa.hub.Hub;
import com.viaoa.oa.OA;
import com.viaoa.oa.sibling.OASiblingHelper;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.performance.OAPerformance;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;
import com.viaoa.serialize.OAObjectSerializer;



/**
 * Server-side helper used during {@code RemoteClient.getDetail(...)} operations.
 * <p>
 * {@code ClientGetDetail} is responsible for constructing the OA model that
 * should be returned to a client when it requests the value of a reference
 * property or hub (detail) from a master object. It determines:
 * <ul>
 *   <li>the master object and its siblings,</li>
 *   <li>the requested property value,</li>
 *   <li>which additional related objects must be sent so that the client
 *       receives a consistent subset of the OA model,</li>
 *   <li>how much reference depth should be loaded for master and detail
 *       objects,</li>
 *   <li>what extra data (e.g., sibling values) must accompany the response,</li>
 *   <li>and which objects have already been fully sent to the client to avoid
 *       redundant transmission.</li>
 * </ul>
 *
 * <h2>Synchronization Support</h2>
 * The class updates a per-client GUID registry that tracks which OAObjects
 * exist on the client. This is used by {@code OASyncServer} to determine which
 * sync messages should be routed or filtered for that client.
 *
 * <h2>Serialization</h2>
 * {@code ClientGetDetail} produces an {@link OAObjectSerializer} with a
 * custom serializer callback that:
 * <ul>
 *   <li>selectively includes or excludes reference properties,</li>
 *   <li>avoids resending objects previously sent with all references,</li>
 *   <li>includes only required properties for siblings,</li>
 *   <li>limits object count and compressed size to prevent large payloads.</li>
 * </ul>
 *
 * <h2>Performance</h2>
 * Several operations are time-bound (typically 40–85 ms) to prevent blocking
 * the main server thread. Objects that cannot be loaded within budget may be
 * scheduled for background loading.
 *
 * <p>
 * This class is central to OA's "detail on demand" remote-loading mechanism.
 */
public class ClientGetDetail {
	private static Logger LOG = Logger.getLogger(ClientGetDetail.class.getName());
	
	/**
	 * Identifier of the remote client requesting detail data.
	 */
	private final int clientId;

	// tracks guid for all oaObjects sent to client, used by sync filter to know which objects exist on client app.
	/**
	 * Map tracking GUIDs of OAObjects that have been sent to the client.
	 * <p>
	 * The value indicates whether the object has been fully sent with all
	 * references.
	 * </p>
	 */
	private final Map<UUID, Boolean> hmGuid;

	
	/**
	 * Creates a new ClientGetDetail instance for a specific client.
	 *
	 * @param clientId the client identifier
	 * @param hmGuid map used to track object GUIDs sent to the client
	 */
	public ClientGetDetail(int clientId, Map<UUID, Boolean> hmGuid) {
		this.clientId = clientId;
		this.hmGuid = hmGuid;
	}

	/**
	 * Removes a GUID from the client tracking map.
	 *
	 * @param guid the object GUID to remove
	 */
	public void removeGuid(long guid) {
	    hmGuid.remove(guid);
	}

	/**
	 * Adds a GUID to the client tracking map.
	 *
	 * @param guid the object GUID to add
	 */
	public void addGuid(UUID guid) {
	    hmGuid.put(guid, false);
	}

	/**
	 * Closes this ClientGetDetail instance.
	 */
	public void close() {
	}

	//    private static volatile int cntx;
	/**
	 * Counter used to limit repeated error logging.
	 */
	private static volatile int errorCnt;

	/**
	 * Retrieves a detail property or hub value for a master object.
	 * <p>
	 * Locates the master object, resolves the requested property value,
	 * optionally loads additional master or sibling data, and returns either
	 * the direct value or an {@link OAObjectSerializer} containing the value
	 * and related objects.
	 * </p>
	 *
	 * @param id request identifier
	 * @param masterClass the class of the master object
	 * @param masterObjectKey key identifying the master object
	 * @param property name of the property or reference to retrieve
	 * @param masterProps additional master properties to load
	 * @param siblingKeys keys of sibling objects to retrieve the same property from
	 * @param bForHubMerger flag indicating hub-merger usage
	 * @return the property value or an {@link OAObjectSerializer} wrapping the result
	 */
	public Object getDetail(final int id, final Class masterClass, final OAObjectKey masterObjectKey,
			final String property, final String[] masterProps, final OAObjectKey[] siblingKeys, final boolean bForHubMerger) {
	    
		if (masterObjectKey == null || property == null) {
			return null;
		}
		final long msStart = System.currentTimeMillis();

		final OA oa = OARuntime.oa(masterClass);
		OAObject masterObject = oa.internal().objects().reflect().getObject(masterClass, masterObjectKey);
		if (masterObject == null) {
			// get from datasource
			
			OADataSource ds = OARuntime.datasource().get(masterClass);
			if (ds != null) {
				masterObject = ds.getObject(masterClass, masterObjectKey);
			}
			
			if (masterObject == null) {
				if (errorCnt++ < 100) {
					LOG.warning("cant find masterObject in cache or DS.  masterClass=" + masterClass + ", key=" + masterObjectKey
							+ ", property=" + property);
				}
				return null;
			}
		}

		// 20171224 need to put siblings and masterObject in a Hub and call OAThreadLocal.detailHub        
		Hub hubHold = new Hub(masterClass);
		hubHold.add(masterObject);
		if (siblingKeys != null) {
			for (OAObjectKey key : siblingKeys) {
				OAObject obj = (OAObject) oa.internal().objects().cache().get(masterClass, key);
				if (obj != null) {
					hubHold.add(obj);
				}
			}
		}

		final OASiblingHelper siblingHelper = new OASiblingHelper(hubHold);
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		srvcOAThreadLocal.addSiblingHelper(siblingHelper);
		Object detailValue = null;
		try {
			detailValue = oa.internal().objects().reflect().getProperty((OAObject) masterObject, property);
		} finally {
			srvcOAThreadLocal.removeSiblingHelper(siblingHelper);
		}
		hubHold.clear();
		hubHold = null;

		Object returnValue;
		boolean b = ((masterProps == null || masterProps.length == 0) && (siblingKeys == null || siblingKeys.length == 0));
		if (b) {
			if (detailValue instanceof Hub) {
				if (((Hub) detailValue).getSize() > 200) {
					b = false;
				}
			}
		}

		int cntMasterPropsLoaded = 0;
		if (masterProps != null && masterObject != null) {
			boolean bx = true;
			for (String s : masterProps) {
				bx = bx && (System.currentTimeMillis() < (msStart + 50));
				if (bx) {
					masterObject.getProperty(s);
					cntMasterPropsLoaded++;
				} else {
					loadDataInBackground(masterObject, s);
				}
			}
		}

		int cntSib = 0;
		if (b && cntMasterPropsLoaded == 0) {
			returnValue = detailValue;
		} else {
			OAObjectSerializer os = getSerializedDetail(msStart, (OAObject) masterObject, detailValue, property, masterProps,
														cntMasterPropsLoaded, siblingKeys, bForHubMerger);

			os.setClientId(clientId);
			os.setId(id);

			os.setMax(1200); // max number of objects to write
			os.setMaxSize(400000); // max size of compressed data to write out

			Object objx = os.getExtraObject();
			if (objx instanceof HashMap) {
				cntSib = ((HashMap) objx).size();
				if (cntSib > 0 && (masterProps != null && masterProps.length > 0)) {
					cntSib--;
				}
			}
			returnValue = os;
		}

		long diff = System.currentTimeMillis() - msStart;
		String s = (diff > 1000) ? " ALERT" : "";

		s = String.format(
							"client=%d, id=%,d, Obj=%s, prop=%s, siblings=%,d/%,d, masterProps=%s, ms=%,d%s",
							clientId, id,
							masterObject.getClass().getSimpleName(),
							property,
							cntSib,
							(siblingKeys == null) ? 0 : siblingKeys.length,
							masterProps == null ? "" : ("" + cntMasterPropsLoaded + "/" + masterProps.length),
							diff,
							s);
		OAPerformance.LOG.fine(s);
		LOG.fine(s);

		return returnValue;
	}

	/**
	 * Creates an {@link OAObjectSerializer} for a detail request.
	 * <p>
	 * Loads required references for the master and detail objects,
	 * prepares any extra sibling data, and configures a serializer
	 * with limits and callbacks.
	 * </p>
	 *
	 * @param msStart start time of the request
	 * @param masterObject the master object
	 * @param detailObject the detail object or hub
	 * @param propFromMaster property name on the master
	 * @param masterProperties additional master properties
	 * @param cntMasterPropsLoaded count of master properties already loaded
	 * @param siblingKeys keys of sibling objects
	 * @param bForHubMerger flag indicating hub-merger usage
	 * @return a configured {@link OAObjectSerializer}
	 */
	protected OAObjectSerializer getSerializedDetail(final long msStart, final OAObject masterObject, final Object detailObject,
			final String propFromMaster, final String[] masterProperties, final int cntMasterPropsLoaded, final OAObjectKey[] siblingKeys,
			final boolean bForHubMerger) {
		// at this point, we know that the client does not have all of the master's references,
		// and we know that value != null, since getDetail would not have been called.
		// include the references "around" this object and master object, along with any siblings

		// see OASyncClient.getDetail(..)

		boolean b = wasFullySentToClient(masterObject);
		final boolean bMasterWasPreviouslySent = b && (masterProperties == null || masterProperties.length == 0);

		int tot = 0;
		Hub dHub = null;
		if (detailObject instanceof Hub) {
			dHub = (Hub) detailObject;
			tot = dHub.size();
			for (Object obj : dHub) {
				if (System.currentTimeMillis() > (msStart + 40)) {
					break;
				}
				if (wasFullySentToClient(obj)) {
					continue;
				}
				final OA oa = OARuntime.oa((OAObject) obj);
				if (oa.internal().objects().reflect().areAllReferencesLoaded((OAObject) obj, false)) {
					continue;
				}
				oa.internal().objects().reflect().loadAllReferences((OAObject) obj, 1, 0, false, 2, msStart + 40);
			}
		} else if ((detailObject instanceof OAObject) && !wasFullySentToClient(detailObject)) {
			final OA oa = OARuntime.oa((OAObject) detailObject);
			oa.internal().objects().reflect().loadAllReferences((OAObject) detailObject, 1, 0, false, 5, msStart + 40);
		}

		HashMap<OAObjectKey, Object> hmExtraData = null;
		if (tot < 5000 && siblingKeys != null && siblingKeys.length > 0) {
			hmExtraData = new HashMap<OAObjectKey, Object>();
			// send back a lightweight hashmap (oaObjKey, value)
			Class clazz = masterObject.getClass();
			boolean bLoad = true;
			
			final OA oa = OARuntime.oa(clazz);

			for (OAObjectKey key : siblingKeys) {
				OAObject obj = (OAObject) oa.internal().objects().cache().get(clazz, key);
				if (obj == null) {
					continue;
				}
				
				Object value = oa.internal().objects().property().getProperty(obj, propFromMaster, true, true);
				if (value instanceof OAMatchNotExist || value instanceof OAObjectKey) { // not loaded from ds
					if (bLoad) {
						bLoad = ((System.currentTimeMillis() - msStart) < (bForHubMerger ? 225 : 85));
					}
					if (!bLoad) {
						loadDataInBackground(obj, propFromMaster);
						continue;
					}
				}

				if (bLoad) {
					// final OA og2 = OARuntime.oa(obj);
					value = oa.internal().objects().reflect().getProperty(obj, propFromMaster); // load from DS
				} else if (value instanceof OAObjectKey) {
					continue;
				}

				if (value instanceof Hub) {
					int x = ((Hub) value).getSize();
					if (tot != 0) {
						if (tot + x > (bForHubMerger ? 5000 : 1250)) {
							continue;
						}
					}
					tot += x;
				}
				hmExtraData.put(key, value);
				if (tot > 5000) {
					break;
				}
			}
		}

		b = ((hmExtraData != null && hmExtraData.size() > 5) || (cntMasterPropsLoaded > 5));
		if (!b) {
			if (detailObject instanceof Hub) {
				if (((Hub) detailObject).getSize() > 200) {
					b = true;
				}
			}
		}

		OAObjectSerializer os = new OAObjectSerializer(detailObject, b);
		if (hmExtraData != null && hmExtraData.size() > 0) {
			if ((masterProperties != null && masterProperties.length > 0)) {
				hmExtraData.put(masterObject.getObjectKey(), masterObject); // so extra props for master can go 
			}
			os.setExtraObject(hmExtraData);
		} else {
			if ((masterProperties != null && masterProperties.length > 0)) {
				os.setExtraObject(masterObject); // so master can be sent to client, and include any other masterProps
			}
		}

		OAObjectSerializerCallback cb = createOAObjectSerializerCallback(	os, masterObject, bMasterWasPreviouslySent,
																			detailObject, dHub, propFromMaster, masterProperties,
																			siblingKeys, hmExtraData);
		os.setCallback(cb);
		return os;
	}

	/**
	 * Loads a property value in the background when it cannot be loaded
	 * within the current request time budget.
	 *
	 * @param obj the object whose property should be loaded
	 * @param property the property name to load
	 */
	protected void loadDataInBackground(OAObject obj, String property) {

	}

	// callback to customize the return values from getDetail(..) 
	/**
	 * Creates a serializer callback used to control object and reference
	 * serialization behavior.
	 *
	 * @param os the object serializer
	 * @param masterObject the master object
	 * @param bMasterWasPreviouslySent flag indicating master was fully sent
	 * @param detailObject the detail object
	 * @param detailHub the detail hub, if applicable
	 * @param propFromMaster property name on the master
	 * @param masterProperties additional master properties
	 * @param siblingKeys sibling object keys
	 * @param hmExtraData extra sibling data to include
	 * @return a configured {@link OAObjectSerializerCallback}
	 */
	private OAObjectSerializerCallback createOAObjectSerializerCallback(
			final OAObjectSerializer os,
			final OAObject masterObject, final boolean bMasterWasPreviouslySent,
			final Object detailObject, final Hub detailHub,
			final String propFromMaster,
			final String[] masterProperties, final OAObjectKey[] siblingKeys,
			final HashMap<OAObjectKey, Object> hmExtraData) {

		// this callback is used by OAObjectSerializer to customize what objects will be include in 
		//    the serialized object.
		OAObjectSerializerCallback callback = new OAObjectSerializerCallback() {
			boolean bMasterSent;
			// keep track of which objects are being sent to client in this serialization
			HashSet<UUID> hsSendingGuid = new HashSet();

			/**
			 * Tracks objects after they are serialized for a client detail response.
			 * @param obj serialized object
			 */
			@Override
			public void afterSerialize(OAObject obj) {
				final OA oa = OARuntime.oa(obj);
				UUID guid = oa.internal().objects().key().getKey(obj).getGuid();
				boolean bx = hsSendingGuid.remove(guid);
				// update tree of sent objects
                hmGuid.put(guid, bx);
			}

			// this will "tell" OAObjectSerializer which reference properties to include with each OAobj
			/**
			 * Prepares an object before it is serialized for a client detail response.
			 * @param obj object about to be serialized
			 */
			@Override
			public void beforeSerialize(final OAObject obj) {
				// parent object - will send all references
				if (obj == masterObject) {
					if (bMasterSent) {
						excludeAllProperties();
						return;
					}
					bMasterSent = true;
					if (bMasterWasPreviouslySent) {
						excludeAllProperties();
						return;
					}

					if (masterProperties == null || masterProperties.length == 0) {
						if (!os.hasReachedMax()) {
							final OA oa = OARuntime.oa(obj);
							hsSendingGuid.add(oa.internal().objects().key().getKey(obj).getGuid()); // flag that all masterObject props have been sent to client
						}
						includeAllProperties();
					} else {
						includeProperties(masterProperties);
					}
					return;
				}

				if (obj == detailObject) {
					if (this.getLevelsDeep() > 0) {
						excludeAllProperties(); // already sent in this batch
					} else if (bMasterWasPreviouslySent) {
						// already had all of master, this is only for a calculated prop
						excludeAllProperties();
					} else if (wasFullySentToClient(obj)) {
						excludeAllProperties(); // already sent
					} else {
						final OA oa = OARuntime.oa(obj);
						boolean b = oa.internal().objects().reflect().areAllReferencesLoaded(obj, false);
						if (b) {
							if (!os.hasReachedMax()) {
								hsSendingGuid.add(obj.getObjectKey().getGuid());
							}
						}
						includeAllProperties();
					}
					return;
				}

				if (detailHub != null && detailHub.contains(obj)) {
					// include all props of first 25
					boolean b = false;
					for (int i = 0; i < 25; i++) {
						Object objx = detailHub.getAt(i);
						if (objx == null || objx == obj) {
							b = true;
							break;
						}
					}
					if (!b) {
						excludeAllProperties();
					} else {
						// this Object is a Hub - will send all references (all that are been loaded)
						if (wasFullySentToClient(obj)) {
							if (!os.hasReachedMax()) {
								final OA oa = OARuntime.oa(obj);
								hsSendingGuid.add(oa.internal().objects().key().getKey(obj).getGuid());
							}
							excludeAllProperties(); // client has it all
						} else {
							final OA oa = OARuntime.oa(obj);
							b = oa.internal().objects().reflect().areAllReferencesLoaded(obj, false);
							if (b) {
								if (!os.hasReachedMax()) {
									hsSendingGuid.add(oa.internal().objects().key().getKey(obj).getGuid());
								}
							}
							includeAllProperties();
						}
					}
					return;
				}

				// for siblings, only send the reference property for now
				if (hmExtraData != null) {
					if (obj.getClass().equals(masterObject.getClass())) {
						if (hmExtraData.get(obj.getObjectKey()) != null) {
							// sibling object either is not on the client or does not have all references
							includeProperties(new String[] { propFromMaster });
							return;
						}
					}
				}

				// second level object - will send all references that are already loaded
				Object objPrevious = this.getPreviousObject();
				boolean b = (objPrevious != null && objPrevious == detailObject);
				b = b || (objPrevious == masterObject);
				b = b || (detailHub != null && (objPrevious != null && detailHub.contains(objPrevious)));

				if (b && !bMasterWasPreviouslySent) {
					if (isOnClient(obj)) {
						excludeAllProperties(); // client already has it, might not be all of it
					} else {
						// client does not have it, send whatever is loaded
						final OA oa = OARuntime.oa(obj);
						b = oa.internal().objects().reflect().areAllReferencesLoaded(obj, false);
						if (b) {
							if (!os.hasReachedMax()) {
								hsSendingGuid.add(oa.internal().objects().key().getKey(obj).getGuid());
							}
						}
						includeAllProperties(); // will send whatever is loaded
					}
					return;
				}

				// "leaf" reference that client does not have, only include owned references
				excludeAllProperties();
			}

			/**
			 * This allows returning an objKey if the object is already on the client.
			 */
			@Override
			public Object getReferenceValueToSend(final Object object) {
				// dont send sibling objects back, use objKey instead
				// called by: OAObjectSerializerDelegate for ref props 
				// called by: HubDataMaster write, so key can be sent instead of masterObject 
				if (!(object instanceof OAObject)) {
					return object;
				}

				OAObjectKey k = null;
				if (object == masterObject || object == detailObject) {
					k = ((OAObject) object).getObjectKey();
					return k;
				}

				if (siblingKeys != null) {
					k = ((OAObject) object).getObjectKey();
					for (OAObjectKey k2 : siblingKeys) {
						if (k.getGuid() == k2.getGuid()) {
							return k;
						}
					}
				}

				if (isOnClient(object)) {
					if (k == null) {
						k = ((OAObject) object).getObjectKey();
					}
					return k;
				}

				return object;
			}

			/* this is called when a reference has already been included, by the setup() method.
			 * this will see if the object already exists on the client to determine if it will
			 * be sent.  Otherwise, oaobject.writeObject will only send the oaKey, so that it will
			 * be looked up on the client. 
			 */
			@Override
			public boolean shouldSerializeReference(final OAObject oaObj, final String propertyName, final Object referenceValue,
					final boolean bDefault) {
				if (!bDefault) {
					return false;
				}
				if (referenceValue == null) {
					return false;
				}

				if (oaObj == masterObject) {
					return true;
				}

				if (oaObj == detailObject) {
					return !wasFullySentToClient(referenceValue);
				}

				final OA oa = OARuntime.oa(oaObj);
				OAObjectKey key = oa.internal().objects().key().getKey(oaObj);
				if (hmExtraData != null) {
					if (oaObj.getClass().equals(masterObject.getClass())) {
						if (hmExtraData.get(key) != null) {
							return true;
						}
					}
				}

				if (referenceValue instanceof Hub) {
					Hub hubValue = (Hub) referenceValue;
					if (hubValue.getSize() == 0) {
						return false;
					}

					// dont include hubs with masterObject in it, so that it wont be sending sibling data for masterObj
					if (hubValue.contains(masterObject)) {
						return false;
					}

					// dont send other sibling data
					if (detailObject != null && detailHub == null && hubValue.contains(detailObject)) {
						return false;
					}

					// this will do a quick test to see if this is a Hub with any of the same objects in it.
					if (detailHub != null) {
						if (!detailHub.getObjectClass().equals(hubValue.getObjectClass())) {
							return true;
						}
						Hub h1, h2;
						if (detailHub.getSize() > hubValue.getSize()) {
							h1 = hubValue;
							h2 = detailHub;
						} else {
							h1 = detailHub;
							h2 = hubValue;
						}
						for (int i = 0; i < 3; i++) {
							Object objx = h1.getAt(i);
							if (objx == null) {
								break;
							}
							if (h2.contains(objx)) {
								return false;
							}
						}
					}
					return true;
				}

				if (!(referenceValue instanceof OAObject)) {
					return true;
				}

				int level = this.getLevelsDeep();

				if (referenceValue == masterObject) {
					if (bMasterSent) {
						return false;
					}
					if (level > 1) {
						return false; // wait for it to be saved at correct position
					}
					return true;
				}

				if (referenceValue == detailObject) {
					return false; // only save as begin obj
				}
				if (detailHub != null && detailHub.contains(referenceValue)) {
					return false; // only save as begin obj
				}

				if (level == 0) {
					return false; // extra data does not send it's references
				}

				UUID guid = key.getGuid();
				
				Object objx = hmGuid.get(guid);
				boolean b = objx != null && ((Boolean) objx).booleanValue();
				if (b) {
					return false; // already sent with all refs
				}

				// second level object - will send all references that are already loaded
				if (level < 3) {
					return true;
				}
				return (objx == null);
			}
		};
		return callback;
	}

	/**
	 * Determines whether an object exists on the client.
	 *
	 * @param obj the object to test
	 * @return {@code true} if the object exists on the client, otherwise {@code false}
	 */
	private boolean isOnClient(Object obj) {
		if (!(obj instanceof OAObject)) {
			return false;
		}
		
		UUID guid = ((OAObject) obj).getObjectKey().getGuid();
		return hmGuid.containsKey(guid);
	}

	/**
	 * Determines whether an object has already been fully sent to the client.
	 *
	 * @param obj the object to test
	 * @return {@code true} if the object was fully sent, otherwise {@code false}
	 */
	private boolean wasFullySentToClient(Object obj) {
		if (!(obj instanceof OAObject)) {
			return false;
		}
		
        UUID guid = ((OAObject) obj).getObjectKey().getGuid();
		Boolean bx = hmGuid.get(guid);
		if (bx == null) return false;
		return bx.booleanValue();
	}
}
