package com.test.pos.model.oa.search;

import com.viaoa.lang.*;
import com.viaoa.select.OASelect;
import java.util.*;
import java.util.logging.*;
import com.test.pos.model.oa.*;
import com.test.pos.model.oa.propertypath.*;
import com.viaoa.annotation.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.hub.filter.*;
import com.viaoa.datetime.OADateTime;
import com.viaoa.datetime.OADate;
import com.viaoa.datasource.*;
import com.viaoa.filter.*;
import com.viaoa.find.*;

@OAClass(useDataSource=false, localOnly=true)
public class ItemRestrictionSearch extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(ItemRestrictionSearch.class.getName());

    public static final String P_Id = "Id";
    public static final String P_Id2 = "Id2";
    public static final String P_Created = "Created";
    public static final String P_Created2 = "Created2";
    public static final String P_ItemRuleType = "ItemRuleType";
    public static final String P_Line = "Line";
    public static final String P_LineUseNull = "LineUseNull";
    public static final String P_LineUseNotNull = "LineUseNotNull";
    public static final String P_ProductLine = "ProductLine";
    public static final String P_ProductLineUseNull = "ProductLineUseNull";
    public static final String P_ProductLineUseNotNull = "ProductLineUseNotNull";
    public static final String P_SubProductLine = "SubProductLine";
    public static final String P_SubProductLineUseNull = "SubProductLineUseNull";
    public static final String P_SubProductLineUseNotNull = "SubProductLineUseNotNull";
    public static final String P_Item = "Item";
    public static final String P_ItemUseNull = "ItemUseNull";
    public static final String P_ItemUseNotNull = "ItemUseNotNull";
    public static final String P_LocationRuleType = "LocationRuleType";
    public static final String P_Store = "Store";
    public static final String P_StoreUseNull = "StoreUseNull";
    public static final String P_StoreUseNotNull = "StoreUseNotNull";
    public static final String P_Zipcode = "Zipcode";
    public static final String P_ZipcodeUseNull = "ZipcodeUseNull";
    public static final String P_ZipcodeUseNotNull = "ZipcodeUseNotNull";
    public static final String P_State = "State";
    public static final String P_StateUseNull = "StateUseNull";
    public static final String P_StateUseNotNull = "StateUseNotNull";
    public static final String P_County = "County";
    public static final String P_CountyUseNull = "CountyUseNull";
    public static final String P_CountyUseNotNull = "CountyUseNotNull";
    public static final String P_RuleSearchValue = "RuleSearchValue";
    public static final String P_RuleSearchValueUseNull = "RuleSearchValueUseNull";
    public static final String P_RuleSearchValueUseNotNull = "RuleSearchValueUseNotNull";
    public static final String P_FlightRestriction = "FlightRestriction";
    public static final String P_FlightRestrictionUseNull = "FlightRestrictionUseNull";
    public static final String P_FlightRestrictionUseNotNull = "FlightRestrictionUseNotNull";
    public static final String P_Caustic = "Caustic";
    public static final String P_CausticUseNull = "CausticUseNull";
    public static final String P_CausticUseNotNull = "CausticUseNotNull";
    public static final String P_HybridElectric = "HybridElectric";
    public static final String P_HybridElectricUseNull = "HybridElectricUseNull";
    public static final String P_HybridElectricUseNotNull = "HybridElectricUseNotNull";
    public static final String P_FreonRestricted = "FreonRestricted";
    public static final String P_FreonRestrictedUseNull = "FreonRestrictedUseNull";
    public static final String P_FreonRestrictedUseNotNull = "FreonRestrictedUseNotNull";
    public static final String P_Restricted = "Restricted";
    public static final String P_RestrictedUseNull = "RestrictedUseNull";
    public static final String P_RestrictedUseNotNull = "RestrictedUseNotNull";
    public static final String P_RestrictedEffectiveDate = "RestrictedEffectiveDate";
    public static final String P_RestrictedEffectiveDate2 = "RestrictedEffectiveDate2";
    public static final String P_RestrictedEffectiveDateUseNull = "RestrictedEffectiveDateUseNull";
    public static final String P_RestrictedEffectiveDateUseNotNull = "RestrictedEffectiveDateUseNotNull";
    public static final String P_ProcessDate = "ProcessDate";
    public static final String P_ProcessDate2 = "ProcessDate2";
    public static final String P_ProcessDateUseNull = "ProcessDateUseNull";
    public static final String P_ProcessDateUseNotNull = "ProcessDateUseNotNull";
    public static final String P_DeleteDate = "DeleteDate";
    public static final String P_DeleteDate2 = "DeleteDate2";
    public static final String P_DeleteDateUseNull = "DeleteDateUseNull";
    public static final String P_DeleteDateUseNotNull = "DeleteDateUseNotNull";
    public static final String P_CustomQuery = "CustomQuery";
    public static final String P_MaxResults = "MaxResults";

    protected int id;
    protected int id2;
    protected OADateTime created;
    protected OADateTime created2;
    protected String itemRuleType;
    protected String line;
    protected boolean lineUseNull;
    protected boolean lineUseNotNull;
    protected int productLine;
    protected boolean productLineUseNull;
    protected boolean productLineUseNotNull;
    protected int subProductLine;
    protected boolean subProductLineUseNull;
    protected boolean subProductLineUseNotNull;
    protected String item;
    protected boolean itemUseNull;
    protected boolean itemUseNotNull;
    protected String locationRuleType;
    protected int store;
    protected boolean storeUseNull;
    protected boolean storeUseNotNull;
    protected String zipcode;
    protected boolean zipcodeUseNull;
    protected boolean zipcodeUseNotNull;
    protected String state;
    protected boolean stateUseNull;
    protected boolean stateUseNotNull;
    protected String county;
    protected boolean countyUseNull;
    protected boolean countyUseNotNull;
    protected String ruleSearchValue;
    protected boolean ruleSearchValueUseNull;
    protected boolean ruleSearchValueUseNotNull;
    protected boolean flightRestriction;
    protected boolean flightRestrictionUseNull;
    protected boolean flightRestrictionUseNotNull;
    protected boolean caustic;
    protected boolean causticUseNull;
    protected boolean causticUseNotNull;
    protected boolean hybridElectric;
    protected boolean hybridElectricUseNull;
    protected boolean hybridElectricUseNotNull;
    protected boolean freonRestricted;
    protected boolean freonRestrictedUseNull;
    protected boolean freonRestrictedUseNotNull;
    protected boolean restricted;
    protected boolean restrictedUseNull;
    protected boolean restrictedUseNotNull;
    protected OADate restrictedEffectiveDate;
    protected OADate restrictedEffectiveDate2;
    protected boolean restrictedEffectiveDateUseNull;
    protected boolean restrictedEffectiveDateUseNotNull;
    protected OADate processDate;
    protected OADate processDate2;
    protected boolean processDateUseNull;
    protected boolean processDateUseNotNull;
    protected OADate deleteDate;
    protected OADate deleteDate2;
    protected boolean deleteDateUseNull;
    protected boolean deleteDateUseNotNull;
    protected String customQuery;
    protected int maxResults;

    @OAProperty(lowerName = "id", displayLength = 6)
    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        int old = id;
        fireBeforePropertyChange(P_Id, old, newValue);
        this.id = newValue;
        firePropertyChange(P_Id, old, this.id);
        if (isLoading()) return;
        if (id > id2) setId2(this.id);
    } 
    public int getId2() {
        return id2;
    }
    public void setId2(int newValue) {
        int old = id2;
        fireBeforePropertyChange(P_Id2, old, newValue);
        this.id2 = newValue;
        firePropertyChange(P_Id2, old, this.id2);
        if (isLoading()) return;
        if (id > id2) setId(this.id2);
    }
    @OAProperty(lowerName = "created", defaultValue = "new OADateTime()", displayLength = 15)
    public OADateTime getCreated() {
        return created;
    }
    public void setCreated(OADateTime newValue) {
        OADateTime old = created;
        fireBeforePropertyChange(P_Created, old, newValue);
        this.created = newValue;
        firePropertyChange(P_Created, old, this.created);
        if (isLoading()) return;
        if (created != null) {
            if (created2 == null) setCreated2(this.created.plusDays(1));
            else if (created.compareTo(created2) > 0) setCreated2(this.created.plusDays(1));
        }
    } 
    public OADateTime getCreated2() {
        return created2;
    }
    public void setCreated2(OADateTime newValue) {
        OADateTime old = created2;
        fireBeforePropertyChange(P_Created2, old, newValue);
        this.created2 = newValue;
        firePropertyChange(P_Created2, old, this.created2);
        if (created != null && created2 != null) {
            if (created.compareTo(created2) > 0) setCreated(this.created2);
        }
    }
    @OAProperty(lowerName = "itemRuleType", displayName = "Item Rule Type", maxLength = 25, displayLength = 20)
    public String getItemRuleType() {
        return itemRuleType;
    }
    public void setItemRuleType(String newValue) {
        String old = itemRuleType;
        fireBeforePropertyChange(P_ItemRuleType, old, newValue);
        this.itemRuleType = newValue;
        firePropertyChange(P_ItemRuleType, old, this.itemRuleType);
    }
      
    @OAProperty(lowerName = "line", maxLength = 3, displayLength = 20)
    public String getLine() {
        return line;
    }
    public void setLine(String newValue) {
        String old = line;
        fireBeforePropertyChange(P_Line, old, newValue);
        this.line = newValue;
        firePropertyChange(P_Line, old, this.line);
    }
      
    public boolean getLineUseNull() {
        return lineUseNull;
    }
    public void setLineUseNull(boolean newValue) {
        boolean old = this.lineUseNull;
        this.lineUseNull = newValue;
        firePropertyChange(P_LineUseNull, old, this.lineUseNull);
    }
    public boolean getLineUseNotNull() {
        return lineUseNotNull;
    }
    public void setLineUseNotNull(boolean newValue) {
        boolean old = this.lineUseNotNull;
        this.lineUseNotNull = newValue;
        firePropertyChange(P_LineUseNotNull, old, this.lineUseNotNull);
    }
    @OAProperty(lowerName = "productLineCode", displayName = "Product Line Code", displayLength = 6, uiColumnLength = 17)
    public int getProductLine() {
        return productLine;
    }
    public void setProductLine(int newValue) {
        int old = productLine;
        fireBeforePropertyChange(P_ProductLine, old, newValue);
        this.productLine = newValue;
        firePropertyChange(P_ProductLine, old, this.productLine);
    }
      
    public boolean getProductLineUseNull() {
        return productLineUseNull;
    }
    public void setProductLineUseNull(boolean newValue) {
        boolean old = this.productLineUseNull;
        this.productLineUseNull = newValue;
        firePropertyChange(P_ProductLineUseNull, old, this.productLineUseNull);
    }
    public boolean getProductLineUseNotNull() {
        return productLineUseNotNull;
    }
    public void setProductLineUseNotNull(boolean newValue) {
        boolean old = this.productLineUseNotNull;
        this.productLineUseNotNull = newValue;
        firePropertyChange(P_ProductLineUseNotNull, old, this.productLineUseNotNull);
    }
    @OAProperty(lowerName = "productLineSubcode", displayName = "Product Line Subcode", displayLength = 6, uiColumnLength = 20)
    public int getSubProductLine() {
        return subProductLine;
    }
    public void setSubProductLine(int newValue) {
        int old = subProductLine;
        fireBeforePropertyChange(P_SubProductLine, old, newValue);
        this.subProductLine = newValue;
        firePropertyChange(P_SubProductLine, old, this.subProductLine);
    }
      
    public boolean getSubProductLineUseNull() {
        return subProductLineUseNull;
    }
    public void setSubProductLineUseNull(boolean newValue) {
        boolean old = this.subProductLineUseNull;
        this.subProductLineUseNull = newValue;
        firePropertyChange(P_SubProductLineUseNull, old, this.subProductLineUseNull);
    }
    public boolean getSubProductLineUseNotNull() {
        return subProductLineUseNotNull;
    }
    public void setSubProductLineUseNotNull(boolean newValue) {
        boolean old = this.subProductLineUseNotNull;
        this.subProductLineUseNotNull = newValue;
        firePropertyChange(P_SubProductLineUseNotNull, old, this.subProductLineUseNotNull);
    }
    @OAProperty(lowerName = "item", maxLength = 14, displayLength = 20)
    public String getItem() {
        return item;
    }
    public void setItem(String newValue) {
        String old = item;
        fireBeforePropertyChange(P_Item, old, newValue);
        this.item = newValue;
        firePropertyChange(P_Item, old, this.item);
    }
      
    public boolean getItemUseNull() {
        return itemUseNull;
    }
    public void setItemUseNull(boolean newValue) {
        boolean old = this.itemUseNull;
        this.itemUseNull = newValue;
        firePropertyChange(P_ItemUseNull, old, this.itemUseNull);
    }
    public boolean getItemUseNotNull() {
        return itemUseNotNull;
    }
    public void setItemUseNotNull(boolean newValue) {
        boolean old = this.itemUseNotNull;
        this.itemUseNotNull = newValue;
        firePropertyChange(P_ItemUseNotNull, old, this.itemUseNotNull);
    }
    @OAProperty(lowerName = "locationRuleType", displayName = "Location Rule Type", maxLength = 20, displayLength = 20)
    public String getLocationRuleType() {
        return locationRuleType;
    }
    public void setLocationRuleType(String newValue) {
        String old = locationRuleType;
        fireBeforePropertyChange(P_LocationRuleType, old, newValue);
        this.locationRuleType = newValue;
        firePropertyChange(P_LocationRuleType, old, this.locationRuleType);
    }
      
    @OAProperty(lowerName = "storeId", displayName = "Store Id", displayLength = 6, uiColumnLength = 8)
    public int getStore() {
        return store;
    }
    public void setStore(int newValue) {
        int old = store;
        fireBeforePropertyChange(P_Store, old, newValue);
        this.store = newValue;
        firePropertyChange(P_Store, old, this.store);
    }
      
    public boolean getStoreUseNull() {
        return storeUseNull;
    }
    public void setStoreUseNull(boolean newValue) {
        boolean old = this.storeUseNull;
        this.storeUseNull = newValue;
        firePropertyChange(P_StoreUseNull, old, this.storeUseNull);
    }
    public boolean getStoreUseNotNull() {
        return storeUseNotNull;
    }
    public void setStoreUseNotNull(boolean newValue) {
        boolean old = this.storeUseNotNull;
        this.storeUseNotNull = newValue;
        firePropertyChange(P_StoreUseNotNull, old, this.storeUseNotNull);
    }
    @OAProperty(lowerName = "zipcode", maxLength = 5, displayLength = 20)
    public String getZipcode() {
        return zipcode;
    }
    public void setZipcode(String newValue) {
        String old = zipcode;
        fireBeforePropertyChange(P_Zipcode, old, newValue);
        this.zipcode = newValue;
        firePropertyChange(P_Zipcode, old, this.zipcode);
    }
      
    public boolean getZipcodeUseNull() {
        return zipcodeUseNull;
    }
    public void setZipcodeUseNull(boolean newValue) {
        boolean old = this.zipcodeUseNull;
        this.zipcodeUseNull = newValue;
        firePropertyChange(P_ZipcodeUseNull, old, this.zipcodeUseNull);
    }
    public boolean getZipcodeUseNotNull() {
        return zipcodeUseNotNull;
    }
    public void setZipcodeUseNotNull(boolean newValue) {
        boolean old = this.zipcodeUseNotNull;
        this.zipcodeUseNotNull = newValue;
        firePropertyChange(P_ZipcodeUseNotNull, old, this.zipcodeUseNotNull);
    }
    @OAProperty(lowerName = "state", maxLength = 20, displayLength = 20)
    public String getState() {
        return state;
    }
    public void setState(String newValue) {
        String old = state;
        fireBeforePropertyChange(P_State, old, newValue);
        this.state = newValue;
        firePropertyChange(P_State, old, this.state);
    }
      
    public boolean getStateUseNull() {
        return stateUseNull;
    }
    public void setStateUseNull(boolean newValue) {
        boolean old = this.stateUseNull;
        this.stateUseNull = newValue;
        firePropertyChange(P_StateUseNull, old, this.stateUseNull);
    }
    public boolean getStateUseNotNull() {
        return stateUseNotNull;
    }
    public void setStateUseNotNull(boolean newValue) {
        boolean old = this.stateUseNotNull;
        this.stateUseNotNull = newValue;
        firePropertyChange(P_StateUseNotNull, old, this.stateUseNotNull);
    }
    @OAProperty(lowerName = "county", maxLength = 30, displayLength = 20)
    public String getCounty() {
        return county;
    }
    public void setCounty(String newValue) {
        String old = county;
        fireBeforePropertyChange(P_County, old, newValue);
        this.county = newValue;
        firePropertyChange(P_County, old, this.county);
    }
      
    public boolean getCountyUseNull() {
        return countyUseNull;
    }
    public void setCountyUseNull(boolean newValue) {
        boolean old = this.countyUseNull;
        this.countyUseNull = newValue;
        firePropertyChange(P_CountyUseNull, old, this.countyUseNull);
    }
    public boolean getCountyUseNotNull() {
        return countyUseNotNull;
    }
    public void setCountyUseNotNull(boolean newValue) {
        boolean old = this.countyUseNotNull;
        this.countyUseNotNull = newValue;
        firePropertyChange(P_CountyUseNotNull, old, this.countyUseNotNull);
    }
    @OAProperty(lowerName = "ruleSearchValue", displayName = "Rule Search Value", maxLength = 75, displayLength = 20)
    public String getRuleSearchValue() {
        return ruleSearchValue;
    }
    public void setRuleSearchValue(String newValue) {
        String old = ruleSearchValue;
        fireBeforePropertyChange(P_RuleSearchValue, old, newValue);
        this.ruleSearchValue = newValue;
        firePropertyChange(P_RuleSearchValue, old, this.ruleSearchValue);
    }
      
    public boolean getRuleSearchValueUseNull() {
        return ruleSearchValueUseNull;
    }
    public void setRuleSearchValueUseNull(boolean newValue) {
        boolean old = this.ruleSearchValueUseNull;
        this.ruleSearchValueUseNull = newValue;
        firePropertyChange(P_RuleSearchValueUseNull, old, this.ruleSearchValueUseNull);
    }
    public boolean getRuleSearchValueUseNotNull() {
        return ruleSearchValueUseNotNull;
    }
    public void setRuleSearchValueUseNotNull(boolean newValue) {
        boolean old = this.ruleSearchValueUseNotNull;
        this.ruleSearchValueUseNotNull = newValue;
        firePropertyChange(P_RuleSearchValueUseNotNull, old, this.ruleSearchValueUseNotNull);
    }
    @OAProperty(lowerName = "flightRestricted", displayName = "Flight Restricted", displayLength = 5, uiColumnLength = 17)
    public boolean getFlightRestriction() {
        return flightRestriction;
    }
    public boolean isFlightRestriction() {
        return getFlightRestriction();
    }
    public void setFlightRestriction(boolean newValue) {
        boolean old = flightRestriction;
        fireBeforePropertyChange(P_FlightRestriction, old, newValue);
        this.flightRestriction = newValue;
        firePropertyChange(P_FlightRestriction, old, this.flightRestriction);
    }
      
    public boolean getFlightRestrictionUseNull() {
        return flightRestrictionUseNull;
    }
    public void setFlightRestrictionUseNull(boolean newValue) {
        boolean old = this.flightRestrictionUseNull;
        this.flightRestrictionUseNull = newValue;
        firePropertyChange(P_FlightRestrictionUseNull, old, this.flightRestrictionUseNull);
    }
    public boolean getFlightRestrictionUseNotNull() {
        return flightRestrictionUseNotNull;
    }
    public void setFlightRestrictionUseNotNull(boolean newValue) {
        boolean old = this.flightRestrictionUseNotNull;
        this.flightRestrictionUseNotNull = newValue;
        firePropertyChange(P_FlightRestrictionUseNotNull, old, this.flightRestrictionUseNotNull);
    }
    @OAProperty(lowerName = "caustic", displayLength = 5, uiColumnLength = 7)
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
      
    public boolean getCausticUseNull() {
        return causticUseNull;
    }
    public void setCausticUseNull(boolean newValue) {
        boolean old = this.causticUseNull;
        this.causticUseNull = newValue;
        firePropertyChange(P_CausticUseNull, old, this.causticUseNull);
    }
    public boolean getCausticUseNotNull() {
        return causticUseNotNull;
    }
    public void setCausticUseNotNull(boolean newValue) {
        boolean old = this.causticUseNotNull;
        this.causticUseNotNull = newValue;
        firePropertyChange(P_CausticUseNotNull, old, this.causticUseNotNull);
    }
    @OAProperty(lowerName = "hybridElectric", displayName = "Hybrid Electric", displayLength = 5, uiColumnLength = 15)
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
      
    public boolean getHybridElectricUseNull() {
        return hybridElectricUseNull;
    }
    public void setHybridElectricUseNull(boolean newValue) {
        boolean old = this.hybridElectricUseNull;
        this.hybridElectricUseNull = newValue;
        firePropertyChange(P_HybridElectricUseNull, old, this.hybridElectricUseNull);
    }
    public boolean getHybridElectricUseNotNull() {
        return hybridElectricUseNotNull;
    }
    public void setHybridElectricUseNotNull(boolean newValue) {
        boolean old = this.hybridElectricUseNotNull;
        this.hybridElectricUseNotNull = newValue;
        firePropertyChange(P_HybridElectricUseNotNull, old, this.hybridElectricUseNotNull);
    }
    @OAProperty(lowerName = "freonRestricted", displayName = "Freon Restricted", displayLength = 5, uiColumnLength = 16)
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
      
    public boolean getFreonRestrictedUseNull() {
        return freonRestrictedUseNull;
    }
    public void setFreonRestrictedUseNull(boolean newValue) {
        boolean old = this.freonRestrictedUseNull;
        this.freonRestrictedUseNull = newValue;
        firePropertyChange(P_FreonRestrictedUseNull, old, this.freonRestrictedUseNull);
    }
    public boolean getFreonRestrictedUseNotNull() {
        return freonRestrictedUseNotNull;
    }
    public void setFreonRestrictedUseNotNull(boolean newValue) {
        boolean old = this.freonRestrictedUseNotNull;
        this.freonRestrictedUseNotNull = newValue;
        firePropertyChange(P_FreonRestrictedUseNotNull, old, this.freonRestrictedUseNotNull);
    }
    @OAProperty(lowerName = "salesRestricted", displayName = "Sales Restricted", displayLength = 5, uiColumnLength = 16)
    public boolean getRestricted() {
        return restricted;
    }
    public boolean isRestricted() {
        return getRestricted();
    }
    public void setRestricted(boolean newValue) {
        boolean old = restricted;
        fireBeforePropertyChange(P_Restricted, old, newValue);
        this.restricted = newValue;
        firePropertyChange(P_Restricted, old, this.restricted);
    }
      
    public boolean getRestrictedUseNull() {
        return restrictedUseNull;
    }
    public void setRestrictedUseNull(boolean newValue) {
        boolean old = this.restrictedUseNull;
        this.restrictedUseNull = newValue;
        firePropertyChange(P_RestrictedUseNull, old, this.restrictedUseNull);
    }
    public boolean getRestrictedUseNotNull() {
        return restrictedUseNotNull;
    }
    public void setRestrictedUseNotNull(boolean newValue) {
        boolean old = this.restrictedUseNotNull;
        this.restrictedUseNotNull = newValue;
        firePropertyChange(P_RestrictedUseNotNull, old, this.restrictedUseNotNull);
    }
    @OAProperty(lowerName = "salesRestrictedEffectiveDate", displayName = "Sales Restricted Effective Date", displayLength = 8, uiColumnLength = 31)
    public OADate getRestrictedEffectiveDate() {
        return restrictedEffectiveDate;
    }
    public void setRestrictedEffectiveDate(OADate newValue) {
        OADate old = restrictedEffectiveDate;
        fireBeforePropertyChange(P_RestrictedEffectiveDate, old, newValue);
        this.restrictedEffectiveDate = newValue;
        firePropertyChange(P_RestrictedEffectiveDate, old, this.restrictedEffectiveDate);
        if (isLoading()) return;
        if (restrictedEffectiveDate != null) {
            if (restrictedEffectiveDate2 == null) setRestrictedEffectiveDate2(this.restrictedEffectiveDate);
            else if (restrictedEffectiveDate.compareTo(restrictedEffectiveDate2) > 0) setRestrictedEffectiveDate2(this.restrictedEffectiveDate);
        }
    } 
    public OADate getRestrictedEffectiveDate2() {
        return restrictedEffectiveDate2;
    }
    public void setRestrictedEffectiveDate2(OADate newValue) {
        OADate old = restrictedEffectiveDate2;
        fireBeforePropertyChange(P_RestrictedEffectiveDate2, old, newValue);
        this.restrictedEffectiveDate2 = newValue;
        firePropertyChange(P_RestrictedEffectiveDate2, old, this.restrictedEffectiveDate2);
        if (restrictedEffectiveDate != null && restrictedEffectiveDate2 != null) {
            if (restrictedEffectiveDate.compareTo(restrictedEffectiveDate2) > 0) setRestrictedEffectiveDate(this.restrictedEffectiveDate2);
        }
    }
    public boolean getRestrictedEffectiveDateUseNull() {
        return restrictedEffectiveDateUseNull;
    }
    public void setRestrictedEffectiveDateUseNull(boolean newValue) {
        boolean old = this.restrictedEffectiveDateUseNull;
        this.restrictedEffectiveDateUseNull = newValue;
        firePropertyChange(P_RestrictedEffectiveDateUseNull, old, this.restrictedEffectiveDateUseNull);
    }
    public boolean getRestrictedEffectiveDateUseNotNull() {
        return restrictedEffectiveDateUseNotNull;
    }
    public void setRestrictedEffectiveDateUseNotNull(boolean newValue) {
        boolean old = this.restrictedEffectiveDateUseNotNull;
        this.restrictedEffectiveDateUseNotNull = newValue;
        firePropertyChange(P_RestrictedEffectiveDateUseNotNull, old, this.restrictedEffectiveDateUseNotNull);
    }
    @OAProperty(lowerName = "processDate", displayName = "Process Date", displayLength = 8, uiColumnLength = 12)
    public OADate getProcessDate() {
        return processDate;
    }
    public void setProcessDate(OADate newValue) {
        OADate old = processDate;
        fireBeforePropertyChange(P_ProcessDate, old, newValue);
        this.processDate = newValue;
        firePropertyChange(P_ProcessDate, old, this.processDate);
        if (isLoading()) return;
        if (processDate != null) {
            if (processDate2 == null) setProcessDate2(this.processDate);
            else if (processDate.compareTo(processDate2) > 0) setProcessDate2(this.processDate);
        }
    } 
    public OADate getProcessDate2() {
        return processDate2;
    }
    public void setProcessDate2(OADate newValue) {
        OADate old = processDate2;
        fireBeforePropertyChange(P_ProcessDate2, old, newValue);
        this.processDate2 = newValue;
        firePropertyChange(P_ProcessDate2, old, this.processDate2);
        if (processDate != null && processDate2 != null) {
            if (processDate.compareTo(processDate2) > 0) setProcessDate(this.processDate2);
        }
    }
    public boolean getProcessDateUseNull() {
        return processDateUseNull;
    }
    public void setProcessDateUseNull(boolean newValue) {
        boolean old = this.processDateUseNull;
        this.processDateUseNull = newValue;
        firePropertyChange(P_ProcessDateUseNull, old, this.processDateUseNull);
    }
    public boolean getProcessDateUseNotNull() {
        return processDateUseNotNull;
    }
    public void setProcessDateUseNotNull(boolean newValue) {
        boolean old = this.processDateUseNotNull;
        this.processDateUseNotNull = newValue;
        firePropertyChange(P_ProcessDateUseNotNull, old, this.processDateUseNotNull);
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
        if (isLoading()) return;
        if (deleteDate != null) {
            if (deleteDate2 == null) setDeleteDate2(this.deleteDate);
            else if (deleteDate.compareTo(deleteDate2) > 0) setDeleteDate2(this.deleteDate);
        }
    } 
    public OADate getDeleteDate2() {
        return deleteDate2;
    }
    public void setDeleteDate2(OADate newValue) {
        OADate old = deleteDate2;
        fireBeforePropertyChange(P_DeleteDate2, old, newValue);
        this.deleteDate2 = newValue;
        firePropertyChange(P_DeleteDate2, old, this.deleteDate2);
        if (deleteDate != null && deleteDate2 != null) {
            if (deleteDate.compareTo(deleteDate2) > 0) setDeleteDate(this.deleteDate2);
        }
    }
    public boolean getDeleteDateUseNull() {
        return deleteDateUseNull;
    }
    public void setDeleteDateUseNull(boolean newValue) {
        boolean old = this.deleteDateUseNull;
        this.deleteDateUseNull = newValue;
        firePropertyChange(P_DeleteDateUseNull, old, this.deleteDateUseNull);
    }
    public boolean getDeleteDateUseNotNull() {
        return deleteDateUseNotNull;
    }
    public void setDeleteDateUseNotNull(boolean newValue) {
        boolean old = this.deleteDateUseNotNull;
        this.deleteDateUseNotNull = newValue;
        firePropertyChange(P_DeleteDateUseNotNull, old, this.deleteDateUseNotNull);
    }

    public String getCustomQuery() {
        return customQuery;
    }
    public void setCustomQuery(String newValue) {
        fireBeforePropertyChange(P_CustomQuery, this.customQuery, newValue);
        String old = customQuery;
        this.customQuery = newValue;
        firePropertyChange(P_CustomQuery, old, this.customQuery);
    }

    public int getMaxResults() {
        return maxResults;
    }
    public void setMaxResults(int newValue) {
        fireBeforePropertyChange(P_MaxResults, this.maxResults, newValue);
        int old = maxResults;
        this.maxResults = newValue;
        firePropertyChange(P_MaxResults, old, this.maxResults);
    }

    public void reset() {
        setId(0);
        setNull(P_Id);
        setId2(0);
        setNull(P_Id2);
        setCreated(null);
        setCreated2(null);
        setItemRuleType((String) null);
        setLine(null);
        setLineUseNull(false);
        setLineUseNotNull(false);
        setProductLine(0);
        setNull(P_ProductLine);
        setProductLineUseNull(false);
        setProductLineUseNotNull(false);
        setSubProductLine(0);
        setNull(P_SubProductLine);
        setSubProductLineUseNull(false);
        setSubProductLineUseNotNull(false);
        setItem(null);
        setItemUseNull(false);
        setItemUseNotNull(false);
        setLocationRuleType((String) null);
        setStore(0);
        setNull(P_Store);
        setStoreUseNull(false);
        setStoreUseNotNull(false);
        setZipcode(null);
        setZipcodeUseNull(false);
        setZipcodeUseNotNull(false);
        setState(null);
        setStateUseNull(false);
        setStateUseNotNull(false);
        setCounty(null);
        setCountyUseNull(false);
        setCountyUseNotNull(false);
        setRuleSearchValue(null);
        setRuleSearchValueUseNull(false);
        setRuleSearchValueUseNotNull(false);
        setFlightRestriction(false);
        setNull(P_FlightRestriction);
        setFlightRestrictionUseNull(false);
        setFlightRestrictionUseNotNull(false);
        setCaustic(false);
        setNull(P_Caustic);
        setCausticUseNull(false);
        setCausticUseNotNull(false);
        setHybridElectric(false);
        setNull(P_HybridElectric);
        setHybridElectricUseNull(false);
        setHybridElectricUseNotNull(false);
        setFreonRestricted(false);
        setNull(P_FreonRestricted);
        setFreonRestrictedUseNull(false);
        setFreonRestrictedUseNotNull(false);
        setRestricted(false);
        setNull(P_Restricted);
        setRestrictedUseNull(false);
        setRestrictedUseNotNull(false);
        setRestrictedEffectiveDate(null);
        setRestrictedEffectiveDate2(null);
        setRestrictedEffectiveDateUseNull(false);
        setRestrictedEffectiveDateUseNotNull(false);
        setProcessDate(null);
        setProcessDate2(null);
        setProcessDateUseNull(false);
        setProcessDateUseNotNull(false);
        setDeleteDate(null);
        setDeleteDate2(null);
        setDeleteDateUseNull(false);
        setDeleteDateUseNotNull(false);
        setCustomQuery(null);
    }

    public boolean isDataEntered() {
        if (!isNull(P_Id)) return true;
        if (getCreated() != null) return true;
        if (getItemRuleType() != null) return true;
        if (getLine() != null) return true;
        if (getLineUseNull()) return true;
        if (getLineUseNotNull()) return true;
        if (!isNull(P_ProductLine)) return true;
        if (getProductLineUseNull()) return true;
        if (getProductLineUseNotNull()) return true;
        if (!isNull(P_SubProductLine)) return true;
        if (getSubProductLineUseNull()) return true;
        if (getSubProductLineUseNotNull()) return true;
        if (getItem() != null) return true;
        if (getItemUseNull()) return true;
        if (getItemUseNotNull()) return true;
        if (getLocationRuleType() != null) return true;
        if (!isNull(P_Store)) return true;
        if (getStoreUseNull()) return true;
        if (getStoreUseNotNull()) return true;
        if (getZipcode() != null) return true;
        if (getZipcodeUseNull()) return true;
        if (getZipcodeUseNotNull()) return true;
        if (getState() != null) return true;
        if (getStateUseNull()) return true;
        if (getStateUseNotNull()) return true;
        if (getCounty() != null) return true;
        if (getCountyUseNull()) return true;
        if (getCountyUseNotNull()) return true;
        if (getRuleSearchValue() != null) return true;
        if (getRuleSearchValueUseNull()) return true;
        if (getRuleSearchValueUseNotNull()) return true;
        if (!isNull(P_FlightRestriction)) return true;
        if (getFlightRestrictionUseNull()) return true;
        if (getFlightRestrictionUseNotNull()) return true;
        if (!isNull(P_Caustic)) return true;
        if (getCausticUseNull()) return true;
        if (getCausticUseNotNull()) return true;
        if (!isNull(P_HybridElectric)) return true;
        if (getHybridElectricUseNull()) return true;
        if (getHybridElectricUseNotNull()) return true;
        if (!isNull(P_FreonRestricted)) return true;
        if (getFreonRestrictedUseNull()) return true;
        if (getFreonRestrictedUseNotNull()) return true;
        if (!isNull(P_Restricted)) return true;
        if (getRestrictedUseNull()) return true;
        if (getRestrictedUseNotNull()) return true;
        if (getRestrictedEffectiveDate() != null) return true;
        if (getRestrictedEffectiveDateUseNull()) return true;
        if (getRestrictedEffectiveDateUseNull()) return true;
        if (getProcessDate() != null) return true;
        if (getProcessDateUseNull()) return true;
        if (getProcessDateUseNull()) return true;
        if (getDeleteDate() != null) return true;
        if (getDeleteDateUseNull()) return true;
        if (getDeleteDateUseNull()) return true;
        if (getCustomQuery() != null) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<ItemRestriction> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<ItemRestriction> f = new OAQueryFilter<ItemRestriction>(ItemRestriction.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<ItemRestriction> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<ItemRestriction> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<ItemRestriction> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        if (!isNull(P_Id)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_Id2) && id != id2) {
                sql += ItemRestriction.P_Id + " >= ?";
                args = OAArray.add(Object.class, args, getId());
                sql += " AND " + ItemRestriction.P_Id + " <= ?";
                args = OAArray.add(Object.class, args, getId2());
            }
            else {
                sql += ItemRestriction.P_Id + " = ?";
                args = OAArray.add(Object.class, args, getId());
            }
        }
        if (created != null) {
            if (sql.length() > 0) sql += " AND ";
            if (created2 != null && !created.equals(created2)) {
                sql += ItemRestriction.P_Created + " >= ?";
                args = OAArray.add(Object.class, args, this.created);
                sql += " AND " + ItemRestriction.P_Created + " <= ?";
                args = OAArray.add(Object.class, args, this.created2);
            }
            else {
                sql += ItemRestriction.P_Created + " = ?";
                args = OAArray.add(Object.class, args, this.created);
            }
        }
        if (OAString.isNotEmpty(this.itemRuleType)) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_ItemRuleType + " = ?";
            args = OAArray.add(Object.class, args, this.itemRuleType);
        }
        if (lineUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += "(" + ItemRestriction.P_Line + " = null OR " + ItemRestriction.P_Line + " == '')";
        }
        else if (lineUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_Line + " != null";
        }
        else if (OAString.isNotEmpty(this.line)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(line);
            if (val.indexOf("%") >= 0) {
                sql += ItemRestriction.P_Line + " LIKE ?";
            }
            else {
                sql += ItemRestriction.P_Line + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (productLineUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_ProductLineCode + " = null";
        }
        else if (productLineUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_ProductLineCode + " != null";
        }
        if (!isNull(P_ProductLine)) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_ProductLineCode + " = ?";
            args = OAArray.add(Object.class, args, this.productLine);
        }
        if (subProductLineUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_ProductLineSubcode + " = null";
        }
        else if (subProductLineUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_ProductLineSubcode + " != null";
        }
        if (!isNull(P_SubProductLine)) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_ProductLineSubcode + " = ?";
            args = OAArray.add(Object.class, args, this.subProductLine);
        }
        if (itemUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += "(" + ItemRestriction.P_Item + " = null OR " + ItemRestriction.P_Item + " == '')";
        }
        else if (itemUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_Item + " != null";
        }
        else if (OAString.isNotEmpty(this.item)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(item);
            if (val.indexOf("%") >= 0) {
                sql += ItemRestriction.P_Item + " LIKE ?";
            }
            else {
                sql += ItemRestriction.P_Item + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (OAString.isNotEmpty(this.locationRuleType)) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_LocationRuleType + " = ?";
            args = OAArray.add(Object.class, args, this.locationRuleType);
        }
        if (storeUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_StoreId + " = null";
        }
        else if (storeUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_StoreId + " != null";
        }
        if (!isNull(P_Store)) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_StoreId + " = ?";
            args = OAArray.add(Object.class, args, this.store);
        }
        if (zipcodeUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += "(" + ItemRestriction.P_Zipcode + " = null OR " + ItemRestriction.P_Zipcode + " == '')";
        }
        else if (zipcodeUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_Zipcode + " != null";
        }
        else if (OAString.isNotEmpty(this.zipcode)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(zipcode);
            if (val.indexOf("%") >= 0) {
                sql += ItemRestriction.P_Zipcode + " LIKE ?";
            }
            else {
                sql += ItemRestriction.P_Zipcode + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (stateUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += "(" + ItemRestriction.P_State + " = null OR " + ItemRestriction.P_State + " == '')";
        }
        else if (stateUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_State + " != null";
        }
        else if (OAString.isNotEmpty(this.state)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(state);
            if (val.indexOf("%") >= 0) {
                sql += ItemRestriction.P_State + " LIKE ?";
            }
            else {
                sql += ItemRestriction.P_State + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (countyUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += "(" + ItemRestriction.P_County + " = null OR " + ItemRestriction.P_County + " == '')";
        }
        else if (countyUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_County + " != null";
        }
        else if (OAString.isNotEmpty(this.county)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(county);
            if (val.indexOf("%") >= 0) {
                sql += ItemRestriction.P_County + " LIKE ?";
            }
            else {
                sql += ItemRestriction.P_County + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (ruleSearchValueUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += "(" + ItemRestriction.P_RuleSearchValue + " = null OR " + ItemRestriction.P_RuleSearchValue + " == '')";
        }
        else if (ruleSearchValueUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_RuleSearchValue + " != null";
        }
        else if (OAString.isNotEmpty(this.ruleSearchValue)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(ruleSearchValue);
            if (val.indexOf("%") >= 0) {
                sql += ItemRestriction.P_RuleSearchValue + " LIKE ?";
            }
            else {
                sql += ItemRestriction.P_RuleSearchValue + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (flightRestrictionUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_FlightRestricted + " = null";
        }
        else if (flightRestrictionUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_FlightRestricted + " != null";
        }
        if (!isNull(P_FlightRestriction)) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_FlightRestricted + " = ?";
            args = OAArray.add(Object.class, args, this.flightRestriction);
        }
        if (causticUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_Caustic + " = null";
        }
        else if (causticUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_Caustic + " != null";
        }
        if (!isNull(P_Caustic)) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_Caustic + " = ?";
            args = OAArray.add(Object.class, args, this.caustic);
        }
        if (hybridElectricUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_HybridElectric + " = null";
        }
        else if (hybridElectricUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_HybridElectric + " != null";
        }
        if (!isNull(P_HybridElectric)) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_HybridElectric + " = ?";
            args = OAArray.add(Object.class, args, this.hybridElectric);
        }
        if (freonRestrictedUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_FreonRestricted + " = null";
        }
        else if (freonRestrictedUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_FreonRestricted + " != null";
        }
        if (!isNull(P_FreonRestricted)) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_FreonRestricted + " = ?";
            args = OAArray.add(Object.class, args, this.freonRestricted);
        }
        if (restrictedUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_SalesRestricted + " = null";
        }
        else if (restrictedUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_SalesRestricted + " != null";
        }
        if (!isNull(P_Restricted)) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_SalesRestricted + " = ?";
            args = OAArray.add(Object.class, args, this.restricted);
        }
        if (restrictedEffectiveDateUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_SalesRestrictedEffectiveDate + " = null";
        }
        else if (restrictedEffectiveDateUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_SalesRestrictedEffectiveDate + " != null";
        }
        else if (restrictedEffectiveDate != null) {
            if (sql.length() > 0) sql += " AND ";
            if (restrictedEffectiveDate2 != null && !restrictedEffectiveDate.equals(restrictedEffectiveDate2)) {
                sql += ItemRestriction.P_SalesRestrictedEffectiveDate + " >= ?";
                args = OAArray.add(Object.class, args, this.restrictedEffectiveDate);
                sql += " AND " + ItemRestriction.P_SalesRestrictedEffectiveDate + " <= ?";
                args = OAArray.add(Object.class, args, this.restrictedEffectiveDate2);
            }
            else {
                sql += ItemRestriction.P_SalesRestrictedEffectiveDate + " = ?";
                args = OAArray.add(Object.class, args, this.restrictedEffectiveDate);
            }
        }
        if (processDateUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_ProcessDate + " = null";
        }
        else if (processDateUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_ProcessDate + " != null";
        }
        else if (processDate != null) {
            if (sql.length() > 0) sql += " AND ";
            if (processDate2 != null && !processDate.equals(processDate2)) {
                sql += ItemRestriction.P_ProcessDate + " >= ?";
                args = OAArray.add(Object.class, args, this.processDate);
                sql += " AND " + ItemRestriction.P_ProcessDate + " <= ?";
                args = OAArray.add(Object.class, args, this.processDate2);
            }
            else {
                sql += ItemRestriction.P_ProcessDate + " = ?";
                args = OAArray.add(Object.class, args, this.processDate);
            }
        }
        if (deleteDateUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_DeleteDate + " = null";
        }
        else if (deleteDateUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemRestriction.P_DeleteDate + " != null";
        }
        else if (deleteDate != null) {
            if (sql.length() > 0) sql += " AND ";
            if (deleteDate2 != null && !deleteDate.equals(deleteDate2)) {
                sql += ItemRestriction.P_DeleteDate + " >= ?";
                args = OAArray.add(Object.class, args, this.deleteDate);
                sql += " AND " + ItemRestriction.P_DeleteDate + " <= ?";
                args = OAArray.add(Object.class, args, this.deleteDate2);
            }
            else {
                sql += ItemRestriction.P_DeleteDate + " = ?";
                args = OAArray.add(Object.class, args, this.deleteDate);
            }
        }
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }

        if (OAString.isNotEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<ItemRestriction> select = new OASelect<ItemRestriction>(ItemRestriction.class, sql, args, sortOrder);
        if (getExtraWhereFilter() != null && getExtraWhereFilter().updateSelect(select)) {
            select.setFilter(new OAAndFilter(this.getCustomFilter(), getExtraWhereFilter()));
        }
        else select.setFilter(this.getCustomFilter());
        select.setDataSourceFilter(this.getDataSourceFilter());
        if (getMaxResults() > 0) select.setMax(getMaxResults());
        return select;
    }

    public void appendSelect(final String fromName, final OASelect select) {
        final String prefix = fromName + ".";
        String sql = "";
        Object[] args = new Object[0];
        if (!isNull(P_Id)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_Id2) && id != id2) {
                sql += prefix + ItemRestriction.P_Id + " >= ?";
                args = OAArray.add(Object.class, args, getId());
                sql += " AND " + prefix + ItemRestriction.P_Id + " <= ?";
                args = OAArray.add(Object.class, args, getId2());
            }
            else {
                sql += prefix + ItemRestriction.P_Id + " = ?";
                args = OAArray.add(Object.class, args, getId());
            }
        }
        if (created != null) {
            if (sql.length() > 0) sql += " AND ";
            if (created2 != null && !created.equals(created2)) {
                sql += prefix + ItemRestriction.P_Created + " >= ?";
                args = OAArray.add(Object.class, args, this.created);
                sql += " AND " + prefix + ItemRestriction.P_Created + " <= ?";
                args = OAArray.add(Object.class, args, this.created2);
            }
            else {
                sql += prefix + ItemRestriction.P_Created + " = ?";
                args = OAArray.add(Object.class, args, this.created);
            }
        }
        if (OAString.isNotEmpty(this.itemRuleType)) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_ItemRuleType + " = ?";
            args = OAArray.add(Object.class, args, this.itemRuleType);
        }
        if (lineUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += "(" + prefix + ItemRestriction.P_Line + " = null OR " + prefix + ItemRestriction.P_Line + " == '')";
        }
        else if (lineUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_Line + " != null";
        }
        else if (OAString.isNotEmpty(this.line)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(line);
            if (val.indexOf("%") >= 0) {
                sql += prefix + ItemRestriction.P_Line + " LIKE ?";
            }
            else {
                sql += prefix + ItemRestriction.P_Line + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (productLineUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_ProductLineCode + " = null";
        }
        else if (productLineUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_ProductLineCode + " != null";
        }
        if (!isNull(P_ProductLine)) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_ProductLineCode + " = ?";
            args = OAArray.add(Object.class, args, this.productLine);
        }
        if (subProductLineUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_ProductLineSubcode + " = null";
        }
        else if (subProductLineUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_ProductLineSubcode + " != null";
        }
        if (!isNull(P_SubProductLine)) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_ProductLineSubcode + " = ?";
            args = OAArray.add(Object.class, args, this.subProductLine);
        }
        if (itemUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += "(" + prefix + ItemRestriction.P_Item + " = null OR " + prefix + ItemRestriction.P_Item + " == '')";
        }
        else if (itemUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_Item + " != null";
        }
        else if (OAString.isNotEmpty(this.item)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(item);
            if (val.indexOf("%") >= 0) {
                sql += prefix + ItemRestriction.P_Item + " LIKE ?";
            }
            else {
                sql += prefix + ItemRestriction.P_Item + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (OAString.isNotEmpty(this.locationRuleType)) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_LocationRuleType + " = ?";
            args = OAArray.add(Object.class, args, this.locationRuleType);
        }
        if (storeUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_StoreId + " = null";
        }
        else if (storeUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_StoreId + " != null";
        }
        if (!isNull(P_Store)) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_StoreId + " = ?";
            args = OAArray.add(Object.class, args, this.store);
        }
        if (zipcodeUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += "(" + prefix + ItemRestriction.P_Zipcode + " = null OR " + prefix + ItemRestriction.P_Zipcode + " == '')";
        }
        else if (zipcodeUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_Zipcode + " != null";
        }
        else if (OAString.isNotEmpty(this.zipcode)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(zipcode);
            if (val.indexOf("%") >= 0) {
                sql += prefix + ItemRestriction.P_Zipcode + " LIKE ?";
            }
            else {
                sql += prefix + ItemRestriction.P_Zipcode + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (stateUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += "(" + prefix + ItemRestriction.P_State + " = null OR " + prefix + ItemRestriction.P_State + " == '')";
        }
        else if (stateUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_State + " != null";
        }
        else if (OAString.isNotEmpty(this.state)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(state);
            if (val.indexOf("%") >= 0) {
                sql += prefix + ItemRestriction.P_State + " LIKE ?";
            }
            else {
                sql += prefix + ItemRestriction.P_State + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (countyUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += "(" + prefix + ItemRestriction.P_County + " = null OR " + prefix + ItemRestriction.P_County + " == '')";
        }
        else if (countyUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_County + " != null";
        }
        else if (OAString.isNotEmpty(this.county)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(county);
            if (val.indexOf("%") >= 0) {
                sql += prefix + ItemRestriction.P_County + " LIKE ?";
            }
            else {
                sql += prefix + ItemRestriction.P_County + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (ruleSearchValueUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += "(" + prefix + ItemRestriction.P_RuleSearchValue + " = null OR " + prefix + ItemRestriction.P_RuleSearchValue + " == '')";
        }
        else if (ruleSearchValueUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_RuleSearchValue + " != null";
        }
        else if (OAString.isNotEmpty(this.ruleSearchValue)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(ruleSearchValue);
            if (val.indexOf("%") >= 0) {
                sql += prefix + ItemRestriction.P_RuleSearchValue + " LIKE ?";
            }
            else {
                sql += prefix + ItemRestriction.P_RuleSearchValue + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (flightRestrictionUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_FlightRestricted + " = null";
        }
        else if (flightRestrictionUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_FlightRestricted + " != null";
        }
        if (!isNull(P_FlightRestriction)) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_FlightRestricted + " = ?";
            args = OAArray.add(Object.class, args, this.flightRestriction);
        }
        if (causticUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_Caustic + " = null";
        }
        else if (causticUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_Caustic + " != null";
        }
        if (!isNull(P_Caustic)) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_Caustic + " = ?";
            args = OAArray.add(Object.class, args, this.caustic);
        }
        if (hybridElectricUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_HybridElectric + " = null";
        }
        else if (hybridElectricUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_HybridElectric + " != null";
        }
        if (!isNull(P_HybridElectric)) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_HybridElectric + " = ?";
            args = OAArray.add(Object.class, args, this.hybridElectric);
        }
        if (freonRestrictedUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_FreonRestricted + " = null";
        }
        else if (freonRestrictedUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_FreonRestricted + " != null";
        }
        if (!isNull(P_FreonRestricted)) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_FreonRestricted + " = ?";
            args = OAArray.add(Object.class, args, this.freonRestricted);
        }
        if (restrictedUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_SalesRestricted + " = null";
        }
        else if (restrictedUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_SalesRestricted + " != null";
        }
        if (!isNull(P_Restricted)) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_SalesRestricted + " = ?";
            args = OAArray.add(Object.class, args, this.restricted);
        }
        if (restrictedEffectiveDateUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_SalesRestrictedEffectiveDate + " = null";
        }
        else if (restrictedEffectiveDateUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_SalesRestrictedEffectiveDate + " != null";
        }
        else if (restrictedEffectiveDate != null) {
            if (sql.length() > 0) sql += " AND ";
            if (restrictedEffectiveDate2 != null && !restrictedEffectiveDate.equals(restrictedEffectiveDate2)) {
                sql += prefix + ItemRestriction.P_SalesRestrictedEffectiveDate + " >= ?";
                args = OAArray.add(Object.class, args, this.restrictedEffectiveDate);
                sql += " AND " + prefix + ItemRestriction.P_SalesRestrictedEffectiveDate + " <= ?";
                args = OAArray.add(Object.class, args, this.restrictedEffectiveDate2);
            }
            else {
                sql += prefix + ItemRestriction.P_SalesRestrictedEffectiveDate + " = ?";
                args = OAArray.add(Object.class, args, this.restrictedEffectiveDate);
            }
        }
        if (processDateUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_ProcessDate + " = null";
        }
        else if (processDateUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_ProcessDate + " != null";
        }
        else if (processDate != null) {
            if (sql.length() > 0) sql += " AND ";
            if (processDate2 != null && !processDate.equals(processDate2)) {
                sql += prefix + ItemRestriction.P_ProcessDate + " >= ?";
                args = OAArray.add(Object.class, args, this.processDate);
                sql += " AND " + prefix + ItemRestriction.P_ProcessDate + " <= ?";
                args = OAArray.add(Object.class, args, this.processDate2);
            }
            else {
                sql += prefix + ItemRestriction.P_ProcessDate + " = ?";
                args = OAArray.add(Object.class, args, this.processDate);
            }
        }
        if (deleteDateUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_DeleteDate + " = null";
        }
        else if (deleteDateUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemRestriction.P_DeleteDate + " != null";
        }
        else if (deleteDate != null) {
            if (sql.length() > 0) sql += " AND ";
            if (deleteDate2 != null && !deleteDate.equals(deleteDate2)) {
                sql += prefix + ItemRestriction.P_DeleteDate + " >= ?";
                args = OAArray.add(Object.class, args, this.deleteDate);
                sql += " AND " + prefix + ItemRestriction.P_DeleteDate + " <= ?";
                args = OAArray.add(Object.class, args, this.deleteDate2);
            }
            else {
                sql += prefix + ItemRestriction.P_DeleteDate + " = ?";
                args = OAArray.add(Object.class, args, this.deleteDate);
            }
        }
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        select.add(sql, args);
    }

    private OAFilter<ItemRestriction> filterDataSourceFilter;
    public OAFilter<ItemRestriction> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<ItemRestriction>() {
            @Override
            public boolean isUsed(ItemRestriction itemRestriction) {
                return ItemRestrictionSearch.this.isUsedForDataSourceFilter(itemRestriction);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<ItemRestriction> filterCustomFilter;
    public OAFilter<ItemRestriction> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<ItemRestriction>() {
            @Override
            public boolean isUsed(ItemRestriction itemRestriction) {
                boolean b = ItemRestrictionSearch.this.isUsedForCustomFilter(itemRestriction);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(ItemRestriction searchItemRestriction) {
        return true;
    }
    public boolean isUsedForCustomFilter(ItemRestriction searchItemRestriction) {
        return true;
    }
}
