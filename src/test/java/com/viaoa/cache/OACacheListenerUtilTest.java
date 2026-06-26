package com.viaoa.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Item;
import com.test.pos.model.oa.Register;
import com.viaoa.oa.OA;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

class OACacheListenerUtilTest {

    private static class RecordingUtil extends OACacheListenerUtil {
        int count;
        OAObject object;
        String propertyName;
        Object oldValue;
        Object newValue;
        String stackTrace;

        RecordingUtil(Class clazz, String property) {
            super(clazz, property);
        }

        @Override
        public void onEvent(OAObject obj, String propertyName, Object oldValue, Object newValue, String stackTrace) {
            count++;
            this.object = obj;
            this.propertyName = propertyName;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.stackTrace = stackTrace;
        }
    }

    @BeforeEach
    void beforeEach() {
        OA oa = OARuntime.createDefaultOA(Register.class);
    }
    @AfterEach
    void afterEach() {
        OAObject.setDebugMode(false);
        OARuntime.oa(Register.class).close();
    }
    
    @Test
    void constructorInitializesListenerAndCloseRemovesIt() throws Exception {
        RecordingUtil util = new RecordingUtil(Item.class, Item.P_Name);
        assertNotNull(listener(util));

        util.close();

        assertNull(listener(util));
    }

    @Test
    void initIsIdempotent() throws Exception {
        class TestUtil extends RecordingUtil {
            TestUtil() {
                super(Item.class, Item.P_Name);
            }

            void initAgain() {
                init();
            }
        }
        TestUtil util = new TestUtil();
        try {
            OAObjectCacheListener<?> first = listener(util);
            util.initAgain();
            assertSame(first, listener(util));
        }
        finally {
            util.close();
        }
    }

    @Test
    void listenerOnlyCallsOnEventForMatchingPropertyIgnoringCase() throws Exception {
        RecordingUtil util = new RecordingUtil(Item.class, Item.P_Name);
        Item item = new Item(1);
        try {
            @SuppressWarnings("unchecked")
            OAObjectCacheListener<Item> listener = (OAObjectCacheListener<Item>) listener(util);

            listener.afterPropertyChange(item, Item.P_Code, "oldCode", "newCode");
            listener.afterPropertyChange(item, "NAME", "oldName", "newName");

            assertEquals(1, util.count);
            assertSame(item, util.object);
            assertEquals("NAME", util.propertyName);
            assertEquals("oldName", util.oldValue);
            assertEquals("newName", util.newValue);
            assertNotNull(util.stackTrace);
            assertTrue(util.stackTrace.contains("Thread="));
        }
        finally {
            util.close();
        }
    }

    @Test
    void nullPropertyListensForAllPropertyChanges() throws Exception {
        RecordingUtil util = new RecordingUtil(Item.class, null);
        Item item = new Item(1);
        assertEquals(2, util.count);
        try {
            @SuppressWarnings("unchecked")
            OAObjectCacheListener<Item> listener = (OAObjectCacheListener<Item>) listener(util);

            listener.afterPropertyChange(item, Item.P_Code, "oldCode", "newCode");
            listener.afterPropertyChange(item, Item.P_Name, "oldName", "newName");

            assertEquals(4, util.count);
        }
        finally {
            util.close();
        }
    }

    @Test
    void onEventDefaultImplementationIsNoOp() {
        OACacheListenerUtil util = new OACacheListenerUtil(Item.class, Item.P_Name);
        try {
            util.onEvent(new Item(1), Item.P_Name, "old", "new", "stack");
        }
        finally {
            util.close();
        }
    }

    private static OAObjectCacheListener<?> listener(OACacheListenerUtil util) throws Exception {
        Field field = OACacheListenerUtil.class.getDeclaredField("listener");
        field.setAccessible(true);
        return (OAObjectCacheListener<?>) field.get(util);
    }
}
