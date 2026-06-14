package com.viaoa.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.sql.Types;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.viaoa.callback.OAObjectCallback;

@SuppressWarnings("deprecation")
class OAObjCallbackTest {

    @Test
    void metadataHasRuntimeRetentionTargetAndDocumented() {
        assertNotNull(OAObjCallback.class.getAnnotation(Documented.class));
        assertEquals(RetentionPolicy.RUNTIME, OAObjCallback.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[] { ElementType.TYPE, ElementType.METHOD }, OAObjCallback.class.getAnnotation(Target.class).value());
    }

    @OAObjCallback(
        enabledProperty = "explicit_enabledProperty",
        enabledValue = false,
        visibleProperty = "explicit_visibleProperty",
        visibleValue = false,
        contextEnabledProperty = "explicit_contextEnabledProperty",
        contextEnabledValue = false,
        contextVisibleProperty = "explicit_contextVisibleProperty",
        contextVisibleValue = false,
        viewDependentProperties = { "one", "two" },
        contextDependentProperties = { "one", "two" },
        supportedTypes = { OAObjectCallback.Type.AllowEnabled }
    )
    private static class ExplicitFixture {}

    @OAObjCallback
    private static class DefaultFixture {}

    @Test
    void defaultEnabledPropertyMatchesDeclaration() throws Exception {
        OAObjCallback ann = DefaultFixture.class.getAnnotation(OAObjCallback.class);

        assertEquals("", ann.enabledProperty());
    }

    @Test
    void defaultEnabledValueMatchesDeclaration() throws Exception {
        OAObjCallback ann = DefaultFixture.class.getAnnotation(OAObjCallback.class);

        assertEquals(true, ann.enabledValue());
    }

    @Test
    void defaultVisiblePropertyMatchesDeclaration() throws Exception {
        OAObjCallback ann = DefaultFixture.class.getAnnotation(OAObjCallback.class);

        assertEquals("", ann.visibleProperty());
    }

    @Test
    void defaultVisibleValueMatchesDeclaration() throws Exception {
        OAObjCallback ann = DefaultFixture.class.getAnnotation(OAObjCallback.class);

        assertEquals(true, ann.visibleValue());
    }

    @Test
    void defaultContextEnabledPropertyMatchesDeclaration() throws Exception {
        OAObjCallback ann = DefaultFixture.class.getAnnotation(OAObjCallback.class);

        assertEquals("", ann.contextEnabledProperty());
    }

    @Test
    void defaultContextEnabledValueMatchesDeclaration() throws Exception {
        OAObjCallback ann = DefaultFixture.class.getAnnotation(OAObjCallback.class);

        assertEquals(true, ann.contextEnabledValue());
    }

    @Test
    void defaultContextVisiblePropertyMatchesDeclaration() throws Exception {
        OAObjCallback ann = DefaultFixture.class.getAnnotation(OAObjCallback.class);

        assertEquals("", ann.contextVisibleProperty());
    }

    @Test
    void defaultContextVisibleValueMatchesDeclaration() throws Exception {
        OAObjCallback ann = DefaultFixture.class.getAnnotation(OAObjCallback.class);

        assertEquals(true, ann.contextVisibleValue());
    }

    @Test
    void defaultViewDependentPropertiesMatchesDeclaration() throws Exception {
        OAObjCallback ann = DefaultFixture.class.getAnnotation(OAObjCallback.class);

        assertArrayEquals(new String[0], ann.viewDependentProperties());
    }

    @Test
    void defaultContextDependentPropertiesMatchesDeclaration() throws Exception {
        OAObjCallback ann = DefaultFixture.class.getAnnotation(OAObjCallback.class);

        assertArrayEquals(new String[0], ann.contextDependentProperties());
    }

    @Test
    void defaultSupportedTypesMatchesDeclaration() throws Exception {
        OAObjCallback ann = DefaultFixture.class.getAnnotation(OAObjCallback.class);

        assertArrayEquals(new OAObjectCallback.Type[0], ann.supportedTypes());
    }

    @Test
    void explicitValuesCanBeReadWithReflection() throws Exception {
        OAObjCallback ann = ExplicitFixture.class.getAnnotation(OAObjCallback.class);

        assertEquals("explicit_enabledProperty", ann.enabledProperty());
        assertEquals(false, ann.enabledValue());
        assertEquals("explicit_visibleProperty", ann.visibleProperty());
        assertEquals(false, ann.visibleValue());
        assertEquals("explicit_contextEnabledProperty", ann.contextEnabledProperty());
        assertEquals(false, ann.contextEnabledValue());
        assertEquals("explicit_contextVisibleProperty", ann.contextVisibleProperty());
        assertEquals(false, ann.contextVisibleValue());
        assertArrayEquals(new String[] { "one", "two" }, ann.viewDependentProperties());
        assertArrayEquals(new String[] { "one", "two" }, ann.contextDependentProperties());
        assertArrayEquals(new OAObjectCallback.Type[] { OAObjectCallback.Type.AllowEnabled }, ann.supportedTypes());
    }
}
