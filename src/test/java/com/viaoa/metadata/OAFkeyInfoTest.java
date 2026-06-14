package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAFkeyInfoTest {
    @Test
    void propertyAndAnnotationAccessorsRoundTrip() {
        OAFkeyInfo fkey = new OAFkeyInfo();
        OAPropertyInfo from = new OAPropertyInfo();
        from.setName("fromId");
        OAPropertyInfo to = new OAPropertyInfo();
        to.setName("id");

        fkey.setFromPropertyInfo(from);
        fkey.setToPropertyInfo(to);
        fkey.setOAFkey(null);

        assertSame(from, fkey.getFromPropertyInfo());
        assertSame(to, fkey.getToPropertyInfo());
        assertNull(fkey.getOAFkey());
    }
}
