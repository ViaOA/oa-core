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
    lowerName = "discountCoupon",
    pluralName = "DiscountCoupons",
    shortName = "dsc",
    displayName = "Discount Coupon",
    displayProperty = "id",
    noPojo = true
)
@OATable(
)
public class DiscountCoupon extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(DiscountCoupon.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Amount = "amount";
    public static final String P_Reference = "reference";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile double amount;
    protected volatile String reference;
     
    public DiscountCoupon() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public DiscountCoupon(int id) {
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

    @OAProperty(lowerName = "amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, uiColumnLength = 8)
    @OAColumn(name = "Amount", sqlType = java.sql.Types.NUMERIC)
    public double getAmount() {
        return amount;
    }
    public void setAmount(double newValue) {
        double old = amount;
        fireBeforePropertyChange(P_Amount, old, newValue);
        this.amount = newValue;
        firePropertyChange(P_Amount, old, this.amount);
    }

    @OAProperty(lowerName = "reference", maxLength = 35, displayLength = 18)
    @OAColumn(name = "Reference", maxLength = 35)
    public String getReference() {
        return reference;
    }
    public void setReference(String newValue) {
        String old = reference;
        fireBeforePropertyChange(P_Reference, old, newValue);
        this.reference = newValue;
        firePropertyChange(P_Reference, old, this.reference);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.amount = rs.getDouble(3);
        setPrimitiveNull(P_Amount, rs.wasNull());
        this.reference = rs.getString(4);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
