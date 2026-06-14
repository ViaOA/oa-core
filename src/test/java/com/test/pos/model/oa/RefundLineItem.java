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
    lowerName = "refundLineItem",
    pluralName = "RefundLineItems",
    shortName = "rli",
    displayName = "Refund Line Item",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "RefundLineItemLineItem", fkey = true, columns = { @OAIndexColumn(name = "LineItemId") }), 
        @OAIndex(name = "RefundLineItemRefundInvoice", fkey = true, columns = { @OAIndexColumn(name = "RefundInvoiceId") })
    }
)
public class RefundLineItem extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(RefundLineItem.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Quantity = "quantity";
    public static final String P_PriceEach = "priceEach";
     
    public static final String P_LineItem = "lineItem";
    public static final String P_LineItemId = "lineItemId"; // fkey
    public static final String P_RefundInvoice = "refundInvoice";
    public static final String P_RefundInvoiceId = "refundInvoiceId"; // fkey
    public static final String P_RefundLineItemTaxes = "refundLineItemTaxes";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int quantity;
    protected volatile double priceEach;
     
    // Links to other objects.
    protected volatile transient LineItem lineItem;
    protected volatile transient RefundInvoice refundInvoice;
    protected transient Hub<RefundLineItemTax> hubRefundLineItemTaxes;
     
    public RefundLineItem() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public RefundLineItem(int id) {
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

    @OAProperty(lowerName = "quantity", displayLength = 6, uiColumnLength = 8)
    @OAColumn(name = "Quantity", sqlType = java.sql.Types.INTEGER)
    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int newValue) {
        int old = quantity;
        fireBeforePropertyChange(P_Quantity, old, newValue);
        this.quantity = newValue;
        firePropertyChange(P_Quantity, old, this.quantity);
    }

    @OAProperty(lowerName = "priceEach", displayName = "Price Each", decimalPlaces = 2, isCurrency = true, displayLength = 9, uiColumnLength = 10)
    @OAColumn(name = "PriceEach", sqlType = java.sql.Types.NUMERIC)
    public double getPriceEach() {
        return priceEach;
    }
    public void setPriceEach(double newValue) {
        double old = priceEach;
        fireBeforePropertyChange(P_PriceEach, old, newValue);
        this.priceEach = newValue;
        firePropertyChange(P_PriceEach, old, this.priceEach);
    }

    @OAOne(
        displayName = "Line Item", 
        reverseName = LineItem.P_RefundLineItems, 
        allowCreateNew = false, 
        selectFromPropertyPath = P_RefundInvoice + "." + RefundInvoice.P_Invoice + "." + Invoice.P_InvoiceBaskets + "." + InvoiceBasket.P_LineItems, 
        fkeys = {@OAFkey(fromProperty = P_LineItemId, toProperty = LineItem.P_Id)}
    )
    public LineItem getLineItem() {
        if (lineItem == null) {
            lineItem = (LineItem) getObject(P_LineItem);
        }
        return lineItem;
    }
    public void setLineItem(LineItem newValue) {
        LineItem old = this.lineItem;
        fireBeforePropertyChange(P_LineItem, old, newValue);
        this.lineItem = newValue;
        firePropertyChange(P_LineItem, old, this.lineItem);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "LineItemId")
    public Integer getLineItemId() {
        return (Integer) getFkeyProperty(P_LineItemId);
    }
    public void setLineItemId(Integer newValue) {
        this.lineItem = null;
        setFkeyProperty(P_LineItemId, newValue);
    }

    @OAOne(
        displayName = "Refund Invoice", 
        reverseName = RefundInvoice.P_RefundLineItems, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_RefundInvoiceId, toProperty = RefundInvoice.P_Id)}
    )
    public RefundInvoice getRefundInvoice() {
        if (refundInvoice == null) {
            refundInvoice = (RefundInvoice) getObject(P_RefundInvoice);
        }
        return refundInvoice;
    }
    public void setRefundInvoice(RefundInvoice newValue) {
        RefundInvoice old = this.refundInvoice;
        fireBeforePropertyChange(P_RefundInvoice, old, newValue);
        this.refundInvoice = newValue;
        firePropertyChange(P_RefundInvoice, old, this.refundInvoice);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "RefundInvoiceId")
    public Integer getRefundInvoiceId() {
        return (Integer) getFkeyProperty(P_RefundInvoiceId);
    }
    public void setRefundInvoiceId(Integer newValue) {
        this.refundInvoice = null;
        setFkeyProperty(P_RefundInvoiceId, newValue);
    }

    @OAMany(
        displayName = "Refund Line Item Taxes", 
        toClass = RefundLineItemTax.class, 
        owner = true, 
        reverseName = RefundLineItemTax.P_RefundLineItem, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<RefundLineItemTax> getRefundLineItemTaxes() {
        if (hubRefundLineItemTaxes == null) {
            hubRefundLineItemTaxes = (Hub<RefundLineItemTax>) getHub(P_RefundLineItemTaxes);
        }
        return hubRefundLineItemTaxes;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.quantity = rs.getInt(3);
        setPrimitiveNull(P_Quantity, rs.wasNull());
        this.priceEach = rs.getDouble(4);
        setPrimitiveNull(P_PriceEach, rs.wasNull());
        int lineItemFkey = rs.getInt(5);
        setFkeyProperty(P_LineItem, rs.wasNull() ? null : lineItemFkey);
        int refundInvoiceFkey = rs.getInt(6);
        setFkeyProperty(P_RefundInvoice, rs.wasNull() ? null : refundInvoiceFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
