package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class DemoNodePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public DemoNodePPx(String name) {
        this(null, name);
    }

    public DemoNodePPx(PPxInterface parent, String name) {
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

    public DemoPPx demo() {
        DemoPPx ppx = new DemoPPx(this, DemoNode.P_Demo);
        return ppx;
    }

    public String id() {
        return pp + "." + DemoNode.P_Id;
    }

    public String created() {
        return pp + "." + DemoNode.P_Created;
    }

    public String type() {
        return pp + "." + DemoNode.P_Type;
    }

    public String name() {
        return pp + "." + DemoNode.P_Name;
    }

    public String started() {
        return pp + "." + DemoNode.P_Started;
    }

    public String paused() {
        return pp + "." + DemoNode.P_Paused;
    }

    public String stopped() {
        return pp + "." + DemoNode.P_Stopped;
    }

    public String disconnect() {
        return pp + "." + DemoNode.P_Disconnect;
    }

    public String showOutput() {
        return pp + "." + DemoNode.P_ShowOutput;
    }

    public String console() {
        return pp + "." + DemoNode.P_Console;
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
 
