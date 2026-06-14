package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.junit.jupiter.api.Test;

class OAObjectPrimitiveNullDeepContractTest {

    public static class Item extends OAObject implements Serializable {
        private static final long serialVersionUID = 1L;
        private int count;
        private long total;
        private boolean active;
        private double amount;

        public int getCount() { return count; }
        public void setCount(int count) {
            int old = this.count;
            this.count = count;
            firePropertyChange("count", old, count);
        }

        public long getTotal() { return total; }
        public void setTotal(long total) {
            long old = this.total;
            this.total = total;
            firePropertyChange("total", old, total);
        }

        public boolean getActive() { return active; }
        public void setActive(boolean active) {
            boolean old = this.active;
            this.active = active;
            firePropertyChange("active", old, active);
        }

        public double getAmount() { return amount; }
        public void setAmount(double amount) {
            double old = this.amount;
            this.amount = amount;
            firePropertyChange("amount", old, amount);
        }
    }

    @Test
    void primitiveNullIndependentPerPropertyDesiredContract() {
        Item item = new Item();

        item.setNull("count");

        assertTrue(item.isNull("count"));
        assertFalse(item.isNull("total"));
        assertFalse(item.isNull("active"));
        assertFalse(item.isNull("amount"));
    }

    @Test
    void settingEachPrimitiveClearsOnlyItsNullStateDesiredContract() {
        Item item = new Item();

        item.setNull("count");
        item.setNull("total");

        item.setCount(0);

        assertFalse(item.isNull("count"));
        assertTrue(item.isNull("total"));
    }

    @Test
    void removePropertyRestoresPrimitiveNullDesiredContract() {
        Item item = new Item();
        item.setCount(5);

        item.removeProperty("count");

        assertTrue(item.isNull("count"));
    }

    @Test
    void primitiveNullSurvivesSerializationDesiredContract() throws Exception {
        Item item = new Item();
        item.setNull("count");
        item.setAmount(12.5);

        Item copy = roundTrip(item);

        assertTrue(copy.isNull("count"));
        assertFalse(copy.isNull("amount"));
        assertEquals(12.5, copy.getAmount(), 0.00001);
    }

    @Test
    void primitiveDefaultValueAfterExplicitSetIsNotNull() {
        Item item = new Item();

        item.setNull("count");
        item.setCount(0);

        assertFalse(item.isNull("count"));
        assertEquals(0, item.getCount());
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            return (T) in.readObject();
        }
    }
}
