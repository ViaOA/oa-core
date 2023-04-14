package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.StatusInfoMessage;
import com.auto.dev.reportercorp.model.oa.search.StatusInfoMessageSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class StatusInfoMessageSearchModel {
	private static Logger LOG = Logger.getLogger(StatusInfoMessageSearchModel.class.getName());

	protected Hub<StatusInfoMessage> hub; // search results
	protected Hub<StatusInfoMessage> hubMultiSelect;
	protected Hub<StatusInfoMessage> hubSearchFrom; // hub (optional) to search from
	protected Hub<StatusInfoMessageSearch> hubStatusInfoMessageSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, StatusInfoMessage> finder;

	// object used for search data
	protected StatusInfoMessageSearch statusInfoMessageSearch;

	public StatusInfoMessageSearchModel() {
	}

	public StatusInfoMessageSearchModel(Hub<StatusInfoMessage> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<StatusInfoMessage> getHub() {
		if (hub == null) {
			hub = new Hub<StatusInfoMessage>(StatusInfoMessage.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<StatusInfoMessage> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<StatusInfoMessage> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					StatusInfoMessageSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<StatusInfoMessage> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(StatusInfoMessage.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, StatusInfoMessage> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, StatusInfoMessage> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public StatusInfoMessageSearch getStatusInfoMessageSearch() {
		if (statusInfoMessageSearch != null) {
			return statusInfoMessageSearch;
		}
		statusInfoMessageSearch = new StatusInfoMessageSearch();
		return statusInfoMessageSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<StatusInfoMessageSearch> getStatusInfoMessageSearchHub() {
		if (hubStatusInfoMessageSearch == null) {
			hubStatusInfoMessageSearch = new Hub<StatusInfoMessageSearch>(StatusInfoMessageSearch.class);
			hubStatusInfoMessageSearch.add(getStatusInfoMessageSearch());
			hubStatusInfoMessageSearch.setPos(0);
		}
		return hubStatusInfoMessageSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses StatusInfoMessageSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<StatusInfoMessage> sel = getStatusInfoMessageSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(StatusInfoMessage statusInfoMessage, Hub<StatusInfoMessage> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<StatusInfoMessage> hub) {
	}
}
