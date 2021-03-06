// Generated by OABuilder
package test.xice.tsac3.model.oa.propertypath;
 
import test.xice.tsac3.model.oa.*;
 
public class RemoteMessagePP {
    private static LLADServerPPx lladServer;
    private static RemoteClientPPx remoteClient;
     

    public static LLADServerPPx lladServer() {
        if (lladServer == null) lladServer = new LLADServerPPx(RemoteMessage.P_LLADServer);
        return lladServer;
    }

    public static RemoteClientPPx remoteClient() {
        if (remoteClient == null) remoteClient = new RemoteClientPPx(RemoteMessage.P_RemoteClient);
        return remoteClient;
    }

    public static String id() {
        String s = RemoteMessage.P_Id;
        return s;
    }

    public static String created() {
        String s = RemoteMessage.P_Created;
        return s;
    }

    public static String name() {
        String s = RemoteMessage.P_Name;
        return s;
    }

    public static String message() {
        String s = RemoteMessage.P_Message;
        return s;
    }

    public static String error() {
        String s = RemoteMessage.P_Error;
        return s;
    }
}
 
