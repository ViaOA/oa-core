package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.annotation.OAProperty;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAPathStringAndFormatTest {

    public static class Root extends OAObject {
        private String name;
        private double amount;
        private int count;

        public Root() {
        }

        public Root(String name, double amount, int count) {
            this.name = name;
            this.amount = amount;
            this.count = count;
        }

        public String getName() {
            return name;
        }

        @OAProperty(decimalPlaces = 2)
        public double getAmount() {
            return amount;
        }

        @OAProperty(format = "0000")
        public int getCount() {
            return count;
        }
    }

    @Test
    void getValueAsStringUsesResolvedValue() {
        Root root = new Root("Bob", 12.345, 7);
        OAPath<Root> pp = new OAPath<>(Root.class, "name");

        assertEquals("Bob", pp.getValueAsString(root));
    }

    @Test
    void getValueAsStringWithExplicitFormatOverridesDefault() {
        Root root = new Root("Bob", 12.345, 7);
        OAPath<Root> pp = new OAPath<>(Root.class, "amount");

        assertEquals("12.3", pp.getValueAsString(null, root, "0.0"));
    }

    @Test
    void getFormatUsesOAPropertyFormatAnnotation() {
        OAPath<Root> pp = new OAPath<>(Root.class, "count");

        assertEquals("0000", pp.getFormat());
    }

    @Test
    void getFormatUsesOAPropertyDecimalPlacesForFloatingPoint() {
        OAPath<Root> pp = new OAPath<>(Root.class, "amount");

        assertNotNull(pp.getFormat());
        assertTrue(pp.getFormat().contains("."));
        assertTrue(pp.getFormat().endsWith("00"));
    }

    @Test
    void getValueAsStringUsesPathFormatWhenAvailable() {
        Root root = new Root("Bob", 12.345, 7);
        OAPath<Root> pp = new OAPath<>(Root.class, "count");

        assertEquals("0007", pp.getValueAsString(root));
    }

    @Test
    void getFormatIsCachedAndStable() {
        OAPath<Root> pp = new OAPath<>(Root.class, "amount");

        String f1 = pp.getFormat();
        String f2 = pp.getFormat();

        assertEquals(f1, f2);
    }
}
