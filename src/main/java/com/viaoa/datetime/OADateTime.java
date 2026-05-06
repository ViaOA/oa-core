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


*/

import java.io.IOException;
import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import com.viaoa.concurrent.OAPool;


/**
 * Superclass of OADate and OATime that combines Calendar, Date, Time, TimeZone,
 * LocalDateTime, and SimpleDateFormat. <br>
 * date/time value with optional time-zone awareness used across OA.
 * <p>
 * OADateTime stores an instant in milliseconds since the epoch and exposes a
 * rich set of calendar operations (add/subtract units, field getters/setters,
 * floor/ceil, comparisons, formatting/parsing) along with convenience views
 * {@link OADate} (date-only) and {@link OATime} (time-only). When a
 * {@link java.util.TimeZone} is attached, field accessors and formatting honor
 * that zone; otherwise the JVM default zone is used.
 *
 * <h3>Design goals</h3>
 * <ul>
 * <li>Correctness first: comparisons and arithmetic are based on the underlying
 * instant</li>
 * <li>Time-zone aware field access and formatting</li>
 * <li>Interop with {@code java.util.Date}, {@code Calendar}, and modern types
 * ({@code Instant}, {@code LocalDate/LocalTime/LocalDateTime},
 * {@code ZonedDateTime})</li>
 * <li>Null-safety: tolerant converters and value-of helpers</li>
 * </ul>
 *
 * <h3>Thread-safety</h3> Instances are immutable. Internal formatter pooling is
 * synchronized since {@link java.text.SimpleDateFormat} is not thread-safe.
 *
 * <h3>Performance</h3> Calendar/formatter objects are pooled to minimize
 * allocation overhead under load.
 *
 * <h3>Known behavior</h3> Arithmetic methods (e.g., {@code addDays},
 * {@code addMonths}) operate on the instant and allow DST transitions to shift
 * wall-clock times as defined by {@link Calendar}.
 */
public class OADateTime implements java.io.Serializable, Comparable {
	private static final long serialVersionUID = 1L;

	/**
	 * Long date/time format including milliseconds and AM/PM.
	 */
	public final static String FORMAT_long = "yyyy/MM/dd hh:mm:ss.S a";
	
	/**
	 * Long date/time format including milliseconds and time zone.
	 */
	public final static String FORMAT_xlong = "yyyy/MM/dd hh:mm:ss.S a z";

	/**
	 * Time value stored as milliseconds since the epoch.
	 */
	protected long _time;
	
	/**
	 * Optional time zone associated with this date/time.
	 */
	protected TimeZone timeZone;
	
	/**
	 * Flag indicating that time zone should be ignored during serialization.
	 */
	protected boolean ignoreTimeZone;

	/**
	 * Instance-specific output format used when converting to a string.
	 */
	protected String format;

	/**
	 * Default time zone used when no instance time zone is specified.
	 */
	private static TimeZone defaultTimeZone;

	/**
	 * Pool of {@link SimpleDateFormat} instances used for formatting and parsing.
	 */
	private static SimpleDateFormat[] simpleDateFormats;
	
	/**
	 * Counter used to rotate through the SimpleDateFormat pool.
	 */
	private static int simpleDateFormatCounter;
	
	static {
		// used by getFormatter()
		simpleDateFormats = new SimpleDateFormat[12]; // keeps a pool of 12 that are shared in a "round robin" pool
	}

	// RFC-339 format
	// Note: the 'Z' is not a timezone, it means that the timezone should be set to
	// UTC.
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
	private static Vector vecDateTimeParseFormat;

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
	 * Pool of GregorianCalendar instances used to reduce allocation overhead.
	 */
	private static final OAPool<GregorianCalendar> poolGregorianCalendar = new OAPool<GregorianCalendar>(GregorianCalendar.class, 20, 50) {
		@Override
		protected GregorianCalendar create() {
			GregorianCalendar cal = new GregorianCalendar();
			return cal;
		}

		@Override
		protected void removed(GregorianCalendar resource) {
		}
	};

	/**
	 * Obtains a GregorianCalendar from the pool, initializes it with the current
	 * time value, and applies the appropriate time zone.
	 *
	 * @return a pooled and initialized GregorianCalendar instance
	 */
	protected GregorianCalendar _getCal() {
		GregorianCalendar cal = poolGregorianCalendar.get();
		cal.setTimeInMillis(_time);

		TimeZone tz = timeZone != null ? timeZone : defaultTimeZone;
		if (cal.getTimeZone() != tz) {
			cal.setTimeZone(tz);
		}
		return cal;
	}

	/**
	 * Releases a previously obtained GregorianCalendar back to the pool.
	 *
	 * @param cal the GregorianCalendar to release
	 */
	protected void _releaseCal(GregorianCalendar cal) {
		poolGregorianCalendar.release(cal);
	}

	/**
	 * Locale used for date/time formatting and parsing.
	 */
	private static Locale locale;

	/**
	 * Sets the locale used for formatting and parsing date/time values and
	 * initializes global parse and output formats based on the locale.
	 *
	 * @param loc the Locale to use
	 */
	public static void setLocale(Locale loc) {
		locale = loc;
		vecDateTimeParseFormat = new Vector(15, 10);
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
			vecDateTimeParseFormat.addElement("MM/dd/yy hh:mm:ss.Sa");
			vecDateTimeParseFormat.addElement("MM/dd/yy hh:mm:ssa");
			vecDateTimeParseFormat.addElement("MM/dd/yy hh:mma");

			vecDateTimeParseFormat.addElement("MM/dd/yy hh:mm:ss.S a");
			vecDateTimeParseFormat.addElement("MM/dd/yy hh:mm:ss a");
			vecDateTimeParseFormat.addElement("MM/dd/yy hh:mm a");

			vecDateTimeParseFormat.addElement("MM/dd/yy HH:mm:ss.S");
			vecDateTimeParseFormat.addElement("MM/dd/yy HH:mm:ss");
			vecDateTimeParseFormat.addElement("MM/dd/yy HH:mm");

			vecDateTimeParseFormat.addElement("MM/dd/yyyy hh:mm:ss.Sa");
			vecDateTimeParseFormat.addElement("MM/dd/yyyy hh:mm:ssa");
			vecDateTimeParseFormat.addElement("MM/dd/yyyy hh:mma");

			vecDateTimeParseFormat.addElement("MM/dd/yyyy HH:mm:ss.S");
			vecDateTimeParseFormat.addElement("MM/dd/yyyy HH:mm:ss");
			vecDateTimeParseFormat.addElement("MM/dd/yyyy HH:mm");
		} else if (bYearFirst) {
			staticOutputFormat = "yyyy/MM/dd hh:mma";
			// the "yy" formats must be before the "yyyy" formats because "yyyy" will
			// convert "05/04/65" -> "05/04/0065"
			vecDateTimeParseFormat.addElement("yy/MM/dd hh:mm:ss.Sa");
			vecDateTimeParseFormat.addElement("yy/MM/dd hh:mm:ssa");
			vecDateTimeParseFormat.addElement("yy/MM/dd hh:mma");

			vecDateTimeParseFormat.addElement("yy/MM/dd HH:mm:ss.S");
			vecDateTimeParseFormat.addElement("yy/MM/dd HH:mm:ss");
			vecDateTimeParseFormat.addElement("yy/MM/dd HH:mm");

			vecDateTimeParseFormat.addElement("yyyy/MM/dd hh:mm:ss.Sa");
			vecDateTimeParseFormat.addElement("yyyy/MM/dd hh:mm:ssa");
			vecDateTimeParseFormat.addElement("yyyy/MM/dd hh:mma");

			vecDateTimeParseFormat.addElement("yyyy/MM/dd HH:mm:ss.S");
			vecDateTimeParseFormat.addElement("yyyy/MM/dd HH:mm:ss");
			vecDateTimeParseFormat.addElement("yyyy/MM/dd HH:mm");
		} else { // day first
			staticOutputFormat = "dd/MM/yyyy hh:mma";
			// the "yy" formats must be before the "yyyy" formats because "yyyy" will
			// convert "05/04/65" -> "05/04/0065"
			vecDateTimeParseFormat.addElement("dd/MM/yy hh:mm:ss.Sa");
			vecDateTimeParseFormat.addElement("dd/MM/yy hh:mm:ssa");
			vecDateTimeParseFormat.addElement("dd/MM/yy hh:mma");

			vecDateTimeParseFormat.addElement("dd/MM/yy HH:mm:ss.S");
			vecDateTimeParseFormat.addElement("dd/MM/yy HH:mm:ss");
			vecDateTimeParseFormat.addElement("dd/MM/yy HH:mm");

			vecDateTimeParseFormat.addElement("dd/MM/yyyy hh:mm:ss.Sa");
			vecDateTimeParseFormat.addElement("dd/MM/yyyy hh:mm:ssa");
			vecDateTimeParseFormat.addElement("dd/MM/yyyy hh:mma");

			vecDateTimeParseFormat.addElement("dd/MM/yyyy HH:mm:ss.S");
			vecDateTimeParseFormat.addElement("dd/MM/yyyy HH:mm:ss");
			vecDateTimeParseFormat.addElement("dd/MM/yyyy HH:mm");
		}
		// SQL date formats
		vecDateTimeParseFormat.addElement("yyyy-MM-dd HH:mm:ss");
		vecDateTimeParseFormat.addElement("yyyy-MM-dd");

		vecDateTimeParseFormat.addElement(getFormat(DateFormat.SHORT));
		vecDateTimeParseFormat.addElement(getFormat(DateFormat.MEDIUM));
		vecDateTimeParseFormat.addElement(getFormat(DateFormat.LONG));
		vecDateTimeParseFormat.addElement(getFormat(DateFormat.DEFAULT));
	}

	/**
	 * Creates a new date/time initialized to the current system time.
	 */
	public OADateTime() {
		this._time = System.currentTimeMillis();
	}

	/**
	 * Creates a new date/time using a SQL Time value.
	 *
	 * @param time the SQL Time instance
	 */
	public OADateTime(java.sql.Time time) {
		this._time = time.getTime();
	}

	public OADateTime(Date date) {
		if (date == null) {
			this._time = System.currentTimeMillis();
		} else {
			this._time = date.getTime();
		}
	}

	/**
	 * Creates a new date/time using the specified millisecond value since the epoch.
	 *
	 * @param time milliseconds since the epoch
	 */
	public OADateTime(long time) {
		this._time = time;
	}

	/**
	 * Creates a new date/time using the specified SQL Timestamp value.
	 * If the timestamp is null, the current system time is used.
	 *
	 * @param date the SQL Timestamp used to initialize this instance
	 */
	public OADateTime(java.sql.Timestamp date) {
		if (date == null) {
			this._time = System.currentTimeMillis();
		} else {
			this._time = date.getTime();
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
	 * Creates a new date/time using the specified LocalDateTime value.
	 *
	 * @param ldt the LocalDateTime used to initialize this instance
	 */
	public OADateTime(LocalDateTime ldt) {
		this(new java.sql.Date(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()).getTime()));
	}

	/**
	 * Creates a new date/time using the specified Instant value.
	 *
	 * @param instant the Instant used to initialize this instance
	 */
	public OADateTime(Instant instant) {
		this(Date.from((Instant) instant).getTime());
	}

	/**
	 * Creates a new date/time using the specified ZonedDateTime value.
	 *
	 * @param zdt the ZonedDateTime used to initialize this instance
	 */
	public OADateTime(ZonedDateTime zdt) {
		this(new java.sql.Date(Date.from(zdt.toInstant()).getTime()));
	}

	/**
	 * Returns this date/time as a LocalDateTime using the current field values.
	 *
	 * @return a LocalDateTime representation of this instance
	 */
	public LocalDateTime getLocalDateTime() {
		LocalDateTime ldt = LocalDateTime.of(getYear(), getMonth() + 1, getDay(), get24Hour(), getMinute(), getSecond(), (int) (getMilliSecond() / Math.pow(10, 6)));
		return ldt;
	}

	/**
	 * Returns this date/time as a ZonedDateTime using the associated time zone.
	 *
	 * @return a ZonedDateTime representation of this instance
	 */
	public ZonedDateTime getZonedDateTime() {
		ZonedDateTime zdt = ZonedDateTime.of(getYear(), getMonth() + 1, getDay(), get24Hour(), getMinute(), getSecond(), (int) (getMilliSecond() / Math.pow(10, 6)), getTimeZone().toZoneId());
		return zdt;
	}

	/**
	 * Returns this date/time as an Instant.
	 *
	 * @return an Instant representing this date/time
	 */
	public Instant getInstant() {
		Instant instant = getZonedDateTime().toInstant();
		return instant;
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
		this.timeZone = d.timeZone;

		if (t != null) {
			setTime(t);
			// was: this._time += t._time; // wrong: does not account for tz
		}
	}

	/**
	 * Creates a new date/time using the specified year, month, and day.
	 *
	 * @param year full year value
	 * @param month month value from 0 to 11
	 * @param day day of month from 1 to 31
	 */
	public OADateTime(int year, int month, int day) {
		this(new Date(year - 1900, month, day));
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
		this(new Date(year - 1900, month, day, hrs, mins));
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
		this(new Date(year - 1900, month, day, hrs, mins, secs));
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
		this(new Date(year - 1900, month, day, hrs, mins, secs));
		this._time += milsecs;
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
		// 20240805
		if (timeZone != null && timeZone != defaultTimeZone) {
			stream.writeInt(9990); // version
			stream.writeUTF(timeZone.getID());
		}

		if (this instanceof OADate) {
			GregorianCalendar cal = _getCal();
			try {
				stream.writeInt(9997); // version
				stream.writeInt(cal.get(Calendar.YEAR));
				stream.writeInt(cal.get(Calendar.MONTH));
				stream.writeInt(cal.get(Calendar.DATE));
			} finally {
				_releaseCal(cal);
			}
		} else if (this instanceof OATime) {
			GregorianCalendar cal = _getCal();
			try {
				stream.writeInt(9998); // version
				stream.writeInt(cal.get(Calendar.HOUR_OF_DAY));
				stream.writeInt(cal.get(Calendar.MINUTE));
				stream.writeInt(cal.get(Calendar.SECOND));
				stream.writeInt(cal.get(Calendar.MILLISECOND));
			} finally {
				_releaseCal(cal);
			}

		} else if (ignoreTimeZone) {
			GregorianCalendar cal = _getCal();
			try {
				stream.writeInt(9995); // version
				stream.writeInt(cal.get(Calendar.YEAR));
				stream.writeInt(cal.get(Calendar.MONTH));
				stream.writeInt(cal.get(Calendar.DATE));
				stream.writeInt(cal.get(Calendar.HOUR_OF_DAY));
				stream.writeInt(cal.get(Calendar.MINUTE));
				stream.writeInt(cal.get(Calendar.SECOND));
				stream.writeInt(cal.get(Calendar.MILLISECOND));
			} finally {
				_releaseCal(cal);
			}
		} else {
			stream.writeInt(9999); // version
			stream.writeLong(_time);
		}
	}

	/**
	 * Custom deserialization logic for reading this object from an ObjectInputStream.
	 * Restores the internal time value and optional time zone based on version data.
	 *
	 * @param in the ObjectInputStream to read from
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException if a class cannot be resolved
	 */
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		int x = in.readInt();
		String tzId = null;
		if (x == 9990) {
			tzId = in.readUTF();
			x = in.readInt();
		}

		if (x == 9997) {
			int year = in.readInt();
			int month = in.readInt();
			int day = in.readInt();
			Date d = new Date(year - 1900, month, day);
			this._time = d.getTime();
		} else if (x == 9998) {
			int hour = in.readInt();
			int minute = in.readInt();
			int second = in.readInt();
			int milisecond = in.readInt();
			Date d = new Date(70, 0, 1, hour, minute, second);
			this._time = d.getTime();
			this._time += milisecond;
		} else if (x == 9999) {
			_time = in.readLong();
		} else if (x == 9995) {
			int year = in.readInt();
			int month = in.readInt();
			int day = in.readInt();
			int hour = in.readInt();
			int minute = in.readInt();
			int second = in.readInt();
			int milisecond = in.readInt();
			Date d = new Date(year - 1900, month, day, hour, minute, second);
			this._time = d.getTime();
			this._time += milisecond;
			this.ignoreTimeZone = true;
		} else { // real old format
			int year = x;
			int month = in.readInt();
			int day = in.readInt();
			int hour = in.readInt();
			int minute = in.readInt();
			int second = in.readInt();
			int milisecond = in.readInt();
			Date d = new Date(year - 1900, month, day, hour, minute, second);
			this._time = d.getTime();
			this._time += milisecond;
		}

		if (tzId != null) {
			TimeZone tz = OATimeZone.getTimeZoneById(tzId);
			this.timeZone = tz;
		}

	}

	/**
	 * Returns the value of the specified calendar field.
	 *
	 * @param fld the Calendar field constant
	 * @return the field value
	 */
	private int getField(int fld) {
		int x;
		GregorianCalendar c = _getCal();
		try {
			x = c.get(fld);
		} finally {
			_releaseCal(c);
		}
		return x;
	}

	/**
	 * Returns a clone of the Calendar used by this date/time instance.
	 *
	 * @return a cloned Calendar representing this date/time
	 */
	public Calendar getCalendar() {
		Calendar cNew;
		GregorianCalendar c = _getCal();
		try {
			cNew = (Calendar) c.clone();
		} finally {
			poolGregorianCalendar.release(c);
		}
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
		long t = new Date(year - 1900, month, day, hrs, mins, secs).getTime();
		this._time = t + milsecs;
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
	 * Sets the internal time value using the specified SQL Time.
	 * If the time is null, the current system time is used.
	 *
	 * @param time the SQL Time to use
	 */
	protected void setCalendar(Time time) {
		if (time == null) {
			this._time = System.currentTimeMillis();
		} else {
			this._time = time.getTime();
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
		if (timeZone == null || timeZone == defaultTimeZone) {
			Date dThis = new Date(_time);

			Date d = new Date(dThis.getYear(), dThis.getMonth(), dThis.getDate());
			_time = d.getTime();
			return;
		}
		GregorianCalendar c = _getCal();
		try {
			c.set(c.HOUR_OF_DAY, 0);
			c.set(c.MINUTE, 0);
			c.set(c.SECOND, 0);
			c.set(c.MILLISECOND, 0);
			_time = c.getTimeInMillis();
		} finally {
			_releaseCal(c);
		}
	}

	/**
	 * Sets the date portion to January 1, 1970 while preserving the time portion.
	 */
	public void clearDate() {
		if (timeZone == null || timeZone == defaultTimeZone) {
			Date dThis = new Date(_time);
			long ms = getMilliSecond();
			Date d = new Date(70, 0, 1, dThis.getHours(), dThis.getMinutes(), dThis.getSeconds());
			_time = d.getTime();
			if (ms > 0) {
				_time += ms;
			}
			return;
		}
		GregorianCalendar c = _getCal();
		try {
			c.set(c.YEAR, 1970);
			c.set(c.MONTH, c.JANUARY);
			c.set(c.DATE, 1);

			// these are added to make sure timezone is calculated correctly
			c.set(c.HOUR_OF_DAY, get24Hour());
			c.set(c.MINUTE, getMinute());
			c.set(c.SECOND, getSecond());
			c.set(c.MILLISECOND, getMilliSecond());

			_time = c.getTimeInMillis();
		} finally {
			_releaseCal(c);
		}
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
		if (timeZone == null || timeZone == defaultTimeZone) {
			Date dThis = new Date(_time);
			Date d = new Date(dThis.getYear(), dThis.getMonth(), dThis.getDate(), hr, m, s);
			_time = d.getTime() + ms;
			return;
		}
		GregorianCalendar c = _getCal();
		try {
			c.set(c.HOUR_OF_DAY, hr);
			c.set(c.MINUTE, m);
			c.set(c.SECOND, s);
			c.set(c.MILLISECOND, ms);
			_time = c.getTimeInMillis();
		} finally {
			_releaseCal(c);
		}
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
		if (timeZone == null || timeZone == defaultTimeZone) {
			setTime(t.getHour(), t.getMinute(), t.getSecond(), t.getMilliSecond());
			return;
		}
		GregorianCalendar c = _getCal();
		try {
			c.set(c.HOUR_OF_DAY, t.get24Hour());
			c.set(c.MINUTE, t.getMinute());
			c.set(c.SECOND, t.getSecond());
			c.set(c.MILLISECOND, t.getMilliSecond());
			_time = c.getTimeInMillis();
		} finally {
			_releaseCal(c);
		}
	}

	/**
	 * Sets the date using year, month, and day values.
	 *
	 * @param yr full year value
	 * @param m month value from 0 to 11
	 * @param d day of month
	 */
	public void setDate(int yr, int m, int d) {
		if (timeZone == null || timeZone == defaultTimeZone) {
			Date dThis = new Date(_time);
			long ms = getMilliSecond();
			Date dNew = new Date(yr - 1900, m, d, dThis.getHours(), dThis.getMinutes(), dThis.getSeconds());
			_time = dNew.getTime();
			if (ms > 0) {
				_time += ms;
			}
			return;
		}
		GregorianCalendar c = _getCal();
		try {
			c.set(c.YEAR, yr);
			c.set(c.MONTH, m);
			c.set(c.DATE, d);
			_time = c.getTimeInMillis();
		} finally {
			_releaseCal(c);
		}
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
		if (timeZone == null || timeZone == defaultTimeZone) {
			Date d = new Date(_time);
			return d.getYear() + 1900;
		}
		GregorianCalendar c = _getCal();
		int yr;
		try {
			yr = c.get(c.YEAR);
		} finally {
			_releaseCal(c);
		}
		return yr;
	}

	/**
	 * Sets the year value for this date/time.
	 *
	 * @param y full year value
	 */
	public void setYear(int y) {
		if (timeZone == null || timeZone == defaultTimeZone) {
			long ms = getMilliSecond();
			Date dThis = new Date(_time);
			Date dNew = new Date(y - 1900, dThis.getMonth(), dThis.getDate(), dThis.getHours(), dThis.getMinutes(), dThis.getSeconds());
			_time = dNew.getTime();
			if (ms > 0) {
				_time += ms;
			}
			return;
		}
		GregorianCalendar c = _getCal();
		try {
			c.set(c.YEAR, y);
			_time = c.getTimeInMillis();
		} finally {
			_releaseCal(c);
		}
	}

	/**
	 * Returns the month value.
	 *
	 * @return month value from 0 to 11
	 */
	public int getMonth() {
		if (timeZone == null || timeZone == defaultTimeZone) {
			Date d = new Date(_time);
			return d.getMonth();
		}
		GregorianCalendar c = _getCal();
		int m;
		try {
			m = c.get(c.MONTH);
		} finally {
			_releaseCal(c);
		}
		return m;
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
	 * Sets the month value.
	 *
	 * @param month month value from 0 to 11
	 */
	public void setMonth(int month) {
		if (timeZone == null || timeZone == defaultTimeZone) {
			long ms = getMilliSecond();
			Date dThis = new Date(_time);
			Date dNew = new Date(dThis.getYear(), month, dThis.getDate(), dThis.getHours(), dThis.getMinutes(), dThis.getSeconds());
			_time = dNew.getTime();
			if (ms > 0) {
				_time += ms;
			}
			return;
		}
		GregorianCalendar c = _getCal();
		try {
			c.set(c.MONTH, month);
			_time = c.getTimeInMillis();
		} finally {
			_releaseCal(c);
		}
	}

	/**
	 * Returns the day of the month.
	 *
	 * @return day of month from 1 to 31
	 */
	public int getDay() {
		if (timeZone == null || timeZone == defaultTimeZone) {
			Date d = new Date(_time);
			return d.getDate();
		}
		GregorianCalendar c = _getCal();
		int d;
		try {
			d = c.get(c.DATE);
		} finally {
			_releaseCal(c);
		}
		return d;
	}

	/**
	 * Sets the day of the month.
	 *
	 * @param d day of month from 1 to 31
	 */
	public void setDay(int d) {
		if (timeZone == null || timeZone == defaultTimeZone) {
			long ms = getMilliSecond();
			Date dThis = new Date(_time);
			Date dNew = new Date(dThis.getYear(), dThis.getMonth(), d, dThis.getHours(), dThis.getMinutes(), dThis.getSeconds());
			_time = dNew.getTime();
			if (ms > 0) {
				_time += ms;
			}
			return;
		}
		GregorianCalendar c = _getCal();
		try {
			c.set(c.DAY_OF_MONTH, d);
			_time = c.getTimeInMillis();
		} finally {
			_releaseCal(c);
		}
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
	 * @param tz the TimeZone to set
	 */
	public void setTimeZone(TimeZone tz) {
		if (tz == timeZone) {
			return;
		}
		if (timeZone == null && tz == defaultTimeZone) {
			return;
		}

		long ms = getMilliSecond();

		// need to create a new cal, otherwise setting tz will adjust the other values
		// (use convertTo(tz) instead)
		GregorianCalendar calNew = new GregorianCalendar(tz);

		GregorianCalendar c = _getCal();
		try {
			calNew.set(c.get(c.YEAR), c.get(c.MONTH), c.get(c.DAY_OF_MONTH), c.get(c.HOUR_OF_DAY), c.get(c.MINUTE), c.get(c.SECOND));
			calNew.set(Calendar.MILLISECOND, c.get(c.MILLISECOND));
		} finally {
			_releaseCal(c);
		}

		this._time = calNew.getTimeInMillis();
		this.timeZone = tz;
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
		if (timeZone == null || timeZone == defaultTimeZone) {
			Date d = new Date(_time);
			int hr = d.getHours();
			return hr;
		}
		GregorianCalendar c = _getCal();
		int hr;
		try {
			hr = c.get(c.HOUR_OF_DAY); // 24 hr
		} finally {
			_releaseCal(c);
		}
		return hr;
	}

	/**
	 * Sets the hour of the day using a 24-hour clock.
	 *
	 * @param hr hour value
	 */
	public void setHour(int hr) {
		if (timeZone == null || timeZone == defaultTimeZone) {
			long ms = getMilliSecond();
			Date dThis = new Date(_time);
			Date dNew = new Date(dThis.getYear(), dThis.getMonth(), dThis.getDate(), hr, dThis.getMinutes(), dThis.getSeconds());
			_time = dNew.getTime();
			if (ms > 0) {
				_time += ms;
			}
			return;
		}
		GregorianCalendar c = _getCal();
		try {
			c.set(c.HOUR_OF_DAY, hr);
			_time = c.getTimeInMillis();
		} finally {
			_releaseCal(c);
		}
	}

	/**
	 * Returns the hour of the day using a 12-hour clock.
	 *
	 * @return hour value from 0 to 11
	 */
	public int get12Hour() {
		if (timeZone == null || timeZone == defaultTimeZone) {
			Date d = new Date(_time);
			int hr = d.getHours();
			if (hr >= 12) {
				hr -= 12;
			}
			return hr;
		}
		GregorianCalendar c = _getCal();
		int hr;
		try {
			hr = c.get(c.HOUR); // 12 hr format
		} finally {
			_releaseCal(c);
		}
		return hr;
	}

	/**
	 * Sets the hour of the day using a 12-hour clock.
	 *
	 * @param hr hour value from 1 to 12
	 * @throws IllegalArgumentException if hr is outside the range 1 to 12
	 */
	public void set12Hour(int hr) {
		// Accept 1–12; coerce into 0–11
		if (hr < 1 || hr > 12)
			throw new IllegalArgumentException("hr must be 1..12");
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
		Date d = new Date(_time);
		int hr = d.getMinutes();
		return hr;
	}

	/**
	 * Sets the minute value.
	 *
	 * @param mins minute value
	 */
	public void setMinute(int mins) {
		long ms = getMilliSecond();
		Date dThis = new Date(_time);
		Date dNew = new Date(dThis.getYear(), dThis.getMonth(), dThis.getDate(), dThis.getHours(), mins, dThis.getSeconds());
		_time = dNew.getTime();
		if (ms > 0) {
			_time += ms;
		}
	}

	/**
	 * Returns the second value.
	 *
	 * @return second value from 0 to 59
	 */
	public int getSecond() {
		Date d = new Date(_time);
		int secs = d.getSeconds();
		return secs;
	}

	/**
	 * Sets the second value.
	 *
	 * @param s second value
	 */
	public void setSecond(int s) {
		long ms = getMilliSecond();
		Date dThis = new Date(_time);
		Date dNew = new Date(dThis.getYear(), dThis.getMonth(), dThis.getDate(), dThis.getHours(), dThis.getMinutes(), s);
		_time = dNew.getTime();
		if (ms > 0) {
			_time += ms;
		}
	}

	/**
	 * Clears the second and millisecond values by setting them to zero.
	 */
	public void clearSecondAndMilliSecond() {
		Date dThis = new Date(_time);
		Date dNew = new Date(dThis.getYear(), dThis.getMonth(), dThis.getDate(), dThis.getHours(), dThis.getMinutes(), 0);
		_time = dNew.getTime();
	}

	/**
	 * Returns the millisecond value.
	 *
	 * @return millisecond value
	 */
	public int getMilliSecond() {
		Date dThis = new Date(_time);
		Date dNew = new Date(dThis.getYear(), dThis.getMonth(), dThis.getDate(), dThis.getHours(), dThis.getMinutes(), dThis.getSeconds());
		long ts = dNew.getTime();
		int ms = (int) (_time - ts);
		return ms;
	}

	/**
	 * Sets the millisecond value.
	 *
	 * @param ms millisecond value
	 */
	public void setMilliSecond(int ms) {
		_time -= getMilliSecond();
		_time += ms;
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
		int x;
		try {
			x = c.get(Calendar.DAY_OF_WEEK);
		} finally {
			poolGregorianCalendar.release(c);
		}
		return x;
	}

	/**
	 * Returns the day of the year.
	 *
	 * @return day of year where January 1 is 1
	 */
	public int getDayOfYear() {
		GregorianCalendar c = _getCal();
		int x;
		try {
			x = c.get(Calendar.DAY_OF_YEAR);
		} finally {
			poolGregorianCalendar.release(c);
		}
		return x;
	}

	/**
	 * Returns the week of the month.
	 *
	 * @return week number within the month, where first week is 1.
	 */
	public int getWeekOfMonth() {
		GregorianCalendar c = _getCal();
		int x;
		try {
			x = c.get(Calendar.WEEK_OF_MONTH);
		} finally {
			poolGregorianCalendar.release(c);
		}
		return x;
	}

	/**
	 * Returns the week of the year.
	 *
	 * @return week number within the year, where first week is 1
	 */
	public int getWeekOfYear() {
		GregorianCalendar c = _getCal();
		int x;
		try {
			x = c.get(Calendar.WEEK_OF_YEAR);
		} finally {
			poolGregorianCalendar.release(c);
		}
		return x;
	}

	/**
	 * Returns the number of days in the current month.
	 *
	 * @return number of days in month
	 */
	public int getDaysInMonth() {
		GregorianCalendar c = _getCal();
		int x;
		try {
			x = c.getActualMaximum(Calendar.DAY_OF_MONTH);
		} finally {
			poolGregorianCalendar.release(c);
		}
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
		OADateTime d = convert(obj, false);
		if (d == null) {
			return 2;
		}

		OADateTime dtThis, dtObj;
		if (!this.getClass().equals(d.getClass())) {
			if (this instanceof OADate || obj instanceof OADate) {
				if (this instanceof OADate) {
					dtThis = this;
				} else {
					dtThis = new OADate(this);
				}

				if (d instanceof OADate) {
					dtObj = d;
				} else {
					dtObj = new OADate(d);
				}
			} else if (this instanceof OATime || obj instanceof OATime) {
				if (this instanceof OATime) {
					dtThis = this;
				} else {
					dtThis = new OATime(this);
				}

				if (d instanceof OATime) {
					dtObj = d;
				} else {
					dtObj = new OATime(d);
				}
			} else {
				dtThis = this;
				dtObj = d;
			}
		} else {
			dtThis = this;
			dtObj = d;
		}
		if (dtThis instanceof OADate) {
			if (dtThis.getYear() == dtObj.getYear()) {
				if (dtThis.getMonth() == dtObj.getMonth()) {
					if (dtThis.getDay() == dtObj.getDay()) {
						return 0;
					}
				}
			}
		} else if (dtThis instanceof OATime) {
			if (dtThis.get24Hour() == dtObj.get24Hour()) {
				if (dtThis.getMinute() == dtObj.getMinute()) {
					if (dtThis.getSecond() == dtObj.getSecond()) {
						if (dtThis.getMilliSecond() == dtObj.getMilliSecond()) {
							return 0;
						}
					}
				}
			}
		}

		if (dtThis._time == dtObj._time) {
			return 0;
		}
		if (dtThis._time > dtObj._time) {
			return 1;
		}
		return -1;
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
		try {
			c.setTimeZone(tz);
			dt = new OADateTime(c);
			if (this instanceof OADate) {
				dt = new OADate(dt);
			} else if (this instanceof OATime) {
				dt = new OATime(dt);
			}
		} finally {
			poolGregorianCalendar.release(c);
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
				try {
					c.setTimeZone(tz.timeZone);
					dt = new OADateTime(c);
					if (this instanceof OATime) {
						dt = new OATime(dt);
					}
				} finally {
					poolGregorianCalendar.release(c);
				}
			}
		}
		return dt;
	}

	/*
	 * Return an OADateTime where a specified amount of days is added.
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
		if (amount == 0) {
			return this;
		}
		if (this instanceof OATime) {
			return new OATime(this);
		}

		OADateTime dtNew;
		final GregorianCalendar c = _getCal();
		try {
			c.add(Calendar.DATE, amount);

			if (this instanceof OADate) {
				dtNew = new OADate(c);
			} else {
				dtNew = new OADateTime(c);
			}
		} finally {
			poolGregorianCalendar.release(c);
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
		try {
			c.add(Calendar.MONTH, amount);

			if (this instanceof OADate) {
				dtNew = new OADate(c);
			} else {
				dtNew = new OADateTime(c);
			}
		} finally {
			poolGregorianCalendar.release(c);
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
		try {
			c.add(Calendar.YEAR, amount);

			if (this instanceof OADate) {
				dtNew = new OADate(c);
			} else {
				dtNew = new OADateTime(c);
			}
		} finally {
			poolGregorianCalendar.release(c);
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
		try {
			c.add(Calendar.HOUR_OF_DAY, amount);

			if (this instanceof OATime) {
				dtNew = new OATime(c);
			} else if (this instanceof OADate) {
				dtNew = new OADate(c);
			} else {
				dtNew = new OADateTime(c);
			}
		} finally {
			poolGregorianCalendar.release(c);
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
		try {
			c.add(Calendar.MINUTE, amount);

			if (this instanceof OATime) {
				dtNew = new OATime(c);
			} else if (this instanceof OADate) {
				dtNew = new OADate(c);
			} else {
				dtNew = new OADateTime(c);
			}
		} finally {
			poolGregorianCalendar.release(c);
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
		try {
			c.add(Calendar.SECOND, amount);

			if (this instanceof OATime) {
				dtNew = new OATime(c);
			} else if (this instanceof OADate) {
				dtNew = new OADate(c);
			} else {
				dtNew = new OADateTime(c);
			}
		} finally {
			poolGregorianCalendar.release(c);
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
		try {
			c.add(Calendar.MILLISECOND, amount);

			if (this instanceof OATime) {
				dtNew = new OATime(c);
			} else if (this instanceof OADate) {
				dtNew = new OADate(c);
			} else {
				dtNew = new OADateTime(c);
			}
		} finally {
			poolGregorianCalendar.release(c);
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
	public int betweenYears(Object obj) {
		OADateTime d = convert(obj, false);
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
	public int betweenMonths(Object obj) {
		OADateTime d = convert(obj, false);

		int amt = this.getYear() - d.getYear();
		amt = Math.abs(amt) * 12;

		if (compareTo(obj) >= 0) {
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
	public int betweenDays(Object obj) {
		OADateTime d = convert(obj, true);
		d.setTime(this.getHour(), this.getMinute(), this.getSecond(), this.getMilliSecond());

		double millis;
		GregorianCalendar cThis = _getCal();
		GregorianCalendar cOther = d._getCal();
		try {
			millis = Math.abs(cThis.getTime().getTime() - cOther.getTime().getTime());
		} finally {
			poolGregorianCalendar.release(cThis);
			poolGregorianCalendar.release(cOther);
		}
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
	public int betweenHours(Object obj) {
		OADateTime d = convert(obj, true);
		d.setTime(d.getHour(), this.getMinute(), this.getSecond(), this.getMilliSecond());

		GregorianCalendar cThis = _getCal();
		GregorianCalendar cOther = d._getCal();

		double millis = Math.abs(cThis.getTime().getTime() - cOther.getTime().getTime());

		poolGregorianCalendar.release(cThis);
		poolGregorianCalendar.release(cOther);

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
	public int betweenMinutes(Object obj) {
		OADateTime d = convert(obj, true);
		d.setTime(d.getHour(), d.getMinute(), this.getSecond(), this.getMilliSecond());

		GregorianCalendar cThis = _getCal();
		GregorianCalendar cOther = d._getCal();

		double millis = Math.abs(cThis.getTime().getTime() - cOther.getTime().getTime());

		poolGregorianCalendar.release(cThis);
		poolGregorianCalendar.release(cOther);

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

		poolGregorianCalendar.release(cThis);
		poolGregorianCalendar.release(cOther);

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

		poolGregorianCalendar.release(cThis);
		poolGregorianCalendar.release(cOther);

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
		Date d = valueOfMain(strDateTime, fmt, bTryOtherFormats ? vecDateTimeParseFormat : null, bTryOtherFormats ? staticOutputFormat : null);
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
	 * @param vec collection of fallback parse formats
	 * @param outputFormat fallback output format
	 * @return a Date instance or null
	 */
	protected static Date valueOfMain(String value, String inputFormat, Vector vec, String outputFormat) {
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
				format = inputFormat.substring(0, pos) + inputFormat.substring(pos + 2);
			}
		}

		Date date = null;
		int x = vec == null ? 0 : vec.size();

		int j = (format == null) ? -1 : -2;
		for (; j <= x && date == null; j++) {
			if (j == -1) {
				format = inputFormat;
			}
			if (j >= 0) {
				if (j < x) {
					format = (String) vec.elementAt(j);
				} else {
					format = outputFormat;
				}
			}
			if (format != null && format.length() > 0) {
				SimpleDateFormat sdf = getFormatter();
				synchronized (sdf) {
					sdf.applyPattern(format);
					try {
						date = sdf.parse(value);
						if (date != null) {
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
		vecDateTimeParseFormat.addElement(fmt);
	}

	/**
	 * Removes a global parse format.
	 *
	 * @param fmt the parse format to remove
	 */
	public static void removeGlobalParseFormat(String fmt) {
		vecDateTimeParseFormat.removeElement(fmt);
	}

	/**
	 * Removes all global parse formats.
	 */
	public static void removeAllGlobalParseFormats() {
		vecDateTimeParseFormat.removeAllElements();
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
	 * Returns a SimpleDateFormat instance from the formatter pool.
	 *
	 * @return a pooled SimpleDateFormat instance
	 */
	protected static SimpleDateFormat getFormatter() {
		SimpleDateFormat sdf;
		synchronized (simpleDateFormats) {
			simpleDateFormatCounter++;
			if (simpleDateFormatCounter >= simpleDateFormats.length) {
				simpleDateFormatCounter = 0;
			}
			sdf = simpleDateFormats[simpleDateFormatCounter];
			if (sdf == null) {
				sdf = simpleDateFormats[simpleDateFormatCounter] = new SimpleDateFormat();
				sdf.setLenient(false);
			}
		}
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

	/**
	 * Sets whether the time zone should be ignored during serialization.
	 *
	 * @param b true to ignore the time zone; false otherwise
	 */
	public void setIgnoreTimeZone(boolean b) {
		this.ignoreTimeZone = b;
	}

	/**
	 * Returns whether the time zone is ignored during serialization.
	 *
	 * @return true if the time zone is ignored; false otherwise
	 */
	public boolean getIgnoreTimeZone() {
		return this.ignoreTimeZone;
	}

	
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
}
