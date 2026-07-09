package com.viaoa.runtime;

import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.viaoa.hub.Hub;
import com.viaoa.oa.OA;
import com.viaoa.object.OAObject;
import com.viaoa.session.OASessionUser;
import com.viaoa.transaction.OATransaction;

/* qqqqqqqqqqqqq
 CODEX
 
#1 — boundary risk
  File/class/method: src/main/java/com/viaoa/runtime/OAThreadService.java:64
  Exact concern: runtime hardcodes Swing EDT detection with SwingUtilities.isEventDispatchThread().
  Why it matters: runtime scheduling behavior in OAObjectInfo and HubMerger depends on this method. Core
  should expose a UI-thread hook, but should not own Swing.
  Minimal fix: keep isUIThread() but back it with a pluggable provider/default false. Let UI/JFC module
  register Swing behavior.
  Suggested invariant ID/name: RUNTIME_UI_THREAD_PROVIDER_IS_OPTIONAL
  Suggested test coverage: default core runtime returns false without Swing provider; UI provider can be
  installed and observed by trigger/HubMerger paths.
 
 
 */


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
	
	
	public Hub<?> getModelUserHub(OA oa) {
		Hub<?> hub = srvcThreadLocal.getModelUserHub(oa);
		return hub;
	}

	public void setModelUserHub(OA oa, Hub<?> hub) {
		srvcThreadLocal.setModelUserHub(oa, hub);
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

	public boolean isRemoteThread() {
		return getRemoteThreadService().isRemoteThread();
	}
	
//qqqqqqqqqqqq
	public boolean isUIThread() {
//qqqqqqq create a plugin/provider for this		
		return SwingUtilities.isEventDispatchThread();		
	}
	
}
