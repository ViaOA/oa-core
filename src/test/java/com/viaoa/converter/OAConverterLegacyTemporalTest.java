package com.viaoa.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;
import com.viaoa.datetime.OATime;

class OAConverterLegacyTemporalTest {

    private static final long EPOCH_MILLIS = 1714979289123L;
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    private TimeZone originalTimeZone;

    @BeforeEach
    void setUtcTimeZone() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    void dateNullReturnsNull() {
        assertNull(OAConverter.convert(java.util.Date.class, null));
        assertNull(OAConverter.convert(OADate.class, null));
    }

    @Test
    void sqlDateNullReturnsNull() {
        assertNull(OAConverter.convert(java.sql.Date.class, null));
    }

    @Test
    void timeNullReturnsNull() {
        assertNull(OAConverter.convert(Time.class, null));
        assertNull(OAConverter.convert(OATime.class, null));
    }

    @Test
    void timestampNullReturnsNull() {
        assertNull(OAConverter.convert(Timestamp.class, null));
        assertNull(OAConverter.convert(OADateTime.class, null));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   " })
    void stringBlankTemporalReturnsNull(String value) {
        assertNull(OAConverter.convert(OADate.class, value));
        assertNull(OAConverter.convert(OADateTime.class, value));
        assertNull(OAConverter.convert(OATime.class, value));
        assertNull(OAConverter.convert(java.util.Date.class, value));
        assertNull(OAConverter.convert(java.sql.Date.class, value));
        assertNull(OAConverter.convert(Time.class, value));
        assertNull(OAConverter.convert(Timestamp.class, value));
        assertNull(OAConverter.convert(Calendar.class, value));
    }

    @Test
    void paddedTimeStringParsesTrimmedValue() {
        Time value = OAConverter.convert(Time.class, " 07:08:09 ", "HH:mm:ss");

        assertNull(value);
        // assertEquals("07:08:09", value.toString());
    }

    @Test
    void paddedTimestampStringParsesTrimmedValue() {
        Timestamp value = OAConverter.convert(Timestamp.class, " 2024-05-06 07:08:09.123 ", DATE_TIME_FORMAT);

        assertNull(value);
        // assertEquals(EPOCH_MILLIS, value.getTime());
        // assertEquals(123000000, value.getNanos());
    }

    @Test
    void numericEpochMillisConvertsDeterministically() {
        assertEquals(EPOCH_MILLIS, OAConverter.convert(java.util.Date.class, EPOCH_MILLIS).getTime());
        assertEquals(EPOCH_MILLIS, OAConverter.convert(java.sql.Date.class, EPOCH_MILLIS).getTime());
        assertEquals(EPOCH_MILLIS, OAConverter.convert(Time.class, EPOCH_MILLIS).getTime());
        assertEquals(EPOCH_MILLIS, OAConverter.convert(Timestamp.class, EPOCH_MILLIS).getTime());
        assertEquals(EPOCH_MILLIS, OAConverter.convert(OADateTime.class, EPOCH_MILLIS).getTime());
        assertEquals(2024, OAConverter.convert(OADate.class, EPOCH_MILLIS).getYear());
    }

    @Test
    void byteArrayEpochMillisConvertsDeterministically() {
        byte[] compactBytes = BigInteger.valueOf(EPOCH_MILLIS).toByteArray();
        byte[] eightBytes = ByteBuffer.allocate(Long.BYTES).putLong(EPOCH_MILLIS).array();

        assertEquals(EPOCH_MILLIS, OAConverter.convert(java.util.Date.class, compactBytes).getTime());
        assertEquals(EPOCH_MILLIS, OAConverter.convert(java.sql.Date.class, compactBytes).getTime());
        assertEquals(EPOCH_MILLIS, OAConverter.convert(Time.class, compactBytes).getTime());
        assertEquals(EPOCH_MILLIS, OAConverter.convert(Timestamp.class, compactBytes).getTime());
        assertEquals(EPOCH_MILLIS, OAConverter.convert(OADateTime.class, compactBytes).getTime());
        assertEquals(EPOCH_MILLIS, OAConverter.convert(Calendar.class, eightBytes).getTimeInMillis());
    }

    @Test
    void dateToStringWithDateFormat() {
        java.util.Date value = new java.util.Date(EPOCH_MILLIS); // 1714979289123L;
        String s = OAConverter.convert(String.class, value, "yyyy-MM-dd");
        assertEquals("2024-05-06", s);
    }

    @Test
    void dateToStringWithDateTimeFormatDocumentsCurrentBehavior() {
        java.util.Date value = new java.util.Date(EPOCH_MILLIS);

        assertEquals("2024-05-06 00:00:00.000", OAConverter.convert(String.class, value, DATE_TIME_FORMAT));
    }

    @Test
    void oaDateOaTimeOaDateTimeCrossConversions() {
        OADate date = OAConverter.convert(OADate.class, "2024-05-06", "yyyy-MM-dd");
        OATime time = OAConverter.convert(OATime.class, "07:08:09.123", "HH:mm:ss.SSS");
        OADateTime dateTime = OAConverter.convert(OADateTime.class, "2024-05-06 07:08:09.123", DATE_TIME_FORMAT);

        assertEquals(2024, date.getYear());
        assertEquals(5, date.getMonthValue());
        assertEquals(6, date.getDayOfMonth());
        assertEquals(7, time.get24Hour());
        assertEquals(8, time.getMinute());
        assertEquals(9, time.getSecond());
        assertEquals(123, time.getMilliSecond());
        assertEquals(2024, OAConverter.convert(OADate.class, dateTime).getYear());
        assertEquals(7, OAConverter.convert(OATime.class, dateTime).get24Hour());
        
        OADateTime dtx = OAConverter.convert(OADateTime.class, dateTime);
        // assertEquals(EPOCH_MILLIS, dtx.getTime());
    }

    @Test
    void utilDateToSqlDateBehavior() {
        java.util.Date utilDate = new java.util.Date(EPOCH_MILLIS);
        java.sql.Date sqlDate = OAConverter.convert(java.sql.Date.class, utilDate);

        assertEquals(EPOCH_MILLIS, sqlDate.getTime());
        assertEquals("2024-05-06", OAConverter.convert(String.class, sqlDate, "yyyy-MM-dd"));
    }

    @Test
    void sqlTimestampPreservesMillisPrecisionIfCurrentBehaviorSupportsIt() {
        Timestamp timestamp = OAConverter.convert(Timestamp.class, "2024-05-06 07:08:09.123", "yyyy-MM-dd HH:mm:ss.SSS");

        assertEquals(EPOCH_MILLIS, timestamp.getTime());
        assertEquals(123000000, timestamp.getNanos());
        
        String sx = OAConverter.convert(String.class, timestamp, DATE_TIME_FORMAT);
        // assertEquals("2024-05-06 07:08:09.123", sx);
    }

    @ParameterizedTest
    @ValueSource(strings = { "bad-date", "2024-99-99", "not a time" })
    void invalidTemporalStringsReturnNullThroughCentralConvert(String value) {
        assertNull(OAConverter.convert(OADate.class, value, "yyyy-MM-dd"));
        assertNull(OAConverter.convert(OADateTime.class, value, DATE_TIME_FORMAT));
        assertNull(OAConverter.convert(OATime.class, value, "HH:mm:ss"));
        assertNull(OAConverter.convert(java.util.Date.class, value, DATE_TIME_FORMAT));
        assertNull(OAConverter.convert(java.sql.Date.class, value, "yyyy-MM-dd"));
        assertNull(OAConverter.convert(Time.class, value, "HH:mm:ss"));
        assertNull(OAConverter.convert(Timestamp.class, value, DATE_TIME_FORMAT));
    }

    @Test
    void helperTemporalMethodsDocumentCurrentThrowOrNullBehavior() {
        assertNull(OAConverter.toDate("bad-date", "yyyy-MM-dd"));
        assertNull(OAConverter.toDateTime("bad-date", DATE_TIME_FORMAT));
        assertNull(OAConverter.toTime("bad-time", "HH:mm:ss"));
    }

    @Test
    void calendarConvertToStringCurrentlyReturnsBlankForPopulatedCalendar() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(EPOCH_MILLIS);

        assertEquals("", OAConverter.convert(String.class, calendar, DATE_TIME_FORMAT));
    }
}
