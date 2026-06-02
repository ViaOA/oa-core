package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class TextPackageInfoTest {
    @Test
    void packageInfoTest() {
        Package pkg = OATextUtil.class.getPackage();
        assertNotNull(pkg);
        assertEquals("com.viaoa.text", pkg.getName());
    }

    @Test
    void packageResponsibilityChartClassesExistTest() {
        assertNotNull(OATextSanitize.class);
        assertNotNull(OATextChars.class);
        assertNotNull(OATextCompare.class);
        assertNotNull(OATextTokenizer.class);
        assertNotNull(OATextEscape.class);
        assertNotNull(OATextFormat.class);
        assertNotNull(OATextGrammar.class);
        assertNotNull(OATextAlign.class);
        assertNotNull(OATextLineWrap.class);
        assertNotNull(OATextUtil.class);
    }
}
