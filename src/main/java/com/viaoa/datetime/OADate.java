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

import com.viaoa.lang.OAString;

/**
 * Immutable date-only value that represents a calendar day independent from
 * time-of-day. OADate is suitable for business rules where wall-clock time is
 * irrelevant (e.g., birthdays, holidays, settlement dates).
 *
 * <p><b>Timezone behavior:</b><br>
 * OADate stores an underlying instant via {@link OADateTime}, but the time
 * fields are always normalized to midnight and ignored for comparisons,
 * formatting, and field access. This ensures that a given date is interpreted
 * consistently across JVM default timezones.
 *
 * <p><b>Key properties:</b>
 * <ul>
 *   <li>Thread-safe and immutable</li>
 *   <li>Parsing supports multiple global formats</li>
 *   <li>Formatting uses a global default when none is specified</li>
 *   <li>Interoperates with {@link java.time.LocalDate}</li>
 * </ul>
 *
 * <p>Default formats used include:
 * <ul>
 *   <li>{@code "MM/dd/yyyy"}</li>
 *   <li>{@code "dd/MM/yyyy"}</li>
 *   <li>{@code "yyyy-MM-dd"} (SQL)</li>
 * </ul>
 *
 * @see OADateTime
 * @see OATime
 */
public class OADate extends OADateTime {
	private static final long serialVersionUID = 1L;

	/**
	 * Standard date format: year-month-day.
	 */
	public final static String Format1 = "yyyy-MM-dd";

	/**
	 * Standard date format: month/day/year.
	 */
	public final static String Format2 = "MM/dd/yyyy";
	
	/**
	 * Standard date format without separators.
	 */
	public final static String Format3 = "yyyyMMdd";
	
	/**
	 * Standard date format using abbreviated month name.
	 */
	public final static String Format4 = "yyyy-MMM-dd";
	
	/**
	 * Standard date format using month name and comma.
	 */
	public final static String Format5 = "MMM dd, yyyy";
	
	/**
	 * Standard date format using abbreviated year.
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
	/** default output format. Default is DateFormat.SHORT */
	protected static String dateOutputFormat;

	/**
	 * Collection of date formats used when parsing strings into dates.
	 */
	private static final List<String> alDateParseFormat = new ArrayList<>();

	static {
		setLocale(Locale.getDefault());
	}

	/**
	 * Sets the default global output format used when converting dates to strings.
	 *
	 * @param fmt the format string to use
	 */
	public static void setGlobalOutputFormat(String fmt) {
		dateOutputFormat = fmt;
	}

	/**
	 * Returns the default global output format used when converting dates to strings.
	 *
	 * @return the global output format
	 */
	public static String getGlobalOutputFormat() {
		return dateOutputFormat;
	}

	/**
	 * Adds a global parse format used when converting strings to dates.
	 *
	 * @param fmt the format string to add
	 */
	public static void addGlobalParseFormat(String fmt) {
		alDateParseFormat.add(fmt);
	}

	/**
	 * Removes a global parse format used when converting strings to dates.
	 *
	 * @param fmt the format string to remove
	 */
	public static void removeGlobalParseFormat(String fmt) {
		alDateParseFormat.remove(fmt);
	}
	
	/**
	 * Sets locale-specific date parsing and output formats.
	 *
	 * @param loc the locale to use when determining date formats
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
	 * Creates a new date initialized to today's date.
	 */
	public OADate() {
		this(LocalDate.now());
	}

	/**
	 * Creates a new date from a millisecond time value.
	 *
	 * @param time milliseconds since epoch
	 */
	public OADate(long time) {
		this(new Date(time));
	}
	
	
	/**
	 * Creates a new date from a {@link Date} instance.
	 *
	 * @param date the source date
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
	 * Creates a new date using year, month, and day values.
	 *
	 * @param year the full year value
	 * @param month the month value (0–11)
	 * @param day the day of the month
	 */
	public OADate(int year, int month, int day) {
		super(year, month, day);
	    this.type = DateTimeType.Floating;
	    this.zoneId = defaultZoneId;
	}
	
	/**
	 * Creates a new date from an {@link OADateTime} instance.
	 *
	 * @param odt the source date-time
	 */
	public OADate(OADateTime dt) {
		super(dt.getYear(), dt.getMonthValue(), dt.getDayOfMonth());
	    this.type = DateTimeType.Floating;
	    this.zoneId = defaultZoneId;
	}
	
	/**
	 * Creates a new date from a {@link Calendar} instance.
	 *
	 * @param c the source calendar
	 */
	public OADate(Calendar c) {
		super(c.get(c.YEAR), c.get(c.MONTH) + 1, c.get(c.DATE));
	    this.type = DateTimeType.Floating;
	    this.zoneId = defaultZoneId;
	}

	/**
	 * Creates a new date from a {@link LocalDate} instance.
	 *
	 * @param ld the source local date
	 */
	public OADate(LocalDate ld) {
		super(ld.getYear(), ld.getMonthValue(), ld.getDayOfMonth());
	    this.type = DateTimeType.Floating;
	    this.zoneId = defaultZoneId;
	}
	
	/**
	 * Creates a new date from a string value.
	 *
	 * @param strDate the string representation of the date
	 */
	public OADate(String strDate) {
		this(strDate, null);
	    this.type = DateTimeType.Floating;
	    this.zoneId = defaultZoneId;
	}

	/**
	 * Creates a new date from a string value using a specified format.
	 *
	 * @param strDate the string representation of the date
	 * @param format the format to use for parsing
	 */
	public OADate(String strDate, String format) {
		OADateTime dt = OADateTime.valueOf(strDate, format);
		if (dt == null) throw new IllegalArgumentException("OADate cant create date from String \"" + strDate + "\", format="+format);
		LocalDate ld = LocalDate.of(dt.getYear(), dt.getMonthValue(), dt.getDayOfMonth());
		
	    this.type = DateTimeType.Floating;
	    this.zoneId = defaultZoneId;
	    this._time = ld.atStartOfDay().atZone(this.zoneId).toInstant().toEpochMilli();
	}

	
	
	@Override
	public OADateTime withTimeZoneUTCSameWallTime() {
		return new OADate(super.withTimeZoneUTCSameWallTime());
	}

	@Override
	public OADateTime withTimeZoneUTCSameInstant() {
		return new OADate(super.withTimeZoneUTCSameInstant());
	}

	@Override
	public OADateTime withZoneIdSameWallTime(ZoneId zid) {
		return new OADate(super.withZoneIdSameWallTime(zid));
	}
	
	@Override
	public OADateTime withZoneIdSameInstant(ZoneId zid) {
		return new OADate(super.withZoneIdSameInstant(zid));
	}
	
	
    /**
     * Converts this date to a string using the default format.
     *
     * @return the formatted date string
     */
	@Override
	public String toString() {
		return toString(null);
	}

	/**
	 * Converts this date to a string using the specified format.
	 *
	 * @param f the format string to use
	 * @return the formatted date string
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
	 * Converts a string to an {@link OADate} using the specified format.
	 *
	 * @param date the string representation of the date
	 * @param fmt the format string to use
	 * @return the parsed OADate instance
	 */
	public static OADate dateValue(String date, String fmt) {
		return (OADate) OADate.valueOf(date, fmt);
	}

	/**
	 * Converts a string to an {@link OADate} using default parsing rules.
	 *
	 * @param date the string representation of the date
	 * @return the parsed OADate instance
	 */
	public static OADate dateValue(String date) {
		return (OADate) OADate.valueOf(date, null);
	}

	/**
	 * Converts a string to an {@link OADateTime} using the specified format
	 * and global parsing rules.
	 *
	 * @param date the string representation of the date
	 * @param fmt the format string to use
	 * @return the parsed OADateTime instance, or null if parsing fails
	 */
	public static OADateTime valueOf(String date, String fmt) {
		if (date == null) {
			return null;
		}
		OADateTime dt = OADate.valueOfMain(date, fmt, alDateParseFormat, dateOutputFormat);
		if (dt == null) {
			if (date.length() < 6 && OAString.isNumber(date)) {
				return OADate.valueOf(date + "/" + (new OADate()).getYear());
			}

			dt = valueOfMain(fixDate(date), fmt, alDateParseFormat, dateOutputFormat);
			if (dt == null) {
				return null;
			}
		}
		return new OADate(dt);
	}

//qqqqqqqqqq change name to createUtil	
    protected OADateTime createUtil(ZonedDateTime zdt) {
		OADate d = new OADate(zdt.toLocalDate());
		d.type = this.type;
		d.zoneId = this.zoneId;
		return d;
    }
//qqqqqqqqqq change name to createUtil	
	protected OADateTime createUtil(int year, int month, int day, int hrs, int mins, int secs, int milsecs) {
		OADateTime dt = new OADate(year, month, day);
		dt.zoneId = this.zoneId;
		dt.type = this.type;
		return dt;
	}
	
    
	/**
	 * Converts a string to an {@link OADateTime} using default parsing rules.
	 *
	 * @param date the string representation of the date
	 * @return the parsed OADateTime instance, or null if parsing fails
	 */
	public static OADateTime valueOf(String date) {
		return OADate.valueOf(date, null);
	}

}

