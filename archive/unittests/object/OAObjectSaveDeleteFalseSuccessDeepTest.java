package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;

import org.junit.jupiter.api.Test;

class OAObjectSaveDeleteFalseSuccessDeepTest {

    public static class Item extends OAObject {
        private String name;
        public String getName() { return name; }
        public void setName(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }
    }

    @Test
    void failedSaveDoesNotClearNewOrChanged() {
        Item item = new Item();
        item.setName("A");

        boolean wasNew = item.isNew();
        boolean wasChanged = item.isChanged();

        try {
            item.save();
        } catch (RuntimeException ex) {
            assertEquals(wasNew, item.isNew());
            assertEquals(wasChanged, item.isChanged());
            assertEquals("A", item.getName());
            return;
        }

        assertFalse(item.isChanged());
    }

    @Test
    void failedSaveWithCascadeAllDoesNotClearLifecycleFlags() {
        Item item = new Item();

        boolean wasNew = item.isNew();
        boolean wasChanged = item.isChanged();

        try {
            item.save(OAObject.CASCADE_ALL_LINKS);
        } catch (RuntimeException ex) {
            assertEquals(wasNew, item.isNew());
            assertEquals(wasChanged, item.isChanged());
        }
    }

    @Test
    void failedDeleteDoesNotRemoveHubMembershipDesiredContract() {
        Item item = new Item();
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(item);

        try {
            item.delete();
        } catch (RuntimeException ex) {
            assertTrue(hub.contains(item), "failed delete should not remove hub membership");
            assertFalse(item.isDeleted());
            return;
        }

        assertTrue(item.isDeleted());
    }

    @Test
    void failedDeleteDoesNotClearSimpleReferenceStateDesiredContract() {
        class Parent extends OAObject {
            private Item child;
            public Item getChild() { return child; }
            public void setChild(Item child) {
                Item old = this.child;
                this.child = child;
                firePropertyChange("child", old, child);
            }
        }

        Parent p = new Parent();
        Item child = new Item();
        p.setChild(child);

        try {
            child.delete();
        } catch (RuntimeException ex) {
            assertSame(child, p.getChild());
            assertFalse(child.isDeleted());
            return;
        }

        assertTrue(child.isDeleted());
    }

    @Test
    void failedDeleteWithCascadeAllDoesNotMarkDeleted() {
        Item item = new Item();

        try {
            item.delete(OAObject.CASCADE_ALL_LINKS);
        } catch (RuntimeException ex) {
            assertFalse(item.isDeleted());
            return;
        }

        assertTrue(item.isDeleted());
    }
}
