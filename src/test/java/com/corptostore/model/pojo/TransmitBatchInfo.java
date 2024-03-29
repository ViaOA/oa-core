// generated by OABuilder
package com.corptostore.model.pojo;
 
import java.util.*;

import com.corptostore.model.pojo.StatusInfo;

import java.time.LocalDateTime;
import java.time.LocalDate;
 
 
public class TransmitBatchInfo implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected LocalDateTime created;
    protected LocalDate transmitBatchDate;
    protected LocalDateTime paused;
    protected LocalDateTime loadDataStart;
    protected LocalDateTime loadDataEnd;

    // References to other objects.
    // StatusInfo
    protected StatusInfo statusInfo;
     
    public TransmitBatchInfo() {
    }
     
    public LocalDateTime getCreated() {
        return created;
    }
    public void setCreated(LocalDateTime newValue) {
        this.created = newValue;
    }
     
    public LocalDate getTransmitBatchDate() {
        return transmitBatchDate;
    }
    public void setTransmitBatchDate(LocalDate newValue) {
        this.transmitBatchDate = newValue;
    }
     
    public LocalDateTime getPaused() {
        return paused;
    }
    public void setPaused(LocalDateTime newValue) {
        this.paused = newValue;
    }
     
    public LocalDateTime getLoadDataStart() {
        return loadDataStart;
    }
    public void setLoadDataStart(LocalDateTime newValue) {
        this.loadDataStart = newValue;
    }
     
    public LocalDateTime getLoadDataEnd() {
        return loadDataEnd;
    }
    public void setLoadDataEnd(LocalDateTime newValue) {
        this.loadDataEnd = newValue;
    }
     
     
    public StatusInfo getStatusInfo() {
        if (statusInfo == null) statusInfo = new StatusInfo();
        return statusInfo;
    }
    
    public void setStatusInfo(StatusInfo newValue) {
        this.statusInfo = newValue;
    }
}
 
