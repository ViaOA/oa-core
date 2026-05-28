package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

class OAFilterSerializationTest {

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(value);
        }

        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            return (T) in.readObject();
        }
    }

    @Test
    void constantFiltersSerializeAndPreserveBehavior() throws Exception {
        OATrueFilter tf = roundTrip(new OATrueFilter());
        OAFalseFilter ff = roundTrip(new OAFalseFilter());

        assertTrue(tf.isUsed(null));
        assertFalse(ff.isUsed(null));
    }

    @Test
    void directComparisonFiltersSerializeAndPreserveBehavior() throws Exception {
        OAEqualFilter eq = roundTrip(new OAEqualFilter("Bob"));
        OAGreaterFilter gt = roundTrip(new OAGreaterFilter(5));
        OALikeFilter like = roundTrip(new OALikeFilter("ab*"));

        assertTrue(eq.isUsed("Bob"));
        assertFalse(eq.isUsed("Sue"));

        assertTrue(gt.isUsed(6));
        assertFalse(gt.isUsed(5));

        assertTrue(like.isUsed("abcdef"));
        assertFalse(like.isUsed("zabcdef"));
    }

    @Test
    void logicalFiltersSerializeAndPreserveBehavior() throws Exception {
        OAAndFilter and = roundTrip(new OAAndFilter(new OAGreaterFilter(5), new OALessFilter(10)));
        OAOrFilter or = roundTrip(new OAOrFilter(new OAEqualFilter("A"), new OAEqualFilter("B")));
        OAXorFilter xor = roundTrip(new OAXorFilter(new OAEqualFilter("A"), new OAEqualFilter("B")));

        assertTrue(and.isUsed(7));
        assertFalse(and.isUsed(10));

        assertTrue(or.isUsed("A"));
        assertTrue(or.isUsed("B"));
        assertFalse(or.isUsed("C"));

        assertTrue(xor.isUsed("A"));
        assertTrue(xor.isUsed("B"));
        assertFalse(xor.isUsed("C"));
    }

    @Test
    void queryFilterSerializesAndPreservesCompiledBehavior() throws Exception {
        class Bean implements java.io.Serializable {
            private static final long serialVersionUID = 1L;
            private String name;
            private int age;
            Bean(String name, int age) {
                this.name = name;
                this.age = age;
            }
            public String getName() { return name; }
            public int getAge() { return age; }
        }

        OAQueryFilter<Bean> f = roundTrip(new OAQueryFilter<>(Bean.class, "name = 'Bob' AND age >= 40"));

        assertTrue(f.isUsed(new Bean("Bob", 42)));
        assertFalse(f.isUsed(new Bean("Bob", 39)));
        assertFalse(f.isUsed(new Bean("Sue", 42)));
    }
}
