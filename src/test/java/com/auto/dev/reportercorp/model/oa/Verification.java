package com.auto.dev.reportercorp.model.oa;

import java.util.logging.Logger;

import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAProperty;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.util.OADateTime;

@OAClass(lowerName = "verification", pluralName = "Verifications", shortName = "vrf", displayName = "Verification", useDataSource = false, displayProperty = "id", noPojo = true)
public class Verification extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(Verification.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";

	public static final String P_EnvironmentVerifications = "environmentVerifications";

	protected volatile int id;
	protected volatile OADateTime created;

	// Links to other objects.
	protected transient Hub<EnvironmentVerification> hubEnvironmentVerifications;

	public Verification() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public Verification(int id) {
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

	@OAMany(displayName = "Environment Verifications", toClass = EnvironmentVerification.class, owner = true, reverseName = EnvironmentVerification.P_Verification, cascadeSave = true, cascadeDelete = true)
	public Hub<EnvironmentVerification> getEnvironmentVerifications() {
		if (hubEnvironmentVerifications == null) {
			hubEnvironmentVerifications = (Hub<EnvironmentVerification>) getHub(P_EnvironmentVerifications);
		}
		return hubEnvironmentVerifications;
	}
}
