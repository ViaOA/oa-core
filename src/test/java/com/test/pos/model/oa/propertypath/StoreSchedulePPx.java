package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class StoreSchedulePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public StoreSchedulePPx(String name) {
        this(null, name);
    }

    public StoreSchedulePPx(PPxInterface parent, String name) {
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

    public StorePPx store() {
        StorePPx ppx = new StorePPx(this, StoreSchedule.P_Store);
        return ppx;
    }

    public StoreDayEndPPx storeDayEnd() {
        StoreDayEndPPx ppx = new StoreDayEndPPx(this, StoreSchedule.P_StoreDayEnd);
        return ppx;
    }

    public StoreDayOpenPPx storeDayOpen() {
        StoreDayOpenPPx ppx = new StoreDayOpenPPx(this, StoreSchedule.P_StoreDayOpen);
        return ppx;
    }

    public TeamMemberPPx teamMembers() {
        TeamMemberPPx ppx = new TeamMemberPPx(this, StoreSchedule.P_TeamMembers);
        return ppx;
    }

    public String id() {
        return pp + "." + StoreSchedule.P_Id;
    }

    public String created() {
        return pp + "." + StoreSchedule.P_Created;
    }

    public String date() {
        return pp + "." + StoreSchedule.P_Date;
    }

    public String nextStep() {
        return pp + "." + StoreSchedule.P_NextStep;
    }

    public String verifySchedule() {
        return pp + "." + StoreSchedule.P_VerifySchedule;
    }

    public String tillAuditCompleted() {
        return pp + "." + StoreSchedule.P_TillAuditCompleted;
    }

    public String calcDisplay() {
        return pp + "." + StoreSchedule.P_CalcDisplay;
    }

    public String runNextStep() {
        return pp + ".runNextStep";
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
