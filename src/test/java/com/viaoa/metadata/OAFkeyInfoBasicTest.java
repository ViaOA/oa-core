package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAFkeyInfoBasicTest {

    @Test
    void propertyInfoReferencesRoundTripByIdentity() {
        OAFkeyInfo fki = new OAFkeyInfo();
        OAPropertyInfo from = new OAPropertyInfo();
        OAPropertyInfo to = new OAPropertyInfo();

        from.setName("customerId");
        to.setName("id");

        fki.setFromPropertyInfo(from);
        fki.setToPropertyInfo(to);

        assertSame(from, fki.getFromPropertyInfo());
        assertSame(to, fki.getToPropertyInfo());
        assertEquals("customerId", fki.getFromPropertyInfo().getName());
        assertEquals("id", fki.getToPropertyInfo().getName());
    }

    @Test
    void unsetPropertyInfosAreNullSafe() {
        OAFkeyInfo fki = new OAFkeyInfo();

        assertNull(fki.getFromPropertyInfo());
        assertNull(fki.getToPropertyInfo());
        assertNull(fki.getOAFkey());
    }
}
