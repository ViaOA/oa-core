/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
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
import com.viaoa.runtime.OARuntime;

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

	/**
	 * Returns the first registered {@link OADataSourceJDBC} instance.
	 * Iterates through all registered DataSources and returns the first one
	 * that is an instance of {@code OADataSourceJDBC}.
	 *
	 * @return the matching JDBC DataSource, or null if none exist
	 * @throws Exception if lookup fails
	 */
	public static OADataSourceJDBC getJDBCDataSource() throws Exception {
		return OARuntime.get().dataSources().jdbc().getJDBCDataSource();
	}

	/**
	 * Retrieves a JDBC {@link Connection} from the first registered
	 * {@link OADataSourceJDBC}. Iterates through all DataSources and returns
	 * the connection obtained from the first JDBC-based DataSource.
	 *
	 * @return a JDBC connection, or null if no JDBC DataSource exists
	 * @throws Exception if connection retrieval fails
	 */
	public static Connection getConnection() throws Exception {
		return OARuntime.get().dataSources().jdbc().getConnection();
	}

	/**
	 * Releases the given JDBC {@link Connection} through the first registered
	 * {@link OADataSourceJDBC}. If the connection is null or no JDBC DataSource
	 * exists, the method exits quietly.
	 *
	 * @param connection the JDBC connection to release
	 */
	public static void releaseConnection(Connection connection) {
		OARuntime.get().dataSources().jdbc().releaseConnection(connection);
	}

	/**
	 * Obtains a default JDBC {@link Statement} from the first registered
	 * {@link OADataSourceJDBC}. Uses an empty message string when requesting
	 * the statement.
	 *
	 * @return a JDBC Statement, or null if no JDBC DataSource exists
	 * @throws Exception if statement creation fails
	 */
	public static Statement getStatement() throws Exception {
		return OARuntime.get().dataSources().jdbc().getStatement();
	}

	/**
	 * Obtains a JDBC {@link Statement} from the first registered
	 * {@link OADataSourceJDBC}, using the provided diagnostic message.
	 *
	 * @param msg optional text for logging or diagnostics
	 * @return a JDBC Statement, or null if none exist
	 * @throws Exception if statement creation fails
	 */
	public static Statement getStatement(String msg) throws Exception {
		return OARuntime.get().dataSources().jdbc().getStatement(msg);
	}

	/**
	 * Retrieves a batch-capable JDBC {@link Statement} from the first
	 * registered {@link OADataSourceJDBC}. Uses an empty message string
	 * for the batch request.
	 *
	 * @return a batch JDBC Statement, or null if none exist
	 * @throws Exception if statement creation fails
	 */
	public static Statement getBatchStatement() throws Exception {
		return OARuntime.get().dataSources().jdbc().getBatchStatement();
	}

	/**
	 * Retrieves a batch-capable JDBC {@link Statement} using the provided
	 * diagnostic message. Searches the registered DataSources and delegates
	 * to the first {@link OADataSourceJDBC}.
	 *
	 * @param msg optional text used during statement creation
	 * @return a batch-enabled Statement, or null if unavailable
	 * @throws Exception if retrieval fails
	 */
	public static Statement getBatchStatement(String msg) throws Exception {
		return OARuntime.get().dataSources().jdbc().getBatchStatement(msg);
	}

	/**
	 * Releases the supplied JDBC {@link Statement} using the first registered
	 * {@link OADataSourceJDBC}. If the statement is null or no JDBC DataSource
	 * exists, the method returns without action.
	 *
	 * @param statement the Statement to release
	 */
	public static void releaseStatement(Statement statement) {
		OARuntime.get().dataSources().jdbc().releaseStatement(statement);
	}
}
