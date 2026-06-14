package com.viaoa.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAThreadServiceTest {
    @Test
    void constructorCreatesThreadLocalAndRemoteServices() {
        OAThreadService service = new OAThreadService();

        assertNotNull(service.getThreadLocalService());
        assertNotNull(service.getRemoteThreadService());
        assertNull(service.getContextUser());
        assertFalse(service.isAdmin());
        assertFalse(service.isRemoteThread());
        assertFalse(service.isRefreshing());
        assertNull(service.getTransaction());
        assertNotNull(service.getAllStackTraces());
    }

    @Test
    void contextUserAndAdminDelegateToThreadLocalService() {
        OAThreadService service = new OAThreadService();

        assertFalse(service.isAdmin());
        service.getThreadLocalService().setAdmin(true);
        assertTrue(service.isAdmin());
        service.getThreadLocalService().setAdmin(false);
        assertFalse(service.isAdmin());
    }
}
