package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAObjectInternalBridgeTest {

    @Test
    void constructorCreatesBridge() {
        assertNotNull(new OAObjectInternalBridge());
    }

    @Test
    void getObjectFriendAccessReturnsSharedFriendAccess() {
        assertNotNull(new OAObjectInternalBridge().getObjectFriendAccess());
    }

    @Test
    void getObjectInfoFriendAccessReturnsSharedFriendAccess() {
        assertNotNull(new OAObjectInternalBridge().getObjectInfoFriendAccess());
    }

    @Test
    void getPropertyInfoFriendAccessReturnsSharedFriendAccess() {
        assertNotNull(new OAObjectInternalBridge().getPropertyInfoFriendAccess());
    }

    @Test
    void getLinkInfoFriendAccessReturnsSharedFriendAccess() {
        assertNotNull(new OAObjectInternalBridge().getLinkInfoFriendAccess());
    }

    @Test
    void getCalcInfoFriendAccessReturnsSharedFriendAccess() {
        assertNotNull(new OAObjectInternalBridge().getCalcInfoFriendAccess());
    }

    @Test
    void getObjectSerializerFriendAccessReturnsSharedFriendAccess() {
        assertNotNull(new OAObjectInternalBridge().getObjectSerializerFriendAccess());
    }
}
