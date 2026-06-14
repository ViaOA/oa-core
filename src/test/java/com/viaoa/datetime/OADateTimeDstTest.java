package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OADateTimeDstTest {
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");

    private ZoneId originalDefaultZoneId;
    private Locale originalLocale;

    @BeforeEach
    void beforeEach() {
        originalDefaultZoneId = OADateTime.getDefaultZoneId();
        originalLocale = Locale.getDefault();
        OADateTime.setLocale(Locale.US);
        OADateTime.setDefaultZoneId(NEW_YORK);
    }

    @AfterEach
    void afterEach() {
        OADateTime.setLocale(originalLocale);
        OADateTime.setDefaultZoneId(originalDefaultZoneId);
    }

    @Test
    void springForwardNonexistentLocalTimeResolvesLikeJavaTime() {
        LocalDateTime missing = LocalDateTime.of(2026, 3, 8, 2, 30);
        ZonedDateTime expected = missing.atZone(NEW_YORK);

        OADateTime actual = new OADateTime(NEW_YORK, 2026, 3, 8, 2, 30, 0, 0);

        assertEquals(expected.toInstant().toEpochMilli(), actual.getTime());
        assertEquals(expected.toLocalDateTime(), actual.toLocalDateTime());
        assertEquals(NEW_YORK, actual.getZoneId());
    }

    @Test
    void fallBackAmbiguousLocalTimeResolvesLikeJavaTime() {
        LocalDateTime ambiguous = LocalDateTime.of(2026, 11, 1, 1, 30);
        ZonedDateTime expected = ambiguous.atZone(NEW_YORK);

        OADateTime actual = new OADateTime(NEW_YORK, 2026, 11, 1, 1, 30, 0, 0);

        assertEquals(expected.toInstant().toEpochMilli(), actual.getTime());
        assertEquals(expected.toLocalDateTime(), actual.toLocalDateTime());
        assertEquals(expected.getOffset(), actual.toZonedDateTime().getOffset());
    }

    @Test
    void exactSpringForwardDayHasOneCalendarDayAndTwentyThreeElapsedHours() {
        OADateTime start = new OADateTime(ZonedDateTime.of(2026, 3, 8, 0, 0, 0, 0, NEW_YORK));
        OADateTime end = new OADateTime(ZonedDateTime.of(2026, 3, 9, 0, 0, 0, 0, NEW_YORK));

        assertEquals(1, start.betweenDays(end));
        assertEquals(23, start.betweenHours(end));
        assertEquals(Duration.ofHours(23), start.betweenDuration(end));
        assertEquals(start.toZonedDateTime().plusDays(1).toInstant().toEpochMilli(), start.plusDays(1).getTime());
    }

    @Test
    void exactFallBackDayHasOneCalendarDayAndTwentyFiveElapsedHours() {
        OADateTime start = new OADateTime(ZonedDateTime.of(2026, 11, 1, 0, 0, 0, 0, NEW_YORK));
        OADateTime end = new OADateTime(ZonedDateTime.of(2026, 11, 2, 0, 0, 0, 0, NEW_YORK));

        assertEquals(1, start.betweenDays(end));
        assertEquals(25, start.betweenHours(end));
        assertEquals(Duration.ofHours(25), start.betweenDuration(end));
        assertEquals(start.toZonedDateTime().plusDays(1).toInstant().toEpochMilli(), start.plusDays(1).getTime());
    }

    @Test
    void formattingShowsOffsetBeforeAndAfterDstTransitions() {
        OADateTime beforeSpring = new OADateTime(ZonedDateTime.of(2026, 3, 8, 1, 30, 0, 0, NEW_YORK));
        OADateTime afterSpring = beforeSpring.plusHours(1);
        OADateTime beforeFall = new OADateTime(ZonedDateTime.of(2026, 11, 1, 0, 30, 0, 0, NEW_YORK));
        OADateTime afterFall = beforeFall.plusHours(2);

        assertEquals("2026-03-08 01:30 -05:00", beforeSpring.toString("yyyy-MM-dd HH:mm XXX"));
        assertEquals("2026-03-08 03:30 -04:00", afterSpring.toString("yyyy-MM-dd HH:mm XXX"));
        assertEquals("2026-11-01 00:30 -04:00", beforeFall.toString("yyyy-MM-dd HH:mm XXX"));
        assertEquals("2026-11-01 01:30 -05:00", afterFall.toString("yyyy-MM-dd HH:mm XXX"));
    }

    @Test
    void sameInstantAndSameWallTimeAcrossDstBoundaryUseDifferentSemantics() {
        OADateTime base = new OADateTime(ZonedDateTime.of(2026, 3, 8, 1, 30, 0, 0, NEW_YORK));

        OADateTime sameInstant = base.withZoneIdSameInstant(CHICAGO);
        OADateTime sameWall = base.withZoneIdSameWallTime(CHICAGO);

        assertEquals(base.getTime(), sameInstant.getTime());
        assertEquals(base.toZonedDateTime().withZoneSameInstant(CHICAGO).toLocalDateTime(), sameInstant.toLocalDateTime());
        assertEquals(base.toLocalDateTime(), sameWall.toLocalDateTime());
        assertEquals(base.toLocalDateTime().atZone(CHICAGO).toInstant().toEpochMilli(), sameWall.getTime());
        assertNotEquals(base.getTime(), sameWall.getTime());
    }
}
