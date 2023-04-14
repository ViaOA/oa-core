package com.auto.dev.reportercorp.model.oa;

import java.util.logging.Logger;

import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.object.OAObject;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;

@OAClass(lowerName = "reportInstanceData", pluralName = "ReportInstanceDatas", shortName = "rid", displayName = "Report Instance Data", useDataSource = false, displayProperty = "id", sortProperty = "id")
public class ReportInstanceData extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(ReportInstanceData.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_Type = "type";
	public static final String P_PrintNow = "printNow";
	public static final String P_PrinterId = "printerId";
	public static final String P_ReportData = "reportData";
	public static final String P_ReportName = "reportName";
	public static final String P_StoreNumber = "storeNumber";
	public static final String P_BusinessDate = "businessDate";
	public static final String P_ProcessingDate = "processingDate";
	public static final String P_AdditionalParameters = "additionalParameters";

	public static final String P_CalcReportInstance = "calcReportInstance";
	public static final String P_CalcReportInstanceId = "calcReportInstanceId"; // fkey

	protected volatile long id;
	protected volatile OADateTime created;
	protected volatile String type;
	protected volatile boolean printNow;
	protected volatile String printerId;
	protected volatile String reportData;
	protected volatile String reportName;
	protected volatile int storeNumber;
	protected volatile OADate businessDate;
	protected volatile OADate processingDate;
	protected volatile String additionalParameters;

	public ReportInstanceData() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public ReportInstanceData(long id) {
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

	@OAProperty(maxLength = 32, displayLength = 10)
	@OAColumn(name = "type", maxLength = 32)
	public String getType() {
		return type;
	}

	public void setType(String newValue) {
		String old = type;
		fireBeforePropertyChange(P_Type, old, newValue);
		this.type = newValue;
		firePropertyChange(P_Type, old, this.type);
	}

	@OAProperty(displayName = "Print Now", displayLength = 5, uiColumnLength = 9)
	@OAColumn(name = "print_now", sqlType = java.sql.Types.BOOLEAN)
	public boolean getPrintNow() {
		return printNow;
	}

	public boolean isPrintNow() {
		return getPrintNow();
	}

	public void setPrintNow(boolean newValue) {
		boolean old = printNow;
		fireBeforePropertyChange(P_PrintNow, old, newValue);
		this.printNow = newValue;
		firePropertyChange(P_PrintNow, old, this.printNow);
	}

	@OAProperty(displayName = "Printer Id", displayLength = 20)
	@OAColumn(name = "printer_id", maxLength = 0)
	public String getPrinterId() {
		return printerId;
	}

	public void setPrinterId(String newValue) {
		String old = printerId;
		fireBeforePropertyChange(P_PrinterId, old, newValue);
		this.printerId = newValue;
		firePropertyChange(P_PrinterId, old, this.printerId);
	}

	@OAProperty(displayName = "Report Data", displayLength = 30, uiColumnLength = 20, isJson = true)
	@OAColumn(name = "report_data", sqlType = java.sql.Types.CLOB)
	public String getReportData() {
		return reportData;
	}

	public void setReportData(String newValue) {
		String old = reportData;
		fireBeforePropertyChange(P_ReportData, old, newValue);
		this.reportData = newValue;
		firePropertyChange(P_ReportData, old, this.reportData);
	}

	@OAProperty(displayName = "Report Name", maxLength = 75, displayLength = 20)
	@OAColumn(name = "report_name", maxLength = 75)
	public String getReportName() {
		return reportName;
	}

	public void setReportName(String newValue) {
		String old = reportName;
		fireBeforePropertyChange(P_ReportName, old, newValue);
		this.reportName = newValue;
		firePropertyChange(P_ReportName, old, this.reportName);
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

	@OAProperty(displayName = "Business Date", displayLength = 8, uiColumnLength = 13, format = "yyyyMMdd")
	@OAColumn(name = "business_date", sqlType = java.sql.Types.DATE)
	public OADate getBusinessDate() {
		return businessDate;
	}

	public void setBusinessDate(OADate newValue) {
		OADate old = businessDate;
		fireBeforePropertyChange(P_BusinessDate, old, newValue);
		this.businessDate = newValue;
		firePropertyChange(P_BusinessDate, old, this.businessDate);
	}

	@OAProperty(displayName = "Processing Date", displayLength = 8, uiColumnLength = 15, format = "yyyyMMdd")
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

	@OAProperty(displayName = "Additional Parameters", displayLength = 30, uiColumnLength = 21, isJson = true)
	@OAColumn(name = "additional_parameters", sqlType = java.sql.Types.CLOB)
	public String getAdditionalParameters() {
		return additionalParameters;
	}

	public void setAdditionalParameters(String newValue) {
		String old = additionalParameters;
		fireBeforePropertyChange(P_AdditionalParameters, old, newValue);
		this.additionalParameters = newValue;
		firePropertyChange(P_AdditionalParameters, old, this.additionalParameters);
	}

	@OAOne(displayName = "Report Instance", isCalculated = true, reverseName = ReportInstance.P_CalcReportInstanceData, allowCreateNew = false, allowAddExisting = false, fkeys = {
			@OAFkey(fromProperty = P_CalcReportInstanceId, toProperty = ReportInstance.P_Id) })
	private ReportInstance getCalcReportInstance() {
		// oamodel has createMethod set to false, this method exists only for annotations.
		return null;
	}
}
