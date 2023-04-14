package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.StoreInfo;
import com.auto.dev.reportercorp.model.oa.search.StoreInfoSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class StoreInfoSearchModel {
	private static Logger LOG = Logger.getLogger(StoreInfoSearchModel.class.getName());

	protected Hub<StoreInfo> hub; // search results
	protected Hub<StoreInfo> hubMultiSelect;
	protected Hub<StoreInfo> hubSearchFrom; // hub (optional) to search from
	protected Hub<StoreInfoSearch> hubStoreInfoSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, StoreInfo> finder;

	// object used for search data
	protected StoreInfoSearch storeInfoSearch;

	public StoreInfoSearchModel() {
	}

	public StoreInfoSearchModel(Hub<StoreInfo> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<StoreInfo> getHub() {
		if (hub == null) {
			hub = new Hub<StoreInfo>(StoreInfo.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<StoreInfo> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<StoreInfo> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					StoreInfoSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<StoreInfo> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(StoreInfo.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, StoreInfo> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, StoreInfo> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public StoreInfoSearch getStoreInfoSearch() {
		if (storeInfoSearch != null) {
			return storeInfoSearch;
		}
		storeInfoSearch = new StoreInfoSearch();
		return storeInfoSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<StoreInfoSearch> getStoreInfoSearchHub() {
		if (hubStoreInfoSearch == null) {
			hubStoreInfoSearch = new Hub<StoreInfoSearch>(StoreInfoSearch.class);
			hubStoreInfoSearch.add(getStoreInfoSearch());
			hubStoreInfoSearch.setPos(0);
		}
		return hubStoreInfoSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses StoreInfoSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<StoreInfo> sel = getStoreInfoSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(StoreInfo storeInfo, Hub<StoreInfo> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<StoreInfo> hub) {
	}
}
