package com.viaoa.datasource.objectcache;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

class ObjectCacheIteratorTest {

    @BeforeEach
    void beforeEach() {
        OAGraphInternal og = (OAGraphInternal) OARuntime.graph(Register.class);
    }
    @AfterEach
    void afterEach() {
        OAObject.setDebugMode(false);
        OARuntime.graph(Register.class).close();
    }

    @Test
    void constructorIteratesObjectsFromOAObjectCache() {
        Register one = new Register(1);
        one.setCode("A");
        Register two = new Register(2);
        two.setCode("B");

        ObjectCacheIterator<Register> it = new ObjectCacheIterator<>(Register.class);

        assertTrue(it.hasNext());
        assertNotNull(it.next());
        assertTrue(it.hasNext());
        assertNotNull(it.next());
        assertFalse(it.hasNext());
        assertNull(it.next());
    }

    @Test
    void constructorWithFilterOnlyReturnsMatchingObjects() {
        Register one = new Register(1);
        one.setCode("A");
        Register two = new Register(2);
        two.setCode("B");

        ObjectCacheIterator<Register> it = new ObjectCacheIterator<>(Register.class, obj -> "B".equals(obj.getCode()));

        assertTrue(it.hasNext());
        assertSame(two, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void setMaxAndGetMaxLimitReturnedObjects() {
        new Register(1);
        new Register(2);
        ObjectCacheIterator<Register> it = new ObjectCacheIterator<>(Register.class);

        it.setMax(1);

        assertEquals(1, it.getMax());
        assertTrue(it.hasNext());
        assertNotNull(it.next());
        assertFalse(it.hasNext());
    }
}
