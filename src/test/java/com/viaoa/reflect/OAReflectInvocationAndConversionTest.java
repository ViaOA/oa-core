package com.viaoa.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class OAReflectInvocationAndConversionTest {

    public static class Bean {
        private String name;
        private int count;
        private boolean active;
        private BigDecimal amount;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getThrows() {
            throw new IllegalStateException("getter failed");
        }

        public void setThrows(String value) {
            throw new IllegalArgumentException("setter failed");
        }
    }

    @Test
    void convertParameterFromStringUsesMethodParameterType() throws Exception {
        Method m = Bean.class.getMethod("setCount", int.class);

        Object val = OAReflect.convertParameterFromString(m, "42");

        assertEquals(Integer.valueOf(42), val);
    }

    @Test
    void convertParameterFromStringSupportsExplicitFormat() throws Exception {
        Method m = Bean.class.getMethod("setAmount", BigDecimal.class);

        Object val = OAReflect.convertParameterFromString(m, "1,234.50", "#,##0.00");

        assertEquals(new BigDecimal("1234.50"), val);
    }

    @Test
    void convertParameterFromStringReturnsNullForNonSingleParamMethod() throws Exception {
        Method m = Bean.class.getMethod("getName");

        assertNull(OAReflect.convertParameterFromString(m, "x"));
    }

    @Test
    void setPropertyValueWithObjectInvokesSetter() throws Exception {
        Bean bean = new Bean();
        Method m = Bean.class.getMethod("setName", String.class);

        OAReflect.setPropertyValue(bean, m, "Bob");

        assertEquals("Bob", bean.getName());
    }

    @Test
    void setPropertyValueWithStringConvertsAndInvokesSetter() throws Exception {
        Bean bean = new Bean();
        Method m = Bean.class.getMethod("setCount", int.class);

        OAReflect.setPropertyValue(bean, m, "42");

        assertEquals(42, bean.getCount());
    }

    @Test
    void getPropertyValueAsStringReturnsConvertedValue() throws Exception {
        Bean bean = new Bean();
        bean.setCount(42);

        Method m = Bean.class.getMethod("getCount");

        assertEquals("42", OAReflect.getPropertyValueAsString(bean, m));
    }

    @Test
    void getPropertyValueAsStringUsesProvidedNullValue() throws Exception {
        Bean bean = new Bean();
        Method m = Bean.class.getMethod("getName");

        assertEquals("NULL", OAReflect.getPropertyValueAsString(bean, m, null, "NULL"));
    }

    @Test
    void getterExceptionPreservesCauseAndContext() throws Exception {
        Bean bean = new Bean();
        Method m = Bean.class.getMethod("getThrows");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> OAReflect.getPropertyValue(bean, m));

        assertTrue(ex.getMessage().contains("Error calling Method"));
        assertNotNull(ex.getCause());
    }

    @Test
    void setterExceptionPreservesCause() throws Exception {
        Bean bean = new Bean();
        Method m = Bean.class.getMethod("setThrows", String.class);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> OAReflect.setPropertyValue(bean, m, "x"));

        assertNotNull(ex.getCause());
    }

    @Test
    void executeMethodWithMethodArrayReturnsNullForNullOrEmptyArray() {
        assertNull(OAReflect.executeMethod(new Bean(), (Method[]) null));
        assertNull(OAReflect.executeMethod(new Bean(), new Method[0]));
    }
}
