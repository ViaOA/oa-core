package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class StoreToStoreTransferPP {
    private static StsDeliveryPPx stsDeliveries;
    private static StsItemPPx stsItems;
    private static StorePPx toStore;
     

    public static StsDeliveryPPx stsDeliveries() {
        if (stsDeliveries == null) stsDeliveries = new StsDeliveryPPx(StoreToStoreTransfer.P_StsDeliveries);
        return stsDeliveries;
    }

    public static StsItemPPx stsItems() {
        if (stsItems == null) stsItems = new StsItemPPx(StoreToStoreTransfer.P_StsItems);
        return stsItems;
    }

    public static StorePPx toStore() {
        if (toStore == null) toStore = new StorePPx(StoreToStoreTransfer.P_ToStore);
        return toStore;
    }

    public static String id() {
        String s = StoreToStoreTransfer.P_Id;
        return s;
    }

    public static String created() {
        String s = StoreToStoreTransfer.P_Created;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
