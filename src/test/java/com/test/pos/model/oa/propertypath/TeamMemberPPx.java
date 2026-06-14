package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class TeamMemberPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public TeamMemberPPx(String name) {
        this(null, name);
    }

    public TeamMemberPPx(PPxInterface parent, String name) {
        String s = null;
        if (parent != null) {
            s = parent.toString();
        }
        if (s == null) s = "";
        if (name != null && name.length() > 0) {
            if (s.length() > 0 && name.charAt(0) != ':') s += ".";
            s += name;
        }
        pp = s;
    }

    public AppUserPPx appUser() {
        AppUserPPx ppx = new AppUserPPx(this, TeamMember.P_AppUser);
        return ppx;
    }

    public RegisterSessionPPx registerSessions() {
        RegisterSessionPPx ppx = new RegisterSessionPPx(this, TeamMember.P_RegisterSessions);
        return ppx;
    }

    public StorePPx store() {
        StorePPx ppx = new StorePPx(this, TeamMember.P_Store);
        return ppx;
    }

    public StoreSafeLedgerEntryPPx storeSafeLedgerEntries() {
        StoreSafeLedgerEntryPPx ppx = new StoreSafeLedgerEntryPPx(this, TeamMember.P_StoreSafeLedgerEntries);
        return ppx;
    }

    public StoreSchedulePPx storeSchedules() {
        StoreSchedulePPx ppx = new StoreSchedulePPx(this, TeamMember.P_StoreSchedules);
        return ppx;
    }

    public TillLedgerEntryPPx tillLedgerEntries() {
        TillLedgerEntryPPx ppx = new TillLedgerEntryPPx(this, TeamMember.P_TillLedgerEntries);
        return ppx;
    }

    public TMPermissionPPx tmPermissions() {
        TMPermissionPPx ppx = new TMPermissionPPx(this, TeamMember.P_TMPermissions);
        return ppx;
    }

    public String id() {
        return pp + "." + TeamMember.P_Id;
    }

    public String created() {
        return pp + "." + TeamMember.P_Created;
    }

    public String empNumber() {
        return pp + "." + TeamMember.P_EmpNumber;
    }

    public String title() {
        return pp + "." + TeamMember.P_Title;
    }

    public String firstName() {
        return pp + "." + TeamMember.P_FirstName;
    }

    public String lastName() {
        return pp + "." + TeamMember.P_LastName;
    }

    public String inactiveDate() {
        return pp + "." + TeamMember.P_InactiveDate;
    }

    public String calcFullName() {
        return pp + "." + TeamMember.P_CalcFullName;
    }

    public String calcDisplayName() {
        return pp + "." + TeamMember.P_CalcDisplayName;
    }

    public TeamMemberPPx activeFilter() {
        TeamMemberPPx ppx = new TeamMemberPPx(this, ":active()");
        return ppx;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
