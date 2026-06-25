package com.viaoa.oa.api.services;

import com.viaoa.oa.api.services.hubs.*;

public interface HubsOps {
	
    public HubAutoMatchOps autoMatch();

    public HubAOOps ao();
    
    public HubDataOps data();
    
    public HubDetailOps detail();
    
    public HubFilterOps filter();
    
    public HubLinkOps link();
    
    public HubMergeOps merge();
    
    public HubShareOps share();
    
    public HubViewOps view();
    
    public HubCopyOps copy();
    
    public HubCombineOps combine();

    public HubStatusOps status();
    
    public HubRootOps root();
}
