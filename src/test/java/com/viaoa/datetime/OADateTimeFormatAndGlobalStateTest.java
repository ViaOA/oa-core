package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OADateTimeFormatAndGlobalStateTest {

    private TimeZone originalJvmTz;
    private TimeZone originalOaTz;
    private Locale originalLocale;
    private String originalDateFormat;
    private String originalTimeFormat;
    private String originalDateTimeFormat;

    @BeforeEach
    void saveGlobals() {
        originalJvmTz = TimeZone.getDefault();
        originalOaTz = OADateTime.getDefaultTimeZone();
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
    }

    @AfterEach
    void restoreGlobals() {
        OADate.setGlobalOutputFormat(originalDateFormat);
        OATime.setGlobalOutputFormat(originalTimeFormat);
        OADateTime.setGlobalOutputFormat(originalDateTimeFormat);
        OADateTime.setDefaultTimeZone(originalOaTz);
        TimeZone.setDefault(originalJvmTz);
        Locale.setDefault(originalLocale);
        OADate.setLocale(originalLocale);
        OADateTime.setLocale(originalLocale);
    }

    @Test
    void explicitFormatOverridesGlobalOutputFormat() {
        OADate date = new OADate(2026, 4, 27);
        OADate.setGlobalOutputFormat("MM/dd/yyyy");

        assertEquals("2026-05-27", date.toString("yyyy-MM-dd"));
    }

    @Test
    void globalDateOutputFormatIsUsedWhenNoInstanceFormat() {
        OADate date = new OADate(2026, 4, 27);
        OADate.setGlobalOutputFormat("yyyyMMdd");

        assertEquals("20260527", date.toString());
    }

    @Test
    void globalTimeOutputFormatIsUsedWhenNoInstanceFormat() {
        OATime time = new OATime(7, 8, 9, 123);
        OATime.setGlobalOutputFormat("HH:mm:ss.SSS");

        String s = time.toString();
        assertEquals("07:08:09.123", s); // 07:08:09.000
    }

    @Test
    void globalDateTimeOutputFormatIsUsedWhenNoInstanceFormat() {
        OADateTime dt = new OADateTime(2026, 4, 27, 7, 8, 9, 123);
        OADateTime.setGlobalOutputFormat("yyyy-MM-dd HH:mm:ss.SSS");

        assertEquals("2026-05-27 07:08:09.123", dt.toString());
    }

    @Test
    void instanceFormatOverridesGlobalFormat() {
        OADateTime dt = new OADateTime(2026, 4, 27, 7, 8, 9, 123);
        OADateTime.setGlobalOutputFormat("yyyyMMdd");
        dt.setFormat("yyyy-MM-dd HH:mm");

        assertEquals("2026-05-27 07:08", dt.toString());
    }

    @Test
    void localeResetKeepsParsingDeterministicForSqlStyleFormats() {
        OADate date = (OADate) OADate.valueOf("2026-05-27");
        OADateTime dt = OADateTime.valueOf("2026-05-27 07:08:09");

        assertNotNull(date);
        assertEquals(2026, date.getYear());
        assertEquals(4, date.getMonth());
        assertEquals(27, date.getDay());

        assertNotNull(dt);
        assertEquals(2026, dt.getYear());
        assertEquals(4, dt.getMonth());
        assertEquals(27, dt.getDay());
        assertEquals(7, dt.get24Hour());
    }
}
