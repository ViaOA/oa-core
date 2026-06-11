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

import java.text.DateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

import com.viaoa.datetime.OADateTime.DateTimeType;

/* CODEX Review
  
Summary Assessment

  No serious OADate correctness issues found.

  The core date-only invariants are mostly coherent now: constructors normalize to midnight, type is normally Floating, zoneId is captured,
  parsing wraps through OADate, and inherited withXxx/arithmetic methods generally route through createUtil and return OADate.

  The remaining risks are semantic edge cases rather than obvious breakages. The biggest concern is that inherited withType(...) can
  intentionally produce an OADate whose type is not Floating, which conflicts with the stated OADate invariant. The next meaningful risk is
  serialization: OADate relies on OADateTime’s private custom serialization and has no subclass-specific read validation.

  Critical

  None.

  High
1. withType(DateTimeType) inherited from OADateTime

  Why it matters:
  OADate’s intended invariant says type should be DateTimeType.Floating, but inherited withType(...) allows callers to create an OADate with
  Instant, ZonedInstant, or null type. It still remains date-only because createUtil(getZonedDateTime()) returns an OADate, but semantic
  type is no longer date-only/Floating.

  Example failure scenario:

  OADate d = new OADate(2026, 6, 9);
  OADateTime x = d.withType(DateTimeType.Instant);

  x is an OADate, time is midnight, but x.getType() is Instant. Serialization now follows OADateTime Instant serialization and writes _time
  as authoritative instead of date fields.

  Recommended fix:
  Override withType(DateTimeType) in OADate to either:

  - always return a Floating OADate and ignore/reject non-Floating types, or
  - throw IllegalArgumentException for anything other than DateTimeType.Floating.

  If non-Floating OADate is intentionally supported, then weaken the invariant: “Normal OADate construction uses Floating; withType may
  deliberately override type.”
  
1. Null handling is inconsistent across constructors

  Method/location:

  - OADate(OADateTime dt)
  - OADate(Calendar c)
  - OADate(LocalDate ld)

  Why it matters:
  Some constructors handle null (OADate(Date)), while others throw NullPointerException. OADateTime constructors often treat null as current
  value. This makes call sites harder to reason about and can create migration bugs.

  Example failure scenario:

  OADateTime maybeDateTime = null;
  new OADate(maybeDateTime); // NPE

  Recommended fix:
  Choose one policy and enforce it consistently:

  - If null means “today,” mirror OADate(Date) and OADateTime null constructor behavior.
  - If null is invalid, use Objects.requireNonNull(...) with a clear message.


  
 2. OADate no-arg and null-Date constructors use JVM default date, not OA default zone

  Method/location:

  - OADate()
  - OADate(Date date) when date == null

  Why it matters:
  OADate() calls LocalDate.now() and OADate(Date null) uses LocalDate.now(), both using the JVM default zone. Other OADate paths use
  OADateTime.defaultZoneId to derive local date and midnight. If JVM default and OA default zone differ near midnight, “today” can be
  different.

  Example failure scenario:
  JVM default is UTC, OA default is America/New_York, current instant is 2026-06-10T02:00Z.

  - JVM LocalDate.now() => June 10
  - OA default local date => June 9

  Recommended fix:
  Use LocalDate.now(defaultZoneId) in OADate() and null-Date handling.

  3. createUtil(long time, ZoneId zid) maps date in target zone but then re-normalizes in default zone

  Method/location:
  OADate.createUtil(long time, ZoneId zid)

  Why it matters:
  This matches the stated intended semantics, but it is subtle. withZoneIdSameInstant(CHICAGO) uses Chicago to pick the calendar date, then
  creates an OADate whose zoneId is defaultZoneId, not Chicago. This can surprise callers who expect zone conversion methods to preserve the
  target zone consistently.

  Example failure scenario:

  OADate d = new OADate(2026, 6, 9); // default New York
  OADateTime c = d.withZoneIdSameInstant(CHICAGO);
  c.zoneId; // defaultZoneId, not CHICAGO

  Recommended fix:
  If this is intended, document it explicitly near createUtil(long, ZoneId) and tests. If it is not intended, assign d.zoneId = zid and
  recompute _time at midnight in zid, consistent with the other createUtil overloads.

  Low

  1. dateValue(...) casts the result of OADate.valueOf(...)

  Method/location:

  - dateValue(String)
  - dateValue(String, String)

  Why it matters:
  Currently safe because OADate.valueOf(...) wraps successful parses into new OADate(dt). But the cast is brittle if future refactoring
  changes valueOf(...) to return a different subtype or plain OADateTime.

  Example failure scenario:
  A future change returns OADateTime directly from valueOf(...); dateValue(...) starts throwing ClassCastException.

  Recommended fix:
  Change return path to:

  OADateTime dt = OADate.valueOf(...);
  return (dt == null) ? null : new OADate(dt);

  or change valueOf return type to OADate if source compatibility allows.

  2. Global parse formats are mutable and unsynchronized

  Method/location:

  - alDateParseFormat
  - setLocale
  - addGlobalParseFormat
  - removeGlobalParseFormat

  Why it matters:
  Concurrent parsing while another thread changes locale or parse formats can see inconsistent state. This is likely consistent with the
  rest of OA date/time globals, but it is still a regression risk for server code.

  Example failure scenario:
  One thread calls OADate.setLocale(...) while another parses with OADate.valueOf(...); the parser observes the list mid-change.

  Recommended fix:
  Use copy-on-write list replacement or synchronize static parse format mutations and reads.

  3. OADate can format time fields if caller supplies a time pattern

  Method/location:
  toString(String f)

  Why it matters:
  The value remains date-only, but toString("yyyy-MM-dd HH:mm:ss") emits midnight time fields. This is technically correct because fields
  are normalized, but if “formatting is date-only” means time fields should never be emitted, explicit patterns can violate that
  presentation rule.

  Example failure scenario:

  new OADate(2026, 6, 9).toString("yyyy-MM-dd HH:mm:ss");
  // "2026-06-09 00:00:00"

  Recommended fix:
  No code change needed if explicit format means caller controls output. If strict date-only presentation is required, validate/reject time
  fields in OADate format strings.
 
  
  

 */

/**
 * Date-only OA value.
 * <p>
 * OADate is a specialization of {@link OADateTime} whose time portion is always
 * normalized to {@code 00:00:00.000}. It represents a calendar date rather than
 * a precise timestamp.
 *
 * <h3>Semantics</h3>
 * OADate uses {@link DateTimeType#Floating} semantics. The business meaning is
 * the local calendar date. Internally, the underlying epoch-millisecond value is
 * derived from midnight of that date in the active/default OA timezone.
 *
 * <h3>Immutability</h3>
 * OADate is immutable. Operations inherited from {@link OADateTime}, including
 * {@code withXxx(...)} and arithmetic methods such as {@code plusDays(...)},
 * return new OADate instances through the protected {@code createUtil(...)}
 * factory methods.
 *
 * <h3>Parsing and formatting</h3>
 * Parsing uses OADate-specific formats and normalizes results to date-only
 * values. Formatting uses either an instance format, the global OADate output
 * format, or a built-in fallback format.
 *
 * @see OADateTime
 * @see OATime
 * @see LocalDate
 */
public class OADate extends OADateTime {
	private static final long serialVersionUID = 1L;

	/**
	 * Standard ISO-style date format using year-month-day order.
	 */
	public final static String Format1 = "yyyy-MM-dd";

	/**
	 * Standard US-style date format using month/day/year order.
	 */
	public final static String Format2 = "MM/dd/yyyy";
	
	/**
	 * Compact date format without separators.
	 */
	public final static String Format3 = "yyyyMMdd";
	
	/**
	 * Date format using an abbreviated month name.
	 */
	public final static String Format4 = "yyyy-MMM-dd";
	
	/**
	 * Date format using an abbreviated month name, day, comma, and full year.
	 */
	public final static String Format5 = "MMM dd, yyyy";
	
	/**
	 * Date format using an abbreviated month name and two-digit year.
	 */
	public final static String Format6 = "MMM dd, yy";

	/**
	 * Date format used for JDBC and SQL interactions.
	 */
	public final static String JdbcFormat = "yyyy-MM-dd"; // SQL
	
	/**
	 * Date format used for JSON serialization.
	 */
	public final static String JsonFormat = Format1;

    // format used by browser: "YYYY-MM-DD";
    // same as JsonFormat
    // public final static String HtmlInputDateFormat = "yyyy-MM-dd"; // java format to use
    
    
	// Unique for this subclass
	/**
	 * Global OADate output format used by {@link #toString()} when this instance
	 * does not have an instance-specific format.
	 */
	protected static String dateOutputFormat;

	/**
	 * Ordered fallback parse formats used by OADate parsing methods.
	 */
	private static final List<String> alDateParseFormat = new ArrayList<>();

	static {
		setLocale(Locale.getDefault());
	}

	/**
	 * Sets the global OADate output format used by {@link #toString()}.
	 *
	 * @param fmt date format pattern, or {@code null} to allow fallback behavior
	 */
	public static void setGlobalOutputFormat(String fmt) {
		dateOutputFormat = fmt;
	}

	/**
	 * Returns the global OADate output format.
	 *
	 * @return current global output format, or {@code null} if no format is set
	 */
	public static String getGlobalOutputFormat() {
		return dateOutputFormat;
	}

	/**
	 * Adds a global fallback parse format for OADate values.
	 *
	 * @param fmt date format pattern to add
	 */
	public static void addGlobalParseFormat(String fmt) {
		alDateParseFormat.add(fmt);
	}

	/**
	 * Removes a global fallback parse format for OADate values.
	 *
	 * @param fmt date format pattern to remove
	 */
	public static void removeGlobalParseFormat(String fmt) {
		alDateParseFormat.remove(fmt);
	}
	
	/**
	 * Rebuilds locale-specific OADate parse and output formats.
	 * <p>
	 * Passing {@code null} uses {@link Locale#getDefault()}.
	 *
	 * @param loc locale used to derive default date formats
	 */
	public static void setLocale(Locale loc) {
		if (loc == null) loc = Locale.getDefault();
		alDateParseFormat.clear();
		String s = getFormat(DateFormat.SHORT, loc);
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
			alDateParseFormat.add(s);
		}
		if (bMonthFirst) {
			alDateParseFormat.add("MM/dd/yy"); // must be before "MM/dd/yyyy" since "MM/dd/yyyy" will convert 5/4/65 -> 05/04/0065
			alDateParseFormat.add("MM/dd/yyyy");
			dateOutputFormat = "MM/dd/yyyy";
		} else if (bYearFirst) {
			alDateParseFormat.add("yy/MM/dd"); // must be before "MM/dd/yyyy" since "MM/dd/yyyy" will convert 5/4/65 -> 05/04/0065
			alDateParseFormat.add("yyyy/MM/dd");
			dateOutputFormat = "yyyy/MM/dd";
		} else { // day first
			alDateParseFormat.add("dd/MM/yy");
			alDateParseFormat.add("dd/MM/yyyy");
			dateOutputFormat = "dd/MM/yyyy";
		}
		// SQL date formats
		alDateParseFormat.add("yyyy-MM-dd");
	}

	/**
	 * Creates an OADate initialized to the current local date.
	 */
	public OADate() {
		this(LocalDate.now());
	}

	/**
	 * Creates an OADate from an epoch-millisecond value.
	 * <p>
	 * The instant is interpreted in the current OA default zone to determine the
	 * calendar date, then normalized to midnight.
	 *
	 * @param time milliseconds from the Java epoch
	 */
	public OADate(long time) {
		this(new Date(time));
	}
	
	
	/**
	 * Creates an OADate from a legacy {@link Date}.
	 * <p>
	 * The supplied instant is interpreted in the OA default zone to determine the
	 * local calendar date. If {@code date} is {@code null}, the current local date
	 * is used.
	 *
	 * @param date source date, or {@code null} for today
	 */
	public OADate(Date date) {
	    LocalDate ld;

	    if (date == null) {
	        ld = LocalDate.now();
	    } 
	    else {
	        ld = Instant.ofEpochMilli(date.getTime()).atZone(defaultZoneId).toLocalDate();
	    }

	    this.type = DateTimeType.Floating;
	    this.zoneId = defaultZoneId;
	    this._time = ld.atStartOfDay().atZone(this.zoneId).toInstant().toEpochMilli();
	}
	
	
	/**
	 * Creates an OADate from explicit date fields.
	 *
	 * @param year full year
	 * @param month month from 1 to 12
	 * @param day day of month
	 */
	public OADate(int year, int month, int day) {
		super(year, month, day);
	    this.type = DateTimeType.Floating;
	    this.zoneId = defaultZoneId;
	}
	
	/**
	 * Creates an OADate from another OA date/time value.
	 * <p>
	 * Only the source local year, month, and day fields are retained. Time-of-day
	 * information is discarded and the result is normalized to midnight.
	 *
	 * @param dt source date/time value
	 */
	public OADate(OADateTime dt) {
		super(dt.getYear(), dt.getMonthValue(), dt.getDayOfMonth());
	    this.type = DateTimeType.Floating;
	    this.zoneId = defaultZoneId;
	}
	
	/**
	 * Creates an OADate from a legacy {@link Calendar}.
	 * <p>
	 * The calendar year, month, and day fields are retained and normalized to an
	 * OADate value.
	 *
	 * @param c source calendar
	 */
	public OADate(Calendar c) {
		super(c.get(c.YEAR), c.get(c.MONTH) + 1, c.get(c.DATE));
	    this.type = DateTimeType.Floating;
	    this.zoneId = defaultZoneId;
	}

	/**
	 * Creates an OADate from a {@link LocalDate}.
	 *
	 * @param ld source local date
	 */
	public OADate(LocalDate ld) {
		super(ld.getYear(), ld.getMonthValue(), ld.getDayOfMonth());
	    this.type = DateTimeType.Floating;
	    this.zoneId = defaultZoneId;
	}
	
	/**
	 * Parses text into an OADate using OADate parse formats.
	 *
	 * @param strDate text to parse
	 * @throws IllegalArgumentException if the text can not be parsed
	 */
	public OADate(String strDate) {
		this(strDate, null);
	    this.type = DateTimeType.Floating;
	    this.zoneId = defaultZoneId;
	}

	/**
	 * Parses text into an OADate using a preferred format.
	 * <p>
	 * Parsing is delegated to OADate parsing methods, then normalized to a
	 * date-only value.
	 *
	 * @param strDate text to parse
	 * @param format preferred parse pattern
	 * @throws IllegalArgumentException if the text can not be parsed
	 */
	public OADate(String strDate, String format) {
		OADateTime dt = OADate.valueOf(strDate, format);
		if (dt == null) throw new IllegalArgumentException("OADate cant create date from String \"" + strDate + "\", format="+format);
		LocalDate ld = LocalDate.of(dt.getYear(), dt.getMonthValue(), dt.getDayOfMonth());
		
	    this.type = DateTimeType.Floating;
	    this.zoneId = defaultZoneId;
	    this._time = ld.atStartOfDay().atZone(this.zoneId).toInstant().toEpochMilli();
	}
	
	/**
	 * Returns a canonical OADate.
	 * <p>
	 * OADate always uses {@link DateTimeType#Floating} semantics. Any requested
	 * type conversion is ignored and the returned value is normalized back to a
	 * standard OADate instance.
	 * <p>
	 * This preserves OADate invariants:
	 * <ul>
	 *   <li>Date-only semantics</li>
	 *   <li>Time normalized to {@code 00:00:00.000}</li>
	 *   <li>{@link DateTimeType#Floating}</li>
	 * </ul>
	 *
	 * @param type requested type (ignored)
	 * @return normalized OADate using Floating semantics
	 */
	@Override
	public OADateTime withType(DateTimeType type) {
	    return new OADate(this);
	}
	
	/**
	 * Formats this OADate using an explicit format.
	 * <p>
	 * When {@code f} is {@code null}, this method falls back to the instance format,
	 * the global OADate output format, and then the built-in fallback format.
	 *
	 * @param f explicit format pattern, or {@code null} to use fallback selection
	 * @return formatted date text
	 */
	@Override
	public String toString(String f) {
		if (f == null) {
			f = (format == null) ? dateOutputFormat : format;
			if (f == null || f.length() == 0) {
				f = "yyyy-MMM-dd";
			}
		}
		return toStringMain(f);
	}

	/**
	 * Parses text into an OADate using a preferred format.
	 *
	 * @param date text to parse
	 * @param fmt preferred parse pattern
	 * @return parsed OADate, or {@code null} if parsing fails
	 */
	public static OADate dateValue(String date, String fmt) {
		return (OADate) OADate.valueOf(date, fmt);
	}

	/**
	 * Parses text into an OADate using default OADate parsing rules.
	 *
	 * @param date text to parse
	 * @return parsed OADate, or {@code null} if parsing fails
	 */
	public static OADate dateValue(String date) {
		return (OADate) OADate.valueOf(date, null);
	}

	/**
	 * Creates a date-only result from a calculated {@link ZonedDateTime}.
	 * <p>
	 * The local date from the supplied value is retained while the time portion is
	 * discarded. The resulting value is normalized to midnight.
	 *
	 * @param zdt calculated zoned date/time
	 * @return OADate normalized from the supplied local date
	 */
	@Override
	protected OADateTime createUtil(ZonedDateTime zdt) {
	    ZoneId zid = zdt.getZone();
	    LocalDate ld = zdt.toLocalDate();

	    OADate d = new OADate(ld);
	    d.zoneId = zid;
	    d.type = DateTimeType.Floating;
	    d._time = ld.atStartOfDay().atZone(zid).toInstant().toEpochMilli();
	    return d;
	}

	
	/**
	 * Creates a date-only result from explicit date/time fields.
	 * <p>
	 * Hour, minute, second, and millisecond values are ignored because OADate only
	 * preserves the date portion. The supplied zone is used to normalize the
	 * resulting midnight value.
	 *
	 * @param zid zone used to resolve the date at midnight; {@code null} uses the OA default zone
	 * @param year full year
	 * @param month month from 1 to 12
	 * @param day day of month
	 * @param hrs ignored
	 * @param mins ignored
	 * @param secs ignored
	 * @param milsecs ignored
	 * @return OADate normalized from the supplied date fields
	 */
	@Override
	protected OADateTime createUtil(ZoneId zid, int year, int month, int day, int hrs, int mins, int secs, int milsecs) {
	    if (zid == null) zid = defaultZoneId;
	    LocalDate ld = LocalDate.of(year, month, day);

	    OADate d = new OADate(ld);
	    d.zoneId = zid;
	    d.type = DateTimeType.Floating;
	    d._time = ld.atStartOfDay().atZone(zid).toInstant().toEpochMilli();
	    return d;
	}
	
	/**
	 * Creates a date-only result from an instant and zone.
	 * <p>
	 * The supplied zone is used only to determine which calendar date the instant
	 * maps to. The resulting OADate is then normalized as a standard Floating
	 * date-only value.
	 *
	 * @param time milliseconds from the Java epoch
	 * @param zid zone used to map the instant to a local date; {@code null} uses the OA default zone
	 * @return OADate for the mapped local date
	 */
	@Override
	protected OADateTime createUtil(long time, ZoneId zid) {
	    if (zid == null) zid = defaultZoneId;
	    
	    LocalDate ld = Instant.ofEpochMilli(time).atZone(zid).toLocalDate();
	    OADate d = new OADate(ld);
	    d.zoneId = zid;
	    d.type = DateTimeType.Floating;
	    d._time = ld.atStartOfDay().atZone(zid).toInstant().toEpochMilli();
	    return d;
	}
	
	/**
	 * Parses text into an OADate using default OADate parsing rules.
	 *
	 * @param strDateTime text to parse
	 * @return parsed OADate, or {@code null} if parsing fails
	 */
	public static OADateTime valueOf(String strDateTime) {
		return valueOf(strDateTime, null);
	}
	
	/**
	 * Parses text into an OADate using a preferred format and default fallback behavior.
	 *
	 * @param strDateTime text to parse
	 * @param fmt preferred parse pattern
	 * @return parsed OADate, or {@code null} if parsing fails
	 */
	public static OADateTime valueOf(String strDateTime, String fmt) {
		return valueOf(strDateTime, fmt, true);
	}
	
	/**
	 * Parses text into an OADate.
	 * <p>
	 * Parsing is delegated to the shared OADateTime parser using OADate-specific
	 * parse formats and then normalized into a date-only value.
	 *
	 * @param date text to parse
	 * @param fmt preferred parse pattern
	 * @param bTryOtherFormats whether OADate fallback parse formats should be tried
	 * @return parsed OADate, or {@code null} if parsing fails
	 */
	public static OADateTime valueOf(String date, String fmt, boolean bTryOtherFormats) {
		if (date == null) return null;
		OADateTime dt = valueOfMain(date, fmt, bTryOtherFormats ? alDateParseFormat : null, bTryOtherFormats ? dateOutputFormat : null);
		if (dt == null) return null;
		return new OADate(dt);
	}
}

/* CODEX invariants 20260610

*/

/*
 * OADate implementation invariants
 * --------------------------------
 * Core date-only semantics:
 * - OADate represents a calendar date only. LocalDate is the conceptual model.
 * - Year, month, and day are the authoritative business fields.
 * - Month values follow java.time numbering: January is 1 and December is 12.
 * - Time-of-day is never business state for OADate. Constructors, factories,
 *   parsing, and inherited withXxx/plusXxx/minusXxx operations must discard or
 *   normalize hour/minute/second/millisecond to 00:00:00.000.
 * - OADate values use DateTimeType.Floating. The stored _time is derived from
 *   the date at local midnight in the zone selected by the constructor/factory.
 * - A valid OADate instance should have a non-null zoneId because Floating values
 *   capture the zone used to derive _time.
 */

/*
 * OADate timezone invariants
 * --------------------------
 * - defaultZoneId is used to resolve constructors that do not receive an
 *   explicit zone-bearing source.
 * - Date and long inputs are instants; they are interpreted in defaultZoneId to
 *   determine the OADate calendar date, then normalized to midnight.
 * - Calendar input uses the Calendar's date fields directly, then normalizes the
 *   result as an OADate.
 * - OADateTime input keeps only the source's effective local year/month/day
 *   fields. Source time-of-day is discarded.
 * - createUtil(long time, ZoneId zid) uses zid to map an instant to a local
 *   calendar date. It must not preserve time-of-day from the instant.
 * - OADate does not generally preserve source OADateTime zone metadata unless a
 *   specific createUtil override explicitly assigns a zone for the returned
 *   date-only value.
 */

/*
 * OADate factory invariants
 * -------------------------
 * - All createUtil(...) overrides must return OADate instances, never plain
 *   OADateTime instances.
 * - Inherited OADateTime withXxx/plusXxx/minusXxx operations rely on createUtil
 *   to preserve the OADate runtime type and date-only semantics.
 * - createUtil(ZoneId, fields...) ignores hour, minute, second, and millisecond.
 *   It keeps only year/month/day and resolves midnight in the supplied zone.
 * - createUtil(ZonedDateTime) keeps only zdt.toLocalDate(), discards time-of-day,
 *   and resolves midnight for that local date.
 * - createUtil(long, ZoneId) maps the instant to a LocalDate in the supplied
 *   zone, then returns a standard Floating OADate normalized to midnight.
 * - Methods inherited from OADateTime that conceptually set time fields, such as
 *   withTime(...), withHours(...), withMinutes(...), withSeconds(...), and
 *   withMilliSeconds(...), must still return date-only OADate values.
 */

/*
 * OADate parsing invariants
 * -------------------------
 * - OADate.valueOf(...) delegates text parsing to OADateTime.valueOfMain(...)
 *   using OADate-specific parse formats and output-format fallback.
 * - A successful parse is wrapped into a new OADate so any parsed time-of-day,
 *   offset, or region-zone instant semantics are reduced to date-only OADate
 *   semantics.
 * - A failed parse returns null from valueOf/dateValue methods.
 * - String constructors throw IllegalArgumentException for invalid non-null text.
 * - Date-only parsing must not preserve parsed time-of-day in the resulting
 *   OADate.
 */

/*
 * OADate formatting invariants
 * ----------------------------
 * - toString() delegates to toString(null).
 * - toString(null) uses the instance format first, then OADate's global
 *   dateOutputFormat, then the built-in date-only fallback format.
 * - An explicit toString(format) argument overrides instance and global formats.
 * - Formatting should emit date-only fields. Patterns containing time fields
 *   should observe normalized midnight values.
 * - Formatting uses OADateTime.toStringMain(...), including OADateTime's format
 *   normalization rules before DateTimeFormatter creation.
 */

/*
 * OADate constructor invariants
 * -----------------------------
 * - The no-arg constructor represents the current LocalDate and normalizes to
 *   midnight.
 * - long and Date constructors interpret the supplied instant in defaultZoneId,
 *   keep only the resulting LocalDate, and normalize to midnight.
 * - Calendar constructor keeps the Calendar year/month/day fields and normalizes
 *   to an OADate.
 * - LocalDate constructor maps directly to midnight for that date.
 * - OADateTime constructor keeps only effective local year/month/day fields from
 *   the source value and discards source time-of-day.
 * - String constructors parse through OADate.valueOf(...), then normalize the
 *   parsed result to date-only Floating state.
 */

/*
 * OADate equality and comparison invariants
 * -----------------------------------------
 * - equals(Object), hashCode(), compareTo(Object), and compare(Object) are
 *   inherited from OADateTime and are based on _time.
 * - Because OADate normalizes _time to local midnight, equality and comparison
 *   operate as date comparisons only within the normalized zone model used to
 *   derive each instance.
 * - zoneId and DateTimeType do not participate in inherited equality,
 *   hashCode, or comparison.
 * - If two OADate instances for the same calendar date are normalized in
 *   different zones, their _time values can differ and inherited equality can
 *   report them as not equal.
 */

/*
 * OADate serialization invariants
 * -------------------------------
 * - OADate inherits OADateTime custom Java serialization.
 * - Serialized OADate state should preserve enough date-only information to
 *   round-trip as an OADate with DateTimeType.Floating, non-null zoneId, and
 *   normalized midnight time fields.
 * - Floating serialization writes wall-clock fields rather than treating _time
 *   as authoritative.
 * - Deserialization must not leave an OADate with non-midnight time fields.
 * - If subclass-specific serialization behavior is required, OADate must define
 *   it deliberately rather than relying accidentally on OADateTime internals.
 */


