package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.delegate.ModelDelegate;
import com.auto.dev.reportercorp.model.oa.Environment;
import com.auto.dev.reportercorp.model.oa.EnvironmentSnapshot;
import com.auto.dev.reportercorp.model.oa.ReporterCorp;
import com.auto.dev.reportercorp.model.search.ReporterCorpSearchModel;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubMakeCopy;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class EnvironmentModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(EnvironmentModel.class.getName());

	// Hubs
	protected Hub<Environment> hub;
	// selected environments
	protected Hub<Environment> hubMultiSelect;
	// detail hubs
	protected Hub<EnvironmentSnapshot> hubEnvironmentSnapshots;
	protected Hub<ReporterCorp> hubReporterCorps;

	// selectFrom
	protected Hub<ReporterCorp> hubReporterCorpsSelectFrom;

	// ObjectModels
	protected EnvironmentSnapshotModel modelEnvironmentSnapshots;
	protected ReporterCorpModel modelReporterCorps;

	// selectFrom
	protected ReporterCorpModel modelReporterCorpsSelectFrom;

	// SearchModels used for references
	protected ReporterCorpSearchModel modelReporterCorpsSearch;

	public EnvironmentModel() {
		setDisplayName("Environment");
		setPluralDisplayName("Environments");
	}

	public EnvironmentModel(Hub<Environment> hubEnvironment) {
		this();
		if (hubEnvironment != null) {
			HubDelegate.setObjectClass(hubEnvironment, Environment.class);
		}
		this.hub = hubEnvironment;
	}

	public EnvironmentModel(Environment environment) {
		this();
		getHub().add(environment);
		getHub().setPos(0);
	}

	public Hub<Environment> getOriginalHub() {
		return getHub();
	}

	public Hub<EnvironmentSnapshot> getEnvironmentSnapshots() {
		if (hubEnvironmentSnapshots == null) {
			hubEnvironmentSnapshots = getHub().getDetailHub(Environment.P_EnvironmentSnapshots);
		}
		return hubEnvironmentSnapshots;
	}

	public Hub<ReporterCorp> getReporterCorps() {
		if (hubReporterCorps == null) {
			hubReporterCorps = getHub().getDetailHub(Environment.P_ReporterCorps);
		}
		return hubReporterCorps;
	}

	public Hub<ReporterCorp> getReporterCorpsSelectFromHub() {
		if (hubReporterCorpsSelectFrom != null) {
			return hubReporterCorpsSelectFrom;
		}
		hubReporterCorpsSelectFrom = ModelDelegate.getReporterCorps().createSharedHub();
		return hubReporterCorpsSelectFrom;
	}

	public Environment getEnvironment() {
		return getHub().getAO();
	}

	public Hub<Environment> getHub() {
		if (hub == null) {
			hub = new Hub<Environment>(Environment.class);
		}
		return hub;
	}

	public Hub<Environment> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<Environment>(Environment.class);
		}
		return hubMultiSelect;
	}

	public EnvironmentSnapshotModel getEnvironmentSnapshotsModel() {
		if (modelEnvironmentSnapshots != null) {
			return modelEnvironmentSnapshots;
		}
		modelEnvironmentSnapshots = new EnvironmentSnapshotModel(getEnvironmentSnapshots());
		modelEnvironmentSnapshots.setDisplayName("Environment Snapshot");
		modelEnvironmentSnapshots.setPluralDisplayName("Environment Snapshots");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getEnvironmentSnapshots())) {
			modelEnvironmentSnapshots.setCreateUI(false);
		}
		modelEnvironmentSnapshots.setForJfc(getForJfc());
		modelEnvironmentSnapshots.setAllowNew(true);
		modelEnvironmentSnapshots.setAllowSave(true);
		modelEnvironmentSnapshots.setAllowAdd(false);
		modelEnvironmentSnapshots.setAllowMove(false);
		modelEnvironmentSnapshots.setAllowRemove(false);
		modelEnvironmentSnapshots.setAllowDelete(true);
		modelEnvironmentSnapshots.setAllowRefresh(false);
		modelEnvironmentSnapshots.setAllowSearch(false);
		modelEnvironmentSnapshots.setAllowHubSearch(false);
		modelEnvironmentSnapshots.setAllowGotoEdit(true);
		modelEnvironmentSnapshots.setViewOnly(getViewOnly());
		modelEnvironmentSnapshots.setAllowTableFilter(true);
		modelEnvironmentSnapshots.setAllowTableSorting(true);
		modelEnvironmentSnapshots.setAllowMultiSelect(false);
		modelEnvironmentSnapshots.setAllowCopy(false);
		modelEnvironmentSnapshots.setAllowCut(false);
		modelEnvironmentSnapshots.setAllowPaste(false);
		// call Environment.environmentSnapshotsModelCallback(EnvironmentSnapshotModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(Environment.class, Environment.P_EnvironmentSnapshots, modelEnvironmentSnapshots);

		return modelEnvironmentSnapshots;
	}

	public ReporterCorpModel getReporterCorpsModel() {
		if (modelReporterCorps != null) {
			return modelReporterCorps;
		}
		modelReporterCorps = new ReporterCorpModel(getReporterCorps());
		modelReporterCorps.setDisplayName("Reporter Corp");
		modelReporterCorps.setPluralDisplayName("Reporter Corps");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getReporterCorps())) {
			modelReporterCorps.setCreateUI(false);
		}
		modelReporterCorps.setForJfc(getForJfc());
		modelReporterCorps.setAllowNew(true);
		modelReporterCorps.setAllowSave(true);
		modelReporterCorps.setAllowAdd(false);
		modelReporterCorps.setAllowMove(false);
		modelReporterCorps.setAllowRemove(false);
		modelReporterCorps.setAllowDelete(true);
		modelReporterCorps.setAllowRefresh(false);
		modelReporterCorps.setAllowSearch(false);
		modelReporterCorps.setAllowHubSearch(true);
		modelReporterCorps.setAllowGotoEdit(true);
		modelReporterCorps.setViewOnly(getViewOnly());
		modelReporterCorps.setAllowTableFilter(true);
		modelReporterCorps.setAllowTableSorting(true);
		modelReporterCorps.setAllowMultiSelect(false);
		modelReporterCorps.setAllowCopy(false);
		modelReporterCorps.setAllowCut(false);
		modelReporterCorps.setAllowPaste(false);
		// call Environment.reporterCorpsModelCallback(ReporterCorpModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(Environment.class, Environment.P_ReporterCorps, modelReporterCorps);

		return modelReporterCorps;
	}

	public ReporterCorpModel getReporterCorpsSelectFromModel() {
		if (modelReporterCorpsSelectFrom != null) {
			return modelReporterCorpsSelectFrom;
		}
		modelReporterCorpsSelectFrom = new ReporterCorpModel(getReporterCorpsSelectFromHub());
		modelReporterCorpsSelectFrom.setDisplayName("Reporter Corp");
		modelReporterCorpsSelectFrom.setPluralDisplayName("Reporter Corps");
		modelReporterCorpsSelectFrom.setForJfc(getForJfc());
		modelReporterCorpsSelectFrom.setAllowNew(false);
		modelReporterCorpsSelectFrom.setAllowSave(true);
		modelReporterCorpsSelectFrom.setAllowAdd(false);
		modelReporterCorpsSelectFrom.setAllowMove(false);
		modelReporterCorpsSelectFrom.setAllowRemove(false);
		modelReporterCorpsSelectFrom.setAllowDelete(false);
		modelReporterCorpsSelectFrom.setAllowRefresh(false);
		modelReporterCorpsSelectFrom.setAllowSearch(false);
		modelReporterCorpsSelectFrom.setAllowHubSearch(true);
		modelReporterCorpsSelectFrom.setAllowGotoEdit(true);
		modelReporterCorpsSelectFrom.setViewOnly(getViewOnly());
		modelReporterCorpsSelectFrom.setAllowNew(false);
		modelReporterCorpsSelectFrom.setAllowTableFilter(true);
		modelReporterCorpsSelectFrom.setAllowTableSorting(true);
		modelReporterCorpsSelectFrom.setAllowCut(false);
		modelReporterCorpsSelectFrom.setAllowCopy(false);
		modelReporterCorpsSelectFrom.setAllowPaste(false);
		modelReporterCorpsSelectFrom.setAllowMultiSelect(true);
		new HubMakeCopy(getReporterCorps(), modelReporterCorpsSelectFrom.getMultiSelectHub());
		return modelReporterCorpsSelectFrom;
	}

	public ReporterCorpSearchModel getReporterCorpsSearchModel() {
		if (modelReporterCorpsSearch != null) {
			return modelReporterCorpsSearch;
		}
		modelReporterCorpsSearch = new ReporterCorpSearchModel();
		return modelReporterCorpsSearch;
	}

	public HubCopy<Environment> createHubCopy() {
		Hub<Environment> hubEnvironmentx = new Hub<>(Environment.class);
		HubCopy<Environment> hc = new HubCopy<>(getHub(), hubEnvironmentx, true);
		return hc;
	}

	public EnvironmentModel createCopy() {
		EnvironmentModel mod = new EnvironmentModel(createHubCopy().getHub());
		return mod;
	}
}
