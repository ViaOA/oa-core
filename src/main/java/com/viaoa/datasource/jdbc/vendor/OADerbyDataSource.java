package com.viaoa.datasource.jdbc.vendor;

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
