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
    lowerName = "cronProcess",
    pluralName = "CronProcesses",
    shortName = "crp",
    displayName = "Cron Process",
    displayProperty = "created",
    singleton = true,
    pojoSingleton = true,
    noPojo = true
)
@OATable(
)
public class CronProcess extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(CronProcess.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Description = "description";
    public static final String P_Enabled = "enabled";
    public static final String P_LastBegin = "lastBegin";
    public static final String P_LastEnd = "lastEnd";
    public static final String P_Console = "console";
     
    public static final String M_Run = "run";
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String description;
    protected volatile boolean enabled;
    protected volatile OADateTime lastBegin;
    protected volatile OADateTime lastEnd;
    protected volatile String console;
     
    public CronProcess() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
        setEnabled(true);
    }
     
    public CronProcess(int id) {
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

    @OAProperty(lowerName = "description", maxLength = 120, displayLength = 22, uiColumnLength = 20)
    @OAColumn(name = "Description", maxLength = 120)
    public String getDescription() {
        return description;
    }
    public void setDescription(String newValue) {
        String old = description;
        fireBeforePropertyChange(P_Description, old, newValue);
        this.description = newValue;
        firePropertyChange(P_Description, old, this.description);
    }

    @OAProperty(lowerName = "enabled", defaultValue = "true", displayLength = 5, uiColumnLength = 7)
    @OAColumn(name = "Enabled", sqlType = java.sql.Types.BOOLEAN)
    public boolean getEnabled() {
        return enabled;
    }
    public boolean isEnabled() {
        return getEnabled();
    }
    public void setEnabled(boolean newValue) {
        boolean old = enabled;
        fireBeforePropertyChange(P_Enabled, old, newValue);
        this.enabled = newValue;
        firePropertyChange(P_Enabled, old, this.enabled);
    }

    @OAProperty(lowerName = "lastBegin", displayName = "Last Begin", displayLength = 15, isProcessed = true, ignoreTimeZone = true)
    @OAColumn(name = "LastBegin", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getLastBegin() {
        return lastBegin;
    }
    public void setLastBegin(OADateTime newValue) {
        OADateTime old = lastBegin;
        fireBeforePropertyChange(P_LastBegin, old, newValue);
        this.lastBegin = newValue;
        firePropertyChange(P_LastBegin, old, this.lastBegin);
    }

    @OAProperty(lowerName = "lastEnd", displayName = "Last End", displayLength = 15, isProcessed = true, ignoreTimeZone = true)
    @OAColumn(name = "LastEnd", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getLastEnd() {
        return lastEnd;
    }
    public void setLastEnd(OADateTime newValue) {
        OADateTime old = lastEnd;
        fireBeforePropertyChange(P_LastEnd, old, newValue);
        this.lastEnd = newValue;
        firePropertyChange(P_LastEnd, old, this.lastEnd);
    }

    @OAProperty(lowerName = "console", maxLength = 175, displayLength = 50, uiColumnLength = 20, isReadOnly = true)
    public String getConsole() {
        return console;
    }
    public void setConsole(String newValue) {
        String old = console;
        fireBeforePropertyChange(P_Console, old, newValue);
        this.console = newValue;
        firePropertyChange(P_Console, old, this.console);
    }
    @OAMethod(displayName = "Run")
    public void run() throws Exception {
        // use this to run on server
        if (isRemoteAvailable()) {
            remote();
            return;
        }
        // custom code
        CronProcessDelegate.run(this);
    }

    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.description = rs.getString(3);
        this.enabled = rs.getBoolean(4);
        setPrimitiveNull(P_Enabled, rs.wasNull());
        timestamp = rs.getTimestamp(5);
        if (timestamp != null) this.lastBegin = new OADateTime(timestamp);
        timestamp = rs.getTimestamp(6);
        if (timestamp != null) this.lastEnd = new OADateTime(timestamp);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
