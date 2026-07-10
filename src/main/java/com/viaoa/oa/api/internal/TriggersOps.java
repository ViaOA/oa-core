package com.viaoa.oa.api.internal;

import com.viaoa.trigger.OATrigger;

/**
 * Internal trigger operation boundary for registering and running OA triggers.
 */
public interface TriggersOps {
	/**
	 * Registers an OA trigger.
	 *
	 * @param trigger the trigger to add
	 */
	void addTrigger(OATrigger trigger);
	/**
	 * Registers an OA trigger with control over first non-many property handling.
	 *
	 * @param trigger the trigger to add
	 * @param bSkipFirstNonManyProperty {@code true} to skip the first non-many property
	 */
	void addTrigger(OATrigger trigger, boolean bSkipFirstNonManyProperty);
	/**
	 * Removes an OA trigger.
	 *
	 * @param trigger the trigger to remove
	 * @return {@code true} if the trigger was removed
	 */
	boolean removeTrigger(OATrigger trigger);
	/**
	 * Runs trigger work through the internal trigger service.
	 *
	 * @param r the trigger work to run
	 */
	void runTrigger(Runnable r);
}
