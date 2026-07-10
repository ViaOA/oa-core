package com.viaoa.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.sql.Types;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.viaoa.callback.OAObjectCallback;

@SuppressWarnings("deprecation")
class OAManyTest {

    @Test
    void metadataHasRuntimeRetentionTargetAndDocumented() {
        assertNotNull(OAMany.class.getAnnotation(Documented.class));
        assertEquals(RetentionPolicy.RUNTIME, OAMany.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[] { ElementType.METHOD }, OAMany.class.getAnnotation(Target.class).value());
    }

    private static class ExplicitFixture {
        @OAMany(
        toClass = String.class,
        lowerName = "explicit_lowerName",
        displayName = "explicit_displayName",
        description = "explicit_description",
        owner = true,
        recursive = true,
        reverseName = "explicit_reverseName",
        cascadeSave = true,
        cascadeDelete = true,
        seqProperty = "explicit_seqProperty",
        toolTip = "explicit_toolTip",
        help = "explicit_help",
        hasCustomCode = true,
        cacheSize = 77,
        createMethod = false,
        matchHub = "explicit_matchHub",
        matchProperty = "explicit_matchProperty",
        matchStopProperty = "explicit_matchStopProperty",
        mustBeEmptyForDelete = true,
        isCalculated = true,
        isServerSideCalc = true,
        uniqueProperty = "explicit_uniqueProperty",
        sortProperty = "explicit_sortProperty",
        sortAsc = false,
        calcDependentProperties = { "one", "two" },
        mergerPath = "explicit_mergerPropertyPath",
        couldBeLarge = true,
        isProcessed = true,
        autoCreateProperty = "explicit_autoCreateProperty",
        equalPath = "explicit_equalPropertyPath",
        selectFromPath = "explicit_selectFromPropertyPath"
    )
        public String value() { return "value"; }
    }

    private static class DefaultFixture {
        @OAMany
        public String value() { return "value"; }
    }

    @Test
    void defaultToClassMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertSame(Object.class, ann.toClass());
    }

    @Test
    void defaultLowerNameMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals("", ann.lowerName());
    }

    @Test
    void defaultDisplayNameMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals("", ann.displayName());
    }

    @Test
    void defaultDescriptionMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals("", ann.description());
    }

    @Test
    void defaultOwnerMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals(false, ann.owner());
    }

    @Test
    void defaultRecursiveMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals(false, ann.recursive());
    }

    @Test
    void defaultReverseNameMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals("", ann.reverseName());
    }

    @Test
    void defaultCascadeSaveMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals(false, ann.cascadeSave());
    }

    @Test
    void defaultCascadeDeleteMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals(false, ann.cascadeDelete());
    }

    @Test
    void defaultSeqPropertyMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals("", ann.seqProperty());
    }

    @Test
    void defaultToolTipMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals("", ann.toolTip());
    }

    @Test
    void defaultHelpMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals("", ann.help());
    }

    @Test
    void defaultHasCustomCodeMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals(false, ann.hasCustomCode());
    }

    @Test
    void defaultCacheSizeMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals(0, ann.cacheSize());
    }

    @Test
    void defaultCreateMethodMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals(true, ann.createMethod());
    }

    @Test
    void defaultMatchHubMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals("", ann.matchHub());
    }

    @Test
    void defaultMatchPropertyMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals("", ann.matchProperty());
    }

    @Test
    void defaultMatchStopPropertyMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals("", ann.matchStopProperty());
    }

    @Test
    void defaultMustBeEmptyForDeleteMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals(false, ann.mustBeEmptyForDelete());
    }

    @Test
    void defaultIsCalculatedMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals(false, ann.isCalculated());
    }

    @Test
    void defaultIsServerSideCalcMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals(false, ann.isServerSideCalc());
    }

    @Test
    void defaultUniquePropertyMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals("", ann.uniqueProperty());
    }

    @Test
    void defaultSortPropertyMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals("", ann.sortProperty());
    }

    @Test
    void defaultSortAscMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals(true, ann.sortAsc());
    }

    @Test
    void defaultCalcDependentPropertiesMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertArrayEquals(new String[0], ann.calcDependentProperties());
    }

    @Test
    void defaultMergerPropertyPathMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals("", ann.mergerPath());
    }

    @Test
    void defaultCouldBeLargeMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals(false, ann.couldBeLarge());
    }

    @Test
    void defaultIsProcessedMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals(false, ann.isProcessed());
    }

    @Test
    void defaultAutoCreatePropertyMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals("", ann.autoCreateProperty());
    }

    @Test
    void defaultEqualPropertyPathMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals("", ann.equalPath());
    }

    @Test
    void defaultSelectFromPropertyPathMatchesDeclaration() throws Exception {
        OAMany ann = DefaultFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertEquals("", ann.selectFromPath());
    }

    @Test
    void explicitValuesCanBeReadWithReflection() throws Exception {
        OAMany ann = ExplicitFixture.class.getMethod("value").getAnnotation(OAMany.class);

        assertSame(String.class, ann.toClass());
        assertEquals("explicit_lowerName", ann.lowerName());
        assertEquals("explicit_displayName", ann.displayName());
        assertEquals("explicit_description", ann.description());
        assertEquals(true, ann.owner());
        assertEquals(true, ann.recursive());
        assertEquals("explicit_reverseName", ann.reverseName());
        assertEquals(true, ann.cascadeSave());
        assertEquals(true, ann.cascadeDelete());
        assertEquals("explicit_seqProperty", ann.seqProperty());
        assertEquals("explicit_toolTip", ann.toolTip());
        assertEquals("explicit_help", ann.help());
        assertEquals(true, ann.hasCustomCode());
        assertEquals(77, ann.cacheSize());
        assertEquals(false, ann.createMethod());
        assertEquals("explicit_matchHub", ann.matchHub());
        assertEquals("explicit_matchProperty", ann.matchProperty());
        assertEquals("explicit_matchStopProperty", ann.matchStopProperty());
        assertEquals(true, ann.mustBeEmptyForDelete());
        assertEquals(true, ann.isCalculated());
        assertEquals(true, ann.isServerSideCalc());
        assertEquals("explicit_uniqueProperty", ann.uniqueProperty());
        assertEquals("explicit_sortProperty", ann.sortProperty());
        assertEquals(false, ann.sortAsc());
        assertArrayEquals(new String[] { "one", "two" }, ann.calcDependentProperties());
        assertEquals("explicit_mergerPropertyPath", ann.mergerPath());
        assertEquals(true, ann.couldBeLarge());
        assertEquals(true, ann.isProcessed());
        assertEquals("explicit_autoCreateProperty", ann.autoCreateProperty());
        assertEquals("explicit_equalPropertyPath", ann.equalPath());
        assertEquals("explicit_selectFromPropertyPath", ann.selectFromPath());
    }
}
