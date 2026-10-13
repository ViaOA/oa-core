package com.viaoa.metadata;

import com.viaoa.filter.OAAndFilter;
import com.viaoa.filter.OAEqualPathFilter;
import com.viaoa.filter.OAFilter;
import com.viaoa.filter.OAInFilter;
import com.viaoa.filter.OAQueryFilter;
import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.hub.merge.HubMerger;
import com.viaoa.hub.view.HubShareOnAOChange;
import com.viaoa.lang.OAStr;
import com.viaoa.oa.OA;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.select.OASelect;

public class OAObjectSearchModel<T extends OAObject> {

    protected final Class<T> type;

    protected final Hub<?> hubMaster;
    protected final OALinkInfo liMasterToDetail;

    protected final Hub<?> hubLinkedTo;
    protected final OALinkInfo liLinkTo;

    protected Hub<T> hub; // main hub
    protected Hub<T> hubSearchResults;

    protected HubShareOnAOChange<T> hubShareOnAOChange;
    protected Hub<T> hubMultiSelect;
    protected Hub<T> hubSearchFrom; // hub (optional) to search from

    protected OAFilter<T> filterExtraWhere;
    
    // finder used to find objects in a path
    protected OAFinder<?, T> finder;

    public OAObjectSearchModel(final Hub<T> hub) {
        this.type = hub.getObjectClass();

        final OA oa = OARuntime.oa(type);
        hubMaster = hub.getMasterHub();
        liMasterToDetail = oa.internal().hubs().detail().getLinkInfoFromMasterObjectToDetail(hub);

        hubLinkedTo = hub.getLinkHub(true);
        liLinkTo = hubLinkedTo == null ? null : hubLinkedTo.getOAObjectInfo().getLinkInfo(hub.getLinkPath(true));

        config(hub, oa);
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

    // hub used for search results
    public Hub<T> getSearchResults() {
        if (hubSearchResults == null) {
            hubSearchResults = new Hub<T>(type);
        }
        return hubSearchResults;
    }

    public Hub<T> getMultiSelectHub() {
        if (hubMultiSelect == null) {
            hubMultiSelect = new Hub<T>(type);
        }
        return hubMultiSelect;
    }

    public Hub<T> getSearchFromHub() {
        return hubSearchFrom;
    }

    public void setSearchFromHub(Hub<T> hub) {
        hubSearchFrom = hub;
        if (hubSearchFrom != null) {
            connectToHub(hubSearchFrom);
            hubSearchFrom.onNewList(onEvent -> OAObjectSearchModel.this.getSearchResults().clear());
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

    protected void config(final Hub<T> hub, final OA oa) {
        if (liMasterToDetail == null) {
            if (liLinkTo == null) {
                return;
            }
        }

        if (liLinkTo != null || liMasterToDetail == null) {
            if (liLinkTo == null) return;

            if (liLinkTo.getType() == liLinkTo.TYPE_ONE) getHub().setSelectLink(hubLinkedTo, liLinkTo.getName());
            else getHub().setSelectLink(getMultiSelectHub(), hub);

            oa.internal().hubs().select().adoptWhereHub(getSearchResults(), liLinkTo.getName(), hubLinkedTo);
        }
        else {
            if (liMasterToDetail.getPrivateMethod()) {
                return;
            }

            oa.internal().hubs().select().adoptWhereHub(getSearchResults(), liMasterToDetail.getName(), hubMaster);

            if (hubMaster.isLinkObjectFor(liMasterToDetail.getName())) {
                getHub().setSelectLink(getMultiSelectHub(), hubMaster, liMasterToDetail.getName());
            }
            else {
                if (liMasterToDetail.getType() == OALinkInfo.TYPE_MANY) {
                    getHub().setSelectLink(getMultiSelectHub(), hub);
                }
                else {
                    getHub().setSelectLink(hubMaster, liMasterToDetail.getName());
                }
            }
        }

        // SelectFromPath
        if (liLinkTo != null) {
            String pp = liLinkTo.getSelectFromPath();
            if (OAStr.isNotEmpty(pp)) {
                setSearchFromHub(new Hub<T>(type));
                new HubMerger(hubLinkedTo, getSearchFromHub(), pp, false);

                OAFilter<T> filter = getExtraWhereFilter();
                if (filter != null) filter = new OAAndFilter(filter, new OAInFilter(hubLinkedTo, pp));
                else filter = new OAInFilter(hubLinkedTo, pp);
                setExtraWhereFilter(filter);
            }
        }
        else if (liMasterToDetail != null) {
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

        // EqualPath
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
                }
            }
        }
        else if (liMasterToDetail != null) {
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

        // Filter name
        String filterName = null;
        if (liLinkTo != null) filterName = liLinkTo.getFilterName();
        else if (liMasterToDetail != null) filterName = liMasterToDetail.getFilterName();

        if (OAStr.isNotEmpty(filterName)) {
            OAFilter<T> filter = getExtraWhereFilter();
            OAFilter filterEqual = new OAQueryFilter(type, ":" + filterName);
            if (filter != null) filter = new OAAndFilter(filter, filterEqual);
            else filter = filterEqual;
            setExtraWhereFilter(filter);
        }
    }
    
  //qqqqqqqqqqqq  make sure that *OAFilters are able to supply SQL for ExtraWhereFilter          
  //     modelSearch.getContactSearch().setExtraWhere("salesPersonList = true");            

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
    
/* qqqqqqqqqqqqqqqq WIP ... more to do     
    
recursive Hubs, root Hubs for trees 
  ex: ItemModel.getItemCategoryHub, getRootItemCategoryHub
      > ItemCategoryModel needs rootHub, and regular Hub (recursive)      
    
Calendar related ... example??    
    

Need close()
       
    The class creates:
      - HubSelectLink target listeners;
      - HubShareOnAOChange source listeners;
      - SearchFrom new-list callbacks;
      - HubMergers.
           the HubMergers are not retained.  HubMerger itself supports close()
       
    
*/    
    
    
    
    
    
}
