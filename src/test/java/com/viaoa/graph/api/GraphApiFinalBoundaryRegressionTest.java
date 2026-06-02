
package com.viaoa.graph.api;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.viaoa.trigger.OATrigger;

import org.junit.jupiter.api.Test;

class GraphApiFinalBoundaryRegressionTest {

    @Test
    void syncOpsPublicMethodSetIsStable() {
        Set<String> expected = Set.of("createServer", "createClient", "start", "stop", "isSingleUser", "isServer", "isClient", "isRunning");
        Set<String> actual = Arrays.stream(SyncOps.class.getDeclaredMethods()).map(Method::getName).collect(Collectors.toSet());
        assertEquals(expected, actual);
    }

    @Test
    void triggerOpsPublicMethodSetIsStable() {
        Set<String> actual = Arrays.stream(TriggerOps.class.getDeclaredMethods()).map(Method::getName).collect(Collectors.toSet());
        assertEquals(Set.of("addTrigger", "removeTrigger"), actual);
        assertEquals(3, TriggerOps.class.getDeclaredMethods().length);
    }

    @Test
    void replOpsPublicContractGapIsExplicit() {
        assertEquals(0, ReplOps.class.getDeclaredMethods().length);
    }

    @Test
    void syncOpsDoesNotExposeInternalTransportTypes() {
        for (Method m : SyncOps.class.getDeclaredMethods()) {
            assertFalse(m.getReturnType().getName().startsWith("com.viaoa.sync."));
            for (Class<?> p : m.getParameterTypes()) {
                assertFalse(p.getName().startsWith("com.viaoa.sync."));
            }
        }
    }

    @Test
    void publicApiDoesNotDependOnGraphServicePackages() {
        for (Class<?> c : new Class<?>[] { SyncOps.class, ReplOps.class, TriggerOps.class }) {
            for (Method m : c.getDeclaredMethods()) {
                assertFalse(m.getReturnType().getName().startsWith("com.viaoa.graph.service."));
                for (Class<?> p : m.getParameterTypes()) {
                    assertFalse(p.getName().startsWith("com.viaoa.graph.service."));
                }
            }
        }
    }

    @Test
    void triggerOpsOnlyExposesTriggerTypeAndPrimitiveSkipFlag() throws Exception {
        assertEquals(void.class, TriggerOps.class.getMethod("addTrigger", OATrigger.class).getReturnType());
        assertEquals(void.class, TriggerOps.class.getMethod("addTrigger", OATrigger.class, boolean.class).getReturnType());
        assertEquals(boolean.class, TriggerOps.class.getMethod("removeTrigger", OATrigger.class).getReturnType());
    }

    @Test
    void syncOpsStartStopAreOnlyCheckedExceptionMethods() {
        for (Method m : SyncOps.class.getDeclaredMethods()) {
            if (m.getName().equals("start") || m.getName().equals("stop")) {
                assertArrayEquals(new Class<?>[] { Exception.class }, m.getExceptionTypes());
            } else {
                assertEquals(0, m.getExceptionTypes().length, m.toString());
            }
        }
    }

    @Test
    void publicApiInterfacesHaveNoFields() {
        assertEquals(0, SyncOps.class.getDeclaredFields().length);
        assertEquals(0, ReplOps.class.getDeclaredFields().length);
        assertEquals(0, TriggerOps.class.getDeclaredFields().length);
    }
}
