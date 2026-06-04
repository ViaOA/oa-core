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

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import com.viaoa.lang.OAStr;

/*qqqqqqqqqq
CODEX

 1. file/class/method
     src/main/java/com/viaoa/datetime/OATime.java — OATime(String strTime) / OATime(String strTime, String fmt)
  2. concrete bug
     Invalid or null parse results flow into OATime(OADateTime od), which dereferences od and throws
     NullPointerException.
  3. runtime scenario
     new OATime("bad-time") calls OATime.valueOf(...), gets null, then calls this((OADateTime) null). The constructor
     eventually executes od.getTimeZone().
  4. why this violates OA/OG datetime semantics
     Parsing failure should fail with an intentional parse/argument exception or return null through valueOf, not a
     constructor NPE. This can corrupt UI/property conversion error handling.
  5. minimal fix direction
     Mirror OADate(String) behavior: use a valueOf2-style helper that throws IllegalArgumentException when parsing
     fails, or explicitly check for null before constructor delegation.
  6. suggested CODEX comment location
     Above OATime(String strTime) and OATime(String strTime, String fmt).

*/

/**
 * Time class that combines Calendar, Time and SimpleDateFormat into a single class.
 * <p>
 * OATime is not affected by timezone. A time created on one system will be the same on another machine, even if the timezone is different.
 * See OADateTime for list or formatting symbols.
 *
 * @see OADateTime
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
	 * Default output format used when converting this time to a String.
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
	 * Collection of default formats used when parsing time values from strings.
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
		this(new Date());
		clearDate();
	}

	/**
	 * Creates a new time instance using the supplied {@link Date}.
	 * <p>
	 * The date portion is cleared so that only time values are retained.
	 *
	 * @param date the date whose time value will be used
	 */
	public OATime(Date date) {
		super(date.getTime());
		clearDate();
	}


	/**
	 * Creates a new time instance using the supplied {@link Calendar}.
	 * <p>
	 * The date portion is cleared so that only time values are retained.
	 *
	 * @param c the calendar whose time value will be used
	 */
	public OATime(Calendar c) {
		super(c);
		clearDate();
	}

	/**
	 * Creates a new time instance using the supplied {@link OADateTime}.
	 * <p>
	 * The date portion is cleared and the timezone is copied from the
	 * supplied instance.
	 *
	 * @param dt the date-time instance to copy from
	 */
	public OATime(OADateTime dt) {
		this(dt.get24Hour(), dt.getMinute(), dt.getSecond(), dt.getMilliSecond());
		if (dt.timeZone != null) setTimeZone(dt.timeZone);
	}

	/**
	 * Creates a new time instance by parsing the supplied string using
	 * the default time format.
	 * <p>
	 * The date portion is cleared so that only time values are retained.
	 *
	 * @param strTime the string representation of the time
	 */
	public OATime(String strTime) {
		this(strTime, null);
	}

	/**
	 * Creates a new time instance by parsing the supplied string using
	 * the specified format.
	 * <p>
	 * The date portion is cleared so that only time values are retained.
	 *
	 * @param strTime the string representation of the time
	 * @param fmt the format used to parse the time string
	 */
	public OATime(String strTime, String fmt) {
		OADateTime dt = OATime.valueOf(strTime, fmt);
		if (dt == null) throw new IllegalArgumentException("OATime cant create time from String \"" + strTime + "\"");
		GregorianCalendar c = dt._getCal();
		setCalendar(1970, 0, 1, c.get(c.HOUR_OF_DAY), c.get(c.MINUTE), c.get(c.SECOND), c.get(c.MILLISECOND));
		if (dt.timeZone != null) setTimeZone(dt.timeZone);
	}

	/**
	 * Creates a new time instance using the supplied {@link LocalTime}.
	 *
	 * @param lt the local time whose hour, minute, second, and millisecond values are used
	 */
	public OATime(LocalTime lt) {
		this(lt.getHour(), lt.getMinute(), lt.getSecond(), (int) (lt.getNano() / 1_000_000));
	}

	/**
	 * Creates a new time instance using the supplied hours, minutes, and seconds.
	 * <p>
	 * The date portion is cleared so that only time values are retained.
	 *
	 * @param hrs the hour value, from 0 to 23
	 * @param mins the minute value
	 * @param secs the second value
	 */
	public OATime(int hrs, int mins, int secs) {
		super(1970, 0, 1, hrs, mins, secs, 0);
		clearDate();
	}

	/**
	 * Creates a new time instance using the supplied hours, minutes, seconds,
	 * and milliseconds.
	 * <p>
	 * The date portion is cleared so that only time values are retained.
	 *
	 * @param hrs the hour value, from 0 to 23
	 * @param mins the minute value
	 * @param secs the second value
	 * @param mili the millisecond value
	 */
	public OATime(int hrs, int mins, int secs, int mili) {
		super(1970, 0, 1, hrs, mins, secs, mili);
		clearDate();
	}

	@Override
	protected void setCalendar(GregorianCalendar c) {
		if (c == null) return;
		setCalendar(1970, 0, 1, c.get(c.HOUR_OF_DAY), c.get(c.MINUTE), c.get(c.SECOND), c.get(c.MILLISECOND));
	}

	@Override
	protected void setCalendar(Date date) {
		super.setCalendar(date);
		clearDate();
	}

	@Override
	protected void setCalendar(int year, int month, int day, int hrs, int mins, int secs, int milsecs) {
		super.setCalendar(1970, 0, 1, hrs, mins, secs, milsecs);
	}

	@Override
	protected void setCalendar(String strDate) {
		OADateTime dt = valueOf(strDate);
		super.setCalendar(1970, 0, 1, dt.getHour(), dt.getMinute(), dt.getSecond(), dt.getMilliSecond());
	}
	
	@Override
	protected void setCalendar(String strDate, String fmt) {
		OADateTime dt = valueOf(strDate, fmt);
		super.setCalendar(1970, 0, 1, dt.getHour(), dt.getMinute(), dt.getSecond(), dt.getMilliSecond());
	}
	
	@Override
	public void setDate(int yr, int m, int d) {
	    // no-op, not used
	}
	
	public void setDate(OADate d) {
	    // no-op, not used
	}
	
	@Override
	public void setYear(int y) {
	    // no-op, not used
	}
	
	@Override
	public void setMonth(int month) {
	    // no-op, not used
	}

	@Override
	public void setMonthValue(int monthValue) {
	    // no-op, not used
	}

	@Override
	public void setDay(int d) {
	    // no-op, not used
	}

	@Override
	public void setTimeZoneUTC() {
	    super.setTimeZoneUTC();
	    clearDate();
	}
	
	@Override
	public void setTimeZone(OATimeZone.TZ tz) {
	    super.setTimeZone(tz);
	    clearDate();
	}
	
	public void setTimeZone(TimeZone tzNew) {
		super.setTimeZone(tzNew);
		clearDate();
	}
	
	@Override
	public OADateTime convertToUTC() {
		OADateTime dt = super.convertToUTC();
		OATime t = new OATime(dt);
		return t;
	}
	
	@Override
	public OADateTime convertTo(TimeZone tz) {
		OADateTime dt = super.convertTo(tz);
		OATime t = new OATime(dt);
		return t;
	}
	
	@Override
	public OADateTime convertTo(OATimeZone.TZ tz) {
		OADateTime dt = super.convertTo(tz);
		OATime t = new OATime(dt);
		return t;
	}

	@Override
	public OADateTime addYears(int amount) {
		OATime t = new OATime(this);
		return t;
	}

	@Override
	public OADateTime subtractYears(int amount) {
		OATime t = new OATime(this);
		return t;
	}
	
	@Override
	public OADateTime addMonths(int amount) {
		OATime t = new OATime(this);
		return t;
	}

	@Override
	public OADateTime subtractMonths(int amount) {
		OATime t = new OATime(this);
		return t;
	}

	
	@Override
	public OADateTime addDays(int amount) {
		OATime t = new OATime(this);
		return t;
	}

	@Override
	public OADateTime subtractDays(int amount) {
		OATime t = new OATime(this);
		return t;
	}
	
	
	
	@Override
	public OADateTime addHours(int amount) {
		OADateTime dt = super.addHours(amount);
		OATime t = new OATime(dt);
		return t;
	}
	@Override
	public OADateTime subtractHours(int amount) {
		OADateTime dt = super.subtractHours(amount);
		OATime t = new OATime(dt);
		return t;
	}

	@Override
	public OADateTime addMinutes(int amount) {
		OADateTime dt = super.addMinutes(amount);
		OATime t = new OATime(dt);
		return t;
	}
	@Override
	public OADateTime subtractMinutes(int amount) {
		OADateTime dt = super.subtractMinutes(amount);
		OATime t = new OATime(dt);
		return t;
	}
	
	@Override
	public OADateTime addSeconds(int amount) {
		OADateTime dt = super.addSeconds(amount);
		OATime t = new OATime(dt);
		return t;
	}
	@Override
	public OADateTime subtractSeconds(int amount) {
		OADateTime dt = super.subtractSeconds(amount);
		OATime t = new OATime(dt);
		return t;
	}

	@Override
	public OADateTime addMilliSeconds(int amount) {
		OADateTime dt = super.addMilliSeconds(amount);
		OATime t = new OATime(dt);
		return t;
	}
	@Override
	public OADateTime subtractMilliSeconds(int amount) {
		OADateTime dt = super.subtractMilliSeconds(amount);
		OATime t = new OATime(dt);
		return t;
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

	/*
	 * Converts a String to an OATime. See OADateTime for list of formatting symbols. If time can not be parsed based on supplied format,
	 * then other formatting and conversions will be used to try to convert to an OATime.
	 * <p>
	 * Note: you will need to cast the return value to a OATime.
	 *
	 * @param fmt is format to use for parsing. See OADateTime for list of formatting symbols.
	 * @see OADateTime
	 * @see #timeValue(String,String)
	 */
	/**
	 * Converts a string to an {@link OADateTime} using the supplied format.
	 * <p>
	 * If parsing fails with the supplied format, additional formats are attempted.
	 *
	 * @param time the string representation of the time
	 * @param fmt the format to use for parsing
	 * @return an {@link OADateTime} representing the parsed time, or {@code null} if parsing fails
	 */
	public static OADateTime valueOf(String time, String fmt) {
		if (time == null) return null;
		if (time.length() > 0) {
			char c = time.charAt(time.length() - 1);
			if (c == 'A' || c == 'a' || c == 'P' || c == 'p') {
				time += "m";
			}
		}
		List<String> alx = OATime.alTimeParseFormat;
		Date d = valueOfMain(time, fmt, alx, timeOutputFormat);
		if (d == null) {
			return null;
		}
		return new OATime(d);
	}

	/*
	 * Converts a String to an OATime using a default format. The default format is the first format that has been set: "format",
	 * "timeOutputFormat" else or "hh:mma" See OADateTime for list of formatting symbols.
	 * <p>
	 * Note: you will need to cast the return value to a OATime.
	 *
	 * @see #valueOf(String,String)
	 * @see OADateTime
	 * @see #timeValue(String,String)
	 */
	/**
	 * Converts a string to an {@link OADateTime} using the default format.
	 *
	 * @param time the string representation of the time
	 * @return an {@link OADateTime} representing the parsed time, or {@code null} if parsing fails
	 */
	public static OADateTime valueOf(String time) {
		return OATime.valueOf(time, null);
	}

	/**
	 * Sets the default global format used when converting times to strings.
	 *
	 * @param fmt the format to use as the global output format
	 */
	public static void setGlobalOutputFormat(String fmt) {
		timeOutputFormat = fmt;
	}

	/**
	 * Returns the default global format used when converting times to strings.
	 *
	 * @return the global output format
	 */
	public static String getGlobalOutputFormat() {
		return timeOutputFormat;
	}

	/**
	 * Adds a global parse format used when converting strings to times.
	 *
	 * @param fmt the format to add
	 */
	public static void addGlobalParseFormat(String fmt) {
		alTimeParseFormat.add(fmt);
	}

	/**
	 * Removes a global parse format used when converting strings to times.
	 *
	 * @param fmt the format to remove
	 */
	public static void removeGlobalParseFormat(String fmt) {
		alTimeParseFormat.remove(fmt);
	}

	
	/**
	 * Converts this time to a {@link LocalTime} instance.
	 *
	 * @return a {@link LocalTime} representing this time
	 */
	public LocalTime getLocalTime() {
		LocalTime lt = LocalTime.of(get24Hour(), getMinute(), getSecond(), (int) (getMilliSecond() * (Math.pow(10, 6))));
		return lt;
	}

	/**
	 * Converts this time to a string using the default output format.
	 *
	 * @return the formatted time string
	 */
	public String toString() {
		return toString(null);
	}

	/**
	 * Converts this time to a string using the supplied format.
	 * <p>
	 * If the format is {@code null}, a default format is selected.
	 *
	 * @param f the format to use, or {@code null} to use the default
	 * @return the formatted time string
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


	@Override
	public OADateTime withYear(int year) {
		return new OATime(this);
	}
	@Override
	public OADateTime withMonth(int month) {
		return new OATime(this);
	}
	@Override
	public OADateTime withDay(int day) {
		return new OATime(this);
	}
	@Override
	public OADateTime withDate(int year, int month, int day) {
		return new OATime(this);
	}
	
	@Override
	public OADateTime withHour(int hour) {
		OADateTime dt = super.withHour(hour);
		return new OATime(dt);
	}
	@Override
	public OADateTime withMinute(int minute) {
		OADateTime dt = super.withMinute(minute);
		return new OATime(dt);
	}
	@Override
	public OADateTime withSecond(int second) {
		OADateTime dt = super.withSecond(second);
		return new OATime(dt);
	}
	@Override
	public OADateTime withMilliSecond(int ms) {
		OADateTime dt = super.withMilliSecond(ms);
		return new OATime(dt);
	}

	@Override
	public OADateTime withTime(int hour, int minute) {
		OADateTime dt = super.withTime(hour, minute);
		return new OATime(dt);
	}
	@Override
	public OADateTime withTime(int hour, int minute, int second) {
		OADateTime dt = super.withTime(hour, minute, second);
		return new OATime(dt);
	}
	@Override
	public OADateTime withTime(int hour, int minute, int second, int millisecond) {
		OADateTime dt = super.withTime(hour, minute, second, millisecond);
		return new OATime(dt);
	}
	
	@Override
	public OADateTime withTimeZone(TimeZone tz) {
		OADateTime dt = super.withTimeZone(tz);
		return new OATime(dt);
	}
}
