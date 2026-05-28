package com.viaoa.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class OAReflectBooleanPropertyContractTest {

    public static class IsOnlyBean {
        private boolean active = true;
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    public static class GetAndIsBean {
        public boolean getActive() { return false; }
        public boolean isActive() { return true; }
    }

    public static class BooleanWrapperBean {
        private Boolean active = Boolean.TRUE;
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }

    @Test
    void isOnlyBooleanGetterIsResolvedForPropertyPath() {
        Method[] ms = OAReflect.getMethods(IsOnlyBean.class, "active");

        assertNotNull(ms);
        assertEquals(1, ms.length);
        assertEquals("isActive", ms[0].getName());
        assertEquals(Boolean.TRUE, OAReflect.getPropertyValue(new IsOnlyBean(), ms));
    }

    @Test
    void getGetterPrecedenceOverIsGetterIsDocumented() {
        Method[] ms = OAReflect.getMethods(GetAndIsBean.class, "active");

        assertNotNull(ms);
        assertEquals(1, ms.length);
        assertEquals("getActive", ms[0].getName());
        assertEquals(Boolean.FALSE, OAReflect.getPropertyValue(new GetAndIsBean(), ms));
    }

    @Test
    void booleanWrapperGetterAndSetterResolveNormally() {
        Method[] ms = OAReflect.getMethods(BooleanWrapperBean.class, "active");
        assertNotNull(ms);
        assertEquals(Boolean.TRUE, OAReflect.getPropertyValue(new BooleanWrapperBean(), ms));

        BooleanWrapperBean bean = new BooleanWrapperBean();
        Method setter = OAReflect.getMethod(BooleanWrapperBean.class, "setActive", Boolean.class);
        assertNotNull(setter);

        OAReflect.setPropertyValue(bean, setter, Boolean.FALSE);

        assertEquals(Boolean.FALSE, bean.getActive());
    }

    @Test
    void isActiveMapsToSetActiveForManualLookup() {
        Method setter = OAReflect.getMethod(IsOnlyBean.class, "setActive", Boolean.class);

        assertNotNull(setter);
        assertEquals("setActive", setter.getName());
        assertEquals(boolean.class, setter.getParameterTypes()[0]);
    }

    @Test
    void invalidBooleanPropertyDoesNotFallBackToSimilarName() {
        assertNull(OAReflect.getMethods(IsOnlyBean.class, "activeFlag", false));
        assertThrows(RuntimeException.class, () -> OAReflect.getMethods(IsOnlyBean.class, "activeFlag", true));
    }
}
