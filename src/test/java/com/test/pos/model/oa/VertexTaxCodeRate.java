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
import com.viaoa.datetime.OADate;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "vertexTaxCodeRate",
    pluralName = "VertexTaxCodeRates",
    shortName = "vtc",
    displayName = "Vertex Tax Code Rate",
    displayProperty = "taxPercent",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "VertexTaxCodeRateVertexTaxCode", fkey = true, columns = { @OAIndexColumn(name = "VertexTaxCodeId") })
    }
)
public class VertexTaxCodeRate extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(VertexTaxCodeRate.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_TaxPercent = "taxPercent";
    public static final String P_DecimalPlaces = "decimalPlaces";
    public static final String P_BeginDate = "beginDate";
    public static final String P_EndDate = "endDate";
    public static final String P_MinTaxable = "minTaxable";
    public static final String P_MaxTaxable = "maxTaxable";
    public static final String P_ThresholdAmount = "thresholdAmount";
     
    public static final String P_CalcVertexTaxCode = "calcVertexTaxCode";
    public static final String P_LineItemTaxes = "lineItemTaxes";
    public static final String P_VertexTaxCode = "vertexTaxCode";
    public static final String P_VertexTaxCodeId = "vertexTaxCodeId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile double taxPercent;
    protected volatile int decimalPlaces;
    protected volatile OADate beginDate;
    protected volatile OADate endDate;
    protected volatile double minTaxable;
    protected volatile double maxTaxable;
    protected volatile double thresholdAmount;
     
    // Links to other objects.
    protected volatile transient VertexTaxCode vertexTaxCode;
     
    public VertexTaxCodeRate() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public VertexTaxCodeRate(int id) {
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

    @OAProperty(lowerName = "decimalPlaces", displayName = "Decimal Places", displayLength = 6, uiColumnLength = 14)
    @OAColumn(name = "DecimalPlaces", sqlType = java.sql.Types.INTEGER)
    public int getDecimalPlaces() {
        return decimalPlaces;
    }
    public void setDecimalPlaces(int newValue) {
        int old = decimalPlaces;
        fireBeforePropertyChange(P_DecimalPlaces, old, newValue);
        this.decimalPlaces = newValue;
        firePropertyChange(P_DecimalPlaces, old, this.decimalPlaces);
    }

    @OAProperty(lowerName = "beginDate", displayName = "Begin Date", displayLength = 8, uiColumnLength = 10)
    @OAColumn(name = "BeginDate", sqlType = java.sql.Types.DATE)
    public OADate getBeginDate() {
        return beginDate;
    }
    public void setBeginDate(OADate newValue) {
        OADate old = beginDate;
        fireBeforePropertyChange(P_BeginDate, old, newValue);
        this.beginDate = newValue;
        firePropertyChange(P_BeginDate, old, this.beginDate);
    }

    @OAProperty(lowerName = "endDate", displayName = "End Date", displayLength = 8)
    @OAColumn(name = "EndDate", sqlType = java.sql.Types.DATE)
    public OADate getEndDate() {
        return endDate;
    }
    public void setEndDate(OADate newValue) {
        OADate old = endDate;
        fireBeforePropertyChange(P_EndDate, old, newValue);
        this.endDate = newValue;
        firePropertyChange(P_EndDate, old, this.endDate);
    }

    @OAProperty(lowerName = "minTaxable", displayName = "Min Taxable", decimalPlaces = 2, displayLength = 7, uiColumnLength = 11)
    @OAColumn(name = "MinTaxable", sqlType = java.sql.Types.DOUBLE)
    public double getMinTaxable() {
        return minTaxable;
    }
    public void setMinTaxable(double newValue) {
        double old = minTaxable;
        fireBeforePropertyChange(P_MinTaxable, old, newValue);
        this.minTaxable = newValue;
        firePropertyChange(P_MinTaxable, old, this.minTaxable);
    }

    @OAProperty(lowerName = "maxTaxable", displayName = "Max Taxable", decimalPlaces = 2, displayLength = 7, uiColumnLength = 11)
    @OAColumn(name = "MaxTaxable", sqlType = java.sql.Types.DOUBLE)
    public double getMaxTaxable() {
        return maxTaxable;
    }
    public void setMaxTaxable(double newValue) {
        double old = maxTaxable;
        fireBeforePropertyChange(P_MaxTaxable, old, newValue);
        this.maxTaxable = newValue;
        firePropertyChange(P_MaxTaxable, old, this.maxTaxable);
    }

    @OAProperty(lowerName = "thresholdAmount", displayName = "Threshold Amount", description = "this tax is only used if amount is greater than threshold amount", decimalPlaces = 2, displayLength = 7, uiColumnLength = 16)
    @OAColumn(name = "ThresholdAmount", sqlType = java.sql.Types.DOUBLE)
    /**
      this tax is only used if amount is greater than threshold amount
    */
    public double getThresholdAmount() {
        return thresholdAmount;
    }
    public void setThresholdAmount(double newValue) {
        double old = thresholdAmount;
        fireBeforePropertyChange(P_ThresholdAmount, old, newValue);
        this.thresholdAmount = newValue;
        firePropertyChange(P_ThresholdAmount, old, this.thresholdAmount);
    }

    @OAOne(
        displayName = "Vertex Tax Code", 
        isCalculated = true, 
        reverseName = VertexTaxCode.P_CurrentVertexTaxCodeRate, 
        allowCreateNew = false, 
        allowAddExisting = false
    )
    private VertexTaxCode getCalcVertexTaxCode() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAMany(
        displayName = "Line Item Taxes", 
        toClass = LineItemTax.class, 
        reverseName = LineItemTax.P_VertexTaxCodeRate, 
        createMethod = false
    )
    private Hub<LineItemTax> getLineItemTaxes() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAOne(
        displayName = "Vertex Tax Code", 
        reverseName = VertexTaxCode.P_VertexTaxCodeRates, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_VertexTaxCodeId, toProperty = VertexTaxCode.P_Id)}
    )
    public VertexTaxCode getVertexTaxCode() {
        if (vertexTaxCode == null) {
            vertexTaxCode = (VertexTaxCode) getObject(P_VertexTaxCode);
        }
        return vertexTaxCode;
    }
    public void setVertexTaxCode(VertexTaxCode newValue) {
        VertexTaxCode old = this.vertexTaxCode;
        fireBeforePropertyChange(P_VertexTaxCode, old, newValue);
        this.vertexTaxCode = newValue;
        firePropertyChange(P_VertexTaxCode, old, this.vertexTaxCode);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "VertexTaxCodeId")
    public Integer getVertexTaxCodeId() {
        return (Integer) getFkeyProperty(P_VertexTaxCodeId);
    }
    public void setVertexTaxCodeId(Integer newValue) {
        this.vertexTaxCode = null;
        setFkeyProperty(P_VertexTaxCodeId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.taxPercent = rs.getDouble(3);
        setPrimitiveNull(P_TaxPercent, rs.wasNull());
        this.decimalPlaces = rs.getInt(4);
        setPrimitiveNull(P_DecimalPlaces, rs.wasNull());
        java.sql.Date date;
        date = rs.getDate(5);
        if (date != null) this.beginDate = new OADate(date);
        date = rs.getDate(6);
        if (date != null) this.endDate = new OADate(date);
        this.minTaxable = rs.getDouble(7);
        setPrimitiveNull(P_MinTaxable, rs.wasNull());
        this.maxTaxable = rs.getDouble(8);
        setPrimitiveNull(P_MaxTaxable, rs.wasNull());
        this.thresholdAmount = rs.getDouble(9);
        setPrimitiveNull(P_ThresholdAmount, rs.wasNull());
        int vertexTaxCodeFkey = rs.getInt(10);
        setFkeyProperty(P_VertexTaxCode, rs.wasNull() ? null : vertexTaxCodeFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
