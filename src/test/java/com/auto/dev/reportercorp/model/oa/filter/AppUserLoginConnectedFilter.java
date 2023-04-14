package com.auto.dev.reportercorp.model.oa.filter;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.AppUserLogin;
import com.auto.dev.reportercorp.model.oa.propertypath.AppUserLoginPP;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAClassFilter;
import com.viaoa.hub.CustomHubFilter;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubFilter;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheFilter;

@OAClass(useDataSource = false, localOnly = true)
@OAClassFilter(name = "Connected", displayName = "Connected", hasInputParams = false, query = "disconnected == null")
public class AppUserLoginConnectedFilter extends OAObject implements CustomHubFilter<AppUserLogin> {
	private static final long serialVersionUID = 1L;

	private static Logger LOG = Logger.getLogger(AppUserLoginConnectedFilter.class.getName());

	public static final String PPCode = ":Connected()";
	private Hub<AppUserLogin> hubMaster;
	private Hub<AppUserLogin> hub;
	private HubFilter<AppUserLogin> hubFilter;
	private OAObjectCacheFilter<AppUserLogin> cacheFilter;
	private boolean bUseObjectCache;

	public AppUserLoginConnectedFilter() {
		this(null, null, false);
	}

	public AppUserLoginConnectedFilter(Hub<AppUserLogin> hub) {
		this(null, hub, true);
	}

	public AppUserLoginConnectedFilter(Hub<AppUserLogin> hubMaster, Hub<AppUserLogin> hub) {
		this(hubMaster, hub, false);
	}

	public AppUserLoginConnectedFilter(Hub<AppUserLogin> hubMaster, Hub<AppUserLogin> hubFiltered, boolean bUseObjectCache) {
		this.hubMaster = hubMaster;
		this.hub = hubFiltered;
		this.bUseObjectCache = bUseObjectCache;
		if (hubMaster != null) {
			getHubFilter();
		}
		if (bUseObjectCache) {
			getObjectCacheFilter();
		}
	}

	public void reset() {
	}

	public boolean isDataEntered() {
		return false;
	}

	public void refresh() {
		if (hubFilter != null) {
			getHubFilter().refresh();
		}
		if (cacheFilter != null) {
			getObjectCacheFilter().refresh();
		}
	}

	@Override
	public HubFilter<AppUserLogin> getHubFilter() {
		if (hubFilter != null) {
			return hubFilter;
		}
		if (hubMaster == null) {
			return null;
		}
		hubFilter = new HubFilter<AppUserLogin>(hubMaster, hub) {
			@Override
			public boolean isUsed(AppUserLogin appUserLogin) {
				return AppUserLoginConnectedFilter.this.isUsed(appUserLogin);
			}
		};
		hubFilter.addDependentProperty(AppUserLoginPP.disconnected(), false);
		hubFilter.addDependentProperty(AppUserLoginPP.created(), false);
		hubFilter.refresh();
		return hubFilter;
	}

	public OAObjectCacheFilter<AppUserLogin> getObjectCacheFilter() {
		if (cacheFilter != null) {
			return cacheFilter;
		}
		if (!bUseObjectCache) {
			return null;
		}
		hub.onBeforeRefresh(e -> reselect());
		cacheFilter = new OAObjectCacheFilter<AppUserLogin>(hub) {
			@Override
			public boolean isUsed(AppUserLogin appUserLogin) {
				return AppUserLoginConnectedFilter.this.isUsed(appUserLogin);
			}

			@Override
			protected void reselect() {
				AppUserLoginConnectedFilter.this.reselect();
			}
		};
		cacheFilter.addDependentProperty(AppUserLoginPP.disconnected(), false);
		cacheFilter.addDependentProperty(AppUserLoginPP.created(), false);
		cacheFilter.refresh();
		return cacheFilter;
	}

	public void reselect() {
		// can be overwritten to query datasource
	}

	// ==================
	// this method has custom code that will need to be put into the OABuilder filter

	@Override
	public boolean isUsed(AppUserLogin appUserLogin) {
		return appUserLogin != null && appUserLogin.getDisconnected() == null;
	}

}
