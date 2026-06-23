package com.viaoa.load;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.Store;
import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.hub.Hub;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.path.OAPath;
import com.viaoa.runtime.OARuntime;

class OAPreLoaderTest {

    private static class ExposedPreLoader extends OAPreLoader {
        final Map<Class<?>, List<?>> loaded = new HashMap<>();
        final List<Class<?>> loadCalls = new ArrayList<>();

        ExposedPreLoader(Class<?> classFrom, String propPath) {
            super(classFrom, propPath);
        }

        List<?> loadLinks(OALinkInfo[] linkInfos) {
            return _load(linkInfos);
        }

        void loadOneToMany(OALinkInfo linkInfo, List<?> many) {
            loadOtoM(linkInfo, many);
        }

        void loadManyToMany(OALinkInfo linkInfo) {
            loadMtoM(linkInfo);
        }

        List loadClass(Class clazz, OALinkInfo linkInfo) {
            return load(clazz, linkInfo);
        }

        void loadRecursiveLink(Class clazz, List<?> list, OALinkInfo linkInfo) {
            loadRecursive(clazz, list, linkInfo);
        }

        @Override
        protected List load(Class clazz, OALinkInfo linkInfo) {
            loadCalls.add(clazz);
            List<?> list = loaded.get(clazz);
            return list == null ? new ArrayList<>() : new ArrayList<>(list);
        }
    }

    @BeforeEach
    void beforeEach() {
        OAGraphInternal og = (OAGraphInternal) OARuntime.graph(Register.class);
    }
    @AfterEach
    void afterEach() {
        OAObject.setDebugMode(false);
        OARuntime.graph(Register.class).close();
    }

    @Test
    void constructorAndLoadReturnNullWhenRootClassMissing() {
        OAPreLoader loader = new OAPreLoader(null, Store.P_Registers);

        assertNull(loader.load());
    }

    @Test
    void loadDelegatesThroughConfiguredPathAndReturnsLoadedRoots() {
        Store store = new Store(1);
        Register register = new Register(2);
        register.setStore(store);
        ExposedPreLoader loader = new ExposedPreLoader(Store.class, Store.P_Registers);
        loader.loaded.put(Store.class, List.of(store));
        loader.loaded.put(Register.class, List.of(register));

        List<?> roots = loader.load();

        assertEquals(List.of(store), roots);
        assertEquals(List.of(Store.class, Register.class), loader.loadCalls);
        assertSame(register, store.getRegisters().getAt(0));
    }

    @Test
    void protectedLoadProcessesManyLinksAndHydratesOneToManyRelationship() {
        Store store = new Store(1);
        Register register = new Register(2);
        register.setStore(store);
        ExposedPreLoader loader = new ExposedPreLoader(Store.class, Store.P_Registers);
        loader.loaded.put(Store.class, List.of(store));
        loader.loaded.put(Register.class, List.of(register));

        List<?> roots = loader.loadLinks(linkInfos(Store.class, Store.P_Registers));

        assertEquals(List.of(store), roots);
        assertSame(register, store.getRegisters().getAt(0));
    }

    @Test
    void loadOtoMIgnoresInvalidMetadataAndHydratesValidReverseOneLink() {
        Store invalidStore = new Store(10);
        Register invalidRegister = new Register(11);
        ExposedPreLoader loader = new ExposedPreLoader(Store.class, Store.P_Registers);

        loader.loadOneToMany(null, List.of(invalidRegister));
        loader.loadOneToMany(linkInfo(Store.class, Store.P_StoreSafe), List.of(invalidRegister));
        assertEquals(0, invalidStore.getRegisters().size());

        Store store = new Store(1);
        Register register = new Register(2);
        register.setStore(store);
        loader.loadOneToMany(linkInfo(Store.class, Store.P_Registers), List.of(register));

        assertTrue(store.getRegisters().contains(register));
    }

    @Test
    void loadMtoMIsNoOpForNullOrNonManyToManyLinks() {
        ExposedPreLoader loader = new ExposedPreLoader(Store.class, Store.P_Registers);

        assertDoesNotThrow(() -> loader.loadManyToMany(null));
        assertDoesNotThrow(() -> loader.loadManyToMany(linkInfo(Store.class, Store.P_Registers)));
    }

    @Test
    void loadClassOverrideReturnsConfiguredObjectsForDeterministicFirstPass() {
        Store store = new Store(1);
        ExposedPreLoader loader = new ExposedPreLoader(Store.class, Store.P_Registers);
        loader.loaded.put(Store.class, List.of(store));

        List<?> list = loader.loadClass(Store.class, null);

        assertEquals(List.of(store), list);
        assertEquals(List.of(Store.class), loader.loadCalls);
    }

    @Test
    void loadRecursiveIgnoresMissingReverseMetadataAndHydratesWhenReverseExists() {
        Store invalidStore = new Store(10);
        Register invalidRegister = new Register(11);
        ExposedPreLoader loader = new ExposedPreLoader(Store.class, Store.P_Registers);

        loader.loadRecursiveLink(Register.class, List.of(invalidRegister), null);
        assertEquals(0, invalidStore.getRegisters().size());

        Store store = new Store(1);
        Register register = new Register(2);
        register.setStore(store);
        loader.loadRecursiveLink(Register.class, List.of(register), linkInfo(Store.class, Store.P_Registers));

        assertTrue(store.getRegisters().contains(register));
    }

    private static OALinkInfo linkInfo(Class<?> rootClass, String path) {
        return linkInfos(rootClass, path)[0];
    }

    private static OALinkInfo[] linkInfos(Class<?> rootClass, String path) {
        return new OAPath(rootClass, path).getLinkInfos();
    }
}
