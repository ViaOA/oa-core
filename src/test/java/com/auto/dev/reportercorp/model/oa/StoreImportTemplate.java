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

@OAClass(lowerName = "storeImportTemplate", pluralName = "StoreImportTemplates", shortName = "sit", displayName = "Store Import Template", useDataSource = false, isProcessed = true, displayProperty = "id", noPojo = true)
public class StoreImportTemplate extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(StoreImportTemplate.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_Md5hash = "md5hash";
	public static final String P_Template = "template";
	public static final String P_UpdateType = "updateType";
	public static final String P_UpdateTypeString = "updateTypeString";
	public static final String P_UpdateTypeEnum = "updateTypeEnum";
	public static final String P_UpdateTypeDisplay = "updateTypeDisplay";
	public static final String P_Updated = "updated";

	public static final String P_ReportTemplate = "reportTemplate";
	public static final String P_ReportTemplateId = "reportTemplateId"; // fkey
	public static final String P_StoreImport = "storeImport";
	public static final String P_StoreImportId = "storeImportId"; // fkey

	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile String md5hash;
	protected volatile transient byte[] template;
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
	protected volatile transient ReportTemplate reportTemplate;
	protected volatile transient StoreImport storeImport;

	public StoreImportTemplate() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public StoreImportTemplate(int id) {
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

	@OAProperty(maxLength = 32, displayLength = 32, uiColumnLength = 20)
	@OAColumn(name = "md5_hash", maxLength = 32, lowerName = "md5_hashLower")
	public String getMd5hash() {
		return md5hash;
	}

	public void setMd5hash(String newValue) {
		String old = md5hash;
		fireBeforePropertyChange(P_Md5hash, old, newValue);
		this.md5hash = newValue;
		firePropertyChange(P_Md5hash, old, this.md5hash);
	}

	@OAProperty(displayName = "Jasper Template", isBlob = true, uiColumnLength = 15)
	@OAColumn(name = "template", sqlType = java.sql.Types.BLOB)
	public byte[] getTemplate() {
		if (template == null) {
			template = getBlob(P_Template);
		}
		return template;
	}

	public void setTemplate(byte[] newValue) {
		byte[] old = template;
		fireBeforePropertyChange(P_Template, old, newValue);
		this.template = newValue;
		firePropertyChange(P_Template, old, this.template);
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

	@OAOne(displayName = "Report Template", reverseName = ReportTemplate.P_StoreImportTemplates, allowCreateNew = false, fkeys = {
			@OAFkey(fromProperty = P_ReportTemplateId, toProperty = ReportTemplate.P_Id) })
	public ReportTemplate getReportTemplate() {
		if (reportTemplate == null) {
			reportTemplate = (ReportTemplate) getObject(P_ReportTemplate);
		}
		return reportTemplate;
	}

	public void setReportTemplate(ReportTemplate newValue) {
		ReportTemplate old = this.reportTemplate;
		fireBeforePropertyChange(P_ReportTemplate, old, newValue);
		this.reportTemplate = newValue;
		firePropertyChange(P_ReportTemplate, old, this.reportTemplate);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "report_template_id")
	public Integer getReportTemplateId() {
		return (Integer) getFkeyProperty(P_ReportTemplateId);
	}

	public void setReportTemplateId(Integer newValue) {
		this.reportTemplate = null;
		setFkeyProperty(P_ReportTemplateId, newValue);
	}

	@OAOne(displayName = "Store Import", reverseName = StoreImport.P_StoreImportTemplates, required = true, isProcessed = true, allowCreateNew = false, fkeys = {
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
