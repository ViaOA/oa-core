package com.viaoa.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class OAReflectSetterAndPartialCommitTest {

    public static class Bean {
        private String name = "original";
        private int count = 1;
        private boolean active = true;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            if ("bad".equals(name)) {
                throw new IllegalArgumentException("bad name");
            }
            this.name = name;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            if (count < 0) {
                throw new IllegalArgumentException("negative");
            }
            this.count = count;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }

    @Test
    void failedStringSetterDoesNotMutateProperty() throws Exception {
        Bean bean = new Bean();
        Method m = Bean.class.getMethod("setName", String.class);

        assertThrows(RuntimeException.class, () -> OAReflect.setPropertyValue(bean, m, "bad"));

        assertEquals("original", bean.getName());
    }

    @Test
    void failedPrimitiveSetterDoesNotMutateProperty() throws Exception {
        Bean bean = new Bean();
        Method m = Bean.class.getMethod("setCount", int.class);

        assertThrows(RuntimeException.class, () -> OAReflect.setPropertyValue(bean, m, -1));

        assertEquals(1, bean.getCount());
    }

    @Test
    void stringConversionFailureDoesNotMutateProperty() throws Exception {
        Bean bean = new Bean();
        Method m = Bean.class.getMethod("setCount", int.class);

        assertThrows(RuntimeException.class, () -> OAReflect.setPropertyValue(bean, m, "notANumber"));

        assertEquals(1, bean.getCount());
    }

    @Test
    void booleanStringSetterConvertsAndMutatesProperty() throws Exception {
        Bean bean = new Bean();
        Method m = Bean.class.getMethod("setActive", boolean.class);

        OAReflect.setPropertyValue(bean, m, "false");

        assertFalse(bean.isActive());
    }

    @Test
    void nullObjectOrNullMethodBoundaryBehaviorIsVisible() throws Exception {
        Method m = Bean.class.getMethod("setName", String.class);

        assertThrows(RuntimeException.class, () -> OAReflect.setPropertyValue(null, m, "x"));
        assertThrows(RuntimeException.class, () -> OAReflect.setPropertyValue(new Bean(), null, "x"));
    }

    @Test
    void getPropertyValueWithNullObjectOrMethodUsesDefinedFallback() throws Exception {
        Bean bean = new Bean();
        Method getName = Bean.class.getMethod("getName");

        assertNull(OAReflect.getPropertyValue(null, getName));
        assertSame(bean, OAReflect.getPropertyValue(bean, (Method) null));
    }
}
