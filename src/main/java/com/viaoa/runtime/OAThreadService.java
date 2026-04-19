package com.viaoa.runtime;

import java.util.logging.Logger;

import com.viaoa.transaction.OATransaction;

public class OAThreadService {
	private static Logger LOG = Logger.getLogger(OAThreadService.class.getName());

	private final OAThreadLocalService srvcThreadLocal;
	private final OARemoteThreadService srvcRemoteThread;
	
	public OAThreadService() {
		this.srvcThreadLocal = new OAThreadLocalService();
		this.srvcRemoteThread = new OARemoteThreadService() {
			@Override
			protected void callThreadLocalNotifyWaitingThread() {
			    srvcThreadLocal.notifyWaitingThread();
			}
		};
	}

	public OAThreadLocalService getThreadLocalService() {
		return srvcThreadLocal;
	}
	
	public OARemoteThreadService getRemoteThreadService() {
		return srvcRemoteThread;
	}
	
	
	public Object getContext() {
		Object context = srvcThreadLocal.getContext();
		return context;
	}

	
	public void setContext(Object context) {
		srvcThreadLocal.setContext(context);
	}
	
	public boolean isAdmin() {
		return srvcThreadLocal.isAdmin();
	}

	
	public String getAllStackTraces() {
		return srvcThreadLocal.getAllStackTraces();
	}

	
	public OATransaction getTransaction() {
		return srvcThreadLocal.getTransaction();
	}

	
	public boolean isRefreshing() {
		return srvcThreadLocal.isRefreshing();
	}

}
