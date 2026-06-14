package com.viaoa.metadata.pojo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.viaoa.metadata.OAObjectInfo;

class PojoDelegateTest {
    @Test
    void lookupMethodsFindPropertiesLinksAndKeysCaseInsensitively() {
        Pojo pojo = samplePojo();

        assertSame(pojo.getPojoRegularProperties().get(0).getPojoProperty(), PojoDelegate.getPojoProperty(pojo, "ID"));
        assertSame(pojo.getPojoRegularProperties().get(1), PojoDelegate.getPojoRegularProperty(pojo, "Name"));
        assertSame(pojo.getPojoLinks().get(0), PojoDelegate.getPojoLink(pojo, "Store"));
        assertTrue(PojoDelegate.hasKey(pojo));
        assertFalse(PojoDelegate.hasCompoundKey(pojo));
        assertEquals(3, PojoDelegate.getPojoProperties(pojo).size());
        assertEquals(1, PojoDelegate.getPojoPropertyKeys(pojo).size());
        assertNull(PojoDelegate.getPojoProperty(null, "id"));
    }

    @Test
    void objectInfoKeyHelpersReflectConfiguredMetadata() {
        OAObjectInfo pkey = new OAObjectInfo(new String[] { "id" });
        assertFalse(PojoDelegate.hasPkey(pkey));

        OAObjectInfo noKey = new OAObjectInfo();
        assertFalse(PojoDelegate.hasPkey(noKey));
        assertFalse(PojoDelegate.hasImportMatchKey(noKey));
        assertFalse(PojoDelegate.hasLinkUniqueKey(noKey));
    }

    static Pojo samplePojo() {
        Pojo pojo = new Pojo();
        pojo.setName("Sample");
        PojoProperty id = prop("id", 1);
        PojoProperty name = prop("name", -1);
        PojoRegularProperty idReg = new PojoRegularProperty();
        idReg.setPojo(pojo);
        idReg.setPojoProperty(id);
        id.setPojoRegularProperty(idReg);
        PojoRegularProperty nameReg = new PojoRegularProperty();
        nameReg.setPojo(pojo);
        nameReg.setPojoProperty(name);
        name.setPojoRegularProperty(nameReg);
        pojo.setPojoRegularProperties(List.of(idReg, nameReg));

        PojoLink link = new PojoLink();
        link.setName("store");
        PojoLinkOne one = new PojoLinkOne();
        link.setPojoLinkOne(one);
        one.setPojoLink(link);
        PojoLinkFkey fk = new PojoLinkFkey();
        PojoProperty storeId = prop("storeId", 2);
        fk.setPojoLinkOne(one);
        fk.setPojoProperty(storeId);
        storeId.setPojoLinkFkey(fk);
        one.setPojoLinkFkeys(List.of(fk));
        pojo.setPojoLinks(List.of(link));
        return pojo;
    }

    static PojoProperty prop(String name, int keyPos) {
        PojoProperty prop = new PojoProperty();
        prop.setName(name);
        prop.setUpperName(name.toUpperCase());
        prop.setPropertyPath(name);
        prop.setJavaType(String.class.getName());
        prop.setKeyPos(keyPos);
        return prop;
    }
}
