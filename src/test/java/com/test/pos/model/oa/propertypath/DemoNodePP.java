package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class DemoNodePP {
    private static DemoPPx demo;
     

    public static DemoPPx demo() {
        if (demo == null) demo = new DemoPPx(DemoNode.P_Demo);
        return demo;
    }

    public static String id() {
        String s = DemoNode.P_Id;
        return s;
    }

    public static String created() {
        String s = DemoNode.P_Created;
        return s;
    }

    public static String type() {
        String s = DemoNode.P_Type;
        return s;
    }

    public static String name() {
        String s = DemoNode.P_Name;
        return s;
    }

    public static String started() {
        String s = DemoNode.P_Started;
        return s;
    }

    public static String paused() {
        String s = DemoNode.P_Paused;
        return s;
    }

    public static String stopped() {
        String s = DemoNode.P_Stopped;
        return s;
    }

    public static String disconnect() {
        String s = DemoNode.P_Disconnect;
        return s;
    }

    public static String showOutput() {
        String s = DemoNode.P_ShowOutput;
        return s;
    }

    public static String console() {
        String s = DemoNode.P_Console;
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
 
