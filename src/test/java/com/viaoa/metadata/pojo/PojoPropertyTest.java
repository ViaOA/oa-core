package com.viaoa.metadata.pojo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PojoPropertyTest {
    @Test
    void constructorAccessorsAndToStringRoundTrip() {
        PojoProperty prop = new PojoProperty();
        PojoImportMatch im = new PojoImportMatch();
        PojoLinkFkey fk = new PojoLinkFkey();
        PojoLinkUnique unique = new PojoLinkUnique();
        PojoRegularProperty regular = new PojoRegularProperty();

        prop.setName("name");
        prop.setUpperName("NAME");
        prop.setPropertyPath("item.name");
        prop.setJavaType(String.class.getName());
        prop.setKeyPos(2);
        prop.setPojoImportMatch(im);
        prop.setPojoLinkFkey(fk);
        prop.setPojoLinkUnique(unique);
        prop.setPojoRegularProperty(regular);

        assertEquals("name", prop.getName());
        assertEquals("NAME", prop.getUpperName());
        assertEquals("item.name", prop.getPropertyPath());
        assertEquals(String.class.getName(), prop.getJavaType());
        assertEquals(2, prop.getKeyPos());
        assertSame(im, prop.getPojoImportMatch());
        assertSame(fk, prop.getPojoLinkFkey());
        assertSame(unique, prop.getPojoLinkUnique());
        assertSame(regular, prop.getPojoRegularProperty());
        assertTrue(prop.toString().contains("name"));
    }
}
