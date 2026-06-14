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
    lowerName = "customerCredit",
    pluralName = "CustomerCredits",
    shortName = "csc",
    displayName = "Customer Credit",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "CustomerCreditCustomer", fkey = true, columns = { @OAIndexColumn(name = "CustomerId") })
    }
)
public class CustomerCredit extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(CustomerCredit.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Limit = "limit";
     
    public static final String P_Customer = "customer";
    public static final String P_CustomerId = "customerId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile double limit;
     
    // Links to other objects.
    protected volatile transient Customer customer;
     
    public CustomerCredit() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public CustomerCredit(int id) {
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

    @OAProperty(lowerName = "limit", decimalPlaces = 2, isCurrency = true, displayLength = 9, uiColumnLength = 8)
    @OAColumn(name = "Limit", sqlType = java.sql.Types.NUMERIC)
    public double getLimit() {
        return limit;
    }
    public void setLimit(double newValue) {
        double old = limit;
        fireBeforePropertyChange(P_Limit, old, newValue);
        this.limit = newValue;
        firePropertyChange(P_Limit, old, this.limit);
    }

    @OAOne(
        reverseName = Customer.P_CustomerCredit, 
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
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.limit = rs.getDouble(3);
        setPrimitiveNull(P_Limit, rs.wasNull());
        int customerFkey = rs.getInt(4);
        setFkeyProperty(P_Customer, rs.wasNull() ? null : customerFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
