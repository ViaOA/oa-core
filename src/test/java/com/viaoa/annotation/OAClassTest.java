package com.viaoa.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.sql.Types;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.viaoa.callback.OAObjectCallback;

@SuppressWarnings("deprecation")
class OAClassTest {

    @Test
    void metadataHasRuntimeRetentionTargetAndDocumented() {
        assertNotNull(OAClass.class.getAnnotation(Documented.class));
        assertEquals(RetentionPolicy.RUNTIME, OAClass.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[] { ElementType.TYPE }, OAClass.class.getAnnotation(Target.class).value());
    }

    @OAClass(
        shortName = "explicit_shortName",
        pluralName = "explicit_pluralName",
        lowerName = "explicit_lowerName",
        displayName = "explicit_displayName",
        description = "explicit_description",
        isLookup = true,
        isPreSelect = true,
        useDataSource = false,
        addToCache = false,
        localOnly = true,
        initialize = false,
        displayProperty = "explicit_displayProperty",
        sortProperty = "explicit_sortProperty",
        viewProperties = { "one", "two" },
        estimatedTotal = 77L,
        filterClasses = { String.class, Integer.class },
        rootTreePaths = { "one", "two" },
        isProcessed = true,
        softDeleteProperty = "explicit_softDeleteProperty",
        softDeleteReasonProperty = "explicit_softDeleteReasonProperty",
        versionProperty = "explicit_versionProperty",
        versionLinkProperty = "explicit_versionLinkProperty",
        timeSeriesProperty = "explicit_timeSeriesProperty",
        freezeProperty = "explicit_freezeProperty",
        singleton = true,
        pojoSingleton = true,
        noPojo = true,
        jsonUsesCapital = true
    )
    private static class ExplicitFixture {}

    @OAClass
    private static class DefaultFixture {}

    @Test
    void defaultShortNameMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals("", ann.shortName());
    }

    @Test
    void defaultPluralNameMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals("", ann.pluralName());
    }

    @Test
    void defaultLowerNameMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals("", ann.lowerName());
    }

    @Test
    void defaultDisplayNameMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals("", ann.displayName());
    }

    @Test
    void defaultDescriptionMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals("", ann.description());
    }

    @Test
    void defaultIsLookupMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals(false, ann.isLookup());
    }

    @Test
    void defaultIsPreSelectMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals(false, ann.isPreSelect());
    }

    @Test
    void defaultUseDataSourceMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals(true, ann.useDataSource());
    }

    @Test
    void defaultAddToCacheMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals(true, ann.addToCache());
    }

    @Test
    void defaultLocalOnlyMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals(false, ann.localOnly());
    }

    @Test
    void defaultInitializeMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals(true, ann.initialize());
    }

    @Test
    void defaultDisplayPropertyMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals("", ann.displayProperty());
    }

    @Test
    void defaultSortPropertyMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals("", ann.sortProperty());
    }

    @Test
    void defaultViewPropertiesMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertArrayEquals(new String[0], ann.viewProperties());
    }

    @Test
    void defaultEstimatedTotalMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals(0, ann.estimatedTotal());
    }

    @Test
    void defaultFilterClassesMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertArrayEquals(new Class[0], ann.filterClasses());
    }

    @Test
    void defaultRootTreePropertyPathsMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertArrayEquals(new String[0], ann.rootTreePaths());
    }

    @Test
    void defaultIsProcessedMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals(false, ann.isProcessed());
    }

    @Test
    void defaultSoftDeletePropertyMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals("", ann.softDeleteProperty());
    }

    @Test
    void defaultSoftDeleteReasonPropertyMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals("", ann.softDeleteReasonProperty());
    }

    @Test
    void defaultVersionPropertyMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals("", ann.versionProperty());
    }

    @Test
    void defaultVersionLinkPropertyMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals("", ann.versionLinkProperty());
    }

    @Test
    void defaultTimeSeriesPropertyMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals("", ann.timeSeriesProperty());
    }

    @Test
    void defaultFreezePropertyMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals("", ann.freezeProperty());
    }

    @Test
    void defaultSingletonMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals(false, ann.singleton());
    }

    @Test
    void defaultPojoSingletonMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals(false, ann.pojoSingleton());
    }

    @Test
    void defaultNoPojoMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals(false, ann.noPojo());
    }

    @Test
    void defaultJsonUsesCapitalMatchesDeclaration() throws Exception {
        OAClass ann = DefaultFixture.class.getAnnotation(OAClass.class);

        assertEquals(false, ann.jsonUsesCapital());
    }

    @Test
    void explicitValuesCanBeReadWithReflection() throws Exception {
        OAClass ann = ExplicitFixture.class.getAnnotation(OAClass.class);

        assertEquals("explicit_shortName", ann.shortName());
        assertEquals("explicit_pluralName", ann.pluralName());
        assertEquals("explicit_lowerName", ann.lowerName());
        assertEquals("explicit_displayName", ann.displayName());
        assertEquals("explicit_description", ann.description());
        assertEquals(true, ann.isLookup());
        assertEquals(true, ann.isPreSelect());
        assertEquals(false, ann.useDataSource());
        assertEquals(false, ann.addToCache());
        assertEquals(true, ann.localOnly());
        assertEquals(false, ann.initialize());
        assertEquals("explicit_displayProperty", ann.displayProperty());
        assertEquals("explicit_sortProperty", ann.sortProperty());
        assertArrayEquals(new String[] { "one", "two" }, ann.viewProperties());
        assertEquals(77L, ann.estimatedTotal());
        assertArrayEquals(new Class[] { String.class, Integer.class }, ann.filterClasses());
        assertArrayEquals(new String[] { "one", "two" }, ann.rootTreePaths());
        assertEquals(true, ann.isProcessed());
        assertEquals("explicit_softDeleteProperty", ann.softDeleteProperty());
        assertEquals("explicit_softDeleteReasonProperty", ann.softDeleteReasonProperty());
        assertEquals("explicit_versionProperty", ann.versionProperty());
        assertEquals("explicit_versionLinkProperty", ann.versionLinkProperty());
        assertEquals("explicit_timeSeriesProperty", ann.timeSeriesProperty());
        assertEquals("explicit_freezeProperty", ann.freezeProperty());
        assertEquals(true, ann.singleton());
        assertEquals(true, ann.pojoSingleton());
        assertEquals(true, ann.noPojo());
        assertEquals(true, ann.jsonUsesCapital());
    }
}
