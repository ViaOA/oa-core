package com.viaoa.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.sql.Types;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.viaoa.callback.OAObjectCallback;

@SuppressWarnings("deprecation")
class OAPropertyTest {

    @Test
    void metadataHasRuntimeRetentionTargetAndDocumented() {
        assertNotNull(OAProperty.class.getAnnotation(Documented.class));
        assertEquals(RetentionPolicy.RUNTIME, OAProperty.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[] { ElementType.METHOD }, OAProperty.class.getAnnotation(Target.class).value());
    }

    private static class ExplicitFixture {
        @OAProperty(
        lowerName = "explicit_lowerName",
        displayName = "explicit_displayName",
        description = "explicit_description",
        defaultValue = "explicit_defaultValue",
        required = true,
        decimalPlaces = 77,
        displayLength = 77,
        minLength = 77,
        maxLength = 77,
        uiColumnName = "explicit_uiColumnName",
        columnName = "explicit_columnName",
        uiColumnLength = 77,
        columnLength = 77,
        format = "explicit_format",
        verify = true,
        validCharacters = "explicit_validCharacters",
        invalidCharacters = "explicit_invalidCharacters",
        toolTip = "explicit_toolTip",
        help = "explicit_help",
        hasCustomCode = true,
        isEncrypted = true,
        isPassword = true,
        isSHAHash = true,
        isReadOnly = true,
        isProcessed = true,
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
        isHtml = true,
        isJson = true,
        isUnique = true,
        isCurrency = true,
        hasValidationMethod = true,
        isBlob = true,
        isNameValue = true,
        isUnicode = true,
        trackPrimitiveNull = false,
        ignoreTimeZone = true,
        isSubmit = true,
        isObjectStatus = true,
        timeZonePropertyPath = "explicit_timeZonePropertyPath",
        isUpper = true,
        isLower = true,
        sensitiveData = true,
        importMatch = true,
        enumPropertyName = "explicit_enumPropertyName",
        isFkeyOnly = true,
        noPojo = true,
        pojoKeyPos = 77
    )
        public String value() { return "value"; }
    }

    private static class DefaultFixture {
        @OAProperty
        public String value() { return "value"; }
    }

    @Test
    void defaultLowerNameMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals("", ann.lowerName());
    }

    @Test
    void defaultDisplayNameMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals("", ann.displayName());
    }

    @Test
    void defaultDescriptionMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals("", ann.description());
    }

    @Test
    void defaultDefaultValueMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals("", ann.defaultValue());
    }

    @Test
    void defaultRequiredMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.required());
    }

    @Test
    void defaultDecimalPlacesMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(-1, ann.decimalPlaces());
    }

    @Test
    void defaultDisplayLengthMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(0, ann.displayLength());
    }

    @Test
    void defaultMinLengthMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(0, ann.minLength());
    }

    @Test
    void defaultMaxLengthMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(0, ann.maxLength());
    }

    @Test
    void defaultUiColumnNameMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals("", ann.uiColumnName());
    }

    @Test
    void defaultColumnNameMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals("", ann.columnName());
    }

    @Test
    void defaultUiColumnLengthMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(0, ann.uiColumnLength());
    }

    @Test
    void defaultColumnLengthMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(0, ann.columnLength());
    }

    @Test
    void defaultFormatMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals("", ann.format());
    }

    @Test
    void defaultVerifyMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.verify());
    }

    @Test
    void defaultValidCharactersMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals("", ann.validCharacters());
    }

    @Test
    void defaultInvalidCharactersMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals("", ann.invalidCharacters());
    }

    @Test
    void defaultToolTipMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals("", ann.toolTip());
    }

    @Test
    void defaultHelpMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals("", ann.help());
    }

    @Test
    void defaultHasCustomCodeMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.hasCustomCode());
    }

    @Test
    void defaultIsEncryptedMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isEncrypted());
    }

    @Test
    void defaultIsPasswordMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isPassword());
    }

    @Test
    void defaultIsSHAHashMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isSHAHash());
    }

    @Test
    void defaultIsReadOnlyMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isReadOnly());
    }

    @Test
    void defaultIsProcessedMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isProcessed());
    }

    @Test
    void defaultIsEmailMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isEmail());
    }

    @Test
    void defaultIsUrlMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isUrl());
    }

    @Test
    void defaultIsImageNameMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isImageName());
    }

    @Test
    void defaultIsIconNameMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isIconName());
    }

    @Test
    void defaultIsXmlMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isXml());
    }

    @Test
    void defaultIsFileNameMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isFileName());
    }

    @Test
    void defaultIsAutoSeqMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isAutoSeq());
    }

    @Test
    void defaultIsTimestampMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isTimestamp());
    }

    @Test
    void defaultIsCaseSensitiveMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isCaseSensitive());
    }

    @Test
    void defaultIsPhoneMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isPhone());
    }

    @Test
    void defaultIsZipCodeMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isZipCode());
    }

    @Test
    void defaultIsHtmlMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isHtml());
    }

    @Test
    void defaultIsJsonMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isJson());
    }

    @Test
    void defaultIsUniqueMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isUnique());
    }

    @Test
    void defaultIsCurrencyMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isCurrency());
    }

    @Test
    void defaultHasValidationMethodMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.hasValidationMethod());
    }

    @Test
    void defaultIsBlobMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isBlob());
    }

    @Test
    void defaultIsNameValueMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isNameValue());
    }

    @Test
    void defaultIsUnicodeMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isUnicode());
    }

    @Test
    void defaultTrackPrimitiveNullMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(true, ann.trackPrimitiveNull());
    }

    @Test
    void defaultIgnoreTimeZoneMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.ignoreTimeZone());
    }

    @Test
    void defaultIsSubmitMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isSubmit());
    }

    @Test
    void defaultIsObjectStatusMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isObjectStatus());
    }

    @Test
    void defaultTimeZonePropertyPathMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals("", ann.timeZonePropertyPath());
    }

    @Test
    void defaultIsUpperMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isUpper());
    }

    @Test
    void defaultIsLowerMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isLower());
    }

    @Test
    void defaultSensitiveDataMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.sensitiveData());
    }

    @Test
    void defaultImportMatchMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.importMatch());
    }

    @Test
    void defaultEnumPropertyNameMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals("", ann.enumPropertyName());
    }

    @Test
    void defaultIsFkeyOnlyMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.isFkeyOnly());
    }

    @Test
    void defaultNoPojoMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(false, ann.noPojo());
    }

    @Test
    void defaultPojoKeyPosMatchesDeclaration() throws Exception {
        OAProperty ann = DefaultFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals(0, ann.pojoKeyPos());
    }

    @Test
    void explicitValuesCanBeReadWithReflection() throws Exception {
        OAProperty ann = ExplicitFixture.class.getMethod("value").getAnnotation(OAProperty.class);

        assertEquals("explicit_lowerName", ann.lowerName());
        assertEquals("explicit_displayName", ann.displayName());
        assertEquals("explicit_description", ann.description());
        assertEquals("explicit_defaultValue", ann.defaultValue());
        assertEquals(true, ann.required());
        assertEquals(77, ann.decimalPlaces());
        assertEquals(77, ann.displayLength());
        assertEquals(77, ann.minLength());
        assertEquals(77, ann.maxLength());
        assertEquals("explicit_uiColumnName", ann.uiColumnName());
        assertEquals("explicit_columnName", ann.columnName());
        assertEquals(77, ann.uiColumnLength());
        assertEquals(77, ann.columnLength());
        assertEquals("explicit_format", ann.format());
        assertEquals(true, ann.verify());
        assertEquals("explicit_validCharacters", ann.validCharacters());
        assertEquals("explicit_invalidCharacters", ann.invalidCharacters());
        assertEquals("explicit_toolTip", ann.toolTip());
        assertEquals("explicit_help", ann.help());
        assertEquals(true, ann.hasCustomCode());
        assertEquals(true, ann.isEncrypted());
        assertEquals(true, ann.isPassword());
        assertEquals(true, ann.isSHAHash());
        assertEquals(true, ann.isReadOnly());
        assertEquals(true, ann.isProcessed());
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
        assertEquals(true, ann.isHtml());
        assertEquals(true, ann.isJson());
        assertEquals(true, ann.isUnique());
        assertEquals(true, ann.isCurrency());
        assertEquals(true, ann.hasValidationMethod());
        assertEquals(true, ann.isBlob());
        assertEquals(true, ann.isNameValue());
        assertEquals(true, ann.isUnicode());
        assertEquals(false, ann.trackPrimitiveNull());
        assertEquals(true, ann.ignoreTimeZone());
        assertEquals(true, ann.isSubmit());
        assertEquals(true, ann.isObjectStatus());
        assertEquals("explicit_timeZonePropertyPath", ann.timeZonePropertyPath());
        assertEquals(true, ann.isUpper());
        assertEquals(true, ann.isLower());
        assertEquals(true, ann.sensitiveData());
        assertEquals(true, ann.importMatch());
        assertEquals("explicit_enumPropertyName", ann.enumPropertyName());
        assertEquals(true, ann.isFkeyOnly());
        assertEquals(true, ann.noPojo());
        assertEquals(77, ann.pojoKeyPos());
    }
}
