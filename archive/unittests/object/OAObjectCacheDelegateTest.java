package com.viaoa.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.viaoa.OAUnitTest;
import com.viaoa.cache.OAObjectCacheListener;
import com.viaoa.callback.OACallback;
import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.autonumber.OADataSourceAuto;
import com.viaoa.hub.Hub;
import com.viaoa.metadata.OAObjectInfo;

import test.xice.tsac3.model.oa.Server;
import test.xice.tsac3.model.oa.ServerType;
import test.xice.tsac3.model.oa.Silo;
import test.xice.tsac3.model.oa.SiloType;
import test.xice.tsac3.model.oa.propertypath.SiloPP;

public class OAObjectCacheDelegateTest extends OAUnitTest {

	@Test
	public void findTest() {
		reset();
		Silo silo = new Silo();

		SiloType siloType = new SiloType();

		ServerType serverType = new ServerType();
		serverType.setCode("1");

		Server server = new Server();

		server.setServerType(serverType);

		silo.getServers().add(server);

		Silo silox = (Silo) OAObjectCacheDelegate.find(Silo.class, SiloPP.servers().serverType().code(), "1");
		assertTrue(silox != null);

		serverType.setCode("2");
		silox = (Silo) OAObjectCacheDelegate.find(Silo.class, SiloPP.servers().serverType().code(), "1");
		assertTrue(silox == null);

		silox = (Silo) OAObjectCacheDelegate.find(Silo.class, SiloPP.servers().serverType().code(), "2");
		assertTrue(silox != null);
	}

	@Test
	public void selectAllHubTest() {
		reset();
		getDataSource();

		Hub<Server> hub = new Hub<Server>(Server.class);
		Hub[] hubs = OAObjectCacheDelegate.getSelectAllHubs(Server.class);
		assertNull(hubs);

		hub.select();
		hubs = OAObjectCacheDelegate.getSelectAllHubs(Server.class);
		assertTrue(hubs != null && hubs.length == 1);
		Hub h = hubs[0];

		OAObjectCacheDelegate.removeSelectAllHub(h);
		hubs = OAObjectCacheDelegate.getSelectAllHubs(Server.class);
		assertNull(hubs);

		reset();
	}

	private volatile int cnt1;
	private volatile int cnt2;

	@Test
	public void listenerTest() throws Exception {
		reset();
		cnt1 = 0;
		cnt2 = 0;
		OAObjectCacheListener hl = new OAObjectCacheListener() {
			@Override
			public void afterPropertyChange(OAObject obj, String propertyName, Object oldValue, Object newValue) {
				cnt2++;
			}

			@Override
			public void afterAdd(OAObject obj) {
				cnt1++;
			}

			@Override
			public void afterAdd(Hub hub, OAObject obj) {
			}

			@Override
			public void afterRemove(Hub hub, OAObject obj) {
			}

			@Override
			public void afterLoad(OAObject obj) {
				// TODO Auto-generated method stub

			}
		};
		OAObjectCacheDelegate.addListener(Server.class, hl);

		OAObjectCacheListener[] hls = OAObjectCacheDelegate.getListeners(Server.class);
		assertTrue(hls != null && hls.length == 1 && hls[0] == hl);

		Hub<Server> hub = new Hub<Server>(Server.class);
		assertEquals(cnt1, 0);
		Server server = new Server();
		hub.add(server);
		for (int i = 0; i < 3; i++) {
			if (cnt1 == 1) {
				break;
			}
			Thread.sleep(25);
		}
		assertEquals(1, cnt1);

		cnt2 = 0;
		server.setHostName("x.z");
		assertEquals(cnt2, 1);

		OAObjectCacheDelegate.removeListener(Server.class, hl);
		hls = OAObjectCacheDelegate.getListeners(Server.class);
		assertTrue(hls == null || hls.length == 0);

		server = new Server();
		hub.add(server);
		assertEquals(cnt1, 1);
	}

	@Test
	public void removeAllTest() {
		reset();
		Server server = new Server();
		server.setId(4);

		Object objx = OAObjectCacheDelegate.get(Server.class, 4);
		assertTrue(objx != null);

		OAObjectCacheDelegate.removeAllObjects();
		objx = OAObjectCacheDelegate.get(Server.class, 4);
		assertTrue(objx == null);
	}

	@Test
	public void callbackTest() {
		reset();
		cnt1 = 0;
		cnt2 = 0;
		Server server = new Server();
		server.setId(4);

		OACallback cb = new OACallback() {
			@Override
			public boolean updateObject(Object obj) {
				cnt1++;
				return true;
			}
		};

		assertEquals(0, cnt1);
		OAObjectCacheDelegate.callback(cb);
		assertEquals(1, cnt1);
	}

	@Test
	public void getObjectTest() {
		reset();
		Server server = new Server();

		Object objx = OAObjectCacheDelegate.getObject(Server.class, server);
		assertEquals(server, objx);

		server.setId(4);
		objx = OAObjectCacheDelegate.getObject(Server.class, 4);
		assertEquals(server, objx);

		OAObjectKey key = new OAObjectKey(4);
		objx = OAObjectCacheDelegate.getObject(Server.class, key);
		assertEquals(server, objx);

		server.setId(5);
		objx = OAObjectCacheDelegate.getObject(Server.class, 4);
		assertNull(objx);
		objx = OAObjectCacheDelegate.getObject(Server.class, 5);
		assertEquals(server, objx);

		key = new OAObjectKey(4);
		objx = OAObjectCacheDelegate.getObject(Server.class, key);
		assertNull(objx);

		key = new OAObjectKey(5);
		objx = OAObjectCacheDelegate.getObject(Server.class, key);
		assertEquals(server, objx);
	}

	@Test
	public void findNextTest() {
		reset();
		Silo silo = new Silo();

		SiloType siloType = new SiloType();

		ServerType serverType = new ServerType();
		serverType.setCode("1");

		Server server = new Server();
		server.setServerType(serverType);
		silo.getServers().add(server);

		Silo silox = (Silo) OAObjectCacheDelegate.findNext(null, Silo.class, SiloPP.servers().serverType().code(), "1");
		assertTrue(silox != null);

		silox = (Silo) OAObjectCacheDelegate.findNext(silox, Silo.class, SiloPP.servers().serverType().code(), "1");
		assertNull(silox);
	}

	@Test
	public void refreshTest() {
		reset(false);
		OAObjectCacheDelegate.clearCache(Server.class);
		OAObjectDelegate.setNextGuid(0);

		final Server server = new Server();

		dsAuto = new OADataSourceAuto(false) {
			@Override
			public Object getObject(OAObjectInfo oi, Class clazz, OAObjectKey key, boolean bDirty) {
				server.setHostName("worked");
				return server;
			}
		};

		OADataSource[] dss = OADataSource.getDataSources();

		OAObjectCacheDelegate.refresh(Server.class);
		assertEquals("worked", server.getHostName());

		dsAuto.close();
	}

	@Test
	public void concurrentTest() {
		reset();

		final CountDownLatch cdLatch = new CountDownLatch(10);

		for (int i = 0; i < 10; i++) {
			Thread t = new TestThread(i + 1, cdLatch);
			t.start();
		}
		try {
			cdLatch.await(10, TimeUnit.SECONDS);
		} catch (Exception e) {
		}
	}

	class TestThread extends Thread {
		int id;
		CountDownLatch cdLatch;

		public TestThread(int id, CountDownLatch cdLatch) {
			this.cdLatch = cdLatch;
			this.id = id;
		}

		public void run() {
			int max = 150;
			int rootId = id * max;
			for (int i = 0; i < max; i++) {
				test(rootId + i);
			}
			cdLatch.countDown();
		}
	}

	private void test(int id) {
		Server server = new Server();
		int idOrig = server.getId();
		OAObjectKey okOrig = server.getObjectKey();

		Server serverx = (Server) OAObjectCacheDelegate.get(server);
		assertEquals(server, serverx);

		serverx = (Server) OAObjectCacheDelegate.get(Server.class, idOrig);
		assertEquals(server, serverx);

		serverx = (Server) OAObjectCacheDelegate.get(Server.class, okOrig);
		assertEquals(server, serverx);
		
		
		server.setId(id);
		serverx = (Server) OAObjectCacheDelegate.get(server);
		assertEquals(server, serverx);

		serverx = (Server) OAObjectCacheDelegate.get(Server.class, id);
		assertEquals(server, serverx);
		delay(2);

		OAObjectCacheDelegate.removeObject(server);
		serverx = (Server) OAObjectCacheDelegate.get(server);
		assertNull(serverx);

		OAObjectCacheDelegate.add(server, true, false);
		serverx = (Server) OAObjectCacheDelegate.get(server);
		assertEquals(server, serverx);

		OAObjectCacheDelegate.removeObject(server);
		serverx = (Server) OAObjectCacheDelegate.get(server);
		assertNull(serverx);
	}

	@Test
	public void cacheGetTest() {
		reset(true); 

		Server server = new Server(); 
		Server serverx = OAObjectCacheDelegate.get(Server.class, server.getId());
		assertEquals(server, serverx);

		serverx = OAObjectCacheDelegate.get(Server.class, server.getObjectKey());
		assertEquals(server, serverx);
	}

	@Test
	public void testGetObjectWhenIdIsChanged() {
		reset(false); // no ds, no auto assign Id
		OAObjectKey ok;
		Server server = new Server(); // no id
		long guid = server.getGuid();
		
		Server serverx;

		serverx = OAObjectCacheDelegate.get(Server.class, server.getObjectKey()); // key only has guid
		assertEquals(server, serverx);

		serverx = OAObjectCacheDelegate.get(Server.class, 0);
		assertNull(serverx);
		
		assertEquals(server.getGuid(), server.getObjectKey().getGuid());

		
		
		server.setId(7777);
		assertEquals(7777, server.getId());
		ok = server.getObjectKey();
		assertEquals(server.getGuid(), ok.getGuid());
		assertEquals(1, ok.getObjectIds().length);
		assertEquals(server.getId(), ok.getObjectIds()[0]);
		assertEquals(server.getGuid(), ok.getGuid());
		
		serverx = OAObjectCacheDelegate.get(Server.class, ok);
		assertEquals(serverx, server);

		ok = new OAObjectKey(null, server.getObjectKey().getGuid());
		serverx = OAObjectCacheDelegate.get(Server.class, ok);
		assertEquals(serverx, server);

		ok = new OAObjectKey(new Object[] { 7777 }, 0L);
		serverx = OAObjectCacheDelegate.get(Server.class, ok);
		assertEquals(serverx, server);
		
		
		server.setId(0); // unset
		assertEquals(0, server.getId());
		
		ok = new OAObjectKey(new Object[] { 7777 }, 0L);
		serverx = OAObjectCacheDelegate.get(Server.class, ok);
		assertNull(serverx);
		
		ok = new OAObjectKey(new Object[] { 0 }, 0L);
		serverx = OAObjectCacheDelegate.get(Server.class, ok);
		assertEquals(serverx, server);
		
	}

}
