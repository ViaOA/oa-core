package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.path.OAPath;

import org.junit.jupiter.api.Test;

class OAFilterDelegateSmokeTest {

    public static class Bean {
        private Child child;

        public Child getChild() {
            return child;
        }

        public void setChild(Child child) {
            this.child = child;
        }
    }

    public static class Child {
        private String name;

        public Child(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Test
    void createFinderForSimpleNonHubPathReturnsNullFinderInfoCurrentContract() {
        OAFilterDelegate.FinderInfo fi = OAFilterDelegate.createFinder(Bean.class, new OAPath(Bean.class, "child.name"));

        assertNull(fi);
    }

    @Test
    void createFinderHandlesNullInputsCurrentContract() {
        assertNull(OAFilterDelegate.createFinder(null, new OAPath("child.name")));
        assertNull(OAFilterDelegate.createFinder(Bean.class, null));
    }

    @Test
    void getPropertyValueThroughDelegateStylePathWorksThroughFilters() {
        Bean bean = new Bean();
        bean.setChild(new Child("abc"));

        assertTrue(new OAEqualFilter("child.name", "abc").isUsed(bean));
        assertTrue(new OAContainsFilter("child.name", "b").isUsed(bean));
        assertTrue(new OAStartsWithFilter("child.name", "a").isUsed(bean));
    }
}
