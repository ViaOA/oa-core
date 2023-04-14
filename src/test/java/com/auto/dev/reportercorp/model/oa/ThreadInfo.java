package com.auto.dev.reportercorp.model.oa;

import java.util.logging.Logger;

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

@OAClass(lowerName = "threadInfo", pluralName = "ThreadInfos", shortName = "thi", displayName = "Thread Info", useDataSource = false, isProcessed = true, displayProperty = "name")
public class ThreadInfo extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(ThreadInfo.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_Name = "name";
	public static final String P_Paused = "paused";
	public static final String P_StackTrace = "stackTrace";
	public static final String P_Active = "active";
	public static final String P_LastActive = "lastActive";
	public static final String P_ActiveCount = "activeCount";

	public static final String P_ReporterCorp = "reporterCorp";
	public static final String P_ReporterCorpId = "reporterCorpId"; // fkey
	public static final String P_ReportInstanceProcesses = "reportInstanceProcesses";
	public static final String P_StatusInfo = "statusInfo";
	public static final String P_StatusInfoId = "statusInfoId"; // fkey

	public static final String M_Pause = "pause";
	public static final String M_Unpause = "unpause";
	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile String name;
	protected volatile OADateTime paused;
	protected volatile String stackTrace;
	protected volatile boolean active;
	protected volatile OADateTime lastActive;
	protected volatile int activeCount;

	// Links to other objects.
	protected volatile transient ReporterCorp reporterCorp;
	protected transient Hub<ReportInstanceProcess> hubReportInstanceProcesses;
	protected volatile transient StatusInfo statusInfo;

	public ThreadInfo() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
		getStatusInfo(); // have it autoCreated
	}

	public ThreadInfo(int id) {
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

	@OAProperty(defaultValue = "new OADateTime()", displayLength = 15, ignoreTimeZone = true)
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

	@OAProperty(maxLength = 30, displayLength = 20, isProcessed = true)
	@OAColumn(name = "name", maxLength = 30)
	public String getName() {
		return name;
	}

	public void setName(String newValue) {
		String old = name;
		fireBeforePropertyChange(P_Name, old, newValue);
		this.name = newValue;
		firePropertyChange(P_Name, old, this.name);
	}

	@OAProperty(displayLength = 15, isProcessed = true, ignoreTimeZone = true)
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

	@OAProperty(displayName = "Stack Trace", displayLength = 30, uiColumnLength = 20, isProcessed = true)
	@OAColumn(name = "stack_trace", sqlType = java.sql.Types.CLOB)
	public String getStackTrace() {
		return stackTrace;
	}

	public void setStackTrace(String newValue) {
		String old = stackTrace;
		fireBeforePropertyChange(P_StackTrace, old, newValue);
		this.stackTrace = newValue;
		firePropertyChange(P_StackTrace, old, this.stackTrace);
	}

	@OAProperty(displayLength = 5, uiColumnLength = 6)
	@OAColumn(name = "active", sqlType = java.sql.Types.BOOLEAN)
	public boolean getActive() {
		return active;
	}

	public boolean isActive() {
		return getActive();
	}

	public void setActive(boolean newValue) {
		boolean old = active;
		fireBeforePropertyChange(P_Active, old, newValue);
		this.active = newValue;
		firePropertyChange(P_Active, old, this.active);
	}

	@OAProperty(displayName = "Last Active", displayLength = 15, ignoreTimeZone = true)
	@OAColumn(name = "last_active", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getLastActive() {
		return lastActive;
	}

	public void setLastActive(OADateTime newValue) {
		OADateTime old = lastActive;
		fireBeforePropertyChange(P_LastActive, old, newValue);
		this.lastActive = newValue;
		firePropertyChange(P_LastActive, old, this.lastActive);
	}

	@OAProperty(displayName = "Active Count", displayLength = 6, uiColumnLength = 12)
	@OAColumn(name = "active_count", sqlType = java.sql.Types.INTEGER)
	public int getActiveCount() {
		return activeCount;
	}

	public void setActiveCount(int newValue) {
		int old = activeCount;
		fireBeforePropertyChange(P_ActiveCount, old, newValue);
		this.activeCount = newValue;
		firePropertyChange(P_ActiveCount, old, this.activeCount);
	}

	@OAOne(displayName = "Reporter Corp", reverseName = ReporterCorp.P_ThreadInfos, required = true, allowCreateNew = false, fkeys = {
			@OAFkey(fromProperty = P_ReporterCorpId, toProperty = ReporterCorp.P_Id) })
	public ReporterCorp getReporterCorp() {
		if (reporterCorp == null) {
			reporterCorp = (ReporterCorp) getObject(P_ReporterCorp);
		}
		return reporterCorp;
	}

	public void setReporterCorp(ReporterCorp newValue) {
		ReporterCorp old = this.reporterCorp;
		fireBeforePropertyChange(P_ReporterCorp, old, newValue);
		this.reporterCorp = newValue;
		firePropertyChange(P_ReporterCorp, old, this.reporterCorp);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "reporter_corp_id")
	public Integer getReporterCorpId() {
		return (Integer) getFkeyProperty(P_ReporterCorpId);
	}

	public void setReporterCorpId(Integer newValue) {
		this.reporterCorp = null;
		setFkeyProperty(P_ReporterCorpId, newValue);
	}

	@OAMany(displayName = "Report Instance Processes", toClass = ReportInstanceProcess.class, reverseName = ReportInstanceProcess.P_ThreadInfo, uniqueProperty = ReportInstanceProcess.P_Counter, equalPropertyPath = P_ReporterCorp)
	public Hub<ReportInstanceProcess> getReportInstanceProcesses() {
		if (hubReportInstanceProcesses == null) {
			hubReportInstanceProcesses = (Hub<ReportInstanceProcess>) getHub(P_ReportInstanceProcesses);
		}
		return hubReportInstanceProcesses;
	}

	@OAOne(displayName = "Status Info", reverseName = StatusInfo.P_ThreadInfo, autoCreateNew = true, fkeys = {
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

	@OAMethod(displayName = "Pause")
	public void pause() {
		if (getPaused() == null) {
			setPaused(new OADateTime());
		}
	}

	@OAObjCallback(enabledProperty = ThreadInfo.P_Paused, enabledValue = false)
	public void pauseCallback(OAObjectCallback cb) {
	}

	@OAMethod(displayName = "Unpause")
	public void unpause() {
		setPaused(null);
	}

	@OAObjCallback(enabledProperty = ThreadInfo.P_Paused)
	public void unpauseCallback(OAObjectCallback cb) {
	}

}
