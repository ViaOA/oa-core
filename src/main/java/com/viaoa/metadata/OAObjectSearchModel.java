package com.viaoa.metadata;

import com.viaoa.filter.OAAndFilter;
import com.viaoa.filter.OAEqualPathFilter;
import com.viaoa.filter.OAFilter;
import com.viaoa.filter.OAInFilter;
import com.viaoa.filter.OAQueryFilter;
import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.merge.HubMerger;
import com.viaoa.hub.view.HubShareOnAOChange;
import com.viaoa.lang.OAStr;
import com.viaoa.oa.OA;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.select.OASelect;

public class OAObjectSearchModel<T extends OAObject> {

    protected final OAObjectModel<T> model;
    protected final Hub<T> hubModel;
    protected Class<T> type;

    protected Hub<T> hub; // main hub

    protected HubShareOnAOChange<T> hubShareOnAOChange;

    protected Hub<T> hubSearchResults;
    protected Hub<T> hubSearchFrom; // hub (optional) to search from
    protected Hub<T> hubMultiSelect; // optional if SearchFrom and wiring is Many

    protected OAFilter<T> filterExtraWhere;

    // finder used to find objects in a path
    protected OAFinder<?, T> finder;

    // from hubDetail perspective
    protected enum ConfigType {
        Standalone, 
        LinkOnly, 
        DetailOne, 
        DetailMany, 
        DetailManyLink, 
        O2M2O
    }
    
    private ConfigType configType;
    private boolean bConfigCalled;
    

    protected OAObjectSearchModel(final OAObjectModel<T> model) {
        this.model = model;
        hubModel = model.getContextHub();
        type = hubModel.getObjectClass();
        config();
    }
    
    public Hub<T> getHub() {
        if (hub == null) {
            hub = new Hub<T>(type);
        }
        return hub;
    }

    protected void connectToHub(Hub<T> hub) {
        if (hubShareOnAOChange == null) {
            hubShareOnAOChange = new HubShareOnAOChange<T>(getHub());
        }
        hubShareOnAOChange.add(hub);
    }

    public Hub<T> getSearchResults() {
        if (hubSearchResults == null) {
            hubSearchResults = new Hub<T>(type);
            connectToHub(hubSearchResults);
        }
        return hubSearchResults;
    }

    public Hub<T> getMultiSelectHub() {
        return hubMultiSelect;
    }

    public Hub<T> getSearchFromHub() {
        return hubSearchFrom;
    }

    private HubListener<T> hlSearchFrom;
    protected void setSearchFromHub(Hub<T> hub) {
        if (hub == hubSearchFrom) return;
        if (hubSearchFrom != null) {
            if (hubShareOnAOChange != null) hubShareOnAOChange.remove(hubSearchFrom);
            if (hlSearchFrom != null) {
                hubSearchFrom.removeHubListener(hlSearchFrom);
                hlSearchFrom = null;
            }
        }

        hubSearchFrom = hub;
        if (hubSearchFrom != null) {
            connectToHub(hubSearchFrom);
            hlSearchFrom = hubSearchFrom.onNewList(onEvent -> OAObjectSearchModel.this.getSearchResults().clear());
        }
    }

    public OAFinder<?, T> getFinder() {
        return finder;
    }

    public void setFinder(OAFinder<?, T> finder) {
        this.finder = finder;
    }

    public void beforeInput() {
        // hook that is called before search input starts
    }

    public void performSearch() {
    }

    public void reset() {
        cancelSelect();
        if (hubSearchResults != null) hubSearchResults.clear();
        if (hub != null) hub.setAO(null);
        if (hubMultiSelect != null) hubMultiSelect.clear();
    }

    public void cancelSelect() {
        getSearchResults().cancelSelect();
    }

    protected void config() {
        if (bConfigCalled) return;
        bConfigCalled = true;

        final OA oa = OARuntime.oa(type);
        Hub<?> hubMaster = hubModel.getMasterHub(); 
        OALinkInfo liMasterToDetail = null;
        
        if (hubMaster == null && model != null) {
            // might be O2M2O, which does not have a hubMaster (hubModel created using HubMerger from hubMaster)
            // example:  Order 1->M OrderContact M->1 Contact
            hubMaster = model.getFromHub();
            if (hubMaster != null) {
                String sx = model.getFromLinkProperty();
                liMasterToDetail = oa.info(hubMaster).getLinkInfo(sx);
            }
        }
        else {
            liMasterToDetail = oa.internal().hubs().detail().getLinkInfoFromMasterObjectToDetail(hubModel);
        }
        
        if (liMasterToDetail != null && liMasterToDetail.getPrivateMethod()) {
            liMasterToDetail = null;
        }

        final Hub<?> hubLinkedTo = hubModel.getLinkHub(true);
        final OALinkInfo liLinkTo = hubLinkedTo == null ? null : hubLinkedTo.getOAObjectInfo().getLinkInfo(hubModel.getLinkPath(true));
        
        configType = ConfigType.Standalone;
        if (liMasterToDetail != null) {
            if (liMasterToDetail.getType() == OALinkInfo.TYPE_ONE) {
                if (hubMaster != null && hubMaster.isLinkObjectFor(liMasterToDetail.getName())) {
                    configType = ConfigType.O2M2O;
                }
                else configType = ConfigType.DetailOne;
            }
            else if (liMasterToDetail.getType() == OALinkInfo.TYPE_MANY) {
                configType = liLinkTo != null ? ConfigType.DetailManyLink : ConfigType.DetailMany;
            }
        }
        else if (liLinkTo != null) {
            configType = ConfigType.LinkOnly;
        }

        // PART 1: Filters
        // inFilter
        if (liLinkTo != null && liMasterToDetail != null && liMasterToDetail.getType() == OALinkInfo.TYPE_MANY) {
            if (hubSearchFrom == null) setSearchFromHub(hubModel.createSharedHub(false));
        }
        
        // adopt where
        if (liLinkTo != null) {
            oa.internal().hubs().select().adoptWhereHub(getSearchResults(), liLinkTo.getName(), hubLinkedTo);
        }
        else if (liMasterToDetail != null) {
            oa.internal().hubs().select().adoptWhereHub(getSearchResults(), liMasterToDetail.getName(), hubMaster);
        }

        // SelectFromPath
        OAFilter<T> filter = model.getExtraFilter();
        if (filter != null) {
            OAFilter<T> f = getExtraWhereFilter();
            if (f != null) filter = new OAAndFilter(f, filter);
            setExtraWhereFilter(filter);
        }
        
        if (model.getChoicesHub() != null) {  // only exists if model.hubContext created it for list of choices
            filter = new OAInFilter(model.getChoicesHub()); //qqqq might be able to the OASelect.hubSelect
            OAFilter<T> f = getExtraWhereFilter();
            if (f != null) filter = new OAAndFilter(f, filter);
            setExtraWhereFilter(filter);
            setSearchFromHub(model.getChoicesHub().createShared());
        }
        
        String sx = model.getExtraWhere(); //qqqqqqqqqqqq WIP:  where to put this?? put into AND + OAQueryFilter LOVE IT
        if (OAStr.isNotEmpty(sx)) {
            filter = new OAQueryFilter(type, sx);
            OAFilter<T> f = getExtraWhereFilter();
            if (f != null) filter = new OAAndFilter(f, filter);
            setExtraWhereFilter(filter);
        }
        
        Hub<?> hx = model.getSelectWhereHub();
        if (hx != null) {
            sx = model.getSelectWhereHubPath();
            if (OAStr.isNotEmpty(sx)) {
                getSearchResults().setSelectWhereHub(hx, sx);
                
                // clear if hubFrom.ao changes
                hx.onChangeAO( event -> hubSearchResults.clear());
            }
        }
        
        // PART 2: set HubSelectLink
        // search results
        switch (configType) {
        case Standalone:
            getSearchResults().setSelectLink(null, hubModel, null, true, true, false); // set hubModel.ao
            break;
        case LinkOnly:
            getSearchResults().setSelectLink(null, hubModel, null, true, true, false); // set hubModel.ao
            break;
        case DetailOne:
            getSearchResults().setSelectLink(null, hubMaster, liMasterToDetail.getName(), true, true, false);
            break;
        case DetailMany:
            getSearchResults().setSelectLink(hubModel);
            break;
        case DetailManyLink:
            getSearchResults().setSelectLink(null, hubModel, null, true, true, false);
            break;
        case O2M2O:
            getSearchResults().setSelectLink(null, hubMaster, liMasterToDetail.getName(), true, false, false);
            break;
        }
        
        // hubSearchFrom
        if (hubSearchFrom != null) {
            switch (configType) {
            case Standalone:
                getSearchFromHub().setSelectLink(null, hubModel, null, true, true, false); // set hubModel.ao
                break;
            case LinkOnly:
                getSearchFromHub().setSelectLink(null, hubModel, null, true, true, false); // set hubModel.ao
                break;
            case DetailOne:
                getSearchFromHub().setSelectLink(null, hubMaster, liMasterToDetail.getName(), true, true, false);
                break;
            case DetailMany:
                hubMultiSelect = new Hub<T>(type);
                getSearchFromHub().setSelectLink(getMultiSelectHub(), hubModel);
                break;
            case DetailManyLink:
                getSearchFromHub().setSelectLink(null, hubModel, null, true, true, false);
                break;
            case O2M2O:
                hubMultiSelect = new Hub<T>(type);
                getSearchFromHub().setSelectLink(getMultiSelectHub(), hubMaster, liMasterToDetail.getName());
                break;
            }
        }
        
        // todo: Hub tree hubNodeSelected qqqqqqqqqqqqqqqqqqqqqqqqqqqqq 
        // todo: Calendar support qqqqqqqqqqqqq
    }

    public void setExtraWhereFilter(OAFilter<T> filter) {
        this.filterExtraWhere = filter;
    }

    public OAFilter<T> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public void updateSelect(final OASelect<T> sel) {
        sel.setSearchHub(getSearchFromHub());
        OAFilter<T> filterExtra = getExtraWhereFilter();
        if (filterExtra != null) {
            if (filterExtra.updateSelect(sel)) {
                OAFilter<T> filter = sel.getFilter();
                if (filter != null) filter = new OAAndFilter(filter, filterExtra);
                else filter = filterExtra;
                sel.setFilter(filter);
            }
        }
        OAFinder f = getFinder();
        if (f != null) sel.setFinder(getFinder());
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
     * 
     * Need close()
     * 
     * The class creates: - HubSelectLink target listeners; - HubShareOnAOChange
     * source listeners; - SearchFrom new-list callbacks; - HubMergers. the
     * HubMergers are not retained. HubMerger itself supports close()
     * 
     * 
     */

}
