package com.viaoa.metadata.pojo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

class PojoLinkOneTest {
    @Test
    void constructorAccessorsListsAndToStringRoundTrip() {
        PojoLinkOne one = new PojoLinkOne();
        PojoLink link = new PojoLink();
        PojoLinkUnique unique = new PojoLinkUnique();
        PojoImportMatch im = new PojoImportMatch();
        PojoLinkFkey fk = new PojoLinkFkey();
        one.setPojoLink(link);
        one.setPojoLinkUnique(unique);
        one.setPojoImportMatches(List.of(im));
        one.setPojoLinkFkeys(List.of(fk));

        assertSame(link, one.getPojoLink());
        assertSame(unique, one.getPojoLinkUnique());
        assertTrue(one.getPojoImportMatches() instanceof CopyOnWriteArrayList);
        assertEquals(List.of(im), one.getPojoImportMatches());
        assertTrue(one.getPojoLinkFkeys() instanceof CopyOnWriteArrayList);
        assertEquals(List.of(fk), one.getPojoLinkFkeys());
        assertNotNull(one.toString());
    }
}
