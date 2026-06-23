package com.viaoa.graph.api.internal;

import com.viaoa.trigger.OATrigger;

public interface TriggersOps {
	void addTrigger(OATrigger trigger);
	void addTrigger(OATrigger trigger, boolean bSkipFirstNonManyProperty);
	boolean removeTrigger(OATrigger trigger);
	void runTrigger(Runnable r);
}
