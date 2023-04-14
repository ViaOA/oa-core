package com.auto.dev.reportercorp.model.filter;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.AppUserLogin;
import com.auto.dev.reportercorp.model.oa.filter.AppUserLoginLastDayFilter;
import com.viaoa.hub.Hub;

public class AppUserLoginLastDayFilterModel {
	private static Logger LOG = Logger.getLogger(AppUserLoginLastDayFilterModel.class.getName());

	// Hubs
	protected Hub<AppUserLoginLastDayFilter> hubFilter;

	// ObjectModels

	// object used for filter data
	protected AppUserLoginLastDayFilter filter;

	public AppUserLoginLastDayFilterModel(Hub<AppUserLogin> hubMaster, Hub<AppUserLogin> hub) {
		filter = new AppUserLoginLastDayFilter(hubMaster, hub);
	}

	public AppUserLoginLastDayFilterModel(Hub<AppUserLogin> hub) {
		filter = new AppUserLoginLastDayFilter(hub);
	}

	// object used to input query data, to be used by filterHub
	public AppUserLoginLastDayFilter getFilter() {
		return filter;
	}

	// hub for filter UI object - used to bind with UI components for entering filter data
	public Hub<AppUserLoginLastDayFilter> getFilterHub() {
		if (hubFilter == null) {
			hubFilter = new Hub<AppUserLoginLastDayFilter>(AppUserLoginLastDayFilter.class);
			hubFilter.add(getFilter());
			hubFilter.setPos(0);
		}
		return hubFilter;
	}

	// get the Filtered hub
	public Hub<AppUserLogin> getHub() {
		return getFilter().getHubFilter().getHub();
	}
}
