package com.viaoa.comm.multiplexer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.viaoa.OAUnitTest;
import com.viaoa.object.OAObject;
import com.viaoa.util.OALogUtil;

public class MultiplexerServerTest extends OAUnitTest {
	private volatile boolean bStopCalled;
	private OAMultiplexerServer multiplexerServer;

	public void test(final int maxConnections) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					_test(maxConnections, latch);
				} catch (Exception e) {
					System.out.println("Error in MultiplexerServerTest serverSocket");
					e.printStackTrace();
				}
			}
		};
		t.setDaemon(true);
		t.start();
		latch.await();
	}

	private void _test(int maxConnections, final CountDownLatch latch) throws Exception {
		//System.out.println("MultiplexerServerTest starting on port 1101");
		multiplexerServer = new OAMultiplexerServer(null, 1101);
		ServerSocket ss = multiplexerServer.createServerSocket("test");
		multiplexerServer.start();
		latch.countDown();

		OAObject.setDebugMode(true);

		for (int i = 0; (maxConnections == 0 && i == 0) || i < maxConnections; i++) {
			Socket s = ss.accept();
			test(s);
		}
	}

	public void stop() {
		bStopCalled = true;
		try {
			multiplexerServer.stop();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	protected void test(final Socket socket) throws Exception {
		Thread t = new Thread() {
			@Override
			public void run() {
				_test(socket);
			}
		};
		t.setName("ServerTestThread.value.x");
		t.start();
	}

	protected void _test(final Socket socket) {
		try {
			aiRunnngCount.incrementAndGet();
			_test2(socket);
		} catch (Exception e) {
			System.out.println("Exception with server virtual socket, exception=" + e);
			e.printStackTrace();
		} finally {
			aiRunnngCount.decrementAndGet();
		}
	}

	private AtomicInteger aiRunnngCount = new AtomicInteger();

	public int getRunningCount() {
		return aiRunnngCount.get();
	}

	private int grandTotal;

	protected void _test2(final Socket socket) throws Exception {
		InputStream is = socket.getInputStream();
		OutputStream os = socket.getOutputStream();

		DataInputStream dis = new DataInputStream(is);
		DataOutputStream dos = new DataOutputStream(os);

		long tot = 0;
		byte[] bs = null;
		for (int i = 0; !bStopCalled; i++) {

			if (i == 500) {
				int xx = 4;
				xx++;
			}
			int x = dis.readInt();
			if (x < 0) {
				break;
			}
			if (i == 0) {
				Thread.currentThread().setName("ServerTestThread.value." + x);
			}
			if (bs == null) {
				bs = new byte[x];
			}

			dis.readFully(bs);
			tot += x;
			//System.out.println("server, cnt="+i+", totBytes="+tot);

			dos.writeInt(bs.length);
			dos.write(bs);
		}
	}

	public static void main(String[] args) throws Exception {
		OALogUtil.consoleOnly(Level.FINE);

		MultiplexerServerTest test = new MultiplexerServerTest();
		test.test(0);
	}
}
