package com.viaoa.graph.api;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.viaoa.trigger.OATrigger;

import org.junit.jupiter.api.Test;

class GraphApiPublicSurfaceContractTest {

    @Test
    void syncOpsDefinesRoleLifecycleAndStateQueryMethods() throws Exception {
        Set<String> names = Arrays.stream(SyncOps.class.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toSet());

        assertTrue(names.contains("createServer"));
        assertTrue(names.contains("createClient"));
        assertTrue(names.contains("start"));
        assertTrue(names.contains("stop"));
        assertTrue(names.contains("isSingleUser"));
        assertTrue(names.contains("isServer"));
        assertTrue(names.contains("isClient"));
        assertTrue(names.contains("isRunning"));

        assertEquals(8, SyncOps.class.getDeclaredMethods().length,
            "SyncOps should remain a small explicit public lifecycle contract");
    }

    @Test
    void syncOpsMethodSignaturesMatchPublicLifecycleContract() throws Exception {
        assertEquals(void.class, SyncOps.class.getMethod("createServer", int.class).getReturnType());
        assertEquals(void.class, SyncOps.class.getMethod("createClient", String.class, int.class).getReturnType());
        assertEquals(void.class, SyncOps.class.getMethod("start").getReturnType());
        assertEquals(void.class, SyncOps.class.getMethod("stop").getReturnType());

        assertEquals(boolean.class, SyncOps.class.getMethod("isSingleUser").getReturnType());
        assertEquals(boolean.class, SyncOps.class.getMethod("isServer").getReturnType());
        assertEquals(boolean.class, SyncOps.class.getMethod("isClient").getReturnType());
        assertEquals(boolean.class, SyncOps.class.getMethod("isRunning").getReturnType());
    }

    @Test
    void syncStartAndStopDeclareCheckedExceptionBoundary() throws Exception {
        assertArrayEquals(new Class<?>[] { Exception.class }, SyncOps.class.getMethod("start").getExceptionTypes());
        assertArrayEquals(new Class<?>[] { Exception.class }, SyncOps.class.getMethod("stop").getExceptionTypes());
    }

    @Test
    void triggerOpsDefinesAddRemoveContract() throws Exception {
        assertEquals(void.class, TriggerOps.class.getMethod("addTrigger", OATrigger.class).getReturnType());
        assertEquals(void.class, TriggerOps.class.getMethod("addTrigger", OATrigger.class, boolean.class).getReturnType());
        assertEquals(boolean.class, TriggerOps.class.getMethod("removeTrigger", OATrigger.class).getReturnType());

        assertEquals(3, TriggerOps.class.getDeclaredMethods().length);
    }

    @Test
    void replOpsIsCurrentlyEmptyPublicContract() {
        assertEquals(0, ReplOps.class.getDeclaredMethods().length,
            "Current public replication API is intentionally/incompletely empty; implementation lifecycle must not be assumed from ReplOps");
    }

    @Test
    void publicApiTypesAreInterfacesAndNotImplementationClasses() {
        assertTrue(SyncOps.class.isInterface());
        assertTrue(ReplOps.class.isInterface());
        assertTrue(TriggerOps.class.isInterface());

        assertTrue(Modifier.isPublic(SyncOps.class.getModifiers()));
        assertTrue(Modifier.isPublic(ReplOps.class.getModifiers()));
        assertTrue(Modifier.isPublic(TriggerOps.class.getModifiers()));
    }

    @Test
    void publicApiPackageDoesNotExposeInternalOpsByInheritance() {
        for (Class<?> c : new Class<?>[] { SyncOps.class, ReplOps.class, TriggerOps.class }) {
            for (Class<?> parent : c.getInterfaces()) {
                assertFalse(parent.getName().contains(".api.internal."),
                    c.getName() + " must not extend internal API type");
            }
        }
    }

    @Test
    void syncOpsRoleQueryNamesAreDistinctAndUnambiguous() throws Exception {
        Method isServer = SyncOps.class.getMethod("isServer");
        Method isClient = SyncOps.class.getMethod("isClient");
        Method isSingleUser = SyncOps.class.getMethod("isSingleUser");
        Method isRunning = SyncOps.class.getMethod("isRunning");

        assertNotEquals(isServer, isClient);
        assertNotEquals(isSingleUser, isRunning);
        assertEquals(0, isServer.getParameterCount());
        assertEquals(0, isClient.getParameterCount());
        assertEquals(0, isSingleUser.getParameterCount());
        assertEquals(0, isRunning.getParameterCount());
    }

    @Test
    void publicApiDoesNotDeclareDefaultMethods() {
        for (Class<?> c : new Class<?>[] { SyncOps.class, ReplOps.class, TriggerOps.class }) {
            for (Method m : c.getDeclaredMethods()) {
                assertFalse(m.isDefault(), "public graph API should define contract only, not default behavior: " + m);
                assertTrue(Modifier.isAbstract(m.getModifiers()));
            }
        }
    }
}
