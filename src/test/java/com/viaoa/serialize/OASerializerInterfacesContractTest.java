package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.Queue;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASerializerInterfacesContractTest {

    static class Item extends OAObject {
    }

    static class RecordingSerializer implements OASerializer {
        OAObject object;
        Hub<?> hub;
        OASerializeWriter writer;
        OASerializeContext context;

        @Override
        public void writeObject(OAObject obj, OASerializeWriter writer, OASerializeContext context) {
            this.object = obj;
            this.writer = writer;
            this.context = context;
            context.markWritten(obj);
        }

        @Override
        public void writeHub(Hub<?> hub, OASerializeWriter writer, OASerializeContext context) {
            this.hub = hub;
            this.writer = writer;
            this.context = context;
        }
    }

    static class RecordingDeserializer implements OADeserializer {
        @Override
        public <T extends OAObject> T readObject(Class<T> type, OASerializeReader reader, OASerializeContext context) {
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public <T extends OAObject> Hub<T> readHub(Class<T> type, OASerializeReader reader, OASerializeContext context) {
            return new Hub<>(type);
        }
    }

    static class DummyWriter implements OASerializeWriter {
    }

    static class QueueReader implements OASerializeReader {
        private final Queue<String> names = new ArrayDeque<>();
        private final Queue<Object> values = new ArrayDeque<>();
        boolean objectStarted;
        boolean hubStarted;

        QueueReader add(String name, Object value) {
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
            return names.remove();
        }

        @Override
        public Object nextValue() {
            return values.remove();
        }

        @Override
        public void beginObject() {
            objectStarted = true;
        }

        @Override
        public void endObject() {
            objectStarted = false;
        }

        @Override
        public void beginHub() {
            hubStarted = true;
        }

        @Override
        public void endHub() {
            hubStarted = false;
        }

        @Override
        public boolean isNull() {
            return !values.isEmpty() && values.peek() == null;
        }
    }

    @Test
    void serializerInterfacePassesObjectWriterAndContext() {
        RecordingSerializer ser = new RecordingSerializer();
        OASerializeContext ctx = new OASerializeContext();
        DummyWriter writer = new DummyWriter();
        Item item = new Item();

        ser.writeObject(item, writer, ctx);

        assertSame(item, ser.object);
        assertSame(writer, ser.writer);
        assertSame(ctx, ser.context);
        assertTrue(ctx.hasWritten(item));
    }

    @Test
    void serializerInterfacePassesHubWriterAndContext() {
        RecordingSerializer ser = new RecordingSerializer();
        OASerializeContext ctx = new OASerializeContext();
        DummyWriter writer = new DummyWriter();
        Hub<Item> hub = new Hub<>(Item.class);

        ser.writeHub(hub, writer, ctx);

        assertSame(hub, ser.hub);
        assertSame(writer, ser.writer);
        assertSame(ctx, ser.context);
    }

    @Test
    void deserializerInterfaceCanCreateTypedObjectAndHub() {
        RecordingDeserializer deser = new RecordingDeserializer();
        OASerializeContext ctx = new OASerializeContext();
        QueueReader reader = new QueueReader();

        Item item = deser.readObject(Item.class, reader, ctx);
        Hub<Item> hub = deser.readHub(Item.class, reader, ctx);

        assertNotNull(item);
        assertEquals(Item.class, hub.getObjectClass());
    }

    @Test
    void readerContractCanStreamNamesAndValuesInOrder() {
        QueueReader reader = new QueueReader().add("a", 1).add("b", null);

        assertTrue(reader.hasNext());
        assertEquals("a", reader.nextName());
        assertEquals(1, reader.nextValue());

        assertTrue(reader.hasNext());
        assertEquals("b", reader.nextName());
        assertTrue(reader.isNull());
        assertNull(reader.nextValue());

        assertFalse(reader.hasNext());
    }

    @Test
    void readerBeginEndObjectAndHubStateRoundTrip() {
        QueueReader reader = new QueueReader();

        reader.beginObject();
        assertTrue(reader.objectStarted);
        reader.endObject();
        assertFalse(reader.objectStarted);

        reader.beginHub();
        assertTrue(reader.hubStarted);
        reader.endHub();
        assertFalse(reader.hubStarted);
    }
}
