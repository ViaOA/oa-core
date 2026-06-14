package com.viaoa.comm.multiplexer.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

class MultiplexerInputStreamControllerTest {

    @Test
    void setDataInputStreamAllowsCreateSocketCommandProcessing() throws Exception {
        TestController controller = new TestController(10);
        byte[] namePayload = new byte[] { 0, 0, 0, 3, 's', 'v', 'c' };
        controller.setDataInputStream(new DataInputStream(new ByteArrayInputStream(namePayload)));

        controller.exposeProcessCommand(MultiplexerSocketController.CMD_CreateVSocket, 4);

        assertEquals(10, controller.createdConnectionId);
        assertEquals(4, controller.createdSocketId);
        assertEquals("svc", controller.createdServerSocketName);
    }

    @Test
    void closeWakesAndCausesReadToFail() throws Exception {
        TestController controller = new TestController(10);
        TestVirtualSocket socket = new TestVirtualSocket(10, 4, "svc");

        controller.exposeClose();

        IOException ex = assertThrows(IOException.class, () -> controller.exposeRead(socket, new byte[1], 0, 1));
        assertTrue(ex.getMessage().contains("closed"));
    }

    @Test
    void getLastReadTimeStartsAtZero() {
        TestController controller = new TestController(10);

        assertEquals(0, controller.getLastReadTime());
    }

    @Test
    void getReadCountAndSizeStartAtZero() {
        TestController controller = new TestController(10);

        assertEquals(0, controller.getReadCount());
        assertEquals(0, controller.getReadSize());
    }

    @Test
    void processCommandCloseSocketCallsSubclass() throws Exception {
        TestController controller = new TestController(10);

        controller.exposeProcessCommand(MultiplexerSocketController.CMD_CloseVSocket, 4);

        assertEquals(4, controller.closedSocketId);
        assertFalse(controller.closedSocketSendCommand);
    }

    @Test
    void processCommandCloseRealSocketCallsSubclass() throws Exception {
        TestController controller = new TestController(10);

        controller.exposeProcessCommand(MultiplexerSocketController.CMD_CloseRealSocket, 0);

        assertTrue(controller.realSocketClosed);
    }

    @Test
    void processCommandPingIsNoOp() throws Exception {
        TestController controller = new TestController(10);

        controller.exposeProcessCommand(MultiplexerSocketController.CMD_Ping, 0);

        assertFalse(controller.realSocketClosed);
        assertEquals(0, controller.closedSocketId);
    }

    private static class TestController extends MultiplexerInputStreamController {
        int createdConnectionId;
        int createdSocketId;
        String createdServerSocketName;
        int closedSocketId;
        boolean closedSocketSendCommand = true;
        boolean realSocketClosed;

        TestController(int connectionId) {
            super(connectionId);
        }

        void exposeProcessCommand(int cmd, int param) throws Exception {
            processCommand(cmd, param);
        }

        void exposeClose() throws IOException {
            close();
        }

        int exposeRead(VirtualSocket vs, byte[] bs, int off, int len) throws IOException {
            return read(vs, bs, off, len);
        }

        @Override
        protected void createNewSocket(int connectionId, int id, String serverSocketName) {
            createdConnectionId = connectionId;
            createdSocketId = id;
            createdServerSocketName = serverSocketName;
        }

        @Override
        protected void closeSocket(int id, boolean bSendCommand) {
            closedSocketId = id;
            closedSocketSendCommand = bSendCommand;
        }

        @Override
        protected void closeRealSocket() {
            realSocketClosed = true;
        }

        @Override
        protected VirtualSocket getSocket(int id) {
            return null;
        }

        @Override
        protected int getMaxSocketId() {
            return 0;
        }
    }

    private static class TestVirtualSocket extends VirtualSocket {
        TestVirtualSocket(int connectionId, int id, String serverSocketName) {
            super(connectionId, id, serverSocketName);
            setTimeoutSeconds(1);
        }

        @Override
        public int read(byte[] bs, int off, int len) { return 0; }

        @Override
        public int read() { return 0; }

        @Override
        public void write(byte[] bs, int off, int len) { }

        @Override
        public void write(int b) { }

        @Override
        public void close(boolean bSendCommand) throws IOException { super.close(); }
    }
}
