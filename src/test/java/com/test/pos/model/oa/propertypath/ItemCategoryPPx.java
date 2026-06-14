package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class ItemCategoryPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ItemCategoryPPx(String name) {
        this(null, name);
    }

    public ItemCategoryPPx(PPxInterface parent, String name) {
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

    public ItemPPx items() {
        ItemPPx ppx = new ItemPPx(this, ItemCategory.P_Items);
        return ppx;
    }

    public ItemCategoryPPx parentItemCategory() {
        ItemCategoryPPx ppx = new ItemCategoryPPx(this, ItemCategory.P_ParentItemCategory);
        return ppx;
    }

    public ItemCategoryPPx subItemCategories() {
        ItemCategoryPPx ppx = new ItemCategoryPPx(this, ItemCategory.P_SubItemCategories);
        return ppx;
    }

    public VertexTaxCodePPx vertexTaxCode() {
        VertexTaxCodePPx ppx = new VertexTaxCodePPx(this, ItemCategory.P_VertexTaxCode);
        return ppx;
    }

    public String id() {
        return pp + "." + ItemCategory.P_Id;
    }

    public String created() {
        return pp + "." + ItemCategory.P_Created;
    }

    public String code() {
        return pp + "." + ItemCategory.P_Code;
    }

    public String name() {
        return pp + "." + ItemCategory.P_Name;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
