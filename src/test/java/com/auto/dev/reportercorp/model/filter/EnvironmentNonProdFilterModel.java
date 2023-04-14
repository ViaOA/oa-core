package com.auto.dev.reportercorp.model.filter;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.Environment;
import com.auto.dev.reportercorp.model.oa.filter.EnvironmentNonProdFilter;
import com.viaoa.hub.Hub;

public class EnvironmentNonProdFilterModel {
	private static Logger LOG = Logger.getLogger(EnvironmentNonProdFilterModel.class.getName());

	// Hubs
	protected Hub<EnvironmentNonProdFilter> hubFilter;

	// ObjectModels

	// object used for filter data
	protected EnvironmentNonProdFilter filter;

	public EnvironmentNonProdFilterModel(Hub<Environment> hubMaster, Hub<Environment> hub) {
		filter = new EnvironmentNonProdFilter(hubMaster, hub);
	}

	public EnvironmentNonProdFilterModel(Hub<Environment> hub) {
		filter = new EnvironmentNonProdFilter(hub);
	}

	// object used to input query data, to be used by filterHub
	public EnvironmentNonProdFilter getFilter() {
		return filter;
	}

	// hub for filter UI object - used to bind with UI components for entering filter data
	public Hub<EnvironmentNonProdFilter> getFilterHub() {
		if (hubFilter == null) {
			hubFilter = new Hub<EnvironmentNonProdFilter>(EnvironmentNonProdFilter.class);
			hubFilter.add(getFilter());
			hubFilter.setPos(0);
		}
		return hubFilter;
	}

	// get the Filtered hub
	public Hub<Environment> getHub() {
		return getFilter().getHubFilter().getHub();
	}
}
