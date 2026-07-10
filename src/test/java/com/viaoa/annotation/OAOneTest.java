package com.viaoa.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.sql.Types;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.viaoa.callback.OAObjectCallback;

@SuppressWarnings("deprecation")
class OAOneTest {

    @Test
    void metadataHasRuntimeRetentionTargetAndDocumented() {
        assertNotNull(OAOne.class.getAnnotation(Documented.class));
        assertEquals(RetentionPolicy.RUNTIME, OAOne.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[] { ElementType.METHOD }, OAOne.class.getAnnotation(Target.class).value());
    }

    private static class ExplicitFixture {
        @OAOne(
        lowerName = "explicit_lowerName",
        displayName = "explicit_displayName",
        description = "explicit_description",
        owner = true,
        reverseName = "explicit_reverseName",
        required = true,
        verify = true,
        cascadeSave = true,
        cascadeDelete = true,
        isTransient = true,
        allowCreateNew = false,
        autoCreateNew = true,
        allowAddExisting = false,
        mustBeEmptyForDelete = true,
        toolTip = "explicit_toolTip",
        help = "explicit_help",
        hasCustomCode = true,
        isCalculated = true,
        calcDependentProperties = { "one", "two" },
        isProcessed = true,
        defaultPath = "explicit_defaultPropertyPath",
        defaultPathIsHierarchy = true,
        defaultPathCanBeChanged = true,
        defaultModelUserPath = "explicit_defaultContextPropertyPath",
        isOneAndOnlyOne = true,
        importMatch = true,
        equalPath = "explicit_equalPropertyPath",
        selectFromPath = "explicit_selectFromPropertyPath",
        fkeys = { @OAFkey(fromProperty = "from", toProperty = "to", columns = { "col" }) },
        pojoNames = { "one", "two" }
    )
        public String value() { return "value"; }
    }

    private static class DefaultFixture {
        @OAOne
        public String value() { return "value"; }
    }

    @Test
    void defaultLowerNameMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals("", ann.lowerName());
    }

    @Test
    void defaultDisplayNameMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals("", ann.displayName());
    }

    @Test
    void defaultDescriptionMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals("", ann.description());
    }

    @Test
    void defaultOwnerMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals(false, ann.owner());
    }

    @Test
    void defaultReverseNameMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals("", ann.reverseName());
    }

    @Test
    void defaultRequiredMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals(false, ann.required());
    }

    @Test
    void defaultVerifyMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals(false, ann.verify());
    }

    @Test
    void defaultCascadeSaveMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals(false, ann.cascadeSave());
    }

    @Test
    void defaultCascadeDeleteMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals(false, ann.cascadeDelete());
    }

    @Test
    void defaultIsTransientMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals(false, ann.isTransient());
    }

    @Test
    void defaultAllowCreateNewMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals(true, ann.allowCreateNew());
    }

    @Test
    void defaultAutoCreateNewMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals(false, ann.autoCreateNew());
    }

    @Test
    void defaultAllowAddExistingMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals(true, ann.allowAddExisting());
    }

    @Test
    void defaultMustBeEmptyForDeleteMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals(false, ann.mustBeEmptyForDelete());
    }

    @Test
    void defaultToolTipMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals("", ann.toolTip());
    }

    @Test
    void defaultHelpMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals("", ann.help());
    }

    @Test
    void defaultHasCustomCodeMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals(false, ann.hasCustomCode());
    }

    @Test
    void defaultIsCalculatedMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals(false, ann.isCalculated());
    }

    @Test
    void defaultCalcDependentPropertiesMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertArrayEquals(new String[0], ann.calcDependentProperties());
    }

    @Test
    void defaultIsProcessedMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals(false, ann.isProcessed());
    }

    @Test
    void defaultDefaultPropertyPathMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals("", ann.defaultPath());
    }

    @Test
    void defaultDefaultPropertyPathIsHierarchyMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals(false, ann.defaultPathIsHierarchy());
    }

    @Test
    void defaultDefaultPropertyPathCanBeChangedMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals(false, ann.defaultPathCanBeChanged());
    }

    @Test
    void defaultDefaultContextPropertyPathMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals("", ann.defaultModelUserPath());
    }

    @Test
    void defaultIsOneAndOnlyOneMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals(false, ann.isOneAndOnlyOne());
    }

    @Test
    void defaultImportMatchMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals(false, ann.importMatch());
    }

    @Test
    void defaultEqualPropertyPathMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals("", ann.equalPath());
    }

    @Test
    void defaultSelectFromPropertyPathMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals("", ann.selectFromPath());
    }

    @Test
    void defaultFkeysMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertArrayEquals(new OAFkey[0], ann.fkeys());
    }

    @Test
    void defaultPojoNamesMatchesDeclaration() throws Exception {
        OAOne ann = DefaultFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertArrayEquals(new String[0], ann.pojoNames());
    }

    @Test
    void explicitValuesCanBeReadWithReflection() throws Exception {
        OAOne ann = ExplicitFixture.class.getMethod("value").getAnnotation(OAOne.class);

        assertEquals("explicit_lowerName", ann.lowerName());
        assertEquals("explicit_displayName", ann.displayName());
        assertEquals("explicit_description", ann.description());
        assertEquals(true, ann.owner());
        assertEquals("explicit_reverseName", ann.reverseName());
        assertEquals(true, ann.required());
        assertEquals(true, ann.verify());
        assertEquals(true, ann.cascadeSave());
        assertEquals(true, ann.cascadeDelete());
        assertEquals(true, ann.isTransient());
        assertEquals(false, ann.allowCreateNew());
        assertEquals(true, ann.autoCreateNew());
        assertEquals(false, ann.allowAddExisting());
        assertEquals(true, ann.mustBeEmptyForDelete());
        assertEquals("explicit_toolTip", ann.toolTip());
        assertEquals("explicit_help", ann.help());
        assertEquals(true, ann.hasCustomCode());
        assertEquals(true, ann.isCalculated());
        assertArrayEquals(new String[] { "one", "two" }, ann.calcDependentProperties());
        assertEquals(true, ann.isProcessed());
        assertEquals("explicit_defaultPropertyPath", ann.defaultPath());
        assertEquals(true, ann.defaultPathIsHierarchy());
        assertEquals(true, ann.defaultPathCanBeChanged());
        assertEquals("explicit_defaultContextPropertyPath", ann.defaultModelUserPath());
        assertEquals(true, ann.isOneAndOnlyOne());
        assertEquals(true, ann.importMatch());
        assertEquals("explicit_equalPropertyPath", ann.equalPath());
        assertEquals("explicit_selectFromPropertyPath", ann.selectFromPath());
        assertEquals(1, ann.fkeys().length);
        assertArrayEquals(new String[] { "one", "two" }, ann.pojoNames());
    }
}
