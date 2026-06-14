package com.viaoa.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.sql.Types;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.viaoa.callback.OAObjectCallback;

@SuppressWarnings("deprecation")
class OAClassFilterTest {

    @Test
    void metadataHasRuntimeRetentionTargetAndDocumented() {
        assertNotNull(OAClassFilter.class.getAnnotation(Documented.class));
        assertEquals(RetentionPolicy.RUNTIME, OAClassFilter.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[] { ElementType.TYPE }, OAClassFilter.class.getAnnotation(Target.class).value());
    }

    @OAClassFilter(
        name = "explicit_name",
        displayName = "explicit_displayName",
        description = "explicit_description",
        hasInputParams = true,
        autoRefreshInterval = 77,
        autoRefreshTimeUnit = TimeUnit.MINUTES,
        query = "explicit_query"
    )
    private static class ExplicitFixture {}

    @OAClassFilter
    private static class DefaultFixture {}

    @Test
    void defaultNameMatchesDeclaration() throws Exception {
        OAClassFilter ann = DefaultFixture.class.getAnnotation(OAClassFilter.class);

        assertEquals("", ann.name());
    }

    @Test
    void defaultDisplayNameMatchesDeclaration() throws Exception {
        OAClassFilter ann = DefaultFixture.class.getAnnotation(OAClassFilter.class);

        assertEquals("", ann.displayName());
    }

    @Test
    void defaultDescriptionMatchesDeclaration() throws Exception {
        OAClassFilter ann = DefaultFixture.class.getAnnotation(OAClassFilter.class);

        assertEquals("", ann.description());
    }

    @Test
    void defaultHasInputParamsMatchesDeclaration() throws Exception {
        OAClassFilter ann = DefaultFixture.class.getAnnotation(OAClassFilter.class);

        assertEquals(false, ann.hasInputParams());
    }

    @Test
    void defaultAutoRefreshIntervalMatchesDeclaration() throws Exception {
        OAClassFilter ann = DefaultFixture.class.getAnnotation(OAClassFilter.class);

        assertEquals(0, ann.autoRefreshInterval());
    }

    @Test
    void defaultAutoRefreshTimeUnitMatchesDeclaration() throws Exception {
        OAClassFilter ann = DefaultFixture.class.getAnnotation(OAClassFilter.class);

        assertEquals(TimeUnit.DAYS, ann.autoRefreshTimeUnit());
    }

    @Test
    void defaultQueryMatchesDeclaration() throws Exception {
        OAClassFilter ann = DefaultFixture.class.getAnnotation(OAClassFilter.class);

        assertEquals("", ann.query());
    }

    @Test
    void explicitValuesCanBeReadWithReflection() throws Exception {
        OAClassFilter ann = ExplicitFixture.class.getAnnotation(OAClassFilter.class);

        assertEquals("explicit_name", ann.name());
        assertEquals("explicit_displayName", ann.displayName());
        assertEquals("explicit_description", ann.description());
        assertEquals(true, ann.hasInputParams());
        assertEquals(77, ann.autoRefreshInterval());
        assertEquals(TimeUnit.MINUTES, ann.autoRefreshTimeUnit());
        assertEquals("explicit_query", ann.query());
    }
}
