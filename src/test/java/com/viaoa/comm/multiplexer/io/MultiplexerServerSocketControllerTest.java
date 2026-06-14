package com.viaoa.comm.multiplexer.io;

import static org.junit.jupiter.api.Assertions.*;

import java.net.Socket;

import org.junit.jupiter.api.Test;

class MultiplexerServerSocketControllerTest {

    @Test
    void constructorStartsWithZeroStats() {
        MultiplexerServerSocketController controller = new MultiplexerServerSocketController();

        assertEquals(0, controller.getWriteCount());
        assertEquals(0, controller.getWriteSize());
        assertEquals(0, controller.getReadCount());
        assertEquals(0, controller.getReadSize());
        assertEquals(0, controller.getCreatedConnectionCount());
        assertEquals(0, controller.getLiveConnectionCount());
    }

    @Test
    void invalidConnectionMessageRoundTrips() {
        MultiplexerServerSocketController controller = new MultiplexerServerSocketController();

        controller.setInvalidConnectionMessage("invalid");

        assertEquals("invalid", controller.getInvalidConnectionMessage());
    }

    @Test
    void startRejectsNullServerSocket() {
        MultiplexerServerSocketController controller = new MultiplexerServerSocketController();

        assertThrows(IllegalArgumentException.class, () -> controller.start(null));
    }

    @Test
    void throttleLimitRoundTrips() {
        MultiplexerServerSocketController controller = new MultiplexerServerSocketController();

        controller.setThrottleLimit(6);

        assertEquals(6, controller.getThrottleLimit());
    }

    @Test
    void getServerSocketReturnsNullForBlankNames() throws Exception {
        MultiplexerServerSocketController controller = new MultiplexerServerSocketController();

        assertNull(controller.getServerSocket(null));
        assertNull(controller.getServerSocket(""));
    }

    @Test
    void getServerSocketReturnsNamedVirtualServerSocketAndCachesIt() throws Exception {
        MultiplexerServerSocketController controller = new MultiplexerServerSocketController();

        VirtualServerSocket socket1 = controller.getServerSocket("service");
        VirtualServerSocket socket2 = controller.getServerSocket("service");

        assertNotNull(socket1);
        assertEquals("service", socket1.getName());
        assertSame(socket1, socket2);
        controller.close();
    }

    @Test
    void addAndRemoveControllerUpdatesCreatedAndLiveCounts() throws Exception {
        TestServerSocketController controller = new TestServerSocketController();
        TestSocketController socketController = new TestSocketController(new Socket(), 3);

        controller.exposeAdd(socketController);
        assertEquals(1, controller.getCreatedConnectionCount());
        assertEquals(1, controller.getLiveConnectionCount());

        controller.exposeRemove(socketController);
        assertEquals(0, controller.getLiveConnectionCount());
        socketController.close(true);
    }

    @Test
    void stopAcceptingCanBeCalledBeforeStart() {
        MultiplexerServerSocketController controller = new MultiplexerServerSocketController();

        assertDoesNotThrow(controller::stopAccepting);
    }

    @Test
    void closeCanBeCalledBeforeStart() {
        MultiplexerServerSocketController controller = new MultiplexerServerSocketController();

        assertDoesNotThrow(() -> controller.close());
    }

    @Test
    void callbacksAreNoOpsByDefault() {
        MultiplexerServerSocketController controller = new MultiplexerServerSocketController();

        assertDoesNotThrow(() -> controller.onClientConnect(new Socket(), 1));
        assertDoesNotThrow(() -> controller.onClientDisconnect(1));
    }

    private static class TestServerSocketController extends MultiplexerServerSocketController {
        void exposeAdd(MultiplexerSocketController controller) {
            add(controller);
        }

        void exposeRemove(MultiplexerSocketController controller) {
            remove(controller);
        }
    }

    private static class TestSocketController extends MultiplexerSocketController {
        TestSocketController(Socket socket, int id) {
            super(socket, id);
        }
    }
}
