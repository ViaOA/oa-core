package com.viaoa.comm.multiplexer.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.Socket;

import org.junit.jupiter.api.Test;

class MultiplexerSocketControllerTest {

    @Test
    void clientConstructorRejectsNullSocket() {
        assertThrows(IllegalArgumentException.class, () -> new MultiplexerSocketController(null));
    }

    @Test
    void serverConstructorRejectsNullSocket() {
        assertThrows(IllegalArgumentException.class, () -> new TestController(null, 1));
    }

    @Test
    void startCreatesReaderThreadOnce() throws Exception {
        TestController controller = new TestController(new Socket(), 12);

        controller.start();
        Thread thread = controller.getThread();
        controller.start();

        assertSame(thread, controller.getThread());
        assertTrue(thread.isDaemon());
        controller.close(true);
    }

    @Test
    void gettersExposeInitialServerSideState() throws Exception {
        TestController controller = new TestController(new Socket(), 12);

        assertEquals(12, controller.getId());
        assertEquals(MultiplexerSocketController.STATUS_Running, controller.getStatus());
        assertFalse(controller.isValid());
        assertTrue(controller.getStartTimeMS() > 0);
        assertNull(controller.getThread());
        assertNull(controller.getInetAddress());
        controller.close(true);
    }

    @Test
    void streamControllersAreLazyAndCached() throws Exception {
        TestController controller = new TestController(new Socket(), 12);

        assertSame(controller.getOutputStreamController(), controller.getOutputStreamController());
        assertSame(controller.getInputStreamController(), controller.getInputStreamController());
        controller.close(true);
    }

    @Test
    void createSocketOnServerSideRegistersVirtualSocketWithoutSendingCommand() throws Exception {
        TestController controller = new TestController(new Socket(), 12);

        VirtualSocket socket = controller.exposeCreateSocket(12, 5, "service");

        assertEquals(12, socket.getConnectionId());
        assertEquals(5, socket.getId());
        assertEquals("service", socket.getServerSocketName());
        assertEquals(1, controller.getLiveSocketCount());
        assertArrayEquals(new VirtualSocket[] { socket }, controller.getMultiplexerSockets());
        controller.close(true);
    }

    @Test
    void closeSocketRemovesVirtualSocketWhenNoPeerCommandIsSent() throws Exception {
        TestController controller = new TestController(new Socket(), 12);
        VirtualSocket socket = controller.exposeCreateSocket(12, 5, "service");

        controller.exposeCloseSocket(socket, false);

        assertEquals(0, controller.getLiveSocketCount());
        controller.close(true);
    }

    @Test
    void onSocketExceptionClosesAndMarksErrorStatus() throws Exception {
        TestController controller = new TestController(new Socket(), 12);

        controller.exposeOnSocketException(new IOException("boom"));

        assertTrue(controller.wasCloseAlreadyCalled());
        assertEquals(MultiplexerSocketController.STATUS_DisconnectedByError, controller.getStatus());
        assertTrue(controller.isClosed());
    }

    @Test
    void errorCloseIsIdempotentAndMarksControllerClosed() throws Exception {
        TestController controller = new TestController(new Socket(), 12);

        controller.close(true);
        controller.close(true);

        assertTrue(controller.wasCloseAlreadyCalled());
        assertTrue(controller.isClosed());
    }

    @Test
    void invalidConnectionMessageRoundTrips() throws Exception {
        TestController controller = new TestController(new Socket(), 12);

        controller.setInvalidConnectionMessage("invalid");

        assertEquals("invalid", controller.getInvalidConnectionMessage());
        controller.close(true);
    }

    private static class TestController extends MultiplexerSocketController {
        TestController(Socket socket, int id) {
            super(socket, id);
        }

        VirtualSocket exposeCreateSocket(int connectionId, int id, String serverSocketName) throws IOException {
            return createSocket(connectionId, id, serverSocketName);
        }

        void exposeCloseSocket(VirtualSocket vs, boolean sendCommand) throws IOException {
            closeSocket(vs, sendCommand);
        }

        void exposeOnSocketException(Exception e) {
            onSocketException(e);
        }
    }
}
