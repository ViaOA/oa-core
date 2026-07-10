package com.viaoa.oa.api.services;

import com.viaoa.oa.api.services.hubs.*;

/**
 * Public OA Hub service families exposed through {@code OA.services().hubs()}.
 * <p>
 * This interface is the curated service boundary for advanced Hub operations
 * that are intended for application and framework use. Lower-level Hub runtime
 * machinery remains under {@code OA.internal().hubs()}.
 */
public interface HubsOps {
	
    /**
     * Returns live auto-match operations for relationship-based Hub matching.
     *
     * @return the auto-match service facade
     */
    public HubAutoMatchOps autoMatch();

    /**
     * Returns active-object operations for Hubs.
     *
     * @return the active-object service facade
     */
    public HubAOOps ao();
    
    /**
     * Returns selected Hub data operations.
     *
     * @return the Hub data service facade
     */
    public HubDataOps data();
    
    /**
     * Returns master/detail relationship navigation operations.
     *
     * @return the detail service facade
     */
    public HubDetailOps detail();
    
    /**
     * Returns live Hub filtering operations.
     *
     * @return the filter service facade
     */
    public HubFilterOps filter();
    
    /**
     * Returns linked-Hub synchronization operations.
     *
     * @return the link service facade
     */
    public HubLinkOps link();
    
    /**
     * Returns live relationship-path merge operations.
     *
     * @return the merge service facade
     */
    public HubMergeOps merge();
    
    /**
     * Returns shared-Hub operations.
     *
     * @return the share service facade
     */
    public HubShareOps share();
    
    /**
     * Returns grouped, flattened, and joined Hub view operations.
     *
     * @return the view service facade
     */
    public HubViewOps view();
    
    /**
     * Returns live Hub copy operations.
     *
     * @return the copy service facade
     */
    public HubCopyOps copy();
    
    /**
     * Returns live Hub combine operations.
     *
     * @return the combine service facade
     */
    public HubCombineOps combine();

    /**
     * Returns Hub status and state-comparison operations.
     *
     * @return the status service facade
     */
    public HubStatusOps status();
    
    /**
     * Returns root-Hub operations.
     *
     * @return the root service facade
     */
    public HubRootOps root();
}
