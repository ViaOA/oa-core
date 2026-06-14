package com.viaoa.datasource.autonumber;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NextNumberTest {

    @Test
    void getIdAndSetIdRoundTrip() {
        NextNumber nn = new NextNumber();

        nn.setId("com.test.Store");

        assertEquals("com.test.Store", nn.getId());
    }

    @Test
    void getNextAndSetNextRoundTrip() {
        NextNumber nn = new NextNumber();

        assertEquals(1, nn.getNext());
        nn.setNext(42);
        assertEquals(42, nn.getNext());
    }

    @Test
    void getPropertyAndSetPropertyRoundTrip() {
        NextNumber nn = new NextNumber();

        nn.setProperty("id");

        assertEquals("id", nn.getProperty());
    }
}
