package com.viaoa.graph;

import java.io.IOException;
import java.util.logging.Logger;

import com.viaoa.graph.object.OAObjectCacheService;
import com.viaoa.graph.object.OAObjectDSService;
import com.viaoa.graph.object.OAObjectGuidService;
import com.viaoa.graph.object.OAObjectInfoService;
import com.viaoa.graph.object.OAObjectInitializeService;
import com.viaoa.graph.object.OAObjectPropertyService;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAReflect;

public class OAGraph {
	private static Logger LOG = Logger.getLogger(OAGraph.class.getName());

	private final OARuntime runtime;
	private final Package packageThis;
	private boolean bInitCalled;
	private boolean bInitCompleted;

	private final OAObjectService srvcObject = new OAObjectService(this);
	private final HubService srvcHub = new HubService(this);
    private final OASyncService srvcSync = new OASyncService(this);
    
	public OAGraph(OARuntime rt, Package pkg) {
		if (rt == null) throw new IllegalArgumentException("OARuntime can not be null");
		this.runtime = rt;
		if (pkg == null) throw new IllegalArgumentException("package can not be null");
		this.packageThis = pkg;
	}

	public synchronized void init() throws ClassNotFoundException, IOException {
		if (bInitCalled) return;
		bInitCalled = true;
		String pn = packageThis.getName();
		String[] classNames = OAReflect.getOAObjectClasses(pn);
		for (String cn : classNames) {
			Class<?> c = Class.forName(pn + "." + cn);
			if (OAObject.class.isAssignableFrom(c)) {
				OAObjectInfoDelegate.getObjectInfo(c);
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
		return srvcObject;
	}

	public HubService hubs() {
		return srvcHub;
	}
	
	// verb for OASyncService
    public OASyncService sync() {
    	return srvcSync;
    }
    
    
 // NEXT qqqqqqqqqqqqqqqqqq	
// graph.objects().initialize(oaobj)    
// guids need to be unique to OAGraph ... needs to assign OAObject.guid based on graph, not runtime static guidCntr 
// metadata
// ObjectCache
// OASync, Remoting	
// locking, transactions
// Eventing / hub wiring / listener tree
	
	
}

