package com.viaoa.graph.service.object;

import java.util.List;
import java.util.logging.Logger;

import com.viaoa.hub.Hub;
import com.viaoa.object.OACallback;
import com.viaoa.object.OACascade;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;

public abstract class OAObjectChangeService {
	private static final Logger LOG = Logger.getLogger(OAObjectChangeService.class.getName());

	private final OAObject.FriendAccess faObject;
	
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
		
		if (faObject.getChangedFlag(oaObj)) return true;
		if (faObject.getNewFlag(oaObj)) return true;

		if (iCascadeRule == oaObj.CASCADE_NONE) {
			return false;
		}
		if (cascade.wasCascaded(oaObj, true)) {
			return false;
		}

		if (faObject.getProperties(oaObj) == null) return false;

		// check link cascade objects
		OAObjectInfo oi = callObjectInfoGetOAObjectInfo(oaObj);
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
				if (obj instanceof OAObject) { // 20110420 could be OANullObject
					if (getChanged((OAObject) obj, iCascadeRule, cascade)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public abstract OAObjectInfo callObjectInfoGetOAObjectInfo(OAObject oaObj);
	public abstract boolean callObjectInfoIsMany2Many(OALinkInfo li);
	public abstract boolean callHubStatusGetChanged(Hub hub, int type, OACascade cascade);
	public abstract Object callObjectReflectGetRawReference(OAObject oaObj, String prop);
    public abstract Object callObjectReflectGetProperty(OAObject oaObj, String prop);
    public abstract boolean callObjectHubGetChanged(Hub hub, int cascadeRule, OACascade cascade);	
    public abstract boolean callObjectReflectIsReferenceNullOrNotLoaded(OAObject oaObj, String prop);
	
}
