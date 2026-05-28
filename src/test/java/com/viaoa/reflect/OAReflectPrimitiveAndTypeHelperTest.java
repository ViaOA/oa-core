package com.viaoa.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

class OAReflectPrimitiveAndTypeHelperTest {

    @Test
    void primitiveClassWrapperMapsPrimitivesToWrappers() {
        assertEquals(Integer.class, OAReflect.getPrimitiveClassWrapper(int.class));
        assertEquals(Boolean.class, OAReflect.getPrimitiveClassWrapper(boolean.class));
        assertEquals(Long.class, OAReflect.getPrimitiveClassWrapper(long.class));
        assertEquals(Double.class, OAReflect.getPrimitiveClassWrapper(double.class));
        assertEquals(String.class, OAReflect.getPrimitiveClassWrapper(String.class));
        assertNull(OAReflect.getPrimitiveClassWrapper(null));
    }

    @Test
    void getClassWrapperMapsPrimitivesAndLeavesReferenceTypes() {
        assertEquals(Integer.class, OAReflect.getClassWrapper(int.class));
        assertEquals(Boolean.class, OAReflect.getClassWrapper(boolean.class));
        assertEquals(String.class, OAReflect.getClassWrapper(String.class));
    }

    @Test
    void primitiveClassWrapperObjectReturnsJavaDefaults() {
        assertEquals(Integer.valueOf(0), OAReflect.getPrimitiveClassWrapperObject(int.class));
        assertEquals(Boolean.FALSE, OAReflect.getPrimitiveClassWrapperObject(boolean.class));
        assertEquals(Long.valueOf(0), OAReflect.getPrimitiveClassWrapperObject(long.class));
        assertEquals(Double.valueOf(0.0d), OAReflect.getPrimitiveClassWrapperObject(double.class));
        assertEquals(Character.valueOf((char) 0), OAReflect.getPrimitiveClassWrapperObject(char.class));
        assertNull(OAReflect.getPrimitiveClassWrapperObject(String.class));
    }

    @Test
    void isPrimitiveClassWrapperRecognizesWrapperTypesOnly() {
        assertTrue(OAReflect.isPrimitiveClassWrapper(Integer.class));
        assertTrue(OAReflect.isPrimitiveClassWrapper(Boolean.class));
        assertTrue(OAReflect.isPrimitiveClassWrapper(Character.class));

        assertFalse(OAReflect.isPrimitiveClassWrapper(int.class));
        assertFalse(OAReflect.isPrimitiveClassWrapper(String.class));
        assertFalse(OAReflect.isPrimitiveClassWrapper(null));
    }

    @Test
    void isNumberIntegerAndFloatUseWrapperSemantics() {
        assertTrue(OAReflect.isNumber(int.class));
        assertTrue(OAReflect.isNumber(Integer.class));
        assertTrue(OAReflect.isNumber(BigDecimal.class));
        assertFalse(OAReflect.isNumber(String.class));
        assertFalse(OAReflect.isNumber(null));

        assertTrue(OAReflect.isInteger(int.class));
        assertTrue(OAReflect.isInteger(BigInteger.class));
        assertFalse(OAReflect.isInteger(double.class));

        assertTrue(OAReflect.isFloat(double.class));
        assertTrue(OAReflect.isFloat(BigDecimal.class));
        assertFalse(OAReflect.isFloat(long.class));
    }

    @Test
    void isEqualEvenIfWrapperMatchesPrimitiveWrapperPairsOnlyForNonNumericFamilies() {
        assertTrue(OAReflect.isEqualEvenIfWrapper(int.class, Integer.class));
        assertTrue(OAReflect.isEqualEvenIfWrapper(Boolean.class, boolean.class));
        assertTrue(OAReflect.isEqualEvenIfWrapper(String.class, String.class));

        assertFalse(OAReflect.isEqualEvenIfWrapper(String.class, Object.class));
        assertFalse(OAReflect.isEqualEvenIfWrapper(null, Integer.class));
        assertFalse(OAReflect.isEqualEvenIfWrapper(Integer.class, null));
    }

    @Test
    void isEqualEvenIfWrapperShouldNotTreatAllNumbersAsEquivalent() {
        assertFalse(OAReflect.isEqualEvenIfWrapper(Integer.class, Long.class));
        assertFalse(OAReflect.isEqualEvenIfWrapper(BigDecimal.class, Double.class));
    }

    @Test
    void emptyPrimitiveBooleanShouldMatchJavaDefaultFalse() {
        assertEquals(Boolean.FALSE, OAReflect.getEmptyPrimitive(boolean.class));
    }

    @Test
    void emptyPrimitiveNumericDefaultsMatchJavaDefaults() {
        assertEquals(Integer.valueOf(0), OAReflect.getEmptyPrimitive(int.class));
        assertEquals(Long.valueOf(0L), OAReflect.getEmptyPrimitive(long.class));
        assertEquals(Short.valueOf((short) 0), OAReflect.getEmptyPrimitive(short.class));
        assertEquals(Double.valueOf(0.0d), OAReflect.getEmptyPrimitive(double.class));
        assertEquals(Float.valueOf(0.0f), OAReflect.getEmptyPrimitive(float.class));
    }

    @Test
    void emptyPrimitiveWrapperBehaviorMatchesDocumentedContract() {
        assertEquals(Boolean.FALSE, OAReflect.getEmptyPrimitive(Boolean.class));
        assertEquals(Integer.valueOf(0), OAReflect.getEmptyPrimitive(Integer.class));
    }

    @Test
    void getClassForMethodReturnsReturnTypeOrSingleParameterType() throws Exception {
        assertEquals(String.class, OAReflect.getClass(Sample.class.getMethod("getName")));
        assertEquals(String.class, OAReflect.getClass(Sample.class.getMethod("setName", String.class)));
        assertNull(OAReflect.getClass(Sample.class.getMethod("clear")));
        assertNull(OAReflect.getClass(Sample.class.getMethod("multi", String.class, String.class)));
        assertNull(OAReflect.getClass(null));
    }

    public static class Sample {
        public String getName() { return "x"; }
        public void setName(String name) { }
        public void clear() { }
        public void multi(String a, String b) { }
    }
}
