// Generated by OABuilder

package com.corptostore.model;

import java.util.logging.*;
import com.viaoa.object.*;
import com.corptostore.delegate.ModelDelegate;
import com.corptostore.model.filter.*;
import com.corptostore.model.oa.*;
import com.corptostore.model.oa.filter.*;
import com.corptostore.model.oa.propertypath.*;
import com.corptostore.model.oa.search.*;
import com.corptostore.model.search.*;
import com.corptostore.resource.Resource;
import com.corptostore.model.StatusInfoModel;
import com.corptostore.model.StoreInfoModel;
import com.corptostore.model.StoreLockInfoModel;
import com.corptostore.model.StoreLockServiceInfoModel;
import com.corptostore.model.ThreadInfoModel;
import com.corptostore.model.oa.StatusInfo;
import com.corptostore.model.oa.StoreInfo;
import com.corptostore.model.oa.StoreLockInfo;
import com.corptostore.model.oa.StoreLockServiceInfo;
import com.corptostore.model.oa.ThreadInfo;
import com.viaoa.annotation.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.filter.*;
import com.viaoa.datasource.*;

public class StoreLockInfoModel extends OAObjectModel {
    private static Logger LOG = Logger.getLogger(StoreLockInfoModel.class.getName());
    
    // Hubs
    protected Hub<StoreLockInfo> hub;
    // selected storeLockInfos
    protected Hub<StoreLockInfo> hubMultiSelect;
    // detail hubs
    protected Hub<StatusInfo> hubStatusInfo;
    protected Hub<StoreInfo> hubStoreInfo;
    protected Hub<StoreLockServiceInfo> hubStoreLockServiceInfo;
    protected Hub<ThreadInfo> hubThreadInfo;
    
    // ObjectModels
    protected StatusInfoModel modelStatusInfo;
    protected StoreInfoModel modelStoreInfo;
    protected StoreLockServiceInfoModel modelStoreLockServiceInfo;
    protected ThreadInfoModel modelThreadInfo;
    
    public StoreLockInfoModel() {
        setDisplayName("Store Lock Info");
        setPluralDisplayName("Store Lock Infos");
    }
    
    public StoreLockInfoModel(Hub<StoreLockInfo> hubStoreLockInfo) {
        this();
        if (hubStoreLockInfo != null) HubDelegate.setObjectClass(hubStoreLockInfo, StoreLockInfo.class);
        this.hub = hubStoreLockInfo;
    }
    public StoreLockInfoModel(StoreLockInfo storeLockInfo) {
        this();
        getHub().add(storeLockInfo);
        getHub().setPos(0);
    }
    
    public Hub<StoreLockInfo> getOriginalHub() {
        return getHub();
    }
    
    public Hub<StatusInfo> getStatusInfoHub() {
        if (hubStatusInfo != null) return hubStatusInfo;
        hubStatusInfo = getHub().getDetailHub(StoreLockInfo.P_StatusInfo);
        return hubStatusInfo;
    }
    public Hub<StoreInfo> getStoreInfoHub() {
        if (hubStoreInfo != null) return hubStoreInfo;
        hubStoreInfo = getHub().getDetailHub(StoreLockInfo.P_StoreInfo);
        return hubStoreInfo;
    }
    public Hub<StoreLockServiceInfo> getStoreLockServiceInfoHub() {
        if (hubStoreLockServiceInfo != null) return hubStoreLockServiceInfo;
        // this is the owner, use detailHub
        hubStoreLockServiceInfo = getHub().getDetailHub(StoreLockInfo.P_StoreLockServiceInfo);
        return hubStoreLockServiceInfo;
    }
    public Hub<ThreadInfo> getThreadInfoHub() {
        if (hubThreadInfo != null) return hubThreadInfo;
        hubThreadInfo = getHub().getDetailHub(StoreLockInfo.P_ThreadInfo);
        return hubThreadInfo;
    }
    public StoreLockInfo getStoreLockInfo() {
        return getHub().getAO();
    }
    
    public Hub<StoreLockInfo> getHub() {
        if (hub == null) {
            hub = new Hub<StoreLockInfo>(StoreLockInfo.class);
        }
        return hub;
    }
    
    public Hub<StoreLockInfo> getMultiSelectHub() {
        if (hubMultiSelect == null) {
            hubMultiSelect = new Hub<StoreLockInfo>(StoreLockInfo.class);
        }
        return hubMultiSelect;
    }
    
    public StatusInfoModel getStatusInfoModel() {
        if (modelStatusInfo != null) return modelStatusInfo;
        modelStatusInfo = new StatusInfoModel(getStatusInfoHub());
        modelStatusInfo.setDisplayName("Status Info");
        modelStatusInfo.setPluralDisplayName("Status Infos");
        modelStatusInfo.setForJfc(getForJfc());
        modelStatusInfo.setAllowNew(false);
        modelStatusInfo.setAllowSave(true);
        modelStatusInfo.setAllowAdd(false);
        modelStatusInfo.setAllowRemove(false);
        modelStatusInfo.setAllowClear(false);
        modelStatusInfo.setAllowDelete(false);
        modelStatusInfo.setAllowSearch(false);
        modelStatusInfo.setAllowHubSearch(false);
        modelStatusInfo.setAllowGotoEdit(true);
        modelStatusInfo.setViewOnly(getViewOnly());
        // call StoreLockInfo.statusInfoModelCallback(StatusInfoModel) to be able to customize this model
        OAObjectCallbackDelegate.onObjectCallbackModel(StoreLockInfo.class, StoreLockInfo.P_StatusInfo, modelStatusInfo);
    
        return modelStatusInfo;
    }
    public StoreInfoModel getStoreInfoModel() {
        if (modelStoreInfo != null) return modelStoreInfo;
        modelStoreInfo = new StoreInfoModel(getStoreInfoHub());
        modelStoreInfo.setDisplayName("Store Info");
        modelStoreInfo.setPluralDisplayName("Store Infos");
        modelStoreInfo.setForJfc(getForJfc());
        modelStoreInfo.setAllowNew(false);
        modelStoreInfo.setAllowSave(true);
        modelStoreInfo.setAllowAdd(false);
        modelStoreInfo.setAllowRemove(false);
        modelStoreInfo.setAllowClear(false);
        modelStoreInfo.setAllowDelete(false);
        modelStoreInfo.setAllowSearch(false);
        modelStoreInfo.setAllowHubSearch(false);
        modelStoreInfo.setAllowGotoEdit(true);
        modelStoreInfo.setViewOnly(true);
        // call StoreLockInfo.storeInfoModelCallback(StoreInfoModel) to be able to customize this model
        OAObjectCallbackDelegate.onObjectCallbackModel(StoreLockInfo.class, StoreLockInfo.P_StoreInfo, modelStoreInfo);
    
        return modelStoreInfo;
    }
    public StoreLockServiceInfoModel getStoreLockServiceInfoModel() {
        if (modelStoreLockServiceInfo != null) return modelStoreLockServiceInfo;
        modelStoreLockServiceInfo = new StoreLockServiceInfoModel(getStoreLockServiceInfoHub());
        modelStoreLockServiceInfo.setDisplayName("Store Lock Service Info");
        modelStoreLockServiceInfo.setPluralDisplayName("Store Lock Service Infos");
        modelStoreLockServiceInfo.setForJfc(getForJfc());
        modelStoreLockServiceInfo.setAllowNew(false);
        modelStoreLockServiceInfo.setAllowSave(true);
        modelStoreLockServiceInfo.setAllowAdd(false);
        modelStoreLockServiceInfo.setAllowRemove(false);
        modelStoreLockServiceInfo.setAllowClear(false);
        modelStoreLockServiceInfo.setAllowDelete(false);
        modelStoreLockServiceInfo.setAllowSearch(false);
        modelStoreLockServiceInfo.setAllowHubSearch(false);
        modelStoreLockServiceInfo.setAllowGotoEdit(true);
        OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
        modelStoreLockServiceInfo.setCreateUI(li == null || !StoreLockInfo.P_StoreLockServiceInfo.equalsIgnoreCase(li.getName()) );
        modelStoreLockServiceInfo.setViewOnly(getViewOnly());
        // call StoreLockInfo.storeLockServiceInfoModelCallback(StoreLockServiceInfoModel) to be able to customize this model
        OAObjectCallbackDelegate.onObjectCallbackModel(StoreLockInfo.class, StoreLockInfo.P_StoreLockServiceInfo, modelStoreLockServiceInfo);
    
        return modelStoreLockServiceInfo;
    }
    public ThreadInfoModel getThreadInfoModel() {
        if (modelThreadInfo != null) return modelThreadInfo;
        modelThreadInfo = new ThreadInfoModel(getThreadInfoHub());
        modelThreadInfo.setDisplayName("Thread Info");
        modelThreadInfo.setPluralDisplayName("Thread Infos");
        modelThreadInfo.setForJfc(getForJfc());
        modelThreadInfo.setAllowNew(false);
        modelThreadInfo.setAllowSave(true);
        modelThreadInfo.setAllowAdd(false);
        modelThreadInfo.setAllowRemove(false);
        modelThreadInfo.setAllowClear(false);
        modelThreadInfo.setAllowDelete(false);
        modelThreadInfo.setAllowSearch(false);
        modelThreadInfo.setAllowHubSearch(false);
        modelThreadInfo.setAllowGotoEdit(true);
        modelThreadInfo.setViewOnly(true);
        // call StoreLockInfo.threadInfoModelCallback(ThreadInfoModel) to be able to customize this model
        OAObjectCallbackDelegate.onObjectCallbackModel(StoreLockInfo.class, StoreLockInfo.P_ThreadInfo, modelThreadInfo);
    
        return modelThreadInfo;
    }
    
    public HubCopy<StoreLockInfo> createHubCopy() {
        Hub<StoreLockInfo> hubStoreLockInfox = new Hub<>(StoreLockInfo.class);
        HubCopy<StoreLockInfo> hc = new HubCopy<>(getHub(), hubStoreLockInfox, true);
        return hc;
    }
    public StoreLockInfoModel createCopy() {
        StoreLockInfoModel mod = new StoreLockInfoModel(createHubCopy().getHub());
        return mod;
    }
}

