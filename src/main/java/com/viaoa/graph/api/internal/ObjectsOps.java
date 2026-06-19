package com.viaoa.graph.api.internal;

import com.viaoa.graph.api.internal.objects.OAObjectCacheOps;
import com.viaoa.graph.api.internal.objects.OAObjectCallbackOps;
import com.viaoa.graph.api.internal.objects.OAObjectDeleteOps;
import com.viaoa.graph.api.internal.objects.OAObjectReflectOps;

public interface ObjectsOps {

	public OAObjectCacheOps cache();
	
	public OAObjectReflectOps reflect();

	public OAObjectCallbackOps callbacks();
	
	public OAObjectDeleteOps delete();
	
}
