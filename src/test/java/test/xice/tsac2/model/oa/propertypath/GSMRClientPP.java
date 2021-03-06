// Generated by OABuilder
package test.xice.tsac2.model.oa.propertypath;
 
import test.xice.tsac2.model.oa.*;
 
public class GSMRClientPP {
    private static ApplicationPPx application;
    private static GSMRServerPPx gsmrServer;
    private static GSRequestPPx gSRequests;
     

    public static ApplicationPPx application() {
        if (application == null) application = new ApplicationPPx(GSMRClient.P_Application);
        return application;
    }

    public static GSMRServerPPx gsmrServer() {
        if (gsmrServer == null) gsmrServer = new GSMRServerPPx(GSMRClient.P_GSMRServer);
        return gsmrServer;
    }

    public static GSRequestPPx gSRequests() {
        if (gSRequests == null) gSRequests = new GSRequestPPx(GSMRClient.P_GSRequests);
        return gSRequests;
    }

    public static String id() {
        String s = GSMRClient.P_Id;
        return s;
    }

    public static String connectionId() {
        String s = GSMRClient.P_ConnectionId;
        return s;
    }

    public static String clientType() {
        String s = GSMRClient.P_ClientType;
        return s;
    }

    public static String clientDescription() {
        String s = GSMRClient.P_ClientDescription;
        return s;
    }

    public static String totalRequests() {
        String s = GSMRClient.P_TotalRequests;
        return s;
    }

    public static String totalRequestTime() {
        String s = GSMRClient.P_TotalRequestTime;
        return s;
    }
}
 
