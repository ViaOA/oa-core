package com.viaoa.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class OAReflectFalseSuccessPreventionTest {

    public static class Bean {
        private String name = "name";
        private String names = "names";
        private int count = 1;

        public String getName() { return name; }
        public String getNames() { return names; }
        public int getCount() { return count; }

        public void setCount(int count) {
            if (count == 99) throw new IllegalArgumentException("bad");
            this.count = count;
        }

        public String getThrows() {
            throw new IllegalStateException("boom");
        }
    }

    @Test
    void similarPropertyNameIsNotUsedAsFallback() {
        assertEquals("name", OAReflect.executeMethod(new Bean(), "name"));
        assertEquals("names", OAReflect.executeMethod(new Bean(), "names"));

        assertThrows(RuntimeException.class, () -> OAReflect.executeMethod(new Bean(), "nam"));
    }

    @Test
    void malformedPathDoesNotInvokeToStringAsHiddenSegment() {
        assertThrows(RuntimeException.class, () -> OAReflect.executeMethod(new Bean(), "name."));
        assertThrows(RuntimeException.class, () -> OAReflect.executeMethod(new Bean(), ".name"));
        assertThrows(RuntimeException.class, () -> OAReflect.executeMethod(new Bean(), "name..length"));
    }

    @Test
    void getterExceptionDoesNotReturnStaleOrFallbackValue() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> OAReflect.executeMethod(new Bean(), "throws"));

        assertNotNull(ex.getCause());
        assertTrue(ex.getMessage().contains("Error calling Method"));
    }

    @Test
    void failedSetterDoesNotClaimSuccessOrMutate() throws Exception {
        Bean bean = new Bean();
        Method m = Bean.class.getMethod("setCount", int.class);

        assertThrows(RuntimeException.class, () -> OAReflect.setPropertyValue(bean, m, 99));

        assertEquals(1, bean.getCount());
    }

    @Test
    void setPropertyValueWrongValueTypeFailsWithoutMutation() throws Exception {
        Bean bean = new Bean();
        Method m = Bean.class.getMethod("setCount", int.class);

        assertThrows(RuntimeException.class, () -> OAReflect.setPropertyValue(bean, m, "not-converted-here"));

        assertEquals(1, bean.getCount());
    }
}
