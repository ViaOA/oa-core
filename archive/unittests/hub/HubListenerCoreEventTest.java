package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class HubListenerCoreEventTest {

    public static class Item extends OAObject {
        private String name;
        public Item() { }
        public Item(String name) { this.name = name; }
        public String getName() { return name; }
        public void setName(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }
    }

    static class RecordingListener extends HubListenerAdapter<Item> {
        final List<String> events = new ArrayList<>();

        @Override
        public void afterAdd(HubEvent<Item> e) {
            events.add("afterAdd:" + ((Item) e.getObject()).getName());
        }

        @Override
        public void afterRemove(HubEvent<Item> e) {
            events.add("afterRemove:" + ((Item) e.getObject()).getName());
        }

        @Override
        public void afterChangeActiveObject(HubEvent<Item> e) {
            Object obj = e.getObject();
            events.add("afterAO:" + (obj instanceof Item ? ((Item) obj).getName() : "null"));
        }

        @Override
        public void onNewList(HubEvent<Item> e) {
            events.add("newList");
        }
    }

    @Test
    void afterAddAndAfterRemoveEventsFire() {
        Hub<Item> hub = new Hub<>(Item.class);
        RecordingListener li = new RecordingListener();
        hub.addHubListener(li);

        Item a = new Item("A");
        hub.add(a);
        hub.remove(a);

        assertTrue(li.events.contains("afterAdd:A"));
        assertTrue(li.events.contains("afterRemove:A"));
    }

    @Test
    void activeObjectChangeEventFires() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a = new Item("A");
        hub.add(a);

        RecordingListener li = new RecordingListener();
        hub.addHubListener(li);

        hub.setAO(a);

        assertTrue(li.events.contains("afterAO:A"));
    }

    @Test
    void clearFiresNewListOrRemoveEvents() {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item("A"));
        hub.add(new Item("B"));

        RecordingListener li = new RecordingListener();
        hub.addHubListener(li);

        hub.clear();

        assertFalse(li.events.isEmpty(), "clear should publish some observable list/remove event");
    }

    @Test
    void removeHubListenerStopsFutureEvents() {
        Hub<Item> hub = new Hub<>(Item.class);
        RecordingListener li = new RecordingListener();

        hub.addHubListener(li);
        hub.removeHubListener(li);

        hub.add(new Item("A"));

        assertTrue(li.events.isEmpty());
    }

    @Test
    void addListenerAliasAndRemoveListenerAliasWork() {
        Hub<Item> hub = new Hub<>(Item.class);
        RecordingListener li = new RecordingListener();

        hub.addListener(li);
        hub.add(new Item("A"));
        hub.removeListener(li);
        hub.add(new Item("B"));

        assertEquals(1, li.events.stream().filter(s -> s.startsWith("afterAdd")).count());
    }

    @Test
    void propertySpecificListenerReceivesPropertyChangeForMember() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a = new Item("A");
        hub.add(a);

        List<String> events = new ArrayList<>();
        hub.addHubListener(new HubListenerAdapter<Item>() {
            @Override
            public void afterPropertyChange(HubEvent<Item> e) {
                events.add(e.getPropertyName() + ":" + ((Item) e.getObject()).getName());
            }
        }, "name");

        a.setName("B");

        assertTrue(events.contains("name:B"));
    }
}
