package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.EnvironmentVerification;
import com.auto.dev.reportercorp.model.oa.ReporterCorpVerification;
import com.auto.dev.reportercorp.model.oa.Verification;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class EnvironmentVerificationModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(EnvironmentVerificationModel.class.getName());

	// Hubs
	protected Hub<EnvironmentVerification> hub;
	// selected environmentVerifications
	protected Hub<EnvironmentVerification> hubMultiSelect;
	// detail hubs
	protected Hub<Verification> hubVerification;
	protected Hub<ReporterCorpVerification> hubReporterCorpVerifications;

	// ObjectModels
	protected VerificationModel modelVerification;
	protected ReporterCorpVerificationModel modelReporterCorpVerifications;

	public EnvironmentVerificationModel() {
		setDisplayName("Environment Verification");
		setPluralDisplayName("Environment Verifications");
	}

	public EnvironmentVerificationModel(Hub<EnvironmentVerification> hubEnvironmentVerification) {
		this();
		if (hubEnvironmentVerification != null) {
			HubDelegate.setObjectClass(hubEnvironmentVerification, EnvironmentVerification.class);
		}
		this.hub = hubEnvironmentVerification;
	}

	public EnvironmentVerificationModel(EnvironmentVerification environmentVerification) {
		this();
		getHub().add(environmentVerification);
		getHub().setPos(0);
	}

	public Hub<EnvironmentVerification> getOriginalHub() {
		return getHub();
	}

	public Hub<Verification> getVerificationHub() {
		if (hubVerification != null) {
			return hubVerification;
		}
		// this is the owner, use detailHub
		hubVerification = getHub().getDetailHub(EnvironmentVerification.P_Verification);
		return hubVerification;
	}

	public Hub<ReporterCorpVerification> getReporterCorpVerifications() {
		if (hubReporterCorpVerifications == null) {
			hubReporterCorpVerifications = getHub().getDetailHub(EnvironmentVerification.P_ReporterCorpVerifications);
		}
		return hubReporterCorpVerifications;
	}

	public EnvironmentVerification getEnvironmentVerification() {
		return getHub().getAO();
	}

	public Hub<EnvironmentVerification> getHub() {
		if (hub == null) {
			hub = new Hub<EnvironmentVerification>(EnvironmentVerification.class);
		}
		return hub;
	}

	public Hub<EnvironmentVerification> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<EnvironmentVerification>(EnvironmentVerification.class);
		}
		return hubMultiSelect;
	}

	public VerificationModel getVerificationModel() {
		if (modelVerification != null) {
			return modelVerification;
		}
		modelVerification = new VerificationModel(getVerificationHub());
		modelVerification.setDisplayName("Verification");
		modelVerification.setPluralDisplayName("Verifications");
		modelVerification.setForJfc(getForJfc());
		modelVerification.setAllowNew(false);
		modelVerification.setAllowSave(true);
		modelVerification.setAllowAdd(false);
		modelVerification.setAllowRemove(false);
		modelVerification.setAllowClear(false);
		modelVerification.setAllowDelete(false);
		modelVerification.setAllowSearch(false);
		modelVerification.setAllowHubSearch(false);
		modelVerification.setAllowGotoEdit(true);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelVerification.setCreateUI(li == null || !EnvironmentVerification.P_Verification.equalsIgnoreCase(li.getName()));
		modelVerification.setViewOnly(getViewOnly());
		// call EnvironmentVerification.verificationModelCallback(VerificationModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	EnvironmentVerification.class, EnvironmentVerification.P_Verification,
														modelVerification);

		return modelVerification;
	}

	public ReporterCorpVerificationModel getReporterCorpVerificationsModel() {
		if (modelReporterCorpVerifications != null) {
			return modelReporterCorpVerifications;
		}
		modelReporterCorpVerifications = new ReporterCorpVerificationModel(getReporterCorpVerifications());
		modelReporterCorpVerifications.setDisplayName("Reporter Corp Verification");
		modelReporterCorpVerifications.setPluralDisplayName("Reporter Corp Verifications");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getReporterCorpVerifications())) {
			modelReporterCorpVerifications.setCreateUI(false);
		}
		modelReporterCorpVerifications.setForJfc(getForJfc());
		modelReporterCorpVerifications.setAllowNew(true);
		modelReporterCorpVerifications.setAllowSave(true);
		modelReporterCorpVerifications.setAllowAdd(false);
		modelReporterCorpVerifications.setAllowMove(false);
		modelReporterCorpVerifications.setAllowRemove(false);
		modelReporterCorpVerifications.setAllowDelete(true);
		modelReporterCorpVerifications.setAllowRefresh(false);
		modelReporterCorpVerifications.setAllowSearch(false);
		modelReporterCorpVerifications.setAllowHubSearch(false);
		modelReporterCorpVerifications.setAllowGotoEdit(true);
		modelReporterCorpVerifications.setViewOnly(getViewOnly());
		modelReporterCorpVerifications.setAllowTableFilter(true);
		modelReporterCorpVerifications.setAllowTableSorting(true);
		modelReporterCorpVerifications.setAllowMultiSelect(false);
		modelReporterCorpVerifications.setAllowCopy(false);
		modelReporterCorpVerifications.setAllowCut(false);
		modelReporterCorpVerifications.setAllowPaste(false);
		// call EnvironmentVerification.reporterCorpVerificationsModelCallback(ReporterCorpVerificationModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	EnvironmentVerification.class, EnvironmentVerification.P_ReporterCorpVerifications,
														modelReporterCorpVerifications);

		return modelReporterCorpVerifications;
	}

	public HubCopy<EnvironmentVerification> createHubCopy() {
		Hub<EnvironmentVerification> hubEnvironmentVerificationx = new Hub<>(EnvironmentVerification.class);
		HubCopy<EnvironmentVerification> hc = new HubCopy<>(getHub(), hubEnvironmentVerificationx, true);
		return hc;
	}

	public EnvironmentVerificationModel createCopy() {
		EnvironmentVerificationModel mod = new EnvironmentVerificationModel(createHubCopy().getHub());
		return mod;
	}
}
