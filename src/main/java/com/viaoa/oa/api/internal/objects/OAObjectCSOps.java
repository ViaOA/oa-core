package com.viaoa.oa.api.internal.objects;

import java.util.UUID;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

public interface OAObjectCSOps {

	public void objectFinalized(UUID guid);
	public <T extends OAObject> Hub<T> getServerReferenceHub(T oaObj, String linkPropertyName);
	public boolean isServer(OAObject oaObj);
	public void updateObjectsWithoutHubs(OAObject oaObj);
	
}
