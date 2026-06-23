package com.viaoa.trigger;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Product;
import com.test.pos.model.oa.Register;
import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.hub.HubEvent;
import com.viaoa.runtime.OARuntime;

class OATriggerMethodListenerTest {

    @BeforeEach
    void beforeEach() {
        RecordingProduct.reset();
        OAGraphInternal og = (OAGraphInternal) OARuntime.graph(Register.class);
    }
    @AfterEach
    void afterEach() {
        OARuntime.graph(Register.class).close();
    }
    
    @Test
    void constructorAcceptsRootClassMethodAndLoadedDataFlag() throws Exception {
        Method method = RecordingProduct.class.getDeclaredMethod("recordTrigger", HubEvent.class);
        method.setAccessible(true);

        OATriggerMethodListener listener = new OATriggerMethodListener(RecordingProduct.class, method, true);

        assertNotNull(listener);
    }

    @Test
    void onTriggerInvokesMethodDirectlyWhenRootObjectIsSupplied() throws Exception {
        Method method = RecordingProduct.class.getDeclaredMethod("recordTrigger", HubEvent.class);
        method.setAccessible(true);
        OATriggerMethodListener listener = new OATriggerMethodListener(RecordingProduct.class, method, true);
        RecordingProduct product = new RecordingProduct();
        HubEvent<Product> event = new HubEvent<>(product, Product.P_Sku, "old", "new");

        listener.onTrigger(product, event, Product.P_Sku);

        assertEquals(1, RecordingProduct.count.get());
        assertSame(product, RecordingProduct.lastProduct.get());
        assertSame(event, RecordingProduct.lastEvent.get());
    }

    @Test
    void onTriggerPropagatesReflectiveFailureWhenRootObjectIsSupplied() throws Exception {
        Method method = RecordingProduct.class.getDeclaredMethod("failingTrigger", HubEvent.class);
        method.setAccessible(true);
        OATriggerMethodListener listener = new OATriggerMethodListener(RecordingProduct.class, method, true);
        RecordingProduct product = new RecordingProduct();

        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> listener.onTrigger(product, new HubEvent<>(product), Product.P_Sku));
        assertInstanceOf(IllegalStateException.class, ex.getCause());
        assertEquals("trigger failed", ex.getCause().getMessage());
    }

    @Test
    void onTriggerWithNullRootUsesLoadedCacheFallbackForEmptyPath() throws Exception {
        Method method = RecordingProduct.class.getDeclaredMethod("recordTrigger", HubEvent.class);
        method.setAccessible(true);
        OATriggerMethodListener listener = new OATriggerMethodListener(RecordingProduct.class, method, true);
        RecordingProduct product = new RecordingProduct();
        HubEvent<Product> event = new HubEvent<>(product, Product.P_Sku, "old", "new");

        listener.onTrigger(null, event, "");

        assertEquals(1, RecordingProduct.count.get());
        assertSame(product, RecordingProduct.lastProduct.get());
        assertSame(event, RecordingProduct.lastEvent.get());
    }

    private static class RecordingProduct extends Product {
        static final AtomicInteger count = new AtomicInteger();
        static final AtomicReference<RecordingProduct> lastProduct = new AtomicReference<>();
        static final AtomicReference<HubEvent> lastEvent = new AtomicReference<>();

        static void reset() {
            count.set(0);
            lastProduct.set(null);
            lastEvent.set(null);
        }

        @SuppressWarnings("unused")
        public void recordTrigger(HubEvent event) {
            count.incrementAndGet();
            lastProduct.set(this);
            lastEvent.set(event);
        }

        @SuppressWarnings("unused")
        public void failingTrigger(HubEvent event) {
            throw new IllegalStateException("trigger failed");
        }
    }
}
