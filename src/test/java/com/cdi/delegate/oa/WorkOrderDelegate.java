package com.cdi.delegate.oa;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Polygon;
import java.util.logging.Logger;

import javax.swing.Icon;

import com.cdi.model.oa.*;
import com.cdi.model.oa.propertypath.WorkOrderPP;
import com.viaoa.hub.*;
import com.viaoa.object.OAFinder;
import com.viaoa.util.OADate;


/**
 * Used by Order.getWOItems()
 * @author vvia
 *
 */
public class WorkOrderDelegate {

	private static Logger LOG = Logger.getLogger(WorkOrderDelegate.class.getName());

    // total time needed for all items on the order.

	public static Truck getFirstTruck(WorkOrder wo) {
        if (wo == null) return null;
        OAFinder<WorkOrder, Truck> find = new OAFinder<>(WorkOrderPP.woDeliveries().deliveryTrucks().truck().pp);
        Truck t = find.findFirst(wo);
	    return t;
	}
	public static String getFirstCarrierName(WorkOrder wo) {
		Truck t = getFirstTruck(wo);
		if (t == null) return "";
		return t.getCarrier();
	}
	
	public static boolean isCompleted(final WorkOrder wo) {
		if (wo != null) {
			if (wo.getCompleteDate() != null) return true;
			Order order = wo.getOrder();
			if (order != null && order.getDateCompleted() != null) return true;
		}
		return false;
	}
	
	public static String getCarrierDescription(final WorkOrder workOrder) {
	    if (workOrder == null) return null;
	    Order order = workOrder.getOrder();
	    if (order == null) return null;
	    String text;
	
	    OADate date = null;
	    WODelivery wotx = workOrder.getWODeliveries().getAt(0);
	    if (wotx != null) {
	        Delivery delivery = wotx.getDelivery();
	        if (delivery != null) date = delivery.getDate();
	    }
	    
	    if (order.getCustomerPickUp()) {
	        text = "Customer Pickup"; 
	        if (date != null) {
	            text += " set for " + date.toString("E MM/dd");
	        }
	        else {
	            text += ", date has not been set";
	        }
	    }
	    else {
	        text = "";
	        for (WODelivery wot : workOrder.getWODeliveries()) {
	            for (DeliveryTruck dt : wot.getDeliveryTrucks()) {
    	            Truck t = dt.getTruck();
    	            if (t == null) continue;
    	            String s = t.getCarrier();
    	            if (s != null && s.length() > 0) {
    	                if (text.length() == 0) text = "Carrier is ";
    	                else text += ", and ";
    	                text += s;
    	                if (date == null) text += " (no date)";
    	                else text += " on " + date.toString("E MM/dd");
    	            }
	            }
	        }
	        if (text.length() == 0) {
	            text = "Carrier has not been assigned";
	            if (date != null) {
	                text = "Ship date set for " + date.toString("E MM/dd") + ", " + text;
	            }
	            else {
	                text = "Ship date has not been set, " + text;
	            }
	        }
	    }
	    return text;
	}

	
	public static String getDeliveryDateError(WorkOrder wo) {
        //LOG.fine("called");
		if (wo == null || isCompleted(wo)) return null;
		Order order = wo.getOrder();
		if (order == null) return null;
		if (order.getCustomerPickUp()) {
			if (wo.getWODeliveries().getSize() > 0) {
			    String msg = "Customer pickup, but carrier is assigned";
	            msg += "<br>Go to \"Delivery\" tab to set or remove the Carrier.";
	            return msg;
			}
		}

		OADate dd = null;
        WODelivery wot = wo.getWODeliveries().getAt(0);
        if (wot != null) {
            Delivery ddx = wot.getDelivery();
            if (ddx != null) dd = ddx.getDate();
        }
		
    	if (dd == null) {
    	    String msg; 
    		if (order.getCustomerPickUp()) msg = "Customer pickup date has not been selected.";
    		else msg = "Carrier ship date has not been selected.";
    		msg += "<br>Go to \"Delivery\" tab to set the <i>shipping/delivery date</i>.";
    		return msg;
    	}
    	else {
    	    OADate d1 = order.getExpectedShipDate();
    	    OADate d2 = dd;
    	    if (d1 != null && d1.before(d2)) {
                String msg = "The ShipDate of "+d2+" has been set after the customers expected date of "+d1;
                return msg;
    	    }
    	}
		if (!order.getCustomerPickUp()) {
			if (wo.getWODeliveries().getSize() == 0) {
			    String msg = "Carrier has not been assigned";
	            msg += "<br>Go to \"Delivery\" tab to set the <i>shipping/delivery date</i>.";
	            return msg;
			}
		}
		return null;
	}
	
    public static String getInspectionError(WorkOrder wo) {
        //LOG.fine("called");
		if (wo == null || isCompleted(wo)) return null;
		if (wo.getInspectDate() != null) return null;
		if (wo.getInspectByDate() == null) return "Inspection date has not been assigned";
		if (wo.getInspectUser() == null) return "Inspection employee has not been assigned";
		return "Inspection not completed";
    }
    public static String getProductionDateError(WorkOrder wo) {
        //LOG.fine("called");
		if (wo == null || isCompleted(wo)) return null;
		if (wo.getScheduledEndDate() == null) return "Production end date has not been assigned";
		return null;
    }

    public static Color getStatusCodeColor(WorkOrder wo) {
        int x = wo.getStatusCode();
        return getStatusCodeColor(x);
    }
    
    public static Color getStatusCodeColor(int statusCode) {
        if (statusCode < 0 || statusCode >= colors.length) statusCode = 0;
        return colors[statusCode];
    }

    public static Icon getStatusCodeIcon(int statusCode) {
        if (statusCode < 0 || statusCode >= colorIcons.length) statusCode = 0;
        return colorIcons[statusCode];
    }
    
	private static final MyColorIcon[] colorIcons = {
        new MyColorIcon(new Color(136, 0, 136), Color.red),
		new MyColorIcon(Color.red),
        new MyColorIcon(new Color(136, 0, 136)),  // purple
		new MyColorIcon(Color.orange),
		new MyColorIcon(Color.yellow),
		new MyColorIcon(Color.green),
		new MyColorIcon(Color.white)
	};
	private static final Color[] colors = {
        Color.red,
		Color.red,
		new Color(136, 0, 136),
		Color.orange,
		Color.yellow,
		Color.green,
		Color.white,
	};
	private static final String[] statusCodeDescriptions = {
        "Unassigned / zero molds",
		"Unassigned production",
        "Items without molds",
		"In Production",
		"Inspection needed",
		"Ready for shipping",
		"Completed"
	};
    
	
	public static String getStatusCodeDescription(int statusCode) {
        //LOG.fine("called");
        if (statusCode < 0 || statusCode >= statusCodeDescriptions.length) return null;
        return statusCodeDescriptions[statusCode];
	}
	public static Icon getRedIcon() {
		return colorIcons[WorkOrder.STATUSCODE_Red]; 
	}
	public static Icon getGreenIcon() {
		return colorIcons[WorkOrder.STATUSCODE_Green]; 
	}

    public static boolean hasMoldsWithZeroOnHand(WorkOrder wo) {
        if (wo == null) return false;
        Hub<WOItem> h = wo.getWOItems();
        for (WOItem woi : h) {
            Mold mold = WOItemDelegate.getMold(woi);
            if (mold == null || mold.getOnHand() == 0) return true;
        }
        return false;
    }

	
    public static String getDeliveryTypeDescription(WorkOrder wo) {
        //LOG.fine("called");
    	if (wo == null) return null;
    	Order order = wo.getOrder();
    	if (order == null) return null;
    	String s;
    	
        OADate dd = null;
        WODelivery wotx = wo.getWODeliveries().getAt(0);
        if (wotx != null) {
            Delivery ddx = wotx.getDelivery();
            if (ddx != null) dd = ddx.getDate();
        }

    	if (order.getCustomerPickUp()) {
            s = "Customer Pickup"; 
            if (dd != null) {
                s += " set for " + dd.toString("E MM/dd");
            }
            else {
                s += ", date has not been set";
            }
    	}
    	else {
            s = "";
            Hub h2 = wo.getWODeliveries();
            for (int j=0; ;j++) {
                WODelivery wot = (WODelivery) h2.getAt(j);
                if (wot == null) break;
                for (DeliveryTruck dt : wot.getDeliveryTrucks()) {
                    Truck t = dt.getTruck();
                    if (t == null) continue;
                    String s2 = t.getCarrier();
                    if (s2 != null && s2.length() > 0) {
                        if (s.length() == 0) s = "Carrier is ";
                        else s += ", and ";
                        s += s2;
                        if (dd == null || dd.getDate() == null) s += " (no date)";
                        else s += " on " + dd.toString("E MM/dd");
                    }
                }
            }
            if (s.length() == 0) {
                s = "Carrier has not been assigned";
                
                if (dd != null) {
                    s = "Ship date set for " + dd.toString("E MM/dd") + ", " + s;
                }
                else {
                    s = "Ship date has not been set, " + s;
                }
            }
    	}
    	return s;
    }
}

class MyColorIcon implements Icon {
    Color color, color2;        
	public MyColorIcon(Color c) {
		this.color = c;
	}
    public MyColorIcon(Color c, Color c2) {
        this.color = c;
        this.color2 = c2;
    }
    public int getIconHeight() {
    	return 17;
    }
    public int getIconWidth() {
    	return 12;
    }

    public void paintIcon(Component comp,Graphics g,int x,int y) {
        Color c = color==null?Color.white:color;
        g.setColor(c);
        g.fillRect(x+1,y+3,12,12);
        if (color2 != null) {
            Polygon p = new Polygon();
            p.addPoint(13, 3);
            p.addPoint(1, 15);
            p.addPoint(13, 15);
            g.setColor(color2);
            g.fillPolygon(p);
        }
    }
}
