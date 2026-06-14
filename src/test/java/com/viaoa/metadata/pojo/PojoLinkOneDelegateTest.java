package com.viaoa.metadata.pojo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class PojoLinkOneDelegateTest {
    @Test
    void lookupAndPropertyCollectionMethodsUseLinkOneMetadata() {
        Pojo pojo = PojoDelegateTest.samplePojo();
        PojoLinkOne one = PojoLinkOneDelegate.getPojoLinkOne(pojo, "STORE");
        assertNotNull(one);

        assertEquals(1, PojoLinkOneDelegate.getLinkFkeyPojoProperties(pojo, "store").size());
        assertEquals(1, PojoLinkOneDelegate.getLinkFkeyPojoProperties(one).size());
        assertEquals(List.of(), PojoLinkOneDelegate.getImportMatchPojoProperties(pojo, "store"));
        assertEquals(List.of(), PojoLinkOneDelegate.getImportMatchPojoProperties(one));
        assertEquals(List.of(), PojoLinkOneDelegate.getLinkUniquePojoProperties(pojo, "store"));
        assertEquals(List.of(), PojoLinkOneDelegate.getLinkUniquePojoProperties(one));
        assertEquals(1, PojoLinkOneDelegate.getLinkOnePojoProperties(one).size());
        assertNull(PojoLinkOneDelegate.getPojoLinkOne(null, "store"));
    }
}
