package com.viaoa.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.sql.Types;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.viaoa.callback.OAObjectCallback;

@SuppressWarnings("deprecation")
class OAIndexTest {

    @Test
    void metadataHasRuntimeRetentionTargetAndDocumented() {
        assertNotNull(OAIndex.class.getAnnotation(Documented.class));
        assertEquals(RetentionPolicy.RUNTIME, OAIndex.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[0], OAIndex.class.getAnnotation(Target.class).value());
    }

    @OATable(indexes = { @OAIndex(name = "idx", columns = { @OAIndexColumn(name = "Col") }, fkey = true, unique = true) })
    private static class ExplicitFixture {}

    @OATable(indexes = { @OAIndex(name = "idx", columns = { @OAIndexColumn(name = "Col") }) })
    private static class DefaultFixture {}

    @Test
    void defaultFkeyMatchesDeclaration() throws Exception {
        OAIndex ann = DefaultFixture.class.getAnnotation(OATable.class).indexes()[0];

        assertEquals(false, ann.fkey());
    }

    @Test
    void defaultUniqueMatchesDeclaration() throws Exception {
        OAIndex ann = DefaultFixture.class.getAnnotation(OATable.class).indexes()[0];

        assertEquals(false, ann.unique());
    }

    @Test
    void nameExplicitValueCanBeRead() throws Exception {
        OAIndex ann = ExplicitFixture.class.getAnnotation(OATable.class).indexes()[0];

        assertEquals("idx", ann.name());
    }

    @Test
    void columnsExplicitValueCanBeRead() throws Exception {
        OAIndex ann = ExplicitFixture.class.getAnnotation(OATable.class).indexes()[0];

        assertEquals(1, ann.columns().length);
    }

    @Test
    void explicitValuesCanBeReadWithReflection() throws Exception {
        OAIndex ann = ExplicitFixture.class.getAnnotation(OATable.class).indexes()[0];

        assertEquals("idx", ann.name());
        assertEquals(1, ann.columns().length);
        assertEquals(true, ann.fkey());
        assertEquals(true, ann.unique());
    }
}
