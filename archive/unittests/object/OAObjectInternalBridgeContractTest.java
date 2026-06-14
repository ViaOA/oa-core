package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAObjectInternalBridgeContractTest {

    @Test
    void bridgeReturnsStableFriendAccessInstances() {
        OAObjectInternalBridge bridge = new OAObjectInternalBridge();

        assertNotNull(bridge.getObjectFriendAccess());
        assertNotNull(bridge.getObjectInfoFriendAccess());
        assertNotNull(bridge.getPropertyInfoFriendAccess());
        assertNotNull(bridge.getLinkInfoFriendAccess());
        assertNotNull(bridge.getCalcInfoFriendAccess());
        assertNotNull(bridge.getObjectSerializerFriendAccess());

        assertSame(bridge.getObjectFriendAccess(), bridge.getObjectFriendAccess());
        assertSame(bridge.getObjectInfoFriendAccess(), bridge.getObjectInfoFriendAccess());
        assertSame(bridge.getPropertyInfoFriendAccess(), bridge.getPropertyInfoFriendAccess());
        assertSame(bridge.getLinkInfoFriendAccess(), bridge.getLinkInfoFriendAccess());
        assertSame(bridge.getCalcInfoFriendAccess(), bridge.getCalcInfoFriendAccess());
        assertSame(bridge.getObjectSerializerFriendAccess(), bridge.getObjectSerializerFriendAccess());
    }

    @Test
    void objectFriendAccessMatchesStaticFriendAccess() {
        OAObjectInternalBridge bridge = new OAObjectInternalBridge();

        assertSame(OAObject.getFriendAccess(), bridge.getObjectFriendAccess());
    }
}
