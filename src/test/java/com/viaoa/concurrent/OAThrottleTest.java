package com.viaoa.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAThrottleTest {

    @Test
    void constructorAndSetDelayStoreDelay() {
        OAThrottle throttle = new OAThrottle(25);
        assertEquals(25, throttle.getDelay());

        throttle.setDelay(50);

        assertEquals(50, throttle.getDelay());
    }

    @Test
    void checkAllowsFirstCallAndThrottlesImmediateSecondCall() {
        OAThrottle throttle = new OAThrottle(10_000);

        assertTrue(throttle.check());
        assertFalse(throttle.check());
        assertEquals(2, throttle.getCheckCount());
        assertEquals(2, throttle.getCount());
        assertTrue(throttle.getLastThrottle() > 0);
    }

    @Test
    void zeroDelayAllowsRepeatedChecks() {
        OAThrottle throttle = new OAThrottle(0);

        assertTrue(throttle.check());
        assertTrue(throttle.check());
        assertEquals(2, throttle.getCount());
    }

    @Test
    void nowReturnsCurrentTimeRange() {
        OAThrottle throttle = new OAThrottle(1);
        long before = System.currentTimeMillis();

        long now = throttle.now();

        long after = System.currentTimeMillis();
        assertTrue(now >= before);
        assertTrue(now <= after);
    }

    @Test
    void resetClearsCountersAndAllowsNextCheck() {
        OAThrottle throttle = new OAThrottle(10_000);
        throttle.check();
        throttle.check();

        throttle.reset();

        assertEquals(0, throttle.getCheckCount());
        assertEquals(0, throttle.getCount());
        assertEquals(0, throttle.getLastThrottle());
        assertTrue(throttle.check());
        assertEquals(1, throttle.getCount());
    }

    @Test
    void getLastThrottleTracksLastSuccessfulCheckTime() {
        OAThrottle throttle = new OAThrottle(10_000);
        long before = System.currentTimeMillis();

        assertTrue(throttle.check());

        long last = throttle.getLastThrottle();
        assertTrue(last >= before);
        assertTrue(last <= System.currentTimeMillis());
    }
}
