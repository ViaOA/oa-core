package com.viaoa.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.Store;
import com.viaoa.datasource.objectcache.OADataSourceObjectCache;

class OADataSourceServiceTest {
    @Test
    void registerUnregisterAndGetAllIgnoreNullAndAvoidDuplicates() {
        OADataSourceService service = new OADataSourceService();
        OADataSourceObjectCache ds = new OADataSourceObjectCache();

        service.register(null);
        assertEquals(0, service.getAll().length);

        service.register(ds);
        service.register(ds);
        assertArrayEquals(new Object[] { ds }, service.getAll());
        assertEquals(0, service.getPosition(ds));

        service.unregister(null);
        assertArrayEquals(new Object[] { ds }, service.getAll());

        service.unregister(ds);
        assertEquals(0, service.getAll().length);
        assertEquals(-1, service.getPosition(ds));
    }

    @Test
    void getSelectsFirstEnabledSupportingDatasourceUnlessLastDatasourceApplies() {
        OADataSourceService service = new OADataSourceService();
        OADataSourceObjectCache first = new OADataSourceObjectCache();
        OADataSourceObjectCache last = new OADataSourceObjectCache();
        last.setLast(true);

        service.register(first);
        service.register(last);

        assertSame(first, service.get(Register.class));

        first.setEnabled(false);
        assertSame(last, service.get(Store.class));

        last.setEnabled(false);
        assertNull(service.get(Register.class));
    }

    @Test
    void setPositionMovesRegisteredDatasourceWithinBounds() {
        OADataSourceService service = new OADataSourceService();
        OADataSourceObjectCache one = new OADataSourceObjectCache();
        OADataSourceObjectCache two = new OADataSourceObjectCache();
        OADataSourceObjectCache three = new OADataSourceObjectCache();

        service.register(one);
        service.register(two);
        service.register(three);

        service.setPosition(-5, three);
        assertArrayEquals(new Object[] { three, one, two }, service.getAll());

        service.setPosition(99, three);
        assertArrayEquals(new Object[] { one, two, three }, service.getAll());

        service.setPosition(0, new OADataSourceObjectCache());
        assertArrayEquals(new Object[] { one, two, three }, service.getAll());
    }
}
