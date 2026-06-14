package com.viaoa.runtime.thread;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAThreadLocalHubMergerCallbackTest {
    @Test
    void callbackInterfaceCanBeImplemented() {
        OAThreadLocalHubMergerCallback callback = new OAThreadLocalHubMergerCallback() {
            @Override
            public void callback() {
            }
        };

        assertDoesNotThrow(callback::callback);
    }
}
