package com.auto.dev.reportercorp.model.oa.filter;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.Environment;
import com.auto.dev.reportercorp.model.oa.propertypath.EnvironmentPP;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAClassFilter;
import com.viaoa.hub.CustomHubFilter;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubFilter;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheFilter;

@OAClass(useDataSource = false, localOnly = true)
@OAClassFilter(name = "NonProd", displayName = "Non Prod", hasInputParams = false)
public class EnvironmentNonProdFilter extends OAObject implements CustomHubFilter<Environment> {
	private static final long serialVersionUID = 1L;

	private static Logger LOG = Logger.getLogger(EnvironmentNonProdFilter.class.getName());

	public static final String PPCode = ":NonProd()";
	private Hub<Environment> hubMaster;
	private Hub<Environment> hub;
	private HubFilter<Environment> hubFilter;
	private OAObjectCacheFilter<Environment> cacheFilter;
	private boolean bUseObjectCache;

	public EnvironmentNonProdFilter() {
		this(null, null, false);
	}

	public EnvironmentNonProdFilter(Hub<Environment> hub) {
		this(null, hub, true);
	}

	public EnvironmentNonProdFilter(Hub<Environment> hubMaster, Hub<Environment> hub) {
		this(hubMaster, hub, false);
	}

	public EnvironmentNonProdFilter(Hub<Environment> hubMaster, Hub<Environment> hubFiltered, boolean bUseObjectCache) {
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
	public HubFilter<Environment> getHubFilter() {
		if (hubFilter != null) {
			return hubFilter;
		}
		if (hubMaster == null) {
			return null;
		}
		hubFilter = new HubFilter<Environment>(hubMaster, hub) {
			@Override
			public boolean isUsed(Environment environment) {
				return EnvironmentNonProdFilter.this.isUsed(environment);
			}
		};
		hubFilter.addDependentProperty(EnvironmentPP.type(), false);
		hubFilter.refresh();
		return hubFilter;
	}

	public OAObjectCacheFilter<Environment> getObjectCacheFilter() {
		if (cacheFilter != null) {
			return cacheFilter;
		}
		if (!bUseObjectCache) {
			return null;
		}
		hub.onBeforeRefresh(e -> reselect());
		cacheFilter = new OAObjectCacheFilter<Environment>(hub) {
			@Override
			public boolean isUsed(Environment environment) {
				return EnvironmentNonProdFilter.this.isUsed(environment);
			}

			@Override
			protected void reselect() {
				EnvironmentNonProdFilter.this.reselect();
			}
		};
		cacheFilter.addDependentProperty(EnvironmentPP.type(), false);
		cacheFilter.refresh();
		return cacheFilter;
	}

	public void reselect() {
		// can be overwritten to query datasource
	}

	// ==================
	// this method has custom code that will need to be put into the OABuilder filter

	@Override
	public boolean isUsed(Environment environment) {
		return (environment.getType() != Environment.TYPE_Prod);
	}
}
