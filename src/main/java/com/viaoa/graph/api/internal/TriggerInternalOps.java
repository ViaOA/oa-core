package com.viaoa.graph.api.internal;

import com.viaoa.graph.api.TriggerOps;

public interface TriggerInternalOps extends TriggerOps {
	void runTrigger(Runnable r);
}
