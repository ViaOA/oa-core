package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.TimeZone;

public class OATimeInvariantTest {
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

    private static void assertFixedDate(OATime t) {
        assertEquals(1970, t.getYear());
        assertEquals(Calendar.JANUARY, t.getMonth());
        assertEquals(1, t.getDay());
    }

    @Test
    public void constructorsAlwaysClearDate() {
        assertFixedDate(new OATime(10, 30, 45, 123));

        OADateTime dt = new OADateTime(2026, Calendar.JUNE, 15, 13, 45, 30, 250);
        assertFixedDate(new OATime(dt));

        assertFixedDate(new OATime(LocalTime.of(9, 8, 7, 123000000)));
    }

    @Test
    public void localTimeConstructorPreservesMilliseconds() {
        OATime t = new OATime(LocalTime.of(9, 8, 7, 456000000));

        assertEquals(9, t.get24Hour());
        assertEquals(8, t.getMinute());
        assertEquals(7, t.getSecond());
        assertEquals(456, t.getMilliSecond());
        assertFixedDate(t);
    }

    @Test
    public void setTimeZonePreservesTimeAndClearsDate() {
        OATime t = new OATime(10, 30, 45, 123);
        t.setTimeZone(CHICAGO);

        assertEquals(10, t.get24Hour());
        assertEquals(30, t.getMinute());
        assertEquals(45, t.getSecond());
        assertEquals(123, t.getMilliSecond());
        assertEquals(CHICAGO.getID(), t.getTimeZone().getID());
        assertFixedDate(t);
    }

    @Test
    public void convertToReturnsOATimeAndClearsDate() {
        OATime t = new OATime(10, 30, 0, 0);
        t.setTimeZone(UTC);

        OATime result = (OATime) t.convertTo(LOS_ANGELES);

        assertTrue(result instanceof OATime);
        assertEquals(LOS_ANGELES.getID(), result.getTimeZone().getID());
        assertFixedDate(result);
    }

    @Test
    public void dateSettersAreNoOp() {
        OATime t = new OATime(10, 30, 45, 123);
        long before = t.getTime();

        t.setYear(2026);
        t.setMonth(Calendar.JUNE);
        t.setMonthValue(6);
        t.setDay(15);
        t.setDate(2026, Calendar.JUNE, 15);

        assertEquals(before, t.getTime());
        assertFixedDate(t);
    }

    @Test
    public void addSubtractDateUnitsReturnOATimeAndClearDate() {
        OATime t = new OATime(10, 30, 45, 123);

        OATime y = (OATime) t.addYears(1);
        OATime m = (OATime) t.addMonths(1);
        OATime d = (OATime) t.addDays(1);

        assertFixedDate(y);
        assertFixedDate(m);
        assertFixedDate(d);
        assertEquals(t.get24Hour(), y.get24Hour());
        assertEquals(t.getMinute(), m.getMinute());
        assertEquals(t.getSecond(), d.getSecond());
    }

    @Test
    public void supportedWithTimeFieldsReturnOATimeAndClearDate() {
        OATime t = new OATime(10, 30, 45, 123);

        OATime h = (OATime) t.withHour(11);
        OATime min = (OATime) t.withMinute(31);
        OATime sec = (OATime) t.withSecond(46);
        OATime ms = (OATime) t.withMilliSecond(124);
        OATime time = (OATime) t.withTime(12, 32, 47, 125);

        assertEquals(11, h.get24Hour());
        assertEquals(31, min.getMinute());
        assertEquals(46, sec.getSecond());
        assertEquals(124, ms.getMilliSecond());
        assertEquals(12, time.get24Hour());
        assertEquals(32, time.getMinute());
        assertEquals(47, time.getSecond());
        assertEquals(125, time.getMilliSecond());

        assertFixedDate(h);
        assertFixedDate(min);
        assertFixedDate(sec);
        assertFixedDate(ms);
        assertFixedDate(time);
    }

    @Test
    public void unsupportedWithDateFieldsReturnOATimeUnchangedAndClearDate() {
        OATime t = new OATime(10, 30, 45, 123);
        long before = t.getTime();

        OATime y = (OATime) t.withYear(2026);
        OATime m = (OATime) t.withMonth(Calendar.JUNE);
        OATime d = (OATime) t.withDay(15);
        OATime date = (OATime) t.withDate(2026, Calendar.JUNE, 15);

        assertEquals(before, y.getTime());
        assertEquals(before, m.getTime());
        assertEquals(before, d.getTime());
        assertEquals(before, date.getTime());

        assertFixedDate(y);
        assertFixedDate(m);
        assertFixedDate(d);
        assertFixedDate(date);
    }

    @Test
    public void withTimeZoneReturnsOATimeAndClearsDate() {
        OATime t = new OATime(10, 30, 45, 123);

        OATime result = (OATime) t.withTimeZone(NEW_YORK);

        assertTrue(result instanceof OATime);
        assertEquals(NEW_YORK.getID(), result.getTimeZone().getID());
        assertFixedDate(result);
    }

    @Test
    public void invalidStringConstructorThrowsIllegalArgumentException() {
        try {
            new OATime("bad-time");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void serializationPreservesExplicitTimezoneAndFixedDate() throws Exception {
        OATime t = new OATime(1, 30, 0, 123);
        t.setTimeZone(CHICAGO);

        OATime copy = serializeRoundTrip(t);

        assertEquals(1, copy.get24Hour());
        assertEquals(30, copy.getMinute());
        assertEquals(0, copy.getSecond());
        assertEquals(123, copy.getMilliSecond());
        assertEquals(CHICAGO.getID(), copy.getTimeZone().getID());
        assertFixedDate(copy);
    }
}