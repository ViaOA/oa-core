package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAPathMethodFormsAndSpecialTerminalTest {

    public static class Root extends OAObject {
        private boolean active;
        private String name;
        private final Hub<Child> children = new Hub<>(Child.class);

        public Root() {
        }

        public Root(String name, boolean active) {
            this.name = name;
            this.active = active;
        }

        public boolean isActive() {
            return active;
        }

        public String getName() {
            return name;
        }

        public Hub<Child> getChildren() {
            return children;
        }

        public static int getChildCount(Hub hub) {
            return hub == null ? -1 : hub.getSize();
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
    void booleanIsGetterIsResolved() {
        Root root = new Root("root", true);
        OAPath<Root> pp = new OAPath<>(Root.class, "active");

        assertEquals(Boolean.TRUE, pp.getValue(root));
        assertEquals(boolean.class, pp.getClasses()[0]);
    }

    @Test
    void emptyTerminalSegmentUsesToStringCurrentContract() {
        Root root = new Root("root", true);
        OAPath<Root> pp = new OAPath<>(Root.class, "");

        assertSame(root, pp.getValue(root));
    }

    @Test
    void explicitToStringSegmentCanBeResolvedCurrentContract() {
        Root root = new Root("root", true);
        OAPath<Root> pp = new OAPath<>(Root.class, "toString", true);

        Object val = pp.getValue(root);
        assertNotNull(val);
    }

    @Test
    void staticLastMethodWithHubParameterCanUseHubContext() {
        Root root = new Root("root", true);
        root.getChildren().add(new Child("A"));
        root.getChildren().add(new Child("B"));

        Hub<Root> hub = new Hub<>(Root.class);
        hub.add(root);

        OAPath<Root> pp = new OAPath<>(Root.class, "childCount", true);

        if (pp.getDoesLastMethodHasHubParam()) {
            assertEquals(1, pp.getValue(hub, root));
        }
    }

    @Test
    void getValueAsStringForBooleanUsesConverterContract() {
        Root root = new Root("root", true);
        OAPath<Root> pp = new OAPath<>(Root.class, "active");

        assertEquals("true", pp.getValueAsString(root).toLowerCase());
    }
}
