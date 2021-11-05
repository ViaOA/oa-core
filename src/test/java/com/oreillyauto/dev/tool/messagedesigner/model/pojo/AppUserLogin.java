package com.oreillyauto.dev.tool.messagedesigner.model.pojo;
 
import java.util.*;
import java.time.LocalDateTime;
 
public class AppUserLogin implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected LocalDateTime created;
    protected String location;
    protected String computerName;
    protected LocalDateTime disconnected;
    protected int connectionId;
    protected String hostName;
    protected String ipAddress;
    protected long totalMemory;
    protected long freeMemory;
     
    // References to other objects.
    protected ArrayList<AppUserError> alAppUserErrors;
     
     
    public AppUserLogin() {
    }
     
    public AppUserLogin(int id) {
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
     
    public String getLocation() {
        return location;
    }
    public void setLocation(String newValue) {
        this.location = newValue;
    }
     
    public String getComputerName() {
        return computerName;
    }
    public void setComputerName(String newValue) {
        this.computerName = newValue;
    }
     
    public LocalDateTime getDisconnected() {
        return disconnected;
    }
    public void setDisconnected(LocalDateTime newValue) {
        this.disconnected = newValue;
    }
     
    public int getConnectionId() {
        return connectionId;
    }
    public void setConnectionId(int newValue) {
        this.connectionId = newValue;
    }
     
    public String getHostName() {
        return hostName;
    }
    public void setHostName(String newValue) {
        this.hostName = newValue;
    }
     
    public String getIpAddress() {
        return ipAddress;
    }
    public void setIpAddress(String newValue) {
        this.ipAddress = newValue;
    }
     
    public long getTotalMemory() {
        return totalMemory;
    }
    public void setTotalMemory(long newValue) {
        this.totalMemory = newValue;
    }
     
    public long getFreeMemory() {
        return freeMemory;
    }
    public void setFreeMemory(long newValue) {
        this.freeMemory = newValue;
    }
     
    public ArrayList<AppUserError> getAppUserErrors() {
        if (alAppUserErrors == null) {
            alAppUserErrors = new ArrayList<AppUserError>();
        }
        return alAppUserErrors;
    }
    
     
}
 
