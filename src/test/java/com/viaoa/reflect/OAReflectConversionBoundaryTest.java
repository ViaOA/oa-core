package com.viaoa.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class OAReflectConversionBoundaryTest {

    public static class Bean {
        private int count;
        private long longValue;
        private BigDecimal amount;

        public void setCount(int count) { this.count = count; }
        public int getCount() { return count; }
        public void setLongValue(long longValue) { this.longValue = longValue; }
        public long getLongValue() { return longValue; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public BigDecimal getAmount() { return amount; }
    }

    @Test
    void lookupDoesNotConvertStringArgumentToNumericParameter() {
        Method m = OAReflect.getMethod(Bean.class, "setCount", new Object[] { "42" });

        assertNull(m, "method lookup should not convert String to int; conversion belongs to setPropertyValue(String)");
    }

    @Test
    void setPropertyValueStringIsTheConversionBoundary() throws Exception {
        Bean bean = new Bean();
        Method m = Bean.class.getMethod("setCount", int.class);

        OAReflect.setPropertyValue(bean, m, "42");

        assertEquals(42, bean.getCount());
    }

    @Test
    void longDoesNotMatchIntParameterWithoutExplicitConversion() {
        Method m = OAReflect.getMethod(Bean.class, "setCount", new Object[] { Long.valueOf(42L) });

        assertNull(m);
    }

    @Test
    void bigDecimalDoesNotMatchLongParameterWithoutExplicitConversion() {
        Method m = OAReflect.getMethod(Bean.class, "setLongValue", new Object[] { new BigDecimal("42") });

        assertNull(m);
    }

    @Test
    void explicitBigDecimalConversionPreservesValue() throws Exception {
        Bean bean = new Bean();
        Method m = Bean.class.getMethod("setAmount", BigDecimal.class);

        OAReflect.setPropertyValue(bean, m, "1234.50", "#,##0.00");

        assertEquals(new BigDecimal("1234.50"), bean.getAmount());
    }

    @Test
    void invalidExplicitConversionFailsWithoutMutation() throws Exception {
        Bean bean = new Bean();
        bean.setCount(7);
        Method m = Bean.class.getMethod("setCount", int.class);

        assertThrows(RuntimeException.class, () -> OAReflect.setPropertyValue(bean, m, "bad-number"));

        assertEquals(7, bean.getCount());
    }
}
