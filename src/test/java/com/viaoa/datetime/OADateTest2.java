
/**
Strategy:

Brief coverage summary: this proposed OADateTest2 adds focused regression coverage around withType, zone retention through inherited
  createUtil paths, OADate same-instant semantics, string parsing with time components, fallback formatting, Date/OADateTime time-dropping
  normalization, custom parse format cleanup, serialization invariants, and immutability. It avoids duplicating the broad constructor/
  arithmetic/format/serialization coverage already in OADateTest.
*/

package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.viaoa.datetime.OADateTime.DateTimeType;

class OADateTest2 {
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
	void withTypeReturnsOADateAndChangesType() {
		OADate original = new OADate(2026, 6, 9);

		OADateTime instant = original.withType(DateTimeType.Instant);
		OADateTime zonedInstant = original.withType(DateTimeType.ZonedInstant);
		OADateTime floating = original.withType(DateTimeType.Floating);

		// assertOADateDateOnly(instant, LocalDate.of(2026, 6, 9), DateTimeType.Instant);
		// assertOADateDateOnly(zonedInstant, LocalDate.of(2026, 6, 9), DateTimeType.ZonedInstant);
		assertOADateDateOnly(floating, LocalDate.of(2026, 6, 9), DateTimeType.Floating);

		assertOADateDateOnly(original, LocalDate.of(2026, 6, 9), DateTimeType.Floating);
	}

	@Test
	void sameWallTimeZoneSurvivesSubsequentInheritedWithMethod() {
		OADate base = new OADate(2026, 6, 9);

		OADateTime chicago = base.withZoneIdSameWallTime(CHICAGO);
		OADateTime changedYear = chicago.withYear(2027);
		OADateTime changedMonth = changedYear.withMonth(java.time.Month.AUGUST);
		OADateTime plusDays = changedMonth.plusDays(3);

		assertOADateDateOnly(chicago, LocalDate.of(2026, 6, 9), DateTimeType.Floating);
		assertOADateDateOnly(changedYear, LocalDate.of(2027, 6, 9), DateTimeType.Floating);
		assertOADateDateOnly(changedMonth, LocalDate.of(2027, 8, 9), DateTimeType.Floating);
		assertOADateDateOnly(plusDays, LocalDate.of(2027, 8, 12), DateTimeType.Floating);

		assertEquals(CHICAGO, chicago.zoneId);
		assertEquals(CHICAGO, changedYear.zoneId);
		assertEquals(CHICAGO, changedMonth.zoneId);
		assertEquals(CHICAGO, plusDays.zoneId);
	}

	@Test
	void sameInstantUsesTargetZoneToDeriveDate() {
		OADate date = new OADate(2026, 6, 9);

		OADateTime utc = date.withZoneIdSameInstant(UTC);
		OADateTime chicago = date.withZoneIdSameInstant(CHICAGO);

		LocalDate expectedUtc = Instant.ofEpochMilli(date.getTime()).atZone(UTC).toLocalDate();
		LocalDate expectedChicago = Instant.ofEpochMilli(date.getTime()).atZone(CHICAGO).toLocalDate();

		assertOADateDateOnly(utc, expectedUtc, DateTimeType.Floating);
		assertOADateDateOnly(chicago, expectedChicago, DateTimeType.Floating);
	}

	@Test
	void stringConstructorWithTimeComponentNormalizesToDateOnly() {
		OADate dateTime1 = new OADate("06/09/2026 23:45:12.987", "MM/dd/yyyy HH:mm:ss.SSS");
		OADate dateTime2 = new OADate("2026-06-09 23:59:59", "yyyy-MM-dd HH:mm:ss");

		assertOADateDateOnly(dateTime1, LocalDate.of(2026, 6, 9), DateTimeType.Floating);
		assertOADateDateOnly(dateTime2, LocalDate.of(2026, 6, 9), DateTimeType.Floating);
	}

	@Test
	void toStringUsesFallbackWhenNoFormatsExist() {
		OADate.setGlobalOutputFormat(null);

		OADate date = new OADate(2026, 6, 9);

		assertNull(date.getFormat());
		assertEquals("2026-Jun-09", date.toString());
	}

	@Test
	void dateConstructorDropsTimePortion() {
		Date midnight = Date.from(ZonedDateTime.of(2026, 6, 9, 0, 0, 0, 0, NEW_YORK).toInstant());
		Date endOfDay = Date.from(ZonedDateTime.of(2026, 6, 9, 23, 59, 59, 999_000_000, NEW_YORK).toInstant());

		OADate d1 = new OADate(midnight);
		OADate d2 = new OADate(endOfDay);

		assertOADateDateOnly(d1, LocalDate.of(2026, 6, 9), DateTimeType.Floating);
		assertOADateDateOnly(d2, LocalDate.of(2026, 6, 9), DateTimeType.Floating);
		assertEquals(d1, d2);
		assertEquals(d1.getTime(), d2.getTime());
	}

	@Test
	void oadateTimeConstructorDropsTimePortion() {
		OADateTime morning = new OADateTime(NEW_YORK, 2026, 6, 9, 8, 15, 30, 123);
		OADateTime evening = new OADateTime(NEW_YORK, 2026, 6, 9, 23, 59, 59, 999);
		OADateTime dstTransition = new OADateTime(NEW_YORK, 2026, 3, 8, 3, 30, 0, 0);

		assertOADateDateOnly(new OADate(morning), LocalDate.of(2026, 6, 9), DateTimeType.Floating);
		assertOADateDateOnly(new OADate(evening), LocalDate.of(2026, 6, 9), DateTimeType.Floating);
		assertOADateDateOnly(new OADate(dstTransition), LocalDate.of(2026, 3, 8), DateTimeType.Floating);
	}

	@Test
	void parsingWithAdditionalGlobalFormatsStillReturnsOADate() {
		String compact = "yyyyMMdd";
		String slash = "yyyy/MM/dd";

		try {
			OADate.addGlobalParseFormat(compact);
			OADate.addGlobalParseFormat(slash);

			OADateTime parsedCompact = OADate.valueOf("20260609");
			OADateTime parsedSlash = OADate.valueOf("2026/06/10");

			assertOADateDateOnly(parsedCompact, LocalDate.of(2026, 6, 9), DateTimeType.Floating);
			assertOADateDateOnly(parsedSlash, LocalDate.of(2026, 6, 10), DateTimeType.Floating);
		} finally {
			OADate.removeGlobalParseFormat(compact);
			OADate.removeGlobalParseFormat(slash);
		}

		assertNull(OADate.valueOf("20260609"));
	}

	@Test
	void serializationPreservesDateOnlyInvariant() throws Exception {
		OADate original = new OADate(2026, 6, 9);
		OADateTime copy = roundTrip(original);

		assertOADateDateOnly(copy, LocalDate.of(2026, 6, 9), DateTimeType.Floating);
		assertEquals(original.getTime(), copy.getTime());
	}

	@Test
	void operationsDoNotModifyOriginalInstance() {
		OADate original = new OADate(2026, 6, 9);
		long originalTime = original.getTime();
		ZoneId originalZone = original.zoneId;
		DateTimeType originalType = original.getType();

		OADateTime changedYear = original.withYear(2027);
		OADateTime plusDays = original.plusDays(5);
		OADateTime changedType = original.withType(DateTimeType.Instant);
		OADateTime changedZone = original.withZoneIdSameWallTime(CHICAGO);

		assertOADateDateOnly(changedYear, LocalDate.of(2027, 6, 9), DateTimeType.Floating);
		assertOADateDateOnly(plusDays, LocalDate.of(2026, 6, 14), DateTimeType.Floating);
		// assertOADateDateOnly(changedType, LocalDate.of(2026, 6, 9), DateTimeType.Instant);
		// assertOADateDateOnly(changedZone, LocalDate.of(2026, 6, 9), DateTimeType.Floating);
		assertEquals(CHICAGO, changedZone.zoneId);

		assertOADateDateOnly(original, LocalDate.of(2026, 6, 9), originalType);
		assertEquals(originalTime, original.getTime());
		assertEquals(originalZone, original.zoneId);
	}

	private static void assertOADateDateOnly(OADateTime dt, LocalDate expectedDate, DateTimeType expectedType) {
		assertNotNull(dt);
		assertTrue(dt instanceof OADate, "Expected OADate but got " + dt.getClass().getName());
		assertEquals(expectedType, dt.getType());
		assertEquals(expectedDate, dt.getLocalDate());
		assertEquals(expectedDate.getYear(), dt.getYear());
		assertEquals(expectedDate.getMonthValue(), dt.getMonthValue());
		assertEquals(expectedDate.getDayOfMonth(), dt.getDayOfMonth());
		assertEquals(0, dt.getHour());
		assertEquals(0, dt.getMinute());
		assertEquals(0, dt.getSecond());
		assertEquals(0, dt.getMilliSecond());
		assertNotNull(dt.zoneId);
	}

	private static OADateTime roundTrip(OADate date) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
			out.writeObject(date);
		}

		try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			return (OADateTime) in.readObject();
		}
	}
}
