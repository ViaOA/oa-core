package com.viaoa.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;
import com.viaoa.datetime.OATime;

class OAConverterRoundTripTest {

    @Test
    void integerStringInteger() {
        Integer value = 42;
        assertEquals(value, OAConverter.convert(Integer.class, OAConverter.toString(value)));
    }

    @Test
    void longStringLong() {
        Long value = 1234567890123L;
        assertEquals(value, OAConverter.convert(Long.class, OAConverter.toString(value)));
    }

    @Test
    void doubleStringDouble() {
        Double value = 123.456d;
        assertEquals(value, OAConverter.convert(Double.class, OAConverter.toString(value)));
    }

    @Test
    void bigDecimalStringBigDecimal() {
        BigDecimal value = new BigDecimal("12345.67");
        BigDecimal result = OAConverter.convert(BigDecimal.class, OAConverter.toString(value));

        assertEquals(0, value.compareTo(result));
    }

    @Test
    void booleanStringBooleanWithExplicitFormat() {
        String fmt = "yes;no;unknown";

        assertEquals(Boolean.TRUE, OAConverter.convert(Boolean.class, OAConverter.toString(Boolean.TRUE, fmt), fmt));
        assertEquals(Boolean.FALSE, OAConverter.convert(Boolean.class, OAConverter.toString(Boolean.FALSE, fmt), fmt));
    }

    @Test
    void enumStringEnum() {
        Sample value = Sample.SECOND;

        assertEquals(value, OAConverter.convert(Sample.class, OAConverter.toString(value)));
    }

    @Test
    void classStringClass() {
        Class<?> value = java.util.Map.class;

        assertSame(value, OAConverter.convert(Class.class, OAConverter.toString(value)));
    }

    @Test
    void oaDateStringOADateWithExplicitFormat() {
        OADate value = new OADate(2024, 4, 6);
        String fmt = "yyyy-MM-dd";
        OADate result = OAConverter.convert(OADate.class, OAConverter.toString(value, fmt), fmt);

        assertEquals(value.getYear(), result.getYear());
        assertEquals(value.getMonth(), result.getMonth());
        assertEquals(value.getDay(), result.getDay());
    }

    @Test
    void oaDateTimeStringOADateTimeWithExplicitFormat() {
        OADateTime value = OAConverter.convert(OADateTime.class, "2024-05-06 07:08:09.123", "yyyy-MM-dd HH:mm:ss.SSS");
        String fmt = "yyyy-MM-dd HH:mm:ss.SSS";

        assertEquals(value.getTime(), OAConverter.convert(OADateTime.class, OAConverter.toString(value, fmt), fmt).getTime());
    }

    @Test
    void oaTimeStringOATimeWithExplicitFormat() {
        OATime value = new OATime(7, 8, 9);
        String fmt = "HH:mm:ss";
        OATime result = OAConverter.convert(OATime.class, OAConverter.toString(value, fmt), fmt);

        assertEquals(value.get24Hour(), result.get24Hour());
        assertEquals(value.getMinute(), result.getMinute());
        assertEquals(value.getSecond(), result.getSecond());
    }

    @Test
    void localDateStringLocalDateWithExplicitFormat() {
        LocalDate value = LocalDate.of(2024, 5, 6);
        String fmt = "yyyy-MM-dd";

        assertEquals(value, OAConverter.convert(LocalDate.class, OAConverter.toString(value, fmt), fmt));
    }

    @Test
    void localDateTimeStringLocalDateTimeWithExplicitFormat() {
        LocalDateTime value = LocalDateTime.of(2024, 5, 6, 7, 8, 9, 123_000_000);
        String fmt = "yyyy-MM-dd HH:mm:ss.SSS";

        assertEquals(value, OAConverter.convert(LocalDateTime.class, OAConverter.toString(value, fmt), fmt));
    }

    @Test
    void localTimeStringLocalTimeWithExplicitFormat() {
        LocalTime value = LocalTime.of(7, 8, 9);
        String fmt = "HH:mm:ss";

        assertEquals(value, OAConverter.convert(LocalTime.class, OAConverter.toString(value, fmt), fmt));
    }

    @Test
    void zoneIdStringZoneId() {
        ZoneId value = ZoneId.of("America/Chicago");

        assertEquals(value, OAConverter.convert(ZoneId.class, OAConverter.toString(value)));
    }

    @Test
    void timeZoneStringTimeZone() {
        TimeZone value = TimeZone.getTimeZone("UTC");
        String text = OAConverter.toString(value);
        TimeZone result = OAConverter.convert(TimeZone.class, text);

        assertNotNull(result);
        assertEquals(value.getRawOffset(), result.getRawOffset());
    }

    private enum Sample {
        FIRST, SECOND
    }
}
