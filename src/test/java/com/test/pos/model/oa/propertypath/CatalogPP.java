package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class CatalogPP {
    private static CatalogCategoryPPx catalogCategories;
     

    public static CatalogCategoryPPx catalogCategories() {
        if (catalogCategories == null) catalogCategories = new CatalogCategoryPPx(Catalog.P_CatalogCategories);
        return catalogCategories;
    }

    public static String id() {
        String s = Catalog.P_Id;
        return s;
    }

    public static String created() {
        String s = Catalog.P_Created;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
