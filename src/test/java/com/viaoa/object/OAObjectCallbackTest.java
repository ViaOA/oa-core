package com.viaoa.object;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.viaoa.OAUnitTest;
import com.viaoa.context.OAContext;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObjectCallback.Type;
import com.viaoa.util.OADate;

import test.hifive.model.oa.*;

public class OAObjectCallbackTest extends OAUnitTest {

    @BeforeClass
    public static void beforeAll() {
        User user = new User();
        // OAThreadLocalDelegate.setContext("test");
        OAContext.setContext(null, user);
    }
    @AfterClass
    public static void afterAll() {
        // OAThreadLocalDelegate.setContext(null);        
    }
    
    /**
     * Employee
     *    class is enabled if inactieDate=null
     * Employee.addresses is enabled if createdate
     * 
     * @throws Exception
     */
    
    @Test
    public void test() throws Exception {
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(Employee.class);
        String s = oi.getEnabledProperty();
        assertTrue("InactiveDate".equalsIgnoreCase(s));
        
        Employee emp = new Employee();
        
        boolean b = OAObjectCallbackDelegate.getAllowEnabled(OAObjectCallback.CHECK_ALL, null, emp, null);
        assertTrue(b);
        
        emp.setInactiveDate(new OADate());
        b = OAObjectCallbackDelegate.getAllowEnabled(OAObjectCallback.CHECK_ALL, null, emp, null);
        assertFalse(b);

        b = OAObjectCallbackDelegate.getAllowEnabled(OAObjectCallback.CHECK_AllButProcessed, null, emp, null);
        assertFalse(b);

        b = OAObjectCallbackDelegate.getAllowEnabled(OAObjectCallback.CHECK_EnabledProperty, null, emp, null);
        assertFalse(b);

        b = OAObjectCallbackDelegate.getAllowEnabled(OAObjectCallback.CHECK_None, null, emp, null);
        assertTrue(b);
        
        b = OAObjectCallbackDelegate.getAllowEnabled(OAObjectCallback.CHECK_Processed, null, emp, null);
        assertTrue(b);
        
        b = OAObjectCallbackDelegate.getAllowEnabled(OAObjectCallback.CHECK_UserEnabledProperty, null, emp, null);
        assertTrue(b);

        emp.setInactiveDate(null);
        
        emp.TestEditQuery_Class = new OAObjectCallback(Type.AllowEnabled);
        emp.TestEditQuery_Class.setAllowed(false);
        
        b = OAObjectCallbackDelegate.getAllowEnabled(OAObjectCallback.CHECK_ALL, null, emp, null);
        assertFalse(b);

        emp.TestEditQuery_Class.setAllowed(true);
        b = OAObjectCallbackDelegate.getAllowEnabled(OAObjectCallback.CHECK_ALL, null, emp, null);
        assertTrue(b);
        
        emp.TestEditQuery_Class = null;
        b = OAObjectCallbackDelegate.getAllowEnabled(OAObjectCallback.CHECK_ALL, null, emp, null);
        assertTrue(b);
        
        int xx = 4;
        xx++;
    }        


    // Employee[enabledProp].employeeAwards [owned]
    @Test
    public void test1() throws Exception {
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(Employee.class);

        Employee emp = new Employee();
        emp.setInactiveDate(new OADate());
        EmployeeAward ea = new EmployeeAward();
        
        OAObjectCallback eq = OAObjectCallbackDelegate.getVerifyAddObjectCallback(emp.getEmployeeAwards(), ea, OAObjectCallback.CHECK_ALL);
        assertFalse(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getVerifyAddObjectCallback(emp.getEmployeeAwards(), ea, OAObjectCallback.CHECK_CallbackMethod);
        assertTrue(eq.getAllowed());
        
        emp.getEmployeeAwards().add(ea);  // this will work, use EQ.checktype=callbackMethod
    }

    // Employee.employeeAwards [enabledProp]
    @Test
    public void test2() throws Exception {
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(Employee.class);
        OALinkInfo li = oi.getLinkInfo(Employee.P_EmployeeAwards);
        
        li.setEnabledProperty(Employee.P_TopLevelManager);
        li.setEnabledValue(true);
        
        Employee emp = new Employee();
        EmployeeAward ea = new EmployeeAward();
        
        OAObjectCallback eq = OAObjectCallbackDelegate.getVerifyAddObjectCallback(emp.getEmployeeAwards(), ea, OAObjectCallback.CHECK_ALL);
        assertFalse(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getVerifyAddObjectCallback(emp.getEmployeeAwards(), ea, OAObjectCallback.CHECK_CallbackMethod);
        assertTrue(eq.getAllowed());

        emp.setTopLevelManager(true);
        eq = OAObjectCallbackDelegate.getVerifyAddObjectCallback(emp.getEmployeeAwards(), ea, OAObjectCallback.CHECK_ALL);
        assertTrue(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getVerifyAddObjectCallback(emp.getEmployeeAwards(), ea, OAObjectCallback.CHECK_CallbackMethod);
        assertTrue(eq.getAllowed());

        emp.getEmployeeAwards().add(ea); 
        li.setEnabledProperty(null);
    }
    
    // Employee.employeeAwards  [userContext]
    @Test
    public void test3() throws Exception {
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(Employee.class);
        OALinkInfo li = oi.getLinkInfo(Employee.P_EmployeeAwards);
        li.setContextEnabledProperty(User.P_Admin);
        li.setContextEnabledValue(true);
        
        final User user = (User) OAContext.getContextObject();

        Employee emp = new Employee();
        EmployeeAward ea = new EmployeeAward();
        
        OAObjectCallback eq = OAObjectCallbackDelegate.getVerifyAddObjectCallback(emp.getEmployeeAwards(), ea, OAObjectCallback.CHECK_ALL);
        assertFalse(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getVerifyAddObjectCallback(emp.getEmployeeAwards(), ea, OAObjectCallback.CHECK_CallbackMethod);
        assertTrue(eq.getAllowed());

        user.setAdmin(true);
        
        eq = OAObjectCallbackDelegate.getVerifyAddObjectCallback(emp.getEmployeeAwards(), ea, OAObjectCallback.CHECK_ALL);
        assertTrue(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getVerifyAddObjectCallback(emp.getEmployeeAwards(), ea, OAObjectCallback.CHECK_CallbackMethod);
        assertTrue(eq.getAllowed());

        emp.getEmployeeAwards().add(ea); 
        li.setContextEnabledProperty(null);
        user.setAdmin(false);
    }
    
    // property permissions
    @Test
    public void test4() throws Exception {
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(Employee.class);

        Employee emp = new Employee();
        
        OAObjectCallback eq = OAObjectCallbackDelegate.getVerifyAddObjectCallback(null, emp, OAObjectCallback.CHECK_ALL);
        assertNull(eq);


        eq = OAObjectCallbackDelegate.getVerifyPropertyChangeObjectCallback(OAObjectCallback.CHECK_ALL, emp, "firstName", null, "xxx");
        assertTrue(eq.getAllowed());
        
        OAPropertyInfo pi = oi.getPropertyInfo("firstname");
        pi.setEnabledProperty("lastname");
        pi.setEnabledValue(true);
        
        eq = OAObjectCallbackDelegate.getVerifyPropertyChangeObjectCallback(OAObjectCallback.CHECK_ALL, emp, "firstName", null, "xxx");
        assertFalse(eq.getAllowed());
        
        int x = emp.cntFirstNameCallback;
        emp.setFirstName("ff");  // allowed to call directly, only will call the firstNameCallback method
        assertEquals(x+2, emp.cntFirstNameCallback); //
        
        emp.setLastName("x");
        
        eq = OAObjectCallbackDelegate.getVerifyPropertyChangeObjectCallback(OAObjectCallback.CHECK_ALL, emp, "firstName", null, "xxx");
        assertTrue(eq.getAllowed());
        
        pi.setEnabledProperty(null);
    }
  
    // Processed
    @Test
    public void test5() throws Exception {
        final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(Employee.class);
        oi.setProcessed(true);
        Employee emp = new Employee();
        
        OAPropertyInfo pi = oi.getPropertyInfo("firstname");
        pi.setProcessed(true);

        OAObjectCallback eq = OAObjectCallbackDelegate.getVerifyPropertyChangeObjectCallback(OAObjectCallback.CHECK_ALL, emp, "firstName", null, "xxx");
        assertFalse(eq.getAllowed());
        
        pi.setProcessed(false);
        oi.setProcessed(false);
    }
    
    @Test
    public void test6() throws Exception {
        final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(Employee.class);
        oi.setProcessed(true);
        Employee emp = new Employee();
        
        EmployeeAward ea = new EmployeeAward();

        OAPropertyInfo pi = oi.getPropertyInfo("firstname");
        pi.setProcessed(true);

        OAObjectCallback eq = OAObjectCallbackDelegate.getVerifyPropertyChangeObjectCallback(OAObjectCallback.CHECK_ALL, emp, "firstName", null, "xxx");
        assertFalse(eq.getAllowed());
        
        pi.setProcessed(false);
        oi.setProcessed(false);
    }

   
    @Test
    public void testa() throws Exception {
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(Employee.class);
        String s = oi.getEnabledProperty();
        assertTrue("InactiveDate".equalsIgnoreCase(s));
        
        Employee emp = new Employee();
        
        assertNotNull(emp.getCreated());
        
        boolean b = OAObjectCallbackDelegate.getAllowEnabled(OAObjectCallback.CHECK_ALL, null, emp, null);
        assertTrue(b);

        assertTrue(emp.isEnabled());

        assertTrue(emp.isEnabled(Employee.P_LastName));
        b = emp.isEnabled(Employee.P_Addresses);
        assertTrue(b);
        b = emp.getAddresses().canAdd();
        assertTrue(b);
        b = emp.getAddresses().canAdd(new Address());
        assertTrue(b);
        
        b = emp.getEmployeeAwards().canAdd();
        assertTrue(b);
        
        b = OAObjectCallbackDelegate.getAllowEnabled(OAObjectCallback.CHECK_ALL, null, emp, "lastName");
        assertTrue(b);
        
        emp.setInactiveDate(new OADate());
        assertFalse(emp.isEnabled());
        
        b = OAObjectCallbackDelegate.getAllowEnabled(OAObjectCallback.CHECK_ALL, null, emp, null);
        assertFalse(b);
        
        b = OAObjectCallbackDelegate.getAllowEnabled(OAObjectCallback.CHECK_ALL, null, emp, "lastName");
        assertFalse(b);

        b = OAObjectCallbackDelegate.getAllowEnabled(OAObjectCallback.CHECK_ALL, null, emp, "EmployeeAwards");
        assertFalse(b);

        b = OAObjectCallbackDelegate.getAllowAdd(emp.getEmployeeAwards(), null, OAObjectCallback.CHECK_ALL);
        assertFalse(b);
        
        b = emp.getEmployeeAwards().canAdd();  // only checks callback method
        assertTrue(b);

        b = emp.getAddresses().canAdd();
        assertTrue(b);
        
        b = emp.getAddresses().canAdd(new Address());
        assertTrue(b);
        b = emp.getAddresses().getAllowRemove(OAObjectCallback.CHECK_ALL, null);
        assertTrue(b);
        
        try {
            emp.getAddresses().add(new Address());
            // fail("emp.getAddresses.add should not be allowed");
        }
        catch (Exception e) {
        }
        
        
        b = emp.getEmployeeAwards().canAdd();
        assertTrue(b);
        try {
            emp.getEmployeeAwards().add(new EmployeeAward());
            // fail("emp.getEmployeeAwards.add should not be allowed");
        }
        catch (Exception e) {
        }
        
        assertFalse(emp.isEnabled(Employee.P_LastName));
        
        assertFalse(emp.isEnabled(Employee.P_Addresses));
        b = OAObjectCallbackDelegate.getAllowEnabled(OAObjectCallback.CHECK_ALL, null, emp, Employee.P_Addresses);
        assertFalse(b);
        b = OAObjectCallbackDelegate.getAllowAdd(emp.getAddresses(), null, OAObjectCallback.CHECK_ALL);
        assertFalse(b);
        
        try {
            emp.setLastName("test");
        //    fail("setLastName should fail");
            //  firePropertyChange calls editQuery, and will only log.warn (not throw exception)
        }
        catch (Exception e) {
        }

        try {
            emp.setInactiveDate(null);
        }
        catch (Exception e) {
            fail("setInactiveDate should not fail");
        }
        
        try {
            emp.setLastName("test");
        }
        catch (Exception e) {
            fail("setLastName should not fail");
        }
    }

/************    
    @Test
    public void testClass() throws Exception {
        Employee emp = new Employee();
        assertNotNull(emp.getCreated());
        boolean b = OAObjectCallbackDelegate.getAllowEnabled(OAObjectCallback.CHECK_ALL, null, emp, null);
        assertTrue(b);
        
        assertTrue(emp.isEnabled());
        assertTrue(emp.isVisible());
        
        emp.setInactiveDate(new OADate());
        assertFalse(emp.isEnabled());
        assertTrue(emp.isVisible());

        emp.setInactiveDate(null);
        assertTrue(emp.isEnabled());
        assertTrue(emp.isVisible());

        emp.TestEditQuery_Class = new OAObjectCallback(Type.AllowEnabled);
        emp.TestEditQuery_Class.setAllowed(false);
        assertFalse(emp.isEnabled());

        emp.TestEditQuery_Class.setAllowed(true);
        assertTrue(emp.isEnabled());
        
        assertTrue(emp.isVisible());
        emp.TestEditQuery_Class = new OAObjectCallback(Type.AllowVisible);
        emp.TestEditQuery_Class.setAllowed(false);
        assertFalse(emp.isVisible());

        emp.TestEditQuery_Class = null;
        
        assertTrue(emp.getAddresses().canAdd());
        
        emp.TestEditQuery_Class = new OAObjectCallback(Type.AllowEnabled);
        emp.TestEditQuery_Class.setAllowed(false);
        assertFalse(emp.isEnabled());
        
        assertFalse(emp.getEmployeeAwards().canAdd());
        
        try {
            emp.getEmployeeAwards().add(new EmployeeAward());
            fail("emp.getEmployeeAwards.add should not be allowed");
        }
        catch (Exception e) {
        }
        emp.TestEditQuery_Class = null;
        try {
            emp.getEmployeeAwards().add(new EmployeeAward());
        }
        catch (Exception e) {
            fail("emp.getEmployeeAwards.add should be allowed");
        }
        
        emp.TestEditQuery_Class = null;
    }    
    @Test
    public void testProperty() throws Exception {
    }    
    @Test
    public void testOwner() throws Exception {
    }    
    @Test
    public void testCalcProperty() throws Exception {
        Employee emp = new Employee();
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(Employee.class);
        OACalcInfo ci = oi.getCalcInfo(emp.P_FullName);
        assertNotNull(ci);
        if (ci.getEnabledProperty() != null) assertEquals("", ci.getEnabledProperty());
    }    
    @Test
    public void testLinkOne() throws Exception {
        Employee emp = new Employee();
        assertNull(emp.getEmployeeType());
        assertTrue(emp.isEnabled());
        assertTrue(emp.isEnabled(emp.P_EmployeeType));
        
        EmployeeAward ea = new EmployeeAward();
        assertTrue(ea.isEnabled());
        assertTrue(ea.isVisible());
        ea.setEmployee(emp);
        assertTrue(ea.isEnabled());
        assertTrue(ea.isVisible());
        emp.setInactiveDate(new OADate());
        assertFalse(ea.isEnabled());
        assertTrue(ea.isVisible());
        emp.setInactiveDate(null);
        assertTrue(ea.isEnabled());
        assertTrue(ea.isVisible());
    }    
    @Test
    public void testLinkMany() throws Exception {
        Employee emp = new Employee();
        
        assertTrue(emp.getAddresses().canAdd());
        assertTrue(emp.getAddresses().getAllowRemove(OAObjectCallback.CHECK_ALL, null));
        assertTrue(emp.getAddresses().canAdd());
        assertTrue(OAObjectCallbackDelegate.getAllowAdd(emp.getAddresses(), null, OAObjectCallback.CHECK_ALL));
        assertTrue(OAObjectCallbackDelegate.getAllowEnabled(OAObjectCallback.CHECK_ALL, emp.getAddresses(), null, null));

        emp.setCreated(null);
        assertFalse(emp.getAddresses().canAdd());
        assertFalse(emp.getAddresses().getAllowRemove(OAObjectCallback.CHECK_ALL, null));
        assertFalse(emp.getAddresses().canAdd());
        assertFalse(OAObjectCallbackDelegate.getAllowAdd(emp.getAddresses(), null, OAObjectCallback.CHECK_ALL));
        assertFalse(OAObjectCallbackDelegate.getAllowEnabled(OAObjectCallback.CHECK_ALL, emp.getAddresses(), null, null));
        
        emp.setCreated(new OADate());
        assertTrue(emp.getAddresses().canAdd());
        assertTrue(emp.getAddresses().getAllowRemove(OAObjectCallback.CHECK_ALL, null));
        assertTrue(emp.getAddresses().canAdd());
        
        Address address = new Address();
        emp.getAddresses().add(address);
        
        emp.setCreated(null);
        emp.setInactiveDate(new OADate());
        assertFalse(emp.getAddresses().canAdd());
        assertFalse(emp.getAddresses().getAllowRemove(OAObjectCallback.CHECK_ALL, null));
        assertFalse(emp.getAddresses().canAdd());
        assertFalse(OAObjectCallbackDelegate.getAllowAdd(emp.getAddresses(), null, OAObjectCallback.CHECK_ALL));
        assertFalse(OAObjectCallbackDelegate.getAllowEnabled(OAObjectCallback.CHECK_ALL, emp.getAddresses(), null, null));
        
        assertFalse(address.isEnabled());
        assertTrue(address.isVisible());
        assertFalse(address.isEnabled(Address.P_Address1));
        assertFalse(emp.getAddresses().canAdd());

        emp.setInactiveDate(null);
        emp.setCreated(null);
        assertFalse(address.isEnabled());
        assertTrue(address.isVisible());
        assertFalse(address.isEnabled(Address.P_Address1));
        assertFalse(emp.getAddresses().canAdd());
        
        emp.setCreated(new OADate());
        assertTrue(address.isEnabled());
        assertTrue(address.isVisible());
        assertTrue(address.isEnabled(Address.P_Address1));
        
    }    
    @Test
    public void testCommand() throws Exception {
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(Employee.class);
        OAMethodInfo mi = oi.getMethodInfo("command");
        assertNotNull(oi);
        assertEquals("", mi.getEnabledProperty());
        assertEquals("birthDate", mi.getVisibleProperty());
        
        Employee emp = new Employee();
        boolean b = OAObjectCallbackDelegate.getAllowVisible(null, emp, "command");
        assertFalse(b);

        emp.setBirthDate(new OADate("05/04/99"));
        b = OAObjectCallbackDelegate.getAllowVisible(null, emp, "command");
        assertTrue(b);
    }   
**/    


    @Test
    public void getAllowVisibleTest() {
        final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(Employee.class);
        oi.setProcessed(true);
        
        Employee emp = new Employee();
        EmployeeAward ea = new EmployeeAward();
        emp.getEmployeeAwards().add(ea);

        OAObjectCallback eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(null, ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
        

        eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(null, ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
        
        oi.setVisibleProperty(Employee.P_InactiveDate);
        oi.setVisibleValue(false);
        
        eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(null, ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());

        Hub hub = new Hub();
        hub.add(ea);
        eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(hub, ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
        
        
        emp.setInactiveDate(new OADate());
        eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertFalse(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(hub, ea, EmployeeAward.P_ApprovedDate);
        assertFalse(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(null, ea, EmployeeAward.P_ApprovedDate);
        assertFalse(eq.getAllowed());
        
        emp.setInactiveDate(null);
        eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(hub, ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(null, ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
    
        final OAPropertyInfo pi = oi.getPropertyInfo(Employee.P_EmployeeCode);
        pi.setVisibleProperty(Employee.P_SuperApprover);
        pi.setVisibleValue(true);
        emp.setSuperApprover(false);
        
        eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(null, emp, null);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(null, emp, Employee.P_EmployeeCode);
        assertFalse(eq.getAllowed());
        
        emp.setSuperApprover(true);
        eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(null, emp, Employee.P_EmployeeCode);
        assertTrue(eq.getAllowed());
        
        OALinkInfo li = oi.getLinkInfo(Employee.P_Location);
        li.setVisibleProperty(Employee.P_SuperApprover);
        li.setVisibleValue(true);
        emp.setSuperApprover(false);
        
        eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(null, emp, Employee.P_Location);
        assertFalse(eq.getAllowed());
        emp.setSuperApprover(true);
        eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(null, emp, Employee.P_Location);
        assertTrue(eq.getAllowed());
    
        li.setVisibleProperty(null);
        pi.setEnabledProperty(null);
        pi.setVisibleProperty(null);
        oi.setVisibleProperty(null);
        oi.setProcessed(false);
    }
    
    

    @Test
    public void getAllowEnabledTest() {
        final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(Employee.class);

        User user = (User) OAContext.getContextObject();
        user.setAdmin(false);
        
        Employee emp = new Employee();
        EmployeeAward ea = new EmployeeAward();
        emp.getEmployeeAwards().add(ea);
        
        OAObjectCallback eq;

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, null, ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), null, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, null, ea, null);
        assertTrue(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, null);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), null, null);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, null);
        assertTrue(eq.getAllowed());
        
        emp.setInactiveDate(new OADate());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, null, ea, EmployeeAward.P_ApprovedDate);
        assertFalse(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertFalse(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), null, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertFalse(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, null, ea, null);
        assertFalse(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, null);
        assertFalse(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), null, null);
        assertFalse(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, null);
        assertFalse(eq.getAllowed());
        
        emp.setInactiveDate(null);
        
        // owner context
        // user.admin=false
        oi.setContextEnabledProperty(User.P_Admin);
        oi.setContextEnabledValue(true);
      

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, null, ea, EmployeeAward.P_ApprovedDate);
        assertFalse(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertFalse(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), null, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertFalse(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, null, ea, null);
        assertFalse(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, null);
        assertFalse(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), null, null);
        assertFalse(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, null);
        assertFalse(eq.getAllowed());
       
        
        // user.admin=true
        user.setAdmin(true);
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, null, ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), null, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, null, ea, null);
        assertTrue(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, null);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), null, null);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, null);
        assertTrue(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, null, emp, Employee.P_LastName);
        assertTrue(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), emp, Employee.P_LastName);
        assertTrue(eq.getAllowed());
        
        Hub hub = new Hub();
        hub.add(emp);
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, hub, emp, Employee.P_LastName);
        assertTrue(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, hub, null, Employee.P_LastName);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, null, emp, null);
        assertTrue(eq.getAllowed());
        

        // emp is inactive
        emp.setInactiveDate(new OADate());
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, null, ea, EmployeeAward.P_ApprovedDate);
        assertFalse(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertFalse(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), null, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertFalse(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, null, ea, null);
        assertFalse(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, null);
        assertFalse(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), null, null);
        assertFalse(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, null);
        assertFalse(eq.getAllowed());

        
        
        // all should pass 
        emp.setInactiveDate(null);
        // none should pass
        oi.setProcessed(true);
        emp.setInactiveDate(new OADate());
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, null, ea, EmployeeAward.P_ApprovedDate);
        assertFalse(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertFalse(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), null, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertFalse(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, null, ea, null);
        assertFalse(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, null);
        assertFalse(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), null, null);
        assertFalse(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, emp.getEmployeeAwards(), ea, null);
        assertFalse(eq.getAllowed());

        
        // all should pass
        oi.setProcessed(true);
        emp.setInactiveDate(null);
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_AllButProcessed, null, ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_AllButProcessed, emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_AllButProcessed, emp.getEmployeeAwards(), null, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_AllButProcessed, emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_AllButProcessed, null, ea, null);
        assertTrue(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_AllButProcessed, emp.getEmployeeAwards(), ea, null);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_AllButProcessed, emp.getEmployeeAwards(), null, null);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_AllButProcessed, emp.getEmployeeAwards(), ea, null);
        assertTrue(eq.getAllowed());
        
        // all should pass
        oi.setProcessed(true);
        emp.setInactiveDate(new OADate());
        emp.setAdmin(false);
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_None, null, ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_None, emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_None, emp.getEmployeeAwards(), null, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_None, emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_None, null, ea, null);
        assertTrue(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_None, emp.getEmployeeAwards(), ea, null);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_None, emp.getEmployeeAwards(), null, null);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_None, emp.getEmployeeAwards(), ea, null);
        assertTrue(eq.getAllowed());
        

        // all should pass
        oi.setProcessed(true);
        emp.setInactiveDate(new OADate());
        emp.setAdmin(false);
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_CallbackMethod, null, ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_CallbackMethod, emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_CallbackMethod, emp.getEmployeeAwards(), null, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_CallbackMethod, emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_CallbackMethod, null, ea, null);
        assertTrue(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_CallbackMethod, emp.getEmployeeAwards(), ea, null);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_CallbackMethod, emp.getEmployeeAwards(), null, null);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_CallbackMethod, emp.getEmployeeAwards(), ea, null);
        assertTrue(eq.getAllowed());
        

        // all should fail
        oi.setProcessed(true);
        emp.setInactiveDate(new OADate());
        emp.setAdmin(false);
        
        int checkType = OAObjectCallback.CHECK_ALL ^ OAObjectCallback.CHECK_IncludeMaster;
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(checkType, null, ea, EmployeeAward.P_ApprovedDate);
        assertFalse(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(checkType, emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertFalse(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(checkType, emp.getEmployeeAwards(), null, EmployeeAward.P_ApprovedDate);
        assertTrue(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(checkType, emp.getEmployeeAwards(), ea, EmployeeAward.P_ApprovedDate);
        assertFalse(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(checkType, null, ea, null);
        assertFalse(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(checkType, emp.getEmployeeAwards(), ea, null);
        assertFalse(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(checkType, emp.getEmployeeAwards(), null, null);
        assertFalse(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(checkType, emp.getEmployeeAwards(), ea, null);
        assertFalse(eq.getAllowed());
    
        
        // property level enabled prop
        user.setAdmin(false);
        oi.setContextEnabledProperty(null);
        oi.setProcessed(false);
        
        final OAPropertyInfo pi = oi.getPropertyInfo(Employee.P_EmployeeCode);
        pi.setEnabledProperty(Employee.P_SuperApprover);
        pi.setEnabledValue(true);
        emp.setSuperApprover(false);

        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, null, emp, null);
        assertFalse(eq.getAllowed());
        
        emp.setInactiveDate(null);
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, null, emp, Employee.P_EmployeeCode);
        assertFalse(eq.getAllowed());
        
        // pass
        emp.setSuperApprover(true);
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, null, emp, Employee.P_EmployeeCode);
        assertTrue(eq.getAllowed());
        
        // li enabled prop
        OALinkInfo li = oi.getLinkInfo(Employee.P_Location);
        li.setEnabledProperty(Employee.P_SuperApprover);
        li.setEnabledValue(true);
        emp.setSuperApprover(false);
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, null, emp, Employee.P_Location);
        assertFalse(eq.getAllowed());
        emp.setSuperApprover(true);
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, null, emp, Employee.P_Location);
        assertTrue(eq.getAllowed());
        
        li.setEnabledProperty(null);
        pi.setEnabledProperty(null);
        user.setAdmin(false);
        oi.setContextEnabledProperty(null);
        oi.setProcessed(false);
    }
    
    @Test
    public void getAllowCopyTest() throws Exception {
        final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(Employee.class);

        User user = (User) OAContext.getContextObject();
        user.setAdmin(false);
        
        Employee emp = new Employee();
        
        OAObjectCallback eq = OAObjectCallbackDelegate.getAllowCopyObjectCallback(emp);
        assertTrue(eq.getAllowed());
    
        
        emp.TestEditQuery_Class = new OAObjectCallback(Type.AllowCopy);
        emp.TestEditQuery_Class.setAllowed(false);
    
        eq = OAObjectCallbackDelegate.getAllowCopyObjectCallback(emp);
        assertFalse(eq.getAllowed());
        
        emp.TestEditQuery_Class.setAllowed(true);
        eq = OAObjectCallbackDelegate.getAllowCopyObjectCallback(emp);
        assertTrue(eq.getAllowed());
    }
    
    @Test
    public void getCopyTest() throws Exception {
        final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(Employee.class);

        User user = (User) OAContext.getContextObject();
        user.setAdmin(false);
        
        Employee emp = new Employee();
        
        Employee empx = (Employee) OAObjectCallbackDelegate.getCopy(emp);
        assertNotNull(empx);

        emp.TestEditQuery_Class = new OAObjectCallback(Type.AllowCopy);
        emp.TestEditQuery_Class.setAllowed(false);
        
        empx = (Employee) OAObjectCallbackDelegate.getCopy(emp);
        assertNull(empx);
    }

    
    @Test
    public void verifyPropertyChangeTest() {
        final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(Employee.class);

        User user = (User) OAContext.getContextObject();
        user.setAdmin(false);
        
        Employee emp = new Employee();
        EmployeeAward ea = new EmployeeAward();
        emp.getEmployeeAwards().add(ea);
        
        OAObjectCallback eq;

        eq = OAObjectCallbackDelegate.getVerifyPropertyChangeObjectCallback(OAObjectCallback.CHECK_ALL, ea, EmployeeAward.P_ApprovedDate, null, new OADate());
        assertTrue(eq.getAllowed());

        emp.setInactiveDate(new OADate());
        
        eq = OAObjectCallbackDelegate.getVerifyPropertyChangeObjectCallback(OAObjectCallback.CHECK_ALL, ea, EmployeeAward.P_ApprovedDate, null, new OADate());
        assertFalse(eq.getAllowed());
        
        eq = OAObjectCallbackDelegate.getVerifyPropertyChangeObjectCallback(OAObjectCallback.CHECK_ALL, ea, Employee.P_Location, null, new Location());
        assertFalse(eq.getAllowed());

        emp.setInactiveDate(null);
        eq = OAObjectCallbackDelegate.getVerifyPropertyChangeObjectCallback(OAObjectCallback.CHECK_ALL, ea, Employee.P_Location, null, new Location());
        assertTrue(eq.getAllowed());

        
        // li enabled prop
        OALinkInfo li = oi.getLinkInfo(Employee.P_Location);
        li.setEnabledProperty(Employee.P_SuperApprover);
        li.setEnabledValue(true);
        emp.setSuperApprover(false);
        
        eq = OAObjectCallbackDelegate.getVerifyPropertyChangeObjectCallback(OAObjectCallback.CHECK_ALL, emp, Employee.P_Location, null, new Location());
        assertFalse(eq.getAllowed());

        eq = OAObjectCallbackDelegate.getVerifyPropertyChangeObjectCallback(OAObjectCallback.CHECK_ALL, emp, Employee.P_Location, null, new Location());
        assertFalse(eq.getAllowed());
        
        emp.setSuperApprover(true);
        emp.TestEditQuery_Location = new OAObjectCallback(Type.AllowEnabled);
        
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, null, emp, Employee.P_Location);
        assertTrue(eq.getAllowed());
        
        emp.TestEditQuery_Location.setAllowed(false);
        eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_CallbackMethod, null, emp, Employee.P_Location);
        assertFalse(eq.getAllowed());
        
        
        li.setEnabledProperty(null);
        // pi.setEnabledProperty(null);
        user.setAdmin(false);
        oi.setContextEnabledProperty(null);
        oi.setProcessed(false);
    }
    
    @Test
    public void getAllowAddEditQueryTest() {
        final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(Employee.class);

        User user = (User) OAContext.getContextObject();
        user.setAdmin(false);
        
        Employee emp = new Employee();
        EmployeeAward ea = new EmployeeAward();
        
        OAObjectCallback eq;
        eq = OAObjectCallbackDelegate.getAllowAddObjectCallback(emp.getEmployeeAwards(), ea, OAObjectCallback.CHECK_ALL);
        assertTrue(eq.getAllowed());

        OALinkInfo li = oi.getLinkInfo(Employee.P_EmployeeAwards);
        li.setContextEnabledProperty(User.P_Admin);
        li.setContextEnabledValue(true);
        
        eq = OAObjectCallbackDelegate.getAllowAddObjectCallback(emp.getEmployeeAwards(), ea, OAObjectCallback.CHECK_ALL);
        assertFalse(eq.getAllowed());
        
        user.setAdmin(true);
        eq = OAObjectCallbackDelegate.getAllowAddObjectCallback(emp.getEmployeeAwards(), ea, OAObjectCallback.CHECK_ALL);
        assertTrue(eq.getAllowed());
        
        li.setContextEnabledProperty(null);
        user.setAdmin(false);
    }
    
}




