package com.viaoa.graph;

import java.io.IOException;
import java.util.logging.Logger;

import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAReflect;

public class OAGraph {
	private static Logger LOG = Logger.getLogger(OAGraph.class.getName());

	private final OARuntime runtime;
	private final Package packageThis;
	private boolean bInitCalled;
	private boolean bInitCompleted;

	private final OAObjectService srvcObject;
	private final HubService srvcHub;
    private final OASyncService srvcSync;
    
	public OAGraph(OARuntime rt, Package pkg) {
		if (rt == null) throw new IllegalArgumentException("OARuntime can not be null");
		this.runtime = rt;
		if (pkg == null) throw new IllegalArgumentException("package can not be null");
		this.packageThis = pkg;
		
		srvcObject = new OAObjectService();
		srvcHub = new HubService();
	    srvcSync = new OASyncService(getPackage());
	    
	    srvcObject.initialize(srvcHub, srvcSync);
	    srvcHub.initialize(srvcObject);
	    srvcSync.initialize();
	}

	public synchronized void init() throws ClassNotFoundException, IOException {
		if (bInitCalled) return;
		bInitCalled = true;
		String pn = packageThis.getName();
		String[] classNames = OAReflect.getOAObjectClasses(pn);
		for (String cn : classNames) {
			Class<?> c = Class.forName(pn + "." + cn);
			if (OAObject.class.isAssignableFrom(c)) {
				srvcObject.getOAObjectInfoService().getObjectInfo(c);
			}
		}
		bInitCompleted = true;
	}

	public OARuntime runtime() {
		return runtime;
	}
	
	public Package getPackage() {
		return packageThis;
	}

	public boolean wasInitCalled() {
		return bInitCalled;
	}
	public boolean wasInitCompleted() {
		return bInitCompleted;
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

