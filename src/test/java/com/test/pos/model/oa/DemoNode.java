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
    lowerName = "demoNode",
    pluralName = "DemoNodes",
    shortName = "dmn",
    displayName = "Demo Node",
    displayProperty = "name",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "DemoNodeDemo", fkey = true, columns = { @OAIndexColumn(name = "DemoId") })
    }
)
public class DemoNode extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(DemoNode.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Type = "type";
    public static final String P_TypeString = "typeString";
    public static final String P_TypeEnum = "typeEnum";
    public static final String P_TypeDisplay = "typeDisplay";
    public static final String P_Name = "name";
    public static final String P_Started = "started";
    public static final String P_Paused = "paused";
    public static final String P_Stopped = "stopped";
    public static final String P_Disconnect = "disconnect";
    public static final String P_ShowOutput = "showOutput";
    public static final String P_Console = "console";
     
    public static final String P_Demo = "demo";
    public static final String P_DemoId = "demoId"; // fkey
     
    public static final String M_Start = "start";
    public static final String M_Pause = "pause";
    public static final String M_Stop = "stop";
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int type;

    public static enum Type {
        Unknown("Unknown"),
        CorpServer("Corp Server"),
        StoreServer("Store Server"),
        OffsiteServer("Offsite Server"),
        CorpClient("Corp Client"),
        StoreClient("Store Client"),
        OffsiteClient("Offsite Client");

        private String display;
        Type(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int TYPE_Unknown = 0;
    public static final int TYPE_CorpServer = 1;
    public static final int TYPE_StoreServer = 2;
    public static final int TYPE_OffsiteServer = 3;
    public static final int TYPE_CorpClient = 4;
    public static final int TYPE_StoreClient = 5;
    public static final int TYPE_OffsiteClient = 6;

    protected volatile String name;
    protected volatile OADateTime started;
    protected volatile OADateTime paused;
    protected volatile OADateTime stopped;
    protected volatile OADateTime disconnect;
    protected volatile boolean showOutput;
    protected volatile String console;
     
    // Links to other objects.
    protected volatile transient Demo demo;
     
    public DemoNode() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public DemoNode(int id) {
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

    @OAProperty(lowerName = "type", displayLength = 6, isNameValue = true)
    @OAColumn(name = "Type", sqlType = java.sql.Types.INTEGER)
    public int getType() {
        return type;
    }
    public void setType(int newValue) {
        int old = type;
        fireBeforePropertyChange(P_Type, old, newValue);
        this.type = newValue;
        firePropertyChange(P_Type, old, this.type);
    }

    @OAProperty(enumPropertyName = P_Type)
    public String getTypeString() {
        Type type = getTypeEnum();
        if (type == null) return null;
        return type.name();
    }
    public void setTypeString(String val) {
        int x = -1;
        if (OAString.isNotEmpty(val)) {
            Type type = Type.valueOf(val);
            if (type != null) x = type.ordinal();
        }
        if (x < 0) setNull(P_Type);
        else setType(x);
    }
    @OAProperty(enumPropertyName = P_Type)
    public Type getTypeEnum() {
        if (isNull(P_Type)) return null;
        final int val = getType();
        if (val < 0 || val >= Type.values().length) return null;
        return Type.values()[val];
    }
    public void setTypeEnum(Type val) {
        if (val == null) {
            setNull(P_Type);
        }
        else {
            setType(val.ordinal());
        }
    }
    @OACalculatedProperty(enumPropertyName = P_Type, displayName = "Type", displayLength = 6, columnLength = 6, properties = {P_Type} )
    public String getTypeDisplay() {
        Type type = getTypeEnum();
        if (type == null) return null;
        return type.getDisplay();
    }

    @OAProperty(lowerName = "name", displayLength = 20)
    @OAColumn(name = "Name", maxLength = 0)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
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

    @OAProperty(lowerName = "disconnect", displayLength = 15, ignoreTimeZone = true)
    @OAColumn(name = "Disconnect", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getDisconnect() {
        return disconnect;
    }
    public void setDisconnect(OADateTime newValue) {
        OADateTime old = disconnect;
        fireBeforePropertyChange(P_Disconnect, old, newValue);
        this.disconnect = newValue;
        firePropertyChange(P_Disconnect, old, this.disconnect);
    }

    @OAProperty(lowerName = "showOutput", displayName = "Show Output", trackPrimitiveNull = false, displayLength = 5, uiColumnLength = 11)
    @OAColumn(name = "ShowOutput", sqlType = java.sql.Types.BOOLEAN)
    public boolean getShowOutput() {
        return showOutput;
    }
    public boolean isShowOutput() {
        return getShowOutput();
    }
    public void setShowOutput(boolean newValue) {
        boolean old = showOutput;
        fireBeforePropertyChange(P_ShowOutput, old, newValue);
        this.showOutput = newValue;
        firePropertyChange(P_ShowOutput, old, this.showOutput);
    }

    @OAProperty(lowerName = "console", uiColumnLength = 7)
    public String getConsole() {
        return console;
    }
    public void setConsole(String newValue) {
        String old = console;
        fireBeforePropertyChange(P_Console, old, newValue);
        this.console = newValue;
        firePropertyChange(P_Console, old, this.console);
    }

    @OAOne(
        reverseName = Demo.P_DemoNodes, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_DemoId, toProperty = Demo.P_Id)}
    )
    public Demo getDemo() {
        if (demo == null) {
            demo = (Demo) getObject(P_Demo);
        }
        return demo;
    }
    public void setDemo(Demo newValue) {
        Demo old = this.demo;
        fireBeforePropertyChange(P_Demo, old, newValue);
        this.demo = newValue;
        firePropertyChange(P_Demo, old, this.demo);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "DemoId")
    public Integer getDemoId() {
        return (Integer) getFkeyProperty(P_DemoId);
    }
    public void setDemoId(Integer newValue) {
        this.demo = null;
        setFkeyProperty(P_DemoId, newValue);
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
        this.type = rs.getInt(3);
        setPrimitiveNull(P_Type, rs.wasNull());
        this.name = rs.getString(4);
        timestamp = rs.getTimestamp(5);
        if (timestamp != null) this.started = new OADateTime(timestamp);
        timestamp = rs.getTimestamp(6);
        if (timestamp != null) this.paused = new OADateTime(timestamp);
        timestamp = rs.getTimestamp(7);
        if (timestamp != null) this.stopped = new OADateTime(timestamp);
        timestamp = rs.getTimestamp(8);
        if (timestamp != null) this.disconnect = new OADateTime(timestamp);
        this.showOutput = rs.getBoolean(9);
        int demoFkey = rs.getInt(10);
        setFkeyProperty(P_Demo, rs.wasNull() ? null : demoFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
