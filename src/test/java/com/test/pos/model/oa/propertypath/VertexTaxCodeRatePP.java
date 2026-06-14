package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class VertexTaxCodeRatePP {
    private static VertexTaxCodePPx calcVertexTaxCode;
    private static LineItemTaxPPx lineItemTaxes;
    private static VertexTaxCodePPx vertexTaxCode;
     

    public static VertexTaxCodePPx calcVertexTaxCode() {
        if (calcVertexTaxCode == null) calcVertexTaxCode = new VertexTaxCodePPx(VertexTaxCodeRate.P_CalcVertexTaxCode);
        return calcVertexTaxCode;
    }

    public static LineItemTaxPPx lineItemTaxes() {
        if (lineItemTaxes == null) lineItemTaxes = new LineItemTaxPPx(VertexTaxCodeRate.P_LineItemTaxes);
        return lineItemTaxes;
    }

    public static VertexTaxCodePPx vertexTaxCode() {
        if (vertexTaxCode == null) vertexTaxCode = new VertexTaxCodePPx(VertexTaxCodeRate.P_VertexTaxCode);
        return vertexTaxCode;
    }

    public static String id() {
        String s = VertexTaxCodeRate.P_Id;
        return s;
    }

    public static String created() {
        String s = VertexTaxCodeRate.P_Created;
        return s;
    }

    public static String taxPercent() {
        String s = VertexTaxCodeRate.P_TaxPercent;
        return s;
    }

    public static String decimalPlaces() {
        String s = VertexTaxCodeRate.P_DecimalPlaces;
        return s;
    }

    public static String beginDate() {
        String s = VertexTaxCodeRate.P_BeginDate;
        return s;
    }

    public static String endDate() {
        String s = VertexTaxCodeRate.P_EndDate;
        return s;
    }

    public static String minTaxable() {
        String s = VertexTaxCodeRate.P_MinTaxable;
        return s;
    }

    public static String maxTaxable() {
        String s = VertexTaxCodeRate.P_MaxTaxable;
        return s;
    }

    public static String thresholdAmount() {
        String s = VertexTaxCodeRate.P_ThresholdAmount;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
