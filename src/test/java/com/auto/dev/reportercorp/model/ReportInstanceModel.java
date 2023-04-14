package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.PypeReportMessage;
import com.auto.dev.reportercorp.model.oa.Report;
import com.auto.dev.reportercorp.model.oa.ReportInstance;
import com.auto.dev.reportercorp.model.oa.ReportInstanceData;
import com.auto.dev.reportercorp.model.oa.ReportVersion;
import com.auto.dev.reportercorp.model.oa.propertypath.ReportInstancePP;
import com.auto.dev.reportercorp.model.search.PypeReportMessageSearchModel;
import com.auto.dev.reportercorp.model.search.ReportInstanceSearchModel;
import com.auto.dev.reportercorp.model.search.ReportVersionSearchModel;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubGroupBy;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.model.oa.VInteger;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class ReportInstanceModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(ReportInstanceModel.class.getName());

	/* overview
	  originalHub   - save the original hub
	  groupByReportHub - detail hub of selected ReportInstance
	  <- hub - points to groupBy or original hub
	*/

	// types of ways to view the original hub
	public static final int TYPE_Original = 0;
	public static final int TYPE_GroupByReport = 1;

	// Hubs
	protected Hub<ReportInstance> hubOriginal;
	// groupBy Report
	protected Hub<Report> hubGroupedByReport;
	protected Hub<ReportInstance> hubReportInstancesGroupedByReport;
	protected HubGroupBy<ReportInstance, Report> hgbReport;

	// main hub that points to hubOriginal, groupedBy hub
	protected Hub<ReportInstance> hub;
	// selected reportInstances
	protected Hub<ReportInstance> hubMultiSelect;
	// detail hubs
	protected Hub<ReportInstanceData> hubCalcReportInstanceData;
	protected Hub<ReportInstance> hubParentCompositeReportInstance;
	protected Hub<PypeReportMessage> hubPypeReportMessage;
	protected Hub<ReportVersion> hubReportVersion;
	protected Hub<ReportInstance> hubCompositeReportInstances;

	// ObjectModels
	protected ReportInstanceDataModel modelCalcReportInstanceData;
	protected ReportInstanceModel modelParentCompositeReportInstance;
	protected PypeReportMessageModel modelPypeReportMessage;
	protected ReportVersionModel modelReportVersion;
	protected ReportInstanceModel modelCompositeReportInstances;
	protected ReportModel modelGroupedByReport;
	protected ReportInstanceModel modelReportInstancesGroupedByReport;

	// SearchModels used for references
	protected ReportInstanceSearchModel modelParentCompositeReportInstanceSearch;
	protected PypeReportMessageSearchModel modelPypeReportMessageSearch;
	protected ReportVersionSearchModel modelReportVersionSearch;
	protected ReportInstanceSearchModel modelCompositeReportInstancesSearch;

	public ReportInstanceModel() {
		setDisplayName("Report Instance");
		setPluralDisplayName("Report Instances");
	}

	public ReportInstanceModel(Hub<ReportInstance> hubReportInstance) {
		this();
		if (hubReportInstance != null) {
			HubDelegate.setObjectClass(hubReportInstance, ReportInstance.class);
		}
		this.hubOriginal = hubReportInstance;
		setType(TYPE_Original);
	}

	public ReportInstanceModel(ReportInstance reportInstance) {
		this();
		getOriginalHub().add(reportInstance);
		getOriginalHub().setPos(0);
		setType(TYPE_Original);
	}

	// the type of hub that is active
	public void setType(int type) {
		updateType(type);
		getVType().setValue(type);
	}

	public int getType() {
		return getVType().getValue();
	}

	// listen to type change
	public void addTypeListener(HubListener hl) {
		if (hl != null) {
			getTypeHub().addHubListener(hl);
		}
	}

	// used to listen to type change
	private Hub<VInteger> hubType;
	private VInteger type;

	private VInteger getVType() {
		if (type == null) {
			type = new VInteger();
		}
		return type;
	}

	private Hub<VInteger> getTypeHub() {
		if (hubType == null) {
			hubType = new Hub<VInteger>(VInteger.class);
			hubType.add(getVType());
			hubType.setPos(0);
		}
		return hubType;
	}

	private void updateType(int newValue) {
		if (!getAllowRecursive()) {
			newValue = TYPE_Original;
		}
		switch (newValue) {
		case TYPE_Original:
			getHub().setSharedHub(getOriginalHub(), true);
			break;
		case TYPE_GroupByReport:
			getHub().setSharedHub(getReportInstancesGroupedByReportHub(), true);
			break;
		}
	}

	public Hub<ReportInstance> getOriginalHub() {
		if (hubOriginal == null) {
			hubOriginal = new Hub<ReportInstance>(ReportInstance.class);
		}
		return hubOriginal;
	}

	public Hub<ReportInstanceData> getCalcReportInstanceDataHub() {
		if (hubCalcReportInstanceData != null) {
			return hubCalcReportInstanceData;
		}
		// this is a calculated
		hubCalcReportInstanceData = getHub().getDetailHub(ReportInstance.P_CalcReportInstanceData);
		return hubCalcReportInstanceData;
	}

	public Hub<ReportInstance> getParentCompositeReportInstanceHub() {
		if (hubParentCompositeReportInstance != null) {
			return hubParentCompositeReportInstance;
		}
		hubParentCompositeReportInstance = getHub().getDetailHub(ReportInstance.P_ParentCompositeReportInstance);
		return hubParentCompositeReportInstance;
	}

	public Hub<PypeReportMessage> getPypeReportMessageHub() {
		if (hubPypeReportMessage != null) {
			return hubPypeReportMessage;
		}
		hubPypeReportMessage = getHub().getDetailHub(ReportInstance.P_PypeReportMessage);
		return hubPypeReportMessage;
	}

	public Hub<ReportVersion> getReportVersionHub() {
		if (hubReportVersion != null) {
			return hubReportVersion;
		}
		hubReportVersion = getHub().getDetailHub(ReportInstance.P_ReportVersion);
		return hubReportVersion;
	}

	public Hub<ReportInstance> getCompositeReportInstances() {
		if (hubCompositeReportInstances == null) {
			hubCompositeReportInstances = getHub().getDetailHub(ReportInstance.P_CompositeReportInstances);
		}
		return hubCompositeReportInstances;
	}

	public Hub<Report> getGroupedByReportHub() {
		if (hubGroupedByReport == null) {
			hubGroupedByReport = getGroupedByReport().getMasterHub();
		}
		return hubGroupedByReport;
	}

	public Hub<ReportInstance> getReportInstancesGroupedByReportHub() {
		if (hubReportInstancesGroupedByReport == null) {
			hubReportInstancesGroupedByReport = getGroupedByReport().getDetailHub();
		}
		return hubReportInstancesGroupedByReport;
	}

	protected HubGroupBy<ReportInstance, Report> getGroupedByReport() {
		if (hgbReport == null) {
			String pp = ReportInstancePP.reportVersion().reportTemplate().report().pp;
			hgbReport = new HubGroupBy<ReportInstance, Report>(getOriginalHub(), pp);
		}
		return hgbReport;
	}

	public ReportInstance getReportInstance() {
		return getHub().getAO();
	}

	public Hub<ReportInstance> getHub() {
		if (hub == null) {
			hub = new Hub<ReportInstance>(ReportInstance.class);
			hub.setSharedHub(getOriginalHub(), true);
		}
		return hub;
	}

	public Hub<ReportInstance> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<ReportInstance>(ReportInstance.class);
		}
		return hubMultiSelect;
	}

	public ReportInstanceDataModel getCalcReportInstanceDataModel() {
		if (modelCalcReportInstanceData != null) {
			return modelCalcReportInstanceData;
		}
		modelCalcReportInstanceData = new ReportInstanceDataModel(getCalcReportInstanceDataHub());
		modelCalcReportInstanceData.setDisplayName("Report Instance Data");
		modelCalcReportInstanceData.setPluralDisplayName("Report Instance Datas");
		modelCalcReportInstanceData.setForJfc(getForJfc());
		modelCalcReportInstanceData.setAllowNew(false);
		modelCalcReportInstanceData.setAllowSave(false);
		modelCalcReportInstanceData.setAllowAdd(false);
		modelCalcReportInstanceData.setAllowRemove(false);
		modelCalcReportInstanceData.setAllowClear(false);
		modelCalcReportInstanceData.setAllowDelete(false);
		modelCalcReportInstanceData.setAllowSearch(false);
		modelCalcReportInstanceData.setAllowHubSearch(true);
		modelCalcReportInstanceData.setAllowGotoEdit(true);
		modelCalcReportInstanceData.setViewOnly(true);
		// call ReportInstance.calcReportInstanceDataModelCallback(ReportInstanceDataModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	ReportInstance.class, ReportInstance.P_CalcReportInstanceData,
														modelCalcReportInstanceData);

		return modelCalcReportInstanceData;
	}

	public ReportInstanceModel getParentCompositeReportInstanceModel() {
		if (modelParentCompositeReportInstance != null) {
			return modelParentCompositeReportInstance;
		}
		modelParentCompositeReportInstance = new ReportInstanceModel(getParentCompositeReportInstanceHub());
		modelParentCompositeReportInstance.setDisplayName("Parent Composite Report Instance");
		modelParentCompositeReportInstance.setPluralDisplayName("Report Instances");
		modelParentCompositeReportInstance.setForJfc(getForJfc());
		modelParentCompositeReportInstance.setAllowNew(false);
		modelParentCompositeReportInstance.setAllowSave(true);
		modelParentCompositeReportInstance.setAllowAdd(false);
		modelParentCompositeReportInstance.setAllowRemove(true);
		modelParentCompositeReportInstance.setAllowClear(true);
		modelParentCompositeReportInstance.setAllowDelete(false);
		modelParentCompositeReportInstance.setAllowSearch(true);
		modelParentCompositeReportInstance.setAllowHubSearch(true);
		modelParentCompositeReportInstance.setAllowGotoEdit(true);
		modelParentCompositeReportInstance.setViewOnly(true);
		// call ReportInstance.parentCompositeReportInstanceModelCallback(ReportInstanceModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	ReportInstance.class, ReportInstance.P_ParentCompositeReportInstance,
														modelParentCompositeReportInstance);

		return modelParentCompositeReportInstance;
	}

	public PypeReportMessageModel getPypeReportMessageModel() {
		if (modelPypeReportMessage != null) {
			return modelPypeReportMessage;
		}
		modelPypeReportMessage = new PypeReportMessageModel(getPypeReportMessageHub());
		modelPypeReportMessage.setDisplayName("Pype Report Message");
		modelPypeReportMessage.setPluralDisplayName("Pype Report Messages");
		modelPypeReportMessage.setForJfc(getForJfc());
		modelPypeReportMessage.setAllowNew(false);
		modelPypeReportMessage.setAllowSave(true);
		modelPypeReportMessage.setAllowAdd(false);
		modelPypeReportMessage.setAllowRemove(false);
		modelPypeReportMessage.setAllowClear(false);
		modelPypeReportMessage.setAllowDelete(false);
		modelPypeReportMessage.setAllowSearch(false);
		modelPypeReportMessage.setAllowHubSearch(true);
		modelPypeReportMessage.setAllowGotoEdit(true);
		modelPypeReportMessage.setViewOnly(true);
		// call ReportInstance.pypeReportMessageModelCallback(PypeReportMessageModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ReportInstance.class, ReportInstance.P_PypeReportMessage, modelPypeReportMessage);

		return modelPypeReportMessage;
	}

	public ReportVersionModel getReportVersionModel() {
		if (modelReportVersion != null) {
			return modelReportVersion;
		}
		modelReportVersion = new ReportVersionModel(getReportVersionHub());
		modelReportVersion.setDisplayName("Report Version");
		modelReportVersion.setPluralDisplayName("Report Versions");
		modelReportVersion.setForJfc(getForJfc());
		modelReportVersion.setAllowNew(false);
		modelReportVersion.setAllowSave(true);
		modelReportVersion.setAllowAdd(false);
		modelReportVersion.setAllowRemove(true);
		modelReportVersion.setAllowClear(true);
		modelReportVersion.setAllowDelete(false);
		modelReportVersion.setAllowSearch(true);
		modelReportVersion.setAllowHubSearch(true);
		modelReportVersion.setAllowGotoEdit(true);
		modelReportVersion.setViewOnly(true);
		// call ReportInstance.reportVersionModelCallback(ReportVersionModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ReportInstance.class, ReportInstance.P_ReportVersion, modelReportVersion);

		return modelReportVersion;
	}

	public ReportInstanceModel getCompositeReportInstancesModel() {
		if (modelCompositeReportInstances != null) {
			return modelCompositeReportInstances;
		}
		modelCompositeReportInstances = new ReportInstanceModel(getCompositeReportInstances());
		modelCompositeReportInstances.setDisplayName("Report Instance");
		modelCompositeReportInstances.setPluralDisplayName("Composite Report Instances");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getCompositeReportInstances())) {
			modelCompositeReportInstances.setCreateUI(false);
		}
		if (!HubDetailDelegate.getIsValidRecursive(getCompositeReportInstances())) {
			modelCompositeReportInstances.setCreateUI(false);
		}
		modelCompositeReportInstances.setForJfc(getForJfc());
		modelCompositeReportInstances.setAllowNew(true);
		modelCompositeReportInstances.setAllowSave(true);
		modelCompositeReportInstances.setAllowAdd(true);
		modelCompositeReportInstances.setAllowMove(true);
		modelCompositeReportInstances.setAllowRemove(true);
		modelCompositeReportInstances.setAllowDelete(false);
		modelCompositeReportInstances.setAllowRefresh(true);
		modelCompositeReportInstances.setAllowSearch(false);
		modelCompositeReportInstances.setAllowHubSearch(true);
		modelCompositeReportInstances.setAllowGotoEdit(true);
		modelCompositeReportInstances.setViewOnly(getViewOnly());
		modelCompositeReportInstances.setAllowTableFilter(false);
		modelCompositeReportInstances.setAllowTableSorting(false);
		modelCompositeReportInstances.setAllowMultiSelect(false);
		modelCompositeReportInstances.setAllowCopy(false);
		modelCompositeReportInstances.setAllowCut(false);
		modelCompositeReportInstances.setAllowPaste(false);
		// call ReportInstance.compositeReportInstancesModelCallback(ReportInstanceModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	ReportInstance.class, ReportInstance.P_CompositeReportInstances,
														modelCompositeReportInstances);

		return modelCompositeReportInstances;
	}

	public ReportModel getGroupedByReportModel() {
		if (modelGroupedByReport != null) {
			return modelGroupedByReport;
		}
		modelGroupedByReport = new ReportModel(getGroupedByReportHub());
		modelGroupedByReport.setDisplayName("Report");
		modelGroupedByReport.setPluralDisplayName("Report");
		modelGroupedByReport.setAllowNew(false);
		modelGroupedByReport.setAllowSave(false);
		modelGroupedByReport.setAllowAdd(false);
		modelGroupedByReport.setAllowRemove(false);
		modelGroupedByReport.setAllowDelete(false);
		modelGroupedByReport.setAllowSearch(false);
		modelGroupedByReport.setAllowHubSearch(true);
		modelGroupedByReport.setAllowGotoEdit(true);
		modelGroupedByReport.setForJfc(getForJfc());
		return modelGroupedByReport;
	}

	public ReportInstanceModel getReportInstancesGroupedByReportModel() {
		if (modelReportInstancesGroupedByReport != null) {
			return modelReportInstancesGroupedByReport;
		}
		modelReportInstancesGroupedByReport = new ReportInstanceModel(getReportInstancesGroupedByReportHub());
		modelReportInstancesGroupedByReport.setDisplayName("Report Instance");
		modelReportInstancesGroupedByReport.setPluralDisplayName("Report Instances");
		modelReportInstancesGroupedByReport.setAllowSave(false);
		modelReportInstancesGroupedByReport.setAllowNew(false);
		modelReportInstancesGroupedByReport.setAllowAdd(false);
		modelReportInstancesGroupedByReport.setAllowRemove(false);
		modelReportInstancesGroupedByReport.setAllowDelete(false);
		modelReportInstancesGroupedByReport.setAllowSearch(false);
		modelReportInstancesGroupedByReport.setAllowHubSearch(true);
		modelReportInstancesGroupedByReport.setAllowGotoEdit(true);
		modelReportInstancesGroupedByReport.setForJfc(getForJfc());
		return modelReportInstancesGroupedByReport;
	}

	public ReportInstanceSearchModel getParentCompositeReportInstanceSearchModel() {
		if (modelParentCompositeReportInstanceSearch != null) {
			return modelParentCompositeReportInstanceSearch;
		}
		modelParentCompositeReportInstanceSearch = new ReportInstanceSearchModel();
		HubSelectDelegate.adoptWhereHub(modelParentCompositeReportInstanceSearch.getHub(), ReportInstance.P_ParentCompositeReportInstance,
										getHub());
		return modelParentCompositeReportInstanceSearch;
	}

	public PypeReportMessageSearchModel getPypeReportMessageSearchModel() {
		if (modelPypeReportMessageSearch != null) {
			return modelPypeReportMessageSearch;
		}
		modelPypeReportMessageSearch = new PypeReportMessageSearchModel();
		HubSelectDelegate.adoptWhereHub(modelPypeReportMessageSearch.getHub(), ReportInstance.P_PypeReportMessage, getHub());
		return modelPypeReportMessageSearch;
	}

	public ReportVersionSearchModel getReportVersionSearchModel() {
		if (modelReportVersionSearch != null) {
			return modelReportVersionSearch;
		}
		modelReportVersionSearch = new ReportVersionSearchModel();
		HubSelectDelegate.adoptWhereHub(modelReportVersionSearch.getHub(), ReportInstance.P_ReportVersion, getHub());
		return modelReportVersionSearch;
	}

	public ReportInstanceSearchModel getCompositeReportInstancesSearchModel() {
		if (modelCompositeReportInstancesSearch != null) {
			return modelCompositeReportInstancesSearch;
		}
		modelCompositeReportInstancesSearch = new ReportInstanceSearchModel();
		return modelCompositeReportInstancesSearch;
	}

	public HubCopy<ReportInstance> createHubCopy() {
		Hub<ReportInstance> hubReportInstancex = new Hub<>(ReportInstance.class);
		HubCopy<ReportInstance> hc = new HubCopy<>(getHub(), hubReportInstancex, true);
		return hc;
	}

	public ReportInstanceModel createCopy() {
		ReportInstanceModel mod = new ReportInstanceModel(createHubCopy().getHub());
		return mod;
	}
}
