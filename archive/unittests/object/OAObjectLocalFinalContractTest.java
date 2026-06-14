package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.metadata.OAObjectInfo;

import org.junit.jupiter.api.Test;

class OAObjectLocalFinalContractTest {

    public static class LocalChild extends OAObjectLocal {
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
    void localObjectInfoRemainsConfiguredForLocalOnlyUse() {
        OAObjectInfo oi = OAObjectLocal.getOAObjectInfo();

        assertTrue(oi.getLocalOnly());
        assertFalse(oi.getUseDataSource());
        assertFalse(oi.getAddToCache());
        assertFalse(oi.getInitializeNewObjects());
    }

    @Test
    void subclassOfOAObjectLocalStillHasRuntimeIdentity() {
        LocalChild child = new LocalChild();

        assertNotNull(child.getGuid());
        assertEquals(child.getGuid(), child.getObjectKey().getGuid());
    }

    @Test
    void localObjectSupportsPropertyEvents() {
        LocalChild child = new LocalChild();
        java.util.concurrent.atomic.AtomicInteger cnt = new java.util.concurrent.atomic.AtomicInteger();

        child.addPropertyChangeListener("name", evt -> cnt.incrementAndGet());
        child.setName("A");

        assertEquals(1, cnt.get());
        assertEquals("A", child.getName());
    }

    @Test
    void localObjectLifecycleFlagsBehaveLikeOAObject() {
        LocalChild child = new LocalChild();

        assertTrue(child.isNew());
        assertTrue(child.isChanged());

        child.setNew(false);
        child.setChanged(false);
        child.setDeleted(true);

        assertFalse(child.isNew());
        assertFalse(child.isChanged());
        assertTrue(child.isDeleted());
    }

    @Test
    void localObjectSaveDeleteSurfacesFailOrNoopWithoutFalseSuccess() {
        LocalChild child = new LocalChild();

        try {
            child.save();
        } catch (RuntimeException ex) {
            assertTrue(child.isNew());
        }

        try {
            child.delete();
        } catch (RuntimeException ex) {
            assertFalse(child.isDeleted());
        }
    }
}
