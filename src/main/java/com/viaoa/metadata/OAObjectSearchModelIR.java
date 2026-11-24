package com.viaoa.metadata;

import com.viaoa.filter.OAAndFilter;
import com.viaoa.filter.OAFilter;
import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.lang.OAStr;
import com.viaoa.object.OAObject;
import com.viaoa.select.OASelect;

public class OAObjectSearchModelIR<T extends OAObject> {
    protected final OAObjectSearchModel<T> search;
    protected final OAObjectModel<T> model;

    protected OAObjectSearchModelIR(final OAObjectSearchModel<T> search) {
        this.search = search;
        this.model = search.model;
        config();
    }

    protected void config() {

        switch (search.type) {
        case DefaultSearch:
            switch (model.ir.type) {
            case None:
                configTypeNone(); 
                break;
            case One:
                configTypeOne();
                break;
            case OMO:
                configTypeOMO();
                break;
            case Many:
                configTypeMany();
                break;
            case ManyPrivate:
                configTypeManyPrivate();
                break;
            }
            // adopt where
            if (model.ir.hubAdoptWhere != null) {
                model.ir.oa.internal().hubs().select().adoptWhereHub(search.getSearchResults(), model.ir.adoptWhereLinkName, model.ir.hubAdoptWhere);            
            }
            break;
        case InnerHubContextSearch:
            configInnerHubContextSearch();
            break;
        case HubContextSharedWithSearchResults:
            configContextHubSharedWithSearchResults();
            break;
        }        
        
        
    }

    // model.hubContext.setShared(this.hubSearchResults)
    protected void configInnerHubContextSearch() {
        model.getContextHub().setSharedHub(search.hubSearchResults, false);
        if (search.model.getAllowMultiSelect()) {
            // no HubSelectLink needed, model.hubMultiSelect is shared
        }
        else {
            // set AO
            search.hubSearchResults.setSelectLink(null, model.ir.hubContext, null, true, true, false);
        }
    }
    
    protected void configContextHubSharedWithSearchResults() {
        model.getContextHub().setSharedHub(search.hubSearchResults, true);
    }
    
    // DefaultSearch
    
    protected void configTypeNone() {
        search.getSearchResults().setSelectLink(null, model.ir.hubContext, null, true, true, false); // set hubModel.ao
        search.getModel().getMultiSelectHub().setSelectLink(null, model.ir.hubContext, null, false, false, false);
        // search.getSearchResultsMultiSelectHub().setSelectLink(null, model.ir.hubContext, null, false, false, false);
        // search.getChoicesMultiSelectHub().setSelectLink(null, model.ir.hubContext, null, false, false, false);
        
        if (model.ir.bUseForSearch) {
            search.getSearchResults().setSharedHub(model.getContextHub(), true);
        }
    }
    protected void configTypeOne() {
        Hub<T> hx = model.getChoicesHub();
        if (hx != null && hx.getLinkHub(true) == null) hx = null;
        
        if (hx != null) search.getSearchResults().setSelectLink(null, model.ir.hubContext, null, true, true, false); // set hubModel.ao
        else search.getSearchResults().setSelectLink(null, model.ir.hubFrom, model.ir.fromLinkProperty, true, true, false); // hubMaster.ao.ref
    }
    protected void configTypeOMO() {
        search.getModel().getMultiSelectHub().setSelectLink(null, model.ir.hubFrom, model.ir.fromLinkProperty, false, false, false);
        // search.getSearchResultsMultiSelectHub().setSelectLink(null, model.ir.hubFrom, model.ir.fromLinkProperty, false, false, false);
        // search.getChoicesMultiSelectHub().setSelectLink(null, model.ir.hubFrom, model.ir.fromLinkProperty, false, false, false);
    }
    protected void configTypeMany() {
        search.getModel().getMultiSelectHub().setSelectLink(null, model.ir.hubContext, null, false, false, false);
        // search.getSearchResultsMultiSelectHub().setSelectLink(null, model.ir.hubContext, null, false, false, false);
        // search.getChoicesMultiSelectHub().setSelectLink(null, model.ir.hubContext, null, false, false, false);
    }
    protected void configTypeManyPrivate() {
        search.getModel().getMultiSelectHub().setSelectLink(null, model.ir.hubContext, null, false, false, false);
        // search.getSearchResultsMultiSelectHub().setSelectLink(null, model.ir.hubContext, null, false, false, false);
        // search.getChoicesMultiSelectHub().setSelectLink(null, model.ir.hubContext, null, false, false, false);
    }

    
    protected void prepareForSelect(final OASelect<T> sel) {
        switch (model.ir.type) {
        case None:
            prepareSelectTypeNone(sel); 
            break;
        case One:
            prepareSelectTypeOne(sel);
            break;
        case OMO:
            prepareSelectTypeOMO(sel);
            break;
        case Many:
            prepareSelectTypeMany(sel);
            break;
        case ManyPrivate:
            prepareSelectTypeManyPrivate(sel);
            break;
        }

        Hub<T> hx = null;
        if (search.type == OAObjectSearchModel.Type.InnerHubContextSearch) {
            hx = model.getContextHub();
        }
        else {
            hx = model.getChoicesHub();
        }
        if (hx != null) sel.setSearchHub(hx); // select only from this Hub

        OAFilter<T> filterExtra = model.ir.extraFilter;
        if (filterExtra != null) {
            if (filterExtra.updateSelect(sel)) {
                OAFilter<T> filter = sel.getFilter();
                if (filter != null) filter = new OAAndFilter(filter, filterExtra);
                else filter = filterExtra;
                sel.setFilter(filter);
            }
        }
        
        OAFinder f = model.getFinder();
        if (f != null) sel.setFinder(f);
        
        
        if (model.ir.hubSelectWhere != null) {
            sel.setWhereHub(model.ir.hubSelectWhere, model.ir.selectWhereHubPath);
        }
        
        String s = model.ir.extraWhere;
        if (OAStr.isNotEmpty(s)) {
            String s2 = sel.getWhere();
            if (OAStr.isEmpty(s2)) s2 = s;
            else s2 += " AND (" + s + ")";
            sel.setWhere(s2);;
        }
    }
    protected void prepareSelectTypeNone(final OASelect<T> sel) {
    }
    protected void prepareSelectTypeOne(final OASelect<T> sel) {
    }
    protected void prepareSelectTypeOMO(final OASelect<T> sel) {
    }
    protected void prepareSelectTypeMany(final OASelect<T> sel) {
    }
    protected void prepareSelectTypeManyPrivate(final OASelect<T> sel) {
    }
}



