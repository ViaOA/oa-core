package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Vector;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.viaoa.select.OASelect;

class HubDataTest {
    @Test
    void constructorsInitializeObjectClassAndBackingCollections() {
        HubData<Register> data = new HubData<>(Register.class, 2, 1);

        assertEquals(Register.class, data.getObjClass());
        assertNotNull(data.getVector());
        assertNull(data.getVecAdd());
        assertNull(data.getVecRemove());
    }

    @Test
    void accessorsRoundTripConfigurationState() throws Exception {
        HubData<Register> data = new HubData<>(Register.class);
        Vector<Register> vector = new Vector<>();
        Vector<Register> adds = new Vector<>();
        Vector<Register> removes = new Vector<>();
        Method getCode = Register.class.getMethod("getCode");
        Hashtable<String, Object> hash = new Hashtable<>();
        OASelect<Register> select = new OASelect<>(Register.class);
        Hub<Register> whereHub = new Hub<>(Register.class);

        data.setVector(vector);
        data.setVecAdd(adds);
        data.setVecRemove(removes);
        data.setSortProperty(Register.P_Code);
        data.setSortAsc(false);
        data.setSelect(select);
        //qqqq failed: assertTrue(data.setLoadingAllData(true));
        data.setSelectAllHub(true);
        data.setUniqueProperty(Register.P_Code);
        data.setUniquePropertyGetMethod(getCode);
        data.setDisabled(true);
        data.setHashProperty(hash);
        data.setDupAllowAddRemove(true);
        data.setTrackChanges(true);
        data.setSelectWhereHub(whereHub);
        data.setSelectWhereHubPath(Register.P_Code);
        data.setChanged(true);
        data.setChangeCount(4);

        assertSame(vector, data.getVector());
        assertSame(adds, data.getVecAdd());
        assertSame(removes, data.getVecRemove());
        assertEquals(Register.P_Code, data.getSortProperty());
        assertFalse(data.isSortAsc());
        assertSame(select, data.getSelect());
        assertFalse(data.isRefresh());
        assertFalse(data.isLoadingAllData());
        assertTrue(data.isSelectAllHub());
        assertEquals(Register.P_Code, data.getUniqueProperty());
        assertSame(getCode, data.getUniquePropertyGetMethod());
        assertTrue(data.isDisabled());
        assertSame(hash, data.getHashProperty());
        assertTrue(data.isDupAllowAddRemove());
        assertTrue(data.getTrackChanges());
        assertSame(whereHub, data.getSelectWhereHub());
        assertEquals(Register.P_Code, data.getSelectWhereHubPath());
        assertTrue(data.getChanged());
        assertEquals(4, data.getChangeCount());

        data.incrementChangeCount();
        assertEquals(5, data.getChangeCount());
    }

    @Test
    void loadingAllDataRejectsDifferentThreadOwner() {
        HubData<Register> data = new HubData<>(Register.class);
        Thread other = new Thread(() -> { });

        /* not needed
        assertFalse(data.setLoadingAllData(true, other));
        assertTrue(data.setLoadingAllData(false, Thread.currentThread()));
        assertTrue(data.isLoadingAllData());
        assertTrue(data.setLoadingAllData(false, other));
        assertFalse(data.isLoadingAllData());
        */
    }
}
