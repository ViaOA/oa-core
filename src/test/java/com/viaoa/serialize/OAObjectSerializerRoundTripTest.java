package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import com.viaoa.callback.OAObjectSerializerCallback;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectSerializerRoundTripTest {
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

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(value);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            return (T) in.readObject();
        }
    }

    @Test
    void uncompressedWrapperRoundTripsObjectAndId() throws Exception {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item("A"), false);
        ser.setId(77);
        ser.setClientId(88);

        OAObjectSerializer<Item> copy = roundTrip(ser);

        assertEquals(77, copy.getId());
        assertEquals(0, copy.getClientId(), "clientId is not written by custom wrapper stream");
        assertEquals("A", copy.getObject().getName());
    }

    @Test
    void compressedWrapperRoundTripsObjectAndId() throws Exception {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item("A"), true);
        ser.setId(78);

        OAObjectSerializer<Item> copy = roundTrip(ser);

        assertEquals(78, copy.getId());
        assertEquals("A", copy.getObject().getName());
    }

    @Test
    void extraObjectRoundTripsUncompressedAndCompressed() throws Exception {
        OAObjectSerializer<Item> a = roundTrip(new OAObjectSerializer<>(new Item("root"), new Item("extra"), false, null));
        assertEquals("root", a.getObject().getName());
        assertEquals("extra", ((Item) a.getExtraObject()).getName());

        OAObjectSerializer<Item> b = roundTrip(new OAObjectSerializer<>(new Item("root"), new Item("extra"), true, null));
        assertEquals("root", b.getObject().getName());
        assertEquals("extra", ((Item) b.getExtraObject()).getName());
    }

    @Test
    void nullObjectAndNullExtraRoundTrip() throws Exception {
        OAObjectSerializer<Object> copy = roundTrip(new OAObjectSerializer<>(null, false));

        assertNull(copy.getObject());
        assertNull(copy.getExtraObject());
    }

    @Test
    void hubRootRoundTripsMembershipOrder() throws Exception {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item("A"));
        hub.add(new Item("B"));

        OAObjectSerializer<Hub<Item>> copy = roundTrip(new OAObjectSerializer<>(hub, false));

        Hub<Item> hub2 = copy.getObject();
        assertEquals(2, hub2.getSize());
        assertEquals("A", hub2.getAt(0).getName());
        assertEquals("B", hub2.getAt(1).getName());
    }

    @Test
    void runtimeOnlyOptionsAreNotSerializedCurrentContract() throws Exception {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item("A"), false);
        ser.setIncludeBlobs(true);
        ser.setMax(10);
        ser.setMaxSize(20);
        ser.setCallback(new OAObjectSerializerCallback() {
			@Override
			public void beforeSerialize(OAObject obj) {
				// TODO Auto-generated method stub
			}
        });

        OAObjectSerializer<Item> copy = roundTrip(ser);

        assertFalse(copy.getIncludeBlobs());
        assertEquals(0, copy.getMax());
        assertEquals(0, copy.getMaxSize());
        assertNull(copy.getCallback());
    }
}
