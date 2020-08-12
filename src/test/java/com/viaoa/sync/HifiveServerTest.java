package com.viaoa.sync;

import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.datasource.objectcache.OADataSourceObjectCache;
import com.viaoa.object.OAObject;
import com.viaoa.util.OACircularQueue;
import com.viaoa.util.OALogUtil;

import test.hifive.delegate.ModelDelegate;
import test.hifive.model.oa.Employee;
import test.hifive.model.oa.Location;
import test.hifive.model.oa.Program;
import test.hifive.model.oa.cs.ServerRoot;

/**
 * Run this manually, and then run OASyncClientTest multiple times, and then run it as a junit test.
 */
public class HifiveServerTest {
	private static Logger LOG = Logger.getLogger(HifiveServerTest.class.getName());

	private ServerRoot serverRoot;
	public OASyncServer syncServer;

	public void start() throws Exception {

		// for non-DB objects
		OADataSourceObjectCache ds = new OADataSourceObjectCache();
		ds.setAssignIdOnCreate(true);

		serverRoot = new ServerRoot();

		// serverRoot = dg.readSerializeFromFile();
		long t1 = System.currentTimeMillis();
		// gen data
		for (int i = 0; i < 5; i++) {
			Program prog = new Program();

			for (int cnt = 0; cnt < 10; cnt++) {
				Location loc = new Location();
				prog.getLocations().add(loc);
				for (int cnt2 = 0; cnt2 < 50; cnt2++) {
					Employee emp = new Employee();
					loc.getEmployees().add(emp);
				}
			}
			serverRoot.getActivePrograms().add(prog);
		}

		long t2 = System.currentTimeMillis();

		long diff = t2 - t1;

		ModelDelegate.initialize(serverRoot);

		syncServer = new OASyncServer(1103) {
			@Override
			protected void onClientConnect(Socket socket, int connectionId) {
				super.onClientConnect(socket, connectionId);
				//OASyncServerTest.this.onClientConnect(socket, connectionId);
			}

			@Override
			protected void onClientDisconnect(int connectionId) {
				super.onClientDisconnect(connectionId);
				//OASyncServerTest.this.onClientDisconnect(connectionId);
			}
		};
		syncServer.start();
	}

	public static void main(String[] args) throws Exception {
		OAObject.setDebugMode(true);
		OALogUtil.consoleOnly(Level.FINE, OACircularQueue.class.getName());//"com.viaoa.util.OACircularQueue");
		OALogUtil.consolePerformance();

		Logger logx = Logger.getLogger(OACircularQueue.class.getName());

		HifiveServerTest test = new HifiveServerTest();
		test.start();

		System.out.println("Hifive test server is now running");
		for (;;) {
			Thread.sleep(10000);
		}
	}

}
