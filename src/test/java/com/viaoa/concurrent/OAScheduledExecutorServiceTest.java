package com.viaoa.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.viaoa.datetime.OADateTime;
import com.viaoa.datetime.OATime;

class OAScheduledExecutorServiceTest {

    @Test
    void constructorInitializesScheduledExecutor() {
        OAScheduledExecutorService service = new OAScheduledExecutorService();
        try {
            assertNotNull(service.getScheduledExecutorService());
        }
        finally {
            service.getScheduledExecutorService().shutdownNow();
        }
    }

    @Test
    void scheduleRunnableWithDateTimeRunsImmediatelyForNullOrPastDate() throws Exception {
        OAScheduledExecutorService service = new OAScheduledExecutorService();
        CountDownLatch nullDate = new CountDownLatch(1);
        CountDownLatch pastDate = new CountDownLatch(1);
        try {
            ScheduledFuture<?> f1 = service.schedule(nullDate::countDown, (OADateTime) null);
            ScheduledFuture<?> f2 = service.schedule(pastDate::countDown, new OADateTime(0L));

            assertTrue(nullDate.await(1, TimeUnit.SECONDS));
            assertTrue(pastDate.await(1, TimeUnit.SECONDS));
            f1.get(1, TimeUnit.SECONDS);
            f2.get(1, TimeUnit.SECONDS);
        }
        finally {
            service.getScheduledExecutorService().shutdownNow();
        }
    }

    @Test
    void scheduleRunnableWithDelayRunsAfterBoundedDelay() throws Exception {
        OAScheduledExecutorService service = new OAScheduledExecutorService();
        CountDownLatch latch = new CountDownLatch(1);
        try {
            ScheduledFuture<?> future = service.schedule(latch::countDown, 0, TimeUnit.MILLISECONDS);

            assertTrue(latch.await(1, TimeUnit.SECONDS));
            future.get(1, TimeUnit.SECONDS);
        }
        finally {
            service.getScheduledExecutorService().shutdownNow();
        }
    }

    @Test
    void scheduleCallableReturnsValue() throws Exception {
        OAScheduledExecutorService service = new OAScheduledExecutorService();
        try {
            ScheduledFuture<?> future = service.schedule(() -> "value", 0, TimeUnit.MILLISECONDS);

            assertEquals("value", future.get(1, TimeUnit.SECONDS));
        }
        finally {
            service.getScheduledExecutorService().shutdownNow();
        }
    }

    @Test
    void scheduleEveryWithOATimeReturnsCancelableDailyFuture() throws Exception {
        OAScheduledExecutorService service = new OAScheduledExecutorService();
        try {
            ScheduledFuture<?> future = service.scheduleEvery(() -> {
            }, new OATime(23, 59, 59));

            assertFalse(future.isDone());
            assertTrue(future.cancel(false));
        }
        finally {
            service.getScheduledExecutorService().shutdownNow();
        }
    }

    @Test
    void scheduleEveryWithFixedDelayRunsRepeatedlyAndCanBeCancelled() throws Exception {
        OAScheduledExecutorService service = new OAScheduledExecutorService();
        CountDownLatch latch = new CountDownLatch(2);
        try {
            ScheduledFuture<?> future = service.scheduleEvery(latch::countDown, 0, 1, TimeUnit.MILLISECONDS);

            assertTrue(latch.await(1, TimeUnit.SECONDS));
            assertTrue(future.cancel(false));
        }
        finally {
            service.getScheduledExecutorService().shutdownNow();
        }
    }

    @Test
    void getScheduledExecutorServiceCreatesDaemonNamedThread() throws Exception {
        OAScheduledExecutorService service = new OAScheduledExecutorService();
        AtomicReference<Thread> thread = new AtomicReference<>();
        try {
            service.schedule(() -> thread.set(Thread.currentThread()), 0, TimeUnit.MILLISECONDS).get(1, TimeUnit.SECONDS);

            assertNotNull(thread.get());
            assertTrue(thread.get().isDaemon());
            assertTrue(thread.get().getName().startsWith("OAScheduledExecutorService.thread"));
        }
        finally {
            service.getScheduledExecutorService().shutdownNow();
        }
    }

    @Test
    void getScheduledExecutorServiceReturnsSameInstance() {
        OAScheduledExecutorService service = new OAScheduledExecutorService();
        try {
            ScheduledExecutorService executor = service.getScheduledExecutorService();
            assertSame(executor, service.getScheduledExecutorService());
        }
        finally {
            service.getScheduledExecutorService().shutdownNow();
        }
    }
}
