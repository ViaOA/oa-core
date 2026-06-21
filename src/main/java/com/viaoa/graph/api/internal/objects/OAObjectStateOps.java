package com.viaoa.graph.api.internal.objects;

import com.viaoa.object.OAObject;

public interface OAObjectStateOps {

	public void callObjectSetNew(OAObject oaObj, boolean bIsNew);
	
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
	/*
	*/
	
	
	
	
	
	
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
	public boolean getChanged(int relationshipType);
	/*

return getOAObjectChangeService().getChanged(oaObj, cascadeRule);
	*/

	
	
	

}


