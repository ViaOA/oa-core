package com.viaoa.oa.api.internal.objects;

import com.viaoa.object.OAObject;

public interface OAObjectFindOps {

	public OAObject[] find(OAObject base, String propertyPath, Object findValue, boolean bFindAll);
}
