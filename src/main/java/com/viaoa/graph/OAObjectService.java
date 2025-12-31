package com.viaoa.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.graph.object.*;
import com.viaoa.hub.Hub;
import com.viaoa.object.OACallback;
import com.viaoa.object.OACascade;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInternalBridge;
import com.viaoa.object.OAObjectEventDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.util.OACompare;

public class OAObjectService {
	private static final Logger LOG = Logger.getLogger(OAObjectService.class.getName());

	private final OAGraph graph;

	private final OAObjectInternalBridge faBridge = new OAObjectInternalBridge();
	
    private final OAObjectCacheService srvcCache = new OAObjectCacheService(this, faBridge.getObjectFriendAccess());
    private final OAObjectInitializeService srvcOAObjectInitialize = new OAObjectInitializeService(this, faBridge.getObjectFriendAccess()); 
    private final OAObjectGuidService srvcGuid = new OAObjectGuidService(this, faBridge.getObjectFriendAccess());
    private final OAObjectInfoService srvcOAObjectInfo = new OAObjectInfoService(this, faBridge.getObjectFriendAccess(), faBridge.getObjectInfoFriendAccess()); 
    private final OAObjectPropertyService srvcOAObjectProperty = new OAObjectPropertyService(this, faBridge.getObjectFriendAccess());
    private final OAObjectDSService srvcOAObjectDS = new OAObjectDSService(this, faBridge.getObjectFriendAccess());
    private final OAObjectReflectService srvcOAObjectReflect = new OAObjectReflectService(this, faBridge.getObjectFriendAccess());
    private final OAObjectHubService srvcOAObjectHub = new OAObjectHubService(this, faBridge.getObjectFriendAccess());
    private final OAObjectAnnotationService srvcOAObjectAnnotation = new OAObjectAnnotationService(this, faBridge.getObjectFriendAccess(), faBridge.getObjectInfoFriendAccess());
    private final OAObjectDatabaseService srvcOAObjectDatabase = new OAObjectDatabaseService(this, faBridge.getObjectFriendAccess());
    private final OAObjectCallbackService srvcOAObjectCallback = new OAObjectCallbackService(this, faBridge.getObjectFriendAccess());
    private final OAObjectCSService srvcOAObjectCS = new OAObjectCSService(this, faBridge.getObjectFriendAccess());
    private final OAObjectDeleteService srvcOAObjectDelete = new OAObjectDeleteService(this, faBridge.getObjectFriendAccess());
    private final OAObjectEmptyHubService srvcOAObjectEmptyHub = new OAObjectEmptyHubService(this, faBridge.getObjectFriendAccess());
    private final OAObjectEnumService srvcOAObjectEnum = new OAObjectEnumService(this, faBridge.getObjectFriendAccess());
    private final OAObjectEventService srvcOAObjectEvent = new OAObjectEventService(this, faBridge.getObjectFriendAccess());
    private final OAObjectImportMatchService srvcOAObjectImportMatch = new OAObjectImportMatchService(this, faBridge.getObjectFriendAccess());
    private final OAObjectKeyService srvcOAObjectKey = new OAObjectKeyService(this, faBridge.getObjectFriendAccess(), faBridge.getObjectInfoFriendAccess());
    private final OAObjectLockService srvcOAObjectLock = new OAObjectLockService(this, faBridge.getObjectFriendAccess());
    private final OAObjectLogService srvcOAObjectLog = new OAObjectLogService(this, faBridge.getObjectFriendAccess());
    private final OAObjectSaveService srvcOAObjectSave = new OAObjectSaveService(this, faBridge.getObjectFriendAccess());
    private final OAObjectSchedulerService srvcOAObjectScheduler = new OAObjectSchedulerService(this, faBridge.getObjectFriendAccess());
    private final OAObjectUniqueService srvcOAObjectUnique = new OAObjectUniqueService(this, faBridge.getObjectFriendAccess());
    private final OAObjectSerializeService srvcOAObjectSerialize = new OAObjectSerializeService(this, faBridge.getObjectFriendAccess(), faBridge.getObjectSerializerFriendAccess());
    private final OAObjectSiblingService srvcOAObjectSibling = new OAObjectSiblingService(this, faBridge.getObjectFriendAccess());
    
	/**
	 * Reserved property name representing an object's "new" lifecycle state.
	 */
	public static final String WORD_New = "NEW";

	/**
	 * Reserved property name representing an object's "changed" lifecycle state.
	 */
	public static final String WORD_Changed = "CHANGED";
	
	/**
	 * Reserved property name representing an object's "deleted" lifecycle state.
	 */
	public static final String WORD_Deleted = "DELETED";
	
	/**
	 * Reserved property name representing whether auto-add behavior is enabled
	 * for reverse-link insertion.
	 */
	public static final String WORD_AutoAdd = "AutoAdd";

	/**
	 * Shared Boolean constant used when firing lifecycle-related property-change
	 * events.
	 */
	public static final Boolean TRUE = Boolean.TRUE;
	
	/**
	 * Shared Boolean constant used when firing lifecycle-related property-change
	 * events.
	 */
	public static final Boolean FALSE = Boolean.FALSE;
    
    
	public OAObjectService(OAGraph graph) {
    	if (graph == null) throw new IllegalArgumentException("graph can not be null");
    	this.graph = graph;
	}
	
    public OAGraph graph() {
    	return graph;
    }

    public OAObjectInitializeService getOAObjectInitializeService() {
    	return srvcOAObjectInitialize;
    }

    public OAObjectGuidService getOAObjectGuidService() {
    	return srvcGuid;
    }
    
    public OAObjectCacheService getOAObjectCacheService() { 
    	return srvcCache; 
    }

    

    public OAObjectInfoService getOAObjectInfoService() {
    	return srvcOAObjectInfo;
    }

    public OAObjectPropertyService getOAObjectPropertyService() {
    	return srvcOAObjectProperty;
    }
    
    public OAObjectHubService getOAObjectHubService() {
    	return srvcOAObjectHub;
    }

    public OAObjectDSService getOAObjectDSService() {
    	return srvcOAObjectDS;
    }

    public OAObjectAnnotationService getOAObjectAnnotationService() {
    	return srvcOAObjectAnnotation;
    }

    public OAObjectDatabaseService getOAObjectDatabaseService() {
    	return srvcOAObjectDatabase;
    }

    public OAObjectCallbackService getOAObjectCallbackService() {
    	return srvcOAObjectCallback;
    }
    
    public OAObjectReflectService getOAObjectReflectService() {
    	return srvcOAObjectReflect;
    }

    public OAObjectCSService getOAObjectCSService() {
    	return srvcOAObjectCS;
    }
    
    public OAObjectDeleteService getOAObjectDeleteService() {
    	return srvcOAObjectDelete;
    }

    public OAObjectEmptyHubService getOAObjectEmptyHubService() {
    	return srvcOAObjectEmptyHub;
    }

    public OAObjectEnumService getOAObjectEnumService() {
    	return srvcOAObjectEnum;
    }

    public OAObjectEventService getOAObjectEventService() {
    	return srvcOAObjectEvent;
    }

    public OAObjectImportMatchService getOAObjectImportMatchService() {
    	return srvcOAObjectImportMatch;
    }

    public OAObjectKeyService getOAObjectKeyService() {
    	return srvcOAObjectKey;
    }

    public OAObjectLockService getOAObjectLockService() {
    	return srvcOAObjectLock;
    }

    public OAObjectLogService getOAObjectLogService() {
    	return srvcOAObjectLog;
    }

    public OAObjectSaveService getOAObjectSaveService() {
    	return srvcOAObjectSave;
    }
    
    public OAObjectSchedulerService getOAObjectSchedulerService() {
    	return srvcOAObjectScheduler;
    }

    public OAObjectUniqueService getOAObjectUniqueService() {
    	return srvcOAObjectUnique;
    }

    public OAObjectSerializeService getOAObjectSerializeService() {
    	return srvcOAObjectSerialize;
    }
    
    public OAObjectSiblingService getOAObjectSiblingService() {
    	return srvcOAObjectSibling;
    }
    
	/**
	 * Updates the {@code newFlag} of the specified {@link OAObject} and fires the
	 * corresponding before/after property-change events for the reserved property
	 * name {@code "NEW"}.
	 *
	 * <p>This method controls the object's lifecycle state with respect to creation
	 * and persistence. When the flag transitions from {@code true} to {@code false},
	 * automatic reverse-link insertion is enabled so that the object can be added to
	 * owning Hub relationships when applicable.</p>
	 *
	 * <h3>Behavior</h3>
	 * <ul>
	 *   <li>Ignores the call if the requested value equals the current value.</li>
	 *   <li>Fires a {@code beforePropertyChange} event with the old and new values.</li>
	 *   <li>Updates the internal {@code newFlag} field.</li>
	 *   <li>Fires an {@code afterPropertyChange} event.</li>
	 *   <li>If switching from new → not-new, invokes {@link #setAutoAdd(OAObject, boolean)}
	 *       to enable automatic reverse-link population.</li>
	 * </ul>
	 *
	 * @param oaObj the object whose new-state is being modified; may be {@code null}.
	 * @param b {@code true} to mark the object as newly created,
	 *          {@code false} to clear the new-state flag.
	 */
	public void setNew(final OAObject oaObj, final boolean b) {
		boolean old = faBridge.getObjectFriendAccess().getNewFlag(oaObj);
		if (b == old) {
			return;
		}
		OAObjectEventDelegate.fireBeforePropertyChange(oaObj, WORD_New, old ? TRUE : FALSE, b ? TRUE : FALSE, false, false);

		faBridge.getObjectFriendAccess().setNew(oaObj, b);
		
		OAObjectEventDelegate.firePropertyChange(oaObj, WORD_New, old ? TRUE : FALSE, b ? TRUE : FALSE, false, false);
		if (!b) {
			setAutoAdd(oaObj, true);
		}
	}
    
	/**
	 * Convenience method that determines whether the specified {@link OAObject} is
	 * considered changed according to the supplied rule. This method allocates a
	 * new {@link OACascade} instance and delegates to
	 * {@link #getChanged(OAObject, int, OACascade)}.
	 *
	 * @param oaObj       the object to evaluate; may be {@code null}.
	 * @param iCascadeRule the rule controlling change evaluation.
	 * @return {@code true} if the object or any related object is considered
	 *         changed; otherwise {@code false}.
	 */
	public boolean getChanged(OAObject oaObj, int iCascadeRule) {
		if (oaObj == null) return false;
		if (iCascadeRule == OAObject.CASCADE_NONE) {
			OAObject.FriendAccess fa = faBridge.getObjectFriendAccess();
			return (fa.getChangedFlag(oaObj) || fa.getNewFlag(oaObj));
		}
		OACascade cascade = new OACascade();
		boolean b = getChanged(oaObj, iCascadeRule, cascade);
		return b;
	}

	
	/**
	 * Determines whether the specified {@link OAObject} is considered changed based
	 * on the supplied cascade rule and {@link OACascade} context. This variant is
	 * used when change detection must be coordinated with an active cascade
	 * operation, ensuring that objects are not visited more than once during a
	 * recursive evaluation.
	 *
	 * <p>If the object is {@code null}, the method returns {@code false}. Otherwise,
	 * the object's change status is evaluated according to the cascade rule:</p>
	 *
	 * <ul>
	 *   <li><b>OAObjectInfo.CHANGED_NONE</b>  
	 *       Always returns {@code false}.</li>
	 *
	 *   <li><b>OAObjectInfo.CHANGED_LOCAL</b>  
	 *       Returns the object's own {@code changedFlag} value.</li>
	 *
	 *   <li><b>OAObjectInfo.CHANGED_ALL</b>  
	 *       Performs a recursive scan of related objects using the provided
	 *       {@link OACascade} instance to track visited objects and prevent loops.</li>
	 *
	 *   <li><b>Depth-based rules</b>  
	 *       Interprets {@code iCascadeRule} as a maximum recursion depth and checks
	 *       linked objects up to that depth.</li>
	 * </ul>
	 *
	 * <p>The recursion is delegated to
	 * {@link #getChanged(OAObject, int, int, OALinkInfo[])} after the cascade context
	 * registers the root object to ensure it is not revisited. If any reachable
	 * object is marked changed, the method returns {@code true}; otherwise it
	 * returns {@code false}.</p>
	 *
	 * @param oaObj the object to evaluate; may be {@code null}.
	 * @param iCascadeRule the rule controlling how far recursive change detection
	 *                     should propagate.
	 * @param cascade the active {@link OACascade} used to record visited objects and
	 *                prevent infinite recursion.
	 * @return {@code true} if the object or any reachable related object is changed
	 *         according to the rule; {@code false} otherwise.
	 */
	public boolean getChanged(final OAObject oaObj, int iCascadeRule, OACascade cascade) {
		if (oaObj == null) return false;
		
		OAObject.FriendAccess fa = faBridge.getObjectFriendAccess();
		if (fa.getChangedFlag(oaObj)) return true;
		if (fa.getNewFlag(oaObj)) return true;

		if (iCascadeRule == oaObj.CASCADE_NONE) {
			return false;
		}
		if (cascade.wasCascaded(oaObj, true)) {
			return false;
		}

		if (fa.getProperties(oaObj) == null) return false;

		// check link cascade objects
		OAObjectInfo oi = getOAObjectInfoService().getOAObjectInfo(oaObj);
		List al = oi.getLinkInfos();
		for (int i = 0; i < al.size(); i++) {
			OALinkInfo li = (OALinkInfo) al.get(i);
			String prop = li.getName();
			if (prop == null || prop.length() < 1) {
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

			// same as OAObjectSaveDelegate.cascadeSave()
			if (getOAObjectReflectService().isReferenceNullOrNotLoaded(oaObj, prop)) {
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

			if (OAObjectInfoDelegate.isMany2Many(li)) {
				Hub hub = (Hub) getOAObjectReflectService().getRawReference(oaObj, prop);
				if (graph.hubs().getChanged(hub, OAObject.CASCADE_NONE, cascade)) {
					return true;
				}
			}
			
			if (!bValidCascade) {
				continue;
			}

			Object obj = getOAObjectReflectService().getProperty(oaObj, li.getName()); // if Hub with Keys, then this will load the correct objects to check
			if (obj == null) {
				continue;
			}

			if (obj instanceof Hub) {
				if (getOAObjectHubService().getChanged((Hub) obj, iCascadeRule, cascade)) {
					return true; //  if there have been adds/removes to hub
				}
			} else {
				if (obj instanceof OAObject) { // 20110420 could be OANullObject
					if (getChanged((OAObject) obj, iCascadeRule, cascade)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Convenience method that initiates a recursive traversal of the object graph
	 * starting from the specified {@link OAObject}. This variant simply allocates a
	 * new {@link OACascade} instance and delegates all traversal logic to
	 * {@link #recurse(OAObject, OACallback, OACascade)}.
	 *
	 * <p>This method exists for callers that do not need to manage or reuse an
	 * {@link OACascade} context. See the cascade-enabled variant for the full
	 * traversal behavior and callback invocation rules.</p>
	 *
	 * @param oaObj the root object to traverse; may be {@code null}.
	 * @param callback the callback invoked for each visited object; must not be {@code null}.
	 */
	public void recurse(OAObject oaObj, OACallback callback) {
		OACascade cascade = new OACascade();
		recurse(oaObj, callback, cascade);
	}

	/**
	 * Recursively traverses the reachable object graph beginning at the specified
	 * {@link OAObject}, invoking the provided {@link OACallback} for the root object
	 * and for each subsequently visited object. The supplied {@link OACascade}
	 * tracks visited objects to ensure each instance is processed at most once and
	 * to prevent infinite loops when cycles exist in the graph.
	 *
	 * <p>If {@code oaObj} is {@code null}, the method returns immediately. Otherwise,
	 * the object is registered with the {@code cascade} and the callback is invoked
	 * for it. The method then retrieves all link relationships from the object's
	 * metadata and recursively visits referenced objects according to the link type:
	 * </p>
	 *
	 * <ul>
	 *   <li><b>One-to-one links</b> — the referenced object is visited if present
	 *       and has not already been processed by the cascade.</li>
	 *   <li><b>One-to-many or many-to-many links</b> — each object in the associated
	 *       hub is visited, again subject to cascade loop-prevention.</li>
	 * </ul>
	 *
	 * <p>The traversal continues until all reachable related objects have been
	 * processed or the cascade prevents further descent. The method performs no
	 * depth limiting; callers wishing to restrict traversal depth must enforce such
	 * behavior externally.</p>
	 *
	 * @param oaObj   the root or current object being processed; may be {@code null}.
	 * @param callback the callback to invoke for each visited object; must not be {@code null}.
	 * @param cascade  the cascade context used to record visited objects and prevent
	 *                 revisiting or infinite recursion; must not be {@code null}.
	 */
	public void recurse(OAObject oaObj, OACallback callback, OACascade cascade) {
		if (cascade.wasCascaded(oaObj, true)) {
			return;
		}

		if (callback != null) {
			callback.updateObject(oaObj);
		}
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);

		List al = oi.getLinkInfos();
		for (int i = 0; i < al.size(); i++) {
			OALinkInfo li = (OALinkInfo) al.get(i);
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

			Object obj = OAObjectReflectDelegate.getProperty(oaObj, li.getName()); // select all
			if (obj == null) {
				continue;
			}

			if (obj instanceof Hub) {
				Hub h = (Hub) obj;
				for (int j = 0;; j++) {
					Object o = h.elementAt(j);
					if (o == null) {
						break;
					}
					if (o instanceof OAObject) {
						recurse((OAObject) o, callback, cascade);
					} else {
						if (callback != null) {
							callback.updateObject(o);
						}
					}
					Object o2 = h.elementAt(j);
					if (o != o2) {
						j--;
					}
				}
			} else {
				if (obj instanceof OAObject) {
					recurse((OAObject) obj, callback, cascade);
				} else {
					if (callback != null) {
						callback.updateObject(obj);
					}
				}
			}
		}
	}

	/**
	 * Searches the object graph beginning at the specified {@link OAObject} for
	 * objects whose property value matches the supplied {@code findValue}, following
	 * the navigation defined by the {@code propertyPath}. This method implements
	 * the full recursive search logic for all {@code find(...)} overloads.
	 *
	 * <p>The {@code propertyPath} is a dot-separated sequence of property or link
	 * names beginning at {@code base}. Each segment may refer to either a simple
	 * property or a relationship link (one-to-one or one-to-many). The method
	 * traverses the path step by step and evaluates the final property value(s)
	 * against the provided {@code findValue}. If {@code bFindAll} is {@code false},
	 * the search stops as soon as the first match is found; otherwise, all matches
	 * reachable along the path are collected.</p>
	 *
	 * <h3>Traversal Behavior</h3>
	 * <ul>
	 *   <li>If {@code base} is {@code null} or the {@code propertyPath} is empty,
	 *       an empty result array is returned.</li>
	 *   <li>The method resolves each segment in the {@code propertyPath} using
	 *       {@link OAPropertyPath} metadata provided by {@code base}'s
	 *       {@link OAObjectInfo}.</li>
	 *   <li>For link segments:
	 *     <ul>
	 *       <li>One-to-one links: the referenced object becomes the next traversal node.</li>
	 *       <li>One-to-many or many-to-many links: each object in the associated hub
	 *           is recursively processed for the remaining path.</li>
	 *     </ul>
	 *   </li>
	 *   <li>For the final segment:
	 *     <ul>
	 *       <li>If it is a property, its value is retrieved via the object's getter.</li>
	 *       <li>A match occurs if {@code findValue == null} and the property value is {@code null},
	 *           or if {@code findValue.equals(propertyValue)} is {@code true}.</li>
	 *     </ul>
	 *   </li>
	 * </ul>
	 *
	 * <h3>Results</h3>
	 * <ul>
	 *   <li>Returns an array of all matching values if {@code bFindAll} is {@code true}.</li>
	 *   <li>Returns a single-element array containing the first match if
	 *       {@code bFindAll} is {@code false}.</li>
	 *   <li>Returns an empty array if no matches are found.</li>
	 * </ul>
	 *
	 * @param base         the root object from which the property path traversal
	 *                     begins; may be {@code null}.
	 * @param propertyPath the dot-separated property or link path to follow; must
	 *                     not be {@code null}.
	 * @param findValue    the value to compare against the resolved property value.
	 * @param bFindAll     if {@code true}, collect all matches; otherwise stop at the first match.
	 * @return an array containing matched values (or objects), never {@code null}.
	 */
	public Object[] find(OAObject base, String propertyPath, Object findValue, boolean bFindAll) {
		if (propertyPath == null || propertyPath.length() == 0) {
			return null;
		}
		StringTokenizer st = new StringTokenizer(propertyPath, ".");
		Object result = base;
		for (; st.hasMoreTokens();) {
			String s = st.nextToken();
			base = (OAObject) result; // previous object
			result = base.getProperty(s);

			if (!st.hasMoreTokens()) {
				// last property, check against findValue
				if (result == findValue || (result != null && OACompare.compare(result, findValue) == 0)) {
					Object[] objs = new Object[] { base };
					return objs;
				}
				return null;
			}

			if (result == null) {
				return null;
			}

			if (result instanceof Hub) {
				String pp = null;
				for (; st.hasMoreTokens();) {
					s = st.nextToken();
					if (pp == null) {
						pp = s;
					} else {
						pp += "." + s;
					}
				}
				ArrayList al = null;
				Hub h = (Hub) result;
				for (int ii = 0;; ii++) {
					Object obj = h.elementAt(ii);
					if (obj == null) {
						break;
					}
					Object[] objs = find((OAObject) obj, pp, findValue, bFindAll);
					if (objs != null) {
						if (!bFindAll) {
							return objs;
						}
						if (al == null) {
							al = new ArrayList(10);
						}
						for (int i3 = 0; i3 < objs.length; i3++) {
							al.add(objs[i3]);
						}
					}
				}
				if (al == null) {
					return null;
				}
				Object[] objs = new Object[al.size()];
				objs = al.toArray(objs);
				return objs;
			}
			if (!(result instanceof OAObject)) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Tracks OAObjects for which automatic reverse-link insertion is disabled.
	 * Presence of a GUID in this map indicates auto-add is turned off.
	 */
	private static final ConcurrentHashMap<Long, Long> hmAutoAdd = new ConcurrentHashMap<Long, Long>();
	
	/**
	 * Enables or disables automatic reverse-link insertion for the specified
	 * {@link OAObject}. When enabled, the object is eligible to be added to
	 * reverse-link hubs when link-one assignments occur.
	 *
	 * <p>Behavior:</p>
	 * <ul>
	 *   <li>If {@code oaObj} is {@code null}, no action is taken.</li>
	 *   <li>Disabling auto-add is ignored if the object is not new.</li>
	 *   <li>Updates the internal auto-add state stored in the {@code hmAutoAdd} map.</li>
	 *   <li>Fires a property-change event for the reserved {@code "AutoAdd"} property.</li>
	 *   <li>When enabling auto-add and the object is not deleted, temporarily
	 *       suppresses client-sync messages and ensures the object is added to any
	 *       applicable reverse-link hubs.</li>
	 * </ul>
	 *
	 * @param oaObj the object whose auto-add behavior is being modified; may be {@code null}.
	 * @param bEnabled {@code true} to enable auto-add; {@code false} to disable it.
	 */
	public void setAutoAdd(final OAObject oaObj, boolean bEnabled) {
		if (oaObj == null) {
			return;
		}
		if (!bEnabled && !oaObj.isNew()) {
			return;
		}

		OAObject.FriendAccess fa = faBridge.getObjectFriendAccess();
		boolean bOld = !hmAutoAdd.containsKey(fa.getGuid(oaObj));
		if (bOld == bEnabled) {
			return;
		}

		long guid = fa.getGuid(oaObj);
		if (!bEnabled) {
			hmAutoAdd.put(guid, guid);
		} else {
			hmAutoAdd.remove(guid);
		}
		OAObjectEventDelegate.firePropertyChange(oaObj, WORD_AutoAdd, bOld ? TRUE : FALSE, bEnabled ? TRUE : FALSE, false, false);

		if (!bEnabled || faBridge.getObjectFriendAccess().getDeleteFlag(oaObj)) {
			return;
		}

		try {
			OAThreadLocalDelegate.setSuppressCSMessages(true);
			// need to see if object should be put into linkOne/masterObject hub(s)
			OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
			for (OALinkInfo li : oi.getLinkInfos()) {
				if (!li.getUsed()) {
					continue;
				}
				if (li.getType() != li.ONE) {
					continue;
				}
				Object objx = OAObjectReflectDelegate.getRawReference(oaObj, li.getName());
				if (!(objx instanceof OAObject)) {
					continue;
				}

				OALinkInfo liRev = OAObjectInfoDelegate.getReverseLinkInfo(li);
				if (liRev == null) {
					continue;
				}
				if (!liRev.getUsed()) {
					continue;
				}
				if (liRev.getType() != li.MANY) {
					continue;
				}
				if (liRev.getPrivateMethod()) {
					continue;
				}

				Object objz = OAObjectReflectDelegate.getProperty((OAObject) objx, liRev.getName());
				if (objz instanceof Hub) {
					((Hub) objz).add(oaObj);
				}
			}
		} finally {
			OAThreadLocalDelegate.setSuppressCSMessages(false);
		}
	}

	/**
	 * Returns whether automatic reverse-link insertion is enabled for the specified
	 * {@link OAObject}. If the object is {@code null}, the method returns
	 * {@code false}.
	 *
	 * <p>This method simply returns the value of the object's internal
	 * {@code autoAddEnabled} flag. It does not evaluate any link relationships or
	 * perform any side effects. The flag determines whether the object should be
	 * automatically inserted into reverse-link Hubs when link assignments occur.</p>
	 *
	 * @param oaObj the object whose auto-add setting is queried; may be {@code null}.
	 * @return {@code true} if automatic reverse-link insertion is enabled,
	 *         {@code false} otherwise.
	 */
	public boolean getAutoAdd(OAObject oaObj) {
		if (oaObj == null) {
			return false;
		}
		return !hmAutoAdd.containsKey(faBridge.getObjectFriendAccess().getGuid(oaObj));
	}

	/**
	 * Convenience method that returns the ID (primary-key) property values of the
	 * specified {@link OAObject}. This method simply delegates to
	 * {@link OAObjectInfoDelegate#getPropertyIdValues(OAObjectInfo, OAObject, String[])}
	 * using the object's {@link OAObjectInfo} metadata.
	 *
	 * <p>If {@code obj} is {@code null}, this method returns {@code null}. Otherwise,
	 * all ID property names defined in the model are resolved through the metadata
	 * and their values are retrieved. For composite keys, all ID components are
	 * returned in the order specified by the model.</p>
	 *
	 * <p>See the delegate method for full details on ID resolution behavior.</p>
	 *
	 * @param obj the object whose ID property values are requested; may be {@code null}.
	 * @return an array of ID values, or {@code null} if {@code obj} is {@code null}.
	 */
	public Object[] getPropertyIdValues(OAObject obj) {
		if (obj == null) return null;
		return OAObjectInfoDelegate.getPropertyIdValues(obj);
	}

	//qqqqqqqqq this was created/added ... needs to be more protected ?? 
	public Object[] getProperties(OAObject obj) {
		if (obj == null) return null;
		return faBridge.getObjectFriendAccess().getProperties(obj);
	}
	
	// flag so that OAObject.finalize should ignore this object.	
	//qqqqqqqqqqqq make sure other code looks for guid=0, and ignore default cleanup (cached, etc)
	public void dontFinalize(OAObject obj) {
		if (obj != null) {
			getOAObjectGuidService().setGuid(obj, 0L);
		}
	}

	
}


