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

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.viaoa.lang.OAStr;

/* CODEX Review

*/


/**
 * Core OA date/time value used for formatting, parsing, conversion,
 * serialization, comparison, and date/time calculations.
 *
 * <p>
 * OADateTime is the OA compatibility layer over Java date/time APIs. It
 * accepts and converts legacy types such as {@link java.util.Date},
 * {@link java.util.Calendar}, {@link java.sql.Date},
 * {@link java.sql.Time}, and {@link java.sql.Timestamp}, while internally
 * using {@code java.time} classes for calculations and field access.
 *
 * <h3>Internal model</h3>
 * Each instance stores:
 * <ul>
 *   <li>{@link #_time} – milliseconds since the Java epoch</li>
 *   <li>optional {@link #zoneId}</li>
 *   <li>{@link DateTimeType} semantic type</li>
 * </ul>
 *
 * <p>
 * The stored {@code _time} value is the canonical internal representation
 * used for equality, hashing, comparison, ordering, and timeline-based
 * calculations.
 *
 * <h3>DateTimeType semantics</h3>
 * <ul>
 *   <li><b>Instant</b> – {@code _time} is authoritative and represents an
 *       exact instant in time.</li>
 *   <li><b>ZonedInstant</b> – {@code _time} and {@code zoneId} are both
 *       authoritative and represent an exact instant whose associated zone
 *       is part of the business meaning.</li>
 *   <li><b>Floating</b> – local wall-clock fields are resolved using the
 *       effective zone at creation or deserialization time. Once resolved,
 *       the resulting {@code _time} and {@code zoneId} become the canonical
 *       immutable representation.</li>
 * </ul>
 *
 * <h3>Floating values</h3>
 * Floating values are not zone-free values.
 * <p>
 * When a Floating value is created, its local date/time fields are resolved
 * using the effective zone and converted into a canonical {@code _time}
 * value. After creation, the instance is immutable and retains its resolved
 * meaning even if {@link #defaultZoneId} later changes.
 *
 * <h3>Serialization</h3>
 * Serialization behavior depends on {@link DateTimeType}.
 * <ul>
 *   <li>Instant preserves the exact instant.</li>
 *   <li>ZonedInstant preserves the exact instant and associated zone.</li>
 *   <li>Floating preserves local wall-clock fields across JVM boundaries by
 *       re-resolving those fields using the receiving JVM's
 *       {@link #defaultZoneId} during deserialization.</li>
 * </ul>
 *
 * <h3>Immutability</h3>
 * OADateTime behaves as an immutable value type.
 * Methods such as {@code withXxx(...)}, {@code plusXxx(...)},
 * {@code minusXxx(...)}, and timezone conversion methods return new
 * instances and do not modify existing values.
 *
 * <h3>Specialized subclasses</h3>
 * <ul>
 *   <li>{@link OADate} – date-only semantics.</li>
 *   <li>{@link OATime} – time-only semantics.</li>
 * </ul>
 *
 * <h3>Java Time</h3>
 * New OA code should prefer {@code java.time} types where practical.
 * OADateTime primarily serves as the OA-compatible value layer for existing
 * applications and frameworks.
 *
 * @see OADate
 * @see OATime
 * @see java.time.Instant
 * @see java.time.LocalDate
 * @see java.time.LocalTime
 * @see java.time.LocalDateTime
 * @see java.time.ZonedDateTime
 */
public class OADateTime implements java.io.Serializable, Comparable {
	/**
	 * Serialization version for OADateTime serialized form.
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * Milliseconds from the epoch. For {@link DateTimeType#Instant} this is the authoritative value; for {@link DateTimeType#Floating} it is derived from local wall-clock fields using the active/default zone.
	 */
	protected long _time;
	
	/**
	 * Optional zone associated with this value. When {@code null}, {@link #defaultZoneId} is used for field access, formatting, and conversions that require a zone.
	 */
	protected ZoneId zoneId;
	
	/**
	 * Optional instance-specific output format used by {@link #toString()} when no explicit format is supplied.
	 */
	protected String format;

	/**
	 * Semantic type for this value. The type determines whether {@link #_time}, {@link #zoneId}, or wall-clock fields are authoritative.
	 */
	protected DateTimeType type = DateTimeType.Instant; 
	
	
	
	/*
	 * defaultZoneId invariant:
	 * defaultZoneId is used when creating or deserializing Floating values.
	 * Existing instances are not updated if defaultZoneId later changes.
	 * A Floating instance keeps the zoneId and _time assigned at creation/read time.
	 */	
	/**
	 * Default zone used when an instance does not carry an explicit {@link #zoneId}.
	 */
	protected static ZoneId defaultZoneId;

	/**
	 * Locale used to initialize default parse and output formats.
	 */
	private static Locale locale;
	
	/**
	 * Default long date/time format including milliseconds and AM/PM marker.
	 */
	public final static String FORMAT_long = "yyyy/MM/dd hh:mm:ss.S a";
	
	/**
	 * Default extended date/time format including milliseconds, AM/PM marker, and time zone text.
	 */
	public final static String FORMAT_xlong = "yyyy/MM/dd hh:mm:ss.S a z";
	
	
	// RFC-339 format
	// Note: the 'Z' is not a timezone, it means that the timezone should be set to UTC.
	// The calling code should call dt.setTimeZoneUTC()

	/**
	 * RFC-3339 style UTC literal format without milliseconds. The literal {@code Z} is output text, not an automatically parsed zone.
	 */
	public final static String RFC339Format = "yyyy-MM-dd'T'HH:mm:ss'Z'"; // 2023-09-04T07:11:12:32-0400
	
	/**
	 * RFC-3339 style UTC literal format with milliseconds. The literal {@code Z} is output text, not an automatically parsed zone.
	 */
	public final static String RFC339FormatWms = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"; // 2023-09-04T07:11:12:32.123-0400

	/**
	 * Global output format used by {@link #toString()} when an instance-specific format has not been set.
	 */
	protected static String staticOutputFormat;
	
	/**
	 * JSON/ISO-style local date/time format without an offset or zone.
	 */
	public final static String JsonFormat = "yyyy-MM-dd'T'HH:mm:ss";
	
	/**
	 * JSON/ISO-style date/time format including an ISO-8601 offset.
	 */
	public final static String JsonFormatTZ = "yyyy-MM-dd'T'HH:mm:ssX";

	/**
	 * JDBC-compatible SQL timestamp format.
	 */
	public final static String JdbcFormat = "yyyy-MM-dd HH:mm:ss"; // SQL

	// format used by browser: : "YYYY-MM-DD'T'HH:mm";
	// same as json format
	// public final static String HtmlInputDateTimeFormat = "yyyy-MM-dd'T'hh:mm"; //

	/**
	 * Ordered fallback parse formats used by {@link #valueOf(String)} and related parsing methods.
	 */
	private static final List<String> alDateTimeParseFormat = new ArrayList<>();

	static {
		setLocale(Locale.getDefault());
		defaultZoneId = ZoneId.systemDefault();
	}

	/**
	 * Sets the default zone used when an OADateTime does not have an instance-specific zone.
	 *
	 * @param zid the default zone; when {@code null}, {@link ZoneId#systemDefault()} is used
	 */
	public static void setDefaultZoneId(ZoneId zid) {
		if (zid == null) {
			zid = ZoneId.systemDefault();
		}
		defaultZoneId = zid;
	}


	/**
	 * Returns the current OA default zone.
	 *
	 * @return default zone used for instances with no explicit zone
	 */
	public static ZoneId getDefaultZoneId() {
		return defaultZoneId;
	}
	
	/**
	 * Sets the locale used to derive default date/time parse and output formats.
	 * <p>
	 * Passing {@code null} resets the locale to {@link Locale#getDefault()}.
	 *
	 * @param loc locale to use for formatting/parsing defaults
	 */
	public static void setLocale(Locale loc) {
		if (loc == null) loc = Locale.getDefault(); 
		locale = loc;
		alDateTimeParseFormat.clear();
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
	 * Defines the business semantics of an OADateTime value.
	 * <p>
	 * The type controls which part of the value is authoritative during serialization,
	 * timezone conversion, display, and reconstruction.
	 */
	public enum DateTimeType {

	    /**
	     * Wall-clock fields are authoritative.
	     * <p>
		 * Floating comparison invariant:
		 * Floating values are resolved into _time at creation/deserialization using
		 * the then-current/default zone captured by the instance. After resolution,
		 * _time is the canonical comparison/equality/hash/duration value.
		 *
		 * Two Floating values with the same displayed fields but created under
		 * different zones are not guaranteed to compare equal. This is intentional.
	     * 
	     * <p>
	     * Examples:
	     * <ul>
	     *   <li>July 4, 2026 at 12:00 PM local time</li>
	     *   <li>Store opens at 8:00 AM local time</li>
	     *   <li>Daily lunch break at noon</li>
	     * </ul>
	     *
	     * Serialization:
	     * <ul>
	     *   <li>Serialize fields (Y/M/D/H/M/S/MS)</li>
	     *   <li>Do not serialize _time as authoritative</li>
	     * </ul>
	     *
	     * Timezone:
	     * <ul>
	     *   <li>No fixed timezone semantics</li>
	     *   <li>Reader/local timezone is used to derive _time</li>
	     * </ul>
	     *
	     * DST:
	     * <ul>
	     *   <li>Fields remain authoritative</li>
	     *   <li>Derived _time may differ by timezone</li>
	     *   <li>Deserialize should use lenient resolution</li>
	     * </ul>
	     */
	    Floating,

	    /**
	     * Represents an exact instant in time.
	     * <p>
	     * Examples:
	     * <ul>
	     *   <li>CreatedDateTime</li>
	     *   <li>UpdatedDateTime</li>
	     *   <li>Audit timestamp</li>
	     *   <li>Replication timestamp</li>
	     * </ul>
	     *
	     * Serialization:
	     * <ul>
	     *   <li>Serialize _time</li>
	     *   <li>Optional timezone metadata may be preserved</li>
	     * </ul>
	     *
	     * Display:
	     * <ul>
	     *   <li>Display using runtime/default timezone</li>
	     * </ul>
	     *
	     * DST:
	     * <ul>
	     *   <li>_time is authoritative</li>
	     *   <li>DST only affects displayed fields</li>
	     * </ul>
	     */
	    Instant,

	    /**
	     * Represents an exact instant whose associated timezone
	     * is part of the business meaning.
	     * <p>
	     * Examples:
	     * <ul>
	     *   <li>Meeting at 9:00 AM America/New_York</li>
	     *   <li>Market open in London</li>
	     *   <li>Flight departure timezone</li>
	     * </ul>
	     *
	     * Serialization:
	     * <ul>
	     *   <li>Serialize _time</li>
	     *   <li>Serialize timeZone</li>
	     * </ul>
	     *
	     * Display:
	     * <ul>
	     *   <li>Display using the stored timezone</li>
	     * </ul>
	     *
	     * DST:
	     * <ul>
	     *   <li>_time and timeZone are authoritative</li>
	     *   <li>DST only affects displayed fields within that timezone</li>
	     * </ul>
	     */
	    ZonedInstant
	}	
	
	
	/**
	 * Creates an instant-valued OADateTime initialized to the current system time.
	 */
	public OADateTime() {
		this._time = System.currentTimeMillis();
	}

	/**
	 * Creates an uninitialized OADateTime for subclass use.
	 * <p>
	 * No fields are initialized. Subclasses are responsible for assigning
	 * {@link #_time}, {@link #zoneId}, and {@link #type}.
	 *
	 * @param bNoInit ignored flag used to bypass normal initialization
	 */
	protected OADateTime(boolean bNoInit) {
	    // no-op for subclasses
	}	
	
	/**
	 * Creates an instant-valued OADateTime using milliseconds from the epoch.
	 *
	 * @param time milliseconds since the epoch
	 */
	public OADateTime(long time) {
		this._time = time;
	}
	

	/**
	 * Creates an OADateTime using milliseconds from the epoch and an optional zone.
	 * <p>
	 * The supplied zone affects field access and formatting; the stored epoch
	 * milliseconds are not recalculated.
	 *
	 * @param time milliseconds since the epoch
	 * @param zid zone to associate with this value, or {@code null} to use the default zone
	 */
	public OADateTime(long time, ZoneId zid) {
		this._time = time;
		this.zoneId = zid;
	}

	/**
	 * Creates an OADateTime using milliseconds from the epoch and an optional legacy {@link TimeZone}.
	 *
	 * @param time milliseconds since the epoch
	 * @param tz legacy timezone to associate with this value, or {@code null} to use the default zone
	 */
	public OADateTime(long time, TimeZone tz) {
		this._time = time;
		if (tz != null) this.zoneId = tz.toZoneId();
	}
	

	/**
	 * Creates an OADateTime from explicit local date/time fields in the supplied zone.
	 * <p>
	 * Month follows {@code java.time} numbering: January is {@code 1}, December is {@code 12}.
	 *
	 * @param zoneId zone used to resolve the local fields; {@code null} uses the OA default zone
	 * @param year full year
	 * @param month month from 1 to 12
	 * @param day day of month
	 * @param hrs hour of day from 0 to 23
	 * @param mins minute of hour
	 * @param secs second of minute
	 * @param milsecs millisecond of second from 0 to 999
	 * @throws RuntimeException if {@code milsecs} is outside 0..999
	 * @throws DateTimeException if the supplied fields do not form a valid date/time
	 */
	public OADateTime(ZoneId zoneId, int year, int month, int day, int hrs, int mins, int secs, int milsecs) {
	    this.zoneId = zoneId;
	    if (zoneId == null) type = DateTimeType.Floating;
	    else type = DateTimeType.ZonedInstant;

	    if (milsecs < 0 || milsecs > 999) {
	        throw new RuntimeException("Invalid millisecond value: " + milsecs);
	    }
	    
	    LocalDateTime ldt = LocalDateTime.of(
	        year,
	        month,
	        day,
	        hrs,
	        mins,
	        secs,
	        milsecs * 1_000_000
	    );
	    this._time = ldt.atZone(getZoneId()).toInstant().toEpochMilli();
	}	
	
	/**
	 * Creates an OADateTime from local date/time fields using the OA default zone.
	 *
	 * @param year full year
	 * @param month month from 1 to 12
	 * @param day day of month
	 * @param hrs hour of day from 0 to 23
	 * @param mins minute of hour
	 * @param secs second of minute
	 * @param milsecs millisecond of second from 0 to 999
	 */
	public OADateTime(int year, int month, int day, int hrs, int mins, int secs, int milsecs) {
		this(null, year, month, day, hrs, mins, secs, milsecs);
	}	
	
	/**
	 * Creates an OADateTime for the start of the supplied date using the OA default zone.
	 *
	 * @param year full year
	 * @param month month from 1 to 12
	 * @param day day of month
	 */
	public OADateTime(int year, int month, int day) {
		this(year, month, day, 0, 0, 0, 0);
	}
	
	/**
	 * Creates an OADateTime from date, hour, and minute fields using the OA default zone.
	 *
	 * @param year full year
	 * @param month month from 1 to 12
	 * @param day day of month
	 * @param hrs hour of day from 0 to 23
	 * @param mins minute of hour
	 */
	public OADateTime(int year, int month, int day, int hrs, int mins) {
		this(year, month, day, hrs, mins, 0, 0);
	}

	/**
	 * Creates an OADateTime from date and time fields using the OA default zone.
	 *
	 * @param year full year
	 * @param month month from 1 to 12
	 * @param day day of month
	 * @param hrs hour of day from 0 to 23
	 * @param mins minute of hour
	 * @param secs second of minute
	 */
	public OADateTime(int year, int month, int day, int hrs, int mins, int secs) {
		this(year, month, day, hrs, mins, secs, 0);
	}
	
	/**
	 * Creates an OADateTime using a {@link Month} enum for the month value.
	 *
	 * @param year full year
	 * @param month month enum value
	 * @param day day of month
	 * @param hrs hour of day from 0 to 23
	 * @param mins minute of hour
	 * @param secs second of minute
	 */
	public OADateTime(int year, Month month, int day, int hrs, int mins, int secs) {
		this(year, month.getValue(), day, hrs, mins, secs, 0);
	}
	
	/**
	 * Copies another OADateTime, preserving epoch milliseconds, zone metadata, and semantic type.
	 * <p>
	 * If {@code dt} is {@code null}, this value is initialized to the current system time.
	 *
	 * @param dt value to copy, or {@code null} for current time
	 */
	public OADateTime(OADateTime dt) {
		if (dt == null) {
			this._time = Instant.now().toEpochMilli();
		} 
		else {
			this._time = dt.getTime();
			this.zoneId = dt.zoneId;
			this.type = dt.type;
		}
	}

	/**
	 * Copies another OADateTime while replacing its associated zone.
	 * <p>
	 * This constructor preserves {@code _time}; it does not recalculate epoch milliseconds
	 * from wall-clock fields.
	 *
	 * @param dt value to copy, or {@code null} for current time
	 * @param zid replacement zone, or {@code null} to use the default zone when accessed
	 */
	public OADateTime(OADateTime dt, ZoneId zid) {
		if (dt == null) {
			this._time = Instant.now().toEpochMilli();
		} else {
			this._time = dt.getTime();
			this.type = dt.type;
		}
		this.zoneId = zid;
	}
	
	
	/**
	 * Creates a floating OADateTime by combining an OA date with an OA time.
	 * <p>
	 * If the date is {@code null}, the current OA date is used. If the time is
	 * {@code null}, midnight is used.
	 *
	 * @param d date portion, or {@code null} for current date
	 * @param t time portion, or {@code null} for midnight
	 */
	public OADateTime(OADate d, OATime t) {
	    if (d == null) {
	    	d = new OADate();
	    }
        int year = d.getYear();
        int month = d.getMonthValue();
        int day = d.getDayOfMonth();

	    final int hour;
	    final int minute;
	    final int second;
	    final int millisecond;
	    if (t == null) {
	        hour = 0;
	        minute = 0;
	        second = 0;
	        millisecond = 0;
	    } else {
	        hour = t.getHour();
	        minute = t.getMinute();
	        second = t.getSecond();
	        millisecond = t.getMilliSecond();
	    }

	    LocalDateTime ldt = LocalDateTime.of(
	        year,
	        month,
	        day,
	        hour,
	        minute,
	        second,
	        millisecond * 1_000_000
	    );

	    this.zoneId = defaultZoneId;
	    this._time = ldt.atZone(this.zoneId).toInstant().toEpochMilli();
	    this.type = DateTimeType.Floating;;
	}	

	/**
	 * Creates an OADateTime from a legacy {@link Calendar}.
	 * <p>
	 * The calendar instant is preserved. A non-default calendar zone is retained as a
	 * {@link DateTimeType#ZonedInstant}; otherwise the value is treated as an
	 * {@link DateTimeType#Instant}.
	 *
	 * @param c calendar to copy, or {@code null} for current system time
	 */
	public OADateTime(Calendar c) {
	    final long time;
	    ZoneId zId;
	    final DateTimeType dtType;

	    if (c == null) {
	        time = System.currentTimeMillis();
	        zId = null;
	        dtType = DateTimeType.Instant;
	    } else {
	        time = c.getTimeInMillis();
	        zId = c.getTimeZone().toZoneId();

	        if (zId != null && !zId.equals(defaultZoneId)) {
	            dtType = DateTimeType.ZonedInstant;
	        } else {
	            dtType = DateTimeType.Instant;
	            zId = null;
	        }
	    }

	    this._time = time;
	    this.zoneId = zId;
	    this.type = dtType;
	}
	
	/**
	 * Creates an instant-valued OADateTime from a {@link Instant}.
	 *
	 * @param instant instant to use, or {@code null} for current system time
	 */
	public OADateTime(Instant instant) {
	    if (instant == null) {
	        this._time = System.currentTimeMillis();
	    } 
	    else {
	        this._time = instant.toEpochMilli();
	    }
	    this.zoneId = null;
	    this.type = DateTimeType.Instant;
	}
	
	/**
	 * Creates a floating OADateTime from local date/time fields using the OA default zone.
	 *
	 * @param ldt local date/time fields, or {@code null} for current system time
	 */
	public OADateTime(LocalDateTime ldt) {
	    this.zoneId = defaultZoneId;
	    this.type = DateTimeType.Floating;

	    if (ldt == null) {
	        this._time = System.currentTimeMillis();
	    }
	    else {
		    this._time = ldt.atZone(this.zoneId).toInstant().toEpochMilli();
	    }
	}

	/**
	 * Creates a floating OADateTime at the start of the supplied local date using the OA default zone.
	 *
	 * @param ld local date, or {@code null} for the current local date
	 */
	public OADateTime(LocalDate ld) {
	    if (ld == null) ld = LocalDate.now(defaultZoneId);
	    this.zoneId = defaultZoneId;
		this.type = DateTimeType.Floating;
	    this._time = ld.atStartOfDay().atZone(this.zoneId).toInstant().toEpochMilli();
	}

	/**
	 * Creates a floating time-only value using {@code 1970-01-01} as the anchor date.
	 * <p>
	 * Nanoseconds are truncated to millisecond precision.
	 *
	 * @param time local time, or {@code null} for the current local time
	 */
	protected OADateTime(LocalTime time) {
	    if (time == null) {
	        time = LocalTime.now(defaultZoneId);
	    }

	    LocalDateTime ldt = LocalDateTime.of(LocalDate.of(1970, 1, 1), time.withNano((time.getNano() / 1_000_000) * 1_000_000));

	    this.zoneId = defaultZoneId;
	    this.type = DateTimeType.Floating;
	    this._time = ldt.atZone(this.zoneId).toInstant().toEpochMilli();
	}	
	
	/**
	 * Creates a zoned-instant OADateTime from a {@link ZonedDateTime}.
	 * <p>
	 * Both the instant and the zone are preserved.
	 *
	 * @param zdt zoned date/time, or {@code null} for current system time
	 */
	public OADateTime(ZonedDateTime zdt) {
	    if (zdt == null) {
	        this._time = System.currentTimeMillis();
	    }
	    else {
			this._time = zdt.toInstant().toEpochMilli();
			zoneId = zdt.getZone();
	    }
		this.type = DateTimeType.ZonedInstant;
	}
	
	/**
	 * Creates an instant-valued OADateTime from a legacy {@link Date}.
	 *
	 * @param date date to copy, or {@code null} for current system time
	 */
	public OADateTime(Date date) {
		if (date == null) {
			this._time = System.currentTimeMillis();
		} else {
			this._time = date.getTime();
		}
		this.type = DateTimeType.Instant;
	}

	/**
	 * Creates an instant-valued OADateTime from a JDBC {@link Timestamp}.
	 *
	 * @param ts timestamp to copy, or {@code null} for current system time
	 */
	public OADateTime(Timestamp ts) {
		if (ts == null) {
			this._time = System.currentTimeMillis();
		} else {
			this._time = ts.getTime();
		}
		this.type = DateTimeType.Instant;
	}
	
	/**
	 * Parses a string into an OADateTime using the global parse formats.
	 *
	 * @param strDate text to parse, or {@code null} for current system time
	 * @see #valueOf(String)
	 */
	public OADateTime(String strDate) {
		this(strDate, null);
	}

	/**
	 * Parses a string into an OADateTime using the supplied format and optional fallback formats.
	 *
	 * @param strDate text to parse, or {@code null} for current system time
	 * @param format preferred parse pattern
	 * @throws IllegalArgumentException if the non-null text cannot be parsed
	 * @see #valueOf(String, String)
	 */
	public OADateTime(String strDate, String format) {
		if (strDate == null) {
			this._time = System.currentTimeMillis();
		} else {
			OADateTime dt = valueOf(strDate, format);
			if (dt == null) {
				throw new IllegalArgumentException("OADateTime cant create date from String \"" + strDate + "\"");
			}
			this._time = dt.getTime();
			this.zoneId = dt.zoneId;
			this.type = dt.type;
		}
	}

	
	/**
	 * Returns the stored epoch-millisecond value.
	 *
	 * @return milliseconds from the epoch
	 */
	public long getTime() {
		return _time;
	}

	/**
	 * Returns the semantic type for this value.
	 *
	 * @return semantic date/time type
	 */
	public DateTimeType getType() {
		return type;
	}
	
	/**
	 * Returns the effective zone for this value.
	 * <p>
	 * If no instance zone is assigned, the OA default zone is returned.
	 *
	 * @return effective zone id
	 */
	public ZoneId getZoneId() {
		if (this.zoneId == null) return defaultZoneId;
		return this.zoneId;
	}

	/**
	 * Returns the effective zone as a legacy {@link TimeZone}.
	 *
	 * @return legacy timezone equivalent of {@link #getZoneId()}
	 */
	public TimeZone getTimeZone() {
		return TimeZone.getTimeZone(getZoneId());
	}
	
	/**
	 * Returns the local date/time fields for this value using the effective zone.
	 *
	 * @return local date/time representation
	 */
	public LocalDateTime toLocalDateTime() {
	    return Instant.ofEpochMilli(_time).atZone(getZoneId()).toLocalDateTime();
	}	

	/**
	 * Returns this value as a {@link ZonedDateTime} using the effective zone.
	 *
	 * @return zoned date/time representation
	 */
	public ZonedDateTime toZonedDateTime() {
		return Instant.ofEpochMilli(_time).atZone(getZoneId());
	}

	/**
	 * Returns this value as an {@link Instant} based on {@link #_time}.
	 *
	 * @return instant representation
	 */
	public Instant toInstant() {
		Instant instant = Instant.ofEpochMilli(_time);
		return instant;
	}

	/**
	 * Converts this value to a JDBC Timestamp.
	 *
	 * Instant and ZonedInstant values preserve their instant.
	 *
	 * Floating values are not semantically compatible with Timestamp because
	 * Timestamp represents an instant. For legacy/JDBC interoperability, floating
	 * values are anchored to UTC while preserving wall-clock fields. This is a
	 * lossy conversion and should not be used where floating semantics matter.
	 */
	public Timestamp toTimestamp() {
		Instant instant;
		if (type == DateTimeType.Floating) instant = withTimeZoneUTCSameWallTime().toInstant();
		else instant = toInstant();
		
	    return Timestamp.from(instant);
	}

	/**
	 * Converts this value to a JDBC date using the timestamp conversion rules.
	 *
	 * @return JDBC date value
	 */
	public java.sql.Date toSqlDate() {
		return new java.sql.Date(toTimestamp().getTime());
	}

	/**
	 * Converts this value to a legacy {@link java.util.Date} using the timestamp conversion rules.
	 *
	 * @return legacy date value
	 */
	public java.util.Date toDate() {
		return new java.util.Date(toTimestamp().getTime());
	}
	
	/**
	 * Writes the custom serialized representation for this value.
	 * <p>
	 * The serialized form is versioned and consists of:
	 * <ul>
	 *   <li>serialization version</li>
	 *   <li>{@link DateTimeType}</li>
	 *   <li>{@link #_time}</li>
	 *   <li>optional {@link #zoneId}</li>
	 * </ul>
	 *
	 * <h3>Serialization semantics</h3>
	 * <ul>
	 *   <li>{@link DateTimeType#Instant}: serializes {@code _time} only.</li>
	 *   <li>{@link DateTimeType#ZonedInstant}: serializes {@code _time} and
	 *       {@code zoneId}.</li>
	 *   <li>{@link DateTimeType#Floating}: serializes {@code _time} and
	 *       {@code zoneId} so that local wall-clock fields can be reconstructed
	 *       during deserialization.</li>
	 * </ul>
	 *
	 * @param stream destination stream
	 * @throws IOException if serialization fails
	 */
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeInt(1); // version
		
		stream.writeUTF(type.name());
		stream.writeLong(_time);

		if (!DateTimeType.Instant.equals(type)) {
			stream.writeUTF(getZoneId().getId());
		}
	}


	/**
	 * Reads the custom serialized representation written by
	 * {@link #writeObject(java.io.ObjectOutputStream)}.
	 *
	 * <h3>Deserialization semantics</h3>
	 * <ul>
	 *   <li>{@link DateTimeType#Instant}: restores the serialized
	 *       {@code _time} value directly.</li>
	 *   <li>{@link DateTimeType#ZonedInstant}: restores both
	 *       {@code _time} and {@code zoneId} directly.</li>
	 *   <li>{@link DateTimeType#Floating}: reconstructs the local wall-clock
	 *       fields using the serialized {@code _time} and {@code zoneId},
	 *       then resolves those same local fields using the current
	 *       {@link #defaultZoneId}. The resulting {@code _time} is recalculated
	 *       and {@code zoneId} is replaced with the current
	 *       {@code defaultZoneId}.</li>
	 * </ul>
	 *
	 * <p>
	 * Floating deserialization is treated as a new creation event on the
	 * receiving JVM. Local date/time fields are preserved, while the resolved
	 * instant is recalculated for the receiver's default zone.
	 *
	 * @param in source stream
	 * @throws IOException if the serialized form is invalid or unreadable
	 * @throws ClassNotFoundException if a required class cannot be resolved
	 */
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		final int version = in.readInt();
		if (version != 1) {
	        throw new IOException("Unknown OADateTime serialized version: " + version);
	    }		
		
		final String s = in.readUTF();
		this.type = DateTimeType.valueOf(s);
		this._time = in.readLong();
		if (!DateTimeType.Instant.equals(this.type)) {
			String zId = in.readUTF();
			this.zoneId = ZoneId.of(zId);
			if (DateTimeType.Floating.equals(this.type)) {
				LocalDateTime ldt = Instant.ofEpochMilli(this._time).atZone(zoneId).toLocalDateTime();
				this._time = ldt.atZone(defaultZoneId).toInstant().toEpochMilli();
				zoneId = defaultZoneId;				
			}
		}
	}
	
	/**
	 * Returns the local date portion using the effective zone.
	 *
	 * @return local date
	 */
	public LocalDate getLocalDate() {
	    return toZonedDateTime().toLocalDate();
	}

	/**
	 * Returns the local time portion using the effective zone, truncated to millisecond precision.
	 *
	 * @return local time
	 */
	public LocalTime getLocalTime() {
	    ZonedDateTime zdt = toZonedDateTime();
	    return zdt.toLocalTime().withNano( (zdt.getNano() / 1_000_000) * 1_000_000 );
	}
	
	/**
	 * Returns a supported {@link ChronoField} value using the effective zone.
	 *
	 * @param fld field to read
	 * @return field value
	 * @throws DateTimeException if the field is not supported by the zoned representation
	 */
	public int getField(ChronoField fld) {
		int x = toInstant().atZone(getZoneId()).get(fld);
		return x;
	}

	/**
	 * Returns a legacy {@link Calendar} representation using the effective zone.
	 *
	 * @return non-lenient GregorianCalendar set to this value
	 */
	public Calendar getCalendar() {
		GregorianCalendar cal = new GregorianCalendar();
		TimeZone tz = TimeZone.getTimeZone(getZoneId());		
	    cal.setTimeZone(tz);
	    cal.setLenient(false);
	    cal.setTimeInMillis(_time);

	    return cal;
	}

	/**
	 * Returns a new OADateTime with all date/time fields replaced, preserving this value's zone and semantic type.
	 *
	 * @param year full year
	 * @param month month from 1 to 12
	 * @param day day of month
	 * @param hrs hour of day from 0 to 23
	 * @param mins minute of hour
	 * @param secs second of minute
	 * @param milsecs millisecond of second from 0 to 999
	 * @return new value with the supplied fields
	 */
	public OADateTime withDateTime(int year, int month, int day, int hrs, int mins, int secs, int milsecs) {
		OADateTime dt = createUtil(year, month, day, hrs, mins, secs, milsecs);
		return dt;
	}

	/**
	 * Creates a new value using this instance's effective zone and semantic type.
	 *
	 * @param year full year
	 * @param month month from 1 to 12
	 * @param day day of month
	 * @param hrs hour of day from 0 to 23
	 * @param mins minute of hour
	 * @param secs second of minute
	 * @param milsecs millisecond of second
	 * @return new value using the supplied fields
	 */
	protected OADateTime createUtil(int year, int month, int day, int hrs, int mins, int secs, int milsecs) {
		return this.createUtil(getZoneId(), year, month, day, hrs, mins, secs, milsecs);
	}
	
	/**
	 * Creates a new value using the supplied zone and this instance's semantic type.
	 *
	 * @param zid zone used to interpret the supplied local fields
	 * @param year full year
	 * @param month month from 1 to 12
	 * @param day day of month
	 * @param hrs hour of day from 0 to 23
	 * @param mins minute of hour
	 * @param secs second of minute
	 * @param milsecs millisecond of second
	 * @return new value using the supplied fields
	 */
	protected OADateTime createUtil(ZoneId zid, int year, int month, int day, int hrs, int mins, int secs, int milsecs) {
	    OADateTime dt = new OADateTime(zid, year, month, day, hrs, mins, secs, milsecs);
	    dt.type = this.type;
	    return dt;
	}	
	/**
	 * Creates a new value from epoch milliseconds and a zone, preserving this instance's semantic type.
	 *
	 * @param time epoch-millisecond value
	 * @param zid zone to associate with the value
	 * @return new value using the supplied instant and zone
	 */
	protected OADateTime createUtil(long time, ZoneId zid) {
	    OADateTime dt = new OADateTime(time, zid);
	    dt.type = this.type;
	    return dt;
	}
	
	/**
	 * Creates a new OADateTime from a calculated {@link ZonedDateTime}, preserving this value's semantic type and zone metadata.
	 * <p>
	 * Subclasses may override to preserve subclass-specific return types.
	 *
	 * @param zdt calculated zoned date/time
	 * @return new OADateTime using the calculated instant
	 */
    protected OADateTime createUtil(ZonedDateTime zdt) {
		OADateTime dt = new OADateTime(zdt);
		dt.type = this.type;
		dt.zoneId = this.zoneId;
		return dt;
    }
	
	
	/**
	 * Returns a new OADateTime with the date fields replaced and the current time fields preserved.
	 *
	 * @param year full year
	 * @param month month from 1 to 12
	 * @param day day of month
	 * @return new value with the supplied date
	 */
	public OADateTime withDate(int year, int month, int day) {
		ZonedDateTime ldt = toZonedDateTime();
		OADateTime dt = createUtil(year, month, day, ldt.getHour(), ldt.getMinute(), ldt.getSecond(), ldt.getNano() / 1_000_000);
		dt.type = this.type;
		return dt;
	}

	/**
	 * Returns a new OADateTime with the year replaced.
	 *
	 * @param year replacement year
	 * @return new value with the supplied year
	 */
	public OADateTime withYear(int year) {
		ZonedDateTime ldt = toZonedDateTime();
		OADateTime dt = createUtil(year, ldt.getMonthValue(), ldt.getDayOfMonth(), ldt.getHour(), ldt.getMinute(), ldt.getSecond(), ldt.getNano() / 1_000_000);
		dt.type = this.type;
		return dt;
	}

	/**
	 * Returns a new OADateTime with the month replaced.
	 *
	 * @param month replacement month
	 * @return new value with the supplied month
	 */
	public OADateTime withMonth(Month month) {
		ZonedDateTime ldt = toZonedDateTime();
		OADateTime dt = createUtil(ldt.getYear(), month.getValue(), ldt.getDayOfMonth(), ldt.getHour(), ldt.getMinute(), ldt.getSecond(), ldt.getNano() / 1_000_000);
		dt.type = this.type;
		return dt;
	}

	/**
	 * Returns a new OADateTime with the month replaced.
	 *
	 * @param month replacement month from 1 to 12
	 * @return new value with the supplied month
	 */
	public OADateTime withMonthValue(int month) {
		ZonedDateTime ldt = toZonedDateTime();
		OADateTime dt = createUtil(ldt.getYear(), month, ldt.getDayOfMonth(), ldt.getHour(), ldt.getMinute(), ldt.getSecond(), ldt.getNano() / 1_000_000);
		dt.type = this.type;
		return dt;
	}
	
	/**
	 * Returns a new OADateTime with the day-of-month replaced.
	 *
	 * @param dom replacement day of month
	 * @return new value with the supplied day of month
	 */
	public OADateTime withDayOfMonth(int dom) {
		ZonedDateTime ldt = toZonedDateTime();
		OADateTime dt = createUtil(ldt.getYear(), ldt.getMonthValue(), dom, ldt.getHour(), ldt.getMinute(), ldt.getSecond(), ldt.getNano() / 1_000_000);
		dt.type = this.type;
		return dt;
	}
	
	/**
	 * Returns a new OADateTime with the date portion anchored to {@code 1970-01-01} and the time portion preserved.
	 *
	 * @return new value with the anchor date
	 */
	public OADateTime withoutDate() {
		return withDate(1970, Month.JANUARY.getValue(), 1);
	}
	
	/**
	 * Returns a new OADateTime with the date portion copied from an {@link OADate}.
	 * <p>
	 * If {@code d} is {@code null}, the date portion is anchored to {@code 1970-01-01}.
	 *
	 * @param d date source, or {@code null} to remove business date fields
	 * @return new value with the supplied date portion
	 */
	public OADateTime withDate(OADate d) {
		if (d == null) {
			return withoutDate();
		}
		return withDate(d.getYear(), d.getMonthValue(), d.getDayOfMonth());
	}

	/**
	 * Returns a new OADateTime with the time fields replaced and the current date fields preserved.
	 *
	 * @param hours hour of day from 0 to 23
	 * @param minutes minute of hour
	 * @param seconds second of minute
	 * @param millisecond millisecond of second from 0 to 999
	 * @return new value with the supplied time
	 */
	public OADateTime withTime(int hours, int minutes, int seconds, int millisecond) {
		ZonedDateTime ldt = toZonedDateTime();
		OADateTime dt = createUtil(ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth(), hours, minutes, seconds, millisecond);
		dt.type = this.type;
		return dt;
	}

	/**
	 * Returns a new OADateTime with hour, minute, and second replaced and milliseconds set to zero.
	 *
	 * @param hours hour of day from 0 to 23
	 * @param minutes minute of hour
	 * @param seconds second of minute
	 * @return new value with the supplied time
	 */
	public OADateTime withTime(int hours, int minutes, int seconds) {
		return withTime(hours, minutes, seconds, 0);
	}
	
	
	/**
	 * Returns a new OADateTime with hour and minute replaced, and seconds/milliseconds set to zero.
	 *
	 * @param hours hour of day from 0 to 23
	 * @param minutes minute of hour
	 * @return new value with the supplied hour and minute
	 */
	public OADateTime withTime(int hours, int minutes) {
		return withTime(hours, minutes, 0, 0);
	}
	
	/**
	 * Returns a new OADateTime with the hour replaced.
	 *
	 * @param hours replacement hour of day from 0 to 23
	 * @return new value with the supplied hour
	 */
	public OADateTime withHours(int hours) {
		ZonedDateTime ldt = toZonedDateTime();
		return withTime(hours, ldt.getMinute(), ldt.getSecond(), ldt.getNano() / 1_000_000);
	}

	/**
	 * Returns a new OADateTime with the minute replaced.
	 *
	 * @param minutes replacement minute of hour
	 * @return new value with the supplied minute
	 */
	public OADateTime withMinutes(int minutes) {
		ZonedDateTime ldt = toZonedDateTime();
		return withTime(ldt.getHour(), minutes, ldt.getSecond(), ldt.getNano() / 1_000_000);
	}

	/**
	 * Returns a new OADateTime with the second replaced.
	 *
	 * @param seconds replacement second of minute
	 * @return new value with the supplied second
	 */
	public OADateTime withSeconds(int seconds) {
		ZonedDateTime ldt = toZonedDateTime();
		return withTime(ldt.getHour(), ldt.getMinute(), seconds, ldt.getNano() / 1_000_000);
	}

	/**
	 * Returns a new OADateTime with the millisecond replaced.
	 *
	 * @param ms replacement millisecond of second from 0 to 999
	 * @return new value with the supplied millisecond
	 */
	public OADateTime withMilliSeconds(int ms) {
		ZonedDateTime ldt = toZonedDateTime();
		return withTime(ldt.getHour(), ldt.getMinute(), ldt.getSecond(), ms);
	}
	
	/**
	 * Returns a new OADateTime with time fields set to midnight.
	 *
	 * @return new value with time set to 00:00:00.000
	 */
	public OADateTime withoutTime() {
		return withTime(0,0,0,0);
	}
	
	/**
	 * Returns a new OADateTime with time fields second and millisecond set to 0.
	 *
	 * @return new value with time set to HH:mm:00.000
	 */
	public OADateTime withoutSecondAndMilliSecond() {
		return withTime(getHour(), getMinute(),0,0);
	}
	
	/**
	 * Returns a new OADateTime with the time portion copied from an {@link OATime}.
	 * <p>
	 * If {@code t} is {@code null}, the time portion is set to midnight.
	 *
	 * @param t time source, or {@code null} for midnight
	 * @return new value with the supplied time portion
	 */
	public OADateTime withTime(OATime t) {
		if (t == null) {
			return withoutTime();
		}
		return withTime(t.getHour(), t.getMinute(), t.getSecond(), t.getMilliSecond());
	}
	
	
	/**
	 * Returns the local year using the effective zone.
	 *
	 * @return year
	 */
	public int getYear() {
		ZonedDateTime dt = toZonedDateTime();
		return dt.get(ChronoField.YEAR);
	}

	/**
	 * Returns the local month value using {@code java.time} numbering.
	 *
	 * @return month from 1 to 12
	 */
	public int getMonthValue() {
		ZonedDateTime dt = toZonedDateTime();
		return dt.get(ChronoField.MONTH_OF_YEAR);
	}

	/**
	 * Returns the local month enum using the effective zone.
	 *
	 * @return month enum
	 */
	public Month getMonth() {
		ZonedDateTime dt = toZonedDateTime();
		return dt.getMonth();
	}
	
	/**
	 * Returns the local day of month using the effective zone.
	 *
	 * @return day of month
	 */
	public int getDayOfMonth() {
		ZonedDateTime dt = toZonedDateTime();
		return dt.get(ChronoField.DAY_OF_MONTH);
	}
	
	/**
	 * Returns the local hour of day using the effective zone.
	 *
	 * @return hour of day from 0 to 23
	 */
	public int getHour() {
		ZonedDateTime dt = toZonedDateTime();
		return dt.get(ChronoField.HOUR_OF_DAY);
	}

	/**
	 * Returns the local hour of day using the effective zone.
	 *
	 * @return hour of day from 0 to 23
	 */
	public int get24Hour() {
		ZonedDateTime dt = toZonedDateTime();
		return dt.get(ChronoField.HOUR_OF_DAY);
	}

	/**
	 * Returns the local minute of hour using the effective zone.
	 *
	 * @return minute of hour
	 */
	public int getMinute() {
		ZonedDateTime dt = toZonedDateTime();
		return dt.get(ChronoField.MINUTE_OF_HOUR);
	}
	
	/**
	 * Returns the local second of minute using the effective zone.
	 *
	 * @return second of minute
	 */
	public int getSecond() {
		ZonedDateTime dt = toZonedDateTime();
		return dt.get(ChronoField.SECOND_OF_MINUTE);
	}
	
	/**
	 * Returns the local millisecond of second using the effective zone.
	 *
	 * @return millisecond of second
	 */
	public int getMilliSecond() {
		ZonedDateTime dt = toZonedDateTime();
		return dt.get(ChronoField.NANO_OF_SECOND) / 1_000_000;
	}
	
	/**
	 * Returns the zero-based calendar quarter.
	 *
	 * @return quarter from 0 to 3
	 */
	public int getQuarter() {
		return (getMonthValue() - 1) / 3;		
	}

	/**
	 * Returns a new value associated with the supplied zone while preserving the displayed wall-clock fields.
	 * <p>
	 * The local year/month/day/hour/minute/second/millisecond stay the same, and
	 * {@code _time} is recalculated for the target zone.
	 *
	 * @param zid target zone, or {@code null} for the OA default zone
	 * @return new value with the same wall-clock fields in the target zone
	 */
	public OADateTime withZoneIdSameWallTime(ZoneId zid) {
	    if (zid == null) zid = defaultZoneId;

	    LocalDateTime ldt = toLocalDateTime();

	    OADateTime dt = createUtil(
	        zid,
	        ldt.getYear(),
	        ldt.getMonthValue(),
	        ldt.getDayOfMonth(),
	        ldt.getHour(),
	        ldt.getMinute(),
	        ldt.getSecond(),
	        ldt.getNano() / 1_000_000
	    );

	    dt.type = this.type;
	    return dt;
	}

	/**
	 * Returns a new value associated with the supplied zone while preserving {@code _time}.
	 * <p>
	 * The instant remains the same; displayed local fields may change in the target zone.
	 *
	 * @param zid target zone, or {@code null} for the OA default zone
	 * @return new value with the same instant in the target zone
	 * @see ZoneOffset#UTC
	 */
	public OADateTime withZoneIdSameInstant(ZoneId zid) {
	    if (zid == null) zid = defaultZoneId;
	    OADateTime dt = createUtil(this._time, zid);
	    return dt;
	}
	
	/**
	 * Returns a new value displayed in UTC while preserving {@code _time}.
	 *
	 * @return new UTC-zoned value for the same instant
	 */
	public OADateTime withTimeZoneUTCSameInstant() {
		ZoneId zid = ZoneOffset.UTC;
		return withZoneIdSameInstant(zid);
	}

	/**
	 * Returns a new value in UTC while preserving local wall-clock fields.
	 *
	 * @return new UTC-zoned value with the same displayed local fields
	 */
	public OADateTime withTimeZoneUTCSameWallTime() {
		ZoneId zid = ZoneOffset.UTC;
		return withZoneIdSameWallTime(zid);
	}
	
	/**
	 * Returns this value as a legacy {@link Date} using {@link #_time}.
	 *
	 * @return legacy Date for the stored instant
	 */
	public Date getDate() {
		return new Date(_time);
	}

	/**
	 * Returns the local day of week using the effective zone.
	 *
	 * @return day-of-week enum
	 */
	public DayOfWeek getDayOfWeek() {
		return toZonedDateTime().getDayOfWeek();
	}

	/**
	 * Returns the local day of year using the effective zone.
	 *
	 * @return day of year from 1 to 365/366
	 */
	public int getDayOfYear() {
		return toZonedDateTime().getDayOfYear();
	}

	/**
	 * Returns the locale-specific week of month using {@link WeekFields} for the default locale.
	 *
	 * @return week of month
	 */
	public int getWeekOfMonth() {
		ZonedDateTime zdt = toZonedDateTime();
		return zdt.get(WeekFields.of(Locale.getDefault()).weekOfMonth());
	}

	/**
	 * Returns the locale-specific week of year using {@link WeekFields} for the default locale.
	 *
	 * @return week of year
	 */
	public int getWeekOfYear() {
		ZonedDateTime zdt = toZonedDateTime();
		return zdt.get(WeekFields.of(Locale.getDefault()).weekOfYear());
	}

	/**
	 * Returns the number of days in this value's local month.
	 *
	 * @return days in month
	 */
	public int getDaysInMonth() {
		ZonedDateTime zdt = toZonedDateTime();
		return zdt.toLocalDate().lengthOfMonth();
	}

	/**
	 * Returns the number of days in this value's local year.
	 *
	 * @return days in year
	 */
	public int getDaysInYear() {
		ZonedDateTime zdt = toZonedDateTime();
		return zdt.toLocalDate().lengthOfYear();
	}
	
	/**
	 * Compares this value to another OADateTime by stored epoch milliseconds.
	 *
	 * @param obj object to compare
	 * @return {@code true} when {@code obj} is an OADateTime with the same {@code _time}
	 */
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof OADateTime) {
			return this._time == ((OADateTime) obj)._time;
		}
		return false;
	}

	/**
	 * Returns a hash code based on the internal time value.
	 *
	 * @return hash code for this instance
	 */
	@Override
	/**
	 * Returns a hash code based on stored epoch milliseconds.
	 *
	 * @return hash code for {@link #_time}
	 */
	public int hashCode() {
		return Long.hashCode(_time);
	}
	
	/**
	 * Compares this value with another object using {@link #compareTo(Object)}.
	 *
	 * @param obj object to compare
	 * @return comparison result
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
	@Override
	/**
	 * Compares this value with another OADateTime by stored epoch milliseconds.
	 * <p>
	 * A {@code null} value sorts before this value. Non-OADateTime objects sort after
	 * valid OADateTime values by returning {@code 2}.
	 *
	 * @param obj object to compare
	 * @return negative, zero, or positive comparison result
	 */
	public int compareTo(Object obj) {
	    if (obj == null) {
	        return 1;
	    }
	    if (!(obj instanceof OADateTime)) {
	        return 2;
	    }

	    OADateTime other = (OADateTime) obj;
	    return Long.compare(this._time, other._time);
	}
	
	/**
	 * Returns whether this value is before another value according to {@link #compareTo(Object)}.
	 *
	 * @param obj object to compare
	 * @return {@code true} if this value is before {@code obj}
	 */
	public boolean before(Object obj) {
		return (compareTo(obj) < 0);
	}

	/**
	 * Returns whether this value is before another value according to {@link #compareTo(Object)}.
	 *
	 * @param obj object to compare
	 * @return {@code true} if this value is before {@code obj}
	 */
	public boolean isBefore(Object obj) {
		return (compareTo(obj) < 0);
	}

	/**
	 * Returns whether this value is after another value according to {@link #compareTo(Object)}.
	 *
	 * @param obj object to compare
	 * @return {@code true} if this value is after {@code obj}
	 */
	public boolean after(Object obj) {
		return (compareTo(obj) > 0);
	}

	/**
	 * Returns whether this value is after another value according to {@link #compareTo(Object)}.
	 *
	 * @param obj object to compare
	 * @return {@code true} if this value is after {@code obj}
	 */
	public boolean isAfter(Object obj) {
		return (compareTo(obj) > 0);
	}

	/**
	 * Delegates to {@link #isBetweenOrEqual(Object, Object)}.
	 *
	 * @param obj1 lower bound
	 * @param obj2 upper bound
	 * @return {@code true} if this value is between the bounds inclusively
	 */
    public boolean betweenOrEqual(Object obj1, Object obj2) {
        return isBetweenOrEqual(obj1, obj2);
    }

	/**
	 * Tests whether this value is between two values inclusively.
	 *
	 * @param obj1 lower bound
	 * @param obj2 upper bound
	 * @return {@code true} if this value is greater than or equal to {@code obj1} and less than or equal to {@code obj2}
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
	 * @param obj1 lower bound
	 * @param obj2 upper bound
	 * @return {@code true} if this value is strictly between the bounds
	 */
    public boolean betweenNotEqual(Object obj1, Object obj2) {
        return isBetweenNotEqual(obj1, obj2);
    }
    
	/**
	 * Tests whether this value is strictly between two values.
	 *
	 * @param obj1 lower bound
	 * @param obj2 upper bound
	 * @return {@code true} if this value is greater than {@code obj1} and less than {@code obj2}
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
	 * Returns a new value with the supplied number of calendar years added.
	 *
	 * @param amount years to add; negative values subtract
	 * @return adjusted value
	 */
	public OADateTime plusYears(int amount) {
		ZonedDateTime zdt = toZonedDateTime().plusYears(amount);
		return createUtil(zdt);
	}

	/**
	 * Returns a new value with the supplied number of calendar years subtracted.
	 *
	 * @param amount years to subtract
	 * @return adjusted value
	 */
	public OADateTime subtractYears(int amount) {
		return plusYears(-amount);
	}
    
	/**
	 * Returns a new value with the supplied number of calendar months added.
	 *
	 * @param amount months to add; negative values subtract
	 * @return adjusted value
	 */
	public OADateTime plusMonths(int amount) {
		ZonedDateTime zdt = toZonedDateTime().plusMonths(amount);
		return createUtil(zdt);
	}

	/**
	 * Returns a new value with the supplied number of calendar months subtracted.
	 *
	 * @param amount months to subtract
	 * @return adjusted value
	 */
	public OADateTime minusMonths(int amount) {
		return plusMonths(-amount);
	}
    
	
	/**
	 * Returns a new value one calendar day after this value.
	 *
	 * @return adjusted value
	 */
	public OADateTime plusDays(int amount) {
		ZonedDateTime zdt = toZonedDateTime().plusDays(amount);
		return createUtil(zdt);
	}

	/**
	 * Returns a new value one calendar day before this value.
	 *
	 * @return adjusted value
	 */
	public OADateTime minusDays(int amount) {
		return plusDays(-amount);
	}

	/**
	 * Returns a new date/time incremented by one day.
	 *
	 * @return a new OADateTime instance
	 */
	public OADateTime plusDay() {
		return plusDays(1);
	}

	/**
	 * Returns a new date/time decremented by one day.
	 *
	 * @return a new OADateTime instance
	 */
	public OADateTime minusDay() {
		return plusDays(-1);
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
	 * Returns a new value with the supplied number of calendar weeks added.
	 *
	 * @param amount weeks to add; negative values subtract
	 * @return adjusted value
	 */
	public OADateTime addWeeks(int amount) {
		ZonedDateTime zdt = toZonedDateTime().plusWeeks(amount);
		return createUtil(zdt);
	}

	/**
	 * Returns a new value with the supplied number of calendar weeks subtracted.
	 *
	 * @param amount weeks to subtract
	 * @return adjusted value
	 */
	public OADateTime minusWeeks(int amount) {
		return plusDays(-(amount * 7));
	}

	/**
	 * Returns a new value with the supplied number of hours added.
	 *
	 * @param amount hours to add; negative values subtract
	 * @return adjusted value
	 */
	public OADateTime plusHours(int amount) {
		ZonedDateTime zdt = toZonedDateTime().plusHours(amount);
		return createUtil(zdt);
	}

	/**
	 * Returns a new value with the supplied number of hours subtracted.
	 *
	 * @param amount hours to subtract
	 * @return adjusted value
	 */
	public OADateTime minusHours(int amount) {
		return plusHours(-amount);
	}

	/**
	 * Returns a new value with the supplied number of minutes added.
	 *
	 * @param amount minutes to add; negative values subtract
	 * @return adjusted value
	 */
	public OADateTime plusMinutes(int amount) {
		ZonedDateTime zdt = toZonedDateTime().plusMinutes(amount);
		return createUtil(zdt);
	}

	/**
	 * Returns a new value with the supplied number of minutes subtracted.
	 *
	 * @param amount minutes to subtract
	 * @return adjusted value
	 */
	public OADateTime minusMinutes(int amount) {
		return plusMinutes(-amount);
	}

	/**
	 * Returns a new value with the supplied number of seconds added.
	 *
	 * @param amount seconds to add; negative values subtract
	 * @return adjusted value
	 */
	public OADateTime plusSeconds(int amount) {
		ZonedDateTime zdt = toZonedDateTime().plusSeconds(amount);
		return createUtil(zdt);
	}

	/**
	 * Returns a new value with the supplied number of seconds subtracted.
	 *
	 * @param amount seconds to subtract
	 * @return adjusted value
	 */
	public OADateTime minusSeconds(int amount) {
		return plusSeconds(-amount);
	}

	/**
	 * Returns a new value with the supplied number of milliseconds added.
	 *
	 * @param amount milliseconds to add; negative values subtract
	 * @return adjusted value
	 */
	public OADateTime plusMilliSeconds(int amount) {
		ZonedDateTime zdt = toZonedDateTime().plusNanos(amount * 1_000_000L);
		return createUtil(zdt);
	}

	/**
	 * Returns a new value with the supplied number of milliseconds subtracted.
	 *
	 * @param amount milliseconds to subtract
	 * @return adjusted value
	 */
	public OADateTime minusMilliSeconds(int amount) {
		return plusMilliSeconds(-amount);
	}

	/**
	 * Returns the calendar period between this value's local date and another value's local date.
	 *
	 * @param dt ending value
	 * @return calendar period, or {@link Period#ZERO} when {@code dt} is {@code null}
	 */
	public Period betweenPeriod(OADateTime dt) {
	    if (dt == null) {
	        return Period.ZERO;
	    }
	    return Period.between(
	        this.getLocalDate(),
	        dt.getLocalDate()
	    );
	}
	
	/**
	 * Returns the timeline duration between this value and another value using stored instants.
	 *
	 * @param dt ending value
	 * @return elapsed duration, or {@link Duration#ZERO} when {@code dt} is {@code null}
	 */
	public Duration betweenDuration(OADateTime dt) {
	    if (dt == null) {
	        return Duration.ZERO;
	    }
	    return Duration.between(
	        Instant.ofEpochMilli(this._time),
	        Instant.ofEpochMilli(dt._time)
	    );
	}	
	
	/**
	 * Returns the number of complete calendar years between the local dates of this value and another value.
	 *
	 * @param dt ending value
	 * @return complete years between local dates, or {@code 0} when {@code dt} is {@code null}
	 */
	public long betweenYears(OADateTime dt) {
	    if (dt == null) return 0;
	    return ChronoUnit.YEARS.between(this.getLocalDate(), dt.getLocalDate());	    
	}
	
	/**
	 * Returns the number of complete calendar months between the local dates of this value and another value.
	 *
	 * @param dt ending value
	 * @return complete months between local dates, or {@code 0} when {@code dt} is {@code null}
	 */
	public long betweenMonths(OADateTime dt) {
	    if (dt == null) return 0;
	    return ChronoUnit.MONTHS.between(this.getLocalDate(), dt.getLocalDate());	    
	}

	/**
	 * Returns the number of calendar days between the local dates of this value and another value.
	 *
	 * @param dt ending value
	 * @return days between local dates, or {@code 0} when {@code dt} is {@code null}
	 */
	public long betweenDays(OADateTime dt) {
	    if (dt == null) return 0;
	    return ChronoUnit.DAYS.between(this.getLocalDate(), dt.getLocalDate());	    
	}
	
	/**
	 * Returns the number of elapsed hours between this value and another value using instants.
	 *
	 * @param dt ending value
	 * @return elapsed hours, or {@code 0} when {@code dt} is {@code null}
	 */
	public long betweenHours(OADateTime dt) {
	    if (dt == null) return 0;
	    return ChronoUnit.HOURS.between(this.toInstant(), dt.toInstant());	    
	}
	
	/**
	 * Returns the number of elapsed minutes between this value and another value using instants.
	 *
	 * @param dt ending value
	 * @return elapsed minutes, or {@code 0} when {@code dt} is {@code null}
	 */
	public long betweenMinutes(OADateTime dt) {
	    if (dt == null) return 0;
	    return ChronoUnit.MINUTES.between(this.toInstant(), dt.toInstant());	    
	}
	
	/**
	 * Returns the number of elapsed seconds between this value and another value using instants.
	 *
	 * @param dt ending value
	 * @return elapsed seconds, or {@code 0} when {@code dt} is {@code null}
	 */
	public long betweenSeconds(OADateTime dt) {
	    if (dt == null) return 0;
	    return ChronoUnit.SECONDS.between(this.toInstant(), dt.toInstant());	    
	}
	
	/**
	 * Returns the number of elapsed milliseconds between this value and another value using instants.
	 *
	 * @param dt ending value
	 * @return elapsed milliseconds, or {@code 0} when {@code dt} is {@code null}
	 */
	public long betweenMilliSeconds(OADateTime dt) {
	    if (dt == null) return 0;
	    return ChronoUnit.MILLIS.between(this.toInstant(), dt.toInstant());	    
	}
	
	/**
	 * Converts supported date/time object types into OADateTime.
	 *
	 * @param obj source object; supports OADateTime, Date, Calendar, String, java.sql.Time, and java.sql.Timestamp
	 * @param bAlways when {@code true}, returns a copy for OADateTime inputs
	 * @return converted value, or {@code null} when unsupported
	 */
	public static OADateTime convert(Object obj, boolean bAlways) {
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
	}

	
	/**
	 * Parses text using global parse formats.
	 *
	 * @param strDateTime text to parse
	 * @return parsed value, or {@code null} when parsing fails
	 */
	public static OADateTime valueOf(String strDateTime) {
		return valueOf(strDateTime, null);
	}
	
	/**
	 * Parses text using a preferred format and fallback formats.
	 *
	 * @param strDateTime text to parse
	 * @param fmt preferred parse pattern
	 * @return parsed value, or {@code null} when parsing fails
	 */
	public static OADateTime valueOf(String strDateTime, String fmt) {
		return valueOf(strDateTime, fmt, true);
	}

	/**
	 * Parses text using a preferred format and optionally global fallback formats.
	 *
	 * @param strDateTime text to parse
	 * @param fmt preferred parse pattern
	 * @param bTryOtherFormats whether to try global fallback formats and output format
	 * @return parsed value, or {@code null} when parsing fails
	 */
	public static OADateTime valueOf(String strDateTime, String fmt, boolean bTryOtherFormats) {
		if (strDateTime == null) return null;
		OADateTime dt = valueOfMain(strDateTime, fmt, bTryOtherFormats ? alDateTimeParseFormat : null, bTryOtherFormats ? staticOutputFormat : null);
		return dt;
	}

	/**
	 * Attempts to parse text using a preferred input format, an output-format fallback, and additional fallback formats.
	 *
	 * @param value text to parse
	 * @param inputFormat preferred parse pattern
	 * @param alFormat fallback parse patterns
	 * @param outputFormat output-format fallback pattern
	 * @return parsed value, or {@code null} when no pattern succeeds
	 */
	protected static OADateTime valueOfMain(String value, String inputFormat, List<String> alFormat, String outputFormat) {
		if (value == null || value.length() == 0) {
			return null;
		}
		if (OAStr.trimSpaces(value).equals("")) {
			return new OADateTime();
		}

		String format = null;
		OADateTime dateTime = null;

		for (int i=0; dateTime == null; i++) {
			if (i == 0) {
				format = inputFormat;
			}
			else if (i == 1) {
				format = outputFormat;
			}
			else {
				if (alFormat == null) break;
				int pos = (i - 2);
				if (pos >= alFormat.size()) break;
				format = alFormat.get(pos);
			}
			if (format != null && format.length() > 0) {
				format = normalizeFormat(format);
				try {
					DateTimeFormatter fmt = DateTimeFormatter.ofPattern(format) .withResolverStyle(ResolverStyle.STRICT);
					dateTime = parseDateTime(value, fmt);
				}
				catch (Exception e) {
					dateTime = null; 
				}
			}
		}
		return dateTime;
	}

	/**
	 * Normalizes the first one or two non-alphanumeric separators in a date string to {@code '/'}.
	 *
	 * @param s source text
	 * @return normalized text, or an empty string when {@code s} is {@code null}
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
	
	/**
	 * Normalizes OA-compatible date/time patterns before creating a {@link DateTimeFormatter}.
	 * <p>
	 * Currently maps common calendar-year symbols to proleptic-year symbols for strict parsing.
	 *
	 * @param format source pattern
	 * @return normalized pattern
	 */
	protected static String normalizeFormat(String format) {
		if (format == null) return null;
	    return format.replace("yyyy", "uuuu").replace("yy", "uu");
	}

	/**
	 * Parses text with a {@link DateTimeFormatter} and derives the OADateTime semantic type from parsed zone/offset content.
	 * <p>
	 * Region zones produce {@link DateTimeType#ZonedInstant}, offsets produce
	 * {@link DateTimeType#Instant}, and local date/time values produce
	 * {@link DateTimeType#Floating}.
	 *
	 * @param text text to parse
	 * @param fmt formatter to use
	 * @return parsed value, or {@code null} when parsing fails or does not consume all input
	 */
	protected static OADateTime parseDateTime(String text, DateTimeFormatter fmt) {
	    ParsePosition pos = new ParsePosition(0);
	    TemporalAccessor ta;
	    
	    try {
	        ta = fmt.parse(text, pos);
	    }
	    catch (DateTimeException e) {
	        return null;
	    }
	    
	    if (pos.getErrorIndex() >= 0 || pos.getIndex() != text.length()) {
	        return null;
	    }

	    ZoneId zone = ta.query(TemporalQueries.zone());
	    ZoneOffset offset = ta.query(TemporalQueries.offset());
	    LocalDate date = ta.query(TemporalQueries.localDate());
	    LocalTime time = ta.query(TemporalQueries.localTime());

	    if (date == null) {
	        return null;
	    }
	    if (time == null) {
	        time = LocalTime.MIDNIGHT;
	    }

	    LocalDateTime ldt = LocalDateTime.of(date, time);

	    if (zone != null && !(zone instanceof ZoneOffset)) {
	        ZonedDateTime zdt = ZonedDateTime.of(ldt, zone);

	        OADateTime dt = new OADateTime(zdt.toInstant());
	        dt.zoneId = zone;
	        dt.type = DateTimeType.ZonedInstant;
	        return dt;
	    }

	    if (offset != null) {
	        OffsetDateTime odt = OffsetDateTime.of(ldt, offset);

	        OADateTime dt = new OADateTime(odt.toInstant());
	        dt.type = DateTimeType.Instant;
	        return dt;
	    }

	    OADateTime dt = new OADateTime(ldt);
	    dt.zoneId = defaultZoneId;
	    dt.type = DateTimeType.Floating;
	    return dt;
	}	
	
	/**
	 * Formats this value using the instance format, global output format, or default fallback format.
	 *
	 * @return formatted text
	 */
	public String toString() {
		return toString(null);
	}

	/**
	 * Formats this value using the supplied pattern, or configured defaults when {@code f} is {@code null}.
	 *
	 * @param f output pattern, or {@code null} for configured defaults
	 * @return formatted text
	 */
	public String toString(String f) {
		if (f == null) {
			f = (format == null) ? staticOutputFormat : format;
			if (f == null || f.length() == 0) {
				f = "yyyy-MMM-dd hh:mma";
				if (zoneId != null)
					f += " z";
			}
		}
		return toStringMain(f);
	}

	/**
	 * Formats this value using the supplied pattern after OA pattern normalization.
	 *
	 * @param format output pattern
	 * @return formatted text
	 * @throws IllegalArgumentException if the pattern is invalid
	 */
	public String toStringMain(String format) {
		if (format == null) format = getGlobalOutputFormat();
		format = normalizeFormat(format);
	    DateTimeFormatter fmt = DateTimeFormatter.ofPattern(format);
	    return toZonedDateTime().format(fmt);
	}	
	
	/**
	 * Sets the global output pattern used when an instance-specific format is not supplied.
	 *
	 * @param fmt output pattern
	 */
	public static void setGlobalOutputFormat(String fmt) {
		staticOutputFormat = fmt;
	}

	/**
	 * Returns the global output pattern.
	 *
	 * @return global output pattern
	 */
	public static String getGlobalOutputFormat() {
		return staticOutputFormat;
	}

	/**
	 * Adds a global fallback parse pattern.
	 *
	 * @param fmt parse pattern to add
	 */
	public static void addGlobalParseFormat(String fmt) {
		alDateTimeParseFormat.add(fmt);
	}

	/**
	 * Removes a global fallback parse pattern.
	 *
	 * @param fmt parse pattern to remove
	 */
	public static void removeGlobalParseFormat(String fmt) {
		alDateTimeParseFormat.remove(fmt);
	}

	/**
	 * Sets the instance-specific output pattern used by {@link #toString()}.
	 *
	 * @param fmt output pattern for this instance
	 */
	public void setFormat(String fmt) {
		this.format = fmt;
	}

	/**
	 * Returns the instance-specific output pattern.
	 *
	 * @return instance output pattern, or {@code null} when not set
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * Creates a non-lenient legacy {@link SimpleDateFormat}.
	 *
	 * @return new non-lenient formatter
	 */
	protected static SimpleDateFormat getFormatter() {
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.setLenient(false);
		return sdf;
	}

	/**
	 * Returns the locale-derived date pattern for the configured OA locale.
	 *
	 * @param type DateFormat style constant
	 * @return date pattern, or {@code null} when unavailable
	 */
	public static String getFormat(int type) {
		return getFormat(type, locale);
	}

	/**
	 * Returns the locale-derived date pattern for the supplied locale.
	 *
	 * @param type DateFormat style constant
	 * @param locale locale to inspect
	 * @return date pattern, or {@code null} when unavailable
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
	 * Returns the last day number for this value's local month.
	 *
	 * @return last day of month, for example 28, 29, 30, or 31
	 */
	public int getLastDayOfMonth() {
		int lastDay = toZonedDateTime().toLocalDate().lengthOfMonth();
		return lastDay;
	}
}

/* CODEX invariants 20260611
 
  Implementation invariants
  -------------------------
 
  Core storage
  - _time is always milliseconds from the Java epoch
    1970-01-01T00:00:00Z.
  - type identifies the semantic model for this value.
  - zoneId is optional metadata for Instant values, but is part of the
    authoritative state for ZonedInstant and Floating values.
  - Instances are immutable value objects. defaultZoneId is a creation,
    parsing, and deserialization setting only; changing defaultZoneId later
    must not change the meaning of an already-created value.
  - getZoneId() may fall back to defaultZoneId for legacy Instant-style values
    with no stored zoneId. Floating values should store a non-null zoneId so
    their displayed fields do not depend on future defaultZoneId changes.
 
  Type semantics
  - Instant: _time is authoritative. zoneId is not required. Display fields are
    derived from _time using the effective zone.
  - ZonedInstant: _time and zoneId are authoritative. The instant is fixed and
    the stored zone controls displayed fields.
  - Floating: local wall-clock fields are resolved using the effective/default
    zone at creation or deserialization. After resolution, the instance stores
    _time, zoneId, and type=Floating. Floating is not zone-free.
 
  Floating semantics
  - Floating values preserve the zone used to resolve their local fields.
  - Existing Floating instances must not be reinterpreted if defaultZoneId
    changes after creation.
  - Two Floating values with identical displayed fields are not guaranteed to
    compare equal if they were resolved using different zones, because _time is
    the comparison value.
 
  Serialization
  - Custom serialization version is currently 1.
  - Instant serializes type and _time only.
  - ZonedInstant serializes type, _time, and zoneId.
  - Floating serializes type, _time, and zoneId.
  - Floating deserialization is treated as a new creation event on the receiving
    JVM: read _time and serialized zoneId, derive local fields from
    _time + serialized zoneId, re-resolve those same local fields using current
    defaultZoneId, assign the adjusted _time, and assign zoneId=defaultZoneId.
  - This preserves Floating wall-clock fields across JVMs while giving the
    receiving JVM its own default-zone resolution.
 
  Comparison
  - equals(Object), hashCode(), compareTo(Object), compare(Object), and timeline
    interval methods use _time.
  - zoneId and DateTimeType do not participate in equality, hashing, or
    ordering.
  - Do not add date-field-only or time-field-only comparison domains here.
    OADate and OATime are constrained OA legacy value types, not pure
    LocalDate/LocalTime replacements.
 
  Zone conversion
  - withZoneIdSameInstant(ZoneId) preserves _time and changes the zone used for
    display. Local displayed fields may change.
  - withZoneIdSameWallTime(ZoneId) preserves local wall-clock fields and
    recalculates _time in the target zone.
  - Null target zones resolve to defaultZoneId at conversion time.
 
  Parsing and formatting
  - Formatting derives fields from _time plus the effective zone.
  - Parsing uses DateTimeFormatter with strict resolver behavior and must
    consume the full input.
  - normalizeFormat maps legacy year patterns such as yyyy/yy to uuuu/uu before
    DateTimeFormatter creation.
  - Parsed region-zone values become ZonedInstant.
  - Parsed offset-only values become Instant.
  - Parsed values with no zone or offset become Floating and capture the current
    defaultZoneId.
 
  Subclass support
  - Methods that create adjusted values should route through createUtil(...)
    so subclasses can preserve their runtime type and invariants.
  - OADate overrides createUtil(...) to remain a date-only
    Floating value with time normalized to 00:00:00.000.
  - OATime overrides createUtil(...) to remain a time-only
    Floating value with date normalized to 1970-01-01.
  - OATime has its own time-only parser because OADateTime parsing requires a
    date component.
*/


