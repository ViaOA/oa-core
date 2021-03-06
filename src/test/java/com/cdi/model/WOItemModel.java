// Generated by OABuilder

package com.cdi.model;

import java.util.logging.Logger;

import com.cdi.model.oa.OrderItem;
import com.cdi.model.oa.WOItem;
import com.cdi.model.oa.WorkOrder;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class WOItemModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(WOItemModel.class.getName());

	// Hubs
	protected Hub<WOItem> hub;
	// selected woItems
	protected Hub<WOItem> hubMultiSelect;
	// detail hubs
	protected Hub<OrderItem> hubOrderItem;
	protected Hub<WorkOrder> hubWorkOrder;

	// ObjectModels
	protected OrderItemModel modelOrderItem;
	protected WorkOrderModel modelWorkOrder;

	public WOItemModel() {
		setDisplayName("WOItem");
		setPluralDisplayName("WOItems");
	}

	public WOItemModel(Hub<WOItem> hubWOItem) {
		this();
		if (hubWOItem != null) {
			HubDelegate.setObjectClass(hubWOItem, WOItem.class);
		}
		this.hub = hubWOItem;
	}

	public WOItemModel(WOItem woItem) {
		this();
		getHub().add(woItem);
		getHub().setPos(0);
	}

	public Hub<WOItem> getOriginalHub() {
		return getHub();
	}

	public Hub<OrderItem> getOrderItemHub() {
		if (hubOrderItem != null) {
			return hubOrderItem;
		}
		hubOrderItem = getHub().getDetailHub(WOItem.P_OrderItem);
		return hubOrderItem;
	}

	public Hub<WorkOrder> getWorkOrderHub() {
		if (hubWorkOrder != null) {
			return hubWorkOrder;
		}
		// this is the owner, use detailHub
		hubWorkOrder = getHub().getDetailHub(WOItem.P_WorkOrder);
		return hubWorkOrder;
	}

	public WOItem getWOItem() {
		return getHub().getAO();
	}

	public Hub<WOItem> getHub() {
		if (hub == null) {
			hub = new Hub<WOItem>(WOItem.class);
		}
		return hub;
	}

	public Hub<WOItem> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<WOItem>(WOItem.class);
		}
		return hubMultiSelect;
	}

	public OrderItemModel getOrderItemModel() {
		if (modelOrderItem != null) {
			return modelOrderItem;
		}
		modelOrderItem = new OrderItemModel(getOrderItemHub());
		modelOrderItem.setDisplayName("Order Item");
		modelOrderItem.setPluralDisplayName("Order Items");
		modelOrderItem.setForJfc(getForJfc());
		modelOrderItem.setAllowNew(false);
		modelOrderItem.setAllowSave(true);
		modelOrderItem.setAllowAdd(false);
		modelOrderItem.setAllowRemove(false);
		modelOrderItem.setAllowClear(false);
		modelOrderItem.setAllowDelete(false);
		modelOrderItem.setAllowSearch(false);
		modelOrderItem.setAllowHubSearch(true);
		modelOrderItem.setAllowGotoEdit(true);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelOrderItem.setCreateUI(li == null || !WOItem.P_OrderItem.equals(li.getName()));
		modelOrderItem.setViewOnly(getViewOnly());
		// call WOItem.onEditQueryOrderItem(OrderItemModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(WOItem.class, WOItem.P_OrderItem, modelOrderItem);

		return modelOrderItem;
	}

	public WorkOrderModel getWorkOrderModel() {
		if (modelWorkOrder != null) {
			return modelWorkOrder;
		}
		modelWorkOrder = new WorkOrderModel(getWorkOrderHub());
		modelWorkOrder.setDisplayName("Work Order");
		modelWorkOrder.setPluralDisplayName("Work Orders");
		modelWorkOrder.setForJfc(getForJfc());
		modelWorkOrder.setAllowNew(false);
		modelWorkOrder.setAllowSave(true);
		modelWorkOrder.setAllowAdd(false);
		modelWorkOrder.setAllowRemove(false);
		modelWorkOrder.setAllowClear(false);
		modelWorkOrder.setAllowDelete(false);
		modelWorkOrder.setAllowSearch(false);
		modelWorkOrder.setAllowHubSearch(true);
		modelWorkOrder.setAllowGotoEdit(true);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelWorkOrder.setCreateUI(li == null || !WOItem.P_WorkOrder.equals(li.getName()));
		modelWorkOrder.setViewOnly(getViewOnly());
		// call WOItem.onEditQueryWorkOrder(WorkOrderModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(WOItem.class, WOItem.P_WorkOrder, modelWorkOrder);

		return modelWorkOrder;
	}

	public HubCopy<WOItem> createHubCopy() {
		Hub<WOItem> hubWOItemx = new Hub<>(WOItem.class);
		HubCopy<WOItem> hc = new HubCopy<>(getHub(), hubWOItemx, true);
		return hc;
	}

	public WOItemModel createCopy() {
		WOItemModel mod = new WOItemModel(createHubCopy().getHub());
		return mod;
	}
}
