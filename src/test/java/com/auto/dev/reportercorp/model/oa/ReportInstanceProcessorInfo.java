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

@OAClass(lowerName = "reportInstanceProcessorInfo", pluralName = "ReportInstanceProcessorInfos", shortName = "rip", displayName = "Report Instance Processor Info", useDataSource = false, displayProperty = "id")
public class ReportInstanceProcessorInfo extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(ReportInstanceProcessorInfo.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_Paused = "paused";
	public static final String P_ReceivedCount = "receivedCount";
	public static final String P_ProcessedCount = "processedCount";
	public static final String P_FixedJsonCount = "fixedJsonCount";
	public static final String P_PypeErrorCount = "pypeErrorCount";
	public static final String P_ProcessingErrorCount = "processingErrorCount";

	public static final String P_ReporterCorp = "reporterCorp";
	public static final String P_ReporterCorpId = "reporterCorpId"; // fkey
	public static final String P_ReportInfos = "reportInfos";
	public static final String P_ReportInstanceProcesses = "reportInstanceProcesses";
	public static final String P_StatusInfo = "statusInfo";
	public static final String P_StatusInfoId = "statusInfoId"; // fkey

	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile OADateTime paused;
	protected volatile int receivedCount;
	protected volatile int processedCount;
	protected volatile int fixedJsonCount;
	protected volatile int pypeErrorCount;
	protected volatile int processingErrorCount;

	// Links to other objects.
	protected volatile transient ReporterCorp reporterCorp;
	protected transient Hub<ReportInfo> hubReportInfos;
	protected transient Hub<ReportInstanceProcess> hubReportInstanceProcesses;
	protected volatile transient StatusInfo statusInfo;

	public ReportInstanceProcessorInfo() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
		getStatusInfo(); // have it autoCreated
	}

	public ReportInstanceProcessorInfo(int id) {
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

	@OAProperty(displayName = "Received Count", displayLength = 6, uiColumnLength = 14)
	@OAColumn(name = "received_count", sqlType = java.sql.Types.INTEGER)
	public int getReceivedCount() {
		return receivedCount;
	}

	public void setReceivedCount(int newValue) {
		int old = receivedCount;
		fireBeforePropertyChange(P_ReceivedCount, old, newValue);
		this.receivedCount = newValue;
		firePropertyChange(P_ReceivedCount, old, this.receivedCount);
	}

	@OAProperty(displayName = "Processed Count", displayLength = 6, uiColumnLength = 15)
	@OAColumn(name = "processed_count", sqlType = java.sql.Types.INTEGER)
	public int getProcessedCount() {
		return processedCount;
	}

	public void setProcessedCount(int newValue) {
		int old = processedCount;
		fireBeforePropertyChange(P_ProcessedCount, old, newValue);
		this.processedCount = newValue;
		firePropertyChange(P_ProcessedCount, old, this.processedCount);
	}

	@OAProperty(displayName = "Fixed Json Count", displayLength = 6, uiColumnLength = 16)
	@OAColumn(name = "fixed_json_count", sqlType = java.sql.Types.INTEGER)
	public int getFixedJsonCount() {
		return fixedJsonCount;
	}

	public void setFixedJsonCount(int newValue) {
		int old = fixedJsonCount;
		fireBeforePropertyChange(P_FixedJsonCount, old, newValue);
		this.fixedJsonCount = newValue;
		firePropertyChange(P_FixedJsonCount, old, this.fixedJsonCount);
	}

	@OAProperty(displayName = "Pype Error Count", displayLength = 6, uiColumnLength = 16)
	@OAColumn(name = "pype_error_count", sqlType = java.sql.Types.INTEGER)
	public int getPypeErrorCount() {
		return pypeErrorCount;
	}

	public void setPypeErrorCount(int newValue) {
		int old = pypeErrorCount;
		fireBeforePropertyChange(P_PypeErrorCount, old, newValue);
		this.pypeErrorCount = newValue;
		firePropertyChange(P_PypeErrorCount, old, this.pypeErrorCount);
	}

	@OAProperty(displayName = "Processing Error Count", displayLength = 6, uiColumnLength = 22)
	@OAColumn(name = "error_count", sqlType = java.sql.Types.INTEGER)
	public int getProcessingErrorCount() {
		return processingErrorCount;
	}

	public void setProcessingErrorCount(int newValue) {
		int old = processingErrorCount;
		fireBeforePropertyChange(P_ProcessingErrorCount, old, newValue);
		this.processingErrorCount = newValue;
		firePropertyChange(P_ProcessingErrorCount, old, this.processingErrorCount);
	}

	@OAOne(displayName = "Reporter Corp", reverseName = ReporterCorp.P_ReportInstanceProcessorInfo, required = true, allowCreateNew = false, allowAddExisting = false, fkeys = {
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

	@OAMany(displayName = "Report Infos", toClass = ReportInfo.class, owner = true, reverseName = ReportInfo.P_ReportInstanceProcessorInfo, cascadeSave = true, cascadeDelete = true, uniqueProperty = ReportInfo.P_Report)
	public Hub<ReportInfo> getReportInfos() {
		if (hubReportInfos == null) {
			hubReportInfos = (Hub<ReportInfo>) getHub(P_ReportInfos);
		}
		return hubReportInfos;
	}

	@OAMany(displayName = "Report Instance Processes", toClass = ReportInstanceProcess.class, owner = true, reverseName = ReportInstanceProcess.P_ReportInstanceProcessorInfo, cascadeSave = true, cascadeDelete = true, uniqueProperty = ReportInstanceProcess.P_Counter)
	public Hub<ReportInstanceProcess> getReportInstanceProcesses() {
		if (hubReportInstanceProcesses == null) {
			hubReportInstanceProcesses = (Hub<ReportInstanceProcess>) getHub(P_ReportInstanceProcesses);
		}
		return hubReportInstanceProcesses;
	}

	@OAOne(displayName = "Status Info", reverseName = StatusInfo.P_ReportInstanceProcessorInfo, autoCreateNew = true, fkeys = {
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
