package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;

class HubEventTest {
    @Test
    void constructorsCaptureHubObjectPropertyValuesAndPositions() {
        Hub<Register> hub = new Hub<>(Register.class);
        Register r1 = new Register();
        Register r2 = new Register();

        HubEvent<Register> hubOnly = new HubEvent<>(hub);
        assertSame(hub, hubOnly.getHub());

        HubEvent<Register> objectEvent = new HubEvent<>(hub, r1);
        assertSame(hub, objectEvent.getHub());
        assertSame(r1, objectEvent.getObject());

        HubEvent<Register> propertyEvent = new HubEvent<>(hub, r1, Register.P_Code, "A", "B");
        assertEquals(Register.P_Code, propertyEvent.getPropertyName());
        assertEquals("A", propertyEvent.getOldValue());
        assertEquals("B", propertyEvent.getNewValue());
        assertTrue(propertyEvent.isProperty("CODE"));
        assertFalse(propertyEvent.isProperty(null));

        HubEvent<Register> replaceEvent = new HubEvent<>(hub, r1, r2);
        assertSame(r1, replaceEvent.getObject());
        assertSame(r2, replaceEvent.getObject2());

        HubEvent<Register> moveEvent = new HubEvent<>(hub, 2, 4);
        assertEquals(2, moveEvent.getFromPos());
        assertEquals(4, moveEvent.getToPos());

        HubEvent<Register> positionalEvent = new HubEvent<>(hub, r1, 3);
        assertEquals(3, positionalEvent.getPos());
    }

    @Test
    void responseAndCancelAreIndependentEventState() {
        HubEvent<Register> event = new HubEvent<>(new Register());
        assertFalse(event.getCancel());

        event.setCancel(true);
        event.setResponse("blocked");

        assertTrue(event.getCancel());
        assertEquals("blocked", event.getResponse());
    }
}
