package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class OAObjectPropertyEventContractTest {

    public static class Item extends OAObject {
        private String name;
        private int count;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            int old = this.count;
            this.count = count;
            firePropertyChange("count", old, count);
        }
    }

    @Test
    void propertyChangeListenerReceivesOldAndNewValues() {
        Item item = new Item();
        List<PropertyChangeEvent> events = new ArrayList<>();
        item.addPropertyChangeListener(events::add);

        item.setName("A");
        item.setName("B");

        assertEquals(2, events.size());

        assertEquals("name", events.get(0).getPropertyName());
        assertNull(events.get(0).getOldValue());
        assertEquals("A", events.get(0).getNewValue());

        assertEquals("A", events.get(1).getOldValue());
        assertEquals("B", events.get(1).getNewValue());
    }

    @Test
    void propertySpecificListenerReceivesOnlyMatchingProperty() {
        Item item = new Item();
        List<PropertyChangeEvent> events = new ArrayList<>();

        item.addPropertyChangeListener("name", events::add);

        item.setName("A");
        item.setCount(5);

        assertEquals(1, events.size());
        assertEquals("name", events.get(0).getPropertyName());
    }

    @Test
    void removingPropertyListenerStopsFutureNotifications() {
        Item item = new Item();
        List<PropertyChangeEvent> events = new ArrayList<>();
        PropertyChangeListener li = events::add;

        item.addPropertyChangeListener(li);
        item.setName("A");

        item.removePropertyChangeListener(li);
        item.setName("B");

        assertEquals(1, events.size());
    }

    @Test
    void removingPropertySpecificListenerStopsFutureNotifications() {
        Item item = new Item();
        List<PropertyChangeEvent> events = new ArrayList<>();
        PropertyChangeListener li = events::add;

        item.addPropertyChangeListener("name", li);
        item.setName("A");

        item.removePropertyChangeListener("name", li);
        item.setName("B");

        assertEquals(1, events.size());
    }

    @Test
    void fireBeforeAndAfterPropertyChangeAreSafeForSimpleObject() {
        Item item = new Item();

        assertDoesNotThrow(() -> item.fireBeforePropertyChange("name", null, "A"));
        assertDoesNotThrow(() -> item.firePropertyChange("name", null, "A"));
        assertDoesNotThrow(() -> item.fireLocalPropertyChange("name", null, "A"));
    }

    @Test
    void firePropertyChangeMarksObjectChangedDesiredContract() {
        Item item = new Item();
        item.setChanged(false);

        item.setName("A");

        assertTrue(item.isChanged(), "completed property mutation should mark object changed");
    }
}
