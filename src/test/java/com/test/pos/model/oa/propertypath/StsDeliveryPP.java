package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class StsDeliveryPP {
    private static DeliveryServicePPx deliveryService;
    private static StoreToStoreTransferPPx storeToStoreTransfer;
    private static StsdItemPPx stsdItems;
     

    public static DeliveryServicePPx deliveryService() {
        if (deliveryService == null) deliveryService = new DeliveryServicePPx(StsDelivery.P_DeliveryService);
        return deliveryService;
    }

    public static StoreToStoreTransferPPx storeToStoreTransfer() {
        if (storeToStoreTransfer == null) storeToStoreTransfer = new StoreToStoreTransferPPx(StsDelivery.P_StoreToStoreTransfer);
        return storeToStoreTransfer;
    }

    public static StsdItemPPx stsdItems() {
        if (stsdItems == null) stsdItems = new StsdItemPPx(StsDelivery.P_StsdItems);
        return stsdItems;
    }

    public static String id() {
        String s = StsDelivery.P_Id;
        return s;
    }

    public static String created() {
        String s = StsDelivery.P_Created;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
