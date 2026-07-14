package com.viaoa.oa.api.internal.objects;

import com.viaoa.callback.OACallback;
import com.viaoa.cascade.OACascade;
import com.viaoa.object.OAObject;

/**
 * 
 */
public interface OAObjectRecurseOps {

	public <T extends OAObject> void recurse(T oaObj, OACallback<OAObject> callback, OACascade cascade);

}


