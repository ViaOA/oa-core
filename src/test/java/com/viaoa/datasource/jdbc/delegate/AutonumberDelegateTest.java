package com.viaoa.datasource.jdbc.delegate;

import static org.junit.Assert.assertNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import org.junit.Test;

import com.viaoa.OAUnitTest;
import com.viaoa.datasource.jdbc.db.Column;
import com.viaoa.datasource.jdbc.db.Table;
import com.viaoa.datasource.jdbc.delegate.AutonumberDelegate;

import test.hifive.model.oa.Employee;

public class AutonumberDelegateTest extends OAUnitTest {

	private final int maxThreads = 5;
	private final CountDownLatch countDownLatch = new CountDownLatch(maxThreads);
	private final CyclicBarrier barrier = new CyclicBarrier(maxThreads);
	private test.hifive.DataSource dataSource;
	private final ConcurrentHashMap<Integer, Integer> hmInt = new ConcurrentHashMap<Integer, Integer>();
	private Table table;
	private Column column;

	@Test
	public void test() throws Exception {
		dataSource = new test.hifive.DataSource();
		table = dataSource.getDatabase().getTable(Employee.class);
		column = table.getPropertyColumn("id");

		for (int i = 0; i < maxThreads; i++) {
			Thread t = new Thread() {
				@Override
				public void run() {
					try {
						barrier.await();

						for (int i = 0; i < 5000; i++) {
							_test();
						}
					} catch (Exception e) {
						e.printStackTrace();
						System.out.println("error: " + e);
					} finally {
						countDownLatch.countDown();
					}
				}
			};
			t.start();
		}
		countDownLatch.await();

	}

	private void _test() throws Exception {
		int x = AutonumberDelegate.getNextNumber(dataSource.getOADataSource(), table, column, true);

		Object obj = hmInt.put(x, x);
		assertNull(obj);
	}

}
