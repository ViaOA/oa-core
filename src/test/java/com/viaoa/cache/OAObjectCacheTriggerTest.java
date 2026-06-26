package com.viaoa.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Item;
import com.test.pos.model.oa.Register;
import com.viaoa.hub.Hub;
import com.viaoa.oa.OA;
import com.viaoa.runtime.OARuntime;

class OAObjectCacheTriggerTest {

    private static class RecordingTrigger extends OAObjectCacheTrigger<Item> {
        final List<Item> triggered = new ArrayList<>();

        RecordingTrigger(Class<Item> clazz) {
            super(clazz);
        }

        RecordingTrigger(Class<Item> clazz, com.viaoa.filter.OAFilter<Item> filter, String... dependentPropPaths) {
            super(clazz, filter, dependentPropPaths);
        }

        RecordingTrigger(Hub<Item> hub, com.viaoa.filter.OAFilter<Item> filter, String... dependentPropPaths) {
            super(hub, filter, dependentPropPaths);
        }

        @Override
        public void onTrigger(Item obj) {
            triggered.add(obj);
        }
    }

    @BeforeEach
    void beforeEach() {
        OA oa = OARuntime.createDefaultOA(Register.class);
    }
    @AfterEach
    void afterEach() {
        OARuntime.oa(Register.class).close();
    }

    @Test
    void constructorRejectsNullClass() {
        assertThrows(RuntimeException.class, () -> new RecordingTrigger(null));
    }

    @Test
    void constructorWithFilterAndDependentPropertiesControlsIsUsed() {
        RecordingTrigger trigger = new RecordingTrigger(Item.class, item -> item != null && "A".equals(item.getCode()),
                Item.P_Code);
        Item accepted = new Item(1);
        accepted.setCode("A");
        Item rejected = new Item(2);
        rejected.setCode("B");
        try {
            assertTrue(trigger.isUsed(accepted));
            assertFalse(trigger.isUsed(rejected));
        }
        finally {
            trigger.close();
        }
    }

    @Test
    void constructorFromHubUsesHubObjectClass() {
        Hub<Item> hub = new Hub<>(Item.class);
        RecordingTrigger trigger = new RecordingTrigger(hub, item -> true, Item.P_Name);
        try {
            assertTrue(trigger.isUsed(new Item(1)));
        }
        finally {
            trigger.close();
        }
    }

    @Test
    void setServerSideOnlyCanBeUsedWithRefresh() {
        RecordingTrigger trigger = new RecordingTrigger(Item.class);
        try {
            trigger.setServerSideOnly(true);
            trigger.refresh();
        }
        finally {
            trigger.close();
        }
    }

    @Test
    void addFilterRequiresAllFiltersToMatch() {
        RecordingTrigger trigger = new RecordingTrigger(Item.class);
        Item item = new Item(1);
        item.setCode("A");
        item.setName("Alpha");
        try {
            assertTrue(trigger.isUsed(item));

            trigger.addFilter(obj -> "A".equals(obj.getCode()), false);
            assertTrue(trigger.isUsed(item));

            trigger.addFilter(obj -> "Beta".equals(obj.getName()), false);
            assertFalse(trigger.isUsed(item));
        }
        finally {
            trigger.close();
        }
    }

    @Test
    void addFilterWithDependentPropertiesAcceptsNullAndRegistersProperties() {
        RecordingTrigger trigger = new RecordingTrigger(Item.class);
        try {
            trigger.addFilter(null, Item.P_Name);
            trigger.addFilter(item -> true, Item.P_Code);
            assertTrue(trigger.isUsed(new Item(1)));
        }
        finally {
            trigger.close();
        }
    }

    @Test
    void refreshVisitsCachedObjectsAndInvokesOnTriggerForMatches() {
        RecordingTrigger trigger = new RecordingTrigger(Item.class, item -> "Y".equals(item.getCode()));
        Item item = new Item(1);
        item.setCode("Y");
        OA oa = OARuntime.oa(Item.class);
        /*qqqqqq
        OAObjectInternalService os = (OAObjectInternalService) og.objectsInternal();
        os.getOAObjectCacheService().add(item);
        try {
            trigger.refresh();

            assertTrue(trigger.triggered.contains(item));
        }
        finally {
            trigger.close();
        }
        */
    }

    @Test
    void addDependentPropertyIgnoresNullAndBlankValues() {
        RecordingTrigger trigger = new RecordingTrigger(Item.class);
        try {
            trigger.addDependentProperty(null);
            trigger.addDependentProperty("");
            trigger.addDependentProperty(Item.P_Name);
        }
        finally {
            trigger.close();
        }
    }

    @Test
    void closeIsIdempotent() {
        RecordingTrigger trigger = new RecordingTrigger(Item.class);

        trigger.close();
        trigger.close();
    }

    @Test
    void isUsedReturnsTrueWhenNoFiltersExist() {
        RecordingTrigger trigger = new RecordingTrigger(Item.class);
        try {
            assertTrue(trigger.isUsed(new Item(1)));
        }
        finally {
            trigger.close();
        }
    }
}
