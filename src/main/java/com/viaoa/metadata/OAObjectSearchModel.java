package com.viaoa.metadata;

import com.viaoa.filter.OAAndFilter;
import com.viaoa.filter.OAEqualPathFilter;
import com.viaoa.filter.OAFilter;
import com.viaoa.filter.OAInFilter;
import com.viaoa.filter.OAQueryFilter;
import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.link.HubSelectLink;
import com.viaoa.hub.merge.HubMerger;
import com.viaoa.hub.view.HubShareOnAOChange;
import com.viaoa.lang.OAStr;
import com.viaoa.oa.OA;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.select.OASelect;

public class OAObjectSearchModel<T extends OAObject> {

    protected final OAObjectModel<T> model;
    protected Class<T> ctype;

    protected Hub<T> hub; // shares active hub
    protected Hub<T> hubSearchResults;
    protected Hub<T> hubMultiSelect; // optional if SearchFrom and wiring is Many

    protected Hub<T> hubSearchFrom; // hub (optional) to search from
    protected OAFilter<T> filterExtraWhere;

    // finder used to find objects in a path
    protected OAFinder<?, T> finder;

    
    // from hubDetail perspective
    protected enum ConfigType {
        Standalone, 
        LinkOnly, 
        DetailOne, 
        DetailManyNoLinkTo, 
        DetailManyWithLinkTo, 
        O2M2O
    }
    
    private ConfigType configType;
    private boolean bConfigCalled;
    

    protected OAObjectSearchModel(final OAObjectModel<T> model) {
        this.model = model;
        Hub<T> hubModel = model.getContextHub();
        ctype = hubModel.getObjectClass();
        config();
    }
    
    public Hub<T> getSearchResults() {
        if (hubSearchResults == null) {
            hubSearchResults = new Hub<T>(ctype);
        }
        return hubSearchResults;
    }

    public Hub<T> getMultiSelectHub() {
        if (hubMultiSelect == null) hubMultiSelect = new Hub<T>(ctype);
        return hubMultiSelect;
    }

    public Hub<T> getSearchFromHub() {
        return hubSearchFrom;
    }

    private HubListener<T> hlSearchFrom;
    public void setSearchFromHub(Hub<T> hub) {
        if (hub == hubSearchFrom) return;
        if (hubSearchFrom != null) {
            if (hlSearchFrom != null) {
                hubSearchFrom.removeHubListener(hlSearchFrom);
                hlSearchFrom = null;
            }
            hubSearchFrom.setSelectLink(null);
        }
        clearResults();

        hubSearchFrom = hub;
        if (hubSearchFrom != null) {
            hlSearchFrom = hubSearchFrom.onNewList(onEvent -> OAObjectSearchModel.this.getSearchResults().clear());
        }
        
        if (hubSearchFrom != null) {
            final OA oa = OARuntime.oa(ctype);
            final Hub<T> hubModel = model.getContextHub();
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

            switch (configType) {
            case Standalone:
                hubSearchFrom.setSelectLink(null, hubModel, null, true, true, false); // set hubModel.ao
                break;
            case LinkOnly:
                hubSearchFrom.setSelectLink(null, hubModel, null, true, true, false); // set hubModel.ao
                break;
            case DetailOne:
                hubSearchFrom.setSelectLink(null, hubMaster, liMasterToDetail.getName(), true, true, false);
                break;
            case DetailManyNoLinkTo:
                //qqq was: hubMultiSelect = new Hub<T>(type);
                hubSearchFrom.setSelectLink(getMultiSelectHub(), hubModel);
                break;
            case DetailManyWithLinkTo:
                hubSearchFrom.setSelectLink(null, hubModel, null, true, true, false);
                break;
            case O2M2O:
                //qqqq was: hubMultiSelect = new Hub<T>(type);
                hubSearchFrom.setSelectLink(getMultiSelectHub(), hubMaster, liMasterToDetail.getName());
                break;
            }
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
        //qqqqqqqqqq this needs to be abstract qqqqqqqqqq
        OASelect<T> sel = getSearchResults().getSelect(true);
        updateSelect(sel);;
        getSearchResults().select();
    }

    public void clearResults() {
        cancelSelect();
        if (hubSearchResults != null) hubSearchResults.clear();
        //was: if (hubMultiSelect != null) hubMultiSelect.clear();
    }

    //qqqqqq make abstract'ish
    /**
     * Clear the search values.
     */
    public void clearSearchValues() {
    }
    
    
    public void cancelSelect() {
        getSearchResults().cancelSelect();
    }
    


    
    protected boolean configA(final OA oa) {
        final Hub<T> hubChoices = model.getChoicesHub();
        if (hubChoices == null) return false;
        if (hubChoices.getLinkHub(true) == null) return false; 
        
        final Hub<T> hubModel = model.getContextHub();
        Hub<?> hubMaster = hubModel.getMasterHub();
        if (hubMaster == null) return false;

        final OA oa = OARuntime.oa(ctype);
        OALinkInfo liMasterToDetail = oa.internal().hubs().detail().getLinkInfoFromMasterObjectToDetail(hubModel);
        
        if (liMasterToDetail == null || liMasterToDetail.getType() != OALinkInfo.TYPE_ONE) return false;

        //qqqqqqqq wip: change to setSearchFromHub(..)
        hubSearchFrom = hubChoices;
        
        OAFilter<T> filter = new OAInFilter(model.getChoicesHub());
        OAFilter<T> f = getExtraWhereFilter();
        if (f != null) filter = new OAAndFilter(f, filter);
        setExtraWhereFilter(filter);

        getSearchResults().setSelectLink(null, hubMaster, liMasterToDetail.getName(), true, true, false);
        
        return true;
    }
    
    
    
    

    protected void config() {
        if (bConfigCalled) return;
        bConfigCalled = true;

        final Hub<T> hubModel = model.getContextHub();
        final OA oa = OARuntime.oa(ctype);
        
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
        
        
        
        
        // PART 1: ConfigType
        configType = ConfigType.Standalone;
        if (liMasterToDetail != null) {
            if (liMasterToDetail.getType() == OALinkInfo.TYPE_ONE) {
                if (hubMaster != null && hubMaster.isLinkObjectFor(liMasterToDetail.getName())) {
                    configType = ConfigType.O2M2O;
                }
                else configType = ConfigType.DetailOne;
            }
            else {
                configType = liLinkTo != null ? ConfigType.DetailManyWithLinkTo : ConfigType.DetailManyNoLinkTo;
            }
        }
        else if (liLinkTo != null) {
            configType = ConfigType.LinkOnly;
        }

        
// add 
// pick
// nav        
   
        
        
        
        
        // PART 2: Filters
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
            filter = new OAQueryFilter(ctype, sx);
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
        
        // PART 3: set HubSelectLink
        // search results
        switch (configType) {
        case Standalone:
            getSearchResults().setSelectLink(null, hubModel, null, true, true, false); // set hubModel.ao
            break;
        case LinkOnly:
            getSearchResults().setSelectLink(null, hubModel, null, true, true, false); // set hubModel.ao
            break;
        case DetailOne:
            getSearchResults().setSelectLink(null, hubMaster, liMasterToDetail.getName(), true, true, false); // hubMaster.ao.ref
            break;
        case DetailManyNoLinkTo:
            getSearchResults().setSelectLink(hubModel); // add/remove with hub
            break;
        case DetailManyWithLinkTo:
            getSearchResults().setSelectLink(null, hubModel, null, true, true, false);
            break;
        case O2M2O:
            getSearchResults().setSelectLink(null, hubMaster, liMasterToDetail.getName(), true, false, false);
            break;
        }
        
//qqqqqqqqq setSelectLink for hubMultiSelect ??        
        
//qqqqqvvvvv could it be call setSelectLink on both? qqqq        
        // hubSearchFrom
        
        // todo: Hub tree hubNodeSelected qqqqqqqqqqqqqqqqqqqqqqqqqqqqq 
        // todo: Calendar support qqqqqqqqqqqqq
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

    public void updateUsingSearchFromHub() {
        Hub<T> hub = getSearchFromHub();
        if (hub != null) {
            HubSelectLink<T, ? extends OAObject> hsl = hub.getSelectLink();
            if (hsl != null) hsl.select();
        }        
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
