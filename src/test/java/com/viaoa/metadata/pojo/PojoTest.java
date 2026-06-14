package com.viaoa.metadata.pojo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

class PojoTest {
    @Test
    void constructorNameListsAndToStringRoundTrip() {
        Pojo pojo = new Pojo();
        PojoLink link = new PojoLink();
        PojoRegularProperty prop = new PojoRegularProperty();

        pojo.setName("StorePojo");
        pojo.setPojoLinks(List.of(link));
        pojo.setPojoRegularProperties(List.of(prop));

        assertEquals("StorePojo", pojo.getName());
        assertTrue(pojo.getPojoLinks() instanceof CopyOnWriteArrayList);
        assertEquals(List.of(link), pojo.getPojoLinks());
        assertTrue(pojo.getPojoRegularProperties() instanceof CopyOnWriteArrayList);
        assertEquals(List.of(prop), pojo.getPojoRegularProperties());
        assertTrue(pojo.toString().contains("StorePojo"));
    }
}
