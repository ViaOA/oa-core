// Generated by OABuilder
package com.messagedesigner.model.oa.propertypath;
 
import com.messagedesigner.model.oa.*;
import com.messagedesigner.model.oa.RpgType;
import com.messagedesigner.model.oa.propertypath.JsonTypePPx;
import com.messagedesigner.model.oa.propertypath.MessageTypeColumnPPx;
 
public class RpgTypePP {
    private static JsonTypePPx jsonType;
    private static MessageTypeColumnPPx messageTypeColumns;
     

    public static JsonTypePPx jsonType() {
        if (jsonType == null) jsonType = new JsonTypePPx(RpgType.P_JsonType);
        return jsonType;
    }

    public static MessageTypeColumnPPx messageTypeColumns() {
        if (messageTypeColumns == null) messageTypeColumns = new MessageTypeColumnPPx(RpgType.P_MessageTypeColumns);
        return messageTypeColumns;
    }

    public static String id() {
        String s = RpgType.P_Id;
        return s;
    }

    public static String created() {
        String s = RpgType.P_Created;
        return s;
    }

    public static String name() {
        String s = RpgType.P_Name;
        return s;
    }

    public static String encodeType() {
        String s = RpgType.P_EncodeType;
        return s;
    }

    public static String defaultSize() {
        String s = RpgType.P_DefaultSize;
        return s;
    }

    public static String defaultFormat() {
        String s = RpgType.P_DefaultFormat;
        return s;
    }

    public static String nullValueType() {
        String s = RpgType.P_NullValueType;
        return s;
    }

    public static String note() {
        String s = RpgType.P_Note;
        return s;
    }

    public static String samples() {
        String s = RpgType.P_Samples;
        return s;
    }

    public static String seq() {
        String s = RpgType.P_Seq;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 