package com.viaoa.oa.api.internal.objects;

import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;

public interface OAObjectDeleteOps {

	public OALinkInfo[] getMustBeEmptyBeforeDelete(OAObject oaObj);
	
	public void setDeleted(OAObject oaObj, boolean bDeleted);
	public void delete(OAObject oaObj);
	public void syncServerDelete(OAObject obj);
	public void syncClientDelete(OAObject obj);
	
}
