package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import com.viaoa.hub.view.HubCombined;
import com.viaoa.hub.view.HubFlattened;
import com.viaoa.hub.view.HubLeftJoin;
import com.viaoa.hub.view.SharedHub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class HubPhase5ViewCompositionTest {

    public static class Parent extends OAObject {
        private String name;
        private final Hub<Child> children = new Hub<>(Child.class);
        private final Hub<Parent> subParents = new Hub<>(Parent.class);

        public Parent() {
        }

        public Parent(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }

        public Hub<Child> getChildren() {
            return children;
        }

        public Hub<Parent> getSubParents() {
            return subParents;
        }
    }

    public static class Child extends OAObject {
        private String name;
        private Parent parent;

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
    }

    private static Hub<Parent> parentHub() {
        Hub<Parent> hub = new Hub<>(Parent.class);

        Parent a = new Parent("A");
        Child a1 = new Child("A1");
        Child a2 = new Child("A2");
        a1.setParent(a);
        a2.setParent(a);
        a.getChildren().add(a1);
        a.getChildren().add(a2);

        Parent b = new Parent("B");
        Child b1 = new Child("B1");
        b1.setParent(b);
        b.getChildren().add(b1);

        Parent c = new Parent("C");

        hub.add(a);
        hub.add(b);
        hub.add(c);
        return hub;
    }

    @Test
    void sharedHubViewReflectsMasterMembershipAndAO() {
        Hub<Parent> master = parentHub();

        SharedHub<Parent> shared = new SharedHub<>(master, true);

        assertEquals(master.getSize(), shared.getSize());
        assertSame(master.getAt(0), shared.getAt(0));

        master.setPos(1);

        assertSame(master.getAO(), shared.getAO());
    }

    @Test
    void sharedHubTracksAddedObjects() {
        Hub<Parent> master = parentHub();
        SharedHub<Parent> shared = new SharedHub<>(master, false);

        Parent d = new Parent("D");
        master.add(d);

        assertTrue(shared.contains(d));
        assertEquals(master.getSize(), shared.getSize());
    }

    @Test
    void hubCombinedContainsMembershipFromBothHubs() {
        Hub<Parent> a = new Hub<>(Parent.class);
        Hub<Parent> b = new Hub<>(Parent.class);
        Parent a1 = new Parent("A1");
        Parent b1 = new Parent("B1");
        a.add(a1);
        b.add(b1);

        HubCombined<Parent> combined = new HubCombined<>(a, b);

        assertTrue(combined.contains(a1));
        assertTrue(combined.contains(b1));
        assertEquals(2, combined.getSize());
    }

    @Test
    void hubCombinedTracksAddsAndRemoves() {
        Hub<Parent> a = new Hub<>(Parent.class);
        Hub<Parent> b = new Hub<>(Parent.class);
        HubCombined<Parent> combined = new HubCombined<>(a, b);

        Parent p = new Parent("P");
        a.add(p);

        assertTrue(combined.contains(p));

        a.remove(p);

        assertFalse(combined.contains(p));
    }

    @Test
    void hubFlattenedExpandsRecursiveHierarchy() {
        Parent root = new Parent("root");
        Parent a = new Parent("A");
        Parent b = new Parent("B");
        Parent aa = new Parent("AA");

        root.getSubParents().add(a);
        root.getSubParents().add(b);
        a.getSubParents().add(aa);

        Hub<Parent> roots = new Hub<>(Parent.class);
        roots.add(root);

        HubFlattened<Parent> flat = new HubFlattened<>(roots, "subParents");

        List<String> names = flat.toList().stream().map(Parent::getName).toList();

        assertTrue(names.contains("root"));
        assertTrue(names.contains("A"));
        assertTrue(names.contains("B"));
        assertTrue(names.contains("AA"));
    }

    @Test
    void hubFlattenedTracksNewChildAddedAfterCreation() {
        Parent root = new Parent("root");
        Hub<Parent> roots = new Hub<>(Parent.class);
        roots.add(root);

        HubFlattened<Parent> flat = new HubFlattened<>(roots, "subParents");

        Parent child = new Parent("child");
        root.getSubParents().add(child);

        assertTrue(flat.contains(child));
    }

    @Test
    void leftJoinPreservesLeftRowsWithoutRightMatches() {
        Hub<Parent> parents = parentHub();

        HubLeftJoin<Parent, Child> leftJoin = new HubLeftJoin<>(parents, "children");

        assertTrue(leftJoin.getSize() >= parents.getSize());
        assertTrue(leftJoin.toList().stream().anyMatch(p -> "C".equals(p.getName())));
    }

    @Test
    void leftJoinTracksAddedRightObjectBoundary() {
        Hub<Parent> parents = parentHub();
        Parent c = parents.find("name", "C");

        HubLeftJoin<Parent, Child> leftJoin = new HubLeftJoin<>(parents, "children");
        int before = leftJoin.getSize();

        Child c1 = new Child("C1");
        c1.setParent(c);
        c.getChildren().add(c1);

        assertTrue(leftJoin.getSize() >= before);
    }

    @Test
    void createSharedAliasReflectsSharedMembership() {
        Hub<Parent> master = parentHub();

        Hub<Parent> shared = master.createShared();

        assertEquals(master.getSize(), shared.getSize());
        assertSame(master.getAt(0), shared.getAt(0));
    }

    @Test
    void propertyEventsPropagateThroughSharedHubListener() {
        Hub<Parent> master = parentHub();
        Hub<Parent> shared = master.createSharedHub();

        List<String> events = new ArrayList<>();
        shared.addHubListener(new HubListenerAdapter<Parent>() {
            @Override
            public void afterPropertyChange(HubEvent<Parent> e) {
                events.add(e.getPropertyName() + ":" + e.getObject().getName());
            }
        }, "name");

        master.getAt(0).setName("A2");

        assertEquals(List.of("name:A2"), events);
    }

    @Test
    void clearingSharedViewClearsMasterBySharedMembershipContract() {
        Hub<Parent> master = parentHub();
        Hub<Parent> shared = master.createSharedHub();

        shared.clear();

        assertEquals(0, shared.getSize());
        assertEquals(0, master.getSize());
    }
}
