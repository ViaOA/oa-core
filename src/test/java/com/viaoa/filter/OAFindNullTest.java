package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAFindNullTest {


    @Test
    void findNullReportsNullRootAndClearsStateBetweenRuns() throws Exception {
        RecordingFindNull finder = new RecordingFindNull();

        assertTrue(finder.findNull(null));
        assertEquals("", finder.lastPath);

        assertFalse(finder.findNull("value"));
    }

    @Test
    void foundOneCanBeOverriddenToRecordPath() throws Exception {
        RecordingFindNull finder = new RecordingFindNull();
        Holder holder = new Holder();

        assertTrue(finder.findNull(holder));
        assertEquals("OAFindNullTest$Holder.value", finder.lastPath);
    }

    static class Holder {
        Object value;
    }

    static class RecordingFindNull extends OAFindNull {
        String lastPath;

        @Override
        public boolean foundOne(String propertyPath) {
            lastPath = propertyPath;
            return true;
        }
    }
}
