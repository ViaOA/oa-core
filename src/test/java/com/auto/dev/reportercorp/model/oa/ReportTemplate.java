package com.auto.dev.reportercorp.model.oa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.filter.ReportTemplateNeedsTemplateFilter;
import com.viaoa.annotation.OACalculatedProperty;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAIndex;
import com.viaoa.annotation.OAIndexColumn;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.annotation.OATable;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAString;

@OAClass(lowerName = "reportTemplate", pluralName = "ReportTemplates", shortName = "rpt", displayName = "Report Template", displayProperty = "calcMd5hashPrefix", filterClasses = {
		ReportTemplateNeedsTemplateFilter.class }, rootTreePropertyPaths = {
				"[Report]." + Report.P_ReportTemplates
		})
@OATable(name = "report_template", indexes = {
		@OAIndex(name = "report_template_md5_hash", unique = true, columns = {
				@OAIndexColumn(name = "md5_hash", lowerName = "md5_hashLower") }),
		@OAIndex(name = "report_template_report", fkey = true, columns = { @OAIndexColumn(name = "report_id") })
})
public class ReportTemplate extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(ReportTemplate.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_Md5hash = "md5hash";
	public static final String P_Template = "template";
	public static final String P_Verified = "verified";

	public static final String P_CalcMd5hashPrefix = "calcMd5hashPrefix";
	public static final String P_HasTemplate = "hasTemplate";
	public static final String P_TemplateSize = "templateSize";

	public static final String P_Report = "report";
	public static final String P_ReportId = "reportId"; // fkey
	public static final String P_ReportVersions = "reportVersions";
	public static final String P_StoreImportTemplates = "storeImportTemplates";

	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile String md5hash;
	protected volatile transient byte[] template;
	protected volatile OADate verified;

	// Links to other objects.
	protected volatile transient Report report;
	protected transient Hub<ReportVersion> hubReportVersions;
	protected transient Hub<ReportInstance> hubSearchReportInstances;

	public ReportTemplate() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public ReportTemplate(int id) {
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

	@OAProperty(maxLength = 32, isUnique = true, displayLength = 32, uiColumnLength = 20, importMatch = true)
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

	@OACalculatedProperty(displayName = "Calc Md5hash Prefix", displayLength = 20, properties = { P_Md5hash })
	public String getCalcMd5hashPrefix() {
		String md5hash = this.getMd5hash();
		if (md5hash == null) {
			return "";
		}
		return OAString.substring(md5hash, 0, 5);
	}

	@OACalculatedProperty(displayName = "Has Template", displayLength = 5, columnLength = 12, properties = { P_Template, P_Md5hash })
	public boolean getHasTemplate() {
		String s = getMd5hash();
		if (OAString.isEmpty(s)) {
			return false;
		}
		byte[] template = this.getTemplate();
		return template != null && template.length > 0;
	}

	@OACalculatedProperty(displayName = "Template Size", displayLength = 6, columnLength = 13, properties = { P_Template })
	public int getTemplateSize() {
		byte[] bs = this.getTemplate();
		return (bs == null ? 0 : bs.length);
	}

	@OAOne(reverseName = Report.P_ReportTemplates, required = true, allowCreateNew = false, fkeys = {
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

	@OAMany(displayName = "Report Versions", toClass = ReportVersion.class, owner = true, reverseName = ReportVersion.P_ReportTemplate, cascadeSave = true, cascadeDelete = true)
	public Hub<ReportVersion> getReportVersions() {
		if (hubReportVersions == null) {
			hubReportVersions = (Hub<ReportVersion>) getHub(P_ReportVersions);
		}
		return hubReportVersions;
	}

	@OAMany(displayName = "Store Import Templates", toClass = StoreImportTemplate.class, reverseName = StoreImportTemplate.P_ReportTemplate, createMethod = false)
	private Hub<StoreImportTemplate> getStoreImportTemplates() {
		// oamodel has createMethod set to false, this method exists only for annotations.
		return null;
	}

	public void load(ResultSet rs, int id) throws SQLException {
		this.id = id;
		java.sql.Timestamp timestamp;
		timestamp = rs.getTimestamp(2);
		if (timestamp != null) {
			this.created = new OADateTime(timestamp);
		}
		this.md5hash = rs.getString(3);
		java.sql.Date date;
		date = rs.getDate(4);
		if (date != null) {
			this.verified = new OADate(date);
		}
		int reportFkey = rs.getInt(5);
		setFkeyProperty(P_Report, rs.wasNull() ? null : reportFkey);

		this.changedFlag = false;
		this.newFlag = false;
	}
}
