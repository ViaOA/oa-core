package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class VertexTaxCodeRatePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public VertexTaxCodeRatePPx(String name) {
        this(null, name);
    }

    public VertexTaxCodeRatePPx(PPxInterface parent, String name) {
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

    public VertexTaxCodePPx calcVertexTaxCode() {
        VertexTaxCodePPx ppx = new VertexTaxCodePPx(this, VertexTaxCodeRate.P_CalcVertexTaxCode);
        return ppx;
    }

    public LineItemTaxPPx lineItemTaxes() {
        LineItemTaxPPx ppx = new LineItemTaxPPx(this, VertexTaxCodeRate.P_LineItemTaxes);
        return ppx;
    }

    public VertexTaxCodePPx vertexTaxCode() {
        VertexTaxCodePPx ppx = new VertexTaxCodePPx(this, VertexTaxCodeRate.P_VertexTaxCode);
        return ppx;
    }

    public String id() {
        return pp + "." + VertexTaxCodeRate.P_Id;
    }

    public String created() {
        return pp + "." + VertexTaxCodeRate.P_Created;
    }

    public String taxPercent() {
        return pp + "." + VertexTaxCodeRate.P_TaxPercent;
    }

    public String decimalPlaces() {
        return pp + "." + VertexTaxCodeRate.P_DecimalPlaces;
    }

    public String beginDate() {
        return pp + "." + VertexTaxCodeRate.P_BeginDate;
    }

    public String endDate() {
        return pp + "." + VertexTaxCodeRate.P_EndDate;
    }

    public String minTaxable() {
        return pp + "." + VertexTaxCodeRate.P_MinTaxable;
    }

    public String maxTaxable() {
        return pp + "." + VertexTaxCodeRate.P_MaxTaxable;
    }

    public String thresholdAmount() {
        return pp + "." + VertexTaxCodeRate.P_ThresholdAmount;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
