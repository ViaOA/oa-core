package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.StoreImportReport;
import com.auto.dev.reportercorp.model.oa.search.StoreImportReportSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class StoreImportReportSearchModel {
	private static Logger LOG = Logger.getLogger(StoreImportReportSearchModel.class.getName());

	protected Hub<StoreImportReport> hub; // search results
	protected Hub<StoreImportReport> hubMultiSelect;
	protected Hub<StoreImportReport> hubSearchFrom; // hub (optional) to search from
	protected Hub<StoreImportReportSearch> hubStoreImportReportSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, StoreImportReport> finder;

	// object used for search data
	protected StoreImportReportSearch storeImportReportSearch;

	public StoreImportReportSearchModel() {
	}

	public StoreImportReportSearchModel(Hub<StoreImportReport> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<StoreImportReport> getHub() {
		if (hub == null) {
			hub = new Hub<StoreImportReport>(StoreImportReport.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<StoreImportReport> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<StoreImportReport> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					StoreImportReportSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<StoreImportReport> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(StoreImportReport.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, StoreImportReport> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, StoreImportReport> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public StoreImportReportSearch getStoreImportReportSearch() {
		if (storeImportReportSearch != null) {
			return storeImportReportSearch;
		}
		storeImportReportSearch = new StoreImportReportSearch();
		return storeImportReportSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<StoreImportReportSearch> getStoreImportReportSearchHub() {
		if (hubStoreImportReportSearch == null) {
			hubStoreImportReportSearch = new Hub<StoreImportReportSearch>(StoreImportReportSearch.class);
			hubStoreImportReportSearch.add(getStoreImportReportSearch());
			hubStoreImportReportSearch.setPos(0);
		}
		return hubStoreImportReportSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses StoreImportReportSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<StoreImportReport> sel = getStoreImportReportSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(StoreImportReport storeImportReport, Hub<StoreImportReport> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<StoreImportReport> hub) {
	}
}
