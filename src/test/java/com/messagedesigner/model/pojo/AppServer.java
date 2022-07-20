package com.messagedesigner.model.pojo;
 
import java.util.*;

import com.messagedesigner.model.pojo.AppUserLogin;

import java.time.LocalDateTime;
 
public class AppServer implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected LocalDateTime created;
    protected LocalDateTime started;
    protected boolean demoMode;
    protected String release;
     
    // References to other objects.
    protected AppUserLogin appUserLogin;
     
     
    public AppServer() {
    }
     
    public AppServer(int id) {
        this();
        setId(id);
    }
    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        this.id = newValue;
    }
     
    public LocalDateTime getCreated() {
        return created;
    }
    public void setCreated(LocalDateTime newValue) {
        this.created = newValue;
    }
     
    public LocalDateTime getStarted() {
        return started;
    }
    public void setStarted(LocalDateTime newValue) {
        this.started = newValue;
    }
     
    public boolean getDemoMode() {
        return demoMode;
    }
    public void setDemoMode(boolean newValue) {
        this.demoMode = newValue;
    }
     
    public String getRelease() {
        return release;
    }
    public void setRelease(String newValue) {
        this.release = newValue;
    }
     
    public AppUserLogin getAppUserLogin() {
        return appUserLogin;
    }
    
    public void setAppUserLogin(AppUserLogin newValue) {
        this.appUserLogin = newValue;
    }
    
     
}
 
