package com.viaoa.schedule;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Store;
import com.viaoa.datetime.OADateTime;

class OASchedulerTest {

    @Test
    void constructorStoresSearchObjectAndWindow() {
        Store store = new Store();
        OADateTime begin = dt(9);
        OADateTime end = dt(17);
        OAScheduler<Store> scheduler = new OAScheduler<>(store, begin, end);

        assertSame(store, scheduler.getSearchObject());
        assertSame(begin, scheduler.getBegin());
        assertSame(end, scheduler.getEnd());
    }

    @Test
    void addIgnoresNullAndStoresPlans() {
        OAScheduler<Store> scheduler = new OAScheduler<>(new Store(), dt(9), dt(17));
        OASchedulerPlan<Store> plan = new OASchedulerPlan<>(dt(9), dt(17));

        scheduler.add(null);
        scheduler.add(plan);

        assertEquals(1, scheduler.getSchedulePlans().size());
        assertSame(plan, scheduler.getSchedulePlans().get(0));
    }

    @Test
    void calculateIsCurrentlyNoOp() {
        OAScheduler<Store> scheduler = new OAScheduler<>(new Store(), dt(9), dt(17));

        assertDoesNotThrow(scheduler::calculate);
        assertTrue(scheduler.getSchedulePlans().isEmpty());
    }

    @Test
    void getSchedulePlansReturnsStableMutableList() {
        OAScheduler<Store> scheduler = new OAScheduler<>(new Store(), dt(9), dt(17));

        assertSame(scheduler.getSchedulePlans(), scheduler.getSchedulePlans());
    }

    @Test
    void isAvailableReturnsTrueWithNoPlansAndRequiresAllPlansToAllow() {
        OAScheduler<Store> scheduler = new OAScheduler<>(new Store(), dt(9), dt(17));

        assertTrue(scheduler.isAvailable(dt(8)), "Current implementation does not enforce scheduler-level bounds.");
        scheduler.add(new FixedPlan(true));
        assertTrue(scheduler.isAvailable(dt(10)));
        scheduler.add(new FixedPlan(false));
        assertFalse(scheduler.isAvailable(dt(10)));
    }

    private static OADateTime dt(int hour) {
        return new OADateTime(2026, 6, 9, hour, 0, 0, 0);
    }

    private static class FixedPlan extends OASchedulerPlan<Store> {
        private final boolean available;

        FixedPlan(boolean available) {
            super(dt(9), dt(17));
            this.available = available;
        }

        @Override
        public boolean isAvailable(OADateTime dt) {
            return available;
        }
    }
}
