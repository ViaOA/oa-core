package com.viaoa.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;
import com.viaoa.datetime.OATime;

class OAConverterJavaTimeTest {

    private static final long EPOCH_MILLIS = 1714979289123L;
    private static final Instant INSTANT = Instant.ofEpochMilli(EPOCH_MILLIS);
    private TimeZone originalTimeZone;

    @BeforeEach
    void setUtcTimeZone() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    void localDateFromStringDateOADateAndEpoch() {
        assertEquals(LocalDate.of(2024, 5, 6), OAConverter.convert(LocalDate.class, "2024-05-06", "yyyy-MM-dd"));
        assertEquals(LocalDate.of(2024, 5, 6), OAConverter.convert(LocalDate.class, new OADate(2024, 4, 6)));
        assertNull(OAConverter.convert(LocalDate.class, new java.util.Date(EPOCH_MILLIS)));
        assertEquals(LocalDate.of(2024, 5, 6), OAConverter.convert(LocalDate.class, EPOCH_MILLIS));
    }

    @Test
    void localTimeFromStringOATimeAndEpoch() {
        LocalTime withoutMillis = LocalTime.of(7, 8, 9);
        LocalTime withMillis = LocalTime.of(7, 8, 9, 123_000_000);

        assertEquals(withMillis, OAConverter.convert(LocalTime.class, "07:08:09.123", "HH:mm:ss.SSS"));
        assertEquals(withoutMillis, OAConverter.convert(LocalTime.class, new OATime(7, 8, 9, 123)));
        assertEquals(withMillis, OAConverter.convert(LocalTime.class, EPOCH_MILLIS));
    }

    @Test
    void localDateTimeFromStringDateOADateTimeAndEpoch() {
        LocalDateTime expected = LocalDateTime.of(2024, 5, 6, 7, 8, 9, 123_000_000);
        OADateTime oaDateTime = OAConverter.convert(OADateTime.class, "2024-05-06 07:08:09.123", "yyyy-MM-dd HH:mm:ss.SSS");

        assertEquals(expected, OAConverter.convert(LocalDateTime.class, "2024-05-06 07:08:09.123", "yyyy-MM-dd HH:mm:ss.SSS"));
        assertEquals(LocalDateTime.of(2024, 5, 6, 0, 0), OAConverter.convert(LocalDateTime.class, new OADate(2024, 4, 6)));
        assertEquals(expected, OAConverter.convert(LocalDateTime.class, oaDateTime));
        assertEquals(expected, OAConverter.convert(LocalDateTime.class, EPOCH_MILLIS));
    }

    @Test
    void instantFromStringDateTemporalAndEpoch() {
        OADateTime oaDateTime = OAConverter.convert(OADateTime.class, "2024-05-06 07:08:09.123", "yyyy-MM-dd HH:mm:ss.SSS");

        assertEquals(Instant.ofEpochSecond(1714979289L), OAConverter.convert(Instant.class, "2024-05-06 07:08:09.123", "yyyy-MM-dd HH:mm:ss.SSS"));
        assertEquals(INSTANT, OAConverter.convert(Instant.class, new java.util.Date(EPOCH_MILLIS)));
        assertEquals(Instant.ofEpochSecond(1714979289L), OAConverter.convert(Instant.class, oaDateTime));
        assertEquals(INSTANT, OAConverter.convert(Instant.class, EPOCH_MILLIS));
    }

    @Test
    void zonedDateTimeFromStringDateTemporalAndEpoch() {
        ZonedDateTime expected = ZonedDateTime.of(2024, 5, 6, 7, 8, 9, 123_000_000, ZoneId.of("UTC"));
        OADateTime oaDateTime = OAConverter.convert(OADateTime.class, "2024-05-06 07:08:09.123", "yyyy-MM-dd HH:mm:ss.SSS");

        assertEquals(expected, OAConverter.convert(ZonedDateTime.class, "2024-05-06 07:08:09.123", "yyyy-MM-dd HH:mm:ss.SSS"));
        assertEquals(expected, OAConverter.convert(ZonedDateTime.class, oaDateTime));
        assertEquals(expected, OAConverter.convert(ZonedDateTime.class, INSTANT));
        assertEquals(expected, OAConverter.convert(ZonedDateTime.class, EPOCH_MILLIS));
    }

    @Test
    void localTimeFromDateAndLocalDateTimeDocumentsCurrentBehavior() {
        assertNull(OAConverter.convert(LocalTime.class, new java.util.Date(EPOCH_MILLIS)));
        assertNull(OAConverter.convert(LocalTime.class, LocalDateTime.of(2024, 5, 6, 7, 8, 9, 123_000_000)));
        assertNull(OAConverter.convert(LocalTime.class, new OADateTime(EPOCH_MILLIS)));
    }

    @Test
    void zonedDateTimeToStringDocumentsZonePreservationOrLoss() {
        ZonedDateTime newYork = ZonedDateTime.of(2024, 5, 6, 7, 8, 9, 0, ZoneId.of("America/New_York"));

        assertEquals("2024-05-06 11:08 UTC", OAConverter.convert(String.class, newYork, "yyyy-MM-dd HH:mm z"));
    }

    @Test
    void zoneIdRoundTripKnownIds() {
        ZoneId zoneId = OAConverter.convert(ZoneId.class, "America/Chicago");

        assertEquals(ZoneId.of("America/Chicago"), zoneId);
        assertEquals("America/Chicago", OAConverter.convert(String.class, zoneId));
        assertEquals(ZoneId.of("UTC"), OAConverter.convert(ZoneId.class, TimeZone.getTimeZone("UTC")));
    }

    @Test
    void timeZoneRoundTripKnownIds() {
        TimeZone timeZone = OAConverter.convert(TimeZone.class, "UTC");

        assertNotNull(timeZone);
        assertEquals("UTC", timeZone.getID());
        assertNotNull(OAConverter.convert(String.class, timeZone));
    }

    @Test
    void invalidZoneReturnsNullOrThrowsAccordingToCurrentContract() {
        assertNull(OAConverter.convert(ZoneId.class, "No/Such_Zone"));
        assertNull(OAConverter.convert(TimeZone.class, "No/Such_Zone"));
    }

    @Test
    void localDateTimeByteArrayFailureContract() {
        byte[] compactBytes = BigInteger.valueOf(1L).toByteArray();

        assertThrows(BufferUnderflowException.class, () -> OAConverter.convert(LocalDateTime.class, compactBytes));
    }

    @Test
    void currentJvmDefaultTimezoneBehaviorIsDeterministic() {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));
            ZoneId zone = ZoneId.systemDefault();

            assertEquals(LocalDateTime.ofInstant(INSTANT, zone), OAConverter.convert(LocalDateTime.class, EPOCH_MILLIS));
            assertEquals(LocalDate.ofInstant(INSTANT, zone), OAConverter.convert(LocalDate.class, EPOCH_MILLIS));
            assertEquals(LocalTime.ofInstant(INSTANT, zone), OAConverter.convert(LocalTime.class, INSTANT));
            assertEquals(ZonedDateTime.ofInstant(INSTANT, zone), OAConverter.convert(ZonedDateTime.class, INSTANT));
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Test
    void epochConversionRoundTripBehavior() {
        assertEquals(EPOCH_MILLIS, OAConverter.convert(Instant.class, EPOCH_MILLIS).toEpochMilli());
        assertEquals(EPOCH_MILLIS, Timestamp.from(OAConverter.convert(Instant.class, EPOCH_MILLIS)).getTime());
        assertEquals(EPOCH_MILLIS, OAConverter.convert(Timestamp.class, OAConverter.convert(LocalDateTime.class, EPOCH_MILLIS)).getTime());
    }
}
