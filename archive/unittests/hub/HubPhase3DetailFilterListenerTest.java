package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.filter.OAFilter;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class HubPhase3DetailFilterListenerTest {

    public static class Child extends OAObject {
        private String name;
        private int amount;
        private Parent parent;

        public Child() {
        }

        public Child(String name, int amount) {
            this.name = name;
            this.amount = amount;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            int old = this.amount;
            this.amount = amount;
            firePropertyChange("amount", old, amount);
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

    public static class Parent extends OAObject {
        private String name;
        private final Hub<Child> children = new Hub<>(Child.class);

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
    }

    private static Hub<Parent> parents() {
        Hub<Parent> hub = new Hub<>(Parent.class);

        Parent a = new Parent("A");
        Child a1 = new Child("A1", 10);
        Child a2 = new Child("A2", 20);
        a1.setParent(a);
        a2.setParent(a);
        a.getChildren().add(a1);
        a.getChildren().add(a2);

        Parent b = new Parent("B");
        Child b1 = new Child("B1", 30);
        b1.setParent(b);
        b.getChildren().add(b1);

        hub.add(a);
        hub.add(b);
        return hub;
    }

    @Test
    void getDetailHubByPathFollowsActiveParent() {
        Hub<Parent> parents = parents();
        Hub<Child> detail = parents.getDetailHub("children");

        parents.setPos(0);

        assertEquals(2, detail.getSize());
        assertEquals("A1", detail.getAt(0).getName());
        assertEquals("A2", detail.getAt(1).getName());

        parents.setPos(1);

        assertEquals(1, detail.getSize());
        assertEquals("B1", detail.getAt(0).getName());
    }

    @Test
    void detailHubForNullActiveObjectIsEmpty() {
        Hub<Parent> parents = parents();
        Hub<Child> detail = parents.getDetailHub("children");

        parents.resetAO();

        assertEquals(0, detail.getSize());
        assertNull(detail.getAO());
    }

    @Test
    void detailHubUsesSameObjectClassAsDetailProperty() {
        Hub<Parent> parents = parents();

        Hub<Child> detail = parents.getDetailHub("children");

        assertEquals(Child.class, detail.getObjectClass());
    }

    @Test
    void detailHubUpdatesWhenChildAddedToActiveParent() {
        Hub<Parent> parents = parents();
        Hub<Child> detail = parents.getDetailHub("children");

        parents.setPos(0);
        Parent parent = parents.getAO();

        Child added = new Child("A3", 40);
        added.setParent(parent);
        parent.getChildren().add(added);

        assertTrue(detail.contains(added));
        assertEquals(3, detail.getSize());
    }

    @Test
    void detailHubUpdatesWhenChildRemovedFromActiveParent() {
        Hub<Parent> parents = parents();
        Hub<Child> detail = parents.getDetailHub("children");

        parents.setPos(0);
        Parent parent = parents.getAO();
        Child removed = parent.getChildren().getAt(0);

        parent.getChildren().remove(removed);

        assertFalse(detail.contains(removed));
        assertEquals(1, detail.getSize());
    }

    @Test
    void detailHubMasterHubMetadataIsAvailable() {
        Hub<Parent> parents = parents();
        Hub<Child> detail = parents.getDetailHub("children");

        assertSame(parents, detail.getMasterHub());
        assertSame(parents.getAO(), detail.getMasterObject());
        assertEquals(Parent.class, detail.getMasterClass());
    }

    @Test
    void invalidDetailPathFailsOrReturnsSafeEmptyHub() {
        Hub<Parent> parents = parents();

        try {
            Hub<?> detail = parents.getDetailHub("missingPath");
            assertNotNull(detail);
            assertEquals(0, detail.getSize());
        } catch (RuntimeException ex) {
            assertNotNull(ex.getMessage());
        }
    }

    @Test
    void propertySpecificListenerReceivesOnlyRequestedProperty() {
        Hub<Child> hub = new Hub<>(Child.class);
        Child child = new Child("A", 10);
        hub.add(child);

        List<String> events = new ArrayList<>();
        hub.addHubListener(new HubListenerAdapter<Child>() {
            @Override
            public void afterPropertyChange(HubEvent<Child> e) {
                events.add(e.getPropertyName() + ":" + e.getObject().getName());
            }
        }, "name");

        child.setName("B");
        child.setAmount(20);

        assertEquals(List.of("name:B"), events);
    }

    @Test
    void activeObjectOnlyPropertyListenerReceivesOnlyAOChanges() {
        Hub<Child> hub = new Hub<>(Child.class);
        Child a = new Child("A", 10);
        Child b = new Child("B", 20);
        hub.add(a);
        hub.add(b);
        hub.setAO(a);

        List<String> events = new ArrayList<>();
        hub.addHubListener(new HubListenerAdapter<Child>() {
            @Override
            public void afterPropertyChange(HubEvent<Child> e) {
                events.add(e.getObject().getName());
            }
        }, "name", true);

        a.setName("A2");
        b.setName("B2");

        assertEquals(List.of("A2"), events);
    }

    @Test
    void listenerExceptionDuringAfterAddLeavesAddCompletedAndExceptionVisible() {
        Hub<Child> hub = new Hub<>(Child.class);
        Child child = new Child("A", 10);

        hub.addHubListener(new HubListenerAdapter<Child>() {
            @Override
            public void afterAdd(HubEvent<Child> e) {
                throw new RuntimeException("boom");
            }
        });

        RuntimeException ex = assertThrows(RuntimeException.class, () -> hub.add(child));

        assertEquals("boom", ex.getMessage());
        assertTrue(hub.contains(child), "after-event failure must not pretend add never completed");
    }

    @Test
    void listenerExceptionDuringBeforeAddPreventsAddDesiredContract() {
        Hub<Child> hub = new Hub<>(Child.class);
        Child child = new Child("A", 10);

        hub.addHubListener(new HubListenerAdapter<Child>() {
            @Override
            public void beforeAdd(HubEvent<Child> e) {
                throw new RuntimeException("before boom");
            }
        });

        RuntimeException ex = assertThrows(RuntimeException.class, () -> hub.add(child));

        assertEquals("before boom", ex.getMessage());
        assertFalse(hub.contains(child), "before-event failure should prevent membership mutation");
    }

    @Test
    void validateListenerCanRejectAddDesiredContract() {
        Hub<Child> hub = new Hub<>(Child.class);
        Child child = new Child("A", 10);

        hub.addHubListener(new HubListenerAdapter<Child>() {
            @Override
            public boolean isValidAdd(HubEvent<Child> e) {
                return false;
            }
        });

        assertFalse(hub.add(child));
        assertFalse(hub.contains(child));
    }

    @Test
    void duplicateListenerRegistrationFiresTwiceCurrentContract() {
        Hub<Child> hub = new Hub<>(Child.class);
        AtomicInteger cnt = new AtomicInteger();

        HubListenerAdapter<Child> li = new HubListenerAdapter<Child>() {
            @Override
            public void afterAdd(HubEvent<Child> e) {
                cnt.incrementAndGet();
            }
        };

        hub.addHubListener(li);
        hub.addHubListener(li);

        hub.add(new Child("A", 10));

        assertEquals(2, cnt.get());
    }

    @Test
    void removingOneDuplicateListenerLeavesOneCurrentContract() {
        Hub<Child> hub = new Hub<>(Child.class);
        AtomicInteger cnt = new AtomicInteger();

        HubListenerAdapter<Child> li = new HubListenerAdapter<Child>() {
            @Override
            public void afterAdd(HubEvent<Child> e) {
                cnt.incrementAndGet();
            }
        };

        hub.addHubListener(li);
        hub.addHubListener(li);
        hub.removeHubListener(li);

        hub.add(new Child("A", 10));

        assertEquals(1, cnt.get());
    }

    @Test
    void filteredMembershipCanBeModeledWithManualFilterContract() {
        Hub<Child> source = new Hub<>(Child.class);
        Child a = new Child("A", 10);
        Child b = new Child("B", 20);
        source.add(a);
        source.add(b);

        OAFilter<Child> filter = child -> child.getAmount() >= 20;

        Hub<Child> filtered = new Hub<>(Child.class);
        for (Child child : source.toList()) {
            if (filter.isUsed(child)) filtered.add(child);
        }

        assertFalse(filtered.contains(a));
        assertTrue(filtered.contains(b));
    }

    @Test
    void setMasterHubExplicitlyCreatesMasterRelationshipBoundary() {
        Hub<Parent> parents = parents();
        Hub<Child> detail = new Hub<>(Child.class);

        assertDoesNotThrow(() -> detail.setMasterHub(parents, "children"));

        assertSame(parents, detail.getMasterHub());
    }

    @Test
    void removeDetailHubRemovesRelationshipOrReturnsFalseForUnknown() {
        Hub<Parent> parents = parents();
        Hub<Child> detail = parents.getDetailHub("children");

        boolean removed = parents.removeDetailHub(detail);

        assertTrue(removed || !parents.hasDetailHubs());
    }

    @Test
    void hasDetailHubsBecomesTrueAfterDetailHubCreation() {
        Hub<Parent> parents = parents();

        assertFalse(parents.hasDetailHubs());

        parents.getDetailHub("children");

        assertTrue(parents.hasDetailHubs());
    }
}
