package com.viaoa.oa.api.internal.objects;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;

public interface OAObjectSiblingOps {

	public OAObjectKey[] getSiblings(OAObject oaObj, String property, int maxAmount, ConcurrentHashMap<UUID, Boolean> hmIgnoreSibling);
	
}
