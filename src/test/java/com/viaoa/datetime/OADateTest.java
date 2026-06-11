
package com.viaoa.datetime;

/**
Strategy:
 Coverage strategy: the proposed test class exercises every public OADate constructor, date-only normalization through inherited mutators/
 arithmetic, parsing/formatting, inherited comparison/interval behavior, DST dates, serialization, and static-state cleanup. It avoids
 OATime entirely.
*/

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.viaoa.datetime.OADateTime.DateTimeType;

class OADateTest {
	private static final ZoneId UTC = ZoneOffset.UTC;
	private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
	private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");

	private TimeZone originalJvmTimeZone;
	private Locale originalJvmLocale;
	private ZoneId originalDefaultZoneId;
	private String originalDateOutputFormat;
	private String originalDateTimeOutputFormat;

	@BeforeEach
	void beforeEach() {
		originalJvmTimeZone = TimeZone.getDefault();
		originalJvmLocale = Locale.getDefault();
		originalDefaultZoneId = OADateTime.getDefaultZoneId();
		originalDateOutputFormat = OADate.getGlobalOutputFormat();
		originalDateTimeOutputFormat = OADateTime.getGlobalOutputFormat();

		TimeZone.setDefault(TimeZone.getTimeZone(UTC));
		Locale.setDefault(Locale.US);
		OADateTime.setLocale(Locale.US);
		OADate.setLocale(Locale.US);
		OADateTime.setDefaultZoneId(NEW_YORK);
		OADate.setGlobalOutputFormat("MM/dd/yyyy");
		OADateTime.setGlobalOutputFormat("MM/dd/yyyy HH:mm:ss.SSS");
	}

	@AfterEach
	void afterEach() {
		OADate.setGlobalOutputFormat(originalDateOutputFormat);
		OADateTime.setGlobalOutputFormat(originalDateTimeOutputFormat);
		OADateTime.setDefaultZoneId(originalDefaultZoneId);
		OADate.setLocale(originalJvmLocale);
		OADateTime.setLocale(originalJvmLocale);
		Locale.setDefault(originalJvmLocale);
		TimeZone.setDefault(originalJvmTimeZone);
	}

	@Test
	void constructorsCreateFloatingDateOnlyValues() {
		LocalDate todayBefore = LocalDate.now();
		OADate now = new OADate();
		LocalDate todayAfter = LocalDate.now();
		assertIsOADate(now);
		assertFloating(now);
		assertTrue(now.getLocalDate().equals(todayBefore) || now.getLocalDate().equals(todayAfter));
		assertTimeIsMidnight(now);

		Instant instant = Instant.parse("2026-06-09T03:30:00Z");
		LocalDate expectedFromInstant = instant.atZone(NEW_YORK).toLocalDate();
		assertDateOnly(new OADate(instant.toEpochMilli()), expectedFromInstant);
		assertDateOnly(new OADate(new Date(instant.toEpochMilli())), expectedFromInstant);

		OADate nullDate = new OADate((Date) null);
		assertIsOADate(nullDate);
		assertFloating(nullDate);
		assertTimeIsMidnight(nullDate);

		assertDateOnly(new OADate(2026, 1, 2), LocalDate.of(2026, 1, 2));

		OADateTime source = new OADateTime(NEW_YORK, 2026, 6, 9, 23, 45, 12, 987);
		assertDateOnly(new OADate(source), LocalDate.of(2026, 6, 9));

		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone(CHICAGO), Locale.US);
		cal.clear();
		cal.set(2026, Calendar.DECEMBER, 31, 18, 20, 30);
		assertDateOnly(new OADate(cal), LocalDate.of(2026, 12, 31));

		assertDateOnly(new OADate(LocalDate.of(2024, 2, 29)), LocalDate.of(2024, 2, 29));
		assertDateOnly(new OADate("06/09/2026"), LocalDate.of(2026, 6, 9));
		assertDateOnly(new OADate("2026**06**09", "yyyy**MM**dd"), LocalDate.of(2026, 6, 9));
	}

	@Test
	void invalidAndNullInputsFollowCurrentBehavior() {
		assertThrows(IllegalArgumentException.class, () -> new OADate("not a date"));
		assertNull(OADate.valueOf(null));
		assertNull(OADate.valueOf("not a date"));
		assertNull(OADate.dateValue("not a date"));

		assertThrows(NullPointerException.class, () -> new OADate((OADateTime) null));
		assertThrows(NullPointerException.class, () -> new OADate((Calendar) null));
		assertThrows(NullPointerException.class, () -> new OADate((LocalDate) null));
	}

	@Test
	void monthValuesUseJavaTimeOneBasedSemantics() {
		assertDateOnly(new OADate(2026, 1, 15), LocalDate.of(2026, 1, 15));
		assertEquals(1, new OADate(2026, 1, 15).getMonthValue());
		assertEquals(Month.JANUARY, new OADate(2026, 1, 15).getMonth());

		assertDateOnly(new OADate(2026, 12, 15), LocalDate.of(2026, 12, 15));
		assertEquals(12, new OADate(2026, 12, 15).getMonthValue());
		assertEquals(Month.DECEMBER, new OADate(2026, 12, 15).getMonth());

		assertThrows(RuntimeException.class, () -> new OADate(2026, 0, 15));
		assertThrows(RuntimeException.class, () -> new OADate(2026, 13, 15));
	}

	@Test
	void inheritedWithDateAndTimeMethodsReturnOADateAndNormalizeTime() {
		OADate base = new OADate(2026, 6, 9);

		assertDateOnly(base.withDateTime(2027, 8, 10, 11, 12, 13, 14), LocalDate.of(2027, 8, 10));
		assertDateOnly(base.withDate(2027, 8, 10), LocalDate.of(2027, 8, 10));
		assertDateOnly(base.withYear(2027), LocalDate.of(2027, 6, 9));
		assertDateOnly(base.withMonth(Month.AUGUST), LocalDate.of(2026, 8, 9));
		assertDateOnly(base.withMonthValue(8), LocalDate.of(2026, 8, 9));
		assertDateOnly(base.withDayOfMonth(10), LocalDate.of(2026, 6, 10));
		assertDateOnly(base.withoutDate(), LocalDate.of(1970, 1, 1));

		assertDateOnly(base.withTime(23, 59, 58, 999), LocalDate.of(2026, 6, 9));
		assertDateOnly(base.withTime(23, 59, 58), LocalDate.of(2026, 6, 9));
		assertDateOnly(base.withTime(23, 59), LocalDate.of(2026, 6, 9));
		assertDateOnly(base.withHours(23), LocalDate.of(2026, 6, 9));
		assertDateOnly(base.withMinutes(59), LocalDate.of(2026, 6, 9));
		assertDateOnly(base.withSeconds(58), LocalDate.of(2026, 6, 9));
		assertDateOnly(base.withMilliSeconds(999), LocalDate.of(2026, 6, 9));
		assertDateOnly(base.withoutTime(), LocalDate.of(2026, 6, 9));
		assertDateOnly(base.withoutSecondAndMilliSecond(), LocalDate.of(2026, 6, 9));
	}

	@Test
	void inheritedArithmeticReturnsOADateAndUsesDateOnlyResult() {
		OADate base = new OADate(2026, 6, 9);

		assertDateOnly(base.plusYears(1), LocalDate.of(2027, 6, 9));
		assertDateOnly(base.subtractYears(1), LocalDate.of(2025, 6, 9));
		assertDateOnly(base.plusMonths(2), LocalDate.of(2026, 8, 9));
		assertDateOnly(base.minusMonths(2), LocalDate.of(2026, 4, 9));
		assertDateOnly(base.plusDays(3), LocalDate.of(2026, 6, 12));
		assertDateOnly(base.minusDays(3), LocalDate.of(2026, 6, 6));
		assertDateOnly(base.plusDay(), LocalDate.of(2026, 6, 10));
		assertDateOnly(base.minusDay(), LocalDate.of(2026, 6, 8));
		assertDateOnly(base.addWeeks(2), LocalDate.of(2026, 6, 23));
		assertDateOnly(base.minusWeeks(2), LocalDate.of(2026, 5, 26));

		assertDateOnly(base.plusHours(23), LocalDate.of(2026, 6, 9));
		assertDateOnly(base.plusHours(24), LocalDate.of(2026, 6, 10));
		assertDateOnly(base.minusHours(1), LocalDate.of(2026, 6, 8));
		assertDateOnly(base.plusMinutes(1_439), LocalDate.of(2026, 6, 9));
		assertDateOnly(base.plusMinutes(1_440), LocalDate.of(2026, 6, 10));
		assertDateOnly(base.minusMinutes(1), LocalDate.of(2026, 6, 8));
		assertDateOnly(base.plusSeconds(86_399), LocalDate.of(2026, 6, 9));
		assertDateOnly(base.plusSeconds(86_400), LocalDate.of(2026, 6, 10));
		assertDateOnly(base.minusSeconds(1), LocalDate.of(2026, 6, 8));
		assertDateOnly(base.plusMilliSeconds(86_399_999), LocalDate.of(2026, 6, 9));
		assertDateOnly(base.plusMilliSeconds(86_400_000), LocalDate.of(2026, 6, 10));
		assertDateOnly(base.minusMilliSeconds(1), LocalDate.of(2026, 6, 8));

		assertDateOnly(new OADate(2026, 1, 31).plusMonths(1), LocalDate.of(2026, 2, 28));
		assertDateOnly(new OADate(2024, 2, 29).plusYears(1), LocalDate.of(2025, 2, 28));
	}

	@Test
	void zoneConversionsReturnOADateAndNormalizeDateOnlySemantics() {
		OADate date = new OADate(2026, 6, 9);

		OADateTime sameInstantChicago = date.withZoneIdSameInstant(CHICAGO);
		LocalDate expectedChicagoDate = Instant.ofEpochMilli(date.getTime()).atZone(CHICAGO).toLocalDate();
		assertDateOnly(sameInstantChicago, expectedChicagoDate);

		OADateTime sameWallChicago = date.withZoneIdSameWallTime(CHICAGO);
		assertDateOnly(sameWallChicago, LocalDate.of(2026, 6, 9));
		assertEquals(CHICAGO, sameWallChicago.zoneId);

		OADateTime utcSameInstant = date.withTimeZoneUTCSameInstant();
		LocalDate expectedUtcDate = Instant.ofEpochMilli(date.getTime()).atZone(UTC).toLocalDate();
		assertDateOnly(utcSameInstant, expectedUtcDate);

		OADateTime utcSameWall = date.withTimeZoneUTCSameWallTime();
		assertDateOnly(utcSameWall, LocalDate.of(2026, 6, 9));
		assertEquals(UTC, utcSameWall.zoneId);

		withDefaultZone(CHICAGO, () -> {
			assertDateOnly(date.withZoneIdSameInstant(null), expectedChicagoDate);
			OADateTime sameWallDefault = date.withZoneIdSameWallTime(null);
			assertDateOnly(sameWallDefault, LocalDate.of(2026, 6, 9));
			assertEquals(CHICAGO, sameWallDefault.zoneId);
		});
	}

	@Test
	void parsingApisNormalizeToOADateAndRejectInvalidInput() {
		assertDateOnly(OADate.valueOf("06/09/2026"), LocalDate.of(2026, 6, 9));
		assertDateOnly(OADate.valueOf("2026-06-09", "yyyy-MM-dd"), LocalDate.of(2026, 6, 9));
		assertDateOnly(OADate.valueOf("2026-06-09", "yyyy-MM-dd", false), LocalDate.of(2026, 6, 9));
		assertDateOnly(OADate.valueOf("06/09/2026", "yyyy-MM-dd", true), LocalDate.of(2026, 6, 9));
		assertDateOnly(OADate.dateValue("06/09/2026"), LocalDate.of(2026, 6, 9));
		assertDateOnly(OADate.dateValue("2026-06-09", "yyyy-MM-dd"), LocalDate.of(2026, 6, 9));

		assertDateOnly(OADate.valueOf("2026-06-09"), LocalDate.of(2026, 6, 9));
		assertDateOnly(OADate.valueOf("06/09/2026"), LocalDate.of(2026, 6, 9));
		assertDateOnly(OADate.valueOf("06/09/2026 23:45:12.987", "MM/dd/yyyy HH:mm:ss.SSS", false), LocalDate.of(2026, 6, 9));

		assertNull(OADate.valueOf("02/30/2026", "MM/dd/yyyy", false));
		assertNull(OADate.valueOf("2026-06-09 trailing", "yyyy-MM-dd", false));

		String compactFormat = "yyyyMMdd";
		try {
			OADate.addGlobalParseFormat(compactFormat);
			assertDateOnly(OADate.valueOf("20260609"), LocalDate.of(2026, 6, 9));
		} finally {
			OADate.removeGlobalParseFormat(compactFormat);
		}
	}

	@Test
	void formattingUsesDateOnlyFormatsAndNormalization() {
		OADate date = new OADate(2026, 6, 9);

		OADate.setGlobalOutputFormat("yyyy-MM-dd");
		assertEquals("2026-06-09", date.toString());
		assertEquals("06/09/2026", date.toString("MM/dd/yyyy"));

		date.setFormat("yyyyMMdd");
		assertEquals("20260609", date.toString());

		assertEquals("2026-06-09", date.toString("uuuu-MM-dd"));
		assertEquals("2026-06-09 00:00:00.000", date.toString("yyyy-MM-dd HH:mm:ss.SSS"));

		OADate.setGlobalOutputFormat(null);
		assertEquals("2026-Jun-09", date.withDate(2026, 6, 9).toString("yyyy-MMM-dd"));

		OADate.setGlobalOutputFormat("");
		OADate fallback = new OADate(2026, 6, 9);
		assertEquals("2026-Jun-09", fallback.toString());
	}

	@Test
	void equalityAndComparisonUseNormalizedMidnightTime() {
		OADate d1 = new OADate(2026, 6, 9);
		OADate d2 = new OADate(LocalDate.of(2026, 6, 9));
		OADate before = new OADate(2026, 6, 8);
		OADate after = new OADate(2026, 6, 10);

		assertEquals(d1, d2);
		assertEquals(d1.hashCode(), d2.hashCode());
		assertNotEquals(d1, after);
		assertTrue(d1.compareTo(before) > 0);
		assertTrue(d1.compareTo(after) < 0);
		assertTrue(before.before(d1));
		assertTrue(before.isBefore(d1));
		assertTrue(after.after(d1));
		assertTrue(after.isAfter(d1));

		assertTrue(d1.betweenOrEqual(before, after));
		assertTrue(d1.isBetweenOrEqual(before, after));
		assertTrue(d1.betweenNotEqual(before, after));
		assertTrue(d1.isBetweenNotEqual(before, after));
		assertTrue(before.betweenOrEqual(before, after));
		assertFalse(before.betweenNotEqual(before, after));
	}

	@Test
	void inheritedIntervalMethodsUseNormalizedDatesAndTimelineDurations() {
		OADate start = new OADate(2024, 2, 29);
		OADate end = new OADate(2025, 2, 28);

		assertEquals(Period.between(start.getLocalDate(), end.getLocalDate()), start.betweenPeriod(end));
		assertEquals(Duration.between(start.getInstant(), end.getInstant()), start.betweenDuration(end));
		assertEquals(0, start.betweenYears(end));
		assertEquals(11, start.betweenMonths(end));
		assertEquals(365, start.betweenDays(end));
		assertEquals(Duration.between(start.getInstant(), end.getInstant()).toHours(), start.betweenHours(end));
		assertEquals(Duration.between(start.getInstant(), end.getInstant()).toMinutes(), start.betweenMinutes(end));
		assertEquals(Duration.between(start.getInstant(), end.getInstant()).getSeconds(), start.betweenSeconds(end));
		assertEquals(Duration.between(start.getInstant(), end.getInstant()).toMillis(), start.betweenMilliSeconds(end));
	}

	@Test
	void dstDatesRemainDateOnlyWhileTimelineDurationsReflectDst() {
		withDefaultZone(NEW_YORK, () -> {
			OADate springStart = new OADate(2026, 3, 8);
			OADate springEnd = new OADate(2026, 3, 9);
			assertDateOnly(springStart, LocalDate.of(2026, 3, 8));
			assertDateOnly(springStart.plusDays(1), LocalDate.of(2026, 3, 9));
			assertEquals(1, springStart.betweenDays(springEnd));
			assertEquals(23, springStart.betweenHours(springEnd));
			assertEquals(Duration.ofHours(23), springStart.betweenDuration(springEnd));

			OADate fallStart = new OADate(2026, 11, 1);
			OADate fallEnd = new OADate(2026, 11, 2);
			assertDateOnly(fallStart, LocalDate.of(2026, 11, 1));
			assertDateOnly(fallStart.plusDays(1), LocalDate.of(2026, 11, 2));
			assertEquals(1, fallStart.betweenDays(fallEnd));
			assertEquals(25, fallStart.betweenHours(fallEnd));
			assertEquals(Duration.ofHours(25), fallStart.betweenDuration(fallEnd));
		});
	}

	@Test
	void serializationRoundTripsDateOnlyValues() throws Exception {
		assertSerializedDateOnly(new OADate(2026, 6, 9), LocalDate.of(2026, 6, 9));
		assertSerializedDateOnly(new OADate(2024, 2, 29), LocalDate.of(2024, 2, 29));

		withDefaultZone(NEW_YORK, () -> {
			try {
				assertSerializedDateOnly(new OADate(2026, 3, 8), LocalDate.of(2026, 3, 8));
				assertSerializedDateOnly(new OADate(2026, 11, 1), LocalDate.of(2026, 11, 1));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static void assertSerializedDateOnly(OADate date, LocalDate expected) throws Exception {
		Object copy = roundTrip(date);
		assertTrue(copy instanceof OADate);
		assertDateOnly((OADateTime) copy, expected);
	}

	private static void assertDateOnly(OADateTime dt, LocalDate expected) {
		assertIsOADate(dt);
		assertFloating(dt);
		assertTimeIsMidnight(dt);
		assertEquals(expected, dt.getLocalDate());
		assertEquals(expected.getYear(), dt.getYear());
		assertEquals(expected.getMonthValue(), dt.getMonthValue());
		assertEquals(expected.getDayOfMonth(), dt.getDayOfMonth());
	}

	private static void assertIsOADate(OADateTime dt) {
		assertNotNull(dt);
		assertTrue(dt instanceof OADate, "Expected OADate but got " + dt.getClass().getName());
	}

	private static void assertFloating(OADateTime dt) {
		assertEquals(DateTimeType.Floating, dt.getType());
		assertNotNull(dt.zoneId);
	}

	private static void assertTimeIsMidnight(OADateTime dt) {
		assertEquals(0, dt.getHour());
		assertEquals(0, dt.getMinute());
		assertEquals(0, dt.getSecond());
		assertEquals(0, dt.getMilliSecond());
	}

	private static Object roundTrip(Object obj) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
			out.writeObject(obj);
		}

		try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			return in.readObject();
		}
	}

	private static void withDefaultZone(ZoneId zone, Runnable test) {
		ZoneId oldZone = OADateTime.getDefaultZoneId();
		try {
			OADateTime.setDefaultZoneId(zone);
			test.run();
		} finally {
			OADateTime.setDefaultZoneId(oldZone);
		}
	}
}
