package com.viaoa.oa.api.services;

import com.viaoa.oa.api.services.objects.*;

public interface ObjectsOps {

	
	public OAObjectCacheOps cache();
	
	public OAObjectReflectOps reflect();

	public OAObjectCallbackOps callbacks();
	
	public OAObjectDeleteOps delete();
	
	
}
