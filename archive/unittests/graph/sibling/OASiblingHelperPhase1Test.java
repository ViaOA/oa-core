package com.viaoa.graph.sibling;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASiblingHelperPhase1Test {
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
    void constructorStoresRootHubAndSameThreadFlagDefaultsFalse() {
        Hub<Parent> hub = parentHub();
        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);

        assertSame(hub, helper.getHub());
        assertFalse(helper.getUseSameThread());
    }

    @Test
    void sameThreadFlagRoundTripsButDoesNotEnforceByItselfCurrentContract() throws Exception {
        OASiblingHelper<Parent> helper = new OASiblingHelper<>(parentHub());

        helper.setUseSameThread(true);

        assertTrue(helper.getUseSameThread());

        ExecutorService es = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> f = es.submit(helper::getUseSameThread);
            assertTrue(f.get(5, TimeUnit.SECONDS));
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void addEmptyOrNullPathIsNoop() {
        OASiblingHelper<Parent> helper = new OASiblingHelper<>(parentHub());
        Parent parent = helper.getHub().getAt(0);

        assertDoesNotThrow(() -> helper.add(null));
        assertDoesNotThrow(() -> helper.add(""));
        assertDoesNotThrow(() -> helper.add("   "));

        assertNull(helper.getPropertyPath(parent, "missing"));
    }

    @Test
    void explicitDirectLinkPathCanBeResolvedFromRootObject() {
        Hub<Parent> hub = parentHub();
        Parent parent = hub.getAt(0);

        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);
        helper.add("child");

        assertEquals("child", helper.getPropertyPath(parent, "child"));
    }

    @Test
    void explicitNestedLinkPathCanBeResolvedFromIntermediateObject() {
        Hub<Parent> hub = parentHub();
        Child child = hub.getAt(0).getChild();

        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);
        helper.add("child.grandChild");

        assertEquals("child.grandChild", helper.getPropertyPath(child, "grandChild"));
    }

    @Test
    void repeatedAddIsIdempotentForResolution() {
        Hub<Parent> hub = parentHub();
        Parent parent = hub.getAt(0);

        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);

        helper.add("child");
        helper.add("child");
        helper.add("child");

        assertEquals("child", helper.getPropertyPath(parent, "child"));
        assertEquals("child", helper.getPropertyPath(parent, "child"));
    }

    @Test
    void invalidPathDoesNotCreateFalseResolution() {
        OASiblingHelper<Parent> helper = new OASiblingHelper<>(parentHub());
        Parent parent = helper.getHub().getAt(0);

        assertDoesNotThrow(() -> helper.add("missing.path"));

        assertNull(helper.getPropertyPath(parent, "missing"));
    }

    @Test
    void scalarOnlyPathDoesNotBecomeSiblingLinkPath() {
        OASiblingHelper<Parent> helper = new OASiblingHelper<>(parentHub());
        Parent parent = helper.getHub().getAt(0);

        assertDoesNotThrow(() -> helper.add("name"));

        assertNull(helper.getPropertyPath(parent, "name"));
    }

    @Test
    void onGetReferenceLearnsDirectReferenceAccess() {
        Hub<Parent> hub = parentHub();
        Parent parent = hub.getAt(0);

        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);

        assertNull(helper.getPropertyPath(parent, "child"));

        helper.onGetReference(parent, "child");

        assertEquals("child", helper.getPropertyPath(parent, "child"));
    }

    @Test
    void onGetReferenceLearnsNestedReferenceByObjectClass() {
        Hub<Parent> hub = parentHub();
        Child child = hub.getAt(0).getChild();

        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);

        helper.onGetReference(child, "grandChild");

        assertEquals("child.grandChild", helper.getPropertyPath(child, "grandChild"));
    }

    @Test
    void onGetReferenceNullInputsAreNoops() {
        Hub<Parent> hub = parentHub();
        Parent parent = hub.getAt(0);

        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);

        assertDoesNotThrow(() -> helper.onGetReference(null, "child"));
        assertDoesNotThrow(() -> helper.onGetReference(parent, null));

        assertNull(helper.getPropertyPath(parent, "child"));
    }

    @Test
    void unrelatedObjectDoesNotResolvePath() {
        OASiblingHelper<Parent> helper = new OASiblingHelper<>(parentHub());

        assertNull(helper.getPropertyPath(new Unrelated(), "name"));

        helper.onGetReference(new Unrelated(), "name");

        assertNull(helper.getPropertyPath(new Unrelated(), "name"));
    }

    @Test
    void differentRootHubClassesDoNotShareLearnedPaths() {
        Hub<Parent> parents = parentHub();
        Hub<Child> children = new Hub<>(Child.class);
        Child child = parents.getAt(0).getChild();
        children.add(child);

        OASiblingHelper<Parent> parentHelper = new OASiblingHelper<>(parents);
        OASiblingHelper<Child> childHelper = new OASiblingHelper<>(children);

        parentHelper.add("child.grandChild");
        childHelper.add("grandChild");

        assertEquals("child.grandChild", parentHelper.getPropertyPath(child, "grandChild"));
        assertEquals("grandChild", childHelper.getPropertyPath(child, "grandChild"));
    }

    @Test
    void deterministicRepeatedResolutionReturnsSamePath() {
        Hub<Parent> hub = parentHub();
        Child child = hub.getAt(0).getChild();

        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);
        helper.add("child.grandChild");

        List<String> paths = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            paths.add(helper.getPropertyPath(child, "grandChild"));
        }

        assertTrue(paths.stream().allMatch("child.grandChild"::equals));
    }

    @Test
    void lastFoundSearchDoesNotHideValidPath() {
        Hub<Parent> hub = parentHub();
        Child child = hub.getAt(0).getChild();

        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);
        helper.add("child.grandChild");

        assertEquals("child.grandChild", helper.getPropertyPath(child, "grandChild"));
        assertEquals("child.grandChild", helper.getPropertyPath(child, "grandChild", true));
        assertEquals("child.grandChild", helper.getPropertyPath(child, "grandChild"));
    }

    @Test
    void concurrentReadOnlyResolutionAfterLearningIsDeterministicBoundary() throws Exception {
        Hub<Parent> hub = parentHub();
        Child child = hub.getAt(0).getChild();

        OASiblingHelper<Parent> helper = new OASiblingHelper<>(hub);
        helper.add("child.grandChild");

        ExecutorService es = Executors.newFixedThreadPool(4);
        try {
            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                tasks.add(() -> helper.getPropertyPath(child, "grandChild"));
            }

            for (Future<String> f : es.invokeAll(tasks)) {
                assertEquals("child.grandChild", f.get(5, TimeUnit.SECONDS));
            }
        } finally {
            es.shutdownNow();
        }
    }
}
