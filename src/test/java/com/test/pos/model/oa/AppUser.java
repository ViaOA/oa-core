package com.test.pos.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.annotation.*;
import com.viaoa.lang.*;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.datetime.OADate;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "appUser",
    pluralName = "AppUsers",
    shortName = "au",
    displayName = "App User",
    isLookup = true,
    isPreSelect = true,
    displayProperty = "displayName",
    noPojo = true
)
@OATable(
)
public class AppUser extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(AppUser.class.getName());

    public static final String P_Id = "id";
    public static final String P_LoginId = "loginId";
    public static final String P_Password = "password";
    public static final String P_Admin = "admin";
    public static final String P_SuperAdmin = "superAdmin";
    public static final String P_EditProcessed = "editProcessed";
    public static final String P_FirstName = "firstName";
    public static final String P_LastName = "lastName";
    public static final String P_InactiveDate = "inactiveDate";
    public static final String P_Note = "note";
     
    public static final String P_FullName = "fullName";
    public static final String P_DisplayName = "displayName";
     
    public static final String P_AppUserLogins = "appUserLogins";
    public static final String P_Reports = "reports";
    public static final String P_ReportsId = "reportsId"; // fkey
    public static final String P_TeamMember = "teamMember";
     
    protected volatile int id;
    protected volatile String loginId;
    protected volatile String password;
    protected volatile boolean admin;
    protected volatile boolean superAdmin;
    protected volatile boolean editProcessed;
    protected volatile String firstName;
    protected volatile String lastName;
    protected volatile OADate inactiveDate;
    protected volatile String note;
     
    // Links to other objects.
    protected transient Hub<AppUserLogin> hubAppUserLogins;
    protected transient Hub<Report> hubReports;
    protected volatile transient TeamMember teamMember;
     
    public AppUser() {
        if (!isLoading()) setObjectDefaults();
    }
     
    public AppUser(int id) {
        this();
        setId(id);
    }
    @OAObjCallback(contextEnabledProperty = AppUser.P_Admin)
    public void callback(final OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
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

    @OAProperty(lowerName = "loginId", displayName = "Login Id", maxLength = 35, displayLength = 14, uiColumnLength = 12)
    @OAColumn(name = "LoginId", maxLength = 35)
    public String getLoginId() {
        return loginId;
    }
    public void setLoginId(String newValue) {
        String old = loginId;
        fireBeforePropertyChange(P_LoginId, old, newValue);
        this.loginId = newValue;
        firePropertyChange(P_LoginId, old, this.loginId);
    }

    @OAProperty(lowerName = "password", maxLength = 50, displayLength = 12, uiColumnLength = 10, isSHAHash = true)
    @OAColumn(name = "Password", maxLength = 50)
    public String getPassword() {
        return password;
    }
    public void setPassword(String newValue) {
        String old = password;
        fireBeforePropertyChange(P_Password, old, newValue);
        this.password = newValue;
        firePropertyChange(P_Password, old, this.password);
    }

    @OAProperty(lowerName = "admin", trackPrimitiveNull = false, displayLength = 5)
    @OAColumn(name = "Admin", sqlType = java.sql.Types.BOOLEAN)
    public boolean getAdmin() {
        return admin;
    }
    public boolean isAdmin() {
        return getAdmin();
    }
    public void setAdmin(boolean newValue) {
        boolean old = admin;
        fireBeforePropertyChange(P_Admin, old, newValue);
        this.admin = newValue;
        firePropertyChange(P_Admin, old, this.admin);
    }

    @OAProperty(lowerName = "superAdmin", displayName = "Super Admin", trackPrimitiveNull = false, displayLength = 5, uiColumnLength = 11)
    public boolean getSuperAdmin() {
        return superAdmin;
    }
    public boolean isSuperAdmin() {
        return getSuperAdmin();
    }
    public void setSuperAdmin(boolean newValue) {
        boolean old = superAdmin;
        fireBeforePropertyChange(P_SuperAdmin, old, newValue);
        this.superAdmin = newValue;
        firePropertyChange(P_SuperAdmin, old, this.superAdmin);
    }
     
    @OAObjCallback(contextEnabledProperty = AppUser.P_Admin)
    public void superAdminCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "editProcessed", displayName = "Edit Processed", trackPrimitiveNull = false, displayLength = 5)
    @OAColumn(name = "EditProcessed", sqlType = java.sql.Types.BOOLEAN)
    public boolean getEditProcessed() {
        return editProcessed;
    }
    public boolean isEditProcessed() {
        return getEditProcessed();
    }
    public void setEditProcessed(boolean newValue) {
        boolean old = editProcessed;
        fireBeforePropertyChange(P_EditProcessed, old, newValue);
        this.editProcessed = newValue;
        firePropertyChange(P_EditProcessed, old, this.editProcessed);
    }

    @OAProperty(lowerName = "firstName", displayName = "First Name", maxLength = 35, displayLength = 12)
    @OAColumn(name = "FirstName", maxLength = 35)
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String newValue) {
        String old = firstName;
        fireBeforePropertyChange(P_FirstName, old, newValue);
        this.firstName = newValue;
        firePropertyChange(P_FirstName, old, this.firstName);
    }

    @OAProperty(lowerName = "lastName", displayName = "Last Name", maxLength = 55, displayLength = 22, uiColumnLength = 18)
    @OAColumn(name = "LastName", maxLength = 55)
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String newValue) {
        String old = lastName;
        fireBeforePropertyChange(P_LastName, old, newValue);
        this.lastName = newValue;
        firePropertyChange(P_LastName, old, this.lastName);
    }

    @OAProperty(lowerName = "inactiveDate", displayName = "Inactive Date", displayLength = 8)
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

    @OAProperty(lowerName = "note", displayLength = 20, uiColumnLength = 15)
    @OAColumn(name = "Note", sqlType = java.sql.Types.CLOB)
    public String getNote() {
        return note;
    }
    public void setNote(String newValue) {
        String old = note;
        fireBeforePropertyChange(P_Note, old, newValue);
        this.note = newValue;
        firePropertyChange(P_Note, old, this.note);
    }
    @OACalculatedProperty(displayName = "Full Name", displayLength = 23, columnLength = 16, properties = {P_FirstName, P_LastName})
    public String getFullName() {
        String fullName = "";
        // firstName
        firstName = this.getFirstName();
        if (firstName != null) {
            if (fullName.length() > 0) fullName += " ";
            fullName += firstName;
        }
    
        // lastName
        lastName = this.getLastName();
        if (lastName != null) {
            if (fullName.length() > 0) fullName += " ";
            fullName += lastName;
        }
    
        return fullName;
    }
    @OACalculatedProperty(displayName = "Display Name", displayLength = 28, columnLength = 22, properties = {P_LoginId, P_FullName})
    public String getDisplayName() {
        String displayName = "";
        loginId = this.getLoginId();
        if (loginId != null) {
            displayName = loginId;
        }
        if (displayName.length() > 0) displayName += " ";
        displayName += " (";
        String fullName = this.getFullName();
        if (fullName != null) {
            displayName += fullName;
        }
        displayName += ")";
        return displayName;
    }

    @OAMany(
        displayName = "App User Logins", 
        toClass = AppUserLogin.class, 
        owner = true, 
        reverseName = AppUserLogin.P_AppUser, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<AppUserLogin> getAppUserLogins() {
        if (hubAppUserLogins == null) {
            hubAppUserLogins = (Hub<AppUserLogin>) getHub(P_AppUserLogins);
        }
        return hubAppUserLogins;
    }

    @OAMany(
        toClass = Report.class, 
        reverseName = Report.P_AppUser
    )
    @OALinkTable(name = "AppUserReport", indexName = "ReportAppUser", columns = {"AppUserId"})
    public Hub<Report> getReports() {
        if (hubReports == null) {
            hubReports = (Hub<Report>) getHub(P_Reports);
        }
        return hubReports;
    }

    @OAOne(
        displayName = "Team Member", 
        reverseName = TeamMember.P_AppUser, 
        allowCreateNew = false
    )
    public TeamMember getTeamMember() {
        if (teamMember == null) {
            teamMember = (TeamMember) getObject(P_TeamMember);
        }
        return teamMember;
    }
    public void setTeamMember(TeamMember newValue) {
        TeamMember old = this.teamMember;
        fireBeforePropertyChange(P_TeamMember, old, newValue);
        this.teamMember = newValue;
        firePropertyChange(P_TeamMember, old, this.teamMember);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        this.loginId = rs.getString(2);
        this.password = rs.getString(3);
        this.admin = rs.getBoolean(4);
        this.editProcessed = rs.getBoolean(5);
        this.firstName = rs.getString(6);
        this.lastName = rs.getString(7);
        java.sql.Date date;
        date = rs.getDate(8);
        if (date != null) this.inactiveDate = new OADate(date);
        this.note = rs.getString(9);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
