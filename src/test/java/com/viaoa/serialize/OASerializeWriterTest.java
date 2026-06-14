package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OASerializeWriterTest {

    @Test
    void writerIsMarkerContractForCurrentFirstPassApi() {
        OASerializeWriter writer = new RecordingWriter();

        assertNotNull(writer);
        assertTrue(writer instanceof OASerializeWriter);
    }

    private static class RecordingWriter implements OASerializeWriter {
    }
}
