package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.StoreImport;
import com.auto.dev.reportercorp.model.oa.StoreImportReport;
import com.auto.dev.reportercorp.model.oa.StoreImportTemplate;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class StoreImportModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(StoreImportModel.class.getName());

	// Hubs
	protected Hub<StoreImport> hub;
	// selected storeImports
	protected Hub<StoreImport> hubMultiSelect;
	// detail hubs
	protected Hub<StoreImportReport> hubStoreImportReports;
	protected Hub<StoreImportTemplate> hubStoreImportTemplates;

	// ObjectModels
	protected StoreImportReportModel modelStoreImportReports;
	protected StoreImportTemplateModel modelStoreImportTemplates;

	public StoreImportModel() {
		setDisplayName("Store Import");
		setPluralDisplayName("Store Imports");
	}

	public StoreImportModel(Hub<StoreImport> hubStoreImport) {
		this();
		if (hubStoreImport != null) {
			HubDelegate.setObjectClass(hubStoreImport, StoreImport.class);
		}
		this.hub = hubStoreImport;
	}

	public StoreImportModel(StoreImport storeImport) {
		this();
		getHub().add(storeImport);
		getHub().setPos(0);
	}

	public Hub<StoreImport> getOriginalHub() {
		return getHub();
	}

	public Hub<StoreImportReport> getStoreImportReports() {
		if (hubStoreImportReports == null) {
			hubStoreImportReports = getHub().getDetailHub(StoreImport.P_StoreImportReports);
		}
		return hubStoreImportReports;
	}

	public Hub<StoreImportTemplate> getStoreImportTemplates() {
		if (hubStoreImportTemplates == null) {
			hubStoreImportTemplates = getHub().getDetailHub(StoreImport.P_StoreImportTemplates);
		}
		return hubStoreImportTemplates;
	}

	public StoreImport getStoreImport() {
		return getHub().getAO();
	}

	public Hub<StoreImport> getHub() {
		if (hub == null) {
			hub = new Hub<StoreImport>(StoreImport.class);
		}
		return hub;
	}

	public Hub<StoreImport> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<StoreImport>(StoreImport.class);
		}
		return hubMultiSelect;
	}

	public StoreImportReportModel getStoreImportReportsModel() {
		if (modelStoreImportReports != null) {
			return modelStoreImportReports;
		}
		modelStoreImportReports = new StoreImportReportModel(getStoreImportReports());
		modelStoreImportReports.setDisplayName("Store Import Report");
		modelStoreImportReports.setPluralDisplayName("Store Import Reports");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getStoreImportReports())) {
			modelStoreImportReports.setCreateUI(false);
		}
		modelStoreImportReports.setForJfc(getForJfc());
		modelStoreImportReports.setAllowNew(false);
		modelStoreImportReports.setAllowSave(true);
		modelStoreImportReports.setAllowAdd(false);
		modelStoreImportReports.setAllowMove(false);
		modelStoreImportReports.setAllowRemove(false);
		modelStoreImportReports.setAllowDelete(true);
		modelStoreImportReports.setAllowRefresh(false);
		modelStoreImportReports.setAllowSearch(false);
		modelStoreImportReports.setAllowHubSearch(false);
		modelStoreImportReports.setAllowGotoEdit(true);
		modelStoreImportReports.setViewOnly(getViewOnly());
		modelStoreImportReports.setAllowTableFilter(true);
		modelStoreImportReports.setAllowTableSorting(true);
		modelStoreImportReports.setAllowMultiSelect(false);
		modelStoreImportReports.setAllowCopy(false);
		modelStoreImportReports.setAllowCut(false);
		modelStoreImportReports.setAllowPaste(false);
		// call StoreImport.storeImportReportsModelCallback(StoreImportReportModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(StoreImport.class, StoreImport.P_StoreImportReports, modelStoreImportReports);

		return modelStoreImportReports;
	}

	public StoreImportTemplateModel getStoreImportTemplatesModel() {
		if (modelStoreImportTemplates != null) {
			return modelStoreImportTemplates;
		}
		modelStoreImportTemplates = new StoreImportTemplateModel(getStoreImportTemplates());
		modelStoreImportTemplates.setDisplayName("Store Import Template");
		modelStoreImportTemplates.setPluralDisplayName("Store Import Templates");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getStoreImportTemplates())) {
			modelStoreImportTemplates.setCreateUI(false);
		}
		modelStoreImportTemplates.setForJfc(getForJfc());
		modelStoreImportTemplates.setAllowNew(true);
		modelStoreImportTemplates.setAllowSave(true);
		modelStoreImportTemplates.setAllowAdd(false);
		modelStoreImportTemplates.setAllowMove(false);
		modelStoreImportTemplates.setAllowRemove(false);
		modelStoreImportTemplates.setAllowDelete(true);
		modelStoreImportTemplates.setAllowRefresh(false);
		modelStoreImportTemplates.setAllowSearch(false);
		modelStoreImportTemplates.setAllowHubSearch(false);
		modelStoreImportTemplates.setAllowGotoEdit(true);
		modelStoreImportTemplates.setViewOnly(getViewOnly());
		modelStoreImportTemplates.setAllowTableFilter(true);
		modelStoreImportTemplates.setAllowTableSorting(true);
		modelStoreImportTemplates.setAllowMultiSelect(false);
		modelStoreImportTemplates.setAllowCopy(false);
		modelStoreImportTemplates.setAllowCut(false);
		modelStoreImportTemplates.setAllowPaste(false);
		// call StoreImport.storeImportTemplatesModelCallback(StoreImportTemplateModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(StoreImport.class, StoreImport.P_StoreImportTemplates, modelStoreImportTemplates);

		return modelStoreImportTemplates;
	}

	public HubCopy<StoreImport> createHubCopy() {
		Hub<StoreImport> hubStoreImportx = new Hub<>(StoreImport.class);
		HubCopy<StoreImport> hc = new HubCopy<>(getHub(), hubStoreImportx, true);
		return hc;
	}

	public StoreImportModel createCopy() {
		StoreImportModel mod = new StoreImportModel(createHubCopy().getHub());
		return mod;
	}
}
