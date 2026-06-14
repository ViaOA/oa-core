package com.viaoa.metadata.pojo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PojoLinkTest {
    @Test
    void constructorAccessorsAndToStringRoundTrip() {
        PojoLink link = new PojoLink();
        Pojo pojo = new Pojo();
        PojoLinkMany many = new PojoLinkMany();
        PojoLinkOne one = new PojoLinkOne();
        link.setName("registers");
        link.setPojo(pojo);
        link.setPojoLinkMany(many);
        link.setPojoLinkOne(one);

        assertEquals("registers", link.getName());
        assertSame(pojo, link.getPojo());
        assertSame(many, link.getPojoLinkMany());
        assertSame(one, link.getPojoLinkOne());
        assertTrue(link.toString().contains("registers"));
    }
}
