package com.auto.dev.reportercorp.model.oa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.filter.ReportMasterOnlyFilter;
import com.auto.dev.reportercorp.model.oa.filter.ReportNeedsTemplateFilter;
import com.viaoa.annotation.OACalculatedProperty;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAIndex;
import com.viaoa.annotation.OAIndexColumn;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAObjCallback;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.annotation.OATable;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAString;

@OAClass(lowerName = "report", pluralName = "Reports", shortName = "rpr", displayName = "Report", isLookup = true, isPreSelect = true, displayProperty = "calcDisplay", sortProperty = "fileName", filterClasses = {
		ReportMasterOnlyFilter.class, ReportNeedsTemplateFilter.class })
@OATable(indexes = {
		@OAIndex(name = "report_name", unique = true, columns = { @OAIndexColumn(name = "name", lowerName = "nameLower") }),
		@OAIndex(name = "report_file_name_composite", unique = true, columns = {
				@OAIndexColumn(name = "file_name", lowerName = "file_nameLower"), @OAIndexColumn(name = "composite") }),
		@OAIndex(name = "report_parent_report", fkey = true, columns = { @OAIndexColumn(name = "parent_report_id") })
})
public class Report extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(Report.class.getName());

	public static final String P_Id = "id";
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

	public static final String P_CalcDisplay = "calcDisplay";

	public static final String P_ParentReport = "parentReport";
	public static final String P_ParentReportId = "parentReportId"; // fkey
	public static final String P_ReportInfos = "reportInfos";
	public static final String P_ReportTemplates = "reportTemplates";
	public static final String P_StoreImportReports = "storeImportReports";
	public static final String P_SubReports = "subReports";

	protected volatile int id;
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
	protected volatile transient Report parentReport;
	protected transient Hub<ReportTemplate> hubReportTemplates;
	protected transient Hub<ReportInstance> hubSearchReportInstances;
	protected transient Hub<Report> hubSubReports;

	public Report() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public Report(int id) {
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
	@OAColumn(name = "name", maxLength = 125, lowerName = "nameLower")
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

	@OAProperty(displayName = "File Name", maxLength = 75, displayLength = 35, uiColumnLength = 20, importMatch = true)
	@OAColumn(name = "file_name", maxLength = 75, lowerName = "file_nameLower")
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

	@OACalculatedProperty(displayName = "Display", displayLength = 25, columnLength = 20, properties = { P_Name, P_FileName, P_ParentReport,
			P_SubReports })
	public String getCalcDisplay() {
		String name = this.getName();
		String suf = "";
		if (OAString.isEmpty(name)) {
			name = getFileName();
			if (OAString.isNotEmpty(name)) {
				suf = "file name";
			}
		}

		if (getParentReportId() != null) {
			suf = OAString.append(suf, "child");
		}

		if (isLoaded(P_SubReports) && getSubReports().size() > 0) {
			suf = OAString.append(suf, "parent");
		}

		String display = OAString.notNull(name);
		if (suf.length() > 0) {
			suf = " (" + suf + ")";
		}

		return name + suf;
	}

	@OAOne(displayName = "Parent Report", reverseName = Report.P_SubReports, required = true, allowCreateNew = false, fkeys = {
			@OAFkey(fromProperty = P_ParentReportId, toProperty = Report.P_Id) })
	public Report getParentReport() {
		if (parentReport == null) {
			parentReport = (Report) getObject(P_ParentReport);
		}
		return parentReport;
	}

	public void setParentReport(Report newValue) {
		Report old = this.parentReport;
		fireBeforePropertyChange(P_ParentReport, old, newValue);
		this.parentReport = newValue;
		firePropertyChange(P_ParentReport, old, this.parentReport);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "parent_report_id")
	public Integer getParentReportId() {
		return (Integer) getFkeyProperty(P_ParentReportId);
	}

	public void setParentReportId(Integer newValue) {
		this.parentReport = null;
		setFkeyProperty(P_ParentReportId, newValue);
	}

	@OAMany(displayName = "Report Infos", toClass = ReportInfo.class, reverseName = ReportInfo.P_Report, createMethod = false)
	private Hub<ReportInfo> getReportInfos() {
		// oamodel has createMethod set to false, this method exists only for annotations.
		return null;
	}

	@OAMany(displayName = "Report Templates", toClass = ReportTemplate.class, owner = true, reverseName = ReportTemplate.P_Report, cascadeSave = true, cascadeDelete = true)
	public Hub<ReportTemplate> getReportTemplates() {
		if (hubReportTemplates == null) {
			hubReportTemplates = (Hub<ReportTemplate>) getHub(P_ReportTemplates);
		}
		return hubReportTemplates;
	}

	@OAMany(displayName = "Store Import Reports", toClass = StoreImportReport.class, reverseName = StoreImportReport.P_Report, createMethod = false)
	private Hub<StoreImportReport> getStoreImportReports() {
		// oamodel has createMethod set to false, this method exists only for annotations.
		return null;
	}

	@OAMany(displayName = "Sub Reports", toClass = Report.class, owner = true, reverseName = Report.P_ParentReport, cascadeSave = true, cascadeDelete = true, seqProperty = Report.P_SubReportSeq, sortProperty = Report.P_SubReportSeq)
	public Hub<Report> getSubReports() {
		if (hubSubReports == null) {
			hubSubReports = (Hub<Report>) getHub(P_SubReports);
		}
		return hubSubReports;
	}

	@OAObjCallback(enabledProperty = Report.P_ParentReport, enabledValue = false)
	public void subReportsCallback(OAObjectCallback cb) {
		if (cb == null) {
			return;
		}
		switch (cb.getType()) {
		}
	}

	public void load(ResultSet rs, int id) throws SQLException {
		this.id = id;
		java.sql.Timestamp timestamp;
		timestamp = rs.getTimestamp(2);
		if (timestamp != null) {
			this.created = new OADateTime(timestamp);
		}
		this.name = rs.getString(3);
		this.title = rs.getString(4);
		this.description = rs.getString(5);
		this.composite = rs.getBoolean(6);
		this.fileName = rs.getString(7);
		this.type = rs.getString(8);
		this.packet = rs.getString(9);
		java.sql.Date date;
		date = rs.getDate(10);
		if (date != null) {
			this.verified = new OADate(date);
		}
		this.display = rs.getString(11);
		this.category = rs.getInt(12);
		OAObjectInfoDelegate.setPrimitiveNull(this, P_Category, rs.wasNull());
		this.retentionDays = rs.getInt(13);
		OAObjectInfoDelegate.setPrimitiveNull(this, P_RetentionDays, rs.wasNull());
		this.subReportSeq = rs.getInt(14);
		int parentReportFkey = rs.getInt(15);
		setFkeyProperty(P_ParentReport, rs.wasNull() ? null : parentReportFkey);

		this.changedFlag = false;
		this.newFlag = false;
	}
}
