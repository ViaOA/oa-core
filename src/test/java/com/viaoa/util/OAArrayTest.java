package com.viaoa.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.viaoa.OAUnitTest;

import test.xice.tsac3.model.oa.Server;

public class OAArrayTest extends OAUnitTest {

	@Test
	public void testAdd() {

		String[] ss = null;
		ss = OAArray.add(ss, "test");
		assertNotNull(ss);
		assertEquals(ss.length, 1);

		// wont compile:  ss = OAArray.add(ss, null);
		ss = (String[]) OAArray.add(String.class, ss, (String) null);

		assertEquals(ss.length, 2);
		assertEquals("test", ss[0]);
		assertNotNull(ss[0]);
		assertNull(ss[1]);

		ss = OAArray.add(ss, (String) null);
		assertEquals(ss.length, 3);
		assertEquals("test", ss[0]);
		assertNotNull(ss[0]);
		assertNull(ss[1]);
		assertNull(ss[2]);

		ss = OAArray.add(ss, "test2");
		assertEquals(ss.length, 4);
		assertEquals("test", ss[0]);
		assertNotNull(ss[0]);
		assertNull(ss[1]);
		assertNull(ss[2]);
		assertEquals("test2", ss[3]);

		// should not compile
		// int[] x = OAArray.add(null, null);

		int[] xs = null;
		xs = OAArray.add(xs, 1);

		assertEquals(xs.length, 1);
		assertEquals(xs[0], 1);

		xs = OAArray.add(xs, 2);
		assertEquals(xs.length, 2);
		assertEquals(xs[0], 1);
		assertEquals(xs[1], 2);

		xs = OAArray.add(xs, new Integer(3));
		assertEquals(xs.length, 3);
		assertEquals(xs[0], 1);
		assertEquals(xs[1], 2);
		assertEquals(xs[2], 3);
	}

	@Test
	public void testInsert() {

		String[] ss = null;
		ss = OAArray.insert(ss, "test", 5);
		assertNotNull(ss);
		assertEquals(ss.length, 1);

		// wont compile:  ss = OAArray.add(ss, null);
		ss = (String[]) OAArray.insert(String.class, ss, (String) null, 5);

		//qqq Add more tests qqqqqqqqq
	}

	@Test
	public void test2() {

		Server server = new Server();
		Server[] servers = (Server[]) OAArray.add(Server.class, (Server[]) null, server);

		assertNotNull(servers);
		assertEquals(1, servers.length);
		assertEquals(server, servers[0]);

		servers = null;
		for (int i = 0; i < 100; i++) {
			server = new Server();
			servers = (Server[]) OAArray.add(Server.class, servers, server);
			assertEquals(i + 1, servers.length);
			assertEquals(server, servers[i]);
		}

		int x = servers.length;
		for (int i = 0; i < 100; i++) {
			server = new Server();
			servers = (Server[]) OAArray.insert(Server.class, servers, server, 0);
			assertEquals(i + x + 1, servers.length);
			assertEquals(server, servers[0]);
		}

		x = servers.length;
		for (int i = 0; i < x; i++) {
			int pos = (int) (Math.random() * servers.length);
			server = servers[pos];
			servers = (Server[]) OAArray.removeAt(Server.class, servers, pos);
			assertEquals(x - (i + 1), servers.length);
			if (servers.length > pos) {
				assertNotEquals(server, servers[pos]);
			}
		}

		// random
		for (int i = 0; i < 25000; i++) {
			x = servers.length;

			int pos;
			double d = Math.random();

			if (x > 1000 || (x > 0 && d < .33)) {
				pos = (int) (Math.random() * x);
				server = servers[pos];
				servers = (Server[]) OAArray.removeAt(Server.class, servers, pos);
				assertEquals(x - 1, servers.length);
				if (servers.length > pos) {
					assertNotEquals(server, servers[pos]);
				}
			} else {
				if (d < .66) {
					server = new Server();
					servers = (Server[]) OAArray.add(Server.class, servers, server);
					assertEquals(x + 1, servers.length);
					assertEquals(server, servers[x]);
				} else {
					pos = (int) (Math.random() * x);
					server = new Server();
					servers = (Server[]) OAArray.insert(Server.class, servers, server, x);
					assertEquals(x + 1, servers.length);
					assertEquals(server, servers[x]);
				}
			}

		}

	}

}
