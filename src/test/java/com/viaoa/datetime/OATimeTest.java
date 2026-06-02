package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Time;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;

/**
 * Internal tests for OATime.
 *
 * Strategy:
 * - One test method per public production method name.
 * - Overloads are tested inside the same methodNameTest().
 * - Comments explain what each assertion is checking.
 * - Tests characterize current time-only behavior using simple ASCII values.
 */
public class OATimeTest {

    @Test
    public void constructorTest() {
        // default constructor creates a usable time-only value
        assertNotNull(new OATime());

        // SQL Time constructor keeps clock fields
        OATime t1 = new OATime(Time.valueOf("13:02:03"));
        assertEquals(13, t1.get24Hour());
        assertEquals(2, t1.getMinute());
        assertEquals(3, t1.getSecond());

        // Date constructor creates a usable time-only value
        assertNotNull(new OATime(new Date(0)));

        // long constructor creates a usable time-only value
        assertNotNull(new OATime(0L));

        // Calendar constructor copies clock fields
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MAY, 18, 14, 15, 16);
        cal.set(Calendar.MILLISECOND, 17);
        OATime t2 = new OATime(cal);
        assertEquals(14, t2.get24Hour());
        assertEquals(15, t2.getMinute());
        assertEquals(16, t2.getSecond());

        // OADateTime constructor copies clock fields
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 10, 20, 30, 40);
        OATime t3 = new OATime(dt);
        assertEquals(10, t3.get24Hour());
        assertEquals(20, t3.getMinute());
        assertEquals(30, t3.getSecond());

        // string constructor parses explicit format
        OATime t4 = new OATime("13:02:03", OATime.Format5);
        assertEquals(13, t4.get24Hour());
        assertEquals(2, t4.getMinute());
        assertEquals(3, t4.getSecond());

        // LocalTime constructor preserves clock fields
        OATime t5 = new OATime(LocalTime.of(9, 8, 7, 6_000_000));
        assertEquals(9, t5.get24Hour());
        assertEquals(8, t5.getMinute());
        assertEquals(7, t5.getSecond());
        assertEquals(6, t5.getMilliSecond());

        // field constructor preserves clock fields
        OATime t6 = new OATime(1, 2, 3, 4);
        assertEquals(1, t6.get24Hour());
        assertEquals(2, t6.getMinute());
        assertEquals(3, t6.getSecond());
        assertEquals(4, t6.getMilliSecond());
    }

    @Test
    public void compareTest() {
        OATime early = new OATime(9, 0, 0);
        OATime late = new OATime(10, 0, 0);

        // same time compares equal
        assertEquals(0, early.compare(new OATime(9, 0, 0)));

        // earlier time compares before later time
        assertTrue(early.compare(late) < 0);

        // later time compares after earlier time
        assertTrue(late.compare(early) > 0);
    }

    @Test
    public void toStringTest() {
        OATime t = new OATime(13, 2, 3, 4);

        // explicit 24-hour format
        assertEquals("13:02:03", t.toString(OATime.Format5));

        // explicit 24-hour format with milliseconds
        assertEquals("13:02:03.004", t.toString(OATime.Format6));

        // default toString returns non-empty text
        assertNotNull(t.toString());
        assertFalse(t.toString().isEmpty());
    }

    @Test
    public void timeValueTest() {
        // explicit format parses time
        OATime t1 = OATime.timeValue("13:02:03", OATime.Format5);
        assertNotNull(t1);
        assertEquals(13, t1.get24Hour());

        // default parse handles 24-hour time
        OATime t2 = OATime.timeValue("13:02:03");
        assertNotNull(t2);
        assertEquals(13, t2.get24Hour());

        // null input returns null
        assertNull(OATime.timeValue(null));
    }

    @Test
    public void valueOfTest() {
        // explicit format parses time
        OATime t1 = (OATime) OATime.valueOf("13:02:03", OATime.Format5);
        assertNotNull(t1);
        assertEquals(13, t1.get24Hour());

        // default parse handles 24-hour time
        OATime t2 = (OATime) OATime.valueOf("13:02:03");
        assertNotNull(t2);
        assertEquals(13, t2.get24Hour());

        // null input returns null
        assertNull(OATime.valueOf(null));
    }

    @Test
    public void setGlobalOutputFormatTest() {
        // set explicit global output format
        OATime.setGlobalOutputFormat(OATime.Format5);
        assertEquals(OATime.Format5, OATime.getGlobalOutputFormat());

        // cleanup
        OATime.setGlobalOutputFormat(OATime.Format1);
    }

    @Test
    public void getGlobalOutputFormatTest() {
        // current global output format is readable after setting
        OATime.setGlobalOutputFormat(OATime.Format4);
        assertEquals(OATime.Format4, OATime.getGlobalOutputFormat());

        // cleanup
        OATime.setGlobalOutputFormat(OATime.Format1);
    }

    @Test
    public void addGlobalParseFormatTest() {
        // custom parse format can be added and used
        OATime.addGlobalParseFormat("HH.mm.ss");
        OATime t = OATime.timeValue("13.02.03");
        assertNotNull(t);
        assertEquals(13, t.get24Hour());

        // cleanup
        OATime.removeGlobalParseFormat("HH.mm.ss");
    }

    @Test
    public void removeGlobalParseFormatTest() {
        // removing a known custom format should not throw
        OATime.addGlobalParseFormat("HH.mm.ss");
        assertDoesNotThrow(() -> OATime.removeGlobalParseFormat("HH.mm.ss"));
    }

    @Test
    public void removeAllGlobalParseFormatsTest() {
        // remove all custom parse formats should not prevent explicit-format parsing
        assertNotNull(OATime.valueOf("13:02:03", OATime.Format5));
    }

    @Test
    public void getLocalTimeTest() {
        // converts to java.time LocalTime
        OATime t = new OATime(13, 2, 3, 4);
        assertEquals(LocalTime.of(13, 2, 3, 4_000_000), t.getLocalTime());
    }
}
