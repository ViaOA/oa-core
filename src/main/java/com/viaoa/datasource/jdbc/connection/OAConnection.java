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
import java.util.Map.Entry;
import java.util.Vector;
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

	protected Connection connection;
	protected Vector<Pool> vecStatement = new Vector<Pool>(5, 5);
	protected Vector<PreparedStatement> vecUsedPreparedStatement = new Vector<PreparedStatement>(5, 5);
	protected Vector<PreparedStatement> vecUsedBatchPreparedStatement = new Vector<PreparedStatement>(5, 5);
	protected volatile boolean bAvailable;
	protected volatile boolean bGettingStatement;

	private ConcurrentHashMap<String, ArrayList<PreparedStatement>> hmSqlToPreparedStatements = new ConcurrentHashMap<String, ArrayList<PreparedStatement>>();
	private ConcurrentHashMap<PreparedStatement, String> hmPreparedStatementToSql = new ConcurrentHashMap<PreparedStatement, String>();

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
		final boolean bIsForBatch = tran != null && tran.getBatchUpdate();
		return bIsForBatch;
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

		if (bIsAllowingBatch && !bBatchUpdate) {
			executeAnyBatches();
		}

		synchronized (vecStatement) {
			int x = vecStatement.size();
			for (int i = 0; i < x; i++) {
				Pool pool = (Pool) vecStatement.elementAt(i);
				if (pool.used) {
					if (!bBatchUpdate) {
						continue;
					}
				}
				if (pool.statement.isClosed()) {
					vecStatement.remove(i);
					i--;
					x--;
					continue;
				}
				pool.used = true;
				pool.bIsForBatch = bBatchUpdate;
				bGettingStatement = false;
				return pool.statement;
			}
		}

		statement = connection.createStatement();
		cntCreateStatement++;

		Pool pool = new Pool(statement, true, message);
		pool.bIsForBatch = bBatchUpdate;
		synchronized (vecStatement) {
			vecStatement.addElement(pool);
			bGettingStatement = false;
		}

		if (vecStatement.size() > 20) {
			LOG.warning("StatementPool is getting large, current=" + vecStatement.size());
		}

		return statement;
	}

	/** returns true if found */
	public boolean releaseStatement(Statement statement) {

		boolean bResult = false;
		synchronized (vecStatement) {
			int x = vecStatement.size();
			for (int i = 0; i < x; i++) {
				Pool pool = (Pool) vecStatement.elementAt(i);
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
					if (vecStatement.size() < 10) {
						break;
					}
					if (!statement.isClosed()) {
						statement.close();
					}
				} catch (Exception e) {
					LOG.log(Level.WARNING, "Exception releasing statement", e);
				}
				vecStatement.remove(i);
				break;
			}
		}
		if (bResult) {
			cntReleaseStatement++;
		}
		return bResult;
	}

	protected void executeAnyBatches() throws SQLException {
		for (Pool pool : vecStatement) {
			if (pool.used && pool.bIsForBatch) {
				pool.statement.executeBatch();
				pool.statement.clearBatch();
				pool.used = false;
				pool.bIsForBatch = false;
			}
		}
		for (PreparedStatement ps : hmPreparedStatementToSql.keySet()) {
			if (vecUsedBatchPreparedStatement.contains(ps)) {
				ps.executeBatch();
				ps.clearBatch();
				vecUsedPreparedStatement.remove(ps);
				vecUsedBatchPreparedStatement.remove(ps);
			}
		}
	}

	protected void clearAnyBatches() throws SQLException {
		for (Pool pool : vecStatement) {
			if (pool.used && pool.bIsForBatch) {
				pool.statement.clearBatch();
				pool.used = false;
				pool.bIsForBatch = false;
			}
		}
		for (PreparedStatement ps : hmPreparedStatementToSql.keySet()) {
			if (vecUsedBatchPreparedStatement.contains(ps)) {
				ps.executeBatch();
				vecUsedPreparedStatement.remove(ps);
				vecUsedBatchPreparedStatement.remove(ps);
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
			executeAnyBatches();
		}

		ArrayList<PreparedStatement> alPreparedStatement;
		synchronized (vecUsedPreparedStatement) {
			cntGetPreparedStatement++;
			alPreparedStatement = hmSqlToPreparedStatements.get(sql);
			if (alPreparedStatement == null) {
				alPreparedStatement = new ArrayList<PreparedStatement>();
				hmSqlToPreparedStatements.put(sql, alPreparedStatement);
			} else {
				for (PreparedStatement ps : alPreparedStatement) {
					if (!vecUsedPreparedStatement.contains(ps)) {
						vecUsedPreparedStatement.addElement(ps);
						bGettingStatement = false;
						if (!bBatchUpdate) {
							return ps;
						}
					}
					if (bBatchUpdate) {
						bGettingStatement = false;
						if (!vecUsedBatchPreparedStatement.contains(ps)) {
							vecUsedBatchPreparedStatement.addElement(ps);
						}
						return ps;
					}
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

		synchronized (vecUsedPreparedStatement) {
			vecUsedPreparedStatement.addElement(ps);
			if (bBatchUpdate) {
				vecUsedBatchPreparedStatement.addElement(ps);
			}
			bGettingStatement = false;
			alPreparedStatement.add(ps);
			hmPreparedStatementToSql.put(ps, sql);
		}
		return ps;
	}

	public boolean releasePreparedStatement(PreparedStatement ps, boolean bCanBeReused) {
		boolean bFound = false;
		synchronized (vecUsedPreparedStatement) {
			int x = vecUsedPreparedStatement.size();
			for (int i = 0; i < x; i++) {
				PreparedStatement ps2 = vecUsedPreparedStatement.elementAt(i);
				if (ps2 != ps) {
					continue;
				}

				if (vecUsedBatchPreparedStatement.contains(ps)) {
					try {
						ps.clearBatch();
					} catch (Exception e) {
					}
					vecUsedBatchPreparedStatement.remove(ps);
				}

				vecUsedPreparedStatement.removeElementAt(i);
				bFound = true;
				break;
			}
		}

		synchronized (vecUsedPreparedStatement) {
			// see if the ps can be closed and removed from cache.
			String sql = hmPreparedStatementToSql.get(ps);
			if (sql != null) {
				ArrayList<PreparedStatement> al = hmSqlToPreparedStatements.get(sql);

				if (al != null && ((bFound && !bCanBeReused) || al.size() > 5)) {
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
		int x = vecUsedPreparedStatement.size();
		x += getCurrentlyUsedStatementCount();
		if (bGettingStatement) {
			x++;
		}
		return x;
	}

	protected int getCurrentlyUsedStatementCount() {
		int totalUsed = 0;
		;
		synchronized (vecStatement) {
			int x = vecStatement.size();
			for (int i = 0; i < x; i++) {
				Pool pool = (Pool) vecStatement.elementAt(i);
				if (pool.used) {
					totalUsed++;
				}
			}
		}
		return totalUsed;
	}

	public void getInfo(Vector vec) {
		try {
			if (connection.isClosed()) {
				vec.addElement("   Connection is closed");
			}
		} catch (Exception e) {
		}
		synchronized (vecStatement) {
			int x = vecStatement.size();
			for (int i = 0; i < x; i++) {
				Pool pool = (Pool) vecStatement.elementAt(i);
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
		for (Entry<String, ArrayList<PreparedStatement>> entry : hmSqlToPreparedStatements.entrySet()) {
			i += entry.getValue().size();
		}
		return i;
	}
}
