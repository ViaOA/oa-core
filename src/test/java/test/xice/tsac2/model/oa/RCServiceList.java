// Generated by OABuilder
package test.xice.tsac2.model.oa;
 
import java.sql.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.annotation.*;
import com.viaoa.util.OADateTime;

import test.xice.tsac2.model.oa.filter.*;
import test.xice.tsac2.model.oa.propertypath.*;
 
@OAClass(
    shortName = "rcsl",
    displayName = "RCService List",
    displayProperty = "created"
)
@OATable(
    indexes = {
        @OAIndex(name = "RCServiceListEnvironment", columns = { @OAIndexColumn(name = "EnvironmentId") })
    }
)
public class RCServiceList extends OAObject {
    private static final long serialVersionUID = 1L;
    public static final String PROPERTY_Id = "Id";
    public static final String P_Id = "Id";
    public static final String PROPERTY_Created = "Created";
    public static final String P_Created = "Created";
     
    public static final String PROPERTY_CanRun = "CanRun";
    public static final String P_CanRun = "CanRun";
    public static final String PROPERTY_CanProcess = "CanProcess";
    public static final String P_CanProcess = "CanProcess";
    public static final String PROPERTY_CanLoad = "CanLoad";
    public static final String P_CanLoad = "CanLoad";
     
    public static final String PROPERTY_Environment = "Environment";
    public static final String P_Environment = "Environment";
    public static final String PROPERTY_RCExecute = "RCExecute";
    public static final String P_RCExecute = "RCExecute";
    public static final String PROPERTY_RCServiceListDetails = "RCServiceListDetails";
    public static final String P_RCServiceListDetails = "RCServiceListDetails";
    public static final String PROPERTY_RemoteClient = "RemoteClient";
    public static final String P_RemoteClient = "RemoteClient";
     
    protected int id;
    protected OADateTime created;
     
    // Links to other objects.
    protected transient Environment environment;
    protected transient RCExecute rcExecute;
    protected transient Hub<RCServiceListDetail> hubRCServiceListDetails;
    protected transient RemoteClient remoteClient;
     
    public RCServiceList() {
        if (!isLoading()) {
            setCreated(new OADateTime());
        }
    }
     
    public RCServiceList(int id) {
        this();
        setId(id);
    }
     
    @OAProperty(isUnique = true, displayLength = 5, isProcessed = true)
    @OAId()
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getId() {
        return id;
    }
    
    public void setId(int newValue) {
        fireBeforePropertyChange(P_Id, this.id, newValue);
        int old = id;
        this.id = newValue;
        firePropertyChange(P_Id, old, this.id);
    }
    @OAProperty(defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true)
    @OAColumn(sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getCreated() {
        return created;
    }
    
    public void setCreated(OADateTime newValue) {
        fireBeforePropertyChange(P_Created, this.created, newValue);
        OADateTime old = created;
        this.created = newValue;
        firePropertyChange(P_Created, old, this.created);
    }
    @OACalculatedProperty(displayName = "Can Run", displayLength = 5, properties = {P_RCExecute+"."+RCExecute.P_Started, P_RemoteClient+"."+RemoteClient.P_Status})
    public boolean getCanRun() {
        RCExecute ex = getRCExecute();
        if (ex != null && ex.getStarted() != null) return false;
    
        return getRemoteClient().getStatus() == RemoteClient.STATUS_Connected; 
    }
     
    @OACalculatedProperty(displayName = "Can Process", displayLength = 5, properties = {P_RCExecute+"."+RCExecute.P_Completed, P_RCExecute+"."+RCExecute.P_Loaded})
    public boolean getCanProcess() {
        RCExecute ex = getRCExecute();
        if (ex == null || ex.getCompleted() == null) return false;
        if (ex.getLoaded() != null) return false;
        return true;
    }
     
    @OACalculatedProperty(displayName = "Can Load", displayLength = 5, properties = {P_RCExecute+"."+RCExecute.P_Loaded, P_RCExecute+"."+RCExecute.P_Processed})
    public boolean getCanLoad() {
        RCExecute ex = getRCExecute();
        if (ex == null || ex.getCompleted() == null) return false;
        if (ex.getProcessed() == null) return false;
        if (ex.getLoaded() != null) return false;
        return true;
    }
     
    @OAOne(
        reverseName = Environment.P_RCServiceLists, 
        required = true, 
        allowCreateNew = false
    )
    @OAFkey(columns = {"EnvironmentId"})
    public Environment getEnvironment() {
        if (environment == null) {
            environment = (Environment) getObject(P_Environment);
        }
        return environment;
    }
    
    public void setEnvironment(Environment newValue) {
        fireBeforePropertyChange(P_Environment, this.environment, newValue);
        Environment old = this.environment;
        this.environment = newValue;
        firePropertyChange(P_Environment, old, this.environment);
    }
    
    @OAOne(
        reverseName = RCExecute.P_RCServiceLists
    )
    @OAFkey(columns = {"RcExecuteId"})
    public RCExecute getRCExecute() {
        if (rcExecute == null) {
            rcExecute = (RCExecute) getObject(P_RCExecute);
        }
        return rcExecute;
    }
    
    public void setRCExecute(RCExecute newValue) {
        fireBeforePropertyChange(P_RCExecute, this.rcExecute, newValue);
        RCExecute old = this.rcExecute;
        this.rcExecute = newValue;
        firePropertyChange(P_RCExecute, old, this.rcExecute);
    }
    
    @OAMany(
        displayName = "RCService List Details", 
        toClass = RCServiceListDetail.class, 
        owner = true, 
        reverseName = RCServiceListDetail.P_RCServiceList, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<RCServiceListDetail> getRCServiceListDetails() {
        if (hubRCServiceListDetails == null) {
            hubRCServiceListDetails = (Hub<RCServiceListDetail>) getHub(P_RCServiceListDetails);
        }
        return hubRCServiceListDetails;
    }
    
    @OAOne(
        displayName = "Remote Client", 
        isCalculated = true, 
        reverseName = RemoteClient.P_RCServiceLists, 
        allowCreateNew = false
    )
    public RemoteClient getRemoteClient() {
        if (remoteClient == null) {
            remoteClient = (RemoteClient) getObject(P_RemoteClient);
            if (remoteClient == null) {
            }
        }
        return remoteClient;
    }
    // run - getting RC service listing
    public void run() throws Exception {
    }
     
    // process - Processing RC service listing
    public void process() throws Exception {
    }
     
    // load - Load into database
    public void load() throws Exception {
    }
     
    // selectAll
    public void selectAll() {
        for (RCServiceListDetail detail : getRCServiceListDetails()) {
            detail.setSelected(true);
        }
    }
     
    // deselectAll
    public void deselectAll() {
        for (RCServiceListDetail detail : getRCServiceListDetails()) {
            detail.setSelected(false);
        }
    }
     
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        int environmentFkey = rs.getInt(3);
        if (!rs.wasNull() && environmentFkey > 0) {
            setProperty(P_Environment, new OAObjectKey(environmentFkey));
        }
        int rcExecuteFkey = rs.getInt(4);
        if (!rs.wasNull() && rcExecuteFkey > 0) {
            setProperty(P_RCExecute, new OAObjectKey(rcExecuteFkey));
        }
        if (rs.getMetaData().getColumnCount() != 4) {
            throw new SQLException("invalid number of columns for load method");
        }

        changedFlag = false;
        newFlag = false;
    }
}
 
