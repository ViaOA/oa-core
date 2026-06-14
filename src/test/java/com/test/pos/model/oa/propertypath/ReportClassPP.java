package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ReportClassPP {
    private static ReportPPx calcReports;
    private static ReportDefPPx reportDefs;
     

    public static ReportPPx calcReports() {
        if (calcReports == null) calcReports = new ReportPPx(ReportClass.P_CalcReports);
        return calcReports;
    }

    public static ReportDefPPx reportDefs() {
        if (reportDefs == null) reportDefs = new ReportDefPPx(ReportClass.P_ReportDefs);
        return reportDefs;
    }

    public static String id() {
        String s = ReportClass.P_Id;
        return s;
    }

    public static String created() {
        String s = ReportClass.P_Created;
        return s;
    }

    public static String name() {
        String s = ReportClass.P_Name;
        return s;
    }

    public static String className() {
        String s = ReportClass.P_ClassName;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
