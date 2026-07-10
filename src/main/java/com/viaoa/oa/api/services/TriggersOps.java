package com.viaoa.oa.api.services;

import com.viaoa.trigger.OATrigger;

/**
 * Public OA trigger service operations.
 * <p>
 * Triggers allow application and framework code to register work that reacts to
 * model changes. Lower-level trigger execution details remain internal to the
 * OA runtime.
 */
public interface TriggersOps {
	
	/**
	 * Registers a trigger.
	 *
	 * @param trigger the trigger to add
	 */
	void addTrigger(OATrigger trigger);

	/**
	 * Registers a trigger with control over first non-many property handling.
	 *
	 * @param trigger the trigger to add
	 * @param bSkipFirstNonManyProperty {@code true} to skip the first non-many
	 *        property during trigger path handling
	 */
	void addTrigger(OATrigger trigger, boolean bSkipFirstNonManyProperty);

	/**
	 * Removes a trigger.
	 *
	 * @param trigger the trigger to remove
	 * @return {@code true} if the trigger was removed
	 */
	boolean removeTrigger(OATrigger trigger);
    
}
