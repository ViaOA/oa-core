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

@OAClass(lowerName = "environmentVerification", pluralName = "EnvironmentVerifications", shortName = "env", displayName = "Environment Verification", useDataSource = false, displayProperty = "id", noPojo = true)
public class EnvironmentVerification extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(EnvironmentVerification.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";

	public static final String P_ReporterCorpVerifications = "reporterCorpVerifications";
	public static final String P_Verification = "verification";
	public static final String P_VerificationId = "verificationId"; // fkey

	protected volatile int id;
	protected volatile OADateTime created;

	// Links to other objects.
	protected transient Hub<ReporterCorpVerification> hubReporterCorpVerifications;
	protected volatile transient Verification verification;

	public EnvironmentVerification() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public EnvironmentVerification(int id) {
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

	@OAMany(displayName = "Reporter Corp Verifications", toClass = ReporterCorpVerification.class, owner = true, reverseName = ReporterCorpVerification.P_EnvironmentVerification, cascadeSave = true, cascadeDelete = true)
	public Hub<ReporterCorpVerification> getReporterCorpVerifications() {
		if (hubReporterCorpVerifications == null) {
			hubReporterCorpVerifications = (Hub<ReporterCorpVerification>) getHub(P_ReporterCorpVerifications);
		}
		return hubReporterCorpVerifications;
	}

	@OAOne(reverseName = Verification.P_EnvironmentVerifications, required = true, allowCreateNew = false, fkeys = {
			@OAFkey(fromProperty = P_VerificationId, toProperty = Verification.P_Id) })
	public Verification getVerification() {
		if (verification == null) {
			verification = (Verification) getObject(P_Verification);
		}
		return verification;
	}

	public void setVerification(Verification newValue) {
		Verification old = this.verification;
		fireBeforePropertyChange(P_Verification, old, newValue);
		this.verification = newValue;
		firePropertyChange(P_Verification, old, this.verification);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "verification_id")
	public Integer getVerificationId() {
		return (Integer) getFkeyProperty(P_VerificationId);
	}

	public void setVerificationId(Integer newValue) {
		this.verification = null;
		setFkeyProperty(P_VerificationId, newValue);
	}
}
