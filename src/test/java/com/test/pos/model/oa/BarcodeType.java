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
import com.test.pos.model.oa.method.*;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "barcodeType",
    pluralName = "BarcodeTypes",
    shortName = "brt",
    displayName = "Barcode Type",
    isLookup = true,
    isPreSelect = true,
    displayProperty = "name",
    noPojo = true
)
@OATable(
)
public class BarcodeType extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(BarcodeType.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Name = "name";
    public static final String P_Type = "type";
    public static final String P_TypeString = "typeString";
    public static final String P_TypeEnum = "typeEnum";
    public static final String P_TypeDisplay = "typeDisplay";
    public static final String P_Rule = "rule";
    public static final String P_RuleString = "ruleString";
    public static final String P_RuleEnum = "ruleEnum";
    public static final String P_RuleDisplay = "ruleDisplay";
     
    public static final String P_ProductUpcs = "productUpcs";
     
    public static final String M_ConvertUpc = "convertUpc";
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String name;
    protected volatile int type;

    public static enum Type {
        Unknown("Unknown"),
        GTIN("GTIN"),
        UPC("UPC"),
        EAN("EAN"),
        PLU("PLU"),
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
    public static final int TYPE_GTIN = 1;
    public static final int TYPE_UPC = 2;
    public static final int TYPE_EAN = 3;
    public static final int TYPE_PLU = 4;
    public static final int TYPE_Other = 5;

    protected volatile int rule;

    public static enum Rule {
        unknown("Unknown");

        private String display;
        Rule(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int RULE_unknown = 0;

     
     
    public BarcodeType() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public BarcodeType(int id) {
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

    @OAProperty(lowerName = "rule", displayLength = 6, isNameValue = true)
    @OAColumn(name = "Rule", sqlType = java.sql.Types.INTEGER)
    public int getRule() {
        return rule;
    }
    public void setRule(int newValue) {
        int old = rule;
        fireBeforePropertyChange(P_Rule, old, newValue);
        this.rule = newValue;
        firePropertyChange(P_Rule, old, this.rule);
    }

    @OAProperty(enumPropertyName = P_Rule)
    public String getRuleString() {
        Rule rule = getRuleEnum();
        if (rule == null) return null;
        return rule.name();
    }
    public void setRuleString(String val) {
        int x = -1;
        if (OAString.isNotEmpty(val)) {
            Rule rule = Rule.valueOf(val);
            if (rule != null) x = rule.ordinal();
        }
        if (x < 0) setNull(P_Rule);
        else setRule(x);
    }
    @OAProperty(enumPropertyName = P_Rule)
    public Rule getRuleEnum() {
        if (isNull(P_Rule)) return null;
        final int val = getRule();
        if (val < 0 || val >= Rule.values().length) return null;
        return Rule.values()[val];
    }
    public void setRuleEnum(Rule val) {
        if (val == null) {
            setNull(P_Rule);
        }
        else {
            setRule(val.ordinal());
        }
    }
    @OACalculatedProperty(enumPropertyName = P_Rule, displayName = "Rule", displayLength = 6, columnLength = 6, properties = {P_Rule} )
    public String getRuleDisplay() {
        Rule rule = getRuleEnum();
        if (rule == null) return null;
        return rule.getDisplay();
    }

    @OAMany(
        displayName = "Product Upcs", 
        toClass = ProductUpc.class, 
        reverseName = ProductUpc.P_BarcodeType, 
        createMethod = false
    )
    private Hub<ProductUpc> getProductUpcs() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }
    @OAMethod(displayName = "Convert Upc")
    public void convertUpc(final BarcodeTypeConvertUpcMethod data) throws Exception {
        if (data == null) return;
        // custom code
        BarcodeTypeDelegate.convertUpc(this, data);
    }

    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.name = rs.getString(3);
        this.type = rs.getInt(4);
        setPrimitiveNull(P_Type, rs.wasNull());
        this.rule = rs.getInt(5);
        setPrimitiveNull(P_Rule, rs.wasNull());

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
