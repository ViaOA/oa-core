package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class OAObjectEventRejectionDeepTest {

    public static class Item extends OAObject {
        private String name;

        public String getName() { return name; }

        public void setNameValidated(String name) {
            String old = this.name;
            if (!isValidPropertyChange("name", old, name)) return;
            fireBeforePropertyChange("name", old, name);
            this.name = name;
            firePropertyChange("name", old, name);
        }
    }

    public static class RejectingItem extends Item {
        @Override
        public boolean isValidPropertyChange(String propertyName, Object oldValue, Object newValue) {
            return false;
        }
    }

    @Test
    void rejectedPropertyChangePublishesNoAfterEvent() {
        RejectingItem item = new RejectingItem();
        List<PropertyChangeEvent> events = new ArrayList<>();
        item.addPropertyChangeListener(events::add);

        item.setNameValidated("A");

        assertNull(item.getName());
        assertTrue(events.isEmpty());
    }

    @Test
    void beforeListenerExceptionPreventsCompletedMutationWhenSetterOrdersBeforeMutation() {
        Item item = new Item();
        item.addPropertyChangeListener("name", evt -> {
            throw new RuntimeException("before fail");
        });

        assertThrows(RuntimeException.class, () -> item.setNameValidated("A"));

        assertNull(item.getName());
    }

    @Test
    void afterListenerExceptionLeavesCompletedValueAndVisibleFailure() {
        class AfterOnlyItem extends OAObject {
            private String name;
            public String getName() { return name; }
            public void setName(String name) {
                String old = this.name;
                this.name = name;
                firePropertyChange("name", old, name);
            }
        }

        AfterOnlyItem item = new AfterOnlyItem();
        item.addPropertyChangeListener("name", evt -> {
            throw new RuntimeException("after fail");
        });

        assertThrows(RuntimeException.class, () -> item.setName("A"));
        assertEquals("A", item.getName());
    }

    @Test
    void oldNewValuesRemainCorrectAcrossMultipleUpdates() {
        class AfterOnlyItem extends OAObject {
            private String name;
            public String getName() { return name; }
            public void setName(String name) {
                String old = this.name;
                this.name = name;
                firePropertyChange("name", old, name);
            }
        }

        AfterOnlyItem item = new AfterOnlyItem();
        List<String> values = new ArrayList<>();
        item.addPropertyChangeListener("name", evt -> values.add(evt.getOldValue() + "->" + evt.getNewValue()));

        item.setName("A");
        item.setName("B");
        item.setName(null);

        assertEquals(List.of("null->A", "A->B", "B->null"), values);
    }
}
