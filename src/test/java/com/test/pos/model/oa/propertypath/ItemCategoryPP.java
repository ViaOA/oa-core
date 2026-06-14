package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ItemCategoryPP {
    private static ItemPPx items;
    private static ItemCategoryPPx parentItemCategory;
    private static ItemCategoryPPx subItemCategories;
    private static VertexTaxCodePPx vertexTaxCode;
     

    public static ItemPPx items() {
        if (items == null) items = new ItemPPx(ItemCategory.P_Items);
        return items;
    }

    public static ItemCategoryPPx parentItemCategory() {
        if (parentItemCategory == null) parentItemCategory = new ItemCategoryPPx(ItemCategory.P_ParentItemCategory);
        return parentItemCategory;
    }

    public static ItemCategoryPPx subItemCategories() {
        if (subItemCategories == null) subItemCategories = new ItemCategoryPPx(ItemCategory.P_SubItemCategories);
        return subItemCategories;
    }

    public static VertexTaxCodePPx vertexTaxCode() {
        if (vertexTaxCode == null) vertexTaxCode = new VertexTaxCodePPx(ItemCategory.P_VertexTaxCode);
        return vertexTaxCode;
    }

    public static String id() {
        String s = ItemCategory.P_Id;
        return s;
    }

    public static String created() {
        String s = ItemCategory.P_Created;
        return s;
    }

    public static String code() {
        String s = ItemCategory.P_Code;
        return s;
    }

    public static String name() {
        String s = ItemCategory.P_Name;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
