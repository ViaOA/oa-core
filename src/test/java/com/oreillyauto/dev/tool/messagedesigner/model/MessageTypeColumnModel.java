// Generated by OABuilder

package com.oreillyauto.dev.tool.messagedesigner.model;

import java.util.logging.*;
import com.viaoa.object.*;
import com.viaoa.annotation.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.filter.*;
import com.viaoa.datasource.*;

import com.oreillyauto.dev.tool.messagedesigner.model.oa.*;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.propertypath.*;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.search.*;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.filter.*;
import com.oreillyauto.dev.tool.messagedesigner.model.search.*;
import com.oreillyauto.dev.tool.messagedesigner.model.filter.*;
import com.oreillyauto.dev.tool.messagedesigner.delegate.ModelDelegate;
import com.oreillyauto.dev.tool.messagedesigner.resource.Resource;

public class MessageTypeColumnModel extends OAObjectModel {
    private static Logger LOG = Logger.getLogger(MessageTypeColumnModel.class.getName());
    
    /* overview
      originalHub   - save the original hub
      <- unfilteredHub - points one of the above hubs
      invalidRpgTypeFilteredHub;
      <- hub - points to unfiltered or filtered hub
    */
    
    // Hubs
    protected Hub<MessageTypeColumn> hubOriginal;
    
    // base hub that points to one of: hubOriginal
    protected Hub<MessageTypeColumn> hubUnfiltered;
    protected Hub<MessageTypeColumn> hubInvalidRpgTypeFilteredHub;
    // main hub that points to hubUnfiltered, hubInvalidRpgTypeFilteredHub
    protected Hub<MessageTypeColumn> hub;
    // selected messageTypeColumns
    protected Hub<MessageTypeColumn> hubMultiSelect;
    // detail hubs
    protected Hub<MessageTypeRecord> hubMessageTypeRecord;
    protected Hub<RpgType> hubRpgType;
    
    // AddHubs used for references
    protected Hub<RpgType> hubRpgTypeSelectFrom;
    
    // ObjectModels
    protected MessageTypeRecordModel modelMessageTypeRecord;
    protected RpgTypeModel modelRpgType;
    
    // selectFrom
    protected RpgTypeModel modelRpgTypeSelectFrom;
    
    // SearchModels used for references
    protected MessageTypeRecordSearchModel modelMessageTypeRecordSearch;
    protected RpgTypeSearchModel modelRpgTypeSearch;
    
    // FilterModels
    protected MessageTypeColumnInvalidRpgTypeFilterModel modelMessageTypeColumnInvalidRpgTypeFilter;
    
    public MessageTypeColumnModel() {
        setDisplayName("Message Type Column");
        setPluralDisplayName("Message Type Columns");
    }
    
    public MessageTypeColumnModel(Hub<MessageTypeColumn> hubMessageTypeColumn) {
        this();
        if (hubMessageTypeColumn != null) HubDelegate.setObjectClass(hubMessageTypeColumn, MessageTypeColumn.class);
        this.hubOriginal = hubMessageTypeColumn;
    }
    public MessageTypeColumnModel(MessageTypeColumn messageTypeColumn) {
        this();
        getHub().add(messageTypeColumn);
        getHub().setPos(0);
    }
    
    public void useUnfilteredHub() {
        getHub().setSharedHub(getUnfilteredHub(), true);
    }
    public void useInvalidRpgTypeFilteredHub() {
        getHub().setSharedHub(getInvalidRpgTypeFilteredHub(), true);
    }
    
    public Hub<MessageTypeColumn> getOriginalHub() {
        if (hubOriginal == null) {
            hubOriginal = new Hub<MessageTypeColumn>(MessageTypeColumn.class);
        }
        return hubOriginal;
    }
    
    public Hub<MessageTypeRecord> getMessageTypeRecordHub() {
        if (hubMessageTypeRecord != null) return hubMessageTypeRecord;
        // this is the owner, use detailHub
        hubMessageTypeRecord = getHub().getDetailHub(MessageTypeColumn.P_MessageTypeRecord);
        return hubMessageTypeRecord;
    }
    public Hub<RpgType> getRpgTypeHub() {
        if (hubRpgType != null) return hubRpgType;
        hubRpgType = getHub().getDetailHub(MessageTypeColumn.P_RpgType);
        return hubRpgType;
    }
    public Hub<RpgType> getRpgTypeSelectFromHub() {
        if (hubRpgTypeSelectFrom != null) return hubRpgTypeSelectFrom;
        hubRpgTypeSelectFrom = new Hub<RpgType>(RpgType.class);
        Hub<RpgType> hubRpgTypeSelectFrom1 = ModelDelegate.getRpgTypes().createSharedHub();
        HubCombined<RpgType> hubCombined = new HubCombined(hubRpgTypeSelectFrom, hubRpgTypeSelectFrom1, getRpgTypeHub());
        hubRpgTypeSelectFrom.setLinkHub(getHub(), MessageTypeColumn.P_RpgType); 
        return hubRpgTypeSelectFrom;
    }
    public Hub<MessageTypeColumn> getUnfilteredHub() {
        if (hubUnfiltered == null) {
            hubUnfiltered = new Hub<MessageTypeColumn>(MessageTypeColumn.class);
            hubUnfiltered.setSharedHub(getOriginalHub(), true);
        }
        return hubUnfiltered;
    }
    public Hub<MessageTypeColumn> getInvalidRpgTypeFilteredHub() {
        if (hubInvalidRpgTypeFilteredHub == null) {
            hubInvalidRpgTypeFilteredHub = new Hub<MessageTypeColumn>(MessageTypeColumn.class);
        }
        return hubInvalidRpgTypeFilteredHub;
    }
    
    public MessageTypeColumn getMessageTypeColumn() {
        return getHub().getAO();
    }
    
    // points to filtered or unfiltered hub
    public Hub<MessageTypeColumn> getHub() {
        if (hub == null) {
            hub = new Hub<MessageTypeColumn>(MessageTypeColumn.class);
            hub.setSharedHub(getUnfilteredHub(), true);
        }
        return hub;
    }
    
    public Hub<MessageTypeColumn> getMultiSelectHub() {
        if (hubMultiSelect == null) {
            hubMultiSelect = new Hub<MessageTypeColumn>(MessageTypeColumn.class);
        }
        return hubMultiSelect;
    }
    
    public MessageTypeRecordModel getMessageTypeRecordModel() {
        if (modelMessageTypeRecord != null) return modelMessageTypeRecord;
        modelMessageTypeRecord = new MessageTypeRecordModel(getMessageTypeRecordHub());
        modelMessageTypeRecord.setDisplayName("Message Type Record");
        modelMessageTypeRecord.setPluralDisplayName("Message Type Records");
        modelMessageTypeRecord.setForJfc(getForJfc());
        modelMessageTypeRecord.setAllowNew(false);
        modelMessageTypeRecord.setAllowSave(true);
        modelMessageTypeRecord.setAllowAdd(false);
        modelMessageTypeRecord.setAllowRemove(false);
        modelMessageTypeRecord.setAllowClear(false);
        modelMessageTypeRecord.setAllowDelete(false);
        modelMessageTypeRecord.setAllowSearch(true);
        modelMessageTypeRecord.setAllowHubSearch(true);
        modelMessageTypeRecord.setAllowGotoEdit(true);
        OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
        modelMessageTypeRecord.setCreateUI(li == null || !MessageTypeColumn.P_MessageTypeRecord.equals(li.getName()) );
        modelMessageTypeRecord.setViewOnly(getViewOnly());
        // call MessageTypeColumn.messageTypeRecordModelCallback(MessageTypeRecordModel) to be able to customize this model
        OAObjectCallbackDelegate.onObjectCallbackModel(MessageTypeColumn.class, MessageTypeColumn.P_MessageTypeRecord, modelMessageTypeRecord);
    
        return modelMessageTypeRecord;
    }
    public RpgTypeModel getRpgTypeModel() {
        if (modelRpgType != null) return modelRpgType;
        modelRpgType = new RpgTypeModel(getRpgTypeHub());
        modelRpgType.setDisplayName("Rpg Type");
        modelRpgType.setPluralDisplayName("Rpg Types");
        modelRpgType.setForJfc(getForJfc());
        modelRpgType.setAllowNew(false);
        modelRpgType.setAllowSave(true);
        modelRpgType.setAllowAdd(false);
        modelRpgType.setAllowRemove(false);
        modelRpgType.setAllowClear(false);
        modelRpgType.setAllowDelete(false);
        modelRpgType.setAllowSearch(true);
        modelRpgType.setAllowHubSearch(true);
        modelRpgType.setAllowGotoEdit(true);
        modelRpgType.setViewOnly(true);
        // call MessageTypeColumn.rpgTypeModelCallback(RpgTypeModel) to be able to customize this model
        OAObjectCallbackDelegate.onObjectCallbackModel(MessageTypeColumn.class, MessageTypeColumn.P_RpgType, modelRpgType);
    
        return modelRpgType;
    }
    
    public RpgTypeModel getRpgTypeSelectFromModel() {
        if (modelRpgTypeSelectFrom != null) return modelRpgTypeSelectFrom;
        modelRpgTypeSelectFrom = new RpgTypeModel(getRpgTypeSelectFromHub());
        modelRpgTypeSelectFrom.setDisplayName("Rpg Type");
        modelRpgTypeSelectFrom.setPluralDisplayName("Rpg Types");
        modelRpgTypeSelectFrom.setForJfc(getForJfc());
        modelRpgTypeSelectFrom.setAllowNew(false);
        modelRpgTypeSelectFrom.setAllowSave(true);
        modelRpgTypeSelectFrom.setAllowAdd(false);
        modelRpgTypeSelectFrom.setAllowMove(false);
        modelRpgTypeSelectFrom.setAllowRemove(false);
        modelRpgTypeSelectFrom.setAllowDelete(false);
        modelRpgTypeSelectFrom.setAllowSearch(true);
        modelRpgTypeSelectFrom.setAllowHubSearch(true);
        modelRpgTypeSelectFrom.setAllowGotoEdit(true);
        modelRpgTypeSelectFrom.setViewOnly(getViewOnly());
        modelRpgTypeSelectFrom.setAllowNew(false);
        modelRpgTypeSelectFrom.setAllowTableFilter(true);
        modelRpgTypeSelectFrom.setAllowTableSorting(true);
        modelRpgTypeSelectFrom.setAllowCut(false);
        modelRpgTypeSelectFrom.setAllowCopy(false);
        modelRpgTypeSelectFrom.setAllowPaste(false);
        modelRpgTypeSelectFrom.setAllowMultiSelect(false);
        return modelRpgTypeSelectFrom;
    }
    public MessageTypeRecordSearchModel getMessageTypeRecordSearchModel() {
        if (modelMessageTypeRecordSearch != null) return modelMessageTypeRecordSearch;
        modelMessageTypeRecordSearch = new MessageTypeRecordSearchModel();
        HubSelectDelegate.adoptWhereHub(modelMessageTypeRecordSearch.getHub(), MessageTypeColumn.P_MessageTypeRecord, getHub());
        return modelMessageTypeRecordSearch;
    }
    public RpgTypeSearchModel getRpgTypeSearchModel() {
        if (modelRpgTypeSearch != null) return modelRpgTypeSearch;
        modelRpgTypeSearch = new RpgTypeSearchModel();
        HubSelectDelegate.adoptWhereHub(modelRpgTypeSearch.getHub(), MessageTypeColumn.P_RpgType, getHub());
        return modelRpgTypeSearch;
    }
    
    public MessageTypeColumnInvalidRpgTypeFilterModel getMessageTypeColumnInvalidRpgTypeFilterModel() {
        if (modelMessageTypeColumnInvalidRpgTypeFilter == null) {
            modelMessageTypeColumnInvalidRpgTypeFilter = new MessageTypeColumnInvalidRpgTypeFilterModel(getUnfilteredHub(), getInvalidRpgTypeFilteredHub());
            new HubShareAO(getUnfilteredHub(), getInvalidRpgTypeFilteredHub());
        }
        return modelMessageTypeColumnInvalidRpgTypeFilter;
    }
    
    public HubCopy<MessageTypeColumn> createHubCopy() {
        Hub<MessageTypeColumn> hubMessageTypeColumnx = new Hub<>(MessageTypeColumn.class);
        HubCopy<MessageTypeColumn> hc = new HubCopy<>(getHub(), hubMessageTypeColumnx, true);
        return hc;
    }
    public MessageTypeColumnModel createCopy() {
        MessageTypeColumnModel mod = new MessageTypeColumnModel(createHubCopy().getHub());
        return mod;
    }
}
