package com.viaoa.datetime;


import java.io.*;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

public class OADateInvariantTest {
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

    private static void assertFixedTime(OADate d) {
        assertEquals(0, d.get24Hour());
        assertEquals(0, d.getMinute());
        assertEquals(0, d.getSecond());
        assertEquals(0, d.getMilliSecond());
    }

    @Test
    public void constructorsAlwaysClearTime() {
        assertFixedTime(new OADate(2026, Calendar.JUNE, 15));

        OADateTime dt = new OADateTime(2026, Calendar.JUNE, 15, 13, 45, 30, 250);
        assertFixedTime(new OADate(dt));

        assertFixedTime(new OADate(LocalDate.of(2026, 6, 15)));
    }

    @Test
    public void localDateConstructorUsesJavaTimeMonthValueCorrectly() {
        OADate d = new OADate(LocalDate.of(2026, 5, 18));

        assertEquals(2026, d.getYear());
        assertEquals(Calendar.MAY, d.getMonth());
        assertEquals(5, d.getMonthValue());
        assertEquals(18, d.getDay());
        assertFixedTime(d);
    }

    @Test
    public void setTimeZonePreservesDateAndClearsTime() {
        OADate d = new OADate(2026, Calendar.MARCH, 8);
        d.setTimeZone(CHICAGO);

        assertEquals(2026, d.getYear());
        assertEquals(Calendar.MARCH, d.getMonth());
        assertEquals(8, d.getDay());
        assertEquals(CHICAGO.getID(), d.getTimeZone().getID());
        assertFixedTime(d);
    }

    @Test
    public void convertToReturnsOADateAndClearsTime() {
        OADate d = new OADate(2026, Calendar.MARCH, 8);
        d.setTimeZone(UTC);

        OADate result = (OADate) d.convertTo(LOS_ANGELES);

        assertTrue(result instanceof OADate);
        assertFixedTime(result);
        assertEquals(LOS_ANGELES.getID(), result.getTimeZone().getID());
    }

    @Test
    public void timeSettersAreNoOp() {
        OADate d = new OADate(2026, Calendar.JANUARY, 10);
        long before = d.getTime();

        d.setHour(12);
        d.setMinute(34);
        d.setSecond(56);
        d.setMilliSecond(789);
        d.setTime(13, 14, 15, 16);

        assertEquals(before, d.getTime());
        assertFixedTime(d);
    }

    @Test
    public void addSubtractTimeUnitsReturnOADateAndClearTime() {
        OADate d = new OADate(2026, Calendar.JANUARY, 10);

        OADate d1 = (OADate) d.addHours(5);
        OADate d2 = (OADate) d.addMinutes(5);
        OADate d3 = (OADate) d.addSeconds(5);
        OADate d4 = (OADate) d.addMilliSeconds(5);

        assertFixedTime(d1);
        assertFixedTime(d2);
        assertFixedTime(d3);
        assertFixedTime(d4);
    }

    @Test
    public void supportedWithDateFieldsReturnOADateAndClearTime() {
        OADate d = new OADate(2026, Calendar.JANUARY, 10);

        OADate y = (OADate) d.withYear(2027);
        OADate m = (OADate) d.withMonth(Calendar.FEBRUARY);
        OADate day = (OADate) d.withDay(11);
        OADate date = (OADate) d.withDate(2028, Calendar.MARCH, 12);

        assertEquals(2027, y.getYear());
        assertEquals(Calendar.FEBRUARY, m.getMonth());
        assertEquals(11, day.getDay());
        assertEquals(2028, date.getYear());
        assertEquals(Calendar.MARCH, date.getMonth());
        assertEquals(12, date.getDay());

        assertFixedTime(y);
        assertFixedTime(m);
        assertFixedTime(day);
        assertFixedTime(date);
    }

    @Test
    public void unsupportedWithTimeFieldsReturnOADateUnchangedAndClearTime() {
        OADate d = new OADate(2026, Calendar.JANUARY, 10);
        long before = d.getTime();

        OADate h = (OADate) d.withHour(12);
        OADate min = (OADate) d.withMinute(30);
        OADate sec = (OADate) d.withSecond(45);
        OADate ms = (OADate) d.withMilliSecond(123);
        OADate time = (OADate) d.withTime(12, 30, 45, 123);

        assertEquals(before, h.getTime());
        assertEquals(before, min.getTime());
        assertEquals(before, sec.getTime());
        assertEquals(before, ms.getTime());
        assertEquals(before, time.getTime());

        assertFixedTime(h);
        assertFixedTime(min);
        assertFixedTime(sec);
        assertFixedTime(ms);
        assertFixedTime(time);
    }

    @Test
    public void withTimeZoneReturnsOADateAndClearsTime() {
        OADate d = new OADate(2026, Calendar.JANUARY, 10);

        OADate result = (OADate) d.withTimeZone(NEW_YORK);

        assertTrue(result instanceof OADate);
        assertEquals(NEW_YORK.getID(), result.getTimeZone().getID());
        assertFixedTime(result);
    }

    @Test
    public void serializationPreservesExplicitTimezoneAndFixedTime() throws Exception {
        OADate d = new OADate(2026, Calendar.NOVEMBER, 1);
        d.setTimeZone(CHICAGO);

        OADate copy = serializeRoundTrip(d);

        assertEquals(2026, copy.getYear());
        assertEquals(Calendar.NOVEMBER, copy.getMonth());
        assertEquals(1, copy.getDay());
        assertEquals(CHICAGO.getID(), copy.getTimeZone().getID());
        assertFixedTime(copy);
    }
}