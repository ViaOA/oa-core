package com.viaoa.oa.service.facade;

import com.viaoa.oa.api.services.TriggersOps;
import com.viaoa.oa.service.OATriggerService;
import com.viaoa.trigger.OATrigger;

/**
 * Public trigger service facade implementation.
 * <p>
 * This facade delegates trigger registration and removal to the OA trigger
 * service owned by the runtime.
 * </p>
 */
public class TriggersOpsImpl implements TriggersOps {

	private OATriggerService srvc;
	
	/**
	 * Creates a trigger facade backed by the trigger service.
	 *
	 * @param srvc trigger service used by this facade
	 */
	public TriggersOpsImpl(OATriggerService srvc) {
		this.srvc = srvc;
	}
	
	
	/**
	 * Adds a trigger to the OA runtime.
	 *
	 * @param trigger trigger to register
	 */
	@Override
	public void addTrigger(OATrigger trigger) {
		srvc.addTrigger(trigger);
	}

	/**
	 * Adds a trigger to the OA runtime with control over first non-many property handling.
	 *
	 * @param trigger trigger to register
	 * @param bSkipFirstNonManyProperty whether to skip the first non-many property in trigger path handling
	 */
	@Override
	public void addTrigger(OATrigger trigger, boolean bSkipFirstNonManyProperty) {
		srvc.addTrigger(trigger, bSkipFirstNonManyProperty);
	}

	/**
	 * Removes a trigger from the OA runtime.
	 *
	 * @param trigger trigger to remove
	 * @return {@code true} when the trigger was removed
	 */
	@Override
	public boolean removeTrigger(OATrigger trigger) {
		return srvc.removeTrigger(trigger);
	}

}
