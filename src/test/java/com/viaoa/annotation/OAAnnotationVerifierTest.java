package com.viaoa.annotation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Store;
import com.viaoa.graph.OAGraph;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.runtime.OARuntime;

class OAAnnotationVerifierTest {

    @Test
    void verifyReturnsBooleanForGeneratedOaposMetadata() throws Exception {
        OAAnnotationVerifier verifier = new OAAnnotationVerifier();
        OAObjectInfo oi = objectInfo(Store.class);

        assertDoesNotThrow(() -> verifier.verify(oi));
    }

    @Test
    void compareReturnsTrueForSameRuntimeMetadata() {
        OAAnnotationVerifier verifier = new OAAnnotationVerifier();
        OAObjectInfo oi = objectInfo(Store.class);

        assertTrue(verifier.compare(oi, oi));
    }

    @Test
    void compareThrowsWhenMetadataArgumentIsNullCurrentBehavior() {
        OAAnnotationVerifier verifier = new OAAnnotationVerifier();
        OAObjectInfo oi = objectInfo(Store.class);

        assertThrows(NullPointerException.class, () -> verifier.compare(oi, null));
        assertThrows(NullPointerException.class, () -> verifier.compare(null, oi));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static OAObjectInfo objectInfo(Class clazz) {
        OAGraph og = OARuntime.graph(clazz);
        return og.internal().objects().info().getOAObjectInfo(clazz);
    }
}
