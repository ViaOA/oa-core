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
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;

@OAClass(lowerName = "snapshotReportTemplate", pluralName = "SnapshotReportTemplates", shortName = "srt", displayName = "Snapshot Report Template", useDataSource = false, displayProperty = "id", noPojo = true)
public class SnapshotReportTemplate extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(SnapshotReportTemplate.class.getName());

	public static final String P_Id = "id";
	public static final String P_RealId = "realId";
	public static final String P_Created = "created";
	public static final String P_Md5hash = "md5hash";
	public static final String P_Template = "template";
	public static final String P_Verified = "verified";

	public static final String P_SnapshotReport = "snapshotReport";
	public static final String P_SnapshotReportId = "snapshotReportId"; // fkey
	public static final String P_SnapshotReportVersions = "snapshotReportVersions";

	protected volatile int id;
	protected volatile int realId;
	protected volatile OADateTime created;
	protected volatile String md5hash;
	protected volatile transient byte[] template;
	protected volatile OADate verified;

	// Links to other objects.
	protected volatile transient SnapshotReport snapshotReport;
	protected transient Hub<SnapshotReportVersion> hubSnapshotReportVersions;

	public SnapshotReportTemplate() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public SnapshotReportTemplate(int id) {
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

	@OAProperty(maxLength = 32, displayLength = 32, uiColumnLength = 20)
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

	@OAOne(displayName = "Snapshot Report", reverseName = SnapshotReport.P_SnapshotReportTemplates, required = true, allowCreateNew = false, fkeys = {
			@OAFkey(fromProperty = P_SnapshotReportId, toProperty = SnapshotReport.P_Id) })
	public SnapshotReport getSnapshotReport() {
		if (snapshotReport == null) {
			snapshotReport = (SnapshotReport) getObject(P_SnapshotReport);
		}
		return snapshotReport;
	}

	public void setSnapshotReport(SnapshotReport newValue) {
		SnapshotReport old = this.snapshotReport;
		fireBeforePropertyChange(P_SnapshotReport, old, newValue);
		this.snapshotReport = newValue;
		firePropertyChange(P_SnapshotReport, old, this.snapshotReport);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "snapshot_report_id")
	public Integer getSnapshotReportId() {
		return (Integer) getFkeyProperty(P_SnapshotReportId);
	}

	public void setSnapshotReportId(Integer newValue) {
		this.snapshotReport = null;
		setFkeyProperty(P_SnapshotReportId, newValue);
	}

	@OAMany(displayName = "Snapshot Report Versions", toClass = SnapshotReportVersion.class, owner = true, reverseName = SnapshotReportVersion.P_SnapshotReportTemplate, cascadeSave = true, cascadeDelete = true)
	public Hub<SnapshotReportVersion> getSnapshotReportVersions() {
		if (hubSnapshotReportVersions == null) {
			hubSnapshotReportVersions = (Hub<SnapshotReportVersion>) getHub(P_SnapshotReportVersions);
		}
		return hubSnapshotReportVersions;
	}
}
