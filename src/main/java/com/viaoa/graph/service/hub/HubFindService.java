package com.viaoa.graph.service.hub;

import java.util.logging.Logger;

import com.viaoa.filter.*;
import com.viaoa.hub.*;
import com.viaoa.object.*;

public class HubFindService {
	private final Logger LOG = Logger.getLogger(HubFindService.class.getName());

	public HubFindService() {
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
    public <T extends OAObject> T findFirst(Hub<T> thisHub, String propertyPath, final Object findValue, final boolean bSetAO, T lastFoundObject) {
        if (thisHub == null) return null;
        
        OAFinder finder = new OAFinder();
        finder.addFilter(new OALikeFilter(propertyPath, findValue));
        T foundObj = (T) finder.findNext(thisHub, (OAObject) lastFoundObject);
        
        if (bSetAO) thisHub.setAO(foundObj);
        return foundObj;
	}

    
}


