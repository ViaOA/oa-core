package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.ReporterCorpParam;
import com.auto.dev.reportercorp.model.oa.search.ReporterCorpParamSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class ReporterCorpParamSearchModel {
	private static Logger LOG = Logger.getLogger(ReporterCorpParamSearchModel.class.getName());

	protected Hub<ReporterCorpParam> hub; // search results
	protected Hub<ReporterCorpParam> hubMultiSelect;
	protected Hub<ReporterCorpParam> hubSearchFrom; // hub (optional) to search from
	protected Hub<ReporterCorpParamSearch> hubReporterCorpParamSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, ReporterCorpParam> finder;

	// object used for search data
	protected ReporterCorpParamSearch reporterCorpParamSearch;

	public ReporterCorpParamSearchModel() {
	}

	public ReporterCorpParamSearchModel(Hub<ReporterCorpParam> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<ReporterCorpParam> getHub() {
		if (hub == null) {
			hub = new Hub<ReporterCorpParam>(ReporterCorpParam.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<ReporterCorpParam> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<ReporterCorpParam> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					ReporterCorpParamSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<ReporterCorpParam> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(ReporterCorpParam.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, ReporterCorpParam> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, ReporterCorpParam> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public ReporterCorpParamSearch getReporterCorpParamSearch() {
		if (reporterCorpParamSearch != null) {
			return reporterCorpParamSearch;
		}
		reporterCorpParamSearch = new ReporterCorpParamSearch();
		return reporterCorpParamSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<ReporterCorpParamSearch> getReporterCorpParamSearchHub() {
		if (hubReporterCorpParamSearch == null) {
			hubReporterCorpParamSearch = new Hub<ReporterCorpParamSearch>(ReporterCorpParamSearch.class);
			hubReporterCorpParamSearch.add(getReporterCorpParamSearch());
			hubReporterCorpParamSearch.setPos(0);
		}
		return hubReporterCorpParamSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses ReporterCorpParamSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<ReporterCorpParam> sel = getReporterCorpParamSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(ReporterCorpParam reporterCorpParam, Hub<ReporterCorpParam> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<ReporterCorpParam> hub) {
	}
}
