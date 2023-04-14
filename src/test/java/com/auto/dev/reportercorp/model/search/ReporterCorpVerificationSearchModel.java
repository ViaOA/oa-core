package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.ReporterCorpVerification;
import com.auto.dev.reportercorp.model.oa.search.ReporterCorpVerificationSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class ReporterCorpVerificationSearchModel {
	private static Logger LOG = Logger.getLogger(ReporterCorpVerificationSearchModel.class.getName());

	protected Hub<ReporterCorpVerification> hub; // search results
	protected Hub<ReporterCorpVerification> hubMultiSelect;
	protected Hub<ReporterCorpVerification> hubSearchFrom; // hub (optional) to search from
	protected Hub<ReporterCorpVerificationSearch> hubReporterCorpVerificationSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, ReporterCorpVerification> finder;

	// object used for search data
	protected ReporterCorpVerificationSearch reporterCorpVerificationSearch;

	public ReporterCorpVerificationSearchModel() {
	}

	public ReporterCorpVerificationSearchModel(Hub<ReporterCorpVerification> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<ReporterCorpVerification> getHub() {
		if (hub == null) {
			hub = new Hub<ReporterCorpVerification>(ReporterCorpVerification.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<ReporterCorpVerification> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<ReporterCorpVerification> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					ReporterCorpVerificationSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<ReporterCorpVerification> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(ReporterCorpVerification.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, ReporterCorpVerification> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, ReporterCorpVerification> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public ReporterCorpVerificationSearch getReporterCorpVerificationSearch() {
		if (reporterCorpVerificationSearch != null) {
			return reporterCorpVerificationSearch;
		}
		reporterCorpVerificationSearch = new ReporterCorpVerificationSearch();
		return reporterCorpVerificationSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<ReporterCorpVerificationSearch> getReporterCorpVerificationSearchHub() {
		if (hubReporterCorpVerificationSearch == null) {
			hubReporterCorpVerificationSearch = new Hub<ReporterCorpVerificationSearch>(ReporterCorpVerificationSearch.class);
			hubReporterCorpVerificationSearch.add(getReporterCorpVerificationSearch());
			hubReporterCorpVerificationSearch.setPos(0);
		}
		return hubReporterCorpVerificationSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses ReporterCorpVerificationSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<ReporterCorpVerification> sel = getReporterCorpVerificationSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(ReporterCorpVerification reporterCorpVerification, Hub<ReporterCorpVerification> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<ReporterCorpVerification> hub) {
	}
}
