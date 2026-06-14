package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HubInternalBridgeTest {
    @Test
    void exposesFriendAccessObjectsForInternalServices() {
        HubInternalBridge bridge = new HubInternalBridge();

        assertNotNull(bridge.getHubFriendAccess());
    }
}
