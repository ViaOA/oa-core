package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Item;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

class OASerializerTest {

    @Test
    void writeObjectReceivesObjectWriterAndContext() {
        RecordingSerializer serializer = new RecordingSerializer();
        Item item = new Item(1);
        OASerializeWriter writer = new OASerializeWriter() {
        };
        OASerializeContext context = new OASerializeContext();

        serializer.writeObject(item, writer, context);

        assertSame(item, serializer.object);
        assertSame(writer, serializer.writer);
        assertSame(context, serializer.context);
    }

    @Test
    void writeHubReceivesHubWriterAndContext() {
        RecordingSerializer serializer = new RecordingSerializer();
        Hub<Item> hub = new Hub<>(Item.class);
        OASerializeWriter writer = new OASerializeWriter() {
        };
        OASerializeContext context = new OASerializeContext();

        serializer.writeHub(hub, writer, context);

        assertSame(hub, serializer.hub);
        assertSame(writer, serializer.writer);
        assertSame(context, serializer.context);
    }

    private static class RecordingSerializer implements OASerializer {
        OAObject object;
        Hub<?> hub;
        OASerializeWriter writer;
        OASerializeContext context;

        @Override
        public void writeObject(OAObject obj, OASerializeWriter writer, OASerializeContext context) {
            this.object = obj;
            this.writer = writer;
            this.context = context;
        }

        @Override
        public void writeHub(Hub<?> hub, OASerializeWriter writer, OASerializeContext context) {
            this.hub = hub;
            this.writer = writer;
            this.context = context;
        }
    }
}
