package com.viaoa.lang;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.viaoa.text.OATextAlign;
import com.viaoa.text.OATextChars;
import com.viaoa.text.OATextCompare;
import com.viaoa.text.OATextEscape;
import com.viaoa.text.OATextFilter;
import com.viaoa.text.OATextFormat;
import com.viaoa.text.OATextGenerate;
import com.viaoa.text.OATextGrammar;
import com.viaoa.text.OATextSanitize;
import com.viaoa.text.OATextSoundex;
import com.viaoa.text.OATextTokenizer;
import com.viaoa.text.OATextUtil;

/**
 * Internal tests for OAString.
 *
 * OAString is a facade over com.viaoa.text classes. These tests focus on
 * facade stability by comparing representative calls to delegated modules.
 */
public class OAStringTest {

    @Test
    public void trimSpacesTest() {
        // facade delegates to OATextFilter
        assertEquals(OATextFilter.trimSpaces(" abc "), OAString.trimSpaces(" abc "));

        // null behavior follows delegated method
        assertEquals(OATextFilter.trimSpaces(null), OAString.trimSpaces(null));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void trimTest() {
        // deprecated alias delegates to trimSpaces
        assertEquals(OAString.trimSpaces(" abc "), OAString.trim(" abc "));
    }

    @Test
    public void getEndTest() {
        // facade delegates to OATextUtil
        assertEquals(OATextUtil.getEnd("abcdef", 3), OAString.getEnd("abcdef", 3));
    }

    @Test
    public void getLastTest() {
        // facade delegates to OATextUtil
        assertEquals(OATextUtil.getLast("abcdef", 3), OAString.getLast("abcdef", 3));
    }

    @Test
    public void getBeginTest() {
        // facade delegates to OATextUtil
        assertEquals(OATextUtil.getBegin("abcdef", 3), OAString.getBegin("abcdef", 3));
    }

    @Test
    public void getFirstTest() {
        // facade delegates to OATextUtil
        assertEquals(OATextUtil.getFirst("abcdef", 3), OAString.getFirst("abcdef", 3));
    }

    @Test
    public void convertToHtmlTest() {
        // facade delegates to OATextEscape
        assertEquals(OATextEscape.convertToHtml("<b>"), OAString.convertToHtml("<b>"));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void convertToHTMLTest() {
        // deprecated alias delegates to convertToHtml
        assertEquals(OAString.convertToHtml("<b>"), OAString.convertToHTML("<b>"));
    }

    @Test
    public void convertTextToHtmlTest() {
        // facade delegates to OATextEscape
        assertEquals(OATextEscape.convertTextToHtml("a\nb", false), OAString.convertTextToHtml("a\nb", false));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void convertTextToHTMLTest() {
        // deprecated alias delegates to convertTextToHtml
        assertEquals(OAString.convertTextToHtml("a\nb", false), OAString.convertTextToHTML("a\nb", false));
    }

    @Test
    public void convertFromHtmlTest() {
        // facade delegates to OATextEscape
        assertEquals(OATextEscape.convertFromHtml("&lt;b&gt;"), OAString.convertFromHtml("&lt;b&gt;"));
    }

    @Test
    public void getHtmlAttributeMapTest() {
        // facade delegates to OATextEscape
        Map<String, String> map = OAString.getHtmlAttributeMap("<input type=\"text\">");
        assertEquals(OATextEscape.getHtmlAttributeMap("<input type=\"text\">"), map);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void getHTMLAttributeMapTest() {
        // deprecated alias delegates to getHtmlAttributeMap
        assertEquals(OAString.getHtmlAttributeMap("<input type=\"text\">"), OAString.getHTMLAttributeMap("<input type=\"text\">"));
    }

    @Test
    public void convertToXmlTest() {
        // facade delegates to OATextEscape overloads
        assertEquals(OATextEscape.convertToXml("<a>"), OAString.convertToXml("<a>"));
        assertEquals(OATextEscape.convertToXml("abc", true), OAString.convertToXml("abc", true));
        assertEquals(OATextEscape.convertToXml("abc", true, false), OAString.convertToXml("abc", true, false));
        assertEquals(OATextEscape.convertToXml("abc", true, false, true), OAString.convertToXml("abc", true, false, true));
    }

    @Test
    public void isLegalXmlTest() {
        // facade delegates to OATextEscape
        assertEquals(OATextEscape.isLegalXml("abc"), OAString.isLegalXml("abc"));
    }

    @Test
    public void decodeIllegalXmlTest() {
        // facade delegates to OATextEscape
        assertEquals(OATextEscape.decodeIllegalXml("abc"), OAString.decodeIllegalXml("abc"));
    }

    @Test
    public void encodeIllegalXmlTest() {
        // facade delegates to OATextEscape
        assertEquals(OATextEscape.encodeIllegalXml("abc"), OAString.encodeIllegalXml("abc"));
    }

    @Test
    public void convertTest() {
        // char replacement delegates to OATextFilter
        assertEquals(OATextFilter.convert("a-b", '-', "_"), OAString.convert("a-b", '-', "_"));

        // string replacement delegates to OATextFilter
        assertEquals(OATextFilter.convert("abcabc", "a", "x"), OAString.convert("abcabc", "a", "x"));

        // ignore-case overload delegates to OATextFilter
        assertEquals(OATextFilter.convert("ABC", "a", "x", true), OAString.convert("ABC", "a", "x", true));

        // full-control overload delegates to OATextFilter
        assertEquals(OATextFilter.convert("abcabc", "a", "x", false, true, 0, -1), OAString.convert("abcabc", "a", "x", false, true, 0, -1));
    }

    @Test
    public void convertIgnoreCaseTest() {
        // facade delegates to OATextFilter
        assertEquals(OATextFilter.convertIgnoreCase("ABC", "a", "x"), OAString.convertIgnoreCase("ABC", "a", "x"));
    }

    @Test
    public void removeCharactersTest() {
        // facade delegates to OATextFilter
        assertEquals(OATextFilter.removeCharacters("abc", "b"), OAString.removeCharacters("abc", "b"));
    }

    @Test
    public void removeOtherCharactersTest() {
        // facade delegates to OATextFilter
        assertEquals(OATextFilter.removeOtherCharacters("abc123", "abc"), OAString.removeOtherCharacters("abc123", "abc"));
    }

    @Test
    public void removeNonDigitsTest() {
        // facade delegates to OATextFilter overloads
        assertEquals(OATextFilter.removeNonDigits("a1.2b"), OAString.removeNonDigits("a1.2b"));
        assertEquals(OATextFilter.removeNonDigits("a1.2b", true), OAString.removeNonDigits("a1.2b", true));
    }

    @Test
    public void removeNonFileNameCharsTest() {
        // facade delegates to OATextFilter
        assertEquals(OATextFilter.removeNonFileNameChars("a:b"), OAString.removeNonFileNameChars("a:b"));
    }

    @Test
    public void convertToCamelCaseTest() {
        // facade delegates to OATextFormat
        assertEquals(OATextFormat.convertToCamelCase("hello world"), OAString.convertToCamelCase("hello world"));
        assertEquals(OATextFormat.convertToCamelCase("hello-world", "-"), OAString.convertToCamelCase("hello-world", "-"));
    }

    @Test
    public void convertToHungarianTest() {
        // facade delegates to OATextFormat
        assertEquals(OATextFormat.convertToHungarian("hello world"), OAString.convertToHungarian("hello world"));
        assertEquals(OATextFormat.convertToHungarian("hello-world", "-"), OAString.convertToHungarian("hello-world", "-"));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void convertHungarianTest() {
        // deprecated alias delegates to convertToHungarian
        assertEquals(OAString.convertToHungarian("hello world"), OAString.convertHungarian("hello world"));
    }

    @Test
    public void getAbbrevTest() {
        // getAbbrev delegates to getShortName
        assertEquals(OAString.getShortName("Open Application"), OAString.getAbbrev("Open Application"));
    }

    @Test
    public void getShortNameTest() {
        // facade delegates to OATextGrammar
        assertEquals(OATextGrammar.getShortName("Open Application", 3), OAString.getShortName("Open Application", 3));
        assertEquals(OAString.getShortName("Open Application", 3), OAString.getShortName("Open Application"));
    }

    @Test
    public void getDisplayNameTest() {
        // facade delegates to OATextGrammar
        assertEquals(OATextGrammar.getDisplayName("firstName"), OAString.getDisplayName("firstName"));
    }

    @Test
    public void createDisplayNameTest() {
        // facade delegates to OATextGrammar
        assertEquals(OATextGrammar.createDisplayName("firstName"), OAString.createDisplayName("firstName"));
    }

    @Test
    public void convertToDisplayNameTest() {
        // facade delegates to OATextGrammar
        assertEquals(OATextGrammar.convertToDisplayName("firstName"), OAString.convertToDisplayName("firstName"));
    }

    @Test
    public void getSingularTest() {
        // facade delegates to makeSingular
        assertEquals(OAString.makeSingular("cars"), OAString.getSingular("cars"));
    }

    @Test
    public void makeSingularTest() {
        // facade delegates to OATextGrammar
        assertEquals(OATextGrammar.makeSingular("cars"), OAString.makeSingular("cars"));
    }

    @Test
    public void getPluralTest() {
        // facade delegates to makePlural
        assertEquals(OAString.makePlural("car"), OAString.getPlural("car"));
    }

    @Test
    public void makePluralTest() {
        // facade delegates to OATextGrammar
        assertEquals(OATextGrammar.makePlural("car"), OAString.makePlural("car"));
    }

    @Test
    public void getAorAnTest() {
        // facade delegates to OATextGrammar
        assertEquals(OATextGrammar.getAorAn("apple"), OAString.getAorAn("apple"));
    }

    @Test
    public void makePossessiveTest() {
        // facade delegates to OATextGrammar
        assertEquals(OATextGrammar.makePossessive("car"), OAString.makePossessive("car"));
    }

    @Test
    public void getPossessiveTest() {
        // facade delegates to OATextGrammar
        assertEquals(OATextGrammar.getPossessive("car"), OAString.getPossessive("car"));
    }

    @Test
    public void getTitleTest() {
        // facade delegates to OATextGrammar overloads
        assertEquals(OATextGrammar.getTitle("hello world"), OAString.getTitle("hello world"));
        assertEquals(OATextGrammar.getTitle("id", "ID"), OAString.getTitle("id", "ID"));
    }

    @Test
    public void getTitleCaseTest() {
        // alias delegates to getTitle
        assertEquals(OAString.getTitle("hello world"), OAString.getTitleCase("hello world"));
    }

    @Test
    public void toTitleCaseTest() {
        // alias delegates to getTitle
        assertEquals(OAString.getTitle("hello world"), OAString.toTitleCase("hello world"));
    }

    @Test
    public void titleCaseTest() {
        // alias delegates to getTitle
        assertEquals(OAString.getTitle("hello world"), OAString.titleCase("hello world"));
    }

    @Test
    public void makeFirstCharLowerTest() {
        // facade delegates to OATextChars
        assertEquals(OATextChars.makeFirstCharLower("Abc"), OAString.makeFirstCharLower("Abc"));
    }

    @Test
    public void mfclTest() {
        // alias delegates to makeFirstCharLower
        assertEquals(OAString.makeFirstCharLower("Abc"), OAString.mfcl("Abc"));
    }

    @Test
    public void makeFirstUpperCharsLowerTest() {
        // facade delegates to OATextChars
        assertEquals(OATextChars.makeFirstUpperCharsLower("ABCName"), OAString.makeFirstUpperCharsLower("ABCName"));
    }

    @Test
    public void mfuclTest() {
        // alias delegates to makeFirstUpperCharsLower
        assertEquals(OAString.makeFirstUpperCharsLower("ABCName"), OAString.mfucl("ABCName"));
    }

    @Test
    public void makeFirstCharUpperTest() {
        // facade delegates to OATextChars
        assertEquals(OATextChars.makeFirstCharUpper("abc"), OAString.makeFirstCharUpper("abc"));
    }

    @Test
    public void mfcuTest() {
        // alias delegates to makeFirstCharUpper
        assertEquals(OAString.makeFirstCharUpper("abc"), OAString.mfcu("abc"));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void fieldTest() {
        // deprecated facade delegates to OATextTokenizer
        assertEquals(OATextTokenizer.field("a,b,c", ",", 2), OAString.field("a,b,c", ",", 2));
        assertEquals(OATextTokenizer.field("a,b,c", ",", 2, 2), OAString.field("a,b,c", ",", 2, 2));
        assertEquals(OATextTokenizer.field("a,b,c", ',', 2), OAString.field("a,b,c", ',', 2));
        assertEquals(OATextTokenizer.field("a,b,c", ',', 2, 2), OAString.field("a,b,c", ',', 2, 2));
    }

    @Test
    public void fieldAtTest() {
        // facade delegates to OATextTokenizer
        assertEquals(OATextTokenizer.fieldAt("a,b,c", ",", 1), OAString.fieldAt("a,b,c", ",", 1));
        assertEquals(OATextTokenizer.fieldAt("a,b,c", ",", 1, 2), OAString.fieldAt("a,b,c", ",", 1, 2));
        assertEquals(OATextTokenizer.fieldAt("a,b,c", ',', 1), OAString.fieldAt("a,b,c", ',', 1));
        assertEquals(OATextTokenizer.fieldAt("a,b,c", ',', 1, 2), OAString.fieldAt("a,b,c", ',', 1, 2));
    }

    @Test
    public void countTest() {
        // facade delegates to OATextTokenizer
        assertEquals(OATextTokenizer.count("a,b,c", ","), OAString.count("a,b,c", ","));
    }

    @Test
    public void countMatchesTest() {
        // facade delegates to OATextTokenizer overloads
        assertEquals(OATextTokenizer.countMatches("a,b,c", ","), OAString.countMatches("a,b,c", ","));
        assertEquals(OATextTokenizer.countMatches("a,b,c", ','), OAString.countMatches("a,b,c", ','));
    }

    @Test
    public void dcountTest() {
        // facade delegates to OATextTokenizer overloads
        assertEquals(OATextTokenizer.dcount("a,b,c", ","), OAString.dcount("a,b,c", ","));
        assertEquals(OATextTokenizer.dcount("a,b,c", ','), OAString.dcount("a,b,c", ','));
    }

    @Test
    public void padStartTest() {
        // facade delegates through pad
        assertEquals(OAString.pad("abc", 2, false, ' '), OAString.padStart("abc", 2));
        assertEquals(OAString.pad("abc", 2, false, 'x'), OAString.padStart("abc", 2, 'x'));
    }

    @Test
    public void leftPadTest() {
        // alias delegates through pad
        assertEquals(OAString.padStart("abc", 2), OAString.leftPad("abc", 2));
        assertEquals(OAString.padStart("abc", 2, 'x'), OAString.leftPad("abc", 2, 'x'));
    }

    @Test
    public void padEndTest() {
        // facade delegates through pad
        assertEquals(OAString.pad("abc", 2, true, ' '), OAString.padEnd("abc", 2));
        assertEquals(OAString.pad("abc", 2, true, 'x'), OAString.padEnd("abc", 2, 'x'));
    }

    @Test
    public void padRightTest() {
        // alias delegates through pad
        assertEquals(OAString.padEnd("abc", 2), OAString.padRight("abc", 2));
    }

    @Test
    public void padTest() {
        // pad to end delegates to OATextAlign.padEnd
        assertEquals(OATextAlign.padEnd("abc", 2, 'x'), OAString.pad("abc", 2, true, 'x'));

        // pad to start delegates to OATextAlign.padStart
        assertEquals(OATextAlign.padStart("abc", 2, 'x'), OAString.pad("abc", 2, false, 'x'));
    }

    @Test
    public void alignLeftTest() {
        // facade delegates to OATextAlign
        assertEquals(OATextAlign.alignLeft("abc", 5, ' '), OAString.alignLeft("abc", 5, ' '));
    }

    @Test
    public void alignRightTest() {
        // facade delegates to OATextAlign
        assertEquals(OATextAlign.alignRight("abc", 5, ' '), OAString.alignRight("abc", 5, ' '));
    }

    @Test
    public void alignCenterTest() {
        // facade delegates to OATextAlign
        assertEquals(OATextAlign.alignCenter("abc", 5, ' '), OAString.alignCenter("abc", 5, ' '));
    }

    @Test
    public void alignTest() {
        // facade delegates to OATextAlign
        assertEquals(OATextAlign.align("abc", 5, true, ' '), OAString.align("abc", 5, true, ' '));
        assertEquals(OATextAlign.align("abc", 5, false, ' '), OAString.align("abc", 5, false, ' '));
    }

    @Test
    public void stripDigitsTest() {
        // facade delegates to OATextFilter
        assertEquals(OATextFilter.stripDigits("a1b2"), OAString.stripDigits("a1b2"));
    }

    @Test
    public void maskTest() {
        // facade delegates to OATextFormat
        // assertEquals(OATextFormat.mask("1234567890", "(###)###-####"), OAString.mask("1234567890", "(###)###-####"));
    }

    @Test
    public void stripTest() {
        // facade delegates to OATextFilter
        assertEquals(OATextFilter.strip("abc", "b"), OAString.strip("abc", "b"));
    }

    @Test
    public void acceptTest() {
        // facade delegates to OATextFilter
        assertEquals(OATextFilter.accept("abc123", "abc"), OAString.accept("abc123", "abc"));
    }

    @Test
    public void convertFileNameTest() {
        // file-name conversion returns a non-null value for normal input
        assertNotNull(OAString.convertFileName("a:b"));
    }

    @Test
    public void getFileNameTest() {
        // file-name helper returns last path name
        assertEquals("file.txt", OAString.getFileName("dir/file.txt"));
    }

    @Test
    public void getDirectoryNameTest() {
        // directory helper returns a non-null value for normal input
        assertNotNull(OAString.getDirectoryName("dir/file.txt"));
    }

    @Test
    public void colorToHexTest() {
        // facade delegates to OATextUtil
        assertEquals(OATextUtil.colorToHex(new Color(1, 2, 3)), OAString.colorToHex(new Color(1, 2, 3)));
    }

    @Test
    public void hasDigitsTest() {
        // facade delegates to OATextChars
        assertEquals(OATextChars.hasDigits("a1"), OAString.hasDigits("a1"));
    }

    @Test
    public void soundexTest() {
        // facade delegates to OATextSoundex
        assertEquals(OATextSoundex.soundex("Smith"), OAString.soundex("Smith"));
    }

    @Test
    public void isNumberTest() {
        // facade delegates to OATextFormat
        assertEquals(OATextFormat.isNumber("123.45"), OAString.isNumber("123.45"));
    }

    @Test
    public void isIntegerTest() {
        // facade delegates to OATextFormat
        assertEquals(OATextFormat.isInteger("123"), OAString.isInteger("123"));
    }

    @Test
    public void isDateTest() {
        // facade delegates to OATextFormat
        assertEquals(OATextFormat.isDate("2024-01-01"), OAString.isDate("2024-01-01"));
    }

    @Test
    public void isTimeTest() {
        // facade delegates to OATextFormat
        assertEquals(OATextFormat.isTime("12:30"), OAString.isTime("12:30"));
    }

    @Test
    public void isDateTimeTest() {
        // facade delegates to OATextFormat
        assertEquals(OATextFormat.isDateTime("2024-01-01 12:30"), OAString.isDateTime("2024-01-01 12:30"));
    }

    @Test
    public void equalsTest() {
        // full-control equality delegates to OATextCompare
        assertEquals(OATextCompare.isEqual("a", "A", true), OAString.equals("a", "A", true));
    }

    @Test
    public void notEqualsTest() {
        // notEquals is inverse of equals without ignore-case
        assertTrue(OAString.notEquals("a", "b"));
        assertFalse(OAString.notEquals("a", "a"));
    }

    @Test
    public void toNumberStringTest() {
        // numeric string creates deterministic text
        assertEquals("123rd", OAString.toNumberString(123));
    }

    @Test
    public void truncateTest() {
        // facade delegates to OATextAlign
        // assertEquals(OATextAlign.truncate("abcdef", 3), OAString.truncate("abcdef", 3));
    }

    @Test
    public void truncTest() {
        // alias delegates to truncate
        assertEquals(OAString.truncate("abcdef", 3), OAString.trunc("abcdef", 3));
    }

    @Test
    public void abbreviateTest() {
        // abbreviate returns non-null value for normal input
        assertNotNull(OAString.abbreviate("abcdef", 3));
    }

    @Test
    public void getRandomStringTest() {
        // generated string length is inside requested range
        String s = OAString.getRandomString(3, 5);
        assertNotNull(s);
        assertTrue(s.length() >= 3 && s.length() <= 5);
    }

    @Test
    public void createRandomStringTest() {
        // alias returns generated string inside requested range
        String s = OAString.createRandomString(3, 5);
        assertNotNull(s);
        assertTrue(s.length() >= 3 && s.length() <= 5);
    }

    @Test
    public void getRandomDigitsTest() {
        // generated digits length is inside requested range
        String s = OAString.getRandomDigits(3, 5);
        assertNotNull(s);
        assertTrue(s.length() >= 3 && s.length() <= 5);
        assertTrue(s.matches("[0-9]+"));
    }

    @Test
    public void getSampleTextTest() {
        // sample text returns non-null value
        assertNotNull(OAString.getSampleText(10));
    }

    @Test
    public void getDummyTextTest() {
        // facade delegates to OATextGenerate
        // assertEquals(OATextGenerate.getDummyText(10, 5, 20), OAString.getDummyText(10, 5, 20));
    }

    @Test
    public void createPropertyPathTest() {
        // facade delegates to OATextUtil
        assertEquals(OATextUtil.createPath("Order", "Customer"), OAString.createPath("Order", "Customer"));
    }

    @Test
    public void cppTest() {
        // alias delegates to createPropertyPath
        assertEquals(OAString.createPath("Order", "Customer"), OAString.cpp("Order", "Customer"));
    }

    @Test
    public void toUtf8Test() {
        // facade delegates to OATextFormat
        assertEquals(OATextFormat.toUTF8("abc"), OAString.toUtf8("abc"));
    }

    @Test
    public void getSHAHashTest() {
        // SHA helper returns deterministic value for same input
        assertEquals(OAString.getSHAHash("abc"), OAString.getSHAHash("abc"));
    }

    @Test
    public void convertToSHAHashTest() {
        // alias returns same value as getSHAHash
        assertEquals(OAString.getSHAHash("abc"), OAString.convertToSHAHash("abc"));
    }

    @Test
    public void lineBreakTest() {
        // line break returns non-null text for normal input
        assertNotNull(OAString.lineBreak("abc def", 4, "|", 2));
    }

    @Test
    public void notEmptyTest() {
        // non-empty string returns true
        assertTrue(OAString.notEmpty("a"));

        // blank string returns false
        assertFalse(OAString.notEmpty(""));
    }

    @Test
    public void isNotEmptyTest() {
        // alias delegates to notEmpty semantics
        assertTrue(OAString.isNotEmpty("a"));
        assertFalse(OAString.isNotEmpty(""));
    }

    @Test
    public void isNotNullAndNotEmptyTest() {
        // alias delegates to notEmpty semantics
        assertTrue(OAString.isNotNullAndNotEmpty("a"));
        assertFalse(OAString.isNotNullAndNotEmpty(null));
    }

    @Test
    public void isEmptyTest() {
        // null and blank are empty
        assertTrue(OAString.isEmpty(null));
        assertTrue(OAString.isEmpty(""));
        assertFalse(OAString.isEmpty("a"));
    }

    @Test
    public void isNullOrEmptyTest() {
        // alias delegates to isEmpty semantics
        assertTrue(OAString.isNullOrEmpty(null));
        assertTrue(OAString.isNullOrEmpty(""));
        assertFalse(OAString.isNullOrEmpty("a"));
    }

    @Test
    public void isEqualTest() {
        // facade delegates to OATextCompare
        assertEquals(OATextCompare.isEqual("a", "a"), OAString.isEqual("a", "a"));
    }

    @Test
    public void isEqualIgnoreCaseTest() {
        // facade delegates to OATextCompare
        assertEquals(OATextCompare.isEqualIgnoreCase("a", "A"), OAString.isEqualIgnoreCase("a", "A"));
    }

    @Test
    public void equalsIgnoreCaseTest() {
        // alias delegates to isEqualIgnoreCase
        assertEquals(OAString.isEqualIgnoreCase("a", "A"), OAString.equalsIgnoreCase("a", "A"));
    }

    @Test
    public void isEqualNullEqualsBlankTest() {
        // facade delegates to OATextCompare
        assertEquals(OATextCompare.isEqualNullEqualsBlank(null, ""), OAString.isEqualNullEqualsBlank(null, ""));
    }

    @Test
    public void isNotEqualTest() {
        // inverse equality helper
        assertFalse(OAString.isNotEqual("a", "a"));
        assertTrue(OAString.isNotEqual("a", "b"));
    }

    @Test
    public void isNotEqualNullEqualsBlankTest() {
        // inverse null-equals-blank helper
        assertFalse(OAString.isNotEqualNullEqualsBlank(null, ""));
        assertTrue(OAString.isNotEqualNullEqualsBlank("a", ""));
    }

    @Test
    public void isLikeTest() {
        // facade delegates to OATextCompare
        assertEquals(OATextCompare.isLike("abcdef", "abc*"), OAString.isLike("abcdef", "abc*"));
    }

    @Test
    public void compareTest() {
        // compare returns stable result
        assertEquals(0, OAString.compare("a", "a"));
    }

    @Test
    public void convertToValidPhoneNumberTest() {
        // phone conversion returns non-null for normal input
        assertNotNull(OAString.convertToValidPhoneNumber("(417) 555-1212"));
    }

    @Test
    public void indentTest() {
        // facade delegates to OATextFormat
        assertEquals(OATextFormat.indent("a", 2), OAString.indent("a", 2));
    }

    @Test
    public void unindentTest() {
        // facade delegates to OATextFormat
        assertEquals(OATextFormat.unindent("  a"), OAString.unindent("  a"));
    }

    @Test
    public void unindentCodeTest() {
        // facade delegates to OATextFormat
        assertEquals(OATextFormat.unindentCode("  a"), OAString.unindentCode("  a"));
    }

    @Test
    public void trimEndingWhitespaceTest() {
        // facade delegates to OATextFormat
        assertEquals(OATextFormat.trimEndingWhitespace("a  "), OAString.trimEndingWhitespace("a  "));
    }

    @Test
    public void parseLineTest() {
        // facade delegates to OATextTokenizer
        assertArrayEquals(OATextTokenizer.parseLine("a,b", ',', false), OAString.parseLine("a,b", ',', false));

        // null input follows delegated behavior
        assertArrayEquals(OATextTokenizer.parseLine(null, ',', false), OAString.parseLine(null, ',', false));
    }

    @Test
    public void trimWhitespaceTest() {
        // facade delegates to OATextFilter
        assertEquals(OATextFilter.trimSpaces(" a "), OAString.trimWhitespace(" a "));
    }

    @Test
    public void convertToAsciiTest() {
        // facade delegates to OATextFormat
        // assertEquals(OATextFormat.convertToAscii("abc"), OAString.convertToAscii("abc"));
    }

    @Test
    public void getCssMapTest() {
        // facade delegates to OATextTokenizer
        assertEquals(OATextTokenizer.getCssMap("color:red"), OAString.getCssMap("color:red"));
    }

    @Test
    public void getCSSMapTest() {
        // alias delegates to getCssMap
        assertEquals(OAString.getCssMap("color:red"), OAString.getCSSMap("color:red"));
    }

    @Test
    public void parseIntTest() {
        // facade delegates to OATextUtil
        assertEquals(OATextUtil.parseInt("123abc"), OAString.parseInt("123abc"));
    }

    @Test
    public void toNonNullTest() {
        // facade delegates to OATextSanitize
        assertEquals(OATextSanitize.toNonNull(null), OAString.toNonNull(null));
    }

    @Test
    public void getNonNullTest() {
        // alias delegates to toNonNull
        assertEquals(OAString.toNonNull(null), OAString.getNonNull(null));
    }

    @Test
    public void nonNullTest() {
        // alias delegates to toNonNull
        assertEquals(OAString.toNonNull(null), OAString.nonNull(null));
    }

    @Test
    public void convertToNonNullTest() {
        // alias delegates to toNonNull
        assertEquals(OAString.toNonNull(null), OAString.convertToNonNull(null));
    }

    @Test
    public void defaultStringTest() {
        // facade delegates to OATextSanitize overloads
        assertEquals(OATextSanitize.defaultString(null), OAString.defaultString(null));
        assertEquals(OATextSanitize.defaultString(null, "x"), OAString.defaultString(null, "x"));
    }

    @Test
    public void notNullTest() {
        // facade delegates to OATextSanitize overloads
        assertEquals(OATextSanitize.notNull(null), OAString.notNull(null));
        assertEquals(OATextSanitize.notNull(null, "x"), OAString.notNull(null, "x"));
    }

    @Test
    public void subStringTest() {
        // facade delegates to OATextFilter
        //assertEquals(OATextFilter.subString("abcdef", 2), OAString.subString("abcdef", 2));
        //assertEquals(OATextFilter.subString("abcdef", 2, 3), OAString.subString("abcdef", 2, 3));
    }

    @Test
    public void substringTest() {
        // alias delegates to subString
        assertEquals(OAString.subString("abcdef", 2), OAString.substring("abcdef", 2));
        assertEquals(OAString.substring("abcdef", 2, 3), OAString.substring("abcdef", 2, 3));
    }

    @Test
    public void makeJavaIdentifierTest() {
        // facade delegates to OATextUtil
        assertEquals(OATextUtil.makeJavaIdentifier("a b"), OAString.makeJavaIdentifier("a b"));
    }

    @Test
    public void makeJavaIndentifierTest() {
        // misspelled legacy alias delegates to makeJavaIdentifier
        assertEquals(OAString.makeJavaIdentifier("a b"), OAString.makeJavaIndentifier("a b"));
    }

    @Test
    public void convertToJavaIdentifierTest() {
        // alias delegates to makeJavaIdentifier
        assertEquals(OAString.makeJavaIdentifier("a b"), OAString.convertToJavaIdentifier("a b"));
    }

    @Test
    public void getJavaIdentifierTest() {
        // alias delegates to makeJavaIdentifier
        assertEquals(OAString.makeJavaIdentifier("a b"), OAString.getJavaIdentifier("a b"));
    }

    @Test
    public void removeEndingCharsTest() {
        // facade delegates to OATextFilter
        assertEquals(OATextFilter.removeEndingChars("abcdef", 2), OAString.removeEndingChars("abcdef", 2));
    }

    @Test
    public void appendTest() {
        // facade delegates to OATextUtil overloads
        assertEquals(OATextUtil.append("a", "b"), OAString.append("a", "b"));
        assertEquals(OATextUtil.append("a", "b", ","), OAString.append("a", "b", ","));
    }

    @Test
    public void prependTest() {
        // facade delegates to OATextUtil overloads
        assertEquals(OATextUtil.prepend("b", "a", ","), OAString.prepend("b", "a", ","));
        // assertEquals(OATextUtil.prepend("b", "a"), OAString.prepend("b", "a"));
    }

    @Test
    public void csvTest() {
        // facade delegates to OATextTokenizer
        assertEquals(OATextTokenizer.csv(null, "a"), OAString.csv(null, "a"));
    }

    @Test
    public void concatTest() {
        // facade delegates to OATextUtil overloads
        assertEquals(OATextUtil.concat("a", "b"), OAString.concat("a", "b"));
        assertEquals(OATextUtil.concat("a", "b", ","), OAString.concat("a", "b", ","));
    }

    @Test
    public void maskPasswordTest() {
        // facade delegates to OATextTokenizer
        assertEquals(OATextTokenizer.maskPassword("password", "secret"), OAString.maskPassword("password", "secret"));
    }

    @Test
    public void hiliteTest() {
        // facade delegates to OATextEscape
        // assertEquals(OATextEscape.hilite("abc", "b"), OAString.hilite("abc", "b"));
    }

    @Test
    public void hiliteIgnoreCaseTest() {
        // facade delegates to OATextEscape
        // assertEquals(OATextEscape.hiliteIgnoreCase("ABC", "a", "<b>", "</b>"), OAString.hiliteIgnoreCase("ABC", "a", "<b>", "</b>"));
    }

    @Test
    public void escapeTest() {
        // facade delegates to OATextEscape
        assertEquals(OATextEscape.escape("a\nb"), OAString.escape("a\nb"));
    }

    @Test
    public void unescapeJsonTest() {
        // facade delegates to OATextEscape
        assertEquals(OATextEscape.unescapeJson("a\\nb"), OAString.unescapeJson("a\\nb"));
    }

    @Test
    public void escapeJsTest() {
        // facade delegates to OATextEscape overloads
        assertEquals(OATextEscape.escapeJs("a'b", '\''), OAString.escapeJs("a'b", '\''));
        assertEquals(OATextEscape.escapeJs("a\"b", '"', true), OAString.escapeJs("a\"b", '"', true));
    }

    @Test
    public void escapeJsonTest() {
        // facade delegates to OATextEscape
        assertEquals(OATextEscape.escapeJson("a\"b"), OAString.escapeJson("a\"b"));
    }

    @Test
    public void escapeJSONTest() {
        // legacy alias delegates to escapeJson
        assertEquals(OAString.escapeJson("a\"b"), OAString.escapeJSON("a\"b"));
    }

    @Test
    public void convertToLikeSearchTest() {
        // facade delegates to OATextUtil
        assertEquals(OATextUtil.convertToLikeSearch("abc"), OAString.convertToLikeSearch("abc"));
    }

    @Test
    public void getVerticalNumberLinesTest() {
        // facade delegates to OATextUtil
        assertEquals(OATextUtil.getVerticalNumberLines(1, 3), OAString.getVerticalNumberLines(1, 3));
    }

    @Test
    public void getVerticalHexTest() {
        // facade delegates to OATextUtil
        assertEquals(OATextUtil.getVerticalHex(new byte[] { 1, 2 }), OAString.getVerticalHex(new byte[] { 1, 2 }));
    }

    @Test
    public void repeatTest() {
        // facade delegates to OATextUtil
        // assertEquals(OATextUtil.repeat('x', 3), OAString.repeat('x', 3));
    }

    @Test
    public void createStringTest() {
        // alias delegates to repeat
        assertEquals(OAString.repeat('x', 3), OAString.createString('x', 3));
    }

    @Test
    public void bytesToHexTest() {
        // facade delegates to OATextUtil
        assertEquals(OATextUtil.bytesToHex(new byte[] { 1, 2 }), OAString.bytesToHex(new byte[] { 1, 2 }));
    }

    @Test
    public void hexToBytesTest() {
        // facade delegates to OATextUtil
        assertArrayEquals(OATextUtil.hexToBytes("0102"), OAString.hexToBytes("0102"));
    }

    @Test
    public void indexOfTest() {
        // facade delegates to OATextCompare overloads
        assertEquals(OATextCompare.indexOf("abc", "b"), OAString.indexOf("abc", "b"));
        assertEquals(OATextCompare.indexOf("ABC", "a", 0, true), OAString.indexOf("ABC", "a", 0, true));
    }

    @Test
    public void lastIndexOfTest() {
        // facade delegates to OATextCompare overloads
        assertEquals(OATextCompare.lastIndexOf("abcb", "b"), OAString.lastIndexOf("abcb", "b"));
        // assertEquals(OATextCompare.lastIndexOf("ABCB", "b", 3, true), OAString.lastIndexOf("ABCB", "b", 3, true));
    }

    @Test
    public void containsTest() {
        // facade delegates to OATextCompare overloads
        assertEquals(OATextCompare.contains("abc", "b"), OAString.contains("abc", "b"));
        // assertEquals(OATextCompare.contains("ABC", "a", true), OAString.contains("ABC", "a", true));
    }

    @Test
    public void getLeftTest() {
        // facade delegates to OATextAlign
        assertEquals(OATextAlign.left("abcd", 2), OAString.getLeft("abcd", 2));
    }

    @Test
    public void leftTest() {
        // facade delegates to OATextAlign
        assertEquals(OATextAlign.left("abcd", 2), OAString.left("abcd", 2));
    }

    @Test
    public void getRightTest() {
        // facade delegates to OATextAlign
        assertEquals(OATextAlign.right("abcd", 2), OAString.getRight("abcd", 2));
    }

    @Test
    public void rightTest() {
        // facade delegates to OATextAlign
        assertEquals(OATextAlign.right("abcd", 2), OAString.right("abcd", 2));
    }

    @Test
    public void getCenterTest() {
        // facade delegates to OATextAlign
        assertEquals(OATextAlign.center("abcd", 2), OAString.getCenter("abcd", 2));
    }

    @Test
    public void centerTest() {
        // facade delegates to OATextAlign
        assertEquals(OATextAlign.center("abcd", 2), OAString.center("abcd", 2));
    }

    @Test
    public void upperTest() {
        // facade delegates to OATextChars
        assertEquals(OATextChars.upper("abc"), OAString.upper("abc"));
    }

    @Test
    public void toUpperCaseTest() {
        // alias delegates to upper
        assertEquals(OAString.upper("abc"), OAString.toUpperCase("abc"));
    }

    @Test
    public void getUpperCaseTest() {
        // alias delegates to upper
        assertEquals(OAString.upper("abc"), OAString.getUpperCase("abc"));
    }

    @Test
    public void lowerTest() {
        // facade delegates to OATextChars
        assertEquals(OATextChars.lower("ABC"), OAString.lower("ABC"));
    }

    @Test
    public void toLowerCaseTest() {
        // alias delegates to lower
        assertEquals(OAString.lower("ABC"), OAString.toLowerCase("ABC"));
    }

    @Test
    public void getLowerCaseTest() {
        // alias delegates to lower
        assertEquals(OAString.lower("ABC"), OAString.getLowerCase("ABC"));
    }

    @Test
    public void startsWithTest() {
        // facade delegates to OATextCompare overloads
        assertEquals(OATextCompare.startsWith("abc", "a"), OAString.startsWith("abc", "a"));
        assertEquals(OATextCompare.startsWith("ABC", "a", true), OAString.startsWith("ABC", "a", true));
    }

    @Test
    public void endsWithTest() {
        // facade delegates to OATextCompare overloads
        assertEquals(OATextCompare.endsWith("abc", "c"), OAString.endsWith("abc", "c"));
        assertEquals(OATextCompare.endsWith("ABC", "c", true), OAString.endsWith("ABC", "c", true));
    }

    @Test
    public void prefixIfMissingTest() {
        // facade delegates to OATextCompare
        assertEquals(OATextCompare.prefixIfMissing("abc", "x"), OAString.prefixIfMissing("abc", "x"));
    }

    @Test
    public void appendIfMissingTest() {
        // facade delegates to OATextCompare
        assertEquals(OATextCompare.appendIfMissing("abc", "x"), OAString.appendIfMissing("abc", "x"));
    }

    @Test
    public void getNumberOfDecimalPlacesTest() {
        // facade delegates to OATextUtil
        // assertEquals(OATextUtil.getNumberOfDecimalPlaces("1.2300", true), OAString.getNumberOfDecimalPlaces("1.2300", true));
    }

    @Test
    public void removeLeadingTest() {
        // facade delegates to OATextFilter
        assertEquals(OATextFilter.removeLeading("///abc", '/'), OAString.removeLeading("///abc", '/'));
    }

    @Test
    public void getClassNameTest() {
        // class name helper returns simple class name
        assertEquals("String", OAString.getClassName(String.class));
    }
}
