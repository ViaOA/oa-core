/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.object;

import java.util.Arrays;
import java.util.List;
import com.viaoa.datasource.OADataSource;
import com.viaoa.hub.Hub;
import com.viaoa.util.OAConverter;

/**
 * Helper for using OAObjectKey. 
 */
public class OAObjectKeyDelegate {

	public static OAObjectKey createObjectKey(OAObject obj) {
		if (obj == null) return null;
		OAObjectKey key = new OAObjectKey(OAObjectDelegate.getPropertyIdValues(obj), obj.getGuid());
		return key;
	}

	public static OAObjectKey createObjectKey(Object[] ids, long guid) {
		return createObjectKey((OAObjectInfo) null, ids, guid);
	}
	
	public static OAObjectKey createObjectKey(final Class c, final Object ...ids) {
		return createObjectKey(c, 0L, ids);
	}
	
	public static OAObjectKey createObjectKey(final Class<? extends OAObject> c, final long guid, final Object ...ids) {
		if (ids != null && ids.length == 1) {
			if (ids[0] instanceof OAObject) {
				return getObjectKey((OAObject) ids[0]);
			}
		}
		OAObjectInfo oi = c == null ? null : OAObjectInfoDelegate.getObjectInfo(c);
		return createObjectKey(oi, ids, guid);
	}
	
	// main
	public static OAObjectKey createObjectKey(OAObjectInfo oi, Object[] ids, long guid) {
		if (oi != null && ids != null && ids.length > 0) {
			String[] idProperties = oi.idProperties;
			if (idProperties != null && idProperties.length == ids.length) {
				for (int i = 0; i < idProperties.length; i++) {
					if (ids[i] instanceof OAObjectKey) {
						continue;
					}
					else if (!(ids[i] instanceof OAObject)) { // note: OAObjectKey constructor will handle id values that are OAObject
						Class c = OAObjectInfoDelegate.getPropertyClass(oi, idProperties[i]);
						ids[i] = OAConverter.convert(c, ids[i], null);
					}
				}
			}
		}
		return new OAObjectKey(ids, guid);
	}
	
	public static OAObjectKey createObjectKey(Object id) {
		if (id == null) return null;
		if (id instanceof OAObjectKey) return (OAObjectKey) id;
		if (id instanceof OAObject) return createObjectKey((OAObject) id);
		if (id.getClass().isArray()) return createObjectKey((OAObjectInfo) null, (Object[]) id, 0L);
		return createObjectKey((OAObjectInfo) null, new Object[] {id}, 0L);
	}

	public static OAObjectKey createObjectKey(Object... ids) {
		if (ids == null || ids.length == 0) return null;
		return createObjectKey((OAObjectInfo) null, (Object[]) ids, 0L);
	}
	

	/**
	 * Checks two objectKeys to see if they represent the same OAObject.
	 * Either the guids are non-0 and match, or the objectIds are equal.
	 * <p>
	 * Note: OAObjectKey.equals requires guid and objectIds to exactly match.<br>
	 * Note: used by OACompare.compare method.  
	 */
	public static boolean isForSameOAObject(final Class<? extends OAObject> clazz, final OAObjectKey key, final OAObjectKey key2) {
		if (key == null || key2 == null) return false;
		
		if (key.equals(key2)) return true;
		
		long g = key.getGuid();
		long g2 = key2.getGuid();
		Object[] ids = key.getObjectIds(); 
		Object[] ids2 = key2.getObjectIds();
		
		if (g != 0L && g2 != 0L) {
			if (g != g2) return false;
			if (ids == null || ids2 == null) return true;
		}

		if (ids != null && ids2 != null) {
			return Arrays.equals(ids, ids2);	    
		}

		// one could have guid and the other objectIds
		if (clazz != null) {
			if ((g != 0 && ids == null) && (g2 == 0 && ids2 != null)) {
				OAObject obj = OAObjectCacheDelegate.get(clazz, key);
				if (obj == null) return false;
				OAObjectKey okx = obj.getObjectKey();
				return Arrays.equals(okx.getObjectIds(), ids2);	    
			}
			else if ((g == 0 && ids != null) && (g2 != 0 && ids2 == null)) {
				OAObject obj = OAObjectCacheDelegate.get(clazz, key2);
				if (obj == null) return false;
				OAObjectKey okx = obj.getObjectKey();
				return Arrays.equals(okx.getObjectIds(), ids);	    
			}
		}
		return false;
	}
	
	/**
	 * Find existing OAObject for key, either in cache or in datasource.
	 */
	public static <T extends OAObject> OAObject getOAObject(Class<T> c, OAObjectKey key) {
		if (c == null || key == null) return null;
		OAObject obj = OAObjectCacheDelegate.get(c, key);
		if (obj != null) return obj;
		obj = (OAObject) OADataSource.getObject(c, key);
		return obj;
	}
	
	
	public static OAObjectKey getKey(OAObject oaObj) {
		return createObjectKey(oaObj);		
	}
	public static OAObjectKey getObjectKey(OAObject oaObj) {
		return createObjectKey(oaObj);		
	}
	public static long getGuid(OAObject oaObj) {
		if (oaObj == null) return 0;
		return oaObj.getGuid();
	}

	public static OAObjectKey createChangedObjectKey(Class<? extends OAObject> clazz, OAObjectKey objKey, String propertyName, Object newValue) {
		if (clazz == null) {
			return null;
		}

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
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
	 * Called when an object's property Id value is changed.  
	 * This will verify the change, and then update the OAObjectCache.
	 * <p>
	 * Used by OAObjectEventDelegate.firePropertyChange 
	 *  
	 * @param okOld previous object key. (not used)
	 * @param bVerify if true, then verifies that there is not another object with same objectKey.
	 * @throws a runtime exception if change is not permitted.
	 */
	protected static boolean afterChangedObjectKeyProperty(final OAObject oaObj, final OAObjectKey okOrig, boolean bVerify) {
		if (oaObj == null) return false;
		final OAObjectKey okNew = createObjectKey(oaObj);
		
		if (bVerify) {
			if (OAObjectCSDelegate.isRemoteThread()) {
				bVerify = false;
			}
			if (bVerify) {
				if (OAObjectDSDelegate.isAssigningId(oaObj)) {
					bVerify = false;
				} else if (OAThreadLocalDelegate.isLoading()) {
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
		OAObjectCacheDelegate.propertyKeyValueChanged(oaObj);

		// need to recalc keys for all children that have this object as part of their object key
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
		List al = oi.getLinkInfos();
		for (int i = 0; al != null && i < al.size(); i++) {
			OALinkInfo li = (OALinkInfo) al.get(i);
			if (li.getPrivateMethod()) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}
			if (!OAObjectReflectDelegate.isReferenceObjectLoadedAndNotEmpty(oaObj, li.name)) {
				continue;
			}

			String revProp = li.getReverseName();
			if (revProp == null || revProp.length() == 0) {
				continue;
			}
			OAObjectInfo oiRev = OAObjectInfoDelegate.getOAObjectInfo(li.getToClass());

			if (!OAObjectInfoDelegate.isIdProperty(oiRev, revProp)) {
				continue;
			}

			Object obj = OAObjectReflectDelegate.getProperty(oaObj, li.getName());
			if (obj instanceof Hub) {
				Hub h = (Hub) obj;
				if (h.isOAObject()) {
					for (int ii = 0;; ii++) {
						OAObject oa = (OAObject) h.elementAt(ii);
						if (oa == null) {
							break;
						}
						OAObjectCacheDelegate.propertyKeyValueChanged(oa);
					}
				}
			} else if (obj instanceof OAObject) {
				OAObjectCacheDelegate.propertyKeyValueChanged((OAObject) obj);
			}
		}
		return true;
	}

	public static String verifyKeyChange(final OAObject oaObj, OAObjectKey newObjectKey) {
		OAObjectInfo oi = null;
		if (!oaObj.getNew() && !oaObj.getDeleted()) {
			if (oi == null) {
				oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
			}
			if (!OAObjectDSDelegate.allowIdChange(oaObj.getClass())) {
				return ("ID property can not be changed if " + oaObj.getClass().getSimpleName() + " has been saved");
			}
		}

		OAObject objInCache = OAObjectCacheDelegate.get(oaObj.getClass(), newObjectKey);
		if ((objInCache == null || objInCache == oaObj)) {
			if (oi == null) {
				oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
			}
			if (!oi.getLocalOnly() && OAObjectCSDelegate.isWorkstation(oaObj)) {
				// check on server.  If server has same object as this, resolve() will return this object
				objInCache = OAObjectCSDelegate.getServerObject(oaObj.getClass(), newObjectKey);
			}
		}

		if (objInCache != null && objInCache != oaObj && objInCache.getDeleted()) {
			OAObjectCacheDelegate.removeObject((OAObject) objInCache);
			objInCache = null;
		}

		if (objInCache != oaObj) {
			if (objInCache != null) {
				if (OAThreadLocalDelegate.getObjectCacheAddMode() == OAObjectCacheDelegate.NO_DUPS) {
					// id already used

					Object[] ids = newObjectKey.getObjectIds();
					//was: Object[] ids = OAObjectInfoDelegate.getPropertyIdValues(oaObj);

					String s = "";
					for (int i = 0; i < ids.length; i++) {
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
				if (!OAThreadLocalDelegate.isLoading()) {
					// make sure object does not already exist in datasource
					if (oi == null) {
						oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
					}
					if (oi.getUseDataSource()) {
						objInCache = (OAObject) OAObjectDSDelegate.getObject(oi, oaObj.getClass(), newObjectKey);
						if (objInCache != oaObj && objInCache != null) {
							Object[] ids = newObjectKey.getObjectIds();
							// Object[] ids = OAObjectInfoDelegate.getPropertyIdValues(oaObj);
							String s = "";
							for (int i = 0; i < ids.length; i++) {
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

	
	
	public static Object getProperty(final Class<? extends OAObject> clazz, final OAObjectKey objectKey, final String propertyName) {
		if (clazz == null || objectKey == null || propertyName == null) {
			return null;
		}

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
		String[] ids = oi.getKeyProperties();
		if (ids == null || ids.length == 0) {
			return null;
		}

		for (int i = 0; ids != null && i < ids.length; i++) {
			if (propertyName.equalsIgnoreCase(ids[i])) {
			    if (objectKey.getObjectIds().length > i) {
			        return objectKey.getObjectIds()[i];
			    }
			}
		}
		return null;
	}

	
}
