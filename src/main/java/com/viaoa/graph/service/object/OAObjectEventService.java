package com.viaoa.graph.service.object;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.annotation.OAParentProvided;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.object.OACalcInfo;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAPropertyInfo;
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

public abstract class OAObjectEventService {
	private static final Logger LOG = Logger.getLogger(OAObjectEventService.class.getName());

	private final OAObject.FriendAccess faObject;

	
    public OAObjectEventService(OAObject.FriendAccess faObject) {
    	if (faObject == null) throw new IllegalArgumentException("OAObject.FriendAccess can not be null");
    	this.faObject = faObject;
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


		final boolean bIsLoading = callThreadLocalIsLoading();
		if (bIsLoading) {
			if (!callHubIsInHub(oaObj)) { // 20110719: could be in the OAObjectCache.SelectAllHubs
				// no listeners, need to load quick as possible
				if (callSyncIsServer()) { // 20150604 if client, then it needs to send prop change to server
					return;
				}
				if (!callSyncIsObjectOnServer(oaObj)) return;
/*qqqqqqqqqqqq was:				
				OASyncClient sc = og.getSyncService().getSyncClient(); 
				if (sc != null && !sc.isObjectOnServer(oaObj)) return;
*/				
			}
		} else if (!callRemoteThreadIsRemoteThread()) {
			// 20180617 validate
			boolean bSkip = false;
			if (propertyName != null) {
				bSkip = OAObjectService.WORD_Changed.equalsIgnoreCase(propertyName);
				bSkip = bSkip || OAObjectService.WORD_New.equalsIgnoreCase(propertyName);
				bSkip = bSkip || OAObjectService.WORD_Deleted.equalsIgnoreCase(propertyName);
			}

			if (!bSkip && !bIsLoading) {
				OAObjectCallback em = callCallbackGetVerifyPropertyChangeObjectCallback(	OAObjectCallback.CHECK_CallbackMethod,
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
			if (callReflectGetPrimitiveNull(oaObj, propertyName) || oldObj instanceof OANullObject) {
				oldObj = null;
			}
		}

		// verify that change is permitted
		// verify if recursive link that new parent is allowed
		final OAObjectInfo oi = callInfoGetObjectInfo(oaObj.getClass());
		final String propertyU = propertyName.toUpperCase();
		final OALinkInfo linkInfo = callInfoGetLinkInfo(oi, propertyU);
		OALinkInfo toLinkInfo;
		if (linkInfo != null) {
			toLinkInfo = callInfoGetReverseLinkInfo(linkInfo);
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
			OALinkInfo liRecursive = callInfoGetRecursiveLinkInfo(oi, OALinkInfo.ONE); // ex: "ParentSection"
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
						
						obj = callReflectGetProperty((OAObject) obj, liRecursive.getName());
						if (obj == null) {
							break;
						}
						if (obj == oaObj) {
							callReflectSetProperty(oaObj, linkInfo.getName(), oldObj, null);
							throw new RuntimeException("Can not assign Parent to a Child");// causes orphans
						}
					}
				}
			}
		}

		// 20151205 check to see if owner is being reassigned
		if (linkInfo != null && oldObj != null && newObj != null && !oaObj.isNew() && linkInfo.getType() == OALinkInfo.ONE
				&& !linkInfo.getCalculated()) {
			OALinkInfo revLinkInfo = callInfoGetReverseLinkInfo(linkInfo);
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
    			if (!callThreadLocalIsDeleting() && callSyncIsServer()) {
    				OAObjectInfo oix = callInfoGetObjectInfo(oldObj.getClass());
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

		if (linkInfo == null && !callRemoteThreadIsRemoteThread()) {
			OAPropertyInfo propInfo = callInfoGetPropertyInfo(oi, propertyU);
			if (!bIsLoading && propInfo != null && propInfo.getIsSubmit() && newObj != null) {
				if (OAConv.toBoolean(newObj)) {
					OAObjectCallback eq = callCallbackGetAllowSubmitObjectCallback(oaObj);
					if (!eq.getAllowed()) {
						throw new RuntimeException("submit failed, Class="
								+ oaObj.getClass().getSimpleName() + ", message=" + eq.getResponse(), eq.getThrowable());
					}
				}
			}

			if (propInfo != null) {
				if (propInfo.getId() && !callDSIsAssigningId(oaObj)) {
					OAObjectKey okx = callKeyCreateChangedObjectKey(oaObj.getClass(), oaObj.getObjectKey(), propertyName, newObj);
					String s = callKeyVerifyKeyChange(oaObj, okx);
					if (s != null) {
						throw new RuntimeException(s);
					}
				}

				if (newObj instanceof OADateTime) { // 20191222
					if (propInfo.getIgnoreTimeZone()) {
						((OADateTime) newObj).setIgnoreTimeZone(true);
					}
				}

				if (propInfo.getUnique() && newObj != null && !propInfo.getId() && !callDSIsAssigningId(oaObj)) {

					if (!bIsLoading) { // 20221219
						// 20180629
						OAObject obj = callUniqueGetUnique(oaObj.getClass(), propertyName, newObj, false);
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
		    boolean b = callSyncIsServer();
		    if (!b) {
	            b = (callSyncIsObjectOnServer(oaObj));
		    }
            if (b) {
            	callCSFireBeforePropertyChange(oaObj, propertyName, oldObj, newObj);
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

		final OAObjectInfo oi = callInfoGetObjectInfo(oaObj.getClass());

		if (oldObj != null && !bUnknownValues) {
			if (callReflectGetPrimitiveNull(oaObj, propertyU) || oldObj instanceof OANullObject) {
				oldObj = null;
			}
		}

		//  note: a primitive null can only be set by calling srvcObject.getOAObjectReflectService().setProperty(...)
		if (newObj instanceof OANullObject) {
			newObj = null;
		}

		if (newObj != null || !bUnknownValues) {
			callReflectSetPrimitiveNull(oaObj, propertyU, (newObj == null));
		}

		if (oldObj instanceof OANullObject) {
			oldObj = null;
		}

		final OALinkInfo linkInfo = callInfoGetLinkInfo(oi, propertyU);
		boolean bWasEmpty = false;
		if (!bUnknownValues && linkInfo != null && oldObj == null) {
			// oldObj might never have been loaded before setMethod was called, which will have the oldValue=null -
			//   need to check in oaObj.properties to see what orig value was.
			oldObj = callPropertyGetProperty(oaObj, propertyName, true, true);
			if (oldObj == OANotExist.instance) {
				bWasEmpty = true;
				oldObj = null;
			}
		}

		Object origOldObj = oldObj;
		if (oldObj instanceof OAObjectKey) {
			boolean b = false;
			if (newObj instanceof OAObject) {
				if (callKeyIsForSameOAObject(null, callKeyGetKey((OAObject) newObj), (OAObjectKey) oldObj)) {
					oldObj = newObj;
					b = true;
				}
			}
			if (!b) {
				Object objx = callCacheGet(linkInfo.getToClass(), (OAObjectKey) oldObj);
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
			propInfo = callInfoGetPropertyInfo(oi, propertyU);
			if (propInfo == null) {
				calcInfo = callInfoGetCalcInfo(oi, propertyU);
			}
		}

		final boolean bIsLoading = callThreadLocalIsLoading();

		OAObjectKey origKey;
		if (propInfo != null && propInfo.getId()) {
			
			origKey = callKeyCreateChangedObjectKey(oaObj.getClass(), oaObj.getObjectKey(), propertyName, oldObj); // make sure key uses the prevId, so that it can be found on other computers
			if (!bIsLoading || !oaObj.isNew()) {
				callKeyAfterChangedObjectKeyProperty(oaObj, origKey, true); // this will make sure that it is a valid (unique) value
			}
		} else {
			origKey = callKeyGetKey(oaObj);
		}

		if (linkInfo != null) {
			// must update ref properties before sending events
			// 20110314: need to store nulls, so that it wont go back to server everytime
			if (!bUnknownValues) {
				callPropertySetPropertyCAS(oaObj, propertyName, newObj, origOldObj, bWasEmpty, false);
			}
		} else {
			// 20130318
			if (propInfo != null && propInfo.isBlob()) {
				callPropertySetPropertyCAS(oaObj, propertyName, newObj, origOldObj, bWasEmpty, false);
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
				callRemoteThreadStartNextThread(); // if this is OAClientThread, so that OAClientMessageHandler can continue with next message

				//note: this next method will just return, since fireBeforePropChange is now doing this
				// srvcObject.getOAObjectCSService().fireAfterPropertyChange(oaObj, origKey, propertyName, oldObj, newObj);
			}
		}

		if (!bIsLoading) {
			// 20110603 added support for creating undoable events if oaThreadLocal.createUndoablePropertyChanges=true
			//      default=false, which means that the individual UI components are controlling this
			if (callThreadLocalGetCreateUndoablePropertyChanges()) {
				if (!bIsChangeProp && OAUndoManager.getUndoManager() != null) {
					OAUndoableEdit ue = OAUndoableEdit.createUndoablePropertyChange(null, oaObj, propertyName, oldObj, newObj,
																					bChangeHold);
					OAUndoManager.add(ue);
				}
			}
		}

		// 20151117 if one2one, and new value is null, then set prop to null in link prop
		if (linkInfo != null && !bUnknownValues) {
			OALinkInfo revLinkInfo = callInfoGetReverseLinkInfo(linkInfo);
			if (revLinkInfo != null) {
				if (revLinkInfo.getType() == OALinkInfo.ONE) {
					if (oldObj instanceof OAObjectKey) {
						if (callSyncIsClient()) { // 20151117 dont get from server if this is client
							OAObject objx = callCacheGet(linkInfo.getToClass(), (OAObjectKey) oldObj);
							callPropertySetPropertyCAS(objx, revLinkInfo.getName(), null, oaObj);
						}
					}
				}
			}
		}

		// Note: this needs to be ran even if isSuppressingEvents(), it wont send messages but it might need to update detail hubs
		if (!bIsLoading) {
			if (callHubIsInHub(oaObj)) {
				sendHubPropertyChange(oaObj, propertyName, oldObj, newObj, linkInfo);
			}
			callCacheFireAfterPropertyChange(oaObj, origKey, propertyName, oldObj, newObj, bLocalOnly, true);
		}

		faObject.setChangedFlag(oaObj, bChangeHold);

		/*was: moved to below
		// 20160304
		if (!bIsLoading) {
		    if (oi.getHasTriggers()) {
		        HubEvent hubEvent = new HubEvent(oaObj, propertyName, oldObj, newObj);
		        try {
		            OARuntime.threadService().addHubEvent(hubEvent);
		            oi.onChange(oaObj, propertyName, hubEvent);
		        }
		        finally {
		            OARuntime.threadService().removeHubEvent(hubEvent);
		        }
		    }
		}
		*/

		// set to Changed
		if (!bIsChangeProp && bSetChanged && !bChangeHold && (calcInfo == null) && !bUnknownValues) {
			if (!oaObj.isChanged()) {
				if (linkInfo == null || !linkInfo.getCalculated()) { // 20120429
					try {
						callThreadLocalSetSuppressCSMessages(true); // the client will setChanged when it gets the propertyChange message
						oaObj.setChanged(true);
					} finally {
						callThreadLocalSetSuppressCSMessages(false);
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
					callThreadLocalAddHubEvent(hubEvent);
					oi.onChange(oaObj, propertyName, hubEvent);
				} finally {
					callThreadLocalRemoveHubEvent(hubEvent);
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
						callReflectSetPrimitiveNull(oaObj, pi.getName(), (newObj == null));
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
		Hub[] hubs = callHubGetHubReferences(oaObj);
		if (hubs == null) {
			return;
		}
		for (Hub h : hubs) {
			if (h != null) {
				callHubEventFireBeforePropertyChange(h, oaObj, propertyName, oldObj, newObj);
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

		Hub[] hubs = callHubGetHubReferences(oaObj);
		if (hubs == null) {
			return;
		}
		for (Hub h : hubs) {
			if (h != null) {
				callHubEventFireAfterPropertyChange(h, oaObj, propertyName, oldObj, newObj, linkInfo);
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

		OALinkInfo revLinkInfo = callInfoGetReverseLinkInfo(linkInfo);
		if (revLinkInfo == null) {
			return;
		}

		Object obj;

		// 20160426 make sure that it has not changed
		obj = callPropertyGetProperty(oaObj, linkInfo.getName());
		if (obj != newObj) {
			return;
		}

		if (revLinkInfo.getType() == OALinkInfo.ONE) {
			try {
				OAObjectInfo oiRev = callInfoGetObjectInfo(linkInfo.getToClass());
				Method m = callInfoGetMethod(oiRev, "get" + revLinkInfo.getName(), 0); // make sure that the method exists
				if (m != null) {
					if (oldObj instanceof OAObjectKey) {
						if (callSyncIsClient()) { // 20151117 dont get from server if this is client
							oldObj = callCacheGet(linkInfo.getToClass(), (OAObjectKey) oldObj);
						} else {
							oldObj = callReflectGetObject(linkInfo.getToClass(), (OAObjectKey) oldObj);
						}
					}
					if (oldObj instanceof OAObject) {
						// 20150820 if one2one, then dont load if null and isClient
						//   this was discovered when deleting an IDL and function/gsmrFunction (1to1) kept going to server for other value
						boolean b = true;
						if (callSyncIsClient()) {
							obj = callPropertyGetProperty((OAObject) oldObj, revLinkInfo.getName());
							if (obj == null) {
								// dont get from server
								b = false;
							}
						}
						if (b) {
							obj = callReflectGetProperty((OAObject) oldObj, revLinkInfo.getName());
							if (obj == oaObj) {
								callReflectSetProperty((OAObject) oldObj, revLinkInfo.getName(), null, null);
							}
						}
					}

					if (newObj instanceof OAObject) {
						// 20170411
						if (revLinkInfo.getOwner()) {
							callPropertySetPropertyCAS((OAObject) newObj, revLinkInfo.getName(), oaObj, null);
							callReflectSetProperty((OAObject) newObj, revLinkInfo.getName(), oaObj, null);
						} else {
							//was
							obj = callReflectGetProperty((OAObject) newObj, revLinkInfo.getName());
							if (obj != oaObj) {
								callReflectSetProperty((OAObject) newObj, revLinkInfo.getName(), oaObj, null);
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
			liRecursive = callInfoGetRecursiveLinkInfo(oi, OALinkInfo.ONE); // ex: "ParentSection"
		} else {
			liRecursive = null;
			//was: OALinkInfo liRecursive = srvcObject.getOAObjectInfoService().getRecursiveLinkInfo(oi, OALinkInfo.ONE);  // ex: "ParentSection"
		}

		boolean bOldIsKeyOnly = (oldObj instanceof OAObjectKey);

		// find all Hubs using this as the active object.
		// By changing a reference property, the object could be moved to another hub
		List<Hub> alUpdateHub = null;
		if (oldObj != null || liRecursive != null) {
			Hub[] hubs = callHubGetHubReferences(oaObj);
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
					Hub[] hubss = callHubShareGetAllSharedHubs(h, filter);

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

				obj = callReflectGetProperty(oaObj, callInfoGetReverseLinkInfo(liRecursive).getName()); // hubSections
				if (!(obj instanceof Hub)) {
					throw new RuntimeException("OAObject.updateLink() method for recursive link not returning a Hub.");
				}
				hub = (Hub) obj;
				for (int i = 0;; i++) {
					obj = hub.elementAt(i); // section
					if (obj == null) {
						break;
					}
					if (callReflectGetProperty((OAObject) obj, linkInfo.getName()) != newObj) {
						callReflectSetProperty((OAObject) obj, linkInfo.getName(), newObj, null); // setCatalog.  This will set all of its recursive children
					}
				}

				obj = callReflectGetProperty(oaObj, liRecursive.getName()); // get parent (section)
				if (obj != null) {
					obj = callReflectGetProperty((OAObject) obj, linkInfo.getName()); // catalog
					if (obj == newObj) {
						newObj = null; // otherwise, this object will be added to the rootHub
					} else {
						// set Parent to null  2003/09/21
						callReflectSetProperty(oaObj, liRecursive.getName(), null, null); // set ParentSection = null
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
								oldObj = callReflectGetObject(linkInfo.getToClass(), (OAObjectKey) oldObj);
							}
							callReflectSetProperty(oaObj, linkInfo.getName(), oldObj, null);
							throw new RuntimeException("Can not set the Parent to Itself");
						}
						// cant assign a child of this object as the new parent - causes orphaned objects
						for (obj = newObj;;) {
							obj = callReflectGetProperty((OAObject) obj, liRecursive.getName());
							if (obj == null) {
								break;
							}
							if (obj == oaObj) {
								if (bOldIsKeyOnly) {
									bOldIsKeyOnly = false;
									oldObj = callReflectGetObject(linkInfo.getToClass(), (OAObjectKey) oldObj);
								}
								callReflectSetProperty(oaObj, linkInfo.getName(), oldObj, null);
								throw new RuntimeException("Can not assign Parent to a Child");// causes orphans
							}
						}
					}

					// find owner link
					boolean bOwned = false;
					OALinkInfo linkOwner = callInfoGetLinkToOwner(oi); // link to catalog
					OALinkInfo liRev = null;
					if (linkOwner != null) {
						liRev = callInfoGetReverseLinkInfo(linkOwner);
					}

					if (liRev != null && liRev.getType() == OALinkInfo.MANY) {
						bOwned = true;
						if (newObj == null) { // parentSection = null
							// if being set to null, then add to root hub.
							// if it was removed from old hub, then dont add to root hub

							boolean bAdd = !callThreadLocalIsDeleting(oaObj);

							if (bAdd && !bOldIsKeyOnly
									&& callReflectIsReferenceHubLoadedAndNotEmpty((OAObject) oldObj, revLinkInfo.getName())) {
								hub = (Hub) callReflectGetProperty((OAObject) oldObj, revLinkInfo.getName()); // Catalog.sections (original hub that this objects belonged to)
								bAdd = hub.contains(oaObj);
							}

							if (bAdd) {
								obj = callReflectGetProperty(oaObj, linkOwner.getName()); // Catalog
								if (obj != null) {
									Object obj2 = callReflectGetProperty((OAObject) obj, liRev.getName()); // catalog.hubSection
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
							obj = callReflectGetProperty((OAObject) newObj, linkOwner.getName());

							if (callReflectGetProperty(oaObj, linkOwner.getName()) != obj) {
								callReflectSetProperty(oaObj, linkOwner.getName(), obj, null); // setCatalog (this will also set child recursive objects)
							}

							if (oldObj == null) {
								// remove from root hub, it is now assigned a parentSection
								obj = callReflectGetProperty(oaObj, linkOwner.getName()); // Catalog
								if (obj != null) {
									obj = callReflectGetProperty((OAObject) obj, liRev.getName()); // catalog.catalogSections
									if (!(obj instanceof Hub)) {
										throw new RuntimeException(
												"OAObject.updateLink() method for recursive link owner not returning a Hub.");
									}
									hub = (Hub) obj; // catalog.catalogSections
									callHubAddRemoveRemove(hub, oaObj, false, true, false, true, false, false);
								}
							}
						}
					}

					if (!bOwned) {
						Hub h = callInfoGetRootHub(oi);
						if (h != null) {
							if (oldObj == null) {
								// take out of unowned root hubs
								h.remove(oaObj);
							} else if (newObj == null) {
								// add to unowned root hubs
								// if it was removed from old hub, then dont add to root hub
								boolean bAdd = true;
								if (oldObj != null && !bOldIsKeyOnly
										&& callReflectIsReferenceHubLoaded((OAObject) oldObj, revLinkInfo.getName())) {
									hub = (Hub) callReflectGetProperty((OAObject) oldObj, revLinkInfo.getName()); // Catalog.sections (original hub that this objects belonged to)
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
				if (callCSIsServer(oaObj)
						|| callReflectIsReferenceHubLoaded((OAObject) oldObj, revLinkInfo.getName())) {
					obj = callReflectGetProperty((OAObject) oldObj, revLinkInfo.getName());
					if (obj instanceof Hub) {
						Hub h = (Hub) obj;
						if (h.contains(oaObj)) {
							callHubAddRemoveRemove(h, oaObj, false, true, false, true, false, false);
							hubRemovedFrom = h;
						}
					}
				}
			} catch (Exception e) {
			}
		}

		if (newObj != null && newObj instanceof OAObject) {
			try {
				if (callCSIsServer(oaObj)
						|| callReflectIsReferenceHubLoaded((OAObject) newObj, revLinkInfo.getName())) {
					hub = (Hub) callReflectGetProperty((OAObject) newObj, revLinkInfo.getName());

					// 20130630 added autoAttach check
					boolean bAutoAdd = callObjectGetAutoAdd(oaObj);

					if (bAutoAdd && hub != null) {
						hub.add(oaObj);

						if (oaObj.isNew()) {
							OAObject objMaster = hub.getMasterObject();
							if (objMaster != null) {
								if (!callObjectGetAutoAdd(objMaster)) {
									// turn off autoAdd for this object
									callObjectSetAutoAdd(oaObj, false);
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
						&& (hub.getSharedHub() != null && callHubDetailGetHubWithMasterHub(hub) != null);
				
                // 20230804 dont allow master AO change on hub where object was removed
				if (bAllowAdjustMaster && hubRemovedFrom != null && hub.getRealHub() == hubRemovedFrom) {
				    bAllowAdjustMaster = false; 
				}
				
				callHubAOSetActiveObject(hub, oaObj, bAllowAdjustMaster, false, false); // adjMaster, updateLink, force
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
		Hub[] hubs = callHubGetHubReferences(oaObj);
		if (hubs == null) {
			return;
		}
		for (Hub h : hubs) {
			if (h != null) {
				callHubEventFireAfterLoadEvent(h, oaObj);
			}
		}
	}


	
	@OAParentProvided (example = "srvcObject.getAutoAdd(oaObj)")
	public abstract boolean callObjectGetAutoAdd(OAObject oaObj);
	
	@OAParentProvided (example = "srvcObject.setAutoAdd(oaObj, false)")
	public abstract void callObjectSetAutoAdd(final OAObject oaObj, boolean bEnabled);
	
	
	
	@OAParentProvided (example = "srvcObject.getOAObjectCacheService().get(c, okj)")
	public abstract <T extends OAObject> T callCacheGet(Class<T> clazz, OAObjectKey ok);

	@OAParentProvided (example = "srvcObject.getOAObjectCacheService().fireAfterPropertyChange(..)")
	public abstract void callCacheFireAfterPropertyChange(OAObject obj, OAObjectKey origKey, String propertyName, Object oldValue, Object newValue, boolean bLocalOnly, boolean bSendEvent);
	
	
	
	@OAParentProvided (example = "srvcObject.getOAObjectCallbackService().getAllowSubmitObjectCallback(..)")
	public abstract OAObjectCallback callCallbackGetAllowSubmitObjectCallback(OAObject obj);

	@OAParentProvided (example = "srvcObject.getOAObjectCallbackService().getVerifyPropertyChangeObjectCallback(..)")
	public abstract OAObjectCallback callCallbackGetVerifyPropertyChangeObjectCallback(final int checkType, final OAObject oaObj, final String propertyName, final Object oldValue, final Object newValue);
	
	
	
	@OAParentProvided (example = "srvcObject.getOAObjectCSService().fireBeforePropertyChange(..)")
	public abstract void callCSFireBeforePropertyChange(OAObject obj, String propertyName, Object oldValue, Object newValue);

	@OAParentProvided (example = "srvcObject.getOAObjectCSService().isServer(oaObj)")
	public abstract boolean callCSIsServer(OAObject obj);
	
	
	
	@OAParentProvided (example = "srvcObject.getOAObjectDSService().isAssigningId(oaObj)")
	public abstract boolean callDSIsAssigningId(OAObject obj);
	
	
	
	@OAParentProvided (example = "srvcObject.getOAObjectHubService().isInHub(..)")
	public abstract boolean callHubIsInHub(OAObject oaObj);
	
	@OAParentProvided (example = "srvcObject.getOAObjectHubService().getHubReferences(oaObj)")
	public abstract Hub[] callHubGetHubReferences(OAObject oaObj);
	
	
	
	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getOAObjectInfo(clazz)")
	public abstract OAObjectInfo callInfoGetObjectInfo(Class clazz); 

	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getLinkInfo(..)")
	public abstract OALinkInfo callInfoGetLinkInfo(OAObjectInfo oi, String propertyName);

	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getReverseLinkInfo(li)")
	public abstract OALinkInfo callInfoGetReverseLinkInfo(OALinkInfo li);

	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getRecursiveLinkInfo(oi, type)")
	public abstract OALinkInfo callInfoGetRecursiveLinkInfo(OAObjectInfo oi, int type);
	
	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getPropertyInfo(oi, name")
	public abstract OAPropertyInfo callInfoGetPropertyInfo(OAObjectInfo oi, String propertyName);
	
	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getOACalcInfo(..)")
	public abstract OACalcInfo callInfoGetCalcInfo(OAObjectInfo thisOI, String name);
	
	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getMethod(oi, name, 0)")
	public abstract Method callInfoGetMethod(OAObjectInfo oi, String methodName, int argumentCount);
	
	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getLinkToOwner(oi)")
	public abstract OALinkInfo callInfoGetLinkToOwner(OAObjectInfo oi);
	
	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getRootHub(oi);")
	public abstract Hub callInfoGetRootHub(OAObjectInfo oi);
	

	
	@OAParentProvided (example = "srvcObject.getOAObjectKeyService().createChangedObjectKey(..)")
	public abstract OAObjectKey callKeyCreateChangedObjectKey(Class<? extends OAObject> clazz, OAObjectKey objKey, String propertyName, Object newValue);

	@OAParentProvided (example = "srvcObject.getOAObjectKeyService().verifyKeyChange(oaObj, okx)")
	public abstract String callKeyVerifyKeyChange(final OAObject oaObj, final OAObjectKey newObjectKey);
	
	@OAParentProvided (example = "srvcObject.getOAObjectKeyService().isForSameOAObject(..)")
	public abstract boolean callKeyIsForSameOAObject(final Class<? extends OAObject> clazz, final OAObjectKey ok1, final OAObjectKey ok2);
	
	@OAParentProvided (example = "srvcObject.getOAObjectKeyService().afterChangedObjectKeyProperty(..)")
	public abstract boolean callKeyAfterChangedObjectKeyProperty(final OAObject oaObj, final OAObjectKey okOrig, boolean bVerify);

	@OAParentProvided (example = "srvcObject.getOAObjectKeyService().getKey(oaObj)")
	public abstract OAObjectKey callKeyGetKey(OAObject oaObj);

	
	
	@OAParentProvided (example = "srvcObject.getOAObjectPropertyService().getProperty(..)")
	public abstract Object callPropertyGetProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef);

	@OAParentProvided (example = "srvcObject.getOAObjectPropertyService().setPropertyCAS")
	public abstract Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue);
	
	@OAParentProvided (example = "srvcObject.getOAObjectPropertyService().setPropertyCAS")
	public abstract Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue, boolean bMustNotExist, boolean bReturnNotExist);
	
	@OAParentProvided (example = "srvcObject.getOAObjectPropertyService().getProperty(oaObj, name)")
	public abstract Object callPropertyGetProperty(OAObject oaObj, String name);
	
	
	
	@OAParentProvided (example = "srvcObject.getOAObjectReflectService().getPrimitiveNull(..)")
	public abstract boolean callReflectGetPrimitiveNull(OAObject oaObj, String propertyName);
	
	@OAParentProvided (example = "srvcObject.getOAObjectReflectService().setPrimitiveNull(..)")
	public abstract void callReflectSetPrimitiveNull(OAObject oaObj, String propertyName, boolean bNull);	
	
	@OAParentProvided (example = "srvcObject.getOAObjectReflectService().getProperty(obj, name)")
	public abstract Object callReflectGetProperty(OAObject oaObj, String propPath);

	@OAParentProvided (example = "srvcObject.getOAObjectReflectService().setProperty(oaObj, name, value, fmt")
	public abstract void callReflectSetProperty(final OAObject oaObj, String propName, Object value, final String fmt);

	@OAParentProvided (example = "srvcObject.getOAObjectReflectService().getObject(..)")
	public abstract OAObject callReflectGetObject(Class clazz, Object key);
	
	@OAParentProvided (example = "srvcObject.getOAObjectReflectService().isReferenceHubLoadedAndNotEmpty(..)")
	public abstract boolean callReflectIsReferenceHubLoadedAndNotEmpty(OAObject oaObj, String propertyName);
	
	@OAParentProvided (example = "srvcObject.getOAObjectReflectService().isReferenceHubLoaded(..)")
	public abstract boolean callReflectIsReferenceHubLoaded(OAObject oaObj, String propertyName);
	
	
	
	@OAParentProvided (example = "srvcObject.getOAObjectUniqueService().getUnique(oaObj.getClass(), propertyName, newObj, false)")
	public abstract OAObject callUniqueGetUnique(final Class<? extends OAObject> clazz, final String propertyName, final Object uniqueKey, final boolean bAutoCreate);



	@OAParentProvided (example = "srvcHub.getHubAddRemoveService().remove(hub, oaObj, false, true, false, true, false, false)")
	public abstract boolean callHubAddRemoveRemove(final Hub thisHub, Object obj, final boolean bForce,
			final boolean bSendEvent, final boolean bDeleting, final boolean bSetAO,
			final boolean bSetPropToMaster, final boolean bIsRemovingAll);

	@OAParentProvided (example = "srvcHub.getHubAOService().setActiveObject(hub, oaObj, bAllowAdjustMaster, false, false)")
	public abstract void callHubAOSetActiveObject(Hub thisHub, Object object, boolean adjustMaster, boolean bUpdateLink, boolean bForce);
	
	@OAParentProvided (example = "srvcHub.getHubDetailService().getHubWithMasterHub(hub)")
	public abstract Hub callHubDetailGetHubWithMasterHub(final Hub thisHub);
	
	@OAParentProvided (example = "srvcHub.getHubEventService().fireBeforePropertyChange(..)")
	public abstract void callHubEventFireBeforePropertyChange(Hub thisHub, OAObject oaObj, String propertyName, Object oldValue, Object newValue);
	
	@OAParentProvided (example = "srvcHub.getHubEventService().fireAfterPropertyChange(..)")
	public abstract void callHubEventFireAfterPropertyChange(final Hub thisHub, final OAObject oaObj, final String propertyName, final Object oldValue,
			final Object newValue, final OALinkInfo linkInfo);

	@OAParentProvided (example = "srvcHub.getHubEventService().fireAfterLoadEvent(h, oaObj)")
	public abstract void callHubEventFireAfterLoadEvent(Hub thisHub, OAObject oaObj);
	
	@OAParentProvided (example = "srvcHub.getHubShareService().getAllSharedHubs(h, filter)")
	public abstract Hub[] callHubShareGetAllSharedHubs(Hub thisHub, OAFilter<Hub> filter);
	

	@OAParentProvided (example = "srvcSync.isServer()")
	public abstract boolean callSyncIsServer();

	@OAParentProvided (example = "srvcSync.isClient()")
	public abstract boolean callSyncIsClient();
	
	@OAParentProvided (example = "srvcSync.getSyncClient().isObjectOnServer(..)")
	public abstract boolean callSyncIsObjectOnServer(OAObject obj);
	
	

	@OAParentProvided (example = "srvcOAThreadLocal.isLoading()")
	public abstract boolean callThreadLocalIsLoading();
	
	@OAParentProvided (example = "srvcOAThreadLocal.isDeleting()")
	public abstract boolean callThreadLocalIsDeleting();

	@OAParentProvided (example = "srvcOAThreadLocal.isDeleting(obj)")
	public abstract boolean callThreadLocalIsDeleting(OAObject obj);
	
	@OAParentProvided (example = "srvcOAThreadLocal.getCreateUndoablePropertyChanges()")
	public abstract boolean callThreadLocalGetCreateUndoablePropertyChanges();

	@OAParentProvided (example = "srvcOAThreadLocal.setSuppressCSMessages(true)")
	public abstract void callThreadLocalSetSuppressCSMessages(boolean b);
	
	@OAParentProvided (example = "srvcOAThreadLocal.setDeleting(..)")
	public abstract void callThreadLocalSetDeleting(Object obj, boolean b);

	@OAParentProvided (example = "srvcOAThreadLocal.addHubEvent(hubEvent)")
	public abstract void callThreadLocalAddHubEvent(HubEvent he);
	
	@OAParentProvided (example = "srvcOAThreadLocal.removeHubEvent(hubEvent)")
	public abstract void callThreadLocalRemoveHubEvent(HubEvent he);
	
	
	
	@OAParentProvided (example = "srvcOARemoteThread.isRemoteThread()")
	public abstract boolean callRemoteThreadIsRemoteThread();
	
	@OAParentProvided (example = "srvcOARemoteThread.startNextThread()")
	public abstract void callRemoteThreadStartNextThread();

}
