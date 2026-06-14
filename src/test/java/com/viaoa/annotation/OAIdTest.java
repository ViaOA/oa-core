package com.viaoa.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.sql.Types;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.viaoa.callback.OAObjectCallback;

@SuppressWarnings("deprecation")
class OAIdTest {

    @Test
    void metadataHasRuntimeRetentionTargetAndDocumented() {
        assertNotNull(OAId.class.getAnnotation(Documented.class));
        assertEquals(RetentionPolicy.RUNTIME, OAId.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[] { ElementType.METHOD }, OAId.class.getAnnotation(Target.class).value());
    }

    private static class ExplicitFixture {
        @OAId(
        autoAssign = false,
        guid = true,
        pos = 77
    )
        public String value() { return "value"; }
    }

    private static class DefaultFixture {
        @OAId
        public String value() { return "value"; }
    }

    @Test
    void defaultAutoAssignMatchesDeclaration() throws Exception {
        OAId ann = DefaultFixture.class.getMethod("value").getAnnotation(OAId.class);

        assertEquals(true, ann.autoAssign());
    }

    @Test
    void defaultGuidMatchesDeclaration() throws Exception {
        OAId ann = DefaultFixture.class.getMethod("value").getAnnotation(OAId.class);

        assertEquals(false, ann.guid());
    }

    @Test
    void defaultPosMatchesDeclaration() throws Exception {
        OAId ann = DefaultFixture.class.getMethod("value").getAnnotation(OAId.class);

        assertEquals(0, ann.pos());
    }

    @Test
    void explicitValuesCanBeReadWithReflection() throws Exception {
        OAId ann = ExplicitFixture.class.getMethod("value").getAnnotation(OAId.class);

        assertEquals(false, ann.autoAssign());
        assertEquals(true, ann.guid());
        assertEquals(77, ann.pos());
    }
}
