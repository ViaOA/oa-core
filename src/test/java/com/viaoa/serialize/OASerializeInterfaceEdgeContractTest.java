package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.Queue;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASerializeInterfaceEdgeContractTest {

    static class Item extends OAObject {
    }

    static class StrictReader implements OASerializeReader {
        final Queue<String> names = new ArrayDeque<>();
        final Queue<Object> values = new ArrayDeque<>();
        boolean inObject;
        boolean inHub;

        StrictReader add(String name, Object value) {
            names.add(name);
            values.add(value);
            return this;
        }

        @Override
        public boolean hasNext() {
            return !names.isEmpty();
        }

        @Override
        public String nextName() {
            if (!hasNext()) throw new IllegalStateException("no next name");
            return names.remove();
        }

        @Override
        public Object nextValue() {
            if (values.isEmpty()) throw new IllegalStateException("no next value");
            return values.remove();
        }

        @Override
        public void beginObject() {
            if (inObject) throw new IllegalStateException("already object");
            inObject = true;
        }

        @Override
        public void endObject() {
            if (!inObject) throw new IllegalStateException("not object");
            inObject = false;
        }

        @Override
        public void beginHub() {
            if (inHub) throw new IllegalStateException("already hub");
            inHub = true;
        }

        @Override
        public void endHub() {
            if (!inHub) throw new IllegalStateException("not hub");
            inHub = false;
        }

        @Override
        public boolean isNull() {
            return !values.isEmpty() && values.peek() == null;
        }
    }

    static class TypeCheckingDeserializer implements OADeserializer {
        @Override
        public <T extends OAObject> T readObject(Class<T> type, OASerializeReader reader, OASerializeContext context) {
            if (type == null) throw new IllegalArgumentException("type required");
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public <T extends OAObject> Hub<T> readHub(Class<T> type, OASerializeReader reader, OASerializeContext context) {
            if (type == null) throw new IllegalArgumentException("type required");
            return new Hub<>(type);
        }
    }

    @Test
    void readerThrowsOnOutOfOrderObjectLifecycleInTestHarness() {
        StrictReader reader = new StrictReader();

        assertThrows(IllegalStateException.class, reader::endObject);

        reader.beginObject();
        assertThrows(IllegalStateException.class, reader::beginObject);
        reader.endObject();
    }

    @Test
    void readerThrowsOnOutOfOrderHubLifecycleInTestHarness() {
        StrictReader reader = new StrictReader();

        assertThrows(IllegalStateException.class, reader::endHub);

        reader.beginHub();
        assertThrows(IllegalStateException.class, reader::beginHub);
        reader.endHub();
    }

    @Test
    void readerNameValueOrderingIsExplicit() {
        StrictReader reader = new StrictReader().add("a", 1).add("b", null);

        assertEquals("a", reader.nextName());
        assertEquals(1, reader.nextValue());

        assertEquals("b", reader.nextName());
        assertTrue(reader.isNull());
        assertNull(reader.nextValue());

        assertFalse(reader.hasNext());
    }

    @Test
    void deserializerTypeNullFailsVisibly() {
        TypeCheckingDeserializer d = new TypeCheckingDeserializer();

        assertThrows(IllegalArgumentException.class, () -> d.readObject(null, new StrictReader(), new OASerializeContext()));
        assertThrows(IllegalArgumentException.class, () -> d.readHub(null, new StrictReader(), new OASerializeContext()));
    }

    @Test
    void deserializerCreatesRequestedTypeAndHubType() {
        TypeCheckingDeserializer d = new TypeCheckingDeserializer();

        Item item = d.readObject(Item.class, new StrictReader(), new OASerializeContext());
        Hub<Item> hub = d.readHub(Item.class, new StrictReader(), new OASerializeContext());

        assertNotNull(item);
        assertEquals(Item.class, hub.getObjectClass());
    }

    @Test
    void writerInterfaceCanRemainMarkerUntilConcreteFormatsImplementCallbacks() {
        OASerializeWriter writer = new OASerializeWriter() {
        };

        assertNotNull(writer);
    }
}
