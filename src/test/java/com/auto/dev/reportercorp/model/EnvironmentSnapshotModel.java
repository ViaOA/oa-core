package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.delegate.ModelDelegate;
import com.auto.dev.reportercorp.model.oa.Environment;
import com.auto.dev.reportercorp.model.oa.EnvironmentSnapshot;
import com.auto.dev.reportercorp.model.oa.SnapshotReport;
import com.auto.dev.reportercorp.model.search.SnapshotReportSearchModel;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCombined;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class EnvironmentSnapshotModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(EnvironmentSnapshotModel.class.getName());

	// Hubs
	protected Hub<EnvironmentSnapshot> hub;
	// selected environmentSnapshots
	protected Hub<EnvironmentSnapshot> hubMultiSelect;
	// detail hubs
	protected Hub<Environment> hubEnvironment;
	protected Hub<SnapshotReport> hubSnapshotReports;

	// AddHubs used for references
	protected Hub<Environment> hubEnvironmentSelectFrom;

	// ObjectModels
	protected EnvironmentModel modelEnvironment;
	protected SnapshotReportModel modelSnapshotReports;

	// selectFrom
	protected EnvironmentModel modelEnvironmentSelectFrom;

	// SearchModels used for references
	protected SnapshotReportSearchModel modelSnapshotReportsSearch;

	public EnvironmentSnapshotModel() {
		setDisplayName("Environment Snapshot");
		setPluralDisplayName("Environment Snapshots");
	}

	public EnvironmentSnapshotModel(Hub<EnvironmentSnapshot> hubEnvironmentSnapshot) {
		this();
		if (hubEnvironmentSnapshot != null) {
			HubDelegate.setObjectClass(hubEnvironmentSnapshot, EnvironmentSnapshot.class);
		}
		this.hub = hubEnvironmentSnapshot;
	}

	public EnvironmentSnapshotModel(EnvironmentSnapshot environmentSnapshot) {
		this();
		getHub().add(environmentSnapshot);
		getHub().setPos(0);
	}

	public Hub<EnvironmentSnapshot> getOriginalHub() {
		return getHub();
	}

	public Hub<Environment> getEnvironmentHub() {
		if (hubEnvironment != null) {
			return hubEnvironment;
		}
		// this is the owner, use detailHub
		hubEnvironment = getHub().getDetailHub(EnvironmentSnapshot.P_Environment);
		return hubEnvironment;
	}

	public Hub<SnapshotReport> getSnapshotReports() {
		if (hubSnapshotReports == null) {
			hubSnapshotReports = getHub().getDetailHub(EnvironmentSnapshot.P_SnapshotReports);
		}
		return hubSnapshotReports;
	}

	public Hub<Environment> getEnvironmentSelectFromHub() {
		if (hubEnvironmentSelectFrom != null) {
			return hubEnvironmentSelectFrom;
		}
		hubEnvironmentSelectFrom = new Hub<Environment>(Environment.class);
		Hub<Environment> hubEnvironmentSelectFrom1 = ModelDelegate.getEnvironments().createSharedHub();
		HubCombined<Environment> hubCombined = new HubCombined(hubEnvironmentSelectFrom, hubEnvironmentSelectFrom1, getEnvironmentHub());
		hubEnvironmentSelectFrom.setLinkHub(getHub(), EnvironmentSnapshot.P_Environment);
		return hubEnvironmentSelectFrom;
	}

	public EnvironmentSnapshot getEnvironmentSnapshot() {
		return getHub().getAO();
	}

	public Hub<EnvironmentSnapshot> getHub() {
		if (hub == null) {
			hub = new Hub<EnvironmentSnapshot>(EnvironmentSnapshot.class);
		}
		return hub;
	}

	public Hub<EnvironmentSnapshot> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<EnvironmentSnapshot>(EnvironmentSnapshot.class);
		}
		return hubMultiSelect;
	}

	public EnvironmentModel getEnvironmentModel() {
		if (modelEnvironment != null) {
			return modelEnvironment;
		}
		modelEnvironment = new EnvironmentModel(getEnvironmentHub());
		modelEnvironment.setDisplayName("Environment");
		modelEnvironment.setPluralDisplayName("Environments");
		modelEnvironment.setForJfc(getForJfc());
		modelEnvironment.setAllowNew(false);
		modelEnvironment.setAllowSave(true);
		modelEnvironment.setAllowAdd(false);
		modelEnvironment.setAllowRemove(false);
		modelEnvironment.setAllowClear(false);
		modelEnvironment.setAllowDelete(false);
		modelEnvironment.setAllowSearch(false);
		modelEnvironment.setAllowHubSearch(false);
		modelEnvironment.setAllowGotoEdit(true);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelEnvironment.setCreateUI(li == null || !EnvironmentSnapshot.P_Environment.equalsIgnoreCase(li.getName()));
		modelEnvironment.setViewOnly(getViewOnly());
		// call EnvironmentSnapshot.environmentModelCallback(EnvironmentModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(EnvironmentSnapshot.class, EnvironmentSnapshot.P_Environment, modelEnvironment);

		return modelEnvironment;
	}

	public SnapshotReportModel getSnapshotReportsModel() {
		if (modelSnapshotReports != null) {
			return modelSnapshotReports;
		}
		modelSnapshotReports = new SnapshotReportModel(getSnapshotReports());
		modelSnapshotReports.setDisplayName("Snapshot Report");
		modelSnapshotReports.setPluralDisplayName("Snapshot Reports");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getSnapshotReports())) {
			modelSnapshotReports.setCreateUI(false);
		}
		modelSnapshotReports.setForJfc(getForJfc());
		modelSnapshotReports.setAllowNew(false);
		modelSnapshotReports.setAllowSave(true);
		modelSnapshotReports.setAllowAdd(true);
		modelSnapshotReports.setAllowMove(false);
		modelSnapshotReports.setAllowRemove(true);
		modelSnapshotReports.setAllowDelete(false);
		modelSnapshotReports.setAllowRefresh(false);
		modelSnapshotReports.setAllowSearch(false);
		modelSnapshotReports.setAllowHubSearch(true);
		modelSnapshotReports.setAllowGotoEdit(true);
		modelSnapshotReports.setViewOnly(getViewOnly());
		modelSnapshotReports.setAllowTableFilter(true);
		modelSnapshotReports.setAllowTableSorting(true);
		modelSnapshotReports.setAllowMultiSelect(false);
		modelSnapshotReports.setAllowCopy(false);
		modelSnapshotReports.setAllowCut(false);
		modelSnapshotReports.setAllowPaste(false);
		// call EnvironmentSnapshot.snapshotReportsModelCallback(SnapshotReportModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	EnvironmentSnapshot.class, EnvironmentSnapshot.P_SnapshotReports,
														modelSnapshotReports);

		return modelSnapshotReports;
	}

	public EnvironmentModel getEnvironmentSelectFromModel() {
		if (modelEnvironmentSelectFrom != null) {
			return modelEnvironmentSelectFrom;
		}
		modelEnvironmentSelectFrom = new EnvironmentModel(getEnvironmentSelectFromHub());
		modelEnvironmentSelectFrom.setDisplayName("Environment");
		modelEnvironmentSelectFrom.setPluralDisplayName("Environments");
		modelEnvironmentSelectFrom.setForJfc(getForJfc());
		modelEnvironmentSelectFrom.setAllowNew(false);
		modelEnvironmentSelectFrom.setAllowSave(true);
		modelEnvironmentSelectFrom.setAllowAdd(false);
		modelEnvironmentSelectFrom.setAllowMove(false);
		modelEnvironmentSelectFrom.setAllowRemove(false);
		modelEnvironmentSelectFrom.setAllowDelete(false);
		modelEnvironmentSelectFrom.setAllowSearch(false);
		modelEnvironmentSelectFrom.setAllowHubSearch(true);
		modelEnvironmentSelectFrom.setAllowGotoEdit(true);
		modelEnvironmentSelectFrom.setViewOnly(getViewOnly());
		modelEnvironmentSelectFrom.setAllowNew(false);
		modelEnvironmentSelectFrom.setAllowTableFilter(true);
		modelEnvironmentSelectFrom.setAllowTableSorting(true);
		modelEnvironmentSelectFrom.setAllowCut(false);
		modelEnvironmentSelectFrom.setAllowCopy(false);
		modelEnvironmentSelectFrom.setAllowPaste(false);
		modelEnvironmentSelectFrom.setAllowMultiSelect(false);
		return modelEnvironmentSelectFrom;
	}

	public SnapshotReportSearchModel getSnapshotReportsSearchModel() {
		if (modelSnapshotReportsSearch != null) {
			return modelSnapshotReportsSearch;
		}
		modelSnapshotReportsSearch = new SnapshotReportSearchModel();
		return modelSnapshotReportsSearch;
	}

	public HubCopy<EnvironmentSnapshot> createHubCopy() {
		Hub<EnvironmentSnapshot> hubEnvironmentSnapshotx = new Hub<>(EnvironmentSnapshot.class);
		HubCopy<EnvironmentSnapshot> hc = new HubCopy<>(getHub(), hubEnvironmentSnapshotx, true);
		return hc;
	}

	public EnvironmentSnapshotModel createCopy() {
		EnvironmentSnapshotModel mod = new EnvironmentSnapshotModel(createHubCopy().getHub());
		return mod;
	}
}
