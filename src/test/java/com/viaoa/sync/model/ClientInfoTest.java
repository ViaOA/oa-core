package com.viaoa.sync.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.viaoa.datetime.OADateTime;

class ClientInfoTest {

    @Test
    void createdStoresAssignedValue() {
        ClientInfo ci = new ClientInfo();
        OADateTime dt = new OADateTime(123456789L);

        ci.setCreated(dt);

        assertSame(dt, ci.getCreated());
    }

    @Test
    void ipAddressStoresAssignedValue() {
        ClientInfo ci = new ClientInfo();

        ci.setIpAddress("192.0.2.10");

        assertEquals("192.0.2.10", ci.getIpAddress());
    }

    @Test
    void hostNameStoresAssignedValue() {
        ClientInfo ci = new ClientInfo();

        ci.setHostName("pos-client-01");

        assertEquals("pos-client-01", ci.getHostName());
    }

    @Test
    void connectionIdDefaultsToMinusOneAndStoresAssignedValue() {
        ClientInfo ci = new ClientInfo();
        assertEquals(-1, ci.getConnectionId());

        ci.setConnectionId(42);

        assertEquals(42, ci.getConnectionId());
    }

    @Test
    void disconnectedStoresAssignedValue() {
        ClientInfo ci = new ClientInfo();
        OADateTime dt = new OADateTime(987654321L);

        ci.setDisconnected(dt);

        assertSame(dt, ci.getDisconnected());
    }

    @Test
    void totalRequestsCanBeSetAndIncremented() {
        ClientInfo ci = new ClientInfo();

        ci.setTotalRequests(7);
        ci.incrementTotalRequests();

        assertEquals(8, ci.getTotalRequests());
    }

    @Test
    void totalRequestTimeCanBeSetAndIncremented() {
        ClientInfo ci = new ClientInfo();

        ci.setTotalRequestTime(100L);
        ci.incrementTotalRequestTime(25L);

        assertEquals(125L, ci.getTotalRequestTime());
    }

    @Test
    void serverHostNameStoresAssignedValue() {
        ClientInfo ci = new ClientInfo();

        ci.setServerHostName("server.example.test");

        assertEquals("server.example.test", ci.getServerHostName());
    }

    @Test
    void serverHostPortStoresAssignedValue() {
        ClientInfo ci = new ClientInfo();

        ci.setServerHostPort(1099);

        assertEquals(1099, ci.getServerHostPort());
    }

    @Test
    void remoteThreadCountStoresAssignedValue() {
        ClientInfo ci = new ClientInfo();

        ci.setRemoteThreadCount(3);

        assertEquals(3, ci.getRemoteThreadCount());
    }

    @Test
    void startedStoresAssignedValue() {
        ClientInfo ci = new ClientInfo();
        assertFalse(ci.isStarted());

        ci.setStarted(true);

        assertTrue(ci.isStarted());
    }

    @Test
    void userFieldsStoreAssignedValues() {
        ClientInfo ci = new ClientInfo();

        ci.setUserId("u123");
        ci.setUserName("Ada Lovelace");
        ci.setLocation("Store 101");

        assertEquals("u123", ci.getUserId());
        assertEquals("Ada Lovelace", ci.getUserName());
        assertEquals("Store 101", ci.getLocation());
    }

    @Test
    void memoryFieldsStoreAssignedValues() {
        ClientInfo ci = new ClientInfo();

        ci.setTotalMemory(2048L);
        ci.setFreeMemory(1024L);

        assertEquals(2048L, ci.getTotalMemory());
        assertEquals(1024L, ci.getFreeMemory());
    }

    @Test
    void versionStoresAssignedValue() {
        ClientInfo ci = new ClientInfo();

        ci.setVersion("4.0-test");

        assertEquals("4.0-test", ci.getVersion());
    }
}
