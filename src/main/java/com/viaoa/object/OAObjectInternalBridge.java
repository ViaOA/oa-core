package com.viaoa.object;

import com.viaoa.metadata.OACalcInfo;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.metadata.OAPropertyInfo;
import com.viaoa.serialize.OAObjectSerializer;

// friend access to classes that have FriendAccess innerclass to access package protected properties and methods.
// OA internal — not public API — subject to chan
public class OAObjectInternalBridge {

	private final OAObject.FriendAccess faObject = OAObject.getFriendAccess();
	
	private final OAObjectInfo.FriendAccess faObjectInfo = OAObjectInfo.getFriendAccess();
	private final OAPropertyInfo.FriendAccess faPropertyInfo = OAPropertyInfo.getFriendAccess();
	private final OALinkInfo.FriendAccess faLinkInfo = OALinkInfo.getFriendAccess();
	private final OACalcInfo.FriendAccess faCalcInfo = OACalcInfo.getFriendAccess();
	private final OAObjectSerializer.FriendAccess faObjectSerializer = OAObjectSerializer.getFriendAccess();
	
	public OAObjectInternalBridge() {
	}
	
	public OAObject.FriendAccess getObjectFriendAccess() {
		return faObject;
	}
	
	public OAObjectInfo.FriendAccess getObjectInfoFriendAccess() {
		return faObjectInfo;
	}
	
	public OAPropertyInfo.FriendAccess getPropertyInfoFriendAccess() {
		return faPropertyInfo;
	}
	
	public OALinkInfo.FriendAccess getLinkInfoFriendAccess() {
		return faLinkInfo;
	}

	public OACalcInfo.FriendAccess getCalcInfoFriendAccess() {
		return faCalcInfo;
	}

	public OAObjectSerializer.FriendAccess getObjectSerializerFriendAccess() {
		return faObjectSerializer;
	}
}
