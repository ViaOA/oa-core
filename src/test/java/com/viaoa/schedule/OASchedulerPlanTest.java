package com.viaoa.schedule;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;

class OASchedulerPlanTest {

    @Test
    void defaultConstructorCreatesOneDayDateWindow() {
        OASchedulerPlan<String> plan = new OASchedulerPlan<>();

        assertNotNull(plan.getBegin());
        assertNotNull(plan.getEnd());
        assertEquals(1, plan.getBegin().betweenDays(plan.getEnd()));
    }

    @Test
    void dateConstructorUsesDateThroughNextDayWindowAndNullDefaultsToToday() {
        OADate date = new OADate(2026, 6, 9);
        OASchedulerPlan<String> plan = new OASchedulerPlan<>(date);
        OASchedulerPlan<String> nullPlan = new OASchedulerPlan<>((OADate) null);

        assertEquals(new OADateTime(date), plan.getBegin());
        assertEquals(new OADateTime(date.plusDay()), plan.getEnd());
        assertNotNull(nullPlan.getBegin());
        assertNotNull(nullPlan.getEnd());
    }

    @Test
    void dateTimeConstructorUsesSuppliedBeginAndNextMidnightEnd() {
        OADateTime begin = dt(13);
        OASchedulerPlan<String> plan = new OASchedulerPlan<>(begin);

        assertEquals(begin, plan.getBegin());
        assertEquals(new OADateTime(new OADate(begin).plusDay()), plan.getEnd());
    }

    @Test
    void explicitConstructorCopiesBeginAndEnd() {
        OADateTime begin = dt(9);
        OADateTime end = dt(17);
        OASchedulerPlan<String> plan = new OASchedulerPlan<>(begin, end);

        assertEquals(begin, plan.getBegin());
        assertEquals(end, plan.getEnd());
        assertNotSame(begin, plan.getBegin());
        assertNotSame(end, plan.getEnd());
    }

    @Test
    void scheduleGettersLazilyReturnStableScheduleInstances() {
        OASchedulerPlan<String> plan = new OASchedulerPlan<>(dt(9), dt(17));

        assertSame(plan.getOpenSchedule(), plan.getOpenSchedule());
        assertSame(plan.getOpenSoftSchedule(), plan.getOpenSoftSchedule());
        assertSame(plan.getPreferredSchedule(), plan.getPreferredSchedule());
        assertSame(plan.getPreferredSoftSchedule(), plan.getPreferredSoftSchedule());
        assertSame(plan.getBlockedSchedule(), plan.getBlockedSchedule());
        assertSame(plan.getBlockedSoftSchedule(), plan.getBlockedSoftSchedule());
        assertSame(plan.getScheduledSchedule(), plan.getScheduledSchedule());
    }

    @Test
    void isAvailableRequiresOpenOrOpenSoftRangeInsidePlanWindow() {
        OASchedulerPlan<String> plan = new OASchedulerPlan<>(dt(9), dt(17));

        assertFalse(plan.isAvailable(null));
        assertFalse(plan.isAvailable(dt(8)));
        assertFalse(plan.isAvailable(dt(10)));

        plan.getOpenSchedule().add(dt(9), dt(12));

        assertTrue(plan.isAvailable(dt(10)));
        assertFalse(plan.isAvailable(dt(18)));
    }

    @Test
    void isAvailableUsesOpenSoftWhenHardOpenDoesNotMatch() {
        OASchedulerPlan<String> plan = new OASchedulerPlan<>(dt(9), dt(17));

        plan.getOpenSoftSchedule().add(dt(13), dt(15));

        assertTrue(plan.isAvailable(dt(14)));
    }

    @Test
    void isAvailableRejectsBlockedSoftBlockedAndScheduledRanges() {
        OASchedulerPlan<String> blocked = new OASchedulerPlan<>(dt(9), dt(17));
        blocked.getOpenSchedule().add(dt(9), dt(17));
        blocked.getBlockedSchedule().add(dt(10), dt(11));
        assertFalse(blocked.isAvailable(dt(10)));

        OASchedulerPlan<String> softBlocked = new OASchedulerPlan<>(dt(9), dt(17));
        softBlocked.getOpenSchedule().add(dt(9), dt(17));
        softBlocked.getBlockedSoftSchedule().add(dt(12), dt(13));
        assertFalse(softBlocked.isAvailable(dt(12)));

        OASchedulerPlan<String> scheduled = new OASchedulerPlan<>(dt(9), dt(17));
        scheduled.getOpenSchedule().add(dt(9), dt(17));
        scheduled.getScheduledSchedule().add(dt(14), dt(15));
        assertFalse(scheduled.isAvailable(dt(14)));
    }

    private static OADateTime dt(int hour) {
        return new OADateTime(2026, 6, 9, hour, 0, 0, 0);
    }
}
