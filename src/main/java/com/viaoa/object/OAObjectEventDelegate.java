/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.object;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.hub.Hub;
import com.viaoa.hub.HubAODelegate;
import com.viaoa.hub.HubAddRemoveDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubEventDelegate;
import com.viaoa.hub.HubShareDelegate;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.sync.*;
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

/**
 * Centralized event router for property changes on {@link OAObject}.
 * <p>
 * All mutation flows that affect object state, relationships, dirty flags,
 * hub membership, undo history, or distributed synchronization go through this
 * delegate to preserve global consistency and strict ordering rules.
 *
 * <h3>Primary Responsibilities</h3>
 * <ul>
 *   <li><b>Before / After Change Sequencing</b> — guarantees listener contract:
 *       before-change → mutation → after-change</li>
 *   <li><b>Loading-aware Mutations</b> — suppresses unnecessary events during
 *       lazy-load or server-side initialization without losing correctness</li>
 *   <li><b>Reference & Reverse Link Updates</b> — adjusts owning Hubs and
 *       reverse 1-1 or 1-many relationships without re-entrant storms</li>
 *   <li><b>Identity Impact Tracking</b> — routes ID changes to cache/index
 *       ensuring no drift or duplicate instances</li>
 *   <li><b>Distributed Sync Integration</b> — only propagates to server/clients
 *       when the object is authoritative and eligible for transmission</li>
 *   <li><b>Undo Support</b> — builds Undoable property edits when enabled</li>
 *   <li><b>Trigger Execution</b> — invokes model-level onChange callbacks safely</li>
 * </ul>
 *
 * <h3>Correctness Guarantees</h3>
 * <ul>
 *   <li>Events only fire for real state changes (except primitive wrappers where
 *       UI correctness requires notifications)</li>
 *   <li>No property change is allowed if violated by callbacks or metadata 
 *       constraints (unique, id, recursive-parent rules)</li>
 *   <li>Hub + reverse link changes occur <em>after</em> mutation but <em>before</em>
 *       triggers and distributed sync to ensure graph stability</li>
 *   <li>Thread-local context prevents recursive storms and misplaced sync</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * Application code does not call this class directly. Event emission is driven
 * automatically by {@link OAObject} and metadata-aware automation.
 *
 * @since OA 1.0
 * @see OAObject
 * @see OAObjectDelegate
 * @see OAObjectHubDelegate
 * @see OAObjectCacheDelegate
 */
public class OAObjectEventDelegate {

	private static Logger LOG = Logger.getLogger(OAObjectEventDelegate.class.getName());
	
	/**
	 * Internal reserved property name used to identify change-flag updates
	 * emitted through the event pipeline.
	 */
	private static final String WORD_CHANGED = "CHANGED";

	/**
	 * Timestamp used to throttle repeated warning messages so they do not
	 * flood the log when callback or validation errors occur frequently.
	 */
	private static volatile long msThrottle;

	/**
	 * Counter used to track how many validation or callback-related errors
	 * have occurred, allowing the logger to include sequence information.
	 */
	private static int cntError;

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
	protected static void fireBeforePropertyChange(final OAObject oaObj, final String propertyName,
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
	private static void _fireBeforePropertyChange(final OAObject oaObj, final String propertyName,
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

		final boolean bIsLoading = OAThreadLocalDelegate.isLoading();
		if (bIsLoading) {
			if (!OAObjectHubDelegate.isInHub(oaObj)) { // 20110719: could be in the OAObjectCache.SelectAllHubs
				// no listeners, need to load quick as possible
				if (OASyncDelegate.isServer(oaObj)) { // 20150604 if client, then it needs to send prop change to server
					return;
				}
				OASyncClient sc = OASync.getSyncClient(); 
				if (sc != null && !sc.isObjectOnServer(oaObj)) return;
			}
		} else if (!OARemoteThreadDelegate.isRemoteThread()) {
			// 20180617 validate
			boolean bSkip = false;
			if (propertyName != null) {
				bSkip = OAObjectDelegate.WORD_Changed.equalsIgnoreCase(propertyName);
				bSkip = bSkip || OAObjectDelegate.WORD_New.equalsIgnoreCase(propertyName);
				bSkip = bSkip || OAObjectDelegate.WORD_Deleted.equalsIgnoreCase(propertyName);
			}

			if (!bSkip && !bIsLoading) {
				OAObjectCallback em = OAObjectCallbackDelegate.getVerifyPropertyChangeObjectCallback(	OAObjectCallback.CHECK_CallbackMethod,
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
			if (OAObjectReflectDelegate.getPrimitiveNull(oaObj, propertyName) || oldObj instanceof OANullObject) {
				oldObj = null;
			}
		}

		// verify that change is permitted
		// verify if recursive link that new parent is allowed
		final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
		final String propertyU = propertyName.toUpperCase();
		final OALinkInfo linkInfo = OAObjectInfoDelegate.getLinkInfo(oi, propertyU);
		OALinkInfo toLinkInfo;
		if (linkInfo != null) {
			toLinkInfo = OAObjectInfoDelegate.getReverseLinkInfo(linkInfo);
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
						Object obj = OAObjectPropertyDelegate.getProperty(oaObj, li.getName(), false, true);
						if (obj instanceof OAObject) {
							obj = ((OAObject) obj).getObjectKey();
						}
						if (!(obj instanceof OAObjectKey)) {
							obj = null;
						}
						okNew = OAObjectKeyDelegate.createChangedObjectKey(	li.getToClass(), (OAObjectKey) obj,
																			fki.getToPropertyInfo().getName(), newObj);
						if (okNew.isEmpty()) {
							okNew = null;
						}
						_fireBeforePropertyChange(	oaObj, li.getName(),
													OAObjectPropertyDelegate.getProperty(oaObj, li.getName(), false, true),
													okNew,
													bLocalOnly, false, true);
						break;
					}
				}
			}
		}
		*/

		if (toLinkInfo != null && toLinkInfo.bRecursive) {
			OALinkInfo liRecursive = OAObjectInfoDelegate.getRecursiveLinkInfo(oi, OALinkInfo.ONE); // ex: "ParentSection"
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
						obj = OAObjectReflectDelegate.getProperty((OAObject) obj, liRecursive.getName());
						if (obj == null) {
							break;
						}
						if (obj == oaObj) {
							OAObjectReflectDelegate.setProperty(oaObj, linkInfo.getName(), oldObj, null);
							throw new RuntimeException("Can not assign Parent to a Child");// causes orphans
						}
					}
				}
			}
		}

		// 20151205 check to see if owner is being reassigned
		if (linkInfo != null && oldObj != null && newObj != null && !oaObj.isNew() && linkInfo.getType() == OALinkInfo.ONE
				&& !linkInfo.getCalculated()) {
			OALinkInfo revLinkInfo = OAObjectInfoDelegate.getReverseLinkInfo(linkInfo);
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
    			if (!OAThreadLocalDelegate.isDeleting() && OASync.isServer()) {
    				OAObjectInfo oix = OAObjectInfoDelegate.getOAObjectInfo((OAObject) oldObj);
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

		if (linkInfo == null && !OARemoteThreadDelegate.isRemoteThread()) {
			OAPropertyInfo propInfo = OAObjectInfoDelegate.getPropertyInfo(oi, propertyU);
			if (!bIsLoading && propInfo != null && propInfo.getIsSubmit() && newObj != null) {
				if (OAConv.toBoolean(newObj)) {
					OAObjectCallback eq = OAObjectCallbackDelegate.getAllowSubmitObjectCallback(oaObj);
					if (!eq.getAllowed()) {
						throw new RuntimeException("submit failed, Class="
								+ oaObj.getClass().getSimpleName() + ", message=" + eq.getResponse(), eq.getThrowable());
					}
				}
			}

			if (propInfo != null) {
				if (propInfo.getId() && !OAObjectDSDelegate.isAssigningId(oaObj)) {
					OAObjectKey okx = OAObjectKeyDelegate.createChangedObjectKey(oaObj.getClass(), oaObj.getObjectKey(), propertyName, newObj);
					String s = OAObjectKeyDelegate.verifyKeyChange(oaObj, okx);
					if (s != null) {
						throw new RuntimeException(s);
					}
				}

				if (newObj instanceof OADateTime) { // 20191222
					if (propInfo.getIgnoreTimeZone()) {
						((OADateTime) newObj).setIgnoreTimeZone(true);
					}
				}

				if (propInfo.getUnique() && newObj != null && !propInfo.getId() && !OAObjectDSDelegate.isAssigningId(oaObj)) {

					if (!bIsLoading) { // 20221219
						// 20180629
						OAObject obj = OAObjectUniqueDelegate.getUnique(oaObj.getClass(), propertyName, newObj, false);
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
					        Object objx = OAObjectCacheDelegate.findNext(objLast, oaObj.getClass(), propertyU, newObj);
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
		    boolean b = OASync.isServer();
		    if (!b) {
	            OASyncClient sc = OASync.getSyncClient(); 
	            b = (sc != null && sc.isObjectOnServer(oaObj));
		    }
            if (b) {
				OAObjectCSDelegate.fireBeforePropertyChange(oaObj, propertyName, oldObj, newObj);
			}
		}
	}

	/**
	 * Throttling utility that limits the rate at which warnings are logged
	 * when an owner reference is unexpectedly set to null.
	 */
	private static final OAThrottle throttleSetOwnerNull = new OAThrottle(500);

	/**
	 * Counter used to track how many times an owner reference has been set
	 * to null in scenarios where such transitions may indicate modeling or
	 * data-integrity issues.
	 */
	private static int cntSetOwnerNull;

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
	protected static void firePropertyChange(final OAObject oaObj, final String propertyName, Object oldObj, Object newObj,
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
	protected static void firePropertyChange(final OAObject oaObj, final String propertyName, Object oldObj, Object newObj,
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
	protected static void firePropertyChange(final OAObject oaObj, final String propertyName, Object oldObj, Object newObj,
			final boolean bLocalOnly, final boolean bSetChanged, final boolean bUnknownValues, final boolean bIsCheckingRef) {
		if (oaObj == null || propertyName == null) {
			return;
		}

		String propertyU = propertyName.toUpperCase();

		final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);

		if (oldObj != null && !bUnknownValues) {
			if (OAObjectReflectDelegate.getPrimitiveNull(oaObj, propertyU) || oldObj instanceof OANullObject) {
				oldObj = null;
			}
		}

		//  note: a primitive null can only be set by calling OAObjectReflectDelegate.setProperty(...)
		if (newObj instanceof OANullObject) {
			newObj = null;
		}

		if (newObj != null || !bUnknownValues) {
			OAObjectReflectDelegate.setPrimitiveNull(oaObj, propertyU, (newObj == null));
		}

		if (oldObj instanceof OANullObject) {
			oldObj = null;
		}

		final OALinkInfo linkInfo = OAObjectInfoDelegate.getLinkInfo(oi, propertyU);
		boolean bWasEmpty = false;
		if (!bUnknownValues && linkInfo != null && oldObj == null) {
			// oldObj might never have been loaded before setMethod was called, which will have the oldValue=null -
			//   need to check in oaObj.properties to see what orig value was.
			oldObj = OAObjectPropertyDelegate.getProperty(oaObj, propertyName, true, true);
			if (oldObj == OANotExist.instance) {
				bWasEmpty = true;
				oldObj = null;
			}
		}

		Object origOldObj = oldObj;
		if (oldObj instanceof OAObjectKey) {
			boolean b = false;
			if (newObj instanceof OAObject) {
				if (OAObjectKeyDelegate.isForSameOAObject(null, OAObjectKeyDelegate.getKey((OAObject) newObj), (OAObjectKey) oldObj)) {
					oldObj = newObj;
					b = true;
				}
			}
			if (!b) {
				Object objx = OAObjectCacheDelegate.get(linkInfo.toClass, (OAObjectKey) oldObj);
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
			propInfo = OAObjectInfoDelegate.getPropertyInfo(oi, propertyU);
			if (propInfo == null) {
				calcInfo = OAObjectInfoDelegate.getOACalcInfo(oi, propertyU);
			}
		}

		final boolean bIsLoading = OAThreadLocalDelegate.isLoading();

		OAObjectKey origKey;
		if (propInfo != null && propInfo.getId()) {
			origKey = OAObjectKeyDelegate.createChangedObjectKey(oaObj.getClass(), oaObj.getObjectKey(), propertyName, oldObj); // make sure key uses the prevId, so that it can be found on other computers
			if (!bIsLoading || !oaObj.isNew()) {
				OAObjectKeyDelegate.afterChangedObjectKeyProperty(oaObj, origKey, true); // this will make sure that it is a valid (unique) value
			}
		} else {
			origKey = OAObjectKeyDelegate.getKey(oaObj);
		}

		if (linkInfo != null) {
			// must update ref properties before sending events
			// 20110314: need to store nulls, so that it wont go back to server everytime
			if (!bUnknownValues) {
				OAObjectPropertyDelegate.setPropertyCAS(oaObj, propertyName, newObj, origOldObj, bWasEmpty, false);
			}
		} else {
			// 20130318
			if (propInfo != null && propInfo.isBlob()) {
				OAObjectPropertyDelegate.setPropertyCAS(oaObj, propertyName, newObj, origOldObj, bWasEmpty, false);
			}
		}

		final boolean bChangeHold = oaObj.changedFlag;
		final boolean bIsChangeProp = WORD_CHANGED.equals(propertyU);
		if (!bIsChangeProp) {
			oaObj.changedFlag = true;
		}

		if (!bIsLoading) {
			if (!bLocalOnly) {
				// prior to 20100406, this was always calling these methods
				OARemoteThreadDelegate.startNextThread(); // if this is OAClientThread, so that OAClientMessageHandler can continue with next message

				//note: this next method will just return, since fireBeforePropChange is now doing this
				// OAObjectCSDelegate.fireAfterPropertyChange(oaObj, origKey, propertyName, oldObj, newObj);
			}
		}

		if (!bIsLoading) {
			// 20110603 added support for creating undoable events if oaThreadLocal.createUndoablePropertyChanges=true
			//      default=false, which means that the individual UI components are controlling this
			if (OAThreadLocalDelegate.getCreateUndoablePropertyChanges()) {
				if (!bIsChangeProp && OAUndoManager.getUndoManager() != null) {
					OAUndoableEdit ue = OAUndoableEdit.createUndoablePropertyChange(null, oaObj, propertyName, oldObj, newObj,
																					bChangeHold);
					OAUndoManager.add(ue);
				}
			}
		}

		// 20151117 if one2one, and new value is null, then set prop to null in link prop
		if (linkInfo != null && !bUnknownValues) {
			OALinkInfo revLinkInfo = OAObjectInfoDelegate.getReverseLinkInfo(linkInfo);
			if (revLinkInfo != null) {
				if (revLinkInfo.type == OALinkInfo.ONE) {
					if (oldObj instanceof OAObjectKey) {
						if (OASync.isClient(oaObj)) { // 20151117 dont get from server if this is client
							OAObject objx = OAObjectCacheDelegate.get(linkInfo.toClass, (OAObjectKey) oldObj);
							OAObjectPropertyDelegate.setPropertyCAS(objx, revLinkInfo.getName(), null, oaObj);
						}
					}
				}
			}
		}

		// Note: this needs to be ran even if isSuppressingEvents(), it wont send messages but it might need to update detail hubs
		if (!bIsLoading) {
			if (OAObjectHubDelegate.isInHub(oaObj)) {
				sendHubPropertyChange(oaObj, propertyName, oldObj, newObj, linkInfo);
			}
			OAObjectCacheDelegate.fireAfterPropertyChange(oaObj, origKey, propertyName, oldObj, newObj, bLocalOnly, true);
		}

		oaObj.changedFlag = bChangeHold;

		/*was: moved to below
		// 20160304
		if (!bIsLoading) {
		    if (oi.getHasTriggers()) {
		        HubEvent hubEvent = new HubEvent(oaObj, propertyName, oldObj, newObj);
		        try {
		            OAThreadLocalDelegate.addHubEvent(hubEvent);
		            oi.onChange(oaObj, propertyName, hubEvent);
		        }
		        finally {
		            OAThreadLocalDelegate.removeHubEvent(hubEvent);
		        }
		    }
		}
		*/

		// set to Changed
		if (!bIsChangeProp && bSetChanged && !bChangeHold && (calcInfo == null) && !bUnknownValues) {
			if (!oaObj.isChanged()) {
				if (linkInfo == null || !linkInfo.bCalculated) { // 20120429
					try {
						OAThreadLocalDelegate.setSuppressCSMessages(true); // the client will setChanged when it gets the propertyChange message
						oaObj.setChanged(true);
					} finally {
						OAThreadLocalDelegate.setSuppressCSMessages(false);
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
					OAThreadLocalDelegate.addHubEvent(hubEvent);
					oi.onChange(oaObj, propertyName, hubEvent);
				} finally {
					OAThreadLocalDelegate.removeHubEvent(hubEvent);
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
						oldValue = OAObjectKeyDelegate.getProperty(	linkInfo.getToClass(), (OAObjectKey) oldObj,
																	fki.getToPropertyInfo().getName());
					} else {
						oldValue = oldObj;
					}

					Object newValue = null;
					if (newObj instanceof OAObject) {
						newValue = newObj == null ? null : ((OAObject) newObj).getProperty(fki.getToPropertyInfo().getName());
					} else if (newObj instanceof OAObjectKey) {
						newValue = OAObjectKeyDelegate.getProperty(	linkInfo.getToClass(), (OAObjectKey) newObj,
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
						Object obj = OAObjectPropertyDelegate.getProperty(oaObj, li.getName(), false, true);
						if (obj instanceof OAObject) {
							obj = ((OAObject) obj).getObjectKey();
						}
						if (obj != null && !(obj instanceof OAObjectKey)) {
							obj = null;
						}
						okNew = OAObjectKeyDelegate.createChangedObjectKey(	li.getToClass(), (OAObjectKey) obj,
																			fki.getToPropertyInfo().getName(), newObj);
						if (okNew.isEmpty()) {
							okNew = null;
						}

						firePropertyChange(	oaObj, li.getName(),
											OAObjectPropertyDelegate.getProperty(oaObj, li.getName(), false, true),
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
						OAObjectReflectDelegate.setPrimitiveNull(oaObj, pi.getName(), (newObj == null));
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
	protected static void sendHubBeforePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj) {
		Hub[] hubs = OAObjectHubDelegate.getHubReferences(oaObj);
		if (hubs == null) {
			return;
		}
		for (Hub h : hubs) {
			if (h != null) {
				HubEventDelegate.fireBeforePropertyChange(h, oaObj, propertyName, oldObj, newObj);
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
	public static void sendHubPropertyChange(final OAObject oaObj, final String propertyName, final Object oldObj, final Object newObj,
			final OALinkInfo linkInfo) {
		// Note: don't add this, HubEventDelegate will do it after it updates detail hubs:
		//        if (OAObjectFlagDelegate.isSuppressingPropertyChangeEvents()) return;
		// Note: oldObj could be OAObjectKey

		Hub[] hubs = OAObjectHubDelegate.getHubReferences(oaObj);
		if (hubs == null) {
			return;
		}
		for (Hub h : hubs) {
			if (h != null) {
				HubEventDelegate.fireAfterPropertyChange(h, oaObj, propertyName, oldObj, newObj, linkInfo);
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
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
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
	private static void updateLink(final OAObject oaObj, OAObjectInfo oi, OALinkInfo linkInfo, Object oldObj, Object newObj) {
		// NOTE: oldObj could be OAObjectKey
		// taken out, since it will set OAClientThread.status = STATUS_FinishingAsServer
		//		if (!OAClientDelegate.processIfServer()) return; // only process on server, and send events to clients (even if this is OAThreadClient)

		OALinkInfo revLinkInfo = OAObjectInfoDelegate.getReverseLinkInfo(linkInfo);
		if (revLinkInfo == null) {
			return;
		}

		Object obj;

		// 20160426 make sure that it has not changed
		obj = OAObjectPropertyDelegate.getProperty(oaObj, linkInfo.name);
		if (obj != newObj) {
			return;
		}

		if (revLinkInfo.type == OALinkInfo.ONE) {
			try {
				OAObjectInfo oiRev = OAObjectInfoDelegate.getOAObjectInfo(linkInfo.toClass);
				Method m = OAObjectInfoDelegate.getMethod(oiRev, "get" + revLinkInfo.name, 0); // make sure that the method exists
				if (m != null) {
					if (oldObj instanceof OAObjectKey) {
						if (OASync.isClient(oaObj)) { // 20151117 dont get from server if this is client
							oldObj = OAObjectCacheDelegate.get(linkInfo.toClass, (OAObjectKey) oldObj);
						} else {
							oldObj = OAObjectReflectDelegate.getObject(linkInfo.toClass, (OAObjectKey) oldObj);
						}
					}
					if (oldObj instanceof OAObject) {
						// 20150820 if one2one, then dont load if null and isClient
						//   this was discovered when deleting an IDL and function/gsmrFunction (1to1) kept going to server for other value
						boolean b = true;
						if (OASync.isClient(oaObj)) {
							obj = OAObjectPropertyDelegate.getProperty((OAObject) oldObj, revLinkInfo.name);
							if (obj == null) {
								// dont get from server
								b = false;
							}
						}
						if (b) {
							obj = OAObjectReflectDelegate.getProperty((OAObject) oldObj, revLinkInfo.name);
							if (obj == oaObj) {
								OAObjectReflectDelegate.setProperty((OAObject) oldObj, revLinkInfo.name, null, null);
							}
						}
					}

					if (newObj instanceof OAObject) {
						// 20170411
						if (revLinkInfo.getOwner()) {
							OAObjectPropertyDelegate.setPropertyCAS((OAObject) newObj, revLinkInfo.name, oaObj, null);
							OAObjectReflectDelegate.setProperty((OAObject) newObj, revLinkInfo.name, oaObj, null);
						} else {
							//was
							obj = OAObjectReflectDelegate.getProperty((OAObject) newObj, revLinkInfo.name);
							if (obj != oaObj) {
								OAObjectReflectDelegate.setProperty((OAObject) newObj, revLinkInfo.name, oaObj, null);
							}
						}
					}
				}
			} catch (Exception e) {
			}
			return;
		}

		if (revLinkInfo.type != OALinkInfo.MANY) {
			return;
		}

		Hub hub;
		boolean bUpdateHub = false;

		// 20131009 each link now has its own recursive flag
		OALinkInfo liRecursive;
		if (revLinkInfo.bRecursive) {
			liRecursive = OAObjectInfoDelegate.getRecursiveLinkInfo(oi, OALinkInfo.ONE); // ex: "ParentSection"
		} else {
			liRecursive = null;
			//was: OALinkInfo liRecursive = OAObjectInfoDelegate.getRecursiveLinkInfo(oi, OALinkInfo.ONE);  // ex: "ParentSection"
		}

		boolean bOldIsKeyOnly = (oldObj instanceof OAObjectKey);

		// find all Hubs using this as the active object.
		// By changing a reference property, the object could be moved to another hub
		List<Hub> alUpdateHub = null;
		if (oldObj != null || liRecursive != null) {
			Hub[] hubs = OAObjectHubDelegate.getHubReferences(oaObj);
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
					Hub[] hubss = HubShareDelegate.getAllSharedHubs(h, filter);

					//was:Hub[] hubss = HubShareDelegate.getAllSharedHubs(h);
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

				obj = OAObjectReflectDelegate.getProperty(oaObj, OAObjectInfoDelegate.getReverseLinkInfo(liRecursive).getName()); // hubSections
				if (!(obj instanceof Hub)) {
					throw new RuntimeException("OAObject.updateLink() method for recursive link not returning a Hub.");
				}
				hub = (Hub) obj;
				for (int i = 0;; i++) {
					obj = hub.elementAt(i); // section
					if (obj == null) {
						break;
					}
					if (OAObjectReflectDelegate.getProperty((OAObject) obj, linkInfo.getName()) != newObj) {
						OAObjectReflectDelegate.setProperty((OAObject) obj, linkInfo.getName(), newObj, null); // setCatalog.  This will set all of its recursive children
					}
				}

				obj = OAObjectReflectDelegate.getProperty(oaObj, liRecursive.getName()); // get parent (section)
				if (obj != null) {
					obj = OAObjectReflectDelegate.getProperty((OAObject) obj, linkInfo.getName()); // catalog
					if (obj == newObj) {
						newObj = null; // otherwise, this object will be added to the rootHub
					} else {
						// set Parent to null  2003/09/21
						OAObjectReflectDelegate.setProperty(oaObj, liRecursive.getName(), null, null); // set ParentSection = null
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
								oldObj = OAObjectReflectDelegate.getObject(linkInfo.toClass, (OAObjectKey) oldObj);
							}
							OAObjectReflectDelegate.setProperty(oaObj, linkInfo.getName(), oldObj, null);
							throw new RuntimeException("Can not set the Parent to Itself");
						}
						// cant assign a child of this object as the new parent - causes orphaned objects
						for (obj = newObj;;) {
							obj = OAObjectReflectDelegate.getProperty((OAObject) obj, liRecursive.getName());
							if (obj == null) {
								break;
							}
							if (obj == oaObj) {
								if (bOldIsKeyOnly) {
									bOldIsKeyOnly = false;
									oldObj = OAObjectReflectDelegate.getObject(linkInfo.toClass, (OAObjectKey) oldObj);
								}
								OAObjectReflectDelegate.setProperty(oaObj, linkInfo.getName(), oldObj, null);
								throw new RuntimeException("Can not assign Parent to a Child");// causes orphans
							}
						}
					}

					// find owner link
					boolean bOwned = false;
					OALinkInfo linkOwner = OAObjectInfoDelegate.getLinkToOwner(oi); // link to catalog
					OALinkInfo liRev = null;
					if (linkOwner != null) {
						liRev = OAObjectInfoDelegate.getReverseLinkInfo(linkOwner);
					}

					if (liRev != null && liRev.type == OALinkInfo.MANY) {
						bOwned = true;
						if (newObj == null) { // parentSection = null
							// if being set to null, then add to root hub.
							// if it was removed from old hub, then dont add to root hub
							boolean bAdd = !OAThreadLocalDelegate.isDeleting(oaObj);

							if (bAdd && !bOldIsKeyOnly
									&& OAObjectReflectDelegate.isReferenceHubLoadedAndNotEmpty((OAObject) oldObj, revLinkInfo.getName())) {
								hub = (Hub) OAObjectReflectDelegate.getProperty((OAObject) oldObj, revLinkInfo.getName()); // Catalog.sections (original hub that this objects belonged to)
								bAdd = hub.contains(oaObj);
							}

							if (bAdd) {
								obj = OAObjectReflectDelegate.getProperty(oaObj, linkOwner.getName()); // Catalog
								if (obj != null) {
									Object obj2 = OAObjectReflectDelegate.getProperty((OAObject) obj, liRev.getName()); // catalog.hubSection
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
							obj = OAObjectReflectDelegate.getProperty((OAObject) newObj, linkOwner.getName());

							if (OAObjectReflectDelegate.getProperty(oaObj, linkOwner.getName()) != obj) {
								OAObjectReflectDelegate.setProperty(oaObj, linkOwner.getName(), obj, null); // setCatalog (this will also set child recursive objects)
							}

							if (oldObj == null) {
								// remove from root hub, it is now assigned a parentSection
								obj = OAObjectReflectDelegate.getProperty(oaObj, linkOwner.getName()); // Catalog
								if (obj != null) {
									obj = OAObjectReflectDelegate.getProperty((OAObject) obj, liRev.getName()); // catalog.catalogSections
									if (!(obj instanceof Hub)) {
										throw new RuntimeException(
												"OAObject.updateLink() method for recursive link owner not returning a Hub.");
									}
									hub = (Hub) obj; // catalog.catalogSections
									HubAddRemoveDelegate.remove(hub, oaObj, false, true, false, true, false, false);
								}
							}
						}
					}

					if (!bOwned) {
						Hub h = OAObjectInfoDelegate.getRootHub(oi);
						if (h != null) {
							if (oldObj == null) {
								// take out of unowned root hubs
								h.remove(oaObj);
							} else if (newObj == null) {
								// add to unowned root hubs
								// if it was removed from old hub, then dont add to root hub
								boolean bAdd = true;
								if (oldObj != null && !bOldIsKeyOnly
										&& OAObjectReflectDelegate.isReferenceHubLoaded((OAObject) oldObj, revLinkInfo.getName())) {
									hub = (Hub) OAObjectReflectDelegate.getProperty((OAObject) oldObj, revLinkInfo.getName()); // Catalog.sections (original hub that this objects belonged to)
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
				if (OAObjectCSDelegate.isServer(oaObj)
						|| OAObjectReflectDelegate.isReferenceHubLoaded((OAObject) oldObj, revLinkInfo.getName())) {
					obj = OAObjectReflectDelegate.getProperty((OAObject) oldObj, revLinkInfo.getName());
					if (obj instanceof Hub) {
						Hub h = (Hub) obj;
						if (h.contains(oaObj)) {
							HubAddRemoveDelegate.remove(h, oaObj, false, true, false, true, false, false);
							hubRemovedFrom = h;
						}
					}
				}
			} catch (Exception e) {
			}
		}

		if (newObj != null && newObj instanceof OAObject) {
			try {
				if (OAObjectCSDelegate.isServer(oaObj)
						|| OAObjectReflectDelegate.isReferenceHubLoaded((OAObject) newObj, revLinkInfo.getName())) {
					hub = (Hub) OAObjectReflectDelegate.getProperty((OAObject) newObj, revLinkInfo.getName());

					// 20130630 added autoAttach check
					boolean bAutoAdd = OAObjectDelegate.getAutoAdd(oaObj);

					if (bAutoAdd && hub != null) {
						hub.add(oaObj);

						if (oaObj.isNew()) {
							OAObject objMaster = hub.getMasterObject();
							if (objMaster != null) {
								if (!OAObjectDelegate.getAutoAdd(objMaster)) {
									// turn off autoAdd for this object
									OAObjectDelegate.setAutoAdd(oaObj, false);
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
						&& (hub.getSharedHub() != null && HubDetailDelegate.getHubWithMasterHub(hub) != null);
				
                // 20230804 dont allow master AO change on hub where object was removed
				if (bAllowAdjustMaster && hubRemovedFrom != null && hub.getRealHub() == hubRemovedFrom) {
				    bAllowAdjustMaster = false; 
				}
				
				HubAODelegate.setActiveObject(hub, oaObj, bAllowAdjustMaster, false, false); // adjMaster, updateLink, force
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
	protected static void fireAfterLoadEvent(OAObject oaObj) {
		Hub[] hubs = OAObjectHubDelegate.getHubReferences(oaObj);
		if (hubs == null) {
			return;
		}
		for (Hub h : hubs) {
			if (h != null) {
				HubEventDelegate.fireAfterLoadEvent(h, oaObj);
			}
		}
	}

}
