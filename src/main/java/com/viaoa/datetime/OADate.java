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
import java.time.LocalDate;
import java.util.*;

import com.viaoa.datetime.OATimeZone.TZ;
import com.viaoa.lang.OAString;

/*qqqqqqqqqqqqqqqqqqqqqq
CODEX

1. file/class/method
     src/main/java/com/viaoa/datetime/OADate.java — OADate(LocalDate ld)
  2. concrete bug
     LocalDate conversion uses deprecated new Date(year, month, day), which resolves midnight in the JVM default
     timezone instead of OA’s configured date semantics.
  3. runtime scenario
     If OA default timezone is UTC but the JVM default timezone is America/Chicago, new OADate(LocalDate.of(2026, 5,
     18)) stores an epoch millis based on Chicago midnight. Serialized, compared, or reconstructed elsewhere, that
     backing millis can represent a different date boundary than the OA runtime expects.
  4. why this violates OA/OG datetime semantics
     LocalDate is date-only and should not silently inherit JVM-default timezone behavior. OA date-only values need
     deterministic distributed semantics.
  5. minimal fix direction
     Construct using an explicit OA timezone/calendar, or store date-only fields using the same normalized path
     intended for OADate.
  6. suggested CODEX comment location
     Above OADate(LocalDate ld).


*/

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
		super();
		clearTime();
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
		super(date);
		clearTime();
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
	}
	
	/**
	 * Creates a new date from an {@link OADateTime} instance.
	 *
	 * @param odt the source date-time
	 */
	public OADate(OADateTime odt) {
		super(odt.getYear(), odt.getMonth(), odt.getDay());
	}
	
	/**
	 * Creates a new date from a {@link Calendar} instance.
	 *
	 * @param c the source calendar
	 */
	public OADate(Calendar c) {
		super(c.get(c.YEAR), c.get(c.MONTH), c.get(c.DATE));
	}

	/**
	 * Creates a new date from a {@link LocalDate} instance.
	 *
	 * @param ld the source local date
	 */
	public OADate(LocalDate ld) {
		super(ld.getYear(), ld.getMonthValue() - 1, ld.getDayOfMonth());
	}
	
	/**
	 * Creates a new date from a string value.
	 *
	 * @param strDate the string representation of the date
	 */
	public OADate(String strDate) {
		this(strDate, null);
	}

	/**
	 * Creates a new date from a string value using a specified format.
	 *
	 * @param strDate the string representation of the date
	 * @param format the format to use for parsing
	 */
	public OADate(String strDate, String format) {
		OADateTime dt = OADateTime.valueOf(strDate, format);
		if (dt == null) throw new IllegalArgumentException("OADate cant create date from String \"" + strDate + "\"");		
		setDate(dt.getYear(), dt.getMonth(), dt.getDay());
		if (dt.timeZone != null) setTimeZone(timeZone);
	}

	/**
	 * Tests whether this date is between or equal to two other values.
	 *
	 * @param obj1 the lower bound value
	 * @param obj2 the upper bound value
	 * @return true if between or equal
	 */
	public boolean between(Object obj1, Object obj2) {
		int i = compareTo(obj1);
		if (i < 0) {
			return false;
		}
		i = compareTo(obj2);
		return (i <= 0);
	}

	/**
	 * Returns a {@link LocalDate} representing this date.
	 *
	 * @return the corresponding LocalDate value
	 */
	public LocalDate getLocalDate() {
		LocalDate ld = LocalDate.of(getYear(), getMonth() + 1, getDay());
		return ld;
	}
	
	@Override
	protected void setCalendar(GregorianCalendar c) {
		if (c == null) return;
		setCalendar(c.get(c.YEAR), c.get(c.MONTH), c.get(c.DAY_OF_MONTH), 0, 0, 0, 0);
	}

	@Override
	protected void setCalendar(Date date) {
		super.setCalendar(date);
		clearTime();
	}

	@Override
	protected void setCalendar(int year, int month, int day, int hrs, int mins, int secs, int milsecs) {
		super.setCalendar(year, month, day, 0,0,0,0);
	}

	@Override
	protected void setCalendar(String strDate) {
		OADateTime dt = valueOf(strDate);
		super.setCalendar(dt.getYear(), dt.getMonth(), dt.getDay(), 0, 0, 0, 0);
	}
	
	@Override
	protected void setCalendar(String strDate, String fmt) {
		OADateTime dt = valueOf(strDate, fmt);
		super.setCalendar(dt.getYear(), dt.getMonth(), dt.getDay(), 0, 0, 0, 0);
	}
	
	@Override
	public void setTime(int hr, int m, int s, int ms) {
		// no-op
	}
	
	@Override
	public void setTime(OATime t) {
		// no-op
	}

	@Override
	public void setTimeZone(TimeZone tz) {
	    super.setTimeZone(tz);
	    clearTime();
	}
	
	@Override
	public void setTimeZoneUTC() {
	    super.setTimeZoneUTC();
	    clearTime();
	}

	@Override
	public void setTimeZone(TZ tz) {
	    super.setTimeZone(tz);
	    clearTime();
	}
	
	@Override
	public void setHour(int hr) {
	    // no-op
	}

	@Override
	public void set12Hour(int hr) {
	    // no-op
	}
	
	@Override
	public void set24Hour(int hr) {
	    // no-op
	}
	
	@Override
	public void setMinute(int mins) {
	    // no-op
	}
	
	@Override
	public void setSecond(int s) {
	    // no-op
	}
	
	@Override
	public void setMilliSecond(int ms) {
	    // no-op
	}
	
	@Override
	public OADateTime addHours(int amount) {
		OADateTime dt = super.addHours(amount);
		OADate d = new OADate(dt);
		return d;
	}
	@Override
	public OADateTime subtractHours(int amount) {
		OADateTime dt = super.subtractHours(amount);
		OADate d = new OADate(dt);
		return d;
	}

	@Override
	public OADateTime addMinutes(int amount) {
		OADateTime dt = super.addMinutes(amount);
		OADate d = new OADate(dt);
		return d;
	}
	@Override
	public OADateTime subtractMinutes(int amount) {
		OADateTime dt = super.subtractMinutes(amount);
		OADate d = new OADate(dt);
		return d;
	}
	
	@Override
	public OADateTime addSeconds(int amount) {
		OADateTime dt = super.addSeconds(amount);
		OADate d = new OADate(dt);
		return d;
	}
	@Override
	public OADateTime subtractSeconds(int amount) {
		OADateTime dt = super.subtractSeconds(amount);
		OADate d = new OADate(dt);
		return d;
	}

	@Override
	public OADateTime addMilliSeconds(int amount) {
		OADateTime dt = super.addMilliSeconds(amount);
		OADate d = new OADate(dt);
		return d;
	}
	@Override
	public OADateTime subtractMilliSeconds(int amount) {
		OADateTime dt = super.subtractMilliSeconds(amount);
		OADate d = new OADate(dt);
		return d;
	}
	
	@Override
	public OADateTime convertToUTC() {
		OADateTime dt = super.convertToUTC();
		OADate d = new OADate(dt);
		return d;
	}
	
	@Override
	public OADateTime convertTo(TimeZone tz) {
		OADateTime dt = super.convertTo(tz);
		OADate d = new OADate(dt);
		return d;
	}
	
	@Override
	public OADateTime convertTo(OATimeZone.TZ tz) {
		OADateTime dt = super.convertTo(tz);
		OADate d = new OADate(dt);
		return d;
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
		return (OADate) valueOf(date, fmt);
	}

	/**
	 * Converts a string to an {@link OADate} using default parsing rules.
	 *
	 * @param date the string representation of the date
	 * @return the parsed OADate instance
	 */
	public static OADate dateValue(String date) {
		return (OADate) valueOf(date, null);
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
		Date d = valueOfMain(date, fmt, alDateParseFormat, dateOutputFormat);
		if (d == null) {
			if (date.length() < 6 && OAString.isNumber(date)) {
				return OADate.valueOf(date + "/" + (new OADate()).getYear());
			}

			d = valueOfMain(fixDate(date), fmt, alDateParseFormat, dateOutputFormat);
			if (d == null) {
				return null;
			}
		}
		return new OADate(d);
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

