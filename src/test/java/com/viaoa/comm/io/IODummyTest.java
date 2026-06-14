package com.viaoa.comm.io;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.viaoa.annotation.OAClass;
import com.viaoa.object.OAObject;

class IODummyTest {

    @Test
    void classExtendsOAObject() {
        assertTrue(OAObject.class.isAssignableFrom(IODummy.class));
    }

    @Test
    void classHasSerializationFallbackAnnotationSettings() {
        OAClass annotation = IODummy.class.getAnnotation(OAClass.class);

        assertNotNull(annotation);
        assertFalse(annotation.addToCache());
        assertFalse(annotation.initialize());
        assertTrue(annotation.localOnly());
        assertFalse(annotation.useDataSource());
    }
}
