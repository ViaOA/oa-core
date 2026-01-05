package com.viaoa.hub;

// friend access to classes that have FriendAccess innerclass to access package protected properties and methods.
// OA internal — not public API — subject to chan
public class HubInternalBridge {

	private final Hub.FriendAccess faHub = Hub.getFriendAccess();

/*	
	private final HubData.FriendAccess faHubData = HubData.getFriendAccess();
	private final HubDatax.FriendAccess faHubDatax = HubDatax.getFriendAccess();
	private final HubDataActive.FriendAccess faHubDataActive = HubDataActive.getFriendAccess();
	private final HubDataMaster.FriendAccess faHubDataMaster = HubDataMaster.getFriendAccess();
	private final HubDataUnique.FriendAccess faHubDataUnique = HubDataUnique.getFriendAccess();
	private final HubDataUniquex.FriendAccess faHubDataUniquex = HubDataUniquex.getFriendAccess();
*/	
//	private final HubDetail.FriendAccess faHubDetail = HubDetail.getFriendAccess();
	
	public HubInternalBridge() {
	}

	public Hub.FriendAccess getHubFriendAccess() {
		return faHub;
	}
/*	
	public HubData.FriendAccess getHubDataFriendAccess() {
		return faHubData;
	}

	public HubDatax.FriendAccess getHubDataxFriendAccess() {
		return faHubDatax;
	}
	
	public HubDataActive.FriendAccess getHubDataActiveFriendAccess() {
		return faHubDataActive;
	}

	public HubDataMaster.FriendAccess getHubDataMasterFriendAccess() {
		return faHubDataMaster;
	}
	
	public HubDataUnique.FriendAccess getHubDataUniqueFriendAccess() {
		return faHubDataUnique;
	}

	public HubDataUniquex.FriendAccess getHubDataUniquexFriendAccess() {
		return faHubDataUniquex;
	}
*/	
/*	
	public HubDetail.FriendAccess getHubDetailFriendAccess() {
		return faHubDetail;
	}
*/	
	
	
	
}
