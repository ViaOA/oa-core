package com.auto.dev.reportercorp.model.oa;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.delegate.oa.ReportInstanceProcessDelegate;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAMethod;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.util.OADateTime;

@OAClass(lowerName = "reportInstanceProcess", pluralName = "ReportInstanceProcesses", shortName = "rip", displayName = "Report Instance Process", useDataSource = false, displayProperty = "pypeReportMessage")
public class ReportInstanceProcess extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(ReportInstanceProcess.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_Counter = "counter";
	public static final String P_Completed = "completed";
	public static final String P_FixedJson = "fixedJson";
	public static final String P_PypeError = "pypeError";
	public static final String P_ProcessingError = "processingError";

	public static final String P_PypeReportMessage = "pypeReportMessage";
	public static final String P_PypeReportMessageId = "pypeReportMessageId"; // fkey
	public static final String P_ReportInfo = "reportInfo";
	public static final String P_ReportInfoId = "reportInfoId"; // fkey
	public static final String P_ReportInstanceProcessorInfo = "reportInstanceProcessorInfo";
	public static final String P_ReportInstanceProcessorInfoId = "reportInstanceProcessorInfoId"; // fkey
	public static final String P_ReportInstanceProcessSteps = "reportInstanceProcessSteps";
	public static final String P_StatusInfo = "statusInfo";
	public static final String P_StatusInfoId = "statusInfoId"; // fkey
	public static final String P_StoreInfo = "storeInfo";
	public static final String P_StoreInfoId = "storeInfoId"; // fkey
	public static final String P_ThreadInfo = "threadInfo";
	public static final String P_ThreadInfoId = "threadInfoId"; // fkey

	public static final String M_ConvertToJson = "convertToJson";
	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile int counter;
	protected volatile OADateTime completed;
	protected volatile boolean fixedJson;
	protected volatile boolean pypeError;
	protected volatile boolean processingError;

	// Links to other objects.
	protected volatile transient PypeReportMessage pypeReportMessage;
	protected volatile transient ReportInfo reportInfo;
	protected volatile transient ReportInstanceProcessorInfo reportInstanceProcessorInfo;
	protected transient Hub<ReportInstanceProcessStep> hubReportInstanceProcessSteps;
	protected volatile transient StatusInfo statusInfo;
	protected volatile transient StoreInfo storeInfo;
	protected volatile transient ThreadInfo threadInfo;

	public ReportInstanceProcess() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
		getStatusInfo(); // have it autoCreated
	}

	public ReportInstanceProcess(int id) {
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

	@OAProperty(displayLength = 6, uiColumnLength = 7)
	@OAColumn(name = "counter", sqlType = java.sql.Types.INTEGER)
	public int getCounter() {
		return counter;
	}

	public void setCounter(int newValue) {
		int old = counter;
		fireBeforePropertyChange(P_Counter, old, newValue);
		this.counter = newValue;
		firePropertyChange(P_Counter, old, this.counter);
	}

	@OAProperty(displayLength = 15, ignoreTimeZone = true)
	@OAColumn(name = "completed", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getCompleted() {
		return completed;
	}

	public void setCompleted(OADateTime newValue) {
		OADateTime old = completed;
		fireBeforePropertyChange(P_Completed, old, newValue);
		this.completed = newValue;
		firePropertyChange(P_Completed, old, this.completed);
	}

	@OAProperty(displayName = "Fixed Json", displayLength = 5, uiColumnLength = 10)
	@OAColumn(name = "fixed_json", sqlType = java.sql.Types.BOOLEAN)
	public boolean getFixedJson() {
		return fixedJson;
	}

	public boolean isFixedJson() {
		return getFixedJson();
	}

	public void setFixedJson(boolean newValue) {
		boolean old = fixedJson;
		fireBeforePropertyChange(P_FixedJson, old, newValue);
		this.fixedJson = newValue;
		firePropertyChange(P_FixedJson, old, this.fixedJson);
	}

	@OAProperty(displayName = "Pype Error", displayLength = 5, uiColumnLength = 10)
	@OAColumn(name = "pype_error", sqlType = java.sql.Types.BOOLEAN)
	public boolean getPypeError() {
		return pypeError;
	}

	public boolean isPypeError() {
		return getPypeError();
	}

	public void setPypeError(boolean newValue) {
		boolean old = pypeError;
		fireBeforePropertyChange(P_PypeError, old, newValue);
		this.pypeError = newValue;
		firePropertyChange(P_PypeError, old, this.pypeError);
	}

	@OAProperty(displayName = "Processing Error", displayLength = 5, uiColumnLength = 16)
	@OAColumn(name = "processing_error", sqlType = java.sql.Types.BOOLEAN)
	public boolean getProcessingError() {
		return processingError;
	}

	public boolean isProcessingError() {
		return getProcessingError();
	}

	public void setProcessingError(boolean newValue) {
		boolean old = processingError;
		fireBeforePropertyChange(P_ProcessingError, old, newValue);
		this.processingError = newValue;
		firePropertyChange(P_ProcessingError, old, this.processingError);
	}

	@OAOne(displayName = "Pype Report Message", reverseName = PypeReportMessage.P_ReportInstanceProcess, fkeys = {
			@OAFkey(fromProperty = P_PypeReportMessageId, toProperty = PypeReportMessage.P_Id) })
	public PypeReportMessage getPypeReportMessage() {
		if (pypeReportMessage == null) {
			pypeReportMessage = (PypeReportMessage) getObject(P_PypeReportMessage);
		}
		return pypeReportMessage;
	}

	public void setPypeReportMessage(PypeReportMessage newValue) {
		PypeReportMessage old = this.pypeReportMessage;
		fireBeforePropertyChange(P_PypeReportMessage, old, newValue);
		this.pypeReportMessage = newValue;
		firePropertyChange(P_PypeReportMessage, old, this.pypeReportMessage);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "pype_report_message_id")
	public Long getPypeReportMessageId() {
		return (Long) getFkeyProperty(P_PypeReportMessageId);
	}

	public void setPypeReportMessageId(Long newValue) {
		this.pypeReportMessage = null;
		setFkeyProperty(P_PypeReportMessageId, newValue);
	}

	@OAOne(displayName = "Report Info", reverseName = ReportInfo.P_ReportInstanceProcesses, allowCreateNew = false, equalPropertyPath = P_ReportInstanceProcessorInfo, selectFromPropertyPath = P_ReportInstanceProcessorInfo
			+ "."
			+ ReportInstanceProcessorInfo.P_ReportInfos, fkeys = { @OAFkey(fromProperty = P_ReportInfoId, toProperty = ReportInfo.P_Id) })
	public ReportInfo getReportInfo() {
		if (reportInfo == null) {
			reportInfo = (ReportInfo) getObject(P_ReportInfo);
		}
		return reportInfo;
	}

	public void setReportInfo(ReportInfo newValue) {
		ReportInfo old = this.reportInfo;
		fireBeforePropertyChange(P_ReportInfo, old, newValue);
		this.reportInfo = newValue;
		firePropertyChange(P_ReportInfo, old, this.reportInfo);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "report_info_id")
	public Integer getReportInfoId() {
		return (Integer) getFkeyProperty(P_ReportInfoId);
	}

	public void setReportInfoId(Integer newValue) {
		this.reportInfo = null;
		setFkeyProperty(P_ReportInfoId, newValue);
	}

	@OAOne(displayName = "Report Instance Processor Info", reverseName = ReportInstanceProcessorInfo.P_ReportInstanceProcesses, required = true, allowCreateNew = false, fkeys = {
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

	@OAMany(displayName = "Report Instance Process Steps", toClass = ReportInstanceProcessStep.class, owner = true, reverseName = ReportInstanceProcessStep.P_ReportInstanceProcess, cascadeSave = true, cascadeDelete = true, matchProperty = ReportInstanceProcessStep.P_ProcessStep)
	public Hub<ReportInstanceProcessStep> getReportInstanceProcessSteps() {
		if (hubReportInstanceProcessSteps == null) {
			Hub<ProcessStep> hubMatch = com.auto.dev.reportercorp.delegate.ModelDelegate.getProcessSteps();
			hubReportInstanceProcessSteps = (Hub<ReportInstanceProcessStep>) getHub(P_ReportInstanceProcessSteps, hubMatch);
		}
		return hubReportInstanceProcessSteps;
	}

	@OAOne(displayName = "Status Info", reverseName = StatusInfo.P_ReportInstanceProcess, required = true, autoCreateNew = true, fkeys = {
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

	@OAOne(displayName = "Store Info", reverseName = StoreInfo.P_ReportInstanceProcesses, allowCreateNew = false, equalPropertyPath = P_ReportInstanceProcessorInfo
			+ "." + ReportInstanceProcessorInfo.P_ReporterCorp, selectFromPropertyPath = P_ReportInstanceProcessorInfo + "."
					+ ReportInstanceProcessorInfo.P_ReporterCorp + "."
					+ ReporterCorp.P_StoreInfos, fkeys = { @OAFkey(fromProperty = P_StoreInfoId, toProperty = StoreInfo.P_Id) })
	public StoreInfo getStoreInfo() {
		if (storeInfo == null) {
			storeInfo = (StoreInfo) getObject(P_StoreInfo);
		}
		return storeInfo;
	}

	public void setStoreInfo(StoreInfo newValue) {
		StoreInfo old = this.storeInfo;
		fireBeforePropertyChange(P_StoreInfo, old, newValue);
		this.storeInfo = newValue;
		firePropertyChange(P_StoreInfo, old, this.storeInfo);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "store_info_id")
	public Integer getStoreInfoId() {
		return (Integer) getFkeyProperty(P_StoreInfoId);
	}

	public void setStoreInfoId(Integer newValue) {
		this.storeInfo = null;
		setFkeyProperty(P_StoreInfoId, newValue);
	}

	@OAOne(displayName = "Thread Info", reverseName = ThreadInfo.P_ReportInstanceProcesses, allowCreateNew = false, equalPropertyPath = P_ReportInstanceProcessorInfo
			+ "." + ReportInstanceProcessorInfo.P_ReporterCorp, selectFromPropertyPath = P_ReportInstanceProcessorInfo + "."
					+ ReportInstanceProcessorInfo.P_ReporterCorp + "."
					+ ReporterCorp.P_ThreadInfos, fkeys = { @OAFkey(fromProperty = P_ThreadInfoId, toProperty = ThreadInfo.P_Id) })
	public ThreadInfo getThreadInfo() {
		if (threadInfo == null) {
			threadInfo = (ThreadInfo) getObject(P_ThreadInfo);
		}
		return threadInfo;
	}

	public void setThreadInfo(ThreadInfo newValue) {
		ThreadInfo old = this.threadInfo;
		fireBeforePropertyChange(P_ThreadInfo, old, newValue);
		this.threadInfo = newValue;
		firePropertyChange(P_ThreadInfo, old, this.threadInfo);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "thread_info_id")
	public Integer getThreadInfoId() {
		return (Integer) getFkeyProperty(P_ThreadInfoId);
	}

	public void setThreadInfoId(Integer newValue) {
		this.threadInfo = null;
		setFkeyProperty(P_ThreadInfoId, newValue);
	}

	@OAMethod(displayName = "Convert To Json")
	public void convertToJson() {
		ReportInstanceProcessDelegate.convertToJson(this);
	}

	public static void convertToJson(Hub<ReportInstanceProcess> hub) {
		ReportInstanceProcessDelegate.convertToJson(hub);
	}

}
