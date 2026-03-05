package test.xice.tsac.model.delegate;

import com.viaoa.graph.service.OASyncService;
import com.viaoa.runtime.OARuntime;

import test.xice.tsac.model.oa.GSMRClient;

public class OASyncDelegate {

	public static boolean callSyncIsServer() {
		return ((OASyncService) OARuntime.graph(GSMRClient.class).sync()).isServer();		
	}

}
