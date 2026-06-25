package com.viaoa.oa.api.services.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.hub.auto.HubAutoMatch;
import com.viaoa.object.OAObject;

public interface HubAutoMatchOps {

    /**
     * Maintains a live match between objects in one Hub and objects in another Hub
     * based on a reference property.
     * <p>
     * {@code match(...)} keeps the contents of {@code hub} synchronized so that it
     * contains only the objects whose {@code property} value matches an object in
     * {@code hubMaster}.
     * <p>
     * The {@code property} must define a relationship or reference on the objects
     * in {@code hub}. As objects in either Hub change, the match is automatically
     * re-evaluated, and the contents of {@code hub} are updated immediately without
     * requiring any manual refresh.
     * <p>
     * This is commonly used to correlate objects across Hubs based on model
     * relationships, allowing one Hub to reflect the subset of objects that match
     * another Hub.
     *
     * @param <T> the model object type contained by the target Hub
     * @param hub the target Hub whose contents are matched and maintained
     * @param property the reference property used for matching
     * @param hubMaster the source Hub providing the matching objects
     */
    <T extends OAObject, T2 extends OAObject> HubAutoMatch<T,T2> match(Hub<T> hub, String property, Hub<T2> hubMaster);    
	
}
