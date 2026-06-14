package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ProductSerialCodePP {
    private static ProductPPx product;
     

    public static ProductPPx product() {
        if (product == null) product = new ProductPPx(ProductSerialCode.P_Product);
        return product;
    }

    public static String id() {
        String s = ProductSerialCode.P_Id;
        return s;
    }

    public static String created() {
        String s = ProductSerialCode.P_Created;
        return s;
    }

    public static String receivedDate() {
        String s = ProductSerialCode.P_ReceivedDate;
        return s;
    }

    public static String serialCode() {
        String s = ProductSerialCode.P_SerialCode;
        return s;
    }

    public static String soldDate() {
        String s = ProductSerialCode.P_SoldDate;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
