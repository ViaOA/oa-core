// Generated by OABuilder
package test.xice.tsac.model.oa.propertypath;
 
import test.xice.tsac.model.oa.*;
 
public class SchedulePP {
    private static ApplicationPPx application;
    private static ApplicationGroupPPx applicationGroup;
     

    public static ApplicationPPx application() {
        if (application == null) application = new ApplicationPPx(Schedule.P_Application);
        return application;
    }

    public static ApplicationGroupPPx applicationGroup() {
        if (applicationGroup == null) applicationGroup = new ApplicationGroupPPx(Schedule.P_ApplicationGroup);
        return applicationGroup;
    }

    public static String id() {
        String s = Schedule.P_Id;
        return s;
    }

    public static String name() {
        String s = Schedule.P_Name;
        return s;
    }

    public static String description() {
        String s = Schedule.P_Description;
        return s;
    }

    public static String stopTime() {
        String s = Schedule.P_StopTime;
        return s;
    }

    public static String startTime() {
        String s = Schedule.P_StartTime;
        return s;
    }

    public static String sunday() {
        String s = Schedule.P_Sunday;
        return s;
    }

    public static String monday() {
        String s = Schedule.P_Monday;
        return s;
    }

    public static String tuesday() {
        String s = Schedule.P_Tuesday;
        return s;
    }

    public static String wednesday() {
        String s = Schedule.P_Wednesday;
        return s;
    }

    public static String thursday() {
        String s = Schedule.P_Thursday;
        return s;
    }

    public static String friday() {
        String s = Schedule.P_Friday;
        return s;
    }

    public static String saturday() {
        String s = Schedule.P_Saturday;
        return s;
    }

    public static String activeBeginDate() {
        String s = Schedule.P_ActiveBeginDate;
        return s;
    }

    public static String activeEndDate() {
        String s = Schedule.P_ActiveEndDate;
        return s;
    }

    public static String realStartTime() {
        String s = Schedule.P_RealStartTime;
        return s;
    }

    public static String realStopTime() {
        String s = Schedule.P_RealStopTime;
        return s;
    }

    public static String isForToday() {
        String s = Schedule.P_IsForToday;
        return s;
    }
}
 
