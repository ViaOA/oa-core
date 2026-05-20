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
package com.viaoa.process;

import java.util.Arrays;
import java.util.Calendar;

import com.viaoa.converter.OAConv;
import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;
import com.viaoa.hub.HubEvent;
import com.viaoa.lang.OAArray;
import com.viaoa.lang.OAString;

/*qqqqqqqqqqqqqqqq
CODEX

5. OACron / findNext and private search methods
     Severity: Medium
     Bug/risk: findNext(OADateTime) stores the input in mutable instance field dtFrom, and all private search methods
     read that field. Concurrent calls on the same OACron can interleave and compute a result using another caller’s
     start time.
     Production impact: A cron used by both scheduled processing and manual/status calls can return wrong next
     execution times, causing missed or duplicate scheduling decisions.
     Area: src/main/java/com/viaoa/process/OACron.java:475
     Minimal hardening: Make the search state local by passing dtFrom through helper methods, or synchronize findNext.

2. OACron / getInts silently broadens invalid cron fields into wildcard schedules
     Severity: High
     Bug/risk: getInts treats any field containing * as wildcard without validating the rest of the field. It also
     treats reversed ranges such as 10-5 as an empty parsed array without setting bValid = false; empty array means
     “all values.”
     Runtime scenario: A cron field like 10-5 for minutes becomes every minute. A typo like 1,* or *x becomes wildcard
     instead of invalid.
     Production impact: Misconfigured jobs can run far more often than intended, silently. For production background
     workflows, that can create duplicate processing, load spikes, or repeated side effects.
     Area: src/main/java/com/viaoa/process/OACron.java:725, src/main/java/com/viaoa/process/OACron.java:746
     Minimal hardening: Only accept * when the trimmed field equals "*". Mark reversed ranges invalid unless
     explicitly supported. If parsing produces no values for a non-wildcard field, mark invalid.
  3. OACron / enabled and last-run state are cross-thread mutable without visibility guarantees
     Severity: Medium
     Bug/risk: bEnabled and dtLast are written/read across scheduler, worker, and caller threads but are not volatile
     or synchronized. OACronProcessor.runThread() reads cron.getEnabled() while application code can call
     setEnabled(false) concurrently. Worker threads call setLast, while monitoring code can read getLast.
     Production impact: Disabling a cron may not be observed promptly by the scheduler thread, and last-run monitoring
     can see stale state. That can produce an unexpected extra execution or misleading runtime status.
     Area: src/main/java/com/viaoa/process/OACron.java:196, src/main/java/com/viaoa/process/OACron.java:432, src/main/
     java/com/viaoa/process/OACron.java:789
     Minimal hardening: Make bEnabled and dtLast volatile, or synchronize cron state access. If richer lifecycle is
     added, use explicit job state.


*/


/**
 * Used to define and find the next time a Cron-like entry should be ran.

        *     *     *   *    *        command to be executed
        -     -     -   -    -
        |     |     |   |    |
        |     |     |   |    +----- day of week (0-6) (Sunday=0) - *Java: 1-7 (Sunday=1)
        |     |     |   +---------- month (1-12) - Java: 0-11    
        |     |     +-------------- day of month (1-31), also allows "last"
        |     +-------------------- hour (0 - 23)
        +-------------------------- min (0 - 59)

 Each field needs to be separate by space or tab.
 Field values can use a single number, "-" for a range, and commas to separate more then one.

 see OACronProcessor# to register cron to be processed
 */


/**
 * Represents a cron-style schedule definition and provides logic to determine
 * the next date and time that satisfies the specification. <p>
 *
 * Each cron consists of five fields:
 * <ul>
 *   <li>minute (0–59)</li>
 *   <li>hour (0–23)</li>
 *   <li>day of month (1–31 or "last")</li>
 *   <li>month (1–12)</li>
 *   <li>day of week (0–6, Sunday=0)</li>
 * </ul>
 *
 * Field values may be expressed as single numbers, comma-separated lists,
 * ranges, or wildcards. Parsed values are stored in sorted form as cron
 * integers. <p>
 *
 * The {@link #findNext(com.viaoa.datetime.OADateTime)} method walks forward from
 * a given starting date and computes the earliest matching time. Subclasses
 * implement {@link #process(boolean)} to perform work when the cron fires.
 */
public abstract class OACron {
    // NOTE: all values are stored as cron values
	
	/**
	 * Starting date/time used by the cron when computing the next matching
	 * scheduled time. Set when {@link #findNext(OADateTime)} is called.
	 */
    private OADateTime dtFrom;

    /**
     * Raw minute field definition string supplied to the constructor. Represents
     * the cron-style minute specification before parsing.
     */
    private String strMins;
    
    /**
     * Raw hour field definition string supplied to the constructor. Represents
     * the cron-style hour specification before parsing.
     */
    private String strHours;
    
    /**
     * Raw day-of-month field definition string supplied to the constructor.
     * Can include numeric values, ranges, lists, or the keyword "last".
     */
    private String strDayOfMonth;
    
    /**
     * Raw month field definition string supplied to the constructor. Parsed
     * into cron-style month values (1–12).
     */
    private String strMonth;
    
    /**
     * Raw day-of-week field definition string supplied to the constructor.
     * Uses cron-style values 0–6 (Sunday = 0).
     */
    private String strDayOfWeek;

    // store sorted cron values (not Java)
    
    /**
     * Parsed and sorted minute values derived from {@code strMins}. Stored
     * using cron-style numbering (0–59). An empty array represents "any minute".
     */
    private int[] mins;

    /**
     * Parsed and sorted hour values derived from {@code strHours}. Stored
     * using cron-style numbering (0–23). An empty array represents "any hour".
     */
    private int[] hrs;

    /**
     * Parsed and sorted day-of-month values derived from {@code strDayOfMonth}.
     * May include explicit days or be empty to represent "any day". Evaluated
     * alongside the "last day" flag when determining matches.
     */
    private int[] monthDays;
    
    /**
     * Parsed and sorted day-of-week values derived from {@code strDayOfWeek}.
     * Stored using cron-style numbering (0–6). An empty array represents any day
     * of the week.
     */
    private int[] daysOfWeek;
    
    /**
     * Parsed and sorted month values derived from {@code strMonth}. Stored as
     * cron-month values (1–12). An empty array is converted to all months.
     */
    private int[] months;

    /**
     * Flag indicating whether the schedule includes the last day of the month.
     * Set during parsing when the keyword "last" is encountered.
     */
    private boolean bIncludeLastDayOfMonth;

    /**
     * Indicates whether all field values parsed successfully and fall within
     * accepted cron ranges. Used to prevent further processing on invalid cron entries.
     */
    private boolean bValid;

    /**
     * Optional descriptive name assigned to this cron entry.
     */
    private String name;

    /**
     * Lazily built human-readable description summarizing the cron schedule.
     * Constructed on demand when {@link #getDescription()} is called.
     */
    private String description;
    
    /**
     * Indicates whether this cron entry is enabled. Disabled entries do not
     * participate in scheduling or processing.
     */
    private boolean bEnabled = true;

    /**
     * Timestamp marking when this cron entry was instantiated. Initialized
     * by the constructor.
     */
    private OADateTime dtCreated;
    
    /**
     * Timestamp of the last time this cron entry was processed. Updated via
     * {@link #setLast(OADateTime)}.
     */
    private OADateTime dtLast;
    
    /**
     * Creates a new cron entry and parses all five cron specification fields.
     *
     * Initializes creation date, stores raw definition strings, and pre-parses
     * field values into sorted integer arrays. Also performs validation checks
     * on resulting values.
     *
     * @param strMins        minute field specification
     * @param strHours       hour field specification
     * @param strDayOfMonth  day-of-month field specification, supporting "last"
     * @param strMonth       month field specification
     * @param strDayOfWeek   day-of-week field specification
     */
    public OACron(String strMins, String strHours, String strDayOfMonth, String strMonth, String strDayOfWeek) {
        this.dtCreated = new OADateTime();
        this.strMins = strMins;
        this.strHours = strHours;
        this.strDayOfMonth = strDayOfMonth;
        this.strMonth = strMonth;
        this.strDayOfWeek = strDayOfWeek;

        // set to default, can get changed when calling getInts
        bValid = true;
        bIncludeLastDayOfMonth = false;
        
        mins = getInts(strMins);
        hrs = getInts(strHours);
        monthDays = getInts(strDayOfMonth, true);
        daysOfWeek = getInts(strDayOfWeek);
        months = getInts(strMonth);

        if (bValid) {
            for (int x : mins) {
                if (x < 0 || x > 59) bValid = false;
            }
            for (int x : hrs) {
                if (x < 0 || x > 23) bValid = false;
            }
            for (int x : monthDays) {
                if (x < 1 || x > 31) bValid = false;
            }
            for (int x : daysOfWeek) {
                if (x < 0 || x > 6) bValid = false;
            }
            
            if (months == null || months.length == 0) { // any month
                months = new int[12];
                for (int i=0; i<12; i++) months[i] = i+1; // store as cron month is 1-12, java is 0-11
            }
            for (int x : months) {
                if (x < 1 || x > 12) bValid = false;
            }
        }
    }

    /**
     * Method invoked when the cron schedule triggers. Subclasses implement
     * the actual work to perform.
     *
     * @param bManuallyCalled true if triggered manually rather than by schedule
     */
    public abstract void process(final boolean bManuallyCalled);
    
    
    /**
     * Returns the parsed minute values for this cron entry.
     *
     * @return array of minute values, or an empty array if all minutes are allowed
     */
    public int[] getMinutes() {
        return mins;
    }

    /**
     * Returns the parsed hour values for this cron entry.
     *
     * @return array of hour values, or an empty array if all hours are allowed
     */
    public int[] getHours() {
        return hrs;
    }

    /**
     * Returns the parsed day-of-month values.
     *
     * @return array of day-of-month values, possibly empty if any day is allowed
     */
    public int[] getMonthDays() {
        return monthDays;
    }

    /**
     * Returns the parsed day-of-week values.
     *
     * @return array of cron-style weekday values, or empty if all are allowed
     */
    public int[] getDaysOfWeek() {
        return daysOfWeek;
    }
    
    /**
     * Returns the parsed month values used by this cron entry.
     *
     * @return array of month values (1–12)
     */
    public int[] getMonths() {
        return months;
    }

    /**
     * Indicates whether this cron entry includes the last day of each month.
     *
     * @return true if "last" was specified in the day-of-month field
     */
    public boolean getIncludeLastDayOfMonth() {
        return bIncludeLastDayOfMonth;
    }

    /**
     * Determines whether the cron definition is valid after parsing.
     *
     * @return true if all fields were parsed successfully and verified
     */
    public boolean isValid() {
        return bValid;
    }

    /**
     * Builds and returns a human-readable description of this cron entry.
     * The description is assembled from parsed field values and cached for
     * subsequent calls. If the cron is invalid, the returned description is
     * prefixed with "INVALID:".
     *
     * @return formatted description of this cron schedule
     */
    public String getDescription() {
        if (description != null) return description;
        description = "";
        int x;

        if (months.length == 12) {
            //if (description.length() > 0) description += "; and ";
            //else description += "all months";
        }
        else if (months.length > 0) {
            if (description.length() > 0) description += "; and";
            else description += "when";
            description += " month is ";

            for (int i=0; i<months.length; i++) {
                x = months[i];
                if (i > 0) description += " or ";
                if (x < 1 || x > 12) {
                    description += "Invalid:"+x;
                }
                else {
                    OADate d = new OADate(2017, x-1, 1);
                    description += d.toString("MMM");
                }
            }
        }

        if (monthDays.length > 0 || bIncludeLastDayOfMonth) {
            if (description.length() > 0) description += "; and";
            else description += "when";
            description += " day of month is ";
            for (int i=0; i<monthDays.length; i++) {
                x = monthDays[i];
                if (i > 0) description += " or ";
                if (x < 0 || x > 31) description += "Invalid:"+x;
                else description += ""+x;
            }
            if (bIncludeLastDayOfMonth) {
                if (monthDays.length > 0) description += " or ";
                description += "last day";
            }
        }

        if (daysOfWeek.length > 0) {
            if (description.length() > 0) description += "; and";
            else description += "when";
            description += " day of week is ";


            for (int i=0; i<daysOfWeek.length; i++) {
                x = daysOfWeek[i];
                if (i > 0) description += " or ";
                if (x < 0 || x > 6) description += "Invalid:"+x;
                else {
                    String s;
                    for (int j=0; j<7; j++) {
                        OADate d = new OADate(2017, 0, 1+j);
                        if (d.getDayOfWeek() != x+1) continue;
                        description += d.toString("EEE");
                        break;
                    }
                }
            }
        }

        if (hrs.length > 0) {
            if (description.length() > 0) description += "; and";
            else description += "when";
            description += " hour is ";
            for (int i=0; i<hrs.length; i++) {
                x = hrs[i];
                if (i > 0) description += " or ";
                if (x < 0 || x > 23) description += "Invalid:"+x;
                else description += ""+x;
            }
        }


        if (mins.length > 0) {
            if (description.length() > 0) description += "; and";
            else description += "when";
            description += " minute is ";
            for (int i=0; i<mins.length; i++) {
                x = mins[i];
                if (i > 0) description += ", ";
                if (x < 0 || x > 59) description += "Invalid:"+x;
                else description += ""+x;
            }
        }
        else {
            if (description.length() > 0) description += "; and ";
            description += "every minute";
        }


        if (!isValid()) description = "INVALID: "+description;

        return description;
    }

    /**
     * Returns the timestamp of the last time this cron entry was processed.
     *
     * @return date/time of last execution, or null if not yet processed
     */
    public OADateTime getLast() {
        return dtLast;
    }

    /**
     * Updates the timestamp of the last time this cron entry was processed.
     *
     * @param dt the date/time this cron last executed
     */
    public void setLast(OADateTime dt) {
        this.dtLast = dt;
    }
    
    /**
     * Computes the next date and time that satisfy this cron entry, using
     * the current time as the starting point.
     *
     * @return next matching date/time, or null if invalid
     */
    public OADateTime getNext() {
        return findNext(new OADateTime());
    }

    /**
     * Computes the next date and time that satisfy this cron entry, beginning
     * from the supplied date/time.
     *
     * @param dtFrom starting point for evaluation
     * @return next matching date/time, or null if invalid
     */
    public OADateTime getNext(OADateTime dtFrom) {
        return findNext(dtFrom);
    }
    
    /**
     * Computes the next scheduled date/time using the current moment as the
     * starting point.
     *
     * @return next matching date/time, or null if invalid
     */
    public OADateTime findNext() {
        return findNext(new OADateTime());
    }

    /**
     * Computes the next scheduled date/time beginning from the specified
     * starting point. Stores {@code dtFrom} for use during matching and
     * delegates month-level evaluation to {@link #findNextMonth()}.
     *
     * @param dtFrom the date/time to begin searching from
     * @return next matching date/time, or null if invalid
     */
    public OADateTime findNext(OADateTime dtFrom) {
        if (!isValid()) return null;
        if (dtFrom == null) dtFrom = new OADateTime();
        this.dtFrom = dtFrom;

        OADateTime dtFound = findNextMonth();
        return dtFound;
    }

    /**
     * Evaluates future months to determine the next month/day combination
     * that satisfies this cron entry. Iterates through allowed months,
     * adjusting year boundaries as needed, and delegates further matching to
     * {@link #findNextMonthDay(OADateTime, OADateTime, OADateTime)}.
     *
     * @return earliest matching date/time within valid months
     */
    private OADateTime findNextMonth() {
        OADateTime dtFound = null;

        final int fromMonth = dtFrom.getMonth();

        OADateTime dtCheck = new OADateTime(dtFrom.getTime());
        for (int i=0; ;i++) {
            if (i > 0) dtCheck = dtCheck.addYears(1);
            for (int m : months) {
                m--; // cron month is 1-12, java is 0-11

                if (i == 0 && m < dtFrom.getMonth()) continue;

                dtCheck.setDay(1);
                dtCheck.setMonth(m);
                dtCheck.clearTime();
                if (dtFound != null && dtFound.before(dtCheck)) continue;
                OADateTime dtTo = dtCheck.addMonths(1);

                if (dtCheck.before(dtFrom)) dtCheck = new OADateTime(dtFrom);

                dtFound = findNextMonthDay(dtFound, dtTo, dtCheck);
            }
            if (dtFound != null) break;
        }
        if (dtFound != null) {
            dtFound.clearSecondAndMilliSecond();            
        }
        return dtFound;
    }
    
    /**
     * Determines the next matching day-of-month within the supplied range.
     * Handles explicit day values as well as optional "last day" logic,
     * delegating to weekday matching via {@link #findClosestDayOfWeek}.
     *
     * @param dtFound current best matching date/time or null
     * @param dtTo    upper boundary for evaluation (exclusive)
     * @param dtCheck candidate date within the month
     * @return updated best matching date/time
     */
    private OADateTime findNextMonthDay(OADateTime dtFound, OADateTime dtTo, OADateTime dtCheck) {
        if (!bIncludeLastDayOfMonth && (monthDays == null || monthDays.length == 0)) {
            dtFound = findClosestDayOfWeek(dtFound, dtTo, dtCheck);
        }
        else {
            OADateTime dtx = new OADateTime(dtFrom);
            dtx.clearTime();

            int max = dtCheck.getDaysInMonth();
            for (int i=0; i<=monthDays.length; i++) {
                int md;
                if (i == monthDays.length) {
                    if (!bIncludeLastDayOfMonth) continue;
                    md = max;
                }
                else md = monthDays[i];

                if (md > max) continue;

                dtCheck.setDay(md);
                if (dtCheck.before(dtx)) continue;
                if (dtFound != null && dtFound.before(dtCheck)) continue;

                OADateTime dtTo2 = dtCheck.addDays(1);
                dtTo2.clearTime();

                dtFound = findClosestDayOfWeek(dtFound, dtTo2, dtCheck);
            }
        }
        return dtFound;
    }

    // Java Sunday=1, cron Sunday=0
    /**
     * Determines the next matching day-of-month within the supplied range.
     * Handles explicit day values as well as optional "last day" logic,
     * delegating to weekday matching via {@link #findClosestDayOfWeek}.
     *
     * @param dtFound current best matching date/time or null
     * @param dtTo    upper boundary for evaluation (exclusive)
     * @param dtCheck candidate date within the month
     * @return updated best matching date/time
     */
    private OADateTime findClosestDayOfWeek(OADateTime dtFound, OADateTime dtTo, OADateTime dtCheck) {
        if (daysOfWeek == null || daysOfWeek.length == 0) { // any dayOfWeek
            for (;;) {
                dtFound = findClosestHour(dtFound, dtTo, dtCheck);
                if (dtFound != null) break;
                dtCheck = dtCheck.addDays(1);
                dtCheck.clearTime();
                if (dtTo != null && dtTo.compareTo(dtCheck) <= 0) break;
            }
        }
        else {
            int fromDayOfWeek = dtCheck.getDayOfWeek();
            OADateTime dtHold = dtCheck;

            int fromWd = dtHold.getDayOfWeek();
            for (int i=0; i<2; i++) {
                for (int wd : daysOfWeek) {
                    wd++;  // cron day is 0 based, java 1 based

                    int diff;
                    if (i > 0) {
                        if (wd != fromWd) continue;
                        diff = 7;
                    }
                    else {
                        if (fromWd > wd) diff = (wd+7) - fromWd;
                        else diff = wd - fromWd;
                    }

                    if (diff != 0) dtCheck = dtHold.addDays(diff);
                    dtCheck.clearTime();

                    if (dtTo != null && dtTo.compareTo(dtCheck) <= 0) continue;

                    if (dtFound != null && dtFound.before(dtCheck)) continue;

                    dtFound = findClosestHour(dtFound, dtTo, dtCheck);
                }
                if (dtFound != null) break;
            }
        }
        return dtFound;
    }

    /**
     * Determines the next valid hour match for the candidate date. When no
     * specific hours are defined, checks the current hour then advances as needed.
     * Delegates minute evaluation to {@link #findClosestMinute}.
     *
     * @param dtFound current best match, or null
     * @param dtTo    upper boundary for evaluation
     * @param dtCheck candidate date/time with hour set
     * @return updated best matching date/time
     */
    private OADateTime findClosestHour(OADateTime dtFound, OADateTime dtTo, OADateTime dtCheck) {
        if (hrs == null || hrs.length == 0) { // any hour
            dtFound = findClosestMinute(dtFound, dtTo, dtCheck);
            if (dtFound == null) {
                dtCheck = dtCheck.addHours(1);
                if (dtTo == null || dtTo.compareTo(dtCheck) > 0) {
                    dtFound = findClosestMinute(dtFound, dtTo, dtCheck);
                }
            }
        }
        else {
            dtCheck.setMinute(0);
            dtCheck.clearSecondAndMilliSecond();            
            OADateTime dtx = new OADateTime(dtFrom);
            dtx.clearTime();
            dtx.set24Hour(dtFrom.get24Hour());

            for (int hr : hrs) {
                dtCheck.set24Hour(hr);
                if (dtCheck.before(dtx)) continue;
                if (dtFound != null && dtFound.before(dtCheck)) continue;
                dtFound = findClosestMinute(dtFound, dtTo, dtCheck);
            }
        }
        return dtFound;
    }
    
    /**
     * Determines the next valid hour match for the candidate date. When no
     * specific hours are defined, checks the current hour then advances as needed.
     * Delegates minute evaluation to {@link #findClosestMinute}.
     *
     * @param dtFound current best match, or null
     * @param dtTo    upper boundary for evaluation
     * @param dtCheck candidate date/time with hour set
     * @return updated best matching date/time
     */
    private OADateTime findClosestMinute(OADateTime dtFound, OADateTime dtTo, OADateTime dtCheck) {
        if (mins == null || mins.length == 0) {
            if (dtFound == null || dtCheck.before(dtFound)) {
                dtFound = new OADateTime(dtCheck);
                if (dtFound.equals(dtFrom)) dtFound = dtFound.addMinutes(1);
            }
            return dtFound;
        }
        else {
            for (int m : mins) {
                dtCheck.setMinute(m);
                if (dtCheck.compareTo(dtFrom) <= 0) continue;
                if (dtFound != null && dtFound.before(dtCheck)) continue;
                if (dtCheck.before(dtFrom)) continue;
                if (dtFound == null || dtCheck.before(dtFound)) {
                    dtFound = new OADateTime(dtCheck);
                }
            }
        }
        return dtFound;
    }


    /**
     * Parses a cron-style field string into sorted integer values without
     * supporting the "last" keyword.
     *
     * @param line raw field definition
     * @return parsed values, or an empty array when "*" is used
     */
    private int[] getInts(String line) {
        return getInts(line, false);
    }

    /**
     * Parses a cron-style field definition into sorted integer values.
     * Supports numbers, ranges, lists, and optionally the keyword "last".
     * Updates internal validity and flags as needed during parsing.
     *
     * @param line        raw field definition to parse
     * @param bAllowLast  true to allow the "last" keyword for day-of-month
     * @return sorted array of parsed values, or empty array for wildcard
     */
    private int[] getInts(String line, boolean bAllowLast) {
        if (OAString.isEmpty(line)) return new int[0];
        if (line.indexOf('*') >= 0) return new int[0]; // all

        line = line.replace(',', ' ');
        line = line.replace(':', ' ');

        int[] ints = new int[0];
        for (String s : line.split("\\s+")) {
            String[] ss = s.split("\\-");
            if (ss == null || ss.length == 0) continue;
            s = ss[0];
            if (!OAString.isInteger(s)) {
                if (bAllowLast && s.equalsIgnoreCase("last")) {
                    bIncludeLastDayOfMonth = true;
                }
                else bValid = false;
                continue;
            }
            int x = OAConv.toInt(s);

            if (ss.length == 2) {
                if (!OAString.isInteger(ss[1])) {
                    bValid = false;
                    if (!OAArray.contains(ints, x)) {
                        ints = OAArray.add(ints, x);
                    }
                    continue;
                }
                int x2 = OAConv.toInt(ss[1]);
                for (int i=x; i<=x2; i++) {
                    if (!OAArray.contains(ints, i)) {
                        ints = OAArray.add(ints, i);
                    }
                }
            }
            else {
                ints = OAArray.add(ints, x);
            }
        }

        Arrays.sort(ints);
        return ints;
    }

    /**
     * Sets the optional name for this cron entry.
     *
     * @param name descriptive label for this cron
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the name assigned to this cron entry.
     *
     * @return name of this cron, or null if none assigned
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the validity state of this cron entry. Equivalent to
     * {@link #isValid()}.
     *
     * @return true if the cron definition is valid
     */
    public boolean getIsValid() {
        return bValid;
    }

    /**
     * Returns the validity state of this cron entry. Equivalent to
     * {@link #isValid()}.
     *
     * @return true if the cron definition is valid
     */
    public void setEnabled(boolean b) {
        this.bEnabled = b;
    }

    /**
     * Indicates whether this cron entry is currently enabled.
     *
     * @return true if enabled
     */
    public boolean getEnabled() {
        return bEnabled;
    }

    /**
     * Returns the timestamp indicating when this cron entry was created.
     *
     * @return creation timestamp
     */
    public OADateTime getCreated() {
        return dtCreated;
    }
}
