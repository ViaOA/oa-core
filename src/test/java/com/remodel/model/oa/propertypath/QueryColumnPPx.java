// Generated by OABuilder
package com.remodel.model.oa.propertypath;
 
import java.io.Serializable;

import com.remodel.model.oa.*;
 
public class QueryColumnPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public QueryColumnPPx(String name) {
        this(null, name);
    }

    public QueryColumnPPx(PPxInterface parent, String name) {
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

    public ColumnPPx column() {
        ColumnPPx ppx = new ColumnPPx(this, QueryColumn.P_Column);
        return ppx;
    }

    public JsonColumnPPx jsonColumn() {
        JsonColumnPPx ppx = new JsonColumnPPx(this, QueryColumn.P_JsonColumn);
        return ppx;
    }

    public QueryTablePPx queryTable() {
        QueryTablePPx ppx = new QueryTablePPx(this, QueryColumn.P_QueryTable);
        return ppx;
    }

    public String id() {
        return pp + "." + QueryColumn.P_Id;
    }

    public String created() {
        return pp + "." + QueryColumn.P_Created;
    }

    public String name() {
        return pp + "." + QueryColumn.P_Name;
    }

    public String seq() {
        return pp + "." + QueryColumn.P_Seq;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
