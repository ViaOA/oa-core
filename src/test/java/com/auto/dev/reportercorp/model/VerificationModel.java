package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.EnvironmentVerification;
import com.auto.dev.reportercorp.model.oa.Verification;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class VerificationModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(VerificationModel.class.getName());

	// Hubs
	protected Hub<Verification> hub;
	// selected verifications
	protected Hub<Verification> hubMultiSelect;
	// detail hubs
	protected Hub<EnvironmentVerification> hubEnvironmentVerifications;

	// ObjectModels
	protected EnvironmentVerificationModel modelEnvironmentVerifications;

	public VerificationModel() {
		setDisplayName("Verification");
		setPluralDisplayName("Verifications");
	}

	public VerificationModel(Hub<Verification> hubVerification) {
		this();
		if (hubVerification != null) {
			HubDelegate.setObjectClass(hubVerification, Verification.class);
		}
		this.hub = hubVerification;
	}

	public VerificationModel(Verification verification) {
		this();
		getHub().add(verification);
		getHub().setPos(0);
	}

	public Hub<Verification> getOriginalHub() {
		return getHub();
	}

	public Hub<EnvironmentVerification> getEnvironmentVerifications() {
		if (hubEnvironmentVerifications == null) {
			hubEnvironmentVerifications = getHub().getDetailHub(Verification.P_EnvironmentVerifications);
		}
		return hubEnvironmentVerifications;
	}

	public Verification getVerification() {
		return getHub().getAO();
	}

	public Hub<Verification> getHub() {
		if (hub == null) {
			hub = new Hub<Verification>(Verification.class);
		}
		return hub;
	}

	public Hub<Verification> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<Verification>(Verification.class);
		}
		return hubMultiSelect;
	}

	public EnvironmentVerificationModel getEnvironmentVerificationsModel() {
		if (modelEnvironmentVerifications != null) {
			return modelEnvironmentVerifications;
		}
		modelEnvironmentVerifications = new EnvironmentVerificationModel(getEnvironmentVerifications());
		modelEnvironmentVerifications.setDisplayName("Environment Verification");
		modelEnvironmentVerifications.setPluralDisplayName("Environment Verifications");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getEnvironmentVerifications())) {
			modelEnvironmentVerifications.setCreateUI(false);
		}
		modelEnvironmentVerifications.setForJfc(getForJfc());
		modelEnvironmentVerifications.setAllowNew(true);
		modelEnvironmentVerifications.setAllowSave(true);
		modelEnvironmentVerifications.setAllowAdd(false);
		modelEnvironmentVerifications.setAllowMove(false);
		modelEnvironmentVerifications.setAllowRemove(false);
		modelEnvironmentVerifications.setAllowDelete(true);
		modelEnvironmentVerifications.setAllowRefresh(false);
		modelEnvironmentVerifications.setAllowSearch(false);
		modelEnvironmentVerifications.setAllowHubSearch(false);
		modelEnvironmentVerifications.setAllowGotoEdit(true);
		modelEnvironmentVerifications.setViewOnly(getViewOnly());
		modelEnvironmentVerifications.setAllowTableFilter(true);
		modelEnvironmentVerifications.setAllowTableSorting(true);
		modelEnvironmentVerifications.setAllowMultiSelect(false);
		modelEnvironmentVerifications.setAllowCopy(false);
		modelEnvironmentVerifications.setAllowCut(false);
		modelEnvironmentVerifications.setAllowPaste(false);
		// call Verification.environmentVerificationsModelCallback(EnvironmentVerificationModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	Verification.class, Verification.P_EnvironmentVerifications,
														modelEnvironmentVerifications);

		return modelEnvironmentVerifications;
	}

	public HubCopy<Verification> createHubCopy() {
		Hub<Verification> hubVerificationx = new Hub<>(Verification.class);
		HubCopy<Verification> hc = new HubCopy<>(getHub(), hubVerificationx, true);
		return hc;
	}

	public VerificationModel createCopy() {
		VerificationModel mod = new VerificationModel(createHubCopy().getHub());
		return mod;
	}
}
