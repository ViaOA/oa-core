package com.viaoa.metadata.pojo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Store;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.metadata.OAPropertyInfo;

class OAObjectPojoLoaderTest {
    @Test
    void constructorAndLoadIntoPojoCreatePojoMetadataFromObjectInfo() {
        OAObjectPojoLoader loader = new OAObjectPojoLoader();
        OAObjectInfo info = new OAObjectInfo(Store.P_Id);
        info.setForClass(Store.class);
        info.setName(Store.class.getSimpleName());
        OAPropertyInfo id = new OAPropertyInfo();
        id.setName(Store.P_Id);
        id.setClassType(Integer.class);
        id.setId(true);
        info.addPropertyInfo(id);
        OAPropertyInfo name = new OAPropertyInfo();
        name.setName(Store.P_Name);
        name.setClassType(String.class);
        info.addPropertyInfo(name);
        Pojo pojo = loader.loadIntoPojo(info);

        assertNotNull(pojo);
        assertEquals(Store.class.getSimpleName(), pojo.getName());
        assertFalse(pojo.getPojoRegularProperties().isEmpty());
        assertNotNull(PojoDelegate.getPojoRegularProperty(pojo, Store.P_Name));
    }
}
