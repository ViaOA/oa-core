package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.AppUser;
import com.auto.dev.reportercorp.model.oa.AppUserLogin;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class AppUserModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(AppUserModel.class.getName());

	// Hubs
	protected Hub<AppUser> hub;
	// selected appUsers
	protected Hub<AppUser> hubMultiSelect;
	// detail hubs
	protected Hub<AppUserLogin> hubAppUserLogins;

	// ObjectModels
	protected AppUserLoginModel modelAppUserLogins;

	public AppUserModel() {
		setDisplayName("App User");
		setPluralDisplayName("App Users");
	}

	public AppUserModel(Hub<AppUser> hubAppUser) {
		this();
		if (hubAppUser != null) {
			HubDelegate.setObjectClass(hubAppUser, AppUser.class);
		}
		this.hub = hubAppUser;
	}

	public AppUserModel(AppUser appUser) {
		this();
		getHub().add(appUser);
		getHub().setPos(0);
	}

	public Hub<AppUser> getOriginalHub() {
		return getHub();
	}

	public Hub<AppUserLogin> getAppUserLogins() {
		if (hubAppUserLogins == null) {
			hubAppUserLogins = getHub().getDetailHub(AppUser.P_AppUserLogins);
		}
		return hubAppUserLogins;
	}

	public AppUser getAppUser() {
		return getHub().getAO();
	}

	public Hub<AppUser> getHub() {
		if (hub == null) {
			hub = new Hub<AppUser>(AppUser.class);
		}
		return hub;
	}

	public Hub<AppUser> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<AppUser>(AppUser.class);
		}
		return hubMultiSelect;
	}

	public AppUserLoginModel getAppUserLoginsModel() {
		if (modelAppUserLogins != null) {
			return modelAppUserLogins;
		}
		modelAppUserLogins = new AppUserLoginModel(getAppUserLogins());
		modelAppUserLogins.setDisplayName("App User Login");
		modelAppUserLogins.setPluralDisplayName("App User Logins");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getAppUserLogins())) {
			modelAppUserLogins.setCreateUI(false);
		}
		modelAppUserLogins.setForJfc(getForJfc());
		modelAppUserLogins.setAllowNew(true);
		modelAppUserLogins.setAllowSave(true);
		modelAppUserLogins.setAllowAdd(false);
		modelAppUserLogins.setAllowMove(false);
		modelAppUserLogins.setAllowRemove(false);
		modelAppUserLogins.setAllowDelete(true);
		modelAppUserLogins.setAllowRefresh(false);
		modelAppUserLogins.setAllowSearch(false);
		modelAppUserLogins.setAllowHubSearch(false);
		modelAppUserLogins.setAllowGotoEdit(true);
		modelAppUserLogins.setViewOnly(getViewOnly());
		modelAppUserLogins.setAllowTableFilter(true);
		modelAppUserLogins.setAllowTableSorting(true);
		modelAppUserLogins.setAllowMultiSelect(false);
		modelAppUserLogins.setAllowCopy(false);
		modelAppUserLogins.setAllowCut(false);
		modelAppUserLogins.setAllowPaste(false);
		// call AppUser.appUserLoginsModelCallback(AppUserLoginModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(AppUser.class, AppUser.P_AppUserLogins, modelAppUserLogins);

		return modelAppUserLogins;
	}

	public HubCopy<AppUser> createHubCopy() {
		Hub<AppUser> hubAppUserx = new Hub<>(AppUser.class);
		HubCopy<AppUser> hc = new HubCopy<>(getHub(), hubAppUserx, true);
		return hc;
	}

	public AppUserModel createCopy() {
		AppUserModel mod = new AppUserModel(createHubCopy().getHub());
		return mod;
	}
}
