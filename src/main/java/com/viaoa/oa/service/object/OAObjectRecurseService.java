package com.viaoa.oa.service.object;

import java.util.List;
import java.util.logging.Logger;

import com.viaoa.callback.OACallback;
import com.viaoa.cascade.OACascade;
import com.viaoa.hub.Hub;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.OAObject;

/**
 * Traverses related OAObjects using metadata links and cascade tracking.
 */
public abstract class OAObjectRecurseService {
	private static final Logger LOG = Logger.getLogger(OAObjectRecurseService.class.getName());

	/**
	 * Convenience method that initiates a recursive traversal of the OA model
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
	public <T extends OAObject> void recurse(T oaObj, OACallback<OAObject> callback) {
		OACascade cascade = new OACascade();
		recurse(oaObj, callback, cascade);
	}

	/**
	 * Recursively traverses the reachable OA model beginning at the specified
	 * {@link OAObject}, invoking the provided {@link OACallback} for the root object
	 * and for each subsequently visited object. The supplied {@link OACascade}
	 * tracks visited objects to ensure each instance is processed at most once and
	 * to prevent infinite loops when cycles exist in the OA model.
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
	public void recurse(final OAObject oaObj, final OACallback<OAObject> callback, OACascade cascade) {
		if (oaObj == null || cascade == null || cascade.wasCascaded(oaObj, true)) {
			return;
		}

		if (callback != null) {
			callback.updateObject(oaObj);
		}
		OAObjectInfo oi = callObjectInfoGetOAObjectInfo(oaObj);
		List<OALinkInfo> al = oi.getLinkInfos();
		for (OALinkInfo li : al) {
			if (li.getCalculated()) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}

			Object obj = callObjectReflectGetProperty(oaObj, li.getName());
			if (obj == null) {
				continue;
			}
			if (obj instanceof Hub) {
				Hub<?> h = (Hub<?>) obj;
				for (int j = 0;; j++) {
					OAObject o = h.elementAt(j);
					if (o == null) {
						break;
					}
					recurse(o, callback, cascade);
					Object o2 = h.elementAt(j);
					if (o != o2) {
						j--;
					}
				}
			} else {
				recurse((OAObject) obj, callback, cascade);
			}
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
	 * Dependency hook used by this service to objectReflectGetProperty.
	 *
	 * @param oaObj method input
	 * @param name method input
	 * @return result value
	 */
	public abstract Object callObjectReflectGetProperty(OAObject oaObj, String name);

}
