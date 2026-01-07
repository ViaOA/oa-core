package com.viaoa.graph.object;

import java.util.List;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.OASelect;
import com.viaoa.graph.HubService;
import com.viaoa.graph.OAGraph;
import com.viaoa.graph.OAObjectService;
import com.viaoa.graph.OASyncService;
import com.viaoa.hub.Hub;
import com.viaoa.object.OACallback;
import com.viaoa.object.OACascade;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.OASync;
import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.sync.remote.RemoteClientInterface;
import com.viaoa.sync.remote.RemoteServerInterface;
import com.viaoa.sync.remote.RemoteSyncInterface;
import com.viaoa.util.OAArray;
import com.viaoa.util.OANotExist;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAStr;

public class OAObjectDeleteService {
	private static final Logger LOG = Logger.getLogger(OAObjectDeleteService.class.getName());

	private final OAObjectService srvcObject;
	private final OAObject.FriendAccess faObject;
	private final HubService srvcHub;
	private final OASyncService srvcSync;

	public OAObjectDeleteService(OAObjectService srvcObject, OAObject.FriendAccess oaObjectFriendAccess, HubService srvcHub, OASyncService srvcSync) {
		if (srvcObject == null) throw new IllegalArgumentException("OAObjectService can not be null");
		this.srvcObject = srvcObject;
		if (oaObjectFriendAccess == null) throw new IllegalArgumentException("OAObjectFriendAccess can not be null");
		this.faObject = oaObjectFriendAccess;
		if (srvcHub == null) throw new IllegalArgumentException("HubService can not be null");
		this.srvcHub = srvcHub;
		if (srvcSync == null) throw new IllegalArgumentException("OASyncService can not be null");
		this.srvcSync = srvcSync;
	}

	public OAObjectService getObjectService() {
		return srvcObject;
	}

	/**
	 * Deletes the specified object using full delete lifecycle processing.
	 * <p>
	 * If client/server routing allows the delete to run locally, an
	 * {@link OACascade} instance is created and the internal delete
	 * method is invoked.
	 *
	 * @param oaObj the object to delete; ignored if {@code null}
	 */
	public void delete(OAObject oaObj) {
		if (oaObj == null) {
			return;
		}
		boolean b = srvcObject.getOAObjectCSService().delete(oaObj);
		if (!b) {
			return;
		}
		OACascade cascade = new OACascade();
		delete(oaObj, cascade);
	}

	/**
	 * Performs a server-side delete for the specified object. A new
	 * {@link OACascade} instance is created and passed to the internal
	 * delete method.
	 *
	 * @param oaObj the object to delete
	 */
    public void syncServerDelete(OAObject oaObj) {
        OACascade cascade = new OACascade();
        delete(oaObj, cascade);
    }

    /**
     * Performs a client-side delete for objects that exist only within
     * the client's cache. A new {@link OACascade} instance is created
     * and passed to the internal delete method.
     *
     * @param oaObj the object to delete
     */
	public void syncClientDelete(OAObject oaObj) {
        OACascade cascade = new OACascade();
        delete(oaObj, cascade);
	}

	/**
	 * Updates the deleted flag on the specified object and fires the
	 * appropriate before/after property-change events. If the object
	 * is being restored (deleted flag set to {@code false}), its key
	 * integrity is reverified and it is re-added to the cache.
	 *
	 * @param oaObj the object whose deleted flag is updated
	 * @param tf the new deleted flag value
	 * @throws RuntimeException if key verification fails when
	 *                          clearing the deleted flag
	 */
	public void setDeleted(OAObject oaObj, final boolean tf) {
		final boolean bOld = faObject.getDeleteFlag(oaObj);
		if (bOld != tf) {
			srvcObject.getOAObjectEventService().fireBeforePropertyChange(	oaObj, OAObjectService.WORD_Deleted,
															bOld ? OAObjectService.TRUE : OAObjectService.FALSE,
															tf ? OAObjectService.TRUE : OAObjectService.FALSE, false, true);
			faObject.setDeletedFlag(oaObj, tf);

			srvcObject.getOAObjectEventService().firePropertyChange(	oaObj, OAObjectService.WORD_Deleted,
														bOld ? OAObjectService.TRUE : OAObjectService.FALSE,
														tf ? OAObjectService.TRUE : OAObjectService.FALSE, false, false);

			// need to reverify the key to make sure that another one was not created with the same Id
			if (!tf) {
				String s = srvcObject.getOAObjectKeyService().verifyKeyChange(oaObj, oaObj.getObjectKey());
				if (s != null) {
					throw new RuntimeException(s);
				} else {
					// make sure it is in the ObjectCache
					srvcObject.getOAObjectCacheService().add(oaObj, false, false);
				}
			}
		}
	}
	
	/**
	 * Performs the full internal delete lifecycle, including event
	 * dispatch, cascade delete processing, reference cleanup, DataSource
	 * delete, hub removal, and distributed client notification.
	 *
	 * @param oaObj the object to delete
	 * @param cascade the cascade-tracking object used to prevent
	 *                re-entrant deletions
	 */
	public void delete(final OAObject oaObj, OACascade cascade) {
		if (oaObj == null) {
			return;
		}
		if (cascade.wasCascaded(oaObj, true)) {
			return;
		}
		
		final boolean bIsSyncClient = OASync.isClient(oaObj);

		final Hub[] hubs = srvcObject.getOAObjectHubService().getHubReferences(oaObj);
		if (!bIsSyncClient && hubs != null) {
			for (Hub h : hubs) {
				if (h == null) {
					continue;
				}
				srvcHub.getHubEventService().fireBeforeDeleteEvent(h, oaObj);
			}
		}
		try {
			OARuntime.get().threadLocalService().setDeleting(oaObj, true);

			if (!bIsSyncClient) {
			    deleteChildren(oaObj, cascade); // delete children first
			}
			
			if (!oaObj.getNew()) {
				try {
					onDelete(oaObj); // this will delete from OADataSource
				} catch (Exception e) {
					String msg = "error calling delete, class=" + oaObj.getClass().getName() + ", key=" + oaObj.getObjectKey();
					// LOG.log(Level.WARNING, msg, e);
					throw new RuntimeException(msg, e);
				}
			}

			oaObj.setDeleted(true);
			// 20120702 if m2m and private, then need to find any hub that is not in oaobj.getHubs()
			OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(oaObj.getClass());
			
	        // doesn't store hub if M2M&Private: reverse linkInfo does not have a method.
	        //   since this could have a lot of references (ex: VetJobs JobCategory has m2m Jobs)
			if (!bIsSyncClient) {
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
    
    				final OALinkInfo liRev = li.getReverseLinkInfo();
    				if (liRev == null) {
    					continue;
    				}
    				if (liRev.getType() != OALinkInfo.TYPE_MANY) {
    					continue;
    				}
    
                    String spp = liRev.getSelectFromPropertyPath();
                    if (OAStr.isNotEmpty(spp)) {
                        OAPropertyPath pp = new OAPropertyPath(li.getToClass(), spp);
                        pp = pp.getReversePropertyPath();
                        if (pp == null) spp = null;
                        else spp = pp.getPropertyPath();
                    }
                    else {
                        spp = li.getEqualPropertyPath();
                        if (OAStr.isNotEmpty(spp)) {
                            String s = liRev.getEqualPropertyPath();
                            if (OAStr.isNotEmpty(s)) {
                                OAPropertyPath pp = new OAPropertyPath(li.getToClass(), s);
                                pp = pp.getReversePropertyPath();
                                if (pp == null) spp = null;
                                else {
                                    s = pp.getPropertyPath();
                                    spp += "." + s;
                                }
                            }
                            else spp = null;
                        }
                    }
    				
                    if (OAStr.isNotEmpty(spp)) {
                        OAFinder f = new OAFinder(spp) {
                            protected boolean isUsed(OAObject obj) {
                                Object objx = liRev.getValue(obj);
                                if (objx instanceof Hub) {
                                    Hub hx = (Hub) objx;
                                    hx.remove(oaObj);
                                }
                                return false;
                            }
                        };
                        f.setUseOnlyLoadedData(true);
                        f.find(oaObj);
                    }
                    else {
                    	srvcObject.getOAObjectCacheService().callback(new OACallback() {
        					@Override
        					public boolean updateObject(Object obj) {
        						if (srvcObject.getOAObjectReflectService().isReferenceNullOrNotLoadedOrEmptyHub((OAObject) obj, liRev.getName())) {
        							return true;
        						}
        						Object objx = liRev.getValue(obj);
        						if (!(objx instanceof Hub)) {
        							return true;
        						}
        						Hub hx = (Hub) objx;
        						hx.remove(oaObj);
        						return true;
        					}
        				}, li.getToClass());
                    }
    			}

    			// M2M with revLink.private needs to clear Hub
                for (OALinkInfo li : oi.getLinkInfos()) {
                    if (li.getPrivateMethod()) {
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
                    if (liRev.getType() != OALinkInfo.TYPE_MANY) {
                        continue;
                    }
                    if (liRev.getPrivateMethod()) {
                        Hub hubx = (Hub) li.getValue(oaObj);
                        hubx.clear();
                    }
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
    
    				//  use find ... but dont want it to load reference (short curcuit on pp)
    				String spp = liRev.getSelectFromPropertyPath();
    				if (OAStr.isNotEmpty(spp)) {
                        OAPropertyPath pp = new OAPropertyPath(li.getToClass(), spp);
    				    pp = pp.getReversePropertyPath();
    				    if (pp == null) spp = null;
    				    else spp = pp.getPropertyPath();
    				}
    				else {
    				    spp = li.getEqualPropertyPath();
    				    if (OAStr.isNotEmpty(spp)) {
    				        String s = liRev.getEqualPropertyPath();
    	                    if (OAStr.isNotEmpty(s)) {
    	                        OAPropertyPath pp = new OAPropertyPath(li.getToClass(), s);
    	                        pp = pp.getReversePropertyPath();
    	                        if (pp == null) spp = null;
    	                        else {
    	                            s = pp.getPropertyPath();
    	                            spp += "." + s;
    	                        }
    	                    }
    	                    else spp = null;
    				    }
    				}
    				
                    if (OAStr.isNotEmpty(spp)) {
                        OAFinder f = new OAFinder(spp) {
                            protected boolean isUsed(OAObject obj) {
                                Object objx = liRev.getValue(obj);
                                if (objx instanceof OAObjectKey) {
                                    if (!srvcObject.getOAObjectKeyService().isForSameOAObject(null, (OAObjectKey) objx, oaObj.getObjectKey())) {
                                        return false;
                                    }
                                    srvcObject.getOAObjectPropertyService().removeProperty((OAObject) obj, liRev.getName(), false);
                                    return false;
                                } else {
                                    if (objx != oaObj) {
                                        return false;
                                    }
                                }
                                ((OAObject) obj).setProperty(liRev.getName(), null);
                                return false;
                            }
                        };
                        f.setUseOnlyLoadedData(true);
                        f.find(oaObj);
                    }
                    else {
                    	srvcObject.getOAObjectCacheService().callback(new OACallback() {
        					@Override
        					public boolean updateObject(Object obj) {
        						Object objx = srvcObject.getOAObjectPropertyService().getProperty((OAObject) obj, liRev.getName(), false, false);
        						if (objx instanceof OAObjectKey) {
        							if (!objx.equals(oaObj.getObjectKey())) {
        								return true;
        							}
        							srvcObject.getOAObjectPropertyService().removeProperty((OAObject) obj, liRev.getName(), false);
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
    			}
			} 
			
            // remove from all hubs (needs to be after above code)
            if (hubs != null) {
                for (Hub h : hubs) {
                    if (h != null) {
                        srvcHub.getHubAddRemoveService().remove(h, oaObj, true, true, true, true, true, false); // force, send, deleting, setAO
                    }
                }
            }
			
			oaObj.setChanged(false);
			srvcObject.setNew(oaObj, true);
		} finally {
			OARuntime.get().threadLocalService().setDeleting(oaObj, false);
		}

        if (!bIsSyncClient) srvcObject.getOAObjectCSService().sendDeleteToClients(oaObj);
		
		if (hubs != null) {
			for (Hub h : hubs) {
				if (h != null) {
					srvcHub.getHubEventService().fireAfterDeleteEvent(h, oaObj);
				}
			}
		}
		
		OARemoteThreadDelegate.startNextThread();
	}

	/**
	 * Determines whether the specified object can be deleted by checking
	 * all link definitions that require the related collection or reference
	 * to be empty prior to deletion.
	 *
	 * @param oaObj the object being evaluated
	 * @return {@code true} if all required links are empty; otherwise {@code false}
	 */
	public boolean canDelete(OAObject oaObj) {
		OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(oaObj);
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

			String prop = li.getName();
			if (prop == null || prop.length() < 1) {
				continue;
			}
			Object obj = srvcObject.getOAObjectReflectService().getProperty(oaObj, prop);
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

	/**
	 * Returns an array of link definitions that must be empty before the
	 * specified object can be deleted. Only links marked as requiring empty
	 * state and containing non-empty values are included.
	 *
	 * @param oaObj the object being evaluated
	 * @return an array of required-empty link definitions, or {@code null}
	 *         if none exist
	 */
	public OALinkInfo[] getMustBeEmptyBeforeDelete(OAObject oaObj) {
		OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(oaObj);
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

			String prop = li.getName();
			if (prop == null || prop.length() < 1) {
				continue;
			}
			Object obj = srvcObject.getOAObjectReflectService().getProperty(oaObj, prop);
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
	 * Performs cascade-delete processing for all child link relationships
	 * of the specified object. Handles one-to-one, one-to-many, and
	 * many-to-many relationships according to cascade and ownership rules.
	 *
	 * @param oaObj the parent object whose children may be deleted
	 * @param cascade the cascade tracker used to prevent reprocessing
	 */
	private void deleteChildren(OAObject oaObj, OACascade cascade) {
		OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(oaObj);
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

			String prop = li.getName();
			if (prop == null || prop.length() < 1) {
				continue;
			}

			// 20160120
			if (bIsNew && srvcObject.getOAObjectPropertyService().getProperty(oaObj, prop, true, false) == OANotExist.instance) {
				continue;
			}

			final OALinkInfo liRev = srvcObject.getOAObjectInfoService().getReverseLinkInfo(li);
			if (liRev == null || !liRev.getUsed()) {
				continue;
			}

			if (li.getType() == OALinkInfo.ONE) {
				if ((li.getOwner() || li.getCascadeDelete()) && !li.getPrivateMethod()) {
					Object obj = srvcObject.getOAObjectReflectService().getProperty(oaObj, prop);
					if (obj instanceof OAObject) {
						delete((OAObject) obj, cascade);
					}
					continue;
				}

				if (liRev.getType() == OALinkInfo.ONE) { // 1to1
					Object obj;
					if (li.getPrivateMethod()) {
						obj = srvcObject.getOAObjectReflectService().getReferenceObject(oaObj, li.getName());
					} else {
						obj = srvcObject.getOAObjectReflectService().getProperty(oaObj, prop);
					}
					if (obj == null) {
						continue;
					}

					// this object is being deleted, remove its reference from reference object
					if (obj instanceof OAObject) {
						srvcObject.getOAObjectReflectService().setProperty((OAObject) obj, liRev.getName(), null, null);
						srvcObject.getOAObjectDSService().removeReference((OAObject) obj, liRev);
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
				Hub hubx = srvcObject.getOAObjectHubService().getHub(oaObj, li);
				if (hubx != null) {
					masterObj = srvcHub.getMasterObject(hubx);
				} else {
					Object objx = srvcObject.getOAObjectReflectService().getReferenceObject(oaObj, li.getName());
					if (objx instanceof OAObject) {
						masterObj = (OAObject) objx;
						objx = srvcObject.getOAObjectPropertyService().getProperty(masterObj, liRev.getName());
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
						ds.updateMany2ManyLinks(masterObj, null, new OAObject[] { oaObj }, liRev.getName());
					}
				}
				if (hubx != null) {
					hubx.remove(oaObj);
					srvcHub.getHubDataService().removeFromRemovedList(hubx, oaObj);
				}
				oaObj.removeProperty(li.getName());

				continue;
			}

			// Many
			Object obj;
			if (!li.getPrivateMethod()) {
				obj = srvcObject.getOAObjectReflectService().getProperty(oaObj, prop);
			} else {
				//  need to get Hub directly.  Ex: a one2many where the one is used as a lookup and does not have a reference to the many.
				obj = srvcObject.getOAObjectReflectService().getReferenceHub(oaObj, prop, null, false, null);
			}

			if (!(obj instanceof Hub)) {
				continue;
			}
			Hub hub = (Hub) obj;
			hub.loadAllData();

			// 20120612 need to remove link table records
			boolean bIsM2m = srvcObject.getOAObjectInfoService().isMany2Many(li);

			//20180615
			if (hub.getMasterObject() != oaObj) {
				continue; // ex: hier or calc hub
			}

			if (!li.getCascadeDelete() && !li.getOwner()) { // remove reference in any object to this object
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
								srvcObject.getOAObjectDSService().removeReference((OAObject) obj, liRev); // update DB so that fkey violation is not thrown
							}
						}
					} else {
						if (OASync.isServer()) {
							srvcHub.getHubCSService().removeAllFromHub(hub);
						}
					}
				}
			} else {
				srvcObject.getOAObjectHubService().deleteAll(hub, cascade);
			}
			if (bIsM2m) {
				// 20120612 need to remove link table records
				srvcHub.getHubDSService().removeMany2ManyLinks(hub);
			}
		}
	}

	
	
	
	/**
	 * Performs the final delete operations for the specified object. If
	 * running on the server, the object's delete action is passed to the
	 * DataSource and logged. The object's {@code afterDelete()} callback
	 * is then invoked.
	 *
	 * @param oaObj the object being deleted
	 */
	private void onDelete(OAObject oaObj) {
		if (oaObj == null) {
			return;
		}
		if (srvcSync.isServer()) {
			srvcObject.getOAObjectLogService().logToXmlFile(oaObj, false);
			OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(oaObj.getClass());
			srvcObject.getOAObjectDSService().delete(oaObj);
		}
		oaObj.afterDelete();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
