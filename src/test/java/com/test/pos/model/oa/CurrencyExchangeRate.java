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
    lowerName = "currencyExchangeRate",
    pluralName = "CurrencyExchangeRates",
    shortName = "cer",
    displayName = "Currency Exchange Rate",
    displayProperty = "rate",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "CurrencyExchangeRateCurrencyType", fkey = true, columns = { @OAIndexColumn(name = "CurrencyTypeId") }), 
        @OAIndex(name = "CurrencyExchangeRateToCurrencyType", fkey = true, columns = { @OAIndexColumn(name = "ToCurrencyTypeId") })
    }
)
public class CurrencyExchangeRate extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(CurrencyExchangeRate.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Rate = "rate";
    public static final String P_BeginDate = "beginDate";
    public static final String P_EndDate = "endDate";
     
    public static final String P_CurrencyType = "currencyType";
    public static final String P_CurrencyTypeId = "currencyTypeId"; // fkey
    public static final String P_ToCurrencyType = "toCurrencyType";
    public static final String P_ToCurrencyTypeId = "toCurrencyTypeId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile double rate;
    protected volatile OADate beginDate;
    protected volatile OADate endDate;
     
    // Links to other objects.
    protected volatile transient CurrencyType currencyType;
    protected volatile transient CurrencyType toCurrencyType;
     
    public CurrencyExchangeRate() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public CurrencyExchangeRate(int id) {
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

    @OAProperty(lowerName = "rate", decimalPlaces = 4, displayLength = 7)
    @OAColumn(name = "Rate", sqlType = java.sql.Types.DOUBLE)
    public double getRate() {
        return rate;
    }
    public void setRate(double newValue) {
        double old = rate;
        fireBeforePropertyChange(P_Rate, old, newValue);
        this.rate = newValue;
        firePropertyChange(P_Rate, old, this.rate);
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

    @OAOne(
        displayName = "Currency Type", 
        reverseName = CurrencyType.P_CurrencyExchangeRates, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_CurrencyTypeId, toProperty = CurrencyType.P_Id)}
    )
    public CurrencyType getCurrencyType() {
        if (currencyType == null) {
            currencyType = (CurrencyType) getObject(P_CurrencyType);
        }
        return currencyType;
    }
    public void setCurrencyType(CurrencyType newValue) {
        CurrencyType old = this.currencyType;
        fireBeforePropertyChange(P_CurrencyType, old, newValue);
        this.currencyType = newValue;
        firePropertyChange(P_CurrencyType, old, this.currencyType);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "CurrencyTypeId")
    public Integer getCurrencyTypeId() {
        return (Integer) getFkeyProperty(P_CurrencyTypeId);
    }
    public void setCurrencyTypeId(Integer newValue) {
        this.currencyType = null;
        setFkeyProperty(P_CurrencyTypeId, newValue);
    }

    @OAOne(
        displayName = "Currency Type", 
        reverseName = CurrencyType.P_ToCurrencyExchangeRates, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_ToCurrencyTypeId, toProperty = CurrencyType.P_Id)}
    )
    public CurrencyType getToCurrencyType() {
        if (toCurrencyType == null) {
            toCurrencyType = (CurrencyType) getObject(P_ToCurrencyType);
        }
        return toCurrencyType;
    }
    public void setToCurrencyType(CurrencyType newValue) {
        CurrencyType old = this.toCurrencyType;
        fireBeforePropertyChange(P_ToCurrencyType, old, newValue);
        this.toCurrencyType = newValue;
        firePropertyChange(P_ToCurrencyType, old, this.toCurrencyType);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "ToCurrencyTypeId")
    public Integer getToCurrencyTypeId() {
        return (Integer) getFkeyProperty(P_ToCurrencyTypeId);
    }
    public void setToCurrencyTypeId(Integer newValue) {
        this.toCurrencyType = null;
        setFkeyProperty(P_ToCurrencyTypeId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.rate = rs.getDouble(3);
        setPrimitiveNull(P_Rate, rs.wasNull());
        java.sql.Date date;
        date = rs.getDate(4);
        if (date != null) this.beginDate = new OADate(date);
        date = rs.getDate(5);
        if (date != null) this.endDate = new OADate(date);
        int currencyTypeFkey = rs.getInt(6);
        setFkeyProperty(P_CurrencyType, rs.wasNull() ? null : currencyTypeFkey);
        int toCurrencyTypeFkey = rs.getInt(7);
        setFkeyProperty(P_ToCurrencyType, rs.wasNull() ? null : toCurrencyTypeFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
