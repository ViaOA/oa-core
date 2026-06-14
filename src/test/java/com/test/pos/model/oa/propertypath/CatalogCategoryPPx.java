package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class CatalogCategoryPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public CatalogCategoryPPx(String name) {
        this(null, name);
    }

    public CatalogCategoryPPx(PPxInterface parent, String name) {
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

    public CatalogPPx catalog() {
        CatalogPPx ppx = new CatalogPPx(this, CatalogCategory.P_Catalog);
        return ppx;
    }

    public CatalogCategoryPPx catalogCategories() {
        CatalogCategoryPPx ppx = new CatalogCategoryPPx(this, CatalogCategory.P_CatalogCategories);
        return ppx;
    }

    public CatalogItemPPx catalogItems() {
        CatalogItemPPx ppx = new CatalogItemPPx(this, CatalogCategory.P_CatalogItems);
        return ppx;
    }

    public CatalogCategoryPPx parentCatalogCategory() {
        CatalogCategoryPPx ppx = new CatalogCategoryPPx(this, CatalogCategory.P_ParentCatalogCategory);
        return ppx;
    }

    public String id() {
        return pp + "." + CatalogCategory.P_Id;
    }

    public String created() {
        return pp + "." + CatalogCategory.P_Created;
    }

    public String name() {
        return pp + "." + CatalogCategory.P_Name;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
