// Generated by OABuilder
package com.messagedesigner.model.oa.propertypath;
 
import com.messagedesigner.model.oa.*;
import com.messagedesigner.model.oa.RpgMessage;
import com.messagedesigner.model.oa.propertypath.MessagePPx;
import com.messagedesigner.model.oa.propertypath.MessageTypeRecordPPx;
 
public class RpgMessagePP {
    private static MessagePPx message;
    private static MessageTypeRecordPPx messageTypeRecord;
     

    public static MessagePPx message() {
        if (message == null) message = new MessagePPx(RpgMessage.P_Message);
        return message;
    }

    public static MessageTypeRecordPPx messageTypeRecord() {
        if (messageTypeRecord == null) messageTypeRecord = new MessageTypeRecordPPx(RpgMessage.P_MessageTypeRecord);
        return messageTypeRecord;
    }

    public static String id() {
        String s = RpgMessage.P_Id;
        return s;
    }

    public static String created() {
        String s = RpgMessage.P_Created;
        return s;
    }

    public static String code() {
        String s = RpgMessage.P_Code;
        return s;
    }

    public static String binary() {
        String s = RpgMessage.P_Binary;
        return s;
    }

    public static String json() {
        String s = RpgMessage.P_Json;
        return s;
    }

    public static String processed() {
        String s = RpgMessage.P_Processed;
        return s;
    }

    public static String cancelled() {
        String s = RpgMessage.P_Cancelled;
        return s;
    }

    public static String error() {
        String s = RpgMessage.P_Error;
        return s;
    }

    public static String status() {
        String s = RpgMessage.P_Status;
        return s;
    }

    public static String binaryDisplay() {
        String s = RpgMessage.P_BinaryDisplay;
        return s;
    }

    public static String convert() {
        String s = "convert";
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
