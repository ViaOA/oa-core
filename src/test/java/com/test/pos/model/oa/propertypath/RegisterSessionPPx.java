package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class RegisterSessionPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public RegisterSessionPPx(String name) {
        this(null, name);
    }

    public RegisterSessionPPx(PPxInterface parent, String name) {
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

    public InvoicePPx invoices() {
        InvoicePPx ppx = new InvoicePPx(this, RegisterSession.P_Invoices);
        return ppx;
    }

    public RefundPPx refunds() {
        RefundPPx ppx = new RefundPPx(this, RegisterSession.P_Refunds);
        return ppx;
    }

    public RegisterPPx register() {
        RegisterPPx ppx = new RegisterPPx(this, RegisterSession.P_Register);
        return ppx;
    }

    public TeamMemberPPx teamMember() {
        TeamMemberPPx ppx = new TeamMemberPPx(this, RegisterSession.P_TeamMember);
        return ppx;
    }

    public TillLedgerEntryPPx tillLedgerEntries() {
        TillLedgerEntryPPx ppx = new TillLedgerEntryPPx(this, RegisterSession.P_TillLedgerEntries);
        return ppx;
    }

    public String id() {
        return pp + "." + RegisterSession.P_Id;
    }

    public String created() {
        return pp + "." + RegisterSession.P_Created;
    }

    public String ended() {
        return pp + "." + RegisterSession.P_Ended;
    }

    public RegisterSessionPPx openFilter() {
        RegisterSessionPPx ppx = new RegisterSessionPPx(this, ":open()");
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
 
