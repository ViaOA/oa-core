package com.viaoa.graph.api.internal.objects;

import com.viaoa.object.OAObject;

public interface OAObjectUniqueOps {

	public OAObject getUnique(final Class<? extends OAObject> clazz, final String propertyName, final Object uniqueKey, final boolean bAutoCreate);

}
