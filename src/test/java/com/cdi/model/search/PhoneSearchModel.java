// Generated by OABuilder
package com.cdi.model.search;

import java.util.logging.*;

import com.viaoa.object.*;
import com.viaoa.datasource.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.util.filter.*;
import com.cdi.model.*;
import com.cdi.model.oa.*;
import com.cdi.model.oa.propertypath.*;
import com.cdi.model.oa.search.*;
import com.cdi.model.oa.filter.*;
import com.cdi.delegate.ModelDelegate;
import com.cdi.resource.Resource;

public class PhoneSearchModel {
    private static Logger LOG = Logger.getLogger(PhoneSearchModel.class.getName());
    
    protected Hub<Phone> hub;  // search results
    protected Hub<Phone> hubMultiSelect;
    protected Hub<Phone> hubSearchFrom;  // hub (optional) to search from
    protected Hub<PhoneSearch> hubPhoneSearch;  // search data, size=1, AO
    // references used in search
    protected Hub<Contact> hubPhoneContact;
    
    // finder used to find objects in a path
    protected OAFinder<?, Phone> finder;
    
    // ObjectModels
    protected ContactModel modelPhoneContact;
    
    // SearchModels
    protected ContactSearchModel modelPhoneContactSearch;
    
    // object used for search data
    protected PhoneSearch searchPhone;
    
    public PhoneSearchModel() {
    }
    
    public PhoneSearchModel(Hub<Phone> hub) {
        this.hub = hub;
    }
    
    // hub used for search results
    public Hub<Phone> getHub() {
        if (hub == null) {
            hub = new Hub<Phone>(Phone.class);
        }
        return hub;
    }
    
    // hub used to search within
    private HubListener hlSearchFromHub;
    public Hub<Phone> getSearchFromHub() {
        return hubSearchFrom;
    }
    public void setSearchFromHub(Hub<Phone> hub) {
        if (this.hlSearchFromHub != null) {
            hubSearchFrom.removeListener(hlSearchFromHub);
            hlSearchFromHub = null;
        }
    
        hubSearchFrom = hub;
        if (hubSearchFrom != null) {
            hlSearchFromHub = new HubListenerAdapter() {
                @Override
                public void onNewList(HubEvent e) {
                    PhoneSearchModel.this.getHub().clear();
                }
            };
            hubSearchFrom.addHubListener(hlSearchFromHub);
        }
    }
    public void close() {
        setSearchFromHub(null);
    }
    
    public Hub<Phone> getMultiSelectHub() {
        if (hubMultiSelect == null) {
            hubMultiSelect = new Hub<>(Phone.class);
        }
        return hubMultiSelect;
    }
    
    public OAFinder<?, Phone> getFinder() {
        return finder;
    }
    public void setFinder(OAFinder<?, Phone> finder) {
        this.finder = finder;
    }
    
    // object used to input query data, to be used by searchHub
    public PhoneSearch getPhoneSearch() {
        if (searchPhone != null) return searchPhone;
        searchPhone = new PhoneSearch();
        return searchPhone;
    }
    
    // hub for search object - used to bind with UI components for entering search data
    public Hub<PhoneSearch> getPhoneSearchHub() {
        if (hubPhoneSearch == null) {
            hubPhoneSearch = new Hub<PhoneSearch>(PhoneSearch.class);
            hubPhoneSearch.add(getPhoneSearch());
            hubPhoneSearch.setPos(0);
        }
        return hubPhoneSearch;
    }
    public Hub<Contact> getPhoneContactHub() {
        if (hubPhoneContact != null) return hubPhoneContact;
        hubPhoneContact = getPhoneSearchHub().getDetailHub(PhoneSearch.P_PhoneContact);
        return hubPhoneContact;
    }
    
    public ContactModel getPhoneContactModel() {
        if (modelPhoneContact != null) return modelPhoneContact;
        modelPhoneContact = new ContactModel(getPhoneContactHub());
        modelPhoneContact.setDisplayName("Contact");
        modelPhoneContact.setPluralDisplayName("Contacts");
        modelPhoneContact.setAllowNew(false);
        modelPhoneContact.setAllowSave(true);
        modelPhoneContact.setAllowAdd(false);
        modelPhoneContact.setAllowRemove(false);
        modelPhoneContact.setAllowClear(true);
        modelPhoneContact.setAllowDelete(false);
        modelPhoneContact.setAllowSearch(true);
        modelPhoneContact.setAllowHubSearch(false);
        modelPhoneContact.setAllowGotoEdit(true);
        return modelPhoneContact;
    }
    
    public ContactSearchModel getPhoneContactSearchModel() {
        if (modelPhoneContactSearch == null) {
            modelPhoneContactSearch = new ContactSearchModel();
            searchPhone.setPhoneContactSearch(modelPhoneContactSearch.getContactSearch());
        }
        return modelPhoneContactSearch;
    }
    
    public void beforeInput() {
        // hook that is called before search input starts
    }
    
    // uses PhoneSearch to build query, and populate Hub 
    public void performSearch() {
        OASelect<Phone> sel = getPhoneSearch().getSelect();
        sel.setSearchHub(getSearchFromHub());
        sel.setFinder(getFinder());
        getHub().select(sel);
    }
    
    // can to overwritten to know when a selection is made
    public void onSelect(Phone phone, Hub<Phone> hub) {
    }
    // can to overwritten to know when a multi-select is made
    public void onSelect(Hub<Phone> hub) {
    }
}

