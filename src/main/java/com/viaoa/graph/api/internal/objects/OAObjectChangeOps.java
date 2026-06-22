package com.viaoa.graph.api.internal.objects;

import com.viaoa.object.OAObject;

public interface OAObjectChangeOps {

	/**
	 * Determines whether this object—or linked objects specified by the given
	 * relationship type—has unsaved changes.
	 * <p>
	 * This method delegates entirely to
	 * {@link OAObjectDelegate#getChanged(OAObject, int)}, which performs the
	 * actual change-detection logic. The delegate evaluates:
	 * <ul>
	 *   <li>whether this object is marked as new,</li>
	 *   <li>whether this object has local property changes,</li>
	 *   <li>whether linked objects should be included based on the supplied
	 *       {@code relationshipType} (e.g., {@code CASCADE_NONE},
	 *       {@code CASCADE_LINK_RULES}),</li>
	 *   <li>whether TYPE=MANY and CASCADE=true links should be traversed.</li>
	 * </ul>
	 *
	 * @param relationshipType the cascade/relationship mode used to determine
	 *                         whether linked objects participate in change
	 *                         evaluation
	 * @return {@code true} if this object or participating linked objects have
	 *         unsaved changes; {@code false} otherwise
	 */
	public boolean getChanged(OAObject oaObj, int cascadeRule);
	/*qqqqqqqqqqqqqqqqqqqqqq

return getOAObjectChangeService().getChanged(oaObj, cascadeRule);
	*/

	
	

}
