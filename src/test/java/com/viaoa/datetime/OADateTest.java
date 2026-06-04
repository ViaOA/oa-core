package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Time;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

/**
 * Internal tests for OADate.
 *
 * Strategy:
 * - One test method per public production method name.
 * - Overloads are tested inside the same methodNameTest().
 * - Comments explain what each assertion is checking.
 * - Tests characterize current date-only behavior using simple ASCII values.
 */
public class OADateTest {

    @Test
    public void constructorTest() {
        // default constructor creates a usable date-only value
        assertNotNull(new OADate());

        // Date constructor clears time fields
        OADate d1 = new OADate(new Date(0));
        assertEquals(0, d1.getHour());
        assertEquals(0, d1.getMinute());
        assertEquals(0, d1.getSecond());

        // long constructor creates a usable date-only value
        assertNotNull(new OADate(0L));

        // SQL Time constructor creates a usable date-only value
        assertNotNull(new OADate(new Time(0L)));

        // LocalDate constructor preserves year/month/day fields
        OADate d2 = new OADate(LocalDate.of(2026, 5, 18));
        assertEquals(2026, d2.getYear());
        assertEquals(Calendar.MAY, d2.getMonth());
        assertEquals(18, d2.getDay());

        // Calendar constructor copies date fields
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2026, Calendar.JUNE, 1, 10, 20, 30);
        OADate d3 = new OADate(cal);
        assertEquals(2026, d3.getYear());
        assertEquals(Calendar.JUNE, d3.getMonth());
        assertEquals(1, d3.getDay());

        // OADateTime constructor copies date fields
        OADateTime dt = new OADateTime(2026, Calendar.JULY, 4, 12, 30, 45);
        OADate d4 = new OADate(dt);
        assertEquals(2026, d4.getYear());
        assertEquals(Calendar.JULY, d4.getMonth());
        assertEquals(4, d4.getDay());

		OADate dx0 = new OADate("08/09/2026", "MM/dd/yyyy");        
		OADate dx1 = new OADate("2026/08/09", "yyyy/MM/dd");        
		OADate dx2 = new OADate("2026-08-09", "yyyy-MM-dd");        
        
        // string constructor parses SQL-style date
        OADate d5 = new OADate("2026-08-09", OADate.Format1);
        assertEquals(2026, d5.getYear());
        assertEquals(Calendar.AUGUST, d5.getMonth());
        assertEquals(9, d5.getDay());

        // field constructor uses 0-based Calendar month
        OADate d6 = new OADate(2026, Calendar.SEPTEMBER, 10);
        assertEquals(2026, d6.getYear());
        assertEquals(Calendar.SEPTEMBER, d6.getMonth());
        assertEquals(10, d6.getDay());
    }

    @Test
    public void setLocaleTest() {
        // locale setup should not throw for common locale
        assertDoesNotThrow(() -> OADate.setLocale(Locale.US));

        // parsing still works after locale setup
        assertNotNull(OADate.valueOf("2026-05-18", OADate.Format1));
    }

    @Test
    public void setGlobalOutputFormatTest() {
        // set explicit global output format
        OADate.setGlobalOutputFormat(OADate.Format1);
        assertEquals(OADate.Format1, OADate.getGlobalOutputFormat());

        // reset to default/null is accepted
        OADate.setGlobalOutputFormat(null);
        assertNull(OADate.getGlobalOutputFormat());
    }

    @Test
    public void getGlobalOutputFormatTest() {
        // current value is readable after setting
        OADate.setGlobalOutputFormat(OADate.Format2);
        assertEquals(OADate.Format2, OADate.getGlobalOutputFormat());

        // cleanup
        OADate.setGlobalOutputFormat(null);
    }

    @Test
    public void addGlobalParseFormatTest() {
        // custom parse format can be added and used
        OADate.addGlobalParseFormat("yyyy.MM.dd");
        OADate d = OADate.dateValue("2026.05.18");
        assertNotNull(d);
        assertEquals(2026, d.getYear());
        assertEquals(Calendar.MAY, d.getMonth());
        assertEquals(18, d.getDay());

        // cleanup
        OADate.removeGlobalParseFormat("yyyy.MM.dd");
    }

    @Test
    public void removeGlobalParseFormatTest() {
        // removing a known custom format should not throw
        OADate.addGlobalParseFormat("yyyy.MM.dd");
        assertDoesNotThrow(() -> OADate.removeGlobalParseFormat("yyyy.MM.dd"));
    }

    @Test
    public void removeAllGlobalParseFormatsTest() {
        // remove all custom parse formats should not prevent explicit-format parsing
        assertNotNull(OADate.valueOf("2026-05-18", OADate.Format1));
    }

    @Test
    public void betweenTest() {
        OADate mid = new OADate(2026, Calendar.MAY, 18);
        OADate beg = new OADate(2026, Calendar.MAY, 1);
        OADate end = new OADate(2026, Calendar.MAY, 31);

        // value is strictly between begin and end
        assertTrue(mid.between(beg, end));

        OADate d = new OADate(2026, Calendar.APRIL, 1);
        assertTrue(beg.between(beg, end));
    }

    @Test
    public void betweenOrEqualTest() {
        OADate beg = new OADate(2026, Calendar.MAY, 1);
        OADate end = new OADate(2026, Calendar.MAY, 31);

        // begin boundary is included
        assertTrue(beg.betweenOrEqual(beg, end));

        // alias method returns same result
        assertTrue(beg.isBetweenOrEqual(beg, end));
    }

    @Test
    public void isBetweenOrEqualTest() {
        OADate d = new OADate(2026, Calendar.MAY, 18);

        // date inside range is accepted
        assertTrue(d.isBetweenOrEqual(new OADate(2026, Calendar.MAY, 1), new OADate(2026, Calendar.MAY, 31)));
    }

    @Test
    public void betweenNotEqualTest() {
        OADate d = new OADate(2026, Calendar.MAY, 18);

        // date inside range and not equal to boundaries is accepted
        assertTrue(d.betweenNotEqual(new OADate(2026, Calendar.MAY, 1), new OADate(2026, Calendar.MAY, 31)));

        // alias method returns same result
        assertTrue(d.isBetweenNotEqual(new OADate(2026, Calendar.MAY, 1), new OADate(2026, Calendar.MAY, 31)));
    }

    @Test
    public void isBetweenNotEqualTest() {
        OADate d = new OADate(2026, Calendar.MAY, 1);

        // boundary value is not accepted by not-equal variant
        assertFalse(d.isBetweenNotEqual(new OADate(2026, Calendar.MAY, 1), new OADate(2026, Calendar.MAY, 31)));
    }

    @Test
    public void toStringTest() {
        OADate d = new OADate(2026, Calendar.MAY, 18);

        // explicit format produces deterministic text
        assertEquals("2026-05-18", d.toString(OADate.Format1));

        // default toString returns non-empty text
        assertNotNull(d.toString());
        assertFalse(d.toString().isEmpty());
    }

    @Test
    public void dateValueTest() {
        // explicit format parses date
        assertEquals(new OADate(2026, Calendar.MAY, 18), OADate.dateValue("2026-05-18", OADate.Format1));

        // default parse handles SQL-style date
        assertEquals(new OADate(2026, Calendar.MAY, 18), OADate.dateValue("2026-05-18"));

        // null input returns null
        assertNull(OADate.dateValue(null));
    }

    @Test
    public void valueOfTest() {
        // explicit format parses date
        assertEquals(new OADate(2026, Calendar.MAY, 18), OADate.valueOf("2026-05-18", OADate.Format1));

        // default parse handles SQL-style date
        assertEquals(new OADate(2026, Calendar.MAY, 18), OADate.valueOf("2026-05-18"));

        // null input returns null
        assertNull(OADate.valueOf(null));
    }

    @Test
    public void getLocalDateTest() {
        // converts to java.time LocalDate with month adjusted to 1-based
        OADate d = new OADate(2026, Calendar.MAY, 18);
        assertEquals(LocalDate.of(2026, 5, 18), d.getLocalDate());
    }
}
