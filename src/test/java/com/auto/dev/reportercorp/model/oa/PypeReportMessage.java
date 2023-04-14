package com.auto.dev.reportercorp.model.oa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import com.auto.dev.reportercorp.delegate.oa.PypeReportMessageDelegate;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAIndex;
import com.viaoa.annotation.OAIndexColumn;
import com.viaoa.annotation.OAMethod;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.annotation.OATable;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;

@OAClass(lowerName = "pypeReportMessage", pluralName = "PypeReportMessages", shortName = "prm", displayName = "Pype Report Message", isProcessed = true, displayProperty = "id")
@OATable(name = "pype_report_message", indexes = {
		@OAIndex(name = "pype_report_message_filename", columns = { @OAIndexColumn(name = "filename", lowerName = "filenameLower") }),
		@OAIndex(name = "pype_report_message_report_instance", fkey = true, columns = { @OAIndexColumn(name = "report_instance_id") })
})
public class PypeReportMessage extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(PypeReportMessage.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_Store = "store";
	public static final String P_ProcessingDate = "processingDate";
	public static final String P_Title = "title";
	public static final String P_Template = "template";
	public static final String P_Filename = "filename";
	public static final String P_Data = "data";
	public static final String P_Subreports = "subreports";
	public static final String P_ConvertedDate = "convertedDate";
	public static final String P_Status = "status";

	public static final String P_ReportInstance = "reportInstance";
	public static final String P_ReportInstanceId = "reportInstanceId"; // fkey
	public static final String P_ReportInstanceProcess = "reportInstanceProcess";

	public static final String M_SendToPypeEndpoint = "sendToPypeEndpoint";
	protected volatile long id;
	protected volatile OADateTime created;
	protected volatile int store;
	protected volatile OADate processingDate;
	protected volatile String title;
	protected volatile String template;
	protected volatile String filename;
	protected volatile String data;
	protected volatile String subreports;
	protected volatile OADateTime convertedDate;
	protected volatile String status;

	// Links to other objects.
	protected volatile transient ReportInstance reportInstance;

	public PypeReportMessage() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public PypeReportMessage(long id) {
		this();
		setId(id);
	}

	@OAProperty(isUnique = true, trackPrimitiveNull = false, displayLength = 6)
	@OAId
	@OAColumn(name = "id", sqlType = java.sql.Types.BIGINT)
	public long getId() {
		return id;
	}

	public void setId(long newValue) {
		long old = id;
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

	@OAProperty(displayLength = 6, importMatch = true)
	@OAColumn(name = "store", sqlType = java.sql.Types.INTEGER)
	public int getStore() {
		return store;
	}

	public void setStore(int newValue) {
		int old = store;
		fireBeforePropertyChange(P_Store, old, newValue);
		this.store = newValue;
		firePropertyChange(P_Store, old, this.store);
	}

	@OAProperty(displayName = "Processing Date", displayLength = 8, uiColumnLength = 15)
	@OAColumn(name = "processing_date", sqlType = java.sql.Types.DATE)
	public OADate getProcessingDate() {
		return processingDate;
	}

	public void setProcessingDate(OADate newValue) {
		OADate old = processingDate;
		fireBeforePropertyChange(P_ProcessingDate, old, newValue);
		this.processingDate = newValue;
		firePropertyChange(P_ProcessingDate, old, this.processingDate);
	}

	@OAProperty(maxLength = 75, displayLength = 25, uiColumnLength = 20)
	@OAColumn(name = "title", maxLength = 75)
	public String getTitle() {
		return title;
	}

	public void setTitle(String newValue) {
		String old = title;
		fireBeforePropertyChange(P_Title, old, newValue);
		this.title = newValue;
		firePropertyChange(P_Title, old, this.title);
	}

	@OAProperty(maxLength = 32, displayLength = 32, uiColumnLength = 20)
	@OAColumn(name = "template", maxLength = 32)
	public String getTemplate() {
		return template;
	}

	public void setTemplate(String newValue) {
		String old = template;
		fireBeforePropertyChange(P_Template, old, newValue);
		this.template = newValue;
		firePropertyChange(P_Template, old, this.template);
	}

	@OAProperty(maxLength = 75, displayLength = 25, uiColumnLength = 20, isFileName = true, importMatch = true)
	@OAColumn(name = "filename", maxLength = 75, lowerName = "filenameLower")
	public String getFilename() {
		return filename;
	}

	public void setFilename(String newValue) {
		String old = filename;
		fireBeforePropertyChange(P_Filename, old, newValue);
		this.filename = newValue;
		firePropertyChange(P_Filename, old, this.filename);
	}

	@OAProperty(displayLength = 30, uiColumnLength = 20, isJson = true)
	@OAColumn(name = "data", sqlType = java.sql.Types.CLOB)
	public String getData() {
		return data;
	}

	public void setData(String newValue) {
		String old = data;
		fireBeforePropertyChange(P_Data, old, newValue);
		this.data = newValue;
		firePropertyChange(P_Data, old, this.data);
	}

	@OAProperty(displayLength = 30, uiColumnLength = 20, isJson = true)
	@OAColumn(name = "subreports", sqlType = java.sql.Types.CLOB)
	/**
	 * map: &#160;reportName : template
	 */
	public String getSubreports() {
		return subreports;
	}

	public void setSubreports(String newValue) {
		String old = subreports;
		fireBeforePropertyChange(P_Subreports, old, newValue);
		this.subreports = newValue;
		firePropertyChange(P_Subreports, old, this.subreports);
	}

	@OAProperty(displayName = "Converted Date", displayLength = 15, ignoreTimeZone = true)
	@OAColumn(name = "converted_date", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getConvertedDate() {
		return convertedDate;
	}

	public void setConvertedDate(OADateTime newValue) {
		OADateTime old = convertedDate;
		fireBeforePropertyChange(P_ConvertedDate, old, newValue);
		this.convertedDate = newValue;
		firePropertyChange(P_ConvertedDate, old, this.convertedDate);
	}

	@OAProperty(maxLength = 125, displayLength = 32, uiColumnLength = 20)
	@OAColumn(name = "status", maxLength = 125)
	public String getStatus() {
		return status;
	}

	public void setStatus(String newValue) {
		String old = status;
		fireBeforePropertyChange(P_Status, old, newValue);
		this.status = newValue;
		firePropertyChange(P_Status, old, this.status);
	}

	@OAOne(displayName = "Report Instance", reverseName = ReportInstance.P_PypeReportMessage, isProcessed = true, allowCreateNew = false, allowAddExisting = false, fkeys = {
			@OAFkey(fromProperty = P_ReportInstanceId, toProperty = ReportInstance.P_Id) })
	public ReportInstance getReportInstance() {
		if (reportInstance == null) {
			reportInstance = (ReportInstance) getObject(P_ReportInstance);
		}
		return reportInstance;
	}

	public void setReportInstance(ReportInstance newValue) {
		ReportInstance old = this.reportInstance;
		fireBeforePropertyChange(P_ReportInstance, old, newValue);
		this.reportInstance = newValue;
		firePropertyChange(P_ReportInstance, old, this.reportInstance);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "report_instance_id")
	public Long getReportInstanceId() {
		return (Long) getFkeyProperty(P_ReportInstanceId);
	}

	public void setReportInstanceId(Long newValue) {
		this.reportInstance = null;
		setFkeyProperty(P_ReportInstanceId, newValue);
	}

	@OAOne(displayName = "Report Instance Process", reverseName = ReportInstanceProcess.P_PypeReportMessage, allowCreateNew = false, allowAddExisting = false)
	private ReportInstanceProcess getReportInstanceProcess() {
		// oamodel has createMethod set to false, this method exists only for annotations.
		return null;
	}

	@OAMethod(displayName = "Send To Pype Endpoint")
	public void sendToPypeEndpoint() {
		PypeReportMessageDelegate.sendToPypeEndpoint(this);
	}

	public static void sendToPypeEndpoint(Hub<PypeReportMessage> hub) {
		PypeReportMessageDelegate.sendToPypeEndpoint(hub);
	}

	public void load(ResultSet rs, long id) throws SQLException {
		this.id = id;
		java.sql.Timestamp timestamp;
		timestamp = rs.getTimestamp(2);
		if (timestamp != null) {
			this.created = new OADateTime(timestamp);
		}
		this.store = rs.getInt(3);
		OAObjectInfoDelegate.setPrimitiveNull(this, P_Store, rs.wasNull());
		java.sql.Date date;
		date = rs.getDate(4);
		if (date != null) {
			this.processingDate = new OADate(date);
		}
		this.title = rs.getString(5);
		this.template = rs.getString(6);
		this.filename = rs.getString(7);
		this.data = rs.getString(8);
		this.subreports = rs.getString(9);
		timestamp = rs.getTimestamp(10);
		if (timestamp != null) {
			this.convertedDate = new OADateTime(timestamp);
		}
		this.status = rs.getString(11);
		long reportInstanceFkey = rs.getLong(12);
		setFkeyProperty(P_ReportInstance, rs.wasNull() ? null : reportInstanceFkey);

		this.changedFlag = false;
		this.newFlag = false;
	}
}
