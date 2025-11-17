/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.datasource;

import java.sql.Connection;
import java.sql.Statement;

import com.viaoa.datasource.jdbc.OADataSourceJDBC;

/**
 * Utility class that provides convenience access to registered {@link OADataSource}
 * instances, especially JDBC‐based implementations.
 * <p>
 * {@code OADataSourceDelegate} simplifies low-level connection handling and
 * cross-DataSource coordination without exposing internal details to the caller.
 * It is primarily used by framework components that need to execute raw SQL or
 * perform lightweight diagnostics outside of the normal OAObject lifecycle.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Locate the active {@link com.viaoa.datasource.jdbc.OADataSourceJDBC}
 *       for a given model class.</li>
 *   <li>Provide helper methods for obtaining and releasing JDBC
 *       {@link java.sql.Connection} and {@link java.sql.Statement} objects.</li>
 *   <li>Serve as a central access point for hybrid or mixed persistence scenarios
 *       (e.g., direct SQL calls alongside OAObject persistence).</li>
 * </ul>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *   <li>Stateless and thread-safe.</li>
 *   <li>Delegates all lookup logic to
 *       {@link OADataSource#getDataSources()}.</li>
 *   <li>Automatically handles null or inactive DataSources safely.</li>
 * </ul>
 *
 * @see OADataSource
 * @see com.viaoa.datasource.jdbc.OADataSourceJDBC
 */
public class OADataSourceDelegate {

	public static OADataSourceJDBC getJDBCDataSource() throws Exception {
		OADataSource[] dss = OADataSource.getDataSources();
		if (dss == null) {
			return null;
		}
		for (OADataSource ds : dss) {
			if (ds instanceof OADataSourceJDBC) {
				OADataSourceJDBC jds = (OADataSourceJDBC) ds;
				return jds;
			}
		}
		return null;
	}

	public static Connection getConnection() throws Exception {
		OADataSource[] dss = OADataSource.getDataSources();
		if (dss == null) {
			return null;
		}
		for (OADataSource ds : dss) {
			if (ds instanceof OADataSourceJDBC) {
				OADataSourceJDBC jds = (OADataSourceJDBC) ds;
				return jds.getConnection();
			}
		}
		return null;
	}

	public static void releaseConnection(Connection connection) {
		if (connection == null) {
			return;
		}
		OADataSource[] dss = OADataSource.getDataSources();
		if (dss == null) {
			return;
		}
		for (OADataSource ds : dss) {
			if (ds instanceof OADataSourceJDBC) {
				OADataSourceJDBC jds = (OADataSourceJDBC) ds;
				jds.releaseConnection(connection);
				break;
			}
		}
	}

	public static Statement getStatement() throws Exception {
		OADataSource[] dss = OADataSource.getDataSources();
		if (dss == null) {
			return null;
		}
		for (OADataSource ds : dss) {
			if (ds instanceof OADataSourceJDBC) {
				OADataSourceJDBC jds = (OADataSourceJDBC) ds;
				return jds.getStatement("");
			}
		}
		return null;
	}

	public static Statement getStatement(String msg) throws Exception {
		OADataSource[] dss = OADataSource.getDataSources();
		if (dss == null) {
			return null;
		}
		for (OADataSource ds : dss) {
			if (ds instanceof OADataSourceJDBC) {
				OADataSourceJDBC jds = (OADataSourceJDBC) ds;
				return jds.getStatement(msg);
			}
		}
		return null;
	}

	public static Statement getBatchStatement() throws Exception {
		OADataSource[] dss = OADataSource.getDataSources();
		if (dss == null) {
			return null;
		}
		for (OADataSource ds : dss) {
			if (ds instanceof OADataSourceJDBC) {
				OADataSourceJDBC jds = (OADataSourceJDBC) ds;
				return jds.getBatchStatement("");
			}
		}
		return null;
	}

	public static Statement getBatchStatement(String msg) throws Exception {
		OADataSource[] dss = OADataSource.getDataSources();
		if (dss == null) {
			return null;
		}
		for (OADataSource ds : dss) {
			if (ds instanceof OADataSourceJDBC) {
				OADataSourceJDBC jds = (OADataSourceJDBC) ds;
				return jds.getBatchStatement(msg);
			}
		}
		return null;
	}

	public static void releaseStatement(Statement statement) {
		if (statement == null) {
			return;
		}
		OADataSource[] dss = OADataSource.getDataSources();
		if (dss == null) {
			return;
		}
		for (OADataSource ds : dss) {
			if (ds instanceof OADataSourceJDBC) {
				OADataSourceJDBC jds = (OADataSourceJDBC) ds;
				jds.releaseStatement(statement);
				break;
			}
		}
	}
}
