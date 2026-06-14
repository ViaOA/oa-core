package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class DemoPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public DemoPPx(String name) {
        this(null, name);
    }

    public DemoPPx(PPxInterface parent, String name) {
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

    public DemoNodePPx demoNodes() {
        DemoNodePPx ppx = new DemoNodePPx(this, Demo.P_DemoNodes);
        return ppx;
    }

    public String id() {
        return pp + "." + Demo.P_Id;
    }

    public String created() {
        return pp + "." + Demo.P_Created;
    }

    public String started() {
        return pp + "." + Demo.P_Started;
    }

    public String paused() {
        return pp + "." + Demo.P_Paused;
    }

    public String stopped() {
        return pp + "." + Demo.P_Stopped;
    }

    public String console() {
        return pp + "." + Demo.P_Console;
    }

    public String start() {
        return pp + ".start";
    }

    public String pause() {
        return pp + ".pause";
    }

    public String stop() {
        return pp + ".stop";
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
