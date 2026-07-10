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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import com.viaoa.datetime.OADateTime.DateTimeType;


/**
 * Date-only OA value.
 * <p>
 * OADate is a specialization of {@link OADateTime} whose time portion is
 * always normalized to {@code 00:00:00.000}. It represents a calendar date
 * rather than a precise timestamp.
 *
 * <h3>Semantics</h3>
 * OADate always uses {@link DateTimeType#Floating} semantics.
 * The business meaning of an OADate is the local calendar date.
 * <p>
 * During creation or deserialization, the local date fields are resolved
 * using the effective OA zone and converted into a canonical internal
 * epoch-millisecond value inherited from {@link OADateTime}. Once created,
 * the instance is immutable.
 *
 * <h3>Internal representation</h3>
 * OADate stores:
 * <ul>
 *   <li>A canonical {@code _time} value inherited from {@link OADateTime}</li>
 *   <li>An associated {@code zoneId}</li>
 *   <li>{@link DateTimeType#Floating}</li>
 * </ul>
 * <p>
 * The stored {@code _time} is derived from midnight
 * ({@code 00:00:00.000}) of the represented date in the effective zone.
 *
 * <h3>Timezone behavior</h3>
 * OADate is not a zone-free value.
 * <p>
 * The effective zone is used when creating, deserializing, converting,
 * formatting, and interpreting the internal value.
 * Existing instances do not change meaning if
 * {@link OADateTime#setDefaultZoneId(java.time.ZoneId)} is changed later.
 *
 * <h3>Comparison and equality</h3>
 * OADate inherits comparison, equality, hashing, ordering, and interval
 * semantics from {@link OADateTime}.
 * <p>
 * These operations are based on the canonical internal value rather than
 * purely on displayed date fields.
 *
 * <h3>Immutability</h3>
 * OADate behaves as an immutable value type.
 * Methods inherited from {@link OADateTime}, including
 * {@code withXxx(...)}, {@code plusXxx(...)}, and
 * {@code minusXxx(...)}, return new OADate instances through
 * {@code createUtil(...)} overrides and never modify existing instances.
 *
 * <h3>Parsing and formatting</h3>
 * OADate provides date-only parsing and formatting behavior.
 * Parsing delegates to the shared OADateTime infrastructure and then
 * normalizes the result into a date-only value.
 * Formatting uses either:
 * <ul>
 *   <li>an instance format</li>
 *   <li>the global OADate output format</li>
 *   <li>a built-in fallback format</li>
 * </ul>
 *
 * <h3>Java Time</h3>
 * New OA code should generally prefer {@link LocalDate} for pure date
 * semantics. OADate primarily exists as the OA-compatible date value
 * layer for existing applications and framework services.
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
		this(LocalDate.now(defaultZoneId));
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
		super(false);
	    LocalDate ld;

	    if (date == null) {
	        ld = LocalDate.now(defaultZoneId);
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
		super(false);
		OADateTime dt = OADate.valueOf(strDate, format);
		if (dt == null) throw new IllegalArgumentException("OADate cant create date from String \"" + strDate + "\", format="+format);
		LocalDate ld = LocalDate.of(dt.getYear(), dt.getMonthValue(), dt.getDayOfMonth());
		
	    this.type = DateTimeType.Floating;
	    this.zoneId = defaultZoneId;
	    this._time = ld.atStartOfDay().atZone(this.zoneId).toInstant().toEpochMilli();
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

/* CODEX invariants 20260611

  OADate implementation invariants
  --------------------------------
 
  Core date-only semantics
  - OADate is the OA date-only value type and is an OADateTime subclass.
  - OADate always represents a local calendar date, not a full timestamp.
  - Date fields are the only input fields retained by OADate-specific
    construction, parsing, and factory paths.
  - Time-of-day is always normalized to 00:00:00.000.
  - Month values use java.time numbering: January is 1 and December is 12.
  - type must be DateTimeType.Floating for canonical OADate values.
  - _time is inherited from OADateTime and is the canonical stored value.
  - _time is derived from the represented local date at midnight in the
    captured/effective zone.
 
  Floating/zone semantics
  - OADate uses OADateTime Floating semantics.
  - Floating is not zone-free. The local date at midnight is resolved into
    _time using the effective zone at creation/deserialization time.
  - OADate stores both _time and zoneId after creation.
  - Existing OADate instances must not change meaning if
    OADateTime.defaultZoneId changes later.
  - OADate values with the same displayed date can have different _time values
    if they were resolved in different zones. That is accepted because _time is
    the inherited comparison value.
 
  Factory/subclass behavior
  - createUtil(...) methods must always return OADate instances.
  - createUtil(...) must discard hour, minute, second, and millisecond inputs.
  - createUtil(ZonedDateTime) retains only the local date from the supplied
    ZonedDateTime and resolves that date at midnight in the supplied zone.
  - createUtil(ZoneId, fields...) retains only year/month/day and resolves
    midnight in the supplied zone, or defaultZoneId when the zone is null.
  - createUtil(long, ZoneId) maps the instant to a local date in the supplied
    zone, then resolves that date at midnight in that zone.
  - Inherited withXxx/plusXxx/minusXxx methods rely on createUtil(...) so
    adjusted OADate values preserve the OADate runtime type and date-only
    invariants.
 
  Parsing/formatting
  - valueOf(...) delegates parsing to OADateTime parsing through
    valueOfMain(...), using OADate parse formats.
  - Successful parses are wrapped/normalized into OADate.
  - Any parsed time portion is discarded; only the parsed local date is retained.
  - Failed valueOf(...) parsing returns null.
  - String constructors throw IllegalArgumentException for invalid non-null
    strings.
  - Formatting uses OADate format selection: instance format, then global
    OADate output format, then the built-in fallback format.
  - Formatting ultimately derives fields from inherited _time plus effective
    zone through OADateTime formatter behavior.
 
  Comparison/equality
  - OADate inherits equals(Object), hashCode(), compareTo(Object), compare(Object),
    and timeline interval behavior from OADateTime.
  - These inherited operations use _time.
  - Do not add date-field-only equality or comparison semantics to OADate.
  - OADate is an OA legacy value type with date-only normalization, not a pure
    LocalDate replacement.
 
  Serialization
  - OADate inherits OADateTime custom serialization.
  - OADate should deserialize as an OADate instance.
  - Because canonical OADate values are Floating, serialization writes type,
    _time, and zoneId through OADateTime.
  - Floating deserialization re-resolves local fields using the receiving
    JVM/default zone according to OADateTime rules.
  - After deserialization, OADate invariants must still hold: runtime type is
    OADate, type is Floating, time is 00:00:00.000, and zoneId is captured.
 
*/


