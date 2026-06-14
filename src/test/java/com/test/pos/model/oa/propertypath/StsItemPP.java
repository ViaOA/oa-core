package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class StsItemPP {
    private static ItemPPx item;
    private static StoreToStoreTransferPPx storeToStoreTransfer;
    private static StsdItemPPx stsdItems;
     

    public static ItemPPx item() {
        if (item == null) item = new ItemPPx(StsItem.P_Item);
        return item;
    }

    public static StoreToStoreTransferPPx storeToStoreTransfer() {
        if (storeToStoreTransfer == null) storeToStoreTransfer = new StoreToStoreTransferPPx(StsItem.P_StoreToStoreTransfer);
        return storeToStoreTransfer;
    }

    public static StsdItemPPx stsdItems() {
        if (stsdItems == null) stsdItems = new StsdItemPPx(StsItem.P_StsdItems);
        return stsdItems;
    }

    public static String id() {
        String s = StsItem.P_Id;
        return s;
    }

    public static String created() {
        String s = StsItem.P_Created;
        return s;
    }

    public static String quantity() {
        String s = StsItem.P_Quantity;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
