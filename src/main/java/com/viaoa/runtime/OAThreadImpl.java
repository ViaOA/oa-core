package com.viaoa.runtime;

import java.util.logging.Logger;

import com.viaoa.runtime.thread.OARemoteThreadService;
import com.viaoa.runtime.thread.OAThreadLocalService;
import com.viaoa.transaction.OATransaction;

public class OAThreadImpl implements OAThread {
	private static Logger LOG = Logger.getLogger(OAThreadImpl.class.getName());

	private final OAThreadLocalService threadLocalService;
	private final OARemoteThreadService remoteThreadService;
	
	public OAThreadImpl() {
		this.threadLocalService = new OAThreadLocalService();
		this.remoteThreadService = new OARemoteThreadService();
	}

	public OAThreadLocalService getThreadLocalService() {
		return threadLocalService;
	}
	
	public OARemoteThreadService getRemoteThreadService() {
		return remoteThreadService;
	}
	
	
	
	@Override
	public Object getContext() {
		final OAThreadLocalService srvcThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();
		Object context = srvcThreadLocal.getContext();
		return context;
	}

	@Override
	public void setContext(Object context) {
		final OAThreadLocalService srvcThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();
		srvcThreadLocal.setContext(context);
	}
	
	
	@Override
	public boolean isAdmin() {
		final OAThreadLocalService srvcThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();
		return srvcThreadLocal.isAdmin();
	}

	@Override
	public String getAllStackTraces() {
		final OAThreadLocalService srvcThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();
		return srvcThreadLocal.getAllStackTraces();
	}

	@Override
	public OATransaction getTransaction() {
		final OAThreadLocalService srvcThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();
		return srvcThreadLocal.getTransaction();
	}

	@Override
	public boolean isRefreshing() {
		final OAThreadLocalService srvcThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();
		return srvcThreadLocal.isRefreshing();
	}


}
