package com.viaoa.oa.service.object;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.callback.OAObjectSerializerCallback;
import com.viaoa.cascade.OACascade;
import com.viaoa.hub.Hub;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.*;
import com.viaoa.serialize.OAObjectSerializer;
import com.viaoa.sync.remote.RemoteSyncInterface;

/*qqqqqqqqqqqqqq
CODEX

#2 — bug
  file/class/method: src/main/java/com/viaoa/graph/service/object/OAObjectSaveService.java:34, save(OAObject,int)
  and src/main/java/com/viaoa/graph/service/object/OAObjectParentService.java:1779, callRemoteSyncAddNewToCache
  exact concern: New objects call srvcSync.getRemoteSync().addNewToCache(oos) before confirming sync-client/server
  role or null remote sync availability.
  why it matters: Saving a new, non-mastered object in single-user/unconfigured runtime can dereference a null
  remote sync service. It also leaks sync replication behavior into local save before the routing decision.
  severity: bug
  minimal fix: Only call addNewToCache when sync is active and the remote sync contract is available, or move this
  behind explicit client/server sync-role handling.
  suggested invariant ID/name: OBJ-SAVE-SYNC-001: local save must not require RemoteSyncInterface
  suggested test coverage: Save a new root object in single-user mode with no sync services; verify local datasource
  path is used and no remote hook is touched.


 #3 — bug
  file/class/method: src/main/java/com/viaoa/graph/service/object/OAObjectSaveService.java:248, recursive save of
  new ONE reference
  exact concern: If callDSSaveWithoutReferences(oaRef) throws, the code still executes callObjectSetNew(oaRef,
  false) before rethrowing.
  why it matters: A failed partial save can leave an unsaved object marked not-new. Later save/delete/identity logic
  can treat it as persisted even though the datasource rejected it.
  severity: bug
  minimal fix: Move callObjectSetNew(oaRef, false) after the exception check, or restore new=true on failure.
  suggested invariant ID/name: OBJ-SAVE-STATE-001: failed saveWithoutReferences preserves new state
  suggested test coverage: Force datasource failure from saveWithoutReferences; assert referenced object remains
  new.

 #4 — invariant risk
  file/class/method: src/main/java/com/viaoa/graph/service/object/OAObjectSaveService.java:348, onSave
  exact concern: setDeleted(false) and setChanged(false) happen before callDSSave(oaObj). The top-level save retry
  path restores changed state, but lower/internal callers can leave the object clean after a failed datasource save.
  why it matters: Persistence failure must not clear dirty/deleted lifecycle state. Otherwise runtime and
  replication can lose pending changes.
  severity: invariant risk
  minimal fix: Clear changed/deleted state only after successful datasource save, or restore prior flags in a catch/
  finally for every caller path.
  suggested invariant ID/name: OBJ-SAVE-STATE-002: failed datasource save preserves dirty lifecycle flags
  suggested test coverage: Exercise both top-level save and internal object-only save with datasource failure.

 #2
  file/class/method: src/main/java/com/viaoa/graph/service/object/OAObjectSaveService.java:137 save(...)

  exact execution path that triggers the bug: save(oaObj, rule, cascade) -> cascade.depthAdd() -> any exception
  before line 191, such as _save(...), before-save listener, onSave(...), or MANY cascade -> method exits without
  cascade.depthSubtract().

  why it is a real correctness risk: the shared OACascade object is left with inflated depth. Any caller reusing
  that cascade can incorrectly hit overflow/depth behavior and skip or defer graph saves.

  severity: invariant bug

  minimal fix: wrap the body after depthAdd() in try/finally and always call depthSubtract().

  suggested test case: pass a shared OACascade, force _save or a before-save listener to throw, then assert cascade
  depth is restored.


#1
  file/class/method: src/main/java/com/viaoa/graph/service/object/OAObjectSaveService.java, save(...)

  exact execution path: save(...) enters retry loop, onSave(oaObj) throws on attempt 1, catch logs, sets changed
  true, calls _save(...), does not continue, then falls through to the “onSave returned false” warning and break.

  why this is still a real correctness bug: a thrown datasource/save exception is converted into a visible warning
  path, then after-save events still fire and MANY cascade save continues. The operation falsely appears completed.

  semantic/invariant violated: failed authoritative save must not fire after-save or continue cascade as though save
  completed.

  minimal additional fix: after catch recovery on attempts < 3, continue; on attempt 3, throw. Also avoid the false-
  return warning after exception paths.

  suggested regression test: force callDSSave to throw on first attempt; assert no after-save event fires, MANY
  cascade does not run, and retry behavior is explicit.



*/

public abstract class OAObjectSaveService {
	private final Logger LOG = Logger.getLogger(OAObjectSaveService.class.getName());

	private final OAObject.FriendAccess faObject;

	public OAObjectSaveService(OAObject.FriendAccess oaObjectFriendAccess) {
		if (oaObjectFriendAccess == null) throw new IllegalArgumentException("OAObjectFriendAccess can not be null");
		this.faObject = oaObjectFriendAccess;
	}

	public void save(OAObject oaObj, int iCascadeRule) {
		if (oaObj == null) {
			return;
		}

		// 20260401 same code that is in OAObjectCSDelegate.save(..)
		final OAObject thisObj = oaObj;		

		if (thisObj.isNew() && !callHubIsInHubWithMaster(thisObj)) {
            OAObjectSerializer<OAObject> oos = new OAObjectSerializer<>(thisObj, false, new OAObjectSerializerCallback() {
                @Override
                public void beforeSerialize(OAObject obj) {
                }
                @Override
                public boolean shouldSerializeReference(OAObject oaObj, String propertyName, Object objRef, boolean bDefault) {
                    if (!bDefault) return false;
                    boolean b = _shouldSerializeReference(oaObj, propertyName, objRef, bDefault);
                    return b;
                }
                
                private boolean _shouldSerializeReference(OAObject oaObj, String propertyName, Object objRef, boolean bDefault) {
                    if (oaObj != thisObj) return false;
                    if (objRef instanceof Hub) return true;
                    if (objRef instanceof OAObject) {
                        if (((OAObject) objRef).isNew()) {
                            if (!callHubIsInHubWithMaster((OAObject)objRef)) return true;                                    
                        }
                    }
                    return false;
                }
            });
            callRemoteSyncAddNewToCache(oos);
    	}
		
		
		if (callCSIsClient(oaObj)) {
			callCSSave(oaObj, iCascadeRule);
			return;
		}

		OACascade cascade = new OACascade();
		save(oaObj, iCascadeRule, cascade, true, true);
	}

	public void save(OAObject oaObj, int iCascadeRule, OACascade cascade) {
		save(oaObj, iCascadeRule, cascade, false, true);
	}

	private <T extends OAObject> void save(final T oaObj, int iCascadeRule, OACascade cascade, boolean bIsFirst, boolean bCheckDepth) {
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
			Hub<T>[] hubs = callHubGetHubReferences(oaObj);
			if (hubs != null) {
				for (Hub<T> h : hubs) {
					if (h != null) {
						callHubEventFireBeforeSaveEvent(h, oaObj);
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
					if (i == 0) _save(oaObj, true, iCascadeRule, cascade); // "ONE" relationships
					
					if (i == 3) throw new RuntimeException("Exception saving", e);
					continue;
				}

				// try again, object might have been changed in the process
				String msg = "onSave returned false, class=" + oaObj.getClass().getSimpleName() + ", key=" + oaObj.getObjectKey()
						+ ", isNew=" + oaObj.isNew() + ", will try again the next time save is called";
				LOG.warning(msg);
				break;
			}

			if (hubs != null) {
				for (Hub<T> h : hubs) {
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
		if (oaObj == null) return;
		OAObjectInfo oi = callInfoGetObjectInfo(oaObj);
		List<OALinkInfo> al = oi.getLinkInfos();
		for (int i = 0; i < al.size(); i++) {
			OALinkInfo li = al.get(i);

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
								try {
									callDSSaveWithoutReferences(oaRef);
									callObjectSetNew(oaRef, false);
									faObject.setChangedFlag(oaRef, true); // so that it will be save/updated
								} catch (Exception e) {
									String msg = "error calling saveWithoutReferences, class=" + oaRef.getClass().getName() + ", key="
											+ oaRef.getObjectKey();
									throw new RuntimeException(msg, e);
								}
								finally {
									synchronized (hmSaveNewLock) {
										hmSaveNewLock.remove(oaRef.getGuid());
										hmSaveNewLock.notifyAll();
									}
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
					Hub<?> hub = (Hub) callReflectGetProperty(oaObj, li.getName()); // get/load "real" objects
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

	private final Map<UUID, Thread> hmSaveNewLock = new HashMap<>(11);

	/**

	*/
	public boolean onSave(OAObject oaObj) {
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

			callDSSave(oaObj);
			oaObj.setChanged(false);
//			callLogLogToXmlFile(oaObj, true);
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

	public abstract boolean callCSIsClient(OAObject oaOjb); 
	public abstract boolean callCSSave(OAObject oaObj, int iCascadeRule);
	public abstract <T extends OAObject> Hub<T>[] callHubGetHubReferences(T oaObj); 
	public abstract OAObjectInfo callInfoGetObjectInfo(OAObject obj); 
	public abstract boolean callReflectIsReferenceNullOrNotLoaded(OAObject oaObj, String propertyName);
	public abstract Object callReflectGetProperty(OAObject oaObj, String propPath); 
	public abstract OAObjectInfo callInfoGetOAObjectInfo(Class<?> clazz);
	public abstract void callDSSaveWithoutReferences(OAObject oaObj);
	public abstract void callObjectSetNew(final OAObject oaObj, final boolean b);
	public abstract void callHubSaveAll(Hub<?> hub, int iCascadeRule, OACascade cascade);
	public abstract Object callReflectGetRawReference(OAObject oaObj, String name);
	public abstract void callDSSave(OAObject oaObj); 
//	public abstract void callLogLogToXmlFile(OAObject oaObj, boolean bSave);
	public abstract <T extends OAObject> void callHubEventFireBeforeSaveEvent(Hub<T> thisHub, T obj);
	public abstract <T extends OAObject> void callHubEventFireAfterSaveEvent(Hub<T> thisHub, T obj);
	public abstract boolean callThreadLocalIsDeleting();

	protected abstract boolean callHubIsInHubWithMaster(OAObject thisObj);
	protected abstract void callRemoteSyncAddNewToCache(OAObjectSerializer<? extends OAObject> oos);

}
