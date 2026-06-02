package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAPropertyInfoFormatAndColumnContractTest {

    @Test
    void columnAndDatabaseMetadataRoundTrip() {
        OAPropertyInfo pi = new OAPropertyInfo();

        pi.setColumnName("FIRST_NAME");
        pi.setColumnSqlType("VARCHAR");
        pi.setColumnLength(40);
        pi.setColumnDecimalPlaces(2);
        pi.setColumnAllowNull(false);
        pi.setColumnLowerName("first_name");

        assertEquals("FIRST_NAME", pi.getColumnName());
        assertEquals("VARCHAR", pi.getColumnSqlType());
        assertEquals(40, pi.getColumnLength());
        assertEquals(2, pi.getColumnDecimalPlaces());
        assertFalse(pi.getColumnAllowNull());
        assertEquals("first_name", pi.getColumnLowerName());
    }

    @Test
    void formatAndDecimalDefaultsAreSafe() {
        OAPropertyInfo pi = new OAPropertyInfo();

        assertNull(pi.getFormat());
        assertEquals(-1, pi.getDecimalPlaces());

        pi.setDecimalPlaces(0);
        pi.setFormat("0");

        assertEquals(0, pi.getDecimalPlaces());
        assertEquals("0", pi.getFormat());
    }

    @Test
    void pojoAndImportMetadataRoundTrip() {
        OAPropertyInfo pi = new OAPropertyInfo();

        pi.setPojoName("pojoFirstName");
        pi.setPojoProperty(true);
        pi.setPojoId(true);
        pi.setPojoKey(true);
        pi.setPojoKeyPos(1);
        pi.setImportMatch(true);
        pi.setImportMatchProperty("code");

        assertEquals("pojoFirstName", pi.getPojoName());
        assertTrue(pi.getPojoProperty());
        assertTrue(pi.getPojoId());
        assertTrue(pi.getPojoKey());
        assertEquals(1, pi.getPojoKeyPos());
        assertTrue(pi.getImportMatch());
        assertEquals("code", pi.getImportMatchProperty());
    }

    @Test
    void sensitiveAndSecurityFlagsAreIndependent() {
        OAPropertyInfo pi = new OAPropertyInfo();

        pi.setSensitiveData(true);
        assertTrue(pi.getSensitiveData());
        assertFalse(pi.isEncrypted());
        assertFalse(pi.isSHAHash());

        pi.setEncrypted(true);
        pi.setSHAHash(true);

        assertTrue(pi.getSensitiveData());
        assertTrue(pi.isEncrypted());
        assertTrue(pi.isSHAHash());
    }

    @Test
    void primitiveNullTrackingDefaultIsTrueAndCanBeDisabled() {
        OAPropertyInfo pi = new OAPropertyInfo();

        assertTrue(pi.getTrackPrimitiveNull());

        pi.setTrackPrimitiveNull(false);
        assertFalse(pi.getTrackPrimitiveNull());

        pi.setTrackPrimitiveNull(true);
        assertTrue(pi.getTrackPrimitiveNull());
    }
}
