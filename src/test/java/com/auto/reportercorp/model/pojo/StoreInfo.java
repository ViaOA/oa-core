package com.auto.reportercorp.model.pojo;
 
import java.util.*;
import com.fasterxml.jackson.annotation.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.*;
 
@JsonIdentityInfo(generator = com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator.class, property = "storeNumber", scope = StoreInfo.class)
public class StoreInfo implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected volatile LocalDateTime created;
    protected volatile int storeNumber;

    // References to other objects
    // ReporterCorp
    protected volatile ReporterCorp reporterCorp;
    // StatusInfo
    protected volatile StatusInfo statusInfo;
    // ReportInstanceProcesses
    protected volatile CopyOnWriteArrayList<ReportInstanceProcess> alReportInstanceProcesses = new CopyOnWriteArrayList<>();
     
    public StoreInfo() {
        this.created = LocalDateTime.now();
    }
     
    public LocalDateTime getCreated() {
        return created;
    }
    public void setCreated(LocalDateTime newValue) {
        this.created = newValue;
    }
     
    public int getStoreNumber() {
        return storeNumber;
    }
    public void setStoreNumber(int newValue) {
        this.storeNumber = newValue;
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
     
    @Override
    public String toString() {
        return "StoreInfo [" +
            "created=" + created +
            ", storeNumber=" + storeNumber +
        "]";
    }
}
 
