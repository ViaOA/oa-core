package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class LineItemTaxPP {
    private static LineItemPPx lineItem;
    private static VertexTaxCodeRatePPx vertexTaxCodeRate;
     

    public static LineItemPPx lineItem() {
        if (lineItem == null) lineItem = new LineItemPPx(LineItemTax.P_LineItem);
        return lineItem;
    }

    public static VertexTaxCodeRatePPx vertexTaxCodeRate() {
        if (vertexTaxCodeRate == null) vertexTaxCodeRate = new VertexTaxCodeRatePPx(LineItemTax.P_VertexTaxCodeRate);
        return vertexTaxCodeRate;
    }

    public static String id() {
        String s = LineItemTax.P_Id;
        return s;
    }

    public static String created() {
        String s = LineItemTax.P_Created;
        return s;
    }

    public static String taxPercent() {
        String s = LineItemTax.P_TaxPercent;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
