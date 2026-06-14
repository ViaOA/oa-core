package com.viaoa.comm.multiplexer.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class VirtualSocketTest {

    @Test
    void constructorRejectsNegativeId() {
        assertThrows(IllegalArgumentException.class, () -> new TestVirtualSocket(1, -1, "service"));
    }

    @Test
    void inputStreamDelegatesSingleAndBulkReads() throws Exception {
        TestVirtualSocket socket = new TestVirtualSocket(2, 3, "service", new byte[] { 10, 11, 12 });

        InputStream in = socket.getInputStream();

        assertEquals(10, in.read());
        byte[] bs = new byte[2];
        assertEquals(2, in.read(bs));
        assertArrayEquals(new byte[] { 11, 12 }, bs);
    }

    @Test
    void inputStreamIgnoresNullOrEmptyBulkReads() throws Exception {
        TestVirtualSocket socket = new TestVirtualSocket(2, 3, "service", new byte[] { 10 });
        InputStream in = socket.getInputStream();

        assertEquals(0, in.read(null, 0, 1));
        assertEquals(0, in.read(new byte[1], 0, 0));
        assertEquals(10, in.read());
    }

    @Test
    void getInputStreamThrowsWhenClosed() throws Exception {
        TestVirtualSocket socket = new TestVirtualSocket(2, 3, "service");

        socket.close();

        assertThrows(SocketException.class, socket::getInputStream);
    }

    @Test
    void outputStreamDelegatesSingleAndBulkWrites() throws Exception {
        TestVirtualSocket socket = new TestVirtualSocket(2, 3, "service");

        OutputStream out = socket.getOutputStream();
        out.write(65);
        out.write(new byte[] { 66, 67 });
        out.write(new byte[] { 1, 2, 3, 4 }, 1, 2);

        assertArrayEquals(new byte[] { 65, 66, 67, 2, 3 }, socket.written());
    }

    @Test
    void outputStreamIgnoresNullOrEmptyBulkWrites() throws Exception {
        TestVirtualSocket socket = new TestVirtualSocket(2, 3, "service");
        OutputStream out = socket.getOutputStream();

        out.write(null, 0, 1);
        out.write(new byte[] { 1 }, 0, 0);

        assertArrayEquals(new byte[0], socket.written());
    }

    @Test
    void getOutputStreamThrowsWhenClosed() throws Exception {
        TestVirtualSocket socket = new TestVirtualSocket(2, 3, "service");

        socket.close();

        assertThrows(SocketException.class, socket::getOutputStream);
    }

    @Test
    void gettersReturnConstructorValues() {
        TestVirtualSocket socket = new TestVirtualSocket(8, 9, "service");

        assertEquals(8, socket.getConnectionId());
        assertEquals(9, socket.getId());
        assertEquals("service", socket.getServerSocketName());
    }

    @Test
    void timeoutSecondsRoundTrips() {
        TestVirtualSocket socket = new TestVirtualSocket(2, 3, "service");

        socket.setTimeoutSeconds(12);

        assertEquals(12, socket.getTimeoutSeconds());
    }

    private static class TestVirtualSocket extends VirtualSocket {
        private byte[] readBytes;
        private int readPos;
        private byte[] written = new byte[0];

        TestVirtualSocket(int connectionId, int id, String serverSocketName) {
            this(connectionId, id, serverSocketName, new byte[0]);
        }

        TestVirtualSocket(int connectionId, int id, String serverSocketName, byte[] readBytes) {
            super(connectionId, id, serverSocketName);
            this.readBytes = readBytes;
        }

        @Override
        public int read(byte[] bs, int off, int len) {
            if (readPos >= readBytes.length) return -1;
            int amt = Math.min(len, readBytes.length - readPos);
            System.arraycopy(readBytes, readPos, bs, off, amt);
            readPos += amt;
            return amt;
        }

        @Override
        public int read() {
            if (readPos >= readBytes.length) return -1;
            return readBytes[readPos++] & 0xff;
        }

        @Override
        public void write(byte[] bs, int off, int len) {
            byte[] next = Arrays.copyOf(written, written.length + len);
            System.arraycopy(bs, off, next, written.length, len);
            written = next;
        }

        @Override
        public void write(int b) {
            byte[] next = Arrays.copyOf(written, written.length + 1);
            next[next.length - 1] = (byte) b;
            written = next;
        }

        @Override
        public void close(boolean bSendCommand) throws IOException {
            super.close();
        }

        byte[] written() {
            return written;
        }
    }
}
