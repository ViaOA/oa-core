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

public class ProductionAreaModel extends OAObjectModel {
    private static Logger LOG = Logger.getLogger(ProductionAreaModel.class.getName());
    
    // Hubs
    protected Hub<ProductionArea> hub;
    // selected productionAreas
    protected Hub<ProductionArea> hubMultiSelect;
    // detail hubs
    protected Hub<Item> hubItems;
    
    // ObjectModels
    protected ItemModel modelItems;
    
    // SearchModels used for references
    protected ItemSearchModel modelItemsSearch;
    
    public ProductionAreaModel() {
        setDisplayName("Production Area");
        setPluralDisplayName("Production Areas");
    }
    
    public ProductionAreaModel(Hub<ProductionArea> hubProductionArea) {
        this();
        if (hubProductionArea != null) HubDelegate.setObjectClass(hubProductionArea, ProductionArea.class);
        this.hub = hubProductionArea;
    }
    public ProductionAreaModel(ProductionArea productionArea) {
        this();
        getHub().add(productionArea);
        getHub().setPos(0);
    }
    
    public Hub<ProductionArea> getOriginalHub() {
        return getHub();
    }
    
    public Hub<Item> getItems() {
        if (hubItems == null) {
            hubItems = getHub().getDetailHub(ProductionArea.P_Items);
        }
        return hubItems;
    }
    public ProductionArea getProductionArea() {
        return getHub().getAO();
    }
    
    public Hub<ProductionArea> getHub() {
        if (hub == null) {
            hub = new Hub<ProductionArea>(ProductionArea.class);
        }
        return hub;
    }
    
    public Hub<ProductionArea> getMultiSelectHub() {
        if (hubMultiSelect == null) {
            hubMultiSelect = new Hub<ProductionArea>(ProductionArea.class);
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
        modelItems.setAllowNew(false);
        modelItems.setAllowSave(true);
        modelItems.setAllowAdd(true);
        modelItems.setAllowMove(false);
        modelItems.setAllowRemove(true);
        modelItems.setAllowDelete(false);
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
        // call ProductionArea.onEditQueryItems(ItemModel) to be able to customize this model
        OAObjectCallbackDelegate.onObjectCallbackModel(ProductionArea.class, ProductionArea.P_Items, modelItems);
    
        return modelItems;
    }
    
    public ItemSearchModel getItemsSearchModel() {
        if (modelItemsSearch != null) return modelItemsSearch;
        modelItemsSearch = new ItemSearchModel();
        return modelItemsSearch;
    }
    
    public HubCopy<ProductionArea> createHubCopy() {
        Hub<ProductionArea> hubProductionAreax = new Hub<>(ProductionArea.class);
        HubCopy<ProductionArea> hc = new HubCopy<>(getHub(), hubProductionAreax, true);
        return hc;
    }
    public ProductionAreaModel createCopy() {
        ProductionAreaModel mod = new ProductionAreaModel(createHubCopy().getHub());
        return mod;
    }
}

