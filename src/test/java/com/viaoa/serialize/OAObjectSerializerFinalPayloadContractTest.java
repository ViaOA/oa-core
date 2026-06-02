package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectSerializerFinalPayloadContractTest {

    public static class Item extends OAObject implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;

        public Item() {
        }

        public Item(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Test
    void payloadCanContainMultipleWrappersSequentiallyUncompressed() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(new OAObjectSerializer<>(new Item("A"), false));
            out.writeObject(new OAObjectSerializer<>(new Item("B"), false));
        }

        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            OAObjectSerializer<Item> a = (OAObjectSerializer<Item>) in.readObject();
            OAObjectSerializer<Item> b = (OAObjectSerializer<Item>) in.readObject();

            assertEquals("A", a.getObject().getName());
            assertEquals("B", b.getObject().getName());
        }
    }

    @Test
    void payloadCanContainMultipleWrappersSequentiallyCompressed() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(new OAObjectSerializer<>(new Item("A"), true));
            out.writeObject(new OAObjectSerializer<>(new Item("B"), true));
        }

        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            OAObjectSerializer<Item> a = (OAObjectSerializer<Item>) in.readObject();
            OAObjectSerializer<Item> b = (OAObjectSerializer<Item>) in.readObject();

            assertEquals("A", a.getObject().getName());
            assertEquals("B", b.getObject().getName());
        }
    }

    @Test
    void payloadCanMixCompressedAndUncompressedWrappersSequentially() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(new OAObjectSerializer<>(new Item("A"), false));
            out.writeObject(new OAObjectSerializer<>(new Item("B"), true));
            out.writeObject("tail");
        }

        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            OAObjectSerializer<Item> a = (OAObjectSerializer<Item>) in.readObject();
            OAObjectSerializer<Item> b = (OAObjectSerializer<Item>) in.readObject();
            Object tail = in.readObject();

            assertEquals("A", a.getObject().getName());
            assertEquals("B", b.getObject().getName());
            assertEquals("tail", tail);
        }
    }

    @Test
    void finalObjectCountIsReadableAfterRootAndExtraObjects() throws Exception {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item("A"), new Item("B"), false, null);

        OAObjectSerializer<Item> copy = roundTrip(ser);

        assertTrue(copy.getTotalObjectsWritten() >= 0);
        assertEquals("A", copy.getObject().getName());
        assertEquals("B", ((Item) copy.getExtraObject()).getName());
    }

    @Test
    void hubPayloadStillAllowsFollowingTailObjectInSameStream() throws Exception {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item("A"));
        hub.add(new Item("B"));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(new OAObjectSerializer<>(hub, true));
            out.writeObject("tail");
        }

        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            OAObjectSerializer<Hub<Item>> copy = (OAObjectSerializer<Hub<Item>>) in.readObject();
            Object tail = in.readObject();

            assertEquals(2, copy.getObject().getSize());
            assertEquals("tail", tail);
        }
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
