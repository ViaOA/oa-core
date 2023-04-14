package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.ReportInstanceData;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.object.OAObjectModel;

public class ReportInstanceDataModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(ReportInstanceDataModel.class.getName());

	// Hubs
	protected Hub<ReportInstanceData> hub;
	// selected reportInstanceDatas
	protected Hub<ReportInstanceData> hubMultiSelect;

	public ReportInstanceDataModel() {
		setDisplayName("Report Instance Data");
		setPluralDisplayName("Report Instance Datas");
	}

	public ReportInstanceDataModel(Hub<ReportInstanceData> hubReportInstanceData) {
		this();
		if (hubReportInstanceData != null) {
			HubDelegate.setObjectClass(hubReportInstanceData, ReportInstanceData.class);
		}
		this.hub = hubReportInstanceData;
	}

	public ReportInstanceDataModel(ReportInstanceData reportInstanceData) {
		this();
		getHub().add(reportInstanceData);
		getHub().setPos(0);
	}

	public Hub<ReportInstanceData> getOriginalHub() {
		return getHub();
	}

	public ReportInstanceData getReportInstanceData() {
		return getHub().getAO();
	}

	public Hub<ReportInstanceData> getHub() {
		if (hub == null) {
			hub = new Hub<ReportInstanceData>(ReportInstanceData.class);
		}
		return hub;
	}

	public Hub<ReportInstanceData> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<ReportInstanceData>(ReportInstanceData.class);
		}
		return hubMultiSelect;
	}

	public HubCopy<ReportInstanceData> createHubCopy() {
		Hub<ReportInstanceData> hubReportInstanceDatax = new Hub<>(ReportInstanceData.class);
		HubCopy<ReportInstanceData> hc = new HubCopy<>(getHub(), hubReportInstanceDatax, true);
		return hc;
	}

	public ReportInstanceDataModel createCopy() {
		ReportInstanceDataModel mod = new ReportInstanceDataModel(createHubCopy().getHub());
		return mod;
	}
}
