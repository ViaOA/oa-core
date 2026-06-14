package com.viaoa.datasource.autonumber;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.Store;
import com.viaoa.datasource.OADataSourceIterator;
import com.viaoa.hub.Hub;

class OADataSourceAutoTest {

    @AfterEach
    void afterEach() {
        OADataSourceAuto.setGlobalNextNumbers(null);
    }

    @Test
    void constructorsConfigureLastFlagAndOptionalHub() {
        Hub<NextNumber> hub = new Hub<>(NextNumber.class);

        assertTrue(new OADataSourceAuto().getLast());
        assertFalse(new OADataSourceAuto(false).getLast());
        assertSame(hub, new OADataSourceAuto(hub, false).getNextNumbers());
        assertSame(hub, new OADataSourceAuto(hub).getNextNumbers());
    }

    @Test
    void getNextNumbersUsesGlobalHubWhenConfigured() {
        Hub<NextNumber> hub = new Hub<>(NextNumber.class);
        OADataSourceAuto.setGlobalNextNumbers(hub);

        assertSame(hub, new OADataSourceAuto(false).getNextNumbers());
        assertSame(hub, OADataSourceAuto.getGlobalNextNumbers());
    }

    @Test
    void startingNextNumberRoundTripsAndControlsAssignedIds() {
        OADataSourceAuto ds = new OADataSourceAuto(new Hub<>(NextNumber.class), false);
        Register register = new Register();

        ds.setStartingNextNumber(100);
        ds.assignId(register);

        assertEquals(100, ds.getStartingNextNumber());
        assertEquals(100, register.getId());
    }

    @Test
    void supportsStorageReturnsFalse() {
        assertFalse(new OADataSourceAuto(false).supportsStorage());
    }

    @Test
    void supportAllClassesControlsClassSupport() {
        OADataSourceAuto ds = new OADataSourceAuto(new Hub<>(NextNumber.class), false);

        assertTrue(ds.getSupportAllClasses());
        assertTrue(ds.isClassSupported(Register.class, null));
        assertFalse(ds.isClassSupported(null, null));
        assertTrue(ds.isClassSupported(NextNumber.class, null));
        ds.setSupportAllClasses(false);
        assertFalse(ds.getSupportAllClasses());
        assertFalse(ds.isClassSupported(Store.class, null));
    }

    @Test
    void assignIdIgnoresNullAndClassesWithoutSupportedSequence() {
        OADataSourceAuto ds = new OADataSourceAuto(new Hub<>(NextNumber.class), false);
        ds.setSupportAllClasses(false);
        Register register = new Register();

        assertDoesNotThrow(() -> ds.assignId(null));
        ds.assignId(register);

        assertEquals(0, register.getId());
    }

    @Test
    void willCreatePropertyValueUsesAutonumberPropertyCaseInsensitively() {
        OADataSourceAuto ds = new OADataSourceAuto(new Hub<>(NextNumber.class), false);
        Register register = new Register();

        assertTrue(ds.willCreatePropertyValue(register, Register.P_Id.toUpperCase()));
        assertFalse(ds.willCreatePropertyValue(register, Register.P_Code));
        assertFalse(ds.willCreatePropertyValue(null, Register.P_Id));
        assertFalse(ds.willCreatePropertyValue(register, null));
    }

    @Test
    void insertAndInsertWithoutReferencesAssignIdsWhenCreateAssignmentIsDisabled() {
        OADataSourceAuto ds = new OADataSourceAuto(new Hub<>(NextNumber.class), false);
        ds.setStartingNextNumber(20);
        Register one = new Register();
        Register two = new Register();

        ds.insert(one);
        ds.insertWithoutReferences(two);

        assertEquals(20, one.getId());
        assertEquals(21, two.getId());
    }

    @Test
    void unsupportedOperationsAreNoOpsOrReturnDefaults() {
        OADataSourceAuto ds = new OADataSourceAuto(new Hub<>(NextNumber.class), false);

        assertDoesNotThrow(() -> ds.updateMany2ManyLinks(null, null, null, null));
        assertDoesNotThrow(() -> ds.update(new Register(), null, null));
        assertDoesNotThrow(() -> ds.delete(new Register()));
        assertNull(ds.execute("x"));
        assertNull(ds.getPropertyBlobValue(new Register(), Register.P_Code));
        assertEquals(-1, ds.count(Register.class, null, null, null, null, null, 0));
        assertEquals(-1, ds.countPassthru(Register.class, null, 0));
        OADataSourceIterator it = ds.select(Register.class, null, null, null, null, null, null, 0, null, false);
        assertNull(it);
        assertNull(ds.selectPassthru(Register.class, null, null, 0, null, false));
    }
}
