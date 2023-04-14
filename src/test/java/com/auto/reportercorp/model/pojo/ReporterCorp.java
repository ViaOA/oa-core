package com.auto.reportercorp.model.pojo;
 
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import com.fasterxml.jackson.annotation.*;
import java.time.*;
 
public class ReporterCorp implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected volatile LocalDateTime created;
    protected volatile LocalDateTime stopped;
    protected volatile LocalDateTime paused;

    // References to other objects
    // ReportInstanceProcessorInfo
    protected volatile ReportInstanceProcessorInfo reportInstanceProcessorInfo;
    // StatusInfo
    protected volatile StatusInfo statusInfo;
    // ReporterCorpParams
    protected volatile CopyOnWriteArrayList<ReporterCorpParam> alReporterCorpParams = new CopyOnWriteArrayList<>();
    // StoreInfos
    protected volatile CopyOnWriteArrayList<StoreInfo> alStoreInfos = new CopyOnWriteArrayList<>();
    // ThreadInfos
    protected volatile CopyOnWriteArrayList<ThreadInfo> alThreadInfos = new CopyOnWriteArrayList<>();
     
    public ReporterCorp() {
        this.created = LocalDateTime.now();
    }
     
    public LocalDateTime getCreated() {
        return created;
    }
    public void setCreated(LocalDateTime newValue) {
        this.created = newValue;
    }
     
    public LocalDateTime getStopped() {
        return stopped;
    }
    public void setStopped(LocalDateTime newValue) {
        this.stopped = newValue;
    }
     
    public LocalDateTime getPaused() {
        return paused;
    }
    public void setPaused(LocalDateTime newValue) {
        this.paused = newValue;
    }

    public ReportInstanceProcessorInfo getReportInstanceProcessorInfo() {
        if (reportInstanceProcessorInfo == null) reportInstanceProcessorInfo = new ReportInstanceProcessorInfo();
        return reportInstanceProcessorInfo;
    }
    public void setReportInstanceProcessorInfo(ReportInstanceProcessorInfo newValue) {
        this.reportInstanceProcessorInfo = newValue;
    }

    public StatusInfo getStatusInfo() {
        if (statusInfo == null) statusInfo = new StatusInfo();
        return statusInfo;
    }
    public void setStatusInfo(StatusInfo newValue) {
        this.statusInfo = newValue;
    }

    @JsonIdentityReference
    public CopyOnWriteArrayList<ReporterCorpParam> getReporterCorpParams() {
        return alReporterCorpParams;
    }
    public void setReporterCorpParams(List<ReporterCorpParam> list) {
        if (list == null) this.alReporterCorpParams.clear();
        else this.alReporterCorpParams = new CopyOnWriteArrayList<>(list);
    }

    @JsonIdentityReference
    public CopyOnWriteArrayList<StoreInfo> getStoreInfos() {
        return alStoreInfos;
    }
    public void setStoreInfos(List<StoreInfo> list) {
        if (list == null) this.alStoreInfos.clear();
        else this.alStoreInfos = new CopyOnWriteArrayList<>(list);
    }

    @JsonIdentityReference
    public CopyOnWriteArrayList<ThreadInfo> getThreadInfos() {
        return alThreadInfos;
    }
    public void setThreadInfos(List<ThreadInfo> list) {
        if (list == null) this.alThreadInfos.clear();
        else this.alThreadInfos = new CopyOnWriteArrayList<>(list);
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
        return "ReporterCorp [" +
            "created=" + created +
            ", stopped=" + stopped +
            ", paused=" + paused +
        "]";
    }
}
 
