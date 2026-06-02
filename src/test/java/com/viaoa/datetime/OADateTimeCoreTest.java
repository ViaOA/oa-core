package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OADateTimeCoreTest {
    private static final long EPOCH_MILLIS = 1714979289123L;
    private TimeZone originalJvmTimeZone;
    private TimeZone originalOaTimeZone;
    private Locale originalLocale;
    private String originalOutputFormat;

    @BeforeEach
    void setUp() {
        originalJvmTimeZone = TimeZone.getDefault();
        originalOaTimeZone = OADateTime.getDefaultTimeZone();
        originalLocale = Locale.getDefault();
        originalOutputFormat = OADateTime.getGlobalOutputFormat();
        TimeZone utc = TimeZone.getTimeZone("UTC");
        TimeZone.setDefault(utc);
        OADateTime.setDefaultTimeZone(utc);
        Locale.setDefault(Locale.US);
        OADateTime.setLocale(Locale.US);
        OADateTime.setGlobalOutputFormat("yyyy-MM-dd HH:mm:ss.SSS");
    }

    @AfterEach
    void tearDown() {
        OADateTime.setGlobalOutputFormat(originalOutputFormat);
        Locale.setDefault(originalLocale);
        OADateTime.setLocale(originalLocale);
        OADateTime.setDefaultTimeZone(originalOaTimeZone);
        TimeZone.setDefault(originalJvmTimeZone);
    }

    @Test
    void epochConstructorPreservesMillisThroughGetTime() {
        OADateTime dt = new OADateTime(EPOCH_MILLIS);

        assertEquals(EPOCH_MILLIS, dt.getTime());
        assertEquals("2024-05-06 07:08:09.123", dt.toString("yyyy-MM-dd HH:mm:ss.SSS"));
    }

    @Test
    void instantConstructorPreservesEpochMillisThroughGetTime() {
        Instant instant = Instant.ofEpochMilli(EPOCH_MILLIS);
        OADateTime dt = new OADateTime(instant);

        assertEquals(EPOCH_MILLIS, dt.getTime());
    }

    @Test
    void getInstantCurrentlyDropsMillisecondPrecision() {
        OADateTime dt = new OADateTime(EPOCH_MILLIS);

        assertEquals(Instant.ofEpochSecond(1714979289L), dt.getInstant());
        assertNotEquals(Instant.ofEpochMilli(EPOCH_MILLIS), dt.getInstant());
    }

    @Test
    void localDateTimeRoundTripDocumentsCurrentMillisecondLoss() {
        OADateTime dt = new OADateTime(EPOCH_MILLIS);
        LocalDateTime ldt = dt.getLocalDateTime();

        assertEquals(LocalDateTime.of(2024, 5, 6, 7, 8, 9, 0), ldt);
    }

    @Test
    void zonedDateTimeConstructorPreservesInstantButCurrentlyLosesZoneIdentity() {
        ZonedDateTime zdt = ZonedDateTime.of(2024, 5, 6, 3, 8, 9, 123_000_000, ZoneId.of("America/New_York"));
        OADateTime dt = new OADateTime(zdt);

        assertEquals(zdt.toInstant().toEpochMilli(), dt.getTime());
        assertEquals(TimeZone.getTimeZone("UTC"), dt.getTimeZone());
    }

    @Test
    void instanceTimezoneControlsFieldAccessAndFormatting() {
        OADateTime dt = new OADateTime(EPOCH_MILLIS);
        TimeZone tz = TimeZone.getTimeZone("America/Chicago");
        dt.setTimeZone(tz);

        String s = dt.toString("yyyy-MM-dd HH:mm z");
        assertEquals("2024-05-06 07:08 CDT", s); // 2024-05-06 07:08 CDT
        int hr = dt.get24Hour();
        assertEquals(7, hr);
    }

    @Test
    void convertToChangesZoneButPreservesInstant() {
        OADateTime utc = new OADateTime(EPOCH_MILLIS);
        utc.setTimeZoneUTC();

        OADateTime chicago = utc.convertTo(TimeZone.getTimeZone("America/Chicago"));

        assertNotSame(utc, chicago);
        assertEquals(utc.getTime(), chicago.getTime());
        assertEquals("America/Chicago", chicago.getTimeZone().getID());
        assertEquals("2024-05-06 02:08 CDT", chicago.toString("yyyy-MM-dd HH:mm z"));
    }

    @Test
    void set12HourCurrentlyLosesPmState() {
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 15, 30, 0, 0);

        dt.set12Hour(4);

        assertEquals(Calendar.AM, dt.getAM_PM());
        assertEquals(4, dt.get24Hour());
    }

    @Test
    void setAmPmPreservesTwelveHourComponent() {
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 3, 30, 0, 0);

        dt.setAM_PM(Calendar.PM);

        assertEquals(15, dt.get24Hour());
        assertEquals(Calendar.PM, dt.getAM_PM());
    }

    @Test
    void invalidFieldConstructionCurrentlyRollsForwardLeniently() {
        OADateTime dt = new OADateTime(2026, Calendar.FEBRUARY, 31, 10, 0, 0, 0);

        assertEquals(2026, dt.getYear());
        assertEquals(Calendar.MARCH, dt.getMonth());
        assertEquals(3, dt.getDay());
    }

    @Test
    void compareAndAfterCurrentlyTreatNonConvertibleObjectAsAfter() {
        OADateTime dt = new OADateTime(EPOCH_MILLIS);
        Object value = new Object();

        assertEquals(2, dt.compareTo(value));
        assertTrue(dt.after(value));
    }

    @Test
    void betweenYearsAndMonthsDocumentBoundaryDifferenceBehavior() {
        OADate dec31 = new OADate(2025, Calendar.DECEMBER, 31);
        OADate jan1 = new OADate(2026, Calendar.JANUARY, 1);
        OADate jan31 = new OADate(2026, Calendar.JANUARY, 31);
        OADate feb1 = new OADate(2026, Calendar.FEBRUARY, 1);

        assertEquals(1, dec31.betweenYears(jan1));
        assertEquals(1, jan31.betweenMonths(feb1));
    }

    @Test
    void addMonthAndYearBoundaryBehaviorIsDeterministic() {
        OADate jan31 = new OADate(2026, Calendar.JANUARY, 31);
        OADate plusOneMonth = (OADate) jan31.addMonths(1);

        assertEquals(2026, plusOneMonth.getYear());
        assertEquals(Calendar.FEBRUARY, plusOneMonth.getMonth());
        assertTrue(plusOneMonth.getDay() >= 28 && plusOneMonth.getDay() <= 31);
    }

    @Test
    void valueOfReturnsNullForNullBlankAndInvalidInput() {
        assertNull(OADateTime.valueOf(null));
        assertNull(OADateTime.valueOf(""));
        assertNull(OADateTime.valueOf("not-a-date-time"));
    }

    @Test
    void valueOfWithExplicitFormatParsesExpectedFields() {
        OADateTime dt = OADateTime.valueOf("2024-05-06 07:08:09.123", "yyyy-MM-dd HH:mm:ss.SSS", false);

        assertNotNull(dt);
        assertEquals(2024, dt.getYear());
        assertEquals(Calendar.MAY, dt.getMonth());
        assertEquals(6, dt.getDay());
        assertEquals(7, dt.get24Hour());
        assertEquals(8, dt.getMinute());
        assertEquals(9, dt.getSecond());
        assertEquals(123, dt.getMilliSecond());
    }
}
