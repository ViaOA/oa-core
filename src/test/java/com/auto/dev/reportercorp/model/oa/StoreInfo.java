package com.auto.dev.reportercorp.model.oa;

import java.util.logging.Logger;

import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.util.OADateTime;

@OAClass(lowerName = "storeInfo", pluralName = "StoreInfos", shortName = "sti", displayName = "Store Info", useDataSource = false, displayProperty = "statusInfo")
public class StoreInfo extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(StoreInfo.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_StoreNumber = "storeNumber";

	public static final String P_ReporterCorp = "reporterCorp";
	public static final String P_ReporterCorpId = "reporterCorpId"; // fkey
	public static final String P_ReportInstanceProcesses = "reportInstanceProcesses";
	public static final String P_StatusInfo = "statusInfo";
	public static final String P_StatusInfoId = "statusInfoId"; // fkey

	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile int storeNumber;

	// Links to other objects.
	protected volatile transient ReporterCorp reporterCorp;
	protected transient Hub<ReportInstanceProcess> hubReportInstanceProcesses;
	protected volatile transient StatusInfo statusInfo;

	public StoreInfo() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
		getStatusInfo(); // have it autoCreated
	}

	public StoreInfo(int id) {
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

	@OAProperty(displayName = "Store Number", displayLength = 6, uiColumnLength = 12)
	@OAColumn(name = "store_number", sqlType = java.sql.Types.INTEGER)
	public int getStoreNumber() {
		return storeNumber;
	}

	public void setStoreNumber(int newValue) {
		int old = storeNumber;
		fireBeforePropertyChange(P_StoreNumber, old, newValue);
		this.storeNumber = newValue;
		firePropertyChange(P_StoreNumber, old, this.storeNumber);
	}

	@OAOne(displayName = "Reporter Corp", reverseName = ReporterCorp.P_StoreInfos, required = true, allowCreateNew = false, fkeys = {
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

	@OAMany(displayName = "Report Instance Processes", toClass = ReportInstanceProcess.class, reverseName = ReportInstanceProcess.P_StoreInfo, uniqueProperty = ReportInstanceProcess.P_Counter, equalPropertyPath = P_ReporterCorp)
	public Hub<ReportInstanceProcess> getReportInstanceProcesses() {
		if (hubReportInstanceProcesses == null) {
			hubReportInstanceProcesses = (Hub<ReportInstanceProcess>) getHub(P_ReportInstanceProcesses);
		}
		return hubReportInstanceProcesses;
	}

	@OAOne(displayName = "Status Info", reverseName = StatusInfo.P_StoreInfo, required = true, autoCreateNew = true, allowAddExisting = false, fkeys = {
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
}
