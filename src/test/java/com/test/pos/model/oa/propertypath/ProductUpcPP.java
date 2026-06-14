package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ProductUpcPP {
    private static BarcodeTypePPx barcodeType;
    private static ProductPPx product;
     

    public static BarcodeTypePPx barcodeType() {
        if (barcodeType == null) barcodeType = new BarcodeTypePPx(ProductUpc.P_BarcodeType);
        return barcodeType;
    }

    public static ProductPPx product() {
        if (product == null) product = new ProductPPx(ProductUpc.P_Product);
        return product;
    }

    public static String id() {
        String s = ProductUpc.P_Id;
        return s;
    }

    public static String created() {
        String s = ProductUpc.P_Created;
        return s;
    }

    public static String upc() {
        String s = ProductUpc.P_UPC;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
