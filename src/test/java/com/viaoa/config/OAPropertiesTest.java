package com.viaoa.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OAPropertiesTest {

    @TempDir
    Path tempDir;

    private static class CloseAwareInputStream extends ByteArrayInputStream {
        boolean closed;

        CloseAwareInputStream(String text) {
            super(text.getBytes(StandardCharsets.ISO_8859_1));
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    @Test
    void defaultConstructorCreatesEmptyProperties() {
        OAProperties props = new OAProperties();

        assertNull(props.getFileName());
        assertFalse(props.exists("missing"));
        assertFalse(props.keys().hasMoreElements());
    }

    @Test
    void fileConstructorStoresFileNameAndLoadsExistingFile() throws Exception {
        Path file = tempDir.resolve("app.properties");
        Files.writeString(file, "ServerPort=1099\nEnabled=true\n", StandardCharsets.ISO_8859_1);

        OAProperties props = new OAProperties(file.toString());

        assertEquals(file.toString(), props.getFileName());
        assertEquals("1099", props.getProperty("serverport"));
        assertTrue(props.getBoolean("enabled"));
    }

    @Test
    void inputStreamConstructorLoadsAndClosesStream() {
        CloseAwareInputStream in = new CloseAwareInputStream("Name=POS\nCount=7\n");

        OAProperties props = new OAProperties(in);

        assertEquals("POS", props.getProperty("name"));
        assertEquals(7, props.getInt("COUNT"));
        assertTrue(in.closed);
    }

    @Test
    void setFileNameAndGetFileNameRoundTrip() {
        OAProperties props = new OAProperties();

        props.setFileName("config/test.properties");

        assertEquals("config/test.properties", props.getFileName());
    }

    @Test
    void loadWithoutFileNameThrows() {
        OAProperties props = new OAProperties();

        assertThrows(IllegalArgumentException.class, props::load);
    }

    @Test
    void loadUsesAssociatedFileName() throws Exception {
        Path file = tempDir.resolve("load.properties");
        Files.writeString(file, "Name=Loaded\n", StandardCharsets.ISO_8859_1);
        OAProperties props = new OAProperties();
        props.setFileName(file.toString());

        props.load();

        assertEquals("Loaded", props.getString("name"));
    }

    @Test
    void loadStringNormalizesMissingFileAsNoOpAndStoresFileName() {
        Path file = tempDir.resolve("missing.properties");
        OAProperties props = new OAProperties();

        props.load(file.toString());

        assertEquals(file.toString(), props.getFileName());
        assertFalse(props.exists("anything"));
    }

    @Test
    void loadInputStreamAddsPropertiesWithoutClearingExistingValues() {
        OAProperties props = new OAProperties();
        props.setProperty("Existing", "old");

        props.load(new ByteArrayInputStream("Name=Loaded\n".getBytes(StandardCharsets.ISO_8859_1)));

        assertEquals("old", props.getString("existing"));
        assertEquals("Loaded", props.getString("name"));
    }

    @Test
    void saveWithoutFileNameThrows() {
        OAProperties props = new OAProperties();

        assertThrows(IllegalArgumentException.class, props::save);
    }

    @Test
    void saveUsesAssociatedFileName() throws Exception {
        Path file = tempDir.resolve("save.properties");
        OAProperties props = new OAProperties();
        props.setFileName(file.toString());
        props.setProperty("Name", "Saved");

        props.save();

        OAProperties loaded = new OAProperties(file.toString());
        assertEquals("Saved", loaded.getString("name"));
    }

    @Test
    void saveStringWritesPropertiesAndUpdatesFileName() throws Exception {
        Path file = tempDir.resolve("saveString.properties");
        OAProperties props = new OAProperties();
        props.setProperty("Name", "Saved");

        props.save(file.toString());

        assertEquals(file.toString(), props.getFileName());
        assertEquals("Saved", new OAProperties(file.toString()).getString("name"));
    }

    @Test
    void saveStringWithTitleCreatesParentDirectoriesAndHandlesNullTitle() throws Exception {
        Path file = tempDir.resolve("nested").resolve("config.properties");
        OAProperties props = new OAProperties();
        props.setProperty("Name", "Saved");

        props.save(file.toString(), null);

        assertTrue(Files.exists(file));
        assertEquals("Saved", new OAProperties(file.toString()).getString("name"));
        assertThrows(RuntimeException.class, () -> props.save((String) null, "title"));
    }

    @Test
    void getPropertyIsCaseInsensitiveAndSupportsMissingKeys() {
        OAProperties props = new OAProperties();
        props.setProperty("ServerPort", "1099");

        assertEquals("1099", props.getProperty("serverport"));
        assertEquals("1099", props.getProperty("SERVERPORT"));
        assertNull(props.getProperty(null));
        assertNull(props.getProperty("missing"));
    }

    @Test
    void getPropertyWithDefaultUsesDefaultOnlyWhenMissing() {
        OAProperties props = new OAProperties();
        props.setProperty("Mode", "dev");

        assertEquals("dev", props.getProperty("mode", "prod"));
        assertEquals("prod", props.getProperty("missing", "prod"));
    }

    @Test
    void getDelegatesToCaseInsensitivePropertyLookup() {
        OAProperties props = new OAProperties();
        props.setProperty("Name", "POS");

        assertEquals("POS", props.get("name"));
        assertNull(props.get((String) null));
    }

    @Test
    void setPropertyStringStoresStringValue() {
        OAProperties props = new OAProperties();

        Object old = props.setProperty("Name", "POS");
        Object old2 = props.setProperty("name", "Store");

        assertNull(old);
        assertNull(old2);
        assertEquals("Store", props.getString("NAME"));
    }

    @Test
    void setPropertyBooleanStoresConvertedValue() {
        OAProperties props = new OAProperties();

        props.setProperty("Enabled", true);

        assertEquals("true", props.getString("enabled"));
        assertTrue(props.getBoolean("enabled"));
    }

    @Test
    void setPropertyIntStoresConvertedValue() {
        OAProperties props = new OAProperties();

        props.setProperty("Port", 1099);

        assertEquals("1099", props.getString("port"));
        assertEquals(1099, props.getInt("port"));
    }

    @Test
    void setPropertyLongStoresConvertedValue() {
        OAProperties props = new OAProperties();

        props.setProperty("Timeout", 1234567890123L);

        assertEquals("1234567890123", props.getString("timeout"));
    }

    @Test
    void setPropertyDoubleStoresConvertedValue() {
        OAProperties props = new OAProperties();

        props.setProperty("Rate", 12.5d);

        assertEquals("12.5", props.getString("rate"));
    }

    @Test
    void setPropertyObjectStoresConvertedValueAndConvertsNullToEmptyString() {
        OAProperties props = new OAProperties();
        props.setProperty("Value", new StringBuilder("abc"));

        props.setProperty("VALUE", (Object) null);

        assertEquals("", props.getProperty("value"));
    }

    @Test
    void keysReturnInsertionOrderAndCurrentCase() {
        OAProperties props = new OAProperties();
        props.setProperty("First", "1");
        props.setProperty("Second", "2");

        assertEquals(List.of("First", "Second"), keys(props));

        props.setProperty("first", "updated");
        assertEquals(List.of("Second", "first"), keys(props));
    }

    @Test
    void clearRemovesPropertiesAndKeyOrder() {
        OAProperties props = new OAProperties();
        props.setProperty("Name", "POS");

        props.clear();

        assertNull(props.getProperty("name"));
        assertFalse(props.keys().hasMoreElements());
    }

    @Test
    void putRejectsNonStringKeysAndStoresStringValues() {
        OAProperties props = new OAProperties();

        assertNull(props.put(123, "bad"));
        props.put("Name", 42);

        assertEquals("42", props.getString("name"));
    }

    @Test
    void putNullValueRemovesExistingProperty() {
        OAProperties props = new OAProperties();
        props.put("Name", "POS");

        Object old = props.put("Name", null);

        assertEquals("POS", old);
        assertNull(props.getString("name"));
    }

    @Test
    void removeIsCaseInsensitiveAndRejectsNonStringKeys() {
        OAProperties props = new OAProperties();
        props.setProperty("Name", "POS");

        assertNull(props.remove(1));
        assertEquals("POS", props.remove("name"));
        assertNull(props.getProperty("NAME"));
        assertNull(props.remove("missing"));
    }

    @Test
    void putIntAndPutBooleanStoreValues() {
        OAProperties props = new OAProperties();

        props.put("Port", 1099);
        props.put("Enabled", true);
        props.put((String) null, 5);
        props.put((String) null, false);

        assertEquals(1099, props.getInt("port"));
        assertTrue(props.getBoolean("enabled"));
    }

    @Test
    void putIntAliasStoresIntegerValue() {
        OAProperties props = new OAProperties();

        props.putInt("Port", 1099);

        assertEquals(1099, props.getInt("port"));
    }

    @Test
    void getIntReturnsMinusOneForMissingOrInvalidValues() {
        OAProperties props = new OAProperties();
        props.setProperty("Bad", "abc");

        assertEquals(-1, props.getInt("missing"));
        assertEquals(-1, props.getInt("bad"));
        assertEquals(-1, props.getInt(null));
    }

    @Test
    void getIntWithDefaultUsesDefaultForMissingOnly() {
        OAProperties props = new OAProperties();
        props.setProperty("Good", "7");
        props.setProperty("Bad", "abc");

        assertEquals(7, props.getInt("good", 99));
        assertEquals(99, props.getInt("missing", 99));
        assertEquals(-1, props.getInt("bad", 99));
    }

    @Test
    void getStringReturnsNullForMissingOrNullName() {
        OAProperties props = new OAProperties();
        props.setProperty("Name", "POS");

        assertEquals("POS", props.getString("name"));
        assertNull(props.getString("missing"));
        assertNull(props.getString(null));
    }

    @Test
    void getStringWithDefaultUsesDefaultForMissingOnly() {
        OAProperties props = new OAProperties();
        props.setProperty("Name", "POS");

        assertEquals("POS", props.getString("name", "default"));
        assertEquals("default", props.getString("missing", "default"));
        assertNull(props.getString(null, "default"));
    }

    @Test
    void getBooleanReturnsFalseForMissingOrNullAndUsesConverterForStrings() {
        OAProperties props = new OAProperties();
        props.setProperty("TrueValue", "true");
        props.setProperty("FalseValue", "false");
        props.setProperty("Bad", "notBoolean");

        assertTrue(props.getBoolean("truevalue"));
        assertFalse(props.getBoolean("falsevalue"));
        assertTrue(props.getBoolean("bad"));
        assertFalse(props.getBoolean("missing"));
        assertFalse(props.getBoolean(null));
    }

    @Test
    void getBooleanWithDefaultUsesDefaultOnlyWhenMissingAndUsesConverterForStrings() {
        OAProperties props = new OAProperties();
        props.setProperty("FalseValue", "false");
        props.setProperty("Bad", "notBoolean");

        assertFalse(props.getBoolean("falsevalue", true));
        assertTrue(props.getBoolean("missing", true));
        assertTrue(props.getBoolean("bad", true));
        assertFalse(props.getBoolean(null, true));
    }

    @Test
    void existsUsesCaseInsensitiveLookup() {
        OAProperties props = new OAProperties();
        props.setProperty("Name", "POS");

        assertTrue(props.exists("name"));
        assertFalse(props.exists("missing"));
        assertFalse(props.exists(null));
    }

    private static List<String> keys(OAProperties props) {
        ArrayList<String> list = new ArrayList<>();
        Enumeration<?> en = props.keys();
        while (en.hasMoreElements()) {
            list.add((String) en.nextElement());
        }
        return list;
    }
}
