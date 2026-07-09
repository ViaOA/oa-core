package com.viaoa.oa.api.internal.objects;

import java.lang.reflect.Method;

import com.viaoa.hub.Hub;
import com.viaoa.metadata.OACalcInfo;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.metadata.OAPropertyInfo;
import com.viaoa.object.OAObject;

public interface OAObjectInfoOps {

	public OAObjectInfo getOAObjectInfo(Class<?> clazz);
	public Class<? extends OAObject> getHubPropertyClass(Class<? extends OAObject> clazz, String propertyName);
	public Class<?> getPropertyClass(Class<? extends OAObject> clazz, String propertyName);
	public Class<?> getPropertyClass(OAObjectInfo oi, String propertyName);
	public boolean isHubProperty(OAObjectInfo oi, String propertyName);
	public OACalcInfo getCalcInfo(OAObjectInfo oi, String name);
	public OALinkInfo getLinkInfo(OAObjectInfo oi, String propertyName);
	public OAObjectInfo getOAObjectInfo(OAObject oaObj);
	public boolean cacheHub(OALinkInfo linkInfo, Hub<?> hub);
	public Method getMethod(OAObjectInfo oi, String string);
	public OALinkInfo getRecursiveLinkInfo(OAObjectInfo oi, int type);
	public Method getMethod(Class<?> clazz, String methodName);
	public OALinkInfo getReverseLinkInfo(OALinkInfo li);
	public OAObjectInfo getObjectInfo(Class<?> clazz);
	public boolean isMany2Many(OALinkInfo li);
	public OALinkInfo getLinkInfo(Class<? extends OAObject> clazz, String property);
	public Method getMethod(OAObjectInfo oi, String methodName, int argumentCount);
	public OAPropertyInfo getPropertyInfo(OAObjectInfo oi, String propertyName);
	public boolean isPojoSingleton(OAObjectInfo toObjectInfo);
	public Class<? extends OAObject>[] getAllClasses();

	
}
