package com.viaoa.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.sql.Types;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.viaoa.callback.OAObjectCallback;

@SuppressWarnings("deprecation")
class OATableTest {

    @Test
    void metadataHasRuntimeRetentionTargetAndDocumented() {
        assertNotNull(OATable.class.getAnnotation(Documented.class));
        assertEquals(RetentionPolicy.RUNTIME, OATable.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[] { ElementType.TYPE }, OATable.class.getAnnotation(Target.class).value());
    }

    @OATable(
        name = "explicit_name",
        indexes = { @OAIndex(name = "idx", columns = { @OAIndexColumn(name = "Col", lowerName = "col", descend = true) }, fkey = true, unique = true) }
    )
    private static class ExplicitFixture {}

    @OATable
    private static class DefaultFixture {}

    @Test
    void defaultNameMatchesDeclaration() throws Exception {
        OATable ann = DefaultFixture.class.getAnnotation(OATable.class);

        assertEquals("", ann.name());
    }

    @Test
    void defaultIndexesMatchesDeclaration() throws Exception {
        OATable ann = DefaultFixture.class.getAnnotation(OATable.class);

        assertArrayEquals(new OAIndex[0], ann.indexes());
    }

    @Test
    void explicitValuesCanBeReadWithReflection() throws Exception {
        OATable ann = ExplicitFixture.class.getAnnotation(OATable.class);

        assertEquals("explicit_name", ann.name());
        assertEquals(1, ann.indexes().length);
    }
}
