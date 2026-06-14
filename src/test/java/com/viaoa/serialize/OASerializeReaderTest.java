package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.Queue;

import org.junit.jupiter.api.Test;

class OASerializeReaderTest {

    @Test
    void hasNextAndNextMethodsExposeReaderEventsInOrder() {
        RecordingReader reader = new RecordingReader();
        reader.add("id", 123);
        reader.add("name", "Brake Pad");

        assertTrue(reader.hasNext());
        assertEquals("id", reader.nextName());
        assertEquals(123, reader.nextValue());
        assertTrue(reader.hasNext());
        assertEquals("name", reader.nextName());
        assertEquals("Brake Pad", reader.nextValue());
        assertFalse(reader.hasNext());
    }

    @Test
    void beginAndEndObjectHubCallbacksArePartOfReaderContract() {
        RecordingReader reader = new RecordingReader();

        reader.beginObject();
        reader.beginHub();
        reader.endHub();
        reader.endObject();

        assertEquals("beginObject,beginHub,endHub,endObject", reader.events.toString());
    }

    @Test
    void isNullReflectsCurrentValue() {
        RecordingReader reader = new RecordingReader();
        reader.add("value", null);

        assertFalse(reader.isNull());
        assertEquals("value", reader.nextName());
        assertNull(reader.nextValue());
        assertTrue(reader.isNull());
    }

    private static class RecordingReader implements OASerializeReader {
        private final Queue<Entry> entries = new ArrayDeque<>();
        private final StringBuilder events = new StringBuilder();
        private Entry current;

        void add(String name, Object value) {
            entries.add(new Entry(name, value));
        }

        @Override
        public boolean hasNext() {
            return !entries.isEmpty();
        }

        @Override
        public String nextName() {
            current = entries.remove();
            return current.name;
        }

        @Override
        public Object nextValue() {
            return current.value;
        }

        @Override
        public void beginObject() {
            append("beginObject");
        }

        @Override
        public void endObject() {
            append("endObject");
        }

        @Override
        public void beginHub() {
            append("beginHub");
        }

        @Override
        public void endHub() {
            append("endHub");
        }

        @Override
        public boolean isNull() {
            return current != null && current.value == null;
        }

        private void append(String event) {
            if (events.length() > 0) {
                events.append(',');
            }
            events.append(event);
        }
    }

    private static class Entry {
        final String name;
        final Object value;

        Entry(String name, Object value) {
            this.name = name;
            this.value = value;
        }
    }
}
