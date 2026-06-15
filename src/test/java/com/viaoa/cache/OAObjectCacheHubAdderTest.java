package com.viaoa.cache;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Item;
import com.test.pos.model.oa.Register;
import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.graph.service.OAObjectInternalService;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;

class OAObjectCacheHubAdderTest {

    @BeforeEach
    void beforeEach() {
        OAGraphInternal og = (OAGraphInternal) OARuntime.graph(Register.class);
        OAObjectInternalService os = (OAObjectInternalService) og.objectsInternal();
        os.getOAObjectCacheService().removeAllObjects();
    }

    @Test
    void constructorRejectsNullHub() {
        assertThrows(IllegalArgumentException.class, () -> new OAObjectCacheHubAdder<Item>(null));
    }

    @Test
    void closeIsIdempotent() {
        Hub<Item> hub = new Hub<>(Item.class);
        OAObjectCacheHubAdder<Item> adder = new OAObjectCacheHubAdder<>(hub);

        adder.close();
        adder.close();
    }

    @Test
    void afterPropertyChangeIsNoOp() {
        Hub<Item> hub = new Hub<>(Item.class);
        OAObjectCacheHubAdder<Item> adder = new OAObjectCacheHubAdder<>(hub);
        try {
            adder.afterPropertyChange(new Item(1), Item.P_Name, "old", "new");
            assertEquals(0, hub.getSize());
        }
        finally {
            adder.close();
        }
    }

    @Test
    void afterAddAddsUsedObjectToHub() {
        Hub<Item> hub = new Hub<>(Item.class);
        OAObjectCacheHubAdder<Item> adder = new OAObjectCacheHubAdder<>(hub);
        Item item = new Item(1);
        try {
            adder.afterAdd(item);

            assertTrue(hub.contains(item));
            assertTrue(adder.isUsed(item));
        }
        finally {
            adder.close();
        }
    }

    @Test
    void afterAddHonorsIsUsedOverrideAndIgnoresNull() {
        Hub<Item> hub = new Hub<>(Item.class);
        OAObjectCacheHubAdder<Item> adder = new OAObjectCacheHubAdder<Item>(hub) {
            @Override
            public boolean isUsed(Item obj) {
                return obj != null && "Y".equals(obj.getCode());
            }
        };
        Item accepted = new Item(1);
        accepted.setCode("Y");
        Item rejected = new Item(2);
        rejected.setCode("N");
        try {
            adder.afterAdd(null);
            adder.afterAdd(rejected);
            adder.afterAdd(accepted);

            assertFalse(hub.contains(rejected));
            assertTrue(hub.contains(accepted));
        }
        finally {
            adder.close();
        }
    }

    @Test
    void afterAddHubAndAfterRemoveAreNoOps() {
        Hub<Item> hub = new Hub<>(Item.class);
        OAObjectCacheHubAdder<Item> adder = new OAObjectCacheHubAdder<>(hub);
        Item item = new Item(1);
        try {
            adder.afterAdd(hub, item);
            adder.afterRemove(hub, item);

            assertEquals(0, hub.getSize());
        }
        finally {
            adder.close();
        }
    }

    @Test
    void afterLoadDelegatesToAfterAdd() {
        Hub<Item> hub = new Hub<>(Item.class);
        OAObjectCacheHubAdder<Item> adder = new OAObjectCacheHubAdder<>(hub);
        Item item = new Item(1);
        try {
            adder.afterLoad(item);

            assertTrue(hub.contains(item));
        }
        finally {
            adder.close();
        }
    }
}
