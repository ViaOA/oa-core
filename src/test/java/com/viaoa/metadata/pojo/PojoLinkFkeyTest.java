package com.viaoa.metadata.pojo;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class PojoLinkFkeyTest {
    @Test
    void constructorAccessorsAndToStringRoundTrip() {
        PojoLinkFkey obj = new PojoLinkFkey();
        PojoLinkOne PojoLinkOne = new PojoLinkOne();
        obj.setPojoLinkOne(PojoLinkOne);
        PojoProperty PojoProperty = new PojoProperty();
        obj.setPojoProperty(PojoProperty);
        assertSame(PojoLinkOne, obj.getPojoLinkOne());
        assertSame(PojoProperty, obj.getPojoProperty());
        assertNotNull(obj.toString());
    }
}
