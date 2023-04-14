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

@OAClass(lowerName = "reportInfo", pluralName = "ReportInfos", shortName = "rpi", displayName = "Report Info", useDataSource = false, displayProperty = "report")
public class ReportInfo extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(ReportInfo.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_ReceivedCount = "receivedCount";
	public static final String P_ProcessedCount = "processedCount";
	public static final String P_FixedJsonCount = "fixedJsonCount";
	public static final String P_ProcessingErrorCount = "processingErrorCount";

	public static final String P_Report = "report";
	public static final String P_ReportId = "reportId"; // fkey
	public static final String P_ReportInstanceProcesses = "reportInstanceProcesses";
	public static final String P_ReportInstanceProcessorInfo = "reportInstanceProcessorInfo";
	public static final String P_ReportInstanceProcessorInfoId = "reportInstanceProcessorInfoId"; // fkey

	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile int receivedCount;
	protected volatile int processedCount;
	protected volatile int fixedJsonCount;
	protected volatile int processingErrorCount;

	// Links to other objects.
	protected volatile transient Report report;
	protected transient Hub<ReportInstanceProcess> hubReportInstanceProcesses;
	protected volatile transient ReportInstanceProcessorInfo reportInstanceProcessorInfo;

	public ReportInfo() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public ReportInfo(int id) {
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

	@OAProperty(displayName = "Processing Error Count", displayLength = 6, uiColumnLength = 22)
	@OAColumn(name = "processing_error_count", sqlType = java.sql.Types.INTEGER)
	public int getProcessingErrorCount() {
		return processingErrorCount;
	}

	public void setProcessingErrorCount(int newValue) {
		int old = processingErrorCount;
		fireBeforePropertyChange(P_ProcessingErrorCount, old, newValue);
		this.processingErrorCount = newValue;
		firePropertyChange(P_ProcessingErrorCount, old, this.processingErrorCount);
	}

	@OAOne(reverseName = Report.P_ReportInfos, allowCreateNew = false, fkeys = {
			@OAFkey(fromProperty = P_ReportId, toProperty = Report.P_Id) })
	public Report getReport() {
		if (report == null) {
			report = (Report) getObject(P_Report);
		}
		return report;
	}

	public void setReport(Report newValue) {
		Report old = this.report;
		fireBeforePropertyChange(P_Report, old, newValue);
		this.report = newValue;
		firePropertyChange(P_Report, old, this.report);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "report_id")
	public Integer getReportId() {
		return (Integer) getFkeyProperty(P_ReportId);
	}

	public void setReportId(Integer newValue) {
		this.report = null;
		setFkeyProperty(P_ReportId, newValue);
	}

	@OAMany(displayName = "Report Instance Processes", toClass = ReportInstanceProcess.class, reverseName = ReportInstanceProcess.P_ReportInfo, uniqueProperty = ReportInstanceProcess.P_Counter, equalPropertyPath = P_ReportInstanceProcessorInfo)
	public Hub<ReportInstanceProcess> getReportInstanceProcesses() {
		if (hubReportInstanceProcesses == null) {
			hubReportInstanceProcesses = (Hub<ReportInstanceProcess>) getHub(P_ReportInstanceProcesses);
		}
		return hubReportInstanceProcesses;
	}

	@OAOne(displayName = "Report Instance Processor Info", reverseName = ReportInstanceProcessorInfo.P_ReportInfos, required = true, allowCreateNew = false, fkeys = {
			@OAFkey(fromProperty = P_ReportInstanceProcessorInfoId, toProperty = ReportInstanceProcessorInfo.P_Id) })
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

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "report_instance_processor_info_id")
	public Integer getReportInstanceProcessorInfoId() {
		return (Integer) getFkeyProperty(P_ReportInstanceProcessorInfoId);
	}

	public void setReportInstanceProcessorInfoId(Integer newValue) {
		this.reportInstanceProcessorInfo = null;
		setFkeyProperty(P_ReportInstanceProcessorInfoId, newValue);
	}
}
