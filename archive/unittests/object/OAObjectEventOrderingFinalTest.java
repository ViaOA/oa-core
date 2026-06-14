package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class OAObjectEventOrderingFinalTest {

    public static class Item extends OAObject {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            String old = this.name;
            fireBeforePropertyChange("name", old, name);
            this.name = name;
            firePropertyChange("name", old, name);
        }

        public void setNameAfterOnly(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }
    }

    @Test
    void afterPropertyChangeSeesCompletedValue() {
        Item item = new Item();
        List<String> values = new ArrayList<>();

        item.addPropertyChangeListener("name", evt -> values.add(item.getName()));

        item.setName("A");

        assertEquals(List.of("A"), values);
    }

    @Test
    void multipleListenersReceiveSameEventValuesInRegistrationOrder() {
        Item item = new Item();
        List<String> values = new ArrayList<>();

        item.addPropertyChangeListener("name", evt -> values.add("1:" + evt.getOldValue() + "->" + evt.getNewValue()));
        item.addPropertyChangeListener("name", evt -> values.add("2:" + evt.getOldValue() + "->" + evt.getNewValue()));

        item.setName("A");

        assertEquals(List.of("1:null->A", "2:null->A"), values);
    }

    @Test
    void listenerExceptionPropagatesButValueIsAlreadyCompletedCurrentContract() {
        Item item = new Item();
        item.addPropertyChangeListener("name", evt -> {
            throw new RuntimeException("boom");
        });

        RuntimeException ex = assertThrows(RuntimeException.class, () -> item.setName("A"));

        assertEquals("boom", ex.getMessage());
        assertEquals("A", item.getName());
    }

    @Test
    void failedBeforePropertyChangePreventsManualMutationWhenCallerChecksFirstDesiredContract() {
        Item item = new Item();
        item.addPropertyChangeListener("name", evt -> {
            throw new RuntimeException("before boom");
        });

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            String old = item.getName();
            item.fireBeforePropertyChange("name", old, "A");
            item.setNameAfterOnly("A");
        });

        assertEquals("before boom", ex.getMessage());
        assertNull(item.getName());
    }

    @Test
    void localPropertyChangeUsesSameListenerSurfaceForSimpleObject() {
        Item item = new Item();
        List<PropertyChangeEvent> events = new ArrayList<>();
        item.addPropertyChangeListener(events::add);

        item.fireLocalPropertyChange("name", null, "A");

        assertEquals(1, events.size());
        assertEquals("name", events.get(0).getPropertyName());
        assertNull(events.get(0).getOldValue());
        assertEquals("A", events.get(0).getNewValue());
    }

    @Test
    void removeAllListenersLeavesMutationSafe() {
        Item item = new Item();
        PropertyChangeListener li = evt -> fail("listener should have been removed");

        item.addPropertyChangeListener(li);
        item.removePropertyChangeListener(li);

        assertDoesNotThrow(() -> item.setName("A"));
        assertEquals("A", item.getName());
    }
}
