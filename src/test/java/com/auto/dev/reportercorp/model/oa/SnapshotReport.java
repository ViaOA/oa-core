package com.auto.dev.reportercorp.model.oa;

import java.util.logging.Logger;

import com.viaoa.annotation.OACalculatedProperty;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAObjCallback;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAString;

@OAClass(lowerName = "snapshotReport", pluralName = "SnapshotReports", shortName = "snr", displayName = "Snapshot Report", useDataSource = false, displayProperty = "id", sortProperty = "fileName", noPojo = true)
public class SnapshotReport extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(SnapshotReport.class.getName());

	public static final String P_Id = "id";
	public static final String P_RealId = "realId";
	public static final String P_Created = "created";
	public static final String P_Name = "name";
	public static final String P_Title = "title";
	public static final String P_Description = "description";
	public static final String P_Composite = "composite";
	public static final String P_FileName = "fileName";
	public static final String P_Type = "type";
	public static final String P_TypeInt = "typeInt";
	public static final String P_TypeEnum = "typeEnum";
	public static final String P_TypeDisplay = "typeDisplay";
	public static final String P_Packet = "packet";
	public static final String P_Verified = "verified";
	public static final String P_Display = "display";
	public static final String P_Category = "category";
	public static final String P_RetentionDays = "retentionDays";
	public static final String P_SubReportSeq = "subReportSeq";

	public static final String P_EnvironmentSnapshot = "environmentSnapshot";
	public static final String P_EnvironmentSnapshotId = "environmentSnapshotId"; // fkey
	public static final String P_ParentSnapshotReport = "parentSnapshotReport";
	public static final String P_ParentSnapshotReportId = "parentSnapshotReportId"; // fkey
	public static final String P_SnapshotReports = "snapshotReports";
	public static final String P_SnapshotReportTemplates = "snapshotReportTemplates";

	protected volatile int id;
	protected volatile int realId;
	protected volatile OADateTime created;
	protected volatile String name;
	protected volatile String title;
	protected volatile String description;
	protected volatile boolean composite;
	protected volatile String fileName;
	protected volatile String type;

	public static enum Type {
		DayEnd("Day End"),
		MonthEnd("Month End"),
		YearEnd("Year End"),
		AdHoc("Ad Hoc"),
		NightlyTransmit("Nightly Transmit"),
		AnnualInventory("Annual Inventory"),
		SubReport("SubReport"),
		Hub("Hub"),
		Other("Other");

		private String display;

		Type(String display) {
			this.display = display;
		}

		public String getDisplay() {
			return display;
		}
	}

	public static final int TYPE_DayEnd = 0;
	public static final int TYPE_MonthEnd = 1;
	public static final int TYPE_YearEnd = 2;
	public static final int TYPE_AdHoc = 3;
	public static final int TYPE_NightlyTransmit = 4;
	public static final int TYPE_AnnualInventory = 5;
	public static final int TYPE_SubReport = 6;
	public static final int TYPE_Hub = 7;
	public static final int TYPE_Other = 8;

	protected volatile String packet;
	protected volatile OADate verified;
	protected volatile String display;
	protected volatile int category;
	protected volatile int retentionDays;
	protected volatile int subReportSeq;

	// Links to other objects.
	protected volatile transient EnvironmentSnapshot environmentSnapshot;
	protected volatile transient SnapshotReport parentSnapshotReport;
	protected transient Hub<SnapshotReport> hubSnapshotReports;
	protected transient Hub<SnapshotReportTemplate> hubSnapshotReportTemplates;

	public SnapshotReport() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public SnapshotReport(int id) {
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

	@OAProperty(displayName = "Real Id", displayLength = 6, uiColumnLength = 7)
	@OAColumn(name = "real_id", sqlType = java.sql.Types.INTEGER)
	public int getRealId() {
		return realId;
	}

	public void setRealId(int newValue) {
		int old = realId;
		fireBeforePropertyChange(P_RealId, old, newValue);
		this.realId = newValue;
		firePropertyChange(P_RealId, old, this.realId);
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

	@OAProperty(maxLength = 125, displayLength = 25, uiColumnLength = 20)
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

	@OAProperty(description = "is this report used to group other reports together?", trackPrimitiveNull = false, displayLength = 5, uiColumnLength = 9)
	@OAColumn(name = "composite", sqlType = java.sql.Types.BOOLEAN)
	/**
	 * is this report used to group other reports together?
	 */
	public boolean getComposite() {
		return composite;
	}

	public boolean isComposite() {
		return getComposite();
	}

	public void setComposite(boolean newValue) {
		boolean old = composite;
		fireBeforePropertyChange(P_Composite, old, newValue);
		this.composite = newValue;
		firePropertyChange(P_Composite, old, this.composite);
	}

	@OAProperty(displayName = "File Name", maxLength = 75, displayLength = 35, uiColumnLength = 20)
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

	@OAProperty(maxLength = 25, displayLength = 15, isNameValue = true)
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

	@OAProperty(enumPropertyName = P_Type)
	public int getTypeInt() {
		Type type = getTypeEnum();
		if (type == null) {
			return -1;
		}
		return type.ordinal();
	}

	public void setTypeInt(int val) {
		if (val < 0 || val >= Type.values().length) {
			setType((String) null);
		} else {
			setType(Type.values()[val].name());
		}
	}

	@OAProperty(enumPropertyName = P_Type)
	public Type getTypeEnum() {
		String val = getType();
		if (OAString.isEmpty(val)) {
			return null;
		}
		for (Type type : Type.values()) {
			if (type.name().equalsIgnoreCase(val)) {
				return type;
			}
		}
		return null;
	}

	public void setTypeEnum(Type val) {
		String sval = (val == null ? null : val.name());
		setType(sval);
	}

	@OACalculatedProperty(enumPropertyName = P_Type, displayName = "Type", displayLength = 15, columnLength = 15, properties = { P_Type })
	public String getTypeDisplay() {
		Type type = getTypeEnum();
		if (type == null) {
			return null;
		}
		return type.getDisplay();
	}

	@OAProperty(maxLength = 15, displayLength = 20)
	@OAColumn(name = "packet", maxLength = 15)
	public String getPacket() {
		return packet;
	}

	public void setPacket(String newValue) {
		String old = packet;
		fireBeforePropertyChange(P_Packet, old, newValue);
		this.packet = newValue;
		firePropertyChange(P_Packet, old, this.packet);
	}

	@OAProperty(displayLength = 8)
	@OAColumn(name = "verified", sqlType = java.sql.Types.DATE)
	public OADate getVerified() {
		return verified;
	}

	public void setVerified(OADate newValue) {
		OADate old = verified;
		fireBeforePropertyChange(P_Verified, old, newValue);
		this.verified = newValue;
		firePropertyChange(P_Verified, old, this.verified);
	}

	@OAProperty(maxLength = 25, displayLength = 15)
	@OAColumn(name = "display", maxLength = 25)
	public String getDisplay() {
		return display;
	}

	public void setDisplay(String newValue) {
		String old = display;
		fireBeforePropertyChange(P_Display, old, newValue);
		this.display = newValue;
		firePropertyChange(P_Display, old, this.display);
	}

	@OAProperty(displayLength = 6, uiColumnLength = 8)
	@OAColumn(name = "category", sqlType = java.sql.Types.INTEGER)
	public int getCategory() {
		return category;
	}

	public void setCategory(int newValue) {
		int old = category;
		fireBeforePropertyChange(P_Category, old, newValue);
		this.category = newValue;
		firePropertyChange(P_Category, old, this.category);
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

	@OAProperty(displayName = "Sub Report Seq", trackPrimitiveNull = false, displayLength = 6, uiColumnLength = 14, isAutoSeq = true)
	@OAColumn(name = "sub_report_seq", sqlType = java.sql.Types.INTEGER)
	public int getSubReportSeq() {
		return subReportSeq;
	}

	public void setSubReportSeq(int newValue) {
		int old = subReportSeq;
		fireBeforePropertyChange(P_SubReportSeq, old, newValue);
		this.subReportSeq = newValue;
		firePropertyChange(P_SubReportSeq, old, this.subReportSeq);
	}

	@OAOne(displayName = "Environment Snapshot", reverseName = EnvironmentSnapshot.P_SnapshotReports, required = true, allowCreateNew = false, fkeys = {
			@OAFkey(fromProperty = P_EnvironmentSnapshotId, toProperty = EnvironmentSnapshot.P_Id) })
	public EnvironmentSnapshot getEnvironmentSnapshot() {
		if (environmentSnapshot == null) {
			environmentSnapshot = (EnvironmentSnapshot) getObject(P_EnvironmentSnapshot);
		}
		return environmentSnapshot;
	}

	public void setEnvironmentSnapshot(EnvironmentSnapshot newValue) {
		EnvironmentSnapshot old = this.environmentSnapshot;
		fireBeforePropertyChange(P_EnvironmentSnapshot, old, newValue);
		this.environmentSnapshot = newValue;
		firePropertyChange(P_EnvironmentSnapshot, old, this.environmentSnapshot);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "environment_snapshot_id")
	public Integer getEnvironmentSnapshotId() {
		return (Integer) getFkeyProperty(P_EnvironmentSnapshotId);
	}

	public void setEnvironmentSnapshotId(Integer newValue) {
		this.environmentSnapshot = null;
		setFkeyProperty(P_EnvironmentSnapshotId, newValue);
	}

	@OAOne(displayName = "Parent Snapshot Report", reverseName = SnapshotReport.P_SnapshotReports, required = true, allowCreateNew = false, fkeys = {
			@OAFkey(fromProperty = P_ParentSnapshotReportId, toProperty = SnapshotReport.P_Id) })
	public SnapshotReport getParentSnapshotReport() {
		if (parentSnapshotReport == null) {
			parentSnapshotReport = (SnapshotReport) getObject(P_ParentSnapshotReport);
		}
		return parentSnapshotReport;
	}

	public void setParentSnapshotReport(SnapshotReport newValue) {
		SnapshotReport old = this.parentSnapshotReport;
		fireBeforePropertyChange(P_ParentSnapshotReport, old, newValue);
		this.parentSnapshotReport = newValue;
		firePropertyChange(P_ParentSnapshotReport, old, this.parentSnapshotReport);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "parent_snapshot_report_id")
	public Integer getParentSnapshotReportId() {
		return (Integer) getFkeyProperty(P_ParentSnapshotReportId);
	}

	public void setParentSnapshotReportId(Integer newValue) {
		this.parentSnapshotReport = null;
		setFkeyProperty(P_ParentSnapshotReportId, newValue);
	}

	@OAMany(displayName = "Snapshot Reports", toClass = SnapshotReport.class, owner = true, reverseName = SnapshotReport.P_ParentSnapshotReport, cascadeSave = true, cascadeDelete = true, seqProperty = SnapshotReport.P_SubReportSeq, sortProperty = SnapshotReport.P_SubReportSeq)
	public Hub<SnapshotReport> getSnapshotReports() {
		if (hubSnapshotReports == null) {
			hubSnapshotReports = (Hub<SnapshotReport>) getHub(P_SnapshotReports);
		}
		return hubSnapshotReports;
	}

	@OAObjCallback(enabledProperty = SnapshotReport.P_ParentSnapshotReport, enabledValue = false)
	public void snapshotReportsCallback(OAObjectCallback cb) {
		if (cb == null) {
			return;
		}
		switch (cb.getType()) {
		}
	}

	@OAMany(displayName = "Snapshot Report Templates", toClass = SnapshotReportTemplate.class, owner = true, reverseName = SnapshotReportTemplate.P_SnapshotReport, cascadeSave = true, cascadeDelete = true)
	public Hub<SnapshotReportTemplate> getSnapshotReportTemplates() {
		if (hubSnapshotReportTemplates == null) {
			hubSnapshotReportTemplates = (Hub<SnapshotReportTemplate>) getHub(P_SnapshotReportTemplates);
		}
		return hubSnapshotReportTemplates;
	}
}
