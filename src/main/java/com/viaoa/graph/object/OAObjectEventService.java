package com.viaoa.graph.object;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.graph.HubService;
import com.viaoa.graph.OAGraph;
import com.viaoa.graph.OAGraphImpl;
import com.viaoa.graph.OAObjectService;
import com.viaoa.graph.OASyncService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.object.OACalcInfo;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.OASyncClient;
import com.viaoa.undo.OAUndoManager;
import com.viaoa.undo.OAUndoableEdit;
import com.viaoa.util.OACompare;
import com.viaoa.util.OAConv;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OANotExist;
import com.viaoa.util.OANullObject;
import com.viaoa.util.OAReflect;
import com.viaoa.util.OAString;
import com.viaoa.util.OAThrottle;

public class OAObjectEventService {
	private static final Logger LOG = Logger.getLogger(OAObjectEventService.class.getName());

	private final OAObjectService srvcObject;
	private final OAObject.FriendAccess faObject;
	private final HubService srvcHub;
	private final OASyncService srvcSync;
	
    public OAObjectEventService(OAObjectService srvcObject, OAObject.FriendAccess oaObjectFriendAccess, HubService srvcHub, OASyncService srvcSync) {
    	if (srvcObject == null) throw new IllegalArgumentException("OAObjectService can not be null");
    	this.srvcObject = srvcObject;
    	if (oaObjectFriendAccess == null) throw new IllegalArgumentException("OAObjectFriendAccess can not be null");
    	this.faObject = oaObjectFriendAccess;
    	if (srvcHub == null) throw new IllegalArgumentException("HubService can not be null");
    	this.srvcHub = srvcHub;
    	if (srvcSync == null) throw new IllegalArgumentException("HubSync can not be null");
    	this.srvcSync = srvcSync;
    }
	
    public OAObjectService getObjectService() {
    	return srvcObject;
    }

	/**
	 * Internal reserved property name used to identify change-flag updates
	 * emitted through the event pipeline.
	 */
	private final String WORD_CHANGED = "CHANGED";

	/**
	 * Timestamp used to throttle repeated warning messages so they do not
	 * flood the log when callback or validation errors occur frequently.
	 */
	private volatile long msThrottle;

	/**
	 * Counter used to track how many validation or callback-related errors
	 * have occurred, allowing the logger to include sequence information.
	 */
	private int cntError;

	/**
	 * Entry point for emitting a before-change notification for a property.
	 * Performs initial null checks and equality checks, then delegates to the
	 * internal method that performs full validation and event routing.
	 *
	 * @param oaObj        object whose property is changing
	 * @param propertyName name of the property
	 * @param oldObj       previous value
	 * @param newObj       new value
	 * @param bLocalOnly   if true, suppresses cross-computer sync
	 * @param bSetChanged  if true, allows downstream logic to mark the object as changed
	 */
	public void fireBeforePropertyChange(final OAObject oaObj, final String propertyName,
			Object oldObj, final Object newObj, final boolean bLocalOnly, final boolean bSetChanged) {
		//qqqqqq method was protected
		_fireBeforePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged, false);
	}

	/**
	 * Internal implementation for property pre-change handling.
	 * Validates that the transition is allowed, enforces metadata rules,
	 * prevents illegal recursive relationships, performs unique and ID checks,
	 * and sends hub-level before-change events. Also determines whether
	 * distributed sync should be notified if the object is server-authoritative.
	 *
	 * @param oaObj           object whose property is changing
	 * @param propertyName    name of the property
	 * @param oldObj          previous value
	 * @param newObj          new value
	 * @param bLocalOnly      if true, suppresses cross-computer sync
	 * @param bSetChanged     if true, allows downstream code to mark object as changed
	 * @param bIsCheckingRef  internal flag used when recursively validating reference changes
	 */
	private void _fireBeforePropertyChange(final OAObject oaObj, final String propertyName,
			Object oldObj, final Object newObj, final boolean bLocalOnly, final boolean bSetChanged, final boolean bIsCheckingRef) {

		if (oaObj == null || propertyName == null) {
			return;
		}

		if (oldObj == newObj) {
			return;
		}
		if (oldObj != null && oldObj.equals(newObj)) {
			if (!OAReflect.isPrimitiveClassWrapper(oldObj.getClass())) {
				return;
			}
		}
		final OAGraphImpl og = (OAGraphImpl) (OARuntime.graph(oaObj));

		final boolean bIsLoading = OARuntime.threadLocalService().isLoading();
		if (bIsLoading) {
			if (!srvcObject.getOAObjectHubService().isInHub(oaObj)) { // 20110719: could be in the OAObjectCache.SelectAllHubs
				// no listeners, need to load quick as possible
				if (srvcSync.isServer()) { // 20150604 if client, then it needs to send prop change to server
					return;
				}
				OASyncClient sc = og.getSyncService().getSyncClient(); 
				if (sc != null && !sc.isObjectOnServer(oaObj)) return;
			}
		} else if (!OARuntime.remoteThreadService().isRemoteThread()) {
			// 20180617 validate
			boolean bSkip = false;
			if (propertyName != null) {
				bSkip = srvcObject.WORD_Changed.equalsIgnoreCase(propertyName);
				bSkip = bSkip || srvcObject.WORD_New.equalsIgnoreCase(propertyName);
				bSkip = bSkip || srvcObject.WORD_Deleted.equalsIgnoreCase(propertyName);
			}

			if (!bSkip && !bIsLoading) {
				OAObjectCallback em = srvcObject.getOAObjectCallbackService().getVerifyPropertyChangeObjectCallback(	OAObjectCallback.CHECK_CallbackMethod,
																										oaObj, propertyName, oldObj,
																										newObj);
				if (!em.getAllowed() || em.getThrowable() != null) {
					String msg = em.getResponse();
					if (em.getThrowable() != null) {
						msg = OAString.concat(msg, "Exception: " + em.getThrowable().getMessage(), ", ");
					} else if (OAString.isEmpty(msg)) {
						msg = "Property change not allowed, property=" + propertyName + ", value=" + newObj;
					} else if (!em.getAllowed()) {
						if (msg == null) {
							msg = "";
						} else {
							msg = "Reason: " + msg;
						}
						msg = (oaObj.getClass().getSimpleName()) + "." + propertyName + " change not allowed, value=" + newObj + msg;
					}

					long ms = System.currentTimeMillis();
					++cntError;
					if (ms > msThrottle + 5000) {
						LOG.warning(cntError + ") " + msg + ", will continue without throwing an exception");
						msThrottle = ms;
					}
					/*
					 * 20181018, 20190502 dont throw an exception until there is more confidence.
					 * throw new RuntimeException(msg, em.getThrowable());
					 */
				}
			}
		}

		// check to see if it is actually changed
		if (oldObj != null) {
			if (srvcObject.getOAObjectReflectService().getPrimitiveNull(oaObj, propertyName) || oldObj instanceof OANullObject) {
				oldObj = null;
			}
		}

		// verify that change is permitted
		// verify if recursive link that new parent is allowed
		final OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(oaObj);
		final String propertyU = propertyName.toUpperCase();
		final OALinkInfo linkInfo = srvcObject.getOAObjectInfoService().getLinkInfo(oi, propertyU);
		OALinkInfo toLinkInfo;
		if (linkInfo != null) {
			toLinkInfo = srvcObject.getOAObjectInfoService().getReverseLinkInfo(linkInfo);
		} else {
			toLinkInfo = null;
		}

		// 20211209 check for changes to link/property that affect this object's other property/link
		/* 20250327 qqqqqqqqq removed, dont want to send fkey msgs
		if (!bIsCheckingRef) {
			if (linkInfo != null) {
				for (OAFkeyInfo fki : linkInfo.getFkeyInfos()) {
					_fireBeforePropertyChange(	oaObj, fki.getFromPropertyInfo().getName(),
												oldObj == null ? null : ((OAObject) oldObj).getProperty(fki.getToPropertyInfo().getName()),
												newObj == null ? null : ((OAObject) newObj).getProperty(fki.getToPropertyInfo().getName()),
												bLocalOnly, false, true);
				}
			} else if (OAString.isNotEmpty(propertyName)) {
				for (OALinkInfo li : oi.getLinkInfos()) {
					if (li.getType() != li.TYPE_ONE) {
						continue;
					}
					for (OAFkeyInfo fki : li.getFkeyInfos()) {
						if (fki.getFromPropertyInfo() == null || !propertyName.equalsIgnoreCase(fki.getFromPropertyInfo().getName())) {
							continue;
						}

						OAObjectKey okNew;
						Object obj = srvcObject.getOAObjectPropertyService().getProperty(oaObj, li.getName(), false, true);
						if (obj instanceof OAObject) {
							obj = ((OAObject) obj).getObjectKey();
						}
						if (!(obj instanceof OAObjectKey)) {
							obj = null;
						}
						okNew = srvcObject.getOAObjectKeyService().createChangedObjectKey(	li.getToClass(), (OAObjectKey) obj,
																			fki.getToPropertyInfo().getName(), newObj);
						if (okNew.isEmpty()) {
							okNew = null;
						}
						_fireBeforePropertyChange(	oaObj, li.getName(),
													srvcObject.getOAObjectPropertyService().getProperty(oaObj, li.getName(), false, true),
													okNew,
													bLocalOnly, false, true);
						break;
					}
				}
			}
		}
		*/

		if (toLinkInfo != null && toLinkInfo.getRecursive()) {
			OALinkInfo liRecursive = srvcObject.getOAObjectInfoService().getRecursiveLinkInfo(oi, OALinkInfo.ONE); // ex: "ParentSection"
			if (liRecursive == linkInfo) {
				// parent property changed.  ex: "setParentSection"
				// verify that it can be placed
				if (newObj != null) {
					if (oaObj == newObj) { // object cant be its own parent
						throw new RuntimeException("Can not set the Parent to itself");
					}
					// cant assign a child of this object as the new parent - causes orphaned objects
					Object obj = newObj;
					for (int i=0; i<100; i++) {
						obj = srvcObject.getOAObjectReflectService().getProperty((OAObject) obj, liRecursive.getName());
						if (obj == null) {
							break;
						}
						if (obj == oaObj) {
							srvcObject.getOAObjectReflectService().setProperty(oaObj, linkInfo.getName(), oldObj, null);
							throw new RuntimeException("Can not assign Parent to a Child");// causes orphans
						}
					}
				}
			}
		}

		// 20151205 check to see if owner is being reassigned
		if (linkInfo != null && oldObj != null && newObj != null && !oaObj.isNew() && linkInfo.getType() == OALinkInfo.ONE
				&& !linkInfo.getCalculated()) {
			OALinkInfo revLinkInfo = srvcObject.getOAObjectInfoService().getReverseLinkInfo(linkInfo);
			if (revLinkInfo != null && revLinkInfo.getOwner()) {
				String s = "FYI (no exception), owner is being reassigned, object=" + oaObj.getClass().getSimpleName() + ", property="
						+ propertyName + ", new value=" + newObj;
				RuntimeException e = new RuntimeException(s);
				LOG.log(Level.FINE, s, e);
				// throw e;
			}
		}

		// 20170420 check to see if owner is being reassigned to null
		if (linkInfo != null && oldObj instanceof OAObject && newObj == null && !oaObj.isDeleted() && !oaObj.isNew()
				&& linkInfo.getType() == OALinkInfo.ONE && !linkInfo.getCalculated()) {
			OALinkInfo rev = linkInfo.getReverseLinkInfo();
		    if (rev != null && rev.getOwner()) {		    
    			if (!OARuntime.get().threadLocalService().isDeleting() && og.getSyncService().isServer()) {
    				OAObjectInfo oix = srvcObject.getOAObjectInfoService().getOAObjectInfo((OAObject) oldObj);
    				if (!oix.getLookup() && !oix.getPreSelect()) {
    					cntSetOwnerNull++;
    					if (throttleSetOwnerNull.check()) {
    						String s = "FYI (no exception), reference is being set to null, object=" + oaObj.getClass().getSimpleName()
    								+ ", property=" + propertyName + ", new value=" + newObj + ", old value=" + oldObj;
    						RuntimeException e = new RuntimeException(s);
    						LOG.log(Level.FINE, "cnt=" + (cntSetOwnerNull) + " " + s, e);
    					}
    				}
    			}
		    }
		}

		if (linkInfo == null && !OARuntime.remoteThreadService().isRemoteThread()) {
			OAPropertyInfo propInfo = srvcObject.getOAObjectInfoService().getPropertyInfo(oi, propertyU);
			if (!bIsLoading && propInfo != null && propInfo.getIsSubmit() && newObj != null) {
				if (OAConv.toBoolean(newObj)) {
					OAObjectCallback eq = srvcObject.getOAObjectCallbackService().getAllowSubmitObjectCallback(oaObj);
					if (!eq.getAllowed()) {
						throw new RuntimeException("submit failed, Class="
								+ oaObj.getClass().getSimpleName() + ", message=" + eq.getResponse(), eq.getThrowable());
					}
				}
			}

			if (propInfo != null) {
				if (propInfo.getId() && !srvcObject.getOAObjectDSService().isAssigningId(oaObj)) {
					OAObjectKey okx = srvcObject.getOAObjectKeyService().createChangedObjectKey(oaObj.getClass(), oaObj.getObjectKey(), propertyName, newObj);
					String s = srvcObject.getOAObjectKeyService().verifyKeyChange(oaObj, okx);
					if (s != null) {
						throw new RuntimeException(s);
					}
				}

				if (newObj instanceof OADateTime) { // 20191222
					if (propInfo.getIgnoreTimeZone()) {
						((OADateTime) newObj).setIgnoreTimeZone(true);
					}
				}

				if (propInfo.getUnique() && newObj != null && !propInfo.getId() && !srvcObject.getOAObjectDSService().isAssigningId(oaObj)) {

					if (!bIsLoading) { // 20221219
						// 20180629
						OAObject obj = srvcObject.getOAObjectUniqueService().getUnique(oaObj.getClass(), propertyName, newObj, false);
						if (obj != null && obj != oaObj) {
							throw new RuntimeException("property is unique, and value already assigned to another object. Class="
									+ oaObj.getClass().getSimpleName() + ", property=" + propertyName + ", value=" + newObj);
						}
					}

					/*was:
					OAFilter<OAObject> filter = new OAFilter<OAObject>() {
					    public boolean isUsed(OAObject obj) {
					        Object objx = obj.getProperty(propertyU);
					        if (objx == null) return false;
					        return objx.equals(newObj);
					    }
					};
					OADataSource ds = OADataSource.getDataSource(oaObj.getClass(), filter);

					if (ds != null && (!(ds instanceof OADataSourceObjectCache))) {
					    Iterator it = ds.select(oaObj.getClass(), propertyU+" = ?", new Object[] {newObj}, null, null, null, null, 2, filter, false);
					    try {
					        for ( ;it != null && it.hasNext(); ) {
					            Object objx = it.next();
					            if (objx != oaObj) {
					                throw new RuntimeException("property is unique, and value is assigned to another object.");
					            }
					        }
					    }
					    finally {
					        if (it != null) it.remove();
					    }
					}
					else if (!propInfo.getId()) {
					    Object objLast = null;
					    for (;;) {
					        Object objx = srvcObject.getOAObjectCacheService().findNext(objLast, oaObj.getClass(), propertyU, newObj);
					        if (objx == null) break;
					        if (objx != oaObj) {
					            throw new RuntimeException("property is unique, and value is assigned to another object.");
					        }
					        objLast = objx;
					    }
					}
					*/
				}
			}
		}

		if (!bIsLoading) {
			sendHubBeforePropertyChange(oaObj, propertyName, oldObj, newObj);
		}

		if (!bLocalOnly && !bIsLoading) {
			// 20140314 if it is in newObjectCache (this computer only), then dont send prop changes
		    boolean b = og.getSyncService().isServer();
		    if (!b) {
	            OASyncClient sc = og.getSyncService().getSyncClient(); 
	            b = (sc != null && sc.isObjectOnServer(oaObj));
		    }
            if (b) {
				srvcObject.getOAObjectCSService().fireBeforePropertyChange(oaObj, propertyName, oldObj, newObj);
			}
		}
	}

	/**
	 * Throttling utility that limits the rate at which warnings are logged
	 * when an owner reference is unexpectedly set to null.
	 */
	private final OAThrottle throttleSetOwnerNull = new OAThrottle(500);

	/**
	 * Counter used to track how many times an owner reference has been set
	 * to null in scenarios where such transitions may indicate modeling or
	 * data-integrity issues.
	 */
	private int cntSetOwnerNull;
	
	/**
	 * Public entry point for emitting an after-change property event.
	 * Delegates to the full implementation with unknown-values disabled
	 * and reference-checking disabled.
	 *
	 * @param oaObj        object whose property changed
	 * @param propertyName name of the modified property
	 * @param oldObj       previous value
	 * @param newObj       new value
	 * @param bLocalOnly   if true, suppresses cross-computer sync
	 * @param bSetChanged  if true, allows flagging the object as changed
	 */
	public void firePropertyChange(final OAObject oaObj, final String propertyName, Object oldObj, Object newObj,
			boolean bLocalOnly, boolean bSetChanged) {
		//qqqqq method was protected
		firePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged, false, false);
	}

	/**
	 * Convenience wrapper for emitting a property-change event with an optional
	 * unknown-values flag, delegating to the full implementation.
	 *
	 * @param oaObj          object whose property changed
	 * @param propertyName   name of the property
	 * @param oldObj         previous value
	 * @param newObj         new value
	 * @param bLocalOnly     if true, suppresses cross-computer sync
	 * @param bSetChanged    if true, allows flagging the object as changed
	 * @param bUnknownValues if true, skips some equality and load-state checks
	 */
	public void firePropertyChange(final OAObject oaObj, final String propertyName, Object oldObj, Object newObj,
			boolean bLocalOnly, boolean bSetChanged, boolean bUnknownValues) {
    	//qqqqqqqqqq method was protected
		firePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged, bUnknownValues, false);
	}

	/**
	 * Full implementation of property-change propagation. Applies metadata and
	 * reference rules, updates primitive-null markers, performs ID and unique
	 * validation, updates inverse references, records undo edits, sends hub
	 * before/after events, updates link membership, applies triggers, manages
	 * distributed-sync routing, and sets the object's changed flag when needed.
	 *
	 * @param oaObj           object whose property changed
	 * @param propertyName    name of the property
	 * @param oldObj          previous value
	 * @param newObj          new value
	 * @param bLocalOnly      if true, suppresses cross-computer sync
	 * @param bSetChanged     if true, allows setting the changed flag
	 * @param bUnknownValues  if true, skips some old-value validation
	 * @param bIsCheckingRef  internal flag used during recursive reference updates
	 */
	public void firePropertyChange(final OAObject oaObj, final String propertyName, Object oldObj, Object newObj,
			final boolean bLocalOnly, final boolean bSetChanged, final boolean bUnknownValues, final boolean bIsCheckingRef) {
    	//qqqqqqqqqq method was protected
		if (oaObj == null || propertyName == null) {
			return;
		}

		String propertyU = propertyName.toUpperCase();

		final OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(oaObj);

		if (oldObj != null && !bUnknownValues) {
			if (srvcObject.getOAObjectReflectService().getPrimitiveNull(oaObj, propertyU) || oldObj instanceof OANullObject) {
				oldObj = null;
			}
		}

		//  note: a primitive null can only be set by calling srvcObject.getOAObjectReflectService().setProperty(...)
		if (newObj instanceof OANullObject) {
			newObj = null;
		}

		if (newObj != null || !bUnknownValues) {
			srvcObject.getOAObjectReflectService().setPrimitiveNull(oaObj, propertyU, (newObj == null));
		}

		if (oldObj instanceof OANullObject) {
			oldObj = null;
		}

		final OALinkInfo linkInfo = srvcObject.getOAObjectInfoService().getLinkInfo(oi, propertyU);
		boolean bWasEmpty = false;
		if (!bUnknownValues && linkInfo != null && oldObj == null) {
			// oldObj might never have been loaded before setMethod was called, which will have the oldValue=null -
			//   need to check in oaObj.properties to see what orig value was.
			oldObj = srvcObject.getOAObjectPropertyService().getProperty(oaObj, propertyName, true, true);
			if (oldObj == OANotExist.instance) {
				bWasEmpty = true;
				oldObj = null;
			}
		}

		Object origOldObj = oldObj;
		if (oldObj instanceof OAObjectKey) {
			boolean b = false;
			if (newObj instanceof OAObject) {
				if (srvcObject.getOAObjectKeyService().isForSameOAObject(null, srvcObject.getOAObjectKeyService().getKey((OAObject) newObj), (OAObjectKey) oldObj)) {
					oldObj = newObj;
					b = true;
				}
			}
			if (!b) {
				Object objx = srvcObject.getOAObjectCacheService().get(linkInfo.getToClass(), (OAObjectKey) oldObj);
				if (objx != null) {
					oldObj = objx;
				}
			}
		}

		if (!bUnknownValues) {
			if (oldObj == newObj && !bWasEmpty) {
				return;
			}
			if (oldObj != null && oldObj.equals(newObj)) {
				return;
			}
		}

		OAPropertyInfo propInfo = null;
		OACalcInfo calcInfo = null;
		if (linkInfo == null) {
			propInfo = srvcObject.getOAObjectInfoService().getPropertyInfo(oi, propertyU);
			if (propInfo == null) {
				calcInfo = srvcObject.getOAObjectInfoService().getOACalcInfo(oi, propertyU);
			}
		}

		final boolean bIsLoading = OARuntime.get().threadLocalService().isLoading();

		OAObjectKey origKey;
		if (propInfo != null && propInfo.getId()) {
			origKey = srvcObject.getOAObjectKeyService().createChangedObjectKey(oaObj.getClass(), oaObj.getObjectKey(), propertyName, oldObj); // make sure key uses the prevId, so that it can be found on other computers
			if (!bIsLoading || !oaObj.isNew()) {
				srvcObject.getOAObjectKeyService().afterChangedObjectKeyProperty(oaObj, origKey, true); // this will make sure that it is a valid (unique) value
			}
		} else {
			origKey = srvcObject.getOAObjectKeyService().getKey(oaObj);
		}

		if (linkInfo != null) {
			// must update ref properties before sending events
			// 20110314: need to store nulls, so that it wont go back to server everytime
			if (!bUnknownValues) {
				srvcObject.getOAObjectPropertyService().setPropertyCAS(oaObj, propertyName, newObj, origOldObj, bWasEmpty, false);
			}
		} else {
			// 20130318
			if (propInfo != null && propInfo.isBlob()) {
				srvcObject.getOAObjectPropertyService().setPropertyCAS(oaObj, propertyName, newObj, origOldObj, bWasEmpty, false);
			}
		}

		final boolean bChangeHold = faObject.getChangedFlag(oaObj);
		final boolean bIsChangeProp = WORD_CHANGED.equals(propertyU);
		if (!bIsChangeProp) {
			faObject.setChangedFlag(oaObj, true);
		}

		if (!bIsLoading) {
			if (!bLocalOnly) {
				// prior to 20100406, this was always calling these methods
				OARuntime.remoteThreadService().startNextThread(); // if this is OAClientThread, so that OAClientMessageHandler can continue with next message

				//note: this next method will just return, since fireBeforePropChange is now doing this
				// srvcObject.getOAObjectCSService().fireAfterPropertyChange(oaObj, origKey, propertyName, oldObj, newObj);
			}
		}

		if (!bIsLoading) {
			// 20110603 added support for creating undoable events if oaThreadLocal.createUndoablePropertyChanges=true
			//      default=false, which means that the individual UI components are controlling this
			if (OARuntime.get().threadLocalService().getCreateUndoablePropertyChanges()) {
				if (!bIsChangeProp && OAUndoManager.getUndoManager() != null) {
					OAUndoableEdit ue = OAUndoableEdit.createUndoablePropertyChange(null, oaObj, propertyName, oldObj, newObj,
																					bChangeHold);
					OAUndoManager.add(ue);
				}
			}
		}
		final OAGraphImpl og = (OAGraphImpl) (OARuntime.graph(oaObj));

		// 20151117 if one2one, and new value is null, then set prop to null in link prop
		if (linkInfo != null && !bUnknownValues) {
			OALinkInfo revLinkInfo = srvcObject.getOAObjectInfoService().getReverseLinkInfo(linkInfo);
			if (revLinkInfo != null) {
				if (revLinkInfo.getType() == OALinkInfo.ONE) {
					if (oldObj instanceof OAObjectKey) {
						if (og.getSyncService().isClient()) { // 20151117 dont get from server if this is client
							OAObject objx = srvcObject.getOAObjectCacheService().get(linkInfo.getToClass(), (OAObjectKey) oldObj);
							srvcObject.getOAObjectPropertyService().setPropertyCAS(objx, revLinkInfo.getName(), null, oaObj);
						}
					}
				}
			}
		}

		// Note: this needs to be ran even if isSuppressingEvents(), it wont send messages but it might need to update detail hubs
		if (!bIsLoading) {
			if (srvcObject.getOAObjectHubService().isInHub(oaObj)) {
				sendHubPropertyChange(oaObj, propertyName, oldObj, newObj, linkInfo);
			}
			srvcObject.getOAObjectCacheService().fireAfterPropertyChange(oaObj, origKey, propertyName, oldObj, newObj, bLocalOnly, true);
		}

		faObject.setChangedFlag(oaObj, bChangeHold);

		/*was: moved to below
		// 20160304
		if (!bIsLoading) {
		    if (oi.getHasTriggers()) {
		        HubEvent hubEvent = new HubEvent(oaObj, propertyName, oldObj, newObj);
		        try {
		            OARuntime.get().threadService().addHubEvent(hubEvent);
		            oi.onChange(oaObj, propertyName, hubEvent);
		        }
		        finally {
		            OARuntime.get().threadService().removeHubEvent(hubEvent);
		        }
		    }
		}
		*/

		// set to Changed
		if (!bIsChangeProp && bSetChanged && !bChangeHold && (calcInfo == null) && !bUnknownValues) {
			if (!oaObj.isChanged()) {
				if (linkInfo == null || !linkInfo.getCalculated()) { // 20120429
					try {
						OARuntime.get().threadLocalService().setSuppressCSMessages(true); // the client will setChanged when it gets the propertyChange message
						oaObj.setChanged(true);
					} finally {
						OARuntime.get().threadLocalService().setSuppressCSMessages(false);
					}
				}
			}
		}

		if (linkInfo != null && !bUnknownValues) {
			updateLink(oaObj, oi, linkInfo, oldObj, newObj);
		}

		// 20181126 moved from above
		if (!bIsLoading && !bUnknownValues) {
			if (oi.getHasTriggers()) {
				HubEvent hubEvent = new HubEvent(oaObj, propertyName, oldObj, newObj);
				try {
					OARuntime.get().threadLocalService().addHubEvent(hubEvent);
					oi.onChange(oaObj, propertyName, hubEvent);
				} finally {
					OARuntime.get().threadLocalService().removeHubEvent(hubEvent);
				}
			}
		}

		// check for changes to link/property that affect this object's other property/link
		/*qqqqqqq 20250327 removed: dont want to send fkey properties msgs   
		if (!bIsCheckingRef && !bUnknownValues) {
			if (linkInfo != null) {
				for (OAFkeyInfo fki : linkInfo.getFkeyInfos()) {
					if (fki.getFromPropertyInfo() == null) {
						continue;
					}
					Object oldValue = null;
					if (oldObj instanceof OAObject) {
						oldValue = oldObj == null ? null : ((OAObject) oldObj).getProperty(fki.getToPropertyInfo().getName());
					} else if (oldObj instanceof OAObjectKey) {
						oldValue = srvcObject.getOAObjectKeyService().getProperty(	linkInfo.getToClass(), (OAObjectKey) oldObj,
																	fki.getToPropertyInfo().getName());
					} else {
						oldValue = oldObj;
					}

					Object newValue = null;
					if (newObj instanceof OAObject) {
						newValue = newObj == null ? null : ((OAObject) newObj).getProperty(fki.getToPropertyInfo().getName());
					} else if (newObj instanceof OAObjectKey) {
						newValue = srvcObject.getOAObjectKeyService().getProperty(	linkInfo.getToClass(), (OAObjectKey) newObj,
																	fki.getToPropertyInfo().getName());
					} else {
						newValue = newObj;
					}
					firePropertyChange(	oaObj, fki.getFromPropertyInfo().getName(),
										oldValue,
										newValue,
										bLocalOnly, false, bUnknownValues, true);
				}

			} else {
				for (OALinkInfo li : oi.getLinkInfos()) {
					if (li.getType() != li.TYPE_ONE) {
						continue;
					}
					for (OAFkeyInfo fki : li.getFkeyInfos()) {
						if (fki.getFromPropertyInfo() == null || !propertyName.equalsIgnoreCase(fki.getFromPropertyInfo().getName())) {
							continue;
						}

						OAObjectKey okNew;
						Object obj = srvcObject.getOAObjectPropertyService().getProperty(oaObj, li.getName(), false, true);
						if (obj instanceof OAObject) {
							obj = ((OAObject) obj).getObjectKey();
						}
						if (obj != null && !(obj instanceof OAObjectKey)) {
							obj = null;
						}
						okNew = srvcObject.getOAObjectKeyService().createChangedObjectKey(	li.getToClass(), (OAObjectKey) obj,
																			fki.getToPropertyInfo().getName(), newObj);
						if (okNew.isEmpty()) {
							okNew = null;
						}

						firePropertyChange(	oaObj, li.getName(),
											srvcObject.getOAObjectPropertyService().getProperty(oaObj, li.getName(), false, true),
											okNew,
											bLocalOnly, false, bUnknownValues, true);
					}
				}
			}
		}
		*/

		// 20220917 check if enum/nameValue property changed, and send helper enum properties changeEvent
		// firePropertyChange for other help enumProperties
		if (propInfo != null && propInfo.isNameValue()) {
			for (OAPropertyInfo pi : oi.getPropertyInfos()) {
				if (OACompare.isEqual(pi.getEnumPropertyName(), propInfo.getName(), true)) {
					if (pi.getPrimitive() && pi.getTrackPrimitiveNull()) {
						srvcObject.getOAObjectReflectService().setPrimitiveNull(oaObj, pi.getName(), (newObj == null));
					}
					firePropertyChange(oaObj, pi.getName(), null, null, bLocalOnly, bSetChanged, true, bIsCheckingRef);
				}
			}
		}

	}

	/**
	 * Notifies all hubs referencing the object that a property is about to change,
	 * allowing listeners to process before-change semantics.
	 *
	 * @param oaObj        object whose property will change
	 * @param propertyName property name
	 * @param oldObj       previous value
	 * @param newObj       new value
	 */
	public void sendHubBeforePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj) {
    	//qqqqqqqqqq method was protected
		Hub[] hubs = srvcObject.getOAObjectHubService().getHubReferences(oaObj);
		if (hubs == null) {
			return;
		}
		for (Hub h : hubs) {
			if (h != null) {
				srvcHub.getHubEventService().fireBeforePropertyChange(h, oaObj, propertyName, oldObj, newObj);
			}
		}
	}

	/**
	 * Handles reference-property updates by adjusting membership in reverse-link
	 * hubs, managing ownership relationships, and maintaining recursive link
	 * consistency. Ensures that the object is removed from old hubs and added to
	 * new hubs when required, and updates master/active objects where appropriate.
	 *
	 * @param oaObj     object whose link reference changed
	 * @param oi        metadata for the object's class
	 * @param linkInfo  metadata describing the modified link
	 * @param oldObj    prior reference value (may be OAObjectKey)
	 * @param newObj    new reference value
	 */
	public void sendHubPropertyChange(final OAObject oaObj, final String propertyName, final Object oldObj, final Object newObj,
			final OALinkInfo linkInfo) {
		// Note: don't add this, HubEventDelegate will do it after it updates detail hubs:
		//        if (OAObjectFlagDelegate.isSuppressingPropertyChangeEvents()) return;
		// Note: oldObj could be OAObjectKey

		Hub[] hubs = srvcObject.getOAObjectHubService().getHubReferences(oaObj);
		if (hubs == null) {
			return;
		}
		for (Hub h : hubs) {
			if (h != null) {
				srvcHub.getHubEventService().fireAfterPropertyChange(h, oaObj, propertyName, oldObj, newObj, linkInfo);
			}
		}

		/* 20101218 replaced by HubListenerTree

		// Check to see if a Calculated property is changed.
		/ * how do properties from other link object notify this objects calc objects?
		Answer: when you add a HubListener to Hub, it will create detail hub and
		    listeners and send calcPropertyChange event
		    @see Hub#addHubListener(HubListener hl, String property) {
		this code here will check for property changes within this object and determine
		if it affects a calc property
		* /
		// see if the property change affects a Calc property
		OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(oaObj);
		ArrayList al = oi.getCalcInfos();
		for (int i=0; i < al.size(); i++) {
			OACalcInfo ci = (OACalcInfo) al.get(i);
		    if (ci.getListenerCount() == 0) continue;  // set by HubEventDelegate.addHubListener(..., property) when a calc property is being used and prop changes need to be checked (here).
		    String[] s = ci.properties;
		    for (int j=0; s != null && j < s.length; j++) {
		        if (propertyName.equalsIgnoreCase(s[j])) {
		            for (j=0; j<h.length; j++) {
		            	HubEventDelegate.fireCalcPropertyChange(h[j], oaObj, ci.getName());
		            }
		            break;
		        }
		    }
		}
		*/
	}

	
	/**
	 * Handles reference-property updates by adjusting membership in reverse-link
	 * hubs, managing ownership relationships, and maintaining recursive link
	 * consistency. Ensures that the object is removed from old hubs and added to
	 * new hubs when required, and updates master/active objects where appropriate.
	 *
	 * @param oaObj     object whose link reference changed
	 * @param oi        metadata for the object's class
	 * @param linkInfo  metadata describing the modified link
	 * @param oldObj    prior reference value (may be OAObjectKey)
	 * @param newObj    new reference value
	 */
	private void updateLink(final OAObject oaObj, OAObjectInfo oi, OALinkInfo linkInfo, Object oldObj, Object newObj) {
		// NOTE: oldObj could be OAObjectKey
		// taken out, since it will set OAClientThread.status = STATUS_FinishingAsServer
		//		if (!OAClientDelegate.processIfServer()) return; // only process on server, and send events to clients (even if this is OAThreadClient)

		OALinkInfo revLinkInfo = srvcObject.getOAObjectInfoService().getReverseLinkInfo(linkInfo);
		if (revLinkInfo == null) {
			return;
		}

		Object obj;

		// 20160426 make sure that it has not changed
		obj = srvcObject.getOAObjectPropertyService().getProperty(oaObj, linkInfo.getName());
		if (obj != newObj) {
			return;
		}

		final OAGraphImpl og = (OAGraphImpl) (OARuntime.graph(oaObj));
		
		if (revLinkInfo.getType() == OALinkInfo.ONE) {
			try {
				OAObjectInfo oiRev = srvcObject.getOAObjectInfoService().getOAObjectInfo(linkInfo.getToClass());
				Method m = srvcObject.getOAObjectInfoService().getMethod(oiRev, "get" + revLinkInfo.getName(), 0); // make sure that the method exists
				if (m != null) {
					if (oldObj instanceof OAObjectKey) {
						if (og.getSyncService().isClient()) { // 20151117 dont get from server if this is client
							oldObj = srvcObject.getOAObjectCacheService().get(linkInfo.getToClass(), (OAObjectKey) oldObj);
						} else {
							oldObj = srvcObject.getOAObjectReflectService().getObject(linkInfo.getToClass(), (OAObjectKey) oldObj);
						}
					}
					if (oldObj instanceof OAObject) {
						// 20150820 if one2one, then dont load if null and isClient
						//   this was discovered when deleting an IDL and function/gsmrFunction (1to1) kept going to server for other value
						boolean b = true;
						if (og.getSyncService().isClient()) {
							obj = srvcObject.getOAObjectPropertyService().getProperty((OAObject) oldObj, revLinkInfo.getName());
							if (obj == null) {
								// dont get from server
								b = false;
							}
						}
						if (b) {
							obj = srvcObject.getOAObjectReflectService().getProperty((OAObject) oldObj, revLinkInfo.getName());
							if (obj == oaObj) {
								srvcObject.getOAObjectReflectService().setProperty((OAObject) oldObj, revLinkInfo.getName(), null, null);
							}
						}
					}

					if (newObj instanceof OAObject) {
						// 20170411
						if (revLinkInfo.getOwner()) {
							srvcObject.getOAObjectPropertyService().setPropertyCAS((OAObject) newObj, revLinkInfo.getName(), oaObj, null);
							srvcObject.getOAObjectReflectService().setProperty((OAObject) newObj, revLinkInfo.getName(), oaObj, null);
						} else {
							//was
							obj = srvcObject.getOAObjectReflectService().getProperty((OAObject) newObj, revLinkInfo.getName());
							if (obj != oaObj) {
								srvcObject.getOAObjectReflectService().setProperty((OAObject) newObj, revLinkInfo.getName(), oaObj, null);
							}
						}
					}
				}
			} catch (Exception e) {
			}
			return;
		}

		if (revLinkInfo.getType() != OALinkInfo.MANY) {
			return;
		}

		Hub hub;
		boolean bUpdateHub = false;

		// 20131009 each link now has its own recursive flag
		OALinkInfo liRecursive;
		if (revLinkInfo.getRecursive()) {
			liRecursive = srvcObject.getOAObjectInfoService().getRecursiveLinkInfo(oi, OALinkInfo.ONE); // ex: "ParentSection"
		} else {
			liRecursive = null;
			//was: OALinkInfo liRecursive = srvcObject.getOAObjectInfoService().getRecursiveLinkInfo(oi, OALinkInfo.ONE);  // ex: "ParentSection"
		}

		boolean bOldIsKeyOnly = (oldObj instanceof OAObjectKey);

		// find all Hubs using this as the active object.
		// By changing a reference property, the object could be moved to another hub
		List<Hub> alUpdateHub = null;
		if (oldObj != null || liRecursive != null) {
			Hub[] hubs = srvcObject.getOAObjectHubService().getHubReferences(oaObj);
			if (hubs != null) {
				for (Hub h : hubs) {
					if (h == null) {
						continue;
					}

					// 20120716
					OAFilter<Hub> filter = new OAFilter<Hub>() {
						@Override
						public boolean isUsed(Hub h) {
							return (h.getAO() == oaObj);
						}
					};
					Hub[] hubss = srvcHub.getHubShareService().getAllSharedHubs(h, filter);

					//was:Hub[] hubss = srvcHub.getHubShareService().getAllSharedHubs(h);
					for (int ii = 0; ii < hubss.length; ii++) {
						hub = hubss[ii];
						if (hub.getAO() == oaObj) {
							if (alUpdateHub == null) {
								alUpdateHub = new ArrayList<Hub>();
							}
							alUpdateHub.add(hub);
						}
					}
				}
			}
		}

		/* recursive hub logic
		    See if recursive hub
		    ex:  Section.setCatalog(catalog)  or  Section.setParentSection(section)
		    This: "Section"
		    Changed Prop: "Catalog" or "ParentSection"

		    linkInfo: from Section -> Catalog or ParentSection
		    toLinkInfo: =  from  Catalog or ParentSection -> Sections
		    liRecursive = "ParentSection"
		    Note: all recursive objects all assigned to the same owner object as the root hub.
		        ex: all sections under a Catalog have Catalog assigned to it.
		        This allows for queries to find all sections for a catalog
		        To find all root (top level) sections for a catalog, select sections without a parentSection assigned
		*/
		if (liRecursive != null) { // if recursive
			if (revLinkInfo.getOwner() && linkInfo != liRecursive) {
				// owner property changed.  ex: "Catalog"
				// need to update all recursive objects under this one.  ex: "hubSections.section.catalog = catalog"

				obj = srvcObject.getOAObjectReflectService().getProperty(oaObj, srvcObject.getOAObjectInfoService().getReverseLinkInfo(liRecursive).getName()); // hubSections
				if (!(obj instanceof Hub)) {
					throw new RuntimeException("OAObject.updateLink() method for recursive link not returning a Hub.");
				}
				hub = (Hub) obj;
				for (int i = 0;; i++) {
					obj = hub.elementAt(i); // section
					if (obj == null) {
						break;
					}
					if (srvcObject.getOAObjectReflectService().getProperty((OAObject) obj, linkInfo.getName()) != newObj) {
						srvcObject.getOAObjectReflectService().setProperty((OAObject) obj, linkInfo.getName(), newObj, null); // setCatalog.  This will set all of its recursive children
					}
				}

				obj = srvcObject.getOAObjectReflectService().getProperty(oaObj, liRecursive.getName()); // get parent (section)
				if (obj != null) {
					obj = srvcObject.getOAObjectReflectService().getProperty((OAObject) obj, linkInfo.getName()); // catalog
					if (obj == newObj) {
						newObj = null; // otherwise, this object will be added to the rootHub
					} else {
						// set Parent to null  2003/09/21
						srvcObject.getOAObjectReflectService().setProperty(oaObj, liRecursive.getName(), null, null); // set ParentSection = null
					}
				}
			} else {
				if (liRecursive == linkInfo) {
					// parent property changed.  ex: "setParentSection"

					// verfy that it can be placed
					if (newObj != null) {
						if (oaObj == newObj) { // object cant be its own parent
							if (bOldIsKeyOnly) {
								bOldIsKeyOnly = false;
								oldObj = srvcObject.getOAObjectReflectService().getObject(linkInfo.getToClass(), (OAObjectKey) oldObj);
							}
							srvcObject.getOAObjectReflectService().setProperty(oaObj, linkInfo.getName(), oldObj, null);
							throw new RuntimeException("Can not set the Parent to Itself");
						}
						// cant assign a child of this object as the new parent - causes orphaned objects
						for (obj = newObj;;) {
							obj = srvcObject.getOAObjectReflectService().getProperty((OAObject) obj, liRecursive.getName());
							if (obj == null) {
								break;
							}
							if (obj == oaObj) {
								if (bOldIsKeyOnly) {
									bOldIsKeyOnly = false;
									oldObj = srvcObject.getOAObjectReflectService().getObject(linkInfo.getToClass(), (OAObjectKey) oldObj);
								}
								srvcObject.getOAObjectReflectService().setProperty(oaObj, linkInfo.getName(), oldObj, null);
								throw new RuntimeException("Can not assign Parent to a Child");// causes orphans
							}
						}
					}

					// find owner link
					boolean bOwned = false;
					OALinkInfo linkOwner = srvcObject.getOAObjectInfoService().getLinkToOwner(oi); // link to catalog
					OALinkInfo liRev = null;
					if (linkOwner != null) {
						liRev = srvcObject.getOAObjectInfoService().getReverseLinkInfo(linkOwner);
					}

					if (liRev != null && liRev.getType() == OALinkInfo.MANY) {
						bOwned = true;
						if (newObj == null) { // parentSection = null
							// if being set to null, then add to root hub.
							// if it was removed from old hub, then dont add to root hub
							boolean bAdd = !OARuntime.get().threadLocalService().isDeleting(oaObj);

							if (bAdd && !bOldIsKeyOnly
									&& srvcObject.getOAObjectReflectService().isReferenceHubLoadedAndNotEmpty((OAObject) oldObj, revLinkInfo.getName())) {
								hub = (Hub) srvcObject.getOAObjectReflectService().getProperty((OAObject) oldObj, revLinkInfo.getName()); // Catalog.sections (original hub that this objects belonged to)
								bAdd = hub.contains(oaObj);
							}

							if (bAdd) {
								obj = srvcObject.getOAObjectReflectService().getProperty(oaObj, linkOwner.getName()); // Catalog
								if (obj != null) {
									Object obj2 = srvcObject.getOAObjectReflectService().getProperty((OAObject) obj, liRev.getName()); // catalog.hubSection
									if (!(obj2 instanceof Hub)) {
										throw new RuntimeException(
												"OAObject.updateLink() method for recursive link owner not returning a Hub.");
									}
									hub = (Hub) obj2;
									if (hub.getObject(oaObj) == null) {
										hub.add(oaObj);
									}
								}
							}
						} else {
							// make sure owner is set for this object.  this.catalog = ((Section)newObj).catalog
							obj = srvcObject.getOAObjectReflectService().getProperty((OAObject) newObj, linkOwner.getName());

							if (srvcObject.getOAObjectReflectService().getProperty(oaObj, linkOwner.getName()) != obj) {
								srvcObject.getOAObjectReflectService().setProperty(oaObj, linkOwner.getName(), obj, null); // setCatalog (this will also set child recursive objects)
							}

							if (oldObj == null) {
								// remove from root hub, it is now assigned a parentSection
								obj = srvcObject.getOAObjectReflectService().getProperty(oaObj, linkOwner.getName()); // Catalog
								if (obj != null) {
									obj = srvcObject.getOAObjectReflectService().getProperty((OAObject) obj, liRev.getName()); // catalog.catalogSections
									if (!(obj instanceof Hub)) {
										throw new RuntimeException(
												"OAObject.updateLink() method for recursive link owner not returning a Hub.");
									}
									hub = (Hub) obj; // catalog.catalogSections
									srvcHub.getHubAddRemoveService().remove(hub, oaObj, false, true, false, true, false, false);
								}
							}
						}
					}

					if (!bOwned) {
						Hub h = srvcObject.getOAObjectInfoService().getRootHub(oi);
						if (h != null) {
							if (oldObj == null) {
								// take out of unowned root hubs
								h.remove(oaObj);
							} else if (newObj == null) {
								// add to unowned root hubs
								// if it was removed from old hub, then dont add to root hub
								boolean bAdd = true;
								if (oldObj != null && !bOldIsKeyOnly
										&& srvcObject.getOAObjectReflectService().isReferenceHubLoaded((OAObject) oldObj, revLinkInfo.getName())) {
									hub = (Hub) srvcObject.getOAObjectReflectService().getProperty((OAObject) oldObj, revLinkInfo.getName()); // Catalog.sections (original hub that this objects belonged to)
									bAdd = hub.contains(oaObj);
								}
								if (bAdd && h.getObject(oaObj) == null) {
									h.add(oaObj);
								}
							}
						}
					}
				}
			}
		}
		// end of recursive logic

		// 20230804
		Hub hubRemovedFrom = null;
		
		if (oldObj instanceof OAObject && !bOldIsKeyOnly) {
			try {
				if (srvcObject.getOAObjectCSService().isServer(oaObj)
						|| srvcObject.getOAObjectReflectService().isReferenceHubLoaded((OAObject) oldObj, revLinkInfo.getName())) {
					obj = srvcObject.getOAObjectReflectService().getProperty((OAObject) oldObj, revLinkInfo.getName());
					if (obj instanceof Hub) {
						Hub h = (Hub) obj;
						if (h.contains(oaObj)) {
							srvcHub.getHubAddRemoveService().remove(h, oaObj, false, true, false, true, false, false);
							hubRemovedFrom = h;
						}
					}
				}
			} catch (Exception e) {
			}
		}

		if (newObj != null && newObj instanceof OAObject) {
			try {
				if (srvcObject.getOAObjectCSService().isServer(oaObj)
						|| srvcObject.getOAObjectReflectService().isReferenceHubLoaded((OAObject) newObj, revLinkInfo.getName())) {
					hub = (Hub) srvcObject.getOAObjectReflectService().getProperty((OAObject) newObj, revLinkInfo.getName());

					// 20130630 added autoAttach check
					boolean bAutoAdd = srvcObject.getAutoAdd(oaObj);

					if (bAutoAdd && hub != null) {
						hub.add(oaObj);

						if (oaObj.isNew()) {
							OAObject objMaster = hub.getMasterObject();
							if (objMaster != null) {
								if (!srvcObject.getAutoAdd(objMaster)) {
									// turn off autoAdd for this object
									srvcObject.setAutoAdd(oaObj, false);
								}
							}
						}
					}
				}
			} catch (Exception e) {
				LOG.log(Level.WARNING, "exception while updating link", e);
			}
		}

		// reset Hub activeObjects in shared hubs
		if (alUpdateHub != null) {
			int x = alUpdateHub.size();
			for (int i = 0; i < x; i++) {
				hub = (Hub) alUpdateHub.get(i);
				// 20110805 dont allow adjusting master if hub is not shared, or if it does not have a masterHub
				boolean bAllowAdjustMaster = (newObj != null)
						&& (hub.getSharedHub() != null && srvcHub.getHubDetailService().getHubWithMasterHub(hub) != null);
				
                // 20230804 dont allow master AO change on hub where object was removed
				if (bAllowAdjustMaster && hubRemovedFrom != null && hub.getRealHub() == hubRemovedFrom) {
				    bAllowAdjustMaster = false; 
				}
				
				srvcHub.getHubAOService().setActiveObject(hub, oaObj, bAllowAdjustMaster, false, false); // adjMaster, updateLink, force
				//was: HubAODelegate.setActiveObject(hub, oaObj, (newObj != null), false, false); // adjMaster, updateLink, force
			}
		}
	}
	
	/**
	 * Sends an after-load event to all hubs referencing the object, allowing
	 * listeners to perform initialization once the object has been fully loaded.
	 *
	 * @param oaObj object that has just completed loading
	 */
	public void fireAfterLoadEvent(OAObject oaObj) {
    	//qqqqqqqqqq method was protected
		Hub[] hubs = srvcObject.getOAObjectHubService().getHubReferences(oaObj);
		if (hubs == null) {
			return;
		}
		for (Hub h : hubs) {
			if (h != null) {
				srvcHub.getHubEventService().fireAfterLoadEvent(h, oaObj);
			}
		}
	}

	
	
	
	
	
	
	
	
}
