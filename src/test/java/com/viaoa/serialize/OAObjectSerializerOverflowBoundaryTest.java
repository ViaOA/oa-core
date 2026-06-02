package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.LinkedList;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectSerializerOverflowBoundaryTest {

    static class Item extends OAObject {
    }

    private final OAObjectSerializer.FriendAccess friend = OAObjectSerializer.getFriendAccess();

    @Test
    void referenceAtOverflowLimitIsDeferredAndNotSerializedImmediately() throws Exception {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        setIntField(ser, "overflowLimit", 1);

        Item parent = new Item();
        Item child = new Item();

        friend.beforeSerialize(parent, ser);

        boolean decision = friend.shouldSerializeReference(ser, parent, "child", child, null);

        assertFalse(decision);
        LinkedList<?> list = (LinkedList<?>) getField(ser, "listOverflow");
        assertNotNull(list);
        assertEquals(1, list.size());

        friend.afterSerialize(parent, ser);
    }

    @Test
    void nullReferenceAtOverflowLimitIsNotDeferred() throws Exception {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        setIntField(ser, "overflowLimit", 1);

        Item parent = new Item();

        friend.beforeSerialize(parent, ser);

        assertTrue(friend.shouldSerializeReference(ser, parent, "child", null, null));
        assertNull(getField(ser, "listOverflow"));

        friend.afterSerialize(parent, ser);
    }

    @Test
    void overflowRecordCapturesParentPropertyAndStackSnapshot() throws Exception {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        setIntField(ser, "overflowLimit", 1);

        Item parent = new Item();
        Item child = new Item();

        friend.beforeSerialize(parent, ser);
        friend.shouldSerializeReference(ser, parent, "child", child, null);

        LinkedList<?> list = (LinkedList<?>) getField(ser, "listOverflow");
        Object overflow = list.getFirst();

        assertSame(parent, getOverflowField(overflow, "parentObject"));
        assertEquals("child", getOverflowField(overflow, "property"));
        assertSame(child, getOverflowField(overflow, "object"));
        assertNotNull(getOverflowField(overflow, "stack"));
        assertEquals(1, getOverflowField(overflow, "levelsDeep"));

        friend.afterSerialize(parent, ser);
    }

    @Test
    void multipleOverflowRecordsPreserveInsertionOrder() throws Exception {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        setIntField(ser, "overflowLimit", 1);

        Item parent = new Item();
        Item a = new Item();
        Item b = new Item();

        friend.beforeSerialize(parent, ser);
        friend.shouldSerializeReference(ser, parent, "a", a, null);
        friend.shouldSerializeReference(ser, parent, "b", b, null);

        LinkedList<?> list = (LinkedList<?>) getField(ser, "listOverflow");
        assertEquals(2, list.size());
        assertEquals("a", getOverflowField(list.get(0), "property"));
        assertEquals("b", getOverflowField(list.get(1), "property"));

        friend.afterSerialize(parent, ser);
    }

    @Test
    void overflowLimitCanBeRaisedToAllowSameDepthReference() throws Exception {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        setIntField(ser, "overflowLimit", 2);

        Item parent = new Item();

        friend.beforeSerialize(parent, ser);

        assertTrue(friend.shouldSerializeReference(ser, parent, "child", new Item(), null));
        assertNull(getField(ser, "listOverflow"));

        friend.afterSerialize(parent, ser);
    }

    private static void setIntField(Object obj, String name, int value) throws Exception {
        Field f = OAObjectSerializer.class.getDeclaredField(name);
        f.setAccessible(true);
        f.setInt(obj, value);
    }

    private static Object getField(Object obj, String name) throws Exception {
        Field f = OAObjectSerializer.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(obj);
    }

    private static Object getOverflowField(Object obj, String name) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(obj);
    }
}
