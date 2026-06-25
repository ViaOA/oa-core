package com.viaoa.oa.internal.facade;

import com.viaoa.oa.api.internal.TriggersOps;
import com.viaoa.oa.service.OATriggerService;
import com.viaoa.trigger.OATrigger;

public class TriggersOpsImpl implements TriggersOps {

	private OATriggerService srvc;
	
	public TriggersOpsImpl(OATriggerService srvc) {
		this.srvc = srvc;
	}

	@Override
	public void addTrigger(OATrigger trigger) {
		srvc.addTrigger(trigger);
	}

	@Override
	public void addTrigger(OATrigger trigger, boolean bSkipFirstNonManyProperty) {
		srvc.addTrigger(trigger, bSkipFirstNonManyProperty);
	}

	@Override
	public boolean removeTrigger(OATrigger trigger) {
		return srvc.removeTrigger(trigger);
	}

	@Override
	public void runTrigger(Runnable r) {
		srvc.runTrigger(r);
	}

}
