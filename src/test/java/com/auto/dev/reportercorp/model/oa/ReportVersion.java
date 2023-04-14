package com.auto.dev.reportercorp.model.oa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

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
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAString;

@OAClass(lowerName = "reportVersion", pluralName = "ReportVersions", shortName = "rpv", displayName = "Report Version", displayProperty = "calcVersionDisplay", rootTreePropertyPaths = {
		"[Report]." + Report.P_ReportTemplates + "." + ReportTemplate.P_ReportVersions
})
@OATable(name = "report_version", indexes = {
		@OAIndex(name = "report_version_parent_report_version", fkey = true, columns = {
				@OAIndexColumn(name = "parent_report_version_id") }),
		@OAIndex(name = "report_version_report_template", fkey = true, columns = { @OAIndexColumn(name = "report_template_id") })
})
public class ReportVersion extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(ReportVersion.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_Verified = "verified";

	public static final String P_CalcVersionDisplay = "calcVersionDisplay";
	public static final String P_AllowSubReportVersion = "allowSubReportVersion";
	public static final String P_CalcUnique = "calcUnique";

	public static final String P_ParentReportVersion = "parentReportVersion";
	public static final String P_ParentReportVersionId = "parentReportVersionId"; // fkey
	public static final String P_ReportInstances = "reportInstances";
	public static final String P_ReportTemplate = "reportTemplate";
	public static final String P_ReportTemplateId = "reportTemplateId"; // fkey
	public static final String P_SubReportVersions = "subReportVersions";

	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile OADate verified;

	// Links to other objects.
	protected volatile transient ReportVersion parentReportVersion;
	protected volatile transient ReportTemplate reportTemplate;
	protected transient Hub<ReportVersion> hubSubReportVersions;

	public ReportVersion() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public ReportVersion(int id) {
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

	@OAProperty(displayLength = 8, isProcessed = true)
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

	@OACalculatedProperty(displayName = "Version", displayLength = 18, properties = { P_ReportTemplate + "." + ReportTemplate.P_Md5hash,
			P_ReportTemplate + "." + ReportTemplate.P_Report + "." + Report.P_Name })
	public String getCalcVersionDisplay() {
		ReportTemplate rt = this.getReportTemplate();
		if (rt == null) {
			return "";
		}
		Report report = rt.getReport();
		if (report == null) {
			return "";
		}

		String version = OAString.notNull(report.getName());
		version = OAString.concat(version, rt.getCalcMd5hashPrefix(), "v");
		return version;
	}

	@OACalculatedProperty(displayName = "Allow Sub Report Version", displayLength = 5, columnLength = 24, properties = {
			P_ReportTemplate + "." + ReportTemplate.P_Report + "." + Report.P_SubReports })
	public boolean getAllowSubReportVersion() {
		ReportTemplate rt = this.getReportTemplate();
		if (rt == null) {
			return false;
		}
		Report report = rt.getReport();
		if (report == null) {
			return false;
		}
		Hub<Report> hubSubReports = report.getSubReports();
		return hubSubReports.getSize() > 0;
	}

	public boolean isAllowSubReportVersion() {
		return getAllowSubReportVersion();
	}

	@OACalculatedProperty(displayName = "Calc Unique", displayLength = 5, columnLength = 11, properties = { P_ReportTemplate,
			P_SubReportVersions + "." + ReportVersion.P_ReportTemplate })
	public boolean getCalcUnique() {
		return true;
	}

	public boolean isCalcUnique() {
		return getCalcUnique();
	}

	@OAOne(displayName = "Parent Report Version", reverseName = ReportVersion.P_SubReportVersions, allowCreateNew = false, allowAddExisting = false, fkeys = {
			@OAFkey(fromProperty = P_ParentReportVersionId, toProperty = ReportVersion.P_Id) })
	public ReportVersion getParentReportVersion() {
		if (parentReportVersion == null) {
			parentReportVersion = (ReportVersion) getObject(P_ParentReportVersion);
		}
		return parentReportVersion;
	}

	public void setParentReportVersion(ReportVersion newValue) {
		ReportVersion old = this.parentReportVersion;
		fireBeforePropertyChange(P_ParentReportVersion, old, newValue);
		this.parentReportVersion = newValue;
		firePropertyChange(P_ParentReportVersion, old, this.parentReportVersion);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "parent_report_version_id")
	public Integer getParentReportVersionId() {
		return (Integer) getFkeyProperty(P_ParentReportVersionId);
	}

	public void setParentReportVersionId(Integer newValue) {
		this.parentReportVersion = null;
		setFkeyProperty(P_ParentReportVersionId, newValue);
	}

	@OAMany(displayName = "Report Instances", toClass = ReportInstance.class, reverseName = ReportInstance.P_ReportVersion, createMethod = false)
	private Hub<ReportInstance> getReportInstances() {
		// oamodel has createMethod set to false, this method exists only for annotations.
		return null;
	}

	@OAOne(displayName = "Report Template", reverseName = ReportTemplate.P_ReportVersions, required = true, allowCreateNew = false, fkeys = {
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

	@OAMany(displayName = "Sub Report Versions", toClass = ReportVersion.class, reverseName = ReportVersion.P_ParentReportVersion, uniqueProperty = ReportVersion.P_ReportTemplate, selectFromPropertyPath = P_ReportTemplate
			+ "." + ReportTemplate.P_Report + "." + Report.P_SubReports + "." + Report.P_ReportTemplates + "."
			+ ReportTemplate.P_ReportVersions)
	public Hub<ReportVersion> getSubReportVersions() {
		if (hubSubReportVersions == null) {
			hubSubReportVersions = (Hub<ReportVersion>) getHub(P_SubReportVersions);
		}
		return hubSubReportVersions;
	}

	@OAObjCallback(enabledProperty = ReportVersion.P_AllowSubReportVersion)
	public void subReportVersionsCallback(OAObjectCallback cb) {
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
		java.sql.Date date;
		date = rs.getDate(3);
		if (date != null) {
			this.verified = new OADate(date);
		}
		int parentReportVersionFkey = rs.getInt(4);
		setFkeyProperty(P_ParentReportVersion, rs.wasNull() ? null : parentReportVersionFkey);
		int reportTemplateFkey = rs.getInt(5);
		setFkeyProperty(P_ReportTemplate, rs.wasNull() ? null : reportTemplateFkey);

		this.changedFlag = false;
		this.newFlag = false;
	}
}
