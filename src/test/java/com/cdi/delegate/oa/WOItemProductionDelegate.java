package com.cdi.delegate.oa;

import java.util.logging.Logger;

import com.cdi.model.oa.*;
import com.viaoa.hub.Hub;
import com.viaoa.util.OADate;

public class WOItemProductionDelegate {
	private static Logger LOG = Logger.getLogger(WOItemProductionDelegate.class.getName());
/**qqqqq	
	public static String getDisplay(WOItemProduction woItemProduction) {
        LOG.fine("called");
		String msg = "";
		if (woItemProduction != null) {
			msg = woItemProduction.getPropertyAsString(WOItemProduction.PROPERTY_Quantity);
			msg += " * ";
			msg += woItemProduction.getPropertyAsString(WOItemProduction.PROPERTY_WOItem+"."+WOItem.PROPERTY_OrderItem+"."+OrderItem.PROPERTY_Item+"."+Item.PROPERTY_Name);
		}
		return msg;
	}

	public static Mold getMold(WOItemProduction woip) {
        LOG.fine("called");
		if (woip == null) return null;
		WOItem woi = woip.getWOItem();
		if (woi == null) return null;
		OrderItem oi = woi.getOrderItem();
		if (oi == null) return null;
		Item item = oi.getItem();
		if (item == null) return null;
		Mold mold = item.getMold();
		return mold;
	}

	public static WorkOrder getWorkOrder(WOItemProduction woip) {
        LOG.fine("called");
		if (woip == null) return null;
		WOItem woi = woip.getWOItem();
		if (woi == null) return null;
		return woi.getWorkOrder();
	}
	

	public static boolean isValidQuantity(WOItemProduction woip) {
        LOG.fine("called");
		if (woip == null) return false;
		ProductionDate pd = woip.getProductionDate();
		if (pd == null || pd.getDate() == null) return true;
		
		Mold mold = getMold(woip);
		if (mold == null || mold.getOnHand() < 1) return false;
		if (woip.getQuantity() > mold.getOnHand()) return false;
		if (woip.getQuantity() < 0) return false;
		
		WOItem woi = woip.getWOItem();
		if (woi == null || woi.getQuantity() < woip.getQuantity()) return false;
		return true;
	}
	
	public static boolean isValidProductionDate(WOItemProduction woip) {
        LOG.fine("called");
		if (woip == null) return false;
		ProductionDate pd = woip.getProductionDate();
		if (pd == null || pd.getDate() == null) return true;
		WorkOrder wo = getWorkOrder(woip);
		if (wo == null) return false;
		OADate d = wo.getScheduledEndDate();
		if (d == null) return false;
		if (pd.getDate().compareTo(d) >= 0) return false;
		return true;
	}	

	
	public static boolean isCompleted(WOItemProduction woip) {
        LOG.fine("called");
		if (woip == null) return false;
		WOItem woi = woip.getWOItem();
		if (woi == null) return false;
		return WOItemDelegate.isCompleted(woi);
	}
	
	// ========== Called by UI ====================
	public static String getProductionDateError(WOItemProduction woip) {
        LOG.fine("called");
		if (woip == null) return null;
		if (isCompleted(woip)) return null;

		ProductionDate pd = woip.getProductionDate();
		if (pd == null || pd.getDate() == null) return "Production date has not been assigned";
		
		WorkOrder wo = getWorkOrder(woip);
		if (wo == null) return null;
		OADate d = wo.getScheduledEndDate();
		if (d == null) return null;
		if (pd.getDate().compareTo(d) >= 0) return "date after production end date for Work Order of "+d;
		return null;
	}
	
	public static String getQuantityError(WOItemProduction woip) {
        LOG.fine("called");
		if (woip == null) return null;
		if (isCompleted(woip)) return null;

		ProductionDate pd = woip.getProductionDate();
		if (pd == null || pd.getDate() == null) return "Production date has not been assigned";
		
		Mold mold = getMold(woip);
		if (mold == null || mold.getOnHand() < 1) return "Mold count is zero";
		if (woip.getQuantity() > mold.getOnHand()) return "quantity greater then Mold count of "+mold.getOnHand();
		if (woip.getQuantity() < 0) return "invalid quantity, less then zero";
		return null;
	}

	public static String getMoldError(WOItemProduction woip) {
        LOG.fine("called");
    	if (woip == null) return null;
		if (isCompleted(woip)) return null;
		WOItem woi = woip.getWOItem();
		if (woi == null) return null;
		return WOItemDelegate.getMoldError(woi);
    }
*/    
}




