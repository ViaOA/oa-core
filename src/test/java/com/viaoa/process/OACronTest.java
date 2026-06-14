package com.viaoa.process;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.viaoa.datetime.OADateTime;

class OACronTest {

    @Test
    void constructorParsesCronFieldsAndDefaultEnabledState() {
        TestCron cron = new TestCron("5,10-12", "8-9", "1,last", "1,12", "0,6");

        assertArrayEquals(new int[] { 5, 10, 11, 12 }, cron.getMinutes());
        assertArrayEquals(new int[] { 8, 9 }, cron.getHours());
        assertArrayEquals(new int[] { 1 }, cron.getMonthDays());
        assertArrayEquals(new int[] { 0, 6 }, cron.getDaysOfWeek());
        assertArrayEquals(new int[] { 1, 12 }, cron.getMonths());
        assertTrue(cron.getIncludeLastDayOfMonth());
        assertTrue(cron.isValid());
        assertTrue(cron.getIsValid());
        assertTrue(cron.getEnabled());
        assertNotNull(cron.getCreated());
    }

    @Test
    void wildcardFieldsRepresentAnyAllowedValue() {
        TestCron cron = new TestCron("*", "*", "*", "*", "*");

        assertArrayEquals(new int[0], cron.getMinutes());
        assertArrayEquals(new int[0], cron.getHours());
        assertArrayEquals(new int[0], cron.getMonthDays());
        assertArrayEquals(new int[0], cron.getDaysOfWeek());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 }, cron.getMonths());
        assertFalse(cron.getIncludeLastDayOfMonth());
        assertTrue(cron.isValid());
    }

    @Test
    void invalidFieldMarksCronInvalidAndFindNextReturnsNull() {
        TestCron cron = new TestCron("60", "24", "32", "13", "7");

        assertFalse(cron.isValid());
        assertFalse(cron.getIsValid());
        assertNull(cron.findNext(new OADateTime(2026, 6, 9, 10, 0, 0, 0)));
    }

    @Test
    void descriptionIsCachedAndIncludesSafeConfiguredFields() {
        TestCron cron = new TestCron("15", "3", "*", "*", "*");

        String description = cron.getDescription();

        assertSame(description, cron.getDescription());
        assertTrue(description.contains("minute is 15"));
        assertTrue(description.contains("hour is 3"));
    }

    @Test
    void lastNameAndEnabledRoundTrip() {
        TestCron cron = new TestCron("*", "*", "*", "*", "*");
        OADateTime last = new OADateTime(2026, 6, 9, 10, 30, 0, 0);

        cron.setName("nightly");
        cron.setLast(last);
        cron.setEnabled(false);

        assertEquals("nightly", cron.getName());
        assertSame(last, cron.getLast());
        assertFalse(cron.getEnabled());
    }

    @Test
    void findNextReturnsNormalizedCandidateForAnyMinuteSchedule() {
        TestCron cron = new TestCron("*", "*", "*", "*", "*");
        OADateTime from = new OADateTime(2026, 6, 9, 10, 30, 0, 0);

        OADateTime next = cron.findNext(from);

        assertNotNull(next);
        assertEquals(0, next.getSecond());
        assertEquals(0, next.getMilliSecond());
    }

    @Test
    void getNextDelegatesToFindNextAndAcceptsNullStart() {
        TestCron cron = new TestCron("*", "*", "*", "*", "*");

        assertNotNull(cron.getNext());
        assertNotNull(cron.getNext(null));
        assertNotNull(cron.findNext());
        assertNotNull(cron.findNext(null));
    }

    @Test
    void processCallbackReceivesManualFlag() {
        TestCron cron = new TestCron("*", "*", "*", "*", "*");

        cron.process(true);
        assertTrue(cron.lastManualFlag);

        cron.process(false);
        assertFalse(cron.lastManualFlag);
        assertEquals(2, cron.processCount);
    }

    private static class TestCron extends OACron {
        int processCount;
        boolean lastManualFlag;

        TestCron(String mins, String hours, String monthDays, String months, String daysOfWeek) {
            super(mins, hours, monthDays, months, daysOfWeek);
        }

        @Override
        public void process(boolean bManuallyCalled) {
            processCount++;
            lastManualFlag = bManuallyCalled;
        }
    }
}
