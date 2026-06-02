package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OALinkInfoExtendedContractTest {

    public static class Parent extends OAObject { }
    public static class Child extends OAObject { }

    @Test
    void noPojoAndPojoNamesRoundTrip() {
        OALinkInfo li = new OALinkInfo("children", Child.class, OALinkInfo.TYPE_MANY);

        assertFalse(li.getNoPojo());

        li.setNoPojo(true);
        li.setPojoNames(new String[] { "items", "children" });

        assertTrue(li.getNoPojo());
        assertArrayEquals(new String[] { "items", "children" }, li.getPojoNames());
    }

    @Test
    void serverSideAndPrivateMethodFlagsAreIndependent() {
        OALinkInfo li = new OALinkInfo("children", Child.class, OALinkInfo.TYPE_MANY);

        li.setServerSideCalc(true);
        assertTrue(li.getServerSideCalc());
        assertFalse(li.getPrivateMethod());

        li.setPrivateMethod(true);
        assertTrue(li.getServerSideCalc());
        assertTrue(li.getPrivateMethod());
    }

    @Test
    void calculatedLinkMetadataIsIndependentOfCalcDependencies() {
        OALinkInfo li = new OALinkInfo("children", Child.class, OALinkInfo.TYPE_MANY);

        li.setCalculated(true);
        assertTrue(li.getCalculated());
        assertNull(li.getCalcDependentProperties());

        li.setCalcDependentProperties(new String[] { "a", "b" });
        assertTrue(li.getCalculated());
        assertArrayEquals(new String[] { "a", "b" }, li.getCalcDependentProperties());
    }

    @Test
    void matchAndAutoCreateFieldsRemainIndependent() {
        OALinkInfo li = new OALinkInfo("child", Child.class, OALinkInfo.TYPE_ONE);

        li.setMatchHub("possibleChildren");
        li.setMatchProperty("code");
        li.setMatchStopProperty("stop");
        li.setAutoCreateNew(true);
        li.setAutoCreateProperty("createChild");

        assertEquals("possibleChildren", li.getMatchHub());
        assertEquals("code", li.getMatchProperty());
        assertEquals("stop", li.getMatchStopProperty());
        assertTrue(li.getAutoCreateNew());
        assertEquals("createChild", li.getAutoCreateProperty());
    }

    @Test
    void oneAndOnlyOneDoesNotImplicitlySetRequiredCurrentContract() {
        OALinkInfo li = new OALinkInfo("child", Child.class, OALinkInfo.TYPE_ONE);

        li.setOneAndOnlyOne(true);

        assertTrue(li.getOneAndOnlyOne());
        assertFalse(li.getRequired());
    }
}
