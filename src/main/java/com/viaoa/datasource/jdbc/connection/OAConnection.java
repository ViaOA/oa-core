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
package com.viaoa.datasource.jdbc.connection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.transaction.OATransaction;

/**
 * Used to <i>wrap</i> functionality around a java.sql.Connection to offer Statement and PreparedStatement Pooling. Mostly used internally
 * by ConnectionPool.
 */
public class OAConnection {
	private static Logger LOG = Logger.getLogger(OAConnection.class.getName());

	protected final Connection connection;
	protected final List<Pool> alStatement = new ArrayList<>();
	protected final List<PreparedStatement> alUsedPreparedStatement = new ArrayList<>();
	protected final List<PreparedStatement> alUsedBatchPreparedStatement = new ArrayList<PreparedStatement>();
	protected volatile boolean bAvailable;
	protected volatile boolean bGettingStatement;

	private final Map<String, List<PreparedStatement>> hmSqlToPreparedStatements = new ConcurrentHashMap<>();
	private final Map<PreparedStatement, String> hmPreparedStatementToSql = new ConcurrentHashMap<PreparedStatement, String>();

	volatile int cntGetStatement;
	volatile int cntCreateStatement;
	volatile int cntReleaseStatement;

	volatile int cntGetPreparedStatement;
	volatile int cntCreatePreparedStatement;
	volatile int cntReleasePreparedStatement;

	public OAConnection(Connection con) {
		connection = con;
	}

	public boolean isAllowingBatch() {
		final OATransaction tran = OAThreadLocalDelegate.getTransaction();
		final boolean bIsForBatch = tran != null && tran.getUseBatch();
		return bIsForBatch;
	}

	public Connection getConnection() {
		return connection;
	}

	/**
	 * Must be in an OATransaction that has allowBatch=true
	 */
	public Statement getBatchStatement(String message) throws SQLException {
		Statement st = _getStatement(message, true);
		return st;
	}

	public Statement getStatement(String message) throws SQLException {
		Statement st = _getStatement(message, false);
		return st;
	}

	private Statement _getStatement(final String message, final boolean bBatchUpdate) throws SQLException {
		Statement statement = null;
		cntGetStatement++;

		final boolean bIsAllowingBatch = isAllowingBatch();
		if (bBatchUpdate && !bIsAllowingBatch) {
			return null;
		}

		if (!bBatchUpdate && bIsAllowingBatch) {
			executeOpenBatches();
		}

		synchronized (alStatement) {
			int x = alStatement.size();
			for (int i = 0; i < x; i++) {
				Pool pool = (Pool) alStatement.get(i);
				if (pool.used) {
					if (!bBatchUpdate) {
						continue;
					}
				}
				if (pool.statement.isClosed()) {
					alStatement.remove(i);
					i--;
					x--;
					continue;
				}
				pool.used = true;
				pool.bIsForBatch = bBatchUpdate;
				bGettingStatement = false;
				pool.message = message;
				return pool.statement;
			}
		}

		statement = connection.createStatement();
		cntCreateStatement++;

		Pool pool = new Pool(statement, true, message);
		pool.bIsForBatch = bBatchUpdate;
		synchronized (alStatement) {
			alStatement.add(pool);
			bGettingStatement = false;
		}

		if (alStatement.size() > 20) {
			LOG.warning("StatementPool is getting large, current=" + alStatement.size());
		}

		return statement;
	}

	/** returns true if found */
	public boolean releaseStatement(Statement statement) {

		boolean bResult = false;
		synchronized (alStatement) {
			int x = alStatement.size();
			for (int i = 0; i < x; i++) {
				Pool pool = (Pool) alStatement.get(i);
				if (pool.statement != statement) {
					continue;
				}

				try {
					if (pool.bIsForBatch) {
						pool.statement.clearBatch();
						pool.bIsForBatch = false;
					}

					pool.used = false;
					bResult = true;
					if (alStatement.size() < 10) {
						break;
					}
					if (!statement.isClosed()) {
						statement.close();
					}
				} catch (Exception e) {
					LOG.log(Level.WARNING, "Exception releasing statement", e);
				}
				alStatement.remove(i);
				break;
			}
		}
		if (bResult) {
			cntReleaseStatement++;
		}
		return bResult;
	}

	protected void executeOpenBatches() throws SQLException {
		for (Pool pool : alStatement) {
			if (pool.used && pool.bIsForBatch) {
				pool.statement.executeBatch();
				releaseStatement(pool.statement);
			}
		}
		for (PreparedStatement ps : hmPreparedStatementToSql.keySet()) {
			if (alUsedBatchPreparedStatement.contains(ps)) {
				ps.executeBatch();
				releasePreparedStatement(ps, true);
			}
		}
	}

	protected void clearOpenBatches() throws SQLException {
		for (Pool pool : alStatement) {
			if (pool.used && pool.bIsForBatch) {
				releaseStatement(pool.statement);
			}
		}
		for (PreparedStatement ps : hmPreparedStatementToSql.keySet()) {
			if (alUsedBatchPreparedStatement.contains(ps)) {
				releasePreparedStatement(ps, true);
			}
		}
	}

	/**
	 * Must be in an OATransaction that has allowBatch=true
	 */
	public PreparedStatement getBatchPreparedStatement(String sql) throws SQLException {
		return _getPreparedStatement(sql, false, true);
	}

	public PreparedStatement getPreparedStatement(String sql, boolean bHasAutoGenerated) throws SQLException {
		return _getPreparedStatement(sql, bHasAutoGenerated, false);
	}

	private PreparedStatement _getPreparedStatement(final String sql, final boolean bHasAutoGenerated, final boolean bBatchUpdate)
			throws SQLException {

		final boolean bIsAllowingBatch = isAllowingBatch();
		if (bBatchUpdate && (!bIsAllowingBatch || bHasAutoGenerated)) {
			return null;
		}

		if (bIsAllowingBatch && !bBatchUpdate) {
			executeOpenBatches();
		}

		List<PreparedStatement> alPreparedStatement;
		synchronized (alUsedPreparedStatement) {
			cntGetPreparedStatement++;
			alPreparedStatement = hmSqlToPreparedStatements.computeIfAbsent(sql, k -> 
				{
					ArrayList<PreparedStatement> al = new ArrayList<PreparedStatement>();
					return al;
				}
			);
			
			
			for (PreparedStatement ps : alPreparedStatement) {
				if (!alUsedPreparedStatement.contains(ps)) {
					alUsedPreparedStatement.add(ps);
					bGettingStatement = false;
					if (!bBatchUpdate) {
						return ps;
					}
				}
				if (bBatchUpdate) {
					bGettingStatement = false;
					if (!alUsedBatchPreparedStatement.contains(ps)) {
						alUsedBatchPreparedStatement.add(ps);
					}
					return ps;
				}
			}
		}

		PreparedStatement ps;
		if (bHasAutoGenerated) {
			ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		} else {
			ps = connection.prepareStatement(sql);
		}
		cntCreatePreparedStatement++;

		synchronized (alUsedPreparedStatement) {
			alUsedPreparedStatement.add(ps);
			if (bBatchUpdate) {
				alUsedBatchPreparedStatement.add(ps);
			}
			bGettingStatement = false;
			alPreparedStatement.add(ps);
			hmPreparedStatementToSql.put(ps, sql);
		}
		return ps;
	}

	public boolean releasePreparedStatement(PreparedStatement ps, boolean bCanBeReused) {
		boolean bFound = false;
		synchronized (alUsedPreparedStatement) {
			int x = alUsedPreparedStatement.size();
			for (int i = 0; i < x; i++) {
				PreparedStatement ps2 = alUsedPreparedStatement.get(i);
				if (ps2 != ps) {
					continue;
				}

				if (alUsedBatchPreparedStatement.contains(ps)) {
					try {
						ps.clearBatch();
					} catch (Exception e) {
					}
					alUsedBatchPreparedStatement.remove(ps);
				}

				alUsedPreparedStatement.remove(i);
				bFound = true;
				break;
			}
		}

		synchronized (alUsedPreparedStatement) {
			// see if the ps can be closed and removed from cache.
			String sql = hmPreparedStatementToSql.get(ps);
			if (sql != null) {
				List<PreparedStatement> al = hmSqlToPreparedStatements.get(sql);

				if (al != null && ((bFound && !bCanBeReused) || al.size() > 5 || hmSqlToPreparedStatements.size() > 25)) {
					try {
						if (!ps.isClosed()) {
							ps.close();
						}
					} catch (Exception e) {
					}
					al.remove(ps);
					if (al.size() == 0) {
						hmSqlToPreparedStatements.remove(sql);
					}
					hmPreparedStatementToSql.remove(ps);
				}
			}
		}
		return bFound;
	}

	public int getTotalUsed() {
		int x = alUsedPreparedStatement.size();
		x += getCurrentlyUsedStatementCount();
		if (bGettingStatement) {
			x++;
		}
		return x;
	}

	protected int getCurrentlyUsedStatementCount() {
		int totalUsed = 0;
		;
		synchronized (alStatement) {
			int x = alStatement.size();
			for (int i = 0; i < x; i++) {
				Pool pool = alStatement.get(i);
				if (pool.used) {
					totalUsed++;
				}
			}
		}
		return totalUsed;
	}

	public void getInfo(List vec) {
		try {
			if (connection.isClosed()) {
				vec.add("   Connection is closed");
			}
		} catch (Exception e) {
		}
		synchronized (alStatement) {
			int x = alStatement.size();
			for (int i = 0; i < x; i++) {
				Pool pool = alStatement.get(i);
				if (pool.used) {
					// vec.addElement("  "+i+") "+pool.message);
				}
			}
		}
		/*
		vec.add("   GetStatement count="+cntGetStatement);
		vec.add("   CreateStatement count="+cntCreateStatement);
		vec.add("   ReleaseStatement count="+cntReleaseStatement);
		vec.add("   GetPreparedStatement count="+cntGetPreparedStatement);
		vec.add("   CreatePreparedStatement count="+cntCreatePreparedStatement);
		vec.add("   ReleasePreparedStatement count="+cntReleasePreparedStatement);
		*/
	}

	/**
	 * internal class used by Connection to get a list of Statement objects
	 */
	class Pool {
		Statement statement;
		boolean used;
		String message;
		boolean bIsForBatch;

		public Pool(Statement s, boolean b, String message) {
			statement = s;
			used = b;
			this.message = message;
		}
	}

	public int getTotalPreparedStatements() {
		int i = 0;
		for (Entry<String, List<PreparedStatement>> entry : hmSqlToPreparedStatements.entrySet()) {
			i += entry.getValue().size();
		}
		return i;
	}
}
