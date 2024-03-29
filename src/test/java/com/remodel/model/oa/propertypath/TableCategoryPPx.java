// Generated by OABuilder
package com.remodel.model.oa.propertypath;
 
import java.io.Serializable;

import com.remodel.model.oa.*;
 
public class TableCategoryPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public TableCategoryPPx(String name) {
        this(null, name);
    }

    public TableCategoryPPx(PPxInterface parent, String name) {
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

    public DatabasePPx database() {
        DatabasePPx ppx = new DatabasePPx(this, TableCategory.P_Database);
        return ppx;
    }

    public TableCategoryPPx parentTableCategory() {
        TableCategoryPPx ppx = new TableCategoryPPx(this, TableCategory.P_ParentTableCategory);
        return ppx;
    }

    public TableCategoryPPx tableCategories() {
        TableCategoryPPx ppx = new TableCategoryPPx(this, TableCategory.P_TableCategories);
        return ppx;
    }

    public TablePPx tables() {
        TablePPx ppx = new TablePPx(this, TableCategory.P_Tables);
        return ppx;
    }

    public String id() {
        return pp + "." + TableCategory.P_Id;
    }

    public String created() {
        return pp + "." + TableCategory.P_Created;
    }

    public String name() {
        return pp + "." + TableCategory.P_Name;
    }

    public String generate() {
        return pp + ".generate";
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
