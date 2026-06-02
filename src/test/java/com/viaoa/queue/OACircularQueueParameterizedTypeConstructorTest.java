package com.viaoa.queue;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class OACircularQueueParameterizedTypeConstructorTest {

    static class StringListQueue extends OACircularQueue<List<String>> {
        StringListQueue() {
            super();
            setSize(3);
        }
    }

    static class GenericBase<T> extends OACircularQueue<T> {
        GenericBase(Class<T> clazz, int size) {
            super(clazz, size);
        }
    }

    static class StringGenericQueue extends GenericBase<String> {
        StringGenericQueue() {
            super(String.class, 3);
        }
    }

    @Test
    void explicitClassConstructorWorksThroughGenericIntermediateSuperclass() throws Exception {
        StringGenericQueue q = new StringGenericQueue();
        long pos = q.getHeadPostion();

        q.addMessage("A");

        assertArrayEquals(new String[] { "A" }, q.getMessages(pos, 10, 0));
    }

    @Test
    void parameterizedMessageTypeConstructorFailsClearlyOrSupportsParameterizedTypeDesiredContract() {
        try {
            StringListQueue q = new StringListQueue();
            assertEquals(3, q.getSize());
        } catch (ClassCastException ex) {
            fail("generic type discovery should not throw ClassCastException for parameterized TYPE; fail clearly or support it");
        } catch (RuntimeException ex) {
            assertNotNull(ex.getMessage());
        }
    }
}
