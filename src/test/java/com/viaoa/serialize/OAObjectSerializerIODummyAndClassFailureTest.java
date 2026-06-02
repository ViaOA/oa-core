package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import com.viaoa.comm.io.IODummy;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectSerializerIODummyAndClassFailureTest {

    static class Item extends OAObject {
    }

    @Test
    void getObjectThrowsWhenResolvedObjectIsIODummy() throws Exception {
        OAObjectSerializer<Object> ser = new OAObjectSerializer<>(null, false);
        setObject(ser, new IODummy());

        RuntimeException ex = assertThrows(RuntimeException.class, ser::getObject);

        assertTrue(ex.getMessage().contains("class not found"));
    }

    @Test
    void parentWrapperIODummyAlsoThrows() throws Exception {
        OAObjectSerializer<Object> parent = new OAObjectSerializer<>(null, false);
        setObject(parent, new IODummy());

        OAObjectSerializer<Object> child = new OAObjectSerializer<>(new Object(), false);
        setParent(child, parent);

        RuntimeException ex = assertThrows(RuntimeException.class, child::getObject);

        assertTrue(ex.getMessage().contains("class not found"));
    }

    @Test
    void getExtraObjectDoesNotPerformIODummyCheckCurrentContract() throws Exception {
        OAObjectSerializer<Object> ser = new OAObjectSerializer<>(new Object(), false);
        IODummy dummy = new IODummy();

        ser.setExtraObject(dummy);

        assertSame(dummy, ser.getExtraObject());
    }

    @Test
    void nullWrappedObjectStillReturnsNull() {
        OAObjectSerializer<Object> ser = new OAObjectSerializer<>(null, false);

        assertNull(ser.getObject());
    }

    private static void setObject(OAObjectSerializer<?> ser, Object value) throws Exception {
        Field f = OAObjectSerializer.class.getDeclaredField("object");
        f.setAccessible(true);
        f.set(ser, value);
    }

    private static void setParent(OAObjectSerializer<?> child, OAObjectSerializer<?> parent) throws Exception {
        Field f = OAObjectSerializer.class.getDeclaredField("parentWrapper");
        f.setAccessible(true);
        f.set(child, parent);
    }
}
