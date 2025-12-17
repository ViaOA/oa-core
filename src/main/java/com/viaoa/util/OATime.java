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
package com.viaoa.util;

import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

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
	private static Vector vecTimeParseFormat = new Vector(10, 10);

	static {
		vecTimeParseFormat.addElement("hh:mm:ss.S a");
		vecTimeParseFormat.addElement("hh:mm:ss a");
		vecTimeParseFormat.addElement("hh:mm a");

		vecTimeParseFormat.addElement("hh:mm:ss.Sa");
		vecTimeParseFormat.addElement("hh:mm:ssa");
		vecTimeParseFormat.addElement("hh:mma");

		vecTimeParseFormat.addElement("HH:mm:ss.S");
		vecTimeParseFormat.addElement("HH:mm:ss");
		vecTimeParseFormat.addElement("HH:mm");

		vecTimeParseFormat.addElement("hha");
		vecTimeParseFormat.addElement("hh a");
		vecTimeParseFormat.addElement("HH");
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
	 * Creates a new time instance using the supplied {@link java.sql.Time}.
	 * <p>
	 * The date portion is cleared so that only time values are retained.
	 *
	 * @param time the SQL time value to use
	 */
	public OATime(java.sql.Time time) {
		super(time);
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
		this(new java.sql.Time(date.getTime()));
		clearDate();
	}

	/**
	 * Creates a new time instance using the supplied time in milliseconds.
	 * <p>
	 * The date portion is cleared so that only time values are retained.
	 *
	 * @param time the time value in milliseconds since the epoch
	 */
	public OATime(long time) {
		this(new java.sql.Time(time));
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
	 * @param od the date-time instance to copy from
	 */
	public OATime(OADateTime od) {
		super(od);
		clearDate();
		this.timeZone = od.getTimeZone();
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
		this(OATime.valueOf(strTime));
		clearDate();
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
		this(OATime.valueOf(strTime, fmt));
		clearDate();
	}

	/**
	 * Creates a new time instance using the supplied {@link LocalTime}.
	 *
	 * @param lt the local time whose hour, minute, second, and millisecond values are used
	 */
	public OATime(LocalTime lt) {
		this(lt.getHour(), lt.getMinute(), lt.getSecond(), (int) (lt.getNano() / Math.pow(10, 6)));
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
		super(0, 0, 0, hrs, mins, secs, 0);
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
		super(0, 0, 0, hrs, mins, secs, mili);
		clearDate();
	}

	/**
	 * Compares this time with another object.
	 * <p>
	 * The supplied object is compared using {@link #compareTo(Object)}.
	 *
	 * @param obj the object to compare with
	 * @return 0 if equal, -1 if less than, 1 if greater than, or 2 if not comparable
	 */
	public int compare(Object obj) {
		return this.compareTo(obj);
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
				f = "hh:mma";
			}
		}
		return toStringMain(f);
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
		if (time != null && time.length() > 0) {
			char c = time.charAt(time.length() - 1);
			if (c == 'A' || c == 'a' || c == 'P' || c == 'p') {
				time += "m";
			}
		}

		Date d = valueOfMain(time, fmt, vecTimeParseFormat, timeOutputFormat);
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
		vecTimeParseFormat.addElement(fmt);
	}

	/**
	 * Removes a global parse format used when converting strings to times.
	 *
	 * @param fmt the format to remove
	 */
	public static void removeGlobalParseFormat(String fmt) {
		vecTimeParseFormat.removeElement(fmt);
	}

	/**
	 * Removes all global parse formats used when converting strings to times.
	 */
	public static void removeAllGlobalParseFormats() {
		vecTimeParseFormat.removeAllElements();
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

}
