package com.viaoa.graph.service;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Product;
import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.Store;

import com.viaoa.hub.Hub;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.metadata.OAPropertyInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.runtime.OARuntime;

class OAObjectParentServiceTest {
/*qqqqqqqqq
    @BeforeEach
    void beforeEach() {
        OA oa = OARuntime.graph(Register.class);
    }
    @AfterEach
    void afterEach() {
        OAObject.setDebugMode(false);
        OARuntime.graph(Register.class).close();
    }

    private static OAObjectInternalService objectService() {
        OA graph = OARuntime.graph(Register.class);
        return (OAObjectInternalService) graph.objectsInternal();
    }

    @Test
    void parentExposesCoordinatedObjectChildServices() {
        assertNotNull(objects.getOAObjectAnnotationService());
        assertNotNull(objects.getOAObjectAutoAddService());
        assertNotNull(objects.getOAObjectCacheService());
        assertNotNull(objects.getOAObjectChangeService());
        assertNotNull(objects.getOAObjectFindService());
        assertNotNull(objects.getOAObjectGuidService());
        assertNotNull(objects.getOAObjectHubService());
        assertNotNull(objects.getOAObjectInfoService());
        assertNotNull(objects.getOAObjectKeyService());
        assertNotNull(objects.getOAObjectPropertyService());
        assertNotNull(objects.getOAObjectReflectService());
        assertNotNull(objects.getOAObjectSaveService());
        assertNotNull(objects.getOAObjectSiblingService());
    }

    @Test
    void objectInfoServiceResolvesOaposMetadataThroughParent() {
        OAObjectInfo info = objects.callObjectInfoGetOAObjectInfo(Register.class);
        assertNotNull(info);
        assertEquals("Register", info.getName());

        assertSame(info, objects.callObjectInfoGetOAObjectInfo(new Register()));
        assertEquals(String.class, objects.callObjectInfoGetPropertyClass(Register.class, Register.P_Code));
        assertEquals(Register.class, objects.callObjectInfoGetHubPropertyClass(Store.class, Store.P_Registers));

        OAPropertyInfo propertyInfo = objects.callObjectInfoGetPropertyInfo(info, Register.P_Code);
        assertNotNull(propertyInfo);
        assertEquals(Register.P_Code, propertyInfo.getName());

        OALinkInfo storeToRegisters = objects.callObjectInfoGetLinkInfo(Store.class, Store.P_Registers);
        assertNotNull(storeToRegisters);
        assertEquals(Store.P_Registers, storeToRegisters.getName());
        assertEquals(Register.class, storeToRegisters.getToClass());
        assertEquals(OALinkInfo.TYPE_MANY, storeToRegisters.getType());

        OALinkInfo registerToStore = objects.callObjectInfoGetReverseLinkInfo(storeToRegisters);
        assertNotNull(registerToStore);
        assertEquals(Register.P_Store, registerToStore.getName());
        assertEquals(Store.class, registerToStore.getToClass());

        Method getter = objects.callObjectInfoGetMethod(info, "getCode");
        assertNotNull(getter);
        assertEquals("getCode", getter.getName());
    }

    @Test
    void objectCacheServiceAddsFindsVisitsAndRemovesObjectsThroughParent() {
        Register register = new Register();
        register.setCode("R1");

        assertEquals(0, objects.callObjectCacheGetTotal(Register.class));
        assertSame(register, objects.callObjectCacheAdd(register, false, false));
        assertEquals(1, objects.callObjectCacheGetTotal(Register.class));

        assertSame(register, objects.callObjectCacheFind(null, Register.class, r -> "R1".equals(r.getCode()), false, false, 1, null));
        assertSame(register, objects.callObjectCacheGetObject(Register.class, register));

        final int[] visited = new int[1];
        objects.callObjectCacheVisit(Register.class, r -> {
            visited[0]++;
            return true;
        });
        assertEquals(1, visited[0]);

        objects.callObjectCacheRemoveObject(register);
        assertEquals(0, objects.callObjectCacheGetTotal(Register.class));
    }

    @Test
    void objectPropertyGuidKeyAndReflectServicesOperateThroughParent() {
        Register register = new Register();

        objects.callObjectReflectSetProperty(register, Register.P_Code, "A1", null);
        assertEquals("A1", objects.callObjectReflectGetProperty(register, Register.P_Code));

        objects.callObjectPropertySetProperty(register, "TransientValue", 42);
        assertEquals(42, objects.callObjectPropertyGetProperty(register, "TransientValue"));
        assertTrue(objects.callObjectPropertyIsPropertyLoaded(register, "TransientValue"));
        objects.callObjectPropertyRemoveProperty(register, "TransientValue", false);
        assertNull(objects.callObjectPropertyGetProperty(register, "TransientValue"));

        UUID guid = UUID.randomUUID();
        objects.callObjectGuidSetGuid(register, guid);
        assertEquals(guid, objects.callObjectGuidGetGuid(register));

        OAObjectKey objectKey = objects.callObjectKeyCreateObjectKey(register);
        assertNotNull(objectKey);

        Register created = objects.callObjectReflectCreateNewObject(Register.class);
        assertNotNull(created);
        assertEquals(Register.class, created.getClass());

        Product product = new Product();
        product.setSku("SKU-2");
        Product copy = (Product) objects.callObjectReflectCreateCopy(product, null);
        assertNotSame(product, copy);
        assertEquals(product.getSku(), copy.getSku());
    }

    @Test
    void objectFindAndReferenceServicesTraverseOaposLinksThroughParent() {
        Store store = new Store();
        Register register = new Register();
        register.setCode("R2");
        store.getRegisters().add(register);

        Object[] found = objects.callObjectFind(store, Store.P_Registers + "." + Register.P_Code, "R2", true);
        assertNotNull(found);
        assertEquals(1, found.length);
        assertSame(register, found[0]);

        Hub<Register> references = objects.callObjectReflectGetReferenceHub(store, Store.P_Registers, null, false, null);
        assertSame(store.getRegisters(), references);
        assertSame(store, objects.callObjectReflectGetReferenceObject(register, Register.P_Store));
        assertEquals(Store.P_Registers, objects.callObjectReflectGetPropertyPathFromMaster(store, store.getRegisters()));
    }

    @Test
    void objectStateServicesTrackAutoAddChangedLockAndDeleteThroughParent() {
        Register register = new Register();

        objects.callObjectSetAutoAdd(register, true);
        assertTrue(objects.callObjectGetAutoAdd(register));

        objects.callObjectReflectSetProperty(register, Register.P_Code, "CHG", null);
        assertTrue(objects.callObjectChangeGetChanged(register, 0));

        objects.callObjectLockLock(register);
        assertTrue(objects.callObjectLockIsLocked(register));
        objects.callObjectLockUnlock(register);
        assertFalse(objects.callObjectLockIsLocked(register));

        assertDoesNotThrow(() -> {
            objects.callObjectSaveSave(register, 0);
            objects.callObjectDeleteSetDeleted(register, true);
            objects.callObjectDeleteSetDeleted(register, false);
        });
    }
*/
}
