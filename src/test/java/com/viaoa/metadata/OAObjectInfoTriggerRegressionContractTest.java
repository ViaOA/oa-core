package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.hub.HubEvent;
import com.viaoa.object.OAObject;
import com.viaoa.trigger.OATrigger;
import com.viaoa.trigger.OATriggerListener;

import org.junit.jupiter.api.Test;

class OAObjectInfoTriggerRegressionContractTest {

    public static class Order extends OAObject {
        private String name;
        private String status;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    private static OATrigger trigger(String name, String[] paths, AtomicInteger cnt) {
        return new OATrigger(name, Order.class, paths, new OATriggerListener() {
            @Override
            public void onTrigger(OAObject obj, HubEvent hubEvent, String propertyPath) {
                cnt.incrementAndGet();
            }
        });
    }

    @Test
    void failedMultiPathTriggerRegistrationShouldNotLeavePartialValidPathCommitted() {
        OAObjectInfo oi = new OAObjectInfo();
        oi.setForClass(Order.class);
        AtomicInteger cnt = new AtomicInteger();

        OATrigger bad = trigger("bad", new String[] { "name", "missing.path" }, cnt);

        assertThrows(RuntimeException.class, () -> oi.createTrigger(bad));

        oi.onChange(new Order(), "name", null);

        assertEquals(0, cnt.get(),
            "failed trigger registration should not leave earlier valid path partially committed");
    }

    @Test
    void sameListenerSamePathDifferentTriggerNamesShouldRemainDistinctIfContractsDiffer() {
        OAObjectInfo oi = new OAObjectInfo();
        oi.setForClass(Order.class);
        AtomicInteger cnt = new AtomicInteger();

        OATriggerListener listener = new OATriggerListener() {
            @Override
            public void onTrigger(OAObject obj, HubEvent hubEvent, String propertyPath) {
                cnt.incrementAndGet();
            }
        };

        OATrigger t1 = new OATrigger("t1", Order.class, new String[] { "name" }, listener);
        OATrigger t2 = new OATrigger("t2", Order.class, new String[] { "name" }, listener);

        oi.createTrigger(t1);
        oi.createTrigger(t2);

        oi.onChange(new Order(), "name", null);

        assertEquals(2, cnt.get(),
            "distinct trigger registrations should not be collapsed solely by listener/path identity");
    }

    @Test
    void triggerListenerExceptionPropagatesAndDoesNotDisableFutureValidDispatch() {
        OAObjectInfo oi = new OAObjectInfo();
        oi.setForClass(Order.class);

        AtomicInteger throwingCnt = new AtomicInteger();
        OATrigger throwing = new OATrigger("throwing", Order.class, new String[] { "name" }, new OATriggerListener() {
            @Override
            public void onTrigger(OAObject obj, HubEvent hubEvent, String propertyPath) {
                throwingCnt.incrementAndGet();
                throw new IllegalStateException("listener boom");
            }
        });

        oi.createTrigger(throwing);

        assertThrows(RuntimeException.class, () -> oi.onChange(new Order(), "name", null));
        assertEquals(1, throwingCnt.get());

        oi.removeTrigger(throwing);

        AtomicInteger validCnt = new AtomicInteger();
        OATrigger valid = trigger("valid", new String[] { "name" }, validCnt);
        oi.createTrigger(valid);

        assertDoesNotThrow(() -> oi.onChange(new Order(), "name", null));
        assertEquals(1, validCnt.get());
    }

    @Test
    void removeOneTriggerDoesNotRemoveAnotherForSamePropertyPath() {
        OAObjectInfo oi = new OAObjectInfo();
        oi.setForClass(Order.class);

        AtomicInteger c1 = new AtomicInteger();
        AtomicInteger c2 = new AtomicInteger();

        OATrigger t1 = trigger("t1", new String[] { "name" }, c1);
        OATrigger t2 = trigger("t2", new String[] { "name" }, c2);

        oi.createTrigger(t1);
        oi.createTrigger(t2);
        oi.removeTrigger(t1);

        oi.onChange(new Order(), "name", null);

        assertEquals(0, c1.get());
        assertEquals(1, c2.get());
    }

    @Test
    void unrelatedPropertyChangeDoesNotFireSimpleTrigger() {
        OAObjectInfo oi = new OAObjectInfo();
        oi.setForClass(Order.class);
        AtomicInteger cnt = new AtomicInteger();

        OATrigger t = trigger("nameTrigger", new String[] { "name" }, cnt);
        oi.createTrigger(t);

        oi.onChange(new Order(), "status", null);

        assertEquals(0, cnt.get());
    }
}
