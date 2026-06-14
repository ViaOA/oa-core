package com.viaoa.metadata.pojo;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class PojoLinkManyTest {
    @Test
    void constructorAccessorsAndToStringRoundTrip() {
        PojoLinkMany obj = new PojoLinkMany();
        PojoLink PojoLink = new PojoLink();
        obj.setPojoLink(PojoLink);
        assertSame(PojoLink, obj.getPojoLink());
        assertNotNull(obj.toString());
    }
}
