package com.viaoa.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAReflectPrimitiveNullContractTest {

    public static class PrimitiveBean extends OAObject {
        private int count;
        private boolean active;
        private int getCountCalls;
        private int setCountCalls;

        public int getCount() {
            getCountCalls++;
            return count;
        }

        public void setCount(int count) {
            setCountCalls++;
            this.count = count;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public int getGetCountCalls() {
            return getCountCalls;
        }

        public int getSetCountCalls() {
            return setCountCalls;
        }
    }

    @Test
    void primitiveNullGetterReturnsNullForOAObjectPrimitiveProperty() throws Exception {
        PrimitiveBean bean = new PrimitiveBean();
        bean.setNull("Count");

        Method m = PrimitiveBean.class.getMethod("getCount");

        Object val = OAReflect.getPropertyValue(bean, m);

        assertNull(val);
    }

    @Test
    void primitiveNullGetterShouldNotInvokeGetterWhenNullPreservingContractRequires() throws Exception {
        PrimitiveBean bean = new PrimitiveBean();
        bean.setNull("Count");

        Method m = PrimitiveBean.class.getMethod("getCount");

        Object val = OAReflect.getPropertyValue(bean, m);

        assertNull(val);
        assertEquals(0, bean.getGetCountCalls(), "primitive-null read should not invoke getter side effects");
    }

    @Test
    void nullAssignmentToPrimitiveOAObjectPropertyMarksPrimitiveNull() throws Exception {
        PrimitiveBean bean = new PrimitiveBean();
        bean.setCount(5);

        Method m = PrimitiveBean.class.getMethod("setCount", int.class);

        OAReflect.setPropertyValue(bean, m, (Object) null);

        assertTrue(bean.isNull("Count"));
        assertEquals(1, bean.getSetCountCalls(), "setter was called only for initial explicit setCount");
    }

    @Test
    void nullAssignmentToPrimitiveBooleanMarksCorrectPropertyName() throws Exception {
        PrimitiveBean bean = new PrimitiveBean();
        Method m = PrimitiveBean.class.getMethod("setActive", boolean.class);

        OAReflect.setPropertyValue(bean, m, (Object) null);

        assertTrue(bean.isNull("Active"));
    }

    @Test
    void nullAssignmentToNonSetterPrimitiveMethodShouldNotDeriveWrongPropertyName() throws Exception {
        class OddBean extends OAObject {
            public void add(int x) {
            }
        }

        OddBean bean = new OddBean();
        Method m = OddBean.class.getMethod("add", int.class);

        OAReflect.setPropertyValue(bean, m, (Object) null);

        assertFalse(bean.isNull(""), "non-setter primitive method should not mark a bogus empty property name");
        assertFalse(bean.isNull("d"), "non-setter primitive method should not derive property by substring(3)");
    }
}
