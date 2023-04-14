package com.auto.dev.reportercorp.model.oa;

import java.util.logging.Logger;

import com.viaoa.annotation.OACalculatedProperty;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.object.OAObject;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAString;

@OAClass(lowerName = "storeImportReport", pluralName = "StoreImportReports", shortName = "sir", displayName = "Store Import Report", useDataSource = false, isProcessed = true, displayProperty = "id", noPojo = true)
public class StoreImportReport extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(StoreImportReport.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_Name = "name";
	public static final String P_FileName = "fileName";
	public static final String P_Title = "title";
	public static final String P_Description = "description";
	public static final String P_RetentionDays = "retentionDays";
	public static final String P_Type = "type";
	public static final String P_Md5hash = "md5hash";
	public static final String P_UpdateType = "updateType";
	public static final String P_UpdateTypeString = "updateTypeString";
	public static final String P_UpdateTypeEnum = "updateTypeEnum";
	public static final String P_UpdateTypeDisplay = "updateTypeDisplay";
	public static final String P_Updated = "updated";

	public static final String P_Report = "report";
	public static final String P_ReportId = "reportId"; // fkey
	public static final String P_StoreImport = "storeImport";
	public static final String P_StoreImportId = "storeImportId"; // fkey

	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile String name;
	protected volatile String fileName;
	protected volatile String title;
	protected volatile String description;
	protected volatile int retentionDays;
	protected volatile String type;
	protected volatile String md5hash;
	protected volatile int updateType;

	public static enum UpdateType {
		None("None"),
		New("New"),
		Update("Update");

		private String display;

		UpdateType(String display) {
			this.display = display;
		}

		public String getDisplay() {
			return display;
		}
	}

	public static final int UPDATETYPE_None = 0;
	public static final int UPDATETYPE_New = 1;
	public static final int UPDATETYPE_Update = 2;

	protected volatile OADateTime updated;

	// Links to other objects.
	protected volatile transient Report report;
	protected volatile transient StoreImport storeImport;

	public StoreImportReport() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public StoreImportReport(int id) {
		this();
		setId(id);
	}

	@OAProperty(isUnique = true, trackPrimitiveNull = false, displayLength = 6)
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

	@OAProperty(maxLength = 125, isUnique = true, displayLength = 25, uiColumnLength = 20, importMatch = true)
	@OAColumn(name = "name", maxLength = 125)
	public String getName() {
		return name;
	}

	public void setName(String newValue) {
		String old = name;
		fireBeforePropertyChange(P_Name, old, newValue);
		this.name = newValue;
		firePropertyChange(P_Name, old, this.name);
	}

	@OAProperty(displayName = "File Name", maxLength = 75, displayLength = 35, uiColumnLength = 20, importMatch = true)
	@OAColumn(name = "file_name", maxLength = 75)
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String newValue) {
		String old = fileName;
		fireBeforePropertyChange(P_FileName, old, newValue);
		this.fileName = newValue;
		firePropertyChange(P_FileName, old, this.fileName);
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

	@OAProperty(maxLength = 150, displayLength = 32, uiColumnLength = 20)
	@OAColumn(name = "description", maxLength = 150)
	public String getDescription() {
		return description;
	}

	public void setDescription(String newValue) {
		String old = description;
		fireBeforePropertyChange(P_Description, old, newValue);
		this.description = newValue;
		firePropertyChange(P_Description, old, this.description);
	}

	@OAProperty(displayName = "Retention Days", displayLength = 6, uiColumnLength = 14)
	@OAColumn(name = "retention_days", sqlType = java.sql.Types.INTEGER)
	public int getRetentionDays() {
		return retentionDays;
	}

	public void setRetentionDays(int newValue) {
		int old = retentionDays;
		fireBeforePropertyChange(P_RetentionDays, old, newValue);
		this.retentionDays = newValue;
		firePropertyChange(P_RetentionDays, old, this.retentionDays);
	}

	@OAProperty(maxLength = 25, displayLength = 15)
	@OAColumn(name = "type", maxLength = 25)
	public String getType() {
		return type;
	}

	public void setType(String newValue) {
		String old = type;
		fireBeforePropertyChange(P_Type, old, newValue);
		this.type = newValue;
		firePropertyChange(P_Type, old, this.type);
	}

	@OAProperty(maxLength = 32, displayLength = 20)
	@OAColumn(name = "md5_hash", maxLength = 32)
	public String getMd5hash() {
		return md5hash;
	}

	public void setMd5hash(String newValue) {
		String old = md5hash;
		fireBeforePropertyChange(P_Md5hash, old, newValue);
		this.md5hash = newValue;
		firePropertyChange(P_Md5hash, old, this.md5hash);
	}

	@OAProperty(displayName = "Update Type", trackPrimitiveNull = false, displayLength = 6, uiColumnLength = 11, isNameValue = true)
	@OAColumn(name = "update_type", sqlType = java.sql.Types.INTEGER)
	public int getUpdateType() {
		return updateType;
	}

	public void setUpdateType(int newValue) {
		int old = updateType;
		fireBeforePropertyChange(P_UpdateType, old, newValue);
		this.updateType = newValue;
		firePropertyChange(P_UpdateType, old, this.updateType);
	}

	@OAProperty(enumPropertyName = P_UpdateType)
	public String getUpdateTypeString() {
		UpdateType updateType = getUpdateTypeEnum();
		if (updateType == null) {
			return null;
		}
		return updateType.name();
	}

	public void setUpdateTypeString(String val) {
		int x = -1;
		if (OAString.isNotEmpty(val)) {
			UpdateType updateType = UpdateType.valueOf(val);
			if (updateType != null) {
				x = updateType.ordinal();
			}
		}
		if (x < 0) {
			x = 0;
		}
		setUpdateType(x);
	}

	@OAProperty(enumPropertyName = P_UpdateType)
	public UpdateType getUpdateTypeEnum() {
		final int val = getUpdateType();
		if (val < 0 || val >= UpdateType.values().length) {
			return null;
		}
		return UpdateType.values()[val];
	}

	public void setUpdateTypeEnum(UpdateType val) {
		if (val == null) {
			setUpdateType(0);
		} else {
			setUpdateType(val.ordinal());
		}
	}

	@OACalculatedProperty(enumPropertyName = P_UpdateType, displayName = "Update Type", displayLength = 6, columnLength = 11, properties = {
			P_UpdateType })
	public String getUpdateTypeDisplay() {
		UpdateType updateType = getUpdateTypeEnum();
		if (updateType == null) {
			return null;
		}
		return updateType.getDisplay();
	}

	@OAProperty(displayLength = 15, ignoreTimeZone = true)
	@OAColumn(name = "updated", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getUpdated() {
		return updated;
	}

	public void setUpdated(OADateTime newValue) {
		OADateTime old = updated;
		fireBeforePropertyChange(P_Updated, old, newValue);
		this.updated = newValue;
		firePropertyChange(P_Updated, old, this.updated);
	}

	@OAOne(reverseName = Report.P_StoreImportReports, allowCreateNew = false, fkeys = {
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

	@OAOne(displayName = "Store Import", reverseName = StoreImport.P_StoreImportReports, required = true, isProcessed = true, allowCreateNew = false, allowAddExisting = false, fkeys = {
			@OAFkey(fromProperty = P_StoreImportId, toProperty = StoreImport.P_Id) })
	public StoreImport getStoreImport() {
		if (storeImport == null) {
			storeImport = (StoreImport) getObject(P_StoreImport);
		}
		return storeImport;
	}

	public void setStoreImport(StoreImport newValue) {
		StoreImport old = this.storeImport;
		fireBeforePropertyChange(P_StoreImport, old, newValue);
		this.storeImport = newValue;
		firePropertyChange(P_StoreImport, old, this.storeImport);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "store_import_id")
	public Integer getStoreImportId() {
		return (Integer) getFkeyProperty(P_StoreImportId);
	}

	public void setStoreImportId(Integer newValue) {
		this.storeImport = null;
		setFkeyProperty(P_StoreImportId, newValue);
	}
}
