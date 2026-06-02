package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.hub.HubEvent;
import com.viaoa.object.OAObject;
import com.viaoa.trigger.OATrigger;
import com.viaoa.trigger.OATriggerListener;

import org.junit.jupiter.api.Test;

class OAObjectInfoTriggerBoundarySmokeTest {

    public static class Order extends OAObject {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    void triggerCreateAndRemoveForSimplePropertyDoesNotThrow() {
        OAObjectInfo oi = new OAObjectInfo();
        oi.setForClass(Order.class);

        OATrigger trigger = new OATrigger("test", Order.class, new String[] { "name" }, new OATriggerListener() {
            @Override
            public void onTrigger(OAObject obj, HubEvent hubEvent, String propertyPath) {
            }
        });

        assertDoesNotThrow(() -> oi.createTrigger(trigger));
        assertDoesNotThrow(() -> oi.removeTrigger(trigger));
    }

    @Test
    void invalidTriggerPathFailsVisiblyWithoutLeavingSimpleValidTriggerBroken() {
        OAObjectInfo oi = new OAObjectInfo();
        oi.setForClass(Order.class);

        OATrigger bad = new OATrigger("bad", Order.class, new String[] { "name", "missing.path" }, new OATriggerListener() {
            @Override
            public void onTrigger(OAObject obj, HubEvent hubEvent, String propertyPath) {
            }
        });

        assertThrows(RuntimeException.class, () -> oi.createTrigger(bad));

        OATrigger good = new OATrigger("good", Order.class, new String[] { "name" }, new OATriggerListener() {
            @Override
            public void onTrigger(OAObject obj, HubEvent hubEvent, String propertyPath) {
            }
        });

        assertDoesNotThrow(() -> oi.createTrigger(good));
        assertDoesNotThrow(() -> oi.removeTrigger(good));
    }

    @Test
    void removedTriggerDoesNotReceiveDirectOnChangeDispatch() {
        OAObjectInfo oi = new OAObjectInfo();
        oi.setForClass(Order.class);

        AtomicInteger cnt = new AtomicInteger();

        OATrigger trigger = new OATrigger("test", Order.class, new String[] { "name" }, new OATriggerListener() {
            @Override
            public void onTrigger(OAObject obj, HubEvent hubEvent, String propertyPath) {
                cnt.incrementAndGet();
            }
        });

        oi.createTrigger(trigger);
        oi.removeTrigger(trigger);

        oi.onChange(new Order(), "name", null);

        assertEquals(0, cnt.get());
    }

    @Test
    void duplicateSameTriggerRegistrationDoesNotCauseDuplicateDirectExecutionCurrentContract() {
        OAObjectInfo oi = new OAObjectInfo();
        oi.setForClass(Order.class);

        AtomicInteger cnt = new AtomicInteger();

        OATrigger trigger = new OATrigger("test", Order.class, new String[] { "name" }, new OATriggerListener() {
            @Override
            public void onTrigger(OAObject obj, HubEvent hubEvent, String propertyPath) {
                cnt.incrementAndGet();
            }
        });

        oi.createTrigger(trigger);
        oi.createTrigger(trigger);

        oi.onChange(new Order(), "name", null);

        assertTrue(cnt.get() <= 1, "same trigger instance should not be committed as duplicate registration");
    }
}
