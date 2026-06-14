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
    lowerName = "demo",
    pluralName = "Demos",
    shortName = "dm",
    displayName = "Demo",
    displayProperty = "created",
    singleton = true,
    pojoSingleton = true,
    noPojo = true
)
@OATable(
)
public class Demo extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(Demo.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Started = "started";
    public static final String P_Paused = "paused";
    public static final String P_Stopped = "stopped";
    public static final String P_Console = "console";
     
    public static final String P_DemoNodes = "demoNodes";
     
    public static final String M_Start = "start";
    public static final String M_Pause = "pause";
    public static final String M_Stop = "stop";
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile OADateTime started;
    protected volatile OADateTime paused;
    protected volatile OADateTime stopped;
    protected volatile String console;
     
    // Links to other objects.
    protected transient Hub<DemoNode> hubDemoNodes;
     
    public Demo() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public Demo(int id) {
        this();
        setId(id);
    }

    @OAProperty(lowerName = "id", isUnique = true, trackPrimitiveNull = false, displayLength = 6)
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

    @OAProperty(lowerName = "created", defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true, ignoreTimeZone = true)
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

    @OAProperty(lowerName = "started", displayLength = 15, ignoreTimeZone = true)
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

    @OAProperty(lowerName = "paused", displayLength = 15, ignoreTimeZone = true)
    @OAColumn(name = "Paused", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getPaused() {
        return paused;
    }
    public void setPaused(OADateTime newValue) {
        OADateTime old = paused;
        fireBeforePropertyChange(P_Paused, old, newValue);
        this.paused = newValue;
        firePropertyChange(P_Paused, old, this.paused);
    }

    @OAProperty(lowerName = "stopped", displayLength = 15, ignoreTimeZone = true)
    @OAColumn(name = "Stopped", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getStopped() {
        return stopped;
    }
    public void setStopped(OADateTime newValue) {
        OADateTime old = stopped;
        fireBeforePropertyChange(P_Stopped, old, newValue);
        this.stopped = newValue;
        firePropertyChange(P_Stopped, old, this.stopped);
    }

    @OAProperty(lowerName = "console", displayLength = 20)
    public String getConsole() {
        return console;
    }
    public void setConsole(String newValue) {
        String old = console;
        fireBeforePropertyChange(P_Console, old, newValue);
        this.console = newValue;
        firePropertyChange(P_Console, old, this.console);
    }

    @OAMany(
        displayName = "Demo Nodes", 
        toClass = DemoNode.class, 
        owner = true, 
        reverseName = DemoNode.P_Demo, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<DemoNode> getDemoNodes() {
        if (hubDemoNodes == null) {
            hubDemoNodes = (Hub<DemoNode>) getHub(P_DemoNodes);
        }
        return hubDemoNodes;
    }
    @OAMethod(displayName = "Start")
    public void start() throws Exception {
        // custom code
        OADateTime dt = this.getStarted();
        if (dt == null) dt = new OADateTime();
        else dt = null;
        setStarted(dt);
    }

    @OAMethod(displayName = "Pause")
    public void pause() throws Exception {
        // custom code
        OADateTime dt = this.getPaused();
        if (dt == null) dt = new OADateTime();
        else dt = null;
        setPaused(dt);
    }

    @OAMethod(displayName = "Stop")
    public void stop() throws Exception {
        // custom code
        OADateTime dt = this.getStopped();
        if (dt == null) dt = new OADateTime();
        else dt = null;
        setStopped(dt);
    }

    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        timestamp = rs.getTimestamp(3);
        if (timestamp != null) this.started = new OADateTime(timestamp);
        timestamp = rs.getTimestamp(4);
        if (timestamp != null) this.paused = new OADateTime(timestamp);
        timestamp = rs.getTimestamp(5);
        if (timestamp != null) this.stopped = new OADateTime(timestamp);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
