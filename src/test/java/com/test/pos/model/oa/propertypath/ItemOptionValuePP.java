package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ItemOptionValuePP {
    private static ItemOptionPPx itemOption;
    private static ItemVariantPPx itemVariants;
    private static PriceBookEntryPPx priceBookEntries;
     

    public static ItemOptionPPx itemOption() {
        if (itemOption == null) itemOption = new ItemOptionPPx(ItemOptionValue.P_ItemOption);
        return itemOption;
    }

    public static ItemVariantPPx itemVariants() {
        if (itemVariants == null) itemVariants = new ItemVariantPPx(ItemOptionValue.P_ItemVariants);
        return itemVariants;
    }

    public static PriceBookEntryPPx priceBookEntries() {
        if (priceBookEntries == null) priceBookEntries = new PriceBookEntryPPx(ItemOptionValue.P_PriceBookEntries);
        return priceBookEntries;
    }

    public static String id() {
        String s = ItemOptionValue.P_Id;
        return s;
    }

    public static String created() {
        String s = ItemOptionValue.P_Created;
        return s;
    }

    public static String value() {
        String s = ItemOptionValue.P_Value;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
