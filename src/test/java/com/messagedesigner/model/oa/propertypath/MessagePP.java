// Generated by OABuilder
package com.messagedesigner.model.oa.propertypath;
 
import com.messagedesigner.model.oa.*;
import com.messagedesigner.model.oa.Message;
import com.messagedesigner.model.oa.propertypath.MessageTypePPx;
import com.messagedesigner.model.oa.propertypath.RpgMessagePPx;
 
public class MessagePP {
    private static MessageTypePPx messageType;
    private static RpgMessagePPx rpgMessages;
     

    public static MessageTypePPx messageType() {
        if (messageType == null) messageType = new MessageTypePPx(Message.P_MessageType);
        return messageType;
    }

    public static RpgMessagePPx rpgMessages() {
        if (rpgMessages == null) rpgMessages = new RpgMessagePPx(Message.P_RpgMessages);
        return rpgMessages;
    }

    public static String id() {
        String s = Message.P_Id;
        return s;
    }

    public static String created() {
        String s = Message.P_Created;
        return s;
    }

    public static String json() {
        String s = Message.P_Json;
        return s;
    }

    public static String processed() {
        String s = Message.P_Processed;
        return s;
    }

    public static String cancelled() {
        String s = Message.P_Cancelled;
        return s;
    }

    public static String seqNumber() {
        String s = Message.P_SeqNumber;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 