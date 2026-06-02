package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectInfoLookupMutationRegressionTest {

    public static class Child extends OAObject {
    }

    @Test
    void propertyNameMutationRequiresResetToRefreshLookupContract() {
        OAObjectInfo oi = new OAObjectInfo();
        OAPropertyInfo pi = new OAPropertyInfo();
        pi.setName("OldName");
        oi.addPropertyInfo(pi);

        assertSame(pi, oi.getPropertyInfo("oldname"));

        pi.setName("NewName");

        oi.resetPropertyInfo();

        assertNull(oi.getPropertyInfo("oldname"));
        assertSame(pi, oi.getPropertyInfo("newname"));
    }

    @Test
    void removingPropertyFromBackingListRequiresResetToRefreshLookupContract() {
        OAObjectInfo oi = new OAObjectInfo();
        OAPropertyInfo pi = new OAPropertyInfo();
        pi.setName("Name");
        oi.addPropertyInfo(pi);

        assertSame(pi, oi.getPropertyInfo("name"));

        oi.getPropertyInfos().remove(pi);
        oi.resetPropertyInfo();

        assertNull(oi.getPropertyInfo("name"));
    }

    @Test
    void linkNameMutationIsReflectedAfterLookupRebuildBySetLinkInfos() {
        OAObjectInfo oi = new OAObjectInfo();
        OALinkInfo li = new OALinkInfo("OldLink", Child.class, OALinkInfo.TYPE_ONE);
        oi.addLinkInfo(li);

        assertSame(li, oi.getLinkInfo("oldlink"));

        li.setName("NewLink");
        oi.setLinkInfos(oi.getLinkInfos());

        assertSame(li, oi.getLinkInfo("newlink"));
    }

    @Test
    void calcNameMutationIsReflectedAfterReAddCurrentContract() {
        OAObjectInfo oi = new OAObjectInfo();
        OACalcInfo ci = new OACalcInfo("OldCalc", new String[] { "a" });
        oi.addCalcInfo(ci);

        assertSame(ci, oi.getCalcInfo("oldcalc"));

        ci.setName("NewCalc");
        oi.getCalcInfos().clear();
        oi.addCalcInfo(ci);

        assertNull(oi.getCalcInfo("oldcalc"));
        assertSame(ci, oi.getCalcInfo("newcalc"));
    }

    @Test
    void methodNameMutationIsReflectedAfterReAddCurrentContract() {
        OAObjectInfo oi = new OAObjectInfo();
        OAMethodInfo mi = new OAMethodInfo();
        mi.setName("OldMethod");
        oi.addMethodInfo(mi);

        assertSame(mi, oi.getMethodInfo("oldmethod"));

        mi.setName("NewMethod");
        oi.getMethodInfos().clear();
        oi.addMethodInfo(mi);

        assertNull(oi.getMethodInfo("oldmethod"));
        assertSame(mi, oi.getMethodInfo("newmethod"));
    }

    @Test
    void addingMetadataWithNullNameIsSafeAndDoesNotBreakValidLookup() {
        OAObjectInfo oi = new OAObjectInfo();

        OAPropertyInfo nullProp = new OAPropertyInfo();
        oi.addPropertyInfo(nullProp);

        OAPropertyInfo prop = new OAPropertyInfo();
        prop.setName("Name");
        oi.addPropertyInfo(prop);

        OALinkInfo link = new OALinkInfo(null, Child.class, OALinkInfo.TYPE_ONE);
        oi.addLinkInfo(link);

        OALinkInfo validLink = new OALinkInfo("Child", Child.class, OALinkInfo.TYPE_ONE);
        oi.addLinkInfo(validLink);

        assertSame(prop, oi.getPropertyInfo("name"));
        assertSame(validLink, oi.getLinkInfo("child"));
    }
}
