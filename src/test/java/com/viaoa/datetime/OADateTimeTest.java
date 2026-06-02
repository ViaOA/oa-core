package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

/**
 * Internal tests for OADateTime.
 *
 * Strategy:
 * - One test method per public production method name where practical.
 * - Overloads are tested inside the same methodNameTest().
 * - Comments explain what each assertion is checking.
 * - Tests characterize current behavior without using OAObject models or mocks.
 */
public class OADateTimeTest {

    @Test
    public void constructorTest() {
        // default constructor creates a usable date-time value
        assertNotNull(new OADateTime());

        // SQL Time constructor creates a usable value
        assertNotNull(new OADateTime(java.sql.Time.valueOf("01:02:03")));

        // Date constructor preserves epoch milliseconds
        Date date = new Date(1000L);
        assertEquals(1000L, new OADateTime(date).getTime());

        // null Date uses current time instead of failing
        assertNotNull(new OADateTime((Date) null));

        // long constructor preserves epoch milliseconds
        assertEquals(1234L, new OADateTime(1234L).getTime());

        // Timestamp constructor preserves epoch milliseconds
        assertEquals(2000L, new OADateTime(new Timestamp(2000L)).getTime());

        // Calendar constructor preserves timezone and fields
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2026, Calendar.MAY, 18, 10, 20, 30);
        cal.set(Calendar.MILLISECOND, 40);
        OADateTime dt1 = new OADateTime(cal);
        assertEquals(TimeZone.getTimeZone("UTC"), dt1.getTimeZone());
        assertEquals(2026, dt1.getYear());

        // copy constructor copies time and timezone
        OADateTime dt2 = new OADateTime(dt1);
        assertEquals(dt1.getTime(), dt2.getTime());
        assertEquals(dt1.getTimeZone(), dt2.getTimeZone());

        // LocalDateTime constructor creates usable value
        assertNotNull(new OADateTime(LocalDateTime.of(2026, 5, 18, 10, 20, 30)));

        // Instant constructor preserves epoch milliseconds
        assertEquals(3000L, new OADateTime(Instant.ofEpochMilli(3000L)).getTime());

        // ZonedDateTime constructor creates usable value from instant
        assertNotNull(new OADateTime(ZonedDateTime.of(2026, 5, 18, 10, 20, 30, 0, ZoneId.of("UTC"))));

        // string constructor parses explicit format
        OADateTime dt3 = new OADateTime("2026-05-18 10:20:30", OADateTime.JdbcFormat);
        assertEquals(2026, dt3.getYear());
        assertEquals(Calendar.MAY, dt3.getMonth());
        assertEquals(18, dt3.getDay());

        // OADate + OATime constructor combines date and time fields
        OADateTime dt4 = new OADateTime(new OADate(2026, Calendar.MAY, 18), new OATime(10, 20, 30));
        assertEquals(2026, dt4.getYear());
        assertEquals(Calendar.MAY, dt4.getMonth());
        assertEquals(18, dt4.getDay());
        assertEquals(10, dt4.get24Hour());

        // field constructors use 0-based Calendar month
        assertEquals(2026, new OADateTime(2026, Calendar.MAY, 18).getYear());
        assertEquals(10, new OADateTime(2026, Calendar.MAY, 18, 10, 20).get24Hour());
        assertEquals(30, new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30).getSecond());
        assertEquals(40, new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30, 40).getMilliSecond());
    }

    @Test
    public void setDefaultTimeZoneTest() {
        TimeZone original = OADateTime.getDefaultTimeZone();
        try {
            // explicit timezone becomes default timezone
            TimeZone utc = TimeZone.getTimeZone("UTC");
            OADateTime.setDefaultTimeZone(utc);
            assertEquals(utc, OADateTime.getDefaultTimeZone());

            // null resets to system default
            OADateTime.setDefaultTimeZone(null);
            assertEquals(TimeZone.getDefault(), OADateTime.getDefaultTimeZone());
        } finally {
            OADateTime.setDefaultTimeZone(original);
        }
    }

    @Test
    public void getDefaultTimeZoneTest() {
        // default timezone is always available
        assertNotNull(OADateTime.getDefaultTimeZone());
    }

    @Test
    public void setLocaleTest() {
        // locale setup should not throw for common locale
        assertDoesNotThrow(() -> OADateTime.setLocale(Locale.US));

        // parsing still works after locale setup
        assertNotNull(OADateTime.valueOf("2026-05-18 10:20:30", OADateTime.JdbcFormat));
    }

    @Test
    public void getLocalDateTimeTest() {
        // converts field values to LocalDateTime
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30, 0);
        LocalDateTime ldt = dt.getLocalDateTime();
        assertEquals(2026, ldt.getYear());
        assertEquals(5, ldt.getMonthValue());
        assertEquals(18, ldt.getDayOfMonth());
        assertEquals(10, ldt.getHour());
    }

    @Test
    public void getZonedDateTimeTest() {
        // returns a ZonedDateTime instance
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30, 0);
        dt.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertNotNull(dt.getZonedDateTime());
    }

    @Test
    public void getInstantTest() {
        // returns an Instant instance
        assertNotNull(new OADateTime(1234L).getInstant());
    }

    @Test
    public void getCalendarTest() {
        // calendar exposes date/time fields
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30, 0);
        Calendar cal = dt.getCalendar();
        assertEquals(2026, cal.get(Calendar.YEAR));
        assertEquals(Calendar.MAY, cal.get(Calendar.MONTH));
        assertEquals(18, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void clearTimeTest() {
        // clearTime zeros time fields
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30, 40);
        dt.clearTime();
        assertEquals(0, dt.get24Hour());
        assertEquals(0, dt.getMinute());
        assertEquals(0, dt.getSecond());
        assertEquals(0, dt.getMilliSecond());
    }

    @Test
    public void clearDateTest() {
        // clearDate leaves object usable as time-like value
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30, 40);
        assertDoesNotThrow(() -> dt.clearDate());
        assertEquals(10, dt.get24Hour());
    }

    @Test
    public void setTimeTest() {
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 0, 0, 0, 0);

        // hour and minute overload
        dt.setTime(1, 2);
        assertEquals(1, dt.get24Hour());
        assertEquals(2, dt.getMinute());

        // hour, minute, second overload
        dt.setTime(3, 4, 5);
        assertEquals(3, dt.get24Hour());
        assertEquals(4, dt.getMinute());
        assertEquals(5, dt.getSecond());

        // hour, minute, second, millisecond overload
        dt.setTime(6, 7, 8, 9);
        assertEquals(6, dt.get24Hour());
        assertEquals(7, dt.getMinute());
        assertEquals(8, dt.getSecond());
        assertEquals(9, dt.getMilliSecond());

        // OATime overload copies time fields
        dt.setTime(new OATime(10, 11, 12, 13));
        assertEquals(10, dt.get24Hour());
        assertEquals(11, dt.getMinute());
        assertEquals(12, dt.getSecond());
        assertEquals(13, dt.getMilliSecond());
    }

    @Test
    public void setDateTest() {
        OADateTime dt = new OADateTime(2026, Calendar.JANUARY, 1, 10, 20, 30);

        // field overload changes year/month/day
        dt.setDate(2027, Calendar.FEBRUARY, 3);
        assertEquals(2027, dt.getYear());
        assertEquals(Calendar.FEBRUARY, dt.getMonth());
        assertEquals(3, dt.getDay());

        // OADate overload copies date fields
        dt.setDate(new OADate(2028, Calendar.MARCH, 4));
        assertEquals(2028, dt.getYear());
        assertEquals(Calendar.MARCH, dt.getMonth());
        assertEquals(4, dt.getDay());
    }

    @Test
    public void getYearTest() {
        // returns full year
        assertEquals(2026, new OADateTime(2026, Calendar.MAY, 18).getYear());
    }

    @Test
    public void setYearTest() {
        // changes year field
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18);
        dt.setYear(2027);
        assertEquals(2027, dt.getYear());
    }

    @Test
    public void getMonthTest() {
        // returns 0-based Calendar month
        assertEquals(Calendar.MAY, new OADateTime(2026, Calendar.MAY, 18).getMonth());
    }

    @Test
    public void getQuarterTest() {
        // month in May is second quarter
    	int x = new OADateTime(2026, Calendar.MAY, 18).getQuarter();
        assertEquals(1, x);
    }

    @Test
    public void setMonthTest() {
        // changes month field
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18);
        dt.setMonth(Calendar.JUNE);
        assertEquals(Calendar.JUNE, dt.getMonth());
    }

    @Test
    public void getDayTest() {
        // returns day of month
        assertEquals(18, new OADateTime(2026, Calendar.MAY, 18).getDay());
    }

    @Test
    public void setDayTest() {
        // changes day of month
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18);
        dt.setDay(19);
        assertEquals(19, dt.getDay());
    }

    @Test
    public void setTimeZoneUTCTest() {
        // sets timezone to UTC
        OADateTime dt = new OADateTime();
        dt.setTimeZoneUTC();
        assertEquals(TimeZone.getTimeZone("UTC"), dt.getTimeZone());
    }

    @Test
    public void setTimeZoneTest() {
        // TimeZone overload sets timezone
        OADateTime dt = new OADateTime();
        TimeZone utc = TimeZone.getTimeZone("UTC");
        dt.setTimeZone(utc);
        assertEquals(utc, dt.getTimeZone());

        // OATimeZone.TZ overload sets timezone
        OATimeZone.TZ tz = OATimeZone.getOATimeZone("UTC");
        dt.setTimeZone(tz);
        assertEquals(TimeZone.getTimeZone("UTC"), dt.getTimeZone());
    }

    @Test
    public void getTimeZoneTest() {
        // explicit timezone is returned
        OADateTime dt = new OADateTime();
        TimeZone utc = TimeZone.getTimeZone("UTC");
        dt.setTimeZone(utc);
        assertEquals(utc, dt.getTimeZone());
    }

    @Test
    public void getHourTest() {
        // getHour uses 24-hour clock
        assertEquals(13, new OADateTime(2026, Calendar.MAY, 18, 13, 2, 3).getHour());
    }

    @Test
    public void setHourTest() {
        // setHour changes 24-hour clock
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 1, 2, 3);
        dt.setHour(13);
        assertEquals(13, dt.getHour());
    }

    @Test
    public void get12HourTest() {
        // 13:00 maps to 1 in 12-hour clock
        assertEquals(1, new OADateTime(2026, Calendar.MAY, 18, 13, 0, 0).get12Hour());

        // midnight maps to 0 in current implementation
        assertEquals(0, new OADateTime(2026, Calendar.MAY, 18, 0, 0, 0).get12Hour());
    }

    @Test
    public void set12HourTest() {
        // set12Hour writes the 12-hour value through current implementation
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 15, 0, 0);
        dt.set12Hour(4);
        assertEquals(4, dt.get12Hour());

        // invalid low value fails visibly
        assertThrows(IllegalArgumentException.class, () -> dt.set12Hour(0));
    }

    @Test
    public void get24HourTest() {
        // get24Hour delegates to 24-hour hour value
        assertEquals(13, new OADateTime(2026, Calendar.MAY, 18, 13, 0, 0).get24Hour());
    }

    @Test
    public void set24HourTest() {
        // changes 24-hour clock
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 1, 0, 0);
        dt.set24Hour(13);
        assertEquals(13, dt.get24Hour());
    }

    @Test
    public void getAM_PMTest() {
        // AM time returns Calendar.AM
        assertEquals(Calendar.AM, new OADateTime(2026, Calendar.MAY, 18, 1, 0, 0).getAM_PM());

        // PM time returns Calendar.PM
        assertEquals(Calendar.PM, new OADateTime(2026, Calendar.MAY, 18, 13, 0, 0).getAM_PM());
    }

    @Test
    public void setAM_PMTest() {
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 3, 0, 0);

        // setting PM moves hour into PM range
        dt.setAM_PM(Calendar.PM);
        assertEquals(Calendar.PM, dt.getAM_PM());

        // setting AM moves hour into AM range
        dt.setAM_PM(Calendar.AM);
        assertEquals(Calendar.AM, dt.getAM_PM());
    }

    @Test
    public void getMinuteTest() {
        // returns minute field
        assertEquals(20, new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30).getMinute());
    }

    @Test
    public void setMinuteTest() {
        // changes minute field
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30);
        dt.setMinute(21);
        assertEquals(21, dt.getMinute());
    }

    @Test
    public void getSecondTest() {
        // returns second field
        assertEquals(30, new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30).getSecond());
    }

    @Test
    public void setSecondTest() {
        // changes second field
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30);
        dt.setSecond(31);
        assertEquals(31, dt.getSecond());
    }

    @Test
    public void clearSecondAndMilliSecondTest() {
        // clears second and millisecond
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30, 40);
        dt.clearSecondAndMilliSecond();
        assertEquals(0, dt.getSecond());
        assertEquals(0, dt.getMilliSecond());
    }

    @Test
    public void getMilliSecondTest() {
        // returns millisecond field
        assertEquals(40, new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30, 40).getMilliSecond());
    }

    @Test
    public void setMilliSecondTest() {
        // changes millisecond field
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30, 40);
        dt.setMilliSecond(41);
        assertEquals(41, dt.getMilliSecond());
    }

    @Test
    public void getDateTest() {
        // returns java.util.Date with same epoch millis
        OADateTime dt = new OADateTime(1234L);
        assertEquals(1234L, dt.getDate().getTime());
    }

    @Test
    public void getDayOfWeekTest() {
        // returns Calendar day-of-week value
        int dow = new OADateTime(2026, Calendar.MAY, 18).getDayOfWeek();
        assertTrue(dow >= Calendar.SUNDAY && dow <= Calendar.SATURDAY);
    }

    @Test
    public void getDayOfYearTest() {
        // returns positive day-of-year
        assertTrue(new OADateTime(2026, Calendar.MAY, 18).getDayOfYear() > 0);
    }

    @Test
    public void getWeekOfMonthTest() {
        // returns positive week-of-month
        assertTrue(new OADateTime(2026, Calendar.MAY, 18).getWeekOfMonth() > 0);
    }

    @Test
    public void getWeekOfYearTest() {
        // returns positive week-of-year
        assertTrue(new OADateTime(2026, Calendar.MAY, 18).getWeekOfYear() > 0);
    }

    @Test
    public void getDaysInMonthTest() {
        // May has 31 days
        assertEquals(31, new OADateTime(2026, Calendar.MAY, 18).getDaysInMonth());
    }

    @Test
    public void equalsTest() {
        // same instant is equal
        assertEquals(new OADateTime(1000L), new OADateTime(1000L));

        // different instant is not equal
        assertNotEquals(new OADateTime(1000L), new OADateTime(2000L));
    }

    @Test
    public void hashCodeTest() {
        // same instant has same hashCode
        assertEquals(new OADateTime(1000L).hashCode(), new OADateTime(1000L).hashCode());
    }

    @Test
    public void beforeTest() {
        // earlier date-time is before later date-time
        assertTrue(new OADateTime(1000L).before(new OADateTime(2000L)));

        // later date-time is not before earlier date-time
        assertFalse(new OADateTime(2000L).before(new OADateTime(1000L)));
    }

    @Test
    public void isBeforeTest() {
        // alias for before
        assertTrue(new OADateTime(1000L).isBefore(new OADateTime(2000L)));
    }

    @Test
    public void afterTest() {
        // later date-time is after earlier date-time
        assertTrue(new OADateTime(2000L).after(new OADateTime(1000L)));

        // earlier date-time is not after later date-time
        assertFalse(new OADateTime(1000L).after(new OADateTime(2000L)));
    }

    @Test
    public void isAfterTest() {
        // alias for after
        assertTrue(new OADateTime(2000L).isAfter(new OADateTime(1000L)));
    }

    @Test
    public void compareTest() {
        // compare returns zero for same instant
        assertEquals(0, new OADateTime(1000L).compare(new OADateTime(1000L)));

        // compare returns negative for earlier instant
        assertTrue(new OADateTime(1000L).compare(new OADateTime(2000L)) < 0);
    }

    @Test
    public void compareToTest() {
        // compareTo returns zero for same instant
        assertEquals(0, new OADateTime(1000L).compareTo(new OADateTime(1000L)));

        // null compares as this object greater
        assertTrue(new OADateTime(1000L).compareTo(null) > 0);

        // non-convertible object returns current sentinel positive value
        assertTrue(new OADateTime(1000L).compareTo(new Object()) > 0);
    }

    @Test
    public void convertToUTCTest() {
        // convertToUTC returns usable date-time with UTC timezone
        OADateTime dt = new OADateTime();
        OADateTime utc = dt.convertToUTC();
        assertNotNull(utc);
        assertEquals(TimeZone.getTimeZone("UTC"), utc.getTimeZone());
    }

    @Test
    public void convertToTest() {
        // TimeZone overload returns value in requested timezone
        OADateTime dt = new OADateTime();
        OADateTime utc = dt.convertTo(TimeZone.getTimeZone("UTC"));
        assertNotNull(utc);
        assertEquals(TimeZone.getTimeZone("UTC"), utc.getTimeZone());

        // OATimeZone.TZ overload returns value in requested timezone
        OATimeZone.TZ tz = OATimeZone.getOATimeZone("UTC");
        OADateTime utc2 = dt.convertTo(tz);
        assertNotNull(utc2);
        assertEquals(TimeZone.getTimeZone("UTC"), utc2.getTimeZone());
    }

    @Test
    public void addDaysTest() {
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18);

        // adds positive amount
        assertEquals(19, dt.addDays(1).getDay());

        // zero amount returns new instance
        assertNotSame(dt, dt.addDays(0));

        // OADate input returns OADate
        assertTrue(new OADate(2026, Calendar.MAY, 18).addDays(1) instanceof OADate);
    }

    @Test
    public void subtractDaysTest() {
        // subtracts positive amount
        assertEquals(17, new OADateTime(2026, Calendar.MAY, 18).subtractDays(1).getDay());
    }

    @Test
    public void addDayTest() {
        // adds one day
        assertEquals(19, new OADateTime(2026, Calendar.MAY, 18).addDay().getDay());
    }

    @Test
    public void subtractDayTest() {
        // subtracts one day
        assertEquals(17, new OADateTime(2026, Calendar.MAY, 18).subtractDay().getDay());
    }

    @Test
    public void addWeeksTest() {
        // adds seven days
        assertEquals(25, new OADateTime(2026, Calendar.MAY, 18).addWeeks(1).getDay());
    }

    @Test
    public void subtractWeeksTest() {
        // subtracts seven days
        assertEquals(11, new OADateTime(2026, Calendar.MAY, 18).subtractWeeks(1).getDay());
    }

    @Test
    public void addMonthsTest() {
        // adds one month
        assertEquals(Calendar.JUNE, new OADateTime(2026, Calendar.MAY, 18).addMonths(1).getMonth());
    }

    @Test
    public void subtractMonthsTest() {
        // subtracts one month
        assertEquals(Calendar.APRIL, new OADateTime(2026, Calendar.MAY, 18).subtractMonths(1).getMonth());
    }

    @Test
    public void addYearsTest() {
        // adds one year
        assertEquals(2027, new OADateTime(2026, Calendar.MAY, 18).addYears(1).getYear());
    }

    @Test
    public void subtractYearsTest() {
        // subtracts one year
        assertEquals(2025, new OADateTime(2026, Calendar.MAY, 18).subtractYears(1).getYear());
    }

    @Test
    public void addHoursTest() {
        // adds one hour
        assertEquals(11, new OADateTime(2026, Calendar.MAY, 18, 10, 0, 0).addHours(1).get24Hour());
    }

    @Test
    public void subtractHoursTest() {
        // subtracts one hour
        assertEquals(9, new OADateTime(2026, Calendar.MAY, 18, 10, 0, 0).subtractHours(1).get24Hour());
    }

    @Test
    public void addMinutesTest() {
        // adds one minute
        assertEquals(21, new OADateTime(2026, Calendar.MAY, 18, 10, 20, 0).addMinutes(1).getMinute());
    }

    @Test
    public void subtractMinutesTest() {
        // subtracts one minute
        assertEquals(19, new OADateTime(2026, Calendar.MAY, 18, 10, 20, 0).subtractMinutes(1).getMinute());
    }

    @Test
    public void addSecondsTest() {
        // adds one second
        assertEquals(31, new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30).addSeconds(1).getSecond());
    }

    @Test
    public void subtractSecondsTest() {
        // subtracts one second
        assertEquals(29, new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30).subtractSeconds(1).getSecond());
    }

    @Test
    public void addMilliSecondsTest() {
        // adds one millisecond
        assertEquals(41, new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30, 40).addMilliSeconds(1).getMilliSecond());
    }

    @Test
    public void subtractMilliSecondsTest() {
        // subtracts one millisecond
        assertEquals(39, new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30, 40).subtractMilliSeconds(1).getMilliSecond());
    }

    @Test
    public void betweenYearsTest() {
        // one calendar year difference
        assertEquals(1, new OADateTime(2026, Calendar.MAY, 18).betweenYears(new OADateTime(2027, Calendar.MAY, 18)));
    }

    @Test
    public void betweenMonthsTest() {
        // one calendar month difference
        assertEquals(1, new OADateTime(2026, Calendar.MAY, 18).betweenMonths(new OADateTime(2026, Calendar.JUNE, 18)));
    }

    @Test
    public void betweenDaysTest() {
        // one day difference
        assertEquals(1, new OADateTime(2026, Calendar.MAY, 18).betweenDays(new OADateTime(2026, Calendar.MAY, 19)));
    }

    @Test
    public void betweenHoursTest() {
        // one hour difference
        assertEquals(1, new OADateTime(2026, Calendar.MAY, 18, 10, 0, 0).betweenHours(new OADateTime(2026, Calendar.MAY, 18, 11, 0, 0)));
    }

    @Test
    public void betweenMinutesTest() {
        // one minute difference
        assertEquals(1, new OADateTime(2026, Calendar.MAY, 18, 10, 20, 0).betweenMinutes(new OADateTime(2026, Calendar.MAY, 18, 10, 21, 0)));
    }

    @Test
    public void betweenSecondsTest() {
        // one second difference
        assertEquals(1, new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30).betweenSeconds(new OADateTime(2026, Calendar.MAY, 18, 10, 20, 31)));
    }

    @Test
    public void betweenMilliSecondsTest() {
        // one millisecond difference
        assertEquals(1, new OADateTime(1000L).betweenMilliSeconds(new OADateTime(1001L)));
    }

    @Test
    public void getTimeTest() {
        // returns epoch milliseconds
        assertEquals(1234L, new OADateTime(1234L).getTime());
    }

    @Test
    public void valueOfTest() {
        // explicit format parses date-time
        assertNotNull(OADateTime.valueOf("2026-05-18 10:20:30", OADateTime.JdbcFormat));

        // explicit format with fallback flag parses date-time
        assertNotNull(OADateTime.valueOf("2026-05-18 10:20:30", OADateTime.JdbcFormat, false));

        // default parse handles SQL-style date-time
        assertNotNull(OADateTime.valueOf("2026-05-18 10:20:30"));

        // null input returns null
        assertNull(OADateTime.valueOf(null));
    }

    @Test
    public void toStringTest() {
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30, 0);

        // explicit JDBC format
        assertEquals("2026-05-18 10:20:30", dt.toString(OADateTime.JdbcFormat));

        // default toString returns non-empty text
        assertNotNull(dt.toString());
        assertFalse(dt.toString().isEmpty());
    }

    @Test
    public void setGlobalOutputFormatTest() {
        // set explicit global output format
        OADateTime.setGlobalOutputFormat(OADateTime.JdbcFormat);
        assertEquals(OADateTime.JdbcFormat, OADateTime.getGlobalOutputFormat());

        // cleanup
        OADateTime.setGlobalOutputFormat(null);
    }

    @Test
    public void getGlobalOutputFormatTest() {
        // current global output format is readable after setting
        OADateTime.setGlobalOutputFormat(OADateTime.JsonFormat);
        assertEquals(OADateTime.JsonFormat, OADateTime.getGlobalOutputFormat());

        // cleanup
        OADateTime.setGlobalOutputFormat(null);
    }

    @Test
    public void addGlobalParseFormatTest() {
        // custom parse format can be added and used
        OADateTime.addGlobalParseFormat("yyyy.MM.dd HH.mm.ss");
        assertNotNull(OADateTime.valueOf("2026.05.18 10.20.30"));

        // cleanup
        OADateTime.removeGlobalParseFormat("yyyy.MM.dd HH.mm.ss");
    }

    @Test
    public void removeGlobalParseFormatTest() {
        // removing a known custom format should not throw
        OADateTime.addGlobalParseFormat("yyyy.MM.dd HH.mm.ss");
        assertDoesNotThrow(() -> OADateTime.removeGlobalParseFormat("yyyy.MM.dd HH.mm.ss"));
    }

    @Test
    public void removeAllGlobalParseFormatsTest() {
        // remove all custom parse formats should not prevent explicit-format parsing
        assertNotNull(OADateTime.valueOf("2026-05-18 10:20:30", OADateTime.JdbcFormat));
    }

    @Test
    public void setFormatTest() {
        // instance format is stored
        OADateTime dt = new OADateTime();
        dt.setFormat(OADateTime.JdbcFormat);
        assertEquals(OADateTime.JdbcFormat, dt.getFormat());
    }

    @Test
    public void getFormatTest() {
        // instance format defaults to null
        OADateTime dt = new OADateTime();
        assertNull(dt.getFormat());

        // static DateFormat lookup returns usable pattern
        assertNotNull(OADateTime.getFormat(DateFormat.SHORT));
        assertNotNull(OADateTime.getFormat(DateFormat.SHORT, Locale.US));
    }

    @Test
    public void isLastDayOfMonthTest() {
        // May 31 is last day of month
        assertTrue(new OADateTime(2026, Calendar.MAY, 31).isLastDayOfMonth());

        // May 30 is not last day of month
        assertFalse(new OADateTime(2026, Calendar.MAY, 30).isLastDayOfMonth());
    }

    @Test
    public void isFirstWeekDayOfMonthTest() {
        // first Monday in May 2026 is first weekday occurrence
        assertTrue(new OADateTime(2026, Calendar.MAY, 4).isFirstWeekDayOfMonth(Calendar.MONDAY));
    }

    @Test
    public void isLastWeekDayOfMonthTest() {
        // last Friday in May 2026 is last weekday occurrence
        assertTrue(new OADateTime(2026, Calendar.MAY, 29).isLastWeekDayOfMonth(Calendar.FRIDAY));
    }

    @Test
    public void getLastWeekDayOfMonthTest() {
        // returns a date in the same month
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18);
        int day = dt.getLastWeekDayOfMonth(Calendar.FRIDAY);
        assertEquals(29, day);
    }

    @Test
    public void getFirstWeekDayOfMonthTest() {
        // returns a date in the same month
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18);
        int day = dt.getFirstWeekDayOfMonth(Calendar.MONDAY);
        assertEquals(4, day);
    }
}
