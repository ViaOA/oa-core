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
    lowerName = "denominationBundle",
    pluralName = "DenominationBundles",
    shortName = "dnb",
    displayName = "Denomination Bundle",
    displayProperty = "name",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "DenominationBundleCurrencyDenomination", fkey = true, columns = { @OAIndexColumn(name = "CurrencyDenominationId") })
    }
)
public class DenominationBundle extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(DenominationBundle.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Name = "name";
    public static final String P_Type = "type";
    public static final String P_TypeString = "typeString";
    public static final String P_TypeEnum = "typeEnum";
    public static final String P_TypeDisplay = "typeDisplay";
    public static final String P_PackSize = "packSize";
     
    public static final String P_CurrencyDenomination = "currencyDenomination";
    public static final String P_CurrencyDenominationId = "currencyDenominationId"; // fkey
    public static final String P_LedgerDenominationBundles = "ledgerDenominationBundles";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String name;
    protected volatile int type;

    public static enum Type {
        Unknown("Unknown"),
        Single("Single"),
        Roll("Roll"),
        Strap("Strap"),
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
    public static final int TYPE_Single = 1;
    public static final int TYPE_Roll = 2;
    public static final int TYPE_Strap = 3;
    public static final int TYPE_Other = 4;

    protected volatile int packSize;
     
    // Links to other objects.
    protected volatile transient CurrencyDenomination currencyDenomination;
     
    public DenominationBundle() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public DenominationBundle(int id) {
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

    @OAProperty(lowerName = "type", displayLength = 8, isNameValue = true)
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
    @OACalculatedProperty(enumPropertyName = P_Type, displayName = "Type", displayLength = 8, columnLength = 8, properties = {P_Type} )
    public String getTypeDisplay() {
        Type type = getTypeEnum();
        if (type == null) return null;
        return type.getDisplay();
    }

    @OAProperty(lowerName = "packSize", displayName = "Pack Size", displayLength = 6, uiColumnLength = 9)
    @OAColumn(name = "PackSize", sqlType = java.sql.Types.INTEGER)
    public int getPackSize() {
        return packSize;
    }
    public void setPackSize(int newValue) {
        int old = packSize;
        fireBeforePropertyChange(P_PackSize, old, newValue);
        this.packSize = newValue;
        firePropertyChange(P_PackSize, old, this.packSize);
    }

    @OAOne(
        displayName = "Currency Denomination", 
        reverseName = CurrencyDenomination.P_DenominationBundles, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_CurrencyDenominationId, toProperty = CurrencyDenomination.P_Id)}
    )
    public CurrencyDenomination getCurrencyDenomination() {
        if (currencyDenomination == null) {
            currencyDenomination = (CurrencyDenomination) getObject(P_CurrencyDenomination);
        }
        return currencyDenomination;
    }
    public void setCurrencyDenomination(CurrencyDenomination newValue) {
        CurrencyDenomination old = this.currencyDenomination;
        fireBeforePropertyChange(P_CurrencyDenomination, old, newValue);
        this.currencyDenomination = newValue;
        firePropertyChange(P_CurrencyDenomination, old, this.currencyDenomination);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "CurrencyDenominationId")
    public Integer getCurrencyDenominationId() {
        return (Integer) getFkeyProperty(P_CurrencyDenominationId);
    }
    public void setCurrencyDenominationId(Integer newValue) {
        this.currencyDenomination = null;
        setFkeyProperty(P_CurrencyDenominationId, newValue);
    }

    @OAMany(
        displayName = "Ledger Denomination Bundles", 
        toClass = LedgerDenominationBundle.class, 
        reverseName = LedgerDenominationBundle.P_DenominationBundle, 
        createMethod = false
    )
    private Hub<LedgerDenominationBundle> getLedgerDenominationBundles() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.name = rs.getString(3);
        this.type = rs.getInt(4);
        setPrimitiveNull(P_Type, rs.wasNull());
        this.packSize = rs.getInt(5);
        setPrimitiveNull(P_PackSize, rs.wasNull());
        int currencyDenominationFkey = rs.getInt(6);
        setFkeyProperty(P_CurrencyDenomination, rs.wasNull() ? null : currencyDenominationFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
