/*
 * Stress/coverage-oriented unit tests for com.viaoa.util.OADateTime.
 *
 * Goals:
 *  - Deterministic tests (force JVM + OADateTime default timezone to UTC)
 *  - Broad behavioral coverage with parameter sweeps
 *  - Extra “stress” coverage: multi-thread formatter usage + reflection-based smoke tests
 *
 * Notes:
 *  - These tests live in the same package as OADateTime so they can access
 *    protected members if needed later.
 *  - This is intentionally not “one tiny test per method”; instead it uses
 *    invariants, round-trips, and sweeps to stress many methods efficiently.
 */
package com.viaoa.util;

import static org.junit.Assert.*;

import java.io.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.*;

import com.viaoa.datetime.OADateTime;

public class OADateTimeStressTest {

    private static TimeZone originalJvmTz;
    private static TimeZone originalOaDefaultTz;
    private static Locale originalLocale;

    @BeforeClass
    public static void beforeClass() {
        originalJvmTz = TimeZone.getDefault();
        originalOaDefaultTz = OADateTime.getDefaultTimeZone();
        originalLocale = Locale.getDefault();

        TimeZone utc = TimeZone.getTimeZone("UTC");
        TimeZone.setDefault(utc);
        OADateTime.setDefaultTimeZone(utc);
        OADateTime.setLocale(Locale.US); // stable patterns (month-first)
        Locale.setDefault(Locale.US);
    }

    @AfterClass
    public static void afterClass() {
        if (originalJvmTz != null) TimeZone.setDefault(originalJvmTz);
        if (originalOaDefaultTz != null) OADateTime.setDefaultTimeZone(originalOaDefaultTz);
        if (originalLocale != null) {
            Locale.setDefault(originalLocale);
            OADateTime.setLocale(originalLocale);
        }
    }

    // --------------------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------------------

    private static OADateTime dtUtc(int y, int mon0, int d, int h, int m, int s, int ms) {
        OADateTime dt = new OADateTime(y, mon0, d, h, m, s, ms);
        dt.setTimeZoneUTC();
        return dt;
    }

    private static GregorianCalendar calUtc(int y, int mon0, int d, int h, int m, int s, int ms) {
        GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        c.set(Calendar.YEAR, y);
        c.set(Calendar.MONTH, mon0);
        c.set(Calendar.DAY_OF_MONTH, d);
        c.set(Calendar.HOUR_OF_DAY, h);
        c.set(Calendar.MINUTE, m);
        c.set(Calendar.SECOND, s);
        c.set(Calendar.MILLISECOND, ms);
        return c;
    }

    private static void assertSameInstant(OADateTime a, OADateTime b) {
        assertNotNull(a);
        assertNotNull(b);
        assertEquals(a.getTime(), b.getTime());
    }

    private static void assertFieldsMatchCalendar(OADateTime dt, Calendar c) {
        assertEquals(c.get(Calendar.YEAR), dt.getYear());
        assertEquals(c.get(Calendar.MONTH), dt.getMonth());
        assertEquals(c.get(Calendar.DAY_OF_MONTH), dt.getDay());
        assertEquals(c.get(Calendar.HOUR_OF_DAY), dt.get24Hour());
        assertEquals(c.get(Calendar.MINUTE), dt.getMinute());
        assertEquals(c.get(Calendar.SECOND), dt.getSecond());
        assertEquals(c.get(Calendar.MILLISECOND), dt.getMilliSecond());
    }

    private static void assertNonThrow(Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            fail("Expected no exception, got: " + t.getClass().getName() + " - " + t.getMessage());
        }
    }

    // --------------------------------------------------------------------------------------------
    // A) Constructors & core state
    // --------------------------------------------------------------------------------------------

    @Test
    public void ctor_default_nearNow() {
        long before = System.currentTimeMillis();
        OADateTime dt = new OADateTime();
        long after = System.currentTimeMillis();
        assertTrue(dt.getTime() >= before && dt.getTime() <= after);
    }

    @Test
    public void ctor_long_roundTrip_getTime() {
        long[] samples = {0L, 1L, -1L, 123456789L, System.currentTimeMillis(), Long.MIN_VALUE / 1024, Long.MAX_VALUE / 1024};
        for (long t : samples) {
            OADateTime dt = new OADateTime(t);
            assertEquals(t, dt.getTime());
        }
    }

    @Test
    public void ctor_date_timestamp_calendar_copy() {
        GregorianCalendar c = calUtc(2023, Calendar.SEPTEMBER, 8, 14, 15, 47, 34);
        Date d = new Date(c.getTimeInMillis());
        Timestamp ts = new Timestamp(c.getTimeInMillis());

        OADateTime a = new OADateTime(d);
        OADateTime b = new OADateTime(ts);
        OADateTime cc = new OADateTime((Calendar) c);
        OADateTime copy = new OADateTime(cc);

        assertEquals(d.getTime(), a.getTime());
        assertEquals(ts.getTime(), b.getTime());
        assertEquals(c.getTimeInMillis(), cc.getTime());
        assertEquals(cc.getTime(), copy.getTime());
    }

    @Test
    public void ctor_string_withFormat_rfc339Wms_example() {
        String sx = "2023-09-08T14:15:47.034Z";
        OADateTime dt = new OADateTime(sx, OADateTime.RFC339FormatWms);
        // File comment says caller should call setTimeZoneUTC() when 'Z' literal is used.
        dt.setTimeZoneUTC();
        assertEquals(sx, dt.toString(OADateTime.RFC339FormatWms));
    }

    // --------------------------------------------------------------------------------------------
    // B) Field getters/setters (round-trip sweeps)
    // --------------------------------------------------------------------------------------------

    @Test
    public void roundTrip_setYear_getYear_sweep() {
        for (int year : new int[]{1970, 1999, 2000, 2023, 2024, 2038}) {
            OADateTime dt = dtUtc(2020, 0, 2, 3, 4, 5, 6);
            dt.setYear(year);
            assertEquals(year, dt.getYear());
        }
    }

    @Test
    public void roundTrip_setMonth_getMonth_sweep() {
        for (int mon = 0; mon <= 11; mon++) {
            OADateTime dt = dtUtc(2023, 0, 15, 10, 11, 12, 13);
            dt.setMonth(mon);
            assertEquals(mon, dt.getMonth());
        }
    }

    @Test
    public void roundTrip_setDay_getDay_sweep_validDays() {
        // Use a month with 31 days to avoid invalid day-of-month.
        for (int day = 1; day <= 31; day++) {
            OADateTime dt = dtUtc(2023, Calendar.JANUARY, 1, 10, 11, 12, 13);
            dt.setDay(day);
            assertEquals(day, dt.getDay());
        }
    }

    @Test
    public void roundTrip_setTime_overloads() {
        OADateTime dt = dtUtc(2023, 0, 1, 0, 0, 0, 0);

        dt.setTime(11, 22);
        assertEquals(11, dt.get24Hour());
        assertEquals(22, dt.getMinute());
        assertEquals(0, dt.getSecond());

        dt.setTime(12, 34, 56);
        assertEquals(12, dt.get24Hour());
        assertEquals(34, dt.getMinute());
        assertEquals(56, dt.getSecond());

        dt.setTime(23, 59, 58, 123);
        assertEquals(23, dt.get24Hour());
        assertEquals(59, dt.getMinute());
        assertEquals(58, dt.getSecond());
        assertEquals(123, dt.getMilliSecond());
    }

    @Test
    public void clearTime_sets_hms_ms_toZero() {
        OADateTime dt = dtUtc(2023, 6, 4, 23, 59, 58, 321);
        dt.clearTime();
        assertEquals(0, dt.get24Hour());
        assertEquals(0, dt.getMinute());
        assertEquals(0, dt.getSecond());
        assertEquals(0, dt.getMilliSecond());
    }

    @Test
    public void clearDate_sets_date_to_1970_01_01_preserves_timeFields() {
        OADateTime dt = dtUtc(2023, 6, 4, 12, 34, 56, 789);
        dt.clearDate();
        assertEquals(1970, dt.getYear());
        assertEquals(Calendar.JANUARY, dt.getMonth());
        assertEquals(1, dt.getDay());
        assertEquals(12, dt.get24Hour());
        assertEquals(34, dt.getMinute());
        assertEquals(56, dt.getSecond());
        assertEquals(789, dt.getMilliSecond());
    }

    // --------------------------------------------------------------------------------------------
    // C) Time zone semantics: setTimeZone keeps wall-clock fields, changes instant;
    //    convertTo keeps instant, changes wall-clock fields.
    // --------------------------------------------------------------------------------------------

    @Test
    public void setTimeZone_keeps_wallClock_fields_changes_instant() {
        OADateTime dt = dtUtc(2023, Calendar.SEPTEMBER, 8, 14, 15, 47, 34);
        long t0 = dt.getTime();

        TimeZone chicago = TimeZone.getTimeZone("America/Chicago");
        dt.setTimeZone(chicago);

        // Wall-clock stays the same (per method comment in file).
        assertEquals(2023, dt.getYear());
        assertEquals(Calendar.SEPTEMBER, dt.getMonth());
        assertEquals(8, dt.getDay());
        assertEquals(14, dt.get24Hour());
        assertEquals(15, dt.getMinute());
        assertEquals(47, dt.getSecond());

        // But underlying instant should change (most of the time; this is deterministic for UTC vs Chicago).
        assertNotEquals(t0, dt.getTime());
    }

    @Test
    public void convertTo_keeps_instant_changes_wallClock_fields() {
        OADateTime dt = dtUtc(2023, Calendar.SEPTEMBER, 8, 14, 15, 47, 34);
        long t0 = dt.getTime();

        TimeZone chicago = TimeZone.getTimeZone("America/Chicago");
        OADateTime converted = dt.convertTo(chicago);

        // convertTo constructs a new instance and adjusts calendar fields by tz,
        // but should preserve the instant (time in millis).
        assertEquals(t0, converted.getTime());

        // In most cases UTC->Chicago shifts the wall-clock hour.
        // We avoid asserting a specific hour offset (DST differences) and just assert "likely different".
        assertTrue("Expected wall-clock hour to differ across zones for this instant",
                converted.get24Hour() != dt.get24Hour() || !converted.getTimeZone().equals(dt.getTimeZone()));
    }

    @Test
    public void convertToUTC_preserves_instant() {
        TimeZone chicago = TimeZone.getTimeZone("America/Chicago");
        // start with an instance whose timezone is Chicago
        OADateTime dt = dtUtc(2023, Calendar.SEPTEMBER, 8, 14, 15, 47, 34);
        dt.setTimeZone(chicago); // changes _time but keeps fields
        long t0 = dt.getTime();

        OADateTime utc = dt.convertToUTC();
        assertEquals(t0, utc.getTime());
    }

    // --------------------------------------------------------------------------------------------
    // D) Comparisons & equality (basic contracts + ordering)
    // --------------------------------------------------------------------------------------------

    @Test
    public void equals_hashCode_compareTo_contracts() {
        OADateTime a = dtUtc(2023, 0, 1, 0, 0, 0, 0);
        OADateTime b = new OADateTime(a);
        b.setTimeZoneUTC();

        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(0, a.compareTo(b));

        OADateTime c = dtUtc(2023, 0, 1, 0, 0, 0, 1);
        assertFalse(a.equals(c));
        assertTrue(a.compareTo(c) < 0);
        assertTrue(c.compareTo(a) > 0);

        assertTrue(a.before(c));
        assertTrue(a.isBefore(c));
        assertTrue(c.after(a));
        assertTrue(c.isAfter(a));
    }

    // --------------------------------------------------------------------------------------------
    // E) Formatting/parsing round-trips (RFC339/JSON/JDBC + toString(format))
    // --------------------------------------------------------------------------------------------

    @Test
    public void toString_withExplicitFormat_matches_calendar() {
        OADateTime dt = dtUtc(2023, Calendar.SEPTEMBER, 8, 14, 15, 47, 34);
        String s = dt.toString("yyyy-MM-dd HH:mm:ss.SSS");
        assertEquals("2023-09-08 14:15:47.034", s);
    }

    @Test
    public void valueOf_withFormat_roundTrip() {
        String fmt = "yyyy-MM-dd HH:mm:ss.SSS";
        String s = "2023-09-08 14:15:47.034";

        OADateTime dt = OADateTime.valueOf(s, fmt);
        assertNotNull(dt);
        dt.setTimeZoneUTC();
        assertEquals(s, dt.toString(fmt));
    }

    @Test
    public void json_roundTrip_basic() {
        OADateTime dt = dtUtc(2023, Calendar.SEPTEMBER, 8, 14, 15, 47, 0);
        String s = OADateTime.toJson(dt);
        OADateTime dt2 = OADateTime.valueOfJson(s);
        assertNotNull(dt2);
        // json helper formats without zone; compare by formatted string in same format
        dt2.setTimeZoneUTC();
        assertEquals(dt.toString(OADateTime.JsonFormat), dt2.toString(OADateTime.JsonFormat));
    }

    @Test
    public void jdbc_roundTrip_basic() {
        OADateTime dt = dtUtc(2023, Calendar.SEPTEMBER, 8, 14, 15, 47, 0);
        String s = OADateTime.toJdbc(dt);
        OADateTime dt2 = OADateTime.valueOfJdbc(s);
        assertNotNull(dt2);
        dt2.setTimeZoneUTC();
        assertEquals(dt.toString(OADateTime.JdbcFormat), dt2.toString(OADateTime.JdbcFormat));
    }

    // --------------------------------------------------------------------------------------------
    // F) Month/weekday helpers (exact, deterministic)
    // --------------------------------------------------------------------------------------------

    @Test
    public void monthWeekdayHelpers_match_calendar_computation_sweep() {
        // Sweep a few months/years; validate helper outputs match Calendar-derived expectations.
        int[] years = {2023, 2024};
        int[] months = {Calendar.JANUARY, Calendar.FEBRUARY, Calendar.MARCH, Calendar.MAY, Calendar.OCTOBER};

        for (int y : years) {
            for (int mon : months) {
                // Choose a safe mid-month day.
                OADateTime dt = dtUtc(y, mon, 15, 12, 0, 0, 0);

                int daysInMonth = dt.getDaysInMonth();
                assertTrue(daysInMonth >= 28 && daysInMonth <= 31);

                // isLastDayOfMonth for last day and day before (if possible)
                OADateTime last = new OADateTime(dt);
                last.setDay(daysInMonth);
                last.setTimeZoneUTC();
                assertTrue(last.isLastDayOfMonth());

                if (daysInMonth > 1) {
                    OADateTime notLast = new OADateTime(dt);
                    notLast.setDay(daysInMonth - 1);
                    notLast.setTimeZoneUTC();
                    assertFalse(notLast.isLastDayOfMonth());
                }

                // Validate first/last weekday day-of-month for all weekdays.
                for (int weekday = Calendar.SUNDAY; weekday <= Calendar.SATURDAY; weekday++) {
                    int expectedFirst = expectedFirstWeekdayOfMonthUtc(y, mon, weekday);
                    int expectedLast = expectedLastWeekdayOfMonthUtc(y, mon, weekday);

                    assertEquals(expectedFirst, dt.getFirstWeekDayOfMonth(weekday));
                    assertEquals(expectedLast, dt.getLastWeekDayOfMonth(weekday));

                    // Validate isFirstWeekDayOfMonth / isLastWeekDayOfMonth at those computed days.
                    OADateTime dFirst = new OADateTime(dt);
                    dFirst.setDay(expectedFirst);
                    dFirst.setTimeZoneUTC();
                    assertTrue(dFirst.isFirstWeekDayOfMonth(weekday));

                    OADateTime dLast = new OADateTime(dt);
                    dLast.setDay(expectedLast);
                    dLast.setTimeZoneUTC();
                    assertTrue(dLast.isLastWeekDayOfMonth(weekday));
                }
            }
        }
    }

    private static int expectedFirstWeekdayOfMonthUtc(int year, int mon0, int weekday) {
        GregorianCalendar c = calUtc(year, mon0, 1, 0, 0, 0, 0);
        for (int i = 0; i < 7; i++) {
            if (c.get(Calendar.DAY_OF_WEEK) == weekday) return 1 + i;
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return -1;
    }

    private static int expectedLastWeekdayOfMonthUtc(int year, int mon0, int weekday) {
        GregorianCalendar c = calUtc(year, mon0, 1, 0, 0, 0, 0);
        int days = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        c.set(Calendar.DAY_OF_MONTH, days);
        for (int i = 0; i < 7; i++) {
            if (c.get(Calendar.DAY_OF_WEEK) == weekday) return days - i;
            c.add(Calendar.DAY_OF_MONTH, -1);
        }
        return -1;
    }

    // --------------------------------------------------------------------------------------------
    // G) ignoreTimeZone flag + Java serialization round-trip
    // --------------------------------------------------------------------------------------------

    @Test
    public void serialization_roundTrip_preserves_time_and_flags() throws Exception {
        OADateTime dt = dtUtc(2023, Calendar.SEPTEMBER, 8, 14, 15, 47, 34);
        dt.setIgnoreTimeZone(true);

        byte[] data = serialize(dt);
        Object obj = deserialize(data);

        assertTrue(obj instanceof OADateTime);
        OADateTime dt2 = (OADateTime) obj;

        // Implementation uses custom writeObject/readObject; when ignoreTimeZone=true,
        // it writes fields. Round-trip should preserve visible fields.
        dt2.setTimeZoneUTC();

        assertEquals(dt.getYear(), dt2.getYear());
        assertEquals(dt.getMonth(), dt2.getMonth());
        assertEquals(dt.getDay(), dt2.getDay());
        assertEquals(dt.get24Hour(), dt2.get24Hour());
        assertEquals(dt.getMinute(), dt2.getMinute());
        assertEquals(dt.getSecond(), dt2.getSecond());
        assertEquals(dt.getMilliSecond(), dt2.getMilliSecond());

        assertTrue(dt2.getIgnoreTimeZone());
    }

    private static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(obj);
        out.flush();
        return bos.toByteArray();
    }

    private static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
        return in.readObject();
    }

    // --------------------------------------------------------------------------------------------
    // H) Stress: formatter pool concurrency (toString(format) under load)
    // --------------------------------------------------------------------------------------------

    @Test
    public void stress_toString_concurrent_noExceptions_and_parseable() throws Exception {
        final int threads = 16;
        final int itersPerThread = 200;
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);
        final List<Throwable> failures = Collections.synchronizedList(new ArrayList<Throwable>());

        final String fmt = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        final TimeZone utc = TimeZone.getTimeZone("UTC");

        for (int i = 0; i < threads; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    start.await();
                    for (int j = 0; j < itersPerThread; j++) {
                        // Create a deterministic but varied timestamp
                        int ms = (id * 17 + j) % 1000;
                        OADateTime dt = dtUtc(2023, Calendar.SEPTEMBER, 8, (id + j) % 24, j % 60, (j * 7) % 60, ms);
                        dt.setTimeZone(utc);

                        String s = dt.toString(fmt);
                        // Parse back using valueOf(fmt). Note: RFC339 uses literal 'Z'.
                        OADateTime dt2 = OADateTime.valueOf(s, fmt);
                        assertNotNull(dt2);
                    }
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    done.countDown();
                }
            }, "OADateTimeStress-" + i).start();
        }

        start.countDown();
        assertTrue("Timed out waiting for workers", done.await(15, TimeUnit.SECONDS));

        if (!failures.isEmpty()) {
            Throwable first = failures.get(0);
            first.printStackTrace();
            fail("Concurrency stress had failures, first=" + first);
        }
    }

    // --------------------------------------------------------------------------------------------
    // I) Reflection-based smoke tests (helps cover “everything else” without writing 500 tests)
    //    This invokes many public no-arg getters and a few safe static methods.
    // --------------------------------------------------------------------------------------------

    @Test
    public void smoke_publicNoArgMethods_doNotThrow() throws Exception {
        OADateTime dt = dtUtc(2023, Calendar.SEPTEMBER, 8, 14, 15, 47, 34);

        // instance: invoke public no-arg methods (excluding main/test entrypoints)
        for (java.lang.reflect.Method m : OADateTime.class.getMethods()) {
            if (!java.lang.reflect.Modifier.isPublic(m.getModifiers())) continue;
            if (m.getDeclaringClass() == Object.class) continue;
            if (m.getParameterTypes().length != 0) continue;

            String name = m.getName();
            if (name.equals("main") || name.equals("main2")) continue;

            // Skip methods that can be too environment-specific or heavy if any appear.
            // (none added here by default)

            assertNonThrow(() -> {
                try {
                    m.invoke(dt);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // static no-arg methods: now/today/time style helpers should not throw
        assertNotNull(OADateTime.now());
        assertNotNull(OADateTime.today());
        assertNotNull(OADateTime.todayUTC());
        assertNotNull(OADateTime.time());
        assertNotNull(OADateTime.timeUTC());
        assertNotNull(OADateTime.getDefaultTimeZone());
        assertNotNull(OADateTime.getGlobalOutputFormat());
    }

    @Test
    public void smoke_static_utilities_knownValues() {
        assertTrue(OADateTime.isLeapYear(2000));
        assertFalse(OADateTime.isLeapYear(1900));
        assertTrue(OADateTime.isLeapYear(2024));
        assertFalse(OADateTime.isLeapYear(2023));

        // Days in month sanity
        assertEquals(29, OADateTime.getDaysInMonth(2024, Calendar.FEBRUARY));
        assertEquals(28, OADateTime.getDaysInMonth(2023, Calendar.FEBRUARY));
    }

    // --------------------------------------------------------------------------------------------
    // J) Invariant-style arithmetic checks (add/subtract inverse properties)
    // --------------------------------------------------------------------------------------------

    @Test
    public void arithmetic_inverse_properties_sweep() {
        OADateTime base = dtUtc(2023, Calendar.SEPTEMBER, 8, 14, 15, 47, 34);

        int[] deltas = { -400, -31, -7, -1, 0, 1, 7, 31, 400 };

        for (int d : deltas) {
            assertSameInstant(base, base.addDays(d).subtractDays(d));
            assertSameInstant(base, base.addWeeks(d).subtractWeeks(d));
            assertSameInstant(base, base.addHours(d).subtractHours(d));
            assertSameInstant(base, base.addMinutes(d).subtractMinutes(d));
            assertSameInstant(base, base.addSeconds(d).subtractSeconds(d));
        }

        int[] mdeltas = { -24, -12, -1, 0, 1, 12, 24 };
        for (int m : mdeltas) {
            assertSameInstant(base, base.addMonths(m).subtractMonths(m));
            assertSameInstant(base, base.addYears(m).subtractYears(m));
        }

        int[] msDeltas = { -123456, -1000, -1, 0, 1, 1000, 123456 };
        for (int ms : msDeltas) {
            assertSameInstant(base, base.addMilliSeconds(ms).subtractMilliSeconds(ms));
        }
    }
}
