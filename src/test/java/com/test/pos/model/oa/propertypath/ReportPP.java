package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ReportPP {
    private static AppServerPPx appServer;
    private static AppUserPPx appUser;
    private static AppUserErrorPPx appUserError;
    private static AppUserLoginPPx appUserLogin;
    private static ReportClassPPx calcReportClass;
    private static ReportDefPPx reportDef;
     

    public static AppServerPPx appServer() {
        if (appServer == null) appServer = new AppServerPPx(Report.P_AppServer);
        return appServer;
    }

    public static AppUserPPx appUser() {
        if (appUser == null) appUser = new AppUserPPx(Report.P_AppUser);
        return appUser;
    }

    public static AppUserErrorPPx appUserError() {
        if (appUserError == null) appUserError = new AppUserErrorPPx(Report.P_AppUserError);
        return appUserError;
    }

    public static AppUserLoginPPx appUserLogin() {
        if (appUserLogin == null) appUserLogin = new AppUserLoginPPx(Report.P_AppUserLogin);
        return appUserLogin;
    }

    public static ReportClassPPx calcReportClass() {
        if (calcReportClass == null) calcReportClass = new ReportClassPPx(Report.P_CalcReportClass);
        return calcReportClass;
    }

    public static ReportDefPPx reportDef() {
        if (reportDef == null) reportDef = new ReportDefPPx(Report.P_ReportDef);
        return reportDef;
    }

    public static String id() {
        String s = Report.P_Id;
        return s;
    }

    public static String created() {
        String s = Report.P_Created;
        return s;
    }

    public static String generated() {
        String s = Report.P_Generated;
        return s;
    }

    public static String html() {
        String s = Report.P_Html;
        return s;
    }

    public static String generate() {
        String s = "generate";
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
