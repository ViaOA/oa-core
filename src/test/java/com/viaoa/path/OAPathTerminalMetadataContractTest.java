package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.annotation.OACalculatedProperty;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAPathTerminalMetadataContractTest {

    public static class Root extends OAObject {
        private String name = "root";
        private Child child = new Child("child");
        private final Hub<Child> children = new Hub<>(Child.class);

        @OAProperty(maxLength = 40)
        public String getName() {
            return name;
        }

        @OAOne
        public Child getChild() {
            return child;
        }

        public Hub<Child> getChildren() {
            return children;
        }

        @OACalculatedProperty
        public String getCalcName() {
            return "calc-" + name;
        }
    }

    public static class Child extends OAObject {
        private String name;

        public Child() {
        }

        public Child(String name) {
            this.name = name;
        }

        @OAProperty(maxLength = 20)
        public String getName() {
            return name;
        }
    }

    @Test
    void terminalScalarPropertySetsEndPropertyInfoOnly() {
        OAPath<Root> pp = new OAPath<>(Root.class, "name");

        assertNotNull(pp.getEndPropertyInfo());
        assertNull(pp.getEndLinkInfo());
        assertNull(pp.getEndCalcInfo());
    }

    @Test
    void terminalOneLinkSetsEndLinkInfoOnly() {
        OAPath<Root> pp = new OAPath<>(Root.class, "child");

        assertNotNull(pp.getEndLinkInfo());
        assertNull(pp.getEndPropertyInfo());
        assertNull(pp.getEndCalcInfo());
    }

    @Test
    void terminalManyLinkSetsEndLinkInfoAndHubProperty() {
        OAPath<Root> pp = new OAPath<>(Root.class, "children");

        assertNotNull(pp.getEndLinkInfo());
        assertTrue(pp.getHasHubProperty());
        assertNull(pp.getEndPropertyInfo());
    }

    @Test
    void terminalCalculatedPropertySetsEndCalcInfo() {
        OAPath<Root> pp = new OAPath<>(Root.class, "calcName");

        assertNotNull(pp.getEndCalcInfo());
        assertNull(pp.getEndLinkInfo());
    }

    @Test
    void mixedLinkScalarPathUpdatesTerminalMetadataToScalar() {
        OAPath<Root> pp = new OAPath<>(Root.class, "child.name");

        assertNotNull(pp.getEndPropertyInfo());
        assertNull(pp.getEndLinkInfo());
        assertEquals("name", pp.getLastPropertyName());
    }

    @Test
    void repeatedSetupDoesNotLeaveStaleTerminalLinkMetadata() {
        OAPath<Root> pp = new OAPath<>("child");

        pp.setup(Root.class);
        assertNotNull(pp.getEndLinkInfo());

        // Current contract should not append/reinterpret on repeated setup.
        pp.setup(Root.class);

        assertNotNull(pp.getEndLinkInfo());
        assertNull(pp.getEndPropertyInfo());
        assertArrayEquals(new String[] { "child" }, pp.getProperties());
    }
}
