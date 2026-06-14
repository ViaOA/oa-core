package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class BarcodeTypePP {
    private static ProductUpcPPx productUpcs;
     

    public static ProductUpcPPx productUpcs() {
        if (productUpcs == null) productUpcs = new ProductUpcPPx(BarcodeType.P_ProductUpcs);
        return productUpcs;
    }

    public static String id() {
        String s = BarcodeType.P_Id;
        return s;
    }

    public static String created() {
        String s = BarcodeType.P_Created;
        return s;
    }

    public static String name() {
        String s = BarcodeType.P_Name;
        return s;
    }

    public static String type() {
        String s = BarcodeType.P_Type;
        return s;
    }

    public static String rule() {
        String s = BarcodeType.P_Rule;
        return s;
    }

    public static String convertUpc() {
        String s = "convertUpc";
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
