package com.viaoa.graph.sibling;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import com.viaoa.annotation.OACalculatedProperty;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASiblingHelperPhase2DeepInvariantTest {

    public static class Parent extends OAObject {
        private String name;
        private Child child;
        private final Hub<Child> children = new Hub<>(Child.class);

        public Parent() {
        }

        public Parent(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Child getChild() {
            return child;
        }

        public void setChild(Child child) {
            Child old = this.child;
            this.child = child;
            firePropertyChange("child", old, child);
        }

        public Hub<Child> getChildren() {
            return children;
        }

        @OACalculatedProperty(properties = { "child.grandChild", "children.grandChildren" })
        public String getCalcText() {
            return name;
        }
    }

    public static class Child extends OAObject {
        private String name;
        private Parent parent;
        private GrandChild grandChild;
        private final Hub<GrandChild> grandChildren = new Hub<>(GrandChild.class);

        public Child() {
        }

        public Child(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Parent getParent() {
            return parent;
        }

        public void setParent(Parent parent) {
            Parent old = this.parent;
            this.parent = parent;
            firePropertyChange("parent", old, parent);
        }

        public GrandChild getGrandChild() {
            return grandChild;
        }

        public void setGrandChild(GrandChild grandChild) {
            GrandChild old = this.grandChild;
            this.grandChild = grandChild;
            firePropertyChange("grandChild", old, grandChild);
        }

        public Hub<GrandChild> getGrandChildren() {
            return grandChildren;
        }
    }

    public static class GrandChild extends OAObject {
        private String name;
        private Child child;

        public GrandChild() {
        }

        public GrandChild(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Child getChild() {
            return child;
        }

        public void setChild(Child child) {
            Child old = this.child;
            this.child = child;
            firePropertyChange("child", old, child);
        }
    }

    public static class OtherRoot extends OAObject {
        private Child child;
        public Child getChild() {
            return child;
        }
        public void setChild(Child child) {
            Child old = this.child;
            this.child = child;
            firePropertyChange("child", old, child);
        }
    }

    public static class Unrelated extends OAObject {
        private String name;
        public String getName() {
            return name;
        }
    }

    private static Hub<Parent> parentHub() {
        Hub<Parent> hub = new Hub<>(Parent.class);
        Parent p = new Parent("P");
        Child c = new Child("C");
        GrandChild g = new GrandChild("G");

        p.setChild(c);
        c.setParent(p);
        c.setGrandChild(g);
        g.setChild(c);

        p.getChildren().add(c);
        c.getGrandChildren().add(g);

        hub.add(p);
        return hub;
    }

    @Test
    void calculatedPropertyDependenciesAreExpandedIntoRelationshipPaths() {
        Hub<Parent> hub = parentHub();
        Parent p = hub.getAt(0);
        Child c = p.getChild();

        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);
        helper.add("calcText");

        assertEquals("child.grandChild", helper.getPropertyPath(c, "grandChild"),
            "dependency child.grandChild from calculated property should become a sibling path");
    }

    @Test
    void calculatedDependencyExpansionDoesNotReportScalarCalcAsLink() {
        Hub<Parent> hub = parentHub();
        Parent p = hub.getAt(0);

        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);
        helper.add("calcText");

        assertNull(helper.getPropertyPath(p, "calcText"),
            "calculated scalar property itself is not a relationship path");
    }

    @Test
    void invalidNestedSegmentStopsWithoutFalseSuccess() {
        Hub<Parent> hub = parentHub();
        Parent p = hub.getAt(0);
        Child c = p.getChild();

        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);
        helper.add("child.missing.grandChild");

        assertNull(helper.getPropertyPath(c, "missing"));
        assertNull(helper.getPropertyPath(c, "grandChild"),
            "invalid intermediate segment must not accidentally learn later valid segment");
    }

    @Test
    void nonRootClassHelperDoesNotResolveUnrelatedRootPath() {
        Hub<Parent> parents = parentHub();

        Hub<OtherRoot> others = new Hub<>(OtherRoot.class);
        OtherRoot other = new OtherRoot();
        other.setChild(parents.getAt(0).getChild());
        others.add(other);

        OASiblingHelper<Parent> parentHelper = new OASiblingHelper<>(parents);
        OASiblingHelper<OtherRoot> otherHelper = new OASiblingHelper<>(others);

        parentHelper.add("child.grandChild");

        assertEquals("child.grandChild", parentHelper.getPropertyPath(parents.getAt(0).getChild(), "grandChild"));
        assertNull(otherHelper.getPropertyPath(other.getChild(), "grandChild"),
            "other helper must not inherit parent helper's learned tree");
    }

    @Test
    void explicitPathLearningDoesNotMutateHubMembershipOrAO() {
        Hub<Parent> hub = parentHub();
        Parent p = hub.getAt(0);
        hub.setAO(p);

        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);

        int size = hub.getSize();
        Parent ao = hub.getAO();

        helper.add("child.grandChild");
        helper.getPropertyPath(p.getChild(), "grandChild");
        helper.onGetReference(p.getChild(), "grandChild");

        assertEquals(size, hub.getSize());
        assertSame(ao, hub.getAO());
        assertSame(p, hub.getAt(0));
    }

    @Test
    void deletedOrDetachedObjectDoesNotCreateFalseUnrelatedPath() {
        Hub<Parent> hub = parentHub();
        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);

        Child detached = new Child("detached");
        detached.setDeleted(true);

        assertNull(helper.getPropertyPath(detached, "grandChild"));

        helper.onGetReference(detached, "grandChild");

        assertEquals("child.grandChild", helper.getPropertyPath(detached, "grandChild"),
            "metadata can discover path by class, but result must remain metadata-only and not imply live membership");
        assertFalse(hub.getAt(0).getChildren().contains(detached));
    }

    @Test
    void nullPropertyPathResolutionReturnsNullOrSafeMetadataResultOnly() {
        Hub<Parent> hub = parentHub();
        Parent p = hub.getAt(0);

        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);
        helper.add("child");

        assertNull(helper.getPropertyPath(p, null));
    }

    @Test
    void repeatedMixedLearningOrderIsDeterministic() {
        Hub<Parent> hub = parentHub();
        Parent p = hub.getAt(0);
        Child c = p.getChild();

        OASiblingHelper<Parent> a = new OASiblingHelper<>(hub);
        a.add("child.grandChild");
        a.onGetReference(c, "grandChild");

        OASiblingHelper<Parent> b = new OASiblingHelper<>(hub);
        b.onGetReference(c, "grandChild");
        b.add("child.grandChild");

        assertEquals("child.grandChild", a.getPropertyPath(c, "grandChild"));
        assertEquals("child.grandChild", b.getPropertyPath(c, "grandChild"));
    }

    @Test
    void lastFoundOptimizationDoesNotLoseAlternateValidResolutionAfterMiss() {
        Hub<Parent> hub = parentHub();
        Parent p = hub.getAt(0);
        Child c = p.getChild();

        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);
        helper.add("child.grandChild");

        assertEquals("child.grandChild", helper.getPropertyPath(c, "grandChild"));
        assertNull(helper.getPropertyPath(new Unrelated(), "name", true));
        assertEquals("child.grandChild", helper.getPropertyPath(c, "grandChild"));
    }

    @Test
    void sameThreadFlagDocumentsRiskWithoutChangingLocalBehavior() {
        Hub<Parent> hub = parentHub();
        Child c = hub.getAt(0).getChild();

        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);
        helper.setUseSameThread(true);
        helper.add("child.grandChild");

        assertTrue(helper.getUseSameThread());
        assertEquals("child.grandChild", helper.getPropertyPath(c, "grandChild"));
    }

    @Test
    void concurrentLearningAndResolutionDoesNotThrowOrReturnForeignPathBoundary() throws Exception {
        Hub<Parent> hub = parentHub();
        Parent p = hub.getAt(0);
        Child c = p.getChild();

        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);

        ExecutorService es = Executors.newFixedThreadPool(4);
        try {
            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                final int x = i;
                tasks.add(() -> {
                    if ((x % 3) == 0) helper.add("child.grandChild");
                    if ((x % 3) == 1) helper.onGetReference(c, "grandChild");
                    return helper.getPropertyPath(c, "grandChild");
                });
            }

            for (Future<String> f : es.invokeAll(tasks)) {
                String path = f.get(5, TimeUnit.SECONDS);
                assertTrue(path == null || "child.grandChild".equals(path),
                    "concurrent boundary should not produce foreign or malformed path: " + path);
            }
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void noForcedLazyLoadingObservableByStableNullReference() {
        Hub<Parent> hub = new Hub<>(Parent.class);
        Parent p = new Parent("P");
        hub.add(p);

        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);

        assertNull(p.getChild());

        helper.add("child.grandChild");
        helper.getPropertyPath(p, "child");
        helper.onGetReference(p, "child");

        assertNull(p.getChild(), "sibling path discovery must not materialize reference objects");
        assertEquals(1, hub.getSize());
    }

    @Test
    void packageInvariantCoverageSmoke() {
        Hub<Parent> hub = parentHub();
        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);

        assertSame(hub, helper.getHub());

        helper.add("child.grandChild");

        assertEquals("child.grandChild", helper.getPropertyPath(hub.getAt(0).getChild(), "grandChild"));
        assertNull(helper.getPropertyPath(new Unrelated(), "name"));
    }
}
