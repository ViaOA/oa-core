package com.auto.reportercorp.model.pojo;
 
import java.util.*;
import com.fasterxml.jackson.annotation.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.*;
 
public class ReportInstanceProcessorInfo implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected volatile LocalDateTime created;
    protected volatile LocalDateTime paused;
    protected volatile int receivedCount;
    protected volatile int processedCount;
    protected volatile int fixedJsonCount;
    protected volatile int pypeErrorCount;
    protected volatile int processingErrorCount;

    // References to other objects
    // ReporterCorp
    protected volatile ReporterCorp reporterCorp;
    // StatusInfo
    protected volatile StatusInfo statusInfo;
    // ReportInfos
    protected volatile CopyOnWriteArrayList<ReportInfo> alReportInfos = new CopyOnWriteArrayList<>();
    // ReportInstanceProcesses
    protected volatile CopyOnWriteArrayList<ReportInstanceProcess> alReportInstanceProcesses = new CopyOnWriteArrayList<>();
     
    public ReportInstanceProcessorInfo() {
        this.created = LocalDateTime.now();
    }
     
    public LocalDateTime getCreated() {
        return created;
    }
    public void setCreated(LocalDateTime newValue) {
        this.created = newValue;
    }
     
    public LocalDateTime getPaused() {
        return paused;
    }
    public void setPaused(LocalDateTime newValue) {
        this.paused = newValue;
    }
     
    public int getReceivedCount() {
        return receivedCount;
    }
    public void setReceivedCount(int newValue) {
        this.receivedCount = newValue;
    }
    public int incrReceivedCount() {
        return ++this.receivedCount;
    }
     
    public int getProcessedCount() {
        return processedCount;
    }
    public void setProcessedCount(int newValue) {
        this.processedCount = newValue;
    }
    public int incrProcessedCount() {
        return ++this.processedCount;
    }
     
    public int getFixedJsonCount() {
        return fixedJsonCount;
    }
    public void setFixedJsonCount(int newValue) {
        this.fixedJsonCount = newValue;
    }
    public int incFixedJsonCount() {
        return ++fixedJsonCount;
    }
     
    public int getPypeErrorCount() {
        return pypeErrorCount;
    }
    public void setPypeErrorCount(int newValue) {
        this.pypeErrorCount = newValue;
    }
    public int incrPypeErrorCount() {
        return ++this.pypeErrorCount;
    }
     
    public int getProcessingErrorCount() {
        return processingErrorCount;
    }
    public void setProcessingErrorCount(int newValue) {
        this.processingErrorCount = newValue;
    }
    public int incrProcessingErrorCount() {
        return ++this.processingErrorCount;
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
    public CopyOnWriteArrayList<ReportInfo> getReportInfos() {
        return alReportInfos;
    }
    public void setReportInfos(List<ReportInfo> list) {
        if (list == null) this.alReportInfos.clear();
        else this.alReportInfos = new CopyOnWriteArrayList<>(list);
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
        return "ReportInstanceProcessorInfo [" +
            "created=" + created +
            ", paused=" + paused +
            ", receivedCount=" + receivedCount +
            ", processedCount=" + processedCount +
            ", fixedJsonCount=" + fixedJsonCount +
            ", pypeErrorCount=" + pypeErrorCount +
            ", processingErrorCount=" + processingErrorCount +
        "]";
    }
}
 
