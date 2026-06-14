package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.Store;

class HubDataMasterTest {
    @Test
    void masterHubMasterObjectAndLinkInfoRoundTrip() {
        HubDataMaster data = new HubDataMaster();
        Hub<Store> stores = new Hub<>(Store.class);
        Store store = new Store();
        data.setMasterHub(stores);
        data.setMasterObject(store);
        assertSame(stores, data.getMasterHub());
        assertSame(store, data.getMasterObject());
        assertNull(data.getDetailToMasterLinkInfo());
    }

    @Test
    void missingLinkInfoProducesSafeDefaults() {
        HubDataMaster data = new HubDataMaster();

        assertNull(data.getUniqueProperty());
        assertNull(data.getUniquePropertyGetMethod());
        assertFalse(data.getTrackChanges());
        assertNull(data.getSortProperty());
        assertFalse(data.isSortAsc());
        assertNull(data.getSeqProperty());
    }
}
