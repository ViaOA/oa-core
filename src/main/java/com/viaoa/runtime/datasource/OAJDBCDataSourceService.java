package com.viaoa.runtime.datasource;

import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.jdbc.OADataSourceJDBC;
import com.viaoa.runtime.OADataSourceImpl;
import com.viaoa.runtime.OARuntime;

public class OAJDBCDataSourceService {
	private Logger LOG = Logger.getLogger(OAJDBCDataSourceService.class.getName());

	public OAJDBCDataSourceService() {
	}
	
	protected OADataSource[] getDataSources() {
		OADataSourceImpl ds = (OADataSourceImpl) OARuntime.datasources();
		return ds.getDataSourceService().getDataSources();
	}
	
	/**
	 * Returns the first registered {@link OADataSourceJDBC} instance.
	 * Iterates through all registered DataSources and returns the first one
	 * that is an instance of {@code OADataSourceJDBC}.
	 *
	 * @return the matching JDBC DataSource, or null if none exist
	 * @throws Exception if lookup fails
	 */
	public OADataSourceJDBC getJDBCDataSource() throws Exception {
		OADataSource[] dss = getDataSources();
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
	
	/**
	 * Retrieves a JDBC {@link Connection} from the first registered
	 * {@link OADataSourceJDBC}. Iterates through all DataSources and returns
	 * the connection obtained from the first JDBC-based DataSource.
	 *
	 * @return a JDBC connection, or null if no JDBC DataSource exists
	 * @throws Exception if connection retrieval fails
	 */
	public Connection getConnection() throws Exception {
		OADataSource[] dss = getDataSources();
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

	/**
	 * Releases the given JDBC {@link Connection} through the first registered
	 * {@link OADataSourceJDBC}. If the connection is null or no JDBC DataSource
	 * exists, the method exits quietly.
	 *
	 * @param connection the JDBC connection to release
	 */
	public void releaseConnection(Connection connection) {
		if (connection == null) {
			return;
		}
		OADataSource[] dss = getDataSources();
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

	/**
	 * Obtains a default JDBC {@link Statement} from the first registered
	 * {@link OADataSourceJDBC}. Uses an empty message string when requesting
	 * the statement.
	 *
	 * @return a JDBC Statement, or null if no JDBC DataSource exists
	 * @throws Exception if statement creation fails
	 */
	public Statement getStatement() throws Exception {
		OADataSource[] dss = getDataSources();
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

	/**
	 * Obtains a JDBC {@link Statement} from the first registered
	 * {@link OADataSourceJDBC}, using the provided diagnostic message.
	 *
	 * @param msg optional text for logging or diagnostics
	 * @return a JDBC Statement, or null if none exist
	 * @throws Exception if statement creation fails
	 */
	public Statement getStatement(String msg) throws Exception {
		OADataSource[] dss = getDataSources();
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

	/**
	 * Retrieves a batch-capable JDBC {@link Statement} from the first
	 * registered {@link OADataSourceJDBC}. Uses an empty message string
	 * for the batch request.
	 *
	 * @return a batch JDBC Statement, or null if none exist
	 * @throws Exception if statement creation fails
	 */
	public Statement getBatchStatement() throws Exception {
		OADataSource[] dss = getDataSources();
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

	/**
	 * Retrieves a batch-capable JDBC {@link Statement} using the provided
	 * diagnostic message. Searches the registered DataSources and delegates
	 * to the first {@link OADataSourceJDBC}.
	 *
	 * @param msg optional text used during statement creation
	 * @return a batch-enabled Statement, or null if unavailable
	 * @throws Exception if retrieval fails
	 */
	public Statement getBatchStatement(String msg) throws Exception {
		OADataSource[] dss = getDataSources();
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

	/**
	 * Releases the supplied JDBC {@link Statement} using the first registered
	 * {@link OADataSourceJDBC}. If the statement is null or no JDBC DataSource
	 * exists, the method returns without action.
	 *
	 * @param statement the Statement to release
	 */
	public void releaseStatement(Statement statement) {
		if (statement == null) {
			return;
		}
		OADataSource[] dss = getDataSources();
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
