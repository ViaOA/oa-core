package com.viaoa.graph;

import java.io.IOException;
import java.util.logging.Logger;

import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAReflect;

public class OAGraph {
	private static Logger LOG = Logger.getLogger(OAGraph.class.getName());

	private final OARuntime runtime;
	private final String pkgName;
	private volatile boolean bInit;

	private final OAObjectService srvcObject;
	private final HubService srvcHub;
    private final OASyncService srvcSync;
    
    //qqqqqqqq must call init() to load
	public OAGraph(OARuntime rt, String pkgName) {
		if (rt == null) throw new IllegalArgumentException("OARuntime can not be null");
		this.runtime = rt;
		this.pkgName = pkgName;
		
		srvcObject = new OAObjectService();
		srvcHub = new HubService();
	    srvcSync = new OASyncService(pkgName);
	    
	    srvcObject.initialize(srvcHub, srvcSync);
	    srvcHub.initialize(srvcObject);
	    srvcSync.initialize();
	}

	public void init() throws ClassNotFoundException, IOException {
		if (bInit) return;
		bInit = true;
		if (pkgName != null && !pkgName.isEmpty()) {
			String[] classNames = OAReflect.getOAObjectClasses(pkgName);
			for (String cn : classNames) {
				Class<?> c = Class.forName(pkgName + "." + cn);
				if (OAObject.class.isAssignableFrom(c)) {
					srvcObject.getOAObjectInfoService().getObjectInfo(c);
				}
			}
		}
	}

	public OARuntime runtime() {
		return runtime;
	}
	
	public String getPackageName() {
		return pkgName;
	}

	public boolean wasInitCalled() {
		return bInit;
	}
    
	// verb for OAObjectService
	public OAObjectService objects() {
//qqqqqqq create a new OAObjectAPI service 		
		return srvcObject;
	}

	public HubService hubs() {
//qqqqqqq create a new OAHubAPI service		
		return srvcHub;
	}
	
	// verb for OASyncService
    public OASyncService sync() {
//qqqqqqq create a new OASyncAPI service		
    	return srvcSync;
    }
    
	
}

