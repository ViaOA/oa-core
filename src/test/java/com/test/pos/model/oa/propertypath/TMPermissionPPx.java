package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class TMPermissionPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public TMPermissionPPx(String name) {
        this(null, name);
    }

    public TMPermissionPPx(PPxInterface parent, String name) {
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

    public TeamMemberPPx teamMembers() {
        TeamMemberPPx ppx = new TeamMemberPPx(this, TMPermission.P_TeamMembers);
        return ppx;
    }

    public String id() {
        return pp + "." + TMPermission.P_Id;
    }

    public String created() {
        return pp + "." + TMPermission.P_Created;
    }

    public String type() {
        return pp + "." + TMPermission.P_Type;
    }

    public String name() {
        return pp + "." + TMPermission.P_Name;
    }

    public String description() {
        return pp + "." + TMPermission.P_Description;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
