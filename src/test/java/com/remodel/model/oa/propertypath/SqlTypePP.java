// Generated by OABuilder
package com.remodel.model.oa.propertypath;
 
import com.remodel.model.oa.*;
 
public class SqlTypePP {
    private static ColumnTypePPx columnTypes;
    private static DataTypePPx dataType;
     

    public static ColumnTypePPx columnTypes() {
        if (columnTypes == null) columnTypes = new ColumnTypePPx(SqlType.P_ColumnTypes);
        return columnTypes;
    }

    public static DataTypePPx dataType() {
        if (dataType == null) dataType = new DataTypePPx(SqlType.P_DataType);
        return dataType;
    }

    public static String id() {
        String s = SqlType.P_Id;
        return s;
    }

    public static String created() {
        String s = SqlType.P_Created;
        return s;
    }

    public static String sqlType() {
        String s = SqlType.P_SqlType;
        return s;
    }

    public static String name() {
        String s = SqlType.P_Name;
        return s;
    }

    public static String seq() {
        String s = SqlType.P_Seq;
        return s;
    }

    public static String display() {
        String s = SqlType.P_Display;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
