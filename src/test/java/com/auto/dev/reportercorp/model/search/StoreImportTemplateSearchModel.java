package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.StoreImportTemplate;
import com.auto.dev.reportercorp.model.oa.search.StoreImportTemplateSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class StoreImportTemplateSearchModel {
	private static Logger LOG = Logger.getLogger(StoreImportTemplateSearchModel.class.getName());

	protected Hub<StoreImportTemplate> hub; // search results
	protected Hub<StoreImportTemplate> hubMultiSelect;
	protected Hub<StoreImportTemplate> hubSearchFrom; // hub (optional) to search from
	protected Hub<StoreImportTemplateSearch> hubStoreImportTemplateSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, StoreImportTemplate> finder;

	// object used for search data
	protected StoreImportTemplateSearch storeImportTemplateSearch;

	public StoreImportTemplateSearchModel() {
	}

	public StoreImportTemplateSearchModel(Hub<StoreImportTemplate> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<StoreImportTemplate> getHub() {
		if (hub == null) {
			hub = new Hub<StoreImportTemplate>(StoreImportTemplate.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<StoreImportTemplate> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<StoreImportTemplate> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					StoreImportTemplateSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<StoreImportTemplate> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(StoreImportTemplate.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, StoreImportTemplate> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, StoreImportTemplate> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public StoreImportTemplateSearch getStoreImportTemplateSearch() {
		if (storeImportTemplateSearch != null) {
			return storeImportTemplateSearch;
		}
		storeImportTemplateSearch = new StoreImportTemplateSearch();
		return storeImportTemplateSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<StoreImportTemplateSearch> getStoreImportTemplateSearchHub() {
		if (hubStoreImportTemplateSearch == null) {
			hubStoreImportTemplateSearch = new Hub<StoreImportTemplateSearch>(StoreImportTemplateSearch.class);
			hubStoreImportTemplateSearch.add(getStoreImportTemplateSearch());
			hubStoreImportTemplateSearch.setPos(0);
		}
		return hubStoreImportTemplateSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses StoreImportTemplateSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<StoreImportTemplate> sel = getStoreImportTemplateSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(StoreImportTemplate storeImportTemplate, Hub<StoreImportTemplate> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<StoreImportTemplate> hub) {
	}
}
