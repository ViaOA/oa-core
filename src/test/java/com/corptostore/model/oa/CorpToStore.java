// Generated by OABuilder
package com.corptostore.model.oa;

import java.util.logging.Logger;

import com.corptostore.delegate.oa.CorpToStoreDelegate;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAMethod;
import com.viaoa.annotation.OAObjCallback;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.annotation.OATriggerMethod;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.util.OADateTime;

@OAClass(lowerName = "corpToStore", pluralName = "CorpToStores", shortName = "cts", displayName = "Corp To Store", useDataSource = false, displayProperty = "nodeName")
public class CorpToStore extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(CorpToStore.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_NodeName = "nodeName";
	public static final String P_BaseUrl = "baseUrl";
	public static final String P_AllPaused = "allPaused";
	public static final String P_ThreadsPaused = "threadsPaused";
	public static final String P_PurgePaused = "purgePaused";
	public static final String P_Console = "console";
	public static final String P_LastSync = "lastSync";

	public static final String P_Environment = "environment";
	public static final String P_PurgeWindows = "purgeWindows";
	public static final String P_StatusInfo = "statusInfo";
	public static final String P_StoreInfos = "storeInfos";
	public static final String P_StoreLockServiceInfo = "storeLockServiceInfo";
	public static final String P_ThreadInfos = "threadInfos";
	public static final String P_TransmitBatchServiceInfo = "transmitBatchServiceInfo";

	public static final String M_UpdateInfo = "updateInfo";
	public static final String M_PauseAll = "pauseAll";
	public static final String M_ContinueAll = "continueAll";
	public static final String M_PauseThreads = "pauseThreads";
	public static final String M_ContinueThreads = "continueThreads";
	public static final String M_PausePurge = "pausePurge";
	public static final String M_ContinuePurge = "continuePurge";
	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile String nodeName;
	protected volatile String baseUrl;
	protected volatile OADateTime allPaused;
	protected volatile OADateTime threadsPaused;
	protected volatile OADateTime purgePaused;
	protected volatile String console;
	protected volatile OADateTime lastSync;

	// Links to other objects.
	protected volatile transient Environment environment;
	protected transient Hub<PurgeWindow> hubPurgeWindows;
	protected volatile transient StatusInfo statusInfo;
	protected transient Hub<StoreInfo> hubStoreInfos;
	protected volatile transient StoreLockServiceInfo storeLockServiceInfo;
	protected transient Hub<ThreadInfo> hubThreadInfos;
	protected volatile transient TransmitBatchServiceInfo transmitBatchServiceInfo;

	public CorpToStore() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
		getStatusInfo(); // have it autoCreated
		getStoreLockServiceInfo(); // have it autoCreated
		getTransmitBatchServiceInfo(); // have it autoCreated
	}

	public CorpToStore(int id) {
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

	@OAProperty(displayName = "Node Name", maxLength = 80, displayLength = 20)
	@OAColumn(name = "node_name", maxLength = 80)
	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String newValue) {
		String old = nodeName;
		fireBeforePropertyChange(P_NodeName, old, newValue);
		this.nodeName = newValue;
		firePropertyChange(P_NodeName, old, this.nodeName);
	}

	@OAProperty(displayName = "Base Url", maxLength = 125, displayLength = 20, isUrl = true)
	@OAColumn(name = "base_url", maxLength = 125)
	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String newValue) {
		String old = baseUrl;
		fireBeforePropertyChange(P_BaseUrl, old, newValue);
		this.baseUrl = newValue;
		firePropertyChange(P_BaseUrl, old, this.baseUrl);
	}

	@OAProperty(displayName = "All Paused", trackPrimitiveNull = false, displayLength = 15, isProcessed = true)
	@OAColumn(name = "all_paused", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getAllPaused() {
		return allPaused;
	}

	public void setAllPaused(OADateTime newValue) {
		OADateTime old = allPaused;
		fireBeforePropertyChange(P_AllPaused, old, newValue);
		this.allPaused = newValue;
		firePropertyChange(P_AllPaused, old, this.allPaused);
	}

	@OAProperty(displayName = "Threads Paused", displayLength = 15, isProcessed = true)
	@OAColumn(name = "threads_paused", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getThreadsPaused() {
		return threadsPaused;
	}

	public void setThreadsPaused(OADateTime newValue) {
		OADateTime old = threadsPaused;
		fireBeforePropertyChange(P_ThreadsPaused, old, newValue);
		this.threadsPaused = newValue;
		firePropertyChange(P_ThreadsPaused, old, this.threadsPaused);
	}

	@OAProperty(displayName = "Purge Paused", displayLength = 15, isProcessed = true)
	@OAColumn(name = "purge_paused", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getPurgePaused() {
		return purgePaused;
	}

	public void setPurgePaused(OADateTime newValue) {
		OADateTime old = purgePaused;
		fireBeforePropertyChange(P_PurgePaused, old, newValue);
		this.purgePaused = newValue;
		firePropertyChange(P_PurgePaused, old, this.purgePaused);
	}

	@OAProperty(maxLength = 254, displayLength = 50, columnLength = 20, isProcessed = true)
	public String getConsole() {
		return console;
	}

	public void setConsole(String newValue) {
		String old = console;
		fireBeforePropertyChange(P_Console, old, newValue);
		this.console = newValue;
		firePropertyChange(P_Console, old, this.console);
	}

	@OAProperty(displayName = "Last Sync", displayLength = 15)
	@OAColumn(name = "last_sync", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getLastSync() {
		return lastSync;
	}

	public void setLastSync(OADateTime newValue) {
		OADateTime old = lastSync;
		fireBeforePropertyChange(P_LastSync, old, newValue);
		this.lastSync = newValue;
		firePropertyChange(P_LastSync, old, this.lastSync);
	}

	@OAOne(reverseName = Environment.P_CorpToStores, required = true, allowCreateNew = false)
	@OAFkey(columns = { "environment_id" })
	public Environment getEnvironment() {
		if (environment == null) {
			environment = (Environment) getObject(P_Environment);
		}
		return environment;
	}

	public void setEnvironment(Environment newValue) {
		Environment old = this.environment;
		fireBeforePropertyChange(P_Environment, old, newValue);
		this.environment = newValue;
		firePropertyChange(P_Environment, old, this.environment);
	}

	@OAMany(displayName = "Purge Windows", toClass = PurgeWindow.class, owner = true, reverseName = PurgeWindow.P_CorpToStore, isProcessed = true, cascadeSave = true, cascadeDelete = true)
	public Hub<PurgeWindow> getPurgeWindows() {
		if (hubPurgeWindows == null) {
			hubPurgeWindows = (Hub<PurgeWindow>) getHub(P_PurgeWindows);
		}
		return hubPurgeWindows;
	}

	@OAOne(displayName = "Status Info", reverseName = StatusInfo.P_CorpToStore, required = true,
			//        autoCreateNew = true,
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

	@OAMany(displayName = "Store Infos", toClass = StoreInfo.class, owner = true, reverseName = StoreInfo.P_CorpToStore, isProcessed = true, cascadeSave = true, cascadeDelete = true, uniqueProperty = StoreInfo.P_Store)
	public Hub<StoreInfo> getStoreInfos() {
		if (hubStoreInfos == null) {
			hubStoreInfos = (Hub<StoreInfo>) getHub(P_StoreInfos);
		}
		return hubStoreInfos;
	}

	@OAOne(displayName = "Store Lock Service Info", owner = true, reverseName = StoreLockServiceInfo.P_CorpToStore, cascadeSave = true, cascadeDelete = true, autoCreateNew = true, allowAddExisting = false)
	public StoreLockServiceInfo getStoreLockServiceInfo() {
		if (storeLockServiceInfo == null) {
			storeLockServiceInfo = (StoreLockServiceInfo) getObject(P_StoreLockServiceInfo);
		}
		return storeLockServiceInfo;
	}

	public void setStoreLockServiceInfo(StoreLockServiceInfo newValue) {
		StoreLockServiceInfo old = this.storeLockServiceInfo;
		fireBeforePropertyChange(P_StoreLockServiceInfo, old, newValue);
		this.storeLockServiceInfo = newValue;
		firePropertyChange(P_StoreLockServiceInfo, old, this.storeLockServiceInfo);
	}

	@OAMany(displayName = "Thread Infos", toClass = ThreadInfo.class, owner = true, reverseName = ThreadInfo.P_CorpToStore, isProcessed = true, cascadeSave = true, cascadeDelete = true, uniqueProperty = ThreadInfo.P_Name)
	public Hub<ThreadInfo> getThreadInfos() {
		if (hubThreadInfos == null) {
			hubThreadInfos = (Hub<ThreadInfo>) getHub(P_ThreadInfos);
		}
		return hubThreadInfos;
	}

	@OAOne(displayName = "Transmit Batch Service Info", owner = true, reverseName = TransmitBatchServiceInfo.P_CorpToStore, cascadeSave = true, cascadeDelete = true, autoCreateNew = false, allowAddExisting = false)
	public TransmitBatchServiceInfo getTransmitBatchServiceInfo() {
		if (transmitBatchServiceInfo == null) {
			transmitBatchServiceInfo = (TransmitBatchServiceInfo) getObject(P_TransmitBatchServiceInfo);
		}
		return transmitBatchServiceInfo;
	}

	public void setTransmitBatchServiceInfo(TransmitBatchServiceInfo newValue) {
		TransmitBatchServiceInfo old = this.transmitBatchServiceInfo;
		fireBeforePropertyChange(P_TransmitBatchServiceInfo, old, newValue);
		this.transmitBatchServiceInfo = newValue;
		firePropertyChange(P_TransmitBatchServiceInfo, old, this.transmitBatchServiceInfo);
	}

	@OATriggerMethod(onlyUseLoadedData = true, runOnServer = true, runInBackgroundThread = true, properties = { P_AllPaused, P_PurgePaused,
			P_StoreLockServiceInfo + "." + StoreLockServiceInfo.P_Paused, P_ThreadInfos + "." + ThreadInfo.P_Paused,
			P_TransmitBatchServiceInfo + "." + TransmitBatchServiceInfo.P_Paused, P_StoreInfos + "." + StoreInfo.P_Paused, P_BaseUrl })
	public void onPauseChangeTrigger(HubEvent hubEvent) {
		if (!beginServerOnly()) {
			return;
		}
		try {
			CorpToStoreDelegate.onPauseChangeTrigger(this, hubEvent);
		} finally {
			endServerOnly();
		}
	}

	@OAMethod(displayName = "Update Info")
	public void updateInfo() {
		CorpToStoreDelegate.updateInfo(this);
	}

	@OAMethod(displayName = "Pause")
	public void pauseAll() {
		setAllPaused(new OADateTime());
	}

	@OAObjCallback(enabledProperty = CorpToStore.P_AllPaused, enabledValue = false)
	public void pauseAllCallback(OAObjectCallback cb) {
	}

	@OAMethod(displayName = "Continue")
	public void continueAll() {
		setAllPaused(null);
	}

	@OAObjCallback(enabledProperty = CorpToStore.P_AllPaused)
	public void continueAllCallback(OAObjectCallback cb) {
	}

	@OAMethod(displayName = "Pause")
	public void pauseThreads() {
		setThreadsPaused(new OADateTime());
	}

	@OAObjCallback(enabledProperty = CorpToStore.P_ThreadsPaused, enabledValue = false)
	public void pauseThreadsCallback(OAObjectCallback cb) {
	}

	@OAMethod(displayName = "Continue")
	public void continueThreads() {
		setThreadsPaused(null);
	}

	@OAObjCallback(enabledProperty = CorpToStore.P_ThreadsPaused)
	public void continueThreadsCallback(OAObjectCallback cb) {
	}

	@OAMethod(displayName = "Pause")
	public void pausePurge() {
		setPurgePaused(new OADateTime());
	}

	@OAObjCallback(enabledProperty = CorpToStore.P_PurgePaused, enabledValue = false)
	public void pausePurgeCallback(OAObjectCallback cb) {
	}

	@OAMethod(displayName = "Continue")
	public void continuePurge() {
		setPurgePaused(null);
	}

	@OAObjCallback(enabledProperty = CorpToStore.P_ThreadsPaused)
	public void continuePurgeCallback(OAObjectCallback cb) {
	}

}
