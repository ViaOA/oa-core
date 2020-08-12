/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.datasource.jdbc.delegate;

import com.viaoa.datasource.jdbc.db.DBMetaData;

/**
 * Delegate used to generate DDL for making database changes.
 * 
 * @author vvia
 */
public class DDLDelegate {

	public static String getCreateTableSQL(DBMetaData dbmd, String tableName) {
		return "CREATE TABLE " + dbmd.leftBracket + tableName + dbmd.rightBracket + ";";
	}

	public static String getBeginCreateTableSQL(DBMetaData dbmd, String tableName) {
		return "CREATE TABLE " + dbmd.leftBracket + tableName + dbmd.rightBracket + "(";
	}

	public static String getEndCreateTableSQL(DBMetaData dbmd) {
		return ");";
	}

	public static String getInsertRecordsSQL_HOLD(DBMetaData dbmd, String fromName, String toName) {
		/* was:  Access did not like the "()" around the select
		String s = "INSERT INTO " + dbmd.leftBracket + toName + dbmd.rightBracket + " ";
		s += "(Select * FROM " + dbmd.leftBracket + fromName + dbmd.rightBracket + ");";
		*/
		/***
		 * 2007/11/19 commented out until it is needed // need to use columns ResultSet rs = null; String colsFrom = null; String colsTo =
		 * null; try { rs = connectionPool.databaseMetaData.getColumns(null,null,toName, null); for ( ;rs.next(); ) { String columnName =
		 * (String) rs.getString(4); if (rs.wasNull()) continue; if (colsTo == null) colsTo = ""; else colsTo += ", "; colsTo +=
		 * dbmd.leftBracket + columnName + dbmd.rightBracket; if (colsFrom == null) colsFrom = ""; else colsFrom += ", "; colsFrom +=
		 * dbmd.leftBracket + columnName + dbmd.rightBracket; } } catch (SQLException e) { e.printStackTrace(); System.out.println(""+e); }
		 * finally { if (rs != null) { try { rs.close(); } catch (SQLException e) { } } } String s = "INSERT INTO " + dbmd.leftBracket +
		 * toName + dbmd.rightBracket + " (" + colsTo + ") "; s += "Select "+colsFrom+" FROM " + dbmd.leftBracket + fromName +
		 * dbmd.rightBracket + ";"; return s;
		 */
		return null;
	}

	public static String getInsertRecordsSQL(DBMetaData dbmd, String fromName, String toName, String columnNames) {
		/*
		String s = "INSERT INTO " + dbmd.leftBracket + toName + dbmd.rightBracket + " (" + columnNames + ") VALUES ";
		s += "(Select "+columnNames+" FROM " + dbmd.leftBracket + fromName + dbmd.rightBracket + ");";
		*/

		// Access
		// INSERT INTO Client (Id) Select Id from Pet
		String s = "INSERT INTO " + dbmd.leftBracket + toName + dbmd.rightBracket + " (" + columnNames + ") ";
		s += "Select " + columnNames + " FROM " + dbmd.leftBracket + fromName + dbmd.rightBracket + ";";
		return s;
	}

	public static String getInsertRecordsSQL(DBMetaData dbmd, String fromName, String toName, String fromColumnNames, String toColumnNames,
			String where) {
		/*
		String s = "INSERT INTO " + dbmd.leftBracket + toName + dbmd.rightBracket + " (" + toColumnNames + ") VALUES ";
		s += "(Select "+fromColumnNames+" FROM " + dbmd.leftBracket + fromName + dbmd.rightBracket + ");";
		*/

		if (where == null) {
			where = "";
		}
		if (where.length() > 0) {
			where = " WHERE " + where;
		}
		String s = "INSERT INTO " + dbmd.leftBracket + toName + dbmd.rightBracket + " (" + toColumnNames + ") ";
		s += "Select " + fromColumnNames + " FROM " + dbmd.leftBracket + fromName + dbmd.rightBracket + where + ";";
		return s;
	}

	public static String getUpdateColumnSQL(DBMetaData dbmd, String tableName, String fromColumnName, String toColumnName) {
		String s = "UPDATE " + dbmd.leftBracket + tableName + dbmd.rightBracket + " SET " + toColumnName + " = " + fromColumnName + ";";
		return s;
	}

	public static String getUpdateColumnSQL(DBMetaData dbmd, String fromTableName, String toTableName, String fromColumnName,
			String toColumnName) {
		return getUpdateColumnSQL(dbmd, fromTableName, toTableName, fromColumnName, toColumnName, null);
	}

	public static String getUpdateColumnSQL(DBMetaData dbmd, String fromTableName, String toTableName, String fromColumnName,
			String toColumnName, String whereClause) {
		return getUpdateColumnSQL(	dbmd, fromTableName, toTableName, new String[] { fromColumnName }, new String[] { toColumnName },
									whereClause);
	}

	public static String getUpdateColumnSQL(DBMetaData dbmd, String fromTableName, String toTableName, String[] fromColumnNames,
			String[] toColumnNames) {
		return getUpdateColumnSQL(dbmd, fromTableName, toTableName, fromColumnNames, toColumnNames, null);
	}

	public static String getUpdateColumnSQL(DBMetaData dbmd, String fromTableName, String toTableName, String[] fromColumnNames,
			String[] toColumnNames, String whereClause) {
		String sql = "";
		switch (dbmd.databaseType) {
		case DBMetaData.ACCESS:
			// update Appointment, Breed  set Appointment.UserId = Breed.Name WHERE Appointment.ID = Breed.ID
			sql = "UPDATE " + dbmd.leftBracket + fromTableName + dbmd.rightBracket + ", " + dbmd.leftBracket + toTableName
					+ dbmd.rightBracket;
			sql += " SET ";
			for (int i = 0; i < fromColumnNames.length; i++) {
				if (i > 0) {
					sql += ", ";
				}
				sql += dbmd.leftBracket + toTableName + dbmd.rightBracket + "." + toColumnNames[i] + " = ";
				sql += dbmd.leftBracket + fromTableName + dbmd.rightBracket + "." + fromColumnNames[i];
			}
			if (whereClause != null && whereClause.length() > 0) {
				sql += " WHERE " + whereClause;
			}
			break;
		case DBMetaData.DERBY:
		case DBMetaData.MYSQL:
		case DBMetaData.ORACLE:
		case DBMetaData.SQLSERVER:
		case DBMetaData.POSTGRES:
		case DBMetaData.OTHER:
		default:
			// this is for SQL Server, not sure about others.
			sql = "UPDATE " + dbmd.leftBracket + toTableName + dbmd.rightBracket;
			sql += " SET ";
			for (int i = 0; i < fromColumnNames.length; i++) {
				if (i > 0) {
					sql += ", ";
				}
				sql += dbmd.leftBracket + toTableName + dbmd.rightBracket + "." + toColumnNames[i] + " = ";
				sql += dbmd.leftBracket + fromTableName + dbmd.rightBracket + "." + fromColumnNames[i];
			}
			// sql += " FROM " + dbmd.leftBracket+fromTableName+dbmd.rightBracket;

			if (whereClause != null && whereClause.length() > 0) {
				// sql += ", " + dbmd.leftBracket+toTableName+dbmd.rightBracket;                
				sql += " WHERE " + whereClause;
			}
			sql += ";";
			break;
		}

		return sql;
	}

	public static String getDropPkeyConstraintSQL(DBMetaData dbmd, String tableName, String pkName) {
		String s = "ALTER TABLE " + dbmd.leftBracket + tableName + dbmd.rightBracket + " DROP Constraint " + pkName + ";";
		return s;
	}

	public static String getAddPkeyConstraintSQL(DBMetaData dbmd, String tableName, String constraintName, String pkeys) {
		String s = "ALTER TABLE " + dbmd.leftBracket + tableName + dbmd.rightBracket;
		s += " Add Constraint " + constraintName;
		s += " PRIMARY KEY (" + pkeys + ");";
		return s;
	}

	public static String getDropIndexSQL(DBMetaData dbmd, String tableName, String indexName) {
		String s;
		if (dbmd.databaseType == DBMetaData.SQLSERVER) {
			s = "DROP INDEX " + dbmd.leftBracket + tableName + dbmd.rightBracket + "." + indexName + ";";
		} else if (dbmd.databaseType == DBMetaData.DERBY) {
			s = "DROP INDEX " + indexName + ";";
		} else {
			s = "DROP INDEX " + indexName + " ON " + dbmd.leftBracket + tableName + dbmd.rightBracket + ";";
		}
		return s;
	}

	public static String getDropTableSQL(DBMetaData dbmd, String tableName) {
		String s = "DROP TABLE " + dbmd.leftBracket + tableName + dbmd.rightBracket + ";";
		return s;
	}

	public static String getCreateIndexSQL(DBMetaData dbmd, String tableName, String indexName, String columnNames) {
		String s = "CREATE INDEX " + indexName + " ON " + dbmd.leftBracket + tableName + dbmd.rightBracket + " (" + columnNames + ");";
		return s;
	}
	/*qqqqqq take out 
	public static String getAddColumnInfoSQL(DBMetaData dbmd, String tableName, String columnInfo) {
		String s = " COLUMN";
		switch (dbmd.databaseType) {
	    case DBMetaData.ACCESS:
	        break;
	    case DBMetaData.MYSQL:
	        break;
	    case DBMetaData.ORACLE:
	        break;
	    case DBMetaData.SQLSERVER:
	    	s = "";
	        break;
	    case DBMetaData.DERBY:
	        break;
		}
	
		s = "ALTER TABLE " + dbmd.leftBracket + tableName + dbmd.rightBracket + " ADD"+s+" " + columnInfo + ";";
	    return s;
	}
	**/

	public static String getAddColumnSQL(DBMetaData dbmd, String tableName, String columnName, String type) {
		String s = " COLUMN";
		switch (dbmd.databaseType) {
		case DBMetaData.ACCESS:
			break;
		case DBMetaData.MYSQL:
			break;
		case DBMetaData.ORACLE:
			break;
		case DBMetaData.SQLSERVER:
			s = "";
			break;
		case DBMetaData.DERBY:
			break;
		}
		String s2 = dbmd.leftBracket + columnName + dbmd.rightBracket + " " + type;
		s = "ALTER TABLE " + dbmd.leftBracket + tableName + dbmd.rightBracket + " ADD" + s + " " + s2 + ";";
		return s;
	}

	/**
	 * Used to create the sql for adding a new column within a create new table command.
	 * 
	 * @param bLastColumn true if this is the last column being added for table.
	 */
	public static String getAddColumnSQL(DBMetaData dbmd, String columnName, String type, boolean bLastColumn) {
		String s = dbmd.leftBracket + columnName + dbmd.rightBracket + " " + type;
		if (!bLastColumn) {
			s += ",";
		}
		return s;
	}

	/**
	 * Used to create the sql for adding a new column within a create new table command.
	 * 
	 * @param params      example: "NOT NULL"
	 * @param bLastColumn true if this is the last column being added for table.
	 */
	public static String getAddColumnSQL(DBMetaData dbmd, String columnName, String type, String params, boolean bLastColumn) {
		String s = dbmd.leftBracket + columnName + dbmd.rightBracket + " " + type;
		if (params != null && params.length() > 0) {
			s += " " + params;
		}
		if (!bLastColumn) {
			s += ",";
		}
		return s;
	}

	public static String getDropColumnSQL(DBMetaData dbmd, String tableName, String columnName) {
		String s = "ALTER TABLE " + dbmd.leftBracket + tableName + dbmd.rightBracket + " DROP COLUMN " + dbmd.leftBracket + columnName
				+ dbmd.rightBracket + ";";
		return s;
	}

	public static String getAlterColumnSQL(DBMetaData dbmd, String tableName, String columnName, String newType) {
		String sql = null;
		switch (dbmd.databaseType) {
		case DBMetaData.ORACLE: // ok 2007/08/31, not tested
		case DBMetaData.MYSQL: // ok 2007/08/31, not tested
			sql = "ALTER TABLE " + dbmd.leftBracket + tableName + dbmd.rightBracket + " MODIFY COLUMN " + dbmd.leftBracket + columnName
					+ dbmd.rightBracket + " " + newType + ";";
			break;
		case DBMetaData.SQLSERVER: // ok 2007/08/31, not tested 
		case DBMetaData.ACCESS: // this is correct for Access
			sql = "ALTER TABLE " + dbmd.leftBracket + tableName + dbmd.rightBracket + " ALTER COLUMN " + dbmd.leftBracket + columnName
					+ dbmd.rightBracket + " " + newType + ";";
			break;
		case DBMetaData.DERBY: // ok 2007/08/31 tested, note: does not work if suffixed with ';'
			sql = "ALTER TABLE " + dbmd.leftBracket + tableName + dbmd.rightBracket + " ALTER COLUMN " + dbmd.leftBracket + columnName
					+ dbmd.rightBracket + " SET DATA TYPE " + newType;
			break;
		}

		return sql;
	}

	public static String getBlobType(DBMetaData dbmd, int maxLen) {
		String sqlType = "BLOB";
		switch (dbmd.databaseType) {
		case DBMetaData.SQLSERVER:
			// 20130112
			sqlType = "varbinary(MAX)";
			break;
		case DBMetaData.POSTGRES:
			// 20190617
			sqlType = "BYTEA";
			break;
		}

		return sqlType;
	}

	public static String getUnicodeType(DBMetaData dbmd, int maxLen) {
		String sqlType = "VARCHAR(" + maxLen + ")";
		switch (dbmd.databaseType) {
		case DBMetaData.ACCESS:
			// todo: need to get correct unicode type for this DB    
			break;
		case DBMetaData.MYSQL:
			// todo: need to get correct unicode type for this DB    
			break;
		case DBMetaData.ORACLE:
			// todo: need to get correct unicode type for this DB    
			break;
		case DBMetaData.SQLSERVER:
			sqlType = "NVARCHAR(" + maxLen + ")";
			break;
		case DBMetaData.DERBY:
			// todo: need to get correct unicode type for this DB    
			break;
		case DBMetaData.POSTGRES:
			sqlType = "VARCHAR(" + maxLen + ")";
			break;
		}
		return sqlType;
	}

	public static String getUnicodeCharType(DBMetaData dbmd, int maxLen) {
		String sqlType = "char(" + maxLen + ")";
		switch (dbmd.databaseType) {
		case DBMetaData.ACCESS:
			// todo: need to get correct unicode type for this DB    
			break;
		case DBMetaData.MYSQL:
			// todo: need to get correct unicode type for this DB    
			break;
		case DBMetaData.ORACLE:
			// todo: need to get correct unicode type for this DB    
			break;
		case DBMetaData.SQLSERVER:
			sqlType = "NCHAR(" + maxLen + ")";
			break;
		case DBMetaData.DERBY:
			// todo: need to get correct unicode type for this DB    
			break;
		case DBMetaData.POSTGRES:
			sqlType = "CHAR(" + maxLen + ")";
			break;
		}
		return sqlType;
	}

	public static String getLongUnicodeType(DBMetaData dbmd, int maxLen) {
		String sqlType = "";
		switch (dbmd.databaseType) {
		case DBMetaData.ACCESS:
			// todo: need to get correct unicode type for this DB    
			sqlType = "memo";
			break;
		case DBMetaData.MYSQL:
			// todo: need to get correct unicode type for this DB    
			sqlType = "LONGTEXT";
			break;
		case DBMetaData.ORACLE:
			// todo: need to get correct unicode type for this DB    
			sqlType = "long";
			break;
		case DBMetaData.SQLSERVER:
			sqlType = "NVARCHAR(MAX)";
			break;
		case DBMetaData.DERBY:
			// todo: need to get correct unicode type for this DB    
			sqlType = "CLOB"; // "CLOB("+maxLen+")";
			break;
		case DBMetaData.POSTGRES:
			sqlType = "TEXT";
			break;
		}
		return sqlType;
	}

	public static String getStringType(DBMetaData dbmd, int maxLen) {
		String sqlType = "VARCHAR(" + maxLen + ")";
		return sqlType;
	}

	public static String getLongTextType(DBMetaData dbmd, int maxLen) {
		String sqlType = "";
		switch (dbmd.databaseType) {
		case DBMetaData.ACCESS:
			sqlType = "memo";
			break;
		case DBMetaData.MYSQL:
			sqlType = "LONGTEXT";
			break;
		case DBMetaData.ORACLE:
			sqlType = "long";
			break;
		case DBMetaData.SQLSERVER:
			// 20130112
			sqlType = "varchar(MAX)";
			//was: sqlType = "text";
			break;
		case DBMetaData.DERBY:
			sqlType = "CLOB"; // "CLOB("+maxLen+")";
			break;
		case DBMetaData.POSTGRES:
			sqlType = "TEXT";
			break;
		}
		return sqlType;
	}

	public static String getBooleanType(DBMetaData dbmd) {
		String sqlType = "bit";
		switch (dbmd.databaseType) {
		case DBMetaData.ACCESS:
			sqlType = "bit";
			break;
		case DBMetaData.ORACLE:
			sqlType = "char";
			break;
		case DBMetaData.SQLSERVER:
			sqlType = "bit";
			break;
		case DBMetaData.MYSQL:
			sqlType = "BIT";
			break;
		case DBMetaData.DERBY:
			sqlType = "smallint";
			break;
		case DBMetaData.POSTGRES:
			sqlType = "boolean";
			break;
		}
		return sqlType;
	}

	public static String getIntType(DBMetaData dbmd) {
		String sqlType = "int";
		return sqlType;
	}

	public static String getSmallIntType(DBMetaData dbmd) {
		String sqlType = "smallint";
		return sqlType;
	}

	public static String getLongType(DBMetaData dbmd) {
		String sqlType = "long";
		switch (dbmd.databaseType) {
		case DBMetaData.SQLSERVER:
		case DBMetaData.POSTGRES:
		case DBMetaData.DERBY:
			sqlType = "BIGINT";
			break;
		}
		return sqlType;
	}

	public static String getFloatType(DBMetaData dbmd, int decimalLength) {
		String sqlType = "float";
		return sqlType;
	}

	public static String getDoubleType(DBMetaData dbmd, int decimalLen) {
		String sqlType = "float";
		switch (dbmd.databaseType) {
		case DBMetaData.SQLSERVER:
			sqlType = "REAL";
			break;
		case DBMetaData.DERBY:
			sqlType = "DOUBLE";
			break;
		case DBMetaData.POSTGRES:
			sqlType = "real";
			break;
		}
		return sqlType;
	}

	public static String getDateType(DBMetaData dbmd) {
		String sqlType = "datetime";
		switch (dbmd.databaseType) {
		case DBMetaData.SQLSERVER:
		case DBMetaData.POSTGRES:
		case DBMetaData.MYSQL:
		case DBMetaData.ORACLE:
		case DBMetaData.DERBY:
			sqlType = "DATE";
			break;
		}
		return sqlType;
	}

	public static String getDateTimeType(DBMetaData dbmd) {
		String sqlType = "DATETIME"; // ??????
		switch (dbmd.databaseType) {
		case DBMetaData.POSTGRES:
		case DBMetaData.MYSQL:
			sqlType = "TIMESTAMP"; // ?????? DATETIME
			break;
		case DBMetaData.ORACLE:
			sqlType = "DATE";
			break;
		case DBMetaData.DERBY:
			sqlType = "TIMESTAMP"; // VALUES TIMESTAMP('1962-09-23 03:23:34.234')
			break;
		}
		return sqlType;
	}

	public static String getTimestampType(DBMetaData dbmd) {
		String sqlType = "TIMESTAMP";
		switch (dbmd.databaseType) {
		case DBMetaData.MYSQL:
			sqlType = "TIMESTAMP";
			break;
		case DBMetaData.ORACLE: // see: http://docs.oracle.com/javase/1.5.0/docs/guide/jdbc/getstart/mapping.html
			sqlType = "DATE";
			break;
		}
		return sqlType;
	}

	public static String getCurrencyType(DBMetaData dbmd, int decimalLen) {
		String sqlType = "float";
		switch (dbmd.databaseType) {
		case DBMetaData.ACCESS:
			sqlType = "currency";
			// numeric(8, 2)
			break;
		case DBMetaData.ORACLE:
			sqlType = "NUMBER(8," + decimalLen + ")";
			break;
		case DBMetaData.SQLSERVER:
			sqlType = "money";
			break;
		case DBMetaData.MYSQL:
			sqlType = "DECIMAL(8," + decimalLen + ")";
			break;
		case DBMetaData.DERBY:
			sqlType = "DECIMAL(16," + decimalLen + ")";
			break;
		case DBMetaData.POSTGRES:
			sqlType = "money";
			break;
		}
		return sqlType;
	}

	public static String getTimeType(DBMetaData dbmd) {
		String sqlType = "datetime";
		switch (dbmd.databaseType) {
		case DBMetaData.MYSQL:
			sqlType = "TIME";
			break;
		case DBMetaData.ORACLE:
			sqlType = "DATE";
			break;
		case DBMetaData.DERBY:
			sqlType = "TIME";
			break;
		case DBMetaData.POSTGRES:
			sqlType = "TIME";
			break;
		}
		return sqlType;
	}

	public static String getNumberType(DBMetaData dbmd, int maxLen, int decimalLength) {
		String sqlType = "numeric(" + maxLen + "," + decimalLength + ")";
		if (dbmd.databaseType == DBMetaData.DERBY) {
			sqlType = "decimal(" + maxLen + "," + decimalLength + ")";
		}
		return sqlType;
	}

	public static String getAddForeignKeySQL(DBMetaData dbmd, String fromTable, String toTable, String constraintName, String fromColumns,
			String toColumns) {
		String s = "";
		s += ("ALTER TABLE " + fromTable + " ADD");
		s += (" CONSTRAINT " + constraintName + " FOREIGN KEY (" + fromColumns + ")");
		s += (" REFERENCES " + toTable + " (" + toColumns + ");");
		return s;
	}

	public static String getDropForeignKeySQL(DBMetaData dbmd, String tableName, String constraintName) {
		String s = "";
		s += "ALTER TABLE " + tableName + " DROP";
		s += " CONSTRAINT " + constraintName + ";";
		return s;
	}

}
