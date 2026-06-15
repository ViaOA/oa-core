package com.viaoa.graph.service.facade;

import com.viaoa.graph.api.services.TriggersOps;
import com.viaoa.graph.service.OATriggerService;
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

}
