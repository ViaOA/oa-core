// Generated by OABuilder
package com.remodel.model.oa.propertypath;
 
import java.io.Serializable;

import com.remodel.model.oa.*;
 
public class SqlTypePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public SqlTypePPx(String name) {
        this(null, name);
    }

    public SqlTypePPx(PPxInterface parent, String name) {
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

    public ColumnTypePPx columnTypes() {
        ColumnTypePPx ppx = new ColumnTypePPx(this, SqlType.P_ColumnTypes);
        return ppx;
    }

    public DataTypePPx dataType() {
        DataTypePPx ppx = new DataTypePPx(this, SqlType.P_DataType);
        return ppx;
    }

    public String id() {
        return pp + "." + SqlType.P_Id;
    }

    public String created() {
        return pp + "." + SqlType.P_Created;
    }

    public String sqlType() {
        return pp + "." + SqlType.P_SqlType;
    }

    public String name() {
        return pp + "." + SqlType.P_Name;
    }

    public String seq() {
        return pp + "." + SqlType.P_Seq;
    }

    public String display() {
        return pp + "." + SqlType.P_Display;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
