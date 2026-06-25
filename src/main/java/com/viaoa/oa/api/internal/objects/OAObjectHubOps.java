package com.viaoa.oa.api.internal.objects;

import java.lang.ref.WeakReference;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

public interface OAObjectHubOps {

	public WeakReference<Hub<? extends OAObject>>[] getHubReferencesNoCopy(OAObject oaObj);
	public <T extends OAObject> Hub<T>[] getHubReferences(T oaObj);
	public <T extends OAObject> boolean addHub(T oaObj, Hub<T> hub, boolean bAlwaysAddIfM2M);
	public boolean isInHubWithMaster(OAObject obj);
	public <T extends OAObject> void removeHub(final T oaObj, Hub<T> hub, boolean bIsOnHubFinalize);
	
}
