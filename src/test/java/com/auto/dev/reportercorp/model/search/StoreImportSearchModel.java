package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.StoreImport;
import com.auto.dev.reportercorp.model.oa.search.StoreImportSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class StoreImportSearchModel {
	private static Logger LOG = Logger.getLogger(StoreImportSearchModel.class.getName());

	protected Hub<StoreImport> hub; // search results
	protected Hub<StoreImport> hubMultiSelect;
	protected Hub<StoreImport> hubSearchFrom; // hub (optional) to search from
	protected Hub<StoreImportSearch> hubStoreImportSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, StoreImport> finder;

	// object used for search data
	protected StoreImportSearch storeImportSearch;

	public StoreImportSearchModel() {
	}

	public StoreImportSearchModel(Hub<StoreImport> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<StoreImport> getHub() {
		if (hub == null) {
			hub = new Hub<StoreImport>(StoreImport.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<StoreImport> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<StoreImport> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					StoreImportSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<StoreImport> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(StoreImport.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, StoreImport> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, StoreImport> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public StoreImportSearch getStoreImportSearch() {
		if (storeImportSearch != null) {
			return storeImportSearch;
		}
		storeImportSearch = new StoreImportSearch();
		return storeImportSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<StoreImportSearch> getStoreImportSearchHub() {
		if (hubStoreImportSearch == null) {
			hubStoreImportSearch = new Hub<StoreImportSearch>(StoreImportSearch.class);
			hubStoreImportSearch.add(getStoreImportSearch());
			hubStoreImportSearch.setPos(0);
		}
		return hubStoreImportSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses StoreImportSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<StoreImport> sel = getStoreImportSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(StoreImport storeImport, Hub<StoreImport> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<StoreImport> hub) {
	}
}
