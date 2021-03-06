// Generated by OABuilder
package test.xice.tsac2.model.oa.propertypath;
 
import java.io.Serializable;

import test.xice.tsac2.model.oa.*;
 
public class ApplicationStatusPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ApplicationStatusPPx(String name) {
        this(null, name);
    }

    public ApplicationStatusPPx(PPxInterface parent, String name) {
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

    public ApplicationPPx applications() {
        ApplicationPPx ppx = new ApplicationPPx(this, ApplicationStatus.P_Applications);
        return ppx;
    }

    public String id() {
        return pp + "." + ApplicationStatus.P_Id;
    }

    public String created() {
        return pp + "." + ApplicationStatus.P_Created;
    }

    public String name() {
        return pp + "." + ApplicationStatus.P_Name;
    }

    public String type() {
        return pp + "." + ApplicationStatus.P_Type;
    }

    public String color() {
        return pp + "." + ApplicationStatus.P_Color;
    }

    @Override
    public String toString() {
        return pp;
    }
}
 
