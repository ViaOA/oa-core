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
    lowerName = "discountType",
    pluralName = "DiscountTypes",
    shortName = "dst",
    displayName = "Discount Type",
    displayProperty = "id",
    noPojo = true
)
@OATable(
)
public class DiscountType extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(DiscountType.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Type = "type";
    public static final String P_TypeString = "typeString";
    public static final String P_TypeEnum = "typeEnum";
    public static final String P_TypeDisplay = "typeDisplay";
    public static final String P_Type2 = "type2";
    public static final String P_Type2String = "type2String";
    public static final String P_Type2Enum = "type2Enum";
    public static final String P_Type2Display = "type2Display";
    public static final String P_Name = "name";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int type;

    public static enum Type {
        unknown("Unknown"),
        percentage("Percentage"),
        fixedAmount("Fixed Amount"),
        bogo("BOGO");

        private String display;
        Type(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int TYPE_unknown = 0;
    public static final int TYPE_percentage = 1;
    public static final int TYPE_fixedAmount = 2;
    public static final int TYPE_bogo = 3;

    protected volatile int type2;

    public static enum Type2 {
        unknown("Unknown"),
        loyalty("Loyalty"),
        promoCode("Promo Code"),
        other("Other");

        private String display;
        Type2(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int TYPE2_unknown = 0;
    public static final int TYPE2_loyalty = 1;
    public static final int TYPE2_promoCode = 2;
    public static final int TYPE2_other = 3;

    protected volatile String name;
     
    public DiscountType() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public DiscountType(int id) {
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

    @OAProperty(lowerName = "type2", displayLength = 6, isNameValue = true)
    @OAColumn(name = "Type2", sqlType = java.sql.Types.INTEGER)
    public int getType2() {
        return type2;
    }
    public void setType2(int newValue) {
        int old = type2;
        fireBeforePropertyChange(P_Type2, old, newValue);
        this.type2 = newValue;
        firePropertyChange(P_Type2, old, this.type2);
    }

    @OAProperty(enumPropertyName = P_Type2)
    public String getType2String() {
        Type2 type2 = getType2Enum();
        if (type2 == null) return null;
        return type2.name();
    }
    public void setType2String(String val) {
        int x = -1;
        if (OAString.isNotEmpty(val)) {
            Type2 type2 = Type2.valueOf(val);
            if (type2 != null) x = type2.ordinal();
        }
        if (x < 0) setNull(P_Type2);
        else setType2(x);
    }
    @OAProperty(enumPropertyName = P_Type2)
    public Type2 getType2Enum() {
        if (isNull(P_Type2)) return null;
        final int val = getType2();
        if (val < 0 || val >= Type2.values().length) return null;
        return Type2.values()[val];
    }
    public void setType2Enum(Type2 val) {
        if (val == null) {
            setNull(P_Type2);
        }
        else {
            setType2(val.ordinal());
        }
    }
    @OACalculatedProperty(enumPropertyName = P_Type2, displayName = "Type2", displayLength = 6, columnLength = 6, properties = {P_Type2} )
    public String getType2Display() {
        Type2 type2 = getType2Enum();
        if (type2 == null) return null;
        return type2.getDisplay();
    }

    @OAProperty(lowerName = "name", maxLength = 50, displayLength = 18)
    @OAColumn(name = "Name", maxLength = 50)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.type = rs.getInt(3);
        setPrimitiveNull(P_Type, rs.wasNull());
        this.type2 = rs.getInt(4);
        setPrimitiveNull(P_Type2, rs.wasNull());
        this.name = rs.getString(5);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
