package com.viaoa.oa.api;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.AppUser;
import com.test.pos.model.oa.Customer;
import com.test.pos.model.oa.Register;
import com.viaoa.hub.Hub;
import com.viaoa.oa.OA;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.session.OASessionAccess;
import com.viaoa.session.OASessionUser;

class SessionUserOpsTest {

    @BeforeEach
    void beforeEach() {
        OA oa = OARuntime.createDefaultOA(Register.class);
    }

    @AfterEach
    void afterEach() {
        OAObject.setDebugMode(false);
        OARuntime.defaultOA().sessionUser().set(null);
        OARuntime.defaultOA().close();
    }

    @Test
    @DisplayName("The session user can be a domain object that is separate from the model user")
    void setTest() {
        OA oa = OARuntime.defaultOA();
        Hub<AppUser> hubAppUser = new Hub<>(AppUser.class);
        AppUser appUser = new AppUser();
        Customer customer = new Customer();
        OASessionAccess sessionAccess = new OASessionAccess(true, true);
        OASessionUser<Customer> sessionUser = new OASessionUser<>(customer);
        sessionUser.setSessionAccess(sessionAccess);

        hubAppUser.add(appUser);
        oa.modelUser().setDefault(hubAppUser);

        oa.sessionUser().set(sessionUser);

        assertSame(hubAppUser, oa.modelUser().getCalc());
        assertSame(appUser, oa.modelUser().getCalc().getAO());
        assertSame(sessionUser, oa.sessionUser().get());
        assertSame(customer, oa.sessionUser().get().getCalcUserObject());
        assertSame(sessionAccess, oa.sessionUser().get().getSessionAccess());
    }

    @Test
    @DisplayName("A Hub-backed session user resolves to the current active object")
    void get_withHubBackedSessionUserTest() {
        OA oa = OARuntime.defaultOA();
        Hub<Customer> hubCustomer = new Hub<>(Customer.class);
        Customer customer = new Customer();
        Customer customer2 = new Customer();
        OASessionUser<Customer> sessionUser = new OASessionUser<>(hubCustomer);

        hubCustomer.add(customer);
        hubCustomer.add(customer2);
        oa.sessionUser().set(sessionUser);

        hubCustomer.setAO(customer);
        assertSame(customer, oa.sessionUser().get().getCalcUserObject());

        hubCustomer.setAO(customer2);
        assertSame(customer2, oa.sessionUser().get().getCalcUserObject());
    }

    @Test
    @DisplayName("Clearing the runtime session user removes the current session principal")
    void set_withNullTest() {
        OA oa = OARuntime.defaultOA();
        OASessionUser<Customer> sessionUser = new OASessionUser<>(new Customer());

        oa.sessionUser().set(sessionUser);
        oa.sessionUser().set(null);

        assertNull(oa.sessionUser().get());
    }
}
