package com.oreillyauto.dev.tool.messagedesigner.model.pojo;
 
import java.util.*;
import java.time.LocalDate;
 
public class AppUser implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected String loginId;
    protected String password;
    protected boolean admin;
    protected boolean superAdmin;
    protected boolean editProcessed;
    protected String firstName;
    protected String lastName;
    protected LocalDate inactiveDate;
    protected String note;
     
    // References to other objects.
    protected ArrayList<AppUserLogin> alAppUserLogins;
     
     
    public AppUser() {
    }
     
    public AppUser(int id) {
        this();
        setId(id);
    }
    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        this.id = newValue;
    }
     
    public String getLoginId() {
        return loginId;
    }
    public void setLoginId(String newValue) {
        this.loginId = newValue;
    }
     
    public String getPassword() {
        return password;
    }
    public void setPassword(String newValue) {
        this.password = newValue;
    }
     
    public boolean getAdmin() {
        return admin;
    }
    public void setAdmin(boolean newValue) {
        this.admin = newValue;
    }
     
    public boolean getSuperAdmin() {
        return superAdmin;
    }
    public void setSuperAdmin(boolean newValue) {
        this.superAdmin = newValue;
    }
     
    public boolean getEditProcessed() {
        return editProcessed;
    }
    public void setEditProcessed(boolean newValue) {
        this.editProcessed = newValue;
    }
     
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String newValue) {
        this.firstName = newValue;
    }
     
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String newValue) {
        this.lastName = newValue;
    }
     
    public LocalDate getInactiveDate() {
        return inactiveDate;
    }
    public void setInactiveDate(LocalDate newValue) {
        this.inactiveDate = newValue;
    }
     
    public String getNote() {
        return note;
    }
    public void setNote(String newValue) {
        this.note = newValue;
    }
     
    public ArrayList<AppUserLogin> getAppUserLogins() {
        if (alAppUserLogins == null) {
            alAppUserLogins = new ArrayList<AppUserLogin>();
        }
        return alAppUserLogins;
    }
    
     
}
 
