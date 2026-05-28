package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAFinderLoadedOnlyAndDataNotFoundTest {

    public static class Root extends OAObject {
        private final Hub<Child> children = new Hub<>(Child.class);
        private Child child;

        public Hub<Child> getChildren() {
            return children;
        }

        public Child getChild() {
            return child;
        }

        public void setChild(Child child) {
            this.child = child;
        }
    }

    public static class Child extends OAObject {
        private String name;

        public Child() {
        }

        public Child(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Test
    void loadedOnlyTraversalStillFindsAlreadyLoadedHubObjects() {
        Root root = new Root();
        root.getChildren().add(new Child("A"));
        root.getChildren().add(new Child("B"));

        OAFinder<Root, Child> finder = new OAFinder<>("children");
        finder.setUseOnlyLoadedData(true);

        assertEquals(2, finder.find(root).size());
    }

    @Test
    void onDataNotFoundHookIsNotCalledForLoadedData() {
        Root root = new Root();
        root.getChildren().add(new Child("A"));
        AtomicInteger cnt = new AtomicInteger();

        OAFinder<Root, Child> finder = new OAFinder<>("children") {
            @Override
            protected void onDataNotFound() {
                cnt.incrementAndGet();
            }
        };
        finder.setUseOnlyLoadedData(true);

        assertEquals(1, finder.find(root).size());
        assertEquals(0, cnt.get());
    }

    @Test
    void loadedOnlyWithNullLoadedReferenceReturnsEmptyWithoutHookCurrentContract() {
        Root root = new Root();
        AtomicInteger cnt = new AtomicInteger();

        OAFinder<Root, Child> finder = new OAFinder<>("child") {
            @Override
            protected void onDataNotFound() {
                cnt.incrementAndGet();
            }
        };
        finder.setUseOnlyLoadedData(true);

        assertTrue(finder.find(root).isEmpty());
        assertEquals(0, cnt.get());
    }
}
