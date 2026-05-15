package com.viaoa.graph.api.internal;

import com.viaoa.graph.api.ReplOps;

public interface ReplInternalOps extends ReplOps {

	public boolean isMaster();
	public boolean isClient();
	
}
