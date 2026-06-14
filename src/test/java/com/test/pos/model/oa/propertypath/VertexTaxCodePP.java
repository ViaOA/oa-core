package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class VertexTaxCodePP {
    private static VertexTaxCodeRatePPx currentVertexTaxCodeRate;
    private static ItemPPx items;
    private static ItemCategoryPPx rootItemCategories;
    private static VertexTaxCodeRatePPx vertexTaxCodeRates;
     

    public static VertexTaxCodeRatePPx currentVertexTaxCodeRate() {
        if (currentVertexTaxCodeRate == null) currentVertexTaxCodeRate = new VertexTaxCodeRatePPx(VertexTaxCode.P_CurrentVertexTaxCodeRate);
        return currentVertexTaxCodeRate;
    }

    public static ItemPPx items() {
        if (items == null) items = new ItemPPx(VertexTaxCode.P_Items);
        return items;
    }

    public static ItemCategoryPPx rootItemCategories() {
        if (rootItemCategories == null) rootItemCategories = new ItemCategoryPPx(VertexTaxCode.P_RootItemCategories);
        return rootItemCategories;
    }

    public static VertexTaxCodeRatePPx vertexTaxCodeRates() {
        if (vertexTaxCodeRates == null) vertexTaxCodeRates = new VertexTaxCodeRatePPx(VertexTaxCode.P_VertexTaxCodeRates);
        return vertexTaxCodeRates;
    }

    public static String id() {
        String s = VertexTaxCode.P_Id;
        return s;
    }

    public static String created() {
        String s = VertexTaxCode.P_Created;
        return s;
    }

    public static String taxCode() {
        String s = VertexTaxCode.P_TaxCode;
        return s;
    }

    public static String taxAuthority() {
        String s = VertexTaxCode.P_TaxAuthority;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
