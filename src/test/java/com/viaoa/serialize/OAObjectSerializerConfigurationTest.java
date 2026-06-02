package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectSerializerConfigurationTest {

    static class Item extends OAObject {
    }

    @Test
    void idAndClientIdRoundTrip() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);

        assertEquals(0, ser.getId());
        assertEquals(0, ser.getClientId());

        ser.setId(123);
        ser.setClientId(456);

        assertEquals(123, ser.getId());
        assertEquals(456, ser.getClientId());
    }

    @Test
    void objectAndExtraObjectRoundTripWithoutParentWrapper() {
        Item item = new Item();
        Item extra = new Item();

        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(item, extra, false, null);

        assertSame(item, ser.getObject());
        assertSame(extra, ser.getExtraObject());

        Item extra2 = new Item();
        ser.setExtraObject(extra2);

        assertSame(extra2, ser.getExtraObject());
    }

    @Test
    void nullRootObjectIsAllowed() {
        OAObjectSerializer<Object> ser = new OAObjectSerializer<>(null, false);

        assertNull(ser.getObject());
        assertNull(ser.getExtraObject());
    }

    @Test
    void includeBlobsDefaultsFalseAndRoundTrips() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);

        assertFalse(ser.getIncludeBlobs());

        ser.setIncludeBlobs(true);
        assertTrue(ser.getIncludeBlobs());

        ser.setIncludeBlobs(false);
        assertFalse(ser.getIncludeBlobs());
    }

    @Test
    void maxObjectAndMaxSizeOptionsRoundTrip() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);

        assertEquals(0, ser.getMax());
        assertEquals(0, ser.getMaxSize());

        ser.setMax(10);
        ser.setMaxSize(2048);

        assertEquals(10, ser.getMax());
        assertEquals(2048, ser.getMaxSize());
    }

    @Test
    void totalObjectsWrittenDefaultsToZero() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);

        assertEquals(0, ser.getTotalObjectsWritten());
    }

    @Test
    void compressedWrittenIsMinusOneBeforeCompressedWriteStarts() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), true);

        assertEquals(-1, ser.getCompressedWritten());
    }

    @Test
    void getCallbackDefaultsNullAndCanBeCleared() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);

        assertNull(ser.getCallback());

        ser.setCallback(null);

        assertNull(ser.getCallback());
    }

    @Test
    void constructorWithAllReferencesTrueSetsIncludeAllReferenceMode() throws Exception {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false, true);

        assertNull(getField(ser, "includeProps"));
        assertSame(OAObjectSerializer.EmptyProperties, getField(ser, "excludeProps"));
    }

    @Test
    void constructorWithAllReferencesFalseSetsExcludeAllReferenceMode() throws Exception {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false, false);

        assertSame(OAObjectSerializer.EmptyProperties, getField(ser, "includeProps"));
        assertNull(getField(ser, "excludeProps"));
    }

    @Test
    void includeExcludePropertyMethodsAreMutuallyExclusive() throws Exception {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);

        String[] include = { "name" };
        String[] exclude = { "children" };

        ser.includeProperties(include);

        assertSame(include, getField(ser, "includeProps"));
        assertNull(getField(ser, "excludeProps"));

        ser.excludeProperties(exclude);

        assertSame(exclude, getField(ser, "excludeProps"));
        assertNull(getField(ser, "includeProps"));

        ser.includeAllProperties();

        assertSame(OAObjectSerializer.EmptyProperties, getField(ser, "excludeProps"));
        assertNull(getField(ser, "includeProps"));

        ser.excludeAllProperties();

        assertSame(OAObjectSerializer.EmptyProperties, getField(ser, "includeProps"));
        assertNull(getField(ser, "excludeProps"));
    }

    @Test
    void hubRootSetsMinimumExpectedAmountForLimitAccounting() {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item());
        hub.add(new Item());

        OAObjectSerializer<Hub<Item>> ser = new OAObjectSerializer<>(hub, false);

        ser.setMax(1);

        assertTrue(ser.hasReachedMax(),
            "hub root minimum expected amount participates in max-object accounting");
    }

    @Test
    void nonHubRootDoesNotImmediatelyReachPositiveMax() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);

        ser.setMax(1);

        assertFalse(ser.hasReachedMax());
    }

    private static Object getField(Object obj, String name) throws Exception {
        Field f = OAObjectSerializer.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(obj);
    }
}
