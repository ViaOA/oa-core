// Generated by OABuilder
package com.messagedesigner.model.oa.propertypath;
 
import java.io.Serializable;

import com.messagedesigner.model.oa.*;
import com.messagedesigner.model.oa.MessageTypeColumn;
import com.messagedesigner.model.oa.propertypath.MessageTypeColumnPPx;
import com.messagedesigner.model.oa.propertypath.MessageTypeRecordPPx;
import com.messagedesigner.model.oa.propertypath.PPxInterface;
import com.messagedesigner.model.oa.propertypath.RpgTypePPx;
 
public class MessageTypeColumnPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public MessageTypeColumnPPx(String name) {
        this(null, name);
    }

    public MessageTypeColumnPPx(PPxInterface parent, String name) {
        String s = null;
        if (parent != null) {
            s = parent.toString();
        }
        if (s == null) s = "";
        if (name != null && name.length() > 0) {
            if (s.length() > 0 && name.charAt(0) != ':') s += ".";
            s += name;
        }
        pp = s;
    }

    public MessageTypeRecordPPx messageTypeRecord() {
        MessageTypeRecordPPx ppx = new MessageTypeRecordPPx(this, MessageTypeColumn.P_MessageTypeRecord);
        return ppx;
    }

    public MessageTypeRecordPPx recordSubCode() {
        MessageTypeRecordPPx ppx = new MessageTypeRecordPPx(this, MessageTypeColumn.P_RecordSubCode);
        return ppx;
    }

    public RpgTypePPx rpgType() {
        RpgTypePPx ppx = new RpgTypePPx(this, MessageTypeColumn.P_RpgType);
        return ppx;
    }

    public String id() {
        return pp + "." + MessageTypeColumn.P_Id;
    }

    public String created() {
        return pp + "." + MessageTypeColumn.P_Created;
    }

    public String name() {
        return pp + "." + MessageTypeColumn.P_Name;
    }

    public String rpgName() {
        return pp + "." + MessageTypeColumn.P_RpgName;
    }

    public String key() {
        return pp + "." + MessageTypeColumn.P_Key;
    }

    public String keyPos() {
        return pp + "." + MessageTypeColumn.P_KeyPos;
    }

    public String fromPos() {
        return pp + "." + MessageTypeColumn.P_FromPos;
    }

    public String toPos() {
        return pp + "." + MessageTypeColumn.P_ToPos;
    }

    public String size() {
        return pp + "." + MessageTypeColumn.P_Size;
    }

    public String description() {
        return pp + "." + MessageTypeColumn.P_Description;
    }

    public String decimalPlaces() {
        return pp + "." + MessageTypeColumn.P_DecimalPlaces;
    }

    public String format() {
        return pp + "." + MessageTypeColumn.P_Format;
    }

    public String specialType() {
        return pp + "." + MessageTypeColumn.P_SpecialType;
    }

    public String nullValueType() {
        return pp + "." + MessageTypeColumn.P_NullValueType;
    }

    public String docType() {
        return pp + "." + MessageTypeColumn.P_DocType;
    }

    public String notUsed() {
        return pp + "." + MessageTypeColumn.P_NotUsed;
    }

    public String seq() {
        return pp + "." + MessageTypeColumn.P_Seq;
    }

    public String note() {
        return pp + "." + MessageTypeColumn.P_Note;
    }

    public String followUp() {
        return pp + "." + MessageTypeColumn.P_FollowUp;
    }

    public String defaultJavaClassType() {
        return pp + "." + MessageTypeColumn.P_DefaultJavaClassType;
    }

    public MessageTypeColumnPPx invalidRpgTypeFilter() {
        MessageTypeColumnPPx ppx = new MessageTypeColumnPPx(this, ":invalidRpgType()");
        return ppx;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 