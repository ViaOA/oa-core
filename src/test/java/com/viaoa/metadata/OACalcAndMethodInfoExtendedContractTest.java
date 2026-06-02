package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OACalcAndMethodInfoExtendedContractTest {

    @Test
    void calcInfoLowerNameDefaultsFromNameAndCanBeOverridden() {
        OACalcInfo ci = new OACalcInfo("FullName", new String[] { "first", "last" });

        assertEquals("fullname", ci.getLowerName());

        ci.setLowerName("custom");
        assertEquals("custom", ci.getLowerName());
    }

    @Test
    void calcInfoNullDependentPropertiesAreAllowed() {
        OACalcInfo ci = new OACalcInfo("Calc", null);

        assertNull(ci.getDependentProperties());

        ci.setDependentProperties(new String[0]);
        assertArrayEquals(new String[0], ci.getDependentProperties());
    }

    @Test
    void calcInfoHubFlagIsConstructorControlledCurrentContract() {
        OACalcInfo objectCalc = new OACalcInfo("ObjectCalc", null);
        OACalcInfo hubCalc = new OACalcInfo("HubCalc", null, true);

        assertFalse(objectCalc.getIsForHub());
        assertTrue(hubCalc.getIsForHub());
    }

    @Test
    void methodInfoNullNameAndDependencyDefaultsAreSafe() {
        OAMethodInfo mi = new OAMethodInfo();

        assertNull(mi.getName());
        assertNull(mi.getViewDependentProperties());
        assertNull(mi.getContextDependentProperties());
        assertNull(mi.getObjectCallbackMethod());
    }

    @Test
    void methodInfoNameCanBeChangedWithoutSideEffects() {
        OAMethodInfo mi = new OAMethodInfo();

        mi.setName("A");
        assertEquals("A", mi.getName());

        mi.setName("B");
        assertEquals("B", mi.getName());

        mi.setName(null);
        assertNull(mi.getName());
    }
}
