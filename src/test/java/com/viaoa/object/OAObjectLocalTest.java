package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.viaoa.metadata.OAObjectInfo;

class OAObjectLocalTest {

    @Test
    void getOAObjectInfoReturnsLocalOnlyTransientMetadata() {
        OAObjectInfo oi = OAObjectLocal.getOAObjectInfo();

        assertNotNull(oi);
        assertTrue(oi.getLocalOnly());
        assertFalse(oi.getUseDataSource());
        assertFalse(oi.getAddToCache());
        assertFalse(oi.getInitializeNewObjects());
    }
}
