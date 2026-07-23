package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Catalog;
import com.test.pos.model.oa.CatalogCategory;
import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.Store;
import com.test.pos.model.oa.Till;
import com.viaoa.oa.OA;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

class HubTest {
    @BeforeEach
    void beforeEach() {
        OA oa = OARuntime.createDefaultOA(Register.class);
    }

    @AfterEach
    void afterEach() {
        OAObject.setDebugMode(false);
        OARuntime.defaultOA().close();
    }

    private static Register register(int id, String code) {
        Register register = new Register();
        register.setId(id);
        register.setCode(code);
        return register;
    }

    private static Store store(int id, int number, String name) {
        Store store = new Store();
        store.setId(id);
        store.setStoreNumber(number);
        store.setName(name);
        return store;
    }

    private static CatalogCategory catalogCategory(int id, String name) {
        CatalogCategory catalogCategory = new CatalogCategory();
        catalogCategory.setId(id);
        catalogCategory.setName(name);
        return catalogCategory;
    }

    private static MasterDetailScenario createMasterDetailScenario() {
        Store store = store(1, 101, "store1");
        Store store2 = store(2, 202, "store2");
        Register register = register(11, "R1");
        Register register2 = register(12, "R2");
        Register register3 = register(13, "R3");
        store.getRegisters().add(register);
        store.getRegisters().add(register2);
        store2.getRegisters().add(register3);

        Hub<Store> hubMasterStore = new Hub<>(Store.class);
        hubMasterStore.add(store);
        hubMasterStore.add(store2);
        hubMasterStore.setAO(store);
        @SuppressWarnings("unchecked")
        Hub<Register> hubDetailRegister = hubMasterStore.getDetailHub(Store.P_Registers);
        return new MasterDetailScenario(hubMasterStore, hubDetailRegister, store, store2, register, register2, register3);
    }

    private static final class MasterDetailScenario {
        final Hub<Store> hubMasterStore;
        final Hub<Register> hubDetailRegister;
        final Store store;
        final Store store2;
        final Register register;
        final Register register2;
        final Register register3;

        MasterDetailScenario(Hub<Store> hubMasterStore, Hub<Register> hubDetailRegister, Store store, Store store2, Register register, Register register2, Register register3) {
            this.hubMasterStore = hubMasterStore;
            this.hubDetailRegister = hubDetailRegister;
            this.store = store;
            this.store2 = store2;
            this.register = register;
            this.register2 = register2;
            this.register3 = register3;
        }
    }

    private static RecursiveCatalogCategoryScenario createRecursiveCatalogCategoryScenario() {
        Hub<Catalog> hubCatalog = new Hub<>(Catalog.class);
    	Catalog catalog = new Catalog();
    	hubCatalog.add(catalog);
    	
        CatalogCategory catalogCategory = catalogCategory(1, "Root");
        catalog.getCatalogCategories().add(catalogCategory);
        
        CatalogCategory catalogCategory2 = catalogCategory(2, "Child");
        catalogCategory2.setParentCatalogCategory(catalogCategory);

        CatalogCategory catalogCategory3 = catalogCategory(3, "Grandchild");
        catalogCategory3.setParentCatalogCategory(catalogCategory2);

        Hub<CatalogCategory> hubCatalogCategory = hubCatalog.getDetailHub(Catalog.P_CatalogCategories);
        hubCatalogCategory.setAO(catalogCategory);

        return new RecursiveCatalogCategoryScenario(
            hubCatalog,
            hubCatalogCategory,
            catalogCategory,
            catalogCategory2,
            catalogCategory3
        );
    }

    private static final class RecursiveCatalogCategoryScenario {
        final Hub<Catalog> hubCatalog;
        final Hub<CatalogCategory> hubCatalogCategory;
        final CatalogCategory catalogCategory;
        final CatalogCategory catalogCategory2;
        final CatalogCategory catalogCategory3;

        RecursiveCatalogCategoryScenario(
            Hub<Catalog> hubCatalog,
            Hub<CatalogCategory> hubCatalogCategory,
            CatalogCategory catalogCategory,
            CatalogCategory catalogCategory2,
            CatalogCategory catalogCategory3
        ) {
            this.hubCatalog = hubCatalog;
            this.hubCatalogCategory = hubCatalogCategory;
            this.catalogCategory = catalogCategory;
            this.catalogCategory2 = catalogCategory2;
            this.catalogCategory3 = catalogCategory3;
        }
    }

    // ====================================================================
    // constructors
    // ====================================================================

    @Test
    @DisplayName("A default Hub starts empty without an object class")
    void constructorTest() {
        Hub<Register> hubRegister = new Hub<>();

        assertNull(hubRegister.getObjectClass());
        assertTrue(hubRegister.isEmpty());
    }

    @Test
    @DisplayName("Constructing a Hub with one object adds it and makes it active")
    void constructor_withObjectTest() {
        Register register = register(1, "R1");

        Hub<Register> hubRegister = new Hub<>(register);

        assertEquals(Register.class, hubRegister.getObjectClass());
        assertEquals(1, hubRegister.size());
        assertSame(register, hubRegister.getAt(0));
        assertSame(register, hubRegister.getAO());
    }

    @Test
    @DisplayName("Constructing a typed Hub records the object class and starts empty")
    void constructor_withObjectClassTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        assertEquals(Register.class, hubRegister.getObjectClass());
        assertTrue(hubRegister.isEmpty());
    }

    @Test
    @DisplayName("Constructing a sized Hub records the object class and remains empty")
    void constructor_withCapacityTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class, 2, 1);

        assertEquals(Register.class, hubRegister.getObjectClass());
        assertTrue(hubRegister.isEmpty());
    }

    @Test
    @DisplayName("Constructing a Hub from another Hub creates shared membership")
    void constructor_withSharedHubTest() {
        Hub<Register> hubSourceRegister = new Hub<>(Register.class);
        Register register = register(1, "R1");
        hubSourceRegister.add(register);

        Hub<Register> hubSharedRegister = new Hub<>(hubSourceRegister);

        assertSame(hubSourceRegister, hubSharedRegister.getSharedHub());
        assertEquals(hubSourceRegister.toList(), hubSharedRegister.toList());
    }

    @Test
    @DisplayName("Constructing a Hub with a master object creates a master-owned Hub")
    void constructor_withMasterObjectTest() {
        Store store = store(1, 101, "store1");

        Hub<Register> hubRegister = new Hub<>(Register.class, store);

        assertEquals(Register.class, hubRegister.getObjectClass());
        assertSame(store, hubRegister.getMasterObject());
    }

    // ====================================================================
    // capacity, dynamic properties, and simple accessors
    // ====================================================================

    @Test
    @DisplayName("ensureCapacity accepts a larger capacity without changing membership")
    void ensureCapacityTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "R1");
        hubRegister.add(register);

        hubRegister.ensureCapacity(8);

        assertEquals(List.of(register), hubRegister.toList());
    }

    @Test
    @DisplayName("resizeToFit keeps membership unchanged")
    void resizeToFitTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "R1");
        hubRegister.add(register);

        hubRegister.resizeToFit();

        assertEquals(List.of(register), hubRegister.toList());
    }

    @Test
    @DisplayName("setProperty stores a dynamic Hub property")
    void setPropertyTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        hubRegister.setProperty("label", "registers");

        assertEquals("registers", hubRegister.getProperty("label"));
    }

    @Test
    @DisplayName("getProperty returns null after a dynamic property is removed")
    void getPropertyTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        hubRegister.setProperty("label", "registers");
        hubRegister.removeProperty("label");

        assertNull(hubRegister.getProperty("label"));
    }

    @Test
    @DisplayName("removeProperty clears only the named dynamic property")
    void removePropertyTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        hubRegister.setProperty("a", "A");
        hubRegister.setProperty("b", "B");

        hubRegister.removeProperty("a");

        assertNull(hubRegister.getProperty("a"));
        assertEquals("B", hubRegister.getProperty("b"));
    }

    @Test
    @DisplayName("toString identifies the Hub object type")
    void toStringTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        assertTrue(hubRegister.toString().contains("Register"));
    }

    @Test
    @DisplayName("setRefresh stores the refresh flag")
    void setRefreshTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        hubRegister.setRefresh(true);

        assertTrue(hubRegister.getRefresh());
    }

    @Test
    @DisplayName("getRefresh reports the current refresh flag")
    void getRefreshTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        assertFalse(hubRegister.getRefresh());
    }

    @Test
    @DisplayName("setChanged controls the Hub changed flag")
    void setChangedTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        hubRegister.setChanged(false);

        assertFalse(hubRegister.getChanged(OAObject.CASCADE_NONE));
    }

    @Test
    @DisplayName("getChanged reports explicit Hub changed state")
    void getChangedTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        hubRegister.setChanged(false);

        assertFalse(hubRegister.getChanged(OAObject.CASCADE_NONE));
    }

    // ====================================================================
    // arrays, lists, size, and object lookup
    // ====================================================================

    @Test
    @DisplayName("copyInto copies Hub contents into the supplied array")
    void copyIntoTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register);
        hubRegister.add(register2);
        Register[] copy = new Register[2];

        hubRegister.copyInto(copy);

        assertArrayEquals(new Register[] { register, register2 }, copy);
    }

    @Test
    @DisplayName("toArray returns Hub contents in current order")
    void toArrayTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register);
        hubRegister.add(register2);

        assertArrayEquals(new Object[] { register, register2 }, hubRegister.toArray());
    }

    @Test
    @DisplayName("toArray with a typed array returns typed Hub contents")
    void toArray_withTypedArrayTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register);
        hubRegister.add(register2);

        assertArrayEquals(new Register[] { register, register2 }, hubRegister.toArray(new Register[0]));
    }

    @Test
    @DisplayName("toList returns Hub contents in current order")
    void toListTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register);
        hubRegister.add(register2);

        assertEquals(List.of(register, register2), hubRegister.toList());
    }

    @Test
    @DisplayName("copyInto copies Hub contents into another Hub")
    void copyInto_withHubTest() {
        Hub<Register> hubSourceRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubSourceRegister.add(register);
        Hub<Register> hubTargetRegister = new Hub<>(Register.class);

        hubSourceRegister.copyInto(hubTargetRegister);

        assertEquals(List.of(register), hubTargetRegister.toList());
    }

    @Test
    @DisplayName("getObjectClass returns the configured Hub element type")
    void getObjectClassTest() {
        assertEquals(Register.class, new Hub<>(Register.class).getObjectClass());
    }

    @Test
    @DisplayName("loadAllData on an in-memory Hub leaves current membership available")
    void loadAllDataTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);

        hubRegister.loadAllData();

        assertEquals(List.of(register), hubRegister.toList());
    }

    @Test
    @Disabled("TODO - deterministic datasource-backed unloaded relationship fixture required")
    @DisplayName("TODO: loading an unloaded relationship Hub materializes reference contents")
    void loadAllData_withUnloadedRelationshipHubTest() {
        // Matrix classification: REQUIRED
        // Evidence level: STRONGLY_INFERRED
        // Required fixture: datasource-backed OAPOS relationship that starts unloaded.
    }

    @Test
    @DisplayName("getCurrentSize reports the number of currently loaded objects")
    void getCurrentSizeTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        hubRegister.add(register(1, "A"));

        assertEquals(1, hubRegister.getCurrentSize());
    }

    @Test
    @DisplayName("getSize reports Hub membership count")
    void getSizeTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        hubRegister.add(register(1, "A"));

        assertEquals(1, hubRegister.getSize());
    }

    @Test
    @DisplayName("size reports Hub membership count")
    void sizeTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        hubRegister.add(register(1, "A"));

        assertEquals(1, hubRegister.size());
    }

    @Test
    @DisplayName("getLoadedSize reports currently loaded membership count")
    void getLoadedSizeTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        hubRegister.add(register(1, "A"));

        assertEquals(1, hubRegister.getLoadedSize());
    }

    @Test
    @Disabled("TODO - save behavior requires deterministic datasource fixture")
    @DisplayName("TODO: saveAll saves Hub objects according to cascade rules")
    void saveAllTest() {
        // Hub.saveAll()
    }

    @Test
    @Disabled("TODO - save behavior requires deterministic datasource fixture")
    @DisplayName("TODO: saveAll with cascade rule saves Hub objects according to the supplied rule")
    void saveAll_withCascadeRuleTest() {
        // Hub.saveAll(int iCascadeRule)
    }

    @Test
    @Disabled("TODO - delete behavior requires verified ownership/cascade fixture")
    @DisplayName("TODO: deleteAll applies delete behavior to all Hub objects")
    void deleteAllTest() {
        // Hub.deleteAll()
    }

    @Test
    @DisplayName("isDeletingAll is false outside a deleteAll operation")
    void isDeletingAllTest() {
        assertFalse(new Hub<>(Register.class).isDeletingAll());
    }

    @Test
    @DisplayName("clone creates a distinct Hub with the same membership")
    void cloneTest() throws CloneNotSupportedException {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);

        @SuppressWarnings("unchecked")
        Hub<Register> hubCloneRegister = (Hub<Register>) hubRegister.clone();

        assertNotSame(hubRegister, hubCloneRegister);
        assertEquals(hubRegister.toList(), hubCloneRegister.toList());
    }

    @Test
    @DisplayName("compareTo orders Hub instances without treating unequal Hubs as equal")
    void compareToTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Hub<Register> hubRegister2 = new Hub<>(Register.class);

        assertNotEquals(0, hubRegister.compareTo(hubRegister2));
        assertEquals(0, hubRegister.compareTo(hubRegister));
    }

    @Test
    @DisplayName("getObject resolves an object by object key")
    void getObjectTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);

        assertSame(register, hubRegister.getObject(register.getObjectKey()));
    }

    @Test
    @DisplayName("getObjectAt returns the object at the requested position")
    void getObjectAtTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);

        assertSame(register, hubRegister.getObjectAt(0));
    }

    @Test
    @DisplayName("getAt returns null for an out-of-range position")
    void getAtTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        assertNull(hubRegister.getAt(0));
    }

    @Test
    @DisplayName("getLast returns the final object in Hub order")
    void getLastTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register);
        hubRegister.add(register2);

        assertSame(register2, hubRegister.getLast());
    }

    @Test
    @DisplayName("contains reports whether an object is a member of the Hub")
    void containsTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);

        assertTrue(hubRegister.contains(register));
        assertFalse(hubRegister.contains(register(2, "B")));
    }

    @Test
    @DisplayName("indexOf returns the current Hub position for a member")
    void indexOfTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register);
        hubRegister.add(register2);

        assertEquals(1, hubRegister.indexOf(register2));
    }

    @Test
    @DisplayName("elementAt returns the object at the requested position")
    void elementAtTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);

        assertSame(register, hubRegister.elementAt(0));
    }

    // ====================================================================
    // active object and position
    //
    // Canonical API coverage:
    //   Hub.setAO(...)
    //   Hub.setActiveObject(...) delegates to the same active-object implementation
    //   and is intentionally covered through setAO* and setPos* tests.
    //
    // Wiring: StandaloneHub, MasterHub, DetailHub, SharedHub,
    //   SharedHubAndDetailHub, LinkHub, LinkHubAndDetailHub
    // State: HubWithoutAO, HubAOOutsideHub
    // Conditions: Null, SameActiveObject, ObjectNotInHub, RecursiveLinkUpdate
    // Supporting services: HubAOService.setActiveObject(...),
    //   HubDetailService.updateAllDetail(...), HubLinkService.updateLinkedFromHub(...)
    // ====================================================================

    @Test
    @DisplayName("getActiveObject returns the current active object")
    void getActiveObjectTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);

        hubRegister.setAO(register);

        assertSame(register, hubRegister.getActiveObject());
    }

    @Test
    @DisplayName("getAO returns the current active object")
    void getAOTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);

        hubRegister.setAO(register);

        assertSame(register, hubRegister.getAO());
    }


    @Test
    @DisplayName("setAO selects the requested object in a standalone Hub")
    void setAOTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register);
        hubRegister.add(register2);

        hubRegister.setAO(register2);

        assertSame(register2, hubRegister.getAO());
        assertEquals(1, hubRegister.getPos());
    }

    @Test
    @DisplayName("Setting AO to null clears the active object")
    void setAO_withNullTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);
        hubRegister.setAO(register);

        hubRegister.setAO((Register) null);

        assertNull(hubRegister.getAO());
        assertEquals(-1, hubRegister.getPos());
    }

    @Test
    @DisplayName("Setting the same AO again leaves the active object unchanged")
    void setAO_withSameActiveObjectTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);
        hubRegister.setAO(register);

        hubRegister.setAO(register);

        assertSame(register, hubRegister.getAO());
        assertEquals(0, hubRegister.getPos());
    }

    @Test
    @DisplayName("Setting AO to an object outside a standalone Hub clears selection")
    void setAO_withObjectNotInHubTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register registerOutside = register(1, "A");

        hubRegister.setAO(registerOutside);

        assertNull(hubRegister.getAO());
        assertEquals(-1, hubRegister.getPos());
    }

    @Test
    @DisplayName("Setting master AO replaces detail Hub contents with the selected master's children")
    void setAO_withMasterHubTest() {
        MasterDetailScenario scenario = createMasterDetailScenario();

        scenario.hubMasterStore.setAO(scenario.store2);

        assertEquals(List.of(scenario.register3), scenario.hubDetailRegister.toList());
        assertSame(scenario.store2, scenario.hubDetailRegister.getMasterHub().getAO());
        
        assertNull(scenario.hubDetailRegister.getAO());
    }

    @Test
    @DisplayName("Setting a detail AO realigns the master Hub to the detail owner")
    void setAO_withDetailHubTest() {
        MasterDetailScenario scenario = createMasterDetailScenario();

        scenario.hubDetailRegister.setAO(scenario.register3);

        assertSame(scenario.store2, scenario.hubMasterStore.getAO());
        assertSame(scenario.register3, scenario.hubDetailRegister.getAO());
        assertEquals(List.of(scenario.register3), scenario.hubDetailRegister.toList());
    }

    @Test
    @DisplayName("Setting recursive detail AO realigns the recursive master AO to the parent")
    void setAO_withRecursiveDetailHubTest() {
        RecursiveCatalogCategoryScenario scenario = createRecursiveCatalogCategoryScenario();

        scenario.hubCatalogCategory.setAO(scenario.catalogCategory3);

        assertSame(scenario.catalogCategory2, scenario.hubCatalogCategory.getAO());
        assertSame(scenario.catalogCategory3, scenario.hubCatalogCategory.getAO());
        assertEquals(List.of(scenario.catalogCategory3), scenario.hubCatalogCategory.toList());
        assertSame(scenario.catalogCategory2, scenario.catalogCategory3.getParentCatalogCategory());
        assertTrue(scenario.catalogCategory2.getCatalogCategories().contains(scenario.catalogCategory3));
    }

    @Test
    @DisplayName("Setting AO in a shared Hub updates shared active data when active sharing is enabled")
    void setAO_withSharedHubTest() {
        Hub<Register> hubSourceRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubSourceRegister.add(register);
        hubSourceRegister.add(register2);
        Hub<Register> hubSharedRegister = hubSourceRegister.createSharedHub(true);
        Hub<Register> hubSharedRegister2 = hubSourceRegister.createSharedHub(false);

        hubSourceRegister.setAO(register2);

        assertSame(register2, hubSharedRegister.getAO());
        assertEquals(hubSourceRegister.toList(), hubSharedRegister.toList());
        assertNull(hubSharedRegister2.getAO());
    }

    @Test
    @DisplayName("Shared master AO changes update dependent detail Hub contents")
    void setAO_withSharedHubAndDetailHubTest() {
        MasterDetailScenario scenario = createMasterDetailScenario();
        Hub<Store> hubSharedStore = scenario.hubMasterStore.createSharedHub(true);
        @SuppressWarnings("unchecked")
        Hub<Register> hubSharedDetailRegister = hubSharedStore.getDetailHub(Store.P_Registers);

        scenario.hubMasterStore.setAO(scenario.store2);

        assertSame(scenario.store2, hubSharedStore.getAO());
        assertEquals(List.of(scenario.register3), hubSharedDetailRegister.toList());
    }

    @Test
    @DisplayName("Setting link-from AO updates the configured link-to object reference")
    void setAO_withLinkHubTest() {
        Hub<Store> hubStore = new Hub<>(Store.class);
        Store store = store(1, 101, "store1");
        Store store2 = store(2, 202, "store2");
        hubStore.add(store);
        hubStore.add(store2);
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register registerSelector = register(10, "selector");
        hubRegister.add(registerSelector);
        hubRegister.setAO(registerSelector);
        hubStore.setLinkHub(hubRegister, Register.P_Store);

        hubStore.setAO(store2);

        assertSame(store2, registerSelector.getStore());
    }

    @Test
    @DisplayName("A linked detail selection realigns the master Hub before assigning the detail active object")
    void setAO_withLinkHubAndDetailHubTest() {
        MasterDetailScenario scenario = createMasterDetailScenario();
        Hub<Till> hubTill = new Hub<>(Till.class);
        Till tillSelector = new Till();
        tillSelector.setRegister(scenario.register);
        Till tillSelector2 = new Till();
        tillSelector2.setRegister(scenario.register3);
        hubTill.add(tillSelector);
        hubTill.add(tillSelector2);
        hubTill.setAO(tillSelector);
        scenario.hubDetailRegister.setLinkHub(hubTill, Till.P_Register);

        assertSame(scenario.store, scenario.hubMasterStore.getAO());
        assertSame(scenario.register, scenario.hubDetailRegister.getAO());
        
        hubTill.setAO(tillSelector2);

        assertSame(scenario.store2, scenario.hubMasterStore.getAO());
        assertSame(scenario.register3, scenario.hubDetailRegister.getAO());
    }

    @Test
    @Disabled("TODO - exact recursive linked-Hub AO realignment contract requires confirmation with CatalogCategory")
    @DisplayName("TODO: define recursive linked-Hub AO realignment behavior")
    void setAO_withLinkHubAndRecursiveLinkUpdateTest() {
        RecursiveCatalogCategoryScenario scenario = createRecursiveCatalogCategoryScenario();
/*qqqqq        
        scenario.hubCatalogCategory.setLinkHub(scenario.hubLinkCatalogCategory, CatalogCategory.P_ParentCatalogCategory);

        scenario.hubLinkCatalogCategory.setAO(scenario.catalogCategory3);

        assertSame(scenario.catalogCategory2, scenario.hubDetailCatalogCategory.getAO());
        assertSame(scenario.catalogCategory, scenario.hubMasterCatalogCategory.getAO());
        assertEquals(List.of(scenario.catalogCategory2), scenario.hubDetailCatalogCategory.toList());
        assertSame(scenario.catalogCategory, scenario.catalogCategory2.getParentCatalogCategory());
        assertTrue(scenario.catalogCategory.getCatalogCategories().contains(scenario.catalogCategory2));
        assertSame(scenario.catalogCategory2, scenario.catalogCategory3.getParentCatalogCategory());
        assertTrue(scenario.catalogCategory2.getCatalogCategories().contains(scenario.catalogCategory3));
*/        
    }

    @Test
    @DisplayName("setPos selects the object at the requested position")
    void setPosTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register);
        hubRegister.add(register2);

        Register registerSelected = hubRegister.setPos(1);

        assertSame(register2, registerSelected);
        assertSame(register2, hubRegister.getAO());
    }

    @Test
    @DisplayName("Setting position in a linked Hub updates the configured link-to object reference")
    void setPos_withLinkHubTest() {
        Hub<Store> hubStore = new Hub<>(Store.class);
        Store store = store(1, 101, "store1");
        Store store2 = store(2, 202, "store2");
        hubStore.add(store);
        hubStore.add(store2);
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register registerSelector = register(10, "selector");
        hubRegister.add(registerSelector);
        hubRegister.setAO(registerSelector);
        hubStore.setLinkHub(hubRegister, Register.P_Store);

        hubStore.setPos(1);

        assertSame(store2, registerSelector.getStore());
    }

    @Test
    @DisplayName("getPos returns the active object position")
    void getPosTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);
        hubRegister.setAO(register);

        assertEquals(0, hubRegister.getPos());
    }

    @Test
    @DisplayName("getPos for an object returns that object's current position")
    void getPos_withObjectTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register);
        hubRegister.add(register2);

        assertEquals(1, hubRegister.getPos(register2));
    }

    @Test
    @DisplayName("getPos with adjust-master can find a detail object by realigning the master Hub")
    void getPos_withObjectAndAdjustMasterTest() {
        MasterDetailScenario scenario = createMasterDetailScenario();

        int pos = scenario.hubDetailRegister.getPos(scenario.register3, true);

        assertEquals(0, pos);
        assertSame(scenario.store2, scenario.hubMasterStore.getAO());
    }

    @Test
    @DisplayName("resetAO leaves a cleared active object unchanged when no default position is active")
    void resetAOTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);
        hubRegister.setAO((Register) null);

        hubRegister.resetAO();

        assertNull(hubRegister.getAO());
    }

    // ====================================================================
    // root, add Hub, ownership, default position
    // ====================================================================

    @Test
    @Disabled("TODO - root-Hub public contract for plain Hubs requires confirmation")
    @DisplayName("TODO: define setRootHub behavior for a standalone Hub")
    void setRootHubTest() {
        // Hub.setRootHub()
    }

    @Test
    @Disabled("TODO - root-Hub public contract for plain Hubs requires confirmation")
    @DisplayName("TODO: define getRootHub behavior for a standalone Hub")
    void getRootHubTest() {
        // Hub.getRootHub()
    }

    @Test
    @DisplayName("setAddHub stores the Hub used for automatic additions")
    void setAddHubTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Hub<Register> hubAddRegister = new Hub<>(Register.class);

        hubRegister.setAddHub(hubAddRegister);

        assertSame(hubAddRegister, hubRegister.getAddHub());
    }

    @Test
    @DisplayName("getAddHub returns the configured automatic-add Hub")
    void getAddHubTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Hub<Register> hubAddRegister = new Hub<>(Register.class);
        hubRegister.setAddHub(hubAddRegister);

        assertSame(hubAddRegister, hubRegister.getAddHub());
    }

    @Test
    @DisplayName("getRealHub returns the backing Hub for a normal Hub")
    void getRealHubTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        assertSame(hubRegister, hubRegister.getRealHub());
    }

    @Test
    @DisplayName("A generated owned relationship Hub reports as owned")
    void isOwnedTest() {
        Store store = store(1, 101, "store1");

        assertTrue(store.getRegisters().isOwned());
    }

    @Test
    @DisplayName("setUniqueProperty rejects duplicate property values")
    void setUniquePropertyTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        hubRegister.setUniqueProperty(Register.P_Code);
        Register register = register(1, "A");
        Register register2 = register(2, "A");

        assertTrue(hubRegister.add(register));
        assertThrows(RuntimeException.class, () -> hubRegister.add(register2));
        assertEquals(List.of(register), hubRegister.toList());
    }

    @Test
    @DisplayName("setDefaultPos stores the default active position")
    void setDefaultPosTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        hubRegister.setDefaultPos(2);

        assertEquals(2, hubRegister.getDefaultPos());
    }

    @Test
    @DisplayName("getDefaultPos returns the configured default active position")
    void getDefaultPosTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        hubRegister.setDefaultPos(1);

        assertEquals(1, hubRegister.getDefaultPos());
    }

    // ====================================================================
    // membership mutation
    // Wiring: StandaloneHub, DetailHub, SharedHub
    // Conditions: DuplicateAdd, RepeatedRemove, ActiveObject, EmptyHub
    // Supporting service: HubAddRemoveService
    // ====================================================================

    @Test
    @DisplayName("Adding an object inserts it into a standalone Hub")
    void addTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");

        assertTrue(hubRegister.add(register));

        assertEquals(List.of(register), hubRegister.toList());
    }

    @Test
    @DisplayName("Adding a duplicate object preserves one Hub membership")
    void add_withDuplicateAddTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");

        assertTrue(hubRegister.add(register));
        assertFalse(hubRegister.add(register));

        assertEquals(1, hubRegister.size());
    }

    @Test
    @DisplayName("Adding to a detail Hub assigns the reverse one-link to the current master")
    void add_withDetailHubTest() {
        MasterDetailScenario scenario = createMasterDetailScenario();
        Register registerAdded = register(20, "R20");

        scenario.hubDetailRegister.add(registerAdded);

        assertSame(scenario.store, registerAdded.getStore());
        assertTrue(scenario.store.getRegisters().contains(registerAdded));
    }

    @Test
    @DisplayName("Adding through a shared Hub updates shared membership")
    void add_withSharedHubTest() {
        Hub<Register> hubSourceRegister = new Hub<>(Register.class);
        Hub<Register> hubSharedRegister = hubSourceRegister.createSharedHub(false);
        Register register = register(1, "A");

        hubSharedRegister.add(register);

        assertTrue(hubSourceRegister.contains(register));
        assertTrue(hubSharedRegister.contains(register));
    }

    @Test
    @DisplayName("Adding a list appends each object in list order")
    void addListTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");

        hubRegister.add(List.of(register, register2));

        assertEquals(List.of(register, register2), hubRegister.toList());
    }

    @Test
    @DisplayName("Adding another Hub appends its members in Hub order")
    void addHubTest() {
        Hub<Register> hubSourceRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubSourceRegister.add(register);
        Hub<Register> hubTargetRegister = new Hub<>(Register.class);

        hubTargetRegister.add(hubSourceRegister);

        assertEquals(List.of(register), hubTargetRegister.toList());
    }

    @Test
    @DisplayName("setEnabled controls whether this Hub accepts mutating operations")
    void setEnabledTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        hubRegister.setEnabled(false);

        assertFalse(hubRegister.getEnabled());
    }

    @Test
    @DisplayName("getEnabled reports the current Hub enabled state")
    void getEnabledTest() {
        assertTrue(new Hub<>(Register.class).getEnabled());
    }

    @Test
    @DisplayName("addElement appends an object to the Hub")
    void addElementTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");

        hubRegister.addElement(register);

        assertEquals(List.of(register), hubRegister.toList());
    }

    @Test
    @DisplayName("swap exchanges objects at two positions")
    void swapTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register);
        hubRegister.add(register2);

        hubRegister.swap(0, 1);

        assertEquals(List.of(register2, register), hubRegister.toList());
    }

    @Test
    @DisplayName("move relocates an object without changing membership")
    void moveTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        Register register3 = register(3, "C");
        hubRegister.add(register);
        hubRegister.add(register2);
        hubRegister.add(register3);

        hubRegister.move(0, 2);

        assertEquals(List.of(register2, register3, register), hubRegister.toList());
    }

    @Test
    @DisplayName("insert places the object at the requested position")
    void insertTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register2);

        assertTrue(hubRegister.insert(register, 0));

        assertEquals(List.of(register, register2), hubRegister.toList());
    }

    @Test
    @DisplayName("Inserting into a detail Hub assigns the reverse one-link to the current master")
    void insert_withDetailHubTest() {
        MasterDetailScenario scenario = createMasterDetailScenario();
        Register registerAdded = register(20, "R20");

        scenario.hubDetailRegister.insert(registerAdded, 0);

        assertSame(scenario.store, registerAdded.getStore());
        assertSame(registerAdded, scenario.hubDetailRegister.getAt(0));
    }

    @Test
    @DisplayName("remove by position removes and returns the object at that position")
    void remove_withPositionTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);

        Register registerRemoved = hubRegister.remove(0);

        assertSame(register, registerRemoved);
        assertTrue(hubRegister.isEmpty());
    }

    @Test
    @DisplayName("remove deletes only Hub membership for a standalone Hub")
    void removeTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);

        assertTrue(hubRegister.remove((Object) register));

        assertFalse(hubRegister.contains(register));
        assertFalse(register.isDeleted());
    }

    @Test
    @DisplayName("Removing from a detail Hub clears the reverse one-link for the current master")
    void remove_withDetailHubTest() {
        MasterDetailScenario scenario = createMasterDetailScenario();

        assertTrue(scenario.hubDetailRegister.remove(scenario.register));

        assertNull(scenario.register.getStore());
        assertFalse(scenario.store.getRegisters().contains(scenario.register));
    }

    @Test
    @DisplayName("Removing the active object clears or moves active selection away from the removed object")
    void remove_withActiveObjectTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register);
        hubRegister.add(register2);
        hubRegister.setAO(register);

        hubRegister.remove(register);

        assertFalse(hubRegister.contains(register));
        assertNotSame(register, hubRegister.getAO());
    }

    @Test
    @DisplayName("Removing an absent object is a no-op")
    void remove_withRepeatedRemoveTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");

        assertFalse(hubRegister.remove(register));
        assertTrue(hubRegister.isEmpty());
    }

    @Test
    @DisplayName("removeAt removes the object currently at the requested position")
    void removeAtTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);

        assertSame(register, hubRegister.removeAt(0));
        assertTrue(hubRegister.isEmpty());
    }

    @Test
    @DisplayName("replace swaps the object at a position and returns the old object")
    void replaceTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register registerOld = register(1, "A");
        Register registerReplacement = register(2, "B");
        hubRegister.add(registerOld);

        hubRegister.replace(0, registerReplacement);

        assertEquals(List.of(registerReplacement), hubRegister.toList());
    }

    @Test
    @DisplayName("setNullOnRemove stores whether removed objects should have reverse links nulled")
    void setNullOnRemoveTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        hubRegister.setNullOnRemove(true);

        assertTrue(hubRegister.getNullOnRemove());
    }

    @Test
    @DisplayName("getNullOnRemove reports the current reverse-null-on-remove setting")
    void getNullOnRemoveTest() {
        assertFalse(new Hub<>(Register.class).getNullOnRemove());
    }

    @Test
    @DisplayName("clear removes all membership and clears AO")
    void clearTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);
        hubRegister.setAO(register);

        hubRegister.clear();

        assertTrue(hubRegister.isEmpty());
        assertNull(hubRegister.getAO());
    }

    @Test
    @DisplayName("Clearing a detail Hub clears reverse references for removed details")
    void clear_withDetailHubTest() {
        MasterDetailScenario scenario = createMasterDetailScenario();

        scenario.hubDetailRegister.clear();

        assertTrue(scenario.hubDetailRegister.isEmpty());
        assertNull(scenario.register.getStore());
        assertNull(scenario.register2.getStore());
    }

    @Test
    @DisplayName("removeAll follows clear behavior for public no-argument removal")
    void removeAllTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        hubRegister.add(register(1, "A"));

        hubRegister.removeAll();

        assertTrue(hubRegister.isEmpty());
    }

    // ====================================================================
    // shared and detail Hub setup
    // ====================================================================

    @Test
    @DisplayName("createSharedHub creates a shared Hub with the same membership")
    void createSharedHubTest() {
        Hub<Register> hubSourceRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubSourceRegister.add(register);

        Hub<Register> hubSharedRegister = hubSourceRegister.createSharedHub();

        assertSame(hubSourceRegister, hubSharedRegister.getSharedHub());
        assertEquals(List.of(register), hubSharedRegister.toList());
    }

    @Test
    @DisplayName("createSharedHub can share active object state")
    void createSharedHub_withSharedActiveObjectTest() {
        Hub<Register> hubSourceRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubSourceRegister.add(register);

        Hub<Register> hubSharedRegister = hubSourceRegister.createSharedHub(true);
        hubSourceRegister.setAO(register);

        assertSame(register, hubSharedRegister.getAO());
    }

    @Test
    @DisplayName("setSharedHub uses the source Hub membership")
    void setSharedHubTest() {
        Hub<Register> hubSourceRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubSourceRegister.add(register);
        Hub<Register> hubSharedRegister = new Hub<>(Register.class);

        hubSharedRegister.setSharedHub(hubSourceRegister);

        assertSame(hubSourceRegister, hubSharedRegister.getSharedHub());
        assertEquals(List.of(register), hubSharedRegister.toList());
    }

    @Test
    @DisplayName("A shared Hub cannot later become a detail Hub with a non-null master")
    void setSharedHub_withRejectedMasterHubSetupTest() {
        Hub<Register> hubSourceRegister = new Hub<>(Register.class);
        Hub<Register> hubSharedRegister = hubSourceRegister.createSharedHub(false);
        Hub<Store> hubMasterStore = new Hub<>(Store.class);

        assertThrows(RuntimeException.class, () -> hubSharedRegister.setMasterHub(hubMasterStore, Store.P_Registers));
        assertSame(hubSourceRegister, hubSharedRegister.getSharedHub());
    }

    @Test
    @DisplayName("getSharedHub returns the Hub that supplies shared membership")
    void getSharedHubTest() {
        Hub<Register> hubSourceRegister = new Hub<>(Register.class);
        Hub<Register> hubSharedRegister = hubSourceRegister.createSharedHub(false);

        assertSame(hubSourceRegister, hubSharedRegister.getSharedHub());
    }

    @Test
    @DisplayName("getDetailHub creates a detail Hub bound to the current master AO")
    void getDetailHubTest() {
        MasterDetailScenario scenario = createMasterDetailScenario();

        assertSame(scenario.hubMasterStore, scenario.hubDetailRegister.getMasterHub());
        assertEquals(List.of(scenario.register, scenario.register2), scenario.hubDetailRegister.toList());
    }

    @Test
    @DisplayName("getDetailHub creates a recursive detail Hub bound to the current recursive master AO")
    void getDetailHub_withRecursiveReferenceTest() {
        RecursiveCatalogCategoryScenario scenario = createRecursiveCatalogCategoryScenario();

        /*qqqqq        
        assertSame(scenario.hubMasterCatalogCategory, scenario.hubDetailCatalogCategory.getMasterHub());
        assertEquals(List.of(scenario.catalogCategory2), scenario.hubDetailCatalogCategory.toList());
        assertSame(scenario.catalogCategory, scenario.catalogCategory2.getParentCatalogCategory());
        assertTrue(scenario.catalogCategory.getCatalogCategories().contains(scenario.catalogCategory2));
        */
    }

    @Test
    @DisplayName("setMasterHub binds this Hub as a detail Hub")
    void setMasterHubTest() {
        MasterDetailScenario scenario = createMasterDetailScenario();
        Hub<Register> hubDetailRegister = new Hub<>(Register.class);

        hubDetailRegister.setMasterHub(scenario.hubMasterStore, Store.P_Registers);

        assertSame(scenario.hubMasterStore, hubDetailRegister.getMasterHub());
        assertEquals(List.of(scenario.register, scenario.register2), hubDetailRegister.toList());
    }

    @Test
    @DisplayName("getMasterHub returns the Hub that owns detail membership")
    void getMasterHubTest() {
        MasterDetailScenario scenario = createMasterDetailScenario();

        assertSame(scenario.hubMasterStore, scenario.hubDetailRegister.getMasterHub());
    }

    @Test
    @DisplayName("getMasterObject returns the owning object for object-owned relationship Hubs")
    void getMasterObjectTest() {
        Store store = store(1, 101, "store1");

        assertSame(store, store.getRegisters().getMasterObject());
    }

    @Test
    @DisplayName("getMasterClass returns the owning class for object-owned relationship Hubs")
    void getMasterClassTest() {
        Store store = store(1, 101, "store1");

        assertEquals(Store.class, store.getRegisters().getMasterClass());
    }

    @Test
    @DisplayName("hasDetailHubs reports whether a master Hub has dependent detail Hubs")
    void hasDetailHubsTest() {
        MasterDetailScenario scenario = createMasterDetailScenario();

        assertTrue(scenario.hubMasterStore.hasDetailHubs());
    }

    @Test
    @DisplayName("removeDetailHub detaches a dependent detail Hub")
    void removeDetailHubTest() {
        MasterDetailScenario scenario = createMasterDetailScenario();

        assertTrue(scenario.hubMasterStore.removeDetailHub(scenario.hubDetailRegister));
        assertFalse(scenario.hubMasterStore.hasDetailHubs());
    }

    // ====================================================================
    // listeners and callbacks
    // ====================================================================

    @Test
    @DisplayName("addHubListener receives Hub add events")
    void addHubListenerTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        AtomicInteger adds = new AtomicInteger();
        HubListener<Register> listener = new HubListenerAdapter<Register>() {
            @Override
            public void afterAdd(HubEvent<Register> e) {
                adds.incrementAndGet();
            }
        };
        hubRegister.addHubListener(listener);

        hubRegister.add(register(1, "A"));

        assertEquals(1, adds.get());
    }

    @Test
    @DisplayName("removeHubListener stops delivery to the removed listener")
    void removeHubListenerTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        AtomicInteger adds = new AtomicInteger();
        HubListener<Register> listener = new HubListenerAdapter<Register>() {
            @Override
            public void afterAdd(HubEvent<Register> e) {
                adds.incrementAndGet();
            }
        };
        hubRegister.addHubListener(listener);
        hubRegister.removeHubListener(listener);

        hubRegister.add(register(1, "A"));

        assertEquals(0, adds.get());
    }

    @Test
    @DisplayName("onChangeAO runs when the active object changes")
    void onChangeAOTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);
        AtomicInteger calls = new AtomicInteger();
        hubRegister.onChangeAO(e -> calls.incrementAndGet());

        hubRegister.setAO(register);

        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("onPropertyChange runs when any member property changes")
    void onPropertyChangeTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);
        AtomicInteger calls = new AtomicInteger();
        hubRegister.onPropertyChange(e -> calls.incrementAndGet());

        register.setCode("B");

        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("onPropertyChange with a property name filters unrelated property changes")
    void onPropertyChange_withPropertyTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);
        AtomicInteger calls = new AtomicInteger();
        hubRegister.onPropertyChange(e -> calls.incrementAndGet(), Register.P_Code);

        register.setDeleteReason("ignore");
        register.setCode("B");

        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("onAdd runs after an object is added")
    void onAddTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        AtomicInteger calls = new AtomicInteger();
        hubRegister.onAdd(e -> calls.incrementAndGet());

        hubRegister.add(register(1, "A"));

        assertEquals(1, calls.get());
    }

    @Test
    @Disabled("TODO - deterministic refresh event fixture required")
    @DisplayName("TODO: onBeforeRefresh runs before a Hub refresh")
    void onBeforeRefreshTest() {
        // Matrix classification: DEFERRED for datasource/refresh event fixture.
    }

    @Test
    @DisplayName("onNewList runs when clear sends a new-list event")
    void onNewListTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        hubRegister.add(register(1, "A"));
        AtomicInteger calls = new AtomicInteger();
        hubRegister.onNewList(e -> calls.incrementAndGet());

        hubRegister.clear();

        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("onRemove runs after an object is removed")
    void onRemoveTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);
        AtomicInteger calls = new AtomicInteger();
        hubRegister.onRemove(e -> calls.incrementAndGet());

        hubRegister.remove(register);

        assertEquals(1, calls.get());
    }

    @Test
    @Disabled("TODO - listener mutation contract requires confirmation")
    @DisplayName("TODO: define supported listener mutation behavior during Hub events")
    void add_withListenerMutationTest() {
        // Matrix classification: DEFERRED
        // Evidence level: AMBIGUOUS
    }

    @Test
    @Disabled("TODO - exact shared/detail event fan-out requires confirmation")
    @DisplayName("TODO: define duplicate-event prevention across shared/detail/link propagation")
    void setAO_withSharedHubAndDetailHubAndDuplicateEventPreventionTest() {
        // Matrix classification: REQUIRED, but exact event-count invariant needs event-recorder fixture.
    }

    // ====================================================================
    // auto sequence, auto match, sorting, finding, and select configuration
    // ====================================================================

    @Test
    @DisplayName("setAutoSequence assigns sequence values as objects are added")
    void setAutoSequenceTest() {
        Hub<Store> hubStore = new Hub<>(Store.class);
        hubStore.setAutoSequence(Store.P_StoreNumber, 1);
        Store store = new Store();
        Store store2 = new Store();

        hubStore.add(store);
        hubStore.add(store2);

        assertEquals(1, store.getStoreNumber());
        assertEquals(2, store2.getStoreNumber());
    }

    @Test
    @Disabled("TODO - deterministic non-unique integer sequence fixture required")
    @DisplayName("TODO: resequence reapplies automatic sequence values in Hub order")
    void resequenceTest() {
        // Store.storeNumber is unique and can conflict during resequence swaps.
    }

    @Test
    @Disabled("TODO - auto-match fixture and expected propagation direction require confirmation")
    @DisplayName("TODO: setAutoMatch assigns matching values during Hub mutation")
    void setAutoMatchTest() {
        // Hub.setAutoMatch(String property, Hub hubMaster)
    }

    @Test
    @DisplayName("sort with a property path orders Hub membership")
    void sortTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "B");
        Register register2 = register(2, "A");
        hubRegister.add(register);
        hubRegister.add(register2);

        hubRegister.sort(Register.P_Code);

        assertEquals(List.of(register2, register), hubRegister.toList());
    }

    @Test
    @DisplayName("sort with a comparator orders Hub membership")
    void sort_withComparatorTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "B");
        Register register2 = register(2, "A");
        hubRegister.add(register);
        hubRegister.add(register2);

        hubRegister.sort(Comparator.comparing(Register::getCode));

        assertEquals(List.of(register2, register), hubRegister.toList());
    }

    @Test
    @DisplayName("Sorting a detail Hub does not alter reverse master references")
    void sort_withDetailHubTest() {
        MasterDetailScenario scenario = createMasterDetailScenario();
        scenario.register.setCode("B");
        scenario.register2.setCode("A");

        scenario.hubDetailRegister.sort(Register.P_Code);

        assertEquals(List.of(scenario.register2, scenario.register), scenario.hubDetailRegister.toList());
        assertSame(scenario.store, scenario.register.getStore());
        assertSame(scenario.store, scenario.register2.getStore());
    }

    @Test
    @DisplayName("isSorted reports true after a Hub sort is installed")
    void isSortedTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        hubRegister.sort(Register.P_Code);

        assertTrue(hubRegister.isSorted());
    }

    @Test
    @DisplayName("cancelSort removes active sort state")
    void cancelSortTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        hubRegister.sort(Register.P_Code);

        hubRegister.cancelSort();

        assertFalse(hubRegister.isSorted());
    }

    @Test
    @DisplayName("resort reapplies the configured sort to current membership")
    void resortTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "B");
        Register register2 = register(2, "A");
        hubRegister.add(register);
        hubRegister.add(register2);
        hubRegister.sort(Register.P_Code);
        register.setCode("0");

        hubRegister.resort();

        assertEquals(List.of(register, register2), hubRegister.toList());
    }

    @Test
    @DisplayName("find returns the first object matching a property value")
    void findTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);

        assertSame(register, hubRegister.find(Register.P_Code, "A"));
    }

    @Test
    @DisplayName("find with set-AO selects the found object")
    void find_withSetAOTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);

        assertSame(register, hubRegister.find(Register.P_Code, "A", true));
        assertSame(register, hubRegister.getAO());
    }

    @Test
    @DisplayName("findNext starts searching after the supplied object")
    void findNextTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "A");
        hubRegister.add(register);
        hubRegister.add(register2);

        assertSame(register2, hubRegister.findNext(register, Register.P_Code, "A"));
    }

    @Test
    @DisplayName("setSelectWhere stores the select where clause")
    void setSelectWhereTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        hubRegister.setSelectWhere("code = ?");

        assertEquals("code = ?", hubRegister.getSelectWhere());
    }

    @Test
    @DisplayName("getSelectWhere returns the configured select where clause")
    void getSelectWhereTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        hubRegister.setSelectWhere("code = ?");

        assertEquals("code = ?", hubRegister.getSelectWhere());
    }

    @Test
    @DisplayName("setSelectOrder stores the select order clause")
    void setSelectOrderTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        hubRegister.setSelectOrder(Register.P_Code);

        assertEquals(Register.P_Code, hubRegister.getSelectOrder(hubRegister));
    }

    @Test
    @DisplayName("getSelectOrder returns the configured select order clause")
    void getSelectOrderTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        hubRegister.setSelectOrder(Register.P_Code);

        assertEquals(Register.P_Code, hubRegister.getSelectOrder(hubRegister));
    }

    @Test
    @DisplayName("setSelectWhereHub stores select criteria without changing membership")
    void setSelectWhereHubTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Hub<Store> hubStore = new Hub<>(Store.class);

        hubRegister.setSelectWhereHub(hubStore, Store.P_Registers);

        assertTrue(hubRegister.isEmpty());
    }

    @Test
    @Disabled("TODO - deterministic datasource select fixture required")
    @DisplayName("TODO: select materializes datasource-backed Hub contents")
    void selectTest() {
        // Hub.select(...) overloads require a deterministic datasource fixture.
    }

    @Test
    @DisplayName("cancelSelect removes active select state without changing loaded membership")
    void cancelSelectTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);
        hubRegister.setSelectWhere("code = ?");

        hubRegister.cancelSelect();

        assertEquals(List.of(register), hubRegister.toList());
    }

    // ====================================================================
    // linked Hub public API
    // ====================================================================

    @Test
    @DisplayName("getLinkHub returns the configured link-to Hub")
    void getLinkHubTest() {
        Hub<Store> hubStore = new Hub<>(Store.class);
        Hub<Register> hubRegister = new Hub<>(Register.class);

        hubStore.setLinkHub(hubRegister, Register.P_Store);

        assertSame(hubRegister, hubStore.getLinkHub(false));
    }

    @Test
    @DisplayName("setLinkHubOnPos maps the link-to integer property to the link-from position")
    void setLinkHubOnPosTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register);
        hubRegister.add(register2);
        Hub<Store> hubSelectorStore = new Hub<>(Store.class);
        Store storeSelector = store(10, 1, "selector");
        hubSelectorStore.add(storeSelector);
        hubRegister.setLinkHubOnPos(hubSelectorStore, Store.P_StoreNumber);

        hubSelectorStore.setAO(storeSelector);

        assertSame(register2, hubRegister.getAO());
    }

    @Test
    @DisplayName("setLinkHub links direct object references from the link-to Hub")
    void setLinkHubTest() {
        Hub<Store> hubStore = new Hub<>(Store.class);
        Store store = store(1, 101, "store1");
        Store store2 = store(2, 202, "store2");
        hubStore.add(store);
        hubStore.add(store2);
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register registerSelector = register(10, "selector");
        registerSelector.setStore(store2);
        hubRegister.add(registerSelector);

        hubStore.setLinkHub(hubRegister, Register.P_Store);
        hubRegister.setAO(registerSelector);

        assertSame(store2, hubStore.getAO());
    }

    @Test
    @DisplayName("setLinkHub with from and to properties selects by matching property values")
    void setLinkHub_withPropertyToPropertyTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register);
        hubRegister.add(register2);
        Hub<Store> hubSelectorStore = new Hub<>(Store.class);
        Store storeSelector = store(10, 999, "B");
        hubSelectorStore.add(storeSelector);

        hubRegister.setLinkHub(Register.P_Code, hubSelectorStore, Store.P_Name);
        hubSelectorStore.setAO(storeSelector);

        assertSame(register2, hubRegister.getAO());
    }

    @Test
    @DisplayName("A property-to-property LinkHub with no match clears the link-from AO")
    void setLinkHub_withNoMatchTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);
        hubRegister.setAO(register);
        Hub<Store> hubSelectorStore = new Hub<>(Store.class);
        Store storeSelector = store(10, 999, "missing");
        hubSelectorStore.add(storeSelector);

        hubRegister.setLinkHub(Register.P_Code, hubSelectorStore, Store.P_Name);
        hubSelectorStore.setAO(storeSelector);

        assertNull(hubRegister.getAO());
    }

    @Test
    @Disabled("TODO - Vince must decide whether duplicate matches select first, fail, or remain undefined")
    @DisplayName("TODO: define behavior when linked property values have multiple matches")
    void setLinkHub_withMultipleMatchesTest() {
        // Matrix classification: DEFERRED
        // Evidence level: CURRENT_IMPLEMENTATION
    }

    @Test
    @DisplayName("removeLinkHub removes the configured link-to Hub")
    void removeLinkHubTest() {
        Hub<Store> hubStore = new Hub<>(Store.class);
        Hub<Register> hubRegister = new Hub<>(Register.class);
        hubStore.setLinkHub(hubRegister, Register.P_Store);

        hubStore.removeLinkHub();

        assertNull(hubStore.getLinkHub(false));
    }

    @Test
    @DisplayName("getLinkPath returns the configured link-to property path")
    void getLinkPathTest() {
        Hub<Store> hubStore = new Hub<>(Store.class);
        Hub<Register> hubRegister = new Hub<>(Register.class);
        hubStore.setLinkHub(hubRegister, Register.P_Store);

        assertEquals(Register.P_Store, hubStore.getLinkPath(false));
    }

    @Test
    @DisplayName("setLink is a public alias for setLinkHub")
    void setLinkTest() {
        Hub<Store> hubStore = new Hub<>(Store.class);
        Hub<Register> hubRegister = new Hub<>(Register.class);

        hubStore.setLink(hubRegister);

        assertSame(hubRegister, hubStore.getLinkHub(false));
    }

    // ====================================================================
    // permissions, loading, validation, collection facade, convenience APIs
    // ====================================================================

    @Test
    @DisplayName("isValid reports true for a normal in-memory Hub")
    void isValidTest() {
        assertTrue(new Hub<>(Register.class).isValid());
    }

    @Test
    @DisplayName("getOAObjectInfo returns metadata for the Hub object class")
    void getOAObjectInfoTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        assertEquals(Register.class, hubRegister.getOAObjectInfo().getForClass());
    }

    @Test
    @DisplayName("createShared is an alias for createSharedHub")
    void createSharedTest() {
        Hub<Register> hubSourceRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubSourceRegister.add(register);

        Hub<Register> hubSharedRegister = hubSourceRegister.createShared();

        assertSame(hubSourceRegister, hubSharedRegister.getSharedHub());
        assertEquals(List.of(register), hubSharedRegister.toList());
    }

    @Test
    @DisplayName("canAdd reports true for a valid object")
    void canAddTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        assertTrue(hubRegister.canAdd(register(1, "A")));
    }

    @Test
    @DisplayName("getCanAddMessage is null for a valid object")
    void getCanAddMessageTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        assertNull(hubRegister.getCanAddMessage(register(1, "A")));
    }

    @Test
    @DisplayName("getAllowAdd reports true for a valid OAObject")
    void getAllowAddTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        assertTrue(hubRegister.getAllowAdd(register(1, "A")));
    }

    @Test
    @DisplayName("getAllowRemove reports true for a valid OAObject")
    void getAllowRemoveTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);

        assertTrue(hubRegister.getAllowRemove(register));
    }

    @Test
    @DisplayName("getVerifyRemove reports true for a valid OAObject")
    void getVerifyRemoveTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);

        assertTrue(hubRegister.getVerifyRemove(register));
    }

    @Test
    @DisplayName("getAllowRemoveAll reports true for a normal in-memory Hub")
    void getAllowRemoveAllTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        assertTrue(hubRegister.getAllowRemoveAll(true, null));
    }

    @Test
    @DisplayName("setLoading changes the thread-local loading state")
    void setLoadingTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        boolean oldLoading = hubRegister.setLoading(true);
        try {
            assertTrue(hubRegister.isLoading());
        }
        finally {
            hubRegister.setLoading(oldLoading);
        }
    }

    @Test
    @DisplayName("isLoading reports false for a normal test thread")
    void isLoadingTest() {
        assertFalse(new Hub<>(Register.class).isLoading());
    }

    @Test
    @Disabled("TODO - sync/remote fixture required for refresh message assertion")
    @DisplayName("TODO: sendRefresh notifies remote clients when sync is configured")
    void sendRefreshTest() {
        // Hub.sendRefresh()
    }

    @Test
    @DisplayName("isEmpty reports true when the Hub has no objects")
    void isEmptyTest() {
        assertTrue(new Hub<>(Register.class).isEmpty());
    }

    @Test
    @DisplayName("containsAll reports true when all supplied objects are members")
    void containsAllTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register);
        hubRegister.add(register2);

        assertTrue(hubRegister.containsAll(List.of(register, register2)));
    }

    @Test
    @DisplayName("addAll appends all supplied collection members")
    void addAllTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");

        assertTrue(hubRegister.addAll(List.of(register, register2)));

        assertEquals(List.of(register, register2), hubRegister.toList());
    }

    @Test
    @DisplayName("addAll at an index inserts all supplied collection members in order")
    void addAll_withIndexTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        Register register3 = register(3, "C");
        hubRegister.add(register3);

        assertTrue(hubRegister.addAll(0, List.of(register, register2)));

        assertEquals(List.of(register, register2, register3), hubRegister.toList());
    }

    @Test
    @DisplayName("removeAll with a collection removes each supplied member")
    void removeAll_withCollectionTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register);
        hubRegister.add(register2);

        assertTrue(hubRegister.removeAll(List.of(register)));

        assertEquals(List.of(register2), hubRegister.toList());
    }

    @Test
    @DisplayName("retainAll keeps only supplied members")
    void retainAllTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register);
        hubRegister.add(register2);

        assertTrue(hubRegister.retainAll(List.of(register2)));

        assertEquals(List.of(register2), hubRegister.toList());
    }

    @Test
    @DisplayName("get from List interface delegates to getAt")
    void getTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);

        assertSame(register, hubRegister.get(0));
    }

    @Test
    @DisplayName("set replaces the object at the requested index")
    void setTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register registerOld = register(1, "A");
        Register registerReplacement = register(2, "B");
        hubRegister.add(registerOld);

        assertSame(registerOld, hubRegister.set(0, registerReplacement));
        assertEquals(List.of(registerReplacement), hubRegister.toList());
    }

    @Test
    @DisplayName("add at an index inserts the object at that index")
    void add_withIndexTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register2);

        hubRegister.add(0, register);

        assertEquals(List.of(register, register2), hubRegister.toList());
    }

    @Test
    @DisplayName("lastIndexOf delegates to current membership position")
    void lastIndexOfTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);

        assertEquals(0, hubRegister.lastIndexOf(register));
    }

    @Test
    @DisplayName("iterator iterates over a snapshot of Hub contents")
    void iteratorTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        hubRegister.add(register);

        Iterator<Register> it = hubRegister.iterator();

        assertTrue(it.hasNext());
        assertSame(register, it.next());
    }

    @Test
    @DisplayName("listIterator iterates over Hub contents in order")
    void listIteratorTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register);
        hubRegister.add(register2);

        ListIterator<Register> it = hubRegister.listIterator();

        assertTrue(it.hasNext());
        assertSame(register, it.next());
        assertSame(register2, it.next());
    }

    @Test
    @DisplayName("listIterator at an index starts iteration at that index")
    void listIterator_withIndexTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register);
        hubRegister.add(register2);

        ListIterator<Register> it = hubRegister.listIterator(1);

        assertSame(register2, it.next());
    }

    @Test
    @DisplayName("subList returns objects in the requested range")
    void subListTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubRegister.add(register);
        hubRegister.add(register2);

        assertEquals(List.of(register), hubRegister.subList(0, 1));
    }

    @Test
    @DisplayName("stream exposes the current Hub membership")
    void streamTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        hubRegister.add(register(1, "A"));
        hubRegister.add(register(2, "B"));

        assertEquals(2, hubRegister.stream().count());
    }

    @Test
    @DisplayName("createFilteredHub creates a Hub containing source objects accepted by the filter")
    void createFilteredHubTest() {
        Hub<Register> hubSourceRegister = new Hub<>(Register.class);
        Register register = register(1, "A");
        Register register2 = register(2, "B");
        hubSourceRegister.add(register);
        hubSourceRegister.add(register2);

        Hub<Register> hubFilteredRegister = hubSourceRegister.createFilteredHub(registerObject -> "A".equals(((Register) registerObject).getCode()), Register.P_Code);

        assertTrue(hubFilteredRegister.contains(register));
        assertFalse(hubFilteredRegister.contains(register2));
    }

    @Test
    @Disabled("TODO - deterministic datasource-backed refresh fixture required")
    @DisplayName("TODO: refresh reselects datasource-backed Hub contents")
    void refreshTest() {
        // Hub.refresh()
    }

    @Test
    @DisplayName("getOA returns the runtime associated with this Hub")
    void getOATest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);

        assertSame(OARuntime.defaultOA(), hubRegister.getOA());
    }

    @Test
    @Disabled("TODO - serialization fixture required")
    @DisplayName("TODO: readResolve restores Hub runtime state after deserialization")
    void readResolveTest() {
        // Hub.readResolve() throws ObjectStreamException
    }

    @Test
    @Disabled("TODO - finalization side effects are not a stable public invariant")
    @DisplayName("TODO: define whether Hub finalization has an observable contract")
    void finalizeTest() {
        // Hub.finalize() throws Throwable
    }

    @Test
    @DisplayName("isMoreData reports false for a fully in-memory Hub")
    void isMoreDataTest() {
        assertFalse(new Hub<>(Register.class).isMoreData());
    }

    @Test
    @DisplayName("addListener registers a Hub listener alias for addHubListener")
    void addListenerTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        AtomicInteger calls = new AtomicInteger();
        HubListener<Register> listener = new HubListenerAdapter<Register>() {
            @Override
            public void afterAdd(HubEvent<Register> e) {
                calls.incrementAndGet();
            }
        };

        hubRegister.addListener(listener);
        hubRegister.add(register(1, "A"));

        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("removeListener unregisters a listener alias for removeHubListener")
    void removeListenerTest() {
        Hub<Register> hubRegister = new Hub<>(Register.class);
        AtomicInteger calls = new AtomicInteger();
        HubListener<Register> listener = new HubListenerAdapter<Register>() {
            @Override
            public void afterAdd(HubEvent<Register> e) {
                calls.incrementAndGet();
            }
        };
        hubRegister.addListener(listener);

        hubRegister.removeListener(listener);
        hubRegister.add(register(1, "A"));

        assertEquals(0, calls.get());
    }

    @Test
    @Disabled("TODO - trigger fixture and expected callback path require confirmation")
    @DisplayName("TODO: addTriggerListener invokes trigger callbacks for dependent path changes")
    void addTriggerListenerTest() {
        // Hub.addTriggerListener(HubListener<TYPE> hl, String property, String path)
    }

    @Test
    @Disabled("TODO - trigger fixture and background-thread contract require confirmation")
    @DisplayName("TODO: addTriggerListener supports background trigger callbacks")
    void addTriggerListener_withBackgroundThreadTest() {
        // Hub.addTriggerListener(HubListener<TYPE> hl, String property, String path, boolean useBackgroundThread)
    }

    @Test
    @Disabled("TODO - select passthrough requires deterministic datasource fixture")
    @DisplayName("TODO: selectPassthru delegates raw where/order clauses to datasource selection")
    void selectPassthruTest() {
        // Hub.selectPassthru(String whereClause, String orderClause)
    }

}
