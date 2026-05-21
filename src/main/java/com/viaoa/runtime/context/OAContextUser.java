package com.viaoa.runtime.context;

import com.viaoa.converter.OAConv;
import com.viaoa.hub.Hub;
import com.viaoa.lang.OAString;
import com.viaoa.object.OAObject;

public class OAContextUser<T extends OAObject> {

	private OAContext<?, T> context;
	private final T userObject;
	private final Hub<T> userHub;
	
	public OAContextUser(OAContext<?, T> context, T userObject) {
		this.context = context;
		this.userObject = userObject;
		this.userHub = null;
	}

	public OAContextUser(OAContext<?, T> context, Hub<T> userHub) {
		this.context = context;
		this.userObject = null;
		this.userHub = userHub;
	}

	public OAContextUser(OAContext<?, T> context) {
		this.context = context;
		this.userObject = null;
		this.userHub = null;
	}
	
	public OAContext<?, T> getContext() {
		return context;
	}
	
	public T getCurrentUserObject() {
		if (userObject != null) return userObject;
		if (userHub == null) return null;
		return userHub.getActiveObject();
	}

	public Hub<T> getUserHub() {
		return userHub;
	}
	public T getUserObject() {
		return userObject;
	}
	
	/**
	 * Evaluates whether the specified context has admin rights. Applies special
	 * server-thread rules when context is null.
	 *
	 * @param context context key
	 * @return true if admin; false otherwise
	 */
	public boolean isAdmin() {
		return isEnabled(context.getAdminPath(), true);
	}
	
	public boolean getAllowEditProcessed() {
		return isEnabled(context.getAllowEditProcessedPath(), true);
	}
	
	/**
	 * Returns whether the current thread’s context has super-admin rights.
	 *
	 * @return true if super-admin; false otherwise
	 */
	public boolean isSuperAdmin() {
		return isEnabled(context.getSuperAdminPath(), true, false);
	}
	
	
	
	
	/**
	 * Determines whether the property at the given path for the current context
	 * equals the specified boolean value. Delegates to
	 * {@link #isEnabled(Object, String, boolean)}.
	 *
	 * @param pp property path
	 * @param bEqualTo required boolean value
	 * @return true if property equals bEqualTo; false otherwise
	 */
	public boolean isEnabled(final String path, final boolean bEqualTo) {
		return isEnabled(path, bEqualTo, true);
	}
	public boolean isEnabled(final String path, final boolean bEqualTo, final boolean bCheckSuperAdmin) {
		if (OAString.isEmpty(path)) return false;
		OAObject oaObj = getCurrentUserObject();
		if (oaObj == null) return false;

		Object val = oaObj.getProperty(path);
		boolean b = OAConv.toBoolean(val);
		b = (b == bEqualTo);
		if (bCheckSuperAdmin) b = b || isSuperAdmin();
		return b;
	}
	
	
}
