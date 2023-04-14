package com.auto.dev.reportercorp.model.oa;

import java.util.logging.Logger;

import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.object.OAObject;
import com.viaoa.util.OADateTime;

@OAClass(lowerName = "reporterCorpVerification", pluralName = "ReporterCorpVerifications", shortName = "rcv", displayName = "Reporter Corp Verification", useDataSource = false, displayProperty = "id", noPojo = true)
public class ReporterCorpVerification extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(ReporterCorpVerification.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";

	public static final String P_EnvironmentVerification = "environmentVerification";
	public static final String P_EnvironmentVerificationId = "environmentVerificationId"; // fkey

	protected volatile int id;
	protected volatile OADateTime created;

	// Links to other objects.
	protected volatile transient EnvironmentVerification environmentVerification;

	public ReporterCorpVerification() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public ReporterCorpVerification(int id) {
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

	@OAOne(displayName = "Environment Verification", reverseName = EnvironmentVerification.P_ReporterCorpVerifications, required = true, allowCreateNew = false, fkeys = {
			@OAFkey(fromProperty = P_EnvironmentVerificationId, toProperty = EnvironmentVerification.P_Id) })
	public EnvironmentVerification getEnvironmentVerification() {
		if (environmentVerification == null) {
			environmentVerification = (EnvironmentVerification) getObject(P_EnvironmentVerification);
		}
		return environmentVerification;
	}

	public void setEnvironmentVerification(EnvironmentVerification newValue) {
		EnvironmentVerification old = this.environmentVerification;
		fireBeforePropertyChange(P_EnvironmentVerification, old, newValue);
		this.environmentVerification = newValue;
		firePropertyChange(P_EnvironmentVerification, old, this.environmentVerification);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "environment_verification_id")
	public Integer getEnvironmentVerificationId() {
		return (Integer) getFkeyProperty(P_EnvironmentVerificationId);
	}

	public void setEnvironmentVerificationId(Integer newValue) {
		this.environmentVerification = null;
		setFkeyProperty(P_EnvironmentVerificationId, newValue);
	}
}
