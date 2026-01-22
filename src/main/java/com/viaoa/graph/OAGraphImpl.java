package com.viaoa.graph;

import java.io.IOException;
import java.util.logging.Logger;

import com.viaoa.graph.api.HubOps;
import com.viaoa.graph.api.OAObjectOps;
import com.viaoa.graph.api.OASyncOps;
import com.viaoa.graph.impl.HubOpsImpl;
import com.viaoa.graph.impl.OAObjectOpsImpl;
import com.viaoa.graph.impl.OASyncOpsImpl;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAReflect;

public class OAGraphImpl implements OAGraph {
	private static Logger LOG = Logger.getLogger(OAGraphImpl.class.getName());

	private final OARuntime runtime;
	private final String pkgName;
	private volatile boolean bInit;

	private final OAObjectService srvcObject;
	private final HubService srvcHub;
    private final OASyncService srvcSync;

    private OAObjectOps opsOAObject;
    private HubOps opsHub;
    private OASyncOps opsOASync;
    
    
	public OAGraphImpl(OARuntime rt, String pkgName) {
		if (rt == null) throw new IllegalArgumentException("OARuntime can not be null");
		this.runtime = rt;
		this.pkgName = pkgName;
		
		srvcObject = new OAObjectService();
		srvcHub = new HubService();
	    srvcSync = new OASyncService(pkgName);
	    
	    srvcObject.initialize(srvcHub, srvcSync);
	    srvcHub.initialize(srvcObject, srvcSync);
	    srvcSync.initialize();
	}

    //qqqqqqqq must call init() to load
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
    

//qqqqqqqqqqqqqqqq ============== internal facing qqqqqqqqqqqqqqqqq    
	
	
	public OAObjectService getOAObjectService() {
		return srvcObject;
	}

	public HubService getHubService() {
		return srvcHub;
	}
	
    public OASyncService getSyncService() {
    	return srvcSync;
    }

    
//qqqqqqqqqqqqqqqq ============== public API facing qqqqqqqqqqqqqqqqq    
    
	
	@Override
	public OAObjectOps objects() {
		if (opsOAObject == null) {
			opsOAObject = new OAObjectOpsImpl(srvcObject);
		}
		return opsOAObject;
	}

	@Override
	public HubOps hubs() {
		if (opsHub == null) {
			opsHub = new HubOpsImpl(getHubService());
		}
		return opsHub;
	}
	
	@Override
    public OASyncOps sync() {
    	if (opsOASync == null) {
    		opsOASync = new OASyncOpsImpl(getSyncService());
    	}
    	return opsOASync;
    }
    
	
	
	
	
	
	
}

