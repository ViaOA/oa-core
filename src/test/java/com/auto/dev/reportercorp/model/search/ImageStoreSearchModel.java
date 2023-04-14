package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.ImageStore;
import com.auto.dev.reportercorp.model.oa.search.ImageStoreSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class ImageStoreSearchModel {
	private static Logger LOG = Logger.getLogger(ImageStoreSearchModel.class.getName());

	protected Hub<ImageStore> hub; // search results
	protected Hub<ImageStore> hubMultiSelect;
	protected Hub<ImageStore> hubSearchFrom; // hub (optional) to search from
	protected Hub<ImageStoreSearch> hubImageStoreSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, ImageStore> finder;

	// object used for search data
	protected ImageStoreSearch imageStoreSearch;

	public ImageStoreSearchModel() {
	}

	public ImageStoreSearchModel(Hub<ImageStore> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<ImageStore> getHub() {
		if (hub == null) {
			hub = new Hub<ImageStore>(ImageStore.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<ImageStore> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<ImageStore> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					ImageStoreSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<ImageStore> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(ImageStore.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, ImageStore> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, ImageStore> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public ImageStoreSearch getImageStoreSearch() {
		if (imageStoreSearch != null) {
			return imageStoreSearch;
		}
		imageStoreSearch = new ImageStoreSearch();
		return imageStoreSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<ImageStoreSearch> getImageStoreSearchHub() {
		if (hubImageStoreSearch == null) {
			hubImageStoreSearch = new Hub<ImageStoreSearch>(ImageStoreSearch.class);
			hubImageStoreSearch.add(getImageStoreSearch());
			hubImageStoreSearch.setPos(0);
		}
		return hubImageStoreSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses ImageStoreSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<ImageStore> sel = getImageStoreSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(ImageStore imageStore, Hub<ImageStore> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<ImageStore> hub) {
	}
}
