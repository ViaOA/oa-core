package com.test.pos.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.lang.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.annotation.*;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.datetime.OADateTime;
import com.viaoa.datetime.OADate;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "teamMember",
    pluralName = "TeamMembers",
    shortName = "tmm",
    displayName = "Team Member",
    displayProperty = "calcDisplayName",
    filterClasses = {TeamMemberActiveFilter.class},
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "TeamMemberEmpNumber", unique = true, columns = {@OAIndexColumn(name = "EmpNumber", lowerName = "EmpNumberLower")}),
        @OAIndex(name = "TeamMemberAppUser", fkey = true, columns = { @OAIndexColumn(name = "AppUserId") }), 
        @OAIndex(name = "TeamMemberStore", fkey = true, columns = { @OAIndexColumn(name = "StoreId") })
    }
)
public class TeamMember extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(TeamMember.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_EmpNumber = "empNumber";
    public static final String P_Title = "title";
    public static final String P_FirstName = "firstName";
    public static final String P_LastName = "lastName";
    public static final String P_InactiveDate = "inactiveDate";
     
    public static final String P_CalcFullName = "calcFullName";
    public static final String P_CalcDisplayName = "calcDisplayName";
    public static final String P_AccessSafePermission = "accessSafePermission";
     
    public static final String P_AppUser = "appUser";
    public static final String P_AppUserId = "appUserId"; // fkey
    public static final String P_RegisterSessions = "registerSessions";
    public static final String P_Store = "store";
    public static final String P_StoreId = "storeId"; // fkey
    public static final String P_StoreSafeLedgerEntries = "storeSafeLedgerEntries";
    public static final String P_StoreSchedules = "storeSchedules";
    public static final String P_StoreSchedulesId = "storeSchedulesId"; // fkey
    public static final String P_TillLedgerEntries = "tillLedgerEntries";
    public static final String P_TMPermissions = "tmPermissions";
    public static final String P_TMPermissionsId = "tmPermissionsId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String empNumber;
    protected volatile String title;
    protected volatile String firstName;
    protected volatile String lastName;
    protected volatile OADate inactiveDate;
     
    // Links to other objects.
    protected volatile transient AppUser appUser;
    protected transient Hub<RegisterSession> hubRegisterSessions;
    protected volatile transient Store store;
    protected transient Hub<StoreSchedule> hubStoreSchedules;
    protected transient Hub<TMPermission> hubTMPermissions;
     
    public TeamMember() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public TeamMember(int id) {
        this();
        setId(id);
    }
    @OAObjCallback(modelUserEnabledProperty = AppUser.P_Admin)
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

    @OAProperty(lowerName = "empNumber", displayName = "Emp Number", maxLength = 20, isUnique = true, displayLength = 20)
    @OAColumn(name = "EmpNumber", maxLength = 20, lowerName = "EmpNumberLower")
    public String getEmpNumber() {
        return empNumber;
    }
    public void setEmpNumber(String newValue) {
        String old = empNumber;
        fireBeforePropertyChange(P_EmpNumber, old, newValue);
        this.empNumber = newValue;
        firePropertyChange(P_EmpNumber, old, this.empNumber);
    }

    @OAProperty(lowerName = "title", maxLength = 50, displayLength = 18)
    @OAColumn(name = "Title", maxLength = 50)
    public String getTitle() {
        return title;
    }
    public void setTitle(String newValue) {
        String old = title;
        fireBeforePropertyChange(P_Title, old, newValue);
        this.title = newValue;
        firePropertyChange(P_Title, old, this.title);
    }

    @OAProperty(lowerName = "firstName", displayName = "First Name", maxLength = 25, displayLength = 15)
    @OAColumn(name = "FirstName", maxLength = 25)
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String newValue) {
        String old = firstName;
        fireBeforePropertyChange(P_FirstName, old, newValue);
        this.firstName = newValue;
        firePropertyChange(P_FirstName, old, this.firstName);
    }

    @OAProperty(lowerName = "lastName", displayName = "Last Name", maxLength = 50, displayLength = 18)
    @OAColumn(name = "LastName", maxLength = 50)
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String newValue) {
        String old = lastName;
        fireBeforePropertyChange(P_LastName, old, newValue);
        this.lastName = newValue;
        firePropertyChange(P_LastName, old, this.lastName);
    }

    @OAProperty(lowerName = "inactiveDate", displayName = "Inactive Date", displayLength = 8, uiColumnLength = 13)
    @OAColumn(name = "InactiveDate", sqlType = java.sql.Types.DATE)
    public OADate getInactiveDate() {
        return inactiveDate;
    }
    public void setInactiveDate(OADate newValue) {
        OADate old = inactiveDate;
        fireBeforePropertyChange(P_InactiveDate, old, newValue);
        this.inactiveDate = newValue;
        firePropertyChange(P_InactiveDate, old, this.inactiveDate);
    }
    @OACalculatedProperty(displayName = "Full Name", displayLength = 20, properties = {P_FirstName, P_LastName})
    public String getCalcFullName() {
        String fn  = OAStr.concat(this.getFirstName(), this.getLastName(), " ");
        return fn;
    }
    @OACalculatedProperty(displayName = "Display Name", displayLength = 20, properties = {P_CalcFullName, P_EmpNumber})
    public String getCalcDisplayName() {
        String dn = this.getCalcFullName();
        String en = OAStr.notNull(getEmpNumber());
        dn = dn + "(" + en + ")";
        return dn;
    }
    @OACalculatedProperty(displayName = "Access Safe Permission", displayLength = 5, columnLength = 22, properties = {P_TMPermissions+"."+TMPermission.P_Type})
    public boolean getAccessSafePermission() {
        Hub<TMPermission> hubTMPermissions = this.getTMPermissions();
        for (TMPermission tmPermission : hubTMPermissions) {
        	if (tmPermission.getType() >= TMPermission.TYPE_manager) {
        		return true;
        	}
        }
        return false;
    }
    public boolean canAccessSafePermission() {
        return getAccessSafePermission();
    }

    @OAOne(
        displayName = "App User", 
        reverseName = AppUser.P_TeamMember, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_AppUserId, toProperty = AppUser.P_Id)}
    )
    public AppUser getAppUser() {
        if (appUser == null) {
            appUser = (AppUser) getObject(P_AppUser);
        }
        return appUser;
    }
    public void setAppUser(AppUser newValue) {
        AppUser old = this.appUser;
        fireBeforePropertyChange(P_AppUser, old, newValue);
        this.appUser = newValue;
        firePropertyChange(P_AppUser, old, this.appUser);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "AppUserId")
    public Integer getAppUserId() {
        return (Integer) getFkeyProperty(P_AppUserId);
    }
    public void setAppUserId(Integer newValue) {
        this.appUser = null;
        setFkeyProperty(P_AppUserId, newValue);
    }

    @OAMany(
        displayName = "Register Sessions", 
        toClass = RegisterSession.class, 
        reverseName = RegisterSession.P_TeamMember
    )
    public Hub<RegisterSession> getRegisterSessions() {
        if (hubRegisterSessions == null) {
            hubRegisterSessions = (Hub<RegisterSession>) getHub(P_RegisterSessions);
        }
        return hubRegisterSessions;
    }

    @OAOne(
        reverseName = Store.P_TeamMembers, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_StoreId, toProperty = Store.P_Id)}
    )
    public Store getStore() {
        if (store == null) {
            store = (Store) getObject(P_Store);
        }
        return store;
    }
    public void setStore(Store newValue) {
        Store old = this.store;
        fireBeforePropertyChange(P_Store, old, newValue);
        this.store = newValue;
        firePropertyChange(P_Store, old, this.store);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "StoreId")
    public Integer getStoreId() {
        return (Integer) getFkeyProperty(P_StoreId);
    }
    public void setStoreId(Integer newValue) {
        this.store = null;
        setFkeyProperty(P_StoreId, newValue);
    }

    @OAMany(
        displayName = "Store Safe Ledger Entries", 
        toClass = StoreSafeLedgerEntry.class, 
        reverseName = StoreSafeLedgerEntry.P_TeamMember, 
        isProcessed = true, 
        createMethod = false
    )
    private Hub<StoreSafeLedgerEntry> getStoreSafeLedgerEntries() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAMany(
        displayName = "Store Schedules", 
        toClass = StoreSchedule.class, 
        reverseName = StoreSchedule.P_TeamMembers
    )
    @OALinkTable(name = "StoreScheduleTeamMember", indexName = "StoreScheduleTeamMember", columns = {"TeamMemberId"})
    public Hub<StoreSchedule> getStoreSchedules() {
        if (hubStoreSchedules == null) {
            hubStoreSchedules = (Hub<StoreSchedule>) getHub(P_StoreSchedules);
        }
        return hubStoreSchedules;
    }

    @OAMany(
        displayName = "Till Ledger Entries", 
        toClass = TillLedgerEntry.class, 
        reverseName = TillLedgerEntry.P_TeamMember, 
        isProcessed = true, 
        createMethod = false
    )
    private Hub<TillLedgerEntry> getTillLedgerEntries() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAMany(
        lowerName = "tmPermissions", 
        displayName = "TM Permissions", 
        toClass = TMPermission.class, 
        reverseName = TMPermission.P_TeamMembers
    )
    @OALinkTable(name = "TMPermissionTeamMember", indexName = "TMPermissionTeamMember", columns = {"TeamMemberId"})
    public Hub<TMPermission> getTMPermissions() {
        if (hubTMPermissions == null) {
            hubTMPermissions = (Hub<TMPermission>) getHub(P_TMPermissions);
        }
        return hubTMPermissions;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.empNumber = rs.getString(3);
        this.title = rs.getString(4);
        this.firstName = rs.getString(5);
        this.lastName = rs.getString(6);
        java.sql.Date date;
        date = rs.getDate(7);
        if (date != null) this.inactiveDate = new OADate(date);
        int appUserFkey = rs.getInt(8);
        setFkeyProperty(P_AppUser, rs.wasNull() ? null : appUserFkey);
        int storeFkey = rs.getInt(9);
        setFkeyProperty(P_Store, rs.wasNull() ? null : storeFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
