package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.StatusInfo;
import com.auto.dev.reportercorp.model.oa.search.StatusInfoSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class StatusInfoSearchModel {
	private static Logger LOG = Logger.getLogger(StatusInfoSearchModel.class.getName());

	protected Hub<StatusInfo> hub; // search results
	protected Hub<StatusInfo> hubMultiSelect;
	protected Hub<StatusInfo> hubSearchFrom; // hub (optional) to search from
	protected Hub<StatusInfoSearch> hubStatusInfoSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, StatusInfo> finder;

	// object used for search data
	protected StatusInfoSearch statusInfoSearch;

	public StatusInfoSearchModel() {
	}

	public StatusInfoSearchModel(Hub<StatusInfo> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<StatusInfo> getHub() {
		if (hub == null) {
			hub = new Hub<StatusInfo>(StatusInfo.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<StatusInfo> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<StatusInfo> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					StatusInfoSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<StatusInfo> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(StatusInfo.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, StatusInfo> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, StatusInfo> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public StatusInfoSearch getStatusInfoSearch() {
		if (statusInfoSearch != null) {
			return statusInfoSearch;
		}
		statusInfoSearch = new StatusInfoSearch();
		return statusInfoSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<StatusInfoSearch> getStatusInfoSearchHub() {
		if (hubStatusInfoSearch == null) {
			hubStatusInfoSearch = new Hub<StatusInfoSearch>(StatusInfoSearch.class);
			hubStatusInfoSearch.add(getStatusInfoSearch());
			hubStatusInfoSearch.setPos(0);
		}
		return hubStatusInfoSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses StatusInfoSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<StatusInfo> sel = getStatusInfoSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(StatusInfo statusInfo, Hub<StatusInfo> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<StatusInfo> hub) {
	}
}
