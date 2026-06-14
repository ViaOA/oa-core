package com.test.pos.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.annotation.*;
import com.viaoa.lang.*;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.datetime.OADateTime;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "appServer",
    pluralName = "AppServers",
    shortName = "as",
    displayName = "App Server",
    isLookup = true,
    isPreSelect = true,
    isProcessed = true,
    displayProperty = "appUserLogin.hostName",
    singleton = true,
    pojoSingleton = true,
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "AppServerAppUserLogin", fkey = true, columns = { @OAIndexColumn(name = "AppUserLoginId") })
    }
)
public class AppServer extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(AppServer.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Started = "started";
    public static final String P_DemoMode = "demoMode";
    public static final String P_TestOnly = "testOnly";
    public static final String P_Release = "release";
     
    public static final String P_AppUserLogin = "appUserLogin";
    public static final String P_AppUserLoginId = "appUserLoginId"; // fkey
    public static final String P_Reports = "reports";
    public static final String P_ReportsId = "reportsId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile OADateTime started;
    protected volatile boolean demoMode;
    protected volatile boolean testOnly;
    protected volatile String release;
     
    // Links to other objects.
    protected volatile transient AppUserLogin appUserLogin;
    protected transient Hub<Report> hubReports;
     
    public AppServer() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public AppServer(int id) {
        this();
        setId(id);
    }

    @OAProperty(lowerName = "id", isUnique = true, displayLength = 5)
    @OAId
    @OAColumn(name = "Id", sqlType = java.sql.Types.INTEGER)
    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        int old = id;
        fireBeforePropertyChange(P_Id, old, newValue);
        this.id = newValue;
        firePropertyChange(P_Id, old, this.id);
    }

    @OAProperty(lowerName = "created", defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true)
    @OAColumn(name = "Created", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getCreated() {
        return created;
    }
    public void setCreated(OADateTime newValue) {
        OADateTime old = created;
        fireBeforePropertyChange(P_Created, old, newValue);
        this.created = newValue;
        firePropertyChange(P_Created, old, this.created);
    }

    @OAProperty(lowerName = "started", displayLength = 15, isProcessed = true)
    @OAColumn(name = "Started", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getStarted() {
        return started;
    }
    public void setStarted(OADateTime newValue) {
        OADateTime old = started;
        fireBeforePropertyChange(P_Started, old, newValue);
        this.started = newValue;
        firePropertyChange(P_Started, old, this.started);
    }

    @OAProperty(lowerName = "demoMode", displayName = "Demo Mode", displayLength = 5, isProcessed = true)
    @OAColumn(name = "DemoMode", sqlType = java.sql.Types.BOOLEAN)
    public boolean getDemoMode() {
        return demoMode;
    }
    public boolean isDemoMode() {
        return getDemoMode();
    }
    public void setDemoMode(boolean newValue) {
        boolean old = demoMode;
        fireBeforePropertyChange(P_DemoMode, old, newValue);
        this.demoMode = newValue;
        firePropertyChange(P_DemoMode, old, this.demoMode);
    }

    @OAProperty(lowerName = "testOnly", displayName = "Test Only", displayLength = 5, uiColumnLength = 9)
    @OAColumn(name = "TestOnly", sqlType = java.sql.Types.BOOLEAN)
    public boolean getTestOnly() {
        return testOnly;
    }
    public boolean isTestOnly() {
        return getTestOnly();
    }
    public void setTestOnly(boolean newValue) {
        boolean old = testOnly;
        fireBeforePropertyChange(P_TestOnly, old, newValue);
        this.testOnly = newValue;
        firePropertyChange(P_TestOnly, old, this.testOnly);
    }

    @OAProperty(lowerName = "release", maxLength = 18, displayLength = 12, uiColumnLength = 8, isProcessed = true)
    @OAColumn(name = "Release", maxLength = 18)
    public String getRelease() {
        return release;
    }
    public void setRelease(String newValue) {
        String old = release;
        fireBeforePropertyChange(P_Release, old, newValue);
        this.release = newValue;
        firePropertyChange(P_Release, old, this.release);
    }

    @OAOne(
        displayName = "App User Login", 
        reverseName = AppUserLogin.P_AppServers, 
        isProcessed = true, 
        fkeys = {@OAFkey(fromProperty = P_AppUserLoginId, toProperty = AppUserLogin.P_Id)}
    )
    public AppUserLogin getAppUserLogin() {
        if (appUserLogin == null) {
            appUserLogin = (AppUserLogin) getObject(P_AppUserLogin);
        }
        return appUserLogin;
    }
    public void setAppUserLogin(AppUserLogin newValue) {
        AppUserLogin old = this.appUserLogin;
        fireBeforePropertyChange(P_AppUserLogin, old, newValue);
        this.appUserLogin = newValue;
        firePropertyChange(P_AppUserLogin, old, this.appUserLogin);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "AppUserLoginId")
    public Integer getAppUserLoginId() {
        return (Integer) getFkeyProperty(P_AppUserLoginId);
    }
    public void setAppUserLoginId(Integer newValue) {
        this.appUserLogin = null;
        setFkeyProperty(P_AppUserLoginId, newValue);
    }

    @OAMany(
        toClass = Report.class, 
        reverseName = Report.P_AppServer
    )
    @OALinkTable(name = "AppServerReport", indexName = "ReportAppServer", columns = {"AppServerId"})
    public Hub<Report> getReports() {
        if (hubReports == null) {
            hubReports = (Hub<Report>) getHub(P_Reports);
        }
        return hubReports;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        timestamp = rs.getTimestamp(3);
        if (timestamp != null) this.started = new OADateTime(timestamp);
        this.demoMode = rs.getBoolean(4);
        setPrimitiveNull(P_DemoMode, rs.wasNull());
        this.testOnly = rs.getBoolean(5);
        setPrimitiveNull(P_TestOnly, rs.wasNull());
        this.release = rs.getString(6);
        int appUserLoginFkey = rs.getInt(7);
        setFkeyProperty(P_AppUserLogin, rs.wasNull() ? null : appUserLoginFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
