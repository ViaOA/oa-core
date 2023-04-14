package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.Environment;
import com.auto.dev.reportercorp.model.oa.search.EnvironmentSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class EnvironmentSearchModel {
	private static Logger LOG = Logger.getLogger(EnvironmentSearchModel.class.getName());

	protected Hub<Environment> hub; // search results
	protected Hub<Environment> hubMultiSelect;
	protected Hub<Environment> hubSearchFrom; // hub (optional) to search from
	protected Hub<EnvironmentSearch> hubEnvironmentSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, Environment> finder;

	// object used for search data
	protected EnvironmentSearch environmentSearch;

	public EnvironmentSearchModel() {
	}

	public EnvironmentSearchModel(Hub<Environment> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<Environment> getHub() {
		if (hub == null) {
			hub = new Hub<Environment>(Environment.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<Environment> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<Environment> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					EnvironmentSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<Environment> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(Environment.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, Environment> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, Environment> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public EnvironmentSearch getEnvironmentSearch() {
		if (environmentSearch != null) {
			return environmentSearch;
		}
		environmentSearch = new EnvironmentSearch();
		return environmentSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<EnvironmentSearch> getEnvironmentSearchHub() {
		if (hubEnvironmentSearch == null) {
			hubEnvironmentSearch = new Hub<EnvironmentSearch>(EnvironmentSearch.class);
			hubEnvironmentSearch.add(getEnvironmentSearch());
			hubEnvironmentSearch.setPos(0);
		}
		return hubEnvironmentSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses EnvironmentSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<Environment> sel = getEnvironmentSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(Environment environment, Hub<Environment> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<Environment> hub) {
	}
}
