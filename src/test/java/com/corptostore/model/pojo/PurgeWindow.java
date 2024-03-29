// generated by OABuilder
package com.corptostore.model.pojo;
 
import java.util.*;

import com.corptostore.model.pojo.StatusInfo;
import com.corptostore.model.pojo.StorePurgeInfo;

import java.time.LocalDateTime;
 
 
public class PurgeWindow implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected LocalDateTime created;
    protected int timeoutMinutes;
    protected int storeLimit;
    protected LocalDateTime finished;

    // References to other objects.
    // StatusInfo
    protected StatusInfo statusInfo;
    // StorePurgeInfos
    protected List<StorePurgeInfo> alStorePurgeInfos;
     
    public PurgeWindow() {
    }
     
    public LocalDateTime getCreated() {
        return created;
    }
    public void setCreated(LocalDateTime newValue) {
        this.created = newValue;
    }
     
    public int getTimeoutMinutes() {
        return timeoutMinutes;
    }
    public void setTimeoutMinutes(int newValue) {
        this.timeoutMinutes = newValue;
    }
     
    public int getStoreLimit() {
        return storeLimit;
    }
    public void setStoreLimit(int newValue) {
        this.storeLimit = newValue;
    }
     
    public LocalDateTime getFinished() {
        return finished;
    }
    public void setFinished(LocalDateTime newValue) {
        this.finished = newValue;
    }
     
     
    public StatusInfo getStatusInfo() {
        if (statusInfo == null) statusInfo = new StatusInfo();
        return statusInfo;
    }
    
    public void setStatusInfo(StatusInfo newValue) {
        this.statusInfo = newValue;
    }
     
    public List<StorePurgeInfo> getStorePurgeInfos() {
        if (alStorePurgeInfos == null) {
            alStorePurgeInfos = new ArrayList<StorePurgeInfo>();
        }
        return alStorePurgeInfos;
    }
    public void setStorePurgeInfos(List<StorePurgeInfo> list) {
        this.alStorePurgeInfos = list;
    }
}
 
