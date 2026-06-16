package com.viaoa.graph.api.services;

import com.viaoa.graph.api.services.hubs.*;

public interface HubsOps {
	
    public HubAutoMatchOps autoMatch();
    
    public HubDetailOps detail();
    
    public HubFilterOps filter();
    
    public HubLinkOps link();
    
    public HubMergeOps merge();
    
    public HubShareOps share();
    
    public HubViewOps view();

}
