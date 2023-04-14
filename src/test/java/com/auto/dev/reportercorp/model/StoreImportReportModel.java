package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.delegate.ModelDelegate;
import com.auto.dev.reportercorp.model.oa.Report;
import com.auto.dev.reportercorp.model.oa.StoreImport;
import com.auto.dev.reportercorp.model.oa.StoreImportReport;
import com.auto.dev.reportercorp.model.search.ReportSearchModel;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCombined;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class StoreImportReportModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(StoreImportReportModel.class.getName());

	// Hubs
	protected Hub<StoreImportReport> hub;
	// selected storeImportReports
	protected Hub<StoreImportReport> hubMultiSelect;
	// detail hubs
	protected Hub<Report> hubReport;
	protected Hub<StoreImport> hubStoreImport;

	// AddHubs used for references
	protected Hub<Report> hubReportSelectFrom;

	// ObjectModels
	protected ReportModel modelReport;
	protected StoreImportModel modelStoreImport;

	// selectFrom
	protected ReportModel modelReportSelectFrom;

	// SearchModels used for references
	protected ReportSearchModel modelReportSearch;

	public StoreImportReportModel() {
		setDisplayName("Store Import Report");
		setPluralDisplayName("Store Import Reports");
	}

	public StoreImportReportModel(Hub<StoreImportReport> hubStoreImportReport) {
		this();
		if (hubStoreImportReport != null) {
			HubDelegate.setObjectClass(hubStoreImportReport, StoreImportReport.class);
		}
		this.hub = hubStoreImportReport;
	}

	public StoreImportReportModel(StoreImportReport storeImportReport) {
		this();
		getHub().add(storeImportReport);
		getHub().setPos(0);
	}

	public Hub<StoreImportReport> getOriginalHub() {
		return getHub();
	}

	public Hub<Report> getReportHub() {
		if (hubReport != null) {
			return hubReport;
		}
		hubReport = getHub().getDetailHub(StoreImportReport.P_Report);
		return hubReport;
	}

	public Hub<StoreImport> getStoreImportHub() {
		if (hubStoreImport != null) {
			return hubStoreImport;
		}
		// this is the owner, use detailHub
		hubStoreImport = getHub().getDetailHub(StoreImportReport.P_StoreImport);
		return hubStoreImport;
	}

	public Hub<Report> getReportSelectFromHub() {
		if (hubReportSelectFrom != null) {
			return hubReportSelectFrom;
		}
		hubReportSelectFrom = new Hub<Report>(Report.class);
		Hub<Report> hubReportSelectFrom1 = ModelDelegate.getReports().createSharedHub();
		HubCombined<Report> hubCombined = new HubCombined(hubReportSelectFrom, hubReportSelectFrom1, getReportHub());
		hubReportSelectFrom.setLinkHub(getHub(), StoreImportReport.P_Report);
		return hubReportSelectFrom;
	}

	public StoreImportReport getStoreImportReport() {
		return getHub().getAO();
	}

	public Hub<StoreImportReport> getHub() {
		if (hub == null) {
			hub = new Hub<StoreImportReport>(StoreImportReport.class);
		}
		return hub;
	}

	public Hub<StoreImportReport> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<StoreImportReport>(StoreImportReport.class);
		}
		return hubMultiSelect;
	}

	public ReportModel getReportModel() {
		if (modelReport != null) {
			return modelReport;
		}
		modelReport = new ReportModel(getReportHub());
		modelReport.setDisplayName("Report");
		modelReport.setPluralDisplayName("Reports");
		modelReport.setForJfc(getForJfc());
		modelReport.setAllowNew(false);
		modelReport.setAllowSave(true);
		modelReport.setAllowAdd(false);
		modelReport.setAllowRemove(false);
		modelReport.setAllowClear(false);
		modelReport.setAllowDelete(false);
		modelReport.setAllowSearch(false);
		modelReport.setAllowHubSearch(true);
		modelReport.setAllowGotoEdit(true);
		modelReport.setViewOnly(true);
		// call StoreImportReport.reportModelCallback(ReportModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(StoreImportReport.class, StoreImportReport.P_Report, modelReport);

		return modelReport;
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
		modelStoreImport.setCreateUI(li == null || !StoreImportReport.P_StoreImport.equalsIgnoreCase(li.getName()));
		modelStoreImport.setViewOnly(getViewOnly());
		// call StoreImportReport.storeImportModelCallback(StoreImportModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(StoreImportReport.class, StoreImportReport.P_StoreImport, modelStoreImport);

		return modelStoreImport;
	}

	public ReportModel getReportSelectFromModel() {
		if (modelReportSelectFrom != null) {
			return modelReportSelectFrom;
		}
		modelReportSelectFrom = new ReportModel(getReportSelectFromHub());
		modelReportSelectFrom.setDisplayName("Report");
		modelReportSelectFrom.setPluralDisplayName("Reports");
		modelReportSelectFrom.setForJfc(getForJfc());
		modelReportSelectFrom.setAllowNew(false);
		modelReportSelectFrom.setAllowSave(true);
		modelReportSelectFrom.setAllowAdd(false);
		modelReportSelectFrom.setAllowMove(false);
		modelReportSelectFrom.setAllowRemove(false);
		modelReportSelectFrom.setAllowDelete(false);
		modelReportSelectFrom.setAllowSearch(false);
		modelReportSelectFrom.setAllowHubSearch(true);
		modelReportSelectFrom.setAllowGotoEdit(true);
		modelReportSelectFrom.setViewOnly(getViewOnly());
		modelReportSelectFrom.setAllowNew(false);
		modelReportSelectFrom.setAllowTableFilter(true);
		modelReportSelectFrom.setAllowTableSorting(true);
		modelReportSelectFrom.setAllowCut(false);
		modelReportSelectFrom.setAllowCopy(false);
		modelReportSelectFrom.setAllowPaste(false);
		modelReportSelectFrom.setAllowMultiSelect(false);
		return modelReportSelectFrom;
	}

	public ReportSearchModel getReportSearchModel() {
		if (modelReportSearch != null) {
			return modelReportSearch;
		}
		modelReportSearch = new ReportSearchModel();
		HubSelectDelegate.adoptWhereHub(modelReportSearch.getHub(), StoreImportReport.P_Report, getHub());
		return modelReportSearch;
	}

	public HubCopy<StoreImportReport> createHubCopy() {
		Hub<StoreImportReport> hubStoreImportReportx = new Hub<>(StoreImportReport.class);
		HubCopy<StoreImportReport> hc = new HubCopy<>(getHub(), hubStoreImportReportx, true);
		return hc;
	}

	public StoreImportReportModel createCopy() {
		StoreImportReportModel mod = new StoreImportReportModel(createHubCopy().getHub());
		return mod;
	}
}
