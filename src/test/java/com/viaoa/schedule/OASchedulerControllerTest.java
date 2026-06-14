package com.viaoa.schedule;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Store;
import com.test.pos.model.oa.StoreSchedule;
import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;
import com.viaoa.hub.Hub;

class OASchedulerControllerTest {

    @Test
    void separateDateTimeConstructorWithLinkHubStoresFromDateProperty() {
        OASchedulerController<Store, StoreSchedule> controller = new OASchedulerController<>(
                null, new Hub<>(StoreSchedule.class), Store.P_StoreSchedules,
                StoreSchedule.P_Date, null, StoreSchedule.P_Date, null);

        assertEquals(0, controller.getType());
        assertNull(controller.getDetailHub());
        assertEquals(StoreSchedule.P_Date, controller.getFromDateProperty());
    }

    @Test
    void separateDateTimeConstructorWithoutLinkHubStoresFromDateProperty() {
        OASchedulerController<Store, StoreSchedule> controller = new OASchedulerController<>(
                null, Store.P_StoreSchedules, StoreSchedule.P_Date, null, StoreSchedule.P_Date, null);

        assertEquals(0, controller.getType());
        assertNull(controller.getDetailHub());
        assertEquals(StoreSchedule.P_Date, controller.getFromDateProperty());
    }

    @Test
    void dateTimeConstructorWithLinkHubFallsBackToDateTimeProperty() {
        OASchedulerController<Store, StoreSchedule> controller = new OASchedulerController<>(
                null, new Hub<>(StoreSchedule.class), Store.P_StoreSchedules,
                StoreSchedule.P_VerifySchedule, StoreSchedule.P_TillAuditCompleted);

        assertEquals(0, controller.getType());
        assertNull(controller.getDetailHub());
        assertEquals(StoreSchedule.P_VerifySchedule, controller.getFromDateProperty());
    }

    @Test
    void dateTimeConstructorWithoutLinkHubFallsBackToDateTimeProperty() {
        OASchedulerController<Store, StoreSchedule> controller = new OASchedulerController<>(
                null, Store.P_StoreSchedules, StoreSchedule.P_VerifySchedule, StoreSchedule.P_TillAuditCompleted);

        assertEquals(0, controller.getType());
        assertNull(controller.getDetailHub());
        assertEquals(StoreSchedule.P_VerifySchedule, controller.getFromDateProperty());
    }

    @Test
    void setupResolvesDetailHubForRealOaposRelationship() {
        Hub<Store> hub = new Hub<>(Store.class);
        OASchedulerController<Store, StoreSchedule> controller = new OASchedulerController<>(
                hub, Store.P_StoreSchedules, StoreSchedule.P_VerifySchedule, StoreSchedule.P_TillAuditCompleted);

        assertNotNull(controller.getDetailHub());
        assertSame(StoreSchedule.class, controller.getDetailHub().getObjectClass());
    }

    @Test
    void getSchedulerCallbackUsesActiveObjectSchedulerDelegatePath() {
        Hub<Store> hub = new Hub<>(Store.class);
        Store store = new Store();
        hub.add(store);
        hub.setAO(store);
        OASchedulerController<Store, StoreSchedule> controller = new OASchedulerController<>(
                hub, Store.P_StoreSchedules, StoreSchedule.P_VerifySchedule, StoreSchedule.P_TillAuditCompleted);

        assertNull(controller.getSchedulerCallback(new OADate(2026, 6, 9)));
    }

    @Test
    void setReturnsForNullDatesMissingActiveObjectAndMissingDetailHub() {
        OADateTime from = new OADateTime(2026, 6, 9, 9, 0, 0, 0);
        OADateTime to = new OADateTime(2026, 6, 9, 10, 0, 0, 0);
        Hub<Store> hub = new Hub<>(Store.class);
        OASchedulerController<Store, StoreSchedule> noHubDetail = new OASchedulerController<>(
                null, Store.P_StoreSchedules, StoreSchedule.P_VerifySchedule, StoreSchedule.P_TillAuditCompleted);
        OASchedulerController<Store, StoreSchedule> noActiveObject = new OASchedulerController<>(
                hub, Store.P_StoreSchedules, StoreSchedule.P_VerifySchedule, StoreSchedule.P_TillAuditCompleted);

        assertDoesNotThrow(() -> noHubDetail.set(from, to));
        assertDoesNotThrow(() -> noActiveObject.set(null, to));
        assertDoesNotThrow(() -> noActiveObject.set(from, null));
        assertDoesNotThrow(() -> noActiveObject.set(from, to));
    }
}
