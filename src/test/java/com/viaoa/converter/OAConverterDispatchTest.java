package com.viaoa.converter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.viaoa.converter.internal.OAConverterInterface;

class OAConverterDispatchTest {

    @Test
    void convertUsesRequestedTargetType() {
        Object integer = OAConverter.convert(Integer.class, "42");
        Object string = OAConverter.convert(String.class, 42);

        assertInstanceOf(Integer.class, integer);
        assertEquals(42, integer);
        assertInstanceOf(String.class, string);
        assertEquals("42", string);
    }

    @Test
    void primitiveTargetsUseWrapperConverters() {
        assertEquals(42, OAConverter.convert(int.class, "42"));
        assertEquals(42L, OAConverter.convert(long.class, "42"));
        assertEquals(4.25d, OAConverter.convert(double.class, "4.25"));
        assertEquals(Boolean.TRUE, OAConverter.convert(boolean.class, "true"));
        assertEquals(Character.valueOf('x'), OAConverter.convert(char.class, "x"));
    }

    @Test
    void assignableValueReturnsSameInstanceWhenNoFormat() {
        String value = new String("same");

        assertSame(value, OAConverter.convert(String.class, value));
        assertSame(value, OAConverter.convert(String.class, value, null));
    }

    @Test
    void oaConvMatchesOAConverterForRepresentativeCalls() {
        assertEquals(OAConverter.convert(Integer.class, "42"), OAConv.convert(Integer.class, "42"));
        assertEquals(OAConverter.toString(42), OAConv.toString(42));
        assertEquals(OAConverter.toBoolean("true"), OAConv.toBoolean("true"));
    }

    @Test
    void converterLookupFindsSuperclassConverter() {
        OAConverterInterface<CustomInteger> converter = OAConverter.getConverter(CustomInteger.class);

        assertNotNull(converter);
        assertSame(OAConverter.getConverter(Number.class), converter);
    }

    @Test
    void unknownTargetWithoutConverterReturnsNull() {
        assertNull(OAConverter.getConverter(UnregisteredTarget.class));
        assertNull(OAConverter.convert(UnregisteredTarget.class, "value"));
    }

    @Test
    void customConverterRegistrationIsRestoredAfterTest() {
        OAConverterInterface<String> original = OAConverter.getConverter(String.class);
        OAConverterInterface<String> converter = new OAConverterInterface<>() {
            @Override
            public String convert(Class<String> thisClass, Object fromValue, String fmt) {
                return "custom:" + fromValue + ":" + fmt;
            }

            @Override
            public String convertToString(String fromValue, String fmt) {
                return "customString:" + fromValue + ":" + fmt;
            }
        };

        try {
            OAConverter.addConverter(String.class, converter);

            assertEquals("custom:abc:fmt", OAConverter.convert(String.class, "abc", "fmt"));
            assertSame(converter, OAConverter.getConverter(String.class));
        } finally {
            OAConverter.addConverter(String.class, original);
        }

        assertSame(original, OAConverter.getConverter(String.class));
    }

    private static final class CustomInteger extends Number {
        private static final long serialVersionUID = 1L;

        @Override
        public int intValue() {
            return 0;
        }

        @Override
        public long longValue() {
            return 0L;
        }

        @Override
        public float floatValue() {
            return 0.0f;
        }

        @Override
        public double doubleValue() {
            return 0.0d;
        }
    }

    private static final class UnregisteredTarget {
    }
}
