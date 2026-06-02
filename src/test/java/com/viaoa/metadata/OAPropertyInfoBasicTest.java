package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class OAPropertyInfoBasicTest {

    public static class Bean {
        public String getName() {
            return "Bob";
        }
    }

    @Test
    void classTypeTracksPrimitiveFlag() {
        OAPropertyInfo pi = new OAPropertyInfo();

        assertFalse(pi.getIsPrimitive());
        assertFalse(pi.getPrimitive());

        pi.setClassType(int.class);
        assertEquals(int.class, pi.getClassType());
        assertTrue(pi.getIsPrimitive());
        assertTrue(pi.getPrimitive());

        pi.setClassType(Integer.class);
        assertEquals(Integer.class, pi.getClassType());
        assertFalse(pi.getIsPrimitive());
        assertFalse(pi.getPrimitive());

        pi.setClassType(null);
        assertNull(pi.getClassType());
        assertFalse(pi.getIsPrimitive());
    }

    @Test
    void idAndKeyAreSameUnderlyingContract() {
        OAPropertyInfo pi = new OAPropertyInfo();

        assertFalse(pi.getId());
        assertFalse(pi.isId());
        assertFalse(pi.getKey());
        assertFalse(pi.isKey());

        pi.setId(true);

        assertTrue(pi.getId());
        assertTrue(pi.isId());
        assertTrue(pi.getKey());
        assertTrue(pi.isKey());
    }

    @Test
    void nameLowerNameAndDisplayNameRoundTrip() {
        OAPropertyInfo pi = new OAPropertyInfo();

        pi.setName("FirstName");
        assertEquals("FirstName", pi.getName());
        assertEquals("firstname", pi.getLowerName());

        pi.setLowerName("customlower");
        assertEquals("customlower", pi.getLowerName());

        pi.setDisplayName("First Name");
        assertEquals("First Name", pi.getDisplayName());
    }

    @Test
    void lengthAndColumnMetadataRoundTrip() {
        OAPropertyInfo pi = new OAPropertyInfo();

        pi.setDisplayLength(12);
        pi.setMinLength(2);
        pi.setMaxLength(40);
        pi.setUIColumnName("Column");
        pi.setUIColumnLength(18);

        assertEquals(12, pi.getDisplayLength());
        assertEquals(2, pi.getMinLength());
        assertEquals(40, pi.getMaxLength());
        assertEquals("Column", pi.getUIColumnName());
        assertEquals(18, pi.getUIColumnLength());
    }

    @Test
    void booleanFlagsRoundTrip() {
        OAPropertyInfo pi = new OAPropertyInfo();

        pi.setRequired(true);
        pi.setUnique(true);
        pi.setAutoAssign(true);
        pi.setProcessed(true);
        pi.setBlob(true);
        pi.setNameValue(true);
        pi.setUnicode(true);
        pi.setEncrypted(true);
        pi.setSHAHash(true);
        pi.setCurrency(true);
        pi.setHtml(true);
        pi.setJson(true);
        pi.setTimestamp(true);
        pi.setTrackPrimitiveNull(false);
        pi.setIsSubmit(true);
        pi.setObjectStatus(true);
        pi.setIgnoreTimeZone(true);
        pi.setUpper(true);
        pi.setLower(true);
        pi.setSensitiveData(true);
        pi.setImportMatch(true);
        pi.setFkeyOnly(true);
        pi.setNoPojo(true);

        assertTrue(pi.getRequired());
        assertTrue(pi.getUnique());
        assertTrue(pi.getAutoAssign());
        assertTrue(pi.getProcessed());
        assertTrue(pi.isBlob());
        assertTrue(pi.isNameValue());
        assertTrue(pi.isUnicode());
        assertTrue(pi.isEncrypted());
        assertTrue(pi.isSHAHash());
        assertTrue(pi.isCurrency());
        assertTrue(pi.getIsCurrency());
        assertTrue(pi.isHtml());
        assertTrue(pi.getIsHtml());
        assertTrue(pi.isJson());
        assertTrue(pi.getIsJson());
        assertTrue(pi.isTimestamp());
        assertFalse(pi.getTrackPrimitiveNull());
        assertTrue(pi.getIsSubmit());
        assertTrue(pi.getSubmit());
        assertTrue(pi.isSubmit());
        assertTrue(pi.getObjectStatus());
        assertTrue(pi.isObjectStatus());
        assertTrue(pi.getIgnoreTimeZone());
        assertTrue(pi.getIsUpper());
        assertTrue(pi.getUpper());
        assertTrue(pi.isUpper());
        assertTrue(pi.getIsLower());
        assertTrue(pi.getLower());
        assertTrue(pi.isLower());
        assertTrue(pi.getSensitiveData());
        assertTrue(pi.isImportMatch());
        assertTrue(pi.getImportMatch());
        assertTrue(pi.isFkeyOnly());
        assertTrue(pi.getIsFkeyOnly());
        assertTrue(pi.getNoPojo());
    }

    @Test
    void formatTimezoneEnumAndPojoMetadataRoundTrip() {
        OAPropertyInfo pi = new OAPropertyInfo();

        pi.setDecimalPlaces(3);
        pi.setFormat("0.000");
        pi.setTimeZonePropertyPath("store.timeZone");
        pi.setEnumPropertyName("statusEnum");
        pi.setPojoKeyPos(2);

        assertEquals(3, pi.getDecimalPlaces());
        assertEquals("0.000", pi.getFormat());
        assertEquals("store.timeZone", pi.getTimeZonePropertyPath());
        assertEquals("statusEnum", pi.getEnumPropertyName());
        assertEquals(2, pi.getPojoKeyPos());
    }

    @Test
    void dependencyAndUiRuleMetadataRoundTripByReference() {
        OAPropertyInfo pi = new OAPropertyInfo();

        String[] view = { "a", "b" };
        String[] context = { "c" };
        pi.setViewDependentProperties(view);
        pi.setContextDependentProperties(context);

        pi.setEnabledProperty("enabled");
        pi.setEnabledValue(true);
        pi.setVisibleProperty("visible");
        pi.setVisibleValue(false);
        pi.setContextEnabledProperty("ctxEnabled");
        pi.setContextEnabledValue(true);
        pi.setContextVisibleProperty("ctxVisible");
        pi.setContextVisibleValue(false);

        assertSame(view, pi.getViewDependentProperties());
        assertSame(context, pi.getContextDependentProperties());
        assertEquals("enabled", pi.getEnabledProperty());
        assertTrue(pi.getEnabledValue());
        assertEquals("visible", pi.getVisibleProperty());
        assertFalse(pi.getVisibleValue());
        assertEquals("ctxEnabled", pi.getContextEnabledProperty());
        assertTrue(pi.getContextEnabledValue());
        assertEquals("ctxVisible", pi.getContextVisibleProperty());
        assertFalse(pi.getContextVisibleValue());
    }

    @Test
    void callbackMethodAndValueAccessUseStoredMethod() throws Exception {
        OAPropertyInfo pi = new OAPropertyInfo();
        Method m = Bean.class.getMethod("getName");

        pi.setObjectCallbackMethod(m);

        assertSame(m, pi.getObjectCallbackMethod());
        assertEquals("Bob", pi.getValue(new Bean()));
    }
}
