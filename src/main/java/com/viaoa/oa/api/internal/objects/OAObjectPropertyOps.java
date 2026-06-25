package com.viaoa.oa.api.internal.objects;

import com.viaoa.object.OAObject;

public interface OAObjectPropertyOps {

	public Object getProperty(OAObject oaObj, String name);
	public Object getProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef);
	public void setProperty(OAObject oaObj, String name, Object value);
	public void removeProperty(OAObject oaObj, String name, boolean bFirePropertyChange);
	public void setPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue, boolean bMustNotExist, boolean bReturnNotExist);
	
	public String[] getPropertyNames(OAObject oaObj);
	public boolean isPropertyLoaded(OAObject oaObj, String prop);
	public boolean isReferenceNull(OAObject oaObj, String prop);
	public void setReferenceable(OAObject oaObj, boolean bIsReferenceable);
	public void clearProperties(OAObject oaObj);
	
}
