// Generated by OABuilder
package com.corptostore.model.oa;

import java.util.logging.Logger;

import com.corptostore.model.oa.CorpToStore;
import com.corptostore.model.oa.StatusInfo;
import com.corptostore.model.oa.StoreLockInfo;
import com.corptostore.model.oa.StoreLockServiceInfo;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAMethod;
import com.viaoa.annotation.OAObjCallback;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.util.OADateTime;

@OAClass(lowerName = "storeLockServiceInfo", pluralName = "StoreLockServiceInfos", shortName = "sls", displayName = "Store Lock Service Info", useDataSource = false, isProcessed = true, displayProperty = "id")
public class StoreLockServiceInfo extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(StoreLockServiceInfo.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_StoreLockMinutes = "storeLockMinutes";
	public static final String P_MaxStoreSelectSize = "maxStoreSelectSize";
	public static final String P_MinimumStoreListLapMinutes = "minimumStoreListLapMinutes";
	public static final String P_Paused = "paused";
	public static final String P_LastCallToExtendStoreLocks = "lastCallToExtendStoreLocks";
	public static final String P_StoreListLapCount = "storeListLapCount";
	public static final String P_BeginStoreListLap = "beginStoreListLap";
	public static final String P_CheckExtendStoreLocksLastError = "checkExtendStoreLocksLastError";
	public static final String P_ThrottleWaitingSeconds = "throttleWaitingSeconds";
	public static final String P_LastUsedStoreId = "lastUsedStoreId";
	public static final String P_CheckExtendStoreLocksLastErrorCounter = "checkExtendStoreLocksLastErrorCounter";

	public static final String P_CorpToStore = "corpToStore";
	public static final String P_StatusInfo = "statusInfo";
	public static final String P_StoreLockInfos = "storeLockInfos";

	public static final String M_PauseService = "pauseService";
	public static final String M_ContinueService = "continueService";
	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile int storeLockMinutes;
	protected volatile int maxStoreSelectSize;
	protected volatile int minimumStoreListLapMinutes;
	protected volatile OADateTime paused;
	protected volatile OADateTime lastCallToExtendStoreLocks;
	protected volatile int storeListLapCount;
	protected volatile OADateTime beginStoreListLap;
	protected volatile OADateTime checkExtendStoreLocksLastError;
	protected volatile int throttleWaitingSeconds;
	protected volatile int lastUsedStoreId;
	protected volatile int checkExtendStoreLocksLastErrorCounter;

	// Links to other objects.
	protected volatile transient CorpToStore corpToStore;
	protected volatile transient StatusInfo statusInfo;
	protected transient Hub<StoreLockInfo> hubStoreLockInfos;

	public StoreLockServiceInfo() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
		getStatusInfo(); // have it autoCreated
	}

	public StoreLockServiceInfo(int id) {
		this();
		setId(id);
	}

	@OAProperty(isUnique = true, trackPrimitiveNull = false, displayLength = 6)
	@OAId
	@OAColumn(sqlType = java.sql.Types.INTEGER)
	public int getId() {
		return id;
	}

	public void setId(int newValue) {
		int old = id;
		fireBeforePropertyChange(P_Id, old, newValue);
		this.id = newValue;
		firePropertyChange(P_Id, old, this.id);
	}

	@OAProperty(defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true)
	@OAColumn(sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getCreated() {
		return created;
	}

	public void setCreated(OADateTime newValue) {
		OADateTime old = created;
		fireBeforePropertyChange(P_Created, old, newValue);
		this.created = newValue;
		firePropertyChange(P_Created, old, this.created);
	}

	@OAProperty(displayName = "Store Lock Minutes", displayLength = 6, columnLength = 18)
	@OAColumn(name = "store_lock_minutes", sqlType = java.sql.Types.INTEGER)
	public int getStoreLockMinutes() {
		return storeLockMinutes;
	}

	public void setStoreLockMinutes(int newValue) {
		int old = storeLockMinutes;
		fireBeforePropertyChange(P_StoreLockMinutes, old, newValue);
		this.storeLockMinutes = newValue;
		firePropertyChange(P_StoreLockMinutes, old, this.storeLockMinutes);
	}

	@OAProperty(displayName = "Max Store Select Size", displayLength = 6, columnLength = 21)
	@OAColumn(name = "max_store_select_size", sqlType = java.sql.Types.INTEGER)
	public int getMaxStoreSelectSize() {
		return maxStoreSelectSize;
	}

	public void setMaxStoreSelectSize(int newValue) {
		int old = maxStoreSelectSize;
		fireBeforePropertyChange(P_MaxStoreSelectSize, old, newValue);
		this.maxStoreSelectSize = newValue;
		firePropertyChange(P_MaxStoreSelectSize, old, this.maxStoreSelectSize);
	}

	@OAProperty(displayName = "Minimum Store List Lap Minutes", displayLength = 6, columnLength = 30)
	@OAColumn(name = "minimum_store_list_lap_minutes", sqlType = java.sql.Types.INTEGER)
	public int getMinimumStoreListLapMinutes() {
		return minimumStoreListLapMinutes;
	}

	public void setMinimumStoreListLapMinutes(int newValue) {
		int old = minimumStoreListLapMinutes;
		fireBeforePropertyChange(P_MinimumStoreListLapMinutes, old, newValue);
		this.minimumStoreListLapMinutes = newValue;
		firePropertyChange(P_MinimumStoreListLapMinutes, old, this.minimumStoreListLapMinutes);
	}

	@OAProperty(displayLength = 15)
	@OAColumn(sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getPaused() {
		return paused;
	}

	public void setPaused(OADateTime newValue) {
		OADateTime old = paused;
		fireBeforePropertyChange(P_Paused, old, newValue);
		this.paused = newValue;
		firePropertyChange(P_Paused, old, this.paused);
	}

	@OAProperty(displayName = "Last Call To Extend Store Locks", displayLength = 15, columnLength = 31, ignoreTimeZone = true)
	@OAColumn(name = "last_call_to_extend_store_locks", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getLastCallToExtendStoreLocks() {
		return lastCallToExtendStoreLocks;
	}

	public void setLastCallToExtendStoreLocks(OADateTime newValue) {
		OADateTime old = lastCallToExtendStoreLocks;
		fireBeforePropertyChange(P_LastCallToExtendStoreLocks, old, newValue);
		this.lastCallToExtendStoreLocks = newValue;
		firePropertyChange(P_LastCallToExtendStoreLocks, old, this.lastCallToExtendStoreLocks);
	}

	@OAProperty(displayName = "Store List Lap Count", displayLength = 6, columnLength = 20)
	@OAColumn(name = "store_list_lap_count", sqlType = java.sql.Types.INTEGER)
	public int getStoreListLapCount() {
		return storeListLapCount;
	}

	public void setStoreListLapCount(int newValue) {
		int old = storeListLapCount;
		fireBeforePropertyChange(P_StoreListLapCount, old, newValue);
		this.storeListLapCount = newValue;
		firePropertyChange(P_StoreListLapCount, old, this.storeListLapCount);
	}

	@OAProperty(displayName = "Begin Store List Lap", displayLength = 15, columnLength = 20, ignoreTimeZone = true)
	@OAColumn(name = "begin_store_list_lap", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getBeginStoreListLap() {
		return beginStoreListLap;
	}

	public void setBeginStoreListLap(OADateTime newValue) {
		OADateTime old = beginStoreListLap;
		fireBeforePropertyChange(P_BeginStoreListLap, old, newValue);
		this.beginStoreListLap = newValue;
		firePropertyChange(P_BeginStoreListLap, old, this.beginStoreListLap);
	}

	@OAProperty(displayName = "Check Extend Store Locks Last Error", displayLength = 15, columnLength = 35, ignoreTimeZone = true)
	@OAColumn(name = "check_extend_store_locks_last_error", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getCheckExtendStoreLocksLastError() {
		return checkExtendStoreLocksLastError;
	}

	public void setCheckExtendStoreLocksLastError(OADateTime newValue) {
		OADateTime old = checkExtendStoreLocksLastError;
		fireBeforePropertyChange(P_CheckExtendStoreLocksLastError, old, newValue);
		this.checkExtendStoreLocksLastError = newValue;
		firePropertyChange(P_CheckExtendStoreLocksLastError, old, this.checkExtendStoreLocksLastError);
	}

	@OAProperty(displayName = "Throttle Waiting Seconds", displayLength = 6, columnLength = 24)
	@OAColumn(name = "throttle_waiting_seconds", sqlType = java.sql.Types.INTEGER)
	public int getThrottleWaitingSeconds() {
		return throttleWaitingSeconds;
	}

	public void setThrottleWaitingSeconds(int newValue) {
		int old = throttleWaitingSeconds;
		fireBeforePropertyChange(P_ThrottleWaitingSeconds, old, newValue);
		this.throttleWaitingSeconds = newValue;
		firePropertyChange(P_ThrottleWaitingSeconds, old, this.throttleWaitingSeconds);
	}

	@OAProperty(displayName = "Last Used Store Id", displayLength = 6, columnLength = 18)
	@OAColumn(name = "last_used_store_id", sqlType = java.sql.Types.INTEGER)
	public int getLastUsedStoreId() {
		return lastUsedStoreId;
	}

	public void setLastUsedStoreId(int newValue) {
		int old = lastUsedStoreId;
		fireBeforePropertyChange(P_LastUsedStoreId, old, newValue);
		this.lastUsedStoreId = newValue;
		firePropertyChange(P_LastUsedStoreId, old, this.lastUsedStoreId);
	}

	@OAProperty(displayName = "Check Extend Store Locks Last Error Counter", displayLength = 6, columnLength = 43)
	@OAColumn(name = "check_extend_store_locks_last_error_counter", sqlType = java.sql.Types.INTEGER)
	public int getCheckExtendStoreLocksLastErrorCounter() {
		return checkExtendStoreLocksLastErrorCounter;
	}

	public void setCheckExtendStoreLocksLastErrorCounter(int newValue) {
		int old = checkExtendStoreLocksLastErrorCounter;
		fireBeforePropertyChange(P_CheckExtendStoreLocksLastErrorCounter, old, newValue);
		this.checkExtendStoreLocksLastErrorCounter = newValue;
		firePropertyChange(P_CheckExtendStoreLocksLastErrorCounter, old, this.checkExtendStoreLocksLastErrorCounter);
	}

	@OAOne(displayName = "Corp To Store", reverseName = CorpToStore.P_StoreLockServiceInfo, required = true, allowCreateNew = false, allowAddExisting = false)
	@OAFkey(columns = { "corp_to_store_id" })
	public CorpToStore getCorpToStore() {
		if (corpToStore == null) {
			corpToStore = (CorpToStore) getObject(P_CorpToStore);
		}
		return corpToStore;
	}

	public void setCorpToStore(CorpToStore newValue) {
		CorpToStore old = this.corpToStore;
		fireBeforePropertyChange(P_CorpToStore, old, newValue);
		this.corpToStore = newValue;
		firePropertyChange(P_CorpToStore, old, this.corpToStore);
	}

	@OAOne(displayName = "Status Info", reverseName = StatusInfo.P_StoreLockServiceInfo, required = true,
			// autoCreateNew = true, 
			allowAddExisting = false)
	@OAFkey(columns = { "status_info_id" })
	public StatusInfo getStatusInfo() {
		if (statusInfo == null) {
			statusInfo = (StatusInfo) getObject(P_StatusInfo);
		}
		return statusInfo;
	}

	public void setStatusInfo(StatusInfo newValue) {
		StatusInfo old = this.statusInfo;
		fireBeforePropertyChange(P_StatusInfo, old, newValue);
		this.statusInfo = newValue;
		firePropertyChange(P_StatusInfo, old, this.statusInfo);
	}

	@OAMany(displayName = "Store Lock Infos", toClass = StoreLockInfo.class, owner = true, reverseName = StoreLockInfo.P_StoreLockServiceInfo, isProcessed = true, cascadeSave = true, cascadeDelete = true)
	public Hub<StoreLockInfo> getStoreLockInfos() {
		if (hubStoreLockInfos == null) {
			hubStoreLockInfos = (Hub<StoreLockInfo>) getHub(P_StoreLockInfos);
		}
		return hubStoreLockInfos;
	}

	@OAMethod(displayName = "Pause")
	public void pauseService() {
		setPaused(new OADateTime());
	}

	@OAObjCallback(enabledProperty = StoreLockServiceInfo.P_Paused, enabledValue = false)
	public void pauseServiceCallback(OAObjectCallback cb) {
	}

	@OAMethod(displayName = "Continue")
	public void continueService() {
		setPaused(null);
	}

	@OAObjCallback(enabledProperty = StoreLockServiceInfo.P_Paused)
	public void continueServiceCallback(OAObjectCallback cb) {
	}

}
