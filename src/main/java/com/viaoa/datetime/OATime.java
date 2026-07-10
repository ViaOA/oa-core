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

import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.viaoa.lang.OAStr;


/**
 * Time-only OA value.
 * <p>
 * OATime is a specialization of {@link OADateTime} whose date portion is always
 * normalized to {@code 1970-01-01}. It represents a time of day rather than a
 * full date/time or a precise business timestamp.
 *
 * <h3>Semantics</h3>
 * OATime uses {@link DateTimeType#Floating} semantics. The business meaning is
 * the local time-of-day. Internally, the underlying epoch-millisecond value is
 * derived from {@code 1970-01-01} plus the time fields in the active/default OA
 * zone.
 *
 * <h3>Timezone behavior</h3>
 * A timezone can be used when deriving a local time from an instant, for example
 * in {@link #OATime(long)} or {@link #createUtil(long, ZoneId)}. After the local
 * time is derived, OATime discards date and zone metadata and normalizes the
 * result back to standard time-only semantics.
 *
 * <h3>Immutability</h3>
 * OATime is immutable. Operations inherited from {@link OADateTime}, including
 * {@code withXxx(...)} and arithmetic methods such as {@code plusHours(...)},
 * return new OATime instances through the protected {@code createUtil(...)}
 * factory methods.
 *
 * <h3>Parsing and formatting</h3>
 * Parsing uses OATime-specific formats and normalizes parsed values to
 * time-only values. Formatting uses either an instance format, the global
 * OATime output format, or a built-in fallback format.
 *
 * @see OADateTime
 * @see OADate
 * @see LocalTime
 */
public class OATime extends OADateTime {
	private static final long serialVersionUID = 1L;

	/**
	 * Time format using 12-hour clock with minutes and AM/PM indicator.
	 */
	public final static String Format1 = "hh:mma";

	/**
	 * Time format using 12-hour clock with minutes, seconds, and AM/PM indicator.
	 */
	public final static String Format2 = "hh:mm:ssa";
	
	/**
	 * Time format using 12-hour clock with minutes, seconds, milliseconds,
	 * and AM/PM indicator.
	 */
	public final static String Format3 = "hh:mm:ss.SSSa";

	/**
	 * Time format using 24-hour clock with hours and minutes.
	 */
	public final static String Format4 = "HH:mm";
	
	/**
	 * Time format using 24-hour clock with hours, minutes, and seconds.
	 */
	public final static String Format5 = "HH:mm:ss";
	
	/**
	 * Time format using 24-hour clock with hours, minutes, seconds,
	 * and milliseconds.
	 */
	public final static String Format6 = "HH:mm:ss.SSS";

	/**
	 * Default output format used by {@link #toString()} when this instance does
	 * not have an instance-specific format.
	 */
	protected static String timeOutputFormat = "hh:mma";

	/**
	 * Time format used for JSON serialization without timezone information.
	 */
	public final static String JsonFormat = "HH:mm:ss";
	
	/**
	 * Time format used for JSON serialization including timezone offset.
	 */
	public final static String JsonFormatTZ = "HH:mm:ssX";

	/**
	 * Time format used for JDBC and SQL operations.
	 */
	public final static String JdbcFormat = "HH:mm:ss"; // SQL
	
	// format used by browser: : HH:mm   ... not all support seconds "HH:mm:ss"
	// same as JsonFormat
    // public final static String HtmlInputTimeFormat = "hh:mm"; // java format to use 
    

	/**
	 * Ordered fallback parse formats used by OATime parsing methods.
	 */
	private static final List<String> alTimeParseFormat = new ArrayList<>();

	static {
		alTimeParseFormat.add("hh:mm:ss.S a");
		alTimeParseFormat.add("hh:mm:ss a");
		alTimeParseFormat.add("hh:mm a");

		alTimeParseFormat.add("hh:mm:ss.Sa");
		alTimeParseFormat.add("hh:mm:ssa");
		alTimeParseFormat.add("hh:mma");

		alTimeParseFormat.add("HH:mm:ss.S");
		alTimeParseFormat.add("HH:mm:ss");
		alTimeParseFormat.add("HH:mm");

		alTimeParseFormat.add("hha");
		alTimeParseFormat.add("hh a");
		alTimeParseFormat.add("HH");
	}

	/**
	 * Creates a new time instance using the current system time.
	 * <p>
	 * The date portion is cleared so that only time values are retained.
	 */
	public OATime() {
		this(LocalTime.now(defaultZoneId));
	}

	/**
	 * Creates a time from an epoch-millisecond instant.
	 * <p>
	 * The instant is interpreted using the current OA default zone to derive the
	 * local time-of-day. The date portion is then discarded and normalized to
	 * {@code 1970-01-01}.
	 *
	 * @param time milliseconds since the Java epoch
	 */
	public OATime(long time) {
	    this(Instant.ofEpochMilli(time).atZone(defaultZoneId).toLocalTime());
	}	
	
	/**
	 * Creates a time from explicit time fields.
	 * <p>
	 * The date portion is normalized to {@code 1970-01-01}. The created value
	 * uses {@link DateTimeType#Floating} semantics.
	 *
	 * @param hrs hour of day from 0 to 23
	 * @param mins minute of hour
	 * @param secs second of minute
	 * @param mili millisecond of second from 0 to 999
	 */
	public OATime(int hrs, int mins, int secs, int mili) {
		super(1970, Month.JANUARY.getValue(), 1, hrs, mins, secs, mili);
		type = DateTimeType.Floating;
		zoneId = defaultZoneId;
	}
	
	/**
	 * Creates a time from a {@link LocalTime}.
	 * <p>
	 * Nanoseconds are truncated to millisecond precision. The date portion is
	 * normalized to {@code 1970-01-01}.
	 *
	 * @param lt local time whose hour, minute, second, and millisecond values are used
	 */
	public OATime(LocalTime lt) {
		this(lt.getHour(), lt.getMinute(), lt.getSecond(), (int) (lt.getNano() / 1_000_000));
	}

	/**
	 * Creates a time from a legacy {@link Date}.
	 * <p>
	 * The date instant is interpreted using the OA default zone to derive the
	 * local time-of-day. The original date portion is discarded.
	 *
	 * @param date source date whose time-of-day value is used
	 */
	public OATime(Date date) {
		this(date.getTime());
	}

	/**
	 * Creates a time from a legacy {@link Calendar}.
	 * <p>
	 * The hour, minute, second, and millisecond fields are copied. The calendar
	 * date portion is discarded.
	 *
	 * @param c source calendar whose time-of-day fields are used
	 */
	public OATime(Calendar c) {
		this(c.get(c.HOUR_OF_DAY), c.get(c.MINUTE), c.get(c.SECOND), c.get(c.MILLISECOND));
	}

	/**
	 * Creates a time from another OA date/time value.
	 * <p>
	 * Only the local hour, minute, second, and millisecond fields are retained.
	 * Date information is discarded and the result is normalized to
	 * {@code 1970-01-01}.
	 *
	 * @param dt source date/time value
	 */
	public OATime(OADateTime dt) {
		this(dt.get24Hour(), dt.getMinute(), dt.getSecond(), dt.getMilliSecond());
	}

	/**
	 * Creates a time by parsing text with OATime parsing rules.
	 * <p>
	 * Parsed date information, if present, is discarded. The result is normalized
	 * to {@code 1970-01-01} plus the parsed time fields.
	 *
	 * @param strTime text representation of the time
	 */
	public OATime(String strTime) {
		this(strTime, null);
	}

	/**
	 * Creates a time by parsing text with the supplied format.
	 * <p>
	 * Parsing delegates to {@link #valueOf(String, String)} and then normalizes
	 * the result to time-only semantics. Parsed date information, if present, is
	 * discarded.
	 *
	 * @param strTime text representation of the time
	 * @param fmt preferred parse format, or {@code null} to use fallback formats
	 * @throws IllegalArgumentException if the text cannot be parsed
	 */
	public OATime(String strTime, String fmt) {
		super(false);
	    OADateTime dt = OATime.valueOf(strTime, fmt);
	    if (dt == null) {
	        throw new IllegalArgumentException("OATime cant create time from String \"" + strTime + "\", format=" + fmt);
	    }

	    LocalTime lt = dt.getLocalTime();

	    this.type = DateTimeType.Floating;
	    this.zoneId = defaultZoneId;
	    this._time = LocalDate.of(1970, 1, 1)
	        .atTime(lt)
	        .atZone(this.zoneId)
	        .toInstant()
	        .toEpochMilli();
	}

	/**
	 * Creates a time from hour, minute, and second fields.
	 * <p>
	 * Milliseconds are set to zero and the date portion is normalized to
	 * {@code 1970-01-01}.
	 *
	 * @param hrs hour of day from 0 to 23
	 * @param mins minute of hour
	 * @param secs second of minute
	 */
	public OATime(int hrs, int mins, int secs) {
		this(hrs, mins, secs, 0);
	}
	
	/**
	 * Creates a time-only result from a calculated {@link ZonedDateTime}.
	 * <p>
	 * The local time from the supplied value is retained while the date and zone
	 * metadata are discarded. The resulting value is normalized to
	 * {@code 1970-01-01}.
	 */
	@Override
	protected OADateTime createUtil(ZonedDateTime zdt) {
	    LocalTime lt = zdt.toLocalTime();

	    OATime t = new OATime(lt);
	    t.type = DateTimeType.Floating;
	    return t;
	}
	
	/**
	 * Creates a time-only result from explicit date/time fields.
	 * <p>
	 * Year, month, day, and zone values are ignored because OATime preserves only
	 * the time portion. The result is normalized to {@code 1970-01-01}.
	 */
	@Override
	protected OADateTime createUtil(ZoneId zid, int year, int month, int day, int hrs, int mins, int secs, int milsecs) {
	    LocalTime lt = LocalTime.of(hrs, mins, secs, milsecs * 1_000_000);
	    return new OATime(lt);
	}	
	
	/**
	 * Creates a time-only result from an instant and timezone.
	 * <p>
	 * The supplied zone is used only to determine which local time-of-day the
	 * instant maps to. The resulting OATime is then normalized to
	 * {@code 1970-01-01}.
	 */
	@Override
	protected OADateTime createUtil(long time, ZoneId zid) {
	    if (zid == null) zid = defaultZoneId;
	    LocalTime lt = Instant.ofEpochMilli(time).atZone(zid).toLocalTime();
	    return new OATime(lt);
	}
	
	/**
	 * Converts a string to an {@link OATime} using the supplied format.
	 *
	 * @param time the string representation of the time
	 * @param fmt the format to use for parsing
	 * @return a new {@link OATime} instance, or {@code null} if parsing fails
	 */
	public static OATime timeValue(String time, String fmt) {
		return (OATime) valueOf(time, fmt);
	}

	/**
	 * Converts a string to an {@link OATime} using the default format.
	 *
	 * @param time the string representation of the time
	 * @return a new {@link OATime} instance, or {@code null} if parsing fails
	 */
	public static OATime timeValue(String time) {
		return (OATime) valueOf(time, null);
	}

	/**
	 * Parses text into an OATime.
	 * <p>
	 * Parsing is delegated to the shared OADateTime parser using OATime parse
	 * formats. The parsed value is then normalized into an OATime, discarding any
	 * parsed date fields.
	 *
	 * @param time text representation of the time
	 * @param fmt preferred parse format, or {@code null} to try fallback formats
	 * @return parsed OATime as an OADateTime reference, or {@code null} if parsing fails
	 */
	public static OADateTime valueOf(String time, String fmt) {
	    if (time == null) return null;

	    if (time.length() > 0) {
	        char c = time.charAt(time.length() - 1);
	        if (c == 'A' || c == 'a' || c == 'P' || c == 'p') {
	            time += "m";
	        }
	    }

	    OADateTime dt = null;

	    if (!OAStr.isEmpty(fmt)) {
	        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(fmt)
	            .withResolverStyle(ResolverStyle.STRICT);
	        dt = parseTime(time, dtf);
	    }

	    for (int i = 0; dt == null && i < alTimeParseFormat.size(); i++) {
	        String format = alTimeParseFormat.get(i);
	        if (OAStr.isEmpty(format)) continue;

	        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format)
	            .withResolverStyle(ResolverStyle.STRICT);
	        dt = parseTime(time, dtf);
	    }

	    if (dt == null && !OAStr.isEmpty(timeOutputFormat)) {
	        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(timeOutputFormat)
	            .withResolverStyle(ResolverStyle.STRICT);
	        dt = parseTime(time, dtf);
	    }

	    return dt;
	}
	
	/**
	 * Parses text into an OATime using default parsing rules.
	 *
	 * @param time text representation of the time
	 * @return parsed OATime as an OADateTime reference, or {@code null} if parsing fails
	 */
	public static OADateTime valueOf(String time) {
		return OATime.valueOf(time, null);
	}

	/**
	 * Parses text into an OATime using the supplied formatter.
	 * <p>
	 * Unlike {@link OADateTime#parseDateTime(String, DateTimeFormatter)},
	 * this method accepts time-only formats that do not contain a date.
	 * <p>
	 * Parsing must consume the entire input text. Any parse error,
	 * partially consumed input, or formatter that does not produce a
	 * {@link LocalTime} result causes {@code null} to be returned.
	 * <p>
	 * Successful parses are normalized into an {@link OATime}, which
	 * uses {@code 1970-01-01} as the fixed date portion and
	 * {@link DateTimeType#Floating} semantics.
	 *
	 * @param text text to parse
	 * @param fmt formatter used to parse the text
	 * @return parsed OATime, or {@code null} if parsing fails
	 */
	protected static OATime parseTime(String text, DateTimeFormatter fmt) {
	    ParsePosition pos = new ParsePosition(0);
	    TemporalAccessor ta;

	    try {
	        ta = fmt.parse(text, pos);
	    } catch (DateTimeException e) {
	        return null;
	    }

	    if (pos.getErrorIndex() >= 0 || pos.getIndex() != text.length()) {
	        return null;
	    }

	    LocalTime lt = ta.query(TemporalQueries.localTime());
	    if (lt == null) {
	        return null;
	    }
	    return new OATime(lt);
	}
	
	/**
	 * Sets the global output format used by OATime string formatting.
	 *
	 * @param fmt output format to use, or {@code null} to allow fallback behavior
	 */
	public static void setGlobalOutputFormat(String fmt) {
		timeOutputFormat = fmt;
	}

	/**
	 * Returns the global output format used by OATime string formatting.
	 *
	 * @return global output format, possibly {@code null}
	 */
	public static String getGlobalOutputFormat() {
		return timeOutputFormat;
	}

	/**
	 * Adds a fallback parse format used by OATime parsing methods.
	 *
	 * @param fmt parse format to add
	 */
	public static void addGlobalParseFormat(String fmt) {
		alTimeParseFormat.add(fmt);
	}

	/**
	 * Removes a fallback parse format used by OATime parsing methods.
	 *
	 * @param fmt parse format to remove
	 */
	public static void removeGlobalParseFormat(String fmt) {
		alTimeParseFormat.remove(fmt);
	}
	
	/**
	 * Converts this time to text.
	 * <p>
	 * If {@code f} is {@code null}, the instance format is used first, then the
	 * OATime global output format, then the built-in fallback format
	 * {@code "hh:mma"}.
	 *
	 * @param f explicit output format, or {@code null} to use configured defaults
	 * @return formatted time string
	 */
	public String toString(String f) {
		if (f == null) {
			f = (format == null) ? timeOutputFormat : format;
			if (f == null || f.length() == 0) {
				f = getGlobalOutputFormat();
				if (OAStr.isEmpty(f)) f = "hh:mma";
			}
		}
		return toStringMain(f);
	}
}

/* CODEX invariants 20260611
 * 
 * OATime implementation invariants
 * --------------------------------
 *
 * Core time-only semantics
 * - OATime is the OA time-only value type and is an OADateTime subclass.
 * - OATime always represents a local time of day, not a full date/time.
 * - Date fields are fixed implementation state and must normalize to
 *   1970-01-01.
 * - Time fields are the only business fields retained by OATime-specific
 *   construction, parsing, and factory paths.
 * - type must be DateTimeType.Floating for canonical OATime values.
 * - _time is inherited from OADateTime and is the canonical stored value.
 * - _time is derived from 1970-01-01 plus the represented local time in the
 *   captured/effective zone.
 *
 * Floating/zone semantics
 * - OATime uses OADateTime Floating semantics.
 * - Floating is not zone-free. The local time on 1970-01-01 is resolved into
 *   _time using the effective zone at creation/deserialization time.
 * - OATime stores both _time and zoneId after creation.
 * - Existing OATime instances must not change meaning if
 *   OADateTime.defaultZoneId changes later.
 * - OATime values with the same displayed clock fields can have different
 *   _time values if they were resolved in different zones. That is accepted
 *   because _time is the inherited comparison value.
 *
 * Factory/subclass behavior
 * - createUtil(...) methods must always return OATime instances.
 * - createUtil(...) must discard year, month, and day inputs.
 * - createUtil(ZonedDateTime) retains only the local time from the supplied
 *   ZonedDateTime and resolves that time on 1970-01-01 as a canonical OATime.
 * - createUtil(ZoneId, fields...) retains only hour/minute/second/millisecond.
 * - createUtil(long, ZoneId) maps the instant to a local time in the supplied
 *   zone, then resolves that time on 1970-01-01 as a canonical OATime.
 * - Inherited withXxx/plusXxx/minusXxx methods rely on createUtil(...) so
 *   adjusted OATime values preserve the OATime runtime type and time-only
 *   invariants.
 *
 * Parsing/formatting
 * - OATime has its own time-only parse path because OADateTime parsing requires
 *   a date component.
 * - valueOf(...) must support time-only inputs such as HH:mm, HH:mm:ss, hh:mma,
 *   and hh:mm:ssa, subject to configured parse formats.
 * - Parsing must consume the full input and return null on invalid input.
 * - Any parsed date portion is discarded; only local time fields are retained.
 * - String constructors throw IllegalArgumentException for invalid non-null
 *   strings.
 * - Formatting uses OATime format selection: instance format, then global
 *   OATime output format, then the built-in fallback format.
 * - Explicit format strings are allowed to expose the fixed 1970-01-01 date if
 *   the caller includes date fields.
 *
 * Comparison/equality
 * - OATime inherits equals(Object), hashCode(), compareTo(Object), compare(Object),
 *   and timeline interval behavior from OADateTime.
 * - These inherited operations use _time.
 * - Do not add time-field-only equality or comparison semantics to OATime.
 * - OATime is an OA legacy value type with time-only normalization, not a pure
 *   LocalTime replacement.
 *
 * Serialization
 * - OATime inherits OADateTime custom serialization.
 * - OATime should deserialize as an OATime instance.
 * - Because canonical OATime values are Floating, serialization writes type,
 *   _time, and zoneId through OADateTime.
 * - Floating deserialization re-resolves local fields using the receiving
 *   JVM/default zone according to OADateTime rules.
 * - After deserialization, OATime invariants must still hold: runtime type is
 *   OATime, type is Floating, date is 1970-01-01, and zoneId is captured.
 */


/*
 * Comparison invariant:
 * OATime normalizes its date fields to 1970-01-01 and uses Floating semantics,
 * but equality, hashCode, compareTo, and timeline interval methods are inherited
 * from OADateTime and are based on the resolved _time value.
 *
 * Therefore, two OATime instances with the same displayed clock fields can
 * compare unequal if they were resolved using different captured zones. This is
 * intentional. OATime is not a pure LocalTime value; it is an OA date/time value
 * normalized to a fixed date.
 */

