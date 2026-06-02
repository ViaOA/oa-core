package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectSerializerReferenceDecisionTest {

    static class Item extends OAObject {
    }

    static class Other extends OAObject {
    }

    private final OAObjectSerializer.FriendAccess friend = OAObjectSerializer.getFriendAccess();

    @Test
    void defaultReferenceDecisionIsTrue() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);

        assertTrue(friend.shouldSerializeReference(ser, new Item(), "child", new Item(), null));
    }

    @Test
    void includePropertiesOnlyAllowsListedPropertiesCaseInsensitively() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        ser.includeProperties(new String[] { "Child" });

        assertTrue(friend.shouldSerializeReference(ser, new Item(), "child", new Item(), null));
        assertFalse(friend.shouldSerializeReference(ser, new Item(), "other", new Item(), null));
    }

    @Test
    void excludePropertiesBlocksListedPropertiesCaseInsensitively() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        ser.excludeProperties(new String[] { "Child" });

        assertFalse(friend.shouldSerializeReference(ser, new Item(), "child", new Item(), null));
        assertTrue(friend.shouldSerializeReference(ser, new Item(), "other", new Item(), null));
    }

    @Test
    void includeAllPropertiesAllowsReferences() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        ser.includeAllProperties();

        assertTrue(friend.shouldSerializeReference(ser, new Item(), "anything", new Item(), null));
    }

    @Test
    void excludeAllPropertiesBlocksReferences() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        ser.excludeAllProperties();

        assertFalse(friend.shouldSerializeReference(ser, new Item(), "anything", new Item(), null));
    }

    @Test
    void excludedClassSuppressesMatchingObjectReference() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        ser.setExcludedReferences(Other.class);

        assertTrue(friend.shouldSerializeReference(ser, new Item(), "item", new Item(), null));
        assertFalse(friend.shouldSerializeReference(ser, new Item(), "other", new Other(), null));
    }

    @Test
    void excludedClassSuppressesHubByObjectClass() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        ser.excludedClasses(Other.class);

        Hub<Other> hub = new Hub<>(Other.class);
        assertFalse(friend.shouldSerializeReference(ser, new Item(), "others", hub, null));
    }

    @Test
    void maxObjectsSuppressesReferenceWhenLimitReached() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        ser.setMax(0);
        assertTrue(friend.shouldSerializeReference(ser, new Item(), "child", new Item(), null));

        ser.setMax(1);
        friend.beforeSerialize(new Item(), ser);

        assertFalse(friend.shouldSerializeReference(ser, new Item(), "child", new Item(), null));

        friend.afterSerialize(new Item(), ser);
    }

    @Test
    void maxObjectsSuppressesHubReferenceWhenHubSizeWouldExceedLimit() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        ser.setMax(2);

        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item());
        hub.add(new Item());
        hub.add(new Item());

        assertFalse(friend.shouldSerializeReference(ser, new Item(), "children", hub, null));
    }

    @Test
    void manyLinkCacheSizeLimitsReferenceExpansionPerLinkInfo() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        OALinkInfo li = new OALinkInfo("children", Item.class, OALinkInfo.TYPE_MANY);
        li.setCacheSize(2);

        assertTrue(friend.shouldSerializeReference(ser, new Item(), "children", new Item(), li));
        assertTrue(friend.shouldSerializeReference(ser, new Item(), "children", new Item(), li));
        assertFalse(friend.shouldSerializeReference(ser, new Item(), "children", new Item(), li));
    }

    @Test
    void oneLinkIgnoresCacheSizeLimit() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        OALinkInfo li = new OALinkInfo("child", Item.class, OALinkInfo.TYPE_ONE);
        li.setCacheSize(1);

        assertTrue(friend.shouldSerializeReference(ser, new Item(), "child", new Item(), li));
        assertTrue(friend.shouldSerializeReference(ser, new Item(), "child", new Item(), li));
    }
}
