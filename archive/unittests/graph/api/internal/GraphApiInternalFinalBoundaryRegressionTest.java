
package com.viaoa.graph.api.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.viaoa.graph.api.ReplOps;
import com.viaoa.graph.api.SyncOps;
import com.viaoa.graph.api.TriggerOps;
import com.viaoa.graph.service.hub.HubStatusService.HubCurrentStateEnum;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class GraphApiInternalFinalBoundaryRegressionTest {

    static class Item extends OAObject {
    }

    @Test
    void internalInterfacesExtendExpectedPublicContracts() {
        assertTrue(SyncOps.class.isAssignableFrom(SyncInternalOps.class));
        assertTrue(ReplOps.class.isAssignableFrom(ReplInternalOps.class));
        assertTrue(TriggerOps.class.isAssignableFrom(TriggerInternalOps.class));
    }

    @Test
    void triggerInternalOnlyAddsRunTrigger() {
        Set<String> declared = Arrays.stream(TriggerInternalOps.class.getDeclaredMethods()).map(Method::getName).collect(Collectors.toSet());
        assertEquals(Set.of("runTrigger"), declared);
    }

    @Test
    void replInternalAddsOnlyRoleQueries() {
        Set<String> declared = Arrays.stream(ReplInternalOps.class.getDeclaredMethods()).map(Method::getName).collect(Collectors.toSet());
        assertEquals(Set.of("isMaster", "isClient"), declared);
    }

    @Test
    void objectsInternalOpsCoverageMapContainsExpectedServiceAreas() {
        Set<String> names = Arrays.stream(ObjectsInternalOps.class.getDeclaredMethods()).map(Method::getName).collect(Collectors.toSet());
        String[] prefixes = { "callObjectAnnotation", "callObjectCache", "callObjectCallback", "callObjectCS", "callObjectDelete", "callObjectDS", "callObjectEmpty", "callObjectEnum", "callObjectEvent", "callObjectGuid", "callObjectHub", "callObjectInfo", "callObjectInitialize", "callObjectKey", "callObjectLock", "callObjectProperty", "callObjectReflect", "callObjectSave", "callObjectScheduler", "callObjectSerialize", "callObjectSibling", "callObjectUnique" };
        for (String prefix : prefixes) assertTrue(names.stream().anyMatch(s -> s.startsWith(prefix)), "missing prefix " + prefix);
    }

    @Test
    void hubsInternalOpsCoverageMapContainsExpectedServiceAreas() {
        Set<String> names = Arrays.stream(HubsInternalOps.class.getDeclaredMethods()).map(Method::getName).collect(Collectors.toSet());
        String[] prefixes = { "callHubAddRemove", "callHubAO", "callHubAutoMatch", "callHubCS", "callHubData", "callHubDelete", "callHubDetail", "callHubEvent", "callHubFind", "callHubLink", "callHubProperty", "callHubRoot", "callHubSave", "callHubSelect", "callHubSequence", "callHubSerialize", "callHubShare", "callHubSize", "callHubSort", "callHubStatus" };
        for (String prefix : prefixes) assertTrue(names.stream().anyMatch(s -> s.startsWith(prefix)), "missing prefix " + prefix);
    }

    @Test
    void objectInternalGuidAndKeyBoundariesHaveExpectedSignatures() throws Exception {
        assertEquals(java.util.UUID.class, ObjectsInternalOps.class.getMethod("callObjectGuidGetGuid", OAObject.class).getReturnType());
        assertEquals(void.class, ObjectsInternalOps.class.getMethod("callObjectGuidSetGuid", OAObject.class, java.util.UUID.class).getReturnType());
        assertEquals(com.viaoa.object.OAObjectKey.class, ObjectsInternalOps.class.getMethod("callObjectKeyGetKey", OAObject.class).getReturnType());
        assertEquals(com.viaoa.object.OAObjectKey.class, ObjectsInternalOps.class.getMethod("callObjectKeyCreateObjectKey", OAObject.class).getReturnType());
    }

    @Test
    void hubInternalAddRemoveAndAoSignaturesAreStable() throws Exception {
        assertEquals(boolean.class, HubsInternalOps.class.getMethod("callHubAddRemoveAdd", Hub.class, OAObject.class).getReturnType());
        assertEquals(boolean.class, HubsInternalOps.class.getMethod("callHubAddRemoveRemove", Hub.class, Object.class).getReturnType());
        assertEquals(void.class, HubsInternalOps.class.getMethod("callHubAddRemoveClear", Hub.class).getReturnType());
        assertEquals(OAObject.class, HubsInternalOps.class.getMethod("callHubAOSetActiveObject", Hub.class, int.class).getReturnType());
        assertEquals(void.class, HubsInternalOps.class.getMethod("callHubAOSetActiveObject", Hub.class, OAObject.class).getReturnType());
    }

    @Test
    void hubInternalServiceEnumLeakIsDocumentedAsCurrentCodexRisk() throws Exception {
        Method m = HubsInternalOps.class.getMethod("callHubStatusGetCurrentState", Hub.class, Hub.class, java.util.ArrayList.class);
        assertEquals(HubCurrentStateEnum.class, m.getReturnType());
        assertTrue(m.getReturnType().getName().startsWith("com.viaoa.graph.service."));
    }

    @Test
    void desiredInternalApiBoundaryWouldHaveNoGraphServiceTypes() {
        boolean hasLeak = false;
        for (Class<?> c : new Class<?>[] { HubsInternalOps.class, ObjectsInternalOps.class, SyncInternalOps.class, ReplInternalOps.class, TriggerInternalOps.class }) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getReturnType().getName().startsWith("com.viaoa.graph.service.")) hasLeak = true;
                for (Class<?> p : m.getParameterTypes()) {
                    if (p.getName().startsWith("com.viaoa.graph.service.")) hasLeak = true;
                }
            }
        }
        assertFalse(hasLeak, "Desired CODEX invariant: graph.api.internal should not depend on graph.service implementation types");
    }

    @Test
    void internalApiInterfacesHaveNoFields() {
        for (Class<?> c : new Class<?>[] { HubsInternalOps.class, ObjectsInternalOps.class, SyncInternalOps.class, ReplInternalOps.class, TriggerInternalOps.class }) {
            assertEquals(0, c.getDeclaredFields().length, c.getName());
        }
    }
}
