package com.viaoa.graph.api.internal.objects;

import com.viaoa.object.OAObject;

public interface OAObjectEventOps {

	public void fireBeforePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged);
	public void firePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged);
	public void firePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged, boolean bUnknownValues);
	public void fireAfterLoadEvent(OAObject oaObj);
	
}
