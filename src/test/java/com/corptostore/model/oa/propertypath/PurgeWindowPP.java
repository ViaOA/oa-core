// Generated by OABuilder
package com.corptostore.model.oa.propertypath;
 
import com.corptostore.model.oa.*;
import com.corptostore.model.oa.PurgeWindow;
import com.corptostore.model.oa.propertypath.CorpToStorePPx;
import com.corptostore.model.oa.propertypath.StatusInfoPPx;
import com.corptostore.model.oa.propertypath.StorePurgeInfoPPx;
 
public class PurgeWindowPP {
    private static CorpToStorePPx corpToStore;
    private static StatusInfoPPx statusInfo;
    private static StorePurgeInfoPPx storePurgeInfos;
     

    public static CorpToStorePPx corpToStore() {
        if (corpToStore == null) corpToStore = new CorpToStorePPx(PurgeWindow.P_CorpToStore);
        return corpToStore;
    }

    public static StatusInfoPPx statusInfo() {
        if (statusInfo == null) statusInfo = new StatusInfoPPx(PurgeWindow.P_StatusInfo);
        return statusInfo;
    }

    public static StorePurgeInfoPPx storePurgeInfos() {
        if (storePurgeInfos == null) storePurgeInfos = new StorePurgeInfoPPx(PurgeWindow.P_StorePurgeInfos);
        return storePurgeInfos;
    }

    public static String id() {
        String s = PurgeWindow.P_Id;
        return s;
    }

    public static String created() {
        String s = PurgeWindow.P_Created;
        return s;
    }

    public static String timeoutMinutes() {
        String s = PurgeWindow.P_TimeoutMinutes;
        return s;
    }

    public static String storeLimit() {
        String s = PurgeWindow.P_StoreLimit;
        return s;
    }

    public static String finished() {
        String s = PurgeWindow.P_Finished;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 