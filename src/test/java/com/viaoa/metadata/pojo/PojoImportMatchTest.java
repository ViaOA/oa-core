package com.viaoa.metadata.pojo;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class PojoImportMatchTest {
    @Test
    void constructorAccessorsAndToStringRoundTrip() {
        PojoImportMatch obj = new PojoImportMatch();
        PojoLinkOne PojoLinkOne = new PojoLinkOne();
        obj.setPojoLinkOne(PojoLinkOne);
        PojoLinkOneReference PojoLinkOneReference = new PojoLinkOneReference();
        obj.setPojoLinkOneReference(PojoLinkOneReference);
        PojoProperty PojoProperty = new PojoProperty();
        obj.setPojoProperty(PojoProperty);
        assertSame(PojoLinkOne, obj.getPojoLinkOne());
        assertSame(PojoLinkOneReference, obj.getPojoLinkOneReference());
        assertSame(PojoProperty, obj.getPojoProperty());
        assertNotNull(obj.toString());
    }
}
