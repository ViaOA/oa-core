package com.viaoa.oa.api;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

/**
 * Public OA model-user operations.
 * <p>
 * The model user is the OAObject type used by generated model permissions for
 * checks such as visible, enabled, admin, super-admin, and edit-processed. The
 * current model user is represented by the active object of a Hub so OA runtime
 * services can observe and react to model-user changes.
 */
public interface ModelUserOps {

	/**
	 * Returns the model-user class for this OA model.
	 *
	 * @return the model-user class, or {@code null} when the model has none
	 */
	Class<? extends OAObject> getModelUserClass();

	// Default model-user Hub used when no thread-local/current Hub is set.
	/**
	 * Returns the default model-user Hub.
	 *
	 * @return the default model-user Hub, or {@code null}
	 */
	Hub<?> getDefault();

	/**
	 * Sets the default model-user Hub.
	 *
	 * @param hub the default model-user Hub
	 */
	void setDefault(Hub<?> hub);
	
	// Current model-user Hub is stored per OA execution context.
	/**
	 * Returns the current model-user Hub for this execution context.
	 *
	 * @return the current model-user Hub, or {@code null}
	 */
	Hub<?> getCurrent();

	/**
	 * Sets the current model-user Hub for this execution context.
	 *
	 * @param hub the current model-user Hub
	 */
	void setCurrent(Hub<?> hub);
	
	/**
	 * Returns the calculated model-user Hub, using the current Hub first and the
	 * default Hub as fallback.
	 *
	 * @return the resolved model-user Hub, or {@code null}
	 */
	 Hub<?> getCalc();

	
	/**
	 * Returns the property name used to determine model-user admin status.
	 *
	 * @return the admin property name
	 */
	String getAdminPropertyName();

	/**
	 * Sets the property name used to determine model-user admin status.
	 *
	 * @param adminPropertyName the admin property name
	 */
	void setAdminPropertyName(String adminPropertyName);

	/**
	 * Returns the property name used to determine whether processed objects can be edited.
	 *
	 * @return the edit-processed property name
	 */
	String getEditProcessedPropertyName();

	/**
	 * Sets the property name used to determine whether processed objects can be edited.
	 *
	 * @param editProcessedPropertyName the edit-processed property name
	 */
	void setEditProcessedPropertyName(String editProcessedPropertyName);
	
	/**
	 * Returns the property name used to determine model-user super-admin status.
	 *
	 * @return the super-admin property name
	 */
	String getSuperAdminPropertyName();

	/**
	 * Sets the property name used to determine model-user super-admin status.
	 *
	 * @param superAdminPropertyName the super-admin property name
	 */
	void setSuperAdminPropertyName(String superAdminPropertyName);

	/**
	 * Returns whether the calculated model user is a super admin.
	 *
	 * @return {@code true} if the calculated model user is super admin
	 */
	boolean isSuperAdmin();
	
	/**
	 * Returns whether the calculated model user is an admin.
	 *
	 * @return {@code true} if the calculated model user is admin
	 */
	boolean isAdmin();
	
	/**
	 * Returns whether the calculated model user can edit processed objects.
	 *
	 * @return {@code true} if processed objects can be edited
	 */
	boolean canEditProcessed();
	
}
