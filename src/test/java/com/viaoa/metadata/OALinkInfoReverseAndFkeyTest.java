package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OALinkInfoReverseAndFkeyTest {

    public static class Parent extends OAObject {
    }

    public static class Child extends OAObject {
    }

    @Test
    void reverseLinkInfoCanBeStoredAndReturnedByIdentity() {
        OALinkInfo parentToChildren = new OALinkInfo("children", Child.class, OALinkInfo.TYPE_MANY, false, "parent");
        OALinkInfo childToParent = new OALinkInfo("parent", Parent.class, OALinkInfo.TYPE_ONE, false, "children");

        parentToChildren.setReverseLinkInfo(childToParent);

        assertSame(childToParent, parentToChildren.getReverseLinkInfo());
        assertEquals("parent", parentToChildren.getReverseName());
    }

    @Test
    void fkeyInfoListPreservesInsertionOrder() {
        OALinkInfo li = new OALinkInfo("parent", Parent.class, OALinkInfo.TYPE_ONE);

        OAFkeyInfo a = new OAFkeyInfo();
        OAFkeyInfo b = new OAFkeyInfo();

        li.getFkeyInfos().add(a);
        li.getFkeyInfos().add(b);

        List<OAFkeyInfo> list = li.getFkeyInfos();
        assertSame(a, list.get(0));
        assertSame(b, list.get(1));
    }

    @Test
    void fkeyPropertyPairReferencesArePreserved() {
        OAPropertyInfo from = new OAPropertyInfo();
        from.setName("parentId");
        OAPropertyInfo to = new OAPropertyInfo();
        to.setName("id");

        OAFkeyInfo fki = new OAFkeyInfo();
        fki.setFromPropertyInfo(from);
        fki.setToPropertyInfo(to);

        OALinkInfo li = new OALinkInfo("parent", Parent.class, OALinkInfo.TYPE_ONE);
        li.getFkeyInfos().add(fki);

        assertSame(from, li.getFkeyInfos().get(0).getFromPropertyInfo());
        assertSame(to, li.getFkeyInfos().get(0).getToPropertyInfo());
    }

    @Test
    void uniquePropertyGetMethodCanBeSetAndCleared() throws Exception {
        OALinkInfo li = new OALinkInfo("parent", Parent.class, OALinkInfo.TYPE_ONE);
        java.lang.reflect.Method m = Parent.class.getMethod("getClass");

        li.setUniquePropertyGetMethod(m);
        assertSame(m, li.getUniquePropertyGetMethod());

        li.setUniquePropertyGetMethod(null);
        assertNull(li.getUniquePropertyGetMethod());
    }

    @Test
    void oneAndManyTypeConstantsRemainStable() {
        assertEquals(0, OALinkInfo.ONE);
        assertEquals(1, OALinkInfo.MANY);
        assertEquals(0, OALinkInfo.TYPE_ONE);
        assertEquals(1, OALinkInfo.TYPE_MANY);
    }
}
