package com.auto.dev.reportercorp.model.oa;

import java.util.logging.Logger;

import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OALinkTable;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAString;

@OAClass(lowerName = "statusInfo", pluralName = "StatusInfos", shortName = "sti", displayName = "Status Info", useDataSource = false, displayProperty = "status")
public class StatusInfo extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(StatusInfo.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_Status = "status";
	public static final String P_LastStatus = "lastStatus";
	public static final String P_StatusCount = "statusCount";
	public static final String P_Alert = "alert";
	public static final String P_LastAlert = "lastAlert";
	public static final String P_AlertCount = "alertCount";
	public static final String P_Activity = "activity";
	public static final String P_LastActivity = "lastActivity";
	public static final String P_ActivityCount = "activityCount";
	public static final String P_Console = "console";

	public static final String P_ReporterCorp = "reporterCorp";
	public static final String P_ReportInstanceProcess = "reportInstanceProcess";
	public static final String P_ReportInstanceProcessorInfo = "reportInstanceProcessorInfo";
	public static final String P_StatusInfoActivityMessages = "statusInfoActivityMessages";
	public static final String P_StatusInfoActivityMessagesId = "statusInfoActivityMessagesId"; // fkey
	public static final String P_StatusInfoAlertMessages = "statusInfoAlertMessages";
	public static final String P_StatusInfoAlertMessagesId = "statusInfoAlertMessagesId"; // fkey
	public static final String P_StatusInfoMessages = "statusInfoMessages";
	public static final String P_StoreInfo = "storeInfo";
	public static final String P_ThreadInfo = "threadInfo";

	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile String status;
	protected volatile OADateTime lastStatus;
	protected volatile int statusCount;
	protected volatile String alert;
	protected volatile OADateTime lastAlert;
	protected volatile int alertCount;
	protected volatile String activity;
	protected volatile OADateTime lastActivity;
	protected volatile int activityCount;
	protected volatile String console;

	// Links to other objects.
	protected transient Hub<StatusInfoMessage> hubStatusInfoActivityMessages;
	protected transient Hub<StatusInfoMessage> hubStatusInfoAlertMessages;
	protected transient Hub<StatusInfoMessage> hubStatusInfoMessages;

	public StatusInfo() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public StatusInfo(int id) {
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

	@OAProperty(maxLength = 250, displayLength = 20, uiColumnLength = 15, hasCustomCode = true)
	@OAColumn(name = "status", maxLength = 250)
	public String getStatus() {
		return status;
	}

	public void setStatus(String newValue) {
		String old = status;
		fireBeforePropertyChange(P_Status, old, newValue);
		this.status = newValue;
		firePropertyChange(P_Status, old, this.status);
		if (isSyncThread() || isLoading()) {
			return;
		}
		if (OAString.isEmpty(newValue)) {
			return;
		}
		if (!startServerOnly()) {
			return;
		}
		try {
			setLastStatus(new OADateTime());
			int x = getStatusCount() + 1;
			setStatusCount(x);
			StatusInfoMessage sim = new StatusInfoMessage();
			sim.setMessage(newValue);
			sim.setType(sim.TYPE_status);
			sim.setCounter(x);
			getStatusInfoMessages().add(sim);
			if (getStatusInfoMessages().size() > 100) {
				getStatusInfoMessages().getAt(0).delete();
			}
			setConsole(newValue);
		} finally {
			endServerOnly();
		}
	}

	@OAProperty(displayName = "Last Status", displayLength = 15, format = "yy/MM/dd hh:mm:ss", isProcessed = true, ignoreTimeZone = true)
	@OAColumn(name = "last_status", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getLastStatus() {
		return lastStatus;
	}

	public void setLastStatus(OADateTime newValue) {
		OADateTime old = lastStatus;
		fireBeforePropertyChange(P_LastStatus, old, newValue);
		this.lastStatus = newValue;
		firePropertyChange(P_LastStatus, old, this.lastStatus);
	}

	@OAProperty(displayName = "Status Count", displayLength = 6, uiColumnLength = 12, isProcessed = true)
	@OAColumn(name = "status_count", sqlType = java.sql.Types.INTEGER)
	public int getStatusCount() {
		return statusCount;
	}

	public void setStatusCount(int newValue) {
		int old = statusCount;
		fireBeforePropertyChange(P_StatusCount, old, newValue);
		this.statusCount = newValue;
		firePropertyChange(P_StatusCount, old, this.statusCount);
	}

	@OAProperty(displayLength = 30, uiColumnLength = 20, hasCustomCode = true)
	@OAColumn(name = "alert", sqlType = java.sql.Types.CLOB)
	public String getAlert() {
		return alert;
	}

	public void setAlert(String newValue) {
		String old = alert;
		fireBeforePropertyChange(P_Alert, old, newValue);
		this.alert = newValue;
		firePropertyChange(P_Alert, old, this.alert);
		if (isSyncThread() || isLoading()) {
			return;
		}
		if (OAString.isEmpty(newValue)) {
			return;
		}
		if (!startServerOnly()) {
			return;
		}
		try {
			setLastAlert(new OADateTime());
			int x = getAlertCount() + 1;
			setAlertCount(x);

			StatusInfoMessage sim = new StatusInfoMessage();
			sim.setMessage(newValue);
			sim.setType(sim.TYPE_alert);
			sim.setCounter(x);
			getStatusInfoAlertMessages().add(sim);
			if (getStatusInfoAlertMessages().size() > 250) {
				getStatusInfoAlertMessages().getAt(0).delete();
			}
			setStatus("Alert: " + newValue);
		} finally {
			endServerOnly();
		}
	}

	@OAProperty(displayName = "Last Alert", displayLength = 15, format = "yy/MM/dd hh:mm:ss", isProcessed = true, ignoreTimeZone = true)
	@OAColumn(name = "last_alert", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getLastAlert() {
		return lastAlert;
	}

	public void setLastAlert(OADateTime newValue) {
		OADateTime old = lastAlert;
		fireBeforePropertyChange(P_LastAlert, old, newValue);
		this.lastAlert = newValue;
		firePropertyChange(P_LastAlert, old, this.lastAlert);
	}

	@OAProperty(displayName = "Alert Count", displayLength = 6, uiColumnLength = 11, isProcessed = true)
	@OAColumn(name = "alert_count", sqlType = java.sql.Types.INTEGER)
	public int getAlertCount() {
		return alertCount;
	}

	public void setAlertCount(int newValue) {
		int old = alertCount;
		fireBeforePropertyChange(P_AlertCount, old, newValue);
		this.alertCount = newValue;
		firePropertyChange(P_AlertCount, old, this.alertCount);
	}

	@OAProperty(maxLength = 250, displayLength = 20, uiColumnLength = 15, hasCustomCode = true)
	@OAColumn(name = "activity", maxLength = 250)
	public String getActivity() {
		return activity;
	}

	public void setActivity(String newValue) {
		String old = activity;
		fireBeforePropertyChange(P_Activity, old, newValue);
		this.activity = newValue;
		firePropertyChange(P_Activity, old, this.activity);
		if (isSyncThread() || isLoading()) {
			return;
		}
		if (OAString.isEmpty(newValue)) {
			return;
		}
		if (!startServerOnly()) {
			return;
		}
		try {
			setLastActivity(new OADateTime());
			int x = getActivityCount() + 1;
			setActivityCount(x);

			StatusInfoMessage sim = new StatusInfoMessage();
			sim.setMessage(newValue);
			sim.setType(sim.TYPE_activity);
			sim.setCounter(x);
			getStatusInfoActivityMessages().add(sim);
			if (getStatusInfoActivityMessages().size() > 250) {
				getStatusInfoActivityMessages().getAt(0).delete();
			}
			setStatus("Activity: " + newValue);
		} finally {
			endServerOnly();
		}
	}

	@OAProperty(displayName = "Last Activity", displayLength = 15, format = "yy/MM/dd hh:mm:ss", isProcessed = true, ignoreTimeZone = true)
	@OAColumn(name = "last_activity", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getLastActivity() {
		return lastActivity;
	}

	public void setLastActivity(OADateTime newValue) {
		OADateTime old = lastActivity;
		fireBeforePropertyChange(P_LastActivity, old, newValue);
		this.lastActivity = newValue;
		firePropertyChange(P_LastActivity, old, this.lastActivity);
	}

	@OAProperty(displayName = "Activity Count", displayLength = 6, uiColumnLength = 14, isProcessed = true)
	@OAColumn(name = "activity_count", sqlType = java.sql.Types.INTEGER)
	public int getActivityCount() {
		return activityCount;
	}

	public void setActivityCount(int newValue) {
		int old = activityCount;
		fireBeforePropertyChange(P_ActivityCount, old, newValue);
		this.activityCount = newValue;
		firePropertyChange(P_ActivityCount, old, this.activityCount);
	}

	@OAProperty(maxLength = 254, displayLength = 50, uiColumnLength = 20, isProcessed = true)
	public String getConsole() {
		return console;
	}

	public void setConsole(String newValue) {
		String old = console;
		fireBeforePropertyChange(P_Console, old, newValue);
		this.console = newValue;
		firePropertyChange(P_Console, old, this.console);
	}

	@OAOne(displayName = "Reporter Corp", reverseName = ReporterCorp.P_StatusInfo, allowCreateNew = false, allowAddExisting = false, isOneAndOnlyOne = true)
	private ReporterCorp getReporterCorp() {
		// oamodel has createMethod set to false, this method exists only for annotations.
		return null;
	}

	@OAOne(displayName = "Report Instance Process", reverseName = ReportInstanceProcess.P_StatusInfo, allowCreateNew = false, allowAddExisting = false, isOneAndOnlyOne = true)
	private ReportInstanceProcess getReportInstanceProcess() {
		// oamodel has createMethod set to false, this method exists only for annotations.
		return null;
	}

	@OAOne(displayName = "Report Instance Processor Info", reverseName = ReportInstanceProcessorInfo.P_StatusInfo, allowCreateNew = false, allowAddExisting = false, isOneAndOnlyOne = true)
	private ReportInstanceProcessorInfo getReportInstanceProcessorInfo() {
		// oamodel has createMethod set to false, this method exists only for annotations.
		return null;
	}

	@OAMany(displayName = "Activity Messages", toClass = StatusInfoMessage.class, reverseName = StatusInfoMessage.P_StatusInfoActivity, isProcessed = true, uniqueProperty = StatusInfoMessage.P_Counter)
	@OALinkTable(name = "status_info_status_info_message", indexName = "status_info_message_status_info_activity", columns = {
			"status_info_id" })
	public Hub<StatusInfoMessage> getStatusInfoActivityMessages() {
		if (hubStatusInfoActivityMessages == null) {
			hubStatusInfoActivityMessages = (Hub<StatusInfoMessage>) getHub(P_StatusInfoActivityMessages);
		}
		return hubStatusInfoActivityMessages;
	}

	@OAMany(displayName = "Alert Messages", toClass = StatusInfoMessage.class, reverseName = StatusInfoMessage.P_StatusInfoAlert, isProcessed = true, uniqueProperty = StatusInfoMessage.P_Counter)
	@OALinkTable(name = "status_info_status_info_message2", indexName = "status_info_message_status_info_alert", columns = {
			"status_info_id" })
	public Hub<StatusInfoMessage> getStatusInfoAlertMessages() {
		if (hubStatusInfoAlertMessages == null) {
			hubStatusInfoAlertMessages = (Hub<StatusInfoMessage>) getHub(P_StatusInfoAlertMessages);
		}
		return hubStatusInfoAlertMessages;
	}

	@OAMany(displayName = "Status Info Messages", toClass = StatusInfoMessage.class, owner = true, reverseName = StatusInfoMessage.P_StatusInfo, isProcessed = true, cascadeSave = true, cascadeDelete = true, uniqueProperty = StatusInfoMessage.P_Counter)
	public Hub<StatusInfoMessage> getStatusInfoMessages() {
		if (hubStatusInfoMessages == null) {
			hubStatusInfoMessages = (Hub<StatusInfoMessage>) getHub(P_StatusInfoMessages);
		}
		return hubStatusInfoMessages;
	}

	@OAOne(displayName = "Store Info", reverseName = StoreInfo.P_StatusInfo, allowCreateNew = false, allowAddExisting = false, isOneAndOnlyOne = true)
	private StoreInfo getStoreInfo() {
		// oamodel has createMethod set to false, this method exists only for annotations.
		return null;
	}

	@OAOne(displayName = "Thread Info", reverseName = ThreadInfo.P_StatusInfo, allowCreateNew = false, allowAddExisting = false, isOneAndOnlyOne = true)
	private ThreadInfo getThreadInfo() {
		// oamodel has createMethod set to false, this method exists only for annotations.
		return null;
	}
}
