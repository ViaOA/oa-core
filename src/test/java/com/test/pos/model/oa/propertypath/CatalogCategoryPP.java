package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class CatalogCategoryPP {
    private static CatalogPPx catalog;
    private static CatalogCategoryPPx catalogCategories;
    private static CatalogItemPPx catalogItems;
    private static CatalogCategoryPPx parentCatalogCategory;
     

    public static CatalogPPx catalog() {
        if (catalog == null) catalog = new CatalogPPx(CatalogCategory.P_Catalog);
        return catalog;
    }

    public static CatalogCategoryPPx catalogCategories() {
        if (catalogCategories == null) catalogCategories = new CatalogCategoryPPx(CatalogCategory.P_CatalogCategories);
        return catalogCategories;
    }

    public static CatalogItemPPx catalogItems() {
        if (catalogItems == null) catalogItems = new CatalogItemPPx(CatalogCategory.P_CatalogItems);
        return catalogItems;
    }

    public static CatalogCategoryPPx parentCatalogCategory() {
        if (parentCatalogCategory == null) parentCatalogCategory = new CatalogCategoryPPx(CatalogCategory.P_ParentCatalogCategory);
        return parentCatalogCategory;
    }

    public static String id() {
        String s = CatalogCategory.P_Id;
        return s;
    }

    public static String created() {
        String s = CatalogCategory.P_Created;
        return s;
    }

    public static String name() {
        String s = CatalogCategory.P_Name;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
