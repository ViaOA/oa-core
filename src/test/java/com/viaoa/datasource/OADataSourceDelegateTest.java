package com.viaoa.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.viaoa.OAUnitTest;

import test.xice.tsam.datasource.DataSource;

public class OADataSourceDelegateTest extends OAUnitTest {

	@Test
	public void getJDBCDataSourceTest() throws Exception {
		reset();
		assertNull(OADataSourceDelegate.getJDBCDataSource());

		DataSource ds = new DataSource();
		ds.open();

		assertNotNull(OADataSourceDelegate.getJDBCDataSource());

		assertEquals(ds.getOADataSource(), OADataSourceDelegate.getJDBCDataSource());

		ds.close();
		assertNull(OADataSourceDelegate.getJDBCDataSource());
	}

}
