/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.datetime;


// NOTE: OA uses Calendar and it uses month 0-11 ... java.time.* uses month 1-12  qqqqqqqqqq 

/*
CODEX

- Class: OADateTime
  - Method: getInstant, getZonedDateTime, getLocalDateTime
  - Issue: Milliseconds are converted to nanoseconds using division instead of multiplication, and getInstant()
    reconstructs an instant from local fields instead of returning _time.
  - Why it is a problem: Milliseconds are truncated to zero for most values. During DST fall-back overlaps,
    reconstructing from local fields can choose the wrong offset and return an instant one hour off.
  - Classification: Fix Now
  - Suggested Fix: getInstant() should return Instant.ofEpochMilli(_time). getZonedDateTime() should use
    Instant.ofEpochMilli(_time).atZone(getTimeZone().toZoneId()). Nanoseconds should be getMilliSecond() *
    1_000_000.
  - Class: OADateTime
  - Method: getMinute, setMinute, getSecond, setSecond, clearSecondAndMilliSecond
  - Issue: These methods ignore timeZone and use deprecated Date methods in the JVM default timezone.
  - Why it is a problem: Instances with non-default timezones can report or mutate the wrong wall-clock minute/
    second, especially for zones with non-hour offsets.
  - Classification: Fix Now
  - Suggested Fix: Use _getCal() for all field access/mutation when timezone semantics matter, matching getYear,
    getMonth, getHour.
  - Class: OADateTime
  - Method: valueOfMain
  - Issue: Pooled SimpleDateFormat instances retain timezone state from previous formatting/parsing use.
  - Why it is a problem: Parsing a timezone-less string can depend on which pooled formatter was last used and what
    timezone it had. That is nondeterministic in server/distributed use.
  - Classification: Fix Now
  - Suggested Fix: Before every parse, set the formatter timezone explicitly to OA default/system default. Also
    reset other mutable formatter state needed for deterministic parsing.
  - Class: OADateTime
  - Method: valueOfMain
  - Issue: Uses SimpleDateFormat.parse(String) without verifying full input consumption.
  - Why it is a problem: Invalid values with trailing garbage can parse as valid dates, producing wrong query/
    filter/replication values.
  - Classification: Fix Now
  - Suggested Fix: Parse with ParsePosition and require pos.getIndex() == value.length().
  - Class: OADateTime
  - Method: OADateTime(ZonedDateTime)
  - Issue: The instant is copied but the ZonedDateTime zone is discarded.
  - Why it is a problem: OA has explicit timezone-aware semantics. A value created from ZonedDateTime loses the
    zone needed for later wall-clock field access and formatting.
  - Classification: Fix Now
  - Suggested Fix: Set _time from zdt.toInstant() and timeZone = TimeZone.getTimeZone(zdt.getZone()).
  - Class: OADateTime
  - Method: RFC339Format, RFC339FormatWms
  - Issue: The Z is quoted as a literal, not parsed/formatted as UTC offset.
  - Why it is a problem: A string ending in Z means UTC, but parsing with this format interprets fields in the
    formatter’s timezone and merely consumes a literal Z.
  - Classification: Fix Now
  - Suggested Fix: Use an ISO offset pattern such as X/XXX, or force UTC timezone when using the literal-Z format.
  - Class: OADate
  - Method: OADate(Calendar), OADate(OADateTime)
  - Issue: Constructors build the date instant in the JVM default timezone, then assign the source timezone
    afterward.
  - Why it is a problem: For non-default source timezones, the stored instant can represent the previous/next day
    when viewed in the assigned timezone.
  - Classification: Fix Now
  - Suggested Fix: Build the backing instant using a Calendar/GregorianCalendar already configured with the source
    timezone and desired Y/M/D at midnight.
  - Class: OADateTime
  - Method: writeObject, readObject
  - Issue: Serialized OADate/OATime with timezone writes local fields plus timezone id, but read reconstructs those
    fields using JVM default timezone before assigning the timezone.
  - Why it is a problem: Date-only and time-only values can shift when deserialized on a JVM in a different default
    timezone.
  - Classification: Fix Now
  - Suggested Fix: During read, resolve tzId first and construct calendar fields in that timezone.
  - Class: OADateTime, OADate, OATime
  - Method: equals, hashCode
  - Issue: equals for OADate and OATime uses semantic date-only/time-only comparison, but hashCode always uses raw
    _time.
  - Why it is a problem: Equal OADate or OATime values can have different hash codes, breaking HashMap, HashSet,
    caches, and graph identity structures.
  - Classification: Fix Now
  - Suggested Fix: Override hash-code semantics for date-only and time-only values, or make base hashCode mirror
    compareTo equality semantics.
  - Class: OATime
  - Method: OATime(Date), OATime(Time), OATime(long), clearDate
  - Issue: Time-only construction depends on JVM default timezone.
  - Why it is a problem: The class contract says OATime is not affected by timezone, but the same millis value can
    become a different wall-clock time on different machines.
  - Classification: Fix Now
  - Suggested Fix: Normalize time-only values using explicit local-time fields or a fixed timezone/epoch
    convention, not JVM default Date field extraction.
  - Class: OADate
  - Method: OADate(Date), OADate(long), clearTime
  - Issue: Date-only construction from an instant depends on JVM default timezone.
  - Why it is a problem: The class documentation says date-only values are consistent across JVM default timezones,
    but the same instant can become different calendar dates.
  - Classification: Fix Now
  - Suggested Fix: Define the intended conversion zone explicitly. For deterministic distributed behavior, avoid
    default timezone unless explicitly requested.
  - Class: OATimeZone
  - Method: getTimeZone, getOATimeZone
  - Issue: Lookup accepts timezone abbreviations and display names.
  - Why it is a problem: Abbreviations such as CST, IST, EST are ambiguous or fixed-offset aliases and can resolve
    differently than intended for DST-aware business zones.
  - Classification: Defer
  - Class: OATimeZone
  - Method: _getOATimeZones, getUtcTimeZone
  - Issue: UTC display/lookup is based on getRawOffset(), not offset at a specific instant.
  - Why it is a problem: DST-observing zones display and sort by standard offset, not current/effective offset. A
    user selecting by UTC-05 during daylight time may not get the intended zone.
  - Classification: Defer
  - Class: OATimeZone
  - Method: getOATimeZones
  - Issue: Refresh path never rebuilds once alTZ is non-null, even when msNextUpdate has expired.
  - Why it is a problem: Cached timezone display metadata is intended to refresh but does not. This matters if
    display names/offset displays are expected to track day changes or timezone database updates.
  - Classification: Defer

  COMPLETE



1. file/class/method
     src/main/java/com/viaoa/datetime/OADateTime.java — set12Hour(int hr)
  2. concrete bug
     set12Hour discards the existing AM/PM state and always writes the hour into the AM half of the day.
  3. runtime scenario
     For an instance representing 3:00 PM:

  dt.set12Hour(4);

  Expected wall-clock result is usually 4:00 PM, preserving PM. Current code does:

  int h = (hr % 12);
  setHour(h);

  So the result becomes 4:00 AM.

  4. why this violates OA/OG datetime semantics
     This can corrupt UI/form-driven time edits where hour and AM/PM are edited independently. OA time semantics must
     preserve the intended time-only/datetime wall-clock value unless the caller explicitly changes AM/PM.
  5. minimal fix direction
     Use the existing AM_PM state when converting 12-hour input to 24-hour time, or set Calendar.HOUR instead of
     HOUR_OF_DAY while preserving Calendar.AM_PM.
  6. suggested CODEX comment location
     Above OADateTime.set12Hour(int hr).


1. file/class/method
     src/main/java/com/viaoa/datetime/OADateTime.java — addDays(int amount)
  2. concrete bug
     addDays(0) returns this, while the method contract says it returns a new OADateTime object.
  3. runtime scenario

  OADate d1 = new OADate(2026, 4, 18);
  OADate d2 = (OADate) d1.addDays(0);
  d2.setDay(19);

  Because d2 == d1, mutating the result mutates the original. Other arithmetic methods generally return a new object,
  so this is an aliasing trap.

  4. why this violates OA/OG datetime semantics
     OA date/time wrappers are mutable in practice. Arithmetic methods that claim to return a new value must not
     sometimes return the original instance. This can corrupt scheduler/date-range/cache logic when the amount happens
     to be zero.

5.1. file/class/method
  src/main/java/com/viaoa/datetime/OADateTime.java — set12Hour(int hr)

  2. concrete bug
     set12Hour discards the current AM/PM state and always writes the hour as AM.
  3. runtime scenario
     For an instance representing 3:30 PM, calling:

  dt.set12Hour(4);

  sets the hour to 04:30 AM, not 04:30 PM.

  Execution path:

  int h = (hr % 12);
  setHour(h);

  4. why this violates OA/OG datetime semantics
     A 12-hour setter should preserve or explicitly coordinate with AM_PM. OA scheduling, UI binding, template/report
     formatting, and property editing can split hour and AM/PM controls. This silently changes the instant by 12
     hours.
  5. minimal fix direction
     Implement set12Hour using the current getAM_PM() value, or set Calendar.HOUR rather than HOUR_OF_DAY. For
     example, preserve PM by adding 12 when current AM_PM is PM and hr != 12.
  6. suggested CODEX comment location
     Above OADateTime.set12Hour(int hr).


 1. file/class/method
     src/main/java/com/viaoa/datetime/OADateTime.java — valueOfMain(...)
  2. concrete bug
     Parsing uses lenient SimpleDateFormat, so invalid dates/times can silently normalize to different valid values.
  3. runtime scenario
     Inputs like these can parse successfully instead of failing:

  OADate.valueOf("2026-02-31")
  OATime.valueOf("25:00")
  OADateTime.valueOf("2026-13-01 10:00")

  SimpleDateFormat can roll these into another date/time instead of rejecting them.

  4. why this violates OA/OG datetime semantics
     Datasource values, query criteria, serialized values, UI input, and filter/template values must not silently
     become a different date/time. This is a false-success parse.
  5. minimal fix direction
     Set sdf.setLenient(false) before parsing, and keep the existing CODEX full-consumption requirement as a separate
     check.
  6. suggested CODEX comment location
     Inside OADateTime.valueOfMain(...), immediately after sdf.applyPattern(format).

 1. file/class/method
     src/main/java/com/viaoa/datetime/OADateTime.java — addDays(int amount)
  2. concrete bug
     addDays(0) returns this, while the method contract says it returns a new date/time instance.
  3. runtime scenario
     Caller code can accidentally alias the original:

  OADate d1 = new OADate();
  OADate d2 = (OADate) d1.addDays(0);
  d2.setDay(1); // mutates d1 too

  Other add methods generally return a new object, so this zero-day path is inconsistent.

  4. why this violates OA/OG datetime semantics
     OA date/time wrappers are mutable. Returning the same object from a method documented as producing a new value
     can corrupt caller state, date-range calculations, scheduling logic, and cache keys.
  5. minimal fix direction
     For amount == 0, return a new instance of the same semantic type (OADate, OATime, or OADateTime) instead of this.
  6. suggested CODEX comment location
     At the top of OADateTime.addDays(int amount), above the if (amount == 0) branch.

  1. file/class/method
     src/main/java/com/viaoa/datetime/OADateTime.java — OADateTime(LocalDateTime ldt)
  2. concrete bug
     LocalDateTime conversion uses ZoneId.systemDefault() instead of OA’s configured defaultTimeZone.
  3. runtime scenario
     If OA sets:

  OADateTime.setDefaultTimeZone(TimeZone.getTimeZone("UTC"));

  on a JVM running in America/Chicago, then:

  new OADateTime(LocalDateTime.of(2026, 5, 18, 10, 0))

  interprets 10:00 in the JVM system zone, not OA’s configured default zone.

  4. why this violates OA/OG datetime semantics
     OA exposes a default timezone for date/time semantics. Bypassing it can create persisted/serialized/comparison
     values that drift between servers, clients, replication nodes, or tests with different JVM defaults.
  5. minimal fix direction
     Resolve LocalDateTime using OADateTime.getDefaultTimeZone().toZoneId() unless the contract explicitly says Java
     LocalDateTime always uses JVM default.
  6. suggested CODEX comment location
     Above OADateTime(LocalDateTime ldt).


  1. file/class/method
     src/main/java/com/viaoa/datetime/OADateTime.java — betweenYears(Object obj) / betweenMonths(Object obj)
  2. concrete bug
     Elapsed year/month calculations ignore lower-order fields. Adjacent dates crossing a year/month boundary can
     return 1 year/month even when only one day apart.
  3. runtime scenario
     new OADate(2025, 11, 31).betweenYears(new OADate(2026, 0, 1)) returns 1.
     new OADate(2026, 0, 31).betweenMonths(new OADate(2026, 1, 1)) returns 1.
  4. why this violates OA/OG datetime semantics
     If these methods are used for elapsed age, duration, scheduling, query windows, or report calculations, they
     silently overstate elapsed time.
  5. minimal fix direction
     Define the contract explicitly: field-boundary difference vs full elapsed units. If full elapsed units are
     intended, account for month/day/time before counting the final year/month.
  6. suggested CODEX comment location
     Above betweenYears(Object obj) and betweenMonths(Object obj).


1. file/class/method
     src/main/java/com/viaoa/datetime/OADateTime.java — field constructors and setters: OADateTime(int...),
     setDate(...), setYear(...), setMonth(...), setDay(...), setTime(...); also inherited by OADate(int...) /
     OATime(int...)
  2. concrete bug
     Field-based construction/mutation uses lenient Date / Calendar behavior, so invalid field combinations silently
     normalize to a different valid date/time.
  3. runtime scenario
     new OADate(2026, Calendar.FEBRUARY, 31) can become a March date.
     dt.setMonth(Calendar.FEBRUARY) on a March 31 value can roll into March instead of producing February semantics or
     failing.
     new OATime(25, 0, 0) can normalize to 01:00.
  4. why this violates OA/OG datetime semantics
     OA date/time values are used for persisted values, query criteria, schedules, UI edits, and replication. Silent
     rollover is false-success mutation: caller asked for one semantic value and got another.
  5. minimal fix direction
     Validate field ranges and actual date validity before committing _time, or use non-lenient calendar construction.
     Month/day edits need an explicit contract: reject invalid day/month combinations or clamp only if that is
     documented.
  6. suggested CODEX comment location
     Above the field-based constructors, setDate, setMonth, setDay, and setTime.

 1. file/class/method
     src/main/java/com/viaoa/datetime/OADateTime.java — compareTo(Object obj), indirectly after(Object) /
     isAfter(Object)
  2. concrete bug
     compareTo returns positive 2 when the object is not convertible, and after() treats any positive value as true.
  3. runtime scenario
     new OADateTime().after(new Object()) returns true because convert(obj, false) returns null, compareTo returns 2,
     and after checks > 0.
  4. why this violates OA/OG datetime semantics
     A non-comparable value should not silently mean “this date is after that value.” That can produce false-positive
     filters, comparisons, or business rules if an unexpected value type reaches date comparison.
  5. minimal fix direction
     Make non-convertible comparison fail visibly or return a comparison sentinel that before/after/between do not
     treat as ordered. Minimal local fix: after/isAfter should only treat known ordered positive comparison as true,
     not the non-comparable sentinel.
  6. suggested CODEX comment location
     Above compareTo(Object obj) and the after/isAfter helpers.
*/

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


/**
 * Core OA date/time class used to normalize, convert, format, parse, compare,
 * and calculate date/time values across the OA platform.
 * <p>
 * This class exists because Java date/time support spans several generations of
 * APIs. OADateTime provides one OA-level abstraction for working with legacy
 * and modern Java date/time types, including:
 * <ul>
 *   <li>{@link java.util.Date}</li>
 *   <li>{@link java.sql.Date}</li>
 *   <li>{@link java.sql.Time}</li>
 *   <li>{@link java.sql.Timestamp}</li>
 *   <li>{@link java.util.Calendar}</li>
 *   <li>{@link java.util.GregorianCalendar}</li>
 *   <li>{@link java.time.Instant}</li>
 *   <li>{@link java.time.LocalDate}</li>
 *   <li>{@link java.time.LocalTime}</li>
 *   <li>{@link java.time.LocalDateTime}</li>
 *   <li>{@link java.time.ZonedDateTime}</li>
 * </ul>
 *
 * <h3>Internal model</h3>
 * OADateTime stores its value as milliseconds from the epoch plus an optional
 * {@link java.util.TimeZone}. A {@link java.util.GregorianCalendar} is created
 * only as a temporary helper for field access, field mutation, formatting,
 * parsing, and arithmetic.
 * <p>
 * OA follows {@link java.util.Calendar} month numbering: January is {@code 0}
 * and December is {@code 11}. Java {@code java.time} APIs use month values
 * {@code 1} through {@code 12}; conversion methods must account for this.
 *
 * <h3>Timezone model</h3>
 * The stored millisecond value represents the underlying instant. When this
 * object has a timezone, calendar fields such as year, month, day, hour,
 * minute, and second are interpreted using that timezone. When no timezone is
 * assigned, the OA default timezone is used.
 *
 * <h3>Mutable and immutable APIs</h3>
 * OADateTime supports both legacy mutable methods and modern immutable-style
 * methods.
 * <p>
 * Methods named {@code setXxx(...)} mutate this instance.
 * <p>
 * Methods named {@code withXxx(...)} return a new instance with the requested
 * field changed.
 * <p>
 * Arithmetic methods such as {@code addDays(...)}, {@code addMonths(...)},
 * {@code subtractDays(...)}, and related methods also return new instances.
 *
 * <h3>Specialized subclasses</h3>
 * {@link OADate} provides date-only semantics.
 * {@link OATime} provides time-only semantics.
 *
 * @see OADate
 * @see OATime
 * @see java.time.Instant
 * @see java.time.LocalDateTime
 * @see java.time.ZonedDateTime
 */
public class OADateTime implements java.io.Serializable, Comparable {
	private static final long serialVersionUID = 1L;


	/**
	 * Time value stored as milliseconds since the epoch.
	 */
	protected long _time;
	
	/**
	 * Optional time zone associated with this date/time.
	 */
	protected TimeZone timeZone;
	
	/**
	 * Instance-specific output format used when converting to a string.
	 */
	protected String format;

	
	/**
	 * Default time zone used when no instance time zone is specified.
	 */
	protected static TimeZone defaultTimeZone;

	/**
	 * Locale used for date/time formatting and parsing.
	 */
	private static Locale locale;
	
	/**
	 * Long date/time format including milliseconds and AM/PM.
	 */
	public final static String FORMAT_long = "yyyy/MM/dd hh:mm:ss.S a";
	
	/**
	 * Long date/time format including milliseconds and time zone.
	 */
	public final static String FORMAT_xlong = "yyyy/MM/dd hh:mm:ss.S a z";
	
	
	// RFC-339 format
	// Note: the 'Z' is not a timezone, it means that the timezone should be set to UTC.
	// The calling code should call dt.setTimeZoneUTC()

	/**
	 * RFC-339 compliant date/time format without milliseconds.
	 */
	public final static String RFC339Format = "yyyy-MM-dd'T'HH:mm:ss'Z'"; // 2023-09-04T07:11:12:32-0400
	
	/**
	 * RFC-339 compliant date/time format with milliseconds.
	 */
	public final static String RFC339FormatWms = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"; // 2023-09-04T07:11:12:32.123-0400

	/**
	 * Global default output format used when converting date/time values to strings.
	 */
	protected static String staticOutputFormat;
	
	/**
	 * JSON date/time format without time zone.
	 */
	public final static String JsonFormat = "yyyy-MM-dd'T'HH:mm:ss";
	
	/**
	 * JSON date/time format including time zone.
	 */
	public final static String JsonFormatTZ = "yyyy-MM-dd'T'HH:mm:ssX";

	/**
	 * JDBC-compatible SQL date/time format.
	 */
	public final static String JdbcFormat = "yyyy-MM-dd HH:mm:ss"; // SQL

	// format used by browser: : "YYYY-MM-DD'T'HH:mm";
	// same as json format
	// public final static String HtmlInputDateTimeFormat = "yyyy-MM-dd'T'hh:mm"; //
	// java format to use

	/**
	 * Collection of global date/time parse formats.
	 */
	private static List<String> alDateTimeParseFormat;

	static {
		setLocale(Locale.getDefault());
		defaultTimeZone = TimeZone.getDefault();
	}

	/**
	 * Sets the default time zone to use when no instance-specific time zone is defined.
	 *
	 * @param tz the default TimeZone to use; if null, the system default is used
	 */
	public static void setDefaultTimeZone(TimeZone tz) {
		if (tz == null) {
			tz = TimeZone.getDefault();
		}
		defaultTimeZone = tz;
	}

	/**
	 * Returns the default time zone used when no instance-specific time zone is set.
	 *
	 * @return the default TimeZone
	 */
	public static TimeZone getDefaultTimeZone() {
		return defaultTimeZone;
	}

	/**
	 * Creates a GregorianCalendar, initializes it with the current
	 * time value, and applies the appropriate time zone.
	 *
	 * @return an initialized GregorianCalendar instance
	 */
	protected GregorianCalendar _getCal() {
		GregorianCalendar cal = new GregorianCalendar();
	    
	    TimeZone tz = (timeZone != null) ? timeZone : defaultTimeZone;
	    cal.setTimeZone(tz);
	    cal.setLenient(false);
	    cal.setTimeInMillis(_time);

	    return cal;
	}
	

	/**
	 * Sets the locale used for formatting and parsing date/time values and
	 * initializes global parse and output formats based on the locale.
	 *
	 * @param loc the Locale to use
	 */
	public static void setLocale(Locale loc) {
		locale = loc;
		alDateTimeParseFormat = new ArrayList<>();
		String s = getFormat(DateFormat.SHORT, locale);
		boolean bMonthFirst = true;
		boolean bYearFirst = false;
		if (s != null && s.length() > 0) {
			char ch = s.charAt(0);
			if (ch != 'M') {
				bMonthFirst = false;
			}
			if (ch == 'y') {
				bYearFirst = true;
			}
		}
		if (bMonthFirst) {
			staticOutputFormat = "MM/dd/yyyy hh:mma";
			// the "yy" formats must be before the "yyyy" formats because "yyyy" will
			// convert "05/04/65" -> "05/04/0065"
			alDateTimeParseFormat.add("MM/dd/yy hh:mm:ss.Sa");
			alDateTimeParseFormat.add("MM/dd/yy hh:mm:ssa");
			alDateTimeParseFormat.add("MM/dd/yy hh:mma");

			alDateTimeParseFormat.add("MM/dd/yy hh:mm:ss.S a");
			alDateTimeParseFormat.add("MM/dd/yy hh:mm:ss a");
			alDateTimeParseFormat.add("MM/dd/yy hh:mm a");

			alDateTimeParseFormat.add("MM/dd/yy HH:mm:ss.S");
			alDateTimeParseFormat.add("MM/dd/yy HH:mm:ss");
			alDateTimeParseFormat.add("MM/dd/yy HH:mm");

			alDateTimeParseFormat.add("MM/dd/yyyy hh:mm:ss.Sa");
			alDateTimeParseFormat.add("MM/dd/yyyy hh:mm:ssa");
			alDateTimeParseFormat.add("MM/dd/yyyy hh:mma");

			alDateTimeParseFormat.add("MM/dd/yyyy HH:mm:ss.S");
			alDateTimeParseFormat.add("MM/dd/yyyy HH:mm:ss");
			alDateTimeParseFormat.add("MM/dd/yyyy HH:mm");
		} else if (bYearFirst) {
			staticOutputFormat = "yyyy/MM/dd hh:mma";
			// the "yy" formats must be before the "yyyy" formats because "yyyy" will
			// convert "05/04/65" -> "05/04/0065"
			alDateTimeParseFormat.add("yy/MM/dd hh:mm:ss.Sa");
			alDateTimeParseFormat.add("yy/MM/dd hh:mm:ssa");
			alDateTimeParseFormat.add("yy/MM/dd hh:mma");

			alDateTimeParseFormat.add("yy/MM/dd HH:mm:ss.S");
			alDateTimeParseFormat.add("yy/MM/dd HH:mm:ss");
			alDateTimeParseFormat.add("yy/MM/dd HH:mm");

			alDateTimeParseFormat.add("yyyy/MM/dd hh:mm:ss.Sa");
			alDateTimeParseFormat.add("yyyy/MM/dd hh:mm:ssa");
			alDateTimeParseFormat.add("yyyy/MM/dd hh:mma");

			alDateTimeParseFormat.add("yyyy/MM/dd HH:mm:ss.S");
			alDateTimeParseFormat.add("yyyy/MM/dd HH:mm:ss");
			alDateTimeParseFormat.add("yyyy/MM/dd HH:mm");
		} else { // day first
			staticOutputFormat = "dd/MM/yyyy hh:mma";
			// the "yy" formats must be before the "yyyy" formats because "yyyy" will
			// convert "05/04/65" -> "05/04/0065"
			alDateTimeParseFormat.add("dd/MM/yy hh:mm:ss.Sa");
			alDateTimeParseFormat.add("dd/MM/yy hh:mm:ssa");
			alDateTimeParseFormat.add("dd/MM/yy hh:mma");

			alDateTimeParseFormat.add("dd/MM/yy HH:mm:ss.S");
			alDateTimeParseFormat.add("dd/MM/yy HH:mm:ss");
			alDateTimeParseFormat.add("dd/MM/yy HH:mm");

			alDateTimeParseFormat.add("dd/MM/yyyy hh:mm:ss.Sa");
			alDateTimeParseFormat.add("dd/MM/yyyy hh:mm:ssa");
			alDateTimeParseFormat.add("dd/MM/yyyy hh:mma");

			alDateTimeParseFormat.add("dd/MM/yyyy HH:mm:ss.S");
			alDateTimeParseFormat.add("dd/MM/yyyy HH:mm:ss");
			alDateTimeParseFormat.add("dd/MM/yyyy HH:mm");
		}
		// SQL date formats
		alDateTimeParseFormat.add("yyyy-MM-dd HH:mm:ss");
		alDateTimeParseFormat.add("yyyy-MM-dd");

		alDateTimeParseFormat.add(getFormat(DateFormat.SHORT));
		alDateTimeParseFormat.add(getFormat(DateFormat.MEDIUM));
		alDateTimeParseFormat.add(getFormat(DateFormat.LONG));
		alDateTimeParseFormat.add(getFormat(DateFormat.DEFAULT));
	}

	/**
	 * Creates a new date/time initialized to the current system time.
	 */
	public OADateTime() {
		this._time = System.currentTimeMillis();
	}

	/**
	 * Creates a new date/time using the specified millisecond value since the epoch.
	 *
	 * @param time milliseconds since the epoch
	 */
	public OADateTime(long time) {
		this._time = time;
	}

	public OADateTime(long time, TimeZone tz) {
		this._time = time;
		this.timeZone = tz;
	}
	
	/**
	 * Creates a new date/time using the specified date, time, and millisecond values.
	 *
	 * @param year full year value
	 * @param month month value from 0 to 11
	 * @param day day of month
	 * @param hrs hour value
	 * @param mins minute value
	 * @param secs second value
	 * @param milsecs millisecond value
	 */
	public OADateTime(int year, int month, int day, int hrs, int mins, int secs, int milsecs) {
		setCalendar(year, month, day, hrs, mins, secs, milsecs);
	}
	
	/**
	 * Creates a new date/time using the specified year, month, and day.
	 *
	 * @param year full year value
	 * @param month month value from 0 to 11
	 * @param day day of month from 1 to 31
	 */
	public OADateTime(int year, int month, int day) {
		setCalendar(year, month, day, 0, 0, 0, 0);
	}

	/**
	 * Creates a new date/time using the specified date and time values.
	 *
	 * @param year full year value
	 * @param month month value from 0 to 11
	 * @param day day of month from 1 to 31
	 * @param hrs hour value
	 * @param mins minute value
	 */
	public OADateTime(int year, int month, int day, int hrs, int mins) {
		setCalendar(year, month, day, hrs, mins, 0, 0);
	}

	/**
	 * Creates a new date/time using the specified date and time values.
	 *
	 * @param year full year value
	 * @param month month value from 0 to 11
	 * @param day day of month from 1 to 31
	 * @param hrs hour value
	 * @param mins minute value
	 * @param secs second value
	 */
	public OADateTime(int year, int month, int day, int hrs, int mins, int secs) {
		setCalendar(year, month, day, hrs, mins, secs, 0);
	}
	
	
	/**
	 * Creates a new date/time using another OADateTime instance.
	 * If the parameter is null, the current system time is used.
	 *
	 * @param odt the OADateTime to copy from
	 */
	public OADateTime(OADateTime odt) {
		if (odt == null) {
			this._time = System.currentTimeMillis();
		} else {
			this._time = odt.getTime();
			this.timeZone = odt.timeZone;
		}
	}

	
	/**
	 * Creates a new date/time using a date and time.
	 * If the date is null, a new OADate is created.
	 * If the time is not null, its time value is applied.
	 *
	 * @param d the OADate providing the date portion
	 * @param t the OATime providing the time portion
	 */
	public OADateTime(OADate d, OATime t) {
		if (d == null) {
			d = new OADate();
		}
		this._time = d.getTime();

		if (t != null) {
			setTime(t);
		}
	}
	
	
	/**
	 * Creates a new date/time using the specified Calendar value.
	 * If the calendar is null, the current system time is used.
	 *
	 * @param c the Calendar used to initialize this instance
	 */
	public OADateTime(Calendar c) {
		if (c == null) {
			this._time = System.currentTimeMillis();
		} else {
			this._time = c.getTimeInMillis();
			this.timeZone = c.getTimeZone();
		}
	}

	/**
	 * Creates a new date/time using the specified Instant value.
	 *
	 * @param instant the Instant used to initialize this instance
	 */
	public OADateTime(Instant instant) {
		this(instant.toEpochMilli());
	}
	
	/**
	 * Creates a new date/time using the specified LocalDateTime value.
	 *
	 * @param ldt the LocalDateTime used to initialize this instance
	 */
	public OADateTime(LocalDateTime ldt) {
	    if (ldt == null) {
	        this._time = System.currentTimeMillis();
	        return;
	    }

	    ZoneId zid = defaultTimeZone.toZoneId();
	    this._time = ldt
	        .atZone(zid)
	        .toInstant()
	        .toEpochMilli();
	}
	
	/**
	 * Creates a new date/time using the specified ZonedDateTime value.
	 *
	 * @param zdt the ZonedDateTime used to initialize this instance
	 */
	public OADateTime(ZonedDateTime zdt) {
	    if (zdt == null) {
	        this._time = System.currentTimeMillis();
	        return;
	    }
		this._time = zdt.toInstant().toEpochMilli();
		ZoneId zid = zdt.getZone();
		this.timeZone = TimeZone.getTimeZone(zid);
	}
	
	public OADateTime(Date date) {
		if (date == null) {
			this._time = System.currentTimeMillis();
		} else {
			this._time = date.getTime();
		}
	}

	/**
	 * Creates a new date/time using a string value.
	 *
	 * @param strDate the string representation of the date/time
	 * @see #valueOf(String)
	 */
	public OADateTime(String strDate) {
		setCalendar(strDate);
	}

	/**
	 * Creates a new date/time using a string value and a specified format.
	 *
	 * @param strDate the string representation of the date/time
	 * @param format the format used to parse the string
	 * @see #valueOf(String)
	 */
	public OADateTime(String strDate, String format) {
		setCalendar(strDate, format);
	}

	/**
	 * Returns this date/time as a LocalDateTime using the current field values.
	 *
	 * @return a LocalDateTime representation of this instance
	 */
	public LocalDateTime getLocalDateTime() {
		GregorianCalendar c = _getCal();
		LocalDateTime ldt = LocalDateTime.of(c.get(c.YEAR), c.get(c.MONTH) + 1, c.get(c.DAY_OF_MONTH), c.get(c.HOUR_OF_DAY), c.get(c.MINUTE), c.get(c.SECOND), (int) (c.get(c.MILLISECOND) * 1_000_000));
		return ldt;
	}

	/**
	 * Returns this date/time as a ZonedDateTime using the associated time zone.
	 *
	 * @return a ZonedDateTime representation of this instance
	 */
	public ZonedDateTime getZonedDateTime() {
		return Instant.ofEpochMilli(_time).atZone(getTimeZone().toZoneId());
	}

	/**
	 * Returns this date/time as an Instant.
	 *
	 * @return an Instant representing this date/time
	 */
	public Instant getInstant() {
		Instant instant = Instant.ofEpochMilli(_time);
		return instant;
	}

	/**
	 * Custom serialization logic for writing this object to an ObjectOutputStream.
	 * Handles different versions and serialization formats depending on type and
	 * time zone settings.
	 *
	 * @param stream the ObjectOutputStream to write to
	 * @throws IOException if an I/O error occurs
	 */
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		if (this instanceof OADate) {
			GregorianCalendar cal = _getCal();
			stream.writeInt(1);
			stream.writeInt(cal.get(Calendar.YEAR)); // 0-11
			stream.writeInt(cal.get(Calendar.MONTH));
			stream.writeInt(cal.get(Calendar.DATE));
		} else if (this instanceof OATime) {
			GregorianCalendar cal = _getCal();
			stream.writeInt(2);
			stream.writeInt(cal.get(Calendar.HOUR_OF_DAY));
			stream.writeInt(cal.get(Calendar.MINUTE));
			stream.writeInt(cal.get(Calendar.SECOND));
			stream.writeInt(cal.get(Calendar.MILLISECOND));
		} else {
			if (this.timeZone != null) {
				stream.writeInt(3);
				stream.writeUTF(this.timeZone.getID());
				GregorianCalendar cal = _getCal();
				stream.writeInt(cal.get(Calendar.YEAR));
				stream.writeInt(cal.get(Calendar.MONTH));
				stream.writeInt(cal.get(Calendar.DATE));
				stream.writeInt(cal.get(Calendar.HOUR_OF_DAY));
				stream.writeInt(cal.get(Calendar.MINUTE));
				stream.writeInt(cal.get(Calendar.SECOND));
				stream.writeInt(cal.get(Calendar.MILLISECOND));
			} else {
				stream.writeInt(4); 
				stream.writeLong(_time);
			}
		}
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		final int x = in.readInt();

		this.timeZone = null;
		int year = 0; 			
		int month = 0;
		int day = 0;
		int hour24 = 0;
		int minute = 0;
		int second = 0;
		int milisecond = 0;
		this._time = 0L;
		
		if (x == 1) {
			year = in.readInt();
			month = in.readInt();
			day = in.readInt();
		} else if (x == 2) {
			year = 1970;
			month = Calendar.JANUARY;
			day = 1;
			hour24 = in.readInt();
			minute = in.readInt();
			second = in.readInt();
			milisecond = in.readInt();
		} else if (x == 3) {
			String tzId = in.readUTF();
			this.timeZone = OATimeZone.getTimeZoneById(tzId);
			
			year = in.readInt();
			month = in.readInt();
			day = in.readInt();
			hour24 = in.readInt();
			minute = in.readInt();
			second = in.readInt();
			milisecond = in.readInt();
		} else if (x == 4) {
			_time = in.readLong();
			return; 
		}
		else {
		    throw new IOException("Unknown OADateTime serialized type: " + x);
		}
		
		TimeZone tz = (this.timeZone != null) ? this.timeZone : defaultTimeZone;
		GregorianCalendar calNew = new GregorianCalendar(tz);
		calNew.clear();
		calNew.setLenient(false);
		calNew.set(year, month, day, hour24, minute, second);
		calNew.set(Calendar.MILLISECOND, milisecond);
		this._time = calNew.getTimeInMillis();		
	}
	
	/**
	 * Returns the value of the specified calendar field.
	 *
	 * @param fld the Calendar field constant
	 * @return the field value
	 */
	public int getField(int fld) {
		GregorianCalendar c = _getCal();
		int x = c.get(fld);
		return x;
	}

	/**
	 * Returns a clone of the Calendar used by this date/time instance.
	 *
	 * @return a cloned Calendar representing this date/time
	 */
	public Calendar getCalendar() {
		GregorianCalendar c = _getCal();
		Calendar cNew = (Calendar) c.clone();
		return cNew;
	}

	/**
	 * Sets the internal time value using the specified date and time components.
	 *
	 * @param year full year value
	 * @param month month value from 0 to 11
	 * @param day day of month
	 * @param hrs hour value
	 * @param mins minute value
	 * @param secs second value
	 * @param milsecs millisecond value
	 */
	protected void setCalendar(int year, int month, int day, int hrs, int mins, int secs, int milsecs) {
		GregorianCalendar c = new GregorianCalendar(getTimeZone());
		c.clear();
		c.setLenient(false);
		c.set(year, month, day, hrs, mins, secs);
		c.set(Calendar.MILLISECOND, milsecs);
		_time = c.getTimeInMillis();		
	}

	/**
	 * Sets the internal time value using the specified GregorianCalendar.
	 * If the calendar is null, the current system time is used.
	 *
	 * @param c the GregorianCalendar to copy values from
	 */
	protected void setCalendar(GregorianCalendar c) {
		if (c == null) {
			this._time = System.currentTimeMillis();
		} else {
			this._time = c.getTimeInMillis();
			this.timeZone = c.getTimeZone();
		}
	}

	/**
	 * Sets the internal time value using the specified SQL Timestamp.
	 * If the timestamp is null, the current system time is used.
	 *
	 * @param date the SQL Timestamp to use
	 */
	protected void setCalendar(java.sql.Timestamp date) {
		if (date == null) {
			this._time = System.currentTimeMillis();
		} else {
			this._time = date.getTime();
		}
	}

	/**
	 * Sets the internal time value using the specified Date.
	 * If the date is null, the current system time is used.
	 *
	 * @param date the Date to use
	 */
	protected void setCalendar(Date date) {
		if (date == null) {
			this._time = System.currentTimeMillis();
		} else {
			this._time = date.getTime();
		}
	}

	/**
	 * Sets the internal time value using another OADateTime instance.
	 * If the instance is null, the current system time is used.
	 *
	 * @param dt the OADateTime to copy values from
	 */
	protected void setCalendar(OADateTime dt) {
		if (dt == null) {
			this._time = System.currentTimeMillis();
		} else {
			this._time = dt.getTime();
			this.timeZone = dt.timeZone;
		}
	}

	/**
	 * Sets the internal time value using a string representation of a date/time.
	 * If the string is null, the current system time is used.
	 *
	 * @param strDate the string representation of the date/time
	 * @throws IllegalArgumentException if the string cannot be converted
	 */
	protected void setCalendar(String strDate) {
		if (strDate == null) {
			this._time = System.currentTimeMillis();
		} else {
			OADateTime dt = valueOf(strDate);
			if (dt == null) {
				throw new IllegalArgumentException("OADateTime cant create date from String \"" + strDate + "\"");
			}
			setCalendar(dt);
		}
	}

	/**
	 * Sets the internal time value using a string representation and format.
	 * If the string is null, the current system time is used.
	 *
	 * @param strDate the string representation of the date/time
	 * @param fmt the format used to parse the string
	 * @throws IllegalArgumentException if the string cannot be converted
	 */
	protected void setCalendar(String strDate, String fmt) {
		if (strDate == null) {
			this._time = System.currentTimeMillis();
		} else {
			OADateTime dt = valueOf(strDate, fmt);
			if (dt == null) {
				throw new IllegalArgumentException("OADateTime cant create date from String \"" + strDate + "\"");
			}
			setCalendar(dt);
		}
	}

	/**
	 * Sets the hour, minute, second, and millisecond values to zero.
	 */
	public void clearTime() {
		GregorianCalendar c = _getCal();
		c.set(c.HOUR_OF_DAY, 0);
		c.set(c.MINUTE, 0);
		c.set(c.SECOND, 0);
		c.set(c.MILLISECOND, 0);
		_time = c.getTimeInMillis();
	}

	/**
	 * Sets the date portion to January 1, 1970 while preserving the time portion.
	 */
	public void clearDate() {
		GregorianCalendar c = _getCal();
		c.set(c.YEAR, 1970);
		c.set(c.MONTH, c.JANUARY);
		c.set(c.DATE, 1);

		// these are added to make sure timezone is calculated correctly
		c.set(c.HOUR_OF_DAY, get24Hour());
		c.set(c.MINUTE, getMinute());
		c.set(c.SECOND, getSecond());
		c.set(c.MILLISECOND, getMilliSecond());

		_time = c.getTimeInMillis();
	}

	/**
	 * Sets the time using hour and minute values.
	 *
	 * @param hr hour value
	 * @param m minute value
	 */
	public void setTime(int hr, int m) {
		setTime(hr, m, 0, 0);
	}

	/**
	 * Sets the time using hour, minute, and second values.
	 *
	 * @param hr hour value
	 * @param m minute value
	 * @param s second value
	 */
	public void setTime(int hr, int m, int s) {
		setTime(hr, m, s, 0);
	}

	/**
	 * Sets the hour, minute, second, and millisecond values.
	 *
	 * @param hr hour value
	 * @param m minute value
	 * @param s second value
	 * @param ms millisecond value
	 */
	public void setTime(int hr, int m, int s, int ms) {
		GregorianCalendar c = _getCal();
		c.set(c.HOUR_OF_DAY, hr);
		c.set(c.MINUTE, m);
		c.set(c.SECOND, s);
		c.set(c.MILLISECOND, ms);
		_time = c.getTimeInMillis();
	}

	/**
	 * Sets the time using an OATime instance.
	 * If the parameter is null, the time portion is cleared.
	 *
	 * @param t the OATime to copy values from
	 */
	public void setTime(OATime t) {
		if (t == null) {
			clearTime();
			return;
		}
		setTime(t.get24Hour(), t.getMinute(), t.getSecond(), t.getMilliSecond());
	}

	/**
	 * Sets the date using year, month, and day values.
	 *
	 * @param yr full year value
	 * @param m month value from 0 to 11
	 * @param d day of month
	 */
	public void setDate(int yr, int m, int d) {
		GregorianCalendar c = _getCal();
		c.set(c.YEAR, yr);
		c.set(c.MONTH, m);
		c.set(c.DATE, d);
		_time = c.getTimeInMillis();
	}

	/**
	 * Sets the date using an OADate instance.
	 * If the parameter is null, the date portion is cleared.
	 *
	 * @param d the OADate to copy values from
	 */
	public void setDate(OADate d) {
		if (d == null) {
			clearDate();
			return;
		}
		setDate(d.getYear(), d.getMonth(), d.getDay());
	}

	/**
	 * Returns the full year value for this date/time.
	 *
	 * @return the year value
	 */
	public int getYear() {
		GregorianCalendar c = _getCal();
		int yr = c.get(c.YEAR);
		return yr;
	}

	/**
	 * Sets the year value for this date/time.
	 *
	 * @param y full year value
	 */
	public void setYear(int y) {
		GregorianCalendar c = _getCal();
		c.set(c.YEAR, y);
		_time = c.getTimeInMillis();
	}


	/**
	 * Returns the quarter of the year.
	 *
	 * @return quarter value from 0 to 3
	 */
	public int getQuarter() {
		int x = getMonth();
		x /= 3;
		return x;
	}

	/**
	 * Returns the month value.
	 * @return month value from 0 to 11
	 */
	public int getMonth() {
		GregorianCalendar c = _getCal();
		int m = c.get(c.MONTH);
		return m;
	}
	
	/**
	 * Sets the month value.
	 * @param month month value from 0 to 11
	 */
	public void setMonth(int month) {
		GregorianCalendar c = _getCal();
		c.set(c.MONTH, month);
		_time = c.getTimeInMillis();
	}

	// Note: support for java.time.* MONTH 1-12 , not 0-11 as Calendar does it
	/**
	 * Returns the month value.
	 * @return month value from 1 to 12
	 */
	public int getMonthValue() {
	    return getMonth() + 1;
	}

	/**
	 * Sets the month value.
	 * @param month month value from 1 to 12
	 */
	public void setMonthValue(int monthValue) {
	    setMonth(monthValue - 1);
	}	
	
	
	/**
	 * Returns the day of the month.
	 *
	 * @return day of month from 1 to 31
	 */
	public int getDay() {
		GregorianCalendar c = _getCal();
		int d = c.get(c.DAY_OF_MONTH);
		return d;
	}

	/**
	 * Sets the day of the month.
	 *
	 * @param d day of month from 1 to 31
	 */
	public void setDay(int d) {
		GregorianCalendar c = _getCal();
		c.set(c.DAY_OF_MONTH, d);
		_time = c.getTimeInMillis();
	}

	/**
	 * Sets the time zone to UTC.
	 */
	public void setTimeZoneUTC() {
		setTimeZone(OATimeZone.getTimeZoneUTC());
	}

	/**
	 * Sets the time zone using an OATimeZone.TZ value.
	 *
	 * @param tz the OATimeZone.TZ to set
	 */
	public void setTimeZone(OATimeZone.TZ tz) {
		setTimeZone(tz.timeZone);
	}

	
	
	/**
	 * Sets the time zone for this date/time while keeping the same date and time
	 * field values, adjusting the underlying time value accordingly.
	 *
     * Important Note:  changes _time so that the current Y/M/D/H/M/S/MS fields remain the same under the new timezone.
	 *
	 * @param tzNew the TimeZone to set
	 */
	public void setTimeZone(TimeZone tzNew) {
		if (tzNew == timeZone) {
			return;
		}
		
		// need to create a new cal, otherwise setting tz will adjust the other values
		// (use convertTo(tz) instead)
		GregorianCalendar calNew = new GregorianCalendar(tzNew != null ? tzNew : defaultTimeZone);
		calNew.clear();
		calNew.setLenient(false);

		GregorianCalendar c = _getCal();
		int y = c.get(c.YEAR); 			
		int month = c.get(c.MONTH);
		int d = c.get(c.DAY_OF_MONTH);
		int h24 = c.get(c.HOUR_OF_DAY);
		int minute = c.get(c.MINUTE);
		int sec = c.get(c.SECOND);
		int ms = c.get(c.MILLISECOND);

		calNew.set(y, month, d, h24, minute, sec);
		calNew.set(Calendar.MILLISECOND, ms);

		this._time = calNew.getTimeInMillis();
		this.timeZone = tzNew;
	}

	
	/**
	 * Returns the time zone associated with this date/time.
	 *
	 * @return the TimeZone for this instance, or the default time zone if none is set
	 */
	public TimeZone getTimeZone() {
		return timeZone == null ? defaultTimeZone : timeZone;
	}

	/**
	 * Returns the hour of the day using a 24-hour clock.
	 *
	 * @return hour value from 0 to 23
	 */
	public int getHour() {
		GregorianCalendar c = _getCal();
		int hr = c.get(c.HOUR_OF_DAY); // 24 hr
		return hr;
	}

	/**
	 * Sets the hour of the day using a 24-hour clock.
	 *
	 * @param hr hour value
	 */
	public void setHour(int hr) {
		GregorianCalendar c = _getCal();
		c.set(c.HOUR_OF_DAY, hr);
		_time = c.getTimeInMillis();
	}

	/**
	 * Returns the hour of the day using a 12-hour clock.
	 *
	 * @return hour value from 0 to 11
	 */
	public int get12Hour() {
		GregorianCalendar c = _getCal();
		int hr = c.get(c.HOUR); // 12 hr format
		return hr;
	}

	/**
	 * Sets the hour of the day using a 12-hour clock.
	 *
	 * @param hr hour value from 1 to 12
	 * @throws IllegalArgumentException if hr is outside the range 1 to 12
	 */
	@Deprecated
	public void set12Hour(int hr) {
		// Accept 1–12; coerce into 0–11
		if (hr < 1 || hr > 12) throw new IllegalArgumentException("hr must be 1..12");
		int h = (hr % 12); // 12→0
		setHour(h);
	}

	/**
	 * Returns the hour of the day using a 24-hour clock.
	 *
	 * @return hour value from 0 to 23
	 */
	public int get24Hour() {
		return getHour();
	}

	/**
	 * Sets the hour of the day using a 24-hour clock.
	 *
	 * @param hr hour value from 0 to 23
	 */
	public void set24Hour(int hr) {
		setHour(hr);
	}

	/**
	 * Returns whether the current time is AM or PM.
	 *
	 * @return Calendar.AM or Calendar.PM
	 */
	public int getAM_PM() {
		if (getHour() >= 12) {
			return Calendar.PM;
		}
		return Calendar.AM;
	}

	/**
	 * Sets the AM or PM value for the current time.
	 *
	 * @param ap Calendar.AM or Calendar.PM
	 */
	@Deprecated
	public void setAM_PM(int ap) {
		int hr = getHour();

		if (ap == Calendar.AM) {
			if (hr >= 12)
				hr -= 12; // 12→0; 13..23→1..11; 0..11 stay
		} else if (ap == Calendar.PM) {
			if (hr < 12)
				hr += 12; // 0..11→12..23
		}
		set24Hour(hr);
	}

	/**
	 * Returns the minute value.
	 *
	 * @return minute value from 0 to 59
	 */
	public int getMinute() {
		GregorianCalendar c = _getCal();
		int m = c.get(c.MINUTE);
		return m;
	}

	/**
	 * Sets the minute value.
	 *
	 * @param mins minute value
	 */
	public void setMinute(int mins) {
		GregorianCalendar c = _getCal();
		c.set(c.MINUTE, mins);
		_time = c.getTimeInMillis();
	}

	/**
	 * Returns the second value.
	 *
	 * @return second value from 0 to 59
	 */
	public int getSecond() {
		GregorianCalendar c = _getCal();
		int x = c.get(c.SECOND);
		return x;
	}

	/**
	 * Sets the second value.
	 *
	 * @param s second value
	 */
	public void setSecond(int s) {
		GregorianCalendar c = _getCal();
		c.set(c.SECOND, s);
		_time = c.getTimeInMillis();
	}

	/**
	 * Clears the second and millisecond values by setting them to zero.
	 */
	public void clearSecondAndMilliSecond() {
		GregorianCalendar c = _getCal();
		c.set(c.SECOND, 0);
		c.set(c.MILLISECOND, 0);
		_time = c.getTimeInMillis();
	}

	/**
	 * Returns the millisecond value.
	 *
	 * @return millisecond value
	 */
	public int getMilliSecond() {
		GregorianCalendar c = _getCal();
		int x = c.get(c.MILLISECOND);
		return x;
	}

	/**
	 * Sets the millisecond value.
	 *
	 * @param ms millisecond value
	 */
	public void setMilliSecond(int ms) {
		GregorianCalendar c = _getCal();
		c.set(c.MILLISECOND, ms);
		_time = c.getTimeInMillis();
	}

	/**
	 * Returns a Date instance representing this date/time.
	 *
	 * @return a Date with the same time value
	 */
	public Date getDate() {
		return new Date(_time);
	}

	/**
	 * Returns the day of the week for this date.
	 *
	 * @return Calendar day-of-week constant
	 */
	public int getDayOfWeek() {
		GregorianCalendar c = _getCal();
		int x = c.get(Calendar.DAY_OF_WEEK);
		return x;
	}

	/**
	 * Returns the day of the year.
	 *
	 * @return day of year where January 1 is 1
	 */
	public int getDayOfYear() {
		GregorianCalendar c = _getCal();
		int x = c.get(Calendar.DAY_OF_YEAR);
		return x;
	}

	/**
	 * Returns the week of the month.
	 *
	 * @return week number within the month, where first week is 1.
	 */
	public int getWeekOfMonth() {
		GregorianCalendar c = _getCal();
		int x = c.get(Calendar.WEEK_OF_MONTH);
		return x;
	}

	/**
	 * Returns the week of the year.
	 *
	 * @return week number within the year, where first week is 1
	 */
	public int getWeekOfYear() {
		GregorianCalendar c = _getCal();
		int x = c.get(Calendar.WEEK_OF_YEAR);
		return x;
	}

	/**
	 * Returns the number of days in the current month.
	 *
	 * @return number of days in month
	 */
	public int getDaysInMonth() {
		GregorianCalendar c = _getCal();
		int x = c.getActualMaximum(Calendar.DAY_OF_MONTH);
		return x;
	}

	/**
	 * Compares this date/time with another object for equality..
	 * If object is not an OADateTime, it
	 * will be converted and then compared.
	 *
	 * @param obj the object to compare
	 * @return true if equal; false otherwise
	 */
	public boolean equals(Object obj) {
		if (this == obj) return true;
		try {
			// Warning: calling compareTo could cause an infinite loop if compareTo calls
			// equals.
			int i = compareTo(obj);
			return (i == 0);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Returns a hash code based on the internal time value.
	 *
	 * @return hash code for this instance
	 */
	@Override
	public int hashCode() {
		return (int) (_time % Integer.MAX_VALUE);
	}

	/**
	 * Determines whether this date/time occurs before another object.
	 * If object is not an OADateTime, it
	 * will be converted and then compared.
	 *
	 * @param obj the object to compare to
	 * @return true if this date/time is before the other
	 */
	public boolean before(Object obj) {
		return (compareTo(obj) < 0);
	}

	/**
	 * Compares this OADateTime with any object. If object is not an OADateTime, it
	 * will be converted and then compared.
	 *
	 * @param obj Date, OADate, Calendar, String, etc.
	 * @see #compareTo
	 */
	public boolean isBefore(Object obj) {
		return (compareTo(obj) < 0);
	}

	/**
	 * Determines whether this date/time occurs after another object.
	 *
	 * @param obj the object to compare to
	 * @return true if this date/time is after the other
	 */
	public boolean after(Object obj) {
		return (compareTo(obj) > 0);
	}

	/**
	 * Determines whether this date/time occurs after another object.
	 *
	 * @param obj the object to compare to
	 * @return true if this date/time is after the other
	 */
	public boolean isAfter(Object obj) {
		return (compareTo(obj) > 0);
	}

	/**
	 * Delegates to {@link #isBetweenOrEqual(Object, Object)}.
	 *
	 * @param obj1 the lower bound value
	 * @param obj2 the upper bound value
	 * @return true if between or equal
	 */
    public boolean betweenOrEqual(Object obj1, Object obj2) {
        return isBetweenOrEqual(obj1, obj2);
    }

    /**
     * Tests whether this date is greater than or equal to the first value
     * and less than or equal to the second value.
     *
     * @param obj1 the lower bound value
     * @param obj2 the upper bound value
     * @return true if between or equal
     */
    public boolean isBetweenOrEqual(Object obj1, Object obj2) {
        int i = compareTo(obj1);
        if (i < 0) {
            return false;
        }
        i = compareTo(obj2);
        return (i <= 0);
    }
	
    /**
     * Delegates to {@link #isBetweenNotEqual(Object, Object)}.
     *
     * @param obj1 the lower bound value
     * @param obj2 the upper bound value
     * @return true if strictly between
     */
    public boolean betweenNotEqual(Object obj1, Object obj2) {
        return isBetweenNotEqual(obj1, obj2);
    }
    
    /**
     * Tests whether this date is strictly greater than the first value
     * and strictly less than the second value.
     *
     * @param obj1 the lower bound value
     * @param obj2 the upper bound value
     * @return true if strictly between
     */
    public boolean isBetweenNotEqual(Object obj1, Object obj2) {
        int i = compareTo(obj1);
        if (i <= 0) {
            return false;
        }
        i = compareTo(obj2);
        return (i < 0);
    }
	
	
	/**
	 * Compares this date/time with another object.
	 *
	 * @param obj the object to compare to
	 * @return the comparison result
	 */
	public int compare(Object obj) {
		return compareTo(obj);
	}

	
	
	/**
	 * Compares this date/time with another object for ordering.
	 * Returns a negative value, zero, or a positive value depending on ordering.
	 *
	 * @param obj the object to compare to
	 * @return comparison result
	 */
	public int compareTo(Object obj) {
		if (obj == null) {
			return 1;
		}
		
		if (!(obj instanceof OADateTime)) {
			 return 2;			
		}
		
		OADateTime dtObj = (OADateTime) obj;
		int result;

	    if (this instanceof OADate || dtObj instanceof OADate) {
			GregorianCalendar cThis = _getCal();
			GregorianCalendar cObj = dtObj._getCal();
			result = Long.compare(cThis.get(cThis.YEAR), cObj.get(cObj.YEAR));
			if (result == 0) {
				result = Long.compare(cThis.get(cThis.MONTH), cObj.get(cObj.MONTH));
				if (result == 0) {
					result = Long.compare(cThis.get(cThis.DAY_OF_MONTH), cObj.get(cObj.DAY_OF_MONTH));
				}
			}
	    }
	    else if (this instanceof OATime || dtObj instanceof OATime) {
			GregorianCalendar cThis = _getCal();
			GregorianCalendar cObj = dtObj._getCal();
			result = Long.compare(cThis.get(cThis.HOUR_OF_DAY), cObj.get(cObj.HOUR_OF_DAY));
			if (result == 0) {
				result = Long.compare(cThis.get(cThis.MINUTE), cObj.get(cObj.MINUTE));
				if (result == 0) {
					result = Long.compare(cThis.get(cThis.SECOND), cObj.get(cObj.SECOND));
					if (result == 0) {
						result = Long.compare(cThis.get(cThis.MILLISECOND), cObj.get(cObj.MILLISECOND));
					}
				}
			}
	    }
	    else {
	    	result = Long.compare(this._time, dtObj._time);
	    }
	    return result;
	}

	/**
	 * Converts this date/time to UTC.
	 *
	 * @return a new OADateTime converted to UTC
	 */
	public OADateTime convertToUTC() {
		return convertTo(OATimeZone.getTimeZoneUTC());
	}

	/*
	 * Convert the current dt to a different tz, which will adjust the (long) time
	 * value, affecting (year,month,day,hour) values Note: for OADate
	 * year,month,day(,hour,min..) are not affected, only the timezone Note: for
	 * OATime only hour and timezone are affected.
	 */
	/**
	 * Converts this date/time to the specified time zone.
	 *
     * Important Note: _time does not change,  Y/M/D/H/M/S/MS will be different. 
     * 
	 * @param tz the TimeZone to convert to
	 * @return a new OADateTime converted to the specified time zone
	 */
	public OADateTime convertTo(TimeZone tz) {
		OADateTime dt;
		if (this instanceof OADate) {
			dt = new OADate(this);
		} else if (this instanceof OATime) {
			dt = new OATime(this);
		} else {
			dt = new OADateTime(this);
		}

		GregorianCalendar c = dt._getCal();
		c.setTimeZone(tz);
		dt = new OADateTime(c);
		if (this instanceof OADate) {
			dt = new OADate(dt);
		} else if (this instanceof OATime) {
			dt = new OATime(dt);
		}
		return dt;
	}

	/**
	 * Converts this date/time to the specified OATimeZone.
	 *
	 * @param tz the OATimeZone.TZ to convert to
	 * @return a new OADateTime converted to the specified time zone
	 */
	public OADateTime convertTo(OATimeZone.TZ tz) {
		OADateTime dt;
		if (this instanceof OADate) {
			dt = new OADate(this);
		} else if (this instanceof OATime) {
			dt = new OATime(this);
		} else {
			dt = new OADateTime(this);
		}

		if (tz != null) {
			if (this instanceof OADate) {
				dt.setTimeZone(tz);
			} else {
				GregorianCalendar c = dt._getCal();
				c.setTimeZone(tz.timeZone);
				dt = new OADateTime(c);
				if (this instanceof OATime) {
					dt = new OATime(dt);
				}
			}
		}
		return dt;
	}

	/*
	 * Return a new OADateTime where a specified amount of days is added.
	 * <p>
	 * Note: if this is an instanceof OADate or OATime, then the returned object
	 * will be the same type.
	 *
	 * @param amount number of days to increment/deincrement (negative number).
	 * @return new OADateTime object.
	 */
	/**
	 * Returns a new date/time with the specified number of days added.
	 *
	 * @param amount number of days to add (negative to subtract)
	 * @return a new OADateTime instance
	 */
	public OADateTime addDays(int amount) {
		if (this instanceof OATime) {
			return new OATime(this);
		}

		OADateTime dtNew;
		final GregorianCalendar c = _getCal();
		c.add(Calendar.DATE, amount);

		if (this instanceof OADate) {
			dtNew = new OADate(c);
		} else {
			dtNew = new OADateTime(c);
		}
		return dtNew;
	}

	/**
	 * Returns a new date/time with the specified number of days subtracted.
	 *
	 * @param amount number of days to subtract
	 * @return a new OADateTime instance
	 */
	public OADateTime subtractDays(int amount) {
		return addDays(-amount);
	}

	/**
	 * Returns a new date/time incremented by one day.
	 *
	 * @return a new OADateTime instance
	 */
	public OADateTime addDay() {
		return addDays(1);
	}

	/**
	 * Returns a new date/time decremented by one day.
	 *
	 * @return a new OADateTime instance
	 */
	public OADateTime subtractDay() {
		return addDays(-1);
	}

	/*
	 * Return an OADateTime where a specified amount of weeks added.
	 * <p>
	 * Note: if this is an instanceof OADate or OATime, then the returned object
	 * will be the same type.
	 *
	 * @param amount number of weeks to increment/deincrement (negative number).
	 * @return new OADateTime object.
	 */
	/**
	 * Returns a new date/time with the specified number of weeks added.
	 *
	 * @param amount number of weeks to add (negative to subtract)
	 * @return a new OADateTime instance
	 */
	public OADateTime addWeeks(int amount) {
		return addDays(amount * 7);
	}

	/**
	 * Returns a new date/time with the specified number of weeks subtracted.
	 *
	 * @param amount number of weeks to subtract
	 * @return a new OADateTime instance
	 */
	public OADateTime subtractWeeks(int amount) {
		return addDays(-(amount * 7));
	}

	/*
	 * Return an OADateTime where a specified amount of months is added.
	 * <p>
	 * Note: if this is an instanceof OADate or OATime, then the returned object
	 * will be the same type.
	 *
	 * @param amount number of months to increment/deincrement (negative number).
	 * @return new OADateTime object.
	 */
	/**
	 * Returns a new date/time with the specified number of months added.
	 *
	 * @param amount number of months to add (negative to subtract)
	 * @return a new OADateTime instance
	 */
	public OADateTime addMonths(int amount) {
		if (this instanceof OATime) {
			return new OATime(this);
		}

		OADateTime dtNew;
		GregorianCalendar c = _getCal();
		c.add(Calendar.MONTH, amount);

		if (this instanceof OADate) {
			dtNew = new OADate(c);
		} else {
			dtNew = new OADateTime(c);
		}
		return dtNew;
	}

	/**
	 * Returns a new date/time with the specified number of months subtracted.
	 *
	 * @param amount number of months to subtract
	 * @return a new OADateTime instance
	 */
	public OADateTime subtractMonths(int amount) {
		return addMonths(-amount);
	}

	/*
	 * Return an OADateTime where a specified amount of years is added.
	 * <p>
	 * Note: if this is an instanceof OADate or OATime, then the returned object
	 * will be the same type.
	 *
	 * @param amount number of years to increment/deincrement (negative number).
	 * @return new OADateTime object.
	 */
	/**
	 * Returns a new date/time with the specified number of years added.
	 *
	 * @param amount number of years to add (negative to subtract)
	 * @return a new OADateTime instance
	 */
	public OADateTime addYears(int amount) {
		if (this instanceof OATime) {
			return new OATime(this);
		}

		OADateTime dtNew;
		GregorianCalendar c = _getCal();
		c.add(Calendar.YEAR, amount);

		if (this instanceof OADate) {
			dtNew = new OADate(c);
		} else {
			dtNew = new OADateTime(c);
		}
		return dtNew;
	}

	/**
	 * Returns a new date/time with the specified number of years subtracted.
	 *
	 * @param amount number of years to subtract
	 * @return a new OADateTime instance
	 */
	public OADateTime subtractYears(int amount) {
		return addYears(-amount);
	}

	/*
	 * Return an OADateTime where a specified amount of hours is added.
	 * <p>
	 * Note: if this is an instanceof OADate or OATime, then the returned object
	 * will be the same type.
	 *
	 * @param amount number of hours to increment/deincrement (negative number).
	 * @return new OADateTime object.
	 */
	/**
	 * Returns a new date/time with the specified number of hours added.
	 *
	 * @param amount number of hours to add (negative to subtract)
	 * @return a new OADateTime instance
	 */
	public OADateTime addHours(int amount) {
		OADateTime dtNew;
		GregorianCalendar c = _getCal();
		c.add(Calendar.HOUR_OF_DAY, amount);

		if (this instanceof OATime) {
			dtNew = new OATime(c);
		} else if (this instanceof OADate) {
			dtNew = new OADate(c);
		} else {
			dtNew = new OADateTime(c);
		}
		return dtNew;
	}

	/**
	 * Returns a new date/time with the specified number of hours subtracted.
	 *
	 * @param amount number of hours to subtract
	 * @return a new OADateTime instance
	 */
	public OADateTime subtractHours(int amount) {
		return addHours(-amount);
	}

	/*
	 * Return an OADateTime where a specified amount of minutes is added.
	 * <p>
	 * Note: if this is an instanceof OADate or OATime, then the returned object
	 * will be the same type.
	 *
	 * @param amount number of minutes to increment/deincrement (negative number).
	 * @return new OADateTime object.
	 */
	/**
	 * Returns a new date/time with the specified number of minutes added.
	 *
	 * @param amount number of minutes to add (negative to subtract)
	 * @return a new OADateTime instance
	 */
	public OADateTime addMinutes(int amount) {
		OADateTime dtNew;
		GregorianCalendar c = _getCal();
		c.add(Calendar.MINUTE, amount);

		if (this instanceof OATime) {
			dtNew = new OATime(c);
		} else if (this instanceof OADate) {
			dtNew = new OADate(c);
		} else {
			dtNew = new OADateTime(c);
		}
		return dtNew;
	}

	/**
	 * Returns a new date/time with the specified number of minutes subtracted.
	 *
	 * @param amount number of minutes to subtract
	 * @return a new OADateTime instance
	 */
	public OADateTime subtractMinutes(int amount) {
		return addMinutes(-amount);
	}

	/*
	 * Return an OADateTime where a specified amount of seconds is added.
	 * <p>
	 * Note: if this is an instanceof OADate or OATime, then the returned object
	 * will be the same type.
	 *
	 * @param amount number of seconds to increment/deincrement (negative number).
	 * @return new OADateTime object.
	 */
	/**
	 * Returns a new date/time with the specified number of seconds added.
	 *
	 * @param amount number of seconds to add (negative to subtract)
	 * @return a new OADateTime instance
	 */
	public OADateTime addSeconds(int amount) {
		OADateTime dtNew;
		GregorianCalendar c = _getCal();
		c.add(Calendar.SECOND, amount);

		if (this instanceof OATime) {
			dtNew = new OATime(c);
		} else if (this instanceof OADate) {
			dtNew = new OADate(c);
		} else {
			dtNew = new OADateTime(c);
		}
		return dtNew;
	}

	/**
	 * Returns a new date/time with the specified number of seconds subtracted.
	 *
	 * @param amount number of seconds to subtract
	 * @return a new OADateTime instance
	 */
	public OADateTime subtractSeconds(int amount) {
		return addSeconds(-amount);
	}

	/*
	 * Return an OADateTime where a specified amount of milliseconds is added.
	 * <p>
	 * Note: if this is an instanceof OADate or OATime, then the returned object
	 * will be the same type.
	 *
	 * @param amount number of milliseconds to increment/deincrement (negative
	 *               number).
	 * @return new OADateTime object.
	 */
	/**
	 * Returns a new date/time with the specified number of milliseconds added.
	 *
	 * @param amount number of milliseconds to add (negative to subtract)
	 * @return a new OADateTime instance
	 */
	public OADateTime addMilliSeconds(int amount) {
		OADateTime dtNew;
		GregorianCalendar c = _getCal();
		c.add(Calendar.MILLISECOND, amount);

		if (this instanceof OATime) {
			dtNew = new OATime(c);
		} else if (this instanceof OADate) {
			dtNew = new OADate(c);
		} else {
			dtNew = new OADateTime(c);
		}
		return dtNew;
	}

	/**
	 * Returns a new date/time with the specified number of milliseconds subtracted.
	 *
	 * @param amount number of milliseconds to subtract
	 * @return a new OADateTime instance
	 */
	public OADateTime subtractMilliSeconds(int amount) {
		return addMilliSeconds(-amount);
	}

	/*
	 * Returns the number of years between this OADateTime and obj.
	 *
	 * @param obj Date, OADateTime, Calendar, etc that can be converted to an
	 *            OADateTime.
	 */
	/**
	 * Returns the number of years between this date/time and another object.
	 *
	 * @param obj an object convertible to OADateTime
	 * @return number of years between the two dates
	 */
	public int betweenYears(OADateTime d) {
		return Math.abs(this.getYear() - d.getYear());
	}

	/*
	 * Returns the number of months betweeen this OADateTime and obj.
	 *
	 * @param obj Date, OADateTime, Calendar, etc that can be converted to an
	 *            OADateTime.
	 */
	/**
	 * Returns the number of months between this date/time and another object.
	 *
	 * @param obj an object convertible to OADateTime
	 * @return number of months between the two dates
	 */
	public int betweenMonths(OADateTime d) {
		int amt = this.getYear() - d.getYear();
		amt = Math.abs(amt) * 12;

		if (compareTo(d) >= 0) {
			amt += (d.getMonth() - this.getMonth());
		} else {
			amt += (this.getMonth() - d.getMonth());
		}

		return Math.abs(amt);
	}

	/*
	 * Returns the number of days between this OADateTime and obj.
	 *
	 * @param obj Date, OADateTime, Calendar, etc that can be converted to an
	 *            OADateTime.
	 */
	/**
	 * Returns the number of days between this date/time and another object.
	 *
	 * @param obj an object convertible to OADateTime
	 * @return number of days between the two dates
	 */
	public int betweenDays(OADateTime d) {
		d.setTime(this.getHour(), this.getMinute(), this.getSecond(), this.getMilliSecond());

		GregorianCalendar cThis = _getCal();
		GregorianCalendar cOther = d._getCal();
		double millis = Math.abs(cThis.getTime().getTime() - cOther.getTime().getTime());
		return (int) Math.floor(millis / (1000 * 60 * 60 * 24) + .5d); // accounts for daylight savings (23hr day, or 25hr day)
	}

	/*
	 * Returns the number of hours betweeen this OADateTime and obj.
	 *
	 * @param obj Date, OADateTime, Calendar, etc that can be converted to an
	 *            OADateTime.
	 */
	/**
	 * Returns the number of hours between this date/time and another object.
	 *
	 * @param obj an object convertible to OADateTime
	 * @return number of hours between the two dates
	 */
	public int betweenHours(OADateTime d) {
		d.setTime(d.getHour(), this.getMinute(), this.getSecond(), this.getMilliSecond());

		GregorianCalendar cThis = _getCal();
		GregorianCalendar cOther = d._getCal();

		double millis = Math.abs(cThis.getTime().getTime() - cOther.getTime().getTime());

		return (int) Math.ceil(millis / (1000 * 60 * 60));
	}

	/*
	 * Returns the number of minutes betweeen this OADateTime and obj.
	 *
	 * @param obj Date, OADateTime, Calendar, etc that can be converted to an
	 *            OADateTime.
	 */
	/**
	 * Returns the number of minutes between this date/time and another object.
	 *
	 * @param obj an object convertible to OADateTime
	 * @return number of minutes between the two dates
	 */
	public int betweenMinutes(OADateTime d) {
		d.setTime(d.getHour(), d.getMinute(), this.getSecond(), this.getMilliSecond());

		GregorianCalendar cThis = _getCal();
		GregorianCalendar cOther = d._getCal();

		double millis = Math.abs(cThis.getTime().getTime() - cOther.getTime().getTime());

		return (int) Math.ceil(millis / (1000 * 60));
	}

	/*
	 * Returns the number of seconds betweeen this OADateTime and obj.
	 *
	 * @param obj Date, OADateTime, Calendar, etc that can be converted to an
	 *            OADateTime.
	 */
	/**
	 * Returns the number of seconds between this date/time and another object.
	 *
	 * @param obj an object convertible to OADateTime
	 * @return number of seconds between the two dates
	 */
	public int betweenSeconds(Object obj) {
		OADateTime d = convert(obj, true);
		d.setTime(d.getHour(), d.getMinute(), d.getSecond(), this.getMilliSecond());

		GregorianCalendar cThis = _getCal();
		GregorianCalendar cOther = d._getCal();

		double millis = Math.abs(cThis.getTime().getTime() - cOther.getTime().getTime());

		return (int) Math.ceil(millis / (1000));
	}

	/*
	 * Returns the number of seconds betweeen this OADateTime and obj.
	 *
	 * @param obj Date, OADateTime, Calendar, etc that can be converted to an
	 *            OADateTime.
	 */
	/**
	 * Returns the number of milliseconds between this date/time and another object.
	 *
	 * @param obj an object convertible to OADateTime
	 * @return number of milliseconds between the two dates
	 */
	public long betweenMilliSeconds(Object obj) {
		OADateTime d = convert(obj, false);

		GregorianCalendar cThis = _getCal();
		GregorianCalendar cOther = d._getCal();

		long millis = Math.abs(cThis.getTime().getTime() - cOther.getTime().getTime());

		return millis;
	}

	/*
	 * Time as milliseconds, same as Date.getTime()
	 */
	/**
	 * Returns the internal time value as milliseconds since the epoch.
	 *
	 * @return milliseconds since the epoch
	 */
	public long getTime() {
		return _time;
	}

	/**
	 * Converts an object to an OADateTime.
	 *
	 * @param obj the object to convert
	 * @param bAlways if true, always return a new instance
	 * @return an OADateTime instance or null if conversion is not possible
	 */
	protected OADateTime convert(Object obj, boolean bAlways) {
		if (obj == null) {
			return null;
		}

		if (obj instanceof OADateTime) {
			if (bAlways) {
				return new OADateTime((OADateTime) obj);
			} else {
				return (OADateTime) obj;
			}
		}
		if (obj instanceof java.sql.Time) {
			return new OADateTime((java.sql.Time) obj);
		}
		if (obj instanceof java.sql.Timestamp) {
			return new OADateTime((java.sql.Timestamp) obj);
		}
		if (obj instanceof Date) {
			return new OADateTime((Date) obj);
		}
		if (obj instanceof Calendar) {
			return new OADateTime((Calendar) obj);
		}
		if (obj instanceof String) {
			return new OADateTime((String) obj);
		}
		return null;
		// throw new IllegalArgumentException("OADateTime cant convert class
		// "+obj.getClass()+" to an OADateTime");
	}

	/*
	 * Static method for converting a String date to an OADateTime.<br>
	 * If date is " " (space) then todays date will be returned.<br>
	 * If date is null or "" then null is returned.<br>
	 *
	 * @param fmt format of date. If not valid, then staticParseFormats and
	 *            staticOutputFormat will be used.
	 * @return OADateTime or null
	 * @see OADateTime#setFormat
	 * @see OADateTime#valueOf to convert a string using global parse strings
	 */
	/**
	 * Converts a string to an OADateTime using the specified format.
	 *
	 * @param strDateTime the string representation of the date/time
	 * @param fmt the format to use for parsing
	 * @return an OADateTime instance or null
	 */
	public static OADateTime valueOf(String strDateTime, String fmt) {
		return valueOf(strDateTime, fmt, true);
	}

	/**
	 * Converts a string to an OADateTime using the specified format.
	 *
	 * @param strDateTime the string representation of the date/time
	 * @param fmt the format to use for parsing
	 * @return an OADateTime instance or null
	 */
	public static OADateTime valueOf(String strDateTime, String fmt, boolean bTryOtherFormats) {
		if (strDateTime == null) {
			return null;
		}
		Date d = valueOfMain(strDateTime, fmt, bTryOtherFormats ? alDateTimeParseFormat : null, bTryOtherFormats ? staticOutputFormat : null);
		if (d == null) {
			return null;
		}
		return new OADateTime(d);
	}

	/*
	 * Internally used to fix a String date.
	 */
	/**
	 * Normalizes a date string by replacing non-alphanumeric separators.
	 *
	 * @param s the input string
	 * @return the normalized date string
	 */
	protected static String fixDate(String s) {
		if (s == null) {
			return "";
		}
		int x = s.length();
		int max = (x > 3) ? 2 : 1;
		StringBuffer sb = new StringBuffer(x + 1);
		for (int i = 0, j = 0; i < x; i++) {
			char c = s.charAt(i);
			if (!Character.isLetterOrDigit(c) && j < max) {
				j++;
				c = '/';
			}
			sb.append(c);
		}
		return new String(sb);
	}

	/*
	 * Converts a String date to an OADateTime. <br>
	 * If value is " " (space) then todays date/time will be returned.<br>
	 * If value is null or "" then null is returned.<br>
	 * StaticParseFormats and staticOutputFormat will be used to try to convert.
	 *
	 * @return OADateTime or null
	 * @see OADateTime#setFormat
	 * @see #setGlobalOutputFormat
	 * @see #addGlobalParseFormat see #getGlobalParseFormats
	 * @see #valueOf(String,String)
	 */
	/**
	 * Converts a string to an OADateTime using global parse formats.
	 *
	 * @param strDateTime the string representation of the date/time
	 * @return an OADateTime instance or null
	 */
	public static OADateTime valueOf(String strDateTime) {
		return valueOf(strDateTime, null);
	}

	/**
	 * Internal method used to parse a string into a Date using multiple formats.
	 *
	 * @param value the string to parse
	 * @param inputFormat the preferred input format
	 * @param alFormat collection of fallback parse formats
	 * @param outputFormat fallback output format
	 * @return a Date instance or null
	 */
	protected static Date valueOfMain(String value, String inputFormat, List<String> alFormat, String outputFormat) {
		if (value == null || value.length() == 0) {
			return null;
		}
		if (value.equals(" ")) {
			return new Date();
		}

		String format = null;
		if (inputFormat != null) {
			// Convert 4 digit year to 2 digit. Otherwise, a 2 digit year input will be
			// wrong. ex: 1/1/65 -> 01/01/0065
			String s = inputFormat.toUpperCase();
			int pos = s.indexOf("YYYY");
			if (pos >= 0) {
				if (value.length() != inputFormat.length()) {
					format = inputFormat.substring(0, pos) + inputFormat.substring(pos + 2);
				}
			}
		}

		Date date = null;
		int x = alFormat == null ? 0 : alFormat.size();

		int j = (format == null) ? -1 : -2;
		for (; j <= x && date == null; j++) {
			if (j == -1) {
				format = inputFormat;
			}
			else if (j >= 0) {
				if (j < x) {
					format = (String) alFormat.get(j);
				} else {
					format = outputFormat;
				}
			}
			if (format != null && format.length() > 0) {
				SimpleDateFormat sdf = getFormatter();
				sdf.setTimeZone(getDefaultTimeZone());
				synchronized (sdf) {
					sdf.applyPattern(format);
					try {
						ParsePosition pos = new ParsePosition(0);
						date = sdf.parse(value, pos);
						if (date != null && pos.getIndex() == value.length()) {
							break;
						}
						
					} catch (Exception e) {
						int xx = 3;
						xx++;
					}
				}
			}
		}
		return date;
	}

	/*
	 * Converts OADateTime to a String using specified formatting String.<br>
	 * Uses the first format that has been set: "format", "staticOutputFormat" else
	 * or "yyyy-MMM-dd hh:mma"
	 */
	/**
	 * Converts this date/time to a String using the configured or default format.
	 *
	 * @return the formatted date/time string
	 */
	public String toString() {
		return toString(null);
	}

	/*
	 * Converts OADateTime to a String using specified formatting String.
	 *
	 * @param f is format to apply
	 */
	/**
	 * Converts this date/time to a String using the specified format.
	 *
	 * @param f the format to apply
	 * @return the formatted date/time string
	 */
	public String toString(String f) {
		if (f == null) {
			f = (format == null) ? staticOutputFormat : format;
			if (f == null || f.length() == 0) {
				f = "yyyy-MMM-dd hh:mma";
				if (timeZone != null)
					f += " z";
			}
		}
		return toStringMain(f);
	}

	// main method called to get string value
	/**
	 * Performs the actual formatting of this date/time using the specified format.
	 *
	 * @param format the format to apply
	 * @return the formatted date/time string
	 */
	protected String toStringMain(String format) {
		if (format == null || format.length() == 0) {
			return getDate().toString();
		}
		String s;
		SimpleDateFormat sdf = getFormatter();
		synchronized (sdf) {
			sdf.applyPattern(format);
			sdf.setTimeZone(getTimeZone());
			s = sdf.format(getDate());
		}
		return s;
	}

	/**
	 * Sets the global output format used when converting date/time values to strings.
	 *
	 * @param fmt the global output format
	 */
	public static void setGlobalOutputFormat(String fmt) {
		staticOutputFormat = fmt;
	}

	/**
	 * Sets the global output format used when converting date/time values to strings.
	 *
	 * @param fmt the global output format
	 */
	public static String getGlobalOutputFormat() {
		return staticOutputFormat;
	}

	/*
	 * Add additional global parse formats that are used when converting a String to
	 * OADateTime.
	 *
	 * @see #setFormat
	 */
	/**
	 * Adds a global parse format used when converting strings to date/time values.
	 *
	 * @param fmt the parse format to add
	 */
	public static void addGlobalParseFormat(String fmt) {
		alDateTimeParseFormat.add(fmt);
	}

	/**
	 * Removes a global parse format.
	 *
	 * @param fmt the parse format to remove
	 */
	public static void removeGlobalParseFormat(String fmt) {
		alDateTimeParseFormat.remove(fmt);
	}

	/**
	 * Set format to use for this OADateTime This format will be used when
	 * converting this datetime to a String, unless a format is specified when
	 * calling toString.
	 * 
	 * <pre>
	    Formatting:
	
	    MM/dd HH:mm:ss
	    MM/dd/yy HH:mm:ss
	    MM/dd/yyyy HH:mm:ss
	
	    'Hms', 'Mdy'
	
	    yyyyMMdd_HHmmss.SSS
	             hhmmssa
	
	    JSON / XML
	    	format – ISO 8601
				2014-03-12T13:37:27+00:00
				"yyyy-MM-dd'T'HH:mm:ssZ"
				also...
				"yyyy-MM-dd'T'HH:mm:sszzz"
	
	        yyyy-MM-dd'T'HH:mm:ssX
	            Notice the X on the end. It will handle timezones in ISO 8601 standard
	            see: http://stackoverflow.com/questions/19112357/java-simpledateformatyyyy-mm-ddthhmmssz-gives-timezone-as-ist
	            ex: 2016-11-22T08:49:02-05
	        yyyy-MM-dd'T'HH:mm:ssXX
	            ex: 2016-11-22T08:50:12-0500
	        yyyy-MM-dd'T'HH:mm:ssXXX
	            ex: 2016-11-22T08:49:02-05:00
	
	    javascript Date.toString()    EEE MMM dd yyyy '00:00:00' 'GMT'Z '('z')'
	
	    XSD dateTime
	        [-]CCYY-MM-DDThh:mm:ss[Z|(+|-)hh:mm]
	        The time zone may be specified as Z (UTC) or (+|-)hh:mm. Time zones that aren't specified are considered undetermined.
	       => yyyy-MM-dd'T'HH:mm:ss      -> 2001-10-26T21:32:52
	       => yyyy-MM-dd'T'HH:mm:ssXXX   -> 2001-10-26T21:32:52+02:00
	       => yyyy-MM-dd'T'HH:mm:ss'Z'  ->  2001-10-26T19:32:52Z   (UTZ)
	
	    <p>
	    Formatting Symbols used for output display.
	
	    SEE: https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
	
	    G  era designator          (Text)              AD
	    y  year                    (Number)            1996
	    M  month in year           (Number)            1, 2, 3, 4 .. 10, 11, 12
	    MM                         (Number)            01, 02, 03, 04 ... 10, 11, 12
	    MMM                        (Text)              Jan, Feb, ... Dec
	    MMMM                       (Text)              January, February, ... December
	    d  day in month            (Number)            10
	    h  hour in am/pm (1~12)    (Number)            12
	    H  hour in day (0~23)      (Number)            0
	    m  minute in hour          (Number)            30
	    s  second in minute        (Number)            55
	    S  millisecond             (Number)            978
	    E  day in week             (Text)              Tues
	    EE
	    EEE
	    EEEE  day in week          (Text)              Tuesday
	    D  day in year             (Number)            189
	    F  day of week in month    (Number)            2 (2nd Wed in July)
	    w  week in year            (Number)            27
	    W  week in month           (Number)            2
	    a  am/pm marker            (Text)              PM
	    k  hour in day (1~24)      (Number)            24
	    K  hour in am/pm (0~11)    (Number)            0
	    z  time zone               (Text)              PST (might not use Abbrev anymore, andthis would be offset amount instead)
	    zzzz                                           Pacific Standard Time
	    zz                         (UTC offset)        -05 
	    X                          (hours)             -04
	    XX                         (hrsMins)           -0400
	    XXX                        (hrs:mins)          -04:00
	    Z                          (hrsMins)           -0400
	    ZZ   (same as using Z)                         -0400
	    ZZZ  (same as using Z)                         -0400
	
	    '  escape for text         (Delimiter)
	
	    '' single quote            (Literal)           '
	
	    Examples:
	    "yyyy.MM.dd G 'at' HH:mm:ss z"    ->>  1996.07.10 AD at 15:08:56 PDT
	    "EEE, MMM d, ''yy"                ->>  Wed, July 10, '96
	    "h:mm a"                          ->>  12:08 PM
	    "hh 'o''clock' a, zzzz"           ->>  12 o'clock PM, Pacific Daylight Time
	    "K:mm a, z"                       ->>  0:00 PM, PST
	    "yyyy.MMMMM.dd GGG hh:mm aaa"     ->>  1996.July.10 AD 12:08 PM
	    "yyyy.MM.dd HH:mm:ss.SSS"
	
	    "E dd M yyyy hh:mm:ss a z"          ->> Thu 30 3 2017 11:58:21 AM EDT
	    "EE dd MM yyyy hh:mm:ss a zz"       ->> Thu 30 03 2017 11:58:52 AM EDT
	    "EEE dd MMM yyyy hh:mm:ss a zzz"    ->> Thu 30 Mar 2017 11:59:35 AM EDT
	    "EEEE dd MMMM yyyy hh:mm:ss a zzzz" ->> Thursday 30 March 2017 12:00:33 PM Eastern Daylight Time
	 * </pre>
	 * 
	 * <br>
	 * 
	 * @see #setGlobalOutputFormat
	 * @see java.text.SimpleDateFormat
	 */
	public void setFormat(String fmt) {
		this.format = fmt;
	}

	/**
	 * Returns the instance-specific format used when converting this date/time to a string.
	 *
	 * @return the instance format
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * Returns a SimpleDateFormat instance.
	 *
	 * @return SimpleDateFormat instance
	 */
	protected static SimpleDateFormat getFormatter() {
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.setLenient(false);
		return sdf;
	}

	/**
	 * Returns the format string to use for system format.
	 *
	 * @param type DateFormat.SHORT, MEDIUM, LONG, FULL, DEFAULT
	 */
	public static String getFormat(int type) {
		return getFormat(type, locale);
	}

	/*
	 * Returns the format string to use for system format.
	 *
	 * @param type DateFormat.SHORT, MEDIUM, LONG, FULL, DEFAULT
	 */
	/**
	 * Returns a date/time format string for the specified DateFormat style and locale.
	 *
	 * @param style the DateFormat style constant
	 * @param loc the Locale to use
	 * @return the format string
	 */
	public static String getFormat(int type, Locale locale) {
		DateFormat df = DateFormat.getDateInstance(type, locale);
		if (df instanceof SimpleDateFormat) {
			String s = ((SimpleDateFormat) df).toPattern();
			return s;
		}
		return null;
	}

	/**
	 * Determines whether this date/time is on the last day of its month.
	 *
	 * @return true if {@link #getDay()} equals {@link #getDaysInMonth()}; otherwise false
	 */
	public boolean isLastDayOfMonth() {
		return getDay() == getDaysInMonth();
	}

	/**
	 * Determines whether this date/time falls on the first occurrence of the
	 * specified weekday within the current month.
	 *
	 * @param weekday the Calendar day-of-week constant to test against
	 * @return true if this date is within the first seven days of the month and
	 *         its day-of-week matches the specified value; otherwise false
	 */
	public boolean isFirstWeekDayOfMonth(int weekday) {
		int day = getDay();
		if (day > 7) {
			return false;
		}
		return (getDayOfWeek() == weekday);
	}

	/**
	 * Determines whether this date/time falls on the last occurrence of the
	 * specified weekday within the current month.
	 *
	 * @param weekday the Calendar day-of-week constant to test against
	 * @return true if this date is within the last seven days of the month and
	 *         its day-of-week matches the specified value; otherwise false
	 */
	public boolean isLastWeekDayOfMonth(int weekday) {
		int d = getDay();
		if (d + 7 <= getDaysInMonth()) {
			return false;
		}
		return (getDayOfWeek() == weekday);
	}

	/**
	 * Returns the day-of-month for the last occurrence of the specified weekday
	 * within the current month.
	 *
	 * @param weekday the Calendar day-of-week constant to locate
	 * @return the day-of-month for the last matching weekday, or -1 if not found
	 */
	public int getLastWeekDayOfMonth(int weekday) {
		OADateTime dt = new OADateTime(this);
		int x = getDaysInMonth();
		for (int i = 0; i < 7; i++) {
			dt.setDay(x - i);
			if (dt.getDayOfWeek() == weekday) {
				return (x - i);
			}
		}
		return -1; // error
	}

	/**
	 * Returns the day-of-month for the first occurrence of the specified weekday
	 * within the current month.
	 *
	 * @param weekday the Calendar day-of-week constant to locate
	 * @return the day-of-month for the first matching weekday, or -1 if not found
	 */
	public int getFirstWeekDayOfMonth(int weekday) {
		OADateTime dt = new OADateTime(this);
		for (int i = 0; i < 7; i++) {
			dt.setDay(i + 1);
			if (dt.getDayOfWeek() == weekday) {
				return (i + 1);
			}
		}
		return -1; // error
	}

	
	// Immutable methods 20260603 qqqqqqqqqqqqqqqqqqqqqqq
	
	
	/**
	 * Returns a copy with a different year.
	 */
	public OADateTime withYear(int year) {
	    OADateTime dt = new OADateTime(this);
	    dt.setYear(year);
	    return dt;
	}

	/**
	 * Returns a copy with a different month (Calendar semantics: 0-11).
	 */
	public OADateTime withMonth(int month) {
	    OADateTime dt = new OADateTime(this);
	    dt.setMonth(month);
	    return dt;
	}

	/**
	 * Returns a copy with a different day.
	 */
	public OADateTime withDay(int day) {
	    OADateTime dt = new OADateTime(this);
	    dt.setDay(day);
	    return dt;
	}

	/**
	 * Returns a copy with a different hour (24-hour clock).
	 */
	public OADateTime withHour(int hour) {
	    OADateTime dt = new OADateTime(this);
	    dt.setHour(hour);
	    return dt;
	}

	/**
	 * Returns a copy with a different minute.
	 */
	public OADateTime withMinute(int minute) {
	    OADateTime dt = new OADateTime(this);
	    dt.setMinute(minute);
	    return dt;
	}

	/**
	 * Returns a copy with a different second.
	 */
	public OADateTime withSecond(int second) {
	    OADateTime dt = new OADateTime(this);
	    dt.setSecond(second);
	    return dt;
	}

	/**
	 * Returns a copy with a different millisecond.
	 */
	public OADateTime withMilliSecond(int ms) {
	    OADateTime dt = new OADateTime(this);
	    dt.setMilliSecond(ms);
	    return dt;
	}	
	
	
	/**
	 * Returns a copy with a different date.
	 */
	public OADateTime withDate(int year, int month, int day) {
	    OADateTime dt = new OADateTime(this);
	    dt.setDate(year, month, day);
	    return dt;
	}

	/**
	 * Returns a copy with a different time.
	 */
	public OADateTime withTime(int hour, int minute) {
	    OADateTime dt = new OADateTime(this);
	    dt.setTime(hour, minute);
	    return dt;
	}

	public OADateTime withTime(int hour, int minute, int second) {
	    OADateTime dt = new OADateTime(this);
	    dt.setTime(hour, minute, second);
	    return dt;
	}

	public OADateTime withTime(int hour, int minute, int second, int millisecond) {
	    OADateTime dt = new OADateTime(this);
	    dt.setTime(hour, minute, second, millisecond);
	    return dt;
	}
	
	
	/**
	 * Returns a copy with a different timezone while preserving wall-clock fields.
	 */
	public OADateTime withTimeZone(TimeZone tz) {
	    OADateTime dt = new OADateTime(this);
	    dt.setTimeZone(tz);
	    return dt;
	}


	public final static int SUNDAY = 1;
	public final static int SUN = 1;
	public final static int MONDAY = 2;
	public final static int MON = 2;
	public final static int TUESDAY = 3;
	public final static int TUES = 3;
	public final static int TUE = 3;
	public final static int WEDNESDAY = 4;
	public final static int WED = 4;
	public final static int THURSDAY = 5;
	public final static int THURS = 5;
	public final static int THU = 5;
	public final static int FRIDAY = 6;
	public final static int FRI = 6;
	public final static int SATURDAY = 7;
	public final static int SAT = 7;

	public final static int JANUARY = 0;
	public final static int JAN = 0;
	public final static int FEBRUARY = 1;
	public final static int FEB = 1;
	public final static int MARCH = 2;
	public final static int MAR = 2;
	public final static int APRIL = 3;
	public final static int APR = 3;
	public final static int MAY = 4;
	public final static int JUNE = 5;
	public final static int JUN = 5;
	public final static int JULY = 6;
	public final static int JUL = 6;
	public final static int AUGUST = 7;
	public final static int AUG = 7;
	public final static int SEPTEMBER = 8;
	public final static int SEPT = 8;
	public final static int SEP = 8;
	public final static int OCTOBER = 9;
	public final static int OCT = 9;
	public final static int NOVEMBER = 10;
	public final static int NOV = 10;
	public final static int DECEMBER = 11;
	public final static int DEC = 11;


	
	
	
	public static void main2(String[] args) throws Exception {
		OADateTime dt;
		SimpleDateFormat sdf;
		String sx;

		sx = (new OADateTime()).toString("yyyy-MM-dd-HH.mm.ss.SSSSSS");
		Thread.sleep(1);
		String sx2 = (new OADateTime()).toString("yyyy-MM-dd-HH.mm.ss.SSSSSS");

		String[] tzs = TimeZone.getAvailableIDs();
		for (String s : tzs) {
			TimeZone tz = TimeZone.getTimeZone(s);
			int xx = 0;
			xx++;
		}

		String[] ids = TimeZone.getAvailableIDs();
		for (String id : ids) {
			TimeZone zone = TimeZone.getTimeZone(id);
			int offset = zone.getRawOffset() / 1000;
			int hour = offset / 3600;
			int minutes = (offset % 3600) / 60;
			System.err.println(String.format("(GMT%+d:%02d) %s", hour, minutes, id));
		}

		dt = new OADateTime().addDays(3);
		String msg1 = dt.toString("yyyy-MM-dd'T'HH:mm:ssZ"); // 2019-11-08T20:31:21-0500
		String msg2 = dt.toString("yyyy-MM-dd'T'HH:mm:ssXXX"); // 2019-11-08T20:31:21-05:00

		sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// or SimpleDateFormat sdf = new SimpleDateFormat( "MM/dd/yyyy KK:mm:ss a Z" );
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		String s = sdf.format(new Date());
		System.out.println(s);

		sx = (new OADateTime()).toString("yyyy-MM-dd'T'HH:mm:ss.S"); // 2019-08-26T15:47:40.902

		OADate d = new OADate("02/22/2019");
		OADate today = new OADate();
		int x = d.compareTo(today);

		System.out.println(d + ", today=" + today + ", x=" + x);
		int xx = 4;
		xx++;

		/*
		 * for (int i=0; i<1;i++) { final int id = i; Thread t = new Thread() {
		 * 
		 * @Override public void run() { test(id); } }; t.start(); }
		 */
		// test(777);
	}

	public static void main(String[] args) throws Exception {
		OADateTime dt;
		String s, sx;

		sx = "2023-09-08T14:15:47.034Z";
		dt = new OADateTime(sx, OADateTime.RFC339FormatWms);
		s = dt.toString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

		if (dt != null)
			dt.setTimeZoneUTC();

		s = dt.toString("yyyy-MM-dd'T'HH:mm:ss.SSS z");

		dt = dt.convertToUTC();
		s = dt.toString("yyyy-MM-dd'T'HH:mm:ss.SSS z");

		dt = dt.convertTo(OATimeZone.getLocalTimeZone());
		s = dt.toString("yyyy-MM-dd'T'HH:mm:ss.SSS z");

		// "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

		sx = "2023-09-08T14:15:47.34Z";
		dt = new OADateTime(sx, OADateTime.RFC339FormatWms);
		s = dt.toString(OADateTime.RFC339FormatWms);

		dt = OADateTime.valueOf(sx, "yyyy-MM-dd'T'HH:mm:ss.S'Z'", false);
		s = dt.toString(OADateTime.RFC339FormatWms);

		sx = "2023-09-08T14:15:47Z";
		dt = new OADateTime(sx, OADateTime.RFC339Format);

		dt = OADateTime.valueOf(sx, "yyyy-MM-dd'T'HH:mm:ss'Z'", false);

		/*
		 * dt.setTimeZone(OATimeZone.getTimeZoneUTC());
		 * 
		 * String sz = dt.toString(OADateTime.FORMAT_xlong);//qqqqq sz =
		 * dt.toString(OADateTime.RFC339FormatWms);//qqqqq
		 * 
		 * OADateTime dtz = dt.convertTo(OATimeZone.getLocalTimeZone()); sz =
		 * dtz.toString(OADateTime.FORMAT_xlong);//qqqqq
		 * 
		 * dtz = new OADateTime(sz, "yyyy/MM/dd hh:mm:ss.S a z"); sz =
		 * dtz.toString(OADateTime.FORMAT_xlong);//qqqqq
		 */

		int xx = 4;
		xx++;
	}

	public static void test(int id) {
		for (int i = 0;; i++) {
			OADate dx = new OADate(1980 + ((int) (Math.random() * 50)), (int) (Math.random() * 12), (int) (Math.random() * 28));
			dx = (OADate) dx.addDays(1);
			// dx = (OADate) dx.addMilliSeconds( (int) (Math.random() * (24*60*60*1000)) );
			if (i % 25000 == 0) {
				System.out.println(id + ") " + i + "   " + dx);
			}
		}
	}
}
