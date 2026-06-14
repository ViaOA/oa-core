package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class StorePP {
    private static AddressPPx address;
    private static LedgerDenominationBundlePPx calcLedgerDenominationBundles;
    private static StoreSafeLedgerEntryPPx calcStoreSafeLedgerEntries;
    private static CurrencyTypePPx currencyType;
    private static ManualPurchaseOrderPPx manualPurchaseOrders;
    private static RegisterPPx registers;
    private static StoreClosedDatePPx storeClosedDates;
    private static StoreHoursOpenPPx storeHoursOpens;
    private static StoreSafePPx storeSafe;
    private static StoreSchedulePPx storeSchedules;
    private static StoreToStoreTransferPPx storeToStoreTransfers;
    private static TeamMemberPPx teamMembers;
    private static TillPPx tills;
     

    public static AddressPPx address() {
        if (address == null) address = new AddressPPx(Store.P_Address);
        return address;
    }

    public static LedgerDenominationBundlePPx calcLedgerDenominationBundles() {
        if (calcLedgerDenominationBundles == null) calcLedgerDenominationBundles = new LedgerDenominationBundlePPx(Store.P_CalcLedgerDenominationBundles);
        return calcLedgerDenominationBundles;
    }

    public static StoreSafeLedgerEntryPPx calcStoreSafeLedgerEntries() {
        if (calcStoreSafeLedgerEntries == null) calcStoreSafeLedgerEntries = new StoreSafeLedgerEntryPPx(Store.P_CalcStoreSafeLedgerEntries);
        return calcStoreSafeLedgerEntries;
    }

    public static CurrencyTypePPx currencyType() {
        if (currencyType == null) currencyType = new CurrencyTypePPx(Store.P_CurrencyType);
        return currencyType;
    }

    public static ManualPurchaseOrderPPx manualPurchaseOrders() {
        if (manualPurchaseOrders == null) manualPurchaseOrders = new ManualPurchaseOrderPPx(Store.P_ManualPurchaseOrders);
        return manualPurchaseOrders;
    }

    public static RegisterPPx registers() {
        if (registers == null) registers = new RegisterPPx(Store.P_Registers);
        return registers;
    }

    public static StoreClosedDatePPx storeClosedDates() {
        if (storeClosedDates == null) storeClosedDates = new StoreClosedDatePPx(Store.P_StoreClosedDates);
        return storeClosedDates;
    }

    public static StoreHoursOpenPPx storeHoursOpens() {
        if (storeHoursOpens == null) storeHoursOpens = new StoreHoursOpenPPx(Store.P_StoreHoursOpens);
        return storeHoursOpens;
    }

    public static StoreSafePPx storeSafe() {
        if (storeSafe == null) storeSafe = new StoreSafePPx(Store.P_StoreSafe);
        return storeSafe;
    }

    public static StoreSchedulePPx storeSchedules() {
        if (storeSchedules == null) storeSchedules = new StoreSchedulePPx(Store.P_StoreSchedules);
        return storeSchedules;
    }

    public static StoreToStoreTransferPPx storeToStoreTransfers() {
        if (storeToStoreTransfers == null) storeToStoreTransfers = new StoreToStoreTransferPPx(Store.P_StoreToStoreTransfers);
        return storeToStoreTransfers;
    }

    public static TeamMemberPPx teamMembers() {
        if (teamMembers == null) teamMembers = new TeamMemberPPx(Store.P_TeamMembers);
        return teamMembers;
    }

    public static TillPPx tills() {
        if (tills == null) tills = new TillPPx(Store.P_Tills);
        return tills;
    }

    public static String id() {
        String s = Store.P_Id;
        return s;
    }

    public static String created() {
        String s = Store.P_Created;
        return s;
    }

    public static String storeNumber() {
        String s = Store.P_StoreNumber;
        return s;
    }

    public static String name() {
        String s = Store.P_Name;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
