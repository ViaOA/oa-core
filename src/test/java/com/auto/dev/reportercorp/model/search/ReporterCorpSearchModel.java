package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.ReporterCorp;
import com.auto.dev.reportercorp.model.oa.search.ReporterCorpSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class ReporterCorpSearchModel {
	private static Logger LOG = Logger.getLogger(ReporterCorpSearchModel.class.getName());

	protected Hub<ReporterCorp> hub; // search results
	protected Hub<ReporterCorp> hubMultiSelect;
	protected Hub<ReporterCorp> hubSearchFrom; // hub (optional) to search from
	protected Hub<ReporterCorpSearch> hubReporterCorpSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, ReporterCorp> finder;

	// object used for search data
	protected ReporterCorpSearch reporterCorpSearch;

	public ReporterCorpSearchModel() {
	}

	public ReporterCorpSearchModel(Hub<ReporterCorp> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<ReporterCorp> getHub() {
		if (hub == null) {
			hub = new Hub<ReporterCorp>(ReporterCorp.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<ReporterCorp> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<ReporterCorp> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					ReporterCorpSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<ReporterCorp> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(ReporterCorp.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, ReporterCorp> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, ReporterCorp> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public ReporterCorpSearch getReporterCorpSearch() {
		if (reporterCorpSearch != null) {
			return reporterCorpSearch;
		}
		reporterCorpSearch = new ReporterCorpSearch();
		return reporterCorpSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<ReporterCorpSearch> getReporterCorpSearchHub() {
		if (hubReporterCorpSearch == null) {
			hubReporterCorpSearch = new Hub<ReporterCorpSearch>(ReporterCorpSearch.class);
			hubReporterCorpSearch.add(getReporterCorpSearch());
			hubReporterCorpSearch.setPos(0);
		}
		return hubReporterCorpSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses ReporterCorpSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<ReporterCorp> sel = getReporterCorpSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(ReporterCorp reporterCorp, Hub<ReporterCorp> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<ReporterCorp> hub) {
	}
}
