// Generated by OABuilder
package com.corptostore.model.oa.propertypath;
 
import com.corptostore.model.oa.*;
import com.corptostore.model.oa.TesterResultType;
import com.corptostore.model.oa.propertypath.TesterResultPPx;
 
public class TesterResultTypePP {
    private static TesterResultPPx testerResults;
     

    public static TesterResultPPx testerResults() {
        if (testerResults == null) testerResults = new TesterResultPPx(TesterResultType.P_TesterResults);
        return testerResults;
    }

    public static String id() {
        String s = TesterResultType.P_Id;
        return s;
    }

    public static String created() {
        String s = TesterResultType.P_Created;
        return s;
    }

    public static String type() {
        String s = TesterResultType.P_Type;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 