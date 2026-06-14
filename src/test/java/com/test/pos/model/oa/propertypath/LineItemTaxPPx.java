package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class LineItemTaxPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public LineItemTaxPPx(String name) {
        this(null, name);
    }

    public LineItemTaxPPx(PPxInterface parent, String name) {
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

    public LineItemPPx lineItem() {
        LineItemPPx ppx = new LineItemPPx(this, LineItemTax.P_LineItem);
        return ppx;
    }

    public VertexTaxCodeRatePPx vertexTaxCodeRate() {
        VertexTaxCodeRatePPx ppx = new VertexTaxCodeRatePPx(this, LineItemTax.P_VertexTaxCodeRate);
        return ppx;
    }

    public String id() {
        return pp + "." + LineItemTax.P_Id;
    }

    public String created() {
        return pp + "." + LineItemTax.P_Created;
    }

    public String taxPercent() {
        return pp + "." + LineItemTax.P_TaxPercent;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
