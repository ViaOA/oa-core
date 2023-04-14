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

@OAClass(lowerName = "snapshotReportVersion", pluralName = "SnapshotReportVersions", shortName = "srv", displayName = "Snapshot Report Version", useDataSource = false, displayProperty = "id", noPojo = true)
public class SnapshotReportVersion extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(SnapshotReportVersion.class.getName());

	public static final String P_Id = "id";
	public static final String P_RealId = "realId";
	public static final String P_Created = "created";
	public static final String P_Verified = "verified";

	public static final String P_ParentSnapshotReportVersion = "parentSnapshotReportVersion";
	public static final String P_ParentSnapshotReportVersionId = "parentSnapshotReportVersionId"; // fkey
	public static final String P_SnapshotReportTemplate = "snapshotReportTemplate";
	public static final String P_SnapshotReportTemplateId = "snapshotReportTemplateId"; // fkey
	public static final String P_SnapshotReportVersions = "snapshotReportVersions";

	protected volatile int id;
	protected volatile int realId;
	protected volatile OADateTime created;
	protected volatile OADate verified;

	// Links to other objects.
	protected volatile transient SnapshotReportVersion parentSnapshotReportVersion;
	protected volatile transient SnapshotReportTemplate snapshotReportTemplate;
	protected transient Hub<SnapshotReportVersion> hubSnapshotReportVersions;

	public SnapshotReportVersion() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public SnapshotReportVersion(int id) {
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

	@OAOne(displayName = "Parent Snapshot Report Version", reverseName = SnapshotReportVersion.P_SnapshotReportVersions, allowCreateNew = false, allowAddExisting = false, fkeys = {
			@OAFkey(fromProperty = P_ParentSnapshotReportVersionId, toProperty = SnapshotReportVersion.P_Id) })
	public SnapshotReportVersion getParentSnapshotReportVersion() {
		if (parentSnapshotReportVersion == null) {
			parentSnapshotReportVersion = (SnapshotReportVersion) getObject(P_ParentSnapshotReportVersion);
		}
		return parentSnapshotReportVersion;
	}

	public void setParentSnapshotReportVersion(SnapshotReportVersion newValue) {
		SnapshotReportVersion old = this.parentSnapshotReportVersion;
		fireBeforePropertyChange(P_ParentSnapshotReportVersion, old, newValue);
		this.parentSnapshotReportVersion = newValue;
		firePropertyChange(P_ParentSnapshotReportVersion, old, this.parentSnapshotReportVersion);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "parent_snapshot_report_version_id")
	public Integer getParentSnapshotReportVersionId() {
		return (Integer) getFkeyProperty(P_ParentSnapshotReportVersionId);
	}

	public void setParentSnapshotReportVersionId(Integer newValue) {
		this.parentSnapshotReportVersion = null;
		setFkeyProperty(P_ParentSnapshotReportVersionId, newValue);
	}

	@OAOne(displayName = "Snapshot Report Template", reverseName = SnapshotReportTemplate.P_SnapshotReportVersions, required = true, allowCreateNew = false, fkeys = {
			@OAFkey(fromProperty = P_SnapshotReportTemplateId, toProperty = SnapshotReportTemplate.P_Id) })
	public SnapshotReportTemplate getSnapshotReportTemplate() {
		if (snapshotReportTemplate == null) {
			snapshotReportTemplate = (SnapshotReportTemplate) getObject(P_SnapshotReportTemplate);
		}
		return snapshotReportTemplate;
	}

	public void setSnapshotReportTemplate(SnapshotReportTemplate newValue) {
		SnapshotReportTemplate old = this.snapshotReportTemplate;
		fireBeforePropertyChange(P_SnapshotReportTemplate, old, newValue);
		this.snapshotReportTemplate = newValue;
		firePropertyChange(P_SnapshotReportTemplate, old, this.snapshotReportTemplate);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "snapshot_report_template_id")
	public Integer getSnapshotReportTemplateId() {
		return (Integer) getFkeyProperty(P_SnapshotReportTemplateId);
	}

	public void setSnapshotReportTemplateId(Integer newValue) {
		this.snapshotReportTemplate = null;
		setFkeyProperty(P_SnapshotReportTemplateId, newValue);
	}

	@OAMany(displayName = "Snapshot Report Versions", toClass = SnapshotReportVersion.class, reverseName = SnapshotReportVersion.P_ParentSnapshotReportVersion, selectFromPropertyPath = P_SnapshotReportTemplate
			+ "." + SnapshotReportTemplate.P_SnapshotReport + "." + SnapshotReport.P_SnapshotReports + "."
			+ SnapshotReport.P_SnapshotReportTemplates + "." + SnapshotReportTemplate.P_SnapshotReportVersions)
	public Hub<SnapshotReportVersion> getSnapshotReportVersions() {
		if (hubSnapshotReportVersions == null) {
			hubSnapshotReportVersions = (Hub<SnapshotReportVersion>) getHub(P_SnapshotReportVersions);
		}
		return hubSnapshotReportVersions;
	}
}
