package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OADateTimeInteroperabilityTest {
    private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");
    private static final ZoneId UTC = ZoneOffset.UTC;

    private ZoneId originalDefaultZoneId;
    private Locale originalLocale;

    @BeforeEach
    void beforeEach() {
        originalDefaultZoneId = OADateTime.getDefaultZoneId();
        originalLocale = Locale.getDefault();
        OADateTime.setDefaultZoneId(CHICAGO);
        OADateTime.setLocale(Locale.US);
        OADate.setLocale(Locale.US);
    }

    @AfterEach
    void afterEach() {
        OADateTime.setLocale(originalLocale);
        OADate.setLocale(originalLocale);
        OADateTime.setDefaultZoneId(originalDefaultZoneId);
    }

    @Test
    void dateAndTimeComposeAndDecomposeWithoutLosingPublicFields() {
        OADate date = new OADate(2026, 6, 9);
        OATime time = new OATime(15, 25, 30, 456);

        OADateTime dateTime = new OADateTime(date, time);
        OADate dateAgain = new OADate(dateTime);
        OATime timeAgain = new OATime(dateTime);

        assertEquals(LocalDate.of(2026, 6, 9), dateTime.getLocalDate());
        assertEquals(LocalTime.of(15, 25, 30, 456_000_000), dateTime.getLocalTime());
        assertEquals(LocalDate.of(2026, 6, 9), dateAgain.getLocalDate());
        assertEquals(LocalTime.MIDNIGHT, dateAgain.getLocalTime());
        assertEquals(LocalDate.of(1970, 1, 1), timeAgain.getLocalDate());
        assertEquals(LocalTime.of(15, 25, 30, 456_000_000), timeAgain.getLocalTime());
    }

    @Test
    void sameDisplayedSubclassValuesCreatedInDifferentZonesUseInheritedEpochComparison() {
        OADate chicagoDate = new OADate(2026, 6, 9);
        OATime chicagoTime = new OATime(15, 25, 0, 0);

        OADateTime.setDefaultZoneId(UTC);
        OADate utcDate = new OADate(2026, 6, 9);
        OATime utcTime = new OATime(15, 25, 0, 0);

        assertNotEquals(chicagoDate.getTime(), utcDate.getTime());
        assertNotEquals(chicagoDate, utcDate);
        assertNotEquals(chicagoTime.getTime(), utcTime.getTime());
        assertNotEquals(chicagoTime, utcTime);
    }

    @Test
    void operationsOnSubclassesRemainImmutableAndReturnConcreteTypes() {
        OADate date = new OADate(2026, 12, 31);
        OATime time = new OATime(23, 59, 59, 999);

        OADateTime nextDate = date.plusDay();
        OADateTime nextTime = time.plusMilliSeconds(1);

        assertInstanceOf(OADate.class, nextDate);
        assertEquals(LocalDate.of(2027, 1, 1), nextDate.getLocalDate());
        assertEquals(LocalDate.of(2026, 12, 31), date.getLocalDate());
        assertEquals(LocalTime.MIDNIGHT, nextDate.getLocalTime());

        assertInstanceOf(OATime.class, nextTime);
        assertEquals(LocalDate.of(1970, 1, 1), nextTime.getLocalDate());
        assertEquals(LocalTime.MIDNIGHT, nextTime.getLocalTime());
        assertEquals(LocalTime.of(23, 59, 59, 999_000_000), time.getLocalTime());
    }
}
