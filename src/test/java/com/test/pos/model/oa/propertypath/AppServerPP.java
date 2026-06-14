package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class AppServerPP {
    private static AppUserLoginPPx appUserLogin;
    private static ReportPPx reports;
     

    public static AppUserLoginPPx appUserLogin() {
        if (appUserLogin == null) appUserLogin = new AppUserLoginPPx(AppServer.P_AppUserLogin);
        return appUserLogin;
    }

    public static ReportPPx reports() {
        if (reports == null) reports = new ReportPPx(AppServer.P_Reports);
        return reports;
    }

    public static String id() {
        String s = AppServer.P_Id;
        return s;
    }

    public static String created() {
        String s = AppServer.P_Created;
        return s;
    }

    public static String started() {
        String s = AppServer.P_Started;
        return s;
    }

    public static String demoMode() {
        String s = AppServer.P_DemoMode;
        return s;
    }

    public static String testOnly() {
        String s = AppServer.P_TestOnly;
        return s;
    }

    public static String release() {
        String s = AppServer.P_Release;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
