package com.viaoa.oa.service.object;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.logging.Logger;

import com.viaoa.cascade.OACascade;
import com.viaoa.datetime.OADateTime;
import com.viaoa.hub.Hub;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.metadata.OAPropertyInfo;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;


/**
 * Evaluates changed-state for OAObjects and related Hubs according to cascade rules.
 */
public abstract class OAObjectChangeService {
	private static final Logger LOG = Logger.getLogger(OAObjectChangeService.class.getName());

	private final OAObject.FriendAccess faObject;
	
	/**
	 * Performs OAObjectChangeService behavior for the OA object service.
	 *
	 * @param faObject method input
	 */
	public OAObjectChangeService(OAObject.FriendAccess faObject) {
    	if (faObject == null) throw new IllegalArgumentException("OAObject.FriendAccess can not be null");
    	this.faObject = faObject;
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
			return (faObject.getChangedFlag(oaObj) || faObject.getNewFlag(oaObj));
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
		
		if (faObject.getChangedFlag(oaObj)) {
			return true;
		}
		if (faObject.getNewFlag(oaObj)) {
			return true;
		}

		if (iCascadeRule == oaObj.CASCADE_NONE) {
			return false;
		}
		if (cascade != null && cascade.wasCascaded(oaObj, true)) {
			return false;
		}

		if (faObject.getProperties(oaObj) == null) return false;

		// check link cascade objects
		OAObjectInfo oi = callObjectInfoGetOAObjectInfo(oaObj);
		List<OALinkInfo> al = oi.getLinkInfos();
		for (int i = 0; i < al.size(); i++) {
			OALinkInfo li = al.get(i);
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
			if (callObjectReflectIsReferenceNullOrNotLoaded(oaObj, prop)) {
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

			
			if (callObjectInfoIsMany2Many(li)) {
				Hub hub = (Hub) callObjectReflectGetRawReference(oaObj, prop);
				if (callHubStatusGetChanged(hub, OAObject.CASCADE_NONE, cascade)) {
					return true;
				}
			}
			
			if (!bValidCascade) {
				continue;
			}

			Object obj = callObjectReflectGetProperty(oaObj, li.getName()); // if Hub with Keys, then this will load the correct objects to check
			if (obj == null) {
				continue;
			}

			if (obj instanceof Hub) {
				if (callObjectHubGetChanged((Hub) obj, iCascadeRule, cascade)) {
					return true; //  if there have been adds/removes to hub
				}
			} else {
				if (obj instanceof OAObject) { // 20110420 could be OAMatchNull
					if (getChanged((OAObject) obj, iCascadeRule, cascade)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public void setChanged(final OAObject oaObj, final boolean tf) {
		if (oaObj == null) return;
		
		final boolean bOld = faObject.getChangedFlag(oaObj);
		if (tf == bOld) return;

		faObject.setChangedFlag(oaObj, tf);
		
  		if (OARuntime.thread().getThreadLocalService().isLoading()) return;
		
		callObjectEventFirePropertyChange(oaObj, OAObjectParentService.WORD_Changed, bOld, tf, false, false);

		if (tf) {
			if (!callRemoteThreadIsRemoteThread()) {
				OAObjectInfo oi = callObjectInfoGetOAObjectInfo(oaObj);
				OAPropertyInfo pi = oi.getTimestampProperty();
				if (pi != null) {
					oaObj.setProperty(pi.getName(), new OADateTime());
				}
			}
			
			callObjectPropertySetReferenceable(oaObj, true);
		}
		// notify owners
		_sendParentChangeEvent(oaObj, 0, true);
	}

	public void sendParentChangeEvent(final OAObject oaObj) {
		_sendParentChangeEvent(oaObj, 0, false);
	}
	
	protected void _sendParentChangeEvent(final OAObject oaObj, final int cnt, final boolean bAlreadySendEvent) {
		if (oaObj == null) return;
		if (cnt > 50) return;
		
		if (!bAlreadySendEvent) callObjectEventFirePropertyChange(oaObj, OAObjectParentService.WORD_Changed, null, null, false, false, true);
		
		WeakReference<Hub<?>>[] refs = callObjectHubGetHubReferencesNoCopy(oaObj);
		if (refs == null) return;
		
		for (WeakReference wr : refs) {
			if (wr == null) continue;
			Hub hx = (Hub) wr.get();
			if (hx == null) continue;
			OALinkInfo li = callObjectHubGetLinkInfoFromMasterToDetail(hx);
			if (li == null) continue;
			if (!li.getOwner() && !li.getCascadeSave()) continue;
			OAObject obj = callObjectHubGetMasterObject(hx);
			if (obj != null) _sendParentChangeEvent(obj, cnt+1, false);
		}
	}
	
	
	
	/**
	 * Dependency hook used by this service to objectInfoGetOAObjectInfo.
	 *
	 * @param oaObj method input
	 * @return result value
	 */
	public abstract OAObjectInfo callObjectInfoGetOAObjectInfo(OAObject oaObj);
	/**
	 * Dependency hook used by this service to objectInfoIsMany2Many.
	 *
	 * @param li method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callObjectInfoIsMany2Many(OALinkInfo li);
	/**
	 * Dependency hook used by this service to hubStatusGetChanged.
	 *
	 * @param hub method input
	 * @param type method input
	 * @param cascade method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callHubStatusGetChanged(Hub<?> hub, int type, OACascade cascade);
	/**
	 * Dependency hook used by this service to objectReflectGetRawReference.
	 *
	 * @param oaObj method input
	 * @param prop method input
	 * @return result value
	 */
	public abstract Object callObjectReflectGetRawReference(OAObject oaObj, String prop);
	/**
	 * Dependency hook used by this service to objectReflectGetProperty.
	 *
	 * @param oaObj method input
	 * @param prop method input
	 * @return result value
	 */
    public abstract Object callObjectReflectGetProperty(OAObject oaObj, String prop);
	/**
	 * Dependency hook used by this service to objectHubGetChanged.
	 *
	 * @param hub method input
	 * @param cascadeRule method input
	 * @param cascade method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
    public abstract boolean callObjectHubGetChanged(Hub<?> hub, int cascadeRule, OACascade cascade);	
	/**
	 * Dependency hook used by this service to objectReflectIsReferenceNullOrNotLoaded.
	 *
	 * @param oaObj method input
	 * @param prop method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
    public abstract boolean callObjectReflectIsReferenceNullOrNotLoaded(OAObject oaObj, String prop);
	
	public abstract boolean callRemoteThreadIsRemoteThread();

	public abstract void callObjectEventFirePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged);

	public abstract void callObjectEventFirePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged, boolean bUnknownValues);
	
	public abstract WeakReference<Hub<?>>[] callObjectHubGetHubReferencesNoCopy(OAObject oaObj);

	public abstract OALinkInfo callObjectHubGetLinkInfoFromMasterToDetail(Hub<?> hub);
	
	public abstract OAObject callObjectHubGetMasterObject(Hub<?> hub);

	public abstract void callObjectPropertySetReferenceable(OAObject obj, boolean bReferenceable);
	
}
