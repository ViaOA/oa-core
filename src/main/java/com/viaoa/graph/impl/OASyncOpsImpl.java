package com.viaoa.graph.impl;

import com.viaoa.graph.OASyncService;
import com.viaoa.graph.api.OASyncOps;

public class OASyncOpsImpl implements OASyncOps {

	private OASyncService srvcOASync;
	
	public OASyncOpsImpl(OASyncService srvc) {
		this.srvcOASync = srvc;;
	}

}
