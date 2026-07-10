package com.viaoa.oa.service.object;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

import com.viaoa.converter.OAConverter;
import com.viaoa.hub.Hub;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;

/*qqqqqqqqqqqqqq
CODEX



*/

public abstract class OAObjectKeyService {
	private static final Logger LOG = Logger.getLogger(OAObjectKeyService.class.getName());

	/**
	 * Performs OAObjectKeyService behavior for the OA object service.
	 */
	public OAObjectKeyService() {
	}

	/**
	 * Creates an {@link OAObjectKey} for the given object.
	 * <p>
	 * The key is constructed using the object's current ID property values and its GUID.
	 *
	 * @param obj the source object, or {@code null}
	 * @return a new {@link OAObjectKey} for the object, or {@code null} if the object is {@code null}
	 */
	public OAObjectKey createObjectKey(OAObject obj) {
		if (obj == null) return null;
		OAObjectKey key = new OAObjectKey(callObjectInfoGetPropertyIdValues(obj), obj.getGuid());
		return key;
	}

	/**
	 * Creates an {@link OAObjectKey} using the provided ID values and GUID.
	 *
	 * @param ids  the ID values to include in the key
	 * @param guid the GUID to associate with the key
	 * @return a new {@link OAObjectKey} instance
	 */
	public OAObjectKey createObjectKey(Object[] ids, UUID guid) {
		return createObjectKey((OAObjectInfo) null, ids, guid);
	}

	/**
	 * Creates an {@link OAObjectKey} for the given class using the supplied ID values.
	 * <p>
	 * Delegates to {@link #createObjectKey(Class, long, Object...)} with a GUID of {@code 0L}.
	 *
	 * @param c   the class used to resolve object metadata, or {@code null}
	 * @param ids the ID values or an {@link OAObject} whose key should be returned
	 * @return a newly created {@link OAObjectKey}, or {@code null} if no IDs are provided
	 */
	public OAObjectKey createObjectKey(final Class<? extends OAObject> c, final Object ...ids) {
		return createObjectKey(c, (UUID) null, ids);
	}

	/**
	 * Creates an {@link OAObjectKey} for the specified class, GUID, and ID values.
	 * <p>
	 * If a single ID value is provided and it is an {@link OAObject}, that object's key
	 * is returned directly. Otherwise, the class metadata is retrieved and used to
	 * construct a new key.
	 *
	 * @param c    the object's class, or {@code null}
	 * @param guid the GUID to associate with the key
	 * @param ids  the ID values or an {@link OAObject} reference
	 * @return the resulting {@link OAObjectKey}
	 */
	public OAObjectKey createObjectKey(final Class<? extends OAObject> c, final UUID guid, final Object ...ids) {
		if (ids != null && ids.length == 1) {
			if (ids[0] instanceof OAObject) {
				return getObjectKey((OAObject) ids[0]);
			}
		}
		OAObjectInfo oi = c == null ? null : callInfogetObjectInfo(c);
		return createObjectKey(oi, ids, guid);
	}
	
	/**
	 * Creates an {@link OAObjectKey} using the provided object info, ID values, and GUID.
	 * <p>
	 * If metadata is available, each ID value is converted to the expected property type
	 * unless it is already an {@link OAObjectKey} or an {@link OAObject}. After any
	 * necessary conversions, a new {@link OAObjectKey} is created.
	 *
	 * @param oi   the {@link OAObjectInfo} describing ID properties, or {@code null}
	 * @param ids  the ID values to include in the key
	 * @param guid the GUID to associate with the key
	 * @return a new {@link OAObjectKey} instance
	 */
	public OAObjectKey createObjectKey(OAObjectInfo oi, Object[] ids, UUID guid) {
		Object[] idsNew = null;
		if (oi != null && ids != null && ids.length > 0) {
			String[] idProperties = oi.getIdProperties();
			if (idProperties != null && idProperties.length == ids.length) {
				for (int i = 0; i < idProperties.length; i++) {
					if (ids[i] instanceof OAObjectKey) {
						continue;
					}
					else if (!(ids[i] instanceof OAObject)) { // note: OAObjectKey constructor will handle id values that are OAObject
						Class c = callInfoGetPropertyClass(oi, idProperties[i]);
						if (idsNew == null) {
							idsNew = new Object[ids.length];
							System.arraycopy(ids, 0, idsNew, 0, ids.length);
						}
						idsNew[i] = OAConverter.convert(c, idsNew[i], null);
					}
				}
			}
		}
		OAObjectKey ok;
		if (idsNew != null) ok = new OAObjectKey(idsNew, guid);
		else ok = new OAObjectKey(ids, guid);
		return ok;
	}

	/**
	 * Creates an {@link OAObjectKey} from a single ID value.
	 * <p>
	 * If the value is already an {@link OAObjectKey}, it is returned unchanged.
	 * If it is an {@link OAObject}, a key is created from that object.
	 * If it is an array, the elements are treated as ID values.
	 *
	 * @param id the ID value, array of IDs, {@link OAObject}, or {@link OAObjectKey}
	 * @return a corresponding {@link OAObjectKey}, or {@code null} if {@code id} is {@code null}
	 */
	public OAObjectKey createObjectKey(Object id) {
		if (id == null) return null;
		if (id instanceof OAObjectKey) return (OAObjectKey) id;
		if (id instanceof OAObject) return createObjectKey((OAObject) id);
		if (id instanceof UUID) return createObjectKey((OAObjectInfo) null, (Object[]) null, (UUID) id);
		if (id.getClass().isArray()) return createObjectKey((OAObjectInfo) null, (Object[]) id, (UUID) null);
		return createObjectKey((OAObjectInfo) null, new Object[] {id}, (UUID) null);
	}

	/**
	 * Performs createObjectKey behavior for the OA object service.
	 *
	 * @param guid method input
	 * @return result value
	 */
	public OAObjectKey createObjectKey(UUID guid) {
		if (guid == null) return null;
		return createObjectKey((OAObjectInfo) null, (Object[]) null, guid);
	}

	/**
	 * Creates an {@link OAObjectKey} from the supplied ID values.
	 * <p>
	 * If no IDs are provided, {@code null} is returned. Otherwise, a new key is
	 * created using the values as the object ID components.
	 *
	 * @param ids the ID values used to build the key
	 * @return a new {@link OAObjectKey}, or {@code null} if no IDs are supplied
	 */
	public OAObjectKey createObjectKey(Object... ids) {
		if (ids == null || ids.length == 0) return null;
		return createObjectKey((OAObjectInfo) null, (Object[]) ids, (UUID) null);
	}

	/**
	 * Determines whether two {@link OAObjectKey} instances refer to the same object.
	 * <p>
	 * Keys are considered to represent the same object if:
	 * <ul>
	 *   <li>they are equal, or</li>
	 *   <li>both have non-zero GUIDs that match, or</li>
	 *   <li>their ID arrays are equal, or</li>
	 *   <li>one has only a GUID and the other only ID values, and the object
	 *       resolved from cache provides matching IDs</li>
	 * </ul>
	 *
	 * @param clazz the object class used for cache lookup when resolving mixed key formats
	 * @param ok1   the first key to compare
	 * @param ok2  the second key to compare
	 * @return {@code true} if both keys refer to the same object; otherwise {@code false}
	 */
	public <T extends OAObject> boolean isForSameOAObject(final Class<T> clazz, final OAObjectKey ok1, final OAObjectKey ok2) {
		if (ok1 == ok2) return true;
		if (ok1 == null || ok2 == null) return false;
		UUID g1 = ok1.getGuid();
		UUID g2 = ok2.getGuid();
		if (g1 != null && g2 != null) {
	        return Objects.equals(g1, g2);  
		}

		
		Object[] ids1 = ok1.hasValidObjectIds() ? ok1.getObjectIds() : null;
		Object[] ids2 = ok2.hasValidObjectIds() ? ok2.getObjectIds() : null;
		if (ids1 == null && ids2 == null) return false;
		
		if (ids1 != null && ids2 != null) {
			return Arrays.equals(ids1, ids2);
		}
		
		if (clazz == null) return false;
		if (g1 == null && g2 == null) return false;
		
		if (g1 != null && ids1 == null) {
			T objx = callCacheGet(clazz, g1);
			if (objx == null) return false;
			OAObjectKey okx = objx.getObjectKey();
			if (okx.hasValidObjectIds()) {
				return isForSameOAObject(null, okx, ok2);
			}
		}
		else if (g2 != null && ids2 == null) {
			T objx = callCacheGet(clazz, g2);
			if (objx == null) return false;
			OAObjectKey okx = objx.getObjectKey();
			if (okx.hasValidObjectIds()) {
				return isForSameOAObject(null, ok1, okx);
			}
		}
		return false;
	}

	/**
	 * Performs hasSameGuid behavior for the OA object service.
	 *
	 * @param a method input
	 * @param b method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public boolean hasSameGuid(final OAObjectKey a, final OAObjectKey b) {
	    return a != null && b != null && Objects.equals(a.getGuid(), b.getGuid());
	}

	/**
	 * Performs hasSameIds behavior for the OA object service.
	 *
	 * @param a method input
	 * @param b method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public boolean hasSameIds(final OAObjectKey a, final OAObjectKey b) {
	    return a != null && b != null && Arrays.equals(a.getObjectIds(), b.getObjectIds());
	}

	/**
	 * Performs guidMatchesButIdsDiffer behavior for the OA object service.
	 *
	 * @param a method input
	 * @param b method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public boolean guidMatchesButIdsDiffer(final OAObjectKey a, final OAObjectKey b) {
	    if (a == null || b == null) return false;
	    UUID g1 = a.getGuid(), g2 = b.getGuid();
	    if (g1 == null || g2 == null) return false;
	    if (!g1.equals(g2)) return false;
	    return !Arrays.equals(a.getObjectIds(), b.getObjectIds());
	}
	
	
	
	/**
	 * Retrieves an {@link OAObject} for the given class and key.
	 * <p>
	 * The cache is checked first; if not found, the object is requested from
	 * the datasource.
	 *
	 * @param c   the object's class
	 * @param key the object key to locate
	 * @return the matching object, or {@code null} if none is found
	 */
	public <T extends OAObject> OAObject getOAObject(Class<T> c, OAObjectKey key) {
		if (c == null || key == null) return null;

		OAObject obj = callCacheGet(c, key);
		if (obj != null) return obj;
		OAObjectInfo oi = callInfogetObjectInfo(c);
		obj = callDSGetObject(oi, c, key);
		return obj;
	}

	/**
	 * Convenience wrapper around {@link #createObjectKey(OAObject)}.
	 *
	 * @param oaObj the source object
	 * @return the object's {@link OAObjectKey}
	 */
	public OAObjectKey getKey(OAObject oaObj) {
		return createObjectKey(oaObj);		
	}

	/**
	 * Convenience wrapper around {@link #createObjectKey(OAObject)}.
	 *
	 * @param oaObj the source object
	 * @return the object's {@link OAObjectKey}
	 */
	public OAObjectKey getObjectKey(OAObject oaObj) {
		return createObjectKey(oaObj);		
	}

	/**
	 * Returns the GUID of the specified object.
	 *
	 * @param oaObj the object whose GUID is requested
	 * @return the object's GUID, or {@code 0} if the object is {@code null}
	 */
	public UUID getGuid(OAObject oaObj) {
		if (oaObj == null) return null;
		return oaObj.getGuid();
	}

	/**
	 * Creates a new {@link OAObjectKey} reflecting a change to one ID property.
	 * <p>
	 * The object's key properties are retrieved from metadata. A new key array is
	 * constructed by copying existing ID values and replacing the one that matches
	 * the specified property name. The GUID is preserved if an original key exists.
	 *
	 * @param clazz        the object's class used to retrieve key metadata
	 * @param objKey       the original key, or {@code null}
	 * @param propertyName the ID property being changed
	 * @param newValue     the new value for the changed property
	 * @return a new {@link OAObjectKey} containing the updated ID values
	 */
	public OAObjectKey createChangedObjectKey(Class<? extends OAObject> clazz, OAObjectKey objKey, String propertyName, Object newValue) {
		if (clazz == null) {
			return null;
		}

		OAObjectInfo oi = callInfogetObjectInfo(clazz);
		String[] ids = oi.getKeyProperties();

		Object[] objsCurrent = null;
		if (objKey != null) {
			objsCurrent = objKey.getObjectIds();
		}

		Object[] objsNew = new Object[ids == null ? 0 : ids.length];

		for (int i = 0; ids != null && i < ids.length; i++) {
			if (propertyName != null && propertyName.equalsIgnoreCase(ids[i])) {
				objsNew[i] = newValue;
			} else {
				if (objsCurrent != null && i < objsCurrent.length) {
					objsNew[i] = objsCurrent[i];
				}
			}
		}

		OAObjectKey ok;
		if (objKey != null) {
			ok = new OAObjectKey(objsNew, objKey.getGuid());
		} else {
			ok = new OAObjectKey(objsNew);
		}
		return ok;
	}

	/**
	 * Handles updates after an object's ID-related property value changes.
	 * <p>
	 * A new {@link OAObjectKey} is created for the object, and—if verification is
	 * enabled—checks are performed to ensure that no other object already uses the
	 * resulting key. Cache indexes are then updated, and any child objects whose
	 * keys include this object are also updated.
	 *
	 * @param oaObj   the object whose key-related property changed
	 * @param okOrig  the original key (not used)
	 * @param bVerify whether to verify that the new key does not conflict with another object
	 * @return {@code true} after processing the key change
	 */
	public boolean afterChangedObjectKeyProperty(final OAObject oaObj, final OAObjectKey okOrig, boolean bVerify) {
		//qqqqqq method was protected
		if (oaObj == null) return false;
		final OAObjectKey okNew = createObjectKey(oaObj);
		
		if (bVerify) {
			if (callIsRemoteThread()) {
				bVerify = false;
			}
			if (bVerify) {
				if (callDSIsAssigningId(oaObj)) {
					bVerify = false;
				} else if (callThreadLocalIsLoading()) {
					bVerify = false;
				}
			}
		}

		if (bVerify) {
			// make sure objectId is unique.  Check in Cache, on Server, in Database
			String s = verifyKeyChange(oaObj, okNew);
			if (s != null) {
				throw new RuntimeException(s);
			}
		}

		// update cache indexes
		callCachePropertyKeyValueChanged(oaObj);

		// need to recalc keys for all children that have this object as part of their object key
		OAObjectInfo oi = callInfogetObjectInfo(oaObj.getClass());
		List<OALinkInfo> al = oi.getLinkInfos();
		for (int i = 0; al != null && i < al.size(); i++) {
			OALinkInfo li = (OALinkInfo) al.get(i);
			if (li.getPrivateMethod()) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}
			if (!callReflectIsReferenceObjectLoadedAndNotEmpty(oaObj, li.getName())) {
				continue;
			}

			String revProp = li.getReverseName();
			if (revProp == null || revProp.length() == 0) {
				continue;
			}
			OAObjectInfo oiRev = callInfogetObjectInfo(li.getToClass());

			if (!callInfoIsIdProperty(oiRev, revProp)) {
				continue;
			}

			Object obj = callReflectGetProperty(oaObj, li.getName());
			if (obj instanceof Hub) {
				Hub h = (Hub) obj;
				for (int ii = 0;; ii++) {
					OAObject oa = (OAObject) h.elementAt(ii);
					if (oa == null) {
						break;
					}
					callCachePropertyKeyValueChanged(oa);
				}
			} else if (obj instanceof OAObject) {
				callCachePropertyKeyValueChanged((OAObject) obj);
			}
		}
		return true;
	}
	
	/**
	 * Verifies that the supplied key change does not conflict with an existing object.
	 * <p>
	 * Ensures that ID changes are allowed, checks for duplicates in the cache, on the
	 * server, and in the datasource, and returns an error message if the new key is
	 * already in use. If no conflict exists, {@code null} is returned.
	 *
	 * @param oaObj         the object whose key is being changed
	 * @param newObjectKey  the newly generated key
	 * @return a descriptive error message if the new key is already used; otherwise {@code null}
	 */
	public String verifyKeyChange(final OAObject oaObj, final OAObjectKey newObjectKey) {
		if (oaObj == null) return null;
		OAObjectInfo oi = null;
		if (!oaObj.getNew() && !oaObj.getDeleted()) {
			if (oi == null) {
				oi = callInfogetObjectInfo(oaObj.getClass());
			}
			if (!callDSAllowIdChange(oaObj.getClass())) {
				return ("ID property can not be changed if " + oaObj.getClass().getSimpleName() + " has been saved");
			}
		}
		
		OAObject objInCache = callCacheGet(oaObj.getClass(), newObjectKey.getObjectIds());
		if ((objInCache == null || objInCache == oaObj)) {
			if (oi == null) {
				oi = callInfogetObjectInfo(oaObj.getClass());
			}
			if (!oi.getLocalOnly() && callCSIsClient(oaObj)) {
				// check on server.  If server has same object as this, resolve() will return this object
				objInCache = callCSGetServerObject(oaObj.getClass(), newObjectKey);
			}
		}

		if (objInCache != null && objInCache != oaObj && objInCache.getDeleted()) {
			callCacheRemoveObject((OAObject) objInCache);
			objInCache = null;
		}

		if (objInCache != oaObj) {
			if (objInCache != null) {
				if (callThreadLocalGetObjectCacheAddMode() == OAObjectCacheService.NO_DUPS) {
					// id already used

					Object[] ids = newObjectKey.getObjectIds();
					//was: Object[] ids = srvcObject.getOAObjectInfoService().getPropertyIdValues(oaObj);

					String s = "";
					for (int i = 0; ids != null && i < ids.length; i++) {
						if (ids[i] != null) {
							if (s.length() > 0) {
								s += " ";
							}
							s += ids[i];
						}
					}
					return ("ObjectId \"" + s + "\" already used.");// by another object - "+oaObj.getClass());
				}
			} else {
				if (!callThreadLocalIsLoading()) {
					// make sure object does not already exist in datasource
					if (oi == null) {
						oi = callInfogetObjectInfo(oaObj.getClass());
					}
					if (oi.getUseDataSource()) {
						objInCache = (OAObject) callDSGetObject(oi, oaObj.getClass(), newObjectKey);
						if (objInCache != oaObj && objInCache != null) {
							Object[] ids = newObjectKey.getObjectIds();
							// Object[] ids = srvcObject.getOAObjectInfoService().getPropertyIdValues(oaObj);
							String s = "";
							for (int i = 0; ids != null && i < ids.length; i++) {
								if (i > 0) {
									s += " ";
								}
								s += ids[i];
							}
							return ("ObjectId \"" + s + "\" already used");// by another object - "+oaObj.getClass());
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Retrieves the value of an ID property from the specified {@link OAObjectKey}.
	 * <p>
	 * The object's metadata is inspected to locate the index of the requested
	 * property name within the key's ID array. If found, the corresponding ID
	 * component is returned.
	 *
	 * @param clazz         the object's class used to obtain key metadata
	 * @param objectKey     the key containing ID values
	 * @param propertyName  the name of the ID property to retrieve
	 * @return the matching ID value, or {@code null} if not found
	 */
	public Object getProperty(final Class<? extends OAObject> clazz, final OAObjectKey objectKey, final String propertyName) {
		if (clazz == null || objectKey == null || propertyName == null) {
			return null;
		}

		OAObjectInfo oi = callInfogetObjectInfo(clazz);
		String[] ids = oi.getKeyProperties();
		if (ids == null || ids.length == 0) {
			return null;
		}

		for (int i = 0; ids != null && i < ids.length; i++) {
			if (propertyName.equalsIgnoreCase(ids[i])) {
				Object[] ids2 = objectKey.getObjectIds();
			    if (ids2 != null && ids2.length > i) {
			        return ids2[i];
			    }
			}
		}
		return null;
	}

	/**
	 * Dependency hook used by this service to cacheGet.
	 *
	 * @param clazz method input
	 * @param ok method input
	 * @return result value
	 */
	public abstract <T extends OAObject> T callCacheGet(Class<T> clazz, OAObjectKey ok);
	/**
	 * Dependency hook used by this service to cachePropertyKeyValueChanged.
	 *
	 * @param obj method input
	 */
	public abstract void callCachePropertyKeyValueChanged(OAObject obj);
	/**
	 * Dependency hook used by this service to cacheGet.
	 *
	 * @param clazz method input
	 * @param key method input
	 * @return result value
	 */
	public abstract <T extends OAObject> T callCacheGet(Class<T> clazz, Object key);
	/**
	 * Dependency hook used by this service to cacheRemoveObject.
	 *
	 * @param obj method input
	 */
	public abstract void callCacheRemoveObject(final OAObject obj); 
	
	/**
	 * Dependency hook used by this service to cSIsSingleUser.
	 *
	 * @param obj method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callCSIsSingleUser(OAObject obj);
	/**
	 * Dependency hook used by this service to cSIsServer.
	 *
	 * @param obj method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callCSIsServer(OAObject obj);
	/**
	 * Dependency hook used by this service to cSIsClient.
	 *
	 * @param obj method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callCSIsClient(OAObject obj);
	
	
	/**
	 * Dependency hook used by this service to cSGetServerObject.
	 *
	 * @param clazz method input
	 * @param key method input
	 * @return result value
	 */
	public abstract <T extends OAObject> T callCSGetServerObject(Class<T> clazz, OAObjectKey key);
	
	/**
	 * Dependency hook used by this service to dSIsAssigningId.
	 *
	 * @param obj method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callDSIsAssigningId(OAObject obj);
	/**
	 * Dependency hook used by this service to dSAllowIdChange.
	 *
	 * @param c method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callDSAllowIdChange(Class<? extends OAObject>  c);
	/**
	 * Dependency hook used by this service to dSGetObject.
	 *
	 * @param oi method input
	 * @param clazz method input
	 * @param key method input
	 * @return result value
	 */
	public abstract <T extends OAObject> T callDSGetObject(OAObjectInfo oi, Class<T> clazz, OAObjectKey key);

	/**
	 * Dependency hook used by this service to infogetObjectInfo.
	 *
	 * @param clazz method input
	 * @return result value
	 */
	public abstract OAObjectInfo callInfogetObjectInfo(Class clazz); 
	/**
	 * Dependency hook used by this service to infoIsIdProperty.
	 *
	 * @param oi method input
	 * @param propertyName method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callInfoIsIdProperty(OAObjectInfo oi, String propertyName);
	/**
	 * Dependency hook used by this service to infoGetPropertyClass.
	 *
	 * @param oi method input
	 * @param propertyName method input
	 * @return result value
	 */
	public abstract Class<? extends OAObject> callInfoGetPropertyClass(OAObjectInfo oi, String propertyName);
	/**
	 * Dependency hook used by this service to objectInfoGetPropertyIdValues.
	 *
	 * @param obj method input
	 * @return result value
	 */
	public abstract Object[] callObjectInfoGetPropertyIdValues(OAObject obj);
	/**
	 * Dependency hook used by this service to reflectIsReferenceObjectLoadedAndNotEmpty.
	 *
	 * @param oaObj method input
	 * @param propertyName method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callReflectIsReferenceObjectLoadedAndNotEmpty(OAObject oaObj, String propertyName);
	/**
	 * Dependency hook used by this service to reflectGetProperty.
	 *
	 * @param oaObj method input
	 * @param propPath method input
	 * @return result value
	 */
	public abstract Object callReflectGetProperty(OAObject oaObj, String propPath);
	/**
	 * Dependency hook used by this service to threadLocalIsLoading.
	 *
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callThreadLocalIsLoading();
	/**
	 * Dependency hook used by this service to threadLocalGetObjectCacheAddMode.
	 *
	 * @return result value
	 */
	public abstract int callThreadLocalGetObjectCacheAddMode();
	/**
	 * Dependency hook used by this service to isRemoteThread.
	 *
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callIsRemoteThread();
	
}
