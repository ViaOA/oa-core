// Generated by OABuilder
package com.remodel.model.oa.propertypath;
 
import com.remodel.model.oa.*;
 
public class ColumnPP {
    private static ColumnTypePPx columnType;
    private static ForeignTableColumnPPx foreignTableColumns;
    private static ForeignTableColumnPPx foreignTableForeignColumns;
    private static IndexColumnPPx indexColumns;
    private static JsonObjectPPx jsonObject;
    private static QueryColumnPPx queryColumns;
    private static QuerySortPPx querySorts;
    private static TablePPx table;
     

    public static ColumnTypePPx columnType() {
        if (columnType == null) columnType = new ColumnTypePPx(Column.P_ColumnType);
        return columnType;
    }

    public static ForeignTableColumnPPx foreignTableColumns() {
        if (foreignTableColumns == null) foreignTableColumns = new ForeignTableColumnPPx(Column.P_ForeignTableColumns);
        return foreignTableColumns;
    }

    public static ForeignTableColumnPPx foreignTableForeignColumns() {
        if (foreignTableForeignColumns == null) foreignTableForeignColumns = new ForeignTableColumnPPx(Column.P_ForeignTableForeignColumns);
        return foreignTableForeignColumns;
    }

    public static IndexColumnPPx indexColumns() {
        if (indexColumns == null) indexColumns = new IndexColumnPPx(Column.P_IndexColumns);
        return indexColumns;
    }

    public static JsonObjectPPx jsonObject() {
        if (jsonObject == null) jsonObject = new JsonObjectPPx(Column.P_JsonObject);
        return jsonObject;
    }

    public static QueryColumnPPx queryColumns() {
        if (queryColumns == null) queryColumns = new QueryColumnPPx(Column.P_QueryColumns);
        return queryColumns;
    }

    public static QuerySortPPx querySorts() {
        if (querySorts == null) querySorts = new QuerySortPPx(Column.P_QuerySorts);
        return querySorts;
    }

    public static TablePPx table() {
        if (table == null) table = new TablePPx(Column.P_Table);
        return table;
    }

    public static String id() {
        String s = Column.P_Id;
        return s;
    }

    public static String created() {
        String s = Column.P_Created;
        return s;
    }

    public static String name() {
        String s = Column.P_Name;
        return s;
    }

    public static String primaryKey() {
        String s = Column.P_PrimaryKey;
        return s;
    }

    public static String autoNumber() {
        String s = Column.P_AutoNumber;
        return s;
    }

    public static String use() {
        String s = Column.P_Use;
        return s;
    }

    public static String newName() {
        String s = Column.P_NewName;
        return s;
    }

    public static String description() {
        String s = Column.P_Description;
        return s;
    }

    public static String size() {
        String s = Column.P_Size;
        return s;
    }

    public static String decimals() {
        String s = Column.P_Decimals;
        return s;
    }

    public static String seq() {
        String s = Column.P_Seq;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
