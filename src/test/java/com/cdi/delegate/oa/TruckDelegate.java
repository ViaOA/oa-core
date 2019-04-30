package com.cdi.delegate.oa;

import java.math.BigDecimal;
import java.util.logging.Logger;

import com.viaoa.hub.*;
import com.cdi.model.oa.*;

public class TruckDelegate {
    private static Logger LOG = Logger.getLogger(TruckDelegate.class.getName());
    
/** 20190105 removed from Truck    
	public static int getTotalOrderWeight(Truck truck) {
        LOG.fine("called");
		if (truck == null) return 0;
		int total = 0;
		Hub hubWOTruck = truck.getWOTrucks();
		for (int i = 0;; i++) {
			WOTruck wot = (WOTruck) hubWOTruck.getAt(i);
			if (wot == null) break;
			WorkOrder wo = wot.getWorkOrder();
			if (wo == null) continue;
			Order ord = wo.getOrder();
			if (ord == null) continue;

            int totalx = ord.getCalcTotalWeight();
			if (totalx > 0) total += totalx;
			else {
    			Hub hubOrderItem = ord.getOrderItems();
                for (int j = 0;; j++) {
                    OrderItem oi = (OrderItem) hubOrderItem.getAt(j);
                    if (oi == null) break;
                    Item item = oi.getItem();
                    if (item == null) continue;
                    total += item.getWeight() * oi.getQuantity();
                }
			}			
		}
	    return total; 
	}
	public static double getTotalOrderPrice(Truck truck) {
        LOG.fine("called");
		if (truck == null) return 0.0;
		double total = 0.0d;
		Hub hubWOTruck = truck.getWOTrucks();
		for (int i = 0;; i++) {
			WOTruck wot = (WOTruck) hubWOTruck.getAt(i);
			if (wot == null) break;
			WorkOrder wo = wot.getWorkOrder();
			if (wo == null) continue;
			Order ord = wo.getOrder();
			if (ord == null) continue;
			double d = ord.getTotalPrice();
			total = total + d; 
		}
	    return total; 
	}
*/	
}
