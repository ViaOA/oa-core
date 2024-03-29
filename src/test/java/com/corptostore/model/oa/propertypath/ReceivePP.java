// Generated by OABuilder
package com.corptostore.model.oa.propertypath;
 
import com.corptostore.model.oa.*;
import com.corptostore.model.oa.Receive;
import com.corptostore.model.oa.propertypath.BatchPPx;
import com.corptostore.model.oa.propertypath.StorePPx;
 
public class ReceivePP {
    private static BatchPPx batch;
    private static StorePPx store;
     

    public static BatchPPx batch() {
        if (batch == null) batch = new BatchPPx(Receive.P_Batch);
        return batch;
    }

    public static StorePPx store() {
        if (store == null) store = new StorePPx(Receive.P_Store);
        return store;
    }

    public static String receiveId() {
        String s = Receive.P_ReceiveId;
        return s;
    }

    public static String created() {
        String s = Receive.P_Created;
        return s;
    }

    public static String processed() {
        String s = Receive.P_Processed;
        return s;
    }

    public static String ifsSeqNumber() {
        String s = Receive.P_IfsSeqNumber;
        return s;
    }

    public static String messageType() {
        String s = Receive.P_MessageType;
        return s;
    }

    public static String messageName() {
        String s = Receive.P_MessageName;
        return s;
    }

    public static String messageData() {
        String s = Receive.P_MessageData;
        return s;
    }

    public static String error() {
        String s = Receive.P_Error;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
