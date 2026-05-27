package com.viaoa.converter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.viaoa.converter.internal.OAConverterInterface;
import com.viaoa.lang.oa.VEnum;

class OAConverterEnumClassVEnumTest {

    @Test
    void enumFromExactStringName() {
        assertEquals(Color.RED, OAConverter.convert(Color.class, "RED"));
    }

    @Test
    void enumFromCaseInsensitiveStringName() {
        assertEquals(Color.GREEN, OAConverter.convert(Color.class, "green"));
    }

    @Test
    void enumFromOrdinalNumber() {
        assertEquals(Color.BLUE, OAConverter.convert(Color.class, 2));
    }

    @Test
    void invalidEnumNameReturnsNull() {
        assertNull(OAConverter.convert(Color.class, "purple"));
    }

    @Test
    void invalidEnumOrdinalReturnsNull() {
        assertNull(OAConverter.convert(Color.class, -1));
        assertNull(OAConverter.convert(Color.class, 99));
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void wrongEnumClassCurrentlyReturnsInputEnumInstance() {
        Object value = OAConverter.convert((Class) Color.class, Size.SMALL);

        assertSame(Size.SMALL, value);
    }

    @Test
    void enumToStringUsesEnumName() {
        assertEquals("BLUE", OAConverter.toString(Color.BLUE));
        assertEquals("BLUE", OAConverter.convert(String.class, Color.BLUE));
    }

    @Test
    void classFromFullyQualifiedClassName() {
        assertSame(String.class, OAConverter.convert(Class.class, "java.lang.String"));
    }

    @Test
    void invalidClassNameReturnsNull() {
        assertNull(OAConverter.convert(Class.class, "no.such.Type"));
    }

    @Test
    void classToStringUsesFullyQualifiedName() {
        assertEquals("java.lang.Integer", OAConverter.toString(Integer.class));
        assertEquals("java.lang.Integer", OAConverter.convert(String.class, Integer.class));
    }

    @Test
    void venumConversionOnlySupportsDirectVEnumInstances() {
        VEnum value = new VEnum();
        value.setName("Active");
        value.setDisplay("Active display");
        value.setValue(7);

        assertSame(value, OAConverter.convert(VEnum.class, value));
        assertSame(value, OAConverter.convert(VEnum.class, value, "10L"));
        assertNull(OAConverter.convert(VEnum.class, "Active"));
        assertNull(OAConverter.convert(VEnum.class, 7));
    }

    @Test
    void venumToStringUsesNameAndFormat() {
        VEnum value = new VEnum();
        value.setName("ActiveStatus");

        assertEquals("ActiveStatus", OAConverter.toString(value));
        assertEquals("Active...", OAConverter.toString(value, "9L."));
    }

    @Test
    void venumNullDirectAndCentralStringBehavior() {
        OAConverterInterface<VEnum> converter = OAConverter.getConverter(VEnum.class);

        assertNull(converter.convert(VEnum.class, null, null));
        String s = converter.convertToString(null, null);
        assertEquals("", s);
        assertEquals("", OAConverter.toString((VEnum) null));
    }

    private enum Color {
        RED, GREEN, BLUE
    }

    private enum Size {
        SMALL, LARGE
    }
}
