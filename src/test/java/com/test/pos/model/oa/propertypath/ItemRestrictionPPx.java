package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class ItemRestrictionPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ItemRestrictionPPx(String name) {
        this(null, name);
    }

    public ItemRestrictionPPx(PPxInterface parent, String name) {
        String s = null;
        if (parent != null) {
            s = parent.toString();
        }
        if (s == null) s = "";
        if (name != null && name.length() > 0) {
            if (s.length() > 0 && name.charAt(0) != ':') s += ".";
            s += name;
        }
        pp = s;
    }

    public String id() {
        return pp + "." + ItemRestriction.P_Id;
    }

    public String created() {
        return pp + "." + ItemRestriction.P_Created;
    }

    public String itemRuleType() {
        return pp + "." + ItemRestriction.P_ItemRuleType;
    }

    public String line() {
        return pp + "." + ItemRestriction.P_Line;
    }

    public String productLineCode() {
        return pp + "." + ItemRestriction.P_ProductLineCode;
    }

    public String productLineSubcode() {
        return pp + "." + ItemRestriction.P_ProductLineSubcode;
    }

    public String item() {
        return pp + "." + ItemRestriction.P_Item;
    }

    public String locationRuleType() {
        return pp + "." + ItemRestriction.P_LocationRuleType;
    }

    public String storeId() {
        return pp + "." + ItemRestriction.P_StoreId;
    }

    public String zipcode() {
        return pp + "." + ItemRestriction.P_Zipcode;
    }

    public String state() {
        return pp + "." + ItemRestriction.P_State;
    }

    public String county() {
        return pp + "." + ItemRestriction.P_County;
    }

    public String ruleSearchValue() {
        return pp + "." + ItemRestriction.P_RuleSearchValue;
    }

    public String flightRestricted() {
        return pp + "." + ItemRestriction.P_FlightRestricted;
    }

    public String caustic() {
        return pp + "." + ItemRestriction.P_Caustic;
    }

    public String hybridElectric() {
        return pp + "." + ItemRestriction.P_HybridElectric;
    }

    public String freonRestricted() {
        return pp + "." + ItemRestriction.P_FreonRestricted;
    }

    public String salesRestricted() {
        return pp + "." + ItemRestriction.P_SalesRestricted;
    }

    public String salesRestrictedEffectiveDate() {
        return pp + "." + ItemRestriction.P_SalesRestrictedEffectiveDate;
    }

    public String processDate() {
        return pp + "." + ItemRestriction.P_ProcessDate;
    }

    public String deleteDate() {
        return pp + "." + ItemRestriction.P_DeleteDate;
    }

    public String verifyRuleSearchValue() {
        return pp + "." + ItemRestriction.P_VerifyRuleSearchValue;
    }

    public String ruleSearchValueDescription() {
        return pp + "." + ItemRestriction.P_RuleSearchValueDescription;
    }

    public String usesLine() {
        return pp + "." + ItemRestriction.P_UsesLine;
    }

    public String usesProductCode() {
        return pp + "." + ItemRestriction.P_UsesProductCode;
    }

    public String usesProductLineSubcode() {
        return pp + "." + ItemRestriction.P_UsesProductLineSubcode;
    }

    public String usesItem() {
        return pp + "." + ItemRestriction.P_UsesItem;
    }

    public String usesStoreId() {
        return pp + "." + ItemRestriction.P_UsesStoreId;
    }

    public String usesZipcode() {
        return pp + "." + ItemRestriction.P_UsesZipcode;
    }

    public String usesState() {
        return pp + "." + ItemRestriction.P_UsesState;
    }

    public String usesCounty() {
        return pp + "." + ItemRestriction.P_UsesCounty;
    }

    public String usesFlightRestricted() {
        return pp + "." + ItemRestriction.P_UsesFlightRestricted;
    }

    public String usesCaustic() {
        return pp + "." + ItemRestriction.P_UsesCaustic;
    }

    public String usesHybridElectric() {
        return pp + "." + ItemRestriction.P_UsesHybridElectric;
    }

    public String usesFreonRestricted() {
        return pp + "." + ItemRestriction.P_UsesFreonRestricted;
    }

    public String usesRestricted() {
        return pp + "." + ItemRestriction.P_UsesRestricted;
    }

    public String reassignRuleSearchValue() {
        return pp + ".reassignRuleSearchValue";
    }

    public ItemRestrictionPPx invalidRuleSearchValueFilter() {
        ItemRestrictionPPx ppx = new ItemRestrictionPPx(this, ":invalidRuleSearchValue()");
        return ppx;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
