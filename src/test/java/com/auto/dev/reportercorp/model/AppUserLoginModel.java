package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.delegate.ModelDelegate;
import com.auto.dev.reportercorp.model.oa.AppServer;
import com.auto.dev.reportercorp.model.oa.AppUser;
import com.auto.dev.reportercorp.model.oa.AppUserError;
import com.auto.dev.reportercorp.model.oa.AppUserLogin;
import com.auto.dev.reportercorp.model.search.AppServerSearchModel;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCombined;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class AppUserLoginModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(AppUserLoginModel.class.getName());

	// Hubs
	protected Hub<AppUserLogin> hub;
	// selected appUserLogins
	protected Hub<AppUserLogin> hubMultiSelect;
	// detail hubs
	protected Hub<AppUser> hubAppUser;
	protected Hub<AppServer> hubAppServers;
	protected Hub<AppUserError> hubAppUserErrors;

	// AddHubs used for references
	protected Hub<AppUser> hubAppUserSelectFrom;

	// ObjectModels
	protected AppUserModel modelAppUser;
	protected AppServerModel modelAppServers;
	protected AppUserErrorModel modelAppUserErrors;

	// selectFrom
	protected AppUserModel modelAppUserSelectFrom;

	// SearchModels used for references
	protected AppServerSearchModel modelAppServersSearch;

	public AppUserLoginModel() {
		setDisplayName("App User Login");
		setPluralDisplayName("App User Logins");
	}

	public AppUserLoginModel(Hub<AppUserLogin> hubAppUserLogin) {
		this();
		if (hubAppUserLogin != null) {
			HubDelegate.setObjectClass(hubAppUserLogin, AppUserLogin.class);
		}
		this.hub = hubAppUserLogin;
	}

	public AppUserLoginModel(AppUserLogin appUserLogin) {
		this();
		getHub().add(appUserLogin);
		getHub().setPos(0);
	}

	public Hub<AppUserLogin> getOriginalHub() {
		return getHub();
	}

	public Hub<AppUser> getAppUserHub() {
		if (hubAppUser != null) {
			return hubAppUser;
		}
		// this is the owner, use detailHub
		hubAppUser = getHub().getDetailHub(AppUserLogin.P_AppUser);
		return hubAppUser;
	}

	public Hub<AppServer> getAppServers() {
		// createMethod=false, used by getAppServersSearchModel() for searches
		if (hubAppServers != null) {
			return hubAppServers;
		}
		hubAppServers = new Hub<AppServer>(AppServer.class);
		hubAppServers.setSelectWhereHub(getHub(), AppUserLogin.P_AppServers);
		getHub().onChangeAO(hubEvent -> hubAppServers.clear());
		return hubAppServers;
	}

	public Hub<AppUserError> getAppUserErrors() {
		if (hubAppUserErrors == null) {
			hubAppUserErrors = getHub().getDetailHub(AppUserLogin.P_AppUserErrors);
		}
		return hubAppUserErrors;
	}

	public Hub<AppUser> getAppUserSelectFromHub() {
		if (hubAppUserSelectFrom != null) {
			return hubAppUserSelectFrom;
		}
		hubAppUserSelectFrom = new Hub<AppUser>(AppUser.class);
		Hub<AppUser> hubAppUserSelectFrom1 = ModelDelegate.getAppUsers().createSharedHub();
		HubCombined<AppUser> hubCombined = new HubCombined(hubAppUserSelectFrom, hubAppUserSelectFrom1, getAppUserHub());
		hubAppUserSelectFrom.setLinkHub(getHub(), AppUserLogin.P_AppUser);
		return hubAppUserSelectFrom;
	}

	public AppUserLogin getAppUserLogin() {
		return getHub().getAO();
	}

	public Hub<AppUserLogin> getHub() {
		if (hub == null) {
			hub = new Hub<AppUserLogin>(AppUserLogin.class);
		}
		return hub;
	}

	public Hub<AppUserLogin> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<AppUserLogin>(AppUserLogin.class);
		}
		return hubMultiSelect;
	}

	public AppUserModel getAppUserModel() {
		if (modelAppUser != null) {
			return modelAppUser;
		}
		modelAppUser = new AppUserModel(getAppUserHub());
		modelAppUser.setDisplayName("App User");
		modelAppUser.setPluralDisplayName("App Users");
		modelAppUser.setForJfc(getForJfc());
		modelAppUser.setAllowNew(false);
		modelAppUser.setAllowSave(true);
		modelAppUser.setAllowAdd(false);
		modelAppUser.setAllowRemove(false);
		modelAppUser.setAllowClear(false);
		modelAppUser.setAllowDelete(false);
		modelAppUser.setAllowSearch(false);
		modelAppUser.setAllowHubSearch(false);
		modelAppUser.setAllowGotoEdit(false);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelAppUser.setCreateUI(li == null || !AppUserLogin.P_AppUser.equalsIgnoreCase(li.getName()));
		modelAppUser.setViewOnly(getViewOnly());
		// call AppUserLogin.appUserModelCallback(AppUserModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(AppUserLogin.class, AppUserLogin.P_AppUser, modelAppUser);

		return modelAppUser;
	}

	public AppServerModel getAppServersModel() {
		if (modelAppServers != null) {
			return modelAppServers;
		}
		modelAppServers = new AppServerModel(getAppServers());
		modelAppServers.setDisplayName("App Server");
		modelAppServers.setPluralDisplayName("App Servers");
		modelAppServers.setForJfc(getForJfc());
		modelAppServers.setAllowNew(false);
		modelAppServers.setAllowSave(true);
		modelAppServers.setAllowAdd(false);
		modelAppServers.setAllowMove(false);
		modelAppServers.setAllowRemove(false);
		modelAppServers.setAllowDelete(true);
		modelAppServers.setAllowRefresh(false);
		modelAppServers.setAllowSearch(true);
		modelAppServers.setAllowHubSearch(false);
		modelAppServers.setAllowGotoEdit(false);
		modelAppServers.setViewOnly(getViewOnly());
		modelAppServers.setAllowTableFilter(true);
		modelAppServers.setAllowTableSorting(true);
		modelAppServers.setAllowMultiSelect(false);
		modelAppServers.setAllowCopy(false);
		modelAppServers.setAllowCut(false);
		modelAppServers.setAllowPaste(false);
		// call AppUserLogin.appServersModelCallback(AppServerModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(AppUserLogin.class, AppUserLogin.P_AppServers, modelAppServers);

		return modelAppServers;
	}

	public AppUserErrorModel getAppUserErrorsModel() {
		if (modelAppUserErrors != null) {
			return modelAppUserErrors;
		}
		modelAppUserErrors = new AppUserErrorModel(getAppUserErrors());
		modelAppUserErrors.setDisplayName("App User Error");
		modelAppUserErrors.setPluralDisplayName("App User Errors");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getAppUserErrors())) {
			modelAppUserErrors.setCreateUI(false);
		}
		modelAppUserErrors.setForJfc(getForJfc());
		modelAppUserErrors.setAllowNew(true);
		modelAppUserErrors.setAllowSave(true);
		modelAppUserErrors.setAllowAdd(false);
		modelAppUserErrors.setAllowMove(false);
		modelAppUserErrors.setAllowRemove(false);
		modelAppUserErrors.setAllowDelete(true);
		modelAppUserErrors.setAllowRefresh(false);
		modelAppUserErrors.setAllowSearch(false);
		modelAppUserErrors.setAllowHubSearch(false);
		modelAppUserErrors.setAllowGotoEdit(true);
		modelAppUserErrors.setViewOnly(getViewOnly());
		modelAppUserErrors.setAllowTableFilter(true);
		modelAppUserErrors.setAllowTableSorting(true);
		modelAppUserErrors.setAllowMultiSelect(false);
		modelAppUserErrors.setAllowCopy(false);
		modelAppUserErrors.setAllowCut(false);
		modelAppUserErrors.setAllowPaste(false);
		// call AppUserLogin.appUserErrorsModelCallback(AppUserErrorModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(AppUserLogin.class, AppUserLogin.P_AppUserErrors, modelAppUserErrors);

		return modelAppUserErrors;
	}

	public AppUserModel getAppUserSelectFromModel() {
		if (modelAppUserSelectFrom != null) {
			return modelAppUserSelectFrom;
		}
		modelAppUserSelectFrom = new AppUserModel(getAppUserSelectFromHub());
		modelAppUserSelectFrom.setDisplayName("App User");
		modelAppUserSelectFrom.setPluralDisplayName("App Users");
		modelAppUserSelectFrom.setForJfc(getForJfc());
		modelAppUserSelectFrom.setAllowNew(false);
		modelAppUserSelectFrom.setAllowSave(true);
		modelAppUserSelectFrom.setAllowAdd(false);
		modelAppUserSelectFrom.setAllowMove(false);
		modelAppUserSelectFrom.setAllowRemove(false);
		modelAppUserSelectFrom.setAllowDelete(false);
		modelAppUserSelectFrom.setAllowSearch(false);
		modelAppUserSelectFrom.setAllowHubSearch(true);
		modelAppUserSelectFrom.setAllowGotoEdit(true);
		modelAppUserSelectFrom.setViewOnly(getViewOnly());
		modelAppUserSelectFrom.setAllowNew(false);
		modelAppUserSelectFrom.setAllowTableFilter(true);
		modelAppUserSelectFrom.setAllowTableSorting(true);
		modelAppUserSelectFrom.setAllowCut(false);
		modelAppUserSelectFrom.setAllowCopy(false);
		modelAppUserSelectFrom.setAllowPaste(false);
		modelAppUserSelectFrom.setAllowMultiSelect(false);
		return modelAppUserSelectFrom;
	}

	public AppServerSearchModel getAppServersSearchModel() {
		if (modelAppServersSearch != null) {
			return modelAppServersSearch;
		}
		modelAppServersSearch = new AppServerSearchModel(getAppServers()); // createMethod=false, directly uses hub
		return modelAppServersSearch;
	}

	public HubCopy<AppUserLogin> createHubCopy() {
		Hub<AppUserLogin> hubAppUserLoginx = new Hub<>(AppUserLogin.class);
		HubCopy<AppUserLogin> hc = new HubCopy<>(getHub(), hubAppUserLoginx, true);
		return hc;
	}

	public AppUserLoginModel createCopy() {
		AppUserLoginModel mod = new AppUserLoginModel(createHubCopy().getHub());
		return mod;
	}
}
