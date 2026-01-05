package com.viaoa.graph.hub;

import java.util.logging.Logger;

import com.viaoa.filter.*;
import com.viaoa.graph.HubService;
import com.viaoa.hub.*;
import com.viaoa.object.*;

public class HubFindService {
	private final Logger LOG = Logger.getLogger(HubFindService.class.getName());

	private final HubService srvcHub;
	private final Hub.FriendAccess faHub;
	
	public HubFindService(HubService srvcHub, Hub.FriendAccess faHub ) {
    	if (srvcHub == null) throw new IllegalArgumentException("HubService can not be null");
    	this.srvcHub = srvcHub;
    	if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
    	this.faHub = faHub;
	}

	/**
	 * Finds the first object in the specified {@code Hub} whose property located by
	 * {@code propertyPath} matches the supplied {@code findValue} using a
	 * {@link com.viaoa.filter.OALikeFilter}.
	 *
	 * <p>If {@code bSetAO} is {@code true}, the found object is also set as the
	 * Hub’s active object.</p>
	 *
	 * @param thisHub the {@code Hub} to search; may be {@code null}
	 * @param propertyPath the property path to evaluate for matching
	 * @param findValue the value to compare against using a like-filter match
	 * @param bSetAO if {@code true}, sets the active object to the found object
	 * @param lastFoundObject the last object found, used by {@link com.viaoa.object.OAFinder#findNext}
	 * @return the first matching object, or {@code null} if none found
	 */
    public Object findFirst(Hub thisHub, String propertyPath, final Object findValue, final boolean bSetAO, OAObject lastFoundObject) {
        if (thisHub == null) return null;
        
        OAFinder finder = new OAFinder();
        finder.addFilter(new OALikeFilter(propertyPath, findValue));
        Object foundObj = finder.findNext(thisHub, (OAObject) lastFoundObject);
        
        if (bSetAO) thisHub.setAO(foundObj);
        return foundObj;
	}

}


