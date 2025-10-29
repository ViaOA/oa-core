package com.viaoa.object;

import org.junit.Test;
import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;
import com.viaoa.util.OACompare;

import test.xice.tsac3.model.oa.Server;

public class OAObjectKeyTest extends OAUnitTest {

    @Test
    public void objectIdTest() {
        reset();
        Server server = new Server();
        OAObjectKey key = server.getObjectKey();
        
        server.setId(1);
        OAObjectKey key2 = server.getObjectKey();
        
        assertEquals(key.getGuid(), key2.getGuid());
        
        // assertFalse(key.equals(key2));
        
        // clean up
        reset();
    }
    
    
    @Test
    public void newTest() {
        reset();
        OAObjectKey key = new OAObjectKey(null, 12);
        OAObjectKey key2 = new OAObjectKey(null, 12);
        assertEquals(key, key2);
        
        key2 = new OAObjectKey(null, 12);
        assertEquals(key, key2);

        key2 = new OAObjectKey(null, 13);
        assertTrue(!key.equals(key2));
        reset();
    }    

    @Test
    public void idTest() {
        reset();
        OAObjectKey key = new OAObjectKey(1);
        OAObjectKey key2 = new OAObjectKey(1);
        assertEquals(key, key2);

        key2 = new OAObjectKey(new Object[]{1}, 14);
        assertNotEquals(key, key2);

        assertEquals(0, OACompare.compare(key, key2));
        
        reset();
    }
    
    @Test
    public void moreThenOneIdTest() {
        reset();
        OAObjectKey key = new OAObjectKey(new Object[] {1, 2, 3});
        
        Object[] objs = key.getObjectIds();
        assertTrue(objs.length == 3 && objs[0].equals(Integer.valueOf(1)) && objs[1].equals(Integer.valueOf(2)) && objs[2].equals(Integer.valueOf(3)));
        assertEquals(key.getGuid(), 0);
        
        OAObjectKey key2 = new OAObjectKey(new Object[] {1, 2, 3});
        assertTrue(key.equals(key2));
        assertEquals(key.getGuid(), 0);
        reset();
    }
    
}
