package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import com.viaoa.callback.OAObjectSerializerCallback;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectSerializerCallbackScopeTest {
    static class Item extends OAObject {
    }

    static class TrackingCallback extends OAObjectSerializerCallback {
        OAObjectSerializer serializer;
        final List<String> events = new ArrayList<>();

        @Override
        public void setOAObjectSerializer(OAObjectSerializer os) {
            this.serializer = os;
            super.setOAObjectSerializer(os);
        }

        @Override
        public void beforeSerialize(OAObject obj) {
            events.add("before");
            serializer.includeProperties(new String[] { "child" });
        }

        @Override
        public void afterSerialize(OAObject obj) {
            events.add("after");
        }

        @Override
        public boolean shouldSerializeReference(OAObject obj, String propertyName, Object ref, boolean defaultValue) {
            events.add("ref:" + propertyName + ":" + defaultValue);
            return defaultValue;
        }

        @Override
        public Object getReferenceValueToSend(Object obj) {
            events.add("value");
            return "replacement";
        }
    }

    private final OAObjectSerializer.FriendAccess friend = OAObjectSerializer.getFriendAccess();

    @Test
    void setCallbackAssignsSerializerBackReference() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        TrackingCallback cb = new TrackingCallback();

        ser.setCallback(cb);

        assertSame(cb, ser.getCallback());
        assertSame(ser, cb.serializer);
    }

    @Test
    void callbackBeforeAfterAreCalledAndStackStateRestored() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        ser.excludeProperties(new String[] { "original" });

        TrackingCallback cb = new TrackingCallback();
        ser.setCallback(cb);

        Item item = new Item();

        friend.beforeSerialize(item, ser);
        assertEquals(1, ser.getStackSize());
        assertTrue(friend.shouldSerializeReference(ser, item, "child", new Item(), null));
        assertFalse(friend.shouldSerializeReference(ser, item, "other", new Item(), null));

        friend.afterSerialize(item, ser);

        assertEquals(List.of("before", "ref:child:true", "ref:other:false", "after"), cb.events);
        assertEquals(0, ser.getStackSize());

        assertFalse(friend.shouldSerializeReference(ser, item, "original", new Item(), null));
        assertTrue(friend.shouldSerializeReference(ser, item, "child", new Item(), null));
    }

    @Test
    void callbackCanOverrideReferenceDecisionToFalse() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        ser.setCallback(new OAObjectSerializerCallback() {
            @Override
            public boolean shouldSerializeReference(OAObject obj, String propertyName, Object ref, boolean defaultValue) {
                return false;
            }

			@Override
			public void beforeSerialize(OAObject obj) {
				// TODO Auto-generated method stub
			}
        });

        assertFalse(friend.shouldSerializeReference(ser, new Item(), "child", new Item(), null));
    }

    @Test
    void callbackCanOverrideReferenceDecisionToTrueEvenWhenDefaultFalse() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        ser.excludeAllProperties();
        ser.setCallback(new OAObjectSerializerCallback() {
            @Override
            public boolean shouldSerializeReference(OAObject obj, String propertyName, Object ref, boolean defaultValue) {
                return true;
            }
			@Override
			public void beforeSerialize(OAObject obj) {
				// TODO Auto-generated method stub
			}
        });

        assertTrue(friend.shouldSerializeReference(ser, new Item(), "child", new Item(), null));
    }

    @Test
    void getReferenceValueToSendUsesCallbackValue() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        TrackingCallback cb = new TrackingCallback();
        ser.setCallback(cb);

        Object value = ser.getReferenceValueToSend(new Item());

        assertEquals("replacement", value);
        assertTrue(cb.events.contains("value"));
    }

    @Test
    void getReferenceValueToSendDefaultsToOriginalObject() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        Item ref = new Item();

        assertSame(ref, ser.getReferenceValueToSend(ref));
    }

    @Test
    void callbackExceptionIsVisibleAndDoesNotLookLikeCleanSuccess() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item(), false);
        ser.setCallback(new OAObjectSerializerCallback() {
            @Override
            public void afterSerialize(OAObject obj) {
                throw new IllegalStateException("boom");
            }

			@Override
			public void beforeSerialize(OAObject obj) {
				// TODO Auto-generated method stub
			}
        });

        Item item = new Item();
        friend.beforeSerialize(item, ser);

        assertThrows(IllegalStateException.class, () -> friend.afterSerialize(item, ser));
        assertTrue(ser.getStackSize() >= 1);
    }
}
