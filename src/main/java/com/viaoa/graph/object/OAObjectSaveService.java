package com.viaoa.graph.object;


import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.graph.OAObjectService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEventDelegate;
import com.viaoa.object.*;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.sync.remote.RemoteSessionInterface;

public class OAObjectSaveService {
	private final Logger LOG = Logger.getLogger(OAObjectSaveService.class.getName());

	private final OAObjectService srvcObject;
	private final OAObject.FriendAccess faObject;

	public OAObjectSaveService(OAObjectService srvcObject, OAObject.FriendAccess oaObjectFriendAccess) {
		if (srvcObject == null)
			throw new IllegalArgumentException("OAObjectService can not be null");
		this.srvcObject = srvcObject;
		if (oaObjectFriendAccess == null)
			throw new IllegalArgumentException("OAObjectFriendAccess can not be null");
		this.faObject = oaObjectFriendAccess;
	}

	public OAObjectService getObjectService() {
		return srvcObject;
	}

	protected void save(OAObject oaObj, int iCascadeRule) {
		if (oaObj == null) {
			return;
		}

		if (OAObjectCSDelegate.isWorkstation(oaObj)) {
			OAObjectCSDelegate.save(oaObj, iCascadeRule);
			return;
		}

		OACascade cascade = new OACascade();
		save(oaObj, iCascadeRule, cascade, true, true);
	}

	public void save(OAObject oaObj, int iCascadeRule, OACascade cascade) {
		save(oaObj, iCascadeRule, cascade, false, true);
	}

	private void save(OAObject oaObj, int iCascadeRule, OACascade cascade, boolean bIsFirst, boolean bCheckDepth) {
		if (bCheckDepth && cascade.getDepth() > 50) {
			if (!cascade.wasCascaded(oaObj, false)) {
				cascade.addToOverflow(oaObj); // add to overflow, (tail recursion)
			}
			return;
		}
		if (OAThreadLocalDelegate.isDeleting(oaObj)) {
			return;
		}

		if (cascade.wasCascaded(oaObj, true)) {
			return;
		}
		cascade.depthAdd();

		boolean b = (oaObj.newFlag || oaObj.changedFlag || bIsFirst);
		_save(oaObj, true, iCascadeRule, cascade); // "ONE" relationships
		// cascadeSave() will check hash to see if object has already been checked
		if (b) {
			Hub[] hubs = OAObjectHubDelegate.getHubReferences(oaObj);
			if (hubs != null) {
				for (Hub h : hubs) {
					if (h != null) {
						HubEventDelegate.fireBeforeSaveEvent(h, oaObj);
					}
				}
			}

			for (int i = 0; i < 4; i++) {
				try {
					if (onSave(oaObj)) {
						if (i > 0) {
							String msg = "Retry save successful, class=" + oaObj.getClass().getSimpleName() + ", key="
									+ oaObj.getObjectKey() + ", try=" + (i + 1);
							LOG.log(Level.WARNING, msg);
						}
						break;
					}
				} catch (Exception e) {
					String msg = "error saving, class=" + oaObj.getClass().getSimpleName() + ", key=" + oaObj.getObjectKey() + ", isNew="
							+ oaObj.isNew() + ", try=" + (i + 1) + " of 4";
					if (i == 3) {
						msg += " ALERT: possible data loss";
					}
					LOG.log(Level.WARNING, msg, e);
					oaObj.setChanged(true);
					_save(oaObj, true, iCascadeRule, cascade); // "ONE" relationships
					continue;
				}

				// try again, object might have been changed in the process
				String msg = "onSave returned false, class=" + oaObj.getClass().getSimpleName() + ", key=" + oaObj.getObjectKey()
						+ ", isNew=" + oaObj.isNew() + ", will try again the next time save is called";
				LOG.warning(msg);
				break;
			}

			if (hubs != null) {
				for (Hub h : hubs) {
					if (h != null) {
						HubEventDelegate.fireAfterSaveEvent(h, oaObj);
					}
				}
			}
		}
		_save(oaObj, false, iCascadeRule, cascade); // "MANY" relationships

		cascade.depthSubtract();
		if (cascade.getDepth() < 1) {
			ArrayList<Object> al = cascade.getOverflowList();
			if (al != null) {
				cascade.clearOverflowList();
				cascade.setDepth(0);
				if (al != null) {
					for (Object obj : al) {
						save(((OAObject) obj), iCascadeRule, cascade, false, true);
					}
				}
			}
		}
	}

	/**
	 * Called by HubSaveDelegate.saveAll() to save all New Many2Many added objects.
	 */
	public void _saveObjectOnly(OAObject oaObj, OACascade cascade) {
		_save(oaObj, true, OAObject.CASCADE_NONE, cascade);
		onSave(oaObj);
	}

	/**
	 * Internal method used when saving an objects cascade save references.
	 * <p>
	 * Check all Links with TYPE=MANY and CASCADE=true to either call "save()" or to check if objects can be saved.<br>
	 * This will also check any Link with TYPE=ONE to see if isNew(). If it isNew then it will be saved (but not its links) before this
	 * object can be saved. This is needed since the OADataSource's will require the parent to exist before this object can be saved.
	 * 
	 * @param checkOnly if true then "canSave" is called, else "save()" is called
	 * @return null if all objects can be saved
	 */
	private void _save(OAObject oaObj, boolean bOne, int iCascadeRule, OACascade cascade) {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
		List al = oi.getLinkInfos();
		for (int i = 0; i < al.size(); i++) {
			OALinkInfo li = (OALinkInfo) al.get(i);

			if (bOne != (li.type == OALinkInfo.ONE)) {
				continue;
			}

			if (li.getTransient()) {
				continue;
			}
			if (li.getCalculated()) {
				continue;
			}
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

			if (OAObjectReflectDelegate.isReferenceNullOrNotLoaded(oaObj, prop)) {
				continue;
			}

			boolean bValidCascade = false;
			if (iCascadeRule == OAObject.CASCADE_LINK_RULES && li.cascadeSave) {
				bValidCascade = true;
			} else if (iCascadeRule == OAObject.CASCADE_OWNED_LINKS && li.getOwner()) {
				bValidCascade = true;
			} else if (iCascadeRule == OAObject.CASCADE_ALL_LINKS) {
				bValidCascade = true;
			}

			// Note: if (iCascadeRule == OAObject.CASCADE_NONE) then only save ONE links that are new objects - so ref integrity is maintained.

			if (li.type == OALinkInfo.ONE) {
				Object obj = OAObjectReflectDelegate.getProperty(oaObj, li.getName());
				if ((obj instanceof OAObject)) {
					OAObject oaRef = (OAObject) obj;
					if (oaRef.getNew()) {
						if (cascade.wasCascaded(oaRef, false)) {
							boolean bSave = false;
							synchronized (hmSaveNewLock) {
								for (;;) {
									if (!oaRef.getNew()) {
										break;
									}
									Thread t = hmSaveNewLock.get(Long.valueOf(oaRef.guid));
									if (t == null) {
										hmSaveNewLock.put(Long.valueOf(oaRef.guid), Thread.currentThread());
										bSave = true;
										break;
									}
									if (t == Thread.currentThread()) {
										break;
									}
									try {
										hmSaveNewLock.wait(100);
									} catch (Exception e) {
									}
								}
							}

							if (bSave) {
								// have to save new reference object before oaObj can be saved.
								OAObjectInfo oiRef = OAObjectInfoDelegate.getOAObjectInfo(oaRef.getClass());
								Exception ex = null;
								try {
									OAObjectDSDelegate.saveWithoutReferences(oaRef);
								} catch (Exception e) {
									ex = e;
								}
								OAObjectDelegate.setNew(oaRef, false);
								oaRef.changedFlag = true; // so that it will be save/updated

								synchronized (hmSaveNewLock) {
									hmSaveNewLock.remove(Long.valueOf(oaRef.guid));
									hmSaveNewLock.notifyAll();
								}

								if (ex != null) {
									String msg = "error calling saveWithoutReferences, class=" + oaRef.getClass().getName() + ", key="
											+ oaRef.getObjectKey();
									throw new RuntimeException(msg, ex);
								}
							}
						} else {
							if (bValidCascade) {
								save(oaRef, iCascadeRule, cascade, false, false);
							} else {
								save(oaRef, OAObject.CASCADE_NONE, cascade, false, false);
							}
						}
					} else {
						if (bValidCascade) {
							save(oaRef, iCascadeRule, cascade);
						}
					}
				}
			} else {
				if (iCascadeRule == OAObject.CASCADE_NONE) {
					continue;
				}
				if (bValidCascade) {
					Hub hub = (Hub) OAObjectReflectDelegate.getProperty(oaObj, li.getName()); // get/load "real" objects
					OAObjectHubDelegate.saveAll(hub, iCascadeRule, cascade);
				} else {
					// save all adds/removes from hub.
					Hub hub = (Hub) OAObjectReflectDelegate.getRawReference(oaObj, prop); // could be Hub with OAObjectKey objects
					if (hub.isOAObject()) {
						// update all links even if cascade is false
						OAObjectHubDelegate.saveAll(hub, OAObject.CASCADE_NONE, cascade); // only save M2M link changes, not the actual objects in the Hub.
					}
				}
			}
		}
	}

	private final HashMap<Long, Thread> hmSaveNewLock = new HashMap<Long, Thread>(11);

	/**

	*/
	protected boolean onSave(OAObject oaObj) {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj.getClass());

		//LOG.fine(oaObj.getClass().getSimpleName()+", isNew="+oaObj.isNew());        
		// if new, then need to hold a lock
		boolean bIsNew = oaObj.isNew();
		if (bIsNew) {
			synchronized (hmSaveNewLock) {
				for (int i = 0;; i++) {
					if (!oaObj.isNew()) {
						return true; // already saved
					}
					Thread t = hmSaveNewLock.get(Long.valueOf(oaObj.guid));
					if (t == null) {
						if (i > 0) {
							return true; // already saved
						}
						hmSaveNewLock.put(Long.valueOf(oaObj.guid), Thread.currentThread());
						break;
					}
					try {
						if (t == Thread.currentThread()) {
							return true; // already saving in this thread
						}
						hmSaveNewLock.wait(100);
					} catch (Exception e) {
					}
				}
			}
		}

		/*
		if (oi.getUseDataSource()) {
		    OAObjectKey key = OAObjectKeyDelegate.getKey(oaObj);
		    String s = String.format("Save, class=%s, id=%s",
		            OAString.getClassName(oaObj.getClass()),
		            key.toString()
		    );
		    OAObject.OALOG.fine(s);
		}
		*/

		try {
			// 20130504 moved before actual save, in case another thread makes a change
			oaObj.setDeleted(false); // in case it was deleted, and then re-saved
			oaObj.setChanged(false);

			OAObjectDSDelegate.save(oaObj);
			OAObjectLogDelegate.logToXmlFile(oaObj, true);
			if (bIsNew) {
				OAObjectDelegate.setNew(oaObj, false);
			}
		} finally {
			if (bIsNew) {
				synchronized (hmSaveNewLock) {
					hmSaveNewLock.remove((Object) (Long.valueOf(oaObj.guid))); // needs to use Object instead of primitive
					hmSaveNewLock.notifyAll();
				}
			}
		}
		oaObj.afterSave();
		return true;
	}

}
