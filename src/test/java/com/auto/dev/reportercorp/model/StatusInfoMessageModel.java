package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.StatusInfo;
import com.auto.dev.reportercorp.model.oa.StatusInfoMessage;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class StatusInfoMessageModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(StatusInfoMessageModel.class.getName());

	// Hubs
	protected Hub<StatusInfoMessage> hub;
	// selected statusInfoMessages
	protected Hub<StatusInfoMessage> hubMultiSelect;
	// detail hubs
	protected Hub<StatusInfo> hubStatusInfo;

	// ObjectModels
	protected StatusInfoModel modelStatusInfo;

	public StatusInfoMessageModel() {
		setDisplayName("Status Info Message");
		setPluralDisplayName("Status Info Messages");
	}

	public StatusInfoMessageModel(Hub<StatusInfoMessage> hubStatusInfoMessage) {
		this();
		if (hubStatusInfoMessage != null) {
			HubDelegate.setObjectClass(hubStatusInfoMessage, StatusInfoMessage.class);
		}
		this.hub = hubStatusInfoMessage;
	}

	public StatusInfoMessageModel(StatusInfoMessage statusInfoMessage) {
		this();
		getHub().add(statusInfoMessage);
		getHub().setPos(0);
	}

	public Hub<StatusInfoMessage> getOriginalHub() {
		return getHub();
	}

	public Hub<StatusInfo> getStatusInfoHub() {
		if (hubStatusInfo != null) {
			return hubStatusInfo;
		}
		// this is the owner, use detailHub
		hubStatusInfo = getHub().getDetailHub(StatusInfoMessage.P_StatusInfo);
		return hubStatusInfo;
	}

	public StatusInfoMessage getStatusInfoMessage() {
		return getHub().getAO();
	}

	public Hub<StatusInfoMessage> getHub() {
		if (hub == null) {
			hub = new Hub<StatusInfoMessage>(StatusInfoMessage.class);
		}
		return hub;
	}

	public Hub<StatusInfoMessage> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<StatusInfoMessage>(StatusInfoMessage.class);
		}
		return hubMultiSelect;
	}

	public StatusInfoModel getStatusInfoModel() {
		if (modelStatusInfo != null) {
			return modelStatusInfo;
		}
		modelStatusInfo = new StatusInfoModel(getStatusInfoHub());
		modelStatusInfo.setDisplayName("Status Info");
		modelStatusInfo.setPluralDisplayName("Status Infos");
		modelStatusInfo.setForJfc(getForJfc());
		modelStatusInfo.setAllowNew(false);
		modelStatusInfo.setAllowSave(true);
		modelStatusInfo.setAllowAdd(false);
		modelStatusInfo.setAllowRemove(false);
		modelStatusInfo.setAllowClear(false);
		modelStatusInfo.setAllowDelete(false);
		modelStatusInfo.setAllowSearch(false);
		modelStatusInfo.setAllowHubSearch(false);
		modelStatusInfo.setAllowGotoEdit(true);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelStatusInfo.setCreateUI(li == null || !StatusInfoMessage.P_StatusInfo.equalsIgnoreCase(li.getName()));
		modelStatusInfo.setViewOnly(getViewOnly());
		// call StatusInfoMessage.statusInfoModelCallback(StatusInfoModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(StatusInfoMessage.class, StatusInfoMessage.P_StatusInfo, modelStatusInfo);

		return modelStatusInfo;
	}

	public HubCopy<StatusInfoMessage> createHubCopy() {
		Hub<StatusInfoMessage> hubStatusInfoMessagex = new Hub<>(StatusInfoMessage.class);
		HubCopy<StatusInfoMessage> hc = new HubCopy<>(getHub(), hubStatusInfoMessagex, true);
		return hc;
	}

	public StatusInfoMessageModel createCopy() {
		StatusInfoMessageModel mod = new StatusInfoMessageModel(createHubCopy().getHub());
		return mod;
	}
}
