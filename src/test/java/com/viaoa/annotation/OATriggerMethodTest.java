package com.viaoa.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.sql.Types;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.viaoa.callback.OAObjectCallback;

@SuppressWarnings("deprecation")
class OATriggerMethodTest {

    @Test
    void metadataHasRuntimeRetentionTargetAndDocumented() {
        assertNotNull(OATriggerMethod.class.getAnnotation(Documented.class));
        assertEquals(RetentionPolicy.RUNTIME, OATriggerMethod.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[] { ElementType.METHOD }, OATriggerMethod.class.getAnnotation(Target.class).value());
    }

    private static class ExplicitFixture {
        @OATriggerMethod(
        properties = { "one", "two" },
        onlyUseLoadedData = false,
        runOnServer = false,
        runInBackgroundThread = true
    )
        public String value() { return "value"; }
    }

    private static class DefaultFixture {
        @OATriggerMethod
        public String value() { return "value"; }
    }

    @Test
    void defaultPropertiesMatchesDeclaration() throws Exception {
        OATriggerMethod ann = DefaultFixture.class.getMethod("value").getAnnotation(OATriggerMethod.class);

        assertArrayEquals(new String[0], ann.properties());
    }

    @Test
    void defaultOnlyUseLoadedDataMatchesDeclaration() throws Exception {
        OATriggerMethod ann = DefaultFixture.class.getMethod("value").getAnnotation(OATriggerMethod.class);

        assertEquals(true, ann.onlyUseLoadedData());
    }

    @Test
    void defaultRunOnServerMatchesDeclaration() throws Exception {
        OATriggerMethod ann = DefaultFixture.class.getMethod("value").getAnnotation(OATriggerMethod.class);

        assertEquals(true, ann.runOnServer());
    }

    @Test
    void defaultRunInBackgroundThreadMatchesDeclaration() throws Exception {
        OATriggerMethod ann = DefaultFixture.class.getMethod("value").getAnnotation(OATriggerMethod.class);

        assertEquals(false, ann.runInBackgroundThread());
    }

    @Test
    void explicitValuesCanBeReadWithReflection() throws Exception {
        OATriggerMethod ann = ExplicitFixture.class.getMethod("value").getAnnotation(OATriggerMethod.class);

        assertArrayEquals(new String[] { "one", "two" }, ann.properties());
        assertEquals(false, ann.onlyUseLoadedData());
        assertEquals(false, ann.runOnServer());
        assertEquals(true, ann.runInBackgroundThread());
    }
}
