package com.viaoa.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class OAConcurrentTest {

    @Test
    void constructorAcceptsNullAndRunReturnsImmediately() throws Exception {
        OAConcurrent concurrent = new OAConcurrent(null);

        concurrent.run();
    }

    @Test
    void runReturnsImmediatelyForEmptyRunnableArray() throws Exception {
        OAConcurrent concurrent = new OAConcurrent(new Runnable[0]);

        concurrent.run();
    }

    @Test
    void runExecutesAllRunnablesBeforeReturning() throws Exception {
        AtomicInteger count = new AtomicInteger();
        OAConcurrent concurrent = new OAConcurrent(new Runnable[] {
                count::incrementAndGet,
                count::incrementAndGet,
                count::incrementAndGet
        });

        concurrent.run();

        assertEquals(3, count.get());
    }

    @Test
    void runStartsTasksTogetherUsingBarrier() throws Exception {
        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger completed = new AtomicInteger();
        Runnable task = () -> {
            entered.countDown();
            try {
                assertTrue(release.await(1, TimeUnit.SECONDS));
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(e);
            }
            completed.incrementAndGet();
        };

        Thread runner = new Thread(() -> {
            try {
                new OAConcurrent(new Runnable[] { task, task }).run();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        runner.start();
        assertTrue(entered.await(1, TimeUnit.SECONDS));
        assertEquals(0, completed.get());

        release.countDown();
        runner.join(1_000);

        assertFalse(runner.isAlive());
        assertEquals(2, completed.get());
    }

    @Test
    void workerExceptionIsLoggedAndDoesNotPreventOtherTasks() throws Exception {
        AtomicInteger count = new AtomicInteger();
        OAConcurrent concurrent = new OAConcurrent(new Runnable[] {
                () -> {
                    throw new RuntimeException("boom");
                },
                count::incrementAndGet
        });

        concurrent.run();

        assertEquals(1, count.get());
    }
}
