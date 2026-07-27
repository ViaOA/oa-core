package com.viaoa.oa.service;

import com.viaoa.converter.OAConv;
import com.viaoa.converter.internal.OAConverterBoolean;
import com.viaoa.hub.Hub;
import com.viaoa.lang.OAStr;
import com.viaoa.oa.OA;
import com.viaoa.oa.api.ModelUserOps;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

/**
 * Manages the ModelUser identity used by OA model permission checks.
 * <p>
 * A ModelUser is represented by a Hub whose active object is the current model
 * user. Thread-local ModelUser state is scoped by OA runtime and falls back to
 * the default ModelUser Hub when no thread-local Hub is set.
 * </p>
 */
public class OAModelUserService implements ModelUserOps {

	private final OA oa;
	private final Class<? extends OAObject> modelUserClass;
	private volatile Hub<?> hubDefaultModelUser;

	/**
	 * Property name used to evaluate ModelUser administrator permission.
	 */
	public String adminPropertyName = "admin";
	/**
	 * Property name used to evaluate ModelUser super-administrator permission.
	 */
	public String superAdminPropertyName = "superAdmin";
	/**
	 * Property name used to evaluate whether processed objects can be edited.
	 */
	public String editProcessedPropertyName  = "editProcessed";
	
	
	/**
	 * Creates a ModelUser service for an OA runtime.
	 *
	 * @param oa owning OA runtime
	 * @param modelUserClass OAObject class marked as the model user class
	 */
	public OAModelUserService(OA oa, Class<? extends OAObject> modelUserClass) {
		this.oa = oa;
		this.modelUserClass = modelUserClass;
	}

	/**
	 * Returns the OAObject class used as the model permission user.
	 *
	 * @return model user class, or {@code null} when the model has none
	 */
	@Override
	public Class<? extends OAObject> getModelUserClass() {
//qqqq verify with modelUserClass, hubDefaultModelUser, getCurrent 		
		return modelUserClass;
	}
	
	/**
	 * Returns the thread-local ModelUser Hub for this OA runtime.
	 *
	 * @return current ModelUser Hub, or {@code null} when none is set
	 */
	@Override
	public Hub<?> getCurrent() {
		return OARuntime.thread().getThreadLocalService().getModelUser(oa);
	}
	/**
	 * Sets the thread-local ModelUser Hub for this OA runtime.
	 *
	 * @param hub ModelUser Hub to make current, or {@code null} to clear
	 */
	@Override
	public void setCurrent(Hub<?> hub) {
//qqqq verify with modelUserClass, hubDefaultModelUser, getCurrent 		
		OARuntime.thread().getThreadLocalService().setModelUser(oa, hub);
	}
	/**
	 * Returns the effective ModelUser Hub, using thread-local state first and the default Hub second.
	 *
	 * @return effective ModelUser Hub, or {@code null} when no current/default Hub exists
	 */
	@Override
	public Hub<?> getCalc() {
		Hub<?> hub = getCurrent();
		if (hub != null) return hub;
		return getDefault();
	}
	
	
	

	/**
	 * Returns the default ModelUser Hub for this OA runtime.
	 *
	 * @return default ModelUser Hub, or {@code null} when none is configured
	 */
	@Override
	public Hub<?> getDefault() {
		return hubDefaultModelUser;
	}
	/**
	 * Sets the default ModelUser Hub for this OA runtime.
	 *
	 * @param hub default ModelUser Hub, or {@code null} to clear
	 */
	@Override
	public void setDefault(Hub<?> hub) {
//qqqq verify with modelUserClass, hubDefaultModelUser, getCurrent 		
		hubDefaultModelUser  = hub;
	}
	
	
	/**
	 * Returns the ModelUser property name used for administrator checks.
	 *
	 * @return admin property name
	 */
	@Override
	public String getAdminPropertyName() {
		return adminPropertyName;
	}
	/**
	 * Sets the ModelUser property name used for administrator checks.
	 *
	 * @param adminPropertyName admin property name
	 */
	@Override
	public void setAdminPropertyName(String adminPropertyName) {
		this.adminPropertyName = adminPropertyName;
	}

	/**
	 * Returns the ModelUser property name used for processed-edit checks.
	 *
	 * @return edit-processed property name
	 */
	@Override
	public String getEditProcessedPropertyName() {
		return editProcessedPropertyName;
	}
	/**
	 * Sets the ModelUser property name used for processed-edit checks.
	 *
	 * @param editProcessedPropertyName edit-processed property name
	 */
	@Override
	public void setEditProcessedPropertyName(String editProcessedPropertyName) {
		this.editProcessedPropertyName = editProcessedPropertyName;
	}
	
	
	/**
	 * Returns the ModelUser property name used for super-administrator checks.
	 *
	 * @return super-admin property name
	 */
	@Override
	public String getSuperAdminPropertyName() {
		return superAdminPropertyName;
	}
	/**
	 * Sets the ModelUser property name used for super-administrator checks.
	 *
	 * @param superAdminPropertyName super-admin property name
	 */
	@Override
	public void setSuperAdminPropertyName(String superAdminPropertyName) {
		this.superAdminPropertyName = superAdminPropertyName;
	}

	/**
	 * Returns whether the effective ModelUser active object has super-admin permission.
	 *
	 * @return {@code true} when the effective ModelUser is a super administrator
	 */
	@Override
	public boolean isSuperAdmin() {
		Hub<?> hub = getCalc();
		if (hub == null) return false;
		OAObject obj = hub.getAO();
		if (obj == null) return false;
		String s = superAdminPropertyName;
		if (OAStr.isEmpty(s)) return false;
		return OAConv.toBoolean(obj.getProperty(s));
	}

	/**
	 * Returns whether the effective ModelUser active object has administrator permission.
	 *
	 * @return {@code true} when the effective ModelUser is an administrator
	 */
	@Override
	public boolean isAdmin() {
		Hub<?> hub = getCalc();
		if (hub == null) return false;
		OAObject obj = hub.getAO();
		if (obj == null) return false;
		String s = adminPropertyName;
		if (OAStr.isEmpty(s)) return false;
		return OAConv.toBoolean(obj.getProperty(s));
	}

	/**
	 * Returns whether the effective ModelUser can edit processed objects.
	 *
	 * @return {@code true} when processed-object editing is allowed
	 */
	@Override
	public boolean canEditProcessed() {
		Hub<?> hub = getCalc();
		if (hub == null) return false;
		OAObject obj = hub.getAO();
		if (obj == null) return false;
		String s = editProcessedPropertyName;
		if (OAStr.isEmpty(s)) return false;
		return OAConv.toBoolean(obj.getProperty(s));
	}

}
