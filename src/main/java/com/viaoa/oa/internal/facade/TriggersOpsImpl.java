package com.viaoa.oa.internal.facade;

import com.viaoa.oa.api.internal.TriggersOps;
import com.viaoa.oa.service.OATriggerService;
import com.viaoa.trigger.OATrigger;

/**
 * Internal trigger facade implementation used by {@code OA.internal().triggers()}.
 */
public class TriggersOpsImpl implements TriggersOps {

	private OATriggerService srvc;
	
	/**
	 * Creates the internal trigger facade backed by the trigger service.
	 *
	 * @param srvc the trigger service
	 */
	public TriggersOpsImpl(OATriggerService srvc) {
		this.srvc = srvc;
	}

	@Override
	/**
	 * Registers a trigger through the internal trigger service.
	 *
	 * @param trigger the trigger to add
	 */
	public void addTrigger(OATrigger trigger) {
		srvc.addTrigger(trigger);
	}

	@Override
	/**
	 * Registers a trigger with control over first non-many property handling.
	 *
	 * @param trigger the trigger to add
	 * @param bSkipFirstNonManyProperty {@code true} to skip the first non-many property during trigger path handling
	 */
	public void addTrigger(OATrigger trigger, boolean bSkipFirstNonManyProperty) {
		srvc.addTrigger(trigger, bSkipFirstNonManyProperty);
	}

	@Override
	/**
	 * Removes a trigger through the internal trigger service.
	 *
	 * @param trigger the trigger to remove
	 * @return {@code true} if the trigger was removed
	 */
	public boolean removeTrigger(OATrigger trigger) {
		return srvc.removeTrigger(trigger);
	}

	@Override
	/**
	 * Runs trigger work through the internal trigger service.
	 *
	 * @param r the trigger work to run
	 */
	public void runTrigger(Runnable r) {
		srvc.runTrigger(r);
	}

}
