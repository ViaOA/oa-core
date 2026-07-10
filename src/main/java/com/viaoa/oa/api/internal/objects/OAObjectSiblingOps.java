package com.viaoa.oa.api.internal.objects;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;

/**
 * Internal sibling-key lookup used for OAObject relationship synchronization.
 */
public interface OAObjectSiblingOps {

	/**
	 * Returns sibling object keys for a relationship.
	 *
	 * @param oaObj the source object
	 * @param property the relationship property
	 * @param maxAmount the maximum number of keys to return
	 * @param hmIgnoreSibling sibling GUIDs to ignore during lookup
	 * @return sibling object keys
	 */
	public OAObjectKey[] getSiblings(OAObject oaObj, String property, int maxAmount, ConcurrentHashMap<UUID, Boolean> hmIgnoreSibling);
	
}
