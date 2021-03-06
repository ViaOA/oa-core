// Generated by OABuilder
package com.cdi.model.oa;
 
import java.util.logging.*;
import java.sql.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.annotation.*;
import com.cdi.delegate.oa.*;
import com.cdi.model.oa.filter.*;
import com.cdi.model.oa.propertypath.*;
import com.viaoa.util.OADate;
import com.viaoa.util.OATime;
import java.awt.Color;
 
@OAClass(
    shortName = "dlt",
    displayName = "Delivery Truck",
    displayProperty = "truck"
)
@OATable(
    indexes = {
        @OAIndex(name = "DeliveryTruckDeliveryDate", columns = {@OAIndexColumn(name = "DeliveryDate")}),
        @OAIndex(name = "DeliveryTruckDelivery", fkey = true, columns = { @OAIndexColumn(name = "DeliveryId") }), 
        @OAIndex(name = "DeliveryTruckTruck", fkey = true, columns = { @OAIndexColumn(name = "TruckId") })
    }
)
public class DeliveryTruck extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(DeliveryTruck.class.getName());

    public static final String PROPERTY_Id = "Id";
    public static final String P_Id = "Id";
    public static final String PROPERTY_DeliveryDate = "DeliveryDate";
    public static final String P_DeliveryDate = "DeliveryDate";
    public static final String PROPERTY_ScheduledTime = "ScheduledTime";
    public static final String P_ScheduledTime = "ScheduledTime";
    public static final String PROPERTY_Weight = "Weight";
    public static final String P_Weight = "Weight";
    public static final String PROPERTY_Footage = "Footage";
    public static final String P_Footage = "Footage";
    public static final String PROPERTY_Driver = "Driver";
    public static final String P_Driver = "Driver";
    public static final String PROPERTY_Notes = "Notes";
    public static final String P_Notes = "Notes";
     
    public static final String PROPERTY_Display = "Display";
    public static final String P_Display = "Display";
    public static final String PROPERTY_IconColor = "IconColor";
    public static final String P_IconColor = "IconColor";
     
    public static final String PROPERTY_Delivery = "Delivery";
    public static final String P_Delivery = "Delivery";
    public static final String PROPERTY_Truck = "Truck";
    public static final String P_Truck = "Truck";
    public static final String PROPERTY_WODeliveries = "WODeliveries";
    public static final String P_WODeliveries = "WODeliveries";
     
    protected volatile int id;
    protected volatile OADate deliveryDate;
    protected volatile OATime scheduledTime;
    protected volatile int weight;
    protected volatile int footage;
    protected volatile String driver;
    protected volatile String notes;
     
    // Links to other objects.
    protected volatile transient Delivery delivery;
    protected volatile transient Truck truck;
    protected transient Hub<WODelivery> hubWODeliveries;
     
    public DeliveryTruck() {
    }
     
    public DeliveryTruck(int id) {
        this();
        setId(id);
    }
     

    @OAProperty(isUnique = true, displayLength = 6)
    @OAId()
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        int old = id;
        fireBeforePropertyChange(P_Id, old, newValue);
        this.id = newValue;
        firePropertyChange(P_Id, old, this.id);
    }
     
    @OAProperty(displayName = "Delivery Date", displayLength = 8, columnLength = 13)
    @OAColumn(sqlType = java.sql.Types.DATE)
    public OADate getDeliveryDate() {
        return deliveryDate;
    }
    public void setDeliveryDate(OADate newValue) {
        OADate old = deliveryDate;
        fireBeforePropertyChange(P_DeliveryDate, old, newValue);
        this.deliveryDate = newValue;
        firePropertyChange(P_DeliveryDate, old, this.deliveryDate);
    }
     
    @OAProperty(displayName = "Scheduled Time", displayLength = 8, columnLength = 14)
    @OAColumn(sqlType = java.sql.Types.TIME)
    public OATime getScheduledTime() {
        return scheduledTime;
    }
    public void setScheduledTime(OATime newValue) {
        OATime old = scheduledTime;
        fireBeforePropertyChange(P_ScheduledTime, old, newValue);
        this.scheduledTime = newValue;
        firePropertyChange(P_ScheduledTime, old, this.scheduledTime);
    }
     
    @OAProperty(displayLength = 6)
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getWeight() {
        return weight;
    }
    public void setWeight(int newValue) {
        int old = weight;
        fireBeforePropertyChange(P_Weight, old, newValue);
        this.weight = newValue;
        firePropertyChange(P_Weight, old, this.weight);
    }
     
    @OAProperty(displayLength = 6, columnLength = 7)
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getFootage() {
        return footage;
    }
    public void setFootage(int newValue) {
        int old = footage;
        fireBeforePropertyChange(P_Footage, old, newValue);
        this.footage = newValue;
        firePropertyChange(P_Footage, old, this.footage);
    }
     
    @OAProperty(maxLength = 75, displayLength = 20)
    @OAColumn(maxLength = 75)
    public String getDriver() {
        return driver;
    }
    public void setDriver(String newValue) {
        String old = driver;
        fireBeforePropertyChange(P_Driver, old, newValue);
        this.driver = newValue;
        firePropertyChange(P_Driver, old, this.driver);
    }
     
    @OAProperty(displayLength = 20)
    @OAColumn(sqlType = java.sql.Types.CLOB)
    public String getNotes() {
        return notes;
    }
    public void setNotes(String newValue) {
        String old = notes;
        fireBeforePropertyChange(P_Notes, old, newValue);
        this.notes = newValue;
        firePropertyChange(P_Notes, old, this.notes);
    }
     
    @OACalculatedProperty(displayLength = 20, properties = {P_DeliveryDate, P_Truck+"."+Truck.P_Carrier, P_WODeliveries+"."+WODelivery.P_WorkOrder})
    public String getDisplay() {
        String display = "";
    
        String carrier = null;
        Truck truck = this.getTruck();
        if (truck != null) {
            carrier = truck.getCarrier();
        }
        display = OAString.concat(display, carrier, " ");
    
    /*
        String calcSalesOrderNumber = null;
        WorkOrder workOrder = this.getWorkOrder();
        if (workOrder != null) {
            calcSalesOrderNumber = workOrder.getCalcSalesOrderNumber();
        }
        display = OAString.concat(display, calcSalesOrderNumber, " ");
    */
        return display;
    }
     
    @OACalculatedProperty(displayName = "Icon Color", displayLength = 12, columnLength = 10, properties = {P_WODeliveries+"."+WODelivery.P_WorkOrder+"."+WorkOrder.P_IconColor})
    public Color getIconColor() {
        int x;
        if (getWODeliveries().size() == 0) x = WorkOrder.STATUSCODE_Red;
        else {
            x = WorkOrder.STATUSCODE_White;
            for (WODelivery wod : getWODeliveries()) {
                x = Math.min(x, wod.getWorkOrder().getCalcStatusCode());
            }
        }
        return WorkOrderDelegate.getStatusCodeColor(x);
    }
     
    @OAOne(
        reverseName = Delivery.P_DeliveryTrucks, 
        required = true, 
        allowCreateNew = false
    )
    @OAFkey(columns = {"DeliveryId"})
    public Delivery getDelivery() {
        if (delivery == null) {
            delivery = (Delivery) getObject(P_Delivery);
        }
        return delivery;
    }
    public void setDelivery(Delivery newValue) {
        Delivery old = this.delivery;
        fireBeforePropertyChange(P_Delivery, old, newValue);
        this.delivery = newValue;
        firePropertyChange(P_Delivery, old, this.delivery);
        
        if (newValue == null) return;
        // custom: support for this.DND to another delivery (date), so that the WODelieries also get moved
        for (WODelivery wod : getWODeliveries()) {
            wod.setDelivery(newValue);
        }
    }
     
    @OAOne(
        reverseName = Truck.P_DeliveryTrucks, 
        cascadeSave = true, 
        allowCreateNew = false, 
        allowAddExisting = false
    )
    @OAFkey(columns = {"TruckId"})
    public Truck getTruck() {
        if (truck == null) {
            truck = (Truck) getObject(P_Truck);
        }
        return truck;
    }
    public void setTruck(Truck newValue) {
        Truck old = this.truck;
        fireBeforePropertyChange(P_Truck, old, newValue);
        this.truck = newValue;
        firePropertyChange(P_Truck, old, this.truck);
    }
     
    @OAMany(
        displayName = "WO Deliveries", 
        toClass = WODelivery.class, 
        reverseName = WODelivery.P_DeliveryTrucks
    )
    @OALinkTable(name = "WODeliveryDeliveryTruck", indexName = "WODeliveryDeliveryTruck", columns = {"DeliveryTruckId"})
    public Hub<WODelivery> getWODeliveries() {
        if (hubWODeliveries == null) {
            hubWODeliveries = (Hub<WODelivery>) getHub(P_WODeliveries);
        }
        return hubWODeliveries;
    }
     
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Date date;
        date = rs.getDate(2);
        if (date != null) this.deliveryDate = new OADate(date);
        java.sql.Time time;
        time = rs.getTime(3);
        if (time != null) this.scheduledTime = new OATime(time);
        this.weight = (int) rs.getInt(4);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, DeliveryTruck.P_Weight, true);
        }
        this.footage = (int) rs.getInt(5);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, DeliveryTruck.P_Footage, true);
        }
        this.driver = rs.getString(6);
        this.notes = rs.getString(7);
        int deliveryFkey = rs.getInt(8);
        if (!rs.wasNull() && deliveryFkey > 0) {
            setProperty(P_Delivery, new OAObjectKey(deliveryFkey));
        }
        int truckFkey = rs.getInt(9);
        if (!rs.wasNull() && truckFkey > 0) {
            setProperty(P_Truck, new OAObjectKey(truckFkey));
        }
        if (rs.getMetaData().getColumnCount() != 9) {
            throw new SQLException("invalid number of columns for load method");
        }

        changedFlag = false;
        newFlag = false;
    }
}
 
