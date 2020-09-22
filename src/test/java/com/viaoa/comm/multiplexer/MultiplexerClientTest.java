package com.viaoa.comm.multiplexer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.OAUnitTest;
import com.viaoa.object.OAObject;

public class MultiplexerClientTest extends OAUnitTest {
	private AtomicInteger aiCount = new AtomicInteger();
	private AtomicInteger aiRunnngCount = new AtomicInteger();

	public void test(int... msgSizes) throws Exception {
		final OAMultiplexerClient mc = new OAMultiplexerClient("localhost", 1101);
		mc.start();
		OAObject.setDebugMode(true);

		for (int i = 0; i < msgSizes.length; i++) {
			final int id = i;
			final int msgSize = msgSizes[i];
			Thread t = new Thread() {
				@Override
				public void run() {
					aiRunnngCount.incrementAndGet();
					try {
						_test(id, mc, msgSize);
					} catch (Exception e) {
						System.out.println("Exception with client virtual socket, exception=" + e);
						e.printStackTrace();
					} finally {
						aiRunnngCount.decrementAndGet();
					}
				}
			};
			t.setDaemon(true);
			t.setName("ClientTestThread." + i + ".value." + msgSize);
			t.start();
		}
	}

	private volatile boolean bStopCalled;

	public void stop() {
		bStopCalled = true;
	}

	public int getRunningCount() {
		return aiRunnngCount.get();
	}

	public void _test(final int id, final OAMultiplexerClient mc, final int msgSize) throws Exception {
		final Socket socket = mc.createSocket("test");

		InputStream is = socket.getInputStream();
		OutputStream os = socket.getOutputStream();

		DataInputStream dis = new DataInputStream(is);
		DataOutputStream dos = new DataOutputStream(os);

		// BufferedOutputStream bos = new BufferedOutputStream(dos);

		long tot = 0;
		byte[] bs = new byte[msgSize];
		int i = 0;
		for (;; i++) {
			dos.writeInt(msgSize);
			dos.write(bs);

			if (bStopCalled) {
				break;
			}
			int x = dis.readInt();
			dis.readFully(bs);

			tot += msgSize;
			if (bStopCalled || i % 100 == 0) {
				System.out
						.println("client, id=" + id + ", cnt=" + i + ", bs=" + msgSize + ", totBytes=" + tot + ", allCnt=" + aiCount.get());
			}

			aiCount.incrementAndGet();
			if (bStopCalled) {
				break;
			}
		}
		dos.writeInt(-1);
		System.out
				.println("closing client, id=" + id + ", cnt=" + i + ", bs=" + msgSize + ", totBytes=" + tot + ", allCnt=" + aiCount.get());
		socket.close();
	}

	public int getCount() {
		return aiCount.get();
	}

	public static void main(String[] args) throws Exception {
		MultiplexerClientTest test = new MultiplexerClientTest();
		test.test(25);
	}
}
