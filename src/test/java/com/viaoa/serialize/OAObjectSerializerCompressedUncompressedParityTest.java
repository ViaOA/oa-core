package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectSerializerCompressedUncompressedParityTest {
    public static class Item extends OAObject implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        public Item() { }
        public Item(String name) { this.name = name; }
        public String getName() { return name; }
    }

    @Test
    void compressedAndUncompressedObjectRoundTripSameSemanticObject() throws Exception {
        Item item = new Item("A");

        OAObjectSerializer<Item> uncompressed = roundTrip(new OAObjectSerializer<>(item, false));
        OAObjectSerializer<Item> compressed = roundTrip(new OAObjectSerializer<>(item, true));

        assertEquals(uncompressed.getObject().getName(), compressed.getObject().getName());
    }

    @Test
    void compressedAndUncompressedExtraObjectRoundTripSameSemantics() throws Exception {
        Item item = new Item("root");
        Item extra = new Item("extra");

        OAObjectSerializer<Item> uncompressed = roundTrip(new OAObjectSerializer<>(item, extra, false, null));
        OAObjectSerializer<Item> compressed = roundTrip(new OAObjectSerializer<>(item, extra, true, null));

        assertEquals(uncompressed.getObject().getName(), compressed.getObject().getName());
        assertEquals(((Item) uncompressed.getExtraObject()).getName(), ((Item) compressed.getExtraObject()).getName());
    }

    @Test
    void compressedAndUncompressedHubRoundTripSameMembershipOrder() throws Exception {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item("A"));
        hub.add(new Item("B"));

        OAObjectSerializer<Hub<Item>> uncompressed = roundTrip(new OAObjectSerializer<>(hub, false));
        OAObjectSerializer<Hub<Item>> compressed = roundTrip(new OAObjectSerializer<>(hub, true));

        assertEquals(uncompressed.getObject().getSize(), compressed.getObject().getSize());
        assertEquals(uncompressed.getObject().getAt(0).getName(), compressed.getObject().getAt(0).getName());
        assertEquals(uncompressed.getObject().getAt(1).getName(), compressed.getObject().getAt(1).getName());
    }

    @Test
    void compressedPayloadIsReadableBeforeTailObjectInSameStream() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(new OAObjectSerializer<>(new Item("A"), true));
            out.writeObject("tail");
        }

        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            OAObjectSerializer<Item> copy = (OAObjectSerializer<Item>) in.readObject();
            Object tail = in.readObject();

            assertEquals("A", copy.getObject().getName());
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
