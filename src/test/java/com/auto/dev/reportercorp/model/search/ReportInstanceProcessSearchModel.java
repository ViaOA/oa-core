package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.ReportInfoModel;
import com.auto.dev.reportercorp.model.ThreadInfoModel;
import com.auto.dev.reportercorp.model.oa.ReportInfo;
import com.auto.dev.reportercorp.model.oa.ReportInstanceProcess;
import com.auto.dev.reportercorp.model.oa.ThreadInfo;
import com.auto.dev.reportercorp.model.oa.search.ReportInstanceProcessSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class ReportInstanceProcessSearchModel {
	private static Logger LOG = Logger.getLogger(ReportInstanceProcessSearchModel.class.getName());

	protected Hub<ReportInstanceProcess> hub; // search results
	protected Hub<ReportInstanceProcess> hubMultiSelect;
	protected Hub<ReportInstanceProcess> hubSearchFrom; // hub (optional) to search from
	protected Hub<ReportInstanceProcessSearch> hubReportInstanceProcessSearch; // search data, size=1, AO
	// references used in search
	protected Hub<ReportInfo> hubReportInfo;
	protected Hub<ThreadInfo> hubThreadInfo;

	// finder used to find objects in a path
	protected OAFinder<?, ReportInstanceProcess> finder;

	// ObjectModels
	protected ReportInfoModel modelReportInfo;
	protected ThreadInfoModel modelThreadInfo;

	// SearchModels
	protected ReportInfoSearchModel modelReportInfoSearch;
	protected ThreadInfoSearchModel modelThreadInfoSearch;

	// object used for search data
	protected ReportInstanceProcessSearch reportInstanceProcessSearch;

	public ReportInstanceProcessSearchModel() {
	}

	public ReportInstanceProcessSearchModel(Hub<ReportInstanceProcess> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<ReportInstanceProcess> getHub() {
		if (hub == null) {
			hub = new Hub<ReportInstanceProcess>(ReportInstanceProcess.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<ReportInstanceProcess> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<ReportInstanceProcess> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					ReportInstanceProcessSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<ReportInstanceProcess> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(ReportInstanceProcess.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, ReportInstanceProcess> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, ReportInstanceProcess> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public ReportInstanceProcessSearch getReportInstanceProcessSearch() {
		if (reportInstanceProcessSearch != null) {
			return reportInstanceProcessSearch;
		}
		reportInstanceProcessSearch = new ReportInstanceProcessSearch();
		return reportInstanceProcessSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<ReportInstanceProcessSearch> getReportInstanceProcessSearchHub() {
		if (hubReportInstanceProcessSearch == null) {
			hubReportInstanceProcessSearch = new Hub<ReportInstanceProcessSearch>(ReportInstanceProcessSearch.class);
			hubReportInstanceProcessSearch.add(getReportInstanceProcessSearch());
			hubReportInstanceProcessSearch.setPos(0);
		}
		return hubReportInstanceProcessSearch;
	}

	public Hub<ReportInfo> getReportInfoHub() {
		if (hubReportInfo != null) {
			return hubReportInfo;
		}
		hubReportInfo = getReportInstanceProcessSearchHub().getDetailHub(ReportInstanceProcessSearch.P_ReportInfo);
		return hubReportInfo;
	}

	public Hub<ThreadInfo> getThreadInfoHub() {
		if (hubThreadInfo != null) {
			return hubThreadInfo;
		}
		hubThreadInfo = getReportInstanceProcessSearchHub().getDetailHub(ReportInstanceProcessSearch.P_ThreadInfo);
		return hubThreadInfo;
	}

	public ReportInfoModel getReportInfoModel() {
		if (modelReportInfo != null) {
			return modelReportInfo;
		}
		modelReportInfo = new ReportInfoModel(getReportInfoHub());
		modelReportInfo.setDisplayName("Report Info");
		modelReportInfo.setPluralDisplayName("Report Infos");
		modelReportInfo.setAllowNew(false);
		modelReportInfo.setAllowSave(true);
		modelReportInfo.setAllowAdd(false);
		modelReportInfo.setAllowRemove(false);
		modelReportInfo.setAllowClear(true);
		modelReportInfo.setAllowDelete(false);
		modelReportInfo.setAllowSearch(true);
		modelReportInfo.setAllowHubSearch(false);
		modelReportInfo.setAllowGotoEdit(true);
		modelReportInfo.setViewOnly(true);
		return modelReportInfo;
	}

	public ThreadInfoModel getThreadInfoModel() {
		if (modelThreadInfo != null) {
			return modelThreadInfo;
		}
		modelThreadInfo = new ThreadInfoModel(getThreadInfoHub());
		modelThreadInfo.setDisplayName("Thread Info");
		modelThreadInfo.setPluralDisplayName("Thread Infos");
		modelThreadInfo.setAllowNew(false);
		modelThreadInfo.setAllowSave(true);
		modelThreadInfo.setAllowAdd(false);
		modelThreadInfo.setAllowRemove(false);
		modelThreadInfo.setAllowClear(true);
		modelThreadInfo.setAllowDelete(false);
		modelThreadInfo.setAllowSearch(true);
		modelThreadInfo.setAllowHubSearch(false);
		modelThreadInfo.setAllowGotoEdit(true);
		modelThreadInfo.setViewOnly(true);
		return modelThreadInfo;
	}

	public ReportInfoSearchModel getReportInfoSearchModel() {
		if (modelReportInfoSearch == null) {
			modelReportInfoSearch = new ReportInfoSearchModel();
			getReportInstanceProcessSearch().setReportInfoSearch(modelReportInfoSearch.getReportInfoSearch());
		}
		return modelReportInfoSearch;
	}

	public ThreadInfoSearchModel getThreadInfoSearchModel() {
		if (modelThreadInfoSearch == null) {
			modelThreadInfoSearch = new ThreadInfoSearchModel();
			getReportInstanceProcessSearch().setThreadInfoSearch(modelThreadInfoSearch.getThreadInfoSearch());
		}
		return modelThreadInfoSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses ReportInstanceProcessSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<ReportInstanceProcess> sel = getReportInstanceProcessSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(ReportInstanceProcess reportInstanceProcess, Hub<ReportInstanceProcess> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<ReportInstanceProcess> hub) {
	}
}
