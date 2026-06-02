package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAMetadataSerializationTest {

    public static class Child extends OAObject {
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
    void propertyInfoSerializesNonTransientState() throws Exception {
        OAPropertyInfo pi = new OAPropertyInfo();
        pi.setName("Name");
        pi.setClassType(String.class);
        pi.setRequired(true);
        pi.setMaxLength(40);
        pi.setFormat("upper");

        OAPropertyInfo copy = roundTrip(pi);

        assertEquals("Name", copy.getName());
        assertEquals(String.class, copy.getClassType());
        assertTrue(copy.getRequired());
        assertEquals(40, copy.getMaxLength());
        assertEquals("upper", copy.getFormat());
        assertNull(copy.getObjectCallbackMethod());
    }

    @Test
    void calcInfoSerializesNonTransientState() throws Exception {
        OACalcInfo ci = new OACalcInfo("Total", new String[] { "lines.amount" }, true);
        ci.setClassType(Double.class);
        ci.setHtml(true);

        OACalcInfo copy = roundTrip(ci);

        assertEquals("Total", copy.getName());
        assertArrayEquals(new String[] { "lines.amount" }, copy.getDependentProperties());
        assertTrue(copy.getIsForHub());
        assertEquals(Double.class, copy.getClassType());
        assertTrue(copy.isHtml());
        assertNull(copy.getObjectCallbackMethod());
    }

    @Test
    void methodInfoSerializesNonTransientState() throws Exception {
        OAMethodInfo mi = new OAMethodInfo();
        mi.setName("DoWork");
        mi.setEnabledProperty("enabled");
        mi.setEnabledValue(true);

        OAMethodInfo copy = roundTrip(mi);

        assertEquals("DoWork", copy.getName());
        assertEquals("enabled", copy.getEnabledProperty());
        assertTrue(copy.getEnabledValue());
        assertNull(copy.getObjectCallbackMethod());
    }

    @Test
    void fkeyInfoSerializesPropertyInfoReferences() throws Exception {
        OAPropertyInfo from = new OAPropertyInfo();
        from.setName("childId");
        OAPropertyInfo to = new OAPropertyInfo();
        to.setName("id");

        OAFkeyInfo fki = new OAFkeyInfo();
        fki.setFromPropertyInfo(from);
        fki.setToPropertyInfo(to);

        OAFkeyInfo copy = roundTrip(fki);

        assertEquals("childId", copy.getFromPropertyInfo().getName());
        assertEquals("id", copy.getToPropertyInfo().getName());
    }
}
