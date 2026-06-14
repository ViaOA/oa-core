package com.viaoa.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class OAExecutorServiceTest {

    @Test
    void defaultConstructorCreatesCachedDaemonExecutorOnDemand() throws Exception {
        OAExecutorService service = new OAExecutorService();
        try {
            Future<?> future = service.submit(() -> {
            });

            future.get(1, TimeUnit.SECONDS);
            assertTrue(service.getExecutorService() instanceof ExecutorService);
            assertEquals(0, service.getQueueSize());
        }
        finally {
            service.getExecutorService().shutdownNow();
        }
    }

    @Test
    void namedConstructorUsesThreadNamePrefix() throws Exception {
        OAExecutorService service = new OAExecutorService("unit");
        AtomicReference<String> threadName = new AtomicReference<>();
        try {
            service.submitAndWait(() -> threadName.set(Thread.currentThread().getName()), 1, TimeUnit.SECONDS);

            assertTrue(threadName.get().startsWith("OAExecutorService.unit."));
        }
        finally {
            service.getExecutorService().shutdownNow();
        }
    }

    @Test
    void fixedSizeConstructorQueuesWorkWhenWorkerIsBusy() throws Exception {
        OAExecutorService service = new OAExecutorService(1, "fixed");
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            Future<?> first = service.submit(() -> {
                started.countDown();
                try {
                    release.await(1, TimeUnit.SECONDS);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            assertTrue(started.await(1, TimeUnit.SECONDS));

            Future<?> second = service.submit(() -> {
            });

            assertEquals(1, service.getQueueSize());
            release.countDown();
            first.get(1, TimeUnit.SECONDS);
            second.get(1, TimeUnit.SECONDS);
        }
        finally {
            release.countDown();
            service.getExecutorService().shutdownNow();
        }
    }

    @Test
    void submitRunnableRunsTaskAndReturnsFuture() throws Exception {
        OAExecutorService service = new OAExecutorService(1, "run");
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Future<?> future = service.submit(latch::countDown);

            assertTrue(latch.await(1, TimeUnit.SECONDS));
            assertNull(future.get(1, TimeUnit.SECONDS));
        }
        finally {
            service.getExecutorService().shutdownNow();
        }
    }

    @Test
    void submitAndWaitRunnableWaitsForCompletionAndPropagatesTimeout() throws Exception {
        OAExecutorService service = new OAExecutorService(1, "wait");
        CountDownLatch release = new CountDownLatch(1);
        try {
            Future<?> done = service.submitAndWait(() -> {
            }, 1, TimeUnit.SECONDS);
            assertTrue(done.isDone());

            assertThrows(TimeoutException.class, () -> service.submitAndWait(() -> {
                try {
                    release.await(1, TimeUnit.SECONDS);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, 1, TimeUnit.MILLISECONDS));
        }
        finally {
            release.countDown();
            service.getExecutorService().shutdownNow();
        }
    }

    @Test
    void submitCallableReturnsValue() throws Exception {
        OAExecutorService service = new OAExecutorService(1, "call");
        try {
            Future<?> future = service.submit(() -> "value");

            assertEquals("value", future.get(1, TimeUnit.SECONDS));
        }
        finally {
            service.getExecutorService().shutdownNow();
        }
    }

    @Test
    void submitAndWaitCallableWaitsForCompletionAndPropagatesException() throws Exception {
        OAExecutorService service = new OAExecutorService(1, "callWait");
        try {
            Future<?> future = service.submitAndWait(() -> "value", 1, TimeUnit.SECONDS);
            assertEquals("value", future.get());

            assertThrows(ExecutionException.class,
                    () -> service.submitAndWait(() -> {
                        throw new IllegalStateException("boom");
                    }, 1, TimeUnit.SECONDS));
        }
        finally {
            service.getExecutorService().shutdownNow();
        }
    }

    @Test
    void closeShutsDownExecutorAndLaterSubmitIsRejected() {
        OAExecutorService service = new OAExecutorService(1, "close");

        service.close();

        assertTrue(service.getExecutorService().isShutdown());
        assertThrows(RejectedExecutionException.class, () -> service.submit(() -> {
        }));
    }

    @Test
    void getExecutorServiceReturnsSameInstance() {
        OAExecutorService service = new OAExecutorService(1, "same");
        try {
            assertSame(service.getExecutorService(), service.getExecutorService());
        }
        finally {
            service.getExecutorService().shutdownNow();
        }
    }

    @Test
    void poolMetricsReflectActiveThreads() throws Exception {
        OAExecutorService service = new OAExecutorService(1, "metrics");
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            Future<?> future = service.submit(() -> {
                started.countDown();
                try {
                    release.await(1, TimeUnit.SECONDS);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            assertTrue(started.await(1, TimeUnit.SECONDS));
            assertEquals(1, service.getThreadPoolSize());
            assertEquals(1, service.getActiveThreads());
            release.countDown();
            future.get(1, TimeUnit.SECONDS);
        }
        finally {
            release.countDown();
            service.getExecutorService().shutdownNow();
        }
    }
}
