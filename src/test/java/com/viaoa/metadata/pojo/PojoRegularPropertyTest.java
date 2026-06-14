package com.viaoa.metadata.pojo;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class PojoRegularPropertyTest {
    @Test
    void constructorAccessorsAndToStringRoundTrip() {
        PojoRegularProperty obj = new PojoRegularProperty();
        Pojo Pojo = new Pojo();
        obj.setPojo(Pojo);
        PojoProperty PojoProperty = new PojoProperty();
        obj.setPojoProperty(PojoProperty);
        assertSame(Pojo, obj.getPojo());
        assertSame(PojoProperty, obj.getPojoProperty());
        assertNotNull(obj.toString());
    }
}
