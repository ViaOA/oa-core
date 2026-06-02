package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import com.viaoa.hub.Hub;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectSerializerMaxLimitContractTest {

    static class Item extends OAObject {
    }

    private final OAObjectSerializer.FriendAccess friend = OAObjectSerializer.getFriendAccess();

    @Test
    void maxObjectsZeroMeansNoObjectCountLimit() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        ser.setMax(0);

        for (int i = 0; i < 5; i++) {
            friend.beforeSerialize(new Item(), ser);
        }

        assertFalse(ser.hasReachedMax());
    }

    @Test
    void maxObjectsBoundaryUsesStrictGreaterThanCurrentContract() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        ser.setMax(2);

        assertFalse(ser.hasReachedMax());

        friend.beforeSerialize(new Item(), ser);
        assertFalse(ser.hasReachedMax());

        friend.beforeSerialize(new Item(), ser);
        assertFalse(ser.hasReachedMax());

        friend.beforeSerialize(new Item(), ser);
        assertTrue(ser.hasReachedMax());
    }

    @Test
    void referenceDecisionUsesTotalObjectsPlusMinExpectedBoundary() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        ser.setMax(1);

        assertTrue(friend.shouldSerializeReference(ser, new Item(), "child", new Item(), null));

        friend.beforeSerialize(new Item(), ser);

        assertFalse(friend.shouldSerializeReference(ser, new Item(), "child", new Item(), null));
    }

    @Test
    void hubReferenceSizeParticipatesInMaxObjectLimit() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        ser.setMax(3);

        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item());
        hub.add(new Item());
        hub.add(new Item());

        assertFalse(friend.shouldSerializeReference(ser, new Item(), "children", hub, null));
    }

    @Test
    void hubReferenceAtExactBoundaryIsAllowedCurrentContract() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        ser.setMax(3);

        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item());
        hub.add(new Item());

        assertTrue(friend.shouldSerializeReference(ser, new Item(), "children", hub, null));
    }

    @Test
    void maxSizeUsesCompressedWrittenOnlyWhenDeflaterExists() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        ser.setMaxSize(1);

        assertFalse(ser.hasReachedMax(), "without active deflater, compressed bytes are unavailable and should not trip maxSize");
    }

    @Test
    void reachedMaxFlagCanBeForcedAndIsStickyCurrentContract() throws Exception {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);

        Field f = OAObjectSerializer.class.getDeclaredField("bReachedMax");
        f.setAccessible(true);
        f.setBoolean(ser, true);

        assertTrue(ser.hasReachedMax());

        ser.setMax(0);
        ser.setMaxSize(0);

        assertTrue(ser.hasReachedMax());
    }

    @Test
    void manyLinkCacheSizeCounterIsPerLinkInfoIdentity() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);

        OALinkInfo a = new OALinkInfo("a", Item.class, OALinkInfo.TYPE_MANY);
        a.setCacheSize(1);
        OALinkInfo b = new OALinkInfo("b", Item.class, OALinkInfo.TYPE_MANY);
        b.setCacheSize(1);

        assertTrue(friend.shouldSerializeReference(ser, new Item(), "a", new Item(), a));
        assertFalse(friend.shouldSerializeReference(ser, new Item(), "a", new Item(), a));

        assertTrue(friend.shouldSerializeReference(ser, new Item(), "b", new Item(), b));
        assertFalse(friend.shouldSerializeReference(ser, new Item(), "b", new Item(), b));
    }
}
