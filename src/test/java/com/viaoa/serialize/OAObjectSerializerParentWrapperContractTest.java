package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectSerializerParentWrapperContractTest {

    static class Item extends OAObject {
        private String name;
        Item() { }
        Item(String name) { this.name = name; }
        public String getName() { return name; }
    }

    private final OAObjectSerializer.FriendAccess friend = OAObjectSerializer.getFriendAccess();

    @Test
    void childWrapperDelegatesGetObjectAndExtraObjectToParent() throws Exception {
        Item root = new Item("root");
        Item extra = new Item("extra");

        OAObjectSerializer<Item> parent = new OAObjectSerializer<>(root, extra, false, null);
        OAObjectSerializer<Item> child = new OAObjectSerializer<>(new Item("child"), false);

        setParent(child, parent);

        assertSame(root, child.getObject());
        assertSame(extra, child.getExtraObject());
    }

    @Test
    void childWrapperReferenceDecisionDelegatesToParentIncludeExcludeRules() throws Exception {
        OAObjectSerializer<Item> parent = new OAObjectSerializer<>(new Item("root"), false);
        parent.excludeProperties(new String[] { "blocked" });

        OAObjectSerializer<Item> child = new OAObjectSerializer<>(new Item("child"), false);
        child.includeAllProperties();

        setParent(child, parent);

        assertFalse(friend.shouldSerializeReference(child, new Item(), "blocked", new Item(), null));
        assertTrue(friend.shouldSerializeReference(child, new Item(), "allowed", new Item(), null));
    }

    @Test
    void childWrapperGetExtraObjectReflectsParentMutations() throws Exception {
        OAObjectSerializer<Item> parent = new OAObjectSerializer<>(new Item("root"), false);
        OAObjectSerializer<Item> child = new OAObjectSerializer<>(new Item("child"), false);
        setParent(child, parent);

        Item extra = new Item("extra");
        parent.setExtraObject(extra);

        assertSame(extra, child.getExtraObject());
    }

    @Test
    void nestedParentDelegationFindsRootParentObject() throws Exception {
        OAObjectSerializer<Item> root = new OAObjectSerializer<>(new Item("root"), false);
        OAObjectSerializer<Item> mid = new OAObjectSerializer<>(new Item("mid"), false);
        OAObjectSerializer<Item> leaf = new OAObjectSerializer<>(new Item("leaf"), false);

        setParent(mid, root);
        setParent(leaf, mid);

        assertEquals("root", leaf.getObject().getName());
    }

    private static void setParent(OAObjectSerializer<?> child, OAObjectSerializer<?> parent) throws Exception {
        Field f = OAObjectSerializer.class.getDeclaredField("parentWrapper");
        f.setAccessible(true);
        f.set(child, parent);
    }
}
