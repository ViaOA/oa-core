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

import java.util.List;

import com.viaoa.hub.Hub;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAConverter;
import com.viaoa.util.OAString;

public class OAObjectKeyDelegate {

	/**
	 * returns the OAObjectKey that uniquely represents this object.
	 */
	public static OAObjectKey getKey(OAObject oaObj) {
		if (oaObj.objectKey == null) {
			oaObj.objectKey = new OAObjectKey(oaObj);
		}
		return oaObj.objectKey;
	}

	public static int getGuid(OAObject oaObj) {
		if (oaObj == null) {
			return -1;
		}
		return oaObj.guid;
	}

	public static OAObjectKey createChangedObjectKey(OAObject oaObj, String propertyName, Object propertyValue) {
		if (propertyName == null || oaObj == null || oaObj.objectKey == null) {
			return null;
		}

		Object[] objsCurrent = oaObj.objectKey.getObjectIds();

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj.getClass());
		String[] ids = oi.getKeyProperties();
		Object[] objsNew = new Object[ids == null ? 0 : ids.length];

		for (int i = 0; ids != null && i < ids.length; i++) {
			if (propertyName.equalsIgnoreCase(ids[i])) {
				objsNew[i] = propertyValue;
			} else {
				if (objsCurrent != null && i < objsCurrent.length) {
					objsNew[i] = objsCurrent[i];
				}
			}
		}

		OAObjectKey ok = new OAObjectKey(objsNew, oaObj.guid, oaObj.newFlag);
		return ok;
	}

	/**
	 * Create a new key, based on property change. Used by OAObjectEventDelegate.
	 */
	public static OAObjectKey getKey(OAObject oaObj, String property, Object propertyValue) {
		OAObjectKey ok;
		if (OAString.isEmpty(property)) {
			ok = new OAObjectKey(new Object[] { propertyValue }, oaObj.guid, oaObj.newFlag);
		} else {
			OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
			String[] idPropertyNames = oi.getIdProperties();
			Object[] idValues = new Object[idPropertyNames == null ? 0 : idPropertyNames.length];

			Object[] origIds = oaObj.objectKey == null ? null : oaObj.objectKey.getObjectIds();

			for (int i = 0; idPropertyNames != null && i < idPropertyNames.length; i++) {
				if (property.equalsIgnoreCase(idPropertyNames[i])) {
					idValues[i] = propertyValue;
				} else {
					if (origIds != null && i < origIds.length) {
						idValues[i] = origIds[i];
					}
				}
			}
			ok = new OAObjectKey(idValues, oaObj.guid, oaObj.newFlag);
		}
		return ok;
	}

	protected static void setKey(OAObject oaObj, OAObjectKey key) {
		oaObj.objectKey = key;
	}

	/**
	 * Used to update Hubs and HubController when an objects unique values (property Id) are changed.<br>
	 *
	 * @return true if HubController is updated
	 * @see OAObjectKey
	 */
	protected static boolean updateKey(OAObject oaObj, boolean bVerify) {
		if (oaObj.objectKey == null) {
			getKey(oaObj);
			return false;
		}

		// replace old key
		OAObjectKey oldKey = oaObj.objectKey;
		oaObj.objectKey = new OAObjectKey(oaObj);
		if (oaObj.objectKey.exactEquals(oldKey)) { // no change
			oaObj.objectKey = oldKey;
			return false;
		}

		if (bVerify) {
			// if change is coming from the server, then it has already been verified
			if (OAObjectCSDelegate.isRemoteThread()) {
				bVerify = false;
			}
		}

		// 20090906 dont need to verify if database/etc has assigned it
		if (bVerify) {
			if (OAObjectDSDelegate.isAssigningId(oaObj)) {
				bVerify = false;
			} else if (OAThreadLocalDelegate.isLoading()) {
				bVerify = false;
			}
		}

		// make sure objectId is unique.  Check in Cache, on Server, in Database
		if (bVerify) {
			String s = verifyKeyChange(oaObj, oaObj.objectKey);
			if (s != null) {
				oaObj.objectKey = oldKey;
				throw new RuntimeException(s);
			}
		}

		// START Rehashing Key ========================
		OAObjectDelegate.rehash(oaObj, oldKey);
		// END Rehashing Key ==========================

		// need to recalc keys for all children that have this object as part of their object key
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
		List al = oi.getLinkInfos();
		for (int i = 0; i < al.size(); i++) {
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
						updateKey(oa, false);
					}
				}
			} else if (obj instanceof OAObject) {
				updateKey((OAObject) obj, false);
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
			if (oi.getUseDataSource()) {
				if (!OAObjectDSDelegate.allowIdChange(oaObj.getClass())) {
					return ("ID property can not be changed if " + oaObj.getClass().getSimpleName() + " has been saved");
				}
			}
		}

		Object objInCache = OAObjectCacheDelegate.get(oaObj.getClass(), newObjectKey);
		if ((objInCache == null || objInCache == oaObj)) {
			if (oi == null) {
				oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
			}
			if (!oi.getLocalOnly() && OAObjectCSDelegate.isWorkstation(oaObj)) {
				// check on server.  If server has same object as this, resolve() will return this object
				objInCache = OAObjectCSDelegate.getServerObject(oaObj.getClass(), newObjectKey);
			}
		}

		if (objInCache instanceof OAObject && objInCache != oaObj && ((OAObject) objInCache).getDeleted()) {
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
						objInCache = OAObjectDSDelegate.getObject(oaObj);
						if (objInCache != oaObj && objInCache != null) {
							Object[] ids = newObjectKey.getObjectIds();
							// Object[] ids = OAObjectInfoDelegate.getPropertyIdValues(oaObj);
							String s = "";
							for (int i = 0; i < ids.length; i++) {
								if (i > 0) {
									s += " ";
								}
								s += s + ids[i];
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
	 * Convert a value to an OAObjectKey
	 */
	public static OAObjectKey convertToObjectKey(OAObjectInfo oi, Object value) {
		if (oi == null || value == null) {
			return null;
		}
		if (value instanceof OAObjectKey) {
			return (OAObjectKey) value;
		}

		String[] ids = oi.idProperties;
		if (ids != null && ids.length > 0) {
			Class c = OAObjectInfoDelegate.getPropertyClass(oi, ids[0]);
			value = OAConverter.convert(c, value, null);
		} else if (OAObject.class.isAssignableFrom(oi.getForClass())) {
			// 20120729 oaObject without pkey property, which will only use guid for key
			int guid = OAConv.toInt(value);
			OAObjectKey key = new OAObjectKey();
			key.guid = guid;
			return key;
		}
		return new OAObjectKey(value);
	}

	/*was:
		public static OAObjectKey convertToObjectKey(OAObjectInfo oi, Object value) {
	    if (oi == null || value == null) return null;
	    if (value instanceof OAObjectKey) return (OAObjectKey) value;

	    String[] ids = oi.idProperties;
	    if (ids != null && ids.length > 0) {
	        Class c = OAObjectInfoDelegate.getPropertyClass(oi, ids[0]);
	        value = OAConverter.convert(c, value, null);
	    }
	    return new OAObjectKey(value);
	}
	*/
	public static OAObjectKey convertToObjectKey(Class clazz, Object value) {
		if (clazz == null || value == null) {
			return null;
		}
		if (value instanceof OAObjectKey) {
			return (OAObjectKey) value;
		}
		if (value instanceof OAObject) {
			return ((OAObject) value).getObjectKey();
		}
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
		return convertToObjectKey(oi, value);
	}

	public static OAObjectKey convertToObjectKey(Class clazz, Object[] values) {
		if (clazz == null || values == null) {
			return null;
		}

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
		String[] ids = oi.idProperties;
		for (int i = 0; ids != null && i < ids.length; i++) {
			Class c = OAObjectInfoDelegate.getPropertyClass(clazz, ids[0]);
			values[i] = OAConverter.convert(c, values[i], null);
		}
		return new OAObjectKey(values);
	}
}
