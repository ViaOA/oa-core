package com.viaoa.oa.api.internal.objects;

import com.viaoa.cascade.OACascade;
import com.viaoa.object.OAObject;

public interface OAObjectSaveOps {
	public void save(OAObject oaObj, int iCascadeRule);
	public void save(OAObject obj, int iCascadeRule, OACascade cascade);
}
