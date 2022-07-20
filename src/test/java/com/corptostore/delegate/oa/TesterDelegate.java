// Copied from OATemplate project by OABuilder 12/04/21 10:02 PM
package com.corptostore.delegate.oa;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Statement;
import java.util.HashMap;

import com.corptostore.delegate.ModelDelegate;
import com.corptostore.model.oa.CorpToStore;
import com.corptostore.model.oa.Store;
import com.corptostore.model.oa.StoreTransmitInfo;
import com.corptostore.model.oa.Tester;
import com.corptostore.model.oa.TesterStep;
import com.corptostore.model.oa.TesterStepType;
import com.corptostore.model.oa.TesterStore;
import com.corptostore.model.oa.TransmitBatch;
import com.corptostore.model.oa.propertypath.TesterStorePP;
import com.corptostore.process.server.TransmitBatchDataGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.corptostore.delegate.oa.CorpToStoreDelegate;
import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.OASelect;
import com.viaoa.datasource.jdbc.OADataSourceJDBC;
import com.viaoa.json.OAJson;
import com.viaoa.transaction.OATransaction;
import com.viaoa.util.OADateTime;

public class TesterDelegate {

	public static TesterStep getTesterStep(final Tester tester, int type) {
		if (tester == null) {
			return null;
		}
		TesterStep ts = tester.getTesterSteps().find(TesterStepType.P_Type, type);
		return ts;
	}

	public static TesterStep getTesterStep(final Tester tester, TesterStepType.Type type) {
		if (tester == null || type == null) {
			return null;
		}
		TesterStep ts = tester.getTesterSteps().find(TesterStepType.P_Type, type.ordinal());
		return ts;
	}

	public static boolean getIsTestDone(final Tester tester) {
		if (tester == null) {
			return false;
		}

		TesterStep ts = getTesterStep(tester, TesterStepType.Type.resetEnvironment);
		if (ts != null && ts.getEnded() != null) {
			return true;
		}
		return false;
	}

	public static TesterStep getNextTesterStep(final Tester tester) {
		if (tester == null) {
			return null;
		}
		if (tester.getEnvironment() == null) {
			return null;
		}
		if (tester.getNumberOfStores() < 1) {
			return null;
		}
		if (tester.getTransmitBatchDate() == null) {
			return null;
		}
		if (tester.getAverageMessagesPerStore() < 1 && tester.getAllStoreMessages() < 1) {
			return null;
		}
		for (TesterStep ts : tester.getTesterSteps()) {
			if (ts.getStarted() == null) {
				TesterStepType tst = ts.getTesterStepType();
				if (tst != null && tst.getType() == tst.TYPE_cancel) {
					continue;
				}
			} else {
				if (ts.getEnded() != null) {
					return null;
				}
			}
			return ts;
		}
		return null;
	}

	public static String getNextTesterStepMessage(final Tester tester) {
		if (tester == null) {
			return "Tester is null";
		}
		if (tester.getEnvironment() == null) {
			return "Environment is null";
		}
		if (tester.getNumberOfStores() < 1) {
			return "Number of stores must be > 0";
		}
		if (tester.getTransmitBatchDate() == null) {
			return "Transmit Batch Date is not set";
		}
		if (tester.getAverageMessagesPerStore() < 1 && tester.getAllStoreMessages() < 1) {
			return "Number of average message per store must be > 0";
		}
		for (TesterStep ts : tester.getTesterSteps()) {
			TesterStepType tst = ts.getTesterStepType();
			if (ts.getStarted() == null) {
				if (tst != null && tst.getType() == tst.TYPE_cancel) {
					continue;
				}
			} else {
				if (ts.getEnded() != null) {
					return "Test Step " + tst.getTypeString() + " was started, but has not ended";
				}
			}
			return "Next Test Step is " + tst.getTypeString();
		}
		return null;
	}

	public static void next(final Tester tester) {
		if (tester == null) {
			return;
		}

		final TesterStep testerStep = getNextTesterStep(tester);
		if (testerStep == null) {
			tester.setConsole("Error: no next test step to perform");
			return;
		}

		testerStep.setStarted(new OADateTime());

		final TesterStepType testerStepType = testerStep.getTesterStepType();
		if (testerStepType == null) {
			tester.setConsole("Error: test step does not have a type");
			return;
		}
		tester.setConsole("Test Step is " + testerStepType.getTypeString());

		if (testerStepType.getType() == TesterStepType.TYPE_getEnvironmentInfo) {
			tester.setConsole("getting Environment Info from each node");
			tester.getEnvironment().updateCorpToStore();
			tester.setConsole("done");
			testerStep.setEnded(new OADateTime());
			return;
		}

		if (testerStepType.getType() == TesterStepType.TYPE_pauseEnvironment) {
			tester.setWasEnvironmentPaused(tester.getEnvironment().getIsAllPaused());
			tester.setConsole(tester.getEnvironment().getIsAllPaused() ? "environment was already paused" : "environment was not paused");
			tester.getEnvironment().pauseAll();
			testerStep.setEnded(new OADateTime());
			tester.setConsole("paused CorpToStore Apps, Environment=" + tester.getEnvironment().getName());
			tester.setConsole("Environment has been paused");
			return;
		}

		if (testerStepType.getType() == TesterStepType.TYPE_selectStores) {
			selectStores(tester);
			testerStep.setEnded(new OADateTime());
			return;
		}

		if (testerStepType.getType() == TesterStepType.TYPE_createTransmitBatch) {
			TransmitBatch tb = new TransmitBatch();
			tb.setTransmitBatchDate(tester.getTransmitBatchDate());
			tester.setTransmitBatch(tb);
			tester.setConsole("created Transmit Batch for " + tb.getTransmitBatchDate());
			tb.save();
			testerStep.setEnded(new OADateTime());
			return;
		}

		if (testerStepType.getType() == TesterStepType.TYPE_CreateTransmitData) {
			createTestData(tester);
			tester.setConsole("created Transmit records (test data) for Transmit Batch " + tester.getTransmitBatchDate());
			testerStep.setEnded(new OADateTime());
			return;
		}

		if (testerStepType.getType() == TesterStepType.TYPE_setBatchAsCompleted) {
			String msg = "Transmit Batch " + tester.getTransmitBatchDate() + " has been marked as completed";
			tester.setConsole(msg);
			tester.getTransmitBatch().setDataPullCompleted(new OADateTime());
			tester.getTransmitBatch().save();
			testerStep.setEnded(new OADateTime());
			return;
		}

		if (testerStepType.getType() == TesterStepType.TYPE_continueEnvironment) {
			tester.setConsole("Environment has continued (un-paused) processing");
			tester.getEnvironment().continueAll();
			tester.setConsole("CorpToStore is now processing, Environment=" + tester.getEnvironment().getName() + ", Batch Date "
					+ tester.getTransmitBatchDate());
			testerStep.setEnded(new OADateTime());
			return;
		}

		if (testerStepType.getType() == TesterStepType.TYPE_verify) {
			verify(tester);
			testerStep.setEnded(new OADateTime());
			return;
		}

		if (testerStepType.getType() == TesterStepType.TYPE_getResults) {
			getResults(tester);
			testerStep.setEnded(new OADateTime());
			return;
		}

		if (testerStepType.getType() == TesterStepType.TYPE_resetEnvironment) {
			reset(tester);

			tester.setWasEnvironmentPaused(tester.getEnvironment().getIsAllPaused());
			tester.setConsole(tester.getEnvironment().getIsAllPaused() ? "environment was already paused" : "environment was not paused");

			testerStep.setEnded(new OADateTime());
			return;
		}
	}

	public static void verify(final Tester tester) {
		//qqqqqqqqqqqq
	}

	public static void getResults(final Tester tester) {
		if (tester == null) {
			return;
		}

		TesterStep ts = getTesterStep(tester, TesterStepType.TYPE_continueEnvironment);
		if (ts != null && ts.getEnded() != null) {
			return;
		}

		try {
			if (_getResults(tester)) {

				String msg = "CorpToStore get results is done and report is available";
				tester.setConsole(msg);

				// tester.setResults("<html><body>GetResults qqqqqqqqqqqqqqqqq</body></html>");
			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			tester.setConsole("Exception: " + e.toString() + ", stacktrace: " + sw);
		}
	}

	private static boolean _getResults(final Tester tester) throws Exception {
		TesterStep ts = getTesterStep(tester, TesterStepType.TYPE_resetEnvironment);
		if (ts != null && ts.getEnded() != null) {
			tester.setConsole("error: cant get results after environment has been reset");
			return false;
		}

		ts = getTesterStep(tester, TesterStepType.TYPE_continueEnvironment);
		if (ts != null && ts.getEnded() != null) {
			tester.setConsole("error: must wait to get results after continue environment step is completed");
			return false;
		}

		//qqqqqqqqqqq gather stats from endpoints and DB (see my daily stats notes

		for (CorpToStore cts : tester.getEnvironment().getCorpToStores()) {
			String result;
			try {
				result = CorpToStoreDelegate.getStoreTransmitInfo(cts);
				ObjectMapper om = OAJson.createObjectMapper();
				HashMap hm = om.readValue(result, HashMap.class);

				hm.forEach((k, v) -> {
					int storeNumber = (Integer) k;
					TesterStore testerStore = tester.getTesterStores().find(TesterStorePP.store().storeNumber(), storeNumber);
					if (testerStore != null) {
						StoreTransmitInfo tsi = testerStore.getCalcStoreTransmitInfo();
						if (tsi == null) {
							//tsi = new StoreTransmitInfo();
							//qqqqqqqqq populate values
							//ts.setStoreTransmitInfo(tsi);
						}
					}
				});

				int xx = 4;
				xx++;
			} catch (Exception e) {
				e.printStackTrace();
				PrintWriter pw = new PrintWriter(new StringWriter());
				e.printStackTrace(pw);
				result = "Exception: " + e.getMessage() + "\n" + pw.toString();
			}
			tester.setResults(result);
		}
		boolean b = true;
		for (TesterStore testerStore : tester.getTesterStores()) {
			if (testerStore.getCalcStoreTransmitInfo() == null) {
				b = false;
				break;
			}
		}
		return true;
	}

	public static boolean cancel(final Tester tester) {
		if (tester == null) {
			return false;
		}

		final TesterStep tsCancel = getTesterStep(tester, TesterStepType.TYPE_cancel);
		final TesterStep tsResetEnv = getTesterStep(tester, TesterStepType.TYPE_resetEnvironment);
		final TesterStep tsPause = getTesterStep(tester, TesterStepType.TYPE_pauseEnvironment);
		final TesterStep tsContinue = getTesterStep(tester, TesterStepType.TYPE_continueEnvironment);

		if (tsResetEnv != null && tsResetEnv.getEnded() != null) {
			tester.setConsole("error: the environment has been already been reset");
			return false;
		}

		if (tsResetEnv != null && tsResetEnv.getStarted() != null) {
			if (tsResetEnv.getEnded() != null) {
				tester.setConsole("error: cancel has already been called and has not ended");
			}
			return false;
		}
		tester.setConsole("Cancelling Test");

		try {
			tsResetEnv.setStarted(new OADateTime());

			if (tsPause != null && tsPause.getStarted() == null) {
				tester.setConsole("Test was never started, Environment was never paused");
			} else if (tester.getTransmitBatch() != null) {
				reset(tester);
			} else {
				TesterStep tsx = getTesterStep(tester, TesterStepType.TYPE_selectStores);
				if (tsx != null && tsx.getStarted() != null) {
					resetStores(tester);
					tsx = getTesterStep(tester, TesterStepType.TYPE_resetEnvironment);
					if (tsx != null) {
						tsx.setEnded(new OADateTime());
					}
					tester.setConsole("only needed to reset stores, no Transmit Batch was created");
				} else {
					tester.setConsole("there was not a Transmit Batch Date, and no Stores that needed to be reset");
				}
			}

			// see if env needs to be continued
			if (tsPause != null && tsPause.getStarted() != null && tsContinue != null && tsContinue.getStarted() == null) {
				if (!tester.getWasEnvironmentPaused()) {
					tester.getEnvironment().continueAll();
					tester.setConsole("Environment has continued processing");
					tsContinue.setEnded(new OADateTime());
				} else {
					tester.setConsole("Environment was previously paused and it will be left as paused");
				}
			}

			tester.setConsole("Test was cancelled");
			if (tsResetEnv.getEnded() == null) {
				tsResetEnv.setEnded(new OADateTime());
			}
			tsCancel.setEnded(new OADateTime());
		} catch (Exception e) {
			tsResetEnv.setStarted(null);
			e.printStackTrace();
			PrintWriter pw = new PrintWriter(new StringWriter());
			e.printStackTrace(pw);
			tester.setConsole("Exception: " + e.getMessage() + "\n" + pw.toString());
		}
		return true;
	}

	private static void reset(final Tester tester) {
		if (tester == null) {
			return;
		}
		final TesterStep tsResetEnv = getTesterStep(tester, TesterStepType.TYPE_resetEnvironment);
		tester.setConsole("Resetting environment by removing test data");

		OADataSourceJDBC ds = (OADataSourceJDBC) OADataSource.getDataSource(TransmitBatch.class);
		Statement st = ds.getStatement("TesterDelegate.resetTester");
		try {
			tsResetEnv.setStarted(new OADateTime());
			resetTester(tester, st);
			resetStores(tester);

			tester.setConsole("Reset completed");
			tsResetEnv.setEnded(new OADateTime());

		} catch (Exception e) {
			tsResetEnv.setStarted(null);
			e.printStackTrace();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			tester.setConsole("Exception: " + e.toString() + "\n" + sw.toString());
		} finally {
			ds.releaseStatement(st);
		}
	}

	private static void resetStores(final Tester tester) {
		if (tester == null) {
			return;
		}
		for (TesterStore ts : tester.getTesterStores()) {
			Store store = ts.getStore();
			if (store == null) {
				continue;
			}
			store.setActive(ts.getWasActive());
			store.setRegistered(ts.getHoldRegisteredDate());
		}

		OATransaction trans = new OATransaction(java.sql.Connection.TRANSACTION_READ_UNCOMMITTED);
		trans.setBatchUpdate(true);
		trans.start();

		try {
			ModelDelegate.getStores().saveAll();
		} finally {
			trans.commit();
		}
	}

	private static void resetTester(final Tester tester, final Statement st) throws Exception {
		final TransmitBatch transmitBatch = tester.getTransmitBatch();

		if (transmitBatch == null) {
			return;
		}

		final String sqlTransmitBatchDate = "'" + transmitBatch.getTransmitBatchDate().toString("yyyy-MM-dd") + "'";

		tester.setConsole("Pausing environment processing");
		tester.getEnvironment().pauseAll();

		tester.setConsole("removing Tester data for batch = " + sqlTransmitBatchDate);

		String sql;

		if (transmitBatch.getDataPullCompleted() != null) {
			tester.setConsole("1: removing Send records");
			sql = "DELETE FROM message_service_send s "
					+ "WHERE EXISTS (select 1 FROM "
					+ "message_service_transmit t WHERE s.begin_transmit_id = t.transmit_id "
					+ "AND t.transmit_batch_date = " + sqlTransmitBatchDate + ")";
			tester.setConsole(sql);
			st.executeUpdate(sql);

			tester.setConsole("2: removing Batch records");
			sql = "DELETE from message_service_batch b "
					+ "where exists ("
					+ "select 1 from message_service_store_batch sb "
					+ "where sb.transmit_batch_date = " + sqlTransmitBatchDate
					+ " AND b.batch_id = sb.batch_id)";
			tester.setConsole(sql);
			st.executeUpdate(sql);

			tester.setConsole("3: removing StoreBatch records");
			sql = "Delete FROM message_service_store_batch "
					+ "where transmit_batch_date = "
					+ sqlTransmitBatchDate;
			tester.setConsole(sql);
			st.executeUpdate(sql);

			tester.setConsole("4: removing StoreTransmitBatch records");
			sql = "Delete FROM message_service_store_transmit_batch "
					+ "where transmit_batch_date = " + sqlTransmitBatchDate;
			tester.setConsole(sql);
			st.executeUpdate(sql);
		} else {
			tester.setConsole("1-4: skipped, since transmit_batch.completed = null & send records were not created.");
		}

		tester.setConsole("5: removing Transmit records");
		sql = "Delete FROM message_service_transmit "
				+ "where transmit_batch_date = " + sqlTransmitBatchDate;
		tester.setConsole(sql);
		st.executeUpdate(sql);

		tester.setConsole("6: removing TransmitBatch record");
		sql = "Delete FROM message_service_transmit_batch "
				+ "where transmit_batch_date = " + sqlTransmitBatchDate;
		tester.setConsole(sql);
		st.executeUpdate(sql);
	}

	protected static void createTestData(final Tester tester) {
		TransmitBatchDataGenerator gen = new TransmitBatchDataGenerator();

		int total = tester.getAllStoreMessages() + (tester.getNumberOfStores() * tester.getAverageMessagesPerStore());
		tester.setConsole("generating " + tester.getAllStoreMessages() + " all store messsages, total messages=" + total);
		gen.generateTransmitRecords(tester.getTransmitBatch(), tester.getAllStoreMessages(), total);

		String msg = "generated " + tester.getAllStoreMessages() + " all store messsages, total messages=" + total;
		tester.setConsole(msg);
	}

	protected static void selectStores(final Tester tester) {
		if (tester == null) {
			return;
		}
		int x = tester.getNumberOfStores();
		tester.setConsole("selecting stores to get " + x + " to make active for testing");

		OASelect<Store> sel = new OASelect<>(Store.class);
		sel.select("", Store.P_StoreNumber);

		OATransaction trans = new OATransaction(java.sql.Connection.TRANSACTION_READ_UNCOMMITTED);
		trans.setBatchUpdate(true);
		trans.start();

		try {
			for (; sel.hasMore();) {
				Store store = sel.next();
				TesterStore ts = new TesterStore();
				ts.setStore(store);

				ts.setWasActive(store.getActive());
				store.setActive(tester.getTesterStores().size() < x);

				ts.setHoldRegisteredDate(store.getRegistered());
				store.setRegistered(new OADateTime());

				tester.getTesterStores().add(ts);
				store.save();
			}
		} finally {
			trans.commit();
		}
		int cntFound = Math.min(tester.getTesterStores().size(), x);
		String msg = "found " + cntFound + " stores to be able to use for test";
		tester.setConsole(msg);
	}
}
