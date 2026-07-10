package com.viaoa.object;

import com.viaoa.metadata.OACalcInfo;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.metadata.OAPropertyInfo;
import com.viaoa.serialize.OAObjectSerializer;

/**
 * Internal bridge that exposes package-protected friend access objects to OA
 * runtime services that must coordinate object, metadata, and serialization
 * internals.
 * <p>
 * This is not an application API. It exists so OA service layers can work with
 * internal state without widening the public surface of the model classes.
 */
public class OAObjectInternalBridge {

	private final OAObject.FriendAccess faObject = OAObject.getFriendAccess();
	
	private final OAObjectInfo.FriendAccess faObjectInfo = OAObjectInfo.getFriendAccess();
	private final OAPropertyInfo.FriendAccess faPropertyInfo = OAPropertyInfo.getFriendAccess();
	private final OALinkInfo.FriendAccess faLinkInfo = OALinkInfo.getFriendAccess();
	private final OACalcInfo.FriendAccess faCalcInfo = OACalcInfo.getFriendAccess();
	private final OAObjectSerializer.FriendAccess faObjectSerializer = OAObjectSerializer.getFriendAccess();
	
	/**
	 * Creates an internal bridge for OA runtime friend access.
	 */
	public OAObjectInternalBridge() {
	}
	
	/**
	 * Returns friend access for OAObject internals.
	 *
	 * @return OAObject friend access
	 */
	public OAObject.FriendAccess getObjectFriendAccess() {
		return faObject;
	}
	
	/**
	 * Returns friend access for object metadata internals.
	 *
	 * @return OAObjectInfo friend access
	 */
	public OAObjectInfo.FriendAccess getObjectInfoFriendAccess() {
		return faObjectInfo;
	}
	
	/**
	 * Returns friend access for property metadata internals.
	 *
	 * @return OAPropertyInfo friend access
	 */
	public OAPropertyInfo.FriendAccess getPropertyInfoFriendAccess() {
		return faPropertyInfo;
	}
	
	/**
	 * Returns friend access for link metadata internals.
	 *
	 * @return OALinkInfo friend access
	 */
	public OALinkInfo.FriendAccess getLinkInfoFriendAccess() {
		return faLinkInfo;
	}

	/**
	 * Returns friend access for calculated-property metadata internals.
	 *
	 * @return OACalcInfo friend access
	 */
	public OACalcInfo.FriendAccess getCalcInfoFriendAccess() {
		return faCalcInfo;
	}

	/**
	 * Returns friend access for object serialization internals.
	 *
	 * @return OAObjectSerializer friend access
	 */
	public OAObjectSerializer.FriendAccess getObjectSerializerFriendAccess() {
		return faObjectSerializer;
	}
}
