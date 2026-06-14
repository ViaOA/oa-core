package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class RegisterPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public RegisterPPx(String name) {
        this(null, name);
    }

    public RegisterPPx(PPxInterface parent, String name) {
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

    public RegisterSessionPPx registerSessions() {
        RegisterSessionPPx ppx = new RegisterSessionPPx(this, Register.P_RegisterSessions);
        return ppx;
    }

    public StorePPx store() {
        StorePPx ppx = new StorePPx(this, Register.P_Store);
        return ppx;
    }

    public TillPPx till() {
        TillPPx ppx = new TillPPx(this, Register.P_Till);
        return ppx;
    }

    public String id() {
        return pp + "." + Register.P_Id;
    }

    public String created() {
        return pp + "." + Register.P_Created;
    }

    public String code() {
        return pp + "." + Register.P_Code;
    }

    public String delete() {
        return pp + "." + Register.P_Delete;
    }

    public String deleteReason() {
        return pp + "." + Register.P_DeleteReason;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
