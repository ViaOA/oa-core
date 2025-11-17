package com.viaoa.datasource.jdbc.vendor;
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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.datasource.jdbc.OADataSourceJDBC;
import com.viaoa.datasource.jdbc.db.DBMetaData;
import com.viaoa.datasource.jdbc.db.Database;
import com.viaoa.util.OALogger;
import com.viaoa.util.OAString;

/**
 * Derby-specific extension of {@link com.viaoa.datasource.jdbc.OADataSourceJDBC}
 * providing database maintenance utilities such as verification, backup,
 * rollforward recovery, and compression.
 * <p>
 * These methods rely on Derby's built-in system procedures
 * under {@code SYSCS_UTIL}, and are no-ops when connected to
 * non-Derby databases.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><b>Integrity verification:</b> Invokes
 *       {@code SYSCS_UTIL.SYSCS_CHECK_TABLE('APP', table)} on each table
 *       to validate internal consistency.</li>
 *   <li><b>Online backup:</b> Uses
 *       {@code SYSCS_UTIL.SYSCS_BACKUP_DATABASE_AND_ENABLE_LOG_ARCHIVE_MODE}
 *       for full database snapshots with roll-forward logs.</li>
 *   <li><b>Roll-forward restore:</b> Opens the database with
 *       {@code rollForwardRecoveryFrom=<backupDir>} to apply archived logs.</li>
 *   <li><b>Table compression:</b> Executes
 *       {@code SYSCS_UTIL.SYSCS_COMPRESS_TABLE('APP', table, 1)} on all tables
 *       to reclaim unused pages.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * Typically invoked through administrative utilities or maintenance scripts.
 * All operations are logged via {@link com.viaoa.util.OALogger} at FINE level.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * OADerbyDataSource ds = new OADerbyDataSource(database, dbmd);
 * ds.backup("DBBackup_2025_11_12");
 * ds.checkForCorruption();
 * ds.compress();
 * }</pre>
 *
 * @see com.viaoa.datasource.jdbc.OADataSourceJDBC
 * @see com.viaoa.datasource.jdbc.db.DBMetaData
 * @since OA 4.0
 */
public class OADerbyDataSource extends OADataSourceJDBC {
	private static Logger LOG = OALogger.getLogger(OADerbyDataSource.class);

	public OADerbyDataSource(Database database, DBMetaData dbmd) {
		super(database, dbmd);
	}

	/**
	 * @see #isDataSourceReady() for descriptions.
	 */
	public void checkForCorruption() throws Exception {
		DBMetaData dbmd = getDBMetaData();
		if (dbmd == null || dbmd.getDatabaseType() != DBMetaData.DERBY) {
			return;
		}
		LOG.fine("Starting Database verification");

		String sql;
		Statement statement = null;
		try {
			statement = getStatement("verify database");

			sql = "SELECT t.tablename from sys.sysschemas s, sys.systables t " +
					"where CAST(s.schemaname AS VARCHAR(128)) = 'APP' AND s.schemaid = t.schemaid " +
					"ORDER BY t.tablename";

			ResultSet rs = statement.executeQuery(sql);
			ArrayList<String> alTable = new ArrayList<String>();
			for (int i = 0; rs.next(); i++) {
				alTable.add(rs.getString(1));
			}
			rs.close();

			int i = 0;
			for (String tableName : alTable) {
				LOG.fine("Verifiying database table " + tableName);
				LOG.fine((++i) + ") verify " + tableName);
				try {
					sql = "SELECT t.tablename, SYSCS_UTIL.SYSCS_CHECK_TABLE('APP', t.tablename) " +
							"from sys.systables t " +
							"where CAST(t.tablename AS VARCHAR(128)) = '" + tableName + "'";
					rs = statement.executeQuery(sql);
					for (; rs.next();) {
						LOG.fine(i + ") " + rs.getString(1) + " = " + rs.getShort(2));
					}
				} catch (Exception e) {
					LOG.log(Level.WARNING, "database verification for table " + tableName + "failed", e);
					throw e;
				}
			}

			LOG.fine("Completed Database verification");
		} finally {
			releaseStatement(statement);
		}
	}

	/**
	 * This will make a backup of the live database, with rollforward support. The database will be under backupDirectory
	 *
	 * @param backupDirectory example: DB20100428
	 * @throws Exception
	 */
	public void backup(String backupDirectory) throws Exception {
		DBMetaData dbmd = getDBMetaData();
		if (dbmd == null || dbmd.getDatabaseType() != DBMetaData.DERBY) {
			return;
		}

		LOG.fine("Starting Database backup to " + backupDirectory);

		Statement statement = null;
		try {
			statement = getStatement("backup database");
			// statement.execute("call SYSCS_UTIL.SYSCS_CHECKPOINT_DATABASE()");
			// statement.execute("call SYSCS_UTIL.SYSCS_BACKUP_DATABASE('"+backupDirectory+"')");

			// create a backup, that will store rollforward log files in the current db log directory.  The '1' will delete previous log files
			String sql = "call SYSCS_UTIL.SYSCS_BACKUP_DATABASE_AND_ENABLE_LOG_ARCHIVE_MODE('" + backupDirectory + "', 1)";
			statement.execute(sql);

			// this is the commad to disable log archive.  The '1' will delete previous log files
			// SYSCS_UTIL.SYSCS_DISABLE_LOG_ARCHIVE_MODE(1)

			// use this to restore
			// connect 'jdbc:derby:wombat;rollForwardRecoveryFrom=d:/backup/wombat';

			LOG.fine("Completed Database backup to " + backupDirectory);
		} finally {
			releaseStatement(statement);
		}
	}

	/**
	 *
	 */
	public void restore(String backupDirectory) throws Exception {
		DBMetaData dbmd = getDBMetaData();
		if (dbmd == null || dbmd.getDatabaseType() != DBMetaData.DERBY) {
			return;
		}
		LOG.fine("Starting forwardRestoreBackupDatabase from " + backupDirectory);
		close();

		Class.forName(dbmd.getDriverJDBC()).newInstance();

		if (backupDirectory != null) {
			backupDirectory = backupDirectory.replace('\\', '/');
		}
		String jdbcUrl = dbmd.getUrlJDBC() + ";rollForwardRecoveryFrom=" + backupDirectory;

		String s = dbmd.getUrlJDBC();
		s = OAString.field(s, ":", OAString.dcount(s, ":"));
		jdbcUrl += "/" + s;

		/// this will open the database and perform a rollForward
		Connection connection = DriverManager.getConnection(jdbcUrl, dbmd.user, dbmd.password);
		connection.close();

		LOG.fine("Completed Database forward restore from " + backupDirectory);
		reopen(0);
	}

	public void compress() throws Exception {
		DBMetaData dbmd = getDBMetaData();
		if (dbmd == null || dbmd.getDatabaseType() != DBMetaData.DERBY) {
			return;
		}
		LOG.config("Starting Database compression");

		String sql;
		Statement statement = null;
		Connection connection = null;
		try {
			statement = getStatement("compress database");

			sql = "SELECT t.tablename from sys.sysschemas s, sys.systables t " +
					"where CAST(s.schemaname AS VARCHAR(128)) = 'APP' AND s.schemaid = t.schemaid " +
					"ORDER BY t.tablename";

			ResultSet rs = statement.executeQuery(sql);
			ArrayList<String> alTable = new ArrayList<String>();
			for (int i = 0; rs.next(); i++) {
				alTable.add(rs.getString(1));
			}
			rs.close();
			releaseStatement(statement);

			connection = getConnection();
			int i = 0;
			for (String tableName : alTable) {
				LOG.fine((++i) + ") compressing table " + tableName);
				try {
					sql = "call SYSCS_UTIL.SYSCS_COMPRESS_TABLE('APP', '" + tableName + "', 1)";
					CallableStatement cs = connection.prepareCall(sql);
					cs.execute();
					cs.close();
				} catch (Exception e) {
					LOG.log(Level.WARNING, "database compression for table " + tableName + "failed", e);
					throw e;
				}
			}

			LOG.fine("Completed Database verification");
		} finally {
			releaseConnection(connection);
		}

	}

}
