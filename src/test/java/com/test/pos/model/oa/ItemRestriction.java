package com.test.pos.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.lang.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.annotation.*;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.datetime.OADateTime;
import com.viaoa.datetime.OADate;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "itemRestriction",
    pluralName = "ItemRestrictions",
    shortName = "itr",
    displayName = "Item Restriction",
    softDeleteProperty = ItemRestriction.P_DeleteDate,
    versionProperty = ItemRestriction.P_Created,
    timeSeriesProperty = ItemRestriction.P_Created,
    displayProperty = "ruleSearchValue",
    filterClasses = {ItemRestrictionInvalidRuleSearchValueFilter.class},
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "ItemRestrictionRule_Search_Value", columns = {@OAIndexColumn(name = "Rule_Search_Value", lowerName = "Rule_Search_ValueLower")})
    }
)
public class ItemRestriction extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(ItemRestriction.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_ItemRuleType = "itemRuleType";
    public static final String P_ItemRuleTypeInt = "itemRuleTypeInt";
    public static final String P_ItemRuleTypeEnum = "itemRuleTypeEnum";
    public static final String P_ItemRuleTypeDisplay = "itemRuleTypeDisplay";
    public static final String P_Line = "line";
    public static final String P_ProductLineCode = "productLineCode";
    public static final String P_ProductLineSubcode = "productLineSubcode";
    public static final String P_Item = "item";
    public static final String P_LocationRuleType = "locationRuleType";
    public static final String P_LocationRuleTypeInt = "locationRuleTypeInt";
    public static final String P_LocationRuleTypeEnum = "locationRuleTypeEnum";
    public static final String P_LocationRuleTypeDisplay = "locationRuleTypeDisplay";
    public static final String P_StoreId = "storeId";
    public static final String P_Zipcode = "zipcode";
    public static final String P_State = "state";
    public static final String P_County = "county";
    public static final String P_RuleSearchValue = "ruleSearchValue";
    public static final String P_FlightRestricted = "flightRestricted";
    public static final String P_Caustic = "caustic";
    public static final String P_HybridElectric = "hybridElectric";
    public static final String P_FreonRestricted = "freonRestricted";
    public static final String P_SalesRestricted = "salesRestricted";
    public static final String P_SalesRestrictedEffectiveDate = "salesRestrictedEffectiveDate";
    public static final String P_ProcessDate = "processDate";
    public static final String P_DeleteDate = "deleteDate";
     
    public static final String P_VerifyRuleSearchValue = "verifyRuleSearchValue";
    public static final String P_RuleSearchValueDescription = "ruleSearchValueDescription";
    public static final String P_UsesLine = "usesLine";
    public static final String P_UsesProductCode = "usesProductCode";
    public static final String P_UsesProductLineSubcode = "usesProductLineSubcode";
    public static final String P_UsesItem = "usesItem";
    public static final String P_UsesStoreId = "usesStoreId";
    public static final String P_UsesZipcode = "usesZipcode";
    public static final String P_UsesState = "usesState";
    public static final String P_UsesCounty = "usesCounty";
    public static final String P_UsesFlightRestricted = "usesFlightRestricted";
    public static final String P_UsesCaustic = "usesCaustic";
    public static final String P_UsesHybridElectric = "usesHybridElectric";
    public static final String P_UsesFreonRestricted = "usesFreonRestricted";
    public static final String P_UsesRestricted = "usesRestricted";
     
    public static final String M_ReassignRuleSearchValue = "reassignRuleSearchValue";
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String itemRuleType;

    public static enum ItemRuleType {
        LINE("Line"),
        PRODUCT_LINE_CODE("Product Line Code"),
        PRODUCT_LINE_SUBCODE("Product Line Subcode"),
        LINE_ITEM("Line Item");

        private String display;
        ItemRuleType(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int ITEMRULETYPE_LINE = 0;
    public static final int ITEMRULETYPE_PRODUCT_LINE_CODE = 1;
    public static final int ITEMRULETYPE_PRODUCT_LINE_SUBCODE = 2;
    public static final int ITEMRULETYPE_LINE_ITEM = 3;

    protected volatile String line;
    protected volatile int productLineCode;
    protected volatile int productLineSubcode;
    protected volatile String item;
    protected volatile String locationRuleType;

    public static enum LocationRuleType {
        NOT_USED("Not Used"),
        STORE_ID("Store Id"),
        ZIPCODE("Zipcode"),
        STATE("State"),
        COUNTY("County");

        private String display;
        LocationRuleType(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int LOCATIONRULETYPE_NOT_USED = 0;
    public static final int LOCATIONRULETYPE_STORE_ID = 1;
    public static final int LOCATIONRULETYPE_ZIPCODE = 2;
    public static final int LOCATIONRULETYPE_STATE = 3;
    public static final int LOCATIONRULETYPE_COUNTY = 4;

    protected volatile int storeId;
    protected volatile String zipcode;
    protected volatile String state;
    protected volatile String county;
    protected volatile String ruleSearchValue;
    protected volatile boolean flightRestricted;
    protected volatile boolean caustic;
    protected volatile boolean hybridElectric;
    protected volatile boolean freonRestricted;
    protected volatile boolean salesRestricted;
    protected volatile OADate salesRestrictedEffectiveDate;
    protected volatile OADate processDate;
    protected volatile OADate deleteDate;
     
    public ItemRestriction() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public ItemRestriction(int id) {
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

    @OAProperty(lowerName = "created", defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true)
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

    @OAProperty(lowerName = "itemRuleType", displayName = "Item Rule Type", maxLength = 25, displayLength = 20, isNameValue = true)
    @OAColumn(name = "Item_Rule_Type", maxLength = 25)
    public String getItemRuleType() {
        return itemRuleType;
    }
    public void setItemRuleType(String newValue) {
        String old = itemRuleType;
        fireBeforePropertyChange(P_ItemRuleType, old, newValue);
        this.itemRuleType = newValue;
        firePropertyChange(P_ItemRuleType, old, this.itemRuleType);
    }
    @OAProperty(enumPropertyName = P_ItemRuleType)
    public int getItemRuleTypeInt() {
        ItemRuleType itemRuleType = getItemRuleTypeEnum();
        if (itemRuleType == null) return -1;
        return itemRuleType.ordinal();
    }
    public void setItemRuleTypeInt(int val) {
        if (val < 0 || val >= ItemRuleType.values().length) setItemRuleType((String) null);
        else setItemRuleType(ItemRuleType.values()[val].name());
    }
    @OAProperty(enumPropertyName = P_ItemRuleType)
    public ItemRuleType getItemRuleTypeEnum() {
        String val = getItemRuleType();
        if (OAString.isEmpty(val)) return null;
        for (ItemRuleType itemRuleType : ItemRuleType.values()) {
            if (itemRuleType.name().equalsIgnoreCase(val)) return itemRuleType;
        }
        return null;
    }
    public void setItemRuleTypeEnum(ItemRuleType val) {
        String sval = (val == null ? null : val.name());
        setItemRuleType(sval);
    }
    @OACalculatedProperty(enumPropertyName = P_ItemRuleType, displayName = "Item Rule Type", displayLength = 20, columnLength = 20, properties = {P_ItemRuleType} )
    public String getItemRuleTypeDisplay() {
        ItemRuleType itemRuleType = getItemRuleTypeEnum();
        if (itemRuleType == null) return null;
        return itemRuleType.getDisplay();
    }

    @OAProperty(lowerName = "line", maxLength = 3, displayLength = 20)
    @OAColumn(name = "Line", maxLength = 3)
    public String getLine() {
        return line;
    }
    public void setLine(String newValue) {
        String old = line;
        fireBeforePropertyChange(P_Line, old, newValue);
        this.line = newValue;
        firePropertyChange(P_Line, old, this.line);
    }
     
    @OAObjCallback(enabledProperty = ItemRestriction.P_UsesLine)
    public void lineCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "productLineCode", displayName = "Product Line Code", displayLength = 6, uiColumnLength = 17)
    @OAColumn(name = "Product_Line_Code", sqlType = java.sql.Types.INTEGER)
    public int getProductLineCode() {
        return productLineCode;
    }
    public void setProductLineCode(int newValue) {
        int old = productLineCode;
        fireBeforePropertyChange(P_ProductLineCode, old, newValue);
        this.productLineCode = newValue;
        firePropertyChange(P_ProductLineCode, old, this.productLineCode);
    }
     
    @OAObjCallback(enabledProperty = ItemRestriction.P_UsesProductCode)
    public void productLineCodeCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "productLineSubcode", displayName = "Product Line Subcode", displayLength = 6, uiColumnLength = 20)
    @OAColumn(name = "Product_Line_Subcode", sqlType = java.sql.Types.INTEGER)
    public int getProductLineSubcode() {
        return productLineSubcode;
    }
    public void setProductLineSubcode(int newValue) {
        int old = productLineSubcode;
        fireBeforePropertyChange(P_ProductLineSubcode, old, newValue);
        this.productLineSubcode = newValue;
        firePropertyChange(P_ProductLineSubcode, old, this.productLineSubcode);
    }
     
    @OAObjCallback(enabledProperty = ItemRestriction.P_UsesProductLineSubcode)
    public void productLineSubcodeCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "item", maxLength = 14, displayLength = 20)
    @OAColumn(name = "Item", maxLength = 14)
    public String getItem() {
        return item;
    }
    public void setItem(String newValue) {
        String old = item;
        fireBeforePropertyChange(P_Item, old, newValue);
        this.item = newValue;
        firePropertyChange(P_Item, old, this.item);
    }
     
    @OAObjCallback(enabledProperty = ItemRestriction.P_UsesItem)
    public void itemCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "locationRuleType", displayName = "Location Rule Type", maxLength = 20, displayLength = 20, isNameValue = true)
    @OAColumn(name = "Location_Rule_Type", maxLength = 20)
    public String getLocationRuleType() {
        return locationRuleType;
    }
    public void setLocationRuleType(String newValue) {
        String old = locationRuleType;
        fireBeforePropertyChange(P_LocationRuleType, old, newValue);
        this.locationRuleType = newValue;
        firePropertyChange(P_LocationRuleType, old, this.locationRuleType);
    }
    @OAProperty(enumPropertyName = P_LocationRuleType)
    public int getLocationRuleTypeInt() {
        LocationRuleType locationRuleType = getLocationRuleTypeEnum();
        if (locationRuleType == null) return -1;
        return locationRuleType.ordinal();
    }
    public void setLocationRuleTypeInt(int val) {
        if (val < 0 || val >= LocationRuleType.values().length) setLocationRuleType((String) null);
        else setLocationRuleType(LocationRuleType.values()[val].name());
    }
    @OAProperty(enumPropertyName = P_LocationRuleType)
    public LocationRuleType getLocationRuleTypeEnum() {
        String val = getLocationRuleType();
        if (OAString.isEmpty(val)) return null;
        for (LocationRuleType locationRuleType : LocationRuleType.values()) {
            if (locationRuleType.name().equalsIgnoreCase(val)) return locationRuleType;
        }
        return null;
    }
    public void setLocationRuleTypeEnum(LocationRuleType val) {
        String sval = (val == null ? null : val.name());
        setLocationRuleType(sval);
    }
    @OACalculatedProperty(enumPropertyName = P_LocationRuleType, displayName = "Location Rule Type", displayLength = 20, columnLength = 20, properties = {P_LocationRuleType} )
    public String getLocationRuleTypeDisplay() {
        LocationRuleType locationRuleType = getLocationRuleTypeEnum();
        if (locationRuleType == null) return null;
        return locationRuleType.getDisplay();
    }

    @OAProperty(lowerName = "storeId", displayName = "Store Id", displayLength = 6, uiColumnLength = 8)
    @OAColumn(name = "Store_Id", sqlType = java.sql.Types.INTEGER)
    public int getStoreId() {
        return storeId;
    }
    public void setStoreId(int newValue) {
        int old = storeId;
        fireBeforePropertyChange(P_StoreId, old, newValue);
        this.storeId = newValue;
        firePropertyChange(P_StoreId, old, this.storeId);
    }
     
    @OAObjCallback(enabledProperty = ItemRestriction.P_UsesStoreId)
    public void storeIdCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "zipcode", maxLength = 5, displayLength = 20)
    @OAColumn(name = "Zipcode", maxLength = 5)
    public String getZipcode() {
        return zipcode;
    }
    public void setZipcode(String newValue) {
        String old = zipcode;
        fireBeforePropertyChange(P_Zipcode, old, newValue);
        this.zipcode = newValue;
        firePropertyChange(P_Zipcode, old, this.zipcode);
    }
     
    @OAObjCallback(enabledProperty = ItemRestriction.P_UsesZipcode)
    public void zipcodeCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "state", maxLength = 20, displayLength = 20)
    @OAColumn(name = "State", maxLength = 20)
    public String getState() {
        return state;
    }
    public void setState(String newValue) {
        String old = state;
        fireBeforePropertyChange(P_State, old, newValue);
        this.state = newValue;
        firePropertyChange(P_State, old, this.state);
    }
     
    @OAObjCallback(enabledProperty = ItemRestriction.P_UsesState)
    public void stateCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "county", maxLength = 30, displayLength = 20)
    @OAColumn(name = "County", maxLength = 30)
    public String getCounty() {
        return county;
    }
    public void setCounty(String newValue) {
        String old = county;
        fireBeforePropertyChange(P_County, old, newValue);
        this.county = newValue;
        firePropertyChange(P_County, old, this.county);
    }
     
    @OAObjCallback(enabledProperty = ItemRestriction.P_UsesCounty)
    public void countyCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "ruleSearchValue", displayName = "Rule Search Value", maxLength = 75, displayLength = 20, hasCustomCode = true, isProcessed = true)
    @OAColumn(name = "Rule_Search_Value", maxLength = 75)
    public String getRuleSearchValue() {
        if (ruleSearchValue == null) {
            String s = ItemRestrictionDelegate.getRuleSearchValue(this);
            setRuleSearchValue(s);
        }
        return ruleSearchValue;
    }
    public void setRuleSearchValue(String newValue) {
        String old = ruleSearchValue;
        fireBeforePropertyChange(P_RuleSearchValue, old, newValue);
        this.ruleSearchValue = newValue;
        firePropertyChange(P_RuleSearchValue, old, this.ruleSearchValue);
    }
     
    @OAObjCallback(enabledProperty = ItemRestriction.P_VerifyRuleSearchValue, enabledValue = false)
    public void ruleSearchValueCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "flightRestricted", displayName = "Flight Restricted", trackPrimitiveNull = false, displayLength = 5, uiColumnLength = 17)
    @OAColumn(name = "Flight_Restricted", sqlType = java.sql.Types.BOOLEAN)
    public boolean getFlightRestricted() {
        return flightRestricted;
    }
    public boolean isFlightRestricted() {
        return getFlightRestricted();
    }
    public void setFlightRestricted(boolean newValue) {
        boolean old = flightRestricted;
        fireBeforePropertyChange(P_FlightRestricted, old, newValue);
        this.flightRestricted = newValue;
        firePropertyChange(P_FlightRestricted, old, this.flightRestricted);
    }
     
    @OAObjCallback(enabledProperty = ItemRestriction.P_UsesFlightRestricted)
    public void flightRestrictedCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "caustic", trackPrimitiveNull = false, displayLength = 5, uiColumnLength = 7)
    @OAColumn(name = "Caustic", sqlType = java.sql.Types.BOOLEAN)
    public boolean getCaustic() {
        return caustic;
    }
    public boolean isCaustic() {
        return getCaustic();
    }
    public void setCaustic(boolean newValue) {
        boolean old = caustic;
        fireBeforePropertyChange(P_Caustic, old, newValue);
        this.caustic = newValue;
        firePropertyChange(P_Caustic, old, this.caustic);
    }
     
    @OAObjCallback(enabledProperty = ItemRestriction.P_UsesCaustic)
    public void causticCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "hybridElectric", displayName = "Hybrid Electric", trackPrimitiveNull = false, displayLength = 5, uiColumnLength = 15)
    @OAColumn(name = "Hybrid_Electric", sqlType = java.sql.Types.BOOLEAN)
    public boolean getHybridElectric() {
        return hybridElectric;
    }
    public boolean isHybridElectric() {
        return getHybridElectric();
    }
    public void setHybridElectric(boolean newValue) {
        boolean old = hybridElectric;
        fireBeforePropertyChange(P_HybridElectric, old, newValue);
        this.hybridElectric = newValue;
        firePropertyChange(P_HybridElectric, old, this.hybridElectric);
    }
     
    @OAObjCallback(enabledProperty = ItemRestriction.P_UsesHybridElectric)
    public void hybridElectricCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "freonRestricted", displayName = "Freon Restricted", trackPrimitiveNull = false, displayLength = 5, uiColumnLength = 16)
    @OAColumn(name = "Freon_Restricted", sqlType = java.sql.Types.BOOLEAN)
    public boolean getFreonRestricted() {
        return freonRestricted;
    }
    public boolean isFreonRestricted() {
        return getFreonRestricted();
    }
    public void setFreonRestricted(boolean newValue) {
        boolean old = freonRestricted;
        fireBeforePropertyChange(P_FreonRestricted, old, newValue);
        this.freonRestricted = newValue;
        firePropertyChange(P_FreonRestricted, old, this.freonRestricted);
    }
     
    @OAObjCallback(enabledProperty = ItemRestriction.P_UsesFreonRestricted)
    public void freonRestrictedCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "salesRestricted", displayName = "Sales Restricted", trackPrimitiveNull = false, displayLength = 5, uiColumnLength = 16)
    @OAColumn(name = "Sales_Restricted", sqlType = java.sql.Types.BOOLEAN)
    public boolean getSalesRestricted() {
        return salesRestricted;
    }
    public boolean isSalesRestricted() {
        return getSalesRestricted();
    }
    public void setSalesRestricted(boolean newValue) {
        boolean old = salesRestricted;
        fireBeforePropertyChange(P_SalesRestricted, old, newValue);
        this.salesRestricted = newValue;
        firePropertyChange(P_SalesRestricted, old, this.salesRestricted);
    }
     
    @OAObjCallback(enabledProperty = ItemRestriction.P_UsesRestricted)
    public void salesRestrictedCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "salesRestrictedEffectiveDate", displayName = "Sales Restricted Effective Date", displayLength = 8, uiColumnLength = 31)
    @OAColumn(name = "Sales_Restricted_Effective_Date", sqlType = java.sql.Types.DATE)
    public OADate getSalesRestrictedEffectiveDate() {
        return salesRestrictedEffectiveDate;
    }
    public void setSalesRestrictedEffectiveDate(OADate newValue) {
        OADate old = salesRestrictedEffectiveDate;
        fireBeforePropertyChange(P_SalesRestrictedEffectiveDate, old, newValue);
        this.salesRestrictedEffectiveDate = newValue;
        firePropertyChange(P_SalesRestrictedEffectiveDate, old, this.salesRestrictedEffectiveDate);
    }
     
    @OAObjCallback(enabledProperty = ItemRestriction.P_UsesRestricted)
    public void salesRestrictedEffectiveDateCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "processDate", displayName = "Process Date", displayLength = 8, uiColumnLength = 12, isProcessed = true)
    public OADate getProcessDate() {
        return processDate;
    }
    public void setProcessDate(OADate newValue) {
        OADate old = processDate;
        fireBeforePropertyChange(P_ProcessDate, old, newValue);
        this.processDate = newValue;
        firePropertyChange(P_ProcessDate, old, this.processDate);
    }

    @OAProperty(lowerName = "deleteDate", displayName = "Delete Date", displayLength = 8, uiColumnLength = 11)
    public OADate getDeleteDate() {
        return deleteDate;
    }
    public void setDeleteDate(OADate newValue) {
        OADate old = deleteDate;
        fireBeforePropertyChange(P_DeleteDate, old, newValue);
        this.deleteDate = newValue;
        firePropertyChange(P_DeleteDate, old, this.deleteDate);
    }
    @OACalculatedProperty(displayName = "Verify Rule Search Value", displayLength = 5, columnLength = 24, properties = {P_ItemRuleType, P_Line, P_ProductLineCode, P_ProductLineSubcode, P_Item, P_State, P_County, P_Zipcode, P_StoreId, P_RuleSearchValue})
    public boolean getVerifyRuleSearchValue() {
        boolean b = OAString.isEqual(getRuleSearchValue(), ItemRestrictionDelegate.getRuleSearchValue(this));
        return b;
    }
    @OACalculatedProperty(displayName = "Rule Search Value Description", displayLength = 20, columnLength = 29, properties = {P_RuleSearchValue})
    public String getRuleSearchValueDescription() {
        String s = ItemRestrictionDelegate.getRuleSearchValueDescription(this.getRuleSearchValue());
        return s;
    }
    @OACalculatedProperty(displayName = "Uses Line", displayLength = 5, columnLength = 9, properties = {P_ItemRuleType})
    public boolean getUsesLine() {
        int x = getItemRuleTypeInt();
        boolean b = x >= 0;
        return b;
    }
    @OACalculatedProperty(displayName = "Uses Product Code", displayLength = 5, columnLength = 17, properties = {P_ItemRuleType})
    public boolean getUsesProductCode() {
        final int x = getItemRuleTypeInt();
        boolean b = x == ITEMRULETYPE_PRODUCT_LINE_CODE || x == ITEMRULETYPE_PRODUCT_LINE_SUBCODE;
        return b;
    }
    @OACalculatedProperty(displayName = "Uses Product Line Subcode", displayLength = 5, columnLength = 25, properties = {P_ItemRuleType})
    public boolean getUsesProductLineSubcode() {
        final int x = getItemRuleTypeInt();
        boolean b = x == ITEMRULETYPE_PRODUCT_LINE_SUBCODE;
        return b;
    }
    @OACalculatedProperty(displayName = "Uses Item", displayLength = 5, columnLength = 9, properties = {P_ItemRuleType})
    public boolean getUsesItem() {
        final int x = getItemRuleTypeInt();
        boolean b = x == ITEMRULETYPE_LINE_ITEM;
        return b;
    }
    @OACalculatedProperty(displayName = "Uses Store Id", displayLength = 5, columnLength = 13, properties = {P_LocationRuleType})
    public boolean getUsesStoreId() {
        boolean b = getLocationRuleTypeInt() == LOCATIONRULETYPE_STORE_ID;
        return b;
    }
    @OACalculatedProperty(displayName = "Uses Zipcode", displayLength = 5, columnLength = 12, properties = {P_LocationRuleType})
    public boolean getUsesZipcode() {
        boolean b = getLocationRuleTypeInt() == LOCATIONRULETYPE_ZIPCODE;
        return b;     
    }
    @OACalculatedProperty(displayName = "Uses State", displayLength = 5, columnLength = 10, properties = {P_LocationRuleType})
    public boolean getUsesState() {
        final int x = getLocationRuleTypeInt();
        boolean b = x == LOCATIONRULETYPE_STATE || x == LOCATIONRULETYPE_COUNTY;
        return b;        
    }
    @OACalculatedProperty(displayName = "Uses County", displayLength = 5, columnLength = 11, properties = {P_LocationRuleType})
    public boolean getUsesCounty() {
        boolean b = getLocationRuleTypeInt() == LOCATIONRULETYPE_COUNTY;
        return b ;
    }
    @OACalculatedProperty(displayName = "Uses Flight Restricted", displayLength = 5, columnLength = 22, properties = {P_ItemRuleType, P_LocationRuleType})
    public boolean getUsesFlightRestricted() {
        int itemRuleType = this.getItemRuleTypeInt();
        int locationRuleType = this.getLocationRuleTypeInt();
        boolean b = itemRuleType == ITEMRULETYPE_LINE_ITEM && locationRuleType == LOCATIONRULETYPE_NOT_USED;
        return b;
    }
    @OACalculatedProperty(displayName = "Uses Caustic", displayLength = 5, columnLength = 12, properties = {P_ItemRuleType, P_LocationRuleType})
    public boolean getUsesCaustic() {
        int itemRuleType = this.getItemRuleTypeInt();
        int locationRuleType = this.getLocationRuleTypeInt();
        boolean b = itemRuleType == ITEMRULETYPE_LINE_ITEM && locationRuleType == LOCATIONRULETYPE_NOT_USED;
        return b;
    }
    @OACalculatedProperty(displayName = "Uses Hybrid Electric", displayLength = 5, columnLength = 20, properties = {P_ItemRuleType, P_LocationRuleType})
    public boolean getUsesHybridElectric() {
        int itemRuleType = this.getItemRuleTypeInt();
        int locationRuleType = this.getLocationRuleTypeInt();
        boolean b = itemRuleType == ITEMRULETYPE_LINE_ITEM && locationRuleType == LOCATIONRULETYPE_NOT_USED;
        return b;
    }
    @OACalculatedProperty(displayName = "Uses Freon Restricted", displayLength = 5, columnLength = 21, properties = {P_ItemRuleType, P_LocationRuleType})
    public boolean getUsesFreonRestricted() {
        int itemRuleType = this.getItemRuleTypeInt();
        int locationRuleType = this.getLocationRuleTypeInt();
        boolean b = itemRuleType == ITEMRULETYPE_LINE_ITEM && locationRuleType == LOCATIONRULETYPE_STATE;
        return b;
    }
    @OACalculatedProperty(displayName = "Uses Restricted", displayLength = 5, columnLength = 15, properties = {P_ItemRuleType, P_LocationRuleType})
    public boolean getUsesRestricted() {
        int itemRuleType = this.getItemRuleTypeInt();
        int locationRuleType = this.getLocationRuleTypeInt();
        boolean b = locationRuleType != LOCATIONRULETYPE_NOT_USED;
        return b;
    }
    @OAMethod(displayName = "Reassign Rule Search Value")
    public void reassignRuleSearchValue() {
        // use this to run on server (remote)
        if (isRemoteAvailable()) {
            remote();
            return;
        }
        // setting ruleSearchValue to null will cause it to recreate on next get.
        ruleSearchValue = null;
        getRuleSearchValue();
    }
    public static void reassignRuleSearchValue(Hub<ItemRestriction> hub) {
        if (isRemoteAvailable(hub)) {
            callRemote(hub);
            return;
        }
        for (ItemRestriction itemRestriction : hub) {
            itemRestriction.reassignRuleSearchValue();
        }
    }

    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.itemRuleType = rs.getString(3);
        this.line = rs.getString(4);
        this.productLineCode = rs.getInt(5);
        setPrimitiveNull(P_ProductLineCode, rs.wasNull());
        this.productLineSubcode = rs.getInt(6);
        setPrimitiveNull(P_ProductLineSubcode, rs.wasNull());
        this.item = rs.getString(7);
        this.locationRuleType = rs.getString(8);
        this.storeId = rs.getInt(9);
        setPrimitiveNull(P_StoreId, rs.wasNull());
        this.zipcode = rs.getString(10);
        this.state = rs.getString(11);
        this.county = rs.getString(12);
        this.ruleSearchValue = rs.getString(13);
        this.flightRestricted = rs.getBoolean(14);
        this.caustic = rs.getBoolean(15);
        this.hybridElectric = rs.getBoolean(16);
        this.freonRestricted = rs.getBoolean(17);
        this.salesRestricted = rs.getBoolean(18);
        java.sql.Date date;
        date = rs.getDate(19);
        if (date != null) this.salesRestrictedEffectiveDate = new OADate(date);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
