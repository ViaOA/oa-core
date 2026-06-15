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

class OAObjectCacheFilterTest {

    @BeforeEach
    void beforeEach() {
        OAGraphInternal og = (OAGraphInternal) OARuntime.graph(Register.class);
        OAObjectInternalService os = (OAObjectInternalService) og.objectsInternal();
        os.getOAObjectCacheService().removeAllObjects();
    }

    @Test
    void constructorRejectsNullHub() {
        assertThrows(RuntimeException.class, () -> new OAObjectCacheFilter<Item>(null));
    }

    @Test
    void constructorWithFilterControlsIsUsed() {
        Hub<Item> hub = new Hub<>(Item.class);
        OAObjectCacheFilter<Item> filter = new OAObjectCacheFilter<>(hub, item -> item != null && "A".equals(item.getCode()));
        Item accepted = new Item(1);
        accepted.setCode("A");
        Item rejected = new Item(2);
        rejected.setCode("B");
        try {
            assertTrue(filter.isUsed(accepted));
            assertFalse(filter.isUsed(rejected));
        }
        finally {
            filter.close();
        }
    }

    @Test
    void constructorWithDependentPropertiesRegistersAndRefreshes() {
        Hub<Item> hub = new Hub<>(Item.class);
        OAObjectCacheFilter<Item> filter = new OAObjectCacheFilter<>(hub, item -> true, Item.P_Name, Item.P_Code);
        try {
            assertTrue(filter.isUsed(new Item(1)));
        }
        finally {
            filter.close();
        }
    }

    @Test
    void setServerSideOnlyCanBeUsedWithRefresh() {
        Hub<Item> hub = new Hub<>(Item.class);
        OAObjectCacheFilter<Item> filter = new OAObjectCacheFilter<>(hub, item -> true);
        try {
            filter.setServerSideOnly(true);
            filter.refresh(false);
        }
        finally {
            filter.close();
        }
    }

    @Test
    void addFilterRequiresAllFiltersToMatch() {
        Hub<Item> hub = new Hub<>(Item.class);
        OAObjectCacheFilter<Item> filter = new OAObjectCacheFilter<>(hub);
        Item item = new Item(1);
        item.setCode("A");
        item.setName("Alpha");
        try {
            assertTrue(filter.isUsed(item));

            filter.addFilter(obj -> "A".equals(obj.getCode()), false);
            assertTrue(filter.isUsed(item));

            filter.addFilter(obj -> "Beta".equals(obj.getName()), false);
            assertFalse(filter.isUsed(item));
        }
        finally {
            filter.close();
        }
    }

    @Test
    void addFilterWithDependentPropertiesAcceptsNullAndRegistersProperties() {
        Hub<Item> hub = new Hub<>(Item.class);
        OAObjectCacheFilter<Item> filter = new OAObjectCacheFilter<>(hub);
        try {
            filter.addFilter(null, Item.P_Name);
            filter.addFilter(item -> true, Item.P_Code);

            assertTrue(filter.isUsed(new Item(1)));
        }
        finally {
            filter.close();
        }
    }

    @Test
    void reselectAndRefreshCallsSubclassReselect() {
        Hub<Item> hub = new Hub<>(Item.class);
        class TestFilter extends OAObjectCacheFilter<Item> {
            int reselectCount;

            TestFilter() {
                super(hub, item -> true);
            }

            @Override
            protected void reselect() {
                reselectCount++;
            }
        }
        TestFilter filter = new TestFilter();
        try {
            int before = filter.reselectCount;
            filter.reselectAndRefresh();
            assertEquals(before + 1, filter.reselectCount);
        }
        finally {
            filter.close();
        }
    }

    @Test
    void refreshRemovesObjectsThatNoLongerMatch() {
        Hub<Item> hub = new Hub<>(Item.class);
        OAObjectCacheFilter<Item> filter = new OAObjectCacheFilter<>(hub, item -> false);
        Item item = new Item(1);
        hub.add(item);
        try {
            filter.refresh(false);

            assertFalse(hub.contains(item));
        }
        finally {
            filter.close();
        }
    }

    @Test
    void addDependentPropertyIgnoresNullAndBlankValues() {
        Hub<Item> hub = new Hub<>(Item.class);
        OAObjectCacheFilter<Item> filter = new OAObjectCacheFilter<>(hub, item -> true);
        try {
            filter.addDependentProperty(null);
            filter.addDependentProperty("");
            filter.addDependentProperty(Item.P_Name, false);
            filter.addDependentProperty(Item.P_Code);
        }
        finally {
            filter.close();
        }
    }

    @Test
    void closeIsIdempotent() {
        Hub<Item> hub = new Hub<>(Item.class);
        OAObjectCacheFilter<Item> filter = new OAObjectCacheFilter<>(hub);

        filter.close();
        filter.close();
    }

    @Test
    void isUsedReturnsTrueWhenNoFiltersExist() {
        Hub<Item> hub = new Hub<>(Item.class);
        OAObjectCacheFilter<Item> filter = new OAObjectCacheFilter<>(hub);
        try {
            assertTrue(filter.isUsed(new Item(1)));
        }
        finally {
            filter.close();
        }
    }
}
