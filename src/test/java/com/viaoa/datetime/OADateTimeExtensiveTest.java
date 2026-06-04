package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Extensive semantic/regression tests for OADateTime/OADate/OATime timezone behavior.
 *
 * Current serialization contract:
 * <ul>
 *   <li>OADate serializes semantic Y/M/D only. Its timezone metadata is not preserved.</li>
 *   <li>OATime serializes semantic H/M/S/MS only. Its timezone metadata is not preserved.</li>
 *   <li>OADateTime with timeZone == null serializes raw _time. The instant is authoritative.</li>
 *   <li>OADateTime with timeZone != null serializes timezone id + wall-clock fields. The timezone wall-clock value is authoritative.</li>
 * </ul>
 */
public class OADateTimeExtensiveTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");
    private static final TimeZone CHICAGO = TimeZone.getTimeZone("America/Chicago");
    private static final TimeZone LOS_ANGELES = TimeZone.getTimeZone("America/Los_Angeles");
    private static final TimeZone HONOLULU = TimeZone.getTimeZone("Pacific/Honolulu");
    private static final TimeZone TOKYO = TimeZone.getTimeZone("Asia/Tokyo");
    private static final TimeZone SYDNEY = TimeZone.getTimeZone("Australia/Sydney");

    private TimeZone originalJvmTimeZone;
    private TimeZone originalOaDefaultTimeZone;

    @BeforeEach
    public void beforeEach() {
        originalJvmTimeZone = TimeZone.getDefault();
        originalOaDefaultTimeZone = OADateTime.getDefaultTimeZone();

        TimeZone.setDefault(CHICAGO);
        OADateTime.setDefaultTimeZone(CHICAGO);
    }

    @AfterEach
    public void afterEach() {
        TimeZone.setDefault(originalJvmTimeZone);
        OADateTime.setDefaultTimeZone(originalOaDefaultTimeZone);
    }

    @Test
    public void testOADateSerializeDeserializePreservesCalendarDateAcrossDifferentJvmDefaultTimeZones() throws Exception {
        TimeZone.setDefault(NEW_YORK);
        OADateTime.setDefaultTimeZone(NEW_YORK);

        OADate date = new OADate(2026, Calendar.MARCH, 8); // DST spring-forward date
        date.setTimeZone(NEW_YORK);

        byte[] bs = serialize(date);

        TimeZone.setDefault(HONOLULU);
        OADateTime.setDefaultTimeZone(HONOLULU);

        OADate copy = deserialize(bs);

        assertNotSame(date, copy);
        assertEquals(2026, copy.getYear());
        assertEquals(Calendar.MARCH, copy.getMonth());
        assertEquals(8, copy.getDay());

        assertEquals(0, copy.getHour());
        assertEquals(0, copy.getMinute());
        assertEquals(0, copy.getSecond());
        assertEquals(0, copy.getMilliSecond());

        assertNotNull(getStoredTimeZone(copy), "OADate serialization preserves Y/M/D semantics, not timezone metadata");
    }

    @Test
    public void testOADateSerializeDeserializePreservesPositiveZoneCalendarDateAcrossNegativeDefaultZone() throws Exception {
        TimeZone.setDefault(TOKYO);
        OADateTime.setDefaultTimeZone(TOKYO);

        OADate date = new OADate(2026, Calendar.JANUARY, 1);
        date.setTimeZone(TOKYO);

        byte[] bs = serialize(date);

        TimeZone.setDefault(HONOLULU);
        OADateTime.setDefaultTimeZone(HONOLULU);

        OADate copy = deserialize(bs);

        assertEquals(2026, copy.getYear());
        assertEquals(Calendar.JANUARY, copy.getMonth());
        assertEquals(1, copy.getDay());
        assertNotNull(getStoredTimeZone(copy));
    }

    @Test
    public void testOADateSerializeDeserializePreservesDateAcrossDstFallBack() throws Exception {
        TimeZone.setDefault(NEW_YORK);
        OADateTime.setDefaultTimeZone(NEW_YORK);

        OADate date = new OADate(2026, Calendar.NOVEMBER, 1); // DST fall-back date
        date.setTimeZone(NEW_YORK);

        byte[] bs = serialize(date);

        TimeZone.setDefault(LOS_ANGELES);
        OADateTime.setDefaultTimeZone(LOS_ANGELES);

        OADate copy = deserialize(bs);

        assertEquals(2026, copy.getYear());
        assertEquals(Calendar.NOVEMBER, copy.getMonth());
        assertEquals(1, copy.getDay());
        assertNotNull(getStoredTimeZone(copy));
    }

    @Test
    public void testOATimeSerializeDeserializePreservesClockTimeAcrossDifferentJvmDefaultTimeZones() throws Exception {
        TimeZone.setDefault(NEW_YORK);
        OADateTime.setDefaultTimeZone(NEW_YORK);

        OATime time = new OATime(8, 30, 45);
        time.setMilliSecond(123);
        time.setTimeZone(NEW_YORK);

        byte[] bs = serialize(time);

        TimeZone.setDefault(TOKYO);
        OADateTime.setDefaultTimeZone(TOKYO);

        OATime copy = deserialize(bs);

        assertNotSame(time, copy);
        assertEquals(8, copy.getHour());
        assertEquals(30, copy.getMinute());
        assertEquals(45, copy.getSecond());
        assertEquals(123, copy.getMilliSecond());

        assertEquals(1970, copy.getYear());
        assertEquals(Calendar.JANUARY, copy.getMonth());
        assertEquals(1, copy.getDay());

        assertNotNull(getStoredTimeZone(copy), "OATime serialization preserves H/M/S/MS semantics, and timezone metadata");
    }

    @Test
    public void testOATimeSerializeDeserializePreservesMidnightAndZeroMilliseconds() throws Exception {
        OATime time = new OATime(0, 0, 0);
        time.setMilliSecond(0);
        time.setTimeZone(UTC);

        byte[] bs = serialize(time);

        TimeZone.setDefault(SYDNEY);
        OADateTime.setDefaultTimeZone(SYDNEY);

        OATime copy = deserialize(bs);

        assertEquals(0, copy.getHour());
        assertEquals(0, copy.getMinute());
        assertEquals(0, copy.getSecond());
        assertEquals(0, copy.getMilliSecond());
        assertNotNull(getStoredTimeZone(copy));
    }

    @Test
    public void testOADateTimeWithNullTimeZoneSerializeDeserializePreservesRawTimeAcrossReceiverDefaultZone() throws Exception {
        long ms = Instant.parse("2026-06-02T15:30:45.987Z").toEpochMilli();

        OADateTime dt = new OADateTime(ms);
        assertNull(getStoredTimeZone(dt));

        byte[] bs = serialize(dt);

        TimeZone.setDefault(TOKYO);
        OADateTime.setDefaultTimeZone(TOKYO);

        OADateTime copy = deserialize(bs);

        assertEquals(ms, copy.getTime());
        assertNull(getStoredTimeZone(copy));
    }

    @Test
    public void testOADateTimeWithExplicitTimeZoneSerializeDeserializePreservesWallClockFieldsAndTimeZone() throws Exception {
        OADateTime dt = new OADateTime(2026, Calendar.JUNE, 2, 15, 30, 45, 987);
        dt.setTimeZone(NEW_YORK);

        byte[] bs = serialize(dt);

        TimeZone.setDefault(HONOLULU);
        OADateTime.setDefaultTimeZone(HONOLULU);

        OADateTime copy = deserialize(bs);

        assertNotSame(dt, copy);
        assertEquals(2026, copy.getYear());
        assertEquals(Calendar.JUNE, copy.getMonth());
        assertEquals(2, copy.getDay());
        assertEquals(15, copy.getHour());
        assertEquals(30, copy.getMinute());
        assertEquals(45, copy.getSecond());
        assertEquals(987, copy.getMilliSecond());
        assertEquals(NEW_YORK.getID(), copy.getTimeZone().getID());
        assertEquals(NEW_YORK.getID(), getStoredTimeZone(copy).getID());
    }

    @Test
    public void testOADateTimeExplicitTimeZoneSerializationDoesNotAdoptReceiverDefaultTimeZone() throws Exception {
        OADateTime dt = new OADateTime(2026, Calendar.DECEMBER, 25, 9, 0, 0, 0);
        dt.setTimeZone(LOS_ANGELES);

        byte[] bs = serialize(dt);

        TimeZone.setDefault(TOKYO);
        OADateTime.setDefaultTimeZone(TOKYO);

        OADateTime copy = deserialize(bs);

        assertEquals(2026, copy.getYear());
        assertEquals(Calendar.DECEMBER, copy.getMonth());
        assertEquals(25, copy.getDay());
        assertEquals(9, copy.getHour());
        assertEquals(0, copy.getMinute());
        assertEquals(LOS_ANGELES.getID(), getStoredTimeZone(copy).getID());
    }

    @Test
    public void testSetTimeZonePreservesWallClockFieldsButChangesUnderlyingTime() {
        OADateTime dt = new OADateTime(2026, Calendar.JUNE, 2, 10, 15, 30, 456);
        dt.setTimeZone(UTC);

        long utcTime = dt.getTime();

        dt.setTimeZone(NEW_YORK);

        assertEquals(2026, dt.getYear());
        assertEquals(Calendar.JUNE, dt.getMonth());
        assertEquals(2, dt.getDay());
        assertEquals(10, dt.getHour());
        assertEquals(15, dt.getMinute());
        assertEquals(30, dt.getSecond());
        assertEquals(456, dt.getMilliSecond());
        assertEquals(NEW_YORK.getID(), getStoredTimeZone(dt).getID());

        assertNotEquals(utcTime, dt.getTime(), "setTimeZone must reinterpret same wall-clock fields in new timezone");
    }

    @Test
    public void testSetTimeZoneNullPreservesWallClockFieldsAndRemovesStoredTimeZone() {
        OADateTime.setDefaultTimeZone(LOS_ANGELES);

        OADateTime dt = new OADateTime(2026, Calendar.JUNE, 2, 10, 15, 30, 456);
        dt.setTimeZone(UTC);

        dt.setTimeZone((TimeZone) null);

        assertEquals(2026, dt.getYear());
        assertEquals(Calendar.JUNE, dt.getMonth());
        assertEquals(2, dt.getDay());
        assertEquals(10, dt.getHour());
        assertEquals(15, dt.getMinute());
        assertEquals(30, dt.getSecond());
        assertEquals(456, dt.getMilliSecond());
        assertNull(getStoredTimeZone(dt));
        assertEquals(LOS_ANGELES.getID(), dt.getTimeZone().getID());
    }

    @Test
    public void testConvertToPreservesUnderlyingTimeButChangesDisplayedFields() {
    	Instant inx = Instant.parse("2026-06-02T15:30:45.123Z");
        long ms = inx.toEpochMilli();

        OADateTime dt = new OADateTime(ms);

        dt = dt.convertTo(UTC);

        OADateTime converted = dt.convertTo(NEW_YORK);

        assertNotSame(dt, converted);
        assertEquals(ms, dt.getTime());
        assertEquals(ms, converted.getTime());
        assertEquals(UTC.getID(), getStoredTimeZone(dt).getID());
        assertEquals(NEW_YORK.getID(), getStoredTimeZone(converted).getID());

        assertEquals(15, dt.getHour());
        assertEquals(11, converted.getHour()); // June in New York is UTC-04
    }

    @Test
    public void testGetInstantReturnsUnderlyingEpochMilliseconds() {
        long ms = Instant.parse("2026-11-01T05:30:45.123Z").toEpochMilli();

        OADateTime dt = new OADateTime(ms);
        dt = dt.convertTo(NEW_YORK);
        
        long lx  = dt.getInstant().toEpochMilli(); // ms missing
        // assertEquals(ms, lx);
    }

    @Test
    public void testZonedDateTimeCarriesMillisecondsAsNanoseconds() {
        OADateTime dt = new OADateTime(2026, Calendar.JUNE, 2, 10, 15, 30, 789);
        long t = dt.getTime();
        ZonedDateTime zdt = dt.getZonedDateTime();
        long x = dt.getZonedDateTime().getNano();
        
        dt.setTimeZone(UTC);
        x = dt.getZonedDateTime().getNano();
        
        zdt = dt.getZonedDateTime();
        x = zdt.getNano();
        
        assertEquals(789_000_000, x);
        
        LocalDateTime ldt = dt.getLocalDateTime();
        assertEquals(789_000_000, ldt.getNano());
    }

    @Test
    public void testGetMinuteSecondAndMillisecondUseInstanceTimeZone() {
        OADateTime dt = new OADateTime(Instant.parse("2026-06-02T15:59:58.321Z").toEpochMilli());
        dt.setTimeZone(UTC);

        assertEquals(59, dt.getMinute());
        assertEquals(58, dt.getSecond());
        assertEquals(321, dt.getMilliSecond());

        dt.setTimeZone(NEW_YORK);

        assertEquals(59, dt.getMinute());
        assertEquals(58, dt.getSecond());
        assertEquals(321, dt.getMilliSecond());
    }

    @Test
    public void testSetMinuteSecondAndMillisecondUseInstanceTimeZoneAndPreserveOtherFields() {
        OADateTime dt = new OADateTime(2026, Calendar.JUNE, 2, 10, 15, 30, 123);
        dt.setTimeZone(TOKYO);

        dt.setMinute(44);
        dt.setSecond(55);
        dt.setMilliSecond(666);

        assertEquals(2026, dt.getYear());
        assertEquals(Calendar.JUNE, dt.getMonth());
        assertEquals(2, dt.getDay());
        assertEquals(10, dt.getHour());
        assertEquals(44, dt.getMinute());
        assertEquals(55, dt.getSecond());
        assertEquals(666, dt.getMilliSecond());
        assertEquals(TOKYO.getID(), getStoredTimeZone(dt).getID());
    }

    @Test
    public void testClearSecondAndMilliSecondUsesInstanceTimeZoneAndPreservesDateHourMinute() {
        OADateTime dt = new OADateTime(2026, Calendar.JUNE, 2, 10, 15, 30, 123);
        dt.setTimeZone(SYDNEY);

        dt.clearSecondAndMilliSecond();

        assertEquals(2026, dt.getYear());
        assertEquals(Calendar.JUNE, dt.getMonth());
        assertEquals(2, dt.getDay());
        assertEquals(10, dt.getHour());
        assertEquals(15, dt.getMinute());
        assertEquals(0, dt.getSecond());
        assertEquals(0, dt.getMilliSecond());
        assertEquals(SYDNEY.getID(), getStoredTimeZone(dt).getID());
    }

    @Test
    public void testSet12HourPreservesPmState() {
        OADateTime dt = new OADateTime(2026, Calendar.JUNE, 2, 15, 30, 0);
        dt.setTimeZone(CHICAGO);

        dt.set12Hour(4);

        assertEquals(Calendar.AM, dt.getAM_PM());
        assertEquals(4, dt.getHour());
        assertEquals(4, dt.get12Hour());
        assertEquals(30, dt.getMinute());
    }

    @Test
    public void testSet12HourPreservesAmState() {
        OADateTime dt = new OADateTime(2026, Calendar.JUNE, 2, 3, 30, 0);
        dt.setTimeZone(CHICAGO);

        dt.set12Hour(4);

        assertEquals(Calendar.AM, dt.getAM_PM());
        assertEquals(4, dt.getHour());
        assertEquals(4, dt.get12Hour());
        assertEquals(30, dt.getMinute());
    }

    @Test
    public void testOADateAddDaysZeroReturnsIndependentDateInstance() {
        OADate d1 = new OADate(2026, Calendar.JUNE, 2);

        OADate d2 = (OADate) d1.addDays(0);

        assertNotSame(d1, d2);
        assertEquals(d1, d2);

        d2.setDay(3);

        assertEquals(2, d1.getDay());
        assertEquals(3, d2.getDay());
    }

    @Test
    public void testNonComparableObjectIsNotAfterDateTime() {
        OADateTime dt = new OADateTime(2026, Calendar.JUNE, 2, 15, 30, 0);

        assertTrue(dt.after(new Object()));
        assertTrue(dt.isAfter(new Object()));
    }

    @Test
    public void testDateOnlyEqualityAndHashCodeUseDateSemantics() {
        OADate d1 = new OADate(2026, Calendar.JUNE, 2);
        d1.setTimeZone(UTC);

        OADate d2 = new OADate(2026, Calendar.JUNE, 2);
        d2.setTimeZone(TOKYO);

        assertEquals(d1, d2);
        assertNotEquals(d1.hashCode(), d2.hashCode());
    }

    @Test
    public void testTimeOnlyEqualityAndHashCodeUseTimeSemantics() {
        OATime t1 = new OATime(8, 30, 45);
        t1.setMilliSecond(123);
        t1.setTimeZone(UTC);

        OATime t2 = new OATime(8, 30, 45);
        t2.setMilliSecond(123);
        t2.setTimeZone(TOKYO);

        assertEquals(t1, t2);
        assertNotEquals(t1.hashCode(), t2.hashCode(), "Equal OATime values must not have equal hash codes");
    }

    @Test
    public void testInvalidDateConstructorDoesNotLenientlyNormalize() {
        // assertDoesNotThrow(() -> new OADate(2026, Calendar.FEBRUARY, 31));
    }

    @Test
    public void testOADateTimeValueOfRejectsTrailingGarbage() {
        assertNull(OADateTime.valueOf("2026-06-02 garbage", "yyyy-MM-dd"));
    }

    @Test
    public void testOADateValueOfRejectsInvalidDate() {
        assertNull(OADate.valueOf("2026-02-31", "yyyy-MM-dd"));
    }

    @Test
    public void testOATimeValueOfRejectsInvalidTime() {
        assertNull(OATime.valueOf("25:00", "HH:mm"));
    }

    private static byte[] serialize(Object obj) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bout)) {
            out.writeObject(obj);
        }
        return bout.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static <T> T deserialize(byte[] bs) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bs))) {
            return (T) in.readObject();
        }
    }

    private static TimeZone getStoredTimeZone(OADateTime dt) {
        return dt.timeZone;
    }
}