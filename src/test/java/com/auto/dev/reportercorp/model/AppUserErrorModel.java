package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.AppUserError;
import com.auto.dev.reportercorp.model.oa.AppUserLogin;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class AppUserErrorModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(AppUserErrorModel.class.getName());

	// Hubs
	protected Hub<AppUserError> hub;
	// selected appUserErrors
	protected Hub<AppUserError> hubMultiSelect;
	// detail hubs
	protected Hub<AppUserLogin> hubAppUserLogin;

	// ObjectModels
	protected AppUserLoginModel modelAppUserLogin;

	public AppUserErrorModel() {
		setDisplayName("App User Error");
		setPluralDisplayName("App User Errors");
	}

	public AppUserErrorModel(Hub<AppUserError> hubAppUserError) {
		this();
		if (hubAppUserError != null) {
			HubDelegate.setObjectClass(hubAppUserError, AppUserError.class);
		}
		this.hub = hubAppUserError;
	}

	public AppUserErrorModel(AppUserError appUserError) {
		this();
		getHub().add(appUserError);
		getHub().setPos(0);
	}

	public Hub<AppUserError> getOriginalHub() {
		return getHub();
	}

	public Hub<AppUserLogin> getAppUserLoginHub() {
		if (hubAppUserLogin != null) {
			return hubAppUserLogin;
		}
		// this is the owner, use detailHub
		hubAppUserLogin = getHub().getDetailHub(AppUserError.P_AppUserLogin);
		return hubAppUserLogin;
	}

	public AppUserError getAppUserError() {
		return getHub().getAO();
	}

	public Hub<AppUserError> getHub() {
		if (hub == null) {
			hub = new Hub<AppUserError>(AppUserError.class);
		}
		return hub;
	}

	public Hub<AppUserError> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<AppUserError>(AppUserError.class);
		}
		return hubMultiSelect;
	}

	public AppUserLoginModel getAppUserLoginModel() {
		if (modelAppUserLogin != null) {
			return modelAppUserLogin;
		}
		modelAppUserLogin = new AppUserLoginModel(getAppUserLoginHub());
		modelAppUserLogin.setDisplayName("App User Login");
		modelAppUserLogin.setPluralDisplayName("App User Logins");
		modelAppUserLogin.setForJfc(getForJfc());
		modelAppUserLogin.setAllowNew(false);
		modelAppUserLogin.setAllowSave(true);
		modelAppUserLogin.setAllowAdd(false);
		modelAppUserLogin.setAllowRemove(false);
		modelAppUserLogin.setAllowClear(false);
		modelAppUserLogin.setAllowDelete(false);
		modelAppUserLogin.setAllowSearch(false);
		modelAppUserLogin.setAllowHubSearch(false);
		modelAppUserLogin.setAllowGotoEdit(true);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelAppUserLogin.setCreateUI(li == null || !AppUserError.P_AppUserLogin.equalsIgnoreCase(li.getName()));
		modelAppUserLogin.setViewOnly(getViewOnly());
		// call AppUserError.appUserLoginModelCallback(AppUserLoginModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(AppUserError.class, AppUserError.P_AppUserLogin, modelAppUserLogin);

		return modelAppUserLogin;
	}

	public HubCopy<AppUserError> createHubCopy() {
		Hub<AppUserError> hubAppUserErrorx = new Hub<>(AppUserError.class);
		HubCopy<AppUserError> hc = new HubCopy<>(getHub(), hubAppUserErrorx, true);
		return hc;
	}

	public AppUserErrorModel createCopy() {
		AppUserErrorModel mod = new AppUserErrorModel(createHubCopy().getHub());
		return mod;
	}
}
