package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Item;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

class OADeserializerTest {

    @Test
    void readObjectReceivesTypeReaderAndContext() {
        RecordingDeserializer deserializer = new RecordingDeserializer();
        OASerializeReader reader = new EmptyReader();
        OASerializeContext context = new OASerializeContext();

        Item item = deserializer.readObject(Item.class, reader, context);

        assertSame(Item.class, deserializer.type);
        assertSame(reader, deserializer.reader);
        assertSame(context, deserializer.context);
        assertNotNull(item);
    }

    @Test
    void readHubReceivesTypeReaderAndContext() {
        RecordingDeserializer deserializer = new RecordingDeserializer();
        OASerializeReader reader = new EmptyReader();
        OASerializeContext context = new OASerializeContext();

        Hub<Item> hub = deserializer.readHub(Item.class, reader, context);

        assertSame(Item.class, deserializer.type);
        assertSame(reader, deserializer.reader);
        assertSame(context, deserializer.context);
        assertSame(Item.class, hub.getObjectClass());
    }

    private static class RecordingDeserializer implements OADeserializer {
        Class<?> type;
        OASerializeReader reader;
        OASerializeContext context;

        @Override
        public <T extends OAObject> T readObject(Class<T> type, OASerializeReader reader, OASerializeContext context) {
            this.type = type;
            this.reader = reader;
            this.context = context;
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public <T extends OAObject> Hub<T> readHub(Class<T> type, OASerializeReader reader, OASerializeContext context) {
            this.type = type;
            this.reader = reader;
            this.context = context;
            return new Hub<>(type);
        }
    }

    private static class EmptyReader implements OASerializeReader {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public String nextName() {
            return null;
        }

        @Override
        public Object nextValue() {
            return null;
        }

        @Override
        public void beginObject() {
        }

        @Override
        public void endObject() {
        }

        @Override
        public void beginHub() {
        }

        @Override
        public void endHub() {
        }

        @Override
        public boolean isNull() {
            return true;
        }
    }
}
