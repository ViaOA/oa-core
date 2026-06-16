package com.viaoa.graph.api.services.hubs;

import com.viaoa.hub.Hub;

public interface HubDetailOps {

    /**
     * Creates a detail Hub based on the supplied master Hub and property path.
     * <p>
     * {@code detail(...)} creates a {@link Hub} that represents the objects
     * referenced by the supplied property path from the active object of the
     * master Hub. The returned Hub is a live structure that automatically updates
     * as the active object or its referenced relationships change.
     * <p>
     * The {@code path} defines the relationship traversal using model property
     * names (for example, {@code "orders"} or {@code "orders.lineItems"}).
     * <p>
     * This is the primary verb for navigating relationships and creating
     * master/detail structures within the Object Graph.
     *
     * @param hub the master Hub
     * @param path the property path used to navigate relationships
     * @return a live detail Hub based on the supplied path
     */
    Hub<?> detail(Hub<?> hub, String path);

    
	
	
}
