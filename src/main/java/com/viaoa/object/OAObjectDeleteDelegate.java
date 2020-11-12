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
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubAddRemoveDelegate;
import com.viaoa.hub.HubCSDelegate;
import com.viaoa.hub.HubDSDelegate;
import com.viaoa.hub.HubDataDelegate;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubEventDelegate;
import com.viaoa.sync.OASync;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.util.OAArray;
import com.viaoa.util.OANotExist;

public class OAObjectDeleteDelegate {
	private static Logger LOG = Logger.getLogger(OAObjectDeleteDelegate.class.getName());

	public static void delete(OAObject oaObj) {
		if (oaObj == null) {
			return;
		}
		boolean b = OAObjectCSDelegate.delete(oaObj);
		if (!b) {
			return; // done on server
		}
		OACascade cascade = new OACascade();
		delete(oaObj, cascade);
	}

	/**
	 * Used to know if an object has been deleted, by calling OAObject.delete().
	 */
	public static void setDeleted(OAObject oaObj, boolean tf) {
		if (oaObj.deletedFlag != tf) {
			boolean bOld = oaObj.deletedFlag;
			OAObjectEventDelegate.fireBeforePropertyChange(	oaObj, OAObjectDelegate.WORD_Deleted,
															bOld ? OAObjectDelegate.TRUE : OAObjectDelegate.FALSE,
															tf ? OAObjectDelegate.TRUE : OAObjectDelegate.FALSE, false, true);
			oaObj.deletedFlag = tf;

			OAObjectEventDelegate.firePropertyChange(	oaObj, OAObjectDelegate.WORD_Deleted,
														bOld ? OAObjectDelegate.TRUE : OAObjectDelegate.FALSE,
														oaObj.deletedFlag ? OAObjectDelegate.TRUE : OAObjectDelegate.FALSE, false, false);
		}
	}

	public static void delete(final OAObject oaObj, OACascade cascade) {
		if (oaObj == null) {
			return;
		}
		if (cascade.wasCascaded(oaObj, true)) {
			return;
		}

		Hub[] hubs = OAObjectHubDelegate.getHubReferences(oaObj);
		if (hubs != null) {
			for (Hub h : hubs) {
				if (h == null) {
					continue;
				}
				/* this can be added later?
				if (!HubEventDelegate.canDelete(h, oaObj)) {
				    throw new RuntimeException("can delete returned false, object can not be deleted.");
				}
				*/
				HubEventDelegate.fireBeforeDeleteEvent(h, oaObj);
			}
		}
		try {
			OAThreadLocalDelegate.setDeleting(oaObj, true);

			OAObjectDeleteDelegate.deleteChildren(oaObj, cascade); // delete children first
			if (!oaObj.getNew()) {
				try {
					OAObjectDeleteDelegate.onDelete(oaObj); // this will delete from OADataSource
				} catch (Exception e) {
					String msg = "error calling delete, class=" + oaObj.getClass().getName() + ", key=" + oaObj.getObjectKey();
					// LOG.log(Level.WARNING, msg, e);
					throw new RuntimeException(msg, e);
				}
			}

			oaObj.setDeleted(true);

			// remove from all hubs
			if (hubs != null) {
				for (Hub h : hubs) {
					if (h != null) {
						HubAddRemoveDelegate.remove(h, oaObj, true, true, true, true, true, false); // force, send, deleting, setAO
					}
				}
			}

			// 20120702 if m2m and private, then need to find any hub that is not in the getHubs()
			OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj.getClass());
			for (OALinkInfo li : oi.getLinkInfos()) {
				if (!li.getPrivateMethod()) {
					continue;
				}
				if (!li.getUsed()) {
					continue;
				}
				if (li.getType() != OALinkInfo.TYPE_MANY) {
					continue;
				}

				final OALinkInfo llRev = li.getReverseLinkInfo();
				if (llRev == null) {
					continue;
				}
				if (llRev.getType() != OALinkInfo.TYPE_MANY) {
					continue;
				}

				OAObjectCacheDelegate.callback(new OACallback() {
					@Override
					public boolean updateObject(Object obj) {
						if (OAObjectReflectDelegate.isReferenceNullOrNotLoadedOrEmptyHub((OAObject) obj, llRev.getName())) {
							return true;
						}
						Object objx = llRev.getValue(obj);
						if (!(objx instanceof Hub)) {
							return true;
						}
						Hub hx = (Hub) objx;
						hx.remove(oaObj);
						return true;
					}
				}, li.getToClass());
			}

			// 20180130
			// M2O where M is private
			for (final OALinkInfo li : oi.getLinkInfos()) {
				if (!li.getPrivateMethod()) {
					continue;
				}
				if (!li.getUsed()) {
					continue;
				}
				if (li.getType() != OALinkInfo.TYPE_MANY) {
					continue;
				}
				final OALinkInfo liRev = li.getReverseLinkInfo();
				if (liRev == null) {
					continue;
				}
				if (liRev.getType() != OALinkInfo.TYPE_ONE) {
					continue;
				}

				OAObjectCacheDelegate.callback(new OACallback() {
					@Override
					public boolean updateObject(Object obj) {
						Object objx = OAObjectPropertyDelegate.getProperty((OAObject) obj, liRev.getName(), false, false);
						if (objx instanceof OAObjectKey) {
							if (!objx.equals(oaObj.getObjectKey())) {
								return true;
							}
							OAObjectPropertyDelegate.removeProperty((OAObject) obj, liRev.getName(), false);
							return true;
						} else {
							if (objx != oaObj) {
								return true;
							}
						}
						((OAObject) obj).setProperty(liRev.getName(), null);
						return true;
					}
				}, li.getToClass());
			}

			oaObj.setChanged(false);
			OAObjectDelegate.setNew(oaObj, true);
		} finally {
			OAThreadLocalDelegate.setDeleting(oaObj, false);
		}
		if (hubs != null) {
			for (Hub h : hubs) {
				if (h != null) {
					HubEventDelegate.fireAfterDeleteEvent(h, oaObj);
				}
			}
		}
	}

	/**
	 * Checks to see if an Object can be deleted. Checks that all child links that have mustBeEmpty are empty. NOTE: this is not called/used
	 * when deleteing an OAObject
	 */
	public static boolean canDelete(OAObject oaObj) {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
		List al = oi.getLinkInfos();
		for (int i = 0; i < al.size(); i++) {
			OALinkInfo li = (OALinkInfo) al.get(i);
			if (!li.getMustBeEmptyForDelete()) {
				continue;
			}
			// if (li.getCalculated()) continue;
			if (li.getPrivateMethod()) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}

			String prop = li.name;
			if (prop == null || prop.length() < 1) {
				continue;
			}
			Object obj = OAObjectReflectDelegate.getProperty(oaObj, prop);
			if (obj == null) {
				continue;
			}

			if (li.getType() == OALinkInfo.ONE) {
				return false;
			} else {
				if (((Hub) obj).getSize() > 0) {
					return false;
				}
			}
		}
		return true;
	}

	public static OALinkInfo[] getMustBeEmptyBeforeDelete(OAObject oaObj) {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
		List al = oi.getLinkInfos();
		OALinkInfo[] lis = null;
		for (int i = 0; i < al.size(); i++) {
			OALinkInfo li = (OALinkInfo) al.get(i);
			if (!li.getMustBeEmptyForDelete()) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}

			String prop = li.name;
			if (prop == null || prop.length() < 1) {
				continue;
			}
			Object obj = OAObjectReflectDelegate.getProperty(oaObj, prop);
			if (obj == null) {
				continue;
			}

			if (li.getType() == OALinkInfo.ONE) {
				lis = (OALinkInfo[]) OAArray.add(OALinkInfo.class, lis, li);
			} else {
				if (((Hub) obj).getSize() > 0) {
					lis = (OALinkInfo[]) OAArray.add(OALinkInfo.class, lis, li);
				}
			}
		}
		return lis;
	}

	/**
	 * Internal method used by delete(oaObj) when deleting an objects cascade delete references.
	 * <p>
	 * Checks to see if all Links with TYPE=MANY and CASCADE can be deleted.<br>
	 * If reference object is not set up to be deleted (cascade delete is false), then it will have the reference to this object set to
	 * null.
	 * <p>
	 * Steps:
	 * <ol>
	 * <li>delete any link objects
	 * <li>if !cascade then remove and save all elements from detailHub. This will take out the reference to this object.
	 * <li>if cascade then call Hub.deleteAll
	 * </ol>
	 */
	private static void deleteChildren(OAObject oaObj, OACascade cascade) {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
		List al = oi.getLinkInfos();
		boolean bIsNew = oaObj.isNew();
		for (int i = 0; i < al.size(); i++) {
			final OALinkInfo li = (OALinkInfo) al.get(i);
			if (li.getCalculated()) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}

			String prop = li.name;
			if (prop == null || prop.length() < 1) {
				continue;
			}

			// 20160120
			if (bIsNew && OAObjectPropertyDelegate.getProperty(oaObj, prop, true, false) == OANotExist.instance) {
				continue;
			}

			final OALinkInfo liRev = OAObjectInfoDelegate.getReverseLinkInfo(li);
			if (liRev == null || !liRev.getUsed()) {
				continue;
			}

			if (li.getType() == OALinkInfo.ONE) {
				if ((li.getOwner() || li.cascadeDelete) && !li.getPrivateMethod()) {
					Object obj = OAObjectReflectDelegate.getProperty(oaObj, prop);
					if (obj instanceof OAObject) {
						delete((OAObject) obj, cascade);
					}
					continue;
				}

				if (liRev.getType() == OALinkInfo.ONE) { // 1to1
					Object obj;
					if (li.getPrivateMethod()) {
						obj = OAObjectReflectDelegate.getReferenceObject(oaObj, li.getName());
					} else {
						obj = OAObjectReflectDelegate.getProperty(oaObj, prop);
					}
					if (obj == null) {
						continue;
					}

					// this object is being deleted, remove its reference from reference object
					if (obj instanceof OAObject) {
						OAObjectReflectDelegate.setProperty((OAObject) obj, liRev.name, null, null);
						OAObjectDSDelegate.removeReference((OAObject) obj, liRev);
						oaObj.removeProperty(li.getName());
					}
					continue;
				}
				// else liRev=Many ..
				if (!li.getPrivateMethod()) {
					continue;
				}

				//  it uses a LinkTable. Need to remove from liRev Hub and remove from link table

				OAObject masterObj;
				Hub hubx = OAObjectHubDelegate.getHub(oaObj, li);
				if (hubx != null) {
					masterObj = HubDelegate.getMasterObject(hubx);
				} else {
					Object objx = OAObjectReflectDelegate.getReferenceObject(oaObj, li.getName());
					if (objx instanceof OAObject) {
						masterObj = (OAObject) objx;
						objx = OAObjectPropertyDelegate.getProperty(masterObj, liRev.getName());
						if (objx instanceof Hub) {
							hubx = (Hub) objx;
						}
					} else {
						masterObj = null;
					}
				}

				if (masterObj != null) {
					OADataSource ds = OADataSource.getDataSource(masterObj.getClass());
					if (ds != null && ds.supportsStorage()) {
						ds.updateMany2ManyLinks(masterObj, null, new OAObject[] { oaObj }, liRev.name);
					}
				}
				if (hubx != null) {
					hubx.remove(oaObj);
					HubDataDelegate.removeFromRemovedList(hubx, oaObj);
				}
				oaObj.removeProperty(li.getName());

				continue;
			}

			// Many
			Object obj;
			if (!li.getPrivateMethod()) {
				obj = OAObjectReflectDelegate.getProperty(oaObj, prop);
			} else {
				//  need to get Hub directly.  Ex: a one2many where the one is used as a lookup and does not have a reference to the many.
				obj = OAObjectReflectDelegate.getReferenceHub(oaObj, prop, null, false, null);
			}

			if (!(obj instanceof Hub)) {
				continue;
			}
			Hub hub = (Hub) obj;
			hub.loadAllData();

			// 20120612 need to remove link table records
			boolean bIsM2m = OAObjectInfoDelegate.isMany2Many(li);

			//20180615
			if (hub.getMasterObject() != oaObj) {
				continue; // ex: hier or calc hub
			}

			if (!li.cascadeDelete && !li.getOwner()) { // remove reference in any object to this object
				if (hub.isOAObject() && hub.getSize() > 0) {
					boolean b;
					if (liRev.getPrivateMethod()) {
						// might have a link table
						OADataSource ds = OADataSource.getDataSource(oaObj.getClass());
						b = (ds != null && ds.supportsStorage());
					} else {
						b = true;
					}

					if (b) {
						int x = hub.getSize();
						for (--x; x >= 0; x--) {
							obj = hub.elementAt(x);
							hub.remove(x); // hub will set property for references master to null.
							if (!bIsM2m) {
								OAObjectDSDelegate.removeReference((OAObject) obj, liRev); // update DB so that fkey violation is not thrown
							}
						}
					} else {
						if (OASync.isServer()) {
							HubCSDelegate.removeAllFromHub(hub);
						}
					}
				}
			} else {
				OAObjectHubDelegate.deleteAll(hub, cascade);
			}
			if (bIsM2m) {
				// 20120612 need to remove link table records
				HubDSDelegate.removeMany2ManyLinks(hub);
			}
		}
	}

	/**
	 * called after beforeDelete() and after all listeners have been called. If this is the server, then it will find the OADataSource to
	 * use and call its "delete(this)"
	 */
	private static void onDelete(OAObject oaObj) {
		if (oaObj == null) {
			return;
		}
		if (OASyncDelegate.isServer(oaObj)) {
			OAObjectLogDelegate.logToXmlFile(oaObj, false);
			OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj.getClass());

			if (oi.getUseDataSource()) {
				OAObjectDSDelegate.delete(oaObj);
			}
		}
		oaObj.afterDelete();
	}
}
