// Generated by OABuilder
package com.corptostore.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.scheduler.*;
import com.viaoa.util.*;
import com.corptostore.delegate.oa.*;
import com.corptostore.model.oa.filter.*;
import com.corptostore.model.oa.propertypath.*;
import com.corptostore.delegate.oa.EnvironmentDelegate;
import com.corptostore.model.oa.CorpToStore;
import com.corptostore.model.oa.DashboardLine;
import com.corptostore.model.oa.Environment;
import com.corptostore.model.oa.Tester;
import com.corptostore.model.oa.filter.EnvironmentNonProdFilter;
import com.viaoa.annotation.*;
import com.viaoa.util.OADateTime;
import java.awt.Color;
import com.viaoa.util.OAConverter;
 
@OAClass(
    lowerName = "environment",
    pluralName = "Environments",
    shortName = "env",
    displayName = "Environment",
    isLookup = true,
    isPreSelect = true,
    useDataSource = false,
    displayProperty = "name",
    filterClasses = {EnvironmentNonProdFilter.class}
)
public class Environment extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(Environment.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Type = "type";
    public static final String P_TypeAsString = "typeString";
    public static final String P_Name = "name";
    public static final String P_UrlTemplate = "urlTemplate";
    public static final String P_NodeCount = "nodeCount";
    public static final String P_Color = "color";
    public static final String P_EnableDashboard = "enableDashboard";
     
    public static final String P_IsAllPaused = "isAllPaused";
    public static final String P_IsAllRunning = "isAllRunning";
     
    public static final String P_CorpToStores = "corpToStores";
    public static final String P_DashboardLines = "dashboardLines";
    public static final String P_Testers = "testers";
     
    public static final String M_UpdateCorpToStore = "updateCorpToStore";
    public static final String M_PauseAll = "pauseAll";
    public static final String M_ContinueAll = "continueAll";
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int type;
    public static enum Type {
        Unknown("Unknown"),
        Local("Local"),
        Dev("Dev"),
        Test("Test"),
        Prod("Prod");

        private String display;
        Type(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int TYPE_Unknown = 0;
    public static final int TYPE_Local = 1;
    public static final int TYPE_Dev = 2;
    public static final int TYPE_Test = 3;
    public static final int TYPE_Prod = 4;
    public static final Hub<String> hubType;
    static {
        hubType = new Hub<String>(String.class);
        hubType.addElement("Unknown");
        hubType.addElement("Local");
        hubType.addElement("Dev");
        hubType.addElement("Test");
        hubType.addElement("Prod");
    }
    protected volatile String name;
    protected volatile String urlTemplate;
    protected volatile int nodeCount;
    protected volatile Color color;
    protected volatile boolean enableDashboard;
     
    // Links to other objects.
    protected transient Hub<CorpToStore> hubCorpToStores;
    protected transient Hub<DashboardLine> hubDashboardLines;
    protected transient Hub<Tester> hubTesters;
     
    public Environment() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public Environment(int id) {
        this();
        setId(id);
    }
     

    @OAProperty(isUnique = true, trackPrimitiveNull = false, displayLength = 6)
    @OAId
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        int old = id;
        fireBeforePropertyChange(P_Id, old, newValue);
        this.id = newValue;
        firePropertyChange(P_Id, old, this.id);
    }
    @OAProperty(defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true)
    @OAColumn(sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getCreated() {
        return created;
    }
    public void setCreated(OADateTime newValue) {
        OADateTime old = created;
        fireBeforePropertyChange(P_Created, old, newValue);
        this.created = newValue;
        firePropertyChange(P_Created, old, this.created);
    }
    @OAProperty(trackPrimitiveNull = false, displayLength = 6, isNameValue = true)
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getType() {
        return type;
    }
    public void setType(int newValue) {
        int old = type;
        fireBeforePropertyChange(P_Type, old, newValue);
        this.type = newValue;
        firePropertyChange(P_Type, old, this.type);
        firePropertyChange(P_Type + "String");
        firePropertyChange(P_Type + "Enum");
    }

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
    @OAProperty(maxLength = 30, displayLength = 20)
    @OAColumn(maxLength = 30)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }
    @OAProperty(displayName = "Url Template", maxLength = 50, displayLength = 20)
    @OAColumn(name = "url_template", maxLength = 50)
    public String getUrlTemplate() {
        return urlTemplate;
    }
    public void setUrlTemplate(String newValue) {
        String old = urlTemplate;
        fireBeforePropertyChange(P_UrlTemplate, old, newValue);
        this.urlTemplate = newValue;
        firePropertyChange(P_UrlTemplate, old, this.urlTemplate);
    }
    @OAProperty(displayName = "Node Count", displayLength = 6, columnLength = 10, hasCustomCode = true)
    @OAColumn(name = "node_count", sqlType = java.sql.Types.INTEGER)
    public int getNodeCount() {
        return nodeCount;
    }
    
    public void setNodeCount(int newValue) {
        int old = nodeCount;
        fireBeforePropertyChange(P_NodeCount, old, newValue);
        this.nodeCount = newValue;
        firePropertyChange(P_NodeCount, old, this.nodeCount);
    
        if (isRemoteThread()) {
            return;
        }
    
        int x = getCorpToStores().size();
        if (x >= nodeCount) {
            return;
        }
        for (; x < nodeCount; x++) {
            getCorpToStores().add(new CorpToStore());
        }
    }
    @OAProperty(displayLength = 12, columnLength = 8)
    @OAColumn(maxLength = 16)
    public Color getColor() {
        return color;
    }
    public void setColor(Color newValue) {
        Color old = color;
        fireBeforePropertyChange(P_Color, old, newValue);
        this.color = newValue;
        firePropertyChange(P_Color, old, this.color);
    }
    @OAProperty(displayName = "Enable Dashboard", displayLength = 5, columnLength = 16)
    @OAColumn(name = "enable_dashboard", sqlType = java.sql.Types.BOOLEAN)
    public boolean getEnableDashboard() {
        return enableDashboard;
    }
    public boolean isEnableDashboard() {
        return getEnableDashboard();
    }
    public void setEnableDashboard(boolean newValue) {
        boolean old = enableDashboard;
        fireBeforePropertyChange(P_EnableDashboard, old, newValue);
        this.enableDashboard = newValue;
        firePropertyChange(P_EnableDashboard, old, this.enableDashboard);
    }
    @OACalculatedProperty(displayName = "Is All Paused", displayLength = 5, columnLength = 13, properties = {P_CorpToStores+"."+CorpToStore.P_AllPaused})
    public boolean getIsAllPaused() {
        boolean b = EnvironmentDelegate.getIsAllPaused(this);
        return b;
    }
    public boolean isIsAllPaused() {
        return getIsAllPaused();
    }
    @OACalculatedProperty(displayName = "Is All Running", displayLength = 5, columnLength = 14, properties = {P_CorpToStores+"."+CorpToStore.P_AllPaused})
    public boolean getIsAllRunning() {
        boolean b = EnvironmentDelegate.getIsAllRunning(this);
        return b;
    }
    public boolean isIsAllRunning() {
        return getIsAllRunning();
    }
    @OAMany(
        displayName = "Corp To Stores", 
        toClass = CorpToStore.class, 
        owner = true, 
        reverseName = CorpToStore.P_Environment, 
        cascadeSave = true, 
        cascadeDelete = true, 
        uniqueProperty = CorpToStore.P_NodeName
    )
    public Hub<CorpToStore> getCorpToStores() {
        if (hubCorpToStores == null) {
            hubCorpToStores = (Hub<CorpToStore>) getHub(P_CorpToStores);
        }
        return hubCorpToStores;
    }
    @OAMany(
        displayName = "Dashboard Lines", 
        toClass = DashboardLine.class, 
        reverseName = DashboardLine.P_Environment, 
        isProcessed = true
    )
    public Hub<DashboardLine> getDashboardLines() {
        if (hubDashboardLines == null) {
            hubDashboardLines = (Hub<DashboardLine>) getHub(P_DashboardLines);
        }
        return hubDashboardLines;
    }
    @OAMany(
        toClass = Tester.class, 
        reverseName = Tester.P_Environment
    )
    public Hub<Tester> getTesters() {
        if (hubTesters == null) {
            hubTesters = (Hub<Tester>) getHub(P_Testers);
        }
        return hubTesters;
    }
    @OAMethod(displayName = "Update Corp To Store")
    public void updateCorpToStore() {
        EnvironmentDelegate.updateCorpToStore(this);
    }

    @OAMethod(displayName = "Pause All")
    public void pauseAll() {
        EnvironmentDelegate.pauseAll(this);
    }
    @OAObjCallback(enabledProperty = Environment.P_IsAllPaused, enabledValue = false)
    public void pauseAllCallback(OAObjectCallback cb) {
    }

    @OAMethod(displayName = "Continue All")
    public void continueAll() {
        EnvironmentDelegate.continueAll(this);
    }
    @OAObjCallback(enabledProperty = Environment.P_IsAllRunning, enabledValue = false
    )
    public void continueAllCallback(OAObjectCallback cb) {
    }

}
 