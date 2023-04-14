package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.EnvironmentVerification;
import com.auto.dev.reportercorp.model.oa.ReporterCorpVerification;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class ReporterCorpVerificationModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(ReporterCorpVerificationModel.class.getName());

	// Hubs
	protected Hub<ReporterCorpVerification> hub;
	// selected reporterCorpVerifications
	protected Hub<ReporterCorpVerification> hubMultiSelect;
	// detail hubs
	protected Hub<EnvironmentVerification> hubEnvironmentVerification;

	// ObjectModels
	protected EnvironmentVerificationModel modelEnvironmentVerification;

	public ReporterCorpVerificationModel() {
		setDisplayName("Reporter Corp Verification");
		setPluralDisplayName("Reporter Corp Verifications");
	}

	public ReporterCorpVerificationModel(Hub<ReporterCorpVerification> hubReporterCorpVerification) {
		this();
		if (hubReporterCorpVerification != null) {
			HubDelegate.setObjectClass(hubReporterCorpVerification, ReporterCorpVerification.class);
		}
		this.hub = hubReporterCorpVerification;
	}

	public ReporterCorpVerificationModel(ReporterCorpVerification reporterCorpVerification) {
		this();
		getHub().add(reporterCorpVerification);
		getHub().setPos(0);
	}

	public Hub<ReporterCorpVerification> getOriginalHub() {
		return getHub();
	}

	public Hub<EnvironmentVerification> getEnvironmentVerificationHub() {
		if (hubEnvironmentVerification != null) {
			return hubEnvironmentVerification;
		}
		// this is the owner, use detailHub
		hubEnvironmentVerification = getHub().getDetailHub(ReporterCorpVerification.P_EnvironmentVerification);
		return hubEnvironmentVerification;
	}

	public ReporterCorpVerification getReporterCorpVerification() {
		return getHub().getAO();
	}

	public Hub<ReporterCorpVerification> getHub() {
		if (hub == null) {
			hub = new Hub<ReporterCorpVerification>(ReporterCorpVerification.class);
		}
		return hub;
	}

	public Hub<ReporterCorpVerification> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<ReporterCorpVerification>(ReporterCorpVerification.class);
		}
		return hubMultiSelect;
	}

	public EnvironmentVerificationModel getEnvironmentVerificationModel() {
		if (modelEnvironmentVerification != null) {
			return modelEnvironmentVerification;
		}
		modelEnvironmentVerification = new EnvironmentVerificationModel(getEnvironmentVerificationHub());
		modelEnvironmentVerification.setDisplayName("Environment Verification");
		modelEnvironmentVerification.setPluralDisplayName("Environment Verifications");
		modelEnvironmentVerification.setForJfc(getForJfc());
		modelEnvironmentVerification.setAllowNew(false);
		modelEnvironmentVerification.setAllowSave(true);
		modelEnvironmentVerification.setAllowAdd(false);
		modelEnvironmentVerification.setAllowRemove(false);
		modelEnvironmentVerification.setAllowClear(false);
		modelEnvironmentVerification.setAllowDelete(false);
		modelEnvironmentVerification.setAllowSearch(false);
		modelEnvironmentVerification.setAllowHubSearch(false);
		modelEnvironmentVerification.setAllowGotoEdit(true);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelEnvironmentVerification
				.setCreateUI(li == null || !ReporterCorpVerification.P_EnvironmentVerification.equalsIgnoreCase(li.getName()));
		modelEnvironmentVerification.setViewOnly(getViewOnly());
		// call ReporterCorpVerification.environmentVerificationModelCallback(EnvironmentVerificationModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	ReporterCorpVerification.class, ReporterCorpVerification.P_EnvironmentVerification,
														modelEnvironmentVerification);

		return modelEnvironmentVerification;
	}

	public HubCopy<ReporterCorpVerification> createHubCopy() {
		Hub<ReporterCorpVerification> hubReporterCorpVerificationx = new Hub<>(ReporterCorpVerification.class);
		HubCopy<ReporterCorpVerification> hc = new HubCopy<>(getHub(), hubReporterCorpVerificationx, true);
		return hc;
	}

	public ReporterCorpVerificationModel createCopy() {
		ReporterCorpVerificationModel mod = new ReporterCorpVerificationModel(createHubCopy().getHub());
		return mod;
	}
}
