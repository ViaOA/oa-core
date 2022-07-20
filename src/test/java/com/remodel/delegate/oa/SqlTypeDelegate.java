package com.remodel.delegate.oa;

import java.util.logging.Logger;

import com.remodel.delegate.ModelDelegate;
import com.remodel.model.oa.DataType;
import com.remodel.model.oa.SqlType;
import com.viaoa.util.OAString;

public class SqlTypeDelegate {

	private static final Object Lock = new Object();
	private static Logger LOG = Logger.getLogger(SqlTypeDelegate.class.getName());

	public static SqlType getSqlType(int type) {
		return getSqlType(type, null, false);
	}

	public static SqlType getSqlType(String name) {
		SqlType st = ModelDelegate.getSqlTypes().find(SqlType.P_SqlType, name);
		if (st == null) {
			st = ModelDelegate.getSqlTypes().find(SqlType.P_SqlType, OAString.convert(name, " ", ""));
		}
		return st;
	}

	public static SqlType getSqlType(int type, String name, boolean bAutoCreateNew) {
		SqlType st = ModelDelegate.getSqlTypes().find(SqlType.P_SqlType, type);
		if (st == null && bAutoCreateNew) {
			synchronized (Lock) {
				st = ModelDelegate.getSqlTypes().find(SqlType.P_SqlType, type);
				if (st == null) {
					st = new SqlType();
					st.setSqlType(type);
					st.setName(name);
					ModelDelegate.getSqlTypes().add(st);
				}
			}
		}
		return st;
	}

	public static void assignDefaultValues() {
		LOG.config("creating and assigning SqlTypes to DataTypes");
		getSqlType(-7, "bit", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_booleanType));
		getSqlType(-6, "tiny int", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_shortType));
		getSqlType(5, "small int", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_shortType));
		getSqlType(4, "integer", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_intType));
		getSqlType(-5, "big int", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_longType));
		getSqlType(6, "float", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_floatType));
		getSqlType(7, "real", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_doubleType));
		getSqlType(8, "double", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_doubleType));
		getSqlType(2, "numeric", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_Number));
		getSqlType(3, "decimal", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_Number));
		getSqlType(1, "char", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_String));
		getSqlType(12, "varchar", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_StringLong));
		getSqlType(-1, "long varchar", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_String));
		getSqlType(91, "date", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_Date));
		getSqlType(92, "time", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_Time));
		getSqlType(93, "timestamp", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_DateTime));
		getSqlType(-2, "binary", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_Blob));
		getSqlType(-3, "varbinary", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_Blob));
		getSqlType(-4, "long varbinary", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_Blob));
		getSqlType(0, "null", true).setDataType(null);
		getSqlType(1111, "other", true).setDataType(null);
		getSqlType(2000, "java object", true).setDataType(null);
		getSqlType(2001, "distinct", true).setDataType(null);
		getSqlType(2002, "struct", true).setDataType(null);
		getSqlType(2003, "array", true).setDataType(null);
		getSqlType(2004, "blob", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_Blob));
		getSqlType(2005, "clob", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_StringLong));
		getSqlType(2006, "ref", true).setDataType(null);
		getSqlType(70, "data link", true).setDataType(null);
		getSqlType(16, "boolean", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_booleanType));
		getSqlType(-8, "row id", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_longType));
		getSqlType(-15, "nchar", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_String));
		getSqlType(-9, "nvarchar", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_StringLong));
		getSqlType(-16, "long nvarchar", true)
				.setDataType(DataTypeDelegate.getDataType(DataType.TYPE_StringLong));
		getSqlType(2011, "nclob", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_StringLong));
		getSqlType(2009, "sql xml", true).setDataType(DataTypeDelegate.getDataType(DataType.TYPE_StringLong));
		getSqlType(2012, "ref cursor", true).setDataType(null);
		getSqlType(2013, "time with timezone", true)
				.setDataType(DataTypeDelegate.getDataType(DataType.TYPE_DateTime));
		getSqlType(2014, "timestamp with timezone", true)
				.setDataType(DataTypeDelegate.getDataType(DataType.TYPE_DateTime));
	}

}
