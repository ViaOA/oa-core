package com.viaoa.graph.service.object;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.hub.Hub;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;

public abstract class OAObjectAutoAddService {
	private static final Logger LOG = Logger.getLogger(OAObjectAutoAddService.class.getName());

	/**
	 * Reserved property name representing whether auto-add behavior is enabled
	 * for reverse-link insertion.
	 */
	public static final String WORD_AutoAdd = "AutoAdd";
	
	private final OAObject.FriendAccess faObject;
	
	public OAObjectAutoAddService(OAObject.FriendAccess faObject) {
    	if (faObject == null) throw new IllegalArgumentException("OAObject.FriendAccess can not be null");
    	this.faObject = faObject;
    }

	/**
	 * Tracks OAObjects for which automatic reverse-link insertion is disabled.
	 * Presence of a GUID in this map indicates auto-add is turned off.
	 */
	private final ConcurrentHashMap<UUID, Long> hmAutoAdd = new ConcurrentHashMap<>();
	
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

		boolean bOld = !hmAutoAdd.containsKey(faObject.getGuid(oaObj));
		if (bOld == bEnabled) {
			return;
		}

		UUID guid = faObject.getGuid(oaObj);
		if (!bEnabled) {
			hmAutoAdd.put(guid, 0L);
		} else {
			hmAutoAdd.remove(guid);
		}
		callObjectEventFirePropertyChange(oaObj, WORD_AutoAdd, bOld, bEnabled, false, false);

		if (!bEnabled || faObject.getDeleteFlag(oaObj)) {
			return;
		}

		final boolean bWas = callThreadLocalGetSendSyncMessages();
		try {
			callThreadLocalSetSendSyncMessages(false);
			// need to see if object should be put into linkOne/masterObject hub(s)
			OAObjectInfo oi = callObjectInfoGetOAObjectInfo(oaObj);
			
			for (OALinkInfo li : oi.getLinkInfos()) {
				if (!li.getUsed()) {
					continue;
				}
				if (li.getType() != li.ONE) {
					continue;
				}
				Object objx = callObjectReflectGetRawReference(oaObj, li.getName());
				if (!(objx instanceof OAObject)) {
					continue;
				}

				OALinkInfo liRev = callObjectInfoGetReverseLinkInfo(li);
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

				Object objz = callObjectReflectGetProperty((OAObject) objx, liRev.getName());
				if (objz instanceof Hub) {
					((Hub) objz).add(oaObj);
				}
			}
		} finally {
			callThreadLocalSetSendSyncMessages(bWas);
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
		return !hmAutoAdd.containsKey(faObject.getGuid(oaObj));
	}

	public abstract void callObjectEventFirePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged);
	public abstract OAObjectInfo callObjectInfoGetOAObjectInfo(OAObject oaObj);
	public abstract Object callObjectReflectGetRawReference(OAObject oaObj, String name);
	public abstract OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo li);
	public abstract Object callObjectReflectGetProperty(OAObject obj, String name);	
	public abstract boolean callThreadLocalGetSendSyncMessages();
	public abstract void callThreadLocalSetSendSyncMessages(boolean b);
	
}
