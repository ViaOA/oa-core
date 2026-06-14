package com.viaoa.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class OACompressWrapperTest {

    @Test
    void constructorStoresWrappedObject() {
        List<String> value = List.of("one", "two");

        OACompressWrapper wrapper = new OACompressWrapper(value);

        assertSame(value, wrapper.getObject());
    }

    @Test
    void getObjectReturnsNullWhenWrappedObjectIsNullAfterRoundTrip() throws Exception {
        OACompressWrapper copy = roundTrip(new OACompressWrapper(null));

        assertNull(copy.getObject());
    }

    @Test
    void serializationRoundTripsSerializableObject() throws Exception {
        ArrayList<String> value = new ArrayList<>();
        value.add("alpha");
        value.add("beta");

        OACompressWrapper copy = roundTrip(new OACompressWrapper(value));

        assertEquals(value, copy.getObject());
        assertNotSame(value, copy.getObject());
    }

    @Test
    void multipleWrappersCanRoundTripInSeparateStreams() throws Exception {
        assertEquals("first", roundTrip(new OACompressWrapper("first")).getObject());
        assertEquals("second", roundTrip(new OACompressWrapper("second")).getObject());
    }

    @Test
    void nonSerializableWrappedObjectFailsDuringSerialization() {
        OACompressWrapper wrapper = new OACompressWrapper(new Object());

        assertThrows(Exception.class, () -> serialize(wrapper));
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws Exception {
        byte[] bytes = serialize(value);
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (T) in.readObject();
        }
    }

    private static byte[] serialize(Object value) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bout)) {
            out.writeObject(value);
        }
        return bout.toByteArray();
    }
}
