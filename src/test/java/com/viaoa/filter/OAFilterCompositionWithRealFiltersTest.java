package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAFilterCompositionWithRealFiltersTest {

    public static class Bean {
        private String name;
        private int age;
        private boolean active;

        public Bean(String name, int age, boolean active) {
            this.name = name;
            this.age = age;
            this.active = active;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public boolean getActive() {
            return active;
        }
    }

    @Test
    void compositeFiltersWorkWithPathBasedRealFilters() {
        Bean bob = new Bean("Bob", 42, true);
        Bean sue = new Bean("Sue", 25, false);
        Bean tim = new Bean("Tim", 50, true);

        OAFilter adult = new OAGreaterOrEqualFilter("age", 40);
        OAFilter active = new OATrueFilter("active");
        OAFilter nameB = new OAStartsWithFilter("name", "B");

        OAFilter f = new OAAndFilter(new OAAndFilter(adult, active), nameB);

        assertTrue(f.isUsed(bob));
        assertFalse(f.isUsed(sue));
        assertFalse(f.isUsed(tim));
    }

    @Test
    void orAndXorCompositionWithPathBasedRealFilters() {
        Bean bob = new Bean("Bob", 42, true);
        Bean sue = new Bean("Sue", 25, false);
        Bean tim = new Bean("Tim", 50, true);

        OAFilter nameBob = new OAEqualFilter("name", "Bob");
        OAFilter young = new OALessFilter("age", 30);

        OAFilter or = new OAOrFilter(nameBob, young);
        assertTrue(or.isUsed(bob));
        assertTrue(or.isUsed(sue));
        assertFalse(or.isUsed(tim));

        OAFilter xor = new OAXorFilter(nameBob, young);
        assertTrue(xor.isUsed(bob));
        assertTrue(xor.isUsed(sue));
        assertFalse(xor.isUsed(tim));
    }

    @Test
    void blockCompositionWithPathBasedRealFilters() {
        Bean bob = new Bean("Bob", 42, true);
        Bean sue = new Bean("Sue", 42, false);

        OAFilter f = new OABlockFilter(
            new OAStartsWithFilter("name", "B"),
            new OAGreaterOrEqualFilter("age", 40),
            new OATrueFilter("active")
        );

        assertTrue(f.isUsed(bob));
        assertFalse(f.isUsed(sue));
    }
}
