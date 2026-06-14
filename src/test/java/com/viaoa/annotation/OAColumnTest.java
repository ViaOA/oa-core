package com.viaoa.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.sql.Types;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.viaoa.callback.OAObjectCallback;

@SuppressWarnings("deprecation")
class OAColumnTest {

    @Test
    void metadataHasRuntimeRetentionTargetAndDocumented() {
        assertNotNull(OAColumn.class.getAnnotation(Documented.class));
        assertEquals(RetentionPolicy.RUNTIME, OAColumn.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[] { ElementType.METHOD }, OAColumn.class.getAnnotation(Target.class).value());
    }

    private static class ExplicitFixture {
        @OAColumn(
        name = "explicit_name",
        sqlType = 77,
        maxLength = 77,
        isFullTextIndex = true,
        lowerName = "explicit_lowerName"
    )
        public String value() { return "value"; }
    }

    private static class DefaultFixture {
        @OAColumn
        public String value() { return "value"; }
    }

    @Test
    void defaultNameMatchesDeclaration() throws Exception {
        OAColumn ann = DefaultFixture.class.getMethod("value").getAnnotation(OAColumn.class);

        assertEquals("", ann.name());
    }

    @Test
    void defaultSqlTypeMatchesDeclaration() throws Exception {
        OAColumn ann = DefaultFixture.class.getMethod("value").getAnnotation(OAColumn.class);

        assertEquals(java.sql.Types.VARCHAR, ann.sqlType());
    }

    @Test
    void defaultMaxLengthMatchesDeclaration() throws Exception {
        OAColumn ann = DefaultFixture.class.getMethod("value").getAnnotation(OAColumn.class);

        assertEquals(0, ann.maxLength());
    }

    @Test
    void defaultIsFullTextIndexMatchesDeclaration() throws Exception {
        OAColumn ann = DefaultFixture.class.getMethod("value").getAnnotation(OAColumn.class);

        assertEquals(false, ann.isFullTextIndex());
    }

    @Test
    void defaultLowerNameMatchesDeclaration() throws Exception {
        OAColumn ann = DefaultFixture.class.getMethod("value").getAnnotation(OAColumn.class);

        assertEquals("", ann.lowerName());
    }

    @Test
    void explicitValuesCanBeReadWithReflection() throws Exception {
        OAColumn ann = ExplicitFixture.class.getMethod("value").getAnnotation(OAColumn.class);

        assertEquals("explicit_name", ann.name());
        assertEquals(77, ann.sqlType());
        assertEquals(77, ann.maxLength());
        assertEquals(true, ann.isFullTextIndex());
        assertEquals("explicit_lowerName", ann.lowerName());
    }
}
