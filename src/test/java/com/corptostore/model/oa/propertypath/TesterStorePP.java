// Generated by OABuilder
package com.corptostore.model.oa.propertypath;
 
import com.corptostore.model.oa.*;
import com.corptostore.model.oa.TesterStore;
import com.corptostore.model.oa.propertypath.StorePPx;
import com.corptostore.model.oa.propertypath.StoreTransmitInfoPPx;
import com.corptostore.model.oa.propertypath.TesterPPx;
 
public class TesterStorePP {
    private static StoreTransmitInfoPPx calcStoreTransmitInfo;
    private static StorePPx store;
    private static TesterPPx tester;
     

    public static StoreTransmitInfoPPx calcStoreTransmitInfo() {
        if (calcStoreTransmitInfo == null) calcStoreTransmitInfo = new StoreTransmitInfoPPx(TesterStore.P_CalcStoreTransmitInfo);
        return calcStoreTransmitInfo;
    }

    public static StorePPx store() {
        if (store == null) store = new StorePPx(TesterStore.P_Store);
        return store;
    }

    public static TesterPPx tester() {
        if (tester == null) tester = new TesterPPx(TesterStore.P_Tester);
        return tester;
    }

    public static String id() {
        String s = TesterStore.P_Id;
        return s;
    }

    public static String created() {
        String s = TesterStore.P_Created;
        return s;
    }

    public static String wasActive() {
        String s = TesterStore.P_WasActive;
        return s;
    }

    public static String holdRegisteredDate() {
        String s = TesterStore.P_HoldRegisteredDate;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
