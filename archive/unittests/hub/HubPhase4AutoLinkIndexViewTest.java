package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.hub.index.HubUniqueIndex;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class HubPhase4AutoLinkIndexViewTest {

    public static class Item extends OAObject {
        private String name;
        private String code;
        private int seq;
        private Category category;

        public Item() {
        }

        public Item(String name, String code) {
            this.name = name;
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            String old = this.code;
            this.code = code;
            firePropertyChange("code", old, code);
        }

        public int getSeq() {
            return seq;
        }

        public void setSeq(int seq) {
            int old = this.seq;
            this.seq = seq;
            firePropertyChange("seq", old, seq);
        }

        public Category getCategory() {
            return category;
        }

        public void setCategory(Category category) {
            Category old = this.category;
            this.category = category;
            firePropertyChange("category", old, category);
        }
    }

    public static class Category extends OAObject {
        private String name;

        public Category() {
        }

        public Category(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static Hub<Item> items() {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item("A", "a"));
        hub.add(new Item("B", "b"));
        hub.add(new Item("C", "c"));
        return hub;
    }

    @Test
    void autoSequenceAssignsSequenceValuesOnExistingAndNewObjects() {
        Hub<Item> hub = items();

        hub.setAutoSequence("seq", 1);

        assertEquals(1, hub.getAt(0).getSeq());
        assertEquals(2, hub.getAt(1).getSeq());
        assertEquals(3, hub.getAt(2).getSeq());

        Item d = new Item("D", "d");
        hub.add(d);

        assertTrue(d.getSeq() >= 1);
    }

    @Test
    void resequenceRestoresContiguousOrderingAfterMove() {
        Hub<Item> hub = items();
        hub.setAutoSequence("seq", 1);

        hub.move(2, 0);
        hub.resequence();

        assertEquals(1, hub.getAt(0).getSeq());
        assertEquals(2, hub.getAt(1).getSeq());
        assertEquals(3, hub.getAt(2).getSeq());
    }

    @Test
    void autoSequenceWithStartNumberUsesStartBoundary() {
        Hub<Item> hub = items();

        hub.setAutoSequence("seq", 10);

        assertEquals(10, hub.getAt(0).getSeq());
        assertEquals(11, hub.getAt(1).getSeq());
        assertEquals(12, hub.getAt(2).getSeq());
    }

    @Test
    void setAutoSequenceInvalidPropertyFailsOrNoopsWithoutMutation() {
        Hub<Item> hub = items();

        try {
            hub.setAutoSequence("missing", 1);
        } catch (RuntimeException ex) {
            assertNotNull(ex.getMessage());
        }

        assertEquals(0, hub.getAt(0).getSeq());
    }

    @Test
    void uniqueIndexFindsObjectsAndUpdatesOnPropertyChange() {
        Hub<Item> hub = items();

        HubUniqueIndex<Item> idx = new HubUniqueIndex<>(hub, "code", false);

        assertSame(hub.getAt(0), idx.get("a"));

        hub.getAt(0).setCode("aa");

        assertNull(idx.get("a"));
        assertSame(hub.getAt(0), idx.get("aa"));

        idx.close();
    }

    @Test
    void uniqueIndexRemovesObjectWhenRemovedFromHub() {
        Hub<Item> hub = items();
        HubUniqueIndex<Item> idx = new HubUniqueIndex<>(hub, "code", false);

        Item a = hub.getAt(0);
        hub.remove(a);

        assertNull(idx.get("a"));

        idx.close();
    }

    @Test
    void uniqueIndexCaseInsensitiveLookupWhenConfigured() {
        Hub<Item> hub = items();

        HubUniqueIndex<Item> idx = new HubUniqueIndex<>(hub, "code", true);

        assertSame(hub.getAt(0), idx.get("A"));
        assertSame(hub.getAt(0), idx.get("a"));

        idx.close();
    }

    @Test
    void uniqueIndexDuplicateKeyReturnsDefinedMember() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a = new Item("A1", "x");
        Item b = new Item("A2", "x");
        hub.add(a);
        hub.add(b);

        HubUniqueIndex<Item> idx = new HubUniqueIndex<>(hub, "code", false);

        Item found = idx.get("x");

        assertTrue(found == a || found == b);

        idx.close();
    }

    @Test
    void linkHubMethodsAreSafeOnUnlinkedHub() {
        Hub<Item> hub = items();

        assertNull(hub.getLinkHub(false));
        assertNull(hub.getLinkPath(false));

        assertDoesNotThrow(hub::removeLinkHub);
        assertTrue(hub.isValid());
    }

    @Test
    void setLinkHubWithDirectHubIsSafeBoundary() {
        Hub<Item> hub = items();
        Hub<Item> link = new Hub<>(Item.class);

        assertDoesNotThrow(() -> hub.setLinkHub(link));

        Hub<?> found = hub.getLinkHub(false);

        assertTrue(found == null || found == link);
    }

    @Test
    void setLinkHubOnPosBoundaryDoesNotCorruptMembership() {
        Hub<Item> hub = items();
        Hub<Item> link = new Hub<>(Item.class);
        Item a = hub.getAt(0);

        assertDoesNotThrow(() -> hub.setLinkHubOnPos(link, "name"));

        assertTrue(hub.contains(a));
        assertEquals(3, hub.getSize());
    }

    @Test
    void addHubAddHubBoundaryRoundTrips() {
        Hub<Item> hub = items();
        Hub<Item> addHub = new Hub<>(Item.class);

        hub.setAddHub(addHub);

        assertSame(addHub, hub.getAddHub());

        Item x = new Item("X", "x");
        hub.add(x);

        assertTrue(hub.contains(x));
    }

    @Test
    void rootHubCanBeMarkedAndRetrieved() {
        Hub<Item> hub = items();

        hub.setRootHub();

        assertSame(hub, hub.getRootHub());
        assertSame(hub, hub.getRealHub());
    }

    @Test
    void listenerValidationRejectsRemoveDesiredContract() {
        Hub<Item> hub = items();
        Item a = hub.getAt(0);

        hub.addHubListener(new HubListenerAdapter<Item>() {
            @Override
            public boolean isValidRemove(HubEvent<Item> e) {
                return false;
            }
        });

        assertFalse(hub.remove(a));
        assertTrue(hub.contains(a));
    }

    @Test
    void beforeRemoveExceptionPreventsRemoveDesiredContract() {
        Hub<Item> hub = items();
        Item a = hub.getAt(0);

        hub.addHubListener(new HubListenerAdapter<Item>() {
            @Override
            public void beforeRemove(HubEvent<Item> e) {
                throw new RuntimeException("before remove");
            }
        });

        RuntimeException ex = assertThrows(RuntimeException.class, () -> hub.remove(a));

        assertEquals("before remove", ex.getMessage());
        assertTrue(hub.contains(a));
    }

    @Test
    void afterRemoveExceptionLeavesRemoveCompletedAndVisible() {
        Hub<Item> hub = items();
        Item a = hub.getAt(0);

        hub.addHubListener(new HubListenerAdapter<Item>() {
            @Override
            public void afterRemove(HubEvent<Item> e) {
                throw new RuntimeException("after remove");
            }
        });

        RuntimeException ex = assertThrows(RuntimeException.class, () -> hub.remove(a));

        assertEquals("after remove", ex.getMessage());
        assertFalse(hub.contains(a));
    }

    @Test
    void beforeReplaceExceptionPreventsReplacementDesiredContract() {
        Hub<Item> hub = items();
        Item old = hub.getAt(0);
        Item replacement = new Item("X", "x");

        hub.addHubListener(new HubListenerAdapter<Item>() {
            @Override
            public void beforeReplace(HubEvent<Item> e) {
                throw new RuntimeException("before replace");
            }
        });

        RuntimeException ex = assertThrows(RuntimeException.class, () -> hub.replace(0, replacement));

        assertEquals("before replace", ex.getMessage());
        assertSame(old, hub.getAt(0));
    }

    @Test
    void afterReplaceExceptionLeavesReplacementCompletedAndVisible() {
        Hub<Item> hub = items();
        Item replacement = new Item("X", "x");

        hub.addHubListener(new HubListenerAdapter<Item>() {
            @Override
            public void afterReplace(HubEvent<Item> e) {
                throw new RuntimeException("after replace");
            }
        });

        RuntimeException ex = assertThrows(RuntimeException.class, () -> hub.replace(0, replacement));

        assertEquals("after replace", ex.getMessage());
        assertSame(replacement, hub.getAt(0));
    }

    @Test
    void clearPublishesObservableEvents() {
        Hub<Item> hub = items();
        List<String> events = new ArrayList<>();

        hub.addHubListener(new HubListenerAdapter<Item>() {
            @Override
            public void onNewList(HubEvent<Item> e) {
                events.add("newList");
            }

            @Override
            public void afterRemove(HubEvent<Item> e) {
                events.add("remove");
            }
        });

        hub.clear();

        assertTrue(events.size() > 0);
        assertEquals(0, hub.getSize());
    }

    @Test
    void listenerCanRemoveItselfDuringEventWithoutCorruptingFutureEvents() {
        Hub<Item> hub = new Hub<>(Item.class);
        AtomicInteger cnt = new AtomicInteger();

        HubListenerAdapter<Item>[] ref = new HubListenerAdapter[1];
        ref[0] = new HubListenerAdapter<Item>() {
            @Override
            public void afterAdd(HubEvent<Item> e) {
                cnt.incrementAndGet();
                hub.removeHubListener(ref[0]);
            }
        };

        hub.addHubListener(ref[0]);

        hub.add(new Item("A", "a"));
        hub.add(new Item("B", "b"));

        assertEquals(1, cnt.get());
    }
}
