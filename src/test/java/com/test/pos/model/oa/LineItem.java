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
    lowerName = "lineItem",
    pluralName = "LineItems",
    shortName = "lni",
    displayName = "Line Item",
    displayProperty = "product",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "LineItemInvoiceBasket", fkey = true, columns = { @OAIndexColumn(name = "InvoiceBasketId") }), 
        @OAIndex(name = "LineItemProduct", fkey = true, columns = { @OAIndexColumn(name = "ProductId") })
    }
)
public class LineItem extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(LineItem.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Quantity = "quantity";
    public static final String P_SerialCode = "serialCode";
    public static final String P_PriceEach = "priceEach";
     
    public static final String P_TotalItemAmount = "totalItemAmount";
    public static final String P_TotalTaxAmount = "totalTaxAmount";
     
    public static final String P_InvoiceBasket = "invoiceBasket";
    public static final String P_InvoiceBasketId = "invoiceBasketId"; // fkey
    public static final String P_LineItemTaxes = "lineItemTaxes";
    public static final String P_Product = "product";
    public static final String P_ProductId = "productId"; // fkey
    public static final String P_RefundLineItems = "refundLineItems";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int quantity;
    protected volatile String serialCode;
    protected volatile double priceEach;
     
    // Links to other objects.
    protected volatile transient InvoiceBasket invoiceBasket;
    protected transient Hub<LineItemTax> hubLineItemTaxes;
    protected volatile transient Product product;
    protected transient Hub<RefundLineItem> hubRefundLineItems;
     
    public LineItem() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public LineItem(int id) {
        this();
        setId(id);
    }
    @OAObjCallback(enabledProperty = LineItem.P_InvoiceBasket+"."+InvoiceBasket.P_Invoice+"."+Invoice.P_Completed, 
        enabledValue = false
    )
    public void callback(final OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
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

    @OAProperty(lowerName = "serialCode", displayName = "Serial Code", maxLength = 75, displayLength = 15)
    @OAColumn(name = "SerialCode", maxLength = 75)
    public String getSerialCode() {
        return serialCode;
    }
    public void setSerialCode(String newValue) {
        String old = serialCode;
        fireBeforePropertyChange(P_SerialCode, old, newValue);
        this.serialCode = newValue;
        firePropertyChange(P_SerialCode, old, this.serialCode);
    }
     
    @OAObjCallback(enabledProperty = LineItem.P_Product+"."+Product.P_Item+"."+Item.P_UseSerialCode)
    public void serialCodeCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
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
    @OACalculatedProperty(displayName = "Total Item Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, columnLength = 17, properties = {P_PriceEach, P_Quantity})
    public double getTotalItemAmount() {
        return LineItemDelegate.getTotalItemAmount(this);
    }
    @OACalculatedProperty(displayName = "Total Tax Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, columnLength = 16, properties = {P_TotalItemAmount, P_LineItemTaxes+"."+LineItemTax.P_TaxPercent})
    public double getTotalTaxAmount() {
        return LineItemDelegate.getTotalTaxAmount(this)
    ;}

    @OAOne(
        displayName = "Invoice Basket", 
        reverseName = InvoiceBasket.P_LineItems, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_InvoiceBasketId, toProperty = InvoiceBasket.P_Id)}
    )
    public InvoiceBasket getInvoiceBasket() {
        if (invoiceBasket == null) {
            invoiceBasket = (InvoiceBasket) getObject(P_InvoiceBasket);
        }
        return invoiceBasket;
    }
    public void setInvoiceBasket(InvoiceBasket newValue) {
        InvoiceBasket old = this.invoiceBasket;
        fireBeforePropertyChange(P_InvoiceBasket, old, newValue);
        this.invoiceBasket = newValue;
        firePropertyChange(P_InvoiceBasket, old, this.invoiceBasket);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "InvoiceBasketId")
    public Integer getInvoiceBasketId() {
        return (Integer) getFkeyProperty(P_InvoiceBasketId);
    }
    public void setInvoiceBasketId(Integer newValue) {
        this.invoiceBasket = null;
        setFkeyProperty(P_InvoiceBasketId, newValue);
    }

    @OAMany(
        displayName = "Line Item Taxes", 
        toClass = LineItemTax.class, 
        reverseName = LineItemTax.P_LineItem, 
        matchHub = (LineItem.P_Product+"."+Product.P_Item+"."+Item.P_VertexTaxCodes+"."+VertexTaxCode.P_VertexTaxCodeRates), 
        matchProperty = LineItemTax.P_VertexTaxCodeRate
    )
    public Hub<LineItemTax> getLineItemTaxes() {
        if (hubLineItemTaxes == null) {
            hubLineItemTaxes = (Hub<LineItemTax>) getHub(P_LineItemTaxes);
        }
        return hubLineItemTaxes;
    }

    @OAOne(
        reverseName = Product.P_LineItems, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_ProductId, toProperty = Product.P_Id)}
    )
    public Product getProduct() {
        if (product == null) {
            product = (Product) getObject(P_Product);
        }
        return product;
    }
    public void setProduct(Product newValue) {
        Product old = this.product;
        fireBeforePropertyChange(P_Product, old, newValue);
        this.product = newValue;
        firePropertyChange(P_Product, old, this.product);
        // custom
        LineItemDelegate.afterSetProduct(this);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "ProductId")
    public Integer getProductId() {
        return (Integer) getFkeyProperty(P_ProductId);
    }
    public void setProductId(Integer newValue) {
        this.product = null;
        setFkeyProperty(P_ProductId, newValue);
    }

    @OAMany(
        displayName = "Refund Line Items", 
        toClass = RefundLineItem.class, 
        reverseName = RefundLineItem.P_LineItem
    )
    public Hub<RefundLineItem> getRefundLineItems() {
        if (hubRefundLineItems == null) {
            hubRefundLineItems = (Hub<RefundLineItem>) getHub(P_RefundLineItems);
        }
        return hubRefundLineItems;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.quantity = rs.getInt(3);
        setPrimitiveNull(P_Quantity, rs.wasNull());
        this.serialCode = rs.getString(4);
        this.priceEach = rs.getDouble(5);
        setPrimitiveNull(P_PriceEach, rs.wasNull());
        int invoiceBasketFkey = rs.getInt(6);
        setFkeyProperty(P_InvoiceBasket, rs.wasNull() ? null : invoiceBasketFkey);
        int productFkey = rs.getInt(7);
        setFkeyProperty(P_Product, rs.wasNull() ? null : productFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
