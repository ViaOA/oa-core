package com.viaoa.replication;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.viaoa.datetime.OADateTime;

class OAReplTLogTest {

    @Test
    void constructorStoresAllFields() {
        OADateTime dt = new OADateTime(1_777_777_777L);
        Object[] args = { Register.class, "value" };

        OAReplTLog tlog = new OAReplTLog("store-1", dt, 11L, 22L, "propertyChange", args);

        assertEquals("store-1", tlog.getSource());
        assertSame(dt, tlog.getDt());
        assertEquals(11L, tlog.getMasterSeq());
        assertEquals(22L, tlog.getClientSeq());
        assertEquals("propertyChange", tlog.getMethodName());
        assertSame(args, tlog.getArgs());
    }

    @Test
    void setSourceUpdatesSource() {
        OAReplTLog tlog = newTLog();

        tlog.setSource("client-2");

        assertEquals("client-2", tlog.getSource());
    }

    @Test
    void setDtUpdatesDatetime() {
        OAReplTLog tlog = newTLog();
        OADateTime dt = new OADateTime(2_000L);

        tlog.setDt(dt);

        assertSame(dt, tlog.getDt());
    }

    @Test
    void setMasterSeqUpdatesMasterSeq() {
        OAReplTLog tlog = newTLog();

        tlog.setMasterSeq(9L);

        assertEquals(9L, tlog.getMasterSeq());
    }

    @Test
    void setClientSeqUpdatesClientSeq() {
        OAReplTLog tlog = newTLog();

        tlog.setClientSeq(7L);

        assertEquals(7L, tlog.getClientSeq());
    }

    @Test
    void setMethodNameUpdatesMethodName() {
        OAReplTLog tlog = newTLog();

        tlog.setMethodName("removeFromHub");

        assertEquals("removeFromHub", tlog.getMethodName());
    }

    @Test
    void setArgsUpdatesArgs() {
        OAReplTLog tlog = newTLog();
        Object[] args = { "x", 3 };

        tlog.setArgs(args);

        assertSame(args, tlog.getArgs());
    }

    @Test
    void serializesAndDeserializesReplicationMessage() throws Exception {
        OADateTime dt = new OADateTime(3_000L);
        OAReplTLog tlog = new OAReplTLog("source", dt, 4L, 5L, "refresh", new Object[] { Register.class });

        OAReplTLog copy = roundTrip(tlog);

        assertEquals("source", copy.getSource());
        assertEquals(dt.getTime(), copy.getDt().getTime());
        assertEquals(4L, copy.getMasterSeq());
        assertEquals(5L, copy.getClientSeq());
        assertEquals("refresh", copy.getMethodName());
        assertArrayEquals(new Object[] { Register.class }, copy.getArgs());
    }

    private static OAReplTLog newTLog() {
        return new OAReplTLog("source", new OADateTime(1_000L), 1L, 2L, "method", new Object[] { "arg" });
    }

    @SuppressWarnings("unchecked")
    private static <T extends Serializable> T roundTrip(T obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            return (T) ois.readObject();
        }
    }
}
