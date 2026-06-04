package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Focused daylight-saving-time tests for OADateTime.
 * <p>
 * These tests intentionally use America/Chicago because it has normal US DST
 * spring-forward and fall-back behavior.
 */
public class OADateTimeDaylightSavingTest {
    private TimeZone originalDefaultTimeZone;
    private TimeZone originalJvmTimeZone;

    private static final TimeZone CHICAGO = TimeZone.getTimeZone("America/Chicago");
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @BeforeEach
    public void beforeEach() {
        originalDefaultTimeZone = OADateTime.getDefaultTimeZone();
        originalJvmTimeZone = TimeZone.getDefault();

        TimeZone.setDefault(CHICAGO);
        OADateTime.setDefaultTimeZone(CHICAGO);
    }

    @AfterEach
    public void afterEach() {
        TimeZone.setDefault(originalJvmTimeZone);
        OADateTime.setDefaultTimeZone(originalDefaultTimeZone);
    }

    @Test
    public void springForwardInstantRoundTripPreservesEpochMillis() {
        ZonedDateTime zdt = ZonedDateTime.of(
            2026, 3, 8, 1, 59, 59, 999_000_000,
            ZoneId.of("America/Chicago")
        );

        OADateTime dt = new OADateTime(zdt);

        assertEquals(zdt.toInstant().toEpochMilli(), dt.getTime());
        assertEquals(zdt.toInstant(), dt.getInstant());
        assertEquals(CHICAGO.getID(), dt.getTimeZone().getID());

        assertEquals(2026, dt.getYear());
        assertEquals(Calendar.MARCH, dt.getMonth());
        assertEquals(8, dt.getDay());
        assertEquals(1, dt.get24Hour());
        assertEquals(59, dt.getMinute());
        assertEquals(59, dt.getSecond());
        assertEquals(999, dt.getMilliSecond());
    }

    @Test
    public void springForwardAddOneMillisecondSkipsToThreeAM() {
        OADateTime dt = new OADateTime(
            ZonedDateTime.of(2026, 3, 8, 1, 59, 59, 999_000_000, ZoneId.of("America/Chicago"))
        );

        OADateTime dt2 = dt.addMilliSeconds(1);

        assertEquals(2026, dt2.getYear());
        assertEquals(Calendar.MARCH, dt2.getMonth());
        assertEquals(8, dt2.getDay());
        assertEquals(3, dt2.get24Hour());
        assertEquals(0, dt2.getMinute());
        assertEquals(0, dt2.getSecond());
        assertEquals(0, dt2.getMilliSecond());
        assertEquals(dt.getTime() + 1, dt2.getTime());
    }

    @Test
    public void springForwardNonexistentLocalTimeDoesNotSilentlyBecomeTwoThirty() {
        assertThrows(RuntimeException.class, () -> {
            new OADateTime(2026, Calendar.MARCH, 8, 2, 30, 0, 0);
        });
    }

    @Test
    public void fallBackFirstOneThirtyPreservesInstantAndZone() {
        ZonedDateTime firstOneThirty = ZonedDateTime.ofLocal(
            LocalDateTime.of(2026, 11, 1, 1, 30, 0, 123_000_000),
            ZoneId.of("America/Chicago"),
            java.time.ZoneOffset.ofHours(-5)
        );

        OADateTime dt = new OADateTime(firstOneThirty);

        assertEquals(firstOneThirty.toInstant().toEpochMilli(), dt.getTime());
        assertEquals(firstOneThirty.toInstant(), dt.getInstant());
        assertEquals(CHICAGO.getID(), dt.getTimeZone().getID());

        assertEquals(1, dt.get24Hour());
        assertEquals(30, dt.getMinute());
        assertEquals(123, dt.getMilliSecond());
    }

    @Test
    public void fallBackSecondOneThirtyPreservesInstantAndZone() {
        ZonedDateTime secondOneThirty = ZonedDateTime.ofLocal(
            LocalDateTime.of(2026, 11, 1, 1, 30, 0, 456_000_000),
            ZoneId.of("America/Chicago"),
            java.time.ZoneOffset.ofHours(-6)
        );

        OADateTime dt = new OADateTime(secondOneThirty);

        assertEquals(secondOneThirty.toInstant().toEpochMilli(), dt.getTime());
        assertEquals(secondOneThirty.toInstant(), dt.getInstant());
        assertEquals(CHICAGO.getID(), dt.getTimeZone().getID());

        assertEquals(1, dt.get24Hour());
        assertEquals(30, dt.getMinute());
        assertEquals(456, dt.getMilliSecond());
    }

    @Test
    public void fallBackTwoOneThirtyValuesHaveDifferentInstants() {
        ZonedDateTime firstOneThirty = ZonedDateTime.ofLocal(
            LocalDateTime.of(2026, 11, 1, 1, 30),
            ZoneId.of("America/Chicago"),
            java.time.ZoneOffset.ofHours(-5)
        );
        ZonedDateTime secondOneThirty = ZonedDateTime.ofLocal(
            LocalDateTime.of(2026, 11, 1, 1, 30),
            ZoneId.of("America/Chicago"),
            java.time.ZoneOffset.ofHours(-6)
        );

        OADateTime dt1 = new OADateTime(firstOneThirty);
        OADateTime dt2 = new OADateTime(secondOneThirty);

        assertNotEquals(dt1.getTime(), dt2.getTime());
        assertEquals(60L * 60L * 1000L, dt2.getTime() - dt1.getTime());

        assertEquals(dt1.getYear(), dt2.getYear());
        assertEquals(dt1.getMonth(), dt2.getMonth());
        assertEquals(dt1.getDay(), dt2.getDay());
        assertEquals(dt1.get24Hour(), dt2.get24Hour());
        assertEquals(dt1.getMinute(), dt2.getMinute());
    }

    @Test
    public void convertToPreservesInstantButChangesDisplayedFields() {
        OADateTime chicago = new OADateTime(
            ZonedDateTime.of(2026, 6, 2, 10, 15, 30, 789_000_000, ZoneId.of("America/Chicago"))
        );

        OADateTime ny = chicago.convertTo(NEW_YORK);

        assertEquals(chicago.getTime(), ny.getTime(), "convertTo must preserve _time/instant");
        assertEquals(NEW_YORK.getID(), ny.getTimeZone().getID());

        assertEquals(11, ny.get24Hour());
        assertEquals(15, ny.getMinute());
        assertEquals(30, ny.getSecond());
        assertEquals(789, ny.getMilliSecond());
    }

    @Test
    public void setTimeZonePreservesWallClockFieldsButChangesInstant() {
        OADateTime chicago = new OADateTime(
            ZonedDateTime.of(2026, 6, 2, 10, 15, 30, 789_000_000, ZoneId.of("America/Chicago"))
        );

        long original = chicago.getTime();

        chicago.setTimeZone(NEW_YORK);

        assertEquals(NEW_YORK.getID(), chicago.getTimeZone().getID());

        assertEquals(2026, chicago.getYear());
        assertEquals(Calendar.JUNE, chicago.getMonth());
        assertEquals(2, chicago.getDay());
        assertEquals(10, chicago.get24Hour());
        assertEquals(15, chicago.getMinute());
        assertEquals(30, chicago.getSecond());
        assertEquals(789, chicago.getMilliSecond());

        assertEquals(original - (60L * 60L * 1000L), chicago.getTime());
    }

    @Test
    public void getZonedDateTimeUsesInstantAndZoneAcrossDst() {
        ZonedDateTime zdt = ZonedDateTime.of(
            2026, 3, 8, 3, 15, 30, 789_000_000,
            ZoneId.of("America/Chicago")
        );

        OADateTime dt = new OADateTime(zdt);
        ZonedDateTime actual = dt.getZonedDateTime();

        assertEquals(zdt.toInstant(), actual.toInstant());
        assertEquals(zdt.getZone(), actual.getZone());
        assertEquals(789_000_000, actual.getNano());
    }

    @Test
    public void localDateTimeConstructorUsesOaDefaultTimeZoneNotJvmDefault() {
        TimeZone.setDefault(UTC);
        OADateTime.setDefaultTimeZone(CHICAGO);

        LocalDateTime ldt = LocalDateTime.of(2026, 6, 2, 10, 15, 30, 789_000_000);
        OADateTime dt = new OADateTime(ldt);

        long expected = ldt.atZone(ZoneId.of("America/Chicago")).toInstant().toEpochMilli();

        assertEquals(expected, dt.getTime());
        assertEquals(10, dt.get24Hour());
        assertEquals(15, dt.getMinute());
        assertEquals(789, dt.getMilliSecond());
    }

    @Test
    public void serializationPreservesTimezoneAwareDateTimeAcrossJvmDefaultTimezoneChange() throws Exception {
        OADateTime original = new OADateTime(
            ZonedDateTime.of(2026, 3, 8, 3, 15, 30, 789_000_000, ZoneId.of("America/Chicago"))
        );

        byte[] bytes = serialize(original);

        TimeZone.setDefault(UTC);
        OADateTime.setDefaultTimeZone(UTC);

        OADateTime copy = deserialize(bytes);

        assertEquals(original.getTimeZone().getID(), copy.getTimeZone().getID());
        assertEquals(original.getYear(), copy.getYear());
        assertEquals(original.getMonth(), copy.getMonth());
        assertEquals(original.getDay(), copy.getDay());
        assertEquals(original.get24Hour(), copy.get24Hour());
        assertEquals(original.getMinute(), copy.getMinute());
        assertEquals(original.getSecond(), copy.getSecond());
        assertEquals(original.getMilliSecond(), copy.getMilliSecond());
        assertEquals(original.getTime(), copy.getTime());
    }

    @Test
    public void addDaysAcrossSpringForwardUsesCalendarDaySemantics() {
        OADateTime before = new OADateTime(
            ZonedDateTime.of(2026, 3, 7, 10, 0, 0, 0, ZoneId.of("America/Chicago"))
        );

        OADateTime after = before.addDays(1);

        assertEquals(2026, after.getYear());
        assertEquals(Calendar.MARCH, after.getMonth());
        assertEquals(8, after.getDay());
        assertEquals(10, after.get24Hour());

        assertEquals(23L * 60L * 60L * 1000L, after.getTime() - before.getTime());
    }

    @Test
    public void addDaysAcrossFallBackUsesCalendarDaySemantics() {
        OADateTime before = new OADateTime(
            ZonedDateTime.of(2026, 10, 31, 10, 0, 0, 0, ZoneId.of("America/Chicago"))
        );

        OADateTime after = before.addDays(1);

        assertEquals(2026, after.getYear());
        assertEquals(Calendar.NOVEMBER, after.getMonth());
        assertEquals(1, after.getDay());
        assertEquals(10, after.get24Hour());

        assertEquals(25L * 60L * 60L * 1000L, after.getTime() - before.getTime());
    }

    private static byte[] serialize(OADateTime dt) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(baos)) {
            out.writeObject(dt);
        }
        return baos.toByteArray();
    }

    private static OADateTime deserialize(byte[] bytes) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (OADateTime) in.readObject();
        }
    }

    @Test
    public void withTimeZonePreservesOriginalAndKeepsWallClockFields() {
        OADateTime original = new OADateTime(
            ZonedDateTime.of(2026, 6, 2, 10, 15, 30, 789_000_000, ZoneId.of("America/Chicago"))
        );

        OADateTime changed = original.withTimeZone(NEW_YORK);

        assertNotSame(original, changed);
        assertEquals(CHICAGO.getID(), original.getTimeZone().getID());
        assertEquals(NEW_YORK.getID(), changed.getTimeZone().getID());

        assertEquals(10, changed.get24Hour());
        assertEquals(15, changed.getMinute());
        assertEquals(30, changed.getSecond());
        assertEquals(789, changed.getMilliSecond());

        assertEquals(original.getTime() - (60L * 60L * 1000L), changed.getTime());
    }

    @Test
    public void convertedToPreservesOriginalAndInstant() {
        OADateTime original = new OADateTime(
            ZonedDateTime.of(2026, 6, 2, 10, 15, 30, 789_000_000, ZoneId.of("America/Chicago"))
        );

        OADateTime changed = original.convertTo(NEW_YORK);

        assertNotSame(original, changed);
        assertEquals(CHICAGO.getID(), original.getTimeZone().getID());
        assertEquals(NEW_YORK.getID(), changed.getTimeZone().getID());

        assertEquals(original.getTime(), changed.getTime());
        assertEquals(11, changed.get24Hour());
        assertEquals(15, changed.getMinute());
        assertEquals(789, changed.getMilliSecond());
    }

    @Test
    public void withHourAcrossSpringForwardDoesNotMutateOriginal() {
        OADateTime original = new OADateTime(
            ZonedDateTime.of(2026, 3, 8, 1, 30, 0, 0, ZoneId.of("America/Chicago"))
        );

        assertThrows(RuntimeException.class, () -> {
            original.withHour(2); // 02:30 does not exist on spring-forward day
        });

        assertEquals(1, original.get24Hour());
        assertEquals(30, original.getMinute());
    }

    @Test
    public void withDayAcrossSpringForwardReturnsNewCalendarDayValue() {
        OADateTime original = new OADateTime(
            ZonedDateTime.of(2026, 3, 7, 10, 0, 0, 0, ZoneId.of("America/Chicago"))
        );

        OADateTime changed = original.withDay(8);

        assertNotSame(original, changed);
        assertEquals(7, original.getDay());
        assertEquals(8, changed.getDay());
        assertEquals(10, changed.get24Hour());

        assertEquals(23L * 60L * 60L * 1000L, changed.getTime() - original.getTime());
    }

    @Test
    public void instantConstructorUsesOaDefaultTimezoneForFieldsDuringDst() {
        OADateTime.setDefaultTimeZone(CHICAGO);

        Instant instant = ZonedDateTime.of(
            2026, 6, 2, 10, 15, 30, 789_000_000,
            ZoneId.of("America/Chicago")
        ).toInstant();

        OADateTime dt = new OADateTime(instant);

        assertEquals(instant.toEpochMilli(), dt.getTime());
        assertEquals(2026, dt.getYear());
        assertEquals(Calendar.JUNE, dt.getMonth());
        assertEquals(2, dt.getDay());
        assertEquals(10, dt.get24Hour());
        assertEquals(15, dt.getMinute());
        assertEquals(30, dt.getSecond());
        assertEquals(789, dt.getMilliSecond());
    }
}
