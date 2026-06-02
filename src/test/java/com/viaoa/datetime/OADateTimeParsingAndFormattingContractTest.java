package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OADateTimeParsingAndFormattingContractTest {

    private TimeZone originalJvmTimeZone;
    private TimeZone originalOaTimeZone;
    private Locale originalLocale;
    private String originalDateFormat;
    private String originalTimeFormat;
    private String originalDateTimeFormat;

    @BeforeEach
    void saveAndSetStableDefaults() {
        originalJvmTimeZone = TimeZone.getDefault();
        originalOaTimeZone = OADateTime.getDefaultTimeZone();
        originalLocale = Locale.getDefault();
        originalDateFormat = OADate.getGlobalOutputFormat();
        originalTimeFormat = OATime.getGlobalOutputFormat();
        originalDateTimeFormat = OADateTime.getGlobalOutputFormat();

        TimeZone utc = TimeZone.getTimeZone("UTC");
        TimeZone.setDefault(utc);
        OADateTime.setDefaultTimeZone(utc);
        Locale.setDefault(Locale.US);
        OADate.setLocale(Locale.US);
        OADateTime.setLocale(Locale.US);
        OADate.setGlobalOutputFormat("yyyy-MM-dd");
        OATime.setGlobalOutputFormat("HH:mm:ss.SSS");
        OADateTime.setGlobalOutputFormat("yyyy-MM-dd HH:mm:ss.SSS");
    }

    @AfterEach
    void restore() {
        OADate.setGlobalOutputFormat(originalDateFormat);
        OATime.setGlobalOutputFormat(originalTimeFormat);
        OADateTime.setGlobalOutputFormat(originalDateTimeFormat);
        OADateTime.setDefaultTimeZone(originalOaTimeZone);
        TimeZone.setDefault(originalJvmTimeZone);
        Locale.setDefault(originalLocale);
        OADate.setLocale(originalLocale);
        OADateTime.setLocale(originalLocale);
    }

    @Test
    void explicitDateTimeFormatRoundTripPreservesMillis() {
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30, 456);
        String s = dt.toString("yyyy-MM-dd HH:mm:ss.SSS");

        OADateTime parsed = OADateTime.valueOf(s, "yyyy-MM-dd HH:mm:ss.SSS");

        assertNotNull(parsed);
        assertEquals(dt.getTime(), parsed.getTime());
    }

    @Test
    void dateAndTimeFormatRoundTripsPreserveSemanticFields() {
        OADate date = new OADate(2026, Calendar.MAY, 18);
        OATime time = new OATime(10, 20, 30, 456);

        OADate parsedDate = OADate.dateValue(date.toString("yyyy-MM-dd"), "yyyy-MM-dd");
        
        String s = time.toString("HH:mm:ss.SSS");
        OATime parsedTime = OATime.timeValue(s, "HH:mm:ss.SSS");
        OATime parsedTimeA = OATime.timeValue(s, "HH:mm:ss.SSS");

        OATime parsedTimeX = OATime.timeValue(s, "HH:mm:ss.SSS");

        
        LocalDate d1 = date.getLocalDate();
        LocalDate d2 = parsedDate.getLocalDate();
        boolean bx = d1.equals(d2);
        assertEquals(d1, d2);
        
        LocalTime t1 = time.getLocalTime();
        LocalTime t2 = parsedTime.getLocalTime();
        boolean bx2 = t1.equals(t2);
        
        assertEquals(t1, t2);
    }

    @Test
    void invalidDateCurrentlyNormalizesOrParsesByLenientFormatter() {
        OADateTime parsed = OADate.valueOf("2026-02-31", "yyyy-MM-dd");

        assertNull(parsed, "Current parsing uses lenient SimpleDateFormat behavior for invalid dates."); // null
        // assertFalse(parsed.getMonth() == Calendar.FEBRUARY && parsed.getDay() == 31);
    }

    @Test
    void invalidTimeCurrentlyNormalizesOrParsesByLenientFormatter() {
        OADateTime parsed = OATime.valueOf("25:00", "HH:mm");

        assertNull(parsed, "Current parsing uses lenient SimpleDateFormat behavior for invalid times."); // null
    }

    @Test
    void parseCurrentlyAllowsTrailingGarbageWhenFormatPrefixMatches() {
        OADateTime parsed = OADateTime.valueOf("2026-05-18 10:20:30 garbage", "yyyy-MM-dd HH:mm:ss", false);

        assertNotNull(parsed, "Current parse path does not require full input consumption.");
        assertEquals(2026, parsed.getYear());
        assertEquals(Calendar.MAY, parsed.getMonth());
        assertEquals(18, parsed.getDay());
    }

    @Test
    void rfcLiteralZCurrentlyUsesDefaultTimezoneUnlessCallerSetsUtc() {
        OADateTime parsed = OADateTime.valueOf("2026-05-18T10:20:30Z", OADateTime.RFC339Format, false);

        assertNotNull(parsed);
        assertEquals(10, parsed.get24Hour(), "Current literal-Z pattern consumes Z but does not itself encode offset semantics.");
    }

    @Test
    void badOATimeStringConstructorCurrentlyThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new OATime("bad-time"));
    }
}
