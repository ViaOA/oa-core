package com.viaoa.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;

class OAReflectConcurrentLookupTest {

    public static class Bean {
        private Child child = new Child("child");

        public Child getChild() {
            return child;
        }

        public String echo(String value) {
            return value;
        }
    }

    public static class Child {
        private String name;

        public Child(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Test
    void concurrentMethodLookupIsStable() throws Exception {
        ExecutorService es = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Method>> calls = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                calls.add(() -> OAReflect.getMethod(Bean.class, "echo", new Object[] { "x" }));
            }

            List<Future<Method>> futures = es.invokeAll(calls);
            Method first = futures.get(0).get(5, TimeUnit.SECONDS);
            assertNotNull(first);

            for (Future<Method> f : futures) {
                assertEquals(first, f.get(5, TimeUnit.SECONDS));
            }
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void concurrentPropertyPathLookupAndEvaluationIsStable() throws Exception {
        ExecutorService es = Executors.newFixedThreadPool(4);
        try {
            List<Callable<String>> calls = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                calls.add(() -> {
                    Method[] ms = OAReflect.getMethods(Bean.class, "child.name");
                    return (String) OAReflect.getPropertyValue(new Bean(), ms);
                });
            }

            for (Future<String> f : es.invokeAll(calls)) {
                assertEquals("child", f.get(5, TimeUnit.SECONDS));
            }
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void returnedMethodArrayCanBeMutatedByCallerWithoutChangingFreshLookupContract() {
        Method[] ms1 = OAReflect.getMethods(Bean.class, "child.name");
        assertNotNull(ms1);
        ms1[0] = null;

        Method[] ms2 = OAReflect.getMethods(Bean.class, "child.name");

        assertNotNull(ms2);
        assertNotNull(ms2[0]);
        assertEquals("getChild", ms2[0].getName());
    }
}
