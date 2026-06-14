package com.viaoa.func;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class StringCallbackTest {

    @Test
    void addReceivesStringMessagesInCallOrder() {
        List<String> messages = new ArrayList<>();
        StringCallback callback = messages::add;

        callback.add("first");
        callback.add(null);
        callback.add("third");

        assertEquals(3, messages.size());
        assertEquals("first", messages.get(0));
        assertNull(messages.get(1));
        assertEquals("third", messages.get(2));
    }
}
