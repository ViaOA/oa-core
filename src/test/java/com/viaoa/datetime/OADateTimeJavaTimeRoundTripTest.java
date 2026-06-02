package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OADateTimeJavaTimeRoundTripTest {

    private TimeZone originalJvmTz;
    private TimeZone originalOaTz;

    @BeforeEach
    void setUtc() {
        originalJvmTz = TimeZone.getDefault();
        originalOaTz = OADateTime.getDefaultTimeZone();
        TimeZone utc = TimeZone.getTimeZone("UTC");
        TimeZone.setDefault(utc);
        OADateTime.setDefaultTimeZone(utc);
    }

    @AfterEach
    void restore() {
        OADateTime.setDefaultTimeZone(originalOaTz);
        TimeZone.setDefault(originalJvmTz);
    }

    @Test
    void localDateRoundTripPreservesCalendarFields() {
        LocalDate ld = LocalDate.of(2026, 5, 27);
        OADate date = new OADate(ld);

        assertEquals(2026, date.getYear());
        assertEquals(4, date.getMonth());
        assertEquals(27, date.getDay());
        assertEquals(ld, date.getLocalDate());
    }

    @Test
    void localTimeRoundTripPreservesMillisecondPrecision() {
        LocalTime lt = LocalTime.of(7, 8, 9, 123_000_000);
        OATime time = new OATime(lt);

        assertEquals(7, time.get24Hour());
        assertEquals(8, time.getMinute());
        assertEquals(9, time.getSecond());
        assertEquals(123, time.getMilliSecond()); // was 0
        assertEquals(lt, time.getLocalTime());
    }

    @Test
    void localDateTimeRoundTripDocumentsCurrentMillisecondToNanosecondBehavior() {
        LocalDateTime ldt = LocalDateTime.of(2026, 5, 27, 7, 8, 9, 123_000_000);
        OADateTime dt = new OADateTime(ldt);

        assertEquals(2026, dt.getYear());
        assertEquals(4, dt.getMonth());
        assertEquals(27, dt.getDay());
        assertEquals(7, dt.get24Hour());
        assertEquals(8, dt.getMinute());
        assertEquals(9, dt.getSecond());

        // Current implementation has had CODEX attention around nanos/millis.
        // This assertion locks the intended public LocalDateTime view.
        assertEquals(ldt, dt.getLocalDateTime()); // expected: <2026-05-27T07:08:09.123> but was: <2026-05-27T07:08:09>
    }

    @Test
    void instantRoundTripPreservesEpochMillis() {
        Instant instant = Instant.ofEpochMilli(1716818889123L);
        OADateTime dt = new OADateTime(instant);

        assertEquals(1716818889123L, dt.getTime());
        assertEquals(instant, dt.getInstant()); // expected: <2024-05-27T14:08:09.123Z> but was: <2024-05-27T14:08:09Z>
    }

    @Test
    void zonedDateTimeRoundTripPreservesInstantAndDocumentsZoneBehavior() {
        ZonedDateTime zdt = ZonedDateTime.of(2026, 5, 27, 7, 8, 9, 123_000_000, ZoneId.of("America/New_York"));
        OADateTime dt = new OADateTime(zdt);

        assertEquals(zdt.toInstant().toEpochMilli(), dt.getTime());

        ZonedDateTime actual = dt.getZonedDateTime();
        assertEquals(zdt.toInstant(), actual.toInstant()); // expected: <2026-05-27T11:08:09.123Z> but was: <2026-05-27T11:08:09Z> 
        
        // If this fails, it documents whether current OADateTime(ZonedDateTime)
        // preserves the source zone or normalizes to the OA/default timezone.
        assertEquals(ZoneId.of("America/New_York"), actual.getZone());
    }

    @Test
    void getLocalDateAndLocalTimeViewsAreConsistentWithFields() {
        OADateTime dt = new OADateTime(2026, 4, 27, 7, 8, 9, 123);

        assertEquals(LocalDate.of(2026, 5, 27), dt.getLocalDateTime());  // 2026-05-27, but was: 2026-05-27T07:08:09
        assertEquals(LocalTime.of(7, 8, 9, 123_000_000), dt.getLocalDateTime());
    }
}
