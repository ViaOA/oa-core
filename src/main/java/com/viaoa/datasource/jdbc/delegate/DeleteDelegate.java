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

import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.datasource.jdbc.OADataSourceJDBC;
import com.viaoa.datasource.jdbc.db.Column;
import com.viaoa.datasource.jdbc.db.Table;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKeyDelegate;

/**
 * Manages deleting for JDBC datasource.
 *
 * @author vvia
 */
public class DeleteDelegate {
	private static Logger LOG = Logger.getLogger(DeleteDelegate.class.getName());

	public static void delete(OADataSourceJDBC ds, OAObject object) {
		if (object == null || !(object instanceof OAObject)) {
			return;
		}
		if (((OAObject) object).getNew()) {
			LOG.fine("delete called on a new object, class=" + object.getClass().getName() + ", key=" + OAObjectKeyDelegate.getKey(object));
			return;
		}
		delete(ds, object, object.getClass());
	}

	private static void delete(OADataSourceJDBC ds, OAObject oaObj, Class clazz) {
		String sql = null;
		try {
			sql = getDeleteSQL(ds, oaObj, clazz);

			/*
			OAObjectKey key = OAObjectKeyDelegate.getKey(oaObj);
			String s = String.format("Update, class=%s, id=%s, sql=%s",
			        OAString.getClassName(oaObj.getClass()),
			        key.toString(),
			        sql
			);
			OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
			if (oi.getUseDataSource()) {
			    OAObject.OALOG.fine(s);
			}
			LOG.fine(s);
			*/
			performDelete(ds, sql);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "exception trying to delete, sql=" + sql, e);
			throw new RuntimeException(e);
		}
		Class c = clazz.getSuperclass();
		if (c != null && !c.equals(OAObject.class)) {
			delete(ds, oaObj, c);
		}
	}

	/**
	 * This is needed when a object has a super class that needs to be deleted.
	 */
	private static String getDeleteSQL(OADataSourceJDBC ds, OAObject oaObj, Class clazz) throws Exception {
		Table table = ds.getDatabase().getTable(clazz);
		if (table == null) {
			throw new Exception("cant find table for Class " + clazz.getName());
		}
		Column[] columns = table.getColumns();
		StringBuffer where = new StringBuffer(64);
		for (int i = 0; columns != null && i < columns.length; i++) {
			Column column = columns[i];
			if (!column.primaryKey || column.propertyName == null || column.propertyName.length() == 0) {
				continue;
			}

			Object obj = oaObj.getProperty(column.propertyName);

			String op = "=";
			String value;
			if (obj == null) {
				op = "IS";
			}
			value = ConverterDelegate.convert(ds.getDBMetaData(), column, obj);
			value = ds.getDBMetaData().leftBracket + column.columnName.toUpperCase() + ds.getDBMetaData().rightBracket + " " + op + " "
					+ value;

			if (where.length() > 0) {
				where.append(" AND ");
			}
			where.append(value);
		}
		String str = "DELETE FROM " + ds.getDBMetaData().leftBracket + table.name.toUpperCase() + ds.getDBMetaData().rightBracket
				+ " WHERE " + where;
		return str;
	}

	private static void performDelete(OADataSourceJDBC ds, String str) throws Exception {
		LOG.fine(str);
		Statement statement = null;
		try {
			// DBLogDelegate.logDelete(str);
			//qqqqqqqqqqqqq
			statement = ds.getBatchStatement(str);
			int x = statement.executeUpdate(str);
			if (x != 1) {
				LOG.warning("row was not DELETEd, no exception thrown");
			}
		} finally {
			if (statement != null) {
				ds.releaseStatement(statement);
			}
		}
	}

}
