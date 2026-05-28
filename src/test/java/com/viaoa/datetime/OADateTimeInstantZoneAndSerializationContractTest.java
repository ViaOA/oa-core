package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OADateTimeInstantZoneAndSerializationContractTest {

    private TimeZone originalJvmTimeZone;
    private TimeZone originalOaTimeZone;

    @BeforeEach
    void setUtc() {
        originalJvmTimeZone = TimeZone.getDefault();
        originalOaTimeZone = OADateTime.getDefaultTimeZone();
        TimeZone utc = TimeZone.getTimeZone("UTC");
        TimeZone.setDefault(utc);
        OADateTime.setDefaultTimeZone(utc);
    }

    @AfterEach
    void restore() {
        OADateTime.setDefaultTimeZone(originalOaTimeZone);
        TimeZone.setDefault(originalJvmTimeZone);
    }

    @Test
    void getInstantCurrentlyDropsMillisecondPrecision() {
        OADateTime dt = new OADateTime(1714979289123L);

        assertEquals(Instant.ofEpochSecond(1714979289L), dt.getInstant(),
            "Current getInstant rebuilds from fields and drops millisecond precision.");
        assertNotEquals(Instant.ofEpochMilli(1714979289123L), dt.getInstant());
    }

    @Test
    void getLocalDateTimeCurrentlyDropsMillisecondPrecision() {
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 10, 30, 15, 123);

        assertEquals(0, dt.getLocalDateTime().getNano(),
            "Current LocalDateTime conversion divides milliseconds instead of multiplying to nanos.");
    }

    @Test
    void zonedDateTimeConstructorCurrentlyLosesOriginalZoneIdentity() {
        ZonedDateTime zdt = ZonedDateTime.of(2026, 5, 18, 10, 0, 0, 0, ZoneId.of("America/New_York"));

        OADateTime dt = new OADateTime(zdt);

        assertEquals(TimeZone.getTimeZone("UTC"), dt.getTimeZone(),
            "Current constructor preserves instant but does not keep source ZonedDateTime zone.");
    }

    @Test
    void convertToChangesZoneWhilePreservingInstant() {
        OADateTime dt = new OADateTime(1714979289123L);
        OADateTime chicago = dt.convertTo(TimeZone.getTimeZone("America/Chicago"));

        assertEquals(dt.getTime(), chicago.getTime());
        assertEquals("America/Chicago", chicago.getTimeZone().getID());
    }

    @Test
    void dateSerializationRoundTripPreservesSemanticDateFieldsInSameJvmZone() throws Exception {
        OADate date = new OADate(2026, Calendar.MAY, 18);
        date.setTimeZone(TimeZone.getTimeZone("America/Chicago"));

        OADate copy = roundTrip(date, OADate.class);

        assertEquals(date.getYear(), copy.getYear());
        assertEquals(date.getMonth(), copy.getMonth());
        assertEquals(date.getDay(), copy.getDay());
        assertEquals(date.getTimeZone().getID(), copy.getTimeZone().getID());
    }

    @Test
    void timeSerializationRoundTripPreservesClockFieldsInSameJvmZone() throws Exception {
        OATime time = new OATime(10, 20, 30, 456);
        time.setTimeZone(TimeZone.getTimeZone("America/Chicago"));

        OATime copy = roundTrip(time, OATime.class);

        assertEquals(time.get24Hour(), copy.get24Hour());
        assertEquals(time.getMinute(), copy.getMinute());
        assertEquals(time.getSecond(), copy.getSecond());
        assertEquals(time.getMilliSecond(), copy.getMilliSecond());
        assertEquals(time.getTimeZone().getID(), copy.getTimeZone().getID());
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value, Class<T> type) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(value);
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            Object obj = ois.readObject();
            assertInstanceOf(type, obj);
            return (T) obj;
        }
    }
}
