package com.viaoa.metadata;

import com.viaoa.hub.Hub;
import com.viaoa.hub.link.HubSelectLink;
import com.viaoa.object.OAObject;
import com.viaoa.select.OASelect;

public class OAObjectSearchModel<T extends OAObject> {
    protected final OAObjectModel<T> model;

    protected Hub<T> hubSearchResults;

    protected final OAObjectSearchModelIR<T> ir;
    
    public static enum Type {
        DefaultSearch,   // based on IR config findings
        HubContextSharedWithSearchResults,  // model.hubContext.setShared(this.hubSearchResults)
        InnerHubContextSearch // this.select.hubSearch=model.hubContext 
    }

    protected final Type type;
    
    protected OAObjectSearchModel(final OAObjectModel<T> model, Type type) {
        this.model = model;
        this.type = type == null ? Type.DefaultSearch : type;
        ir = new OAObjectSearchModelIR<>(this); 
    }
    
    public OAObjectModel<T> getModel() {
        return this.model;
    }

    public Hub<T> getSearchResults() {
        if (hubSearchResults == null) {
            hubSearchResults = new Hub<T>(model.ir.ctype);
        }
        return hubSearchResults;
    }

    public Type getType() {
        return this.type;
    }
    
    public void beforeInput() {
        // hook that is called before search input starts
    }

    public void performSearch() {
        OASelect<T> sel = getSelect();
        ir.prepareForSelect(sel);
        getSearchResults().select(sel);
    }
    
    public OASelect<T> getSelect() {
        OASelect<T> sel = new OASelect<>(model.ir.ctype);
        return sel;
    }
    
    public void clearResults() {
        cancelSelect();
        if (hubSearchResults != null) hubSearchResults.clear();
    }

    /**
     * Clear the search values.
     */
    public void clearSearchValues() {
    }
    
    public void cancelSelect() {
        getSearchResults().cancelSelect();
    }

    /**
     * Uses HubSelectLink to update wired Hubs.
     */
    public void updateUsingSearchResults() {
        Hub<T> hub = getSearchResults();
        if (hub != null) {
            HubSelectLink<T, ? extends OAObject> hsl = hub.getSelectLink();
            if (hsl != null) hsl.select();
        }        
    }
    
    public void updateUsingMultiSelect() {
        Hub<T> hub = model.getMultiSelectHub();
        if (hub != null) {
            HubSelectLink<T, ? extends OAObject> hsl = hub.getSelectLink();
            if (hsl != null) hsl.select();
        }        
    }


    /*
     * qqqqqqqqqqqqqqqq WIP ... more to do
     * 
     * recursive Hubs, root Hubs for trees ex: ItemModel.getItemCategoryHub,
     * getRootItemCategoryHub > ItemCategoryModel needs rootHub, and regular Hub
     * (recursive)
     * 
     * Calendar related ... example??
     * 
     * Need close()
     * 
     * The class creates: - HubSelectLink target listeners; - HubShareOnAOChange
     * source listeners; - SearchFrom new-list callbacks; - HubMergers. the
     * HubMergers are not retained. HubMerger itself supports close()
     * 
     */
}
