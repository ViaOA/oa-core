package com.viaoa.graph.api.internal.objects;

import com.viaoa.object.OAObject;

public interface OAObjectUniqueOps {

	public <T extends OAObject> T getUnique(Class<T> clazz, String propertyName, Object uniqueKey, boolean bAutoCreate);

}
