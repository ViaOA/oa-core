// Generated by OABuilder
package com.corptostore.model.oa.propertypath;
 
import com.corptostore.model.oa.*;
import com.corptostore.model.oa.Batch;
import com.corptostore.model.oa.propertypath.ReceivePPx;
import com.corptostore.model.oa.propertypath.SendPPx;
import com.corptostore.model.oa.propertypath.StoreBatchPPx;
import com.corptostore.model.oa.propertypath.StorePPx;
 
public class BatchPP {
    private static SendPPx beginAllStoreSend;
    private static SendPPx endAllStoreSend;
    private static StorePPx fromStore;
    private static ReceivePPx receives;
    private static SendPPx sends;
    private static StoreBatchPPx storeBatch;
    private static StorePPx toStore;
     

    public static SendPPx beginAllStoreSend() {
        if (beginAllStoreSend == null) beginAllStoreSend = new SendPPx(Batch.P_BeginAllStoreSend);
        return beginAllStoreSend;
    }

    public static SendPPx endAllStoreSend() {
        if (endAllStoreSend == null) endAllStoreSend = new SendPPx(Batch.P_EndAllStoreSend);
        return endAllStoreSend;
    }

    public static StorePPx fromStore() {
        if (fromStore == null) fromStore = new StorePPx(Batch.P_FromStore);
        return fromStore;
    }

    public static ReceivePPx receives() {
        if (receives == null) receives = new ReceivePPx(Batch.P_Receives);
        return receives;
    }

    public static SendPPx sends() {
        if (sends == null) sends = new SendPPx(Batch.P_Sends);
        return sends;
    }

    public static StoreBatchPPx storeBatch() {
        if (storeBatch == null) storeBatch = new StoreBatchPPx(Batch.P_StoreBatch);
        return storeBatch;
    }

    public static StorePPx toStore() {
        if (toStore == null) toStore = new StorePPx(Batch.P_ToStore);
        return toStore;
    }

    public static String batchId() {
        String s = Batch.P_BatchId;
        return s;
    }

    public static String available() {
        String s = Batch.P_Available;
        return s;
    }

    public static String readyToUse() {
        String s = Batch.P_ReadyToUse;
        return s;
    }

    public static String created() {
        String s = Batch.P_Created;
        return s;
    }

    public static String sessionStartDate() {
        String s = Batch.P_SessionStartDate;
        return s;
    }

    public static String sessionSequenceNumber() {
        String s = Batch.P_SessionSequenceNumber;
        return s;
    }

    public static String previousStartDate() {
        String s = Batch.P_PreviousStartDate;
        return s;
    }

    public static String previousSequenceNumber() {
        String s = Batch.P_PreviousSequenceNumber;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 