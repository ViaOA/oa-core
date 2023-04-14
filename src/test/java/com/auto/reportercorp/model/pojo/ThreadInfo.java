package com.auto.reportercorp.model.pojo;
 
import java.util.*;
import com.fasterxml.jackson.annotation.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.*;
 
@JsonIdentityInfo(generator = com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator.class, property = "name", scope = ThreadInfo.class)
public class ThreadInfo implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected volatile LocalDateTime created;
    protected volatile String name;
    protected volatile LocalDateTime paused;
    protected volatile String stackTrace;
    protected volatile boolean active;
    protected volatile LocalDateTime lastActive;
    protected volatile int activeCount;

    // References to other objects
    // ReporterCorp
    protected volatile ReporterCorp reporterCorp;
    // StatusInfo
    protected volatile StatusInfo statusInfo;
    // ReportInstanceProcesses
    protected volatile CopyOnWriteArrayList<ReportInstanceProcess> alReportInstanceProcesses = new CopyOnWriteArrayList<>();
     
    public ThreadInfo() {
        this.created = LocalDateTime.now();
    }
     
    public LocalDateTime getCreated() {
        return created;
    }
    public void setCreated(LocalDateTime newValue) {
        this.created = newValue;
    }
     
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        this.name = newValue;
    }
     
    public LocalDateTime getPaused() {
        return paused;
    }
    public void setPaused(LocalDateTime newValue) {
        this.paused = newValue;
    }
     
    public String getStackTrace() {
        return stackTrace;
    }
    public void setStackTrace(String newValue) {
        this.stackTrace = newValue;
    }
     
    public boolean getActive() {
        return active;
    }
    public void setActive(boolean newValue) {
        this.active = newValue;
    }
     
    public LocalDateTime getLastActive() {
        return lastActive;
    }
    public void setLastActive(LocalDateTime newValue) {
        this.lastActive = newValue;
    }
     
    public int getActiveCount() {
        return activeCount;
    }
    public void setActiveCount(int newValue) {
        this.activeCount = newValue;
    }

    @JsonIgnore
    public ReporterCorp getReporterCorp() {
        return reporterCorp;
    }
    public void setReporterCorp(ReporterCorp newValue) {
        this.reporterCorp = newValue;
    }

    public StatusInfo getStatusInfo() {
        if (statusInfo == null) statusInfo = new StatusInfo();
        return statusInfo;
    }
    public void setStatusInfo(StatusInfo newValue) {
        this.statusInfo = newValue;
    }

    @JsonIdentityReference
    public CopyOnWriteArrayList<ReportInstanceProcess> getReportInstanceProcesses() {
        return alReportInstanceProcesses;
    }
    public void setReportInstanceProcesses(List<ReportInstanceProcess> list) {
        if (list == null) this.alReportInstanceProcesses.clear();
        else this.alReportInstanceProcesses = new CopyOnWriteArrayList<>(list);
    }
     
    // custom code from OABuilder
    public void addAlert(String msg) {
        getStatusInfo().setAlert(msg);
    }
    public void addAlert(String msg, Exception e) {
        getStatusInfo().setAlert(msg, e);
    }
    public void addStatus(String msg) {
        getStatusInfo().setStatus(msg);
    }
    public void addActivity(String msg) {
        getStatusInfo().setActivity(msg);
    }
     
    @Override
    public String toString() {
        return "ThreadInfo [" +
            "created=" + created +
            ", name=" + name +
            ", paused=" + paused +
            ", active=" + active +
            ", lastActive=" + lastActive +
            ", activeCount=" + activeCount +
        "]";
    }
}
 
