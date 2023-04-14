package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.Verification;
import com.auto.dev.reportercorp.model.oa.search.VerificationSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class VerificationSearchModel {
	private static Logger LOG = Logger.getLogger(VerificationSearchModel.class.getName());

	protected Hub<Verification> hub; // search results
	protected Hub<Verification> hubMultiSelect;
	protected Hub<Verification> hubSearchFrom; // hub (optional) to search from
	protected Hub<VerificationSearch> hubVerificationSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, Verification> finder;

	// object used for search data
	protected VerificationSearch verificationSearch;

	public VerificationSearchModel() {
	}

	public VerificationSearchModel(Hub<Verification> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<Verification> getHub() {
		if (hub == null) {
			hub = new Hub<Verification>(Verification.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<Verification> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<Verification> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					VerificationSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<Verification> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(Verification.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, Verification> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, Verification> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public VerificationSearch getVerificationSearch() {
		if (verificationSearch != null) {
			return verificationSearch;
		}
		verificationSearch = new VerificationSearch();
		return verificationSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<VerificationSearch> getVerificationSearchHub() {
		if (hubVerificationSearch == null) {
			hubVerificationSearch = new Hub<VerificationSearch>(VerificationSearch.class);
			hubVerificationSearch.add(getVerificationSearch());
			hubVerificationSearch.setPos(0);
		}
		return hubVerificationSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses VerificationSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<Verification> sel = getVerificationSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(Verification verification, Hub<Verification> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<Verification> hub) {
	}
}
