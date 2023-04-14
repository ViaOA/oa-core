package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.ReportTemplate;
import com.auto.dev.reportercorp.model.oa.StoreImport;
import com.auto.dev.reportercorp.model.oa.StoreImportTemplate;
import com.auto.dev.reportercorp.model.search.ReportTemplateSearchModel;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class StoreImportTemplateModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(StoreImportTemplateModel.class.getName());

	// Hubs
	protected Hub<StoreImportTemplate> hub;
	// selected storeImportTemplates
	protected Hub<StoreImportTemplate> hubMultiSelect;
	// detail hubs
	protected Hub<ReportTemplate> hubReportTemplate;
	protected Hub<StoreImport> hubStoreImport;

	// ObjectModels
	protected ReportTemplateModel modelReportTemplate;
	protected StoreImportModel modelStoreImport;

	// SearchModels used for references
	protected ReportTemplateSearchModel modelReportTemplateSearch;

	public StoreImportTemplateModel() {
		setDisplayName("Store Import Template");
		setPluralDisplayName("Store Import Templates");
	}

	public StoreImportTemplateModel(Hub<StoreImportTemplate> hubStoreImportTemplate) {
		this();
		if (hubStoreImportTemplate != null) {
			HubDelegate.setObjectClass(hubStoreImportTemplate, StoreImportTemplate.class);
		}
		this.hub = hubStoreImportTemplate;
	}

	public StoreImportTemplateModel(StoreImportTemplate storeImportTemplate) {
		this();
		getHub().add(storeImportTemplate);
		getHub().setPos(0);
	}

	public Hub<StoreImportTemplate> getOriginalHub() {
		return getHub();
	}

	public Hub<ReportTemplate> getReportTemplateHub() {
		if (hubReportTemplate != null) {
			return hubReportTemplate;
		}
		hubReportTemplate = getHub().getDetailHub(StoreImportTemplate.P_ReportTemplate);
		return hubReportTemplate;
	}

	public Hub<StoreImport> getStoreImportHub() {
		if (hubStoreImport != null) {
			return hubStoreImport;
		}
		// this is the owner, use detailHub
		hubStoreImport = getHub().getDetailHub(StoreImportTemplate.P_StoreImport);
		return hubStoreImport;
	}

	public StoreImportTemplate getStoreImportTemplate() {
		return getHub().getAO();
	}

	public Hub<StoreImportTemplate> getHub() {
		if (hub == null) {
			hub = new Hub<StoreImportTemplate>(StoreImportTemplate.class);
		}
		return hub;
	}

	public Hub<StoreImportTemplate> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<StoreImportTemplate>(StoreImportTemplate.class);
		}
		return hubMultiSelect;
	}

	public ReportTemplateModel getReportTemplateModel() {
		if (modelReportTemplate != null) {
			return modelReportTemplate;
		}
		modelReportTemplate = new ReportTemplateModel(getReportTemplateHub());
		modelReportTemplate.setDisplayName("Report Template");
		modelReportTemplate.setPluralDisplayName("Report Templates");
		modelReportTemplate.setForJfc(getForJfc());
		modelReportTemplate.setAllowNew(false);
		modelReportTemplate.setAllowSave(true);
		modelReportTemplate.setAllowAdd(false);
		modelReportTemplate.setAllowRemove(true);
		modelReportTemplate.setAllowClear(true);
		modelReportTemplate.setAllowDelete(false);
		modelReportTemplate.setAllowSearch(true);
		modelReportTemplate.setAllowHubSearch(true);
		modelReportTemplate.setAllowGotoEdit(true);
		modelReportTemplate.setViewOnly(true);
		// call StoreImportTemplate.reportTemplateModelCallback(ReportTemplateModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	StoreImportTemplate.class, StoreImportTemplate.P_ReportTemplate,
														modelReportTemplate);

		return modelReportTemplate;
	}

	public StoreImportModel getStoreImportModel() {
		if (modelStoreImport != null) {
			return modelStoreImport;
		}
		modelStoreImport = new StoreImportModel(getStoreImportHub());
		modelStoreImport.setDisplayName("Store Import");
		modelStoreImport.setPluralDisplayName("Store Imports");
		modelStoreImport.setForJfc(getForJfc());
		modelStoreImport.setAllowNew(false);
		modelStoreImport.setAllowSave(true);
		modelStoreImport.setAllowAdd(false);
		modelStoreImport.setAllowRemove(false);
		modelStoreImport.setAllowClear(false);
		modelStoreImport.setAllowDelete(false);
		modelStoreImport.setAllowSearch(false);
		modelStoreImport.setAllowHubSearch(false);
		modelStoreImport.setAllowGotoEdit(true);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelStoreImport.setCreateUI(li == null || !StoreImportTemplate.P_StoreImport.equalsIgnoreCase(li.getName()));
		modelStoreImport.setViewOnly(getViewOnly());
		// call StoreImportTemplate.storeImportModelCallback(StoreImportModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(StoreImportTemplate.class, StoreImportTemplate.P_StoreImport, modelStoreImport);

		return modelStoreImport;
	}

	public ReportTemplateSearchModel getReportTemplateSearchModel() {
		if (modelReportTemplateSearch != null) {
			return modelReportTemplateSearch;
		}
		modelReportTemplateSearch = new ReportTemplateSearchModel();
		HubSelectDelegate.adoptWhereHub(modelReportTemplateSearch.getHub(), StoreImportTemplate.P_ReportTemplate, getHub());
		return modelReportTemplateSearch;
	}

	public HubCopy<StoreImportTemplate> createHubCopy() {
		Hub<StoreImportTemplate> hubStoreImportTemplatex = new Hub<>(StoreImportTemplate.class);
		HubCopy<StoreImportTemplate> hc = new HubCopy<>(getHub(), hubStoreImportTemplatex, true);
		return hc;
	}

	public StoreImportTemplateModel createCopy() {
		StoreImportTemplateModel mod = new StoreImportTemplateModel(createHubCopy().getHub());
		return mod;
	}
}
