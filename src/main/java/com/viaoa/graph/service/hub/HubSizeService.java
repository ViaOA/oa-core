package com.viaoa.graph.service.hub;

import java.util.logging.Logger;

import com.viaoa.hub.*;
import com.viaoa.object.OAObject;

public abstract class HubSizeService {
	private final Logger LOG = Logger.getLogger(HubSizeService.class.getName());

	private final Hub.FriendAccess faHub;
	
	
	public HubSizeService(Hub.FriendAccess faHub) {
		if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
		this.faHub = faHub;
	}


	/**
	 * Returns the logical size of this hub. If the hub is backed by a select with
	 * more data available, counting and fetch operations are used to determine the
	 * full size. If no select applies, the in-memory object count is returned.
	 *
	 * @param thisHub the hub whose size is requested
	 * @return the number of objects the hub represents
	 */
	public int getSize(Hub<?> thisHub) {
		if (callHubSelectIsMoreData(thisHub)) {
			if (!callHubSelectIsCounted(thisHub)) {
				if (callHubDataGetCurrentSize(thisHub) == 0) {
					callHubSelectFetchMore(thisHub); // see if this will load it, before calling count on the select
					if (!callHubSelectIsMoreData(thisHub)) {
						return getSize(thisHub);
					}
				}
			}
			int x = callHubSelectGetCount(thisHub);
			if (x > 0) {
				return x;
			}
		}
		return callHubDataGetCurrentSize(thisHub);
	}

	/**
	 * Ensures that all data is loaded into the hub and then returns its size. A
	 * {@code null} hub returns zero.
	 *
	 * @param thisHub the hub whose fully loaded size is requested
	 * @return the loaded size of the hub
	 */
	public int getLoadedSize(Hub thisHub) {
		if (thisHub == null) {
			return 0;
		}
		thisHub.loadAllData();
		return getSize(thisHub);
	}

	private int cntLoadedSizeError;

	
	
	public abstract boolean callHubSelectIsMoreData(Hub<?> thisHub);
	public abstract <T extends OAObject> boolean callHubSelectIsCounted(Hub<T> thisHub);
	public abstract int callHubDataGetCurrentSize(Hub<?> thisHub);
	public abstract <T extends OAObject> int callHubSelectFetchMore(Hub<T> thisHub);	
	public abstract <T extends OAObject> int callHubSelectGetCount(Hub<T> thisHub);

	
	
	
	
	
	
	
}
