package com.viaoa.oa.service.object;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.callback.OAObjectCallback;
import com.viaoa.compare.OACompare;
import com.viaoa.compare.match.OAMatchNotExist;
import com.viaoa.compare.match.OAMatchNull;
import com.viaoa.concurrent.OAThrottle;
import com.viaoa.converter.OAConv;
import com.viaoa.datetime.OADateTime;
import com.viaoa.filter.OAFilter;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.lang.OAString;
import com.viaoa.metadata.OACalcInfo;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.metadata.OAPropertyInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.reflect.OAReflect;
import com.viaoa.undo.OAUndoManager;
import com.viaoa.undo.OAUndoableEdit;

/*qqqqqqqq
CODEX

#6
  File/Class/Method: src/main/java/com/viaoa/oa/service/object/OAObjectEventService.java, firePropertyChange(...)

  Exact execution path: for link-property changes, firePropertyChange(...) updates the stored property/CAS, sets
  temporary changed state, fires Hub/cache after-property-change events, then later calls updateLink(...). If
  updateLink(...) throws, reverse Hub/detail ownership updates may not complete even though observers and cache
  listeners already saw the property change.

  Why it is a correctness bug: forward reference state and published events can commit before reverse-link OA model
  consistency is established.

  Semantic/invariant violated: link property transitions must update forward and reverse OA model state before after-
  events/sync publication.

  Minimal fix: perform updateLink(...) before external after-change/cache publication, or add rollback/explicit
  partial-failure handling around reverse-link update failures.

  Suggested test: link setter where reverse Hub getter returns an invalid non-Hub or reverse update throws; assert
  no after-property-change event is fired unless forward and reverse state are consistent.



#1

  1. file/class/method
     src/main/java/com/viaoa/oa/service/object/OAObjectEventService.java:816
     OAObjectEventService.updateLink(...)
  2. exact execution path
     A link property changes successfully. firePropertyChange(...) reaches updateLink(...). For a reverse ONE link,
     the method attempts to clear the old reverse reference or set the new reverse reference inside:

  try {
      ...
      callReflectSetProperty(...)
      ...
  } catch (Exception e) {
  }

  For reverse MANY/hub maintenance, old-hub removal is also swallowed, and new-hub add logs but does not fail the
  operation.

  3. why this is a real correctness bug
     The primary property change succeeds and caller sees success, but reverse link/hub maintenance can fail
     silently. That can leave child.parent == newParent while newParent.children does not contain child, or
     oldParent.children still contains it.
  4. semantic/invariant violated
     Bidirectional relationship maintenance must either complete or fail visibly. Silent inverse-link failure
     corrupts hub membership/reference consistency.
  5. minimal fix
     Do not swallow these exceptions. Propagate them, or explicitly mark the reverse-update as best-effort only with
     a reconciliation path. At minimum, replace empty catches with thrown runtime exceptions carrying object/
     property context.
  6. suggested regression test
     Create a bidirectional ONE/MANY or ONE/ONE relationship where the reverse setter/listener throws. Set the
     forward property. Assert the caller gets an exception and the OA model does not report a successful forward-only
     relationship transition.




*/

public abstract class OAObjectEventService {
	private static final Logger LOG = Logger.getLogger(OAObjectEventService.class.getName());

	private final OAObject.FriendAccess faObject;


	/**
	 * Performs OAObjectEventService behavior for the OA object service.
	 *
	 * @param faObject method input
	 */
    public OAObjectEventService(OAObject.FriendAccess faObject) {
    	if (faObject == null) throw new IllegalArgumentException("OAObject.FriendAccess can not be null");
    	this.faObject = faObject;
    }
	
	/**
	 * Internal reserved property name used to identify change-flag updates
	 * emitted through the event pipeline.
	 */
	public final static String WORD_CHANGED = "CHANGED";

	/**
	 * Counter used to track how many validation or callback-related errors
	 * have occurred, allowing the logger to include sequence information.
	 */
	private volatile int cntError;

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
				if (!callSyncIsClient()) { // 20150604 if client, then it needs to send prop change to server
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
				bSkip = "Changed".equalsIgnoreCase(propertyName);
				bSkip = bSkip || "New".equalsIgnoreCase(propertyName);
				bSkip = bSkip || "Deleted".equalsIgnoreCase(propertyName);
			}

			if (!bSkip && !bIsLoading) {
				OAObjectCallback em = callRulesGetVerifyPropertyChangeCallbackOnlyObjectCallback(oaObj, propertyName, oldObj, newObj
				);
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
					throw new RuntimeException(msg, em.getThrowable());
				}
			}
		}

		// check to see if it is actually changed
		if (oldObj != null) {
			if (callReflectGetPrimitiveNull(oaObj, propertyName) || oldObj instanceof OAMatchNull) {
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
					for (int i=0; i<250; i++) {
						
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
			/* 20260720 commented out, not sure why this is here.  A property that isSubmit=true will not pass until it's value is set ... this is the 'before' set
			if (!bIsLoading && propInfo != null && propInfo.getIsSubmit() && newObj != null) {
				if (OAConv.toBoolean(newObj)) {
					OAObjectCallback eq = callRulesGetAllowSubmitObjectCallback(oaObj);
					if (!eq.getAllowed()) {
						throw new RuntimeException("submit failed, Class="
								+ oaObj.getClass().getSimpleName() + ", message=" + eq.getResponse(), eq.getThrowable());
					}
				}
			}
			*/

			if (propInfo != null) {
				if (propInfo.getId() && !callDSIsAssigningId(oaObj)) {
					OAObjectKey okx = callKeyCreateChangedObjectKey(oaObj.getClass(), oaObj.getObjectKey(), propertyName, newObj);
					String s = callKeyVerifyKeyChange(oaObj, okx);
					if (s != null) {
						throw new RuntimeException(s);
					}
				}

				if (newObj instanceof OADateTime) {
					if (propInfo.getIgnoreTimeZone()) {
						// 20260603 no longer in OADateTime
						// ((OADateTime) newObj).setIgnoreTimeZone(true);
					}
				}

				if (propInfo.getUnique() && newObj != null && !propInfo.getId() && !callDSIsAssigningId(oaObj)) {
					if (!bIsLoading) { // 20221219
						OAObject obj = callUniqueGetUnique(oaObj.getClass(), propertyName, newObj, false);
						if (obj != null && obj != oaObj) {
							throw new RuntimeException("property is unique, and value already assigned to another object. Class="
									+ oaObj.getClass().getSimpleName() + ", property=" + propertyName + ", value=" + newObj);
						}
					}
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
		if (oaObj == null || propertyName == null) {
			return;
		}

		String propertyU = propertyName.toUpperCase();

		final OAObjectInfo oi = callInfoGetObjectInfo(oaObj.getClass());

		if (oldObj != null && !bUnknownValues) {
			if (callReflectGetPrimitiveNull(oaObj, propertyU) || oldObj instanceof OAMatchNull) {
				oldObj = null;
			}
		}

		//  note: a primitive null can only be set by calling srvcObject.getOAObjectReflectService().setProperty(...)
		if (newObj instanceof OAMatchNull) {
			newObj = null;
		}

		if (newObj != null || !bUnknownValues) {
			callReflectSetPrimitiveNull(oaObj, propertyU, (newObj == null));
		}

		if (oldObj instanceof OAMatchNull) {
			oldObj = null;
		}

		final OALinkInfo linkInfo = callInfoGetLinkInfo(oi, propertyU);
		boolean bWasEmpty = false;
		if (!bUnknownValues && linkInfo != null && oldObj == null) {
			// oldObj might never have been loaded before setMethod was called, which will have the oldValue=null -
			//   need to check in oaObj.properties to see what orig value was.
			oldObj = callPropertyGetProperty(oaObj, propertyName, true, true);
			if (oldObj == OAMatchNotExist.instance) {
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
				Object objx = callCacheGetUsingKey(linkInfo.getToClass(), (OAObjectKey) oldObj);
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
					OAUndoableEdit ue = OAUndoableEdit.createUndoablePropertyChange(null, oaObj, propertyName, oldObj, newObj,bChangeHold);
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
							OAObject objx = callCacheGetUsingKey(linkInfo.getToClass(), (OAObjectKey) oldObj);
							if (objx != null) {
								callPropertySetPropertyCAS(objx, revLinkInfo.getName(), null, oaObj);
							}
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

		if (!bIsChangeProp) {
			faObject.setChangedFlag(oaObj, bChangeHold);
		}

		// set to Changed
		if (!bIsChangeProp && bSetChanged && !bChangeHold && (calcInfo == null) && !bUnknownValues) {
			if (!oaObj.isChanged()) {
				if (linkInfo == null || !linkInfo.getCalculated()) { // 20120429
					final boolean bWas = callThreadLocalGetSendSyncMessages(); 
					try {
						callThreadLocalSetSendSyncMessages(false); // the client will setChanged when it gets the propertyChange message
						oaObj.setChanged(true);
					} finally {
						callThreadLocalSetSendSyncMessages(bWas);
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
	public <T extends OAObject> void sendHubBeforePropertyChange(T oaObj, String propertyName, Object oldObj, Object newObj) {
		Hub<T>[] hubs = callHubGetHubReferences(oaObj);
		if (hubs == null) {
			return;
		}
		for (Hub<T> h : hubs) {
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
	public <T extends OAObject> void sendHubPropertyChange(final T oaObj, final String propertyName, final Object oldObj, final Object newObj,
			final OALinkInfo linkInfo) {
		// Note: don't add this, HubEventDelegate will do it after it updates detail hubs:
		//        if (OAObjectFlagDelegate.isSuppressingPropertyChangeEvents()) return;
		// Note: oldObj could be OAObjectKey

		Hub<T>[] hubs = callHubGetHubReferences(oaObj);
		if (hubs == null) {
			return;
		}
		for (Hub<T> h : hubs) {
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
	private <T extends OAObject> void updateLink(final T oaObj, OAObjectInfo oi, OALinkInfo linkInfo, Object oldObj, Object newObj) {
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
							oldObj = callCacheGetUsingKey(linkInfo.getToClass(), (OAObjectKey) oldObj);
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
			Hub<T>[] hubs = callHubGetHubReferences(oaObj);
			if (hubs != null) {
				for (Hub<T> h : hubs) {
					if (h == null) {
						continue;
					}

					// 20120716
					OAFilter<Hub<T>> filter = new OAFilter<Hub<T>>() {
						@Override
	/**
	 * Returns whether used is true.
	 *
	 * @param h method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
						public boolean isUsed(Hub<T> h) {
							return h.getAO() == oaObj;
						}
					};
					Hub<T>[] hubss = callHubShareGetAllSharedHubs(h, filter);

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
				if (callSyncIsServer()
						|| callReflectIsReferenceHubLoaded((OAObject) oldObj, revLinkInfo.getName())) {
					obj = callReflectGetProperty((OAObject) oldObj, revLinkInfo.getName());
					if (obj instanceof Hub) {
						Hub<?> h = (Hub) obj;
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
				if (callSyncIsServer()
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
	public <T extends OAObject> void fireAfterLoadEvent(T oaObj) {
		Hub<T>[] hubs = callHubGetHubReferences(oaObj);
		if (hubs == null) {
			return;
		}
		for (Hub<T> h : hubs) {
			if (h != null) {
				callHubEventFireAfterLoadEvent(h, oaObj);
			}
		}
	}


	/**
	 * Dependency hook used by this service to objectGetAutoAdd.
	 *
	 * @param oaObj method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callObjectGetAutoAdd(OAObject oaObj);
	/**
	 * Dependency hook used by this service to objectSetAutoAdd.
	 *
	 * @param oaObj method input
	 * @param bEnabled method input
	 */
	public abstract void callObjectSetAutoAdd(final OAObject oaObj, boolean bEnabled);
	/**
	 * Dependency hook used by this service to cacheGet.
	 *
	 * @param clazz method input
	 * @param ok method input
	 * @return result value
	 */
	public abstract <T extends OAObject> T callCacheGetUsingKey(Class<T> clazz, OAObjectKey ok);
	/**
	 * Dependency hook used by this service to cacheFireAfterPropertyChange.
	 *
	 * @param obj method input
	 * @param origKey method input
	 * @param propertyName method input
	 * @param oldValue method input
	 * @param newValue method input
	 * @param bLocalOnly method input
	 * @param bSendEvent method input
	 */
	public abstract void callCacheFireAfterPropertyChange(OAObject obj, OAObjectKey origKey, String propertyName, Object oldValue, Object newValue, boolean bLocalOnly, boolean bSendEvent);
	/**
	 * Dependency hook used by this service to rulesGetAllowSubmitObjectCallback.
	 *
	 * @param obj method input
	 * @return result value
	 */
	public abstract OAObjectCallback callRulesGetAllowSubmitObjectCallback(OAObject obj);
	/**
	 * Dependency hook used by this service to rulesGetVerifyPropertyChangeCallbackOnlyObjectCallback.
	 *
	 * @param oaObj method input
	 * @param propertyName method input
	 * @param oldValue method input
	 * @param newValue method input
	 * @return result value
	 */
	public abstract OAObjectCallback callRulesGetVerifyPropertyChangeCallbackOnlyObjectCallback(final OAObject oaObj, final String propertyName, final Object oldValue, final Object newValue);
	/**
	 * Dependency hook used by this service to cSFireBeforePropertyChange.
	 *
	 * @param obj method input
	 * @param propertyName method input
	 * @param oldValue method input
	 * @param newValue method input
	 */
	public abstract void callCSFireBeforePropertyChange(OAObject obj, String propertyName, Object oldValue, Object newValue);
	/**
	 * Dependency hook used by this service to dSIsAssigningId.
	 *
	 * @param obj method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callDSIsAssigningId(OAObject obj);
	/**
	 * Dependency hook used by this service to hubIsInHub.
	 *
	 * @param oaObj method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callHubIsInHub(OAObject oaObj);
	/**
	 * Dependency hook used by this service to hubGetHubReferences.
	 *
	 * @param oaObj method input
	 * @return result value
	 */
	public abstract <T extends OAObject> Hub<T>[] callHubGetHubReferences(T oaObj);
	/**
	 * Dependency hook used by this service to infoGetObjectInfo.
	 *
	 * @param clazz method input
	 * @return result value
	 */
	public abstract OAObjectInfo callInfoGetObjectInfo(Class<?> clazz);
	/**
	 * Dependency hook used by this service to infoGetLinkInfo.
	 *
	 * @param oi method input
	 * @param propertyName method input
	 * @return result value
	 */
	public abstract OALinkInfo callInfoGetLinkInfo(OAObjectInfo oi, String propertyName);
	/**
	 * Dependency hook used by this service to infoGetReverseLinkInfo.
	 *
	 * @param li method input
	 * @return result value
	 */
	public abstract OALinkInfo callInfoGetReverseLinkInfo(OALinkInfo li);
	/**
	 * Dependency hook used by this service to infoGetRecursiveLinkInfo.
	 *
	 * @param oi method input
	 * @param type method input
	 * @return result value
	 */
	public abstract OALinkInfo callInfoGetRecursiveLinkInfo(OAObjectInfo oi, int type);
	/**
	 * Dependency hook used by this service to infoGetPropertyInfo.
	 *
	 * @param oi method input
	 * @param propertyName method input
	 * @return result value
	 */
	public abstract OAPropertyInfo callInfoGetPropertyInfo(OAObjectInfo oi, String propertyName);
	/**
	 * Dependency hook used by this service to infoGetCalcInfo.
	 *
	 * @param thisOI method input
	 * @param name method input
	 * @return result value
	 */
	public abstract OACalcInfo callInfoGetCalcInfo(OAObjectInfo thisOI, String name);
	/**
	 * Dependency hook used by this service to infoGetMethod.
	 *
	 * @param oi method input
	 * @param methodName method input
	 * @param argumentCount method input
	 * @return result value
	 */
	public abstract Method callInfoGetMethod(OAObjectInfo oi, String methodName, int argumentCount);
	/**
	 * Dependency hook used by this service to infoGetLinkToOwner.
	 *
	 * @param oi method input
	 * @return result value
	 */
	public abstract OALinkInfo callInfoGetLinkToOwner(OAObjectInfo oi);
	/**
	 * Dependency hook used by this service to infoGetRootHub.
	 *
	 * @param oi method input
	 * @return result value
	 */
	public abstract Hub callInfoGetRootHub(OAObjectInfo oi);
	/**
	 * Dependency hook used by this service to keyCreateChangedObjectKey.
	 *
	 * @param clazz method input
	 * @param objKey method input
	 * @param propertyName method input
	 * @param newValue method input
	 * @return result value
	 */
	public abstract OAObjectKey callKeyCreateChangedObjectKey(Class<? extends OAObject> clazz, OAObjectKey objKey, String propertyName, Object newValue);
	/**
	 * Dependency hook used by this service to keyVerifyKeyChange.
	 *
	 * @param oaObj method input
	 * @param newObjectKey method input
	 * @return result value
	 */
	public abstract String callKeyVerifyKeyChange(final OAObject oaObj, final OAObjectKey newObjectKey);
	/**
	 * Dependency hook used by this service to keyIsForSameOAObject.
	 *
	 * @param clazz method input
	 * @param ok1 method input
	 * @param ok2 method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callKeyIsForSameOAObject(final Class<? extends OAObject> clazz, final OAObjectKey ok1, final OAObjectKey ok2);
	/**
	 * Dependency hook used by this service to keyAfterChangedObjectKeyProperty.
	 *
	 * @param oaObj method input
	 * @param okOrig method input
	 * @param bVerify method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callKeyAfterChangedObjectKeyProperty(final OAObject oaObj, final OAObjectKey okOrig, boolean bVerify);
	/**
	 * Dependency hook used by this service to keyGetKey.
	 *
	 * @param oaObj method input
	 * @return result value
	 */
	public abstract OAObjectKey callKeyGetKey(OAObject oaObj);
	/**
	 * Dependency hook used by this service to propertyGetProperty.
	 *
	 * @param oaObj method input
	 * @param name method input
	 * @param bReturnNotExist method input
	 * @param bConvertWeakRef method input
	 * @return result value
	 */
	public abstract Object callPropertyGetProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef);
	/**
	 * Dependency hook used by this service to propertySetPropertyCAS.
	 *
	 * @param oaObj method input
	 * @param name method input
	 * @param newValue method input
	 * @param matchValue method input
	 * @return result value
	 */
	public abstract Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue);
	/**
	 * Dependency hook used by this service to propertySetPropertyCAS.
	 *
	 * @param oaObj method input
	 * @param name method input
	 * @param newValue method input
	 * @param matchValue method input
	 * @param bMustNotExist method input
	 * @param bReturnNotExist method input
	 * @return result value
	 */
	public abstract Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue, boolean bMustNotExist, boolean bReturnNotExist);
	/**
	 * Dependency hook used by this service to propertyGetProperty.
	 *
	 * @param oaObj method input
	 * @param name method input
	 * @return result value
	 */
	public abstract Object callPropertyGetProperty(OAObject oaObj, String name);
	/**
	 * Dependency hook used by this service to reflectGetPrimitiveNull.
	 *
	 * @param oaObj method input
	 * @param propertyName method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callReflectGetPrimitiveNull(OAObject oaObj, String propertyName);
	/**
	 * Dependency hook used by this service to reflectSetPrimitiveNull.
	 *
	 * @param oaObj method input
	 * @param propertyName method input
	 * @param bNull method input
	 */
	public abstract void callReflectSetPrimitiveNull(OAObject oaObj, String propertyName, boolean bNull);
	/**
	 * Dependency hook used by this service to reflectGetProperty.
	 *
	 * @param oaObj method input
	 * @param propPath method input
	 * @return result value
	 */
	public abstract Object callReflectGetProperty(OAObject oaObj, String propPath);
	/**
	 * Dependency hook used by this service to reflectSetProperty.
	 *
	 * @param oaObj method input
	 * @param propName method input
	 * @param value method input
	 * @param fmt method input
	 */
	public abstract void callReflectSetProperty(final OAObject oaObj, String propName, Object value, final String fmt);
	/**
	 * Dependency hook used by this service to reflectGetObject.
	 *
	 * @param clazz method input
	 * @param key method input
	 * @return result value
	 */
	public abstract OAObject callReflectGetObject(Class<? extends OAObject> clazz, Object key);
	/**
	 * Dependency hook used by this service to reflectIsReferenceHubLoadedAndNotEmpty.
	 *
	 * @param oaObj method input
	 * @param propertyName method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callReflectIsReferenceHubLoadedAndNotEmpty(OAObject oaObj, String propertyName);
	/**
	 * Dependency hook used by this service to reflectIsReferenceHubLoaded.
	 *
	 * @param oaObj method input
	 * @param propertyName method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callReflectIsReferenceHubLoaded(OAObject oaObj, String propertyName);
	/**
	 * Dependency hook used by this service to uniqueGetUnique.
	 *
	 * @param clazz method input
	 * @param propertyName method input
	 * @param uniqueKey method input
	 * @param bAutoCreate method input
	 * @return result value
	 */
	public abstract OAObject callUniqueGetUnique(final Class<? extends OAObject> clazz, final String propertyName, final Object uniqueKey, final boolean bAutoCreate);
	/**
	 * Dependency hook used by this service to hubAddRemoveRemove.
	 *
	 * @param thisHub method input
	 * @param obj method input
	 * @param bForce method input
	 * @param bSendEvent method input
	 * @param bDeleting method input
	 * @param bSetAO method input
	 * @param bSetPropToMaster method input
	 * @param bIsRemovingAll method input
	 * @return result value
	 */
	public abstract <T extends OAObject> T callHubAddRemoveRemove(final Hub<T> thisHub, Object obj, final boolean bForce,
			final boolean bSendEvent, final boolean bDeleting, final boolean bSetAO,
			final boolean bSetPropToMaster, final boolean bIsRemovingAll);
	/**
	 * Dependency hook used by this service to hubAOSetActiveObject.
	 *
	 * @param thisHub method input
	 * @param object method input
	 * @param adjustMaster method input
	 * @param bUpdateLink method input
	 * @param bForce method input
	 */
	public abstract <T extends OAObject> void callHubAOSetActiveObject(Hub<T> thisHub, T object, boolean adjustMaster, boolean bUpdateLink, boolean bForce);
	/**
	 * Dependency hook used by this service to hubDetailGetHubWithMasterHub.
	 *
	 * @param thisHub method input
	 */
	public abstract <T extends OAObject> Hub<T> callHubDetailGetHubWithMasterHub(final Hub<T> thisHub);
	/**
	 * Dependency hook used by this service to hubEventFireBeforePropertyChange.
	 *
	 * @param thisHub method input
	 * @param oaObj method input
	 * @param propertyName method input
	 * @param oldValue method input
	 * @param newValue method input
	 */
	public abstract <T extends OAObject> void callHubEventFireBeforePropertyChange(Hub<T> thisHub, T oaObj, String propertyName, Object oldValue, Object newValue);
	/**
	 * Dependency hook used by this service to hubEventFireAfterPropertyChange.
	 *
	 * @param thisHub method input
	 * @param oaObj method input
	 * @param propertyName method input
	 * @param oldValue method input
	 * @param newValue method input
	 * @param linkInfo method input
	 */
	public abstract <T extends OAObject> void callHubEventFireAfterPropertyChange(final Hub<T> thisHub, final T oaObj, final String propertyName, final Object oldValue,
			final Object newValue, final OALinkInfo linkInfo);
	/**
	 * Dependency hook used by this service to hubEventFireAfterLoadEvent.
	 *
	 * @param thisHub method input
	 * @param oaObj method input
	 */
	public abstract <T extends OAObject> void callHubEventFireAfterLoadEvent(Hub<T> thisHub, T oaObj);
	/**
	 * Dependency hook used by this service to hubShareGetAllSharedHubs.
	 *
	 * @param thisHub method input
	 * @param filter method input
	 * @return result value
	 */
	public abstract <T extends OAObject> Hub<T>[] callHubShareGetAllSharedHubs(Hub<T> thisHub, OAFilter<Hub<T>> filter);
	/**
	 * Dependency hook used by this service to syncIsServer.
	 *
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callSyncIsServer();
	/**
	 * Dependency hook used by this service to syncIsClient.
	 *
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callSyncIsClient();
	/**
	 * Dependency hook used by this service to syncIsObjectOnServer.
	 *
	 * @param obj method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callSyncIsObjectOnServer(OAObject obj);
	/**
	 * Dependency hook used by this service to threadLocalIsLoading.
	 *
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callThreadLocalIsLoading();
	/**
	 * Dependency hook used by this service to threadLocalIsDeleting.
	 *
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callThreadLocalIsDeleting();
	/**
	 * Dependency hook used by this service to threadLocalIsDeleting.
	 *
	 * @param obj method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callThreadLocalIsDeleting(OAObject obj);
	/**
	 * Dependency hook used by this service to threadLocalGetCreateUndoablePropertyChanges.
	 *
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callThreadLocalGetCreateUndoablePropertyChanges();
	/**
	 * Dependency hook used by this service to threadLocalSetDeleting.
	 *
	 * @param obj method input
	 * @param b method input
	 */
	public abstract void callThreadLocalSetDeleting(Object obj, boolean b);
	/**
	 * Dependency hook used by this service to threadLocalAddHubEvent.
	 *
	 * @param he method input
	 */
	public abstract void callThreadLocalAddHubEvent(HubEvent<?> he);
	/**
	 * Dependency hook used by this service to threadLocalRemoveHubEvent.
	 *
	 * @param he method input
	 */
	public abstract void callThreadLocalRemoveHubEvent(HubEvent<?>  he);
	/**
	 * Dependency hook used by this service to remoteThreadIsRemoteThread.
	 *
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callRemoteThreadIsRemoteThread();
	/**
	 * Dependency hook used by this service to remoteThreadStartNextThread.
	 */
	public abstract void callRemoteThreadStartNextThread();
	/**
	 * Dependency hook used by this service to threadLocalGetSendSyncMessages.
	 *
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callThreadLocalGetSendSyncMessages();
	/**
	 * Dependency hook used by this service to threadLocalSetSendSyncMessages.
	 *
	 * @param b method input
	 */
	public abstract void callThreadLocalSetSendSyncMessages(boolean b);
}
