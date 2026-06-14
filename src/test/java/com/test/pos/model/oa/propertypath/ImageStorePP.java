package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ImageStorePP {
     

    public static String id() {
        String s = ImageStore.P_Id;
        return s;
    }

    public static String created() {
        String s = ImageStore.P_Created;
        return s;
    }

    public static String bytes() {
        String s = ImageStore.P_Bytes;
        return s;
    }

    public static String origFileName() {
        String s = ImageStore.P_OrigFileName;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
