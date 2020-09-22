package com.viaoa.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.viaoa.OAUnitTest;
import com.viaoa.datasource.autonumber.OADataSourceAuto;
import com.viaoa.hub.Hub;

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

		OAObjectCacheDelegate.callback(cb);
		assertEquals(2, cnt1);
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
		reset();
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

		OAObjectCacheDelegate.refresh(Server.class);
		assertEquals("worked", server.getHostName());

		dsAuto.close();
	}

	private boolean bStop;

	@Test
	public void concurrentTest() {
		reset();
		for (int i = 0; i < 10; i++) {
			Thread t = new TestThread(i + 1);
			t.start();
		}
		for (int i = 0; i < 3; i++) {
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		bStop = true;
	}

	class TestThread extends Thread {
		int id;

		public TestThread(int id) {
			this.id = id;
		}

		public void run() {
			int max = 1500;
			int rootId = id * max;
			for (int i = 0; i < max && !bStop; i++) {
				test(rootId + i);
			}
		}
	}

	private void test(int id) {
		Server server = new Server();
		Server serverx = (Server) OAObjectCacheDelegate.get(server);
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
		reset(false); // no ds, no auto assign Id

		Server server = new Server(); // no id
		Server serverx = OAObjectCacheDelegate.get(Server.class, server.getGuid());
		assertNull(serverx);

		serverx = OAObjectCacheDelegate.get(Server.class, 0);
		assertNull(serverx);

		serverx = OAObjectCacheDelegate.get(Server.class, server.getObjectKey());
		assertEquals(server, serverx);
	}

	@Test
	public void testGetObjectWhenIdIsChanged() {
		reset(false); // no ds, no auto assign Id

		Server server = new Server(); // no id

		Server serverx = OAObjectCacheDelegate.get(Server.class, server.getObjectKey()); // key only has guid
		assertEquals(server, serverx);

		serverx = OAObjectCacheDelegate.get(Server.class, server.getGuid()); // expects an Id value, not guid
		assertNull(serverx);

		serverx = OAObjectCacheDelegate.get(Server.class, 0); // does not have an Id assigned, still using guid for objKey
		assertNull(serverx);

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(Server.class);
		// dont update cache when Id is change
		oi.setAddToCache(false);
		server.setId(1);

		serverx = OAObjectCacheDelegate.get(Server.class, server.getObjectKey());
		assertNull(serverx);

		OAObjectKey ok = new OAObjectKey(null, server.getObjectKey().getGuid(), server.getObjectKey().isNew());
		serverx = OAObjectCacheDelegate.get(Server.class, ok);

		server.setId(0); // unset
		OAObjectKeyDelegate.setKey(server, ok); // set back to guid only
		oi.setAddToCache(true); // cache will still have the ok=guid

		serverx = OAObjectCacheDelegate.get(Server.class, server.getObjectKey()); // key only has guid
		assertEquals(server, serverx);

		server.setId(4);
		serverx = OAObjectCacheDelegate.get(Server.class, ok);
		assertNull(serverx);
	}

}
