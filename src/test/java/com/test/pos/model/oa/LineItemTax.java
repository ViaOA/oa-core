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
    lowerName = "lineItemTax",
    pluralName = "LineItemTaxes",
    shortName = "lit",
    displayName = "Line Item Tax",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "LineItemTaxLineItem", fkey = true, columns = { @OAIndexColumn(name = "LineItemId") }), 
        @OAIndex(name = "LineItemTaxVertexTaxCodeRate", fkey = true, columns = { @OAIndexColumn(name = "VertexTaxCodeRateId") })
    }
)
public class LineItemTax extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(LineItemTax.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_TaxPercent = "taxPercent";
     
    public static final String P_LineItem = "lineItem";
    public static final String P_LineItemId = "lineItemId"; // fkey
    public static final String P_VertexTaxCodeRate = "vertexTaxCodeRate";
    public static final String P_VertexTaxCodeRateId = "vertexTaxCodeRateId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile double taxPercent;
     
    // Links to other objects.
    protected volatile transient LineItem lineItem;
    protected volatile transient VertexTaxCodeRate vertexTaxCodeRate;
     
    public LineItemTax() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public LineItemTax(int id) {
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
        displayName = "Line Item", 
        reverseName = LineItem.P_LineItemTaxes, 
        allowCreateNew = false, 
        allowAddExisting = false, 
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
        displayName = "Vertex Tax Code Rate", 
        reverseName = VertexTaxCodeRate.P_LineItemTaxes, 
        required = true, 
        allowCreateNew = false, 
        selectFromPropertyPath = P_LineItem + "." + LineItem.P_Product + "." + Product.P_Item + "." + Item.P_VertexTaxCodes + "." + VertexTaxCode.P_VertexTaxCodeRates, 
        fkeys = {@OAFkey(fromProperty = P_VertexTaxCodeRateId, toProperty = VertexTaxCodeRate.P_Id)}
    )
    public VertexTaxCodeRate getVertexTaxCodeRate() {
        if (vertexTaxCodeRate == null) {
            vertexTaxCodeRate = (VertexTaxCodeRate) getObject(P_VertexTaxCodeRate);
        }
        return vertexTaxCodeRate;
    }
    public void setVertexTaxCodeRate(VertexTaxCodeRate newValue) {
        VertexTaxCodeRate old = this.vertexTaxCodeRate;
        fireBeforePropertyChange(P_VertexTaxCodeRate, old, newValue);
        this.vertexTaxCodeRate = newValue;
        firePropertyChange(P_VertexTaxCodeRate, old, this.vertexTaxCodeRate);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "VertexTaxCodeRateId")
    public Integer getVertexTaxCodeRateId() {
        return (Integer) getFkeyProperty(P_VertexTaxCodeRateId);
    }
    public void setVertexTaxCodeRateId(Integer newValue) {
        this.vertexTaxCodeRate = null;
        setFkeyProperty(P_VertexTaxCodeRateId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.taxPercent = rs.getDouble(3);
        setPrimitiveNull(P_TaxPercent, rs.wasNull());
        int lineItemFkey = rs.getInt(4);
        setFkeyProperty(P_LineItem, rs.wasNull() ? null : lineItemFkey);
        int vertexTaxCodeRateFkey = rs.getInt(5);
        setFkeyProperty(P_VertexTaxCodeRate, rs.wasNull() ? null : vertexTaxCodeRateFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
