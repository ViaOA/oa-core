package com.viaoa.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.sql.Types;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.viaoa.callback.OAObjectCallback;

@SuppressWarnings("deprecation")
class OALinkTableTest {

    @Test
    void metadataHasRuntimeRetentionTargetAndDocumented() {
        assertNotNull(OALinkTable.class.getAnnotation(Documented.class));
        assertEquals(RetentionPolicy.RUNTIME, OALinkTable.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[] { ElementType.METHOD }, OALinkTable.class.getAnnotation(Target.class).value());
    }

    private static class ExplicitFixture {
        @OALinkTable(
        name = "explicit_name",
        columns = { "one", "two" },
        indexName = "explicit_indexName"
    )
        public String value() { return "value"; }
    }

    private static class DefaultFixture {
        @OALinkTable(name = "default_link", columns = { "Id" }, indexName = "default_idx")
        public String value() { return "value"; }
    }

    @Test
    void nameExplicitValueCanBeRead() throws Exception {
        OALinkTable ann = ExplicitFixture.class.getMethod("value").getAnnotation(OALinkTable.class);

        assertEquals("explicit_name", ann.name());
    }

    @Test
    void columnsExplicitValueCanBeRead() throws Exception {
        OALinkTable ann = ExplicitFixture.class.getMethod("value").getAnnotation(OALinkTable.class);

        assertArrayEquals(new String[] { "one", "two" }, ann.columns());
    }

    @Test
    void indexNameExplicitValueCanBeRead() throws Exception {
        OALinkTable ann = ExplicitFixture.class.getMethod("value").getAnnotation(OALinkTable.class);

        assertEquals("explicit_indexName", ann.indexName());
    }

    @Test
    void explicitValuesCanBeReadWithReflection() throws Exception {
        OALinkTable ann = ExplicitFixture.class.getMethod("value").getAnnotation(OALinkTable.class);

        assertEquals("explicit_name", ann.name());
        assertArrayEquals(new String[] { "one", "two" }, ann.columns());
        assertEquals("explicit_indexName", ann.indexName());
    }
}
