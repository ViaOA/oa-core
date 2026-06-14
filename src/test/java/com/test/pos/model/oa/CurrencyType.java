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
    lowerName = "currencyType",
    pluralName = "CurrencyTypes",
    shortName = "crt",
    displayName = "Currency Type",
    isLookup = true,
    isPreSelect = true,
    displayProperty = "code",
    noPojo = true
)
@OATable(
)
public class CurrencyType extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(CurrencyType.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Code = "code";
    public static final String P_Name = "name";
    public static final String P_Description = "description";
    public static final String P_Symbol = "symbol";
    public static final String P_JavaFormatCode = "javaFormatCode";
    public static final String P_MinorUnit = "minorUnit";
    public static final String P_RoundingRule = "roundingRule";
    public static final String P_RoundingRuleString = "roundingRuleString";
    public static final String P_RoundingRuleEnum = "roundingRuleEnum";
    public static final String P_RoundingRuleDisplay = "roundingRuleDisplay";
     
    public static final String P_CurrencyDenominations = "currencyDenominations";
    public static final String P_CurrencyExchangeRates = "currencyExchangeRates";
    public static final String P_Stores = "stores";
    public static final String P_ToCurrencyExchangeRates = "toCurrencyExchangeRates";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String code;
    protected volatile String name;
    protected volatile String description;
    protected volatile String symbol;
    protected volatile String javaFormatCode;
    protected volatile int minorUnit;
    protected volatile int roundingRule;

    public static enum RoundingRule {
        Unknown("Unknown"),
        HalfUp("Half Up"),
        HalfDown("Half Down"),
        HalfEven("Half Even");

        private String display;
        RoundingRule(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int ROUNDINGRULE_Unknown = 0;
    public static final int ROUNDINGRULE_HalfUp = 1;
    public static final int ROUNDINGRULE_HalfDown = 2;
    public static final int ROUNDINGRULE_HalfEven = 3;

     
    // Links to other objects.
    protected transient Hub<CurrencyDenomination> hubCurrencyDenominations;
    protected transient Hub<CurrencyExchangeRate> hubCurrencyExchangeRates;
     
    public CurrencyType() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public CurrencyType(int id) {
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

    @OAProperty(lowerName = "code", maxLength = 12, displayLength = 5)
    @OAColumn(name = "Code", maxLength = 12)
    public String getCode() {
        return code;
    }
    public void setCode(String newValue) {
        String old = code;
        fireBeforePropertyChange(P_Code, old, newValue);
        this.code = newValue;
        firePropertyChange(P_Code, old, this.code);
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

    @OAProperty(lowerName = "description", maxLength = 120, displayLength = 22, uiColumnLength = 20)
    @OAColumn(name = "Description", maxLength = 120)
    public String getDescription() {
        return description;
    }
    public void setDescription(String newValue) {
        String old = description;
        fireBeforePropertyChange(P_Description, old, newValue);
        this.description = newValue;
        firePropertyChange(P_Description, old, this.description);
    }

    @OAProperty(lowerName = "symbol", maxLength = 10, displayLength = 10)
    @OAColumn(name = "Symbol", maxLength = 10)
    public String getSymbol() {
        return symbol;
    }
    public void setSymbol(String newValue) {
        String old = symbol;
        fireBeforePropertyChange(P_Symbol, old, newValue);
        this.symbol = newValue;
        firePropertyChange(P_Symbol, old, this.symbol);
    }

    @OAProperty(lowerName = "javaFormatCode", displayName = "Java Format Code", maxLength = 10, displayLength = 10, uiColumnLength = 16)
    @OAColumn(name = "JavaFormatCode", maxLength = 10)
    public String getJavaFormatCode() {
        return javaFormatCode;
    }
    public void setJavaFormatCode(String newValue) {
        String old = javaFormatCode;
        fireBeforePropertyChange(P_JavaFormatCode, old, newValue);
        this.javaFormatCode = newValue;
        firePropertyChange(P_JavaFormatCode, old, this.javaFormatCode);
    }

    @OAProperty(lowerName = "minorUnit", displayName = "Minor Unit", description = "number of decimal places", displayLength = 6, uiColumnLength = 10)
    @OAColumn(name = "MinorUnit", sqlType = java.sql.Types.INTEGER)
    /**
      number of decimal places
    */
    public int getMinorUnit() {
        return minorUnit;
    }
    public void setMinorUnit(int newValue) {
        int old = minorUnit;
        fireBeforePropertyChange(P_MinorUnit, old, newValue);
        this.minorUnit = newValue;
        firePropertyChange(P_MinorUnit, old, this.minorUnit);
    }

    @OAProperty(lowerName = "roundingRule", displayName = "Rounding Rule", displayLength = 6, uiColumnLength = 13, isNameValue = true)
    @OAColumn(name = "RoundingRule", sqlType = java.sql.Types.INTEGER)
    public int getRoundingRule() {
        return roundingRule;
    }
    public void setRoundingRule(int newValue) {
        int old = roundingRule;
        fireBeforePropertyChange(P_RoundingRule, old, newValue);
        this.roundingRule = newValue;
        firePropertyChange(P_RoundingRule, old, this.roundingRule);
    }

    @OAProperty(enumPropertyName = P_RoundingRule)
    public String getRoundingRuleString() {
        RoundingRule roundingRule = getRoundingRuleEnum();
        if (roundingRule == null) return null;
        return roundingRule.name();
    }
    public void setRoundingRuleString(String val) {
        int x = -1;
        if (OAString.isNotEmpty(val)) {
            RoundingRule roundingRule = RoundingRule.valueOf(val);
            if (roundingRule != null) x = roundingRule.ordinal();
        }
        if (x < 0) setNull(P_RoundingRule);
        else setRoundingRule(x);
    }
    @OAProperty(enumPropertyName = P_RoundingRule)
    public RoundingRule getRoundingRuleEnum() {
        if (isNull(P_RoundingRule)) return null;
        final int val = getRoundingRule();
        if (val < 0 || val >= RoundingRule.values().length) return null;
        return RoundingRule.values()[val];
    }
    public void setRoundingRuleEnum(RoundingRule val) {
        if (val == null) {
            setNull(P_RoundingRule);
        }
        else {
            setRoundingRule(val.ordinal());
        }
    }
    @OACalculatedProperty(enumPropertyName = P_RoundingRule, displayName = "Rounding Rule", displayLength = 6, columnLength = 13, properties = {P_RoundingRule} )
    public String getRoundingRuleDisplay() {
        RoundingRule roundingRule = getRoundingRuleEnum();
        if (roundingRule == null) return null;
        return roundingRule.getDisplay();
    }

    @OAMany(
        displayName = "Currency Denominations", 
        toClass = CurrencyDenomination.class, 
        owner = true, 
        reverseName = CurrencyDenomination.P_CurrencyType, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<CurrencyDenomination> getCurrencyDenominations() {
        if (hubCurrencyDenominations == null) {
            hubCurrencyDenominations = (Hub<CurrencyDenomination>) getHub(P_CurrencyDenominations);
        }
        return hubCurrencyDenominations;
    }

    @OAMany(
        displayName = "Currency Exchange Rates", 
        toClass = CurrencyExchangeRate.class, 
        reverseName = CurrencyExchangeRate.P_CurrencyType
    )
    public Hub<CurrencyExchangeRate> getCurrencyExchangeRates() {
        if (hubCurrencyExchangeRates == null) {
            hubCurrencyExchangeRates = (Hub<CurrencyExchangeRate>) getHub(P_CurrencyExchangeRates);
        }
        return hubCurrencyExchangeRates;
    }

    @OAMany(
        toClass = Store.class, 
        reverseName = Store.P_CurrencyType, 
        createMethod = false
    )
    private Hub<Store> getStores() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAMany(
        displayName = "Currency Exchange Rates", 
        toClass = CurrencyExchangeRate.class, 
        reverseName = CurrencyExchangeRate.P_ToCurrencyType, 
        createMethod = false
    )
    private Hub<CurrencyExchangeRate> getToCurrencyExchangeRates() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.code = rs.getString(3);
        this.name = rs.getString(4);
        this.description = rs.getString(5);
        this.symbol = rs.getString(6);
        this.javaFormatCode = rs.getString(7);
        this.minorUnit = rs.getInt(8);
        setPrimitiveNull(P_MinorUnit, rs.wasNull());
        this.roundingRule = rs.getInt(9);
        setPrimitiveNull(P_RoundingRule, rs.wasNull());

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
