package com.viaoa.context;

import static org.junit.Assert.*;

import org.junit.Test;

import com.cdi.model.oa.*;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectEditQueryDelegate;
import com.viaoa.object.OAObjectPropertyDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.util.OADate;


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
        assertTrue(OAContext.isAdmin());
        assertTrue(OAContext.getAllowEditProcessed());

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
        
        assertNotNull(OAContext.getAdminPropertyPath());
        
        assertFalse(OAContext.isAdmin());
        assertFalse(OAContext.isAdmin(context));
        
        au.setAdmin(true);
        assertTrue(OAContext.isAdmin());
        assertTrue(OAContext.isAdmin(context));
        
        au.setEditProcessed(true);
        assertTrue(OAContext.getAllowEditProcessed());
        assertTrue(OAContext.getAllowEditProcessed(context));

        OAThreadLocalDelegate.setContext(null);
        OAContext.removeContextHub(context);
        OAContext.removeContextHub();
    }

    @Test
    public void test2() throws Exception {
        OAThreadLocalDelegate.setContext(null);
        SalesOrder so = new SalesOrder();
        so.setContractor("adfa1");
        boolean b = OAObjectEditQueryDelegate.getAllowEnabled(so, "Contractor");
        assertTrue(b);

        so.setDateSubmitted(new OADate());
        b = OAObjectEditQueryDelegate.getAllowEnabled(so, "Contractor");
        assertTrue(b); // running as server, without an assigned context/user

        so.setContractor("adfa2");

        AppUser au = new AppUser();
        User user = new User();
        au.setUser(user);
        
        Object context = new Object();
        OAContext.setContextObject(context, user);
        OAThreadLocalDelegate.setContext(context);

        b = OAObjectEditQueryDelegate.getAllowEnabled(so, "Contractor");
        assertFalse(b);
        
        // this will cause a warning (wont throw an exception)
        so.setContractor("adfa3");
        
        b = OAObjectEditQueryDelegate.getAllowEnabled(so, "Contractor");
        assertFalse(b);
        
        OAThreadLocalDelegate.setContext(null);
        
        OAContext.removeContext(context);
        OAContext.removeContext();
    }
    
    @Test
    public void test3() throws Exception {
        OAThreadLocalDelegate.setContext(null);
        SalesOrder so = new SalesOrder();
        so.setContractor("adfa1");
        boolean b = OAObjectEditQueryDelegate.getAllowEnabled(so, "Contractor");
        assertTrue(b);

        so.setDateSubmitted(new OADate());
        b = OAObjectEditQueryDelegate.getAllowEnabled(so, "Contractor");
        assertTrue(b); // running as server, without an assigned context/user

        so.setContractor("adfa2");

        AppUser au = new AppUser();
        User user = new User();
        au.setUser(user);
        
        Object context = new Object();
        OAContext.setContextObject(context, user);
        OAThreadLocalDelegate.setContext(context);

        
        b = OAObjectEditQueryDelegate.getAllowEnabled(so, "Contractor");
        assertFalse(b);

        OAThreadLocalDelegate.setAlwaysAllowEnabled(true);
        b = OAObjectEditQueryDelegate.getAllowEnabled(so, "Contractor");
        assertTrue(b);
        
        OAThreadLocalDelegate.setAlwaysAllowEnabled(false);
        b = OAObjectEditQueryDelegate.getAllowEnabled(so, "Contractor");
        assertFalse(b);

        
        OAThreadLocalDelegate.setContext(null);
        OAThreadLocalDelegate.setAlwaysAllowEditProcessed(false);
        OAThreadLocalDelegate.setAlwaysAllowEnabled(false);
        OAContext.removeContext(context);
        OAContext.removeContext();
    }
    
    @Test
    public void test4() throws Exception {
        OAThreadLocalDelegate.setContext(null);
        SalesOrder so = new SalesOrder();
        so.setContractor("adfa1");
        assertTrue(so.isEnabled());

        AppUser au = new AppUser();
        User user = new User();
        au.setUser(user);
        
        Object context = new Object();
        OAContext.setContextObject(context, au);
        OAThreadLocalDelegate.setContext(context);

        assertFalse(so.isEnabled());  //  contextEnabledProperty = AppUser.P_User+"."+User.P_CalcSalesWriteAccess
        user.setSalesAccess(user.SALESACCESS_yes);
        assertTrue(so.isEnabled());
        
        so.setDateSubmitted(new OADate());

        assertFalse(so.isEnabled());
        
        OAThreadLocalDelegate.setContext(null);
        OAThreadLocalDelegate.setAlwaysAllowEditProcessed(false);
        OAThreadLocalDelegate.setAlwaysAllowEnabled(false);
        OAContext.removeContext(context);
        OAContext.removeContext();
    }
    
    @Test
    public void test5() throws Exception {
        OAThreadLocalDelegate.setContext(null);
        SalesOrder so = new SalesOrder();
        so.setContractor("adfa1");
        assertTrue(so.isEnabled());

        AppUser au = new AppUser();
        User user = new User();
        au.setUser(user);
        
        Object context = new Object();
        OAContext.setContextObject(context, au);
        OAThreadLocalDelegate.setContext(context);

        assertFalse(so.isEnabled());  //  contextEnabledProperty = AppUser.P_User+"."+User.P_CalcSalesWriteAccess
        user.setSalesAccess(user.SALESACCESS_yes);
        assertTrue(so.isEnabled());
        
        so.setDateSubmitted(new OADate());

        assertFalse(so.isEnabled());
        assertFalse(so.isEnabled());
        assertFalse(so.isEnabled(so.P_DateSubmitted));

        OAThreadLocalDelegate.setAlwaysAllowEditProcessed(true);
        OAThreadLocalDelegate.setAlwaysAllowEnabled(true);
        assertTrue(so.isEnabled());
        assertTrue(so.isEnabled(so.P_DateSubmitted));
        
        OAThreadLocalDelegate.setAlwaysAllowEditProcessed(false);
        OAThreadLocalDelegate.setAlwaysAllowEnabled(true);
        assertTrue(so.isEnabled());
        assertTrue(so.isEnabled(so.P_DateSubmitted));
        
        OAThreadLocalDelegate.setAlwaysAllowEditProcessed(true);
        OAThreadLocalDelegate.setAlwaysAllowEnabled(false);
        assertFalse(so.isEnabled());
        assertFalse(so.isEnabled(so.P_DateSubmitted));

        
        OAThreadLocalDelegate.setContext(null);
        OAThreadLocalDelegate.setAlwaysAllowEditProcessed(false);
        OAThreadLocalDelegate.setAlwaysAllowEnabled(false);
        OAContext.removeContext(context);
        OAContext.removeContext();
    }
    
}


