// Generated by OABuilder

package com.cdi.model;

import java.util.logging.*;
import com.viaoa.object.*;
import com.viaoa.annotation.*;
import com.viaoa.datasource.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.util.filter.*;
import com.cdi.model.oa.*;
import com.cdi.model.oa.propertypath.*;
import com.cdi.model.oa.search.*;
import com.cdi.model.oa.filter.*;
import com.cdi.model.search.*;
import com.cdi.model.filter.*;
import com.cdi.delegate.ModelDelegate;
import com.cdi.resource.Resource;

public class MoldModel extends OAObjectModel {
    private static Logger LOG = Logger.getLogger(MoldModel.class.getName());
    
    // Hubs
    protected Hub<Mold> hub;
    // selected molds
    protected Hub<Mold> hubMultiSelect;
    // detail hubs
    protected Hub<Item> hubItems;
    
    // ObjectModels
    protected ItemModel modelItems;
    
    public MoldModel() {
        setDisplayName("Mold");
        setPluralDisplayName("Molds");
    }
    
    public MoldModel(Hub<Mold> hubMold) {
        this();
        if (hubMold != null) HubDelegate.setObjectClass(hubMold, Mold.class);
        this.hub = hubMold;
    }
    public MoldModel(Mold mold) {
        this();
        getHub().add(mold);
        getHub().setPos(0);
    }
    
    public Hub<Mold> getOriginalHub() {
        return getHub();
    }
    
    public Hub<Item> getItems() {
        if (hubItems == null) {
            hubItems = getHub().getDetailHub(Mold.P_Items);
        }
        return hubItems;
    }
    public Mold getMold() {
        return getHub().getAO();
    }
    
    public Hub<Mold> getHub() {
        if (hub == null) {
            hub = new Hub<Mold>(Mold.class);
        }
        return hub;
    }
    
    public Hub<Mold> getMultiSelectHub() {
        if (hubMultiSelect == null) {
            hubMultiSelect = new Hub<Mold>(Mold.class);
        }
        return hubMultiSelect;
    }
    
    public ItemModel getItemsModel() {
        if (modelItems != null) return modelItems;
        modelItems = new ItemModel(getItems());
        modelItems.setDisplayName("Item");
        modelItems.setPluralDisplayName("Items");
        if (HubDetailDelegate.getLinkInfoFromMasterToDetail(getOriginalHub().getMasterHub()) == HubDetailDelegate.getLinkInfoFromMasterToDetail(getItems())) {
            modelItems.setCreateUI(false);
        }
        modelItems.setForJfc(getForJfc());
        modelItems.setAllowNew(true);
        modelItems.setAllowSave(true);
        modelItems.setAllowAdd(false);
        modelItems.setAllowMove(false);
        modelItems.setAllowRemove(false);
        modelItems.setAllowDelete(true);
        modelItems.setAllowSearch(false);
        modelItems.setAllowHubSearch(true);
        modelItems.setAllowGotoEdit(true);
        modelItems.setViewOnly(getViewOnly());
        modelItems.setAllowTableFilter(true);
        modelItems.setAllowTableSorting(true);
         // default is always false for these, can be turned by custom code in editQuery call (below)
        modelItems.setAllowMultiSelect(false);
        modelItems.setAllowCopy(false);
        modelItems.setAllowCut(false);
        modelItems.setAllowPaste(false);
        // call Mold.onEditQueryItems(ItemModel) to be able to customize this model
        OAObjectCallbackDelegate.onObjectCallbackModel(Mold.class, Mold.P_Items, modelItems);
    
        return modelItems;
    }
    
    public HubCopy<Mold> createHubCopy() {
        Hub<Mold> hubMoldx = new Hub<>(Mold.class);
        HubCopy<Mold> hc = new HubCopy<>(getHub(), hubMoldx, true);
        return hc;
    }
    public MoldModel createCopy() {
        MoldModel mod = new MoldModel(createHubCopy().getHub());
        return mod;
    }
}

