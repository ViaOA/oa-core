package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class CatalogItemPP {
    private static ItemPPx item;
    private static CatalogCategoryPPx rootCatalogCategories;
    private static VehicleModelPackagePPx vehicleModelPackages;
    private static VehicleModelPPx vehicleModels;
     

    public static ItemPPx item() {
        if (item == null) item = new ItemPPx(CatalogItem.P_Item);
        return item;
    }

    public static CatalogCategoryPPx rootCatalogCategories() {
        if (rootCatalogCategories == null) rootCatalogCategories = new CatalogCategoryPPx(CatalogItem.P_RootCatalogCategories);
        return rootCatalogCategories;
    }

    public static VehicleModelPackagePPx vehicleModelPackages() {
        if (vehicleModelPackages == null) vehicleModelPackages = new VehicleModelPackagePPx(CatalogItem.P_VehicleModelPackages);
        return vehicleModelPackages;
    }

    public static VehicleModelPPx vehicleModels() {
        if (vehicleModels == null) vehicleModels = new VehicleModelPPx(CatalogItem.P_VehicleModels);
        return vehicleModels;
    }

    public static String id() {
        String s = CatalogItem.P_Id;
        return s;
    }

    public static String created() {
        String s = CatalogItem.P_Created;
        return s;
    }

    public static String name() {
        String s = CatalogItem.P_Name;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
