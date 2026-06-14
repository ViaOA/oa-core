package com.viaoa.comm.multiplexer.io;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VirtualServerSocketTest {

    @Test
    void constructorStoresName() throws Exception {
        VirtualServerSocket socket = new VirtualServerSocket("service");

        assertEquals("service", socket.getName());
        socket.close();
    }

    @Test
    void closeMarksSocketClosed() throws Exception {
        VirtualServerSocket socket = new VirtualServerSocket("service");

        socket.close();

        assertTrue(socket.isClosed());
    }
}
