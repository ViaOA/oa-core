package com.viaoa.comm.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

class OAObjectInputStreamTest {

    @Test
    void constructorReadsStandardSerializedObjects() throws Exception {
        byte[] bytes = serialize("value");

        try (OAObjectInputStream in = new OAObjectInputStream(new ByteArrayInputStream(bytes))) {
            assertEquals("value", in.readObject());
        }
    }

    @Test
    void constructorWithOldPackageReadsWhenNoRemapIsNeeded() throws Exception {
        byte[] bytes = serialize("value");

        try (OAObjectInputStream in = new OAObjectInputStream(new ByteArrayInputStream(bytes), "old.package")) {
            assertEquals("value", in.readObject());
        }
    }

    @Test
    void constructorWithOldAndNewPackageReadsWhenNoRemapIsNeeded() throws Exception {
        byte[] bytes = serialize("value");

        try (OAObjectInputStream in = new OAObjectInputStream(new ByteArrayInputStream(bytes), "old.package", "new.package")) {
            assertEquals("value", in.readObject());
        }
    }

    @Test
    void replaceClassNameCanBeRegisteredBeforeReadingUnchangedStream() throws Exception {
        byte[] bytes = serialize("value");

        try (OAObjectInputStream in = new OAObjectInputStream(new ByteArrayInputStream(bytes), "old.package", "new.package")) {
            in.replaceClassName("OldName", "NewName");
            assertEquals("value", in.readObject());
        }
    }

    private static byte[] serialize(Object obj) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bout)) {
            out.writeObject(obj);
        }
        return bout.toByteArray();
    }

}
