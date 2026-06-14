package com.viaoa.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.sql.Types;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.viaoa.callback.OAObjectCallback;

@SuppressWarnings("deprecation")
class OAFkeyTest {

    @Test
    void metadataHasRuntimeRetentionTargetAndDocumented() {
        assertNotNull(OAFkey.class.getAnnotation(Documented.class));
        assertEquals(RetentionPolicy.RUNTIME, OAFkey.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[] { ElementType.METHOD }, OAFkey.class.getAnnotation(Target.class).value());
    }

    private static class ExplicitFixture {
        @OAFkey(
        fromProperty = "explicit_fromProperty",
        toProperty = "explicit_toProperty",
        columns = { "one", "two" }
    )
        public String value() { return "value"; }
    }

    private static class DefaultFixture {
        @OAFkey
        public String value() { return "value"; }
    }

    @Test
    void defaultFromPropertyMatchesDeclaration() throws Exception {
        OAFkey ann = DefaultFixture.class.getMethod("value").getAnnotation(OAFkey.class);

        assertEquals("", ann.fromProperty());
    }

    @Test
    void defaultToPropertyMatchesDeclaration() throws Exception {
        OAFkey ann = DefaultFixture.class.getMethod("value").getAnnotation(OAFkey.class);

        assertEquals("", ann.toProperty());
    }

    @Test
    void defaultColumnsMatchesDeclaration() throws Exception {
        OAFkey ann = DefaultFixture.class.getMethod("value").getAnnotation(OAFkey.class);

        assertArrayEquals(new String[0], ann.columns());
    }

    @Test
    void explicitValuesCanBeReadWithReflection() throws Exception {
        OAFkey ann = ExplicitFixture.class.getMethod("value").getAnnotation(OAFkey.class);

        assertEquals("explicit_fromProperty", ann.fromProperty());
        assertEquals("explicit_toProperty", ann.toProperty());
        assertArrayEquals(new String[] { "one", "two" }, ann.columns());
    }
}
