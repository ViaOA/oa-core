package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class StoreDayOpenPP {
    private static StoreSafeLedgerEntryPPx storeSafeLedgerEntries;
    private static StoreSchedulePPx storeSchedule;
     

    public static StoreSafeLedgerEntryPPx storeSafeLedgerEntries() {
        if (storeSafeLedgerEntries == null) storeSafeLedgerEntries = new StoreSafeLedgerEntryPPx(StoreDayOpen.P_StoreSafeLedgerEntries);
        return storeSafeLedgerEntries;
    }

    public static StoreSchedulePPx storeSchedule() {
        if (storeSchedule == null) storeSchedule = new StoreSchedulePPx(StoreDayOpen.P_StoreSchedule);
        return storeSchedule;
    }

    public static String id() {
        String s = StoreDayOpen.P_Id;
        return s;
    }

    public static String created() {
        String s = StoreDayOpen.P_Created;
        return s;
    }

    public static String createStoreSafeAudit() {
        String s = "createStoreSafeAudit";
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
