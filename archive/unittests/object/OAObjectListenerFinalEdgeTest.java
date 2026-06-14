package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class OAObjectListenerFinalEdgeTest {

    public static class Item extends OAObject {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }
    }

    @Test
    void sameListenerAddedTwiceReceivesTwoEventsCurrentContract() {
        Item item = new Item();
        AtomicInteger cnt = new AtomicInteger();
        PropertyChangeListener li = evt -> cnt.incrementAndGet();

        item.addPropertyChangeListener("name", li);
        item.addPropertyChangeListener("name", li);

        item.setName("A");

        assertEquals(2, cnt.get(), "duplicate listener registration is currently honored");
    }

    @Test
    void removingOneDuplicateListenerLeavesOneRegistrationCurrentContract() {
        Item item = new Item();
        AtomicInteger cnt = new AtomicInteger();
        PropertyChangeListener li = evt -> cnt.incrementAndGet();

        item.addPropertyChangeListener("name", li);
        item.addPropertyChangeListener("name", li);
        item.removePropertyChangeListener("name", li);

        item.setName("A");

        assertEquals(1, cnt.get());
    }

    @Test
    void globalAndPropertySpecificListenersBothReceiveMatchingEvent() {
        Item item = new Item();
        List<String> list = new ArrayList<>();

        item.addPropertyChangeListener(evt -> list.add("global:" + evt.getPropertyName()));
        item.addPropertyChangeListener("name", evt -> list.add("specific:" + evt.getPropertyName()));

        item.setName("A");

        assertEquals(List.of("global:name", "specific:name"), list);
    }

    @Test
    void removingGlobalListenerDoesNotRemovePropertySpecificListener() {
        Item item = new Item();
        AtomicInteger global = new AtomicInteger();
        AtomicInteger specific = new AtomicInteger();

        PropertyChangeListener g = evt -> global.incrementAndGet();
        PropertyChangeListener s = evt -> specific.incrementAndGet();

        item.addPropertyChangeListener(g);
        item.addPropertyChangeListener("name", s);

        item.removePropertyChangeListener(g);

        item.setName("A");

        assertEquals(0, global.get());
        assertEquals(1, specific.get());
    }

    @Test
    void listenerReceivesSourceObject() {
        Item item = new Item();
        List<PropertyChangeEvent> events = new ArrayList<>();

        item.addPropertyChangeListener(events::add);

        item.setName("A");

        assertSame(item, events.get(0).getSource());
    }

    @Test
    void listenerCanRemoveItselfDuringCallbackWithoutCorruptingFutureEvents() {
        Item item = new Item();
        AtomicInteger cnt = new AtomicInteger();

        PropertyChangeListener[] ref = new PropertyChangeListener[1];
        ref[0] = evt -> {
            cnt.incrementAndGet();
            item.removePropertyChangeListener("name", ref[0]);
        };

        item.addPropertyChangeListener("name", ref[0]);

        item.setName("A");
        item.setName("B");

        assertEquals(1, cnt.get());
    }
}
