package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectSerializerStackAndLifecycleTest {

    static class Item extends OAObject {
    }

    private final OAObjectSerializer.FriendAccess friend = OAObjectSerializer.getFriendAccess();

    @Test
    void stackDefaultsEmptyAndPreviousObjectIsNull() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);

        assertEquals(0, ser.getStackSize());
        assertNull(ser.getPreviousObject());
        assertNull(ser.getStackObject(0));
        assertEquals(0, ser.getLevelsDeep());
    }

    @Test
    void beforeAfterSerializeMaintainsStackAndDepth() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        Item a = new Item();

        friend.beforeSerialize(a, ser);

        assertEquals(1, ser.getStackSize());
        assertEquals(a, ser.getPreviousObject());
        assertSame(a, ser.getStackObject(0));
        assertEquals(1, ser.getLevelsDeep());
        assertEquals(1, ser.getTotalObjectsWritten());

        friend.afterSerialize(a, ser);

        assertEquals(0, ser.getStackSize());
        assertNull(ser.getPreviousObject());
        assertEquals(0, ser.getLevelsDeep());
    }

    @Test
    void nestedBeforeAfterSerializeUsesLastInFirstOutStack() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        Item a = new Item();
        Item b = new Item();

        friend.beforeSerialize(a, ser);
        friend.beforeSerialize(b, ser);

        assertEquals(2, ser.getStackSize());
        assertSame(b, ser.getStackObject(0));
        assertSame(a, ser.getStackObject(1));
        assertNull(ser.getStackObject(2));
        assertEquals(2, ser.getLevelsDeep());

        friend.afterSerialize(b, ser);

        assertEquals(1, ser.getStackSize());
        assertSame(a, ser.getStackObject(0));
        assertEquals(1, ser.getLevelsDeep());

        friend.afterSerialize(a, ser);

        assertEquals(0, ser.getStackSize());
        assertEquals(0, ser.getLevelsDeep());
    }

    @Test
    void afterSerializeWithoutMatchingBeforeFailsVisibly() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);

        assertThrows(RuntimeException.class, () -> friend.afterSerialize(new Item(), ser));
    }

    @Test
    void totalObjectsWrittenCountsBeforeSerializeCallsOnly() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);

        friend.beforeSerialize(new Item(), ser);
        friend.beforeSerialize(new Item(), ser);

        assertEquals(2, ser.getTotalObjectsWritten());

        friend.afterSerialize(new Item(), ser);
        friend.afterSerialize(new Item(), ser);

        assertEquals(2, ser.getTotalObjectsWritten());
    }

    @Test
    void hasReachedMaxBecomesStickyAfterReached() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        ser.setMax(1);

        assertFalse(ser.hasReachedMax());

        friend.beforeSerialize(new Item(), ser);

        assertTrue(ser.hasReachedMax());

        ser.setMax(1000);

        assertTrue(ser.hasReachedMax(), "reached max flag is sticky for the serializer operation");
    }
}
