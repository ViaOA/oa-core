package com.viaoa.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.sql.Types;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.viaoa.callback.OAObjectCallback;

@SuppressWarnings("deprecation")
class OACalculatedPropertyTest {

    @Test
    void metadataHasRuntimeRetentionTargetAndDocumented() {
        assertNotNull(OACalculatedProperty.class.getAnnotation(Documented.class));
        assertEquals(RetentionPolicy.RUNTIME, OACalculatedProperty.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[] { ElementType.METHOD }, OACalculatedProperty.class.getAnnotation(Target.class).value());
    }

    private static class ExplicitFixture {
        @OACalculatedProperty(
        lowerName = "explicit_lowerName",
        displayName = "explicit_displayName",
        description = "explicit_description",
        outputFormat = "explicit_outputFormat",
        properties = { "one", "two" },
        displayLength = 77,
        columnLength = 77,
        decimalPlaces = 77,
        isEmail = true,
        isUrl = true,
        isImageName = true,
        isIconName = true,
        isXml = true,
        isFileName = true,
        isAutoSeq = true,
        isTimestamp = true,
        isCaseSensitive = true,
        isPhone = true,
        isZipCode = true,
        isCurrency = true,
        isHtml = true,
        isObjectStatus = true,
        columnName = "explicit_columnName",
        toolTip = "explicit_toolTip",
        help = "explicit_help",
        enumPropertyName = "explicit_enumPropertyName"
    )
        public String value() { return "value"; }
    }

    private static class DefaultFixture {
        @OACalculatedProperty
        public String value() { return "value"; }
    }

    @Test
    void defaultLowerNameMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals("", ann.lowerName());
    }

    @Test
    void defaultDisplayNameMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals("", ann.displayName());
    }

    @Test
    void defaultDescriptionMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals("", ann.description());
    }

    @Test
    void defaultOutputFormatMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals("", ann.outputFormat());
    }

    @Test
    void defaultPropertiesMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertArrayEquals(new String[0], ann.properties());
    }

    @Test
    void defaultDisplayLengthMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals(0, ann.displayLength());
    }

    @Test
    void defaultColumnLengthMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals(0, ann.columnLength());
    }

    @Test
    void defaultDecimalPlacesMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals(0, ann.decimalPlaces());
    }

    @Test
    void defaultIsEmailMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals(false, ann.isEmail());
    }

    @Test
    void defaultIsUrlMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals(false, ann.isUrl());
    }

    @Test
    void defaultIsImageNameMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals(false, ann.isImageName());
    }

    @Test
    void defaultIsIconNameMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals(false, ann.isIconName());
    }

    @Test
    void defaultIsXmlMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals(false, ann.isXml());
    }

    @Test
    void defaultIsFileNameMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals(false, ann.isFileName());
    }

    @Test
    void defaultIsAutoSeqMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals(false, ann.isAutoSeq());
    }

    @Test
    void defaultIsTimestampMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals(false, ann.isTimestamp());
    }

    @Test
    void defaultIsCaseSensitiveMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals(false, ann.isCaseSensitive());
    }

    @Test
    void defaultIsPhoneMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals(false, ann.isPhone());
    }

    @Test
    void defaultIsZipCodeMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals(false, ann.isZipCode());
    }

    @Test
    void defaultIsCurrencyMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals(false, ann.isCurrency());
    }

    @Test
    void defaultIsHtmlMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals(false, ann.isHtml());
    }

    @Test
    void defaultIsObjectStatusMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals(false, ann.isObjectStatus());
    }

    @Test
    void defaultColumnNameMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals("", ann.columnName());
    }

    @Test
    void defaultToolTipMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals("", ann.toolTip());
    }

    @Test
    void defaultHelpMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals("", ann.help());
    }

    @Test
    void defaultEnumPropertyNameMatchesDeclaration() throws Exception {
        OACalculatedProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals("", ann.enumPropertyName());
    }

    @Test
    void explicitValuesCanBeReadWithReflection() throws Exception {
        OACalculatedProperty ann = ExplicitFixture.class.getMethod("value").getAnnotation(OACalculatedProperty.class);

        assertEquals("explicit_lowerName", ann.lowerName());
        assertEquals("explicit_displayName", ann.displayName());
        assertEquals("explicit_description", ann.description());
        assertEquals("explicit_outputFormat", ann.outputFormat());
        assertArrayEquals(new String[] { "one", "two" }, ann.properties());
        assertEquals(77, ann.displayLength());
        assertEquals(77, ann.columnLength());
        assertEquals(77, ann.decimalPlaces());
        assertEquals(true, ann.isEmail());
        assertEquals(true, ann.isUrl());
        assertEquals(true, ann.isImageName());
        assertEquals(true, ann.isIconName());
        assertEquals(true, ann.isXml());
        assertEquals(true, ann.isFileName());
        assertEquals(true, ann.isAutoSeq());
        assertEquals(true, ann.isTimestamp());
        assertEquals(true, ann.isCaseSensitive());
        assertEquals(true, ann.isPhone());
        assertEquals(true, ann.isZipCode());
        assertEquals(true, ann.isCurrency());
        assertEquals(true, ann.isHtml());
        assertEquals(true, ann.isObjectStatus());
        assertEquals("explicit_columnName", ann.columnName());
        assertEquals("explicit_toolTip", ann.toolTip());
        assertEquals("explicit_help", ann.help());
        assertEquals("explicit_enumPropertyName", ann.enumPropertyName());
    }
}
