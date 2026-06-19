package com.viaoa.graph.api.services.objects;

import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;

public interface OAObjectDeleteOps {

	public OALinkInfo[] getMustBeEmptyBeforeDelete(OAObject oaObj);
}
