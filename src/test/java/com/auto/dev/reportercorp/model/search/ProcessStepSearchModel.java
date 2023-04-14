package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.ProcessStep;
import com.auto.dev.reportercorp.model.oa.search.ProcessStepSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class ProcessStepSearchModel {
	private static Logger LOG = Logger.getLogger(ProcessStepSearchModel.class.getName());

	protected Hub<ProcessStep> hub; // search results
	protected Hub<ProcessStep> hubMultiSelect;
	protected Hub<ProcessStep> hubSearchFrom; // hub (optional) to search from
	protected Hub<ProcessStepSearch> hubProcessStepSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, ProcessStep> finder;

	// object used for search data
	protected ProcessStepSearch processStepSearch;

	public ProcessStepSearchModel() {
	}

	public ProcessStepSearchModel(Hub<ProcessStep> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<ProcessStep> getHub() {
		if (hub == null) {
			hub = new Hub<ProcessStep>(ProcessStep.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<ProcessStep> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<ProcessStep> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					ProcessStepSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<ProcessStep> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(ProcessStep.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, ProcessStep> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, ProcessStep> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public ProcessStepSearch getProcessStepSearch() {
		if (processStepSearch != null) {
			return processStepSearch;
		}
		processStepSearch = new ProcessStepSearch();
		return processStepSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<ProcessStepSearch> getProcessStepSearchHub() {
		if (hubProcessStepSearch == null) {
			hubProcessStepSearch = new Hub<ProcessStepSearch>(ProcessStepSearch.class);
			hubProcessStepSearch.add(getProcessStepSearch());
			hubProcessStepSearch.setPos(0);
		}
		return hubProcessStepSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses ProcessStepSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<ProcessStep> sel = getProcessStepSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(ProcessStep processStep, Hub<ProcessStep> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<ProcessStep> hub) {
	}
}
