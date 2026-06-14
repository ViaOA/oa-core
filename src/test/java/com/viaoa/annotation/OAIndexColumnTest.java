package com.viaoa.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.sql.Types;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.viaoa.callback.OAObjectCallback;

@SuppressWarnings("deprecation")
class OAIndexColumnTest {

    @Test
    void metadataHasRuntimeRetentionTargetAndDocumented() {
        assertNotNull(OAIndexColumn.class.getAnnotation(Documented.class));
        assertEquals(RetentionPolicy.RUNTIME, OAIndexColumn.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[0], OAIndexColumn.class.getAnnotation(Target.class).value());
    }

    @OATable(indexes = { @OAIndex(name = "idx", columns = { @OAIndexColumn(name = "Col", lowerName = "col", descend = true) }) })
    private static class ExplicitFixture {}

    @OATable(indexes = { @OAIndex(name = "idx", columns = { @OAIndexColumn(name = "Col") }) })
    private static class DefaultFixture {}

    @Test
    void defaultLowerNameMatchesDeclaration() throws Exception {
        OAIndexColumn ann = DefaultFixture.class.getAnnotation(OATable.class).indexes()[0].columns()[0];

        assertEquals("", ann.lowerName());
    }

    @Test
    void defaultDescendMatchesDeclaration() throws Exception {
        OAIndexColumn ann = DefaultFixture.class.getAnnotation(OATable.class).indexes()[0].columns()[0];

        assertEquals(false, ann.descend());
    }

    @Test
    void nameExplicitValueCanBeRead() throws Exception {
        OAIndexColumn ann = ExplicitFixture.class.getAnnotation(OATable.class).indexes()[0].columns()[0];

        assertEquals("Col", ann.name());
    }

    @Test
    void explicitValuesCanBeReadWithReflection() throws Exception {
        OAIndexColumn ann = ExplicitFixture.class.getAnnotation(OATable.class).indexes()[0].columns()[0];

        assertEquals("Col", ann.name());
        assertEquals("col", ann.lowerName());
        assertEquals(true, ann.descend());
    }
}
