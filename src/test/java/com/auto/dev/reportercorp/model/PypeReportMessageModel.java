package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.PypeReportMessage;
import com.auto.dev.reportercorp.model.oa.ReportInstance;
import com.auto.dev.reportercorp.model.search.ReportInstanceSearchModel;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class PypeReportMessageModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(PypeReportMessageModel.class.getName());

	// Hubs
	protected Hub<PypeReportMessage> hub;
	// selected pypeReportMessages
	protected Hub<PypeReportMessage> hubMultiSelect;
	// detail hubs
	protected Hub<ReportInstance> hubReportInstance;

	// ObjectModels
	protected ReportInstanceModel modelReportInstance;

	// SearchModels used for references
	protected ReportInstanceSearchModel modelReportInstanceSearch;

	public PypeReportMessageModel() {
		setDisplayName("Pype Report Message");
		setPluralDisplayName("Pype Report Messages");
	}

	public PypeReportMessageModel(Hub<PypeReportMessage> hubPypeReportMessage) {
		this();
		if (hubPypeReportMessage != null) {
			HubDelegate.setObjectClass(hubPypeReportMessage, PypeReportMessage.class);
		}
		this.hub = hubPypeReportMessage;
	}

	public PypeReportMessageModel(PypeReportMessage pypeReportMessage) {
		this();
		getHub().add(pypeReportMessage);
		getHub().setPos(0);
	}

	public Hub<PypeReportMessage> getOriginalHub() {
		return getHub();
	}

	public Hub<ReportInstance> getReportInstanceHub() {
		if (hubReportInstance != null) {
			return hubReportInstance;
		}
		hubReportInstance = getHub().getDetailHub(PypeReportMessage.P_ReportInstance);
		return hubReportInstance;
	}

	public PypeReportMessage getPypeReportMessage() {
		return getHub().getAO();
	}

	public Hub<PypeReportMessage> getHub() {
		if (hub == null) {
			hub = new Hub<PypeReportMessage>(PypeReportMessage.class);
		}
		return hub;
	}

	public Hub<PypeReportMessage> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<PypeReportMessage>(PypeReportMessage.class);
		}
		return hubMultiSelect;
	}

	public ReportInstanceModel getReportInstanceModel() {
		if (modelReportInstance != null) {
			return modelReportInstance;
		}
		modelReportInstance = new ReportInstanceModel(getReportInstanceHub());
		modelReportInstance.setDisplayName("Report Instance");
		modelReportInstance.setPluralDisplayName("Report Instances");
		modelReportInstance.setForJfc(getForJfc());
		modelReportInstance.setAllowNew(false);
		modelReportInstance.setAllowSave(true);
		modelReportInstance.setAllowAdd(false);
		modelReportInstance.setAllowRemove(false);
		modelReportInstance.setAllowClear(false);
		modelReportInstance.setAllowDelete(false);
		modelReportInstance.setAllowSearch(false);
		modelReportInstance.setAllowHubSearch(true);
		modelReportInstance.setAllowGotoEdit(true);
		modelReportInstance.setViewOnly(true);
		// call PypeReportMessage.reportInstanceModelCallback(ReportInstanceModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(PypeReportMessage.class, PypeReportMessage.P_ReportInstance, modelReportInstance);

		return modelReportInstance;
	}

	public ReportInstanceSearchModel getReportInstanceSearchModel() {
		if (modelReportInstanceSearch != null) {
			return modelReportInstanceSearch;
		}
		modelReportInstanceSearch = new ReportInstanceSearchModel();
		HubSelectDelegate.adoptWhereHub(modelReportInstanceSearch.getHub(), PypeReportMessage.P_ReportInstance, getHub());
		return modelReportInstanceSearch;
	}

	public HubCopy<PypeReportMessage> createHubCopy() {
		Hub<PypeReportMessage> hubPypeReportMessagex = new Hub<>(PypeReportMessage.class);
		HubCopy<PypeReportMessage> hc = new HubCopy<>(getHub(), hubPypeReportMessagex, true);
		return hc;
	}

	public PypeReportMessageModel createCopy() {
		PypeReportMessageModel mod = new PypeReportMessageModel(createHubCopy().getHub());
		return mod;
	}
}
