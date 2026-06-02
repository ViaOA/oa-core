package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectSerializerOptionDeterminismTest {

    static class Item extends OAObject {
    }

    static class Other extends OAObject {
    }

    private final OAObjectSerializer.FriendAccess friend = OAObjectSerializer.getFriendAccess();

    @Test
    void includeThenExcludeThenIncludeAllProducesDeterministicReferenceDecisions() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);

        ser.includeProperties(new String[] { "a" });
        assertTrue(friend.shouldSerializeReference(ser, new Item(), "a", new Item(), null));
        assertFalse(friend.shouldSerializeReference(ser, new Item(), "b", new Item(), null));

        ser.excludeProperties(new String[] { "a" });
        assertFalse(friend.shouldSerializeReference(ser, new Item(), "a", new Item(), null));
        assertTrue(friend.shouldSerializeReference(ser, new Item(), "b", new Item(), null));

        ser.includeAllProperties();
        assertTrue(friend.shouldSerializeReference(ser, new Item(), "a", new Item(), null));
        assertTrue(friend.shouldSerializeReference(ser, new Item(), "b", new Item(), null));

        ser.excludeAllProperties();
        assertFalse(friend.shouldSerializeReference(ser, new Item(), "a", new Item(), null));
        assertFalse(friend.shouldSerializeReference(ser, new Item(), "b", new Item(), null));
    }

    @Test
    void excludedReferencesCanBeReplacedByLaterCall() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);

        ser.setExcludedReferences(Other.class);
        assertFalse(friend.shouldSerializeReference(ser, new Item(), "other", new Other(), null));
        assertTrue(friend.shouldSerializeReference(ser, new Item(), "item", new Item(), null));

        ser.setExcludedReferences(Item.class);
        assertTrue(friend.shouldSerializeReference(ser, new Item(), "other", new Other(), null));
        assertFalse(friend.shouldSerializeReference(ser, new Item(), "item", new Item(), null));
    }

    @Test
    void excludedReferencesNullClearsExclusionCurrentContract() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);

        ser.setExcludedReferences(Other.class);
        assertFalse(friend.shouldSerializeReference(ser, new Item(), "other", new Other(), null));

        ser.setExcludedReferences((Class[]) null);
        assertTrue(friend.shouldSerializeReference(ser, new Item(), "other", new Other(), null));
    }

    @Test
    void excludedHubReferenceUsesHubObjectClassNotHubClass() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        ser.setExcludedReferences(Other.class);

        Hub<Other> otherHub = new Hub<>(Other.class);
        Hub<Item> itemHub = new Hub<>(Item.class);

        assertFalse(friend.shouldSerializeReference(ser, new Item(), "others", otherHub, null));
        assertTrue(friend.shouldSerializeReference(ser, new Item(), "items", itemHub, null));
    }

    @Test
    void maxAndMaxSizeSettersDoNotMutatePropertyInclusionOptions() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);

        ser.includeProperties(new String[] { "a" });

        ser.setMax(2);
        ser.setMaxSize(100);

        assertTrue(friend.shouldSerializeReference(ser, new Item(), "a", new Item(), null));
        assertFalse(friend.shouldSerializeReference(ser, new Item(), "b", new Item(), null));
    }

    @Test
    void extraObjectOptionDoesNotAffectRootObjectIdentity() {
        Item root = new Item();
        Item extra = new Item();

        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(root, false);
        ser.setExtraObject(extra);

        assertSame(root, ser.getObject());
        assertSame(extra, ser.getExtraObject());
    }
}
