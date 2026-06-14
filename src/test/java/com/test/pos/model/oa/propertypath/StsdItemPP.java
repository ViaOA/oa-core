package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class StsdItemPP {
    private static StsDeliveryPPx stsDelivery;
    private static StsdItemEachPPx stsdItemEaches;
    private static StsItemPPx stsItem;
     

    public static StsDeliveryPPx stsDelivery() {
        if (stsDelivery == null) stsDelivery = new StsDeliveryPPx(StsdItem.P_StsDelivery);
        return stsDelivery;
    }

    public static StsdItemEachPPx stsdItemEaches() {
        if (stsdItemEaches == null) stsdItemEaches = new StsdItemEachPPx(StsdItem.P_StsdItemEaches);
        return stsdItemEaches;
    }

    public static StsItemPPx stsItem() {
        if (stsItem == null) stsItem = new StsItemPPx(StsdItem.P_StsItem);
        return stsItem;
    }

    public static String id() {
        String s = StsdItem.P_Id;
        return s;
    }

    public static String created() {
        String s = StsdItem.P_Created;
        return s;
    }

    public static String quantity() {
        String s = StsdItem.P_Quantity;
        return s;
    }

    public static String received() {
        String s = StsdItem.P_Received;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
