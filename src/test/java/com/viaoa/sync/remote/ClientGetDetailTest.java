package com.viaoa.sync.remote;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

class ClientGetDetailTest {

    @Test
    void constructorAllowsGuidTrackingThroughAddAndRemove() {
        Map<UUID, Boolean> map = new ConcurrentHashMap<>();
        ClientGetDetail detail = new ClientGetDetail(1, map);
        UUID guid = UUID.randomUUID();

        detail.addGuid(guid);
        assertEquals(Boolean.FALSE, map.get(guid));

        detail.removeGuid(guid.getMostSignificantBits());
        assertTrue(map.containsKey(guid), "removeGuid(long) does not remove UUID keys; this pins current behavior");
    }

    @Test
    void closeIsNoOp() {
        ClientGetDetail detail = new ClientGetDetail(1, new ConcurrentHashMap<>());

        assertDoesNotThrow(detail::close);
    }

    @Test
    void getDetailReturnsNullForNullKeyOrProperty() {
        ClientGetDetail detail = new ClientGetDetail(1, new ConcurrentHashMap<>());

        assertNull(detail.getDetail(1, com.test.pos.model.oa.Store.class, null, "name", null, null, false));
        assertNull(detail.getDetail(1, com.test.pos.model.oa.Store.class, new com.viaoa.object.OAObjectKey(new Object[] { 1 }), null, null, null, false));
    }
}
