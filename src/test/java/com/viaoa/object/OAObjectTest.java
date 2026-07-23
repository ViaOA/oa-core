package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.*;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.hub.Hub;
import com.viaoa.lang.oa.VEnum;
import com.viaoa.oa.OA;
import com.viaoa.runtime.OARuntime;

class OAObjectTest {

    private static CatalogCategory catalogCategory(int id, String name) {
        CatalogCategory catalogCategory = new CatalogCategory();
        catalogCategory.setId(id);
        catalogCategory.setName(name);
        return catalogCategory;
    }

    private static RecursiveCatalogCategoryScenario createRecursiveCatalogCategoryScenario() {
        CatalogCategory catalogCategory = catalogCategory(1, "Root");
        CatalogCategory catalogCategory2 = catalogCategory(2, "Child");
        CatalogCategory catalogCategory3 = catalogCategory(3, "Grandchild");

        catalogCategory2.setProperty(CatalogCategory.P_ParentCatalogCategory, catalogCategory);
        catalogCategory3.setProperty(CatalogCategory.P_ParentCatalogCategory, catalogCategory2);

        return new RecursiveCatalogCategoryScenario(catalogCategory, catalogCategory2, catalogCategory3);
    }

    private static final class RecursiveCatalogCategoryScenario {
        final CatalogCategory catalogCategory;
        final CatalogCategory catalogCategory2;
        final CatalogCategory catalogCategory3;

        RecursiveCatalogCategoryScenario(CatalogCategory catalogCategory, CatalogCategory catalogCategory2, CatalogCategory catalogCategory3) {
            this.catalogCategory = catalogCategory;
            this.catalogCategory2 = catalogCategory2;
            this.catalogCategory3 = catalogCategory3;
        }
    }

    @BeforeEach
    void beforeEach() {
        OA oa = OARuntime.createDefaultOA(Register.class);
    }

    @AfterEach
    void afterEach() {
        OAObject.setDebugMode(false);
        OARuntime.defaultOA().close();
    }

    @Test
    @DisplayName("The OA version identifies the loaded OA 4 runtime")
    void getOAVersionTest() {
        assertTrue(OAObject.getOAVersion().startsWith("4.0.0"));
    }

    @Test
    @DisplayName("Constructing an OAObject initializes identity and lifecycle state")
    void constructorTest() {
        Store store = new Store();

        assertNotNull(store.getGuid());
        assertNotNull(store.getObjectKey());
        assertTrue(store.getNew());
        assertTrue(store.isNew());
        assertTrue(store.getChanged());
        assertTrue(store.isChanged());
        assertFalse(store.getDeleted());
        assertFalse(store.wasDeleted());
        assertFalse(store.isDeleted());
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.readResolve")
    void readResolveTest() {
        // OAObject.readResolve() throws ObjectStreamException
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @DisplayName("A boolean property can be assigned through OAObject property metadata")
    void setPropertyBooleanTest() {
        // OAObject.setProperty(String propName, boolean value)
        AppServer appServer = new AppServer();

        appServer.setProperty(AppServer.P_DemoMode, true);

        assertTrue(appServer.getDemoMode());
    }

    @Test
    @DisplayName("An int property can be assigned through OAObject property metadata")
    void setPropertyIntTest() {
        // OAObject.setProperty(String propName, int value)
        Store store = new Store();

        store.setProperty(Store.P_StoreNumber, 55);

        assertEquals(55, store.getStoreNumber());
    }

    @Test
    @DisplayName("A long property can be assigned through OAObject property metadata")
    void setPropertyLongTest() {
        // OAObject.setProperty(String propName, long value)
        Store store = new Store();

        store.setProperty(Store.P_Id, 7L);

        assertEquals(7L, store.getId());
    }

    @Test
    @DisplayName("A double value is converted when assigned to a compatible int property")
    void setPropertyDoubleTest() {
        // OAObject.setProperty(String propName, double value)
        Store store = new Store();

        store.setProperty(Store.P_StoreNumber, 12.0d);

        assertEquals(12, store.getStoreNumber());
    }

    @Test
    @DisplayName("An object value can be assigned through OAObject property metadata")
    void setPropertyObjectTest() {
        // OAObject.setProperty(String propName, Object value)
        Store store = new Store();

        store.setProperty(Store.P_Name, "Main");

        assertEquals("Main", store.getName());
    }

    @Test
    @DisplayName("Assigning a many-to-one property adds the object to the reverse many-Hub")
    void setProperty_withOneToManyTest() {
        // OAObject.setProperty(String propName, Object value)
        Store store = new Store(1);
        Register register = new Register(2);

        register.setProperty(Register.P_Store, store);

        assertSame(store, register.getStore());
        assertTrue(store.getRegisters().contains(register));
    }

    @Test
    @DisplayName("Assigning a one-to-one property updates the reverse scalar reference")
    void setProperty_withOneToOneTest() {
        // OAObject.setProperty(String propName, Object value)
        Store store = new Store(1);
        Address address = new Address(2);

        store.setProperty(Store.P_Address, address);

        assertSame(address, store.getAddress());
        assertSame(store, address.getStore());
    }

    @Test
    @DisplayName("Reassigning a many-to-one property removes the object from the prior reverse many-Hub")
    void setProperty_withExistingParentTest() {
        // OAObject.setProperty(String propName, Object value)
        Store storeOld = new Store(1);
        Store storeNew = new Store(2);
        Register register = new Register(3);
        register.setStore(storeOld);

        register.setProperty(Register.P_Store, storeNew);

        assertSame(storeNew, register.getStore());
        assertFalse(storeOld.getRegisters().contains(register));
        assertTrue(storeNew.getRegisters().contains(register));
    }

    @Test
    @DisplayName("Clearing a many-to-one property removes the object from the reverse many-Hub")
    void setProperty_withNullReferenceTest() {
        // OAObject.setProperty(String propName, Object value)
        Store store = new Store(1);
        Register register = new Register(2);
        register.setStore(store);

        register.setProperty(Register.P_Store, null);

        assertNull(register.getStore());
        assertFalse(store.getRegisters().contains(register));
    }

    @Test
    @DisplayName("Assigning a recursive one-link updates the reverse recursive many-Hub")
    void setProperty_withRecursiveReferenceTest() {
        // OAObject.setProperty(String propName, Object value)
        CatalogCategory catalogCategory = catalogCategory(1, "Root");
        CatalogCategory catalogCategory2 = catalogCategory(2, "Child");

        catalogCategory2.setProperty(CatalogCategory.P_ParentCatalogCategory, catalogCategory);

        assertSame(catalogCategory, catalogCategory2.getParentCatalogCategory());
        assertTrue(catalogCategory.getCatalogCategories().contains(catalogCategory2));
    }

    @Test
    @DisplayName("setNull clears the property value and records its null state")
    void setNullTest() {
        Store store = new Store();
        store.setName("North");

        store.setNull(Store.P_Name);

        assertNull(store.getName());
        assertTrue(store.isNull(Store.P_Name));
        assertTrue(store.getNull(Store.P_Name));
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.setPrimitiveNull")
    void setPrimitiveNullTest() {
        // OAObject.setPrimitiveNull(String propName, boolean b)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @DisplayName("Formatted property assignment updates the target property value")
    void setPropertyFormattedTest() {
        // OAObject.setProperty(String propName, Object value, String fmt)
        Store store = new Store();

        store.setProperty(Store.P_Name, "Formatted", null);

        assertEquals("Formatted", store.getName());
    }

    @Test
    @DisplayName("getProperty returns the current value stored for a model property")
    void getPropertyTest() {
        Store store = new Store();
        store.setStoreNumber(123);

        assertEquals(123, store.getProperty(Store.P_StoreNumber));
    }

    @Test
    @DisplayName("getPropertyAsString returns the default String representation")
    void getPropertyAsStringTest() {
        // OAObject.getPropertyAsString(String propName)
        Store store = new Store();
        store.setStoreNumber(123);

        assertEquals("123", store.getPropertyAsString(Store.P_StoreNumber));
    }

    @Test
    @DisplayName("getPropertyAsString can apply default formatting rules")
    void getPropertyAsStringDefaultFormattingTest() {
        // OAObject.getPropertyAsString(String propName, boolean bUseDefaultFormatting)
        Store store = new Store();
        store.setStoreNumber(123);

        assertEquals("123", store.getPropertyAsString(Store.P_StoreNumber, true));
    }

    @Test
    @DisplayName("getPropertyAsString accepts an explicit format argument")
    void getPropertyAsStringFormatTest() {
        // OAObject.getPropertyAsString(String propName, String fmt)
        Store store = new Store();
        store.setStoreNumber(123);

        assertEquals("123", store.getPropertyAsString(Store.P_StoreNumber, ""));
    }

    @Test
    @DisplayName("getPropertyAsString returns the supplied null value for null properties")
    void getPropertyAsStringNullValueTest() {
        // OAObject.getPropertyAsString(String propName, String fmt, String nullValue)
        Store store = new Store();

        assertEquals("missing", store.getPropertyAsString(Store.P_Name, null, "missing"));
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.removeProperty")
    void removePropertyTest() {
        // OAObject.removeProperty(String name)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @DisplayName("Normal model property changes are valid by default")
    void isValidPropertyChangeTest() {
        // OAObject.isValidPropertyChange(String propertyName, Object oldValue, Object newValue)
        Store store = new Store();

        assertTrue(store.isValidPropertyChange(Store.P_Name, null, "New"));
    }

    @Test
    @DisplayName("Normal model property assignments are valid by default")
    void isValidPropertyChangeNewValueTest() {
        // OAObject.isValidPropertyChange(String propertyName, Object newValue)
        Store store = new Store();

        assertTrue(store.isValidPropertyChange(Store.P_Name, "New"));
    }

    @Test
    @DisplayName("Property-change verification returns a callback result object")
    void getIsValidPropertyChangeObjectCallbackTest() {
        // OAObject.getIsValidPropertyChangeObjectCallback(String propertyName, Object oldValue, Object newValue)
        Store store = new Store();

        OAObjectCallback callback = store.getIsValidPropertyChangeObjectCallback(Store.P_Name, null, "New");

        assertNotNull(callback);
        assertTrue(callback.getAllowed());
    }

    @Test
    @DisplayName("Property assignment verification returns a callback result object")
    void getIsValidPropertyChangeObjectCallbackNewValueTest() {
        // OAObject.getIsValidPropertyChangeObjectCallback(String propertyName, Object newValue)
        Store store = new Store();

        OAObjectCallback callback = store.getIsValidPropertyChangeObjectCallback(Store.P_Name, "New");

        assertNotNull(callback);
        assertTrue(callback.getAllowed());
    }

    @Test
    @DisplayName("Property enabled checks return the OA rules engine decision")
    void isEnabledTest() {
        // OAObject.isEnabled(String propertyName)
        Store store = new Store();

        assertEquals(store.getIsEnabledObjectCallback(Store.P_Name, null, null).getAllowed(), store.isEnabled(Store.P_Name));
    }

    @Test
    @DisplayName("Property enabled evaluation returns a callback result object with the current decision")
    void getIsEnabledObjectCallbackTest() {
        // OAObject.getIsEnabledObjectCallback(String propertyName, Object oldValue, Object newValue)
        Store store = new Store();

        OAObjectCallback callback = store.getIsEnabledObjectCallback(Store.P_Name, null, "New");

        assertNotNull(callback);
        assertFalse(callback.getAllowed());
    }

    @Test
    @DisplayName("Object enabled checks return the OA rules engine decision")
    void isEnabledObjectTest() {
        // OAObject.isEnabled()
        Store store = new Store();

        assertEquals(store.getIsEnabledObjectCallback().getAllowed(), store.isEnabled());
    }

    @Test
    @DisplayName("Object enabled evaluation returns a callback result object with the current decision")
    void getIsEnabledObjectCallbackObjectTest() {
        // OAObject.getIsEnabledObjectCallback()
        Store store = new Store();

        OAObjectCallback callback = store.getIsEnabledObjectCallback();

        assertNotNull(callback);
        assertFalse(callback.getAllowed());
    }

    @Test
    @DisplayName("Normal model properties are visible by default")
    void isVisibleTest() {
        // OAObject.isVisible(String propertyName)
        Store store = new Store();

        assertTrue(store.isVisible(Store.P_Name));
    }

    @Test
    @DisplayName("Property visible evaluation returns a callback result object")
    void getIsVisibleObjectCallbackTest() {
        // OAObject.getIsVisibleObjectCallback(String propertyName)
        Store store = new Store();

        OAObjectCallback callback = store.getIsVisibleObjectCallback(Store.P_Name);

        assertNotNull(callback);
        assertTrue(callback.getAllowed());
    }

    @Test
    @DisplayName("Objects are visible by default")
    void isVisibleObjectTest() {
        // OAObject.isVisible()
        Store store = new Store();

        assertTrue(store.isVisible());
    }

    @Test
    @DisplayName("Object visible evaluation returns a callback result object")
    void getIsVisibleObjectCallbackObjectTest() {
        // OAObject.getIsVisibleObjectCallback()
        Store store = new Store();

        OAObjectCallback callback = store.getIsVisibleObjectCallback();

        assertNotNull(callback);
        assertTrue(callback.getAllowed());
    }

    @Test
    @DisplayName("Command verification returns the OA rules engine decision")
    void verifyCommandTest() {
        Store store = new Store();

        assertEquals(store.getVerifyCommand("callback").getAllowed(), store.verifyCommand("callback"));
    }

    @Test
    @DisplayName("Command verification returns a callback result object with the current decision")
    void getVerifyCommandTest() {
        Store store = new Store();

        OAObjectCallback callback = store.getVerifyCommand("callback");

        assertNotNull(callback);
        assertFalse(callback.getAllowed());
    }

    @Test
    @DisplayName("Submit permission returns a callback result object")
    void getAllowSubmitTest() {
        Store store = new Store();

        assertNotNull(store.getAllowSubmit());
    }

    @Test
    @DisplayName("Save verification returns a callback result object")
    void getVerifySaveObjectCallbackTest() {
        Store store = new Store();

        assertNotNull(store.getVerifySaveObjectCallback());
    }

    @Test
    @DisplayName("New objects report the new flag through getNew")
    void getNewTest() {
        assertTrue(new Store().getNew());
    }

    @Test
    @DisplayName("New objects report the new flag through isNew")
    void isNewTest() {
        assertTrue(new Store().isNew());
    }

    @Test
    @DisplayName("setNew updates both new-state accessors")
    void setNewTest() {
        Store store = new Store();

        store.setNew(false);

        assertFalse(store.getNew());
        assertFalse(store.isNew());
    }

    @Test
    @DisplayName("New objects are not deleted by default")
    void getDeletedTest() {
        assertFalse(new Store().getDeleted());
    }

    @Test
    @DisplayName("New objects have not previously been deleted")
    void wasDeletedTest() {
        assertFalse(new Store().wasDeleted());
    }

    @Test
    @DisplayName("New objects are not deleted by default through isDeleted")
    void isDeletedTest() {
        assertFalse(new Store().isDeleted());
    }

    @Test
    @DisplayName("setDeleted updates all deleted-state accessors")
    void setDeletedTest() {
        Store store = new Store();

        store.setDeleted(true);

        assertTrue(store.getDeleted());
        assertTrue(store.wasDeleted());
        assertTrue(store.isDeleted());
    }

    @Test
    @DisplayName("OAObject equality is based on runtime GUID identity")
    void equalsTest() {
        Store store = new Store(1);
        Store store2 = new Store(2);
        OAObject.FriendAccess friendAccess = new OAObjectInternalBridge().getObjectFriendAccess();
        UUID guid = store.getGuid();
        friendAccess.setGuid(store2, null);
        friendAccess.setGuid(store2, guid);

        assertEquals(store, store2);
        assertNotEquals(store, "x");
        assertNotEquals(store, new Store(3));
    }

    @Test
    @DisplayName("OAObject hash codes are derived from runtime GUID identity")
    void hashCodeTest() {
        Store store = new Store(1);
        Store store2 = new Store(2);
        OAObject.FriendAccess friendAccess = new OAObjectInternalBridge().getObjectFriendAccess();
        UUID guid = store.getGuid();
        friendAccess.setGuid(store2, null);
        friendAccess.setGuid(store2, guid);

        assertEquals(store.hashCode(), store2.hashCode());
    }

    @Test
    @DisplayName("OAObject comparison follows runtime GUID identity and orders null last")
    void compareToTest() {
        Store store = new Store(1);
        Store store2 = new Store(2);
        OAObject.FriendAccess friendAccess = new OAObjectInternalBridge().getObjectFriendAccess();
        UUID guid = store.getGuid();
        friendAccess.setGuid(store2, null);
        friendAccess.setGuid(store2, guid);

        assertEquals(0, store.compareTo(store2));
        assertTrue(store.compareTo(null) > 0);
    }

    @Test
    @DisplayName("New objects are changed by default")
    void getChangedTest() {
        assertTrue(new Store().getChanged());
    }

    @Test
    @DisplayName("New objects are changed by default through isChanged")
    void isChangedTest() {
        assertTrue(new Store().isChanged());
    }

    @Test
    @DisplayName("Cleared objects report unchanged when link changes are excluded")
    void getChangedIncludeLinksTest() {
        // OAObject.getChanged(boolean bIncludeLinks)
        Store store = new Store();
        new OAObjectInternalBridge().getObjectFriendAccess().setNew(store, false);

        store.setChanged(false);

        assertFalse(store.getChanged(false));
    }

    @Test
    @DisplayName("Cleared objects report unchanged through isChanged when link changes are excluded")
    void isChangedIncludeLinksTest() {
        // OAObject.isChanged(boolean bIncludeLinks)
        Store store = new Store();
        new OAObjectInternalBridge().getObjectFriendAccess().setNew(store, false);

        store.setChanged(false);

        assertFalse(store.isChanged(false));
    }

    @Test
    @DisplayName("Cleared objects report unchanged for cascade-none relationship checks")
    void getChangedRelationshipTypeTest() {
        // OAObject.getChanged(int relationshipType)
        Store store = new Store();
        new OAObjectInternalBridge().getObjectFriendAccess().setNew(store, false);

        store.setChanged(false);

        assertFalse(store.getChanged(OAObject.CASCADE_NONE));
    }

    @Test
    @DisplayName("setChanged controls the object changed flag without changing property values")
    void setChangedTest() {
        Store store = new Store();
        store.setName("Stable");
        new OAObjectInternalBridge().getObjectFriendAccess().setNew(store, false);

        store.setChanged(false);

        assertFalse(store.getChanged());
        assertEquals("Stable", store.getName());
    }

    @Test
    @DisplayName("createCopy copies scalar model properties into a distinct object")
    void createCopyTest() {
        Store store = new Store(1);
        store.setName("Original");
        store.setStoreNumber(101);

        Store storeCopy = (Store) store.createCopy();

        assertNotSame(store, storeCopy);
        assertEquals("Original", storeCopy.getName());
        assertEquals(101, storeCopy.getStoreNumber());
    }

    @Test
    @DisplayName("createCopy honors excluded property names")
    void createCopyExcludePropertiesTest() {
        // OAObject.createCopy(String[] excludePropertyNames)
        Store store = new Store(1);
        store.setName("Original");

        Store storeCopy = (Store) store.createCopy(new String[] { Store.P_Name });

        assertNull(storeCopy.getName());
    }

    @Test
    @DisplayName("copyInto copies scalar model properties into the target object")
    void copyIntoTest() {
        Store store = new Store(1);
        store.setName("Original");
        Store storeTarget = new Store();

        store.copyInto(storeTarget);

        assertEquals("Original", storeTarget.getName());
    }

    @Test
    @DisplayName("setFinalizeSave does not enable finalize-save behavior in the current runtime")
    void setFinalizeSaveTest() {
        OAObject.setFinalizeSave(true);

        assertFalse(OAObject.getFinalizeSave());
    }

    @Test
    @DisplayName("getFinalizeSave is false in the current runtime")
    void getFinalizeSaveTest() {
        assertFalse(OAObject.getFinalizeSave());
    }

    @Test
    @DisplayName("New in-memory objects are not in a loading state")
    void isLoadingTest() {
        assertFalse(new Store().isLoading());
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.fireBeforePropertyChange")
    void fireBeforePropertyChangeLocalOnlyTest() {
        // OAObject.fireBeforePropertyChange(String propertyName, Object oldObj, Object newObj, boolean bLocalOnly)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.fireBeforePropertyChange")
    void fireBeforePropertyChangeTest() {
        // OAObject.fireBeforePropertyChange(String propertyName, Object oldObj, Object newObj)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.fireBeforePropertyChange")
    void fireBeforePropertyChangeBooleanTest() {
        // OAObject.fireBeforePropertyChange(String property, boolean oldObj, boolean newObj)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.fireBeforePropertyChange")
    void fireBeforePropertyChangeIntTest() {
        // OAObject.fireBeforePropertyChange(String property, int oldObj, int newObj)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.fireBeforePropertyChange")
    void fireBeforePropertyChangeLongTest() {
        // OAObject.fireBeforePropertyChange(String property, long oldObj, long newObj)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.fireBeforePropertyChange")
    void fireBeforePropertyChangeDoubleTest() {
        // OAObject.fireBeforePropertyChange(String property, double oldObj, double newObj)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.firePropertyChange")
    void firePropertyChangeLocalOnlyTest() {
        // OAObject.firePropertyChange(String propertyName, Object oldObj, Object newObj, boolean bLocalOnly)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.firePropertyChange")
    void firePropertyChangeTest() {
        // OAObject.firePropertyChange(String propertyName, Object oldObj, Object newObj)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.firePropertyChange")
    void firePropertyChangeNameOnlyTest() {
        // OAObject.firePropertyChange(String propertyName)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.fireNewList")
    void fireNewListTest() {
        // OAObject.fireNewList(String hubPropertyName)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.firePropertyChange")
    void firePropertyChangeBooleanTest() {
        // OAObject.firePropertyChange(String property, boolean oldObj, boolean newObj)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.firePropertyChange")
    void firePropertyChangeIntTest() {
        // OAObject.firePropertyChange(String property, int oldObj, int newObj)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.firePropertyChange")
    void firePropertyChangeLongTest() {
        // OAObject.firePropertyChange(String property, long oldObj, long newObj)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.firePropertyChange")
    void firePropertyChangeDoubleTest() {
        // OAObject.firePropertyChange(String property, double oldObj, double newObj)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.fireLocalPropertyChange")
    void fireLocalPropertyChangeTest() {
        // OAObject.fireLocalPropertyChange(String property, Object oldObj, Object newObj)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.fireLocalPropertyChange")
    void fireLocalPropertyChangeNameOnlyTest() {
        // OAObject.fireLocalPropertyChange(String property)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.fireLocalPropertyChange")
    void fireLocalPropertyChangeIntTest() {
        // OAObject.fireLocalPropertyChange(String property, int oldObj, int newObj)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @DisplayName("getHub returns the generated many-Hub for a link property")
    void getHubTest() {
        // OAObject.getHub(String linkPropertyName)
        Store store = new Store();

        assertSame(store.getRegisters(), store.getHub(Store.P_Registers));
    }

    @Test
    @DisplayName("getHub returns a typed generated many-Hub for a link property")
    void getHubTypedTest() {
        // OAObject.getHub(Class<T> type, String linkPropertyName)
        Store store = new Store();

        assertSame(store.getRegisters(), store.getHub(Register.class, Store.P_Registers));
    }

    @Test
    @DisplayName("getHub returns the recursive many-Hub and preserves reverse parent links")
    void getHub_withRecursiveReferenceTest() {
        // OAObject.getHub(String linkPropertyName)
        RecursiveCatalogCategoryScenario scenario = createRecursiveCatalogCategoryScenario();

        Hub<?> hubCatalogCategory = scenario.catalogCategory.getHub(CatalogCategory.P_CatalogCategories);

        assertSame(scenario.catalogCategory.getCatalogCategories(), hubCatalogCategory);
        assertTrue(hubCatalogCategory.contains(scenario.catalogCategory2));
        assertSame(scenario.catalogCategory, scenario.catalogCategory2.getParentCatalogCategory());
    }

    @Test
    @DisplayName("getHub returns the already initialized many-Hub reference")
    void getHub_withManyHubInitializedTest() {
        // OAObject.getHub(String linkPropertyName)
        Store store = new Store();
        Hub<Register> hubRegister = store.getRegisters();

        assertSame(hubRegister, store.getHub(Store.P_Registers));
        assertTrue(store.isHubLoaded(Store.P_Registers));
    }

    @Test
    @DisplayName("getHub returns a generated many-Hub that OA considers loaded")
    void getHub_withManyHubNotInitializedTest() {
        // OAObject.getHub(String linkPropertyName)
        Store store = new Store();

        assertTrue(store.isHubLoaded(Store.P_Registers));
        Hub<?> hubRegister = store.getHub(Store.P_Registers);

        assertNotNull(hubRegister);
        assertTrue(store.isHubLoaded(Store.P_Registers));
    }

    @Test
    @DisplayName("setHub assigns the supplied many-Hub to the reference property")
    void setHubTest() {
        Store store = new Store(1);
        Hub<Register> hubRegister = new Hub<>(Register.class);

        store.setHub(Store.P_Registers, hubRegister);

        assertSame(hubRegister, store.getRegisters());
    }

    @Test
    @DisplayName("Assigning a many-Hub allows additions to maintain the reverse one-link")
    void setHub_withReverseOneLinkTest() {
        Store store = new Store(1);
        Hub<Register> hubRegister = new Hub<>(Register.class);
        Register register = new Register(2);

        store.setHub(Store.P_Registers, hubRegister);
        store.getRegisters().add(register);

        assertSame(hubRegister, store.getRegisters());
        assertSame(store, register.getStore());
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.getHub")
    void getHubSortOrderTest() {
        // OAObject.getHub(String linkPropertyName, String sortOrder)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.getHub")
    void getHubSortOrderSequenceTest() {
        // OAObject.getHub(String linkPropertyName, String sortOrder, boolean bSequence)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.getHub")
    void getHubSortOrderSequenceMatchHubTest() {
        // OAObject.getHub(String linkPropertyName, String sortOrder, boolean bSequence, Hub hubMatch)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.getHub")
    void getHubSortOrderMatchHubTest() {
        // OAObject.getHub(String linkPropertyName, String sortOrder, Hub hubMatch)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.getHub")
    void getHubMatchHubTest() {
        // OAObject.getHub(String linkPropertyName, Hub hubMatch)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @DisplayName("getObject returns the current one-link object")
    void getObjectTest() {
        Register register = new Register(2);
        Store store = new Store(1);

        register.setStore(store);

        assertSame(store, register.getObject(Register.P_Store));
    }

    @Test
    @DisplayName("getObject returns the materialized one-link reference")
    void getObject_withLoadedReferenceTest() {
        // OAObject.getObject(String linkPropertyName)
        Register register = new Register(2);
        Store store = new Store(1);
        register.setStore(store);

        assertSame(store, register.getObject(Register.P_Store));
        assertTrue(register.isLoaded(Register.P_Store));
    }

    @Test
    @Disabled("TODO - deterministic datasource-backed object-key reference fixture required")
    @DisplayName("TODO: getObject materializes an unloaded one-link reference")
    void getObject_withUnloadedReferenceTest() {
        // Matrix classification: REQUIRED
        // Evidence level: STRONGLY_INFERRED
        // Required fixture: Register with Store foreign key set but Store reference not materialized.
    }

    @Test
    @DisplayName("A loaded many-Hub reference is not considered null")
    void isReferenceObjectNullTest() {
        Store store = new Store();

        store.getRegisters();

        assertFalse(store.isReferenceObjectNull(Store.P_Registers));
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.getBlob")
    void getBlobTest() {
        // OAObject.getBlob(String linkPropertyName)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.save")
    void saveTest() {
        // OAObject.save()
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.save")
    void saveCascadeTest() {
        // OAObject.save(int iCascadeRule)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @DisplayName("canSave returns the OA rules engine decision for a new in-memory object")
    void canSaveTest() {
        assertFalse(new Store().canSave());
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.saveAll")
    void saveAllTest() {
        // OAObject.saveAll()
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @DisplayName("afterSave can be invoked for an in-memory object without changing identity")
    void afterSaveTest() {
        Store store = new Store();
        UUID guid = store.getGuid();

        store.afterSave();

        assertEquals(guid, store.getGuid());
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.delete")
    void deleteTest() {
        // OAObject.delete()
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @Disabled("TODO - verified cascade-delete datasource/lifecycle fixture required")
    @DisplayName("TODO: deleting an owner applies the configured owned-reference delete contract")
    void delete_withOwnedReferenceTest() {
        // Matrix classification: REQUIRED
        // Evidence level: STRONGLY_INFERRED
        // Candidate fixture: Store.Registers ownership with cascadeDelete=true.
    }

    @Test
    @Disabled("TODO - optional reference delete invariant requires confirmation")
    @DisplayName("TODO: deleting an object preserves optional non-owned references according to OA delete rules")
    void delete_withOptionalReferenceTest() {
        // Matrix classification: DEFERRED
        // Evidence level: AMBIGUOUS
        // Required fixture: verified optional, non-owned OAPOS reference.
    }

    @Test
    @DisplayName("canDelete returns the OA rules engine decision for a new in-memory object")
    void canDeleteTest() {
        assertFalse(new Store().canDelete());
    }

    @Test
    @DisplayName("afterDelete can be invoked for an in-memory object without changing identity")
    void afterDeleteTest() {
        Store store = new Store();
        UUID guid = store.getGuid();

        store.afterDelete();

        assertEquals(guid, store.getGuid());
    }

    @Test
    @DisplayName("lock marks the object as locked")
    void lockTest() {
        Store store = new Store();

        store.lock();

        assertTrue(store.isLocked());
    }

    @Test
    @DisplayName("unlock clears the object locked state")
    void unlockTest() {
        Store store = new Store();
        store.lock();

        store.unlock();

        assertFalse(store.isLocked());
    }

    @Test
    @DisplayName("New objects are not locked by default")
    void isLockedTest() {
        assertFalse(new Store().isLocked());
    }

    @Test
    @DisplayName("find returns the first related object matching a path value")
    void findTest() {
        Store store = new Store(1);
        Register register = new Register(2);
        register.setCode("R1");
        Register register2 = new Register(3);
        register2.setCode("R2");
        store.getRegisters().add(register);
        store.getRegisters().add(register2);

        assertSame(register2, store.find(Store.P_Registers + "." + Register.P_Code, "R2"));
    }

    @Test
    @DisplayName("findAll returns every related object matching a path value")
    void findAllTest() {
        Store store = new Store(1);
        Register register = new Register(2);
        register.setCode("R1");
        Register register2 = new Register(3);
        register2.setCode("R2");
        store.getRegisters().add(register);
        store.getRegisters().add(register2);

        Object[] found = store.findAll(Store.P_Registers + "." + Register.P_Code, "R1");

        assertEquals(1, found.length);
        assertSame(register, found[0]);
    }

    @Test
    @DisplayName("getNull reports true after a property is explicitly nulled")
    void getNullTest() {
        Store store = new Store();
        store.setName("North");

        store.setNull(Store.P_Name);

        assertTrue(store.getNull(Store.P_Name));
    }

    @Test
    @DisplayName("isNull reports true after a property is explicitly nulled")
    void isNullTest() {
        Store store = new Store();
        store.setName("North");

        store.setNull(Store.P_Name);

        assertTrue(store.isNull(Store.P_Name));
    }

    @Test
    @DisplayName("Objects created in a local test runtime are not remote-thread objects")
    void isRemoteThreadTest() {
        assertFalse(new Store().isRemoteThread());
    }

    @Test
    @DisplayName("sendMessages accepts both enabled and disabled states in a local runtime")
    void sendMessagesTest() {
        Store store = new Store();

        assertDoesNotThrow(() -> store.sendMessages(false));
        assertDoesNotThrow(() -> store.sendMessages(true));
    }

    @Test
    @DisplayName("afterLoad can be invoked for an in-memory object without changing identity")
    void afterLoadTest() {
        Store store = new Store();
        UUID guid = store.getGuid();

        store.afterLoad();

        assertEquals(guid, store.getGuid());
    }

    @Test
    @DisplayName("getObjectKey returns an identity key for a constructed object")
    void getObjectKeyTest() {
        assertNotNull(new Store(1).getObjectKey());
    }

    @Test
    @DisplayName("Every OAObject is assigned a runtime GUID")
    void getGuidTest() {
        assertNotNull(new Store().getGuid());
    }

    @Test
    @DisplayName("setAutoAdd controls automatic-add behavior")
    void setAutoAddTest() {
        Store store = new Store();

        store.setAutoAdd(false);
        assertFalse(store.getAutoAdd());
        store.setAutoAdd(true);
        assertTrue(store.getAutoAdd());
    }

    @Test
    @DisplayName("getAutoAdd reports the current automatic-add flag")
    void getAutoAddTest() {
        Store store = new Store();

        store.setAutoAdd(false);

        assertFalse(store.getAutoAdd());
    }

    @Test
    @DisplayName("isEmpty treats an empty String as empty and non-empty content as not empty")
    void isEmptyTest() {
        Store store = new Store();

        assertTrue(store.isEmpty(""));
        assertFalse(store.isEmpty("x"));
    }

    @Test
    @DisplayName("Generated many-Hub access marks the Hub reference as loaded")
    void isHubLoadedTest() {
        Store store = new Store(1);

        store.getRegisters();

        assertTrue(store.isHubLoaded(Store.P_Registers));
    }

    @Test
    @DisplayName("loadReferences accepts include-calc-only traversal for in-memory graphs")
    void loadReferencesTest() {
        // OAObject.loadReferences(boolean bIncludeCalc)
        Store store = new Store(1);
        store.getRegisters().add(new Register(2));

        assertDoesNotThrow(() -> store.loadReferences(false));
    }

    @Test
    @DisplayName("loadReferences accepts explicit one/many traversal options for in-memory graphs")
    void loadReferencesOneManyTest() {
        // OAObject.loadReferences(boolean bOne, boolean bMany, boolean bIncludeCalc)
        Store store = new Store(1);
        store.getRegisters().add(new Register(2));

        assertDoesNotThrow(() -> store.loadReferences(true, true, false));
    }

    @Test
    @DisplayName("loadReferences accepts level-limited traversal for in-memory graphs")
    void loadReferencesLevelsTest() {
        // OAObject.loadReferences(int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc)
        Store store = new Store(1);
        store.getRegisters().add(new Register(2));

        assertDoesNotThrow(() -> store.loadReferences(1, 1, false));
    }

    @Test
    @DisplayName("loadReferences accepts reference-limited traversal for in-memory graphs")
    void loadReferencesMaxRefsTest() {
        // OAObject.loadReferences(int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc, int maxRefsToLoad)
        Store store = new Store(1);
        store.getRegisters().add(new Register(2));

        assertDoesNotThrow(() -> store.loadReferences(1, 1, false, 10));
    }

    @Test
    @DisplayName("callRemote returns null when no Hub context is supplied")
    void callRemoteTest() {
        assertNull(OAObject.callRemote(null, "x"));
    }

    @Test
    @DisplayName("remote throws when no remote runtime is available")
    void remoteTest() {
        Store store = new Store();

        assertThrows(RuntimeException.class, () -> store.remote("x"));
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.isUnique")
    void isUniqueTest() {
        // OAObject.isUnique(String property, Object value)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @DisplayName("getUniqueInstance returns null when auto-create is disabled and no match exists")
    void getUniqueInstanceTest() {
        assertNull(OAObject.getUniqueInstance(Store.class, Store.P_StoreNumber, 999, false));
    }

    @Test
    @DisplayName("getUniqueInstance can create a missing unique instance")
    void getUniqueInstanceAutoCreateTest() {
        OAObject oaObject = OAObject.getUniqueInstance(Store.class, Store.P_StoreNumber, 999, true);

        assertTrue(oaObject instanceof Store);
        assertEquals(999, ((Store) oaObject).getStoreNumber());
    }

    @Test
    @DisplayName("Object remote invocation is unavailable without a sync client")
    void isRemoteAvailableTest() {
        assertFalse(new Store().isRemoteAvailable());
    }

    @Test
    @DisplayName("Hub remote invocation is unavailable without a sync client")
    void isRemoteAvailableHubTest() {
        // OAObject.isRemoteAvailable(Hub hub)
        Hub<Store> hubStore = new Hub<>(Store.class);

        assertFalse(OAObject.isRemoteAvailable(hubStore));
    }

    @Test
    @DisplayName("A populated one-link reports as loaded")
    void isLoadedTest() {
        Store store = new Store(1);
        Register register = new Register(2);

        register.setStore(store);

        assertTrue(register.isLoaded(Register.P_Store));
    }

    @Test
    @DisplayName("isLoaded reports true for a materialized one-link reference")
    void isLoaded_withLoadedReferenceTest() {
        // OAObject.isLoaded(String prop)
        Store store = new Store(1);
        Register register = new Register(2);
        register.setStore(store);

        assertTrue(register.isLoaded(Register.P_Store));
    }

    @Test
    @Disabled("TODO - deterministic unloaded-reference fixture required")
    @DisplayName("TODO: isLoaded reports false before an object-key reference is materialized")
    void isLoaded_withUnloadedReferenceTest() {
        // Matrix classification: REQUIRED
        // Evidence level: STRONGLY_INFERRED
        // Required fixture: one-link represented by foreign-key/object-key state only.
    }

    @Test
    @DisplayName("An unset scalar property reports as not loaded")
    void isPropertyLoadedTest() {
        assertFalse(new Store().isPropertyLoaded(Store.P_Name));
    }

    @Test
    @DisplayName("A populated one-link reference is not null")
    void isReferenceNullTest() {
        Store store = new Store(1);
        Register register = new Register(2);

        register.setStore(store);

        assertFalse(register.isReferenceNull(Register.P_Store));
    }

    @Test
    @DisplayName("hierFind walks a recursive parent path and returns the first non-empty property value")
    void hierFindTest() {
        // OAObject.hierFind(String propertyName, String heirarchyPath)
        RecursiveCatalogCategoryScenario scenario = createRecursiveCatalogCategoryScenario();
        scenario.catalogCategory3.setName(null);

        Object value = scenario.catalogCategory3.hierFind(CatalogCategory.P_Name, CatalogCategory.P_ParentCatalogCategory);

        assertEquals("Child", value);
        assertSame(scenario.catalogCategory2, scenario.catalogCategory3.getParentCatalogCategory());
        assertTrue(scenario.catalogCategory2.getCatalogCategories().contains(scenario.catalogCategory3));
    }

    @Test
    @DisplayName("getReferenceObjectKey returns the key for a populated one-link reference")
    void getReferenceObjectKeyTest() {
        Store store = new Store(1);
        Register register = new Register(2);

        register.setStore(store);

        assertEquals(store.getObjectKey(), register.getReferenceObjectKey(Register.P_Store));
    }

    @Test
    @DisplayName("startServerOnly returns false in a local unit-test runtime")
    void startServerOnlyTest() {
        assertFalse(new Store().startServerOnly());
    }

    @Test
    @DisplayName("endServerOnly can be called after an unsuccessful server-only start")
    void endServerOnlyTest() {
        Store store = new Store();

        assertDoesNotThrow(store::endServerOnly);
    }

    @Test
    @DisplayName("runOnServerOnly does not run locally when server-only mode is unavailable")
    void runOnServerOnlyTest() {
        Store store = new Store();
        AtomicInteger calls = new AtomicInteger();

        store.runOnServerOnly(calls::incrementAndGet);

        assertEquals(0, calls.get());
    }

    @Test
    @DisplayName("setDebugMode controls the static debug flag")
    void setDebugModeTest() {
        OAObject.setDebugMode(true);

        assertTrue(OAObject.getDebugMode());
    }

    @Test
    @DisplayName("getDebugMode returns the current static debug flag")
    void getDebugModeTest() {
        OAObject.setDebugMode(true);

        assertTrue(OAObject.getDebugMode());
    }

    @Test
    @DisplayName("Properties are not locked by default")
    void isPropertyLockedTest() {
        assertFalse(new Store().isPropertyLocked(Store.P_Name));
    }

    @Test
    @DisplayName("New in-memory objects are submitted by default")
    void isSubmittedTest() {
        assertTrue(new Store().isSubmitted());
    }

    @Test
    @DisplayName("Submitted recursion helper returns true for bounded recursion counts")
    void _isSubmittedTest() {
        assertTrue(new Store()._isSubmitted(11));
    }

    @Test
    @DisplayName("compareAndSwap updates a property only when its current value matches the expected value")
    void compareAndSwapTest() {
        // OAObject.compareAndSwap(String property, Object oldValue, Object newValue)
        Store store = new Store();
        store.setName("Old");

        assertTrue(store.compareAndSwap(Store.P_Name, "Old", "New"));
        assertEquals("New", store.getName());
    }

    @Test
    @DisplayName("compareAndSwap with distributed locking does not update when the expected value differs")
    void compareAndSwapDistributedLockTest() {
        // OAObject.compareAndSwap(String property, Object oldValue, Object newValue, final boolean bUseDistributedLock)
        Store store = new Store();
        store.setName("Old");

        assertFalse(store.compareAndSwap(Store.P_Name, "Wrong", "New", false));
        assertEquals("Old", store.getName());
    }

    @Test
    @DisplayName("compareAndSwap rejects missing property names")
    void compareAndSwapInvalidPropertyTest() {
        Store store = new Store();
        store.setName("Old");

        assertFalse(store.compareAndSwap(null, "Old", "New", false));
        assertFalse(store.compareAndSwap("", "Old", "New", false));
        assertEquals("Old", store.getName());
    }

    @Test
    @DisplayName("setObjectDefaults can initialize generated default values")
    void setObjectDefaultsTest() {
        Store store = new Store();

        store.setObjectDefaults();

        assertNotNull(store.getCreated());
    }

    @Test
    @DisplayName("setFkeyProperty updates a foreign-key scalar for a one-link")
    void setFkeyPropertyTest() {
        // OAObject.setFkeyProperty(final String fkeyPropertyName, final Object newValue)
        Store store = new Store(44);
        Store storeNew = new Store(55);
        Register register = new Register(2);
        register.setStore(store);

        assertTrue(register.setFkeyProperty(Register.P_StoreId, 55));

        assertEquals(55, register.getFkeyProperty(Register.P_StoreId));
        assertSame(storeNew, register.getStore());
    }


    @Test
    @DisplayName("setFkeyProperty rejects a missing foreign-key property name")
    void setFkeyPropertyMissingNameTest() {
        // OAObject.setFkeyProperty(final String fkeyPropertyName, final Object newValue)
        Register register = new Register(2);

        assertFalse(register.setFkeyProperty(null, 1));
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.setFkeyProperty")
    void setFkeyPropertyLinkInfoTest() {
        // OAObject.setFkeyProperty(final String fkeyPropertyName, final OALinkInfo linkInfo, final OAFkeyInfo fi, Object newValue)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @DisplayName("getFkeyProperty returns the scalar foreign-key value for a one-link")
    void getFkeyPropertyTest() {
        // OAObject.getFkeyProperty(final String fkeyPropertyName)
        Store store = new Store(44);
        Register register = new Register(2);
        register.setStore(store);

        assertEquals(44, register.getFkeyProperty(Register.P_StoreId));
    }


    @Test
    @DisplayName("getFkeyProperty returns null for a missing foreign-key property name")
    void getFkeyPropertyMissingNameTest() {
        // OAObject.getFkeyProperty(final String fkeyPropertyName)
        Register register = new Register(2);

        assertNull(register.getFkeyProperty("missing"));
    }

    @Test
    @DisplayName("getFkeyProperty can resolve a foreign-key value through a link name")
    void getFkeyPropertyLinkNameTest() {
        // OAObject.getFkeyProperty(final String linkName, String linkToPropertyName)
        Store store = new Store(44);
        Register register = new Register(2);
        register.setStore(store);

        assertEquals(44, register.getFkeyProperty(Register.P_Store, Store.P_Id));
    }


    @Test
    @DisplayName("getFkeyProperty returns null for a missing link name")
    void getFkeyPropertyMissingLinkNameTest() {
        // OAObject.getFkeyProperty(final String linkName, String linkToPropertyName)
        Register register = new Register(2);

        assertNull(register.getFkeyProperty("", Store.P_Id));
    }

    @Test
    @DisplayName("getFkeyProperty throws when a known link uses an unknown target property")
    void getFkeyPropertyMissingLinkToPropertyTest() {
        // OAObject.getFkeyProperty(final String linkName, String linkToPropertyName)
        Store store = new Store(44);
        Register register = new Register(2);
        register.setStore(store);

        assertThrows(RuntimeException.class, () -> register.getFkeyProperty(Register.P_Store, "missing"));
    }

    @Test
    @DisplayName("refresh can be called for a new object without a datasource")
    void refreshTest() {
        // OAObject.refresh()
        Store store = new Store(1);

        assertDoesNotThrow(() -> store.refresh());
    }

    @Test
    @DisplayName("refresh accepts a link property name for a new object without a datasource")
    void refreshLinkPropertyTest() {
        // OAObject.refresh(String linkPropertyName)
        Store store = new Store(1);

        assertDoesNotThrow(() -> store.refresh(Store.P_Registers));
    }


    @Test
    @DisplayName("refresh accepts an unknown link property name without failing for a new object")
    void refreshMissingLinkPropertyTest() {
        // OAObject.refresh(String linkPropertyName)
        Store store = new Store(1);

        assertDoesNotThrow(() -> store.refresh("missing"));
    }

    @Test
    @DisplayName("getNameValues returns generated enum values for name-value properties")
    void getNameValuesTest() {
        Customer customer = new Customer();

        Hub<VEnum> hubVEnum = customer.getNameValues(Customer.P_Type);

        assertNotNull(hubVEnum);
        assertTrue(hubVEnum.getSize() > 0);
    }

    @Test
    @DisplayName("getFriendAccess returns the singleton internal friend-access bridge")
    void getFriendAccessTest() {
        assertSame(OAObject.getFriendAccess(), OAObject.getFriendAccess());
    }

    @Test
    @DisplayName("FriendAccess getGuid reads the object runtime GUID")
    void friendAccessGetGuidTest() {
        Store store = new Store();
        OAObject.FriendAccess friendAccess = new OAObjectInternalBridge().getObjectFriendAccess();

        assertEquals(store.getGuid(), friendAccess.getGuid(store));
    }

    @Test
    @DisplayName("FriendAccess setGuid assigns a GUID when the object has no GUID")
    void friendAccessSetGuidTest() {
        Store store = new Store();
        OAObject.FriendAccess friendAccess = new OAObjectInternalBridge().getObjectFriendAccess();
        UUID guid = UUID.randomUUID();

        friendAccess.setGuid(store, null);
        friendAccess.setGuid(store, guid);

        assertEquals(guid, friendAccess.getGuid(store));
    }

    @Test
    @DisplayName("FriendAccess isNew reads the raw new flag")
    void friendAccessIsNewTest() {
        Store store = new Store();
        OAObject.FriendAccess friendAccess = new OAObjectInternalBridge().getObjectFriendAccess();

        assertTrue(friendAccess.isNew(store));
    }

    @Test
    @DisplayName("FriendAccess getNewFlag reads the raw new flag")
    void friendAccessGetNewFlagTest() {
        Store store = new Store();
        OAObject.FriendAccess friendAccess = new OAObjectInternalBridge().getObjectFriendAccess();

        assertTrue(friendAccess.getNewFlag(store));
    }

    @Test
    @DisplayName("FriendAccess getDeleteFlag reads the raw deleted flag")
    void friendAccessGetDeleteFlagTest() {
        Store store = new Store();
        OAObject.FriendAccess friendAccess = new OAObjectInternalBridge().getObjectFriendAccess();

        assertFalse(friendAccess.getDeleteFlag(store));
    }

    @Test
    @DisplayName("FriendAccess setDeletedFlag writes the raw deleted flag")
    void friendAccessSetDeletedFlagTest() {
        Store store = new Store();
        OAObject.FriendAccess friendAccess = new OAObjectInternalBridge().getObjectFriendAccess();

        friendAccess.setDeletedFlag(store, true);

        assertTrue(friendAccess.getDeleteFlag(store));
    }

    @Test
    @DisplayName("FriendAccess setNew writes the raw new flag")
    void friendAccessSetNewTest() {
        Store store = new Store();
        OAObject.FriendAccess friendAccess = new OAObjectInternalBridge().getObjectFriendAccess();

        friendAccess.setNew(store, false);

        assertFalse(friendAccess.getNewFlag(store));
    }

    @Test
    @DisplayName("FriendAccess getNulls reads primitive-null tracking bytes")
    void friendAccessGetNullsTest() {
        Store store = new Store();
        OAObject.FriendAccess friendAccess = new OAObjectInternalBridge().getObjectFriendAccess();
        byte[] nulls = new byte[] { 1 };
        friendAccess.setNulls(store, nulls);

        assertArrayEquals(nulls, friendAccess.getNulls(store));
    }

    @Test
    @DisplayName("FriendAccess setNulls writes primitive-null tracking bytes")
    void friendAccessSetNullsTest() {
        Store store = new Store();
        OAObject.FriendAccess friendAccess = new OAObjectInternalBridge().getObjectFriendAccess();
        byte[] nulls = new byte[] { 1 };

        friendAccess.setNulls(store, nulls);

        assertArrayEquals(nulls, friendAccess.getNulls(store));
    }

    @Test
    @DisplayName("FriendAccess getChangedFlag reads the raw changed flag")
    void friendAccessGetChangedFlagTest() {
        Store store = new Store();
        OAObject.FriendAccess friendAccess = new OAObjectInternalBridge().getObjectFriendAccess();

        assertFalse(friendAccess.getChangedFlag(store));
    }

    @Test
    @DisplayName("FriendAccess setChangedFlag writes the raw changed flag")
    void friendAccessSetChangedFlagTest() {
        Store store = new Store();
        OAObject.FriendAccess friendAccess = new OAObjectInternalBridge().getObjectFriendAccess();

        friendAccess.setChangedFlag(store, false);

        assertFalse(friendAccess.getChangedFlag(store));
    }

    @Test
    @DisplayName("FriendAccess getWeakHubs reads weak Hub references")
    @SuppressWarnings("unchecked")
    void friendAccessGetWeakHubsTest() {
        Store store = new Store();
        OAObject.FriendAccess friendAccess = new OAObjectInternalBridge().getObjectFriendAccess();
        WeakReference<Hub<?>>[] weakHubRefs = new WeakReference[] { new WeakReference<Hub<?>>(new Hub<>(Store.class)) };
        friendAccess.setWeakHubs(store, weakHubRefs);

        assertSame(weakHubRefs, friendAccess.getWeakHubs(store));
    }

    @Test
    @DisplayName("FriendAccess setWeakHubs writes weak Hub references")
    @SuppressWarnings("unchecked")
    void friendAccessSetWeakHubsTest() {
        Store store = new Store();
        OAObject.FriendAccess friendAccess = new OAObjectInternalBridge().getObjectFriendAccess();
        WeakReference<Hub<?>>[] weakHubRefs = new WeakReference[] { new WeakReference<Hub<?>>(new Hub<>(Store.class)) };

        friendAccess.setWeakHubs(store, weakHubRefs);

        assertSame(weakHubRefs, friendAccess.getWeakHubs(store));
    }

    @Test
    @DisplayName("FriendAccess getProperties reads the internal property array")
    void friendAccessGetPropertiesTest() {
        Store store = new Store();
        OAObject.FriendAccess friendAccess = new OAObjectInternalBridge().getObjectFriendAccess();
        Object[] props = new Object[] { "x" };
        friendAccess.setProperties(store, props);

        assertArrayEquals(props, friendAccess.getProperties(store));
    }

    @Test
    @DisplayName("FriendAccess setProperties writes the internal property array")
    void friendAccessSetPropertiesTest() {
        Store store = new Store();
        OAObject.FriendAccess friendAccess = new OAObjectInternalBridge().getObjectFriendAccess();
        Object[] props = new Object[] { "x" };

        friendAccess.setProperties(store, props);

        assertArrayEquals(props, friendAccess.getProperties(store));
    }

    @Test
    @Disabled("TODO - invariant needs to be defined")
    @DisplayName("TODO: define the invariant for OAObject.FriendAccess.firePropertyChange")
    void friendAccessFirePropertyChangeTest() {
        // OAObject.FriendAccess.firePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj)
        // TODO: define arrange, act, and assertions.
    }

    @Test
    @DisplayName("getOA returns the runtime that owns the object")
    void getOATest() {
        assertSame(OARuntime.oa(Store.class), new Store().getOA());
    }
}
