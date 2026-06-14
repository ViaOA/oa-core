package com.test.pos.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.annotation.*;
import com.viaoa.lang.*;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.datetime.OADateTime;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "onlineOrder",
    pluralName = "OnlineOrders",
    shortName = "ono",
    displayName = "Online Order",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "OnlineOrderCustomer", fkey = true, columns = { @OAIndexColumn(name = "CustomerId") })
    }
)
public class OnlineOrder extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(OnlineOrder.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
     
    public static final String P_Customer = "customer";
    public static final String P_CustomerId = "customerId"; // fkey
    public static final String P_OnlineOrderDeliveries = "onlineOrderDeliveries";
    public static final String P_OnlineOrderItems = "onlineOrderItems";
     
    protected volatile int id;
    protected volatile OADateTime created;
     
    // Links to other objects.
    protected volatile transient Customer customer;
    protected transient Hub<OnlineOrderDelivery> hubOnlineOrderDeliveries;
    protected transient Hub<OnlineOrderItem> hubOnlineOrderItems;
     
    public OnlineOrder() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public OnlineOrder(int id) {
        this();
        setId(id);
    }

    @OAProperty(lowerName = "id", isUnique = true, trackPrimitiveNull = false, displayLength = 6)
    @OAId
    @OAColumn(name = "Id", sqlType = java.sql.Types.INTEGER)
    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        int old = id;
        fireBeforePropertyChange(P_Id, old, newValue);
        this.id = newValue;
        firePropertyChange(P_Id, old, this.id);
    }

    @OAProperty(lowerName = "created", defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true, ignoreTimeZone = true)
    @OAColumn(name = "Created", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getCreated() {
        return created;
    }
    public void setCreated(OADateTime newValue) {
        OADateTime old = created;
        fireBeforePropertyChange(P_Created, old, newValue);
        this.created = newValue;
        firePropertyChange(P_Created, old, this.created);
    }

    @OAOne(
        reverseName = Customer.P_OnlineOrders, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_CustomerId, toProperty = Customer.P_Id)}
    )
    public Customer getCustomer() {
        if (customer == null) {
            customer = (Customer) getObject(P_Customer);
        }
        return customer;
    }
    public void setCustomer(Customer newValue) {
        Customer old = this.customer;
        fireBeforePropertyChange(P_Customer, old, newValue);
        this.customer = newValue;
        firePropertyChange(P_Customer, old, this.customer);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "CustomerId")
    public Integer getCustomerId() {
        return (Integer) getFkeyProperty(P_CustomerId);
    }
    public void setCustomerId(Integer newValue) {
        this.customer = null;
        setFkeyProperty(P_CustomerId, newValue);
    }

    @OAMany(
        displayName = "Online Order Deliveries", 
        toClass = OnlineOrderDelivery.class, 
        reverseName = OnlineOrderDelivery.P_OnlineOrder
    )
    public Hub<OnlineOrderDelivery> getOnlineOrderDeliveries() {
        if (hubOnlineOrderDeliveries == null) {
            hubOnlineOrderDeliveries = (Hub<OnlineOrderDelivery>) getHub(P_OnlineOrderDeliveries);
        }
        return hubOnlineOrderDeliveries;
    }

    @OAMany(
        displayName = "Online Order Items", 
        toClass = OnlineOrderItem.class, 
        owner = true, 
        reverseName = OnlineOrderItem.P_OnlineOrder, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<OnlineOrderItem> getOnlineOrderItems() {
        if (hubOnlineOrderItems == null) {
            hubOnlineOrderItems = (Hub<OnlineOrderItem>) getHub(P_OnlineOrderItems);
        }
        return hubOnlineOrderItems;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        int customerFkey = rs.getInt(3);
        setFkeyProperty(P_Customer, rs.wasNull() ? null : customerFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
