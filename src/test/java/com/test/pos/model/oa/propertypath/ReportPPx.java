package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class ReportPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ReportPPx(String name) {
        this(null, name);
    }

    public ReportPPx(PPxInterface parent, String name) {
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

    public AppServerPPx appServer() {
        AppServerPPx ppx = new AppServerPPx(this, Report.P_AppServer);
        return ppx;
    }

    public AppUserPPx appUser() {
        AppUserPPx ppx = new AppUserPPx(this, Report.P_AppUser);
        return ppx;
    }

    public AppUserErrorPPx appUserError() {
        AppUserErrorPPx ppx = new AppUserErrorPPx(this, Report.P_AppUserError);
        return ppx;
    }

    public AppUserLoginPPx appUserLogin() {
        AppUserLoginPPx ppx = new AppUserLoginPPx(this, Report.P_AppUserLogin);
        return ppx;
    }

    public ReportClassPPx calcReportClass() {
        ReportClassPPx ppx = new ReportClassPPx(this, Report.P_CalcReportClass);
        return ppx;
    }

    public ReportDefPPx reportDef() {
        ReportDefPPx ppx = new ReportDefPPx(this, Report.P_ReportDef);
        return ppx;
    }

    public String id() {
        return pp + "." + Report.P_Id;
    }

    public String created() {
        return pp + "." + Report.P_Created;
    }

    public String generated() {
        return pp + "." + Report.P_Generated;
    }

    public String html() {
        return pp + "." + Report.P_Html;
    }

    public String generate() {
        return pp + ".generate";
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
