package com.auto.dev.reportercorp.model.oa;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.delegate.oa.ReporterCorpDelegate;
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

@OAClass(lowerName = "reporterCorp", pluralName = "ReporterCorps", shortName = "rpc", displayName = "Reporter Corp", isPreSelect = true, useDataSource = false, displayProperty = "nodeName", pojoSingleton = true)
public class ReporterCorp extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(ReporterCorp.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_LastSync = "lastSync";
	public static final String P_Stopped = "stopped";
	public static final String P_NodeName = "nodeName";
	public static final String P_BaseUrl = "baseUrl";
	public static final String P_Paused = "paused";
	public static final String P_Console = "console";

	public static final String P_Environment = "environment";
	public static final String P_EnvironmentId = "environmentId"; // fkey
	public static final String P_ReporterCorpParams = "reporterCorpParams";
	public static final String P_ReportInstanceProcessorInfo = "reportInstanceProcessorInfo";
	public static final String P_StatusInfo = "statusInfo";
	public static final String P_StatusInfoId = "statusInfoId"; // fkey
	public static final String P_StoreInfos = "storeInfos";
	public static final String P_ThreadInfos = "threadInfos";

	public static final String M_Pause = "pause";
	public static final String M_Unpause = "unpause";
	public static final String M_ClearCache = "clearCache";
	public static final String M_GetPojoReports = "getPojoReports";
	public static final String M_ResizeCacheObjects = "resizeCacheObjects";
	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile OADateTime lastSync;
	protected volatile OADateTime stopped;
	protected volatile String nodeName;
	protected volatile String baseUrl;
	protected volatile OADateTime paused;
	protected volatile String console;

	// Links to other objects.
	protected volatile transient Environment environment;
	protected transient Hub<ReporterCorpParam> hubReporterCorpParams;
	protected volatile transient ReportInstanceProcessorInfo reportInstanceProcessorInfo;
	protected volatile transient StatusInfo statusInfo;
	protected transient Hub<StoreInfo> hubStoreInfos;
	protected transient Hub<ThreadInfo> hubThreadInfos;

	public ReporterCorp() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
		getReportInstanceProcessorInfo(); // have it autoCreated
		getStatusInfo(); // have it autoCreated
	}

	public ReporterCorp(int id) {
		this();
		setId(id);
	}

	@OAProperty(isUnique = true, trackPrimitiveNull = false, displayLength = 6, noPojo = true)
	@OAId
	@OAColumn(name = "id", sqlType = java.sql.Types.INTEGER)
	public int getId() {
		return id;
	}

	public void setId(int newValue) {
		int old = id;
		fireBeforePropertyChange(P_Id, old, newValue);
		this.id = newValue;
		firePropertyChange(P_Id, old, this.id);
	}

	@OAProperty(defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true, ignoreTimeZone = true)
	@OAColumn(name = "created", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getCreated() {
		return created;
	}

	public void setCreated(OADateTime newValue) {
		OADateTime old = created;
		fireBeforePropertyChange(P_Created, old, newValue);
		this.created = newValue;
		firePropertyChange(P_Created, old, this.created);
	}

	@OAProperty(displayName = "Last Sync", displayLength = 15, isProcessed = true, noPojo = true, ignoreTimeZone = true)
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

	@OAProperty(displayLength = 15, ignoreTimeZone = true)
	@OAColumn(name = "stopped", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getStopped() {
		return stopped;
	}

	public void setStopped(OADateTime newValue) {
		OADateTime old = stopped;
		fireBeforePropertyChange(P_Stopped, old, newValue);
		this.stopped = newValue;
		firePropertyChange(P_Stopped, old, this.stopped);
	}

	@OAProperty(displayName = "Node Name", maxLength = 80, displayLength = 20, noPojo = true)
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

	@OAProperty(displayName = "Base Url", maxLength = 125, displayLength = 20, isUrl = true, noPojo = true)
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

	@OAProperty(trackPrimitiveNull = false, displayLength = 15, isProcessed = true, ignoreTimeZone = true)
	@OAColumn(name = "paused", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getPaused() {
		return paused;
	}

	public void setPaused(OADateTime newValue) {
		OADateTime old = paused;
		fireBeforePropertyChange(P_Paused, old, newValue);
		this.paused = newValue;
		firePropertyChange(P_Paused, old, this.paused);
	}

	@OAProperty(maxLength = 254, displayLength = 50, uiColumnLength = 20, isProcessed = true, noPojo = true)
	public String getConsole() {
		return console;
	}

	public void setConsole(String newValue) {
		String old = console;
		fireBeforePropertyChange(P_Console, old, newValue);
		this.console = newValue;
		firePropertyChange(P_Console, old, this.console);
	}

	@OAOne(reverseName = Environment.P_ReporterCorps, required = true, allowCreateNew = false, fkeys = {
			@OAFkey(fromProperty = P_EnvironmentId, toProperty = Environment.P_Id) })
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

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "environment_id")
	public Integer getEnvironmentId() {
		return (Integer) getFkeyProperty(P_EnvironmentId);
	}

	public void setEnvironmentId(Integer newValue) {
		this.environment = null;
		setFkeyProperty(P_EnvironmentId, newValue);
	}

	@OAMany(displayName = "Reporter Corp Params", toClass = ReporterCorpParam.class, owner = true, reverseName = ReporterCorpParam.P_ReporterCorp, cascadeSave = true, cascadeDelete = true, uniqueProperty = ReporterCorpParam.P_Name)
	public Hub<ReporterCorpParam> getReporterCorpParams() {
		if (hubReporterCorpParams == null) {
			hubReporterCorpParams = (Hub<ReporterCorpParam>) getHub(P_ReporterCorpParams);
		}
		return hubReporterCorpParams;
	}

	@OAOne(displayName = "Report Instance Processor Info", owner = true, reverseName = ReportInstanceProcessorInfo.P_ReporterCorp, cascadeSave = true, cascadeDelete = true, autoCreateNew = true, allowAddExisting = false)
	public ReportInstanceProcessorInfo getReportInstanceProcessorInfo() {
		if (reportInstanceProcessorInfo == null) {
			reportInstanceProcessorInfo = (ReportInstanceProcessorInfo) getObject(P_ReportInstanceProcessorInfo);
		}
		return reportInstanceProcessorInfo;
	}

	public void setReportInstanceProcessorInfo(ReportInstanceProcessorInfo newValue) {
		ReportInstanceProcessorInfo old = this.reportInstanceProcessorInfo;
		fireBeforePropertyChange(P_ReportInstanceProcessorInfo, old, newValue);
		this.reportInstanceProcessorInfo = newValue;
		firePropertyChange(P_ReportInstanceProcessorInfo, old, this.reportInstanceProcessorInfo);
	}

	@OAOne(displayName = "Status Info", reverseName = StatusInfo.P_ReporterCorp, required = true, autoCreateNew = true, allowAddExisting = false, fkeys = {
			@OAFkey(fromProperty = P_StatusInfoId, toProperty = StatusInfo.P_Id) })
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

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "status_info_id")
	public Integer getStatusInfoId() {
		return (Integer) getFkeyProperty(P_StatusInfoId);
	}

	public void setStatusInfoId(Integer newValue) {
		this.statusInfo = null;
		setFkeyProperty(P_StatusInfoId, newValue);
	}

	@OAMany(displayName = "Store Infos", toClass = StoreInfo.class, owner = true, reverseName = StoreInfo.P_ReporterCorp, cascadeSave = true, cascadeDelete = true, uniqueProperty = StoreInfo.P_StoreNumber)
	public Hub<StoreInfo> getStoreInfos() {
		if (hubStoreInfos == null) {
			hubStoreInfos = (Hub<StoreInfo>) getHub(P_StoreInfos);
		}
		return hubStoreInfos;
	}

	@OAMany(displayName = "Thread Infos", toClass = ThreadInfo.class, owner = true, reverseName = ThreadInfo.P_ReporterCorp, cascadeSave = true, cascadeDelete = true, uniqueProperty = ThreadInfo.P_Name)
	public Hub<ThreadInfo> getThreadInfos() {
		if (hubThreadInfos == null) {
			hubThreadInfos = (Hub<ThreadInfo>) getHub(P_ThreadInfos);
		}
		return hubThreadInfos;
	}

	@OAMethod(displayName = "Pause")
	public void pause() {
		ReporterCorpDelegate.setPaused(this);
	}

	@OAObjCallback(enabledProperty = ReporterCorp.P_Paused, enabledValue = false)
	public void pauseCallback(OAObjectCallback cb) {
	}

	@OAMethod(displayName = "Unpause")
	public void unpause() {
		ReporterCorpDelegate.setPaused(this);
	}

	@OAObjCallback(enabledProperty = ReporterCorp.P_Paused)
	public void unpauseCallback(OAObjectCallback cb) {
	}

	@OAMethod(displayName = "Clear Cache")
	public void clearCache() {
		ReporterCorpDelegate.clearCache(this);
	}

	@OAMethod(displayName = "Get Pojo Reports")
	public void getPojoReports() {
		ReporterCorpDelegate.getPojoReports(this);
	}

	@OAMethod(displayName = "Resize Cache Objects")
	public void resizeCacheObjects() {
		ReporterCorpDelegate.resizeCacheObjects(this);
	}

}
