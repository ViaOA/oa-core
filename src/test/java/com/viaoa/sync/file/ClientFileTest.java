package com.viaoa.sync.file;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;

class ClientFileTest {

    @Test
    void downloadReturnsFalseForEmptyNameOrNullFileWithoutUsingMultiplexer() throws Exception {
        ClientFile cf = new ClientFile();

        assertFalse(cf.download(null, new File("ignored"), null));
        assertFalse(cf.download("", new File("ignored"), null));
        assertFalse(cf.download("remote.txt", null, null));
    }

    @Test
    void uploadReturnsFalseForEmptyNameOrNullFileWithoutUsingMultiplexer() throws Exception {
        ClientFile cf = new ClientFile();

        assertFalse(cf.upload(null, new File("ignored"), null));
        assertFalse(cf.upload("", new File("ignored"), null));
        assertFalse(cf.upload("remote.txt", null, null));
    }
}
