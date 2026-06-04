package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.time.*;
import java.util.Calendar;
import java.util.TimeZone;


public class OADateTimeInvariantTest {
    private TimeZone origJvmTz;
    private TimeZone origOaTz;

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone CHICAGO = TimeZone.getTimeZone("America/Chicago");
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");
    private static final TimeZone LOS_ANGELES = TimeZone.getTimeZone("America/Los_Angeles");

    @BeforeEach
    public void before() {
        origJvmTz = TimeZone.getDefault();
        origOaTz = OADateTime.getDefaultTimeZone();
        TimeZone.setDefault(UTC);
        OADateTime.setDefaultTimeZone(UTC);
    }

    @AfterEach
    public void after() {
        TimeZone.setDefault(origJvmTz);
        OADateTime.setDefaultTimeZone(origOaTz);
    }

    @SuppressWarnings("unchecked")
    private static <T> T serializeRoundTrip(T obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            return (T) in.readObject();
        }
    }

    @Test
    public void setTimeZonePreservesWallClockFieldsAndChangesInstant() {
        OADateTime dt = new OADateTime(2026, Calendar.JANUARY, 15, 10, 30, 45, 123);
        long before = dt.getTime();

        dt.setTimeZone(NEW_YORK);

        assertEquals(2026, dt.getYear());
        assertEquals(Calendar.JANUARY, dt.getMonth());
        assertEquals(15, dt.getDay());
        assertEquals(10, dt.get24Hour());
        assertEquals(30, dt.getMinute());
        assertEquals(45, dt.getSecond());
        assertEquals(123, dt.getMilliSecond());
        assertEquals(NEW_YORK.getID(), dt.getTimeZone().getID());
        assertNotEquals(before, dt.getTime(), "setTimeZone must change _time to preserve wall-clock fields");
    }

    @Test
    public void convertToPreservesInstantAndChangesDisplayedFields() {
        OADateTime dt = new OADateTime(2026, Calendar.JANUARY, 15, 10, 0, 0, 0);
        long before = dt.getTime();

        OADateTime converted = dt.convertTo(CHICAGO);

        assertEquals(before, converted.getTime(), "convertTo must preserve instant");
        assertEquals(CHICAGO.getID(), converted.getTimeZone().getID());
        assertEquals(4, converted.get24Hour(), "UTC 10:00 should display as Chicago 04:00 in January");
    }

    @Test
    public void instantAndZonedDateTimeUseStoredEpochMillis() {
        OADateTime dt = new OADateTime(1234567890123L, LOS_ANGELES);

        assertEquals(Instant.ofEpochMilli(1234567890123L), dt.getInstant());
        assertEquals(1234567890123L, dt.getZonedDateTime().toInstant().toEpochMilli());
        assertEquals(LOS_ANGELES.toZoneId(), dt.getZonedDateTime().getZone());
    }

    @Test
    public void localDateTimeConstructorUsesOaDefaultTimezone() {
        OADateTime.setDefaultTimeZone(CHICAGO);

        LocalDateTime ldt = LocalDateTime.of(2026, 6, 1, 10, 0, 0);
        OADateTime dt = new OADateTime(ldt);

        long expected = ldt.atZone(CHICAGO.toZoneId()).toInstant().toEpochMilli();
        assertEquals(expected, dt.getTime());
    }

    @Test
    public void zonedDateTimeConstructorPreservesInstantAndZone() {
        ZonedDateTime zdt = ZonedDateTime.of(2026, 6, 1, 10, 15, 30, 123000000, NEW_YORK.toZoneId());

        OADateTime dt = new OADateTime(zdt);

        assertEquals(zdt.toInstant().toEpochMilli(), dt.getTime());
        assertEquals(NEW_YORK.getID(), dt.getTimeZone().getID());
        assertEquals(10, dt.get24Hour());
        assertEquals(15, dt.getMinute());
        assertEquals(30, dt.getSecond());
        assertEquals(123, dt.getMilliSecond());
    }

    @Test
    public void parsingRejectsTrailingGarbage() {
        assertNull(OADateTime.valueOf("2026-01-01xyz"));
        assertNull(OADate.valueOf("2026-01-01xyz"));
        assertNull(OATime.valueOf("10:30xyz"));
    }

    @Test
    public void parsingRejectsLenientRolloverValues() {
        assertNull(OADate.valueOf("2026-02-31"));
        assertNull(OATime.valueOf("25:00"));
        assertNull(OADateTime.valueOf("2026-13-01 10:00"));
    }

    @Test
    public void serializationPreservesExplicitTimezoneForOADateTime() throws Exception {
        OADateTime dt = new OADateTime(2026, Calendar.MARCH, 8, 1, 30, 0, 0);
        dt.setTimeZone(CHICAGO);

        OADateTime copy = serializeRoundTrip(dt);

        assertEquals(dt.getTime(), copy.getTime());
        assertEquals(CHICAGO.getID(), copy.getTimeZone().getID());
        assertEquals(dt.getYear(), copy.getYear());
        assertEquals(dt.getMonth(), copy.getMonth());
        assertEquals(dt.getDay(), copy.getDay());
        assertEquals(dt.get24Hour(), copy.get24Hour());
    }

    @Test
    public void compareToNonComparableReturnsTwoAndAfterIsIntentionalTrue() {
        OADateTime dt = new OADateTime(2026, Calendar.JANUARY, 1);

        assertEquals(2, dt.compareTo(new Object()));
        assertTrue(dt.after(new Object()), "OA contract: valid date sorts after non-date value");
    }

    @Test
    public void betweenMilliSecondsDoesNotMutateArgument() {
        OADateTime d1 = new OADateTime(2026, Calendar.JANUARY, 1, 10, 0, 0, 0);
        OADateTime d2 = new OADateTime(2026, Calendar.JANUARY, 1, 10, 0, 1, 250);
        long before = d2.getTime();

        assertEquals(1250L, d1.betweenMilliSeconds(d2));
        assertEquals(before, d2.getTime(), "betweenMilliSeconds must not mutate argument");
    }

    @Test
    public void betweenSecondsMinutesHoursUseElapsedInstantSemantics() {
        OADateTime d1 = new OADateTime(2026, Calendar.JANUARY, 1, 10, 0, 0, 0);
        OADateTime d2 = new OADateTime(2026, Calendar.JANUARY, 1, 12, 30, 45, 0);

        assertEquals(9045, d1.betweenSeconds(d2));
        assertEquals(150, d1.betweenMinutes(d2));
        assertEquals(2, d1.betweenHours(d2));
    }

    @Test
    public void betweenHoursIsDstSafeForSpringForward() {
        OADateTime.setDefaultTimeZone(CHICAGO);

        OADateTime d1 = new OADateTime(2026, Calendar.MARCH, 8, 1, 0, 0, 0);
        OADateTime d2 = new OADateTime(2026, Calendar.MARCH, 8, 3, 0, 0, 0);

        assertEquals(1, d1.betweenHours(d2), "Spring-forward 01:00 to 03:00 is one elapsed hour");
        assertEquals(60, d1.betweenMinutes(d2));
    }

    @Test
    public void betweenDaysUsesCalendarDaySemanticsAcrossSpringForward() {
        OADateTime.setDefaultTimeZone(CHICAGO);

        OADateTime d1 = new OADateTime(2026, Calendar.MARCH, 8, 0, 0, 0, 0);
        OADateTime d2 = new OADateTime(2026, Calendar.MARCH, 9, 0, 0, 0, 0);

        assertEquals(1, d1.betweenDays(d2), "Spring-forward midnight to midnight is one calendar day");
    }

    @Test
    public void betweenDaysDoesNotMutateArgument() {
        OADateTime d1 = new OADateTime(2026, Calendar.JANUARY, 1, 12, 34, 56, 789);
        OADateTime d2 = new OADateTime(2026, Calendar.JANUARY, 3, 23, 45, 10, 111);
        long before = d2.getTime();

        assertEquals(2, d1.betweenDays(d2));
        assertEquals(before, d2.getTime(), "betweenDays must not mutate argument");
    }
}