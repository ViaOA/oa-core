// Generated by OABuilder
package com.oreillyauto.remodel.model.oa.propertypath;
 
import com.oreillyauto.remodel.model.oa.*;
 
public class IndexPP {
    private static IndexColumnPPx indexColumns;
    private static TablePPx table;
     

    public static IndexColumnPPx indexColumns() {
        if (indexColumns == null) indexColumns = new IndexColumnPPx(Index.P_IndexColumns);
        return indexColumns;
    }

    public static TablePPx table() {
        if (table == null) table = new TablePPx(Index.P_Table);
        return table;
    }

    public static String id() {
        String s = Index.P_Id;
        return s;
    }

    public static String created() {
        String s = Index.P_Created;
        return s;
    }

    public static String name() {
        String s = Index.P_Name;
        return s;
    }

    public static String newName() {
        String s = Index.P_newName;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 