package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.Method;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAMetadataSerializationExtendedTest {

    public static class Bean extends OAObject {
        public String getName() { return "name"; }
        public void doWork() { }
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(value);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            return (T) in.readObject();
        }
    }

    @Test
    void propertyInfoTransientCallbackMethodIsNotSerialized() throws Exception {
        OAPropertyInfo pi = new OAPropertyInfo();
        pi.setName("Name");
        pi.setClassType(String.class);
        pi.setObjectCallbackMethod(Bean.class.getMethod("getName"));

        OAPropertyInfo copy = roundTrip(pi);

        assertEquals("Name", copy.getName());
        assertEquals(String.class, copy.getClassType());
        assertNull(copy.getObjectCallbackMethod());
    }

    @Test
    void calcInfoTransientCallbackMethodIsNotSerialized() throws Exception {
        OACalcInfo ci = new OACalcInfo("NameCalc", new String[] { "name" });
        ci.setObjectCallbackMethod(Bean.class.getMethod("getName"));
        ci.setClassType(String.class);

        OACalcInfo copy = roundTrip(ci);

        assertEquals("NameCalc", copy.getName());
        assertArrayEquals(new String[] { "name" }, copy.getDependentProperties());
        assertEquals(String.class, copy.getClassType());
        assertNull(copy.getObjectCallbackMethod());
    }

    @Test
    void methodInfoTransientCallbackMethodIsNotSerialized() throws Exception {
        OAMethodInfo mi = new OAMethodInfo();
        mi.setName("doWork");
        mi.setObjectCallbackMethod(Bean.class.getMethod("doWork"));

        OAMethodInfo copy = roundTrip(mi);

        assertEquals("doWork", copy.getName());
        assertNull(copy.getObjectCallbackMethod());
    }

    @Test
    void objectInfoSerializationPreservesBasicMetadataAndDropsTransientCallback() throws Exception {
        OAObjectInfo oi = new OAObjectInfo(new String[] { "id" });
        oi.setName("Bean");
        oi.setForClass(Bean.class);
        oi.setObjectCallbackMethod(Bean.class.getMethod("doWork"));

        OAPropertyInfo pi = new OAPropertyInfo();
        pi.setName("Name");
        pi.setClassType(String.class);
        oi.addPropertyInfo(pi);

        OAObjectInfo copy = roundTrip(oi);

        assertEquals("Bean", copy.getName());
        assertArrayEquals(new String[] { "id" }, copy.getIdProperties());
        assertEquals(Bean.class, copy.getForClass());
        assertNotNull(copy.getPropertyInfo("name"));
        assertNull(copy.getObjectCallbackMethod());
    }

    @Test
    void objectModelSerializationPreservesFlagsAndNames() throws Exception {
        OAObjectModel m = new OAObjectModel();
        m.defaultAll(false);
        m.setDisplayName("Order");
        m.setPluralDisplayName("Orders");
        m.setAllowAdd(true);
        m.setAllowSave(true);

        OAObjectModel copy = roundTrip(m);

        assertEquals("Order", copy.getDisplayName());
        assertEquals("Orders", copy.getPluralDisplayName());
        assertTrue(copy.getAllowAdd());
        assertTrue(copy.getAllowSave());
        assertFalse(copy.getAllowDelete());
    }
}
