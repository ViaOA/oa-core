package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class StoreSchedulePP {
    private static StorePPx store;
    private static StoreDayEndPPx storeDayEnd;
    private static StoreDayOpenPPx storeDayOpen;
    private static TeamMemberPPx teamMembers;
     

    public static StorePPx store() {
        if (store == null) store = new StorePPx(StoreSchedule.P_Store);
        return store;
    }

    public static StoreDayEndPPx storeDayEnd() {
        if (storeDayEnd == null) storeDayEnd = new StoreDayEndPPx(StoreSchedule.P_StoreDayEnd);
        return storeDayEnd;
    }

    public static StoreDayOpenPPx storeDayOpen() {
        if (storeDayOpen == null) storeDayOpen = new StoreDayOpenPPx(StoreSchedule.P_StoreDayOpen);
        return storeDayOpen;
    }

    public static TeamMemberPPx teamMembers() {
        if (teamMembers == null) teamMembers = new TeamMemberPPx(StoreSchedule.P_TeamMembers);
        return teamMembers;
    }

    public static String id() {
        String s = StoreSchedule.P_Id;
        return s;
    }

    public static String created() {
        String s = StoreSchedule.P_Created;
        return s;
    }

    public static String date() {
        String s = StoreSchedule.P_Date;
        return s;
    }

    public static String nextStep() {
        String s = StoreSchedule.P_NextStep;
        return s;
    }

    public static String verifySchedule() {
        String s = StoreSchedule.P_VerifySchedule;
        return s;
    }

    public static String tillAuditCompleted() {
        String s = StoreSchedule.P_TillAuditCompleted;
        return s;
    }

    public static String calcDisplay() {
        String s = StoreSchedule.P_CalcDisplay;
        return s;
    }

    public static String runNextStep() {
        String s = "runNextStep";
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
