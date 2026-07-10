package com.viaoa.hub;

/**
 * Internal bridge that exposes Hub friend access to OA runtime services.
 * <p>
 * This is not an application API. It allows OA service code to coordinate Hub
 * internals without making those internals public on {@link Hub}.
 */
public class HubInternalBridge {

	private final Hub.FriendAccess faHub = Hub.getFriendAccess();

	/**
	 * Creates an internal bridge for Hub friend access.
	 */
	public HubInternalBridge() {
	}

	/**
	 * Returns friend access for Hub internals.
	 *
	 * @return Hub friend access
	 */
	public Hub.FriendAccess getHubFriendAccess() {
		return faHub;
	}
}
