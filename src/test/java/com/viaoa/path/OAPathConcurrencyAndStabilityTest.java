package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAPathConcurrencyAndStabilityTest {

    public static class Root extends OAObject {
        private Child child;

        public Root() {
        }

        public Root(String childName) {
            this.child = new Child(childName);
        }

        public Child getChild() {
            return child;
        }
    }

    public static class Child extends OAObject {
        private String name;

        public Child() {
        }

        public Child(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Test
    void compiledPathConcurrentReadEvaluationIsStable() throws Exception {
        OAPath<Root> pp = new OAPath<>(Root.class, "child.name");

        int props = pp.getProperties().length;
        int methods = pp.getMethods().length;
        int classes = pp.getClasses().length;

        ExecutorService es = Executors.newFixedThreadPool(4);
        try {
            List<Callable<String>> calls = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                final int x = i;
                calls.add(() -> (String) pp.getValue(new Root("child-" + x)));
            }

            List<Future<String>> futures = es.invokeAll(calls);
            for (int i = 0; i < futures.size(); i++) {
                assertEquals("child-" + i, futures.get(i).get(5, TimeUnit.SECONDS));
            }
        } finally {
            es.shutdownNow();
        }

        assertEquals(props, pp.getProperties().length);
        assertEquals(methods, pp.getMethods().length);
        assertEquals(classes, pp.getClasses().length);
    }

    @Test
    void repeatedValueAsStringEvaluationDoesNotMutateCompiledArrays() {
        OAPath<Root> pp = new OAPath<>(Root.class, "child.name");
        Root root = new Root("A");

        int props = pp.getProperties().length;
        int methods = pp.getMethods().length;
        int classes = pp.getClasses().length;

        for (int i = 0; i < 25; i++) {
            assertEquals("A", pp.getValueAsString(root));
            assertEquals(props, pp.getProperties().length);
            assertEquals(methods, pp.getMethods().length);
            assertEquals(classes, pp.getClasses().length);
        }
    }

    @Test
    void compiledMetadataAccessorsReturnStableArraysByLength() {
        OAPath<Root> pp = new OAPath<>(Root.class, "child.name");

        assertSame(pp.getProperties(), pp.getProperties());
        assertSame(pp.getMethods(), pp.getMethods());
        assertSame(pp.getClasses(), pp.getClasses());
        assertSame(pp.getLinkInfos(), pp.getLinkInfos());
    }
}
