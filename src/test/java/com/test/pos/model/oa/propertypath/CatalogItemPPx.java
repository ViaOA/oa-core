package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class CatalogItemPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public CatalogItemPPx(String name) {
        this(null, name);
    }

    public CatalogItemPPx(PPxInterface parent, String name) {
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

    public ItemPPx item() {
        ItemPPx ppx = new ItemPPx(this, CatalogItem.P_Item);
        return ppx;
    }

    public CatalogCategoryPPx rootCatalogCategories() {
        CatalogCategoryPPx ppx = new CatalogCategoryPPx(this, CatalogItem.P_RootCatalogCategories);
        return ppx;
    }

    public VehicleModelPackagePPx vehicleModelPackages() {
        VehicleModelPackagePPx ppx = new VehicleModelPackagePPx(this, CatalogItem.P_VehicleModelPackages);
        return ppx;
    }

    public VehicleModelPPx vehicleModels() {
        VehicleModelPPx ppx = new VehicleModelPPx(this, CatalogItem.P_VehicleModels);
        return ppx;
    }

    public String id() {
        return pp + "." + CatalogItem.P_Id;
    }

    public String created() {
        return pp + "." + CatalogItem.P_Created;
    }

    public String name() {
        return pp + "." + CatalogItem.P_Name;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
