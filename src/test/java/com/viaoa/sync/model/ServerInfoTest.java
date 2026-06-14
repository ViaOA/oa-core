package com.viaoa.sync.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.viaoa.datetime.OADateTime;

class ServerInfoTest {

    @Test
    void createdStoresAssignedValue() {
        ServerInfo si = new ServerInfo();
        OADateTime dt = new OADateTime(2468L);

        si.setCreated(dt);

        assertSame(dt, si.getCreated());
    }

    @Test
    void ipAddressStoresAssignedValue() {
        ServerInfo si = new ServerInfo();

        si.setIpAddress("198.51.100.7");

        assertEquals("198.51.100.7", si.getIpAddress());
    }

    @Test
    void hostNameStoresAssignedValue() {
        ServerInfo si = new ServerInfo();

        si.setHostName("sync-server");

        assertEquals("sync-server", si.getHostName());
    }

    @Test
    void versionStoresAssignedValue() {
        ServerInfo si = new ServerInfo();

        si.setVersion("4.0-test");

        assertEquals("4.0-test", si.getVersion());
    }

    @Test
    void startedStoresAssignedValue() {
        ServerInfo si = new ServerInfo();
        assertFalse(si.isStarted());

        si.setStarted(true);

        assertTrue(si.isStarted());
    }

    @Test
    void suspendedStoresAssignedValue() {
        ServerInfo si = new ServerInfo();
        assertFalse(si.isSuspended());

        si.setSuspended(true);

        assertTrue(si.isSuspended());
    }

    @Test
    void discoveryEnabledStoresAssignedValue() {
        ServerInfo si = new ServerInfo();
        assertFalse(si.isDiscoveryEnabled());

        si.setDiscoveryEnabled(true);

        assertTrue(si.isDiscoveryEnabled());
    }
}
