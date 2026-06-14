package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.metadata.OAObjectInfo;

import org.junit.jupiter.api.Test;

class OAObjectLocalContractTest {

    @Test
    void objectInfoIsConfiguredLocalOnly() {
        OAObjectInfo oi = OAObjectLocal.getOAObjectInfo();

        assertNotNull(oi);
        assertTrue(oi.getLocalOnly());
        assertFalse(oi.getUseDataSource());
        assertFalse(oi.getAddToCache());
        assertFalse(oi.getInitializeNewObjects());
    }

    @Test
    void getOAObjectInfoReturnsStableSingleton() {
        assertSame(OAObjectLocal.getOAObjectInfo(), OAObjectLocal.getOAObjectInfo());
    }

    @Test
    void localObjectCanBeConstructedAndHasRuntimeGuid() {
        OAObjectLocal obj = new OAObjectLocal();

        assertNotNull(obj.getGuid());
    }
}
