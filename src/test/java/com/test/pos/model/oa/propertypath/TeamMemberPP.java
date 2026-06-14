package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class TeamMemberPP {
    private static AppUserPPx appUser;
    private static RegisterSessionPPx registerSessions;
    private static StorePPx store;
    private static StoreSafeLedgerEntryPPx storeSafeLedgerEntries;
    private static StoreSchedulePPx storeSchedules;
    private static TillLedgerEntryPPx tillLedgerEntries;
    private static TMPermissionPPx tmPermissions;
     

    public static AppUserPPx appUser() {
        if (appUser == null) appUser = new AppUserPPx(TeamMember.P_AppUser);
        return appUser;
    }

    public static RegisterSessionPPx registerSessions() {
        if (registerSessions == null) registerSessions = new RegisterSessionPPx(TeamMember.P_RegisterSessions);
        return registerSessions;
    }

    public static StorePPx store() {
        if (store == null) store = new StorePPx(TeamMember.P_Store);
        return store;
    }

    public static StoreSafeLedgerEntryPPx storeSafeLedgerEntries() {
        if (storeSafeLedgerEntries == null) storeSafeLedgerEntries = new StoreSafeLedgerEntryPPx(TeamMember.P_StoreSafeLedgerEntries);
        return storeSafeLedgerEntries;
    }

    public static StoreSchedulePPx storeSchedules() {
        if (storeSchedules == null) storeSchedules = new StoreSchedulePPx(TeamMember.P_StoreSchedules);
        return storeSchedules;
    }

    public static TillLedgerEntryPPx tillLedgerEntries() {
        if (tillLedgerEntries == null) tillLedgerEntries = new TillLedgerEntryPPx(TeamMember.P_TillLedgerEntries);
        return tillLedgerEntries;
    }

    public static TMPermissionPPx tmPermissions() {
        if (tmPermissions == null) tmPermissions = new TMPermissionPPx(TeamMember.P_TMPermissions);
        return tmPermissions;
    }

    public static String id() {
        String s = TeamMember.P_Id;
        return s;
    }

    public static String created() {
        String s = TeamMember.P_Created;
        return s;
    }

    public static String empNumber() {
        String s = TeamMember.P_EmpNumber;
        return s;
    }

    public static String title() {
        String s = TeamMember.P_Title;
        return s;
    }

    public static String firstName() {
        String s = TeamMember.P_FirstName;
        return s;
    }

    public static String lastName() {
        String s = TeamMember.P_LastName;
        return s;
    }

    public static String inactiveDate() {
        String s = TeamMember.P_InactiveDate;
        return s;
    }

    public static String calcFullName() {
        String s = TeamMember.P_CalcFullName;
        return s;
    }

    public static String calcDisplayName() {
        String s = TeamMember.P_CalcDisplayName;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
