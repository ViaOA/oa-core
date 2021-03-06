// Generated by OABuilder

package com.cdi.model;

import java.util.logging.Logger;

import com.cdi.model.oa.Delivery;
import com.cdi.model.oa.DeliveryTruck;
import com.cdi.model.oa.WODelivery;
import com.cdi.model.oa.WorkOrder;
import com.cdi.model.oa.propertypath.WODeliveryPP;
import com.cdi.model.search.DeliveryTruckSearchModel;
import com.viaoa.filter.OAInFilter;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;
import com.viaoa.util.OAFilter;

public class WODeliveryModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(WODeliveryModel.class.getName());

	// Hubs
	protected Hub<WODelivery> hub;
	// selected woDeliveries
	protected Hub<WODelivery> hubMultiSelect;
	// detail hubs
	protected Hub<Delivery> hubDelivery;
	protected Hub<WorkOrder> hubWorkOrder;
	protected Hub<DeliveryTruck> hubDeliveryTrucks;

	// ObjectModels
	protected DeliveryModel modelDelivery;
	protected WorkOrderModel modelWorkOrder;
	protected DeliveryTruckModel modelDeliveryTrucks;

	// SearchModels used for references
	protected DeliveryTruckSearchModel modelDeliveryTrucksSearch;

	public WODeliveryModel() {
		setDisplayName("WO Delivery");
		setPluralDisplayName("WO Deliveries");
	}

	public WODeliveryModel(Hub<WODelivery> hubWODelivery) {
		this();
		if (hubWODelivery != null) {
			HubDelegate.setObjectClass(hubWODelivery, WODelivery.class);
		}
		this.hub = hubWODelivery;
	}

	public WODeliveryModel(WODelivery woDelivery) {
		this();
		getHub().add(woDelivery);
		getHub().setPos(0);
	}

	public Hub<WODelivery> getOriginalHub() {
		return getHub();
	}

	public Hub<Delivery> getDeliveryHub() {
		if (hubDelivery != null) {
			return hubDelivery;
		}
		hubDelivery = getHub().getDetailHub(WODelivery.P_Delivery);
		return hubDelivery;
	}

	public Hub<WorkOrder> getWorkOrderHub() {
		if (hubWorkOrder != null) {
			return hubWorkOrder;
		}
		hubWorkOrder = getHub().getDetailHub(WODelivery.P_WorkOrder);
		return hubWorkOrder;
	}

	public Hub<DeliveryTruck> getDeliveryTrucks() {
		if (hubDeliveryTrucks == null) {
			hubDeliveryTrucks = getHub().getDetailHub(WODelivery.P_DeliveryTrucks);
		}
		return hubDeliveryTrucks;
	}

	public WODelivery getWODelivery() {
		return getHub().getAO();
	}

	public Hub<WODelivery> getHub() {
		if (hub == null) {
			hub = new Hub<WODelivery>(WODelivery.class);
		}
		return hub;
	}

	public Hub<WODelivery> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<WODelivery>(WODelivery.class);
		}
		return hubMultiSelect;
	}

	public DeliveryModel getDeliveryModel() {
		if (modelDelivery != null) {
			return modelDelivery;
		}
		modelDelivery = new DeliveryModel(getDeliveryHub());
		modelDelivery.setDisplayName("Delivery");
		modelDelivery.setPluralDisplayName("Deliveries");
		modelDelivery.setForJfc(getForJfc());
		modelDelivery.setAllowNew(false);
		modelDelivery.setAllowSave(true);
		modelDelivery.setAllowAdd(false);
		modelDelivery.setAllowRemove(false);
		modelDelivery.setAllowClear(false);
		modelDelivery.setAllowDelete(false);
		modelDelivery.setAllowSearch(false);
		modelDelivery.setAllowHubSearch(true);
		modelDelivery.setAllowGotoEdit(true);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelDelivery.setCreateUI(li == null || !WODelivery.P_Delivery.equals(li.getName()));
		modelDelivery.setViewOnly(true);
		// call WODelivery.onEditQueryDelivery(DeliveryModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(WODelivery.class, WODelivery.P_Delivery, modelDelivery);

		return modelDelivery;
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
		modelWorkOrder.setCreateUI(li == null || !WODelivery.P_WorkOrder.equals(li.getName()));
		modelWorkOrder.setViewOnly(true);
		// call WODelivery.onEditQueryWorkOrder(WorkOrderModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(WODelivery.class, WODelivery.P_WorkOrder, modelWorkOrder);

		return modelWorkOrder;
	}

	public DeliveryTruckModel getDeliveryTrucksModel() {
		if (modelDeliveryTrucks != null) {
			return modelDeliveryTrucks;
		}
		modelDeliveryTrucks = new DeliveryTruckModel(getDeliveryTrucks());
		modelDeliveryTrucks.setDisplayName("Delivery Truck");
		modelDeliveryTrucks.setPluralDisplayName("Delivery Trucks");
		if (HubDetailDelegate.getLinkInfoFromMasterToDetail(getOriginalHub().getMasterHub()) == HubDetailDelegate
				.getLinkInfoFromMasterToDetail(getDeliveryTrucks())) {
			modelDeliveryTrucks.setCreateUI(false);
		}
		modelDeliveryTrucks.setForJfc(getForJfc());
		modelDeliveryTrucks.setAllowNew(false);
		modelDeliveryTrucks.setAllowSave(true);
		modelDeliveryTrucks.setAllowAdd(true);
		modelDeliveryTrucks.setAllowMove(false);
		modelDeliveryTrucks.setAllowRemove(true);
		modelDeliveryTrucks.setAllowDelete(false);
		modelDeliveryTrucks.setAllowSearch(false);
		modelDeliveryTrucks.setAllowHubSearch(true);
		modelDeliveryTrucks.setAllowGotoEdit(true);
		modelDeliveryTrucks.setViewOnly(getViewOnly());
		modelDeliveryTrucks.setAllowTableFilter(true);
		modelDeliveryTrucks.setAllowTableSorting(true);
		// default is always false for these, can be turned by custom code in editQuery call (below)
		modelDeliveryTrucks.setAllowMultiSelect(false);
		modelDeliveryTrucks.setAllowCopy(false);
		modelDeliveryTrucks.setAllowCut(false);
		modelDeliveryTrucks.setAllowPaste(false);
		// call WODelivery.onEditQueryDeliveryTrucks(DeliveryTruckModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(WODelivery.class, WODelivery.P_DeliveryTrucks, modelDeliveryTrucks);

		return modelDeliveryTrucks;
	}

	public DeliveryTruckSearchModel getDeliveryTrucksSearchModel() {
		if (modelDeliveryTrucksSearch != null) {
			return modelDeliveryTrucksSearch;
		}
		modelDeliveryTrucksSearch = new DeliveryTruckSearchModel() {
			@Override
			public void performSearch() {
				WODelivery woDelivery = WODeliveryModel.this.getHub().getAO();
				if (woDelivery == null) {
					// dont allow search, cant apply required filter
					getHub().clear();
					return;
				}
				OAFilter filter = new OAInFilter(woDelivery, WODeliveryPP.delivery().deliveryTrucks().pp);
				getDeliveryTruckSearch().setExtraWhereFilter(filter);
				super.performSearch();
			}
		};
		return modelDeliveryTrucksSearch;
	}

	public HubCopy<WODelivery> createHubCopy() {
		Hub<WODelivery> hubWODeliveryx = new Hub<>(WODelivery.class);
		HubCopy<WODelivery> hc = new HubCopy<>(getHub(), hubWODeliveryx, true);
		return hc;
	}

	public WODeliveryModel createCopy() {
		WODeliveryModel mod = new WODeliveryModel(createHubCopy().getHub());
		return mod;
	}
}
