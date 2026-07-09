package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Store;

class OAPropertyInfoTest {
    @Test
    void constructorAndCoreAccessorsRoundTrip() {
        OAPropertyInfo info = new OAPropertyInfo();
        info.setClassType(int.class);
        info.setId(true);
        info.setUnique(true);
        info.setAutoAssign(true);
        info.setProcessed(true);
        info.setDisplayLength(20);
        info.setMinLength(1);
        info.setMaxLength(40);
        info.setUIColumnLength(12);
        info.setUIColumnName("Store Name");
        info.setName(Store.P_Name);
        info.setLowerName("name");
        info.setDisplayName("Name");
        info.setRequired(true);
        info.setDecimalPlaces(2);

        assertEquals(int.class, info.getClassType());
        assertTrue(info.getIsPrimitive());
        assertTrue(info.getPrimitive());
        assertTrue(info.getId());
        assertTrue(info.isId());
        assertTrue(info.getKey());
        assertTrue(info.isKey());
        assertTrue(info.getUnique());
        assertTrue(info.getAutoAssign());
        assertTrue(info.getProcessed());
        assertEquals(20, info.getDisplayLength());
        assertEquals(1, info.getMinLength());
        assertEquals(40, info.getMaxLength());
        assertEquals(12, info.getUIColumnLength());
        assertEquals("Store Name", info.getUIColumnName());
        assertEquals(Store.P_Name, info.getName());
        assertEquals("name", info.getLowerName());
        assertEquals("Name", info.getDisplayName());
        assertTrue(info.getRequired());
        assertEquals(2, info.getDecimalPlaces());
    }

    @Test
    void flagsAnnotationsDependenciesAndFormatAccessorsRoundTrip() {
        OAPropertyInfo info = new OAPropertyInfo();
        info.setBlob(true);
        info.setNameValue(true);
        info.setUnicode(true);
        info.setEncrypted(true);
        info.setSHAHash(true);
        info.setOAProperty(null);
        info.setCurrency(true);
        info.setHtml(true);
        info.setJson(true);
        info.setTimestamp(true);
        info.setViewDependentProperties(new String[] { "view" });
        info.setModelUserDependentProperties(new String[] { "context" });
        info.setEnabledProperty("enabled");
        info.setEnabledValue(false);
        info.setVisibleProperty("visible");
        info.setVisibleValue(false);
        info.setModelUserEnabledProperty("ctxEnabled");
        info.setModelUserEnabledValue(false);
        info.setModelUserVisibleProperty("ctxVisible");
        info.setModelUserVisibleValue(false);
        info.setObjectCallbackMethod(null);
        info.setTrackPrimitiveNull(true);
        info.setSubmit(true);
        info.setObjectStatus(true);
        info.setIgnoreTimeZone(true);
        info.setTimeZonePropertyPath("tz");
        info.setUpper(true);
        info.setLower(true);
        info.setSensitiveData(true);
        info.setImportMatch(true);
        info.setEnumPropertyName("enumName");
        info.setOAColumn(null);
        info.setFormat("#,##0.00");
        info.setFkeyOnly(true);
        info.setNoPojo(true);
        info.setPojoKeyPos(3);

        assertTrue(info.isBlob());
        assertTrue(info.isNameValue());
        assertTrue(info.isUnicode());
        assertTrue(info.isEncrypted());
        assertTrue(info.isSHAHash());
        assertNull(info.getOAProperty());
        assertTrue(info.isCurrency());
        assertTrue(info.getIsCurrency());
        assertTrue(info.isHtml());
        assertTrue(info.getIsHtml());
        assertTrue(info.isJson());
        assertTrue(info.getIsJson());
        assertTrue(info.isTimestamp());
        assertArrayEquals(new String[] { "view" }, info.getViewDependentProperties());
        assertArrayEquals(new String[] { "context" }, info.getModelUserDependentProperties());
        assertEquals("enabled", info.getEnabledProperty());
        assertFalse(info.getEnabledValue());
        assertEquals("visible", info.getVisibleProperty());
        assertFalse(info.getVisibleValue());
        assertEquals("ctxEnabled", info.getModelUserEnabledProperty());
        assertFalse(info.getModelUserEnabledValue());
        assertEquals("ctxVisible", info.getModelUserVisibleProperty());
        assertFalse(info.getModelUserVisibleValue());
        assertNull(info.getObjectCallbackMethod());
        assertTrue(info.getTrackPrimitiveNull());
        assertTrue(info.getSubmit());
        assertTrue(info.getIsSubmit());
        assertTrue(info.isSubmit());
        assertTrue(info.getObjectStatus());
        assertTrue(info.isObjectStatus());
        assertTrue(info.getIgnoreTimeZone());
        assertEquals("tz", info.getTimeZonePropertyPath());
        assertTrue(info.getUpper());
        assertTrue(info.getIsUpper());
        assertTrue(info.isUpper());
        assertTrue(info.getLower());
        assertTrue(info.getIsLower());
        assertTrue(info.isLower());
        assertTrue(info.getSensitiveData());
        assertTrue(info.isImportMatch());
        assertTrue(info.getImportMatch());
        assertEquals("enumName", info.getEnumPropertyName());
        assertNull(info.getOAColumn());
        assertEquals("#,##0.00", info.getFormat());
        assertTrue(info.isFkeyOnly());
        assertTrue(info.getIsFkeyOnly());
        assertTrue(info.getNoPojo());
        assertEquals(3, info.getPojoKeyPos());
        assertSame(OAPropertyInfo.getFriendAccess(), OAPropertyInfo.getFriendAccess());
    }

    @Test
    void getValueReturnsNullForNonOAObject() {
        OAPropertyInfo name = new OAPropertyInfo();
        name.setName(Store.P_Name);
        assertNull(name.getValue("not oa"));
    }

}
