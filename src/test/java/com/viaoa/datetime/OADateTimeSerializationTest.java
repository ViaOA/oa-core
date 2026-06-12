package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.viaoa.datetime.OADateTime.DateTimeType;

class OADateTimeSerializationTest {
    private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final ZoneId UTC = ZoneId.of("UTC");

    private ZoneId originalDefaultZoneId;
    private Locale originalLocale;

    @BeforeEach
    void beforeEach() {
        originalDefaultZoneId = OADateTime.getDefaultZoneId();
        originalLocale = Locale.getDefault();
        OADateTime.setLocale(Locale.US);
        OADate.setLocale(Locale.US);
        OADateTime.setDefaultZoneId(CHICAGO);
    }

    @AfterEach
    void afterEach() {
        OADateTime.setLocale(originalLocale);
        OADate.setLocale(originalLocale);
        OADateTime.setDefaultZoneId(originalDefaultZoneId);
    }

    @Test
    void instantRoundTripPreservesTypeAndEpochMillis() throws Exception {
        OADateTime original = new OADateTime(Instant.parse("2026-06-09T15:30:15.123Z"));

        OADateTime copy = roundTrip(original);

        assertEquals(DateTimeType.Instant, copy.getType());
        assertEquals(original.getTime(), copy.getTime());
        assertEquals(CHICAGO, copy.getZoneId());
    }

    @Test
    void zonedInstantRoundTripPreservesTypeEpochMillisAndZone() throws Exception {
        ZonedDateTime zdt = ZonedDateTime.of(2026, 3, 8, 3, 30, 15, 123_000_000, NEW_YORK);
        OADateTime original = new OADateTime(zdt);

        OADateTime copy = roundTrip(original);

        assertEquals(DateTimeType.ZonedInstant, copy.getType());
        assertEquals(original.getTime(), copy.getTime());
        assertEquals(NEW_YORK, copy.getZoneId());
        assertEquals(zdt.toLocalDateTime(), copy.getLocalDateTime());
    }

    @Test
    void explicitZoneFieldConstructorRoundTripPreservesTypeEpochMillisAndZone() throws Exception {
        OADateTime original = new OADateTime(NEW_YORK, 2026, 6, 9, 10, 30, 15, 123);
        LocalDateTime expectedFields = LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000);
        long expectedTime = expectedFields.atZone(NEW_YORK).toInstant().toEpochMilli();

        OADateTime.setDefaultZoneId(UTC);
        OADateTime copy = roundTrip(original);

        assertEquals(DateTimeType.ZonedInstant, original.getType());
        assertEquals(DateTimeType.ZonedInstant, copy.getType());
        assertEquals(expectedTime, copy.getTime());
        assertEquals(NEW_YORK, copy.getZoneId());
        assertEquals(expectedFields, copy.getLocalDateTime());
    }

    @Test
    void floatingRoundTripUnderDifferentDefaultZonePreservesWallFieldsAndAdoptsReceivingZone() throws Exception {
        OADateTime original = new OADateTime(LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000));
        assertEquals(CHICAGO, original.getZoneId());

        OADateTime.setDefaultZoneId(UTC);
        OADateTime copy = roundTrip(original);

        assertEquals(DateTimeType.Floating, copy.getType());
        assertEquals(UTC, copy.getZoneId());
        assertEquals(LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000), copy.getLocalDateTime());
        assertEquals(LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000).atZone(UTC).toInstant().toEpochMilli(), copy.getTime());
        assertNotEquals(original.getTime(), copy.getTime());

        OADateTime.setDefaultZoneId(NEW_YORK);
        assertEquals(UTC, copy.getZoneId());
        assertEquals(LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000), copy.getLocalDateTime());
    }

    @Test
    void floatingRoundTripAtDstTransitionPreservesResolvedWallFieldsAndAdoptsReceivingZone() throws Exception {
        OADateTime.setDefaultZoneId(NEW_YORK);
        OADateTime original = new OADateTime(LocalDateTime.of(2026, 3, 8, 2, 30, 0, 0));
        LocalDateTime resolvedFields = original.getLocalDateTime();

        OADateTime.setDefaultZoneId(CHICAGO);
        OADateTime copy = roundTrip(original);

        assertEquals(DateTimeType.Floating, copy.getType());
        assertEquals(CHICAGO, copy.getZoneId());
        assertEquals(resolvedFields, copy.getLocalDateTime());
        assertEquals(resolvedFields.atZone(CHICAGO).toInstant().toEpochMilli(), copy.getTime());

        OADateTime.setDefaultZoneId(UTC);
        assertEquals(CHICAGO, copy.getZoneId());
        assertEquals(resolvedFields, copy.getLocalDateTime());
    }

    @Test
    void oadateRoundTripPreservesDateOnlyInvariantAndAdoptsReceivingZone() throws Exception {
        OADate original = new OADate(2026, 6, 9);

        OADateTime.setDefaultZoneId(UTC);
        OADate copy = roundTrip(original);

        assertEquals(DateTimeType.Floating, copy.getType());
        assertEquals(UTC, copy.getZoneId());
        assertEquals(LocalDate.of(2026, 6, 9), copy.getLocalDate());
        assertEquals(LocalTime.MIDNIGHT, copy.getLocalTime());

        OADateTime.setDefaultZoneId(NEW_YORK);
        assertEquals(UTC, copy.getZoneId());
        assertEquals(LocalDate.of(2026, 6, 9), copy.getLocalDate());
        assertEquals(LocalTime.MIDNIGHT, copy.getLocalTime());
    }

    @Test
    void oatimeRoundTripPreservesTimeOnlyInvariantAndAdoptsReceivingZone() throws Exception {
        OATime original = new OATime(15, 25, 30, 456);

        OADateTime.setDefaultZoneId(UTC);
        OATime copy = roundTrip(original);

        assertEquals(DateTimeType.Floating, copy.getType());
        assertEquals(UTC, copy.getZoneId());
        assertEquals(LocalDate.of(1970, 1, 1), copy.getLocalDate());
        assertEquals(LocalTime.of(15, 25, 30, 456_000_000), copy.getLocalTime());

        OADateTime.setDefaultZoneId(NEW_YORK);
        assertEquals(UTC, copy.getZoneId());
        assertEquals(LocalDate.of(1970, 1, 1), copy.getLocalDate());
        assertEquals(LocalTime.of(15, 25, 30, 456_000_000), copy.getLocalTime());
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(value);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            return (T) in.readObject();
        }
    }
}
