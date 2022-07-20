package com.messagedesigner.model.pojo;
 
import java.util.*;
import java.time.LocalDateTime;
 
public class ProcessorStatus implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected LocalDateTime started;
    protected int status;
    public static final int STATUS_Unknown = 0;
    public static final int STATUS_Started = 1;
    public static final int STATUS_Stopped = 2;
    public static final int STATUS_Paused = 3;
    protected LocalDateTime statusDateTime;
    protected int requestStatus;
    public static final int REQUESTSTATUS_Unknown = 0;
    public static final int REQUESTSTATUS_Start = 1;
    public static final int REQUESTSTATUS_Stop = 2;
    public static final int REQUESTSTATUS_Pause = 3;
    protected LocalDateTime requstStatusDateTime;
    protected int startupStatus;
    public static final int STARTUPSTATUS_Unknown = 0;
    public static final int STARTUPSTATUS_Start = 1;
    public static final int STARTUPSTATUS_Stop = 2;
    public static final int STARTUPSTATUS_Pause = 3;
     
    // References to other objects.
     
     
    public ProcessorStatus() {
    }
     
    public ProcessorStatus(int id) {
        this();
        setId(id);
    }
    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        this.id = newValue;
    }
     
    public LocalDateTime getStarted() {
        return started;
    }
    public void setStarted(LocalDateTime newValue) {
        this.started = newValue;
    }
     
    public int getStatus() {
        return status;
    }
    public void setStatus(int newValue) {
        this.status = newValue;
    }
     
    public LocalDateTime getStatusDateTime() {
        return statusDateTime;
    }
    public void setStatusDateTime(LocalDateTime newValue) {
        this.statusDateTime = newValue;
    }
     
    public int getRequestStatus() {
        return requestStatus;
    }
    public void setRequestStatus(int newValue) {
        this.requestStatus = newValue;
    }
     
    public LocalDateTime getRequstStatusDateTime() {
        return requstStatusDateTime;
    }
    public void setRequstStatusDateTime(LocalDateTime newValue) {
        this.requstStatusDateTime = newValue;
    }
     
    public int getStartupStatus() {
        return startupStatus;
    }
    public void setStartupStatus(int newValue) {
        this.startupStatus = newValue;
    }
     
}
 
