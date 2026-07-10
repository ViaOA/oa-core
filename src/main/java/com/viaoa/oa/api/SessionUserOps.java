package com.viaoa.oa.api;

import com.viaoa.session.OASessionUser;

/**
 * Public OA session-user operations.
 * <p>
 * The session user represents the actor for an application/session path. It is
 * separate from the model user used by generated model permissions.
 */
public interface SessionUserOps {

	/**
	 * Returns the current session user.
	 *
	 * @return the current session user, or {@code null}
	 */
	OASessionUser<?> get();

	/**
	 * Sets the current session user.
	 *
	 * @param obj the session user
	 */
	void set(OASessionUser<?> obj);
	
}
