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
    lowerName = "refundLineItemTax",
    pluralName = "RefundLineItemTaxes",
    shortName = "rli",
    displayName = "Refund Line Item Tax",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "RefundLineItemTaxRefundLineItem", fkey = true, columns = { @OAIndexColumn(name = "RefundLineItemId") })
    }
)
public class RefundLineItemTax extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(RefundLineItemTax.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_TaxPercent = "taxPercent";
     
    public static final String P_RefundLineItem = "refundLineItem";
    public static final String P_RefundLineItemId = "refundLineItemId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile double taxPercent;
     
    // Links to other objects.
    protected volatile transient RefundLineItem refundLineItem;
     
    public RefundLineItemTax() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public RefundLineItemTax(int id) {
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

    @OAProperty(lowerName = "taxPercent", displayName = "Tax Percent", decimalPlaces = 4, displayLength = 7, uiColumnLength = 11)
    @OAColumn(name = "TaxPercent", sqlType = java.sql.Types.DOUBLE)
    public double getTaxPercent() {
        return taxPercent;
    }
    public void setTaxPercent(double newValue) {
        double old = taxPercent;
        fireBeforePropertyChange(P_TaxPercent, old, newValue);
        this.taxPercent = newValue;
        firePropertyChange(P_TaxPercent, old, this.taxPercent);
    }

    @OAOne(
        displayName = "Refund Line Item", 
        reverseName = RefundLineItem.P_RefundLineItemTaxes, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_RefundLineItemId, toProperty = RefundLineItem.P_Id)}
    )
    public RefundLineItem getRefundLineItem() {
        if (refundLineItem == null) {
            refundLineItem = (RefundLineItem) getObject(P_RefundLineItem);
        }
        return refundLineItem;
    }
    public void setRefundLineItem(RefundLineItem newValue) {
        RefundLineItem old = this.refundLineItem;
        fireBeforePropertyChange(P_RefundLineItem, old, newValue);
        this.refundLineItem = newValue;
        firePropertyChange(P_RefundLineItem, old, this.refundLineItem);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "RefundLineItemId")
    public Integer getRefundLineItemId() {
        return (Integer) getFkeyProperty(P_RefundLineItemId);
    }
    public void setRefundLineItemId(Integer newValue) {
        this.refundLineItem = null;
        setFkeyProperty(P_RefundLineItemId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.taxPercent = rs.getDouble(3);
        setPrimitiveNull(P_TaxPercent, rs.wasNull());
        int refundLineItemFkey = rs.getInt(4);
        setFkeyProperty(P_RefundLineItem, rs.wasNull() ? null : refundLineItemFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
