package com.viaoa.graph;

import java.io.IOException;
import java.util.logging.Logger;

import com.viaoa.graph.api.HubsOps;
import com.viaoa.graph.api.ObjectsOps;
import com.viaoa.graph.api.SyncOps;
import com.viaoa.graph.api.internal.HubsInternalOps;
import com.viaoa.graph.api.internal.ObjectsInternalOps;
import com.viaoa.graph.api.internal.SyncInternalOps;
import com.viaoa.graph.service.HubService;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.graph.service.OASyncService;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadImpl;
import com.viaoa.runtime.thread.OARemoteThreadService;
import com.viaoa.runtime.thread.OAThreadLocalService;
import com.viaoa.util.OAReflect;

public class OAGraphImpl implements OAGraphInternal {
	private static Logger LOG = Logger.getLogger(OAGraphImpl.class.getName());

	private final OARuntime runtime;
	private final String pkgName;
	private volatile boolean bInit;

	private final OAObjectService srvcOAObject;
	private final HubService srvcHub;
    private final OASyncService srvcOASync;

	public OAGraphImpl(OARuntime rt, String pkgName) {
		if (rt == null) throw new IllegalArgumentException("OARuntime can not be null");
		this.runtime = rt;
		this.pkgName = pkgName;
		
		srvcOAObject = new OAObjectService();
		srvcHub = new HubService();
	    srvcOASync = new OASyncService(pkgName);

//	public void initialize(HubService srvcHub, OASyncService srvcSync, OAThreadLocalService srvcOAThreadLocal, OARemoteThreadService srvcOARemoteThread) {
	    OAThreadImpl tl = (OAThreadImpl) runtime.thread();
	    srvcOAObject.initialize(srvcHub, srvcOASync, tl.getThreadLocalService(), tl.getRemoteThreadService());
	    srvcHub.initialize(srvcOAObject, srvcOASync);
	    srvcOASync.initialize();
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
					srvcOAObject.getOAObjectInfoService().getObjectInfo(c);
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
    

	
	public ObjectsOps objects() {
		return srvcOAObject;
	}

	@Override
	public HubsOps hubs() {
		return srvcHub;
	}
	
	@Override
    public SyncOps sync() {
    	return srvcOASync;
    }

	@Override
	public ObjectsInternalOps objectsInternal() {
		return srvcOAObject;
	}

	@Override
	public HubsInternalOps hubsInternal() {
		return srvcHub;
	}
	
	@Override
    public SyncInternalOps syncInternal() {
    	return srvcOASync;
    }
	
	
/*qqqqqqqqqqqq	dont allow these to be leaked
	public OAObjectService getOAObjectService() {
		return srvcOAObject;
	}

	public HubService getHubService() {
		return srvcHub;
	}
	
    public OASyncService getOASyncService() {
    	return srvcOASync;
    }
*/    
	
}

