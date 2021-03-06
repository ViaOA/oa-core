// Generated by OABuilder

package com.cdi.model;

import java.util.logging.*;
import com.viaoa.object.*;
import com.viaoa.annotation.*;
import com.viaoa.datasource.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.util.filter.*;
import com.cdi.model.oa.*;
import com.cdi.model.oa.propertypath.*;
import com.cdi.model.oa.search.*;
import com.cdi.model.oa.filter.*;
import com.cdi.model.search.*;
import com.cdi.model.filter.*;
import com.cdi.delegate.ModelDelegate;
import com.cdi.resource.Resource;

public class AppUserModel extends OAObjectModel {
    private static Logger LOG = Logger.getLogger(AppUserModel.class.getName());
    
    // Hubs
    protected Hub<AppUser> hub;
    // selected appUsers
    protected Hub<AppUser> hubMultiSelect;
    // detail hubs
    protected Hub<User> hubUser;
    protected Hub<AppUserLogin> hubAppUserLogins;
    
    // ObjectModels
    protected UserModel modelUser;
    protected AppUserLoginModel modelAppUserLogins;
    
    public AppUserModel() {
        setDisplayName("App User");
        setPluralDisplayName("App Users");
    }
    
    public AppUserModel(Hub<AppUser> hubAppUser) {
        this();
        if (hubAppUser != null) HubDelegate.setObjectClass(hubAppUser, AppUser.class);
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
    
    public Hub<User> getUserHub() {
        if (hubUser != null) return hubUser;
        hubUser = getHub().getDetailHub(AppUser.P_User);
        return hubUser;
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
    
    public UserModel getUserModel() {
        if (modelUser != null) return modelUser;
        modelUser = new UserModel(getUserHub());
        modelUser.setDisplayName("User");
        modelUser.setPluralDisplayName("Users");
        modelUser.setForJfc(getForJfc());
        modelUser.setAllowNew(false);
        modelUser.setAllowSave(true);
        modelUser.setAllowAdd(false);
        modelUser.setAllowRemove(false);
        modelUser.setAllowClear(false);
        modelUser.setAllowDelete(false);
        modelUser.setAllowSearch(false);
        modelUser.setAllowHubSearch(true);
        modelUser.setAllowGotoEdit(false);
        OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
        modelUser.setCreateUI(li == null || !AppUser.P_User.equals(li.getName()) );
        modelUser.setViewOnly(true);
        // call AppUser.onEditQueryUser(UserModel) to be able to customize this model
        OAObjectCallbackDelegate.onObjectCallbackModel(AppUser.class, AppUser.P_User, modelUser);
    
        return modelUser;
    }
    public AppUserLoginModel getAppUserLoginsModel() {
        if (modelAppUserLogins != null) return modelAppUserLogins;
        modelAppUserLogins = new AppUserLoginModel(getAppUserLogins());
        modelAppUserLogins.setDisplayName("App User Login");
        modelAppUserLogins.setPluralDisplayName("App User Logins");
        if (HubDetailDelegate.getLinkInfoFromMasterToDetail(getOriginalHub().getMasterHub()) == HubDetailDelegate.getLinkInfoFromMasterToDetail(getAppUserLogins())) {
            modelAppUserLogins.setCreateUI(false);
        }
        modelAppUserLogins.setForJfc(getForJfc());
        modelAppUserLogins.setAllowNew(true);
        modelAppUserLogins.setAllowSave(true);
        modelAppUserLogins.setAllowAdd(false);
        modelAppUserLogins.setAllowMove(false);
        modelAppUserLogins.setAllowRemove(false);
        modelAppUserLogins.setAllowDelete(true);
        modelAppUserLogins.setAllowSearch(false);
        modelAppUserLogins.setAllowHubSearch(true);
        modelAppUserLogins.setAllowGotoEdit(true);
        modelAppUserLogins.setViewOnly(getViewOnly());
        modelAppUserLogins.setAllowTableFilter(true);
        modelAppUserLogins.setAllowTableSorting(true);
         // default is always false for these, can be turned by custom code in editQuery call (below)
        modelAppUserLogins.setAllowMultiSelect(false);
        modelAppUserLogins.setAllowCopy(false);
        modelAppUserLogins.setAllowCut(false);
        modelAppUserLogins.setAllowPaste(false);
        // call AppUser.onEditQueryAppUserLogins(AppUserLoginModel) to be able to customize this model
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

