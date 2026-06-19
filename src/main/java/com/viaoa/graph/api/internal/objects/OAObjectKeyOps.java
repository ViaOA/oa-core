package com.viaoa.graph.api.internal.objects;

import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;

public interface OAObjectKeyOps {

	public OAObjectKey getKey(OAObject oaObj);
	public OAObjectKey createObjectKey(OAObject oaObj);
	public OAObjectKey createObjectKey(Class<? extends OAObject> clazz, final Object ...ids);
	public boolean isForSameOAObject(final Class<? extends OAObject> clazz, final OAObjectKey ok1, final OAObjectKey ok2);
	public OAObjectKey createObjectKey(Object id);
	
}
