package com.viaoa.object;

import org.junit.Test;
import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;
import com.viaoa.object.OAObjectCallback.Type;

import test.xice.tsac3.model.oa.*;

public class OAEditMessageTest extends OAUnitTest {

    @Test
    public void test() {
        Server server = new Server();
        OAObjectCallback em = new OAObjectCallback(Type.AllowAdd);
        
        // assertEquals("test", em.getMessage());
    }
    
}
