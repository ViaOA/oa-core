package com.viaoa.graph;

import java.io.IOException;
import java.util.logging.Logger;

import com.viaoa.graph.api.HubsOps;
import com.viaoa.graph.api.ObjectsOps;
import com.viaoa.graph.api.ReplOps;
import com.viaoa.graph.api.SyncOps;
import com.viaoa.graph.api.internal.HubsInternalOps;
import com.viaoa.graph.api.internal.ObjectsInternalOps;
import com.viaoa.graph.api.internal.ReplInternalOps;
import com.viaoa.graph.api.internal.SyncInternalOps;
import com.viaoa.graph.service.HubService;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.graph.service.OAReplicationService;
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
	private final Package thisPackage;
	private volatile boolean bInit;

	private OAObjectService srvcOAObject;
	private HubService srvcHub;
    private OASyncService srvcOASync;
    private OAReplicationService srvcOAReplication;

	public OAGraphImpl(OARuntime rt, Package thisPackage) {
		if (rt == null) throw new IllegalArgumentException("OARuntime can not be null");
		this.runtime = rt;
		this.thisPackage = thisPackage;
	}

	public void initialize() throws ClassNotFoundException, IOException {
		if (bInit) return;
		bInit = true;
		
	    OAThreadImpl tl = (OAThreadImpl) runtime.thread();

		srvcOAObject = new OAObjectService();
		srvcHub = new HubService();
	    srvcOASync = new OASyncService(this);

		srvcOAObject.initialize(srvcHub, srvcOASync, tl.getThreadLocalService(), tl.getRemoteThreadService());
		srvcHub.initialize(srvcOAObject, srvcOASync, tl.getThreadLocalService(), tl.getRemoteThreadService());
		
		
		if (thisPackage != null) {
			String pkgName = thisPackage.getName();
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
	
	public Package getPackage() {
		return thisPackage;
	}

	public boolean wasInitCalled() {
		return bInit;
	}
    

/*	
	public ObjectsOps objects() {
		return srvcOAObject;
	}

	@Override
	public HubsOps hubs() {
		return srvcHub;
	}
*/	
	@Override
    public SyncOps sync() {
    	return srvcOASync;
    }

	@Override
    public ReplOps replication() {
    	return srvcOAReplication;
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
	
	@Override
    public ReplInternalOps replInternal() {
    	return srvcOAReplication;
    }
}

