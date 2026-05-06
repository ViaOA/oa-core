package com.viaoa.graph.service.hub;

import java.util.logging.Logger;
import com.viaoa.hub.*;
import com.viaoa.hub.auto.HubAutoSequence;
import com.viaoa.object.OAObject;

public abstract class HubSequenceService {
	private final Logger LOG = Logger.getLogger(HubSequenceService.class.getName());

	private final Hub.FriendAccess faHub;

	public HubSequenceService(Hub.FriendAccess faHub) {
		if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
		this.faHub = faHub;
	}


	/**
	 * Enables automatic sequencing of objects in this hub by assigning sequential
	 * values to the specified property. Existing auto-sequence handlers are closed
	 * before creating a new one. Sorting is canceled to preserve sequence order.
	 * When the hub is a detail hub, sequencing is only enabled on the server side.
	 *
	 * @param thisHub     the hub whose objects will receive sequence values
	 * @param property    the property to update with sequence numbers
	 * @param startNumber the initial sequence number
	 * @param bKeepSeq    whether sequence values are preserved after removals
	 */
	public <T extends OAObject> void setAutoSequence(Hub<T> thisHub, String property, int startNumber, boolean bKeepSeq) {
		// 20091030 only set for server for detail hubs
		boolean bServerOnly = false;
		if (thisHub.getMasterObject() != null) {
			if (!callHubCSIsServer(thisHub)) {
				return; // only set up for server
			}
			bServerOnly = true;
		}
		final HubData<T> hd = faHub.getHubData(thisHub);
		if (hd.getAutoSequence() != null) {
			hd.getAutoSequence().close();
		}
		
		callHubSortCancelSort(thisHub); // 20090801 need to remove any sorters
		hd.setAutoSequence(new HubAutoSequence(thisHub, property, startNumber, bKeepSeq, bServerOnly));
	}

	/**
	 * Returns the auto-sequence controller for this hub, or {@code null} if none is
	 * assigned.
	 *
	 * @param thisHub the hub whose auto-sequence handler is requested
	 * @return the auto-sequence object, or {@code null} if not configured
	 */
	public <T extends OAObject> HubAutoSequence getAutoSequence(Hub<T> thisHub) {
		final HubData<T> hd = faHub.getHubData(thisHub);
		return hd.getAutoSequence();
	}

	/**
	 * Recomputes sequence values for all objects in this hub when auto-sequence is
	 * enabled. If no auto-sequence handler exists, no action is taken.
	 *
	 * @param thisHub the hub whose sequence values will be recalculated
	 */
	public <T extends OAObject> void resequence(Hub<T> thisHub) {
		final HubData<T> hd = faHub.getHubData(thisHub);
		if (hd.getAutoSequence() != null) {
			hd.getAutoSequence().resequence();
		}
	}

	public abstract boolean callHubCSIsServer(Hub<?> thisHub);
	public abstract void callHubSortCancelSort(Hub<?> hub);

}
