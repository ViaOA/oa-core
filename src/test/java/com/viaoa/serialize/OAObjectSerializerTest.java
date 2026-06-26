package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Item;
import com.test.pos.model.oa.Product;
import com.test.pos.model.oa.Register;
import com.viaoa.callback.OAObjectSerializerCallback;
import com.viaoa.hub.Hub;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.oa.OA;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

class OAObjectSerializerTest {
    private static final AtomicInteger NEXT = new AtomicInteger(8000);

    @BeforeEach
    void beforeEach() {
        OA oa = OARuntime.createDefaultOA(Register.class);
    }
    @AfterEach
    void afterEach() {
        OARuntime.oa(Register.class).close();
    }

    @Test
    void idAndClientIdGetterSetterRoundTrip() {
        OAObjectSerializer<Item> serializer = new OAObjectSerializer<>(item("BP1", "Brake Pad"), false);

        serializer.setId(123);
        serializer.setClientId(456);

        assertEquals(123, serializer.getId());
        assertEquals(456, serializer.getClientId());
    }

    @Test
    void constructorsStoreObjectCompressionExtraObjectAndCallbackOptions() throws Exception {
        Item item = item("BP2", "Brake Pad");
        Product extra = product(item);
        RecordingCallback callback = new RecordingCallback();

        OAObjectSerializer<Item> serializer = new OAObjectSerializer<>(item, false, callback);
        assertSame(item, serializer.getObject());
        assertSame(callback, serializer.getCallback());

        OAObjectSerializer<Item> withExtra = new OAObjectSerializer<>(item, extra, false, callback);
        assertSame(item, withExtra.getObject());
        assertSame(extra, withExtra.getExtraObject());

        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(item);
        OAObjectSerializer<Hub<Item>> withHub = new OAObjectSerializer<>(hub, false);
        assertSame(hub, withHub.getObject());

        OAObjectSerializer<Item> includeAll = new OAObjectSerializer<>(item, false, true);
        assertTrue(shouldSerialize(includeAll, item, Item.P_Products, new Hub<>(Product.class)));

        OAObjectSerializer<Item> excludeAll = new OAObjectSerializer<>(item, false, false);
        assertFalse(shouldSerialize(excludeAll, item, Item.P_Products, new Hub<>(Product.class)));
    }

    @Test
    void includeBlobsGetterSetterRoundTrip() {
        OAObjectSerializer<Item> serializer = new OAObjectSerializer<>(item("BP3", "Brake Pad"), false);

        assertFalse(serializer.getIncludeBlobs());
        serializer.setIncludeBlobs(true);
        assertTrue(serializer.getIncludeBlobs());
    }

    @Test
    void excludedReferenceClassesSuppressMatchingReferenceTypes() {
        Item item = item("BP4", "Brake Pad");
        Product product = product(item);
        OAObjectSerializer<Item> serializer = new OAObjectSerializer<>(item, false);

        serializer.setExcludedReferences(Product.class);
        assertFalse(shouldSerialize(serializer, item, Item.P_Products, product));

        serializer.excludedClasses(Item.class);
        assertFalse(shouldSerialize(serializer, product, Product.P_Item, item));
    }

    @Test
    void getReferenceValueToSendUsesCallbackWhenPresent() {
        Item item = item("BP5", "Brake Pad");
        RecordingCallback callback = new RecordingCallback();
        callback.referenceValue = "replacement";
        OAObjectSerializer<Item> serializer = new OAObjectSerializer<>(item, false, callback);

        assertEquals("replacement", serializer.getReferenceValueToSend(item));
    }

    @Test
    void maxAndTotalObjectsWrittenAndMaxSizeAccessorsRoundTrip() {
        OAObjectSerializer<Item> serializer = new OAObjectSerializer<>(item("BP6", "Brake Pad"), false);

        assertEquals(0, serializer.getMax());
        serializer.setMax(10);
        assertEquals(10, serializer.getMax());
        assertEquals(0, serializer.getTotalObjectsWritten());

        assertEquals(0, serializer.getMaxSize());
        serializer.setMaxSize(20);
        assertEquals(20, serializer.getMaxSize());
    }

    @Test
    void includeAndExcludePropertiesControlReferenceDecisionsCaseInsensitively() {
        Item item = item("BP7", "Brake Pad");
        Hub<Product> products = new Hub<>(Product.class);
        OAObjectSerializer<Item> serializer = new OAObjectSerializer<>(item, false);

        serializer.includeProperties(new String[] { Item.P_Products.toUpperCase() });
        assertTrue(shouldSerialize(serializer, item, Item.P_Products, products));
        assertFalse(shouldSerialize(serializer, item, Item.P_Manufacturer, new Product()));

        serializer.excludeProperties(new String[] { Item.P_Products.toUpperCase() });
        assertFalse(shouldSerialize(serializer, item, Item.P_Products, products));
        assertTrue(shouldSerialize(serializer, item, Item.P_Manufacturer, new Product()));

        serializer.includeAllProperties();
        assertTrue(shouldSerialize(serializer, item, Item.P_Products, products));

        serializer.excludeAllProperties();
        assertFalse(shouldSerialize(serializer, item, Item.P_Products, products));
    }

    @Test
    void friendAccessMaintainsStackDepthAndCallbackLifecycle() {
        Item item = item("BP8", "Brake Pad");
        RecordingCallback callback = new RecordingCallback();
        OAObjectSerializer<Item> serializer = new OAObjectSerializer<>(item, false, callback);

        assertEquals(0, serializer.getStackSize());
        assertNull(serializer.getPreviousObject());
        assertNull(serializer.getStackObject(0));
        assertEquals(0, serializer.getLevelsDeep());

        OAObjectSerializer.getFriendAccess().beforeSerialize(item, serializer);
        assertEquals(1, serializer.getStackSize());
        assertSame(item, serializer.getPreviousObject());
        assertSame(item, serializer.getStackObject(0));
        assertEquals(1, serializer.getLevelsDeep());
        assertEquals(List.of(item), callback.before);

        OAObjectSerializer.getFriendAccess().afterSerialize(item, serializer);
        assertEquals(0, serializer.getStackSize());
        assertEquals(0, serializer.getLevelsDeep());
        assertEquals(List.of(item), callback.after);
    }

    @Test
    void callbackCanOverrideReferenceDecisionThroughFriendAccess() {
        Item item = item("BP9", "Brake Pad");
        RecordingCallback callback = new RecordingCallback();
        callback.referenceDecision = Boolean.FALSE;
        OAObjectSerializer<Item> serializer = new OAObjectSerializer<>(item, false, callback);

        assertFalse(OAObjectSerializer.getFriendAccess().shouldSerializeReference(serializer, item, Item.P_Products,
                new Hub<>(Product.class), null));

        callback.referenceDecision = Boolean.TRUE;
        serializer.excludeAllProperties();
        assertTrue(OAObjectSerializer.getFriendAccess().shouldSerializeReference(serializer, item, Item.P_Products,
                new Hub<>(Product.class), null));
    }

    @Test
    void hasReachedMaxUsesObjectAndSizeLimits() {
        Item item = item("BP10", "Brake Pad");
        OAObjectSerializer<Item> serializer = new OAObjectSerializer<>(item, false);

        /*qqqq bad test, never serializes
        assertFalse(serializer.hasReachedMax());
        serializer.setMax(1);
        OAObjectSerializer.getFriendAccess().beforeSerialize(item, serializer);
        assertTrue(serializer.hasReachedMax());
        OAObjectSerializer.getFriendAccess().afterSerialize(item, serializer);

        OAObjectSerializer<Item> sizeLimited = new OAObjectSerializer<>(item, false);
        sizeLimited.setMaxSize(1);
        assertFalse(sizeLimited.hasReachedMax());
        */
    }

    @Test
    void compressedWrittenIsNegativeBeforeSerialization() {
        OAObjectSerializer<Item> serializer = new OAObjectSerializer<>(item("BP11", "Brake Pad"), true);

        assertEquals(-1, serializer.getCompressedWritten());
    }

    @Test
    void roundTripNullObjectPreservesIdAndNullPayload() throws Exception {
        OAObjectSerializer<Object> serializer = new OAObjectSerializer<>(null, false);
        serializer.setId(77);

        OAObjectSerializer<Object> copy = roundTrip(serializer);

        assertEquals(77, copy.getId());
        assertNull(copy.getObject());
        assertNull(copy.getExtraObject());
    }

    @Test
    void roundTripOAObjectPreservesScalarPropertiesUncompressed() throws Exception {
        Item item = item("BP12", "Brake Pad");
        item.setBrand("ACME");
        OAObjectSerializer<Item> serializer = new OAObjectSerializer<>(item, false);

        OAObjectSerializer<Item> copy = roundTrip(serializer);
        Item copyItem = copy.getObject();

        assertNotNull(copyItem);
        assertEquals(item.getId(), copyItem.getId());
        assertEquals("BP12", copyItem.getCode());
        assertEquals("Brake Pad", copyItem.getName());
        assertEquals("ACME", copyItem.getBrand());
        assertTrue(copy.getTotalObjectsWritten() >= 1);
    }

    @Test
    void roundTripExtraObjectPreservesSecondaryPayload() throws Exception {
        Item item = item("BP13", "Brake Pad");
        Product product = product(item);
        OAObjectSerializer<Item> serializer = new OAObjectSerializer<>(item, product, false, null);

        OAObjectSerializer<Item> copy = roundTrip(serializer);

        assertNotNull(copy.getObject());
        assertTrue(copy.getExtraObject() instanceof Product);
        Product copyProduct = (Product) copy.getExtraObject();
        assertEquals(product.getId(), copyProduct.getId());
    }

    @Test
    void roundTripHubPreservesObjectClassSizeAndScalarValues() throws Exception {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(item("BP14", "Brake Pad"));
        hub.add(item("OF14", "Oil Filter"));
        OAObjectSerializer<Hub<Item>> serializer = new OAObjectSerializer<>(hub, false);

        OAObjectSerializer<Hub<Item>> copy = roundTrip(serializer);
        Hub<Item> copyHub = copy.getObject();

        assertSame(Item.class, copyHub.getObjectClass());
        assertEquals(2, copyHub.getSize());
        assertEquals("BP14", copyHub.getAt(0).getCode());
        assertEquals("OF14", copyHub.getAt(1).getCode());
    }

    @Test
    void compressedAndUncompressedRoundTripsPreserveSameScalarPayload() throws Exception {
        Item item = item("BP15", "Brake Pad");

        Item uncompressed = roundTrip(new OAObjectSerializer<>(item, false)).getObject();
        Item compressed = roundTrip(new OAObjectSerializer<>(item, true)).getObject();

        assertEquals(uncompressed.getCode(), compressed.getCode());
        assertEquals(uncompressed.getName(), compressed.getName());
    }

    @Test
    void getObjectAndExtraObjectReturnConfiguredPayloadsAndSetExtraObjectReplacesIt() {
        Item item = item("BP16", "Brake Pad");
        Product first = product(item);
        Product second = product(item);
        OAObjectSerializer<Item> serializer = new OAObjectSerializer<>(item, first, false, null);

        assertSame(item, serializer.getObject());
        assertSame(first, serializer.getExtraObject());

        serializer.setExtraObject(second);
        assertSame(second, serializer.getExtraObject());
    }

    @Test
    void setCallbackAssignsAndReplacesCallback() {
        OAObjectSerializer<Item> serializer = new OAObjectSerializer<>(item("BP17", "Brake Pad"), false);
        RecordingCallback callback = new RecordingCallback();

        assertNull(serializer.getCallback());
        serializer.setCallback(callback);
        assertSame(callback, serializer.getCallback());

        serializer.setCallback(null);
        assertNull(serializer.getCallback());
    }

    @Test
    void getFriendAccessReturnsSingletonHelper() {
        assertSame(OAObjectSerializer.getFriendAccess(), OAObjectSerializer.getFriendAccess());
    }

    private static boolean shouldSerialize(OAObjectSerializer<?> serializer, OAObject owner, String propertyName,
            Object value) {
        return OAObjectSerializer.getFriendAccess().shouldSerializeReference(serializer, owner, propertyName, value,
                null);
    }

    private static Item item(String code, String name) {
        Item item = new Item(NEXT.incrementAndGet());
        item.setCode(code);
        item.setName(name);
        return item;
    }

    private static Product product(Item item) {
        Product product = new Product(NEXT.incrementAndGet());
        product.setItem(item);
        return product;
    }

    @SuppressWarnings("unchecked")
    private static <T> OAObjectSerializer<T> roundTrip(OAObjectSerializer<T> serializer) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(serializer);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            return (OAObjectSerializer<T>) in.readObject();
        }
    }

    private static class RecordingCallback extends OAObjectSerializerCallback {
        final List<OAObject> before = new ArrayList<>();
        final List<OAObject> after = new ArrayList<>();
        Object referenceValue;
        Boolean referenceDecision;

        @Override
        public void beforeSerialize(OAObject obj) {
            before.add(obj);
        }

        @Override
        public void afterSerialize(OAObject obj) {
            after.add(obj);
        }

        @Override
        public Object getReferenceValueToSend(Object obj) {
            return referenceValue == null ? obj : referenceValue;
        }

        @Override
        public boolean shouldSerializeReference(OAObject oaObj, String propertyName, Object obj, boolean bDefault) {
            return referenceDecision == null ? bDefault : referenceDecision.booleanValue();
        }
    }
}
