package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OADateTimeComparisonAndOrderingTest {

    @Test
    void dateOnlyComparisonIgnoresTimeOfDay() {
        OADate d1 = new OADate(new OADateTime(2026, 4, 27, 0, 0, 0, 0));
        OADate d2 = new OADate(new OADateTime(2026, 4, 27, 23, 59, 59, 999));

        assertEquals(0, d1.compareTo(d2));
        assertEquals(d1, d2);
    }

    @Test
    void timeOnlyComparisonIgnoresDatePortion() {
        OATime t1 = new OATime(new OADateTime(2026, 0, 1, 7, 8, 9, 123));
        OATime t2 = new OATime(new OADateTime(2030, 11, 31, 7, 8, 9, 123));

        assertEquals(0, t1.compareTo(t2));
        assertEquals(t1, t2);
    }

    @Test
    void dateTimeComparisonUsesEpochOrder() {
        OADateTime a = new OADateTime(1_000L);
        OADateTime b = new OADateTime(2_000L);

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(b.after(a));
        assertTrue(a.before(b));
    }

    @Test
    void betweenInclusiveAndExclusiveDateChecks() {
        OADate target = new OADate(2026, 4, 27);
        OADate start = new OADate(2026, 4, 27);
        OADate end = new OADate(2026, 4, 28);

        assertTrue(target.between(start, end));  
        assertTrue(target.betweenOrEqual(start, end));
        assertFalse(target.betweenNotEqual(start, end));
    }

    @Test
    void nonConvertibleObjectComparisonDocumentsCurrentSentinelBehavior() {
        OADateTime dt = new OADateTime(2026, 4, 27, 7, 8, 9, 0);

        assertEquals(2, dt.compareTo(new Object()));
        assertTrue(dt.after(new Object()));
    }

    @Test
    void hashCodeContractForEqualDateOnlyValuesIsDocumented() {
        OADate d1 = new OADate(new OADateTime(2026, 4, 27, 0, 0, 0, 0));
        OADate d2 = new OADate(new OADateTime(2026, 4, 27, 23, 59, 59, 999));

        assertEquals(d1, d2);

        // Current CODEX notes have flagged possible mismatch between semantic
        // equality and raw-time hashCode. This assertion documents current contract.
        assertEquals(d1.hashCode(), d2.hashCode());
    }

    @Test
    void hashCodeContractForEqualTimeOnlyValuesIsDocumented() {
        OATime t1 = new OATime(new OADateTime(2026, 0, 1, 7, 8, 9, 123));
        OATime t2 = new OATime(new OADateTime(2030, 11, 31, 7, 8, 9, 123));

        assertEquals(t1, t2);

        // Current CODEX notes have flagged possible mismatch between semantic
        // equality and raw-time hashCode. This assertion documents current contract.
        assertEquals(t1.hashCode(), t2.hashCode());
    }
}
