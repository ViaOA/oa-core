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
import com.viaoa.util.OADateTime;

@OAClass(lowerName = "environmentSnapshot", pluralName = "EnvironmentSnapshots", shortName = "ens", displayName = "Environment Snapshot", useDataSource = false, displayProperty = "id", noPojo = true)
public class EnvironmentSnapshot extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(EnvironmentSnapshot.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";

	public static final String P_Environment = "environment";
	public static final String P_EnvironmentId = "environmentId"; // fkey
	public static final String P_SnapshotReports = "snapshotReports";

	protected volatile int id;
	protected volatile OADateTime created;

	// Links to other objects.
	protected volatile transient Environment environment;
	protected transient Hub<SnapshotReport> hubSnapshotReports;

	public EnvironmentSnapshot() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public EnvironmentSnapshot(int id) {
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

	@OAOne(reverseName = Environment.P_EnvironmentSnapshots, required = true, allowCreateNew = false, fkeys = {
			@OAFkey(fromProperty = P_EnvironmentId, toProperty = Environment.P_Id) })
	public Environment getEnvironment() {
		if (environment == null) {
			environment = (Environment) getObject(P_Environment);
		}
		return environment;
	}

	public void setEnvironment(Environment newValue) {
		Environment old = this.environment;
		fireBeforePropertyChange(P_Environment, old, newValue);
		this.environment = newValue;
		firePropertyChange(P_Environment, old, this.environment);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "environment_id")
	public Integer getEnvironmentId() {
		return (Integer) getFkeyProperty(P_EnvironmentId);
	}

	public void setEnvironmentId(Integer newValue) {
		this.environment = null;
		setFkeyProperty(P_EnvironmentId, newValue);
	}

	@OAMany(displayName = "Snapshot Reports", toClass = SnapshotReport.class, reverseName = SnapshotReport.P_EnvironmentSnapshot)
	public Hub<SnapshotReport> getSnapshotReports() {
		if (hubSnapshotReports == null) {
			hubSnapshotReports = (Hub<SnapshotReport>) getHub(P_SnapshotReports);
		}
		return hubSnapshotReports;
	}
}
