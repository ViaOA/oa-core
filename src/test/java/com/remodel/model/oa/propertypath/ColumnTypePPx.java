// Generated by OABuilder
package com.remodel.model.oa.propertypath;
 
import java.io.Serializable;

import com.remodel.model.oa.*;
 
public class ColumnTypePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ColumnTypePPx(String name) {
        this(null, name);
    }

    public ColumnTypePPx(PPxInterface parent, String name) {
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

    public DataTypePPx calcDataType() {
        DataTypePPx ppx = new DataTypePPx(this, ColumnType.P_CalcDataType);
        return ppx;
    }

    public ColumnPPx columns() {
        ColumnPPx ppx = new ColumnPPx(this, ColumnType.P_Columns);
        return ppx;
    }

    public DatabaseTypePPx databaseType() {
        DatabaseTypePPx ppx = new DatabaseTypePPx(this, ColumnType.P_DatabaseType);
        return ppx;
    }

    public DataTypePPx dataType() {
        DataTypePPx ppx = new DataTypePPx(this, ColumnType.P_DataType);
        return ppx;
    }

    public SqlTypePPx sqlType() {
        SqlTypePPx ppx = new SqlTypePPx(this, ColumnType.P_SqlType);
        return ppx;
    }

    public String id() {
        return pp + "." + ColumnType.P_Id;
    }

    public String created() {
        return pp + "." + ColumnType.P_Created;
    }

    public String isJsonb() {
        return pp + "." + ColumnType.P_IsJsonb;
    }

    public String seq() {
        return pp + "." + ColumnType.P_Seq;
    }

    public String display() {
        return pp + "." + ColumnType.P_Display;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
