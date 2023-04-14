package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.EnvironmentVerification;
import com.auto.dev.reportercorp.model.oa.search.EnvironmentVerificationSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class EnvironmentVerificationSearchModel {
	private static Logger LOG = Logger.getLogger(EnvironmentVerificationSearchModel.class.getName());

	protected Hub<EnvironmentVerification> hub; // search results
	protected Hub<EnvironmentVerification> hubMultiSelect;
	protected Hub<EnvironmentVerification> hubSearchFrom; // hub (optional) to search from
	protected Hub<EnvironmentVerificationSearch> hubEnvironmentVerificationSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, EnvironmentVerification> finder;

	// object used for search data
	protected EnvironmentVerificationSearch environmentVerificationSearch;

	public EnvironmentVerificationSearchModel() {
	}

	public EnvironmentVerificationSearchModel(Hub<EnvironmentVerification> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<EnvironmentVerification> getHub() {
		if (hub == null) {
			hub = new Hub<EnvironmentVerification>(EnvironmentVerification.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<EnvironmentVerification> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<EnvironmentVerification> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					EnvironmentVerificationSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<EnvironmentVerification> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(EnvironmentVerification.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, EnvironmentVerification> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, EnvironmentVerification> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public EnvironmentVerificationSearch getEnvironmentVerificationSearch() {
		if (environmentVerificationSearch != null) {
			return environmentVerificationSearch;
		}
		environmentVerificationSearch = new EnvironmentVerificationSearch();
		return environmentVerificationSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<EnvironmentVerificationSearch> getEnvironmentVerificationSearchHub() {
		if (hubEnvironmentVerificationSearch == null) {
			hubEnvironmentVerificationSearch = new Hub<EnvironmentVerificationSearch>(EnvironmentVerificationSearch.class);
			hubEnvironmentVerificationSearch.add(getEnvironmentVerificationSearch());
			hubEnvironmentVerificationSearch.setPos(0);
		}
		return hubEnvironmentVerificationSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses EnvironmentVerificationSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<EnvironmentVerification> sel = getEnvironmentVerificationSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(EnvironmentVerification environmentVerification, Hub<EnvironmentVerification> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<EnvironmentVerification> hub) {
	}
}
