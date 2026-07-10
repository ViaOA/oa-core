package com.viaoa.runtime;

import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.viaoa.hub.Hub;
import com.viaoa.oa.OA;
import com.viaoa.object.OAObject;
import com.viaoa.session.OASessionUser;
import com.viaoa.transaction.OATransaction;



/**
 * Runtime state field used by OA services for {.
 */
public class OAThreadService {
	private static Logger LOG = Logger.getLogger(OAThreadService.class.getName());

	private final OAThreadLocalService srvcThreadLocal;
	private final OARemoteThreadService srvcRemoteThread;
	
	/**
	 * Creates the runtime service instance.
	 */
	public OAThreadService() {
		this.srvcThreadLocal = new OAThreadLocalService();
		this.srvcRemoteThread = new OARemoteThreadService() {
			@Override
			/**
			 * Runtime hook called by the owning service implementation.
			 */
			protected void callThreadLocalNotifyWaitingThread() {
			    srvcThreadLocal.notifyWaitingThread();
			}
		};
	}

	/**
	 * Returns the ThreadLocalService value.
	 *
	 * @return the ThreadLocalService value
	 */
	public OAThreadLocalService getThreadLocalService() {
		return srvcThreadLocal;
	}
	
	/**
	 * Returns the RemoteThreadService value.
	 *
	 * @return the RemoteThreadService value
	 */
	public OARemoteThreadService getRemoteThreadService() {
		return srvcRemoteThread;
	}
	
	
	/**
	 * Returns the ModelUserHub value.
	 *
	 * @param oa the lookup context
	 *
	 * @return the ModelUserHub value
	 */
	public Hub<?> getModelUserHub(OA oa) {
		Hub<?> hub = srvcThreadLocal.getModelUserHub(oa);
		return hub;
	}

	/**
	 * Sets the ModelUserHub value.
	 * @param oa the ModelUserHub value
	 * @param hub the ModelUserHub value
	 */
	public void setModelUserHub(OA oa, Hub<?> hub) {
		srvcThreadLocal.setModelUserHub(oa, hub);
	}
	
	/**
	 * Returns whether Admin is active for the current runtime context.
	 *
	 * @return {@code true} if Admin is active
	 */
	public boolean isAdmin() {
		return srvcThreadLocal.isAdmin();
	}

	
	/**
	 * Returns the AllStackTraces value.
	 *
	 * @return the AllStackTraces value
	 */
	public String getAllStackTraces() {
		return srvcThreadLocal.getAllStackTraces();
	}

	
	/**
	 * Returns the Transaction value.
	 *
	 * @return the Transaction value
	 */
	public OATransaction getTransaction() {
		return srvcThreadLocal.getTransaction();
	}

	
	/**
	 * Returns whether Refreshing is active for the current runtime context.
	 *
	 * @return {@code true} if Refreshing is active
	 */
	public boolean isRefreshing() {
		return srvcThreadLocal.isRefreshing();
	}

	/**
	 * Returns whether RemoteThread is active for the current runtime context.
	 *
	 * @return {@code true} if RemoteThread is active
	 */
	public boolean isRemoteThread() {
		return getRemoteThreadService().isRemoteThread();
	}
	
	/**
	 * Returns whether UIThread is active for the current runtime context.
	 *
	 * @return {@code true} if UIThread is active
	 */
	public boolean isUIThread() {
		return SwingUtilities.isEventDispatchThread();		
	}
	
}
