package com.viaoa.graph.service.object;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.hub.Hub;
import com.viaoa.object.*;

public abstract class OAObjectSaveService {
	private final Logger LOG = Logger.getLogger(OAObjectSaveService.class.getName());

	private final OAObject.FriendAccess faObject;

	public OAObjectSaveService(OAObject.FriendAccess oaObjectFriendAccess) {
		if (oaObjectFriendAccess == null) throw new IllegalArgumentException("OAObjectFriendAccess can not be null");
		this.faObject = oaObjectFriendAccess;
	}

	public void save(OAObject oaObj, int iCascadeRule) {
    	//qqqqqqqqqq method was protected
		if (oaObj == null) {
			return;
		}

		if (callCSIsWorkstation()) {
			callCSSave(oaObj, iCascadeRule);
			return;
		}

		OACascade cascade = new OACascade();
		save(oaObj, iCascadeRule, cascade, true, true);
	}

	public void save(OAObject oaObj, int iCascadeRule, OACascade cascade) {
		save(oaObj, iCascadeRule, cascade, false, true);
	}

	private void save(OAObject oaObj, int iCascadeRule, OACascade cascade, boolean bIsFirst, boolean bCheckDepth) {
		if (callThreadLocalIsDeleting()) {
			return;
		}

		if (cascade.wasCascaded(oaObj, true)) {
			return;
		}

		if (bCheckDepth && cascade.getDepth() > 50) {
			if (!cascade.wasCascaded(oaObj, false)) {
				cascade.addToOverflow(oaObj); // add to overflow, (tail recursion)
			}
			return;
		}
		
		cascade.depthAdd();

		boolean b = (faObject.getNewFlag(oaObj) || faObject.getChangedFlag(oaObj) || bIsFirst);
		_save(oaObj, true, iCascadeRule, cascade); // "ONE" relationships
		// cascadeSave() will check hash to see if object has already been checked
		if (b) {
			Hub<?>[] hubs = callHubGetHubReferences(oaObj);
			if (hubs != null) {
				for (Hub<?> h : hubs) {
					if (h != null) {
						callHubEventFireBeforeSaveEvent((Hub<OAObject>) h, oaObj);
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
						callHubEventFireAfterSaveEvent(h, oaObj);
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
		OAObjectInfo oi = callInfoGetObjectInfo(oaObj);
		List al = oi.getLinkInfos();
		for (int i = 0; i < al.size(); i++) {
			OALinkInfo li = (OALinkInfo) al.get(i);

			if (bOne != (li.getType() == OALinkInfo.ONE)) {
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

			if (callReflectIsReferenceNullOrNotLoaded(oaObj, prop)) {
				continue;
			}

			boolean bValidCascade = false;
			if (iCascadeRule == OAObject.CASCADE_LINK_RULES && li.getCascadeSave()) {
				bValidCascade = true;
			} else if (iCascadeRule == OAObject.CASCADE_OWNED_LINKS && li.getOwner()) {
				bValidCascade = true;
			} else if (iCascadeRule == OAObject.CASCADE_ALL_LINKS) {
				bValidCascade = true;
			}

			// Note: if (iCascadeRule == OAObject.CASCADE_NONE) then only save ONE links that are new objects - so ref integrity is maintained.

			if (li.getType() == OALinkInfo.ONE) {
				Object obj = callReflectGetProperty(oaObj, li.getName());
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
									Thread t = hmSaveNewLock.get(oaRef.getGuid());
									if (t == null) {
										hmSaveNewLock.put(oaRef.getGuid(), Thread.currentThread());
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
								OAObjectInfo oiRef = callInfoGetOAObjectInfo(oaRef.getClass());
								Exception ex = null;
								try {
									callDSSaveWithoutReferences(oaRef);
								} catch (Exception e) {
									ex = e;
								}
								callObjectSetNew(oaRef, false);
								faObject.setChangedFlag(oaRef, true); // so that it will be save/updated

								synchronized (hmSaveNewLock) {
									hmSaveNewLock.remove(oaRef.getGuid());
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
					Hub hub = (Hub) callReflectGetProperty(oaObj, li.getName()); // get/load "real" objects
					callHubSaveAll(hub, iCascadeRule, cascade);
				} else {
					// save all adds/removes from hub.
					Hub hub = (Hub) callReflectGetRawReference(oaObj, prop); // could be Hub with OAObjectKey objects
					// update all links even if cascade is false
					callHubSaveAll(hub, OAObject.CASCADE_NONE, cascade); // only save M2M link changes, not the actual objects in the Hub.
				}
			}
		}
	}

	private final Map<UUID, Thread> hmSaveNewLock = new HashMap(11);

	/**

	*/
	public boolean onSave(OAObject oaObj) {
    	//qqqqqqqqqq method was protected
		OAObjectInfo oi = callInfoGetOAObjectInfo(oaObj.getClass());

		//LOG.fine(oaObj.getClass().getSimpleName()+", isNew="+oaObj.isNew());        
		// if new, then need to hold a lock
		boolean bIsNew = oaObj.isNew();
		if (bIsNew) {
			synchronized (hmSaveNewLock) {
				for (int i = 0;; i++) {
					if (!oaObj.isNew()) {
						return true; // already saved
					}
					Thread t = hmSaveNewLock.get(oaObj.getGuid());
					if (t == null) {
						if (i > 0) {
							return true; // already saved
						}
						hmSaveNewLock.put(oaObj.getGuid(), Thread.currentThread());
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

			callDSSave(oaObj);
			callLogLogToXmlFile(oaObj, true);
			if (bIsNew) {
				callObjectSetNew(oaObj, false);
			}
		} finally {
			if (bIsNew) {
				synchronized (hmSaveNewLock) {
					hmSaveNewLock.remove(oaObj.getGuid()); // needs to use Object instead of primitive
					hmSaveNewLock.notifyAll();
				}
			}
		}
		oaObj.afterSave();
		return true;
	}

	public abstract boolean callCSIsWorkstation(); 
	public abstract boolean callCSSave(OAObject oaObj, int iCascadeRule);
	public abstract Hub<?>[] callHubGetHubReferences(OAObject oaObj); 
	public abstract OAObjectInfo callInfoGetObjectInfo(OAObject obj); 
	public abstract boolean callReflectIsReferenceNullOrNotLoaded(OAObject oaObj, String propertyName);
	public abstract Object callReflectGetProperty(OAObject oaObj, String propPath); 
	public abstract OAObjectInfo callInfoGetOAObjectInfo(Class<?> clazz);
	public abstract void callDSSaveWithoutReferences(OAObject oaObj);
	public abstract void callObjectSetNew(final OAObject oaObj, final boolean b);
	public abstract void callHubSaveAll(Hub<?> hub, int iCascadeRule, OACascade cascade);
	public abstract Object callReflectGetRawReference(OAObject oaObj, String name);
	public abstract void callDSSave(OAObject oaObj); 
	public abstract void callLogLogToXmlFile(OAObject oaObj, boolean bSave);
	public abstract <T extends OAObject> void callHubEventFireBeforeSaveEvent(Hub<T> thisHub, T obj);
	public abstract <T extends OAObject> void callHubEventFireAfterSaveEvent(Hub<T> thisHub, T obj);
	public abstract boolean callThreadLocalIsDeleting();
	
}


