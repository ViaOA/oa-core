package com.viaoa.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.Store;
import com.viaoa.hub.Hub;
import com.viaoa.oa.OA;

class OARuntimeTest {
    @Test
    void singletonAndGraphLookupsAreDeterministic() {
        assertSame(OARuntime.get(), OARuntime.get());

        OA defaultGraph = OARuntime.createDefaultOA(Register.class);
        assertSame(defaultGraph, OARuntime.oa());
        assertNotSame(defaultGraph, OARuntime.oa((Class<?>) null));
        assertNotSame(defaultGraph, OARuntime.oa((String) null));

        OA posGraph = OARuntime.oa(Register.class);
        assertSame(posGraph, OARuntime.oa(Register.class));
        assertSame(posGraph, OARuntime.oa(Store.class));
        assertSame(posGraph, OARuntime.oa(new Hub<>(Register.class)));
        assertSame(posGraph, OARuntime.oa(Register.class.getPackage()));
    }

    @Test
    void createGraphHandlesNullAndCachesPackageGraph() {
        assertNull(OARuntime.createOA((String) null));
        assertNull(OARuntime.createOA((Package) null));

        OA graph = OARuntime.createOA(Register.class.getPackage());
        assertSame(graph, OARuntime.createOA(Register.class.getPackage().getName()));
    }

    @Test
    void serviceAccessorsReturnStableSingletonServices() {
        assertSame(OARuntime.thread(), OARuntime.thread());
        assertSame(OARuntime.datasource(), OARuntime.datasource());
        assertSame(OARuntime.context(), OARuntime.context());
    }

    @Test
    void unitTestResetRequiresUnitTestMode() {
        OARuntime runtime = OARuntime.get();

        runtime.setUnitTestMode(false);
        assertThrows(RuntimeException.class, runtime::unitTestReset);

        runtime.setUnitTestMode(true);
        assertDoesNotThrow(runtime::unitTestReset);
        runtime.setUnitTestMode(false);
    }
}
