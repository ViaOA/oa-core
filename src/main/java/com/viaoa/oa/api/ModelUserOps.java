package com.viaoa.oa.api;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

public interface ModelUserOps {

	Class<? extends OAObject> getModelUserClass();

	// note: "catch all"
	Hub<?> getDefault();
	void setDefault(Hub<?> hub);
	
	// note: uses OAThreadLocal to store value
	Hub<?> getCurrent();
	void setCurrent(Hub<?> hub);
	
	 Hub<?> getCalc();

	
	String getAdminPropertyName();
	void setAdminPropertyName(String adminPropertyName);

	String getEditProcessedPropertyName();
	void setEditProcessedPropertyName(String editProcessedPropertyName);
	
	String getSuperAdminPropertyName();
	void setSuperAdminPropertyName(String superAdminPropertyName);

	boolean isSuperAdmin();
	
	boolean isAdmin();
	
	boolean canEditProcessed();
	
}
