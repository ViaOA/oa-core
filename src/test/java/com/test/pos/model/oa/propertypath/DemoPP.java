package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class DemoPP {
    private static DemoNodePPx demoNodes;
     

    public static DemoNodePPx demoNodes() {
        if (demoNodes == null) demoNodes = new DemoNodePPx(Demo.P_DemoNodes);
        return demoNodes;
    }

    public static String id() {
        String s = Demo.P_Id;
        return s;
    }

    public static String created() {
        String s = Demo.P_Created;
        return s;
    }

    public static String started() {
        String s = Demo.P_Started;
        return s;
    }

    public static String paused() {
        String s = Demo.P_Paused;
        return s;
    }

    public static String stopped() {
        String s = Demo.P_Stopped;
        return s;
    }

    public static String console() {
        String s = Demo.P_Console;
        return s;
    }

    public static String start() {
        String s = "start";
        return s;
    }

    public static String pause() {
        String s = "pause";
        return s;
    }

    public static String stop() {
        String s = "stop";
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
