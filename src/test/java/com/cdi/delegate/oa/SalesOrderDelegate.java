package com.cdi.delegate.oa;

import java.util.ArrayList;
import java.util.logging.Logger;
import com.cdi.model.oa.*;
import com.viaoa.hub.Hub;
import com.viaoa.util.*;

public class SalesOrderDelegate {
    private static Logger LOG = Logger.getLogger(SalesOrderDelegate.class.getName());
    
    public static int getTotalWeight(SalesOrder salesOrder) {
        if (salesOrder == null) return 0;
        
        Hub<SalesOrderItem> hub = salesOrder.getSalesOrderItems();
        int total = 0;
        for (SalesOrderItem soi : hub) {
            total += soi.getTotalWeight();
        }
        return total;
    }

    /**
     * Get error messages for an SalesOrder
     * @return array of errrors that can be displayed to user
     */
    public static String[] getInvalidReasons(final SalesOrder order, final boolean bForSubmit) {
        
        if (order == null) return null;
        ArrayList<String> al = new ArrayList<String>();

        // PriceCode        
        PriceCode pc = order.getPriceCode();
        if (pc == null) al.add("Price Code is required");

        // Expected date        
        // if (bForSubmit && order.getDateExpected() == null) al.add("Expected Date is missing");
        
        // TaxRate        
        double bd = order.getTaxRate();
        if (bd == 0.0d) {
            SalesCustomer cust = order.getSalesCustomer();
            if (cust == null || !cust.getTaxExempt()) {
                if (bForSubmit) al.add("Tax rate is required, or Customer needs to be tax exempt in Quickbooks.");
            }
        }
        
        // Color        
        if (bForSubmit && OAString.isEmpty(order.getColor())) al.add("Color is required");
        // Texture
        if (bForSubmit && OAString.isEmpty(order.getTexture())) al.add("Texture is required");
        
        
        // SalesCustomer, number, ship address, etc
        if (bForSubmit) {
            SalesCustomer salesCust = order.getSalesCustomer();
            if (salesCust == null) {
                al.add("Customer is null");
            }
            else {
                Customer cust = salesCust.getCustomer();
                if (cust == null) {
                    al.add("An existing customer is required.");
                }
                else if (OAString.isEmpty(cust.getQbListId())) {
                    al.add("quickbooks id is missing for this customer");                
                }
                
                /*
                String s = salesCust.getCustomerNumber();
                if (OAString.isEmpty(s)) al.add("Customer number is required.");
                else {
                    Customer customer;
                    Hub<Customer> h = new Hub(Customer.class);
                    h.select(Customer.PROPERTY_CustomerNumber + " = '" + salesCust.getCustomerNumber() + "'");
                    customer = h.getAt(0);
                    if (customer == null) al.add("customer not found");
                    else {
                        if (OAString.isEmpty(customer.getQbListId())) {
                            al.add("customer not found in Quickbooks");                
                        }
                    }
                }
                */
                
                if (!order.getCustomerPickUp()) {
                    ShipTo st = salesCust.getShipTo();
                    if (st == null) al.add("Ship to is required");
                    else {
                        if (OAString.isEmpty(st.getAddress())) al.add("Ship to address is required");
                        // if (OAString.isEmpty(st.getAddress2())) al.add("Ship to address2 is required");
                        if (OAString.isEmpty(st.getCity())) al.add("Ship to city is required");
                        if (OAString.isEmpty(st.getState())) al.add("Ship to state is required");
    
                        // 20100726: zip must be 5+ alphanumeric
                        String zip = st.getZip();
                        if (zip == null) zip = "";
                        int cntDigit = 0;
                        int x = zip.length();
                        for (int i=0; i<x ; i++) {
                            if (Character.isLetterOrDigit(zip.charAt(i))) cntDigit++;
                        }
                        if (cntDigit < 5) al.add("Ship to zip code (5+ alpha-numeric) is required");
                    }
                }
            }
        
            // SalesPerson
            User salesPersonUser = order.getSalesPersonUser();
            if (salesPersonUser == null) {
                al.add("SalesPerson is required");
            }
            else {
                String s = salesPersonUser.getCode();
                // if (OAString.isEmpty(s)) al.add("SalesPerson Code is empty, please have an Admin user set it");
            }
        }
        
        boolean bCustomError = false;
        Hub<SalesOrderItem> hub = order.getSalesOrderItems();
        int i = 0;
        for (SalesOrderItem soi : hub) {
            // no custom items
            i++;
            if (bForSubmit && soi.getCustomItem() && !bCustomError) {
                al.add("Custom items must be converted to Items");
                bCustomError = true;
            }
            
            // service codes must have item number
            ServiceCode sc = soi.getServiceCode();
            if (sc != null) {
                if (OAString.isEmpty(sc.getQbListId())) {
                    if (bForSubmit) al.add("Service Code on line "+i+" was not found in Quickbooks");
                }

                if (OAString.isEmpty(sc.getItemCode())) {
                    if (bForSubmit)  al.add("Service Code "+sc.getName()+" on line "+i+" must have an item number set up in Admin");
                }
            }
            
            Item item = soi.getItem();
            if (item != null) {
                if (OAString.isEmpty(item.getQbListId())) {
                    if (bForSubmit) al.add("Item on line "+i+" was not found in Quickbooks");
                }
                if (soi.getQuantity() < 1) {
                    al.add("Item on line "+i+" does not have a quantity");
                }
                if ((bForSubmit || order.getDateSubmitted() == null) && item.getDiscontinuedDate() != null) {
                    al.add("Item on line "+i+" is discontinued");
                }
            }
            else if (soi.getCustomItem()) {
                if (soi.getUnitPrice() == 0.0d) {
                    al.add("Custom Item on line "+i+" does not have a price");
                }
                if (soi.getUnitWeight() == 0) {
                    al.add("Custom Item on line "+i+" does not have a unit weight");
                }
            }
        }
        
        if (al.isEmpty()) return null;
        return (String[]) al.toArray(new String[0]);
    }
    
    public static String getCurrentDateStatus(SalesOrder so) {
        if (so == null) return "";
        OADate d = so.getDateSubmitted();
        if (d != null) return "Sent EDI " + d.toString("MM/dd/yy");
        
        d = so.getDateComplete();
        if (d != null) return "Completed " + d.toString("MM/dd/yy");

        d = so.getDateClosed();
        if (d != null) return "Closed " + d.toString("MM/dd/yy");
        
        return "Open";
    }
    
    public static OADate getLastFollowUp(SalesOrder order) {
        if (order == null) return null;
        OADate date = null;
        Hub<SalesOrderNote> h = order.getSalesOrderNotes();
        for (SalesOrderNote note : h) {
            if (note.getFollowedUp()) continue;
            OADate d = note.getFollowupDate();
            if (d != null && d.after(date)) date = d;
        }
        return date;
    }

}

