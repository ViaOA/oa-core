// Generated by OABuilder
package com.oreillyauto.dev.tool.messagedesigner.model.oa.propertypath;
 
import com.oreillyauto.dev.tool.messagedesigner.model.oa.*;
 
public class RpgProgramPP {
    private static MessageTypeRecordPPx messageTypeRecords;
     

    public static MessageTypeRecordPPx messageTypeRecords() {
        if (messageTypeRecords == null) messageTypeRecords = new MessageTypeRecordPPx(RpgProgram.P_MessageTypeRecords);
        return messageTypeRecords;
    }

    public static String id() {
        String s = RpgProgram.P_Id;
        return s;
    }

    public static String created() {
        String s = RpgProgram.P_Created;
        return s;
    }

    public static String name() {
        String s = RpgProgram.P_Name;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 