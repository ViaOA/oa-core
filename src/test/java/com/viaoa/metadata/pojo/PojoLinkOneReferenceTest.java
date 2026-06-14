package com.viaoa.metadata.pojo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PojoLinkOneReferenceTest {
    @Test
    void constructorAccessorsAndToStringRoundTrip() {
        PojoLinkOneReference ref = new PojoLinkOneReference();
        PojoImportMatch im = new PojoImportMatch();
        PojoLinkOne one = new PojoLinkOne();
        PojoLinkUnique unique = new PojoLinkUnique();
        ref.setName("store");
        ref.setPojoImportMatch(im);
        ref.setPojoLinkOne(one);
        ref.setPojoLinkUnique(unique);

        assertEquals("store", ref.getName());
        assertSame(im, ref.getPojoImportMatch());
        assertSame(one, ref.getPojoLinkOne());
        assertSame(unique, ref.getPojoLinkUnique());
        assertTrue(ref.toString().contains("store"));
    }
}
