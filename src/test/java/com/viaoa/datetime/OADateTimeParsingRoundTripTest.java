package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.viaoa.datetime.OADateTime.DateTimeType;

class OADateTimeParsingRoundTripTest {
    private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    private ZoneId originalDefaultZoneId;
    private Locale originalLocale;

    @BeforeEach
    void beforeEach() {
        originalDefaultZoneId = OADateTime.getDefaultZoneId();
        originalLocale = Locale.getDefault();
        OADateTime.setDefaultZoneId(CHICAGO);
        OADateTime.setLocale(Locale.US);
        OADate.setLocale(Locale.US);
        OADateTime.setGlobalOutputFormat(null);
        OADate.setGlobalOutputFormat(null);
        OATime.setGlobalOutputFormat(null);
    }

    @AfterEach
    void afterEach() {
        OADateTime.removeGlobalParseFormat("yyyy-MM-dd HH:mm VV");
        OADateTime.removeGlobalParseFormat("yyyy-MM-dd HH:mm XXX");
        OADateTime.removeGlobalParseFormat("yyyyMMdd HHmmss");
        OADate.removeGlobalParseFormat("yyyyMMdd");
        OATime.removeGlobalParseFormat("HHmmss");
        OADateTime.setLocale(originalLocale);
        OADate.setLocale(originalLocale);
        OADateTime.setDefaultZoneId(originalDefaultZoneId);
    }

    @Test
    void regionZoneOffsetAndNoZoneParsingCreateExpectedSemanticTypes() {
        OADateTime region = OADateTime.valueOf("2026-06-09 10:30 America/New_York", "yyyy-MM-dd HH:mm VV", false);
        OADateTime offset = OADateTime.valueOf("2026-06-09 10:30 -04:00", "yyyy-MM-dd HH:mm XXX", false);
        OADateTime local = OADateTime.valueOf("2026-06-09 10:30", "yyyy-MM-dd HH:mm", false);

        assertNotNull(region);
        assertEquals(DateTimeType.ZonedInstant, region.getType());
        assertEquals(NEW_YORK, region.getZoneId());
        assertNotNull(offset);
        assertEquals(DateTimeType.Instant, offset.getType());
        assertNotNull(local);
        assertEquals(DateTimeType.Floating, local.getType());
        assertEquals(CHICAGO, local.getZoneId());
    }

    @Test
    void regionZoneParsingAtDstGapAndOverlapMatchesJavaTimeResolution() {
        OADateTime spring = OADateTime.valueOf("2026-03-08 02:30 America/New_York", "yyyy-MM-dd HH:mm VV", false);
        OADateTime fall = OADateTime.valueOf("2026-11-01 01:30 America/New_York", "yyyy-MM-dd HH:mm VV", false);
        ZonedDateTime expectedSpring = LocalDateTime.of(2026, 3, 8, 2, 30).atZone(NEW_YORK);
        ZonedDateTime expectedFall = LocalDateTime.of(2026, 11, 1, 1, 30).atZone(NEW_YORK);

        assertNotNull(spring);
        assertEquals(DateTimeType.ZonedInstant, spring.getType());
        assertEquals(NEW_YORK, spring.getZoneId());
        assertEquals(expectedSpring.toInstant().toEpochMilli(), spring.getTime());
        assertEquals(expectedSpring.toLocalDateTime(), spring.toLocalDateTime());

        assertNotNull(fall);
        assertEquals(DateTimeType.ZonedInstant, fall.getType());
        assertEquals(NEW_YORK, fall.getZoneId());
        assertEquals(expectedFall.toInstant().toEpochMilli(), fall.getTime());
        assertEquals(expectedFall.getOffset(), fall.toZonedDateTime().getOffset());
    }

    @Test
    void strictParsingRejectsInvalidDatesAndTrailingInput() {
        assertNull(OADateTime.valueOf("2026-02-30 10:30", "yyyy-MM-dd HH:mm", false));
        assertNull(OADateTime.valueOf("2026-06-09 10:30 trailing", "yyyy-MM-dd HH:mm", false));
        assertNull(OADate.valueOf("2026-02-30", "yyyy-MM-dd", false));
        assertNull(OATime.valueOf("10:30 trailing"));
    }

    @Test
    void formattedDateTimeRoundTripPreservesInstantForExplicitOffset() {
        OADateTime original = new OADateTime(NEW_YORK, 2026, 6, 9, 10, 30, 15, 0);
        String text = original.toString("yyyy-MM-dd HH:mm:ss XXX");

        OADateTime parsed = OADateTime.valueOf(text, "yyyy-MM-dd HH:mm:ss XXX", false);

        assertNotNull(parsed);
        assertEquals(DateTimeType.Instant, parsed.getType());
        assertEquals(original.getTime(), parsed.getTime());
    }

    @Test
    void oadateAndOatimeRoundTripThroughCustomFormats() {
        OADate.addGlobalParseFormat("yyyyMMdd");
        OATime.addGlobalParseFormat("HHmmss");

        OADate date = new OADate(2026, 6, 9);
        OATime time = new OATime(15, 25, 30, 0);

        OADate parsedDate = OADate.dateValue(date.toString("yyyyMMdd"));
        OATime parsedTime = OATime.timeValue(time.toString("HHmmss"));

        assertEquals(LocalDate.of(2026, 6, 9), parsedDate.getLocalDate());
        assertEquals(LocalTime.MIDNIGHT, parsedDate.getLocalTime());
        assertEquals(LocalDate.of(1970, 1, 1), parsedTime.getLocalDate());
        assertEquals(LocalTime.of(15, 25, 30), parsedTime.getLocalTime());
    }

    @Test
    void removedGlobalParseFormatNoLongerParses() {
        OADateTime.addGlobalParseFormat("yyyyMMdd HHmmss");
        assertNotNull(OADateTime.valueOf("20260609 153015"));

        OADateTime.removeGlobalParseFormat("yyyyMMdd HHmmss");
        assertNull(OADateTime.valueOf("20260609 153015"));
    }
}
