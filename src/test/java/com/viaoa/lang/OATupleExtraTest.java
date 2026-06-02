package com.viaoa.lang;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OATupleExtraTest {

    @Test
    void tupleStoresValuesInFinalFields() throws Exception {
        Tuple<String, Integer> tuple = new Tuple<>("a", 1);

        assertEquals("a", tuple.a);
        assertEquals(1, tuple.b);
        assertFinalField(Tuple.class, "a");
        assertFinalField(Tuple.class, "b");
    }

    @Test
    void tupleCurrentlyUsesIdentityEqualityAndObjectToString() {
        Tuple<String, Integer> one = new Tuple<>("a", 1);
        Tuple<String, Integer> two = new Tuple<>("a", 1);

        assertNotEquals(one, two);
        assertEquals(System.identityHashCode(one), one.hashCode());
        assertTrue(one.toString().startsWith(Tuple.class.getName() + "@"));
    }

    @Test
    void tuple3StoresValuesInFinalFields() throws Exception {
        Tuple3<String, Integer, Boolean> tuple = new Tuple3<>("a", 1, true);

        assertEquals("a", tuple.a);
        assertEquals(1, tuple.b);
        assertTrue(tuple.c);
        assertFinalField(Tuple3.class, "a");
        assertFinalField(Tuple3.class, "b");
        assertFinalField(Tuple3.class, "c");
    }

    @Test
    void tuple3CurrentlyUsesIdentityEqualityAndObjectToString() {
        Tuple3<String, Integer, Boolean> one = new Tuple3<>("a", 1, true);
        Tuple3<String, Integer, Boolean> two = new Tuple3<>("a", 1, true);

        assertNotEquals(one, two);
        assertEquals(System.identityHashCode(one), one.hashCode());
        assertTrue(one.toString().startsWith(Tuple3.class.getName() + "@"));
    }

    private static void assertFinalField(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        assertTrue(Modifier.isFinal(field.getModifiers()));
        assertTrue(Modifier.isPublic(field.getModifiers()));
    }
}
