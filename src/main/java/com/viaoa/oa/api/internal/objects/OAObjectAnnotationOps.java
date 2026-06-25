package com.viaoa.oa.api.internal.objects;

import java.lang.reflect.Method;

import com.viaoa.annotation.OAMany;
import com.viaoa.object.OAObject;

public interface OAObjectAnnotationOps {
	public Class<? extends OAObject> getHubObjectClass(OAMany annotation, Method method);
	
}
