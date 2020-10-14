package com.viaoa.object;

import org.junit.Test;
import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;
import com.viaoa.util.OANotExist;
import com.viaoa.util.OANullObject;

import test.xice.tsac3.model.oa.*;

public class OAObjectPropertyDelegateTest extends OAUnitTest {

    @Test
    public void test() {

        reset(false);

        Server server = new Server();
        
        server.setProperty("serverId", "4");
        assertEquals(4, server.getServerId());
        assertNull(OAObjectPropertyDelegate.getProperty(server, "ServerId"));

        server.setProperty("serverIdX", "5");
        assertEquals(4, server.getServerId());
        
        assertNotNull(OAObjectPropertyDelegate.getProperty(server, "ServerIdX"));

        assertEquals("5", OAObjectPropertyDelegate.getProperty(server, "ServerIdX"));
        assertEquals("5", server.getProperty("ServerIdX"));
        assertEquals("5", server.getProperty("ServerIdx"));
        
        server.setProperty("serverIdX", null);
        Object objx = OAObjectPropertyDelegate.getProperty(server, "ServerIdX", true, true);
        assertNull(objx);
        
        objx = OAObjectPropertyDelegate.getProperty(server, "zzz", true, true);
        assertTrue(objx instanceof OANotExist);
        
        OAObjectPropertyDelegate.removeProperty(server, "serveridx", true);
        objx = OAObjectPropertyDelegate.getProperty(server, "ServerIdX", true, true);
        assertTrue(objx instanceof OANotExist);
    }
    
}
