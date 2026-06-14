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
 
/**
  Team Member permissions
  
*/
@OAClass(
    lowerName = "tmPermission",
    pluralName = "TMPermissions",
    shortName = "tmp",
    displayName = "TM Permission",
    description = "Team Member permissions ",
    isLookup = true,
    isPreSelect = true,
    displayProperty = "name",
    noPojo = true
)
@OATable(
)
public class TMPermission extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(TMPermission.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Type = "type";
    public static final String P_TypeString = "typeString";
    public static final String P_TypeEnum = "typeEnum";
    public static final String P_TypeDisplay = "typeDisplay";
    public static final String P_Name = "name";
    public static final String P_Description = "description";
     
    public static final String P_TeamMembers = "teamMembers";
    public static final String P_TeamMembersId = "teamMembersId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int type;

    public static enum Type {
        unknown("Unknown"),
        employee("Employee"),
        operateRegister("Operate Register"),
        useEquipment("Use Equipment"),
        manager("Manager"),
        districtManager("District Manager");

        private String display;
        Type(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int TYPE_unknown = 0;
    public static final int TYPE_employee = 1;
    public static final int TYPE_operateRegister = 2;
    public static final int TYPE_useEquipment = 3;
    public static final int TYPE_manager = 4;
    public static final int TYPE_districtManager = 5;

    protected volatile String name;
    protected volatile String description;
     
    // Links to other objects.
    protected transient Hub<TeamMember> hubTeamMembers;
     
    public TMPermission() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public TMPermission(int id) {
        this();
        setId(id);
    }
    @OAObjCallback(contextEnabledProperty = AppUser.P_Admin)
    public void callback(final OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
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

    @OAProperty(lowerName = "type", displayLength = 17, isNameValue = true)
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
    @OACalculatedProperty(enumPropertyName = P_Type, displayName = "Type", displayLength = 17, columnLength = 17, properties = {P_Type} )
    public String getTypeDisplay() {
        Type type = getTypeEnum();
        if (type == null) return null;
        return type.getDisplay();
    }

    @OAProperty(lowerName = "name", maxLength = 25, displayLength = 15)
    @OAColumn(name = "Name", maxLength = 25)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }

    @OAProperty(lowerName = "description", maxLength = 175, displayLength = 22, uiColumnLength = 20)
    @OAColumn(name = "Description", maxLength = 175)
    public String getDescription() {
        return description;
    }
    public void setDescription(String newValue) {
        String old = description;
        fireBeforePropertyChange(P_Description, old, newValue);
        this.description = newValue;
        firePropertyChange(P_Description, old, this.description);
    }

    @OAMany(
        displayName = "Team Members", 
        toClass = TeamMember.class, 
        reverseName = TeamMember.P_TMPermissions
    )
    @OALinkTable(name = "TMPermissionTeamMember", indexName = "TeamMemberTmPermission", columns = {"TMPermissionId"})
    public Hub<TeamMember> getTeamMembers() {
        if (hubTeamMembers == null) {
            hubTeamMembers = (Hub<TeamMember>) getHub(P_TeamMembers);
        }
        return hubTeamMembers;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.type = rs.getInt(3);
        setPrimitiveNull(P_Type, rs.wasNull());
        this.name = rs.getString(4);
        this.description = rs.getString(5);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
