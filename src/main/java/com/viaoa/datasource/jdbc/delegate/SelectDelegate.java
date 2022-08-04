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

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSourceIterator;
import com.viaoa.datasource.jdbc.OADataSourceJDBC;
import com.viaoa.datasource.jdbc.db.Column;
import com.viaoa.datasource.jdbc.db.DBMetaData;
import com.viaoa.datasource.jdbc.db.DataAccessObject;
import com.viaoa.datasource.jdbc.db.Link;
import com.viaoa.datasource.jdbc.db.ManyToMany;
import com.viaoa.datasource.jdbc.db.Table;
import com.viaoa.datasource.jdbc.query.QueryConverter;
import com.viaoa.datasource.jdbc.query.ResultSetIterator;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectKeyDelegate;
import com.viaoa.transaction.OATransaction;
import com.viaoa.util.OAString;

/**
 * Manages Selects/Queries for JDBC datasource.
 *
 * @author vvia
 */
public class SelectDelegate {
	private static Logger LOG = Logger.getLogger(SelectDelegate.class.getName());

	private static ConcurrentHashMap<WhereObjectSelect, String> hmPreparedStatementSql = new ConcurrentHashMap<WhereObjectSelect, String>();
	private static ConcurrentHashMap<Class, String> hmPreparedStatementSqlx = new ConcurrentHashMap<Class, String>();

	private static ConcurrentHashMap<Class, String> hmPreparedStatementSqlxDirty = new ConcurrentHashMap<Class, String>();
	private static ConcurrentHashMap<Class, Column[]> hmPreparedStatementSqlxDirtyColumns = new ConcurrentHashMap<Class, Column[]>();

	/*
	public static Iterator select(OADataSourceJDBC ds, Class clazz, String queryWhere, String queryOrder, int max, boolean bDirty) {
		return select(ds, clazz, queryWhere, (Object[]) null, queryOrder, max, bDirty);
	}
	*/

	/*
	public static Iterator select(OADataSourceJDBC ds, Class clazz, String queryWhere, Object param, String queryOrder, int max, boolean bDirty) {
		Object[] params = null;
		if (param != null) params = new Object[] {param};
		return select(ds, clazz, queryWhere, params, queryOrder, max, bDirty);
	}
	*/

	public static OADataSourceIterator select(OADataSourceJDBC ds, Class clazz, String queryWhere, Object[] params, String queryOrder,
			int max, boolean bDirty) {
		if (ds == null) {
			return null;
		}
		if (clazz == null) {
			return null;
		}
		Table table = ds.getDatabase().getTable(clazz);
		if (table == null) {
			return null;
		}
		QueryConverter qc = new QueryConverter(ds);
		String[] queries = getSelectSQL(qc, ds, clazz, queryWhere, params, queryOrder, max, bDirty);

		ResultSetIterator rsi;
		DataAccessObject dao = table.getDataAccessObject();
		if (!bDirty && dao != null) {
			rsi = new ResultSetIterator(ds, clazz, dao, queries[0], queries[1], max);
		} else {
			Column[] columns = qc.getSelectColumnArray(clazz);
			if (queries[1] != null) {
				// this will take 2 queries.  The first will only select pkey columns.
				//   the second query will then select the record using the pkey values in the where clause.
				rsi = new ResultSetIterator(ds, clazz, columns, queries[0], queries[1], max);
			} else {
				rsi = new ResultSetIterator(ds, clazz, columns, queries[0], max);
			}
		}
		rsi.setDirty(bDirty);
		return rsi;
	}

	/**
	 * @returns array [0]=sql [1]=sql2 (if needed)
	 */
	private static String[] getSelectSQL(QueryConverter qc, OADataSourceJDBC ds, Class clazz, String queryWhere, Object[] params,
			String queryOrder, int max, boolean bDirty) {
		String[] queries = new String[2];

		queries[0] = qc.convertToSql(clazz, queryWhere, params, queryOrder);
		if (qc.getUseDistinct()) {
			// distinct query will also need to have the order by keys
			String s = " ORDER BY ";
			int x = queries[0].indexOf(s);
			if (x > 0) {
				x += s.length();
				s = queries[0].substring(x);

				// need to remove ASC, DESC
				// todo: this might not be needed anymore
				StringTokenizer st = new StringTokenizer(s, ", ", false);
				String s1 = null;
				for (; st.hasMoreElements();) {
					String s2 = (String) st.nextElement();
					String s3 = s2.toUpperCase();
					if (s3.equals("ASC")) {
						continue;
					}
					if (s3.equals("DESC")) {
						continue;
					}
					if (s1 == null) {
						s1 = s2;
					} else {
						s1 += ", " + s2;
					}
				}
				s = ", " + s1;
			} else {
				s = "";
			}

			// this will take 2 queries.  The first will only select pkey columns.
			//   the second query will then select the record using the pkey values in the where clause.
			queries[0] = "SELECT " + ds.getDBMetaData().distinctKeyword + " " + qc.getPrimaryKeyColumns(clazz) + s + " " + queries[0];

			OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
			String[] ids = oi.getIdProperties();
			params = new Object[ids.length];
			queries[1] = "";
			for (int i = 0; ids != null && i < ids.length; i++) {
				if (i > 0) {
					queries[1] += " AND ";
				}
				queries[1] += ids[i] + " = ?";
				params[i] = "7"; // fake out/position holder
			}
			queries[1] = qc.convertToSql(clazz, queries[1], params, null);
			queries[1] = "SELECT " + qc.getSelectColumns(clazz, bDirty) + " " + queries[1];
			queries[1] = OAString.convert(queries[1], "7", "?");
		} else {
			queries[0] = "SELECT " + qc.getSelectColumns(clazz, bDirty) + " " + queries[0];
			//was: queries[0] = "SELECT " + getMax(ds,max) + qc.getSelectColumns(clazz, bDirty) + " " + queries[0];
		}
		return queries;
	}

	private static class WhereObjectSelect {
		private Class clazz;
		private Class whereClazz;
		private String propertyFromWhereObject;

		public WhereObjectSelect(Class clazz, Class whereClazz, String propertyFromWhereObject) {
			this.clazz = clazz;
			this.whereClazz = whereClazz;
			this.propertyFromWhereObject = propertyFromWhereObject;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof WhereObjectSelect)) {
				return false;
			}
			WhereObjectSelect x = (WhereObjectSelect) obj;

			if (clazz != x.clazz) {
				if (clazz == null || x.clazz == null) {
					return false;
				}
				if (!clazz.equals(x.clazz)) {
					return false;
				}
			}
			if (whereClazz != x.whereClazz) {
				if (whereClazz == null || x.whereClazz == null) {
					return false;
				}
				if (!whereClazz.equals(x.whereClazz)) {
					return false;
				}
			}
			if (propertyFromWhereObject != x.propertyFromWhereObject) {
				if (propertyFromWhereObject == null || x.propertyFromWhereObject == null) {
					return false;
				}
				if (!propertyFromWhereObject.equals(x.propertyFromWhereObject)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public int hashCode() {
			int x = 0;
			if (clazz != null) {
				x += clazz.hashCode();
			}
			if (whereClazz != null) {
				x += whereClazz.hashCode();
			}
			if (propertyFromWhereObject != null) {
				x += propertyFromWhereObject.hashCode();
			}
			return x;
		}
	}

	public static OADataSourceIterator select(OADataSourceJDBC ds, Class clazz,
			OAObject whereObject, String propertyFromWhereObject,
			String queryWhere, Object[] params,
			String extraWhere,
			String queryOrder, int max, boolean bDirty) {

		/*was:
		public static OADataSourceIterator select(OADataSourceJDBC ds, Class clazz, OAObject whereObject, String extraWhere, Object[] params,
				String propertyFromWhereObject, String queryOrder, int max, boolean bDirty) {
			// dont need to select if master object (whereObject) is new
		 */
		if (whereObject == null || whereObject.getNew()) {
			return null;
		}

		Table table = ds.getDatabase().getTable(clazz);
		if (table == null) {
			return null;
		}
		DataAccessObject dao = table.getDataAccessObject();

		if (dao == null || whereObject == null || OAString.isEmpty(propertyFromWhereObject) || (params != null && params.length > 0)
				|| max > 0) {
			QueryConverter qc = new QueryConverter(ds);
			String query = getSelectSQL(ds, qc, clazz, whereObject, propertyFromWhereObject, queryWhere, params, extraWhere, queryOrder,
										max, bDirty);

			ResultSetIterator rsi;
			if (!bDirty && dao != null) {
				rsi = new ResultSetIterator(ds, clazz, dao, query, null, max);
			} else {
				Column[] columns = qc.getSelectColumnArray(clazz);
				rsi = new ResultSetIterator(ds, clazz, columns, query, max);
			}
			rsi.setDirty(bDirty);
			return rsi;
		}

		WhereObjectSelect wos = new WhereObjectSelect(clazz, whereObject == null ? null : whereObject.getClass(), propertyFromWhereObject);
		String query = bDirty ? null : hmPreparedStatementSql.get(wos);

		if (query == null) {
			QueryConverter qc = new QueryConverter(ds);
			query = "SELECT " + qc.getSelectColumns(clazz, bDirty);
			query += " " + qc.convertToPreparedStatementSql(clazz, whereObject, propertyFromWhereObject, queryWhere, params, extraWhere,
															queryOrder);

			params = qc.getArguments();
			if (whereObject != null && (params == null || params.length == 0)) {
				return null; // null reference
			}
			if (!bDirty) {
				hmPreparedStatementSql.put(wos, query);
			}
		} else {
			OAObjectKey key = OAObjectKeyDelegate.getKey(whereObject);
			params = key.getObjectIds();
		}

		ResultSetIterator rsi;
		if (!bDirty && dao != null) {
			rsi = new ResultSetIterator(ds, clazz, dao, query, params);
		} else {
			QueryConverter qc = new QueryConverter(ds);
			Column[] columns = qc.getSelectColumnArray(clazz);
			rsi = new ResultSetIterator(ds, clazz, columns, query, params, max);
		}
		rsi.setDirty(bDirty);
		return rsi;
	}

	// 20121013
	public static OADataSourceIterator selectObject(OADataSourceJDBC ds, Class clazz, OAObjectKey key, boolean bDirty) throws Exception {
		if (ds == null) {
			return null;
		}
		if (clazz == null) {
			return null;
		}

		Table table = ds.getDatabase().getTable(clazz);
		if (table == null) {
			return null;
		}
		DataAccessObject dao = table.getDataAccessObject();

		ResultSetIterator rsi;
		if (!bDirty && dao != null) {
			String sql = hmPreparedStatementSqlx.get(clazz);
			if (sql == null) {
				sql = dao.getSelectColumns();
				sql = "SELECT " + sql;
				sql += " FROM " + table.name + " WHERE ";

				// 20211212 query columns must match same order as used by objKey properties
				OAObjectInfo oi = OAObjectInfoDelegate.getObjectInfo(clazz);
				boolean b = false;
				String[] ss = oi.getKeyProperties();
				if (ss != null) {
					for (String propName : ss) {
						if (!b) {
							b = true;
						} else {
							sql += " AND ";
						}
						Column col = table.getPropertyColumn(propName);
						sql += col.columnName + " = ?";
					}
				}
				hmPreparedStatementSqlx.put(clazz, sql);
			}
			rsi = new ResultSetIterator(ds, clazz, dao, sql, key.getObjectIds());
		} else {
			String sql = hmPreparedStatementSqlxDirty.get(clazz);
			Column[] columns = hmPreparedStatementSqlxDirtyColumns.get(clazz);

			if (sql == null) {
				QueryConverter qc = new QueryConverter(ds);
				sql = "SELECT " + qc.getSelectColumns(clazz, bDirty); // could use dao
				sql += " FROM " + table.name + " WHERE ";

				// 20211212 query columns must match same order as used by objKey properties
				OAObjectInfo oi = OAObjectInfoDelegate.getObjectInfo(clazz);
				boolean b = false;
				String[] ss = oi.getKeyProperties();
				if (ss != null) {
					for (String propName : ss) {
						if (!b) {
							b = true;
						} else {
							sql += " AND ";
						}
						Column col = table.getPropertyColumn(propName);
						sql += col.columnName + " = ?";
					}
				}
				hmPreparedStatementSqlxDirty.put(clazz, sql);
				columns = qc.getSelectColumnArray(clazz);
				hmPreparedStatementSqlxDirtyColumns.put(clazz, columns);
			}
			rsi = new ResultSetIterator(ds, clazz, columns, sql, key.getObjectIds(), 0);
		}
		rsi.setDirty(bDirty);
		return rsi;
	}

	public static String getSelectSQL(OADataSourceJDBC ds, QueryConverter qc, Class clazz,
			OAObject whereObject, String propertyFromWhereObject,
			String queryWhere, Object[] args,
			String extraWhere,
			String queryOrder, int max, boolean bDirty) {

		if (propertyFromWhereObject == null) {
			propertyFromWhereObject = "";
		}
		String query = "SELECT " + qc.getSelectColumns(clazz, bDirty);
		//was: String query = "SELECT " + getMax(ds,max) + qc.getSelectColumns(clazz, bDirty);
		query += " " + qc.convertToSql(clazz, whereObject, propertyFromWhereObject, queryWhere, args, extraWhere, queryOrder);
		return query;
	}

	public static Iterator selectPassthru(OADataSourceJDBC ds, Class clazz, String query, int max, boolean bDirty) {
		Table table = ds.getDatabase().getTable(clazz);
		if (table == null) {
			return null;
		}

		QueryConverter qc = new QueryConverter(ds);

		query = qc.getSelectColumns(clazz, bDirty) + " " + query;

		ResultSetIterator rsi;
		DataAccessObject dao = table.getDataAccessObject();
		if (!bDirty && dao != null) {
			rsi = new ResultSetIterator(ds, clazz, dao, query, null, max);
		} else {
			Column[] columns = qc.getSelectColumnArray(clazz);
			rsi = new ResultSetIterator(ds, clazz, columns, "SELECT " + query, max);
			//was: rsi = new ResultSetIterator(ds, clazz, columns, "SELECT "+getMax(ds,max)+query, max);
		}
		rsi.setDirty(bDirty);
		return rsi;
	}

	/* use statement.setMaxRows(x) instead
	private static String getMax(OADataSourceJDBC ds, int max) {
		String str = "";
		if (max > 0) {
			DBMetaData dbmd = ds.getDBMetaData();
			if (OAString.isNotEmpty(dbmd.maxString)) {
				str = OAString.convert(dbmd.maxString, "?", (max+"")) + " ";
			}
		}
		return str;
	}
	*/

	/**
	 * Note: queryWhere needs to begin with "FROM TABLENAME WHERE ..." queryOrder will be prefixed with "ORDER BY "
	 */
	public static OADataSourceIterator selectPassthru(OADataSourceJDBC ds, Class clazz, String queryWhere, String queryOrder, int max,
			boolean bDirty) {
		Table table = ds.getDatabase().getTable(clazz);
		if (table == null) {
			return null;
		}

		QueryConverter qc = new QueryConverter(ds);
		String query = qc.getSelectColumns(clazz, bDirty);
		if (queryWhere != null && queryWhere.length() > 0) {
			query += " " + queryWhere;
		}
		if (queryOrder != null && queryOrder.length() > 0) {
			query += " ORDER BY " + queryOrder;
		}

		ResultSetIterator rsi;
		DataAccessObject dao = table.getDataAccessObject();
		if (!bDirty && dao != null) {
			rsi = new ResultSetIterator(ds, clazz, dao, "SELECT " + query, null, max);
		} else {
			Column[] columns = qc.getSelectColumnArray(clazz);
			rsi = new ResultSetIterator(ds, clazz, columns, "SELECT " + query, max);
			//was: rsi = new ResultSetIterator(ds, clazz, columns, "SELECT "+getMax(ds,max)+query, max);
		}
		rsi.setDirty(bDirty);
		return rsi;
	}

	public static Object execute(OADataSourceJDBC ds, String command) {
		// LOG.fine("command="+command);
		Statement st = null;
		try {
			st = ds.getStatement(command);
			st.execute(command);
			return null;
		} catch (Exception e) {
			throw new RuntimeException("OADataSourceJDBC.execute() " + command, e);
		} finally {
			if (st != null) {
				ds.releaseStatement(st);
			}
		}
	}

	// Note: queryWhere needs to begin with "FROM TABLENAME WHERE ..."
	public static int countPassthru(OADataSourceJDBC ds, String query, int max) {
		String s = "SELECT COUNT(*) ";
		//was: String s = "SELECT "+getMax(ds, max)+"COUNT(*) ";
		if (query != null && query.length() > 0) {
			s += query;
		}
		// LOG.fine("sql="+s);
		Statement st = null;
		try {
			st = ds.getStatement(s);
			java.sql.ResultSet rs = st.executeQuery(s);
			rs.next();
			int x = rs.getInt(1);
			if (max > 0 && x > max) {
				x = max;
			}
			return x;
		} catch (Exception e) {
			throw new RuntimeException("OADataSourceJDBC.count() " + query, e);
		} finally {
			if (st != null) {
				ds.releaseStatement(st);
			}
		}

	}

	public static int count(OADataSourceJDBC ds, Class selectClass, Object whereObject, String propertyFromWhereObject, int max) {
		return count(ds, selectClass, whereObject, propertyFromWhereObject, null, null, null, max);
	}

	public static int count(OADataSourceJDBC ds, Class selectClass,
			Object whereObject, String propertyFromWhereObject,
			String queryWhere, Object[] args,
			String extraWhere,
			int max) {
		if (whereObject instanceof OAObject) {
			if (((OAObject) whereObject).getNew()) {
				return 0;
			}
		}

		if (propertyFromWhereObject == null) {
			propertyFromWhereObject = "";
		}
		QueryConverter qc = new QueryConverter(ds);
		String s = qc.convertToSql(selectClass, whereObject, propertyFromWhereObject, queryWhere, args, extraWhere, "");

		s = "SELECT COUNT(*) " + s;
		//was: s = "SELECT "+getMax(ds, max)+"COUNT(*) " + s;
		// LOG.fine("selectClass="+selectClass.getName()+", whereObject="+whereObject+", extraWhere="+extraWhere+", propertyFromWhereObject="+propertyFromWhereObject+", sql="+s);

		Statement st = null;
		try {
			st = ds.getStatement(s);
			if (max > 0) {
				st.setMaxRows(max);
			}
			java.sql.ResultSet rs = st.executeQuery(s);
			rs.next();
			int x = rs.getInt(1);
			if (max > 0 && x > max) {
				x = max;
			}
			return x;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (max > 0) {
					st.setMaxRows(0);
				}
			} catch (Exception ex) {
			}
			;
			if (st != null) {
				ds.releaseStatement(st);
			}
		}
	}

	public static int count(OADataSourceJDBC ds, Class clazz, String queryWhere, int max) {
		return count(ds, clazz, queryWhere, (Object[]) null, max);
	}

	public static int count(OADataSourceJDBC ds, Class clazz, String queryWhere, Object param, int max) {
		Object[] params = null;
		if (param != null) {
			params = new Object[] { param };
		}
		return count(ds, clazz, queryWhere, params, max);
	}

	public static int count(OADataSourceJDBC ds, Class clazz, String queryWhere, Object[] params, int max) {
		QueryConverter qc = new QueryConverter(ds);

		String s = qc.convertToSql(clazz, queryWhere, params, "");
		s = "SELECT COUNT(*) " + s;
		//was: s = "SELECT "+getMax(ds,max)+"COUNT(*) " + s;
		// LOG.fine("selectClass="+clazz.getName()+", querWhere="+queryWhere+", sql="+s);

		Statement st = null;
		try {
			st = ds.getStatement(s);
			if (max > 0) {
				st.setMaxRows(max);
			}
			java.sql.ResultSet rs = st.executeQuery(s);
			rs.next();
			int x = rs.getInt(1);
			if (max > 0 && x > max) {
				x = max;
			}
			return x;
		} catch (Exception e) {
			throw new RuntimeException("OADataSourceJDBC.count() ", e);
		} finally {
			if (max > 0) {
				try {
					st.setMaxRows(0);
				} catch (Exception ex) {
				}
				;
			}
			if (st != null) {
				ds.releaseStatement(st);
			}
		}
	}

	public static byte[] getPropertyBlobValue(OADataSourceJDBC ds, OAObject whereObject, String property) throws Exception {
		if (whereObject.getNew()) {
			return null;
		}
		if (property == null) {
			return null;
		}

		Class clazz = whereObject.getClass();
		Table table = ds.getDatabase().getTable(clazz);
		if (table == null) {
			throw new Exception("table not found for class=" + clazz + ", property=" + property);
		}
		QueryConverter qc = new QueryConverter(ds);

		Column[] cols = qc.getSelectColumnArray(clazz);
		String colName = "";
		String pkeyColName = "";
		String pkey = null;
		Column[] columns = null;
		for (Column c : cols) {
			if (property.equalsIgnoreCase(c.propertyName)) {
				colName = c.columnName;
				columns = new Column[] { c };
			} else if (c.primaryKey) {
				pkeyColName = c.columnName;
				pkey = whereObject.getPropertyAsString(c.propertyName);
			}
		}
		if (columns == null) {
			throw new Exception("column name not found for class=" + clazz + ", property=" + property);
		}
		if (pkey == null) {
			throw new Exception("pkey column not found for class=" + clazz + ", property=" + property);
		}

		String query = "SELECT " + colName;
		query += " FROM " + table.name + " WHERE " + pkeyColName + " = " + pkey;

		byte[] result = null;
		Statement statement = null;
		OATransaction trans = null;
		try {
			//trans = new OATransaction(java.sql.Connection.TRANSACTION_READ_UNCOMMITTED);
			//trans.start();

			statement = ds.getStatement(query);
			ResultSet rs = statement.executeQuery(query);
			boolean b = rs.next();
			if (!b) {
				return null;
			}

			// 20211212 postgress (bytea) did not like getBlob logic, failed on reading long (size)
			result = rs.getBytes(1);
			/*
			Blob blob = rs.getBlob(1);
			if (blob != null) {
				result = blob.getBytes(1, (int) blob.length());
			}
			*/
			rs.close();
		} finally {
			ds.releaseStatement(statement);
			//trans.commit();
		}
		return result;
	}

	/**
	 * 20180602 select Link table.
	 */
	public static ArrayList<ManyToMany> getManyToMany(OADataSourceJDBC ds, OALinkInfo linkInfo) {
		if (linkInfo == null) {
			return null;
		}

		OALinkInfo revLinkInfo = linkInfo.getReverseLinkInfo();

		if (linkInfo.getType() != OALinkInfo.MANY) {
			return null;
		}
		if (revLinkInfo.getType() != OALinkInfo.MANY) {
			return null;
		}

		Class classFrom = revLinkInfo.getToClass();
		Class classTo = linkInfo.getToClass();

		// Note: this assumes that fkeys are only one column

		DBMetaData dbmd = ds.getDBMetaData();
		Table linkTable = null;

		Table fromTable = ds.getDatabase().getTable(classFrom);
		if (fromTable == null) {
			return null;
		}
		Link[] fromTableLinks = fromTable.getLinks();
		if (fromTableLinks == null) {
			return null;
		}
		Column[] fromFKeys = null;

		for (int i = 0; i < fromTableLinks.length; i++) {
			if (!fromTableLinks[i].toTable.bLink) {
				continue;
			}
			if (!fromTableLinks[i].propertyName.equalsIgnoreCase(linkInfo.getName())) {
				continue;
			}
			linkTable = fromTableLinks[i].toTable;
			fromFKeys = fromTableLinks[i].fkeys;
			break;
		}
		if (linkTable == null) {
			return null;
		}
		if (fromFKeys == null) {
			return null;
		}

		fromTableLinks = linkTable.getLinks();
		if (fromTableLinks == null) {
			return null;
		}
		Column[] linkTableFromFKeys = null;
		for (int i = 0; i < fromTableLinks.length; i++) {
			if (fromTableLinks[i].toTable == fromTable) {
				linkTableFromFKeys = fromTableLinks[i].fkeys;
				break;
			}
		}
		if (linkTableFromFKeys == null) {
			return null;
		}

		Table toTable = ds.getDatabase().getTable(classTo);
		if (toTable == null) {
			return null;
		}
		Link[] toTableLinks = toTable.getLinks();
		if (toTableLinks == null) {
			return null;
		}
		Column[] toFKeys = null;

		for (int i = 0; i < toTableLinks.length; i++) {
			if (!toTableLinks[i].toTable.bLink) {
				continue;
			}
			if (!toTableLinks[i].propertyName.equalsIgnoreCase(revLinkInfo.getName())) {
				continue;
			}
			linkTable = toTableLinks[i].toTable;
			toFKeys = toTableLinks[i].fkeys;
			break;
		}
		if (toFKeys == null) {
			return null;
		}

		toTableLinks = linkTable.getLinks();
		if (toTableLinks == null) {
			return null;
		}
		Column[] linkTableToFKeys = null;
		for (int i = 0; i < toTableLinks.length; i++) {
			if (toTableLinks[i].toTable == toTable && linkTableFromFKeys != toTableLinks[i].fkeys) {
				linkTableToFKeys = toTableLinks[i].fkeys;
				break;
			}
		}
		if (linkTableToFKeys == null) {
			return null;
		}

		String query = "SELECT ";
		query += linkTableFromFKeys[0].columnName;
		query += ", " + linkTableToFKeys[0].columnName;
		query += " FROM " + linkTable.name;

		ArrayList<ManyToMany> al = null;
		Statement st = null;
		try {
			st = ds.getStatement(query);
			ResultSet rs = st.executeQuery(query);

			al = new ArrayList<>();
			while (rs.next()) {
				OAObjectKey ok1 = new OAObjectKey(rs.getInt(1));
				OAObjectKey ok2 = new OAObjectKey(rs.getInt(2));
				al.add(new ManyToMany(ok1, ok2));
			}
		} catch (Exception e) {
			throw new RuntimeException("OADataSourceJDBC.execute() " + query, e);
		} finally {
			if (st != null) {
				ds.releaseStatement(st);
			}
		}
		return al;
	}

}
