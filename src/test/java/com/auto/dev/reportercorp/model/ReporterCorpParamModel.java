package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.delegate.ModelDelegate;
import com.auto.dev.reportercorp.model.oa.ReporterCorp;
import com.auto.dev.reportercorp.model.oa.ReporterCorpParam;
import com.auto.dev.reportercorp.model.search.ReporterCorpSearchModel;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCombined;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class ReporterCorpParamModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(ReporterCorpParamModel.class.getName());

	// Hubs
	protected Hub<ReporterCorpParam> hub;
	// selected reporterCorpParams
	protected Hub<ReporterCorpParam> hubMultiSelect;
	// detail hubs
	protected Hub<ReporterCorp> hubReporterCorp;

	// AddHubs used for references
	protected Hub<ReporterCorp> hubReporterCorpSelectFrom;

	// ObjectModels
	protected ReporterCorpModel modelReporterCorp;

	// selectFrom
	protected ReporterCorpModel modelReporterCorpSelectFrom;

	// SearchModels used for references
	protected ReporterCorpSearchModel modelReporterCorpSearch;

	public ReporterCorpParamModel() {
		setDisplayName("Reporter Corp Param");
		setPluralDisplayName("Reporter Corp Params");
	}

	public ReporterCorpParamModel(Hub<ReporterCorpParam> hubReporterCorpParam) {
		this();
		if (hubReporterCorpParam != null) {
			HubDelegate.setObjectClass(hubReporterCorpParam, ReporterCorpParam.class);
		}
		this.hub = hubReporterCorpParam;
	}

	public ReporterCorpParamModel(ReporterCorpParam reporterCorpParam) {
		this();
		getHub().add(reporterCorpParam);
		getHub().setPos(0);
	}

	public Hub<ReporterCorpParam> getOriginalHub() {
		return getHub();
	}

	public Hub<ReporterCorp> getReporterCorpHub() {
		if (hubReporterCorp != null) {
			return hubReporterCorp;
		}
		// this is the owner, use detailHub
		hubReporterCorp = getHub().getDetailHub(ReporterCorpParam.P_ReporterCorp);
		return hubReporterCorp;
	}

	public Hub<ReporterCorp> getReporterCorpSelectFromHub() {
		if (hubReporterCorpSelectFrom != null) {
			return hubReporterCorpSelectFrom;
		}
		hubReporterCorpSelectFrom = new Hub<ReporterCorp>(ReporterCorp.class);
		Hub<ReporterCorp> hubReporterCorpSelectFrom1 = ModelDelegate.getReporterCorps().createSharedHub();
		HubCombined<ReporterCorp> hubCombined = new HubCombined(hubReporterCorpSelectFrom, hubReporterCorpSelectFrom1,
				getReporterCorpHub());
		hubReporterCorpSelectFrom.setLinkHub(getHub(), ReporterCorpParam.P_ReporterCorp);
		return hubReporterCorpSelectFrom;
	}

	public ReporterCorpParam getReporterCorpParam() {
		return getHub().getAO();
	}

	public Hub<ReporterCorpParam> getHub() {
		if (hub == null) {
			hub = new Hub<ReporterCorpParam>(ReporterCorpParam.class);
		}
		return hub;
	}

	public Hub<ReporterCorpParam> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<ReporterCorpParam>(ReporterCorpParam.class);
		}
		return hubMultiSelect;
	}

	public ReporterCorpModel getReporterCorpModel() {
		if (modelReporterCorp != null) {
			return modelReporterCorp;
		}
		modelReporterCorp = new ReporterCorpModel(getReporterCorpHub());
		modelReporterCorp.setDisplayName("Reporter Corp");
		modelReporterCorp.setPluralDisplayName("Reporter Corps");
		modelReporterCorp.setForJfc(getForJfc());
		modelReporterCorp.setAllowNew(false);
		modelReporterCorp.setAllowSave(true);
		modelReporterCorp.setAllowAdd(false);
		modelReporterCorp.setAllowRemove(false);
		modelReporterCorp.setAllowClear(false);
		modelReporterCorp.setAllowDelete(false);
		modelReporterCorp.setAllowSearch(false);
		modelReporterCorp.setAllowHubSearch(true);
		modelReporterCorp.setAllowGotoEdit(true);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelReporterCorp.setCreateUI(li == null || !ReporterCorpParam.P_ReporterCorp.equalsIgnoreCase(li.getName()));
		modelReporterCorp.setViewOnly(getViewOnly());
		// call ReporterCorpParam.reporterCorpModelCallback(ReporterCorpModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ReporterCorpParam.class, ReporterCorpParam.P_ReporterCorp, modelReporterCorp);

		return modelReporterCorp;
	}

	public ReporterCorpModel getReporterCorpSelectFromModel() {
		if (modelReporterCorpSelectFrom != null) {
			return modelReporterCorpSelectFrom;
		}
		modelReporterCorpSelectFrom = new ReporterCorpModel(getReporterCorpSelectFromHub());
		modelReporterCorpSelectFrom.setDisplayName("Reporter Corp");
		modelReporterCorpSelectFrom.setPluralDisplayName("Reporter Corps");
		modelReporterCorpSelectFrom.setForJfc(getForJfc());
		modelReporterCorpSelectFrom.setAllowNew(false);
		modelReporterCorpSelectFrom.setAllowSave(true);
		modelReporterCorpSelectFrom.setAllowAdd(false);
		modelReporterCorpSelectFrom.setAllowMove(false);
		modelReporterCorpSelectFrom.setAllowRemove(false);
		modelReporterCorpSelectFrom.setAllowDelete(false);
		modelReporterCorpSelectFrom.setAllowSearch(false);
		modelReporterCorpSelectFrom.setAllowHubSearch(true);
		modelReporterCorpSelectFrom.setAllowGotoEdit(true);
		modelReporterCorpSelectFrom.setViewOnly(getViewOnly());
		modelReporterCorpSelectFrom.setAllowNew(false);
		modelReporterCorpSelectFrom.setAllowTableFilter(true);
		modelReporterCorpSelectFrom.setAllowTableSorting(true);
		modelReporterCorpSelectFrom.setAllowCut(false);
		modelReporterCorpSelectFrom.setAllowCopy(false);
		modelReporterCorpSelectFrom.setAllowPaste(false);
		modelReporterCorpSelectFrom.setAllowMultiSelect(false);
		return modelReporterCorpSelectFrom;
	}

	public ReporterCorpSearchModel getReporterCorpSearchModel() {
		if (modelReporterCorpSearch != null) {
			return modelReporterCorpSearch;
		}
		modelReporterCorpSearch = new ReporterCorpSearchModel();
		HubSelectDelegate.adoptWhereHub(modelReporterCorpSearch.getHub(), ReporterCorpParam.P_ReporterCorp, getHub());
		return modelReporterCorpSearch;
	}

	public HubCopy<ReporterCorpParam> createHubCopy() {
		Hub<ReporterCorpParam> hubReporterCorpParamx = new Hub<>(ReporterCorpParam.class);
		HubCopy<ReporterCorpParam> hc = new HubCopy<>(getHub(), hubReporterCorpParamx, true);
		return hc;
	}

	public ReporterCorpParamModel createCopy() {
		ReporterCorpParamModel mod = new ReporterCorpParamModel(createHubCopy().getHub());
		return mod;
	}
}
