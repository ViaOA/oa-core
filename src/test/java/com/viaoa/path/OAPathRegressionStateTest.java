package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAPathRegressionStateTest {

    public static class Root extends OAObject {
        private Child child = new Child("child");
        private final Hub<Child> children = new Hub<>(Child.class);

        public Root() {
            children.add(new Child("A"));
        }

        public Child getChild() {
            return child;
        }

        public Hub<Child> getChildren() {
            return children;
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
    void setupTwiceDoesNotDuplicateFilterMetadataArrays() {
        OAPath<Root> pp = new OAPath<>(Root.class, "children:Named(\"A\").name", true);

        int props = pp.getProperties().length;
        int casts = pp.getCastNames().length;
        int filters = pp.getFilterNames().length;
        int params = pp.getFilterParams().length;
        int methods = pp.getMethods().length;
        int classes = pp.getClasses().length;
        int links = pp.getLinkInfos().length;

        pp.setup(Root.class, true);

        assertEquals(props, pp.getProperties().length);
        assertEquals(casts, pp.getCastNames().length);
        assertEquals(filters, pp.getFilterNames().length);
        assertEquals(params, pp.getFilterParams().length);
        assertEquals(methods, pp.getMethods().length);
        assertEquals(classes, pp.getClasses().length);
        assertEquals(links, pp.getLinkInfos().length);
    }

    @Test
    void quotedFilterArgumentParsingTerminatesQuicklyAcrossRepeatedSetups() {
        OAPath<Root> pp = new OAPath<>("children:Named(\"a,b:c.d\").name");

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            pp.setup(Root.class, true);
            pp.setup(Root.class, true);
        });
    }

    @Test
    void failedSetupDoesNotConvertPathIntoDifferentSuccessfulPath() {
        OAPath<Root> pp = new OAPath<>("missing.name");

        assertThrows(RuntimeException.class, () -> pp.setup(Root.class));

        OAPath<Root> good = new OAPath<>("child.name");
        assertEquals("child", good.getValue(new Root()));

        assertThrows(RuntimeException.class, () -> pp.setup(Root.class));
    }

    @Test
    void evaluationAfterNullIntermediateDoesNotCacheNullForLaterNonNullRoot() {
        OAPath<Root> pp = new OAPath<>(Root.class, "child.name");

        class NullRoot extends Root {
            @Override
            public Child getChild() {
                return null;
            }
        }

        assertNull(pp.getValue(new NullRoot()));
        assertEquals("child", pp.getValue(new Root()));
    }

    @Test
    void evaluationAfterExceptionDoesNotCacheBadStateForLaterSuccess() {
        class ThrowRoot extends Root {
            @Override
            public Child getChild() {
                throw new IllegalStateException("boom");
            }
        }

        OAPath<Root> pp = new OAPath<>(Root.class, "child.name");

        assertThrows(RuntimeException.class, () -> pp.getValue(new ThrowRoot()));
        assertEquals("child", pp.getValue(new Root()));
    }
}
