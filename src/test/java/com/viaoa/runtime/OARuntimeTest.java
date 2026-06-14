package com.viaoa.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.Store;
import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;

class OARuntimeTest {
    @Test
    void singletonAndGraphLookupsAreDeterministic() {
        assertSame(OARuntime.get(), OARuntime.get());

        OAGraph defaultGraph = OARuntime.defaultGraph();
        assertSame(defaultGraph, OARuntime.graph());
        assertSame(defaultGraph, OARuntime.graph((Class<?>) null));
        assertSame(defaultGraph, OARuntime.graph((String) null));

        OAGraph posGraph = OARuntime.graph(Register.class);
        assertSame(posGraph, OARuntime.graph(Register.class));
        assertSame(posGraph, OARuntime.graph(Store.class));
        assertSame(posGraph, OARuntime.graph(new Hub<>(Register.class)));
        assertSame(posGraph, OARuntime.graph(Register.class.getPackage()));
    }

    @Test
    void createGraphHandlesNullAndCachesPackageGraph() {
        assertNull(OARuntime.createGraph((String) null));
        assertNull(OARuntime.createGraph((Package) null));

        OAGraph graph = OARuntime.createGraph(Register.class.getPackage());
        assertSame(graph, OARuntime.createGraph(Register.class.getPackage().getName()));
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
