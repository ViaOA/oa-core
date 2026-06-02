package com.viaoa.graph.api.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.viaoa.graph.api.ReplOps;
import com.viaoa.graph.api.SyncOps;
import com.viaoa.graph.api.TriggerOps;
import com.viaoa.graph.service.hub.HubStatusService.HubCurrentStateEnum;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.trigger.OATrigger;

import org.junit.jupiter.api.Test;

class GraphApiInternalSurfaceContractTest {

    @Test
    void internalOpsExtendTheirPublicApiContracts() {
        assertTrue(SyncOps.class.isAssignableFrom(SyncInternalOps.class));
        assertTrue(ReplOps.class.isAssignableFrom(ReplInternalOps.class));
        assertTrue(TriggerOps.class.isAssignableFrom(TriggerInternalOps.class));
    }

    @Test
    void syncInternalAddsRuntimeInspectionMethods() throws Exception {
        Set<String> names = Arrays.stream(SyncInternalOps.class.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toSet());

        assertTrue(names.contains("getClient"));
        assertTrue(names.contains("getServer"));
        assertTrue(names.contains("isConnected"));
        assertTrue(names.contains("getConnectionId"));
        assertTrue(names.contains("sendException"));
        assertTrue(names.contains("getClientInfo"));
        assertTrue(names.contains("updateClientInfo"));
        assertTrue(names.contains("saveCache"));
        assertTrue(names.contains("performDGC"));
        assertTrue(names.contains("callRemoteClientRefresh"));
        assertTrue(names.contains("getRemoteClient"));
        assertTrue(names.contains("getRemoteServer"));
    }

    @Test
    void syncInternalRetainsPublicRoleQueryMethodsThroughInheritance() throws Exception {
        assertEquals(boolean.class, SyncInternalOps.class.getMethod("isSingleUser").getReturnType());
        assertEquals(boolean.class, SyncInternalOps.class.getMethod("isServer").getReturnType());
        assertEquals(boolean.class, SyncInternalOps.class.getMethod("isClient").getReturnType());
        assertEquals(boolean.class, SyncInternalOps.class.getMethod("isRunning").getReturnType());
    }

    @Test
    void syncInternalRemoteRefreshOverloadsAreExplicit() throws Exception {
        assertEquals(void.class, SyncInternalOps.class.getMethod("callRemoteClientRefresh", Class.class, OAObjectKey.class).getReturnType());
        assertEquals(void.class, SyncInternalOps.class.getMethod("callRemoteClientRefresh", Class.class, OAObjectKey.class, String.class).getReturnType());
    }

    @Test
    void replInternalAddsRoleQueriesToEmptyPublicContract() throws Exception {
        assertEquals(boolean.class, ReplInternalOps.class.getMethod("isMaster").getReturnType());
        assertEquals(boolean.class, ReplInternalOps.class.getMethod("isClient").getReturnType());

        assertEquals(2, ReplInternalOps.class.getDeclaredMethods().length);
        assertEquals(0, ReplOps.class.getDeclaredMethods().length);
    }

    @Test
    void triggerInternalAddsRunTriggerBoundary() throws Exception {
        assertEquals(void.class, TriggerInternalOps.class.getMethod("runTrigger", Runnable.class).getReturnType());

        assertEquals(void.class, TriggerInternalOps.class.getMethod("addTrigger", OATrigger.class).getReturnType());
        assertEquals(boolean.class, TriggerInternalOps.class.getMethod("removeTrigger", OATrigger.class).getReturnType());
    }

    @Test
    void objectsInternalOpsIsLargeButInterfaceOnlyBoundary() {
        assertTrue(ObjectsInternalOps.class.isInterface());
        assertTrue(ObjectsInternalOps.class.getDeclaredMethods().length > 50,
            "ObjectsInternalOps is expected to be the large object-service internal friend API");
        for (Method m : ObjectsInternalOps.class.getDeclaredMethods()) {
            assertTrue(Modifier.isAbstract(m.getModifiers()));
            assertFalse(m.isDefault());
        }
    }

    @Test
    void hubsInternalOpsIsLargeButInterfaceOnlyBoundary() {
        assertTrue(HubsInternalOps.class.isInterface());
        assertTrue(HubsInternalOps.class.getDeclaredMethods().length > 50,
            "HubsInternalOps is expected to be the large hub-service internal friend API");
        for (Method m : HubsInternalOps.class.getDeclaredMethods()) {
            assertTrue(Modifier.isAbstract(m.getModifiers()));
            assertFalse(m.isDefault());
        }
    }

    @Test
    void hubsInternalCurrentStateMethodExposesServiceImplementationEnumCurrentRisk() throws Exception {
        Method m = HubsInternalOps.class.getMethod("callHubStatusGetCurrentState", com.viaoa.hub.Hub.class, com.viaoa.hub.Hub.class, java.util.ArrayList.class);

        assertEquals(HubCurrentStateEnum.class, m.getReturnType(),
            "Documents current CODEX import-boundary risk: api.internal depends on graph.service.hub nested enum");
    }

    @Test
    void internalApiShouldNotDependOnServiceImplementationTypesDesiredContract() {
        boolean hasServiceTypeLeak = false;

        for (Method m : HubsInternalOps.class.getDeclaredMethods()) {
            if (m.getReturnType().getName().startsWith("com.viaoa.graph.service.")) {
                hasServiceTypeLeak = true;
            }
            for (Class<?> p : m.getParameterTypes()) {
                if (p.getName().startsWith("com.viaoa.graph.service.")) {
                    hasServiceTypeLeak = true;
                }
            }
        }

        assertFalse(hasServiceTypeLeak,
            "Desired contract: api.internal should not import/return graph.service implementation types");
    }

    @Test
    void objectsInternalIncludesCoreIdentityCachePropertySaveDeleteBoundaries() {
        Set<String> names = Arrays.stream(ObjectsInternalOps.class.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toSet());

        assertTrue(names.contains("callObjectGuidGetGuid"));
        assertTrue(names.contains("callObjectGuidSetGuid"));
        assertTrue(names.contains("callObjectKeyGetKey"));
        assertTrue(names.contains("callObjectCacheGet"));
        assertTrue(names.contains("callObjectPropertyGetProperty"));
        assertTrue(names.contains("callObjectPropertySetProperty"));
        assertTrue(names.contains("callObjectSaveSave"));
        assertTrue(names.contains("callObjectDeleteDelete"));
        assertTrue(names.contains("callObjectSiblingGetSiblings"));
    }

    @Test
    void hubsInternalIncludesCoreMembershipAoEventShareSortBoundaries() {
        Set<String> names = Arrays.stream(HubsInternalOps.class.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toSet());

        assertTrue(names.contains("callHubAddRemoveAdd"));
        assertTrue(names.contains("callHubAddRemoveRemove"));
        assertTrue(names.contains("callHubAOSetActiveObject"));
        assertTrue(names.contains("callHubEventAddHubListener"));
        assertTrue(names.contains("callHubShareCreateSharedHub"));
        assertTrue(names.contains("callHubSortSort"));
        assertTrue(names.contains("callHubSelectSelect"));
        assertTrue(names.contains("callHubSerializeReadResolve"));
    }

    @Test
    void internalApiTypesArePublicInterfaces() {
        for (Class<?> c : new Class<?>[] {
            SyncInternalOps.class, ReplInternalOps.class, TriggerInternalOps.class,
            ObjectsInternalOps.class, HubsInternalOps.class
        }) {
            assertTrue(c.isInterface(), c.getName());
            assertTrue(Modifier.isPublic(c.getModifiers()), c.getName());
        }
    }
}
