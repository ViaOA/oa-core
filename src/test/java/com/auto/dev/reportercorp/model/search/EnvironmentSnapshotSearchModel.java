package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.EnvironmentSnapshot;
import com.auto.dev.reportercorp.model.oa.search.EnvironmentSnapshotSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class EnvironmentSnapshotSearchModel {
	private static Logger LOG = Logger.getLogger(EnvironmentSnapshotSearchModel.class.getName());

	protected Hub<EnvironmentSnapshot> hub; // search results
	protected Hub<EnvironmentSnapshot> hubMultiSelect;
	protected Hub<EnvironmentSnapshot> hubSearchFrom; // hub (optional) to search from
	protected Hub<EnvironmentSnapshotSearch> hubEnvironmentSnapshotSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, EnvironmentSnapshot> finder;

	// object used for search data
	protected EnvironmentSnapshotSearch environmentSnapshotSearch;

	public EnvironmentSnapshotSearchModel() {
	}

	public EnvironmentSnapshotSearchModel(Hub<EnvironmentSnapshot> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<EnvironmentSnapshot> getHub() {
		if (hub == null) {
			hub = new Hub<EnvironmentSnapshot>(EnvironmentSnapshot.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<EnvironmentSnapshot> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<EnvironmentSnapshot> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					EnvironmentSnapshotSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<EnvironmentSnapshot> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(EnvironmentSnapshot.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, EnvironmentSnapshot> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, EnvironmentSnapshot> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public EnvironmentSnapshotSearch getEnvironmentSnapshotSearch() {
		if (environmentSnapshotSearch != null) {
			return environmentSnapshotSearch;
		}
		environmentSnapshotSearch = new EnvironmentSnapshotSearch();
		return environmentSnapshotSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<EnvironmentSnapshotSearch> getEnvironmentSnapshotSearchHub() {
		if (hubEnvironmentSnapshotSearch == null) {
			hubEnvironmentSnapshotSearch = new Hub<EnvironmentSnapshotSearch>(EnvironmentSnapshotSearch.class);
			hubEnvironmentSnapshotSearch.add(getEnvironmentSnapshotSearch());
			hubEnvironmentSnapshotSearch.setPos(0);
		}
		return hubEnvironmentSnapshotSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses EnvironmentSnapshotSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<EnvironmentSnapshot> sel = getEnvironmentSnapshotSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(EnvironmentSnapshot environmentSnapshot, Hub<EnvironmentSnapshot> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<EnvironmentSnapshot> hub) {
	}
}
