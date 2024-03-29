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
import com.corptostore.model.oa.AppUser;
import com.corptostore.model.oa.AppUserLogin;
import com.viaoa.annotation.*;
import com.viaoa.util.OADate;
 
@OAClass(
    lowerName = "appUser",
    pluralName = "AppUsers",
    shortName = "au",
    displayName = "App User",
    isLookup = true,
    isPreSelect = true,
    useDataSource = false,
    displayProperty = "displayName"
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
    @OAProperty(isUnique = true, displayLength = 5)
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
    @OAProperty(displayName = "Login Id", maxLength = 24, displayLength = 12)
    @OAColumn(name = "login_id", maxLength = 24)
    public String getLoginId() {
        return loginId;
    }
    public void setLoginId(String newValue) {
        String old = loginId;
        fireBeforePropertyChange(P_LoginId, old, newValue);
        this.loginId = newValue;
        firePropertyChange(P_LoginId, old, this.loginId);
    }
    @OAProperty(maxLength = 50, displayLength = 12, columnLength = 10, isSHAHash = true)
    @OAColumn(maxLength = 50)
    public String getPassword() {
        return password;
    }
    public void setPassword(String newValue) {
        String old = password;
        fireBeforePropertyChange(P_Password, old, newValue);
        this.password = newValue;
        firePropertyChange(P_Password, old, this.password);
    }
    @OAProperty(displayLength = 5)
    @OAColumn(sqlType = java.sql.Types.BOOLEAN)
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
    @OAProperty(displayName = "Super Admin", displayLength = 5, columnLength = 11)
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
    @OAProperty(displayName = "Edit Processed", displayLength = 5)
    @OAColumn(name = "edit_processed", sqlType = java.sql.Types.BOOLEAN)
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
    @OAProperty(displayName = "First Name", maxLength = 30, displayLength = 12)
    @OAColumn(name = "first_name", maxLength = 30)
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String newValue) {
        String old = firstName;
        fireBeforePropertyChange(P_FirstName, old, newValue);
        this.firstName = newValue;
        firePropertyChange(P_FirstName, old, this.firstName);
    }
    @OAProperty(displayName = "Last Name", maxLength = 55, displayLength = 12)
    @OAColumn(name = "last_name", maxLength = 55)
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String newValue) {
        String old = lastName;
        fireBeforePropertyChange(P_LastName, old, newValue);
        this.lastName = newValue;
        firePropertyChange(P_LastName, old, this.lastName);
    }
    @OAProperty(displayName = "Inactive Date", displayLength = 8)
    @OAColumn(name = "inactive_date", sqlType = java.sql.Types.DATE)
    public OADate getInactiveDate() {
        return inactiveDate;
    }
    public void setInactiveDate(OADate newValue) {
        OADate old = inactiveDate;
        fireBeforePropertyChange(P_InactiveDate, old, newValue);
        this.inactiveDate = newValue;
        firePropertyChange(P_InactiveDate, old, this.inactiveDate);
    }
    @OAProperty(displayLength = 20, columnLength = 15)
    @OAColumn(sqlType = java.sql.Types.CLOB)
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
}
 
