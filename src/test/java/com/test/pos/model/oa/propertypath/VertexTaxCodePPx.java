package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class VertexTaxCodePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public VertexTaxCodePPx(String name) {
        this(null, name);
    }

    public VertexTaxCodePPx(PPxInterface parent, String name) {
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

    public VertexTaxCodeRatePPx currentVertexTaxCodeRate() {
        VertexTaxCodeRatePPx ppx = new VertexTaxCodeRatePPx(this, VertexTaxCode.P_CurrentVertexTaxCodeRate);
        return ppx;
    }

    public ItemPPx items() {
        ItemPPx ppx = new ItemPPx(this, VertexTaxCode.P_Items);
        return ppx;
    }

    public ItemCategoryPPx rootItemCategories() {
        ItemCategoryPPx ppx = new ItemCategoryPPx(this, VertexTaxCode.P_RootItemCategories);
        return ppx;
    }

    public VertexTaxCodeRatePPx vertexTaxCodeRates() {
        VertexTaxCodeRatePPx ppx = new VertexTaxCodeRatePPx(this, VertexTaxCode.P_VertexTaxCodeRates);
        return ppx;
    }

    public String id() {
        return pp + "." + VertexTaxCode.P_Id;
    }

    public String created() {
        return pp + "." + VertexTaxCode.P_Created;
    }

    public String taxCode() {
        return pp + "." + VertexTaxCode.P_TaxCode;
    }

    public String taxAuthority() {
        return pp + "." + VertexTaxCode.P_TaxAuthority;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
