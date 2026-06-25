package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.Store;
import com.viaoa.oa.OA;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

class HubTest {
    @BeforeEach
    void beforeEach() {
        OA oa = OARuntime.oa(Register.class);
    }
    @AfterEach
    void afterEach() {
        OAObject.setDebugMode(false);
        OARuntime.oa(Register.class).close();
    }

    private static Register register(int id, String code) {
        Register r = new Register();
        r.setId(id);
        r.setCode(code);
        return r;
    }

    @Test
    void constructorsCreateTypedAndPopulatedHubs() {
        Hub<Register> hub = new Hub<>(Register.class);
        assertEquals(Register.class, hub.getObjectClass());
        assertTrue(hub.isEmpty());

        Register r = register(1, "R1");
        Hub<Register> one = new Hub<>(r);
        assertEquals(Register.class, one.getObjectClass());
        assertEquals(1, one.size());
        assertSame(r, one.getAt(0));
        assertSame(r, one.getAO());

        Hub<Register> sized = new Hub<>(Register.class, 2, 1);
        sized.ensureCapacity(4);
        sized.resizeToFit();
        assertEquals(Register.class, sized.getObjectClass());
    }

    @Test
    void propertiesAndStringHelpersStoreDynamicValues() {
        Hub<Register> hub = new Hub<>(Register.class);
        hub.setProperty("label", "registers");

        assertEquals("registers", hub.getProperty("label"));
        assertTrue(hub.toString().contains("Register"));

        hub.removeProperty("label");
        assertNull(hub.getProperty("label"));
    }

    @Test
    void addInsertMoveSwapReplaceRemoveAndClearMaintainOrderAndActiveObject() {
        Hub<Register> hub = new Hub<>(Register.class);
        Register r1 = register(1, "A");
        Register r2 = register(2, "B");
        Register r3 = register(3, "C");

        assertTrue(hub.add(r1));
        hub.addElement(r3);
        hub.insert(r2, 1);

        assertEquals(List.of(r1, r2, r3), hub.toList());
        assertTrue(hub.contains(r2));
        assertEquals(1, hub.indexOf(r2));
        assertSame(r3, hub.getLast());

        hub.move(0, 2);
        assertEquals(List.of(r2, r3, r1), hub.toList());

        hub.swap(0, 1);
        assertEquals(List.of(r3, r2, r1), hub.toList());

        Register r4 = register(4, "D");
        hub.replace(1, r4);
        assertEquals(List.of(r3, r4, r1), hub.toList());

        hub.setPos(1);
        assertSame(r4, hub.getActiveObject());
        hub.setAO(r1);
        assertEquals(2, hub.getPos());
        hub.resetAO();
        assertEquals(-1, hub.getPos());

        assertSame(r3, hub.removeAt(0));
        assertTrue(hub.remove(r4));
        assertEquals(List.of(r1), hub.toList());

        hub.clear();
        assertTrue(hub.isEmpty());
    }

    @Test
    void collectionViewsAndArrayMethodsReflectHubContents() {
        Hub<Register> hub = new Hub<>(Register.class);
        Register r1 = register(1, "A");
        Register r2 = register(2, "B");
        hub.addAll(Arrays.asList(r1, r2));

        assertArrayEquals(new Object[] { r1, r2 }, hub.toArray());
        assertArrayEquals(new Register[] { r1, r2 }, hub.toArray(new Register[0]));

        Register[] copied = new Register[2];
        hub.copyInto(copied);
        assertArrayEquals(new Register[] { r1, r2 }, copied);

        Hub<Register> copy = new Hub<>(Register.class);
        hub.copyInto(copy);
        assertEquals(hub.toList(), copy.toList());

        Iterator<Register> iterator = hub.iterator();
        assertSame(r1, iterator.next());
        assertEquals(List.of(r1), hub.subList(0, 1));
        assertEquals(2, hub.stream().count());
    }

    @Test
    void sharedHubCanShareOrIsolateActiveObject() {
        Hub<Register> hub = new Hub<>(Register.class);
        Register r1 = register(1, "A");
        Register r2 = register(2, "B");
        hub.add(r1);
        hub.add(r2);

        Hub<Register> shared = hub.createSharedHub(true);
        assertEquals(hub.toList(), shared.toList());

        hub.setAO(r2);
        assertSame(r2, shared.getAO());

        Hub<Register> isolated = hub.createSharedHub(false);
        isolated.setAO(r1);
        assertSame(r2, hub.getAO());
        assertSame(r1, isolated.getAO());
    }

    @Test
    void masterDetailHubFromOaObjectRelationshipTracksOwnerAndReverseLink() {
        Store store = new Store();
        Register r1 = register(1, "A");
        Register r2 = register(2, "B");

        Hub<Register> registers = store.getRegisters();
        registers.add(r1);
        registers.add(r2);

        assertSame(store, registers.getMasterObject());
        assertEquals(Store.class, registers.getMasterClass());
        assertSame(store, r1.getStore());
        assertSame(registers, r2.getStore().getRegisters());
    }

    @Test
    void listenersAndConvenienceCallbacksFireForHubAndPropertyChanges() {
        Hub<Register> hub = new Hub<>(Register.class);
        Register r1 = register(1, "A");
        AtomicInteger adds = new AtomicInteger();
        AtomicInteger removes = new AtomicInteger();
        AtomicInteger aos = new AtomicInteger();
        AtomicInteger props = new AtomicInteger();

        hub.onAdd(e -> adds.incrementAndGet());
        hub.onRemove(e -> removes.incrementAndGet());
        hub.onChangeAO(e -> aos.incrementAndGet());
        hub.onPropertyChange(e -> props.incrementAndGet(), Register.P_Code);

        hub.add(r1);
        hub.setAO(r1);
        r1.setCode("A2");
        hub.remove(r1);

        assertEquals(1, adds.get());
        assertEquals(1, removes.get());
        assertEquals(1, aos.get());
        assertEquals(1, props.get());
    }

    @Test
    void sortFindAndSelectConfigurationUseOaProperties() {
        Hub<Register> hub = new Hub<>(Register.class);
        Register r1 = register(1, "B");
        Register r2 = register(2, "A");
        hub.add(r1);
        hub.add(r2);

        hub.sort(Register.P_Code);
        assertEquals(List.of(r2, r1), hub.toList());
        assertSame(r1, hub.find(Register.P_Code, "B"));

        hub.setSelectWhere("code = ?");
        hub.setSelectOrder(Register.P_Code);
        assertEquals("code = ?", hub.getSelectWhere());
        assertEquals(Register.P_Code, hub.getSelectOrder(hub));
        hub.cancelSelect();
    }

    @Test
    void stateFlagsAndGraphAccessAreStable() {
        Hub<Register> hub = new Hub<>(Register.class);

        assertTrue(hub.isValid());
        assertFalse(hub.isLoading());
        hub.setLoading(true);
        assertTrue(hub.isLoading());
        hub.setLoading(false);

        assertNotNull(hub.getOA());
    }
}
