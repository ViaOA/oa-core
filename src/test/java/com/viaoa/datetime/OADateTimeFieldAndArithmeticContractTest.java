package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Calendar;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OADateTimeFieldAndArithmeticContractTest {

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
    void addDaysZeroCurrentlyReturnsSameInstance() {
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 10, 30, 15, 123);

        OADateTime result = dt.addDays(0);

        assertSame(dt, result, "Current behavior aliases original for addDays(0); this is a documented contract question.");
    }

    @Test
    void addMonthsAndYearsPreserveSemanticType() {
        OADate date = new OADate(2026, Calendar.JANUARY, 31);
        OATime time = new OATime(10, 20, 30, 456);
        OADateTime dateTime = new OADateTime(2026, Calendar.JANUARY, 31, 10, 20, 30, 456);

        assertInstanceOf(OADate.class, date.addMonths(1));
        assertInstanceOf(OATime.class, time.addHours(1));
        assertInstanceOf(OADateTime.class, dateTime.addYears(1));
    }

    @Test
    void addMillisecondsPreservesPrecision() {
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 10, 30, 15, 123);

        OADateTime result = dt.addMilliSeconds(7);

        assertEquals(dt.getTime() + 7, result.getTime());
        assertEquals(130, result.getMilliSecond());
    }

    @Test
    void set12HourCurrentlyLosesPmState() {
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 15, 30, 0, 0);

        dt.set12Hour(4);

        assertEquals(4, dt.get24Hour(), "Current behavior writes 4 AM, not 4 PM.");
    }

    @Test
    void invalidDateFieldsCurrentlyNormalizeInsteadOfFailing() {
        OADate date = new OADate(2026, Calendar.FEBRUARY, 31);

        assertFalse(date.getMonth() == Calendar.FEBRUARY && date.getDay() == 31,
            "Current field constructor normalizes invalid February 31 rather than preserving/failing.");
    }

    @Test
    void invalidTimeFieldsCurrentlyNormalizeInsteadOfFailing() {
        OATime time = new OATime(25, 0, 0);

        assertEquals(1, time.get24Hour(), "Current time constructor normalizes hour 25 to 01:00.");
    }

    @Test
    void setMonthOnInvalidDayCurrentlyNormalizes() {
        OADateTime dt = new OADateTime(2026, Calendar.MARCH, 31, 10, 0, 0, 0);

        dt.setMonth(Calendar.FEBRUARY);

        assertFalse(dt.getMonth() == Calendar.FEBRUARY && dt.getDay() == 31,
            "Current month setter normalizes an invalid February 31 combination.");
    }
}
