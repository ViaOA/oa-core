package com.viaoa.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.sql.Types;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.viaoa.callback.OAObjectCallback;

@SuppressWarnings("deprecation")
class OAMethodTest {

    @Test
    void metadataHasRuntimeRetentionTargetAndDocumented() {
        assertNotNull(OAMethod.class.getAnnotation(Documented.class));
        assertEquals(RetentionPolicy.RUNTIME, OAMethod.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[] { ElementType.METHOD }, OAMethod.class.getAnnotation(Target.class).value());
    }

    private static class ExplicitFixture {
        @OAMethod(
        displayName = "explicit_displayName",
        description = "explicit_description",
        toolTip = "explicit_toolTip",
        help = "explicit_help"
    )
        public String value() { return "value"; }
    }

    private static class DefaultFixture {
        @OAMethod
        public String value() { return "value"; }
    }

    @Test
    void defaultDisplayNameMatchesDeclaration() throws Exception {
        OAMethod ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMethod.class);

        assertEquals("", ann.displayName());
    }

    @Test
    void defaultDescriptionMatchesDeclaration() throws Exception {
        OAMethod ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMethod.class);

        assertEquals("", ann.description());
    }

    @Test
    void defaultToolTipMatchesDeclaration() throws Exception {
        OAMethod ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMethod.class);

        assertEquals("", ann.toolTip());
    }

    @Test
    void defaultHelpMatchesDeclaration() throws Exception {
        OAMethod ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMethod.class);

        assertEquals("", ann.help());
    }

    @Test
    void explicitValuesCanBeReadWithReflection() throws Exception {
        OAMethod ann = ExplicitFixture.class.getMethod("value").getAnnotation(OAMethod.class);

        assertEquals("explicit_displayName", ann.displayName());
        assertEquals("explicit_description", ann.description());
        assertEquals("explicit_toolTip", ann.toolTip());
        assertEquals("explicit_help", ann.help());
    }
}
