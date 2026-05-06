package com.viaoa.graph.api;

import com.viaoa.trigger.OATrigger;

public interface TriggerOps {
	void addTrigger(OATrigger trigger);
	void addTrigger(OATrigger trigger, boolean bSkipFirstNonManyProperty);
	boolean removeTrigger(OATrigger trigger);
}
