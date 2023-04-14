package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.PypeReportMessage;
import com.auto.dev.reportercorp.model.oa.search.PypeReportMessageSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class PypeReportMessageSearchModel {
	private static Logger LOG = Logger.getLogger(PypeReportMessageSearchModel.class.getName());

	protected Hub<PypeReportMessage> hub; // search results
	protected Hub<PypeReportMessage> hubMultiSelect;
	protected Hub<PypeReportMessage> hubSearchFrom; // hub (optional) to search from
	protected Hub<PypeReportMessageSearch> hubPypeReportMessageSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, PypeReportMessage> finder;

	// object used for search data
	protected PypeReportMessageSearch pypeReportMessageSearch;

	public PypeReportMessageSearchModel() {
	}

	public PypeReportMessageSearchModel(Hub<PypeReportMessage> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<PypeReportMessage> getHub() {
		if (hub == null) {
			hub = new Hub<PypeReportMessage>(PypeReportMessage.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<PypeReportMessage> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<PypeReportMessage> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					PypeReportMessageSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<PypeReportMessage> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(PypeReportMessage.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, PypeReportMessage> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, PypeReportMessage> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public PypeReportMessageSearch getPypeReportMessageSearch() {
		if (pypeReportMessageSearch != null) {
			return pypeReportMessageSearch;
		}
		pypeReportMessageSearch = new PypeReportMessageSearch();
		return pypeReportMessageSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<PypeReportMessageSearch> getPypeReportMessageSearchHub() {
		if (hubPypeReportMessageSearch == null) {
			hubPypeReportMessageSearch = new Hub<PypeReportMessageSearch>(PypeReportMessageSearch.class);
			hubPypeReportMessageSearch.add(getPypeReportMessageSearch());
			hubPypeReportMessageSearch.setPos(0);
		}
		return hubPypeReportMessageSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses PypeReportMessageSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<PypeReportMessage> sel = getPypeReportMessageSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(PypeReportMessage pypeReportMessage, Hub<PypeReportMessage> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<PypeReportMessage> hub) {
	}
}
