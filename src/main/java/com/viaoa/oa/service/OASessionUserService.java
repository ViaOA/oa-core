package com.viaoa.oa.service;

import com.viaoa.oa.OA;
import com.viaoa.oa.api.SessionUserOps;
import com.viaoa.runtime.OARuntime;
import com.viaoa.session.OASessionUser;

/**
 * Stores and resolves the current session user for an OA execution path.
 * <p>
 * Session users represent the application/session actor and are distinct from
 * ModelUser permission identity.
 * </p>
 */
public class OASessionUserService implements SessionUserOps {

	private final OA oa;
	
	/**
	 * Creates a session-user service for an OA runtime.
	 *
	 * @param oa owning OA runtime
	 */
	public OASessionUserService(OA oa) {
		this.oa = oa;
	}
	
	/**
	 * Returns the current thread-local session user.
	 *
	 * @return current session user, or {@code null} when none is set
	 */
	@Override
	public OASessionUser<?> get() {
		return OARuntime.thread().getThreadLocalService().getSessionUser();
	}

	/**
	 * Sets the current thread-local session user.
	 *
	 * @param su session user to make current, or {@code null} to clear
	 */
	@Override
	public void set(OASessionUser<?> su) {
		OARuntime.thread().getThreadLocalService().setSessionUser(su);
	}

}
