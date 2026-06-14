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
    lowerName = "currencyDenomination",
    pluralName = "CurrencyDenominations",
    shortName = "crd",
    displayName = "Currency Denomination",
    displayProperty = "name",
    sortProperty = "unitValue",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "CurrencyDenominationCurrencyType", fkey = true, columns = { @OAIndexColumn(name = "CurrencyTypeId") })
    }
)
public class CurrencyDenomination extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(CurrencyDenomination.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Type = "type";
    public static final String P_TypeString = "typeString";
    public static final String P_TypeEnum = "typeEnum";
    public static final String P_TypeDisplay = "typeDisplay";
    public static final String P_Name = "name";
    public static final String P_UnitValue = "unitValue";
     
    public static final String P_CurrencyType = "currencyType";
    public static final String P_CurrencyTypeId = "currencyTypeId"; // fkey
    public static final String P_DenominationBundles = "denominationBundles";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int type;

    public static enum Type {
        Unknown("Unknown"),
        Coin("Coin"),
        Bill("Bill"),
        Token("Token"),
        Other("Other");

        private String display;
        Type(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int TYPE_Unknown = 0;
    public static final int TYPE_Coin = 1;
    public static final int TYPE_Bill = 2;
    public static final int TYPE_Token = 3;
    public static final int TYPE_Other = 4;

    protected volatile String name;
    protected volatile double unitValue;
     
    // Links to other objects.
    protected volatile transient CurrencyType currencyType;
    protected transient Hub<DenominationBundle> hubDenominationBundles;
     
    public CurrencyDenomination() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public CurrencyDenomination(int id) {
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

    @OAProperty(lowerName = "type", displayLength = 6, isNameValue = true)
    @OAColumn(name = "Type", sqlType = java.sql.Types.INTEGER)
    public int getType() {
        return type;
    }
    public void setType(int newValue) {
        int old = type;
        fireBeforePropertyChange(P_Type, old, newValue);
        this.type = newValue;
        firePropertyChange(P_Type, old, this.type);
    }

    @OAProperty(enumPropertyName = P_Type)
    public String getTypeString() {
        Type type = getTypeEnum();
        if (type == null) return null;
        return type.name();
    }
    public void setTypeString(String val) {
        int x = -1;
        if (OAString.isNotEmpty(val)) {
            Type type = Type.valueOf(val);
            if (type != null) x = type.ordinal();
        }
        if (x < 0) setNull(P_Type);
        else setType(x);
    }
    @OAProperty(enumPropertyName = P_Type)
    public Type getTypeEnum() {
        if (isNull(P_Type)) return null;
        final int val = getType();
        if (val < 0 || val >= Type.values().length) return null;
        return Type.values()[val];
    }
    public void setTypeEnum(Type val) {
        if (val == null) {
            setNull(P_Type);
        }
        else {
            setType(val.ordinal());
        }
    }
    @OACalculatedProperty(enumPropertyName = P_Type, displayName = "Type", displayLength = 6, columnLength = 6, properties = {P_Type} )
    public String getTypeDisplay() {
        Type type = getTypeEnum();
        if (type == null) return null;
        return type.getDisplay();
    }

    @OAProperty(lowerName = "name", maxLength = 30, displayLength = 18)
    @OAColumn(name = "Name", maxLength = 30)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }

    @OAProperty(lowerName = "unitValue", displayName = "Unit Value", description = "examples:  .01, .05, .10, .25, 1, 2, 5, 20, 50, 100", decimalPlaces = 2, displayLength = 4, uiColumnLength = 8)
    @OAColumn(name = "UnitValue", sqlType = java.sql.Types.DOUBLE)
    /**
      examples:  .01, .05, .10, .25, 1, 2, 5, 20, 50, 100
    */
    public double getUnitValue() {
        return unitValue;
    }
    public void setUnitValue(double newValue) {
        double old = unitValue;
        fireBeforePropertyChange(P_UnitValue, old, newValue);
        this.unitValue = newValue;
        firePropertyChange(P_UnitValue, old, this.unitValue);
    }

    @OAOne(
        displayName = "Currency Type", 
        reverseName = CurrencyType.P_CurrencyDenominations, 
        required = true, 
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

    @OAMany(
        displayName = "Denomination Bundles", 
        toClass = DenominationBundle.class, 
        owner = true, 
        reverseName = DenominationBundle.P_CurrencyDenomination, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<DenominationBundle> getDenominationBundles() {
        if (hubDenominationBundles == null) {
            hubDenominationBundles = (Hub<DenominationBundle>) getHub(P_DenominationBundles);
        }
        return hubDenominationBundles;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.type = rs.getInt(3);
        setPrimitiveNull(P_Type, rs.wasNull());
        this.name = rs.getString(4);
        this.unitValue = rs.getDouble(5);
        setPrimitiveNull(P_UnitValue, rs.wasNull());
        int currencyTypeFkey = rs.getInt(6);
        setFkeyProperty(P_CurrencyType, rs.wasNull() ? null : currencyTypeFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
