package com.viaoa.context;

import static org.junit.Assert.*;

import org.junit.Test;

import com.cdi.model.oa.*;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectPropertyDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.sync.OASync;


/**
    need to call:
         OAContext.setContextObject(context, value)
         OAThreadLocalDelegate.setContext(context);
    
 */
public class OAContextTest {

    @Test
    public void test() throws Exception {
        OAContext.removeContextHub(null);
        assertNull(OAContext.getContextObject());
        assertNull(OAContext.getContextHub());

        // server w/o context, defaults to Admin=true and EditProcessed=true
        assertTrue(OAContext.canAdminEdit());
        assertTrue(OAContext.canEditProcessed());

        final String context = "id";
        final OAObject obj = new OAObject();

        OAContext.setContextObject(context, obj);
        assertEquals(obj, OAContext.getContextObject(context));
        assertNull(OAContext.getContextObject());
        assertNull(OAContext.getContextHub());
        assertNotNull(OAContext.getContextObject(context));
        assertNotNull(OAContext.getContextHub(context));

        AppUser au = new AppUser();
        User user = new User();
        au.setUser(user);

        // 1: set the context/object
        OAContext.setContextObject(context, au);
        assertNull(OAContext.getContextObject());
        assertNull(OAContext.getContextHub());
        assertNotNull(OAContext.getContextObject(context));
        assertNotNull(OAContext.getContextHub(context));
        assertEquals(au, OAContext.getContextObject(context));

        // 2: set the thread context
        OAThreadLocalDelegate.setContext(context);
        assertNotNull(OAThreadLocalDelegate.getContext());
        assertEquals(au, OAContext.getContextObject());
        assertEquals(au, OAContext.getContextObject(context));

        OAThreadLocalDelegate.setContext(null);
        assertNull(OAThreadLocalDelegate.getContext());
        assertNull(OAContext.getContextObject());
        assertEquals(au, OAContext.getContextObject(context));
        
        OAThreadLocalDelegate.setContext(context);
        
        // ready to go
        OrderNote ordNote = new OrderNote();
        assertEquals(user, OAObjectPropertyDelegate.getProperty(ordNote, OrderNote.P_User));
        assertEquals(user, ordNote.getUser());
        
        assertNotNull(OAContext.getAllowAdminEditPropertyPath());
        
        assertFalse(OAContext.canAdminEdit());
        assertFalse(OAContext.canAdminEdit(context));
        
        au.setEditProcessed(true);
        assertTrue(OAContext.canAdminEdit());
        assertTrue(OAContext.canAdminEdit(context));
        
        assertTrue(OAContext.canEditProcessed());
        assertTrue(OAContext.canEditProcessed(context));

        OAThreadLocalDelegate.setContext(null);
        OAContext.removeContextHub(context);
        OAContext.removeContextHub();
    }

    @Test
    public void test2() throws Exception {
        if (OASync.isServer()) {
            
        }
    }
}




