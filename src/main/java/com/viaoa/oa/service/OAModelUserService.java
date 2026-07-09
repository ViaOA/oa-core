package com.viaoa.oa.service;

import com.viaoa.converter.OAConv;
import com.viaoa.converter.internal.OAConverterBoolean;
import com.viaoa.hub.Hub;
import com.viaoa.lang.OAStr;
import com.viaoa.oa.OA;
import com.viaoa.oa.api.ModelUserOps;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

public class OAModelUserService implements ModelUserOps {

	private final OA oa;
	private final Class<? extends OAObject> modelUserClass;
	private volatile Hub<?> hubDefaultModelUser;

	public String adminPropertyName = "admin";
	public String superAdminPropertyName = "superAdmin";
	public String editProcessedPropertyName  = "editProcessed";
	
	
	public OAModelUserService(OA oa, Class<? extends OAObject> modelUserClass) {
		this.oa = oa;
		this.modelUserClass = modelUserClass;
	}

	@Override
	public Class<? extends OAObject> getModelUserClass() {
//qqqq verify with modelUserClass, hubDefaultModelUser, getCurrent 		
		return modelUserClass;
	}
	
	@Override
	public Hub<?> getCurrent() {
		return OARuntime.thread().getThreadLocalService().getModelUserHub(oa);
	}
	@Override
	public void setCurrent(Hub<?> hub) {
//qqqq verify with modelUserClass, hubDefaultModelUser, getCurrent 		
		OARuntime.thread().getThreadLocalService().setModelUserHub(oa, hub);
	}
	@Override
	public Hub<?> getCalc() {
		Hub<?> hub = getCurrent();
		if (hub != null) return hub;
		return getDefault();
	}
	
	
	

	@Override
	public Hub<?> getDefault() {
		return hubDefaultModelUser;
	}
	@Override
	public void setDefault(Hub<?> hub) {
//qqqq verify with modelUserClass, hubDefaultModelUser, getCurrent 		
		hubDefaultModelUser  = hub;
	}
	
	
	@Override
	public String getAdminPropertyName() {
		return adminPropertyName;
	}
	@Override
	public void setAdminPropertyName(String adminPropertyName) {
		this.adminPropertyName = adminPropertyName;
	}

	@Override
	public String getEditProcessedPropertyName() {
		return editProcessedPropertyName;
	}
	@Override
	public void setEditProcessedPropertyName(String editProcessedPropertyName) {
		this.editProcessedPropertyName = editProcessedPropertyName;
	}
	
	
	@Override
	public String getSuperAdminPropertyName() {
		return superAdminPropertyName;
	}
	@Override
	public void setSuperAdminPropertyName(String superAdminPropertyName) {
		this.superAdminPropertyName = superAdminPropertyName;
	}

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
