package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class CronProcessPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public CronProcessPPx(String name) {
        this(null, name);
    }

    public CronProcessPPx(PPxInterface parent, String name) {
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

    public String id() {
        return pp + "." + CronProcess.P_Id;
    }

    public String created() {
        return pp + "." + CronProcess.P_Created;
    }

    public String description() {
        return pp + "." + CronProcess.P_Description;
    }

    public String enabled() {
        return pp + "." + CronProcess.P_Enabled;
    }

    public String lastBegin() {
        return pp + "." + CronProcess.P_LastBegin;
    }

    public String lastEnd() {
        return pp + "." + CronProcess.P_LastEnd;
    }

    public String console() {
        return pp + "." + CronProcess.P_Console;
    }

    public String run() {
        return pp + ".run";
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
