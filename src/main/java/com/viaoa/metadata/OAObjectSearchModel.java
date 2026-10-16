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
        Standalone, LinkOnly, DetailOne, DetailMany, DetailManyLink, O2M2O
    }
    
    private ConfigType configType;
    private boolean bConfigCalled;
    
    
    public OAObjectSearchModel(final Hub<T> hubModel) {
        this.hubModel = hubModel;
        this.type = hubModel.getObjectClass();
    }

    public Hub<T> getHub() {
        if (hub == null) {
            hub = new Hub<T>(type);
            hubShareOnAOChange = new HubShareOnAOChange<T>(hub);
            connectToHub(getSearchResults());
        }
        return hub;
    }

    protected void connectToHub(Hub<T> hub) {
        getHub();
        hubShareOnAOChange.add(hub);
    }

    public Hub<T> getSearchResults() {
        if (hubSearchResults == null) {
            hubSearchResults = new Hub<T>(type);
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
    public void setSearchFromHub(Hub<T> hub) {
//qqqqqqqqqqq setting after config is not allowed qqqqqqqqqqqqqq        
        if (hub == hubSearchFrom) return;
        if (hubSearchFrom != null) {
            hubShareOnAOChange.remove(hubSearchFrom);
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
        final Hub<?> hubMaster = hubModel.getMasterHub();
        OALinkInfo liMasterToDetail = oa.internal().hubs().detail().getLinkInfoFromMasterObjectToDetail(hubModel);
        
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
        if (liLinkTo != null && liMasterToDetail != null) {
            OAFilter<T> filter = getExtraWhereFilter();
            if (filter != null) filter = new OAAndFilter(filter, new OAInFilter(hubModel));
            else filter = new OAInFilter(hubModel);
            setExtraWhereFilter(filter);

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
        boolean bFound = false;
        if (liLinkTo != null) {
            String pp = liLinkTo.getSelectFromPath();
            if (OAStr.isNotEmpty(pp)) {
                setSearchFromHub(new Hub<T>(type));
                new HubMerger(hubLinkedTo, getSearchFromHub(), pp, false);

                OAFilter<T> filter = getExtraWhereFilter();
                if (filter != null) filter = new OAAndFilter(filter, new OAInFilter(hubLinkedTo, pp));
                else filter = new OAInFilter(hubLinkedTo, pp);
                setExtraWhereFilter(filter);
                bFound = true;
            }
        }
        if (!bFound && liMasterToDetail != null) {
            String pp = liMasterToDetail.getSelectFromPath();

            if (OAStr.isNotEmpty(pp)) {
                setSearchFromHub(new Hub<T>(type));
                new HubMerger(hubMaster, getSearchFromHub(), pp, false);

                OAFilter<T> filter = getExtraWhereFilter();
                if (filter != null) filter = new OAAndFilter(filter, new OAInFilter(hubMaster, pp));
                else filter = new OAInFilter(hubMaster, pp);
                setExtraWhereFilter(filter);
            }
        }
        
        // EqualPath -------------------------------
        bFound = false;
        if (liLinkTo != null) {
            OALinkInfo liRev = liLinkTo.getReverseLinkInfo();
            if (liRev != null) {
                String ppFrom = liLinkTo.getEqualPath();
                String ppTo = liRev.getEqualPath();
                if (OAStr.isNotEmpty(ppFrom) || OAStr.isNotEmpty(ppTo)) {
                    OAFilter<T> filter = getExtraWhereFilter();
                    OAFilter filterEqual = new OAEqualPathFilter(hubLinkedTo, ppFrom, ppTo);
                    if (filter != null) filter = new OAAndFilter(filter, filterEqual);
                    else filter = filterEqual;
                    setExtraWhereFilter(filter);
                    bFound = true;
                }
            }
        }
        if (!bFound && liMasterToDetail != null) {
            OALinkInfo liRev = liMasterToDetail.getReverseLinkInfo();
            if (liRev != null) {
                String ppfrom = liMasterToDetail.getEqualPath();
                String ppTo = liRev.getEqualPath();
                if (OAStr.isNotEmpty(ppfrom) || OAStr.isNotEmpty(ppTo)) {
                    OAFilter<T> filter = getExtraWhereFilter();
                    OAFilter filterEqual = new OAEqualPathFilter(hubMaster, ppfrom, ppTo);
                    if (filter != null) filter = new OAAndFilter(filter, filterEqual);
                    else filter = filterEqual;
                    setExtraWhereFilter(filter);
                }
            }
        }
        
        // Filter name -------------------------------
        String filterName = null;
        if (liLinkTo != null) filterName = liLinkTo.getFilterName();
        if (OAStr.isEmpty(filterName) && liMasterToDetail != null) filterName = liMasterToDetail.getFilterName();

        if (OAStr.isNotEmpty(filterName)) {
            OAFilter<T> filter = getExtraWhereFilter();
            OAFilter filterEqual = new OAQueryFilter(type, ":" + filterName);
            if (filter != null) filter = new OAAndFilter(filter, filterEqual);
            else filter = filterEqual;
            setExtraWhereFilter(filter);
        }
        
        
        
        // PART 2: set HubSelectLink
        // search results
//qqqqqqqqqq todo: make this optional (ex: when using an dropdown Add table listing        
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

    // qqqqqqqqqqqq make sure that *OAFilters are able to supply SQL for
    // ExtraWhereFilter
    // modelSearch.getContactSearch().setExtraWhere("salesPersonList = true");

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
